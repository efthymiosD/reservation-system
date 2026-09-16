# reservation-system — Agent Instructions

## Implementation status

All phases are implemented and compiling; **178 tests green** (includes Testcontainers Postgres integration/concurrency via Rancher Desktop/Docker) plus go-live hardening and configurable site content. An opt-in `@Tag("keycloak")` realm-contract test is excluded by default and runs only against a live Keycloak.

Go-live hardening (live bookings only; no payments/notifications/waitlist/multi-venue):
- **Observability** — actuator (`/actuator/health` + liveness/readiness probes, `show-details: never`, health permitted unauthenticated; verified by `ActuatorHealthIntegrationTest`), logging (`application.yml` console/file pattern + size/rotation, Tomcat access log), lifecycle INFO logs on create/cancel/override.
- **Audit trail** — `cancelled_by` on both `reservations` and `resource_blocks` + `resource_blocks.cancelled_at` (folded into `V1__init_schema.sql` after the migration merge); actor is the resolved **internal `CustomerId`**; admin overrides and block cancels thread the actor through the web→service→domain path.
- **Secret hygiene** — dev RSA keys gitignored, `.env.example` documents secrets, `docker-compose.yml` reads creds from env (dev defaults), `application-prod.yml` requires `JWT_ISSUER_URI` + DB env vars and forbids the dev-key fallback (`app.security.allow-dev-key=false`) and disables springdoc unless `SPRINGDOC_ENABLED`.
- **JWT hardening** — optional audience validation via `AudienceJwtValidator` when `app.security.audience` (default `reservation-api`) is set, combined with the issuer validator in `ValidatingJwtDecoder`; **CORS** allowlist via `app.security.cors.allowed-origins` (empty default).
- **Admin read-side** — list-all reservations (pagination + status filter), list-all/none resource blocks, GET current booking/cancellation/pricing policies; customer reservations support page/size + status filter.
- **Configurable site content** — `content` module (`api` named interface, Spring-free domain service on `TransactionRunner` + `Clock`): `SiteContentService` reads/updates text blocks keyed by stable `key`; `VenueService.updateProfile` + `ContentAdminController` expose `GET/PUT /api/admin/content` and `GET/PUT /api/admin/venue`; webui `ContentAdvice` exposes `@ModelAttribute("siteContent")` map for `home/about/contact` templates (`${siteContent['key'] ?: 'fallback'}`); `AdminContentController` renders `/admin/content` (venue profile, photos, opening hours, text blocks); `AdminMapController` renders `/admin/map` drag-and-drop field-layout editor. The homepage hero falls back to the bundled `default_homepage.jpeg` when `home.hero.photo` is empty; the about page falls back to the bundled `default_about_page.jpeg` when `about.photo` is empty. **Reservation-page map fields live on `/admin/map`**, a graphical drag-and-drop grid editor (pointer events + SVG CTM so it works at any scale incl. touch/mobile): move fields by dragging the body, resize via the corner handle, positions snap to a 20-unit grid; saves the whole layout via per-field `layout`+`field` form inputs posted to `POST /admin/map/layout` (no-JS fallback works), adds fields via `/admin/map/fields`, toggles activity via `/admin/map/field/{id}/activate|deactivate`; layout is persisted as `venue.map.layout` JSON, read dynamically by `VenueLayoutLoader` with classpath `webui/venue-layout.json` fallback, drawn by `map-editor.js`/`static/css` map-editor block. Public `home/about/contact` templates render DB texts with hardcoded `?:` fallbacks via the `siteContent` model attribute; keys are stable and i18n-ready. **Opening hours support overnight windows**: `DailyOpeningHours` accepts `closesAt` before `opensAt` (e.g. 20:00–02:00 = open until 02:00 the following day); equal times are rejected (`INVALID_OPENING_HOURS`). **Contact page** merges address, getting-here text, phone/email (configurable via `contact.phone`/`contact.email` site-content keys) and the weekly opening-hours table; `/opening-hours` redirects to `/contact`. Venue name propagates to the nav bar, footer, and page title from the DB (`VenueAdvice`). Admin messages shown once via the global `fragments/messages` fragment (page-local duplicates removed).

