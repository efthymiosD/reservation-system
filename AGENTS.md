# reservation-system — Agent Instructions

## Status

All phases implemented (Boot 4.1.1 + Modulith 2.1.1, Java 26, 10 modules); **189 tests green** (`./mvnw verify`, Testcontainers Postgres via Rancher Desktop) incl. go-live hardening, mobile-first responsive UI, and the Playwright e2e layer. Opt-in `@Tag("keycloak")` realm-contract test excluded by default (runs only vs live Keycloak).

Highlights (details in code/ADRs, not repeated here): exclusion-constraint concurrency safety (`btree_gist` on `tstzrange`); internal identity (`sub` → app-owned `CustomerId`, auto-provisioned `customers` row); SPRING.FREE domain (b1: services are plain beans on `TransactionRunner` port, tx with `NoRollbackRule(DataIntegrityViolationException)` for constraint translation); configurable site content module (text blocks, photos, `/admin/map` drag-and-drop field-layout editor persisted as `venue.map.layout` JSON; overnight opening hours where `closesAt < opensAt`, only equal times rejected); audit `cancelled_by/cancelled_at` threading the internal actor; JWT audience validation + CORS allowlist; prod profile forbids dev-key fallback.

## Quick start

```bash
source ~/tools/env.sh          # JDK 26 + Maven 3.9.16
./mvnw test                    # all tests (integration need Docker)
./mvnw test -Dtest=ModularityTests        # module boundaries
./mvnw test -Dtest=DomainPurityTest       # domain free of framework
./mvnw test -Dtest="!*IntegrationTest,!ApplicationContextSmokeTest"  # unit only
./mvnw test -Dsurefire.excludedGroups=    # also opt-in keycloak tests (KC up)
./mvnw spotless:apply          # ALWAYS after editing Java (imports+format)
```

## E2E tests (Playwright + Python)

- `e2e-tests/`: Python 3.12 (brew `/opt/homebrew/bin/python3.12`; system 3.9 too old), pytest + pytest-playwright, venv `.venv` gitignored. CI (optional job, `continue-on-error`): `--browser chromium --browser webkit`; local Chromium default.
- Deterministic stack `docker-compose.e2e.yml` (app+postgres+keycloak; fixed clock `2026-10-12T10:00:00Z` → journeys book `2026-10-26` + **per-device day offsets** via `constants.booking_date(device)`; cancel deadline 120 min rules out in-test cleanup). **Reset = `down -v --remove-orphans && up --build -d`** (Flyway reseeds). Always reset before a full rerun.
- Implemented: `tests/test_responsive.py` only (booking journey × iPhone SE/Pixel 7/iPhone 14/iPad/Desktop, no-horizontal-scroll, navbar collapse; browser matrix passed explicitly — `pytest.ini` deliberately has NO `--browser` flags, duplicated params double-run). Booking/availability/admin specs documented in `SPECS-BACKLOG.md` for the first QA engineer.
- **No `pytest-rerunfailures`** — a rerun replays a booking into the same DB → false red.
- Auth via the real Keycloak OAuth2 form (`conftest.login`); `prompt=login` keeps sessions fresh. Compose legs: browser-facing URIs `localhost:8081`, server legs use the compose hostname; `KC_HOSTNAME_URL=http://localhost:8081` pins `iss`. Runbook: `e2e-tests/README.md`.

## Environment

- Docker (Rancher Desktop) required for integration tests. **Socket fix (applied here)**: `sudo ln -sf ~/.rd/docker.sock /var/run/docker.sock` — ryuk bind-mounts the socket into its sidecar and Rancher refuses `~/.rd/docker.sock` directly; without the symlink tests die with "Could not find a valid Docker environment". (Route B = Testcontainers Desktop: its `~/.testcontainers.properties` pins `tc.host tcp://…49340` and suppresses socket fallback — reset those pins if TCD is not running. Both routes + Windows equivalent in README.)
- Local Postgres alternative: set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (default `jdbc:postgresql://localhost:5432/reservation`).

