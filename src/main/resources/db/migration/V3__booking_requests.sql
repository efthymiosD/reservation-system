-- Booking-request link workflow (V3): a coach shares an opaque WhatsApp link,
-- the customer submits a booking REQUEST, the coach approves or rejects it in
-- the authenticated app. Requests NEVER reserve or block a slot: there is no
-- exclusion constraint on this table on purpose — availability is only
-- revalidated authoritatively at approval time through the regular
-- reservation creation path.

CREATE TABLE booking_request_links
(
    id         UUID PRIMARY KEY,
    label      VARCHAR(80) NOT NULL,
    -- Only the SHA-256 hash of the shareable token is stored; the raw token
    -- is shown once at creation/regeneration and lives in nobody's database.
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

-- At most ONE active (non-revoked) link — enforced at the DATABASE level: a
-- racing/duplicated issue cannot mint two reusable links.
CREATE UNIQUE INDEX booking_request_links_active_unique
    ON booking_request_links ((1)) WHERE revoked_at IS NULL;

CREATE TABLE booking_requests
(
    id                          UUID PRIMARY KEY,
    -- Request lifecycle, separate from reservation status: PENDING reserves
    -- nothing. Only APPROVED requests carry a created reservation id.
    status                      VARCHAR(16)  NOT NULL
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    resource_id                 UUID         NOT NULL REFERENCES resources (id),
    -- Venue-owner wall-clock times (same semantics as the reserve API input).
    start_at                    TIMESTAMP    NOT NULL,
    duration_minutes            INT          NOT NULL,
    end_at                      TIMESTAMP    NOT NULL,
    customer_name               VARCHAR(120) NOT NULL,
    contact                     VARCHAR(200) NOT NULL,
    note                        VARCHAR(500),
    link_id                     UUID         NOT NULL REFERENCES booking_request_links (id),
    -- Per-form-visit dedupe key: same GET-page resubmission (refresh/back
    -- button) resolves to the original request instead of double-queuing.
    submission_key              VARCHAR(64)  NOT NULL UNIQUE,
    advisory_free_at_submission BOOLEAN      NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    decided_at                  TIMESTAMPTZ,
    decided_by                  VARCHAR(120),
    reservation_id              UUID,
    version                     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX booking_requests_status_created_idx ON booking_requests (status, created_at);
CREATE INDEX booking_requests_link_idx ON booking_requests (link_id);
