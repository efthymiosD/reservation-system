-- Deterministic seed data for the single MVP venue.
-- Fixed UUIDs so tests and clients can reference them reliably.

INSERT INTO venues (id, name, description, address, timezone, created_at, updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000001',
        'Five-a-Side Football Centre',
        'Six floodlit 5x5 football fields available for hourly booking.',
        'Sportowa 5, 00-001 Warszawa',
        'Europe/Warsaw',
        now(), now(), 0);

INSERT INTO resource_groups (id, venue_id, name, type, created_at, updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001',
        'Football Fields',
        'FOOTBALL_FIELD',
        now(), now(), 0);

INSERT INTO resources (id, resource_group_id, venue_id, name, code, type, status, created_at, updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000101', 'a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001', 'Field 1', 'FIELD-01', 'FOOTBALL_FIELD', 'ACTIVE',
        now(), now(), 0),
       ('a0000000-0000-0000-0000-000000000102', 'a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001', 'Field 2', 'FIELD-02', 'FOOTBALL_FIELD', 'ACTIVE',
        now(), now(), 0),
       ('a0000000-0000-0000-0000-000000000103', 'a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001', 'Field 3', 'FIELD-03', 'FOOTBALL_FIELD', 'ACTIVE',
        now(), now(), 0),
       ('a0000000-0000-0000-0000-000000000104', 'a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001', 'Field 4', 'FIELD-04', 'FOOTBALL_FIELD', 'ACTIVE',
        now(), now(), 0),
       ('a0000000-0000-0000-0000-000000000105', 'a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001', 'Field 5', 'FIELD-05', 'FOOTBALL_FIELD', 'ACTIVE',
        now(), now(), 0),
       ('a0000000-0000-0000-0000-000000000106', 'a0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001', 'Field 6', 'FIELD-06', 'FOOTBALL_FIELD', 'ACTIVE',
        now(), now(), 0);

INSERT INTO booking_policies (venue_id, min_duration_minutes, max_duration_minutes,
                              duration_step_minutes, start_time_step_minutes, max_advance_booking,
                              updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000001',
        60, 120, 30, 30, 'P1M',
        now(), 0);

INSERT INTO cancellation_policies (venue_id, deadline_before_start_minutes, updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000001',
        120, now(), 0);

INSERT INTO pricing_policies (venue_id, hourly_price, currency, updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000001',
        80.00, 'PLN', now(), 0);

INSERT INTO opening_hours (venue_id, day_of_week, opens_at, closes_at)
VALUES ('a0000000-0000-0000-0000-000000000001', 'MONDAY', '14:00', '23:00'),
       ('a0000000-0000-0000-0000-000000000001', 'TUESDAY', '14:00', '23:00'),
       ('a0000000-0000-0000-0000-000000000001', 'WEDNESDAY', '14:00', '23:00'),
       ('a0000000-0000-0000-0000-000000000001', 'THURSDAY', '14:00', '23:00'),
       ('a0000000-0000-0000-0000-000000000001', 'FRIDAY', '14:00', '23:00'),
       ('a0000000-0000-0000-0000-000000000001', 'SATURDAY', '14:00', '23:00'),
       ('a0000000-0000-0000-0000-000000000001', 'SUNDAY', '14:00', '23:00');
