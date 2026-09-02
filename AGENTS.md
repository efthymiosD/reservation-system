# reservation-system — Agent Instructions

## Implementation status

All phases are implemented and compiling; **99 tests green** (includes Testcontainers Postgres integration/concurrency via Rancher Desktop/Docker) plus go-live hardening. An opt-in `@Tag("keycloak")` realm-contract test is excluded by default and runs only against a live Keycloak.

Go-live hardening (live bookings only; no payments/notifications/waitlist/multi-venue):
- **Observability** — actuator (`/actuator/health` + liveness/readiness probes, `show-details: never`, health permitted unauthenticated; verified by `ActuatorHealthIntegrationTest`), logging (`application.yml` console/file pattern + size/rotation, Tomcat access log), lifecycle INFO logs on create/cancel/override.
- **Audit trail** — `cancelled_by` on both `reservations` and `resource_blocks` + `resource_blocks.cancelled_at` (folded into `V1__init_schema.sql` after the migration merge); actor is the resolved **internal `CustomerId`**; admin overrides and block cancels thread the actor through the web→service→domain path.
- **Secret hygiene** — dev RSA keys gitignored, `.env.example` documents secrets, `docker-compose.yml` reads creds from env (dev defaults), `application-prod.yml` requires `JWT_ISSUER_URI` + DB env vars and forbids the dev-key fallback (`app.security.allow-dev-key=false`) and disables springdoc unless `SPRINGDOC_ENABLED`.
- **JWT hardening** — optional audience validation via `AudienceJwtValidator` when `app.security.audience` (default `reservation-api`) is set, combined with the issuer validator in `ValidatingJwtDecoder`; **CORS** allowlist via `app.security.cors.allowed-origins` (empty default).
- **Admin read-side** — list-all reservations (pagination + status filter), list-all/none resource blocks, GET current booking/cancellation/pricing policies; customer reservations support page/size + status filter.

- **Phase 0 Foundation** — `pom.xml` (Boot 4.1.1, Modulith 2.1.1, Lombok 1.18.46, Testcontainers 2.0.5), `ReservationApplication`, security (`SecurityConfig`/`JwtDecoderConfig`/`JwtRolesConverter`), `application.yml`, `docker-compose.yml`, dev RSA keypair, test infra (`PostgresIntegrationTest`, `JwtSupport`, `ModularityTests`).
- **Phase 1 Schema** — `V1__init_schema.sql` (venues, opening_hours, booking/cancellation/pricing policies, resource_groups, resources, reservations + exclusion constraints, resource_blocks + audit columns, customers; btree_gist), `V2__seed_data.sql`. Migrations V3/V4 were **merged into V1** (only `V1` + `V2` remain).
- **Phase 2 Core domain** — `shared`, `venue`, `resource`, `policy`, `pricing`, `identity` modules + admin/public endpoints.
- **Phase 3 Reservation** — `CreateReservationService`, `CancelReservationService`, `ReservationQueryService`, persistence adapter (constraint translation), `ReservationController`.
- **Phase 4 Availability** — `AvailabilityService`, `AvailableResource`, `AvailabilityController`.
- **Phase 5 Admin/Blocks** — `administration` module (`BlockResourceService`, `OverrideResourceBlockService`), `ResourceBlockAdminController`, resource admin CRUD.
- **Phase 6 Tests** — unit tests pass. Integration tests (`DatabaseConstraintsIntegrationTest`, `ReservationApiIntegrationTest`, `BlockApiIntegrationTest`, `ConcurrencyIntegrationTest`) run green against Testcontainers Postgres (Docker via Rancher Desktop).
- **Phase 7 Internal identity** — `customers` table (in `V1`) + `identity` module port/persistence so the domain operates on an **app-owned internal `CustomerId`** (UUID) decoupled from the IdP: `CustomerAccountService.resolveOrProvision(sub)` auto-provisions a `customers` row on first authenticated request and reuses it thereafter. `CurrentCustomerResolver` (web adapter in `identity.adapter.in`) maps the Keycloak `sub` claim → internal `CustomerId` (no `preferred_username` fallback). `CustomerAccountServiceTest`, `CustomerIdentityIntegrationTest` joined the suite; opt-in `KeycloakTokenContractIntegrationTest` (`@Tag("keycloak")`) verifies issued tokens carry `sub` and resolve to a stable `CustomerId`.

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