## Keycloak (dev)

- docker-compose: KC 26 on :8081 (app :8080), realm `reservation` auto-imported from `keycloak/realm-export.json`; console admin/admin. Users: `admin/admin` (ADMIN), `alice`/`bob` (CUSTOMER). Client `reservation-api` has a subject mapper so tokens carry `sub`.
- `JwtRolesConverter`: realm_access + resource_access + flat `roles`, `ROLE_`-prefixed, uppercase.
- Identity: `CurrentCustomerResolver` maps `sub` only (no `preferred_username` fallback) → `CustomerAccountService.resolveOrProvision` provisions/reuses internal UUID.
- **Integration tests must NOT depend on Keycloak** — `PostgresIntegrationTest` sets `issuer-uri=` (dev key) + `JwtSupport.customer/admin(subject)` mock JWTs (admin adds `.authorities("ROLE_ADMIN")`).

## Build & test quirks

- JDK 26 renders `ISO_LOCAL_TIME` with seconds (`18:00:00`) — matters for JSON-body assertions.
- Lombok processor is NOT implicit on JDK 25+ — `annotationProcessorPaths` in pom must stay.
- Boot 4: `@AutoConfigureMockMvc` lives in separate `spring-boot-webmvc-test` artifact; Jackson is **3** (`tools.jackson.databind.ObjectMapper`); flyway needs `flyway-database-postgresql` added manually.
- Testcontainers 2.x: `@ServiceConnection` + `PostgreSQLContainer` broken — static `{ POSTGRES.start(); }` + `@DynamicPropertySource` (base class `PostgresIntegrationTest`, fixed clock `2026-09-01T10:00:00Z`, subclasses add `@AutoConfigureMockMvc`).
- One shared Postgres container across classes: base truncates `reservations, recurring_reservations` per test; `AdminReadApiIntegrationTest` uses 09-20/09-21 slots (others 09-04..09-07); content tests are order-independent by design. Raw-SQL tests must self-isolate (exclusion constraints).
- Tests are coupled to seed: Field 1–6 UUIDs `…00000000010{1..6}`, 60/120 min durations, 30-min grid, P1M advance, 120-min cancel deadline, 80 PLN/h (90-min booking = `120.00`). Changing `V2__seed_data.sql` breaks them.
- Opt-in keycloak tag: `-Dsurefire.excludedGroups=` runs it; `-Dtest=...` does NOT bypass the exclusion.
- Thymeleaf model attr must be `siteContent` (never `content` — collides with layout fragment parameter).
- `replaceOpeningHours` must reconcile in place, NOT delete-all+reinsert (re-insert onto delete-pending managed rows = 0 inserts → 422 later).
- Multipart: `max-file-size` 6MB > app's own 5MB check so the friendly error wins Tomcat's.
- `ReservationPageModelFactory.timeOptions` uses minutes-of-day +1440 for overnight windows (`LocalTime.isAfter` fails at midnight wrap).

## Module conventions (enforced by ModularityTests + arch tests)

- Exactly one export per module: `adapter.api` (`@NamedInterface("api")`) — interface over use cases + DTO views; domain/adapters/internal wiring private. API views **delegate** to domain logic (no rule duplication); api record components use api/shared/JDK types only.
- `webui` consumes only module APIs. `shared.domain` = shared kernel (Money, ReservationPeriod, BusinessException, ErrorCode, TransactionRunner). `administration` has no API.
- ADR-0001 module layout (authoritative: `docs/adr/0001-module-structure.adoc`, 10 rules — flat package-private `domain/`, ports as `public interface` in `domain/port`, `XxxDomainConfig` owns beans, adapters own their configs, adapters never call adapters, domain returns immutable snapshots (`…Info`/`…DataValue`), web controllers use domain ports not `adapter.api`):
  ```
  <module>/{domain/, domain/port/, domain/XxxDomainConfig,
             adapter/api/  (XxxApi + impl + config — the only export),
             adapter/persistence/, adapter/web/}
  ```