- **Phase 0 Foundation** — `pom.xml` (Boot 4.1.1, Modulith 2.1.1, Lombok 1.18.46, Testcontainers 2.0.5), `ReservationApplication`, security (`SecurityConfig`/`JwtDecoderConfig`/`JwtRolesConverter`), `application.yml`, `docker-compose.yml`, dev RSA keypair, test infra (`PostgresIntegrationTest`, `JwtSupport`, `ModularityTests`).
- **Phase 1 Schema** — `V1__init_schema.sql` (venues, opening_hours, booking/cancellation/pricing policies, resource_groups, resources, reservations + exclusion constraints, resource_blocks + audit columns, customers, site_content; btree_gist), `V2__seed_data.sql` (incl. site content + `venue.map.layout`). Migrations are consolidated into exactly two: schema + seed.
- **Phase 2 Core domain** — `shared`, `venue`, `resource`, `policy`, `pricing`, `identity` modules + admin/public endpoints.
- **Phase 3 Reservation** — `CreateReservationService`, `CancelReservationService`, `ReservationQueryService`, persistence adapter (constraint translation), `ReservationController`.
- **Phase 4 Availability** — `AvailabilityService`, `AvailableResource`, `AvailabilityController`.
- **Phase 5 Admin/Blocks** — `administration` module (`BlockResourceService`, `OverrideResourceBlockService`), `ResourceBlockAdminController`, resource admin CRUD.
- **Phase 6 Tests** — unit tests pass. Integration tests (`DatabaseConstraintsIntegrationTest`, `ReservationApiIntegrationTest`, `BlockApiIntegrationTest`, `ConcurrencyIntegrationTest`) run green against Testcontainers Postgres (Docker via Rancher Desktop).
- **Phase 7 Internal identity** — `customers` table (in `V1`) + `identity` module port/persistence so the domain operates on an **app-owned internal `CustomerId`** (UUID) decoupled from the IdP: `CustomerAccountService.resolveOrProvision(sub)` auto-provisions a `customers` row on first authenticated request and reuses it thereafter. `CurrentCustomerResolver` (web adapter in `identity.adapter.in`) maps the Keycloak `sub` claim → internal `CustomerId` (no `preferred_username` fallback). `CustomerAccountServiceTest`, `CustomerIdentityIntegrationTest` joined the suite; opt-in `KeycloakTokenContractIntegrationTest` (`@Tag("keycloak")`) verifies issued tokens carry `sub` and resolve to a stable `CustomerId`.
- **Phase 8 Configurable site content** — `content` module (`api` named interface, Spring-free domain service on `TransactionRunner` + `Clock`): `SiteContentService` reads/updates text blocks keyed by stable `key`; `VenueService.updateProfile` + `ContentAdminController` expose `GET/PUT /api/admin/content` and `GET/PUT /api/admin/venue`; webui `ContentAdvice` exposes `@ModelAttribute("siteContent")` map for `home/about/contact` templates (`${siteContent['key'] ?: 'fallback'}`); `AdminContentController` renders `/admin/content` (venue profile, photos, opening hours, text blocks); `AdminMapController` renders `/admin/map` drag-and-drop field-layout editor. The homepage hero falls back to the bundled `default_homepage.jpeg` when `home.hero.photo` is empty; the about page falls back to the bundled `default_about_page.jpeg` when `about.photo` is empty. **Reservation-page map fields live on `/admin/map`**, a graphical drag-and-drop grid editor (pointer events + SVG CTM so it works at any scale incl. touch/mobile): move fields by dragging the body, resize via the corner handle, positions snap to a 20-unit grid; saves the whole layout via per-field `layout`+`field` form inputs posted to `POST /admin/map/layout` (no-JS fallback works), adds fields via `/admin/map/fields`, toggles activity via `/admin/map/field/{id}/activate|deactivate`; layout is persisted as `venue.map.layout` JSON, read dynamically by `VenueLayoutLoader` with classpath `webui/venue-layout.json` fallback, drawn by `map-editor.js`/`static/css` map-editor block. **Opening hours support overnight windows**: `DailyOpeningHours` accepts `closesAt` before `opensAt` (e.g. 20:00–02:00 = open until 02:00 the following day); equal times are rejected (`INVALID_OPENING_HOURS`). **Contact page** merges address, getting-here text, phone/email (configurable via `contact.phone`/`contact.email` site-content keys) and the weekly opening-hours table; `/opening-hours` redirects to `/contact`. Venue name propagates to the nav bar, footer, and page title from the DB (`VenueAdvice`). Admin messages shown once via the global `fragments/messages` fragment (page-local duplicates removed). Tests: `SiteContentAdminApiIntegrationTest`, `AdminContentPageIntegrationTest`, `AdminMapPageIntegrationTest`.

