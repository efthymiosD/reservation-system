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
-- Editable site content for the public pages (home, about, contact) and
-- non-text keys (photos, layout). Pristine seed rows carry updated_at=epoch;
-- Site content seeds only NON-TEXT keys and the venue map layout. Every
-- user-visible text field is per-language, admin-authored (admin panel
-- ?contentLang=); empty fields fall through to the built-in bundle defaults
-- (messages_*.properties), which ship localized copy for every supported
-- language out of the box. No locale-qualified rows are seeded.
INSERT INTO site_content (key, body, updated_at) VALUES
    ('contact.phone', '', now()),
    ('contact.email', '', now()),
    ('home.hero.photo', '', now()),
    ('about.photo', '', now()),
    ('venue.map.layout',
     '{"canvasWidth":1200,"canvasHeight":400,"placements":[{"resourceId":"a0000000-0000-0000-0000-000000000101","x":0,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000102","x":200,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000103","x":400,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000104","x":600,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000105","x":800,"y":0,"width":200,"height":400},{"resourceId":"a0000000-0000-0000-0000-000000000106","x":1000,"y":0,"width":200,"height":400}]}',
     now());