- **Lombok annotation processor is NOT implicit on JDK 25+** — `pom.xml` explicitly configures `annotationProcessorPaths` for lombok 1.18.46. Do not remove it.
- **`spring-boot-webmvc-test` must be declared separately** — Spring Boot 4.1.1 does not bundle `@AutoConfigureMockMvc` in `spring-boot-starter-test`. It lives in `org.springframework.boot:spring-boot-webmvc-test` under `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- **Spring Boot 4 ships Jackson 3 (`tools.jackson`)** — the auto-configured bean is `tools.jackson.databind.ObjectMapper`, NOT `com.fasterxml.jackson.databind.ObjectMapper`. Tests injecting `ObjectMapper` must import `tools.jackson.databind.*`. (Old `com.fasterxml` Jackson 2 is only on the classpath transitively via springdoc.) API: `readTree(String)` returns `JsonNode`, `JsonNode.get(String).asText()` — same as Jackson 2.
- **Spring Boot 4 `spring-boot-starter-flyway` only brings `flyway-core`, not PostgreSQL support** — you MUST also add `org.flywaydb:flyway-database-postgresql` (version-managed by Boot BOM). Without it, startup fails with `FlywayException: Unsupported Database: PostgreSQL 17.11`.
- **Config-schema integration tests share one Postgres container and dataset** — data persists across test classes. `DatabaseConstraintsIntegrationTest` TRUNCATEs `resource_blocks, reservations` (and `CustomerIdentityIntegrationTest` also TRUNCATEs `customers`) in `@BeforeEach`; `AdminReadApiIntegrationTest` TRUNCATEs `resource_blocks, reservations` and uses the 09-20/09-21 slots so it never collides with the 09-04..09-07 slots of `ReservationApiIntegrationTest`/`BlockApiIntegrationTest`; keep any raw-SQL tests self-isolated or they collide on the exclusion constraints.
- **Opt-in `@Tag("keycloak")` tests are excluded by default** via surefire `<excludedGroups>${surefire.excludedGroups}</excludedGroups>` (property defaults to `keycloak`). Run them with `-Dsurefire.excludedGroups=`. Setting `-Dtest=...` does not bypass this exclusion.
- **`@ServiceConnection` + Testcontainers 2.x `PostgreSQLContainer` does NOT work** — use a static initializer (`POSTGRES.start()` in `static {}`) in `PostgresIntegrationTest` and `@DynamicPropertySource` instead.
- Integration test base class: `PostgresIntegrationTest` (`@SpringBootTest(properties = "app.clock.fixed-instant=2026-09-01T10:00:00Z")`). Subclasses add `@AutoConfigureMockMvc`.
- `JwtSupport.customer(subject)` / `JwtSupport.admin(subject)` create mock JWTs for `MockMvc`. Admin needs `.authorities("ROLE_ADMIN")`.
- **Integration tests are coupled to seed data**: they reference fixed resource UUIDs `a0000000-...-00000000010{1..6}` (Field 1–6) and rely on seeded defaults (durations 60/120 min, 30-min grid, P1M advance window, 120-min cancellation deadline, 80 PLN/hr). Changing `V2__seed_data.sql` will break them. E.g. a 90-min booking at 80 PLN/hr is asserted as `priceAmount=120.00`.

## Architecture

- **Spring Boot 4.1.1 + Spring Modulith 2.1.1** modular monolith (9 modules), Java 26, Maven
- **Tactical DDD + pragmatic hexagonal**: domain aggregates/value objects + services in `domain/`, repository ports in `domain/port/`, adapters in `adapter/in/web` (controller) and `adapter/out/persistence` (JPA). Framework imports (Spring/JPA) must NOT appear in `..domain..` — enforced by `DomainPurityTest` (ArchUnit, which ignores `package-info` metadata classes).
- 9 modules: `shared`, `venue`, `resource`, `reservation`, `availability`, `pricing`, `policy`, `identity`, `administration`
- **b1 / framework-agnostic domain**: domain services carry no `@Service`/`@Transactional` (wired in root `DomainServicesConfig`), transactions go through the `TransactionRunner` port (`shared/domain`) implemented by `SpringTransactionRunner` (`shared/adapter`); `shared` itself is split into `shared/domain` (Money, ReservationPeriod, BusinessException, ErrorCode, TransactionRunner) + `shared/adapter` (security/, web/, ClockConfig, OpenApiConfig, SpringTransactionRunner).
- **Modulith boundaries**: each module's `domain/{model,service,port}` (plus `shared/domain` and `identity/adapter/in`) are exposed as named interfaces via `@NamedInterface` in `package-info.java` files so `ModularityTests` (`ApplicationModules.verify()`) passes while domain stays Spring-free.
- **No persistence in domain**: entities are pure Java (Lombok `@Getter` + `@NoArgsConstructor(access=PROTECTED)`); JPA `@Entity` lives only in `adapter/out/persistence/*Entity`.
- **Concurrency safety**: PostgreSQL `btree_gist` exclusion constraints on `tstzrange` for ACTIVE reservations (per-resource AND per-customer) and resource blocks. Cancelled rows are excluded from constraints.
- **Clock is property-driven** via `app.clock.fixed-instant` — integration tests fix it to `2026-09-01T10:00:00Z` for deterministic time.
- **Dev JWT**: dev RSA keypair at `src/main/resources/dev/jwt-public.pem`. Used only when no `JWT_ISSUER_URI` is set (empty issuer).
- **Flyway migrations** in `src/main/resources/db/migration/` (`V1__init_schema.sql`, `V2__seed_data.sql`). V3/V4 were merged into V1.
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
