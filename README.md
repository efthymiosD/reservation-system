# Reservation System

A generic field reservation system for an hourly-rental venue, built as a
**Spring Modulith modular monolith** with a server-rendered Thymeleaf web UI.

Feature highlights: interactive venue-map slot selection, availability checks with
PostgreSQL `btree_gist` exclusion constraints (race-proof double bookings),
booking/cancellation/pricing policies, six admin windows (reservations, pricing,
blocks, site content, field-layout editor, recurring reservations) and
Keycloak-backed OAuth2 login.

| | |
|---|---|
| Java | 26 (Temurin/Adoptium) |
| Framework | Spring Boot 4.1.1 + Spring Modulith 2.1.1, Maven (`./mvnw`, no local Maven needed) |
| Database | PostgreSQL 17 (Flyway-managed schema) |
| Identity | Keycloak 26 (realm `reservation`, OAuth2 authorization-code login) |
| UI | Thymeleaf + vendored Bootstrap 5.3.3, deliberately small JS |
| Tests | JUnit (unit/integration/concurrency via Testcontainers) + Spotless formatting |
| E2E | Playwright (Python 3.12) in `e2e-tests/` — see `e2e-tests/README.md` |

## Local setup (new joiners)

Prerequisites: **git**, **JDK 26**, **Docker Desktop** (or Rancher Desktop on
macOS), and for e2e only: **Python 3.12**.

### macOS

```bash
# 1) Homebrew (skip if you have it)
NONINTERACTIVE=1 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"

# 2) JDK 26 (Temurin)
brew install --cask temurin@26
# becomes the default JVM:
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 26)' >> ~/.zprofile
source ~/.zprofile

# 3) Docker (Docker Desktop; Rancher Desktop is also fine)
brew install --cask docker
open -a Docker   # launch it once and follow the first-run dialog

# 4) clone + verify the toolchain
git clone https://github.com/efthymiosD/reservation-system.git
cd reservation-system
./mvnw --version   # must report Java 26

# 5) recommended IDE: IntelliJ IDEA (Community is enough)
brew install --cask intellij-idea-ce
```

### Windows

> Run in **PowerShell** (winget ships with Windows 10/11).

```powershell
# 1) JDK 26 (Temurin)
winget install --id EclipseAdoptium.Temurin.26.JDK -e
#    then set JAVA_HOME (adjust the path if the installer differs):
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-26"

# 2) Docker Desktop
winget install --id Docker.DockerDesktop -e

# 3) Git (skip if present)
winget install --id Git.Git -e

# 4) clone + verify the toolchain (restart PowerShell after the installs)
git clone https://github.com/efthymiosD/reservation-system.git
cd reservation-system
.\mvnw.cmd --version   # must report Java 26

# 5) recommended IDE: IntelliJ IDEA (Community is enough)
winget install --id JetBrains.IntelliJIDEA.Community -e
```

### First open in IntelliJ IDEA

1. **File → Open…** → select the cloned `reservation-system` folder.
2. IDEA detects the Maven project and downloads dependencies automatically
   (first sync takes a few minutes). Accept the JDK prompt and pick **JDK 26**.
3. Lombok plugin + annotation processing are enabled by IDEA defaults; if asked,
   enable **Settings → Build Tools → Maven → Runner → "Delegate IDE build/run
   actions to Maven"** for parity with the CLI.
4. Running/debugging the app from the IDE: open `ReservationApplication`,
   press ▶ (Run). Docker services must be up first (see "Run the app" below).

## Run the app

1. **Start the supporting services** (Postgres 17 on :5432, Keycloak on :8081 —
   this board needs Keycloak for every web login):

   ```bash
   docker compose up -d        # Windows: docker compose up -d
   ```

   Keycloak auto-imports the dev realm (users `admin/admin` for ADMIN,
   `alice/alice` & `bob/bob` for CUSTOMER). Admin console: http://localhost:8081.

2. **Boot the app** (port :8080):

   ```bash
   ./mvnw spring-boot:run      # Windows: .\mvnw.cmd spring-boot:run
   ```

3. Open **http://localhost:8080** and log in as `alice`/`alice` (customer) or
   `admin`/`admin`. The booking window is under **"Make a reservation"**.

Useful URLs once the app is up:

 - `http://localhost:8080/swagger-ui.html` — OpenAPI
 - `http://localhost:8080/actuator/health` — health endpoint
 - `http://localhost:8080/?lang=pl&lang=pl` trick not needed — use the flag selector (EN/PL/EL) in the navbar; the choice persists in the `locale` cookie for a year.