- **b1 — framework-agnostic domain (transaction port)** — all domain services are now **Spring-free** and wired in `DomainServicesConfig` (package `com.decoupledx.reservation`, the module root) as explicit `@Bean`s. `@Transactional`/`@Service` were removed from every domain service; transaction control goes through the `TransactionRunner` port (`shared.domain`) implemented by `SpringTransactionRunner` (`shared.adapter`), which uses two `TransactionTemplate`s (read-write + read-only) and a `RuleBasedTransactionAttribute` with `NoRollbackRuleAttribute(DataIntegrityViolationException.class)` so the persistence adapters' internal constraint translation isn't rolled back. Write services wrap bodies in `tx.run(Supplier)`, read services (`ReservationQueryService`, `AvailabilityService`) drop `@Transactional(readOnly)` entirely. `ModularityTests` now requires each module's `domain.{model,service,port}` (and `shared.domain`, `identity.adapter.in`) packages to be exposed as Modulith named interfaces via `@NamedInterface` `package-info.java` files; `DomainPurityTest` therefore excludes `package-info` classes (structural metadata only, no domain logic) from the purity rules.

## Quick start

```bash
source ~/tools/env.sh          # sets JAVA_HOME + PATH for JDK 26 + Maven 3.9.16
./mvnw test                    # run all tests (unit + ArchUnit; integration tests need Docker)
./mvnw test -Dtest=ModularityTests   # verify module boundaries
./mvnw test -Dtest=DomainPurityTest  # enforce domain-free-of-framework
./mvnw test -Dtest=ReservationPeriodTest MoneyTest  # targeted unit tests
./mvnw test -Dtest="!*IntegrationTest,!ApplicationContextSmokeTest"  # unit tests only
./mvnw test -Dsurefire.excludedGroups=  # also run opt-in @Tag("keycloak") tests (Keycloak must be up)
```

## Environment

- JDK 26.0.2.1 + Maven 3.9.16 (pre-installed; activate via `source ~/tools/env.sh`, which now sets `JAVA_HOME` to `~/Library/Java/JavaVirtualMachines/openjdk-26.0.2.1`)
- **Docker required for integration tests** — run via **Rancher Desktop** (dockerd/moby engine). Docker binaries are on PATH via the `~/.rd/bin` block Rancher added to `~/.zshrc`.
- If using a local Postgres instead of Testcontainers, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` env vars; the app defaults to `jdbc:postgresql://localhost:5432/reservation`.

## Security / Keycloak

