# E2E Tests (Playwright + Python)

Cross-device/cross-browser safety net for the reservation web UI.
Playwright for Python (`pytest-playwright`), Python **3.12**.

## Recommended IDE: PyCharm

**PyCharm Community is enough** (Professional adds nothing for pytest;
only the paid Ultimate supports specific frameworks out of the box).

```bash
# macOS
brew install --cask pycharm-ce
```
```powershell
# Windows
winget install --id JetBrains.PyCharm.Community -e
```

First open: **File → Open…** → the repo's `e2e-tests` folder (open the folder
itself, not the parent repo). Then:

1. **Settings → Project → Python Interpreter → Add Interpreter → Existing**
   and point it at the venv you created below
   (`e2e-tests/.venv/bin/python` on macOS, `e2e-tests\.venv\Scripts\python.exe`
   on Windows).
2. PyCharm enables the pytest integration automatically when it sees
   `pytest.ini` (run single tests with the gutter ▶ marks).
3. Optional, for headed debugging: add `--headed --slowmo 250` to the
   run configuration's additional args to watch the browser live.

## One-time setup

The suite needs: Python 3.12, a browser install of Playwright's bundled
Chromium/WebKit, and the deterministic dockerized stack from
`docker-compose.e2e.yml` (root of the repo).

### macOS

```bash
# 1) Python 3.12 (system python3 is 3.9 — too old)
brew install python@3.12
/opt/homebrew/bin/python3.12 --version     # 3.12.x

# 2) project virtualenv + dependencies (run from e2e-tests/)
cd e2e-tests
/opt/homebrew/bin/python3.12 -m venv .venv
./.venv/bin/pip install -r requirements.txt

# 3) Playwright browsers (WebKit download is ~70MB; no --with-deps needed on macOS)
./.venv/bin/python -m playwright install chromium webkit
```

### Windows

> Run in **PowerShell**.

```powershell
# 1) Python 3.12
winget install --id Python.Python.3.12 -e
#    restart PowerShell, then confirm:
py -3.12 --version

# 2) project virtualenv + dependencies (run from e2e-tests/)
cd e2e-tests
py -3.12 -m venv .venv
.venv\Scripts\pip install -r requirements.txt

# 3) Playwright browsers (no --with-deps needed on Windows)
.venv\Scripts\python -m playwright install chromium webkit
```

## Deterministic dockerized environment

The whole environment is reproducible and starts from the same state:

```bash
# 0) from the repo root: reset + boot app + postgres + keycloak
docker compose -f docker-compose.e2e.yml down -v --remove-orphans
docker compose -f docker-compose.e2e.yml up --build -d
#    -> wait for the app container healthcheck
#       (docker compose -f docker-compose.e2e.yml ps shows `healthy`)
```

- The app runs with a **fixed clock** (`APP_CLOCK_FIXED_INSTANT=2026-10-12T10:00:00Z`)
  so every journey books a deterministic date (`constants.py`).
- `down -v` + `up` **recreates the database and re-imports the Keycloak realm** —
  no manual test-data steps. Always start a new run this way.

## See the tests run live (watch the browser)

By default pytest-playwright runs **headless**. Three ways to watch:

```bash
# 1) LIVE — a real browser window opens on your desktop and you watch the
#    journey click through. --slowmo (ms) pads every action so you can follow.
./.venv/bin/pytest tests/test_responsive.py --browser chromium --headed --slowmo 250   # macOS
.venv\Scripts\pytest tests\test_responsive.py --browser chromium --headed --slowmo 250   # Windows

# 2) HTML report (after any run, headed or headless)
./.venv/bin/pytest tests/test_responsive.py --browser chromium \
  --html=playwright-report/report.html --self-contained-html     # macOS
#    then open e2e-tests/playwright-report/report.html in a browser.

# 3) Failure artifacts: pytest.ini already captures video + screenshot +
#    trace (retain-on-failure) into test-results/ — open the .webm video
#    or the trace zip at https://trace.playwright.dev after any red run.
```

IMPORTANT: reruns (any mode, live included) need a fresh database — repeat the
`down -v --remove-orphans && up` reset above first, or the journey tests
collide with the bookings a previous run left behind (deliberate design, see
Conventions below).

## Run the suite

```bash
# from e2e-tests/ — ALWAYS do the down -v reset above first
./.venv/bin/pytest tests/test_responsive.py --browser chromium        # macOS, faster
./.venv/bin/pytest tests/test_responsive.py --browser chromium --browser webkit   # full matrix
# Windows:
.venv\Scripts\pytest tests\test_responsive.py --browser chromium
.venv\Scripts\pytest tests\test_responsive.py --browser chromium --browser webkit
```

Locally choose any of the playwright browsers: `--browser chromium` /
`--browser webkit` / `--browser firefox`.

### CI: manual only

The e2e suite is **deliberately not part of the PR pipeline** (too slow:
backend build + image build + browser matrix). A manual workflow exists —
run it from the **Actions tab → "E2E (Playwright, manual)" → Run workflow**
(optionally choosing the browsers). It boots the deterministic stack, waits
for health, runs the suite with `-` artifacts on failure, then tears the
stack down.

The suite fails fast when the stack is not reachable (`_stack_ready` fixture)
with a hint pointing to the bring-up commands above.

## What's implemented vs. backlog

- **Implemented**: `tests/test_responsive.py` — booking journey × all device
  descriptors (iPhone SE 375×667 … Desktop 1280) + no-horizontal-scroll +
  navbar collapse/expansion asserts.
- **Backlog** for the first QA automation engineer: booking/availability/admin
  specs with full steps, assertions and the selector cheat-sheet in
  `SPECS-BACKLOG.md`.

## Conventions for new specs (important)

- **Booking data must never collide.** Every journey reserve test picks a
  deterministic date from `constants.booking_date(device, browser)` — add new offsets
  there instead of hardcoding dates. Cancellation deadline is 120 min, so
  same-slot cleanup from inside a test is impossible; the only real reset is
  `docker compose -f ... down -v`.
- Do **not** add automatic reruns (`pytest-rerunfailures` must NOT be
  installed): a rerun replays a booking journey against a database that
  already holds its own booking → false red.
- `pytest.ini` intentionally has no `--browser` flags (browser matrix passed
  explicitly; duplicated `--browser` params double-run every test).
- Auth is driven through the **real Keycloak OAuth2 form** (`conftest.login`)
  — no API shortcuts; sessions are fresh via the app's `prompt=login` resolver.
- Keep specs to critical user journeys — business rules belong in the
  JUnit/integration layers (see docs/plans/playwright-e2e-introduction.md).
