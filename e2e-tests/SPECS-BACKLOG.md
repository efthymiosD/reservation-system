# SPECS BACKLOG

Welcome! This file contains the next specs to implement. Read `README.md` first (setup,
run loop, and the hard-won conventions — especially the no-collision and
no-rerun rules). `tests/test_responsive.py` is a working example of the
whole pattern: login fixture, selectors, deterministic slot, assertions.

## Runtime contract (already in place, do not change it)

| Aspect | Value |
|---|---|
| Stack | `docker compose -f docker-compose.e2e.yml` (app :8080, keycloak :8081) |
| Reset between full runs | `down -v --remove-orphans` then `up` — Flyway reseeds |
| Fixed clock | `2026-10-12T10:00:00Z`; deterministic base date 2026-10-26 |
| Users | `alice/alice` (CUSTOMER), `bob/bob` (CUSTOMER), `admin/admin` (ADMIN) |
| Seed | Field 1-6 = `a0000000-…-00000000010{1..6}`, 80.00 PLN/h, hours 14:00-23:00 Europe/Warsaw |
| Families | `constants.py` (dates/UUIDs/prices), `conftest.py` (`login()`, `_stack_ready`) |

## Selector cheat-sheet (stable ids/classes in `templates/`)

| Element | Selector |
|---|---|
| Date input | `#date` (type=date) |
| Start select | `#start` (options 14:00…22:30, 30-min steps) |
| Duration select | `#durationMinutes` (labels "1 h", "1 h 30 min", "2 h") |
| Field tile | `.venue-map .resource[data-label='Field 1']` (classes `resource--available` / `resource--reserved` / `resource--blocked`) |
| Tile state text | `.resource__state` ("✓ free" / "✕ reserved") |
| Hidden resource id | `#hidden-resource-id` (filled by JS on select) |
| Reserve button | `#reserve-button` (disabled until a tile is selected) |
| Already-held tooltip span | in `#reserve-form` (`th:if` page.held) |
| Success redirect | `/reservations/{id}/confirmation` |
| Booking failure | redirect back to `/reserve?…` with flash error |
| Nav login | `#mainNav` link `Log in` → `/oauth2/authorization/keycloak`; logout: POST form in nav |
| Pipelines | Keycloak login form: `#username`, `#password`, submit button |

## Spec 1 — `tests/test_booking.py` (PRIORITY)

**Purpose:** the highest-value end-to-end reservation journey incl. the
already-taken-slot guardrail.

**Part 1 (alice books):**
1. `login(page, alice, alice)`; `goto /reserve`
2. date = `constants.booking_date()`; start = `18:00`; duration = `1 h`
3. Click `.resource[data-label='Field 1']` → `#hidden-resource-id` =
   `a0000000-…0101`, `#reserve-button` becomes enabled and is clicked
4. Expect URL match `/reservations/[0-9a-f-]+/confirmation`; the page
   shows `80.00`, `PLN`, `Field 1`, `18:00` – `19:00`
5. `goto /my-reservations` → a card contains `Field 1` and the booked date

**Part 2 (bob sees it taken):**
1. Use a *fresh browser context* (do not reuse alice's session): log in as
   `bob/bob`
2. Same slot, same field: the Field 1 tile carries class
   `resource--reserved` (assert `expect(tile).not_to_be_available()`,
   i.e. not `resource--available`) and its `.resource__state` reads
   `✕ reserved`
3. As an extra guard, assert the tile's `aria-disabled` is `true`

**Hints:** reuse `book_first_field()`/`_no_horizontal_scroll()` from
`test_responsive.py` (consider extracting to `tests/support.py` first —
that is the moment to extract, see the principle in
docs/plans/playwright-e2e-introduction.md). One booking date suffices:
alice books it once, bob only *observes*. Do not add reruns.

## Spec 2 — `tests/test_availability.py`

**Purpose:** the availability render path without a booking.

1. login alice → `/reserve`; no booking needed
2. For a far-future slot (`booking_date("iPhone 14") + 7` days): all six
   tiles render with class `resource--available` incl. `Field 1`; assert
   `len(page.locator(".resource--available")) == 6`
3. Start select (`#start`) has 30-min-step options (14:00, 14:30, …);
   durations are exactly `1 h`, `1 h 30 min`, `2 h`
4. No tile has more than one state class; after (optional) a `.resource--blocked`
   exists when an admin block is present (deferred — needs the block API;
   leave out for now)
5. No horizontal overflow assert applies at desktop AND iPhone SE sizes

## Spec 3 (optional, later) — `tests/test_admin_journeys.py`

**Purpose:** admin read/actuation sanity. login `admin/admin`:
- `/admin/reservations` lists the row created by spec 1 (needs spec 1
  executed first in the same full run!) and cancel via the row button;
  the row's status flips
- `/admin/pricing`: change rate to 100 + save; `/api/public` pricing reads
  100 afterwards; change back (or accept `down -v` before next run)

Deliberately NOT in scope (keep it that way until asked): everything the
`@WebMvc`/integration tests already cover — validation rules, policy
limits, pricing math, concurrency (exclusion constraints).

## Definition of done for each new spec

- runs green twice consecutively (`down -v` reset between)
- chromium + webkit locally
- no selector inside templates duplicated in more than one file
  (extract page-object style helper classes only when a second spec needs
  the same interactions)