- **OAuth2 resource server (JWT)**. Auth for `/api/**` comes from Keycloak by default: `app.security.issuer-uri` defaults to `http://localhost:8081/realms/reservation` (override with `JWT_ISSUER_URI`). Set it to empty to fall back to the bundled dev RSA public key.
- **Keycloak runs via docker-compose** on host port **8081** (app stays on 8080). Realm `reservation` is auto-imported from `keycloak/realm-export.json` (`quay.io/keycloak/keycloak:26.0`, `start-dev --import-realm`, admin console at http://localhost:8081 with `admin`/`admin`).
- **Test users**: `admin`/`admin` (realm role `ADMIN`), `alice`/`alice` (realm role `CUSTOMER`). Client `reservation-api` (public, direct-access grant enabled) carries a **subject protocol mapper** so access tokens always include the `sub` claim (Keycloak user UUID).
- **Role mapping**: `JwtRolesConverter` reads Keycloak's `realm_access.roles` (and `resource_access.*.roles`), plus a flat `roles` claim as fallback, prefixing with `ROLE_`. Role names are uppercase to match `hasRole('ADMIN')`/`hasRole('CUSTOMER')`.
- **Internal identity (`sub` → `CustomerId`)**: `CurrentCustomerResolver` maps the `sub` claim **only** (no `preferred_username` fallback) to an **app-owned internal `CustomerId`** via `CustomerAccountService.resolveOrProvision(sub)`, which auto-provisions a `customers` row (see the `customers` table in `V1__init_schema.sql`) on first authenticated request and reuses it thereafter. The domain persists only the internal UUID, decoupled from Keycloak.
- **Opt-in Keycloak contract test**: `KeycloakTokenContractIntegrationTest` (`@Tag("keycloak")`) hits the real realm token endpoint, asserts the token carries `sub`, and resolves it to a stable `CustomerId`. Excluded by default (surefire `excludedGroups=keycloak`); run it with `./mvnw test -Dsurefire.excludedGroups=` when Keycloak is up.
- **Integration tests must NOT depend on Keycloak** — `PostgresIntegrationTest` sets `app.security.issuer-uri=` (empty) so tests use the dev key + `JwtSupport` mock tokens. Keep it that way. `JwtSupport` mock tokens set `sub` via `jwt.subject(...)`.

## Build & test quirks

- **JDK 26 formats `ISO_LOCAL_TIME` with seconds** — `LocalTime.of(18,0)` renders as `18:00:00`, not `18:00`. Matters wherever JSON templates append `:00` (e.g. `ConcurrencyIntegrationTest` JSON bodies).

- **Lombok annotation processor is NOT implicit on JDK 25+** — `pom.xml` explicitly configures `annotationProcessorPaths` for lombok 1.18.46. Do not remove it.
- **`spring-boot-webmvc-test` must be declared separately** — Spring Boot 4.1.1 does not bundle `@AutoConfigureMockMvc` in `spring-boot-starter-test`. It lives in `org.springframework.boot:spring-boot-webmvc-test` under `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- **Spring Boot 4 ships Jackson 3 (`tools.jackson`)** — the auto-configured bean is `tools.jackson.databind.ObjectMapper`, NOT `com.fasterxml.jackson.databind.ObjectMapper`. Tests injecting `ObjectMapper` must import `tools.jackson.databind.*`. (Old `com.fasterxml` Jackson 2 is only on the classpath transitively via springdoc.) API: `readTree(String)` returns `JsonNode`, `JsonNode.get(String).asText()` — same as Jackson 2.
- **Spring Boot 4 `spring-boot-starter-flyway` only brings `flyway-core`, not PostgreSQL support** — you MUST also add `org.flywaydb:flyway-database-postgresql` (version-managed by Boot BOM). Without it, startup fails with `FlywayException: Unsupported Database: PostgreSQL 17.11`.
- **Config-schema integration tests share one Postgres container and dataset** — data persists across test classes. `PostgresIntegrationTest` TRUNCATEs `recurring_reservations, reservations` in `@BeforeEach`; `AdminReadApiIntegrationTest` uses the 09-20/09-21 slots so it never collides with the 09-04..09-07 slots of `ReservationApiIntegrationTest`/`BlockApiIntegrationTest`; keep any raw-SQL tests self-isolated or they collide on the exclusion constraints. Content tests mutate venue profile + `site_content` (persist across classes — keep them order-independent).
- **Opt-in `@Tag("keycloak")` tests are excluded by default** via surefire `<excludedGroups>${surefire.excludedGroups}</excludedGroups>` (property defaults to `keycloak`). Run them with `-Dsurefire.excludedGroups=`. Setting `-Dtest=...` does not bypass this exclusion.
- **`@ServiceConnection` + Testcontainers 2.x `PostgreSQLContainer` does NOT work** — use a static initializer (`POSTGRES.start()` in `static {}`) in `PostgresIntegrationTest` and `@DynamicPropertySource` instead.
- Integration test base class: `PostgresIntegrationTest` (`@SpringBootTest(properties = "app.clock.fixed-instant=2026-09-01T10:00:00Z")`). Subclasses add `@AutoConfigureMockMvc`.
- `JwtSupport.customer(subject)` / `JwtSupport.admin(subject)` create mock JWTs for `MockMvc`. Admin needs `.authorities("ROLE_ADMIN")`.
- **Integration tests are coupled to seed data**: they reference fixed resource UUIDs `a0000000-...-00000000010{1..6}` (Field 1–6) and rely on seeded defaults (durations 60/120 min, 30-min grid, P1M advance window, 120-min cancellation deadline, 80 PLN/hr). Changing `V2__seed_data.sql` will break them. E.g. a 90-min booking at 80 PLN/hr is asserted as `priceAmount=120.00`.
- **Thymeleaf model attribute must be `siteContent`, never `content`** — `layout.html` declares `th:fragment="page(title, content)"`, so `${content[...]}` resolves to the fragment parameter string, not the map, and throws `TemplateProcessingException`. The webui `ContentAdvice` attribute is deliberately named `siteContent`.
- **Venue `opening_hours` wipe trap** — `VenuePersistenceAdapter.replaceOpeningHours` MUST NOT be swapped back to a delete-all-then-reinsert strategy: re-inserting rows with the same composite PK `(venue_id, day_of_week)` merges onto the delete-pending managed entities, whose unchanged state stops Hibernate from scheduling an insert → 0 rows and 422 `OUTSIDE_OPENING_HOURS` on every later booking. It reconciles in place (update existing / persist new / delete removed days). DB constraint is `CHECK (closes_at <> opens_at)` — overnight windows (`closesAt < opensAt`, e.g. 20:00–02:00) are valid; only equal times are rejected.
- **Multipart upload limits** — slow uploads are rejected in Tomcat *before* the controller when the file exceeds `spring.servlet.multipart.max-file-size` (default 1 MB). It is set to **6MB** so the app's own authoritative 5 MB check (`AdminContentController.MAX_PHOTO_BYTES`) produces the friendly error.
- **`ReservationPageModelFactory.timeOptions` uses minutes-of-day arithmetic** — overnight opening hours (`closesAt < opensAt`) require the end time to be shifted by +24 hours when comparing candidate start times. `LocalTime.isAfter`/`isBefore` cannot be used here because `00:00` is before `22:00` even though it is logically later. The method uses `minutesOfDay()` to convert times to integers and adds 1440 for overnight windows.

## Module API convention (enforced by ModularityTests)

Every module exports **exactly one API**: its `…api` package (`@NamedInterface("api")`) — an interface over the module's use cases plus the pure value types / DTO views that cross module boundaries. Everything else (domain model/service/port, adapters, `…internal` wiring) is private; `ModularityTests.verify()` fails on any internal cross-module import.

- API views **delegate** to domain logic (e.g. `OpeningHoursView.fits`, `BookingPolicy.validate*`) — business rules are never duplicated in the API layer; api record components reference only api/shared/JDK types.
- `webui` (and future REST-for-mobile) consume only module APIs; each module's own REST adapter may use its own internals.
- `reservation.api.ReservationApi` is implemented by a module-internal facade over Create/Cancel/Query services; per-module `…internal/*ModuleConfig` classes do the bean wiring (root `DomainServicesConfig` was removed).
- `shared.domain` remains the exposed shared kernel (Money, ReservationPeriod, BusinessException, ErrorCode, TransactionRunner).
- `administration` has no API (single admin controller, internal only).

## Testing conventions

- **Integration tests are black-box only**: through the HTTP API (MockMvc, `JwtSupport`/`WebUserSupport` principals) or raw SQL at the DB boundary (`DatabaseConstraintsIntegrationTest`). No direct domain-service calls.
- **Unit tests only for business logic**, which lives in domain models (`BookingPolicyTest`, `ReservationTest`, `MoneyTest`, …). **No unit tests for services with mocked dependencies.**
- **Web-UI tests cover core flows only** (login/logout redirections, page reachability, status codes) — **never page internals** (no HTML text, buttons, tooltips, labels). Page rendering is verified manually/live.
- Concurrency tests race through the HTTP API (`ConcurrencyIntegrationTest`): exactly one 201 among 409s.

## Architecture

- **Spring Boot 4.1.1 + Spring Modulith 2.1.1** modular monolith (10 modules), Java 26, Maven
- **Tactical DDD + pragmatic hexagonal**: domain aggregates/value objects + services in `domain/`, repository ports in `domain/port/`, adapters in `adapter/in/web` (controller) and `adapter/out/persistence` (JPA). Framework imports (Spring/JPA) must NOT appear in `..domain..` — enforced by `DomainPurityTest` (ArchUnit, which ignores `package-info` metadata classes).
- 10 modules: `shared`, `venue`, `resource`, `reservation`, `availability`, `pricing`, `policy`, `identity`, `administration`, `content`
- **b1 / framework-agnostic domain**: domain services carry no `@Service`/`@Transactional` (wired in root `DomainServicesConfig`), transactions go through the `TransactionRunner` port (`shared/domain`) implemented by `SpringTransactionRunner` (`shared/adapter`); `shared` itself is split into `shared/domain` (Money, ReservationPeriod, BusinessException, ErrorCode, TransactionRunner) + `shared/adapter` (security/, web/, ClockConfig, OpenApiConfig, SpringTransactionRunner).
- **Modulith boundaries**: each module's `domain/{model,service,port}` (plus `shared/domain` and `identity/adapter/in`) are exposed as named interfaces via `@NamedInterface` in `package-info.java` files so `ModularityTests` (`ApplicationModules.verify()`) passes while domain stays Spring-free.
- **No persistence in domain**: entities are pure Java (Lombok `@Getter` + `@NoArgsConstructor(access=PROTECTED)`); JPA `@Entity` lives only in `adapter/out/persistence/*Entity`.
- **Concurrency safety**: PostgreSQL `btree_gist` exclusion constraints on `tstzrange` for ACTIVE reservations (per-resource AND per-customer) and resource blocks. Cancelled rows are excluded from constraints.
- **Clock is property-driven** via `app.clock.fixed-instant` — integration tests fix it to `2026-09-01T10:00:00Z` for deterministic time.
- **Dev JWT**: dev RSA keypair at `src/main/resources/dev/jwt-public.pem`. Used only when no `JWT_ISSUER_URI` is set (empty issuer).
- **Flyway migrations** in `src/main/resources/db/migration/` — exactly two: `V1__init_schema.sql` (all schema incl. `site_content`) and `V2__seed_data.sql` (all default data incl. site content + `venue.map.layout`). The legacy V3/V4 (time/period merge, later site-content/V4 tweaks) were folded into V1/V2. A local DB that already applied the old V3/V4 must be recreated (or `flyway repair`) after this consolidation, since those files no longer exist.
- **OpenAPI**: `springdoc` at `http://localhost:8080/swagger-ui.html`.

## Module package layout

Each module follows:
```
com.decoupledx.reservation.<module>/
  ├── domain/
  │   ├── model/    # pure aggregates/value objects (no framework imports)
  │   ├── service/  # framework-agnostic services (wired in root DomainServicesConfig)
  │   └── port/     # repository interfaces
  └── adapter/
      ├── in/web/          # REST controllers
      └── out/persistence/  # JPA entities, repositories, persistence adapters
```
`shared/` is `shared/domain` (BusinessException, ErrorCode, Money, ReservationPeriod, TransactionRunner) + `shared/adapter` (security/, web/GlobalExceptionHandler, ClockConfig, OpenApiConfig, SpringTransactionRunner). Controllers that need the current user use `identity.adapter.in.CurrentCustomerResolver`.

## Key endpoint paths

- `GET /api/public/venue` — public venue info (no auth)
- `GET /api/availability?date&start&durationMinutes` — returns available resources
- `POST /api/reservations` — create (request: `{resourceId, startTime, durationMinutes}`)
- `GET /api/reservations?status&page&size` (paged), `GET /api/reservations/{id}`, `POST /api/reservations/{id}/cancel`
- `GET/POST /api/admin/resources`, `POST /resources/{id}/activate`, `/deactivate`, `PATCH /resources/{id}`
- `GET/PUT /api/admin/booking-policy`, `/cancellation-policy`, `GET/PUT /api/admin/pricing`
- `GET /api/admin/reservations?status&page&size` — list all reservations (paged, optional status filter)
- `POST /api/admin/resource-blocks` (normal), `/override` (atomic cancel+create), `/{id}/cancel`
- `GET /api/admin/resource-blocks?resourceId&status` — list resource blocks (all or per resource)
- `GET/PUT /api/admin/venue` — read/update venue profile (name, description, address)
- `GET/PUT /api/admin/content`, `PUT /api/admin/content/{key}` — admin read/update site text blocks (incl. photo web paths and `venue.map.layout` JSON)
- `GET /admin/content` (+ `POST /admin/content/venue`, `/photo?photoKey=...`, `/opening-hours`, `/{key}`) — web-UI admin window for site content
- `GET /admin/map` (+ `POST /admin/map/layout`, `/fields`, `/field/{id}/activate|deactivate`) — graphical drag-and-drop field-layout editor

## CI/CD (GitHub: efthymiosD/reservation-system, public repo)

Pipeline: `feature/* → PR (CI) → main → Docker image → GHCR → (QNAP deploy deferred)`.

- **`.github/workflows/ci.yml`** — PRs to `main`: JDK 26 (temurin, setup-java@v5), Maven cache, `./mvnw -ntp verify` with Testcontainers Postgres. No Keycloak dependency (opt-in `@Tag("keycloak")` stays excluded). Triggers on `pull_request` only — the removed `push` trigger used to create duplicate required check runs that deadlocked branch protection.
- **`.github/workflows/release.yml`** — push to `main`: `verify` job, then build image (buildx, GHA cache) → Trivy scan (fail on HIGH/CRITICAL, `ignore-unfixed`) → push to `ghcr.io/efthymiosd/reservation-system` with immutable git-SHA tag + `main` branch tag. Never `latest`.
- **`.github/workflows/deploy.yml`** — manual `workflow_dispatch` with an exact SHA; SSH deploy to the QNAP (compose up, health wait, smoke tests). **Deferred**: secrets `QNAP_DEPLOY_HOST/USER/SSH_KEY` not configured yet; `docker-compose.prod.yml` + host env file carry runtime secrets (DB password, issuer URI).
- **Branch protection on `main`**: PRs required (0 approvals), required check `mvn verify (unit + integration)`, enforced for admins, no force-push/delete.
- **Dependabot**: maven + docker + github-actions ecosystems, weekly; merge its PRs one at a time (they may conflict on the same `uses:` lines; ask Dependabot to rebase).
- **Dockerfile**: multi-stage (`maven:3.9-eclipse-temurin-26` → `eclipse-temurin:26-jre`), non-root `app` user, healthcheck via installed `curl`, `/usr/bin/pebble` removed (Canonical init daemon baked into the Ubuntu base; its bundled Go stdlib fails the Trivy gate). Dev *public* key is intentionally in the image (dev fallback only; prod profile forbids it).
- **Integration-test isolation**: `PostgresIntegrationTest` truncates `recurring_reservations, reservations` in `@BeforeEach` — every test class shares one container and must start clean regardless of execution order.
- **Exclusion-constraint races**: both persistence adapters translate `ConcurrencyFailureException` (deadlock/lock-timeout victims of the `btree_gist` race, SQLState 40P01) into the same 409 business conflicts as `DataIntegrityViolationException` (23P01) — lock-race losers must not surface as 500s.
