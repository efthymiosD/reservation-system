-- Deterministic seed data for the single MVP venue.
-- Fixed UUIDs so tests and clients can reference them reliably.

INSERT INTO venues (id, name, description, address, timezone, created_at, updated_at, version)
VALUES ('a0000000-0000-0000-0000-000000000001',
        'Five-a-Side Football Centre',
        'Six floodlit 5x5 football fields available for hourly booking.',
        'Sportowa 35, 51-146 Wrocław',
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

-- ---------------------------------------------------------------------------
-- Editable site content for the public pages (home, about, contact).
-- about.photo — about-page hero image (empty default -> bundled default_about_page.jpeg)
-- venue.map.layout — editable JSON layout for the reservation page venue map
-- ---------------------------------------------------------------------------
INSERT INTO site_content (key, body, updated_at) VALUES
    ('home.feature.1.title', 'Live availability', now()),
    ('home.feature.1.body', 'The venue map shows every field: available or reserved — straight from the booking system.', now()),
    ('home.feature.2.title', 'Upfront pricing', now()),
    ('home.feature.2.body', 'The total price is calculated by the venue before you confirm. No surprises at the gate.', now()),
    ('home.feature.3.title', 'Easy cancellation', now()),
    ('home.feature.3.body', 'Plans change? Cancel your booking yourself until the venue''s cancellation deadline.', now()),
    ('about.booking.body', 'Reserve a field online — pick a date, start time and duration, and see the price before you confirm.', now()),
    ('contact.getting_here.body', 'The venue is located in Wrocław. Parking is available on site; the fields are floodlit for evening play.', now()),
    ('contact.contact.body', 'For bookings use the online reservation page — it shows live availability and the exact price. For anything else, talk to the staff at the venue during opening hours.', now()),
    ('contact.phone', '', now()),
    ('contact.email', '', now()),
    ('home.hero.photo', '', now()),
    ('about.photo', '', now()),
    ('venue.map.layout',
     '{"canvasWidth":1200,"canvasHeight":400,"placements":[{"resourceId":"a0000000-0000-0000-0000-000000000101","x":0,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000102","x":200,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000103","x":400,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000104","x":600,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000105","x":800,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000106","x":1000,"y":0,"width":200,"height":400}]}',
     now());