## Languages & site content (how i18n works)

- **System copy** (buttons, labels, errors) ships translated in code — `src/main/resources/messages/messages{,_en,_pl,_el}.properties`. Adding a language = new bundle file + entry in `webui/SupportedLocales` + flag fragment in `templates/fragments/flags.html`.
- **Site-content defaults** are bundle-backed too: until an admin authors a block, every language shows the built-in translated default.
- **Admin-authored content** is never translated by the system. On `Admin → Site content → Page text` pick the editor language (English/Polski/Ελληνικά dropdown); fields saved for one language render verbatim to that language's visitors only; leave a field empty to fall back to the built-in default.
- `Admin → Site content → Languages` enables/disables visitor languages (the default language is permanently on). Disabled languages vanish from the selector; stale visitors resolve to the next enabled language.

## Run the tests

- **Full Maven suite** (unit + integration through Testcontainers — needs Docker running):

  ```bash
  ./mvnw test    # Windows: .\mvnw.cmd test
  ```

  Integration tests spin up a disposable PostgreSQL 17 container through
  **Testcontainers**, which needs to find your Docker endpoint. Pick ONE of the
  two routes below.

  ### Route A (recommended — no extra app, no account): docker socket at the standard path

  Testcontainers' default strategy probes the standard Unix socket
  `/var/run/docker.sock`. Docker Desktop creates that symlink for you; **Rancher
  Desktop on macOS does not** (it keeps its socket at `~/.rd/docker.sock`), so
  create a one-time symlink to the standard path:

  ```bash
  # macOS + Rancher Desktop (one-time; needs your password):
  sudo ln -sf $HOME/.rd/docker.sock /var/run/docker.sock

  # verify, then run the tests — no extra apps, no accounts, no env vars:
  docker ps
  ./mvnw test      # Windows: .\mvnw.cmd test
  ```

  That symlink is the whole story for macOS + Rancher Desktop: ryuk (the
  Testcontainers cleanup sidecar) bind-mounts the docker socket into itself,
  and Rancher refuses to mount `~/.rd/docker.sock` directly
  (`mkdir ... operation not supported`) — but happily mounts the standard
  `/var/run/docker.sock` path. Verified green: full suite 189 tests.

  #### Windows equivalent (Docker Desktop)

  Docker Desktop on Windows already publishes the named pipe
  `//./pipe/docker_engine` that Testcontainers probes natively — **no symlink
  or extra setup is required**:

  ```powershell
  docker ps          # must connect; Docker Desktop running
  .\mvnw.cmd test
  ```

  If you run Docker through a non-standard endpoint (rare; e.g. a custom
  engine over TCP), point Testcontainers at it explicitly instead:
  `$env:DOCKER_HOST = "npipe:////./pipe/docker_engine"` (PowerShell) or
  `$env:DOCKER_HOST = "tcp://<host>:2375"` — then re-run. `setx DOCKER_HOST ...`
  makes the variable permanent for future shells.

  ### Route B (alternative — Testcontainers Desktop app)

  Testcontainers Desktop pins one runtime and helps you route containers
  through a cloud or local daemon:

  1. Install **Testcontainers Desktop** (https://testcontainers.com/desktop/)
     and **create an account + log in**.
  2. In the tray/menu-bar app's **context menu**, choose
     **"Containers running locally"**.
  3. Select your runtime from the list (this repo's team standard on macOS is
     **Rancher Desktop**, which references the socket at `~/.rd/docker.sock`).

  > **Caveats:** Testcontainers Desktop must be **running** whenever you run
  > `mvn test` — it owns the endpoint (`tcp://127.0.0.1:49340`) it writes into
  > `~/.testcontainers.properties` (`tc.host` / `docker.host`), and while that
  > pin exists, a stopped Desktop app means "Could not find a valid Docker
  > environment" errors on every integration test (the pinned TCP host is
  > tried exclusively; there is no socket fallback). If you switch back to
  > Route A, have the app running once more (or reset the two properties
  > lines in `~/.testcontainers.properties`).

- **E2E (Playwright, optional)**: see `e2e-tests/README.md`; the deterministic
  environment lives in `docker-compose.e2e.yml` (own app container; fixed clock).

### Formatting / linting

```bash
./mvnw spotless:apply   # run before committing Java changes
```

## More documentation

- `docs/adr/` — architecture decisions (module structure, record boundaries)
- Keycloak realm export: `keycloak/realm-export.json` (dev users + clients)
- E2E project README: `e2e-tests/README.md` (setup incl. PyCharm recommendation)