- ADR-0002: adapters never resolve/thread the logged-in `CustomerId` — ports/APIs expose implicit "me" variants (resolved in domain) + explicit variants for scheduler/audit actors; webui `CurrentUserAdvice` is the only presentation consumer of `CurrentCustomerApi` (identity exports two APIs in one package).
- ADR-0003: records in own top-level files (`RecordPlacementTest`), **package-private by default**; `public` reserved for cross-module records (`adapter.api` views, `…DataValue` snapshots) and shared webui config types (`Placement`/`VenueLayout`).
- Spotless 3.0.0 in pom — run `spotless:apply` after Java edits.

## Testing conventions

- Integration tests are black-box: HTTP API (MockMvc) or raw SQL at the DB boundary; no direct domain-service calls. Unit tests only for domain-model business logic; none for services with mocked deps. Web-UI tests cover flows/status codes only, never page internals. Concurrency (`ConcurrencyIntegrationTest`): exactly one 201, rest 409.
- Persistence adapters translate BOTH constraint races to 409: `DataIntegrityViolationException` (23P01) and `ConcurrencyFailureException` (deadlock 40P01) — lock-race losers must not 500.

## Architecture

- Tactical DDD + hexagonal, domains Spring-free (`DomainPurityTest`, ignores `package-info`); entities pure Java + Lombok (`@Getter`, PROTECTED ctor), JPA only in `adapter/persistence/*Entity`.
- Clock property-driven (`app.clock.fixed-instant`); dev JWT fallback only with empty issuer (`src/main/resources/dev/jwt-public.pem`); springdoc at `/swagger-ui.html`.
- Flyway: exactly two migrations (`V1__init_schema.sql`, `V2__seed_data.sql` — legacy V3/V4 folded in; re-importing history needs `flyway repair`).

## Key endpoint paths

- `GET /api/public/venue`; `GET /api/availability?date&start&durationMinutes`
- `POST /api/reservations` `{resourceId, startTime, durationMinutes}`; `GET /api/reservations?status&page&size`, `/{id}`, `POST /{id}/cancel`
- `GET/POST /api/admin/resources` (+ activate/deactivate/PATCH); `GET/PUT /api/admin/booking-policy|/cancellation-policy|/pricing`
- `GET /api/admin/reservations?status&page&size`; `POST /api/admin/resource-blocks` + `/override` (atomic) + `/{id}/cancel` + list `?resourceId&status`
- `GET/PUT /api/admin/venue`; `GET/PUT /api/admin/content` (+ `PUT /{key}`)
- Web-UI admin: `/admin/content` (+ venue/photos/opening-hours/{key} POSTs), `/admin/map` (+ `/layout`, `/fields`, `/field/{id}/activate|deactivate`)

## CI/CD (github.com/efthymiosD/reservation-system)

- `ci.yml` — PR → main: `mvn verify` (JDK 26, Testcontainers; no Keycloak) + optional not-blocking `e2e` job (`needs: verify`, `continue-on-error`: compose stack → pytest chromium+webkit, artifacts on failure). `pull_request` trigger only (push trigger deadlocked branch protection with duplicate checks).
- `release.yml` — push to main: verify → buildx image → Trivy (fail HIGH/CRITICAL, ignore-unfixed) → push `ghcr.io/…:sha` + `:main`, never `latest`. Jackson pins live in `pom.xml` (`jackson2.version=2.22.3`, `jackson-bom.version=3.2.3`) for the Trivy gate.
- `deploy.yml` — manual dispatch by SHA → QNAP SSH (deferred: secrets unconfigured; `docker-compose.prod.yml` carries runtime secrets).
- Branch protection: PRs required (0 approvals), check `mvn verify (unit + integration)` required, enforced for admins. Dependabot weekly (maven/docker/actions) — merge one PR at a time.
- Dockerfile: multi-stage temurin-26, non-root `app` user (owns `/app/logs`), curl healthcheck, `/usr/bin/pebble` removed for Trivy.
