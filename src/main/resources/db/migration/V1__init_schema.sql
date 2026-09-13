CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------------
-- Venues
-- ---------------------------------------------------------------------------
CREATE TABLE venues
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    address     TEXT,
    timezone    VARCHAR(64)  NOT NULL DEFAULT 'Europe/Warsaw',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------------
-- Opening hours: one interval per venue per weekday
-- ---------------------------------------------------------------------------
CREATE TABLE opening_hours
(
    venue_id    UUID        NOT NULL REFERENCES venues (id),
    day_of_week VARCHAR(9)  NOT NULL,
    opens_at    TIME        NOT NULL,
    closes_at   TIME        NOT NULL,
    PRIMARY KEY (venue_id, day_of_week),
    CONSTRAINT opening_hours_day_check CHECK (day_of_week IN
                                              ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY',
                                               'SATURDAY', 'SUNDAY')),
    CONSTRAINT opening_hours_interval_check CHECK (closes_at > opens_at)
);

-- ---------------------------------------------------------------------------
-- Booking policy (per venue)
-- ---------------------------------------------------------------------------
CREATE TABLE booking_policies
(
    venue_id                UUID PRIMARY KEY REFERENCES venues (id),
    min_duration_minutes    INT         NOT NULL,
    max_duration_minutes    INT         NOT NULL,
    duration_step_minutes   INT         NOT NULL,
    start_time_step_minutes INT         NOT NULL,
    max_advance_booking     VARCHAR(32) NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL,
    version                 BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT booking_policy_duration_bounds CHECK (min_duration_minutes > 0 AND max_duration_minutes >= min_duration_minutes),
    CONSTRAINT booking_policy_steps CHECK (duration_step_minutes > 0 AND start_time_step_minutes > 0)
);

-- ---------------------------------------------------------------------------
-- Cancellation policy (per venue). Deadline evaluated dynamically at cancel time.
-- ---------------------------------------------------------------------------
CREATE TABLE cancellation_policies
(
    venue_id                    UUID PRIMARY KEY REFERENCES venues (id),
    deadline_before_start_minutes INT         NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    version                     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT cancellation_policy_deadline CHECK (deadline_before_start_minutes >= 0)
);

-- ---------------------------------------------------------------------------
-- Pricing policy (per venue). MVP single hourly price.
-- ---------------------------------------------------------------------------
CREATE TABLE pricing_policies
(
    venue_id     UUID PRIMARY KEY REFERENCES venues (id),
    hourly_price NUMERIC(12, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    updated_at   TIMESTAMPTZ    NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pricing_policy_price CHECK (hourly_price >= 0)
);

-- ---------------------------------------------------------------------------
-- Resource groups
-- ---------------------------------------------------------------------------
CREATE TABLE resource_groups
(
    id         UUID PRIMARY KEY,
    venue_id   UUID         NOT NULL REFERENCES venues (id),
    name       VARCHAR(200) NOT NULL,
    type       VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------------
-- Resources
-- ---------------------------------------------------------------------------
CREATE TABLE resources
(
    id                UUID PRIMARY KEY,
    resource_group_id UUID         NOT NULL REFERENCES resource_groups (id),
    venue_id          UUID         NOT NULL REFERENCES venues (id),
    name              VARCHAR(200) NOT NULL,
    code              VARCHAR(64)  NOT NULL,
    type              VARCHAR(64)  NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT resource_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT resource_code_unique UNIQUE (venue_id, code)
);

CREATE INDEX resources_group_idx ON resources (resource_group_id);
CREATE INDEX resources_venue_idx ON resources (venue_id);

-- ---------------------------------------------------------------------------
-- Reservations
-- ---------------------------------------------------------------------------
CREATE TABLE reservations
(
    id                       UUID PRIMARY KEY,
    resource_id              UUID           NOT NULL REFERENCES resources (id),
    customer_id              VARCHAR(128)   NOT NULL,
    start_time               TIMESTAMPTZ    NOT NULL,
    end_time                 TIMESTAMPTZ    NOT NULL,
    status                   VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    price_amount             NUMERIC(12, 2) NOT NULL,
    price_currency           VARCHAR(3)     NOT NULL,
    created_at               TIMESTAMPTZ    NOT NULL,
    cancelled_at             TIMESTAMPTZ,
    cancelled_by             VARCHAR(128),
    recurring_reservation_id  UUID,
    version                  BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT reservation_period_check CHECK (end_time > start_time),
    CONSTRAINT reservation_status_check CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

-- The two hard concurrency invariants. Enforced by PostgreSQL, not the app.
-- Cancelled reservations are excluded from both constraints.
ALTER TABLE reservations
    ADD CONSTRAINT reservations_no_resource_overlap
        EXCLUDE USING gist (
            resource_id WITH =,
            (tstzrange(start_time, end_time, '[)')) WITH &&
            ) WHERE (status = 'ACTIVE');

ALTER TABLE reservations
    ADD CONSTRAINT reservations_no_customer_overlap
        EXCLUDE USING gist (
            customer_id WITH =,
            (tstzrange(start_time, end_time, '[)')) WITH &&
            ) WHERE (status = 'ACTIVE');

CREATE INDEX reservations_resource_lookup_idx ON reservations (resource_id, status, start_time);
CREATE INDEX reservations_customer_lookup_idx ON reservations (customer_id, status, start_time);
CREATE INDEX reservations_recurring_reservation_lookup_idx ON reservations (recurring_reservation_id);

-- ---------------------------------------------------------------------------
-- Internal, application-owned customer identity.
-- Decouples the domain's CustomerId from the external IdP (Keycloak).
-- `idp_subject` binds the external identity (Keycloak `sub` claim) to the
-- stable internal UUID. Auto-provisioned on first authenticated request.
-- ---------------------------------------------------------------------------
CREATE TABLE customers
(
    customer_id  UUID         NOT NULL PRIMARY KEY,
    idp_subject  VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(255) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Recurring reservations: a customer is assigned a recurring weekly slot on a
-- resource (venue-local weekday + start/end time). The materializer turns each
-- upcoming occurrence inside the recurring reservation's booking window
-- (1/3/6 months, `window_months`) into a real reservation.
-- `next_occurrence` is the cursor: the next date (venue-local) to materialize.
-- ---------------------------------------------------------------------------
CREATE TABLE recurring_reservations
(
    id              UUID        NOT NULL PRIMARY KEY,
    resource_id     UUID        NOT NULL REFERENCES resources (id),
    customer_id     UUID        NOT NULL,
    weekday         VARCHAR(9)  NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    next_occurrence DATE        NOT NULL,
    window_months   INT         NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    cancelled_at    TIMESTAMPTZ,
    cancelled_by    VARCHAR(128),
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT recurring_reservation_period_check CHECK (end_time > start_time),
    CONSTRAINT recurring_reservation_status_check CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT recurring_reservation_window_check CHECK (window_months IN (1, 3, 6)),
    CONSTRAINT recurring_reservation_day_check CHECK (weekday IN
                                                      ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY',
                                                       'SATURDAY', 'SUNDAY'))
);

CREATE INDEX recurring_reservations_resource_idx ON recurring_reservations (resource_id, status);
CREATE INDEX recurring_reservations_customer_idx ON recurring_reservations (customer_id, status);