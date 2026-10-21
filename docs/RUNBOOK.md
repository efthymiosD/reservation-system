# QNAP Deployment Runbook

DEV environment on the QNAP, publicly reachable via **Tailscale Funnel**:
two public origins on `<node>.<tailnet>.ts.net` — the app on `443` and
Keycloak on `:8443`. The Netask ISP line is CGNAT (the router never owns a
public IP), so router port forwarding is impossible by design; instead, BOTH
critical paths dial **outward**: the deploy runs on a **self-hosted GitHub
runner inside the LAN** (runner → GitHub outbound), and public access comes
in through the **funnel carrier → Tailscale edge outbound**. MyQNAPcloud
DDNS is no longer part of the app story (may stay for NAS console
convenience). The stack runs the app with the prod Spring profile on
purpose — the box is internet-exposed, so real OAuth2/JWT enforcement
applies despite the environment label. TLS terminates at the funnel node
(Tailscale-issued Let's Encrypt certs for the ts.net name); node → stack is
plain HTTP over loopback — documented, dev-grade. The funnel publishes ONLY
the two configured services (opt-in per `tailscale funnel` call).

```
GitHub Actions (Deploy) ── outbound runner connection ──┐
                                                        ▼
Internet ─ Tailscale funnel relay (ts.net DNS → relay IPs)
    └── outbound tunnel ── QNAP (tailscale carrier, reservation-qnap)
          ├─ funnel 443  → NAS loopback :6680 → compose `app`        (reservation site/API)
          └─ funnel 8443 → NAS loopback :6681 → compose `keycloak`   (browser OIDC redirects)
```


## NAS safety box (hard rules)

- Never delete `/container-station-data`, reinstall QTS, or touch unrelated
  services (e.g. QuMagie's `system-docker`) from this runbook.
- Never run `docker compose down -v` against the QNAP deployment — data
  (postgres volume, keycloak data) survives routine deploys.
- Never paste database passwords, client secrets, tokens or
  diagnostic-backup contents into logs/chat (the deploy workflow is written
  to fail loudly without printing the realm secret).
- The self-hosted runner container runs with a mounted docker.sock —
  root-equivalent on the NAS. Accepted DEV trade-off (branch protection +
  a repo-scoped PAT gate who can trigger it). Do not reuse that PAT
  elsewhere.
- Diagnostic backups exist at
  `/share/CACHEDEV1_DATA/Container-Station-diagnostic-backup-*` — read-only
  reference.


## One-time host provisioning

1. **Deployment directory** — create the dir the workflow deploys into
   (default `/opt/reservation`; set the `QNAP_DEPLOY_DIR` GitHub variable to
   deviate). The runner container bind-mounts it at the SAME path, so
   compose-relative paths agree on both sides.

2. **Tailscale + Funnel** (public access):
   - Create a Tailscale account (free plan suffices). In the admin console:
     **DNS → MagicDNS** enable; **DNS → HTTPS Certificates** enable.
   - Funnel permission: the first `funnel` run walks you through an approval
     URL and adds the default `nodeAttrs` (`autogroup:member` →
     `attr: ["funnel"]`). You can pre-add or restrict it under
     Access controls → Funnel.
   - Generate an auth key (**Settings → Keys**, reusable + preauthorized)
     and put it into the host `.env` as `TS_AUTHKEY`.
   - Copy `docker-compose.tailscale.yml` from this repo to the deploy dir,
     then:
     ```bash
     cd /opt/reservation
     docker compose -f docker-compose.tailscale.yml --env-file .env up -d
     ```
   - Run `scripts/qnap/tailscale-setup.sh` (from a repo clone on the NAS or
     the runner workspace). First run blocks until you approve Funnel in the
     browser, then prints both public URLs.
   - Mind Let's Encrypt rate limits: do NOT toggle funnel on/off repeatedly
     (up to 34h lockout).
   - If QTS cannot provide `/dev/net/tun` to Container Station, use the
     userspace fallback documented in `docker-compose.tailscale.yml`
     (`TS_USERSPACE: "true"`, bridge network) and re-run the setup script
     with `FUNNEL_APP_TARGET=http://192.168.0.3:6680`
     `FUNNEL_KC_TARGET=http://192.168.0.3:6681`.

3. **Self-hosted GitHub runner** (deploy path; replaces the old SSH story):
   - Check the NAS CPU: `uname -m` (x86_64 vs aarch64) when choosing the
     runner image.
   - Run the runner as a Container Station container so it survives QTS
     reboots. Pragmatic image: `myoung34/github-runner:latest` (git +
     docker CLI + PAT auto-registration; verify `python3 --version`
     inside — the realm step needs it). Requirements for the image: git,
     python3, docker CLI.
     ```yaml
     services:
       runner:
         image: myoung34/github-runner:latest
         container_name: reservation-runner
         restart: unless-stopped
         network_mode: host          # so localhost:6680 = the NAS app port
         environment:
           ACCESS_TOKEN: ${GH_RUNNER_PAT:?classic PAT required}
           RUNNER_SCOPE: repo
           LABELS: reservation
           RUNNER_NAME: reservation-qnap
           DOCKER_CONFIG: /runner-docker
         volumes:
           - /var/run/docker.sock:/var/run/docker.sock      # compose control
           - /opt/reservation:/opt/reservation              # SAME host path
           - reservation-runner-docker:/runner-docker       # GHCR login persists
     volumes:
       reservation-runner-docker:
     ```
   - One classic PAT with `repo` + `read:packages` scopes: used as
     `ACCESS_TOKEN` (runner registration) and for the GHCR pull. Generate
     it, `docker exec -it reservation-runner docker login ghcr.io -u <user>`
     (password via stdin), then store it ONLY in the runner's env file on
     the NAS (`GH_RUNNER_PAT`) — never in GitHub.
   - Verify the runner appears: repo → Settings → Actions → Runners, label
     `reservation`.

4. **GitHub variables**
   - `QNAP_PUBLIC_APP_URL` — the app's public funnel URL
     (`https://reservation-qnap.<tailnet>.ts.net`)
   - `QNAP_PUBLIC_BASE_URL` — the Keycloak public funnel URL
     (`https://reservation-qnap.<tailnet>.ts.net:8443`)
   - `QNAP_DEPLOY_DIR` — deployment directory
     (e.g. `/opt/reservation`)

5. **GitHub secrets cleanup** — DELETE (no longer used):
   `QNAP_DEPLOY_HOST`, `QNAP_DEPLOY_USER`, `QNAP_DEPLOY_SSH_KEY`, and
   `KC_WEB_CLIENT_SECRET` (the client secret now lives ONLY on the NAS:
   host `.env` for the realm generation and the app container).

6. **Host env file** — `/opt/reservation/.env` (mode 600):

   ```bash
   # data plane
   POSTGRES_PASSWORD=<openssl rand -base64 24>

   # Keycloak temporary bootstrap admin (Keycloak 26 naming; legacy
   # KEYCLOAK_ADMIN pair is deprecated). Created only while the master
   # realm doesn't exist; exchange for the permanent admin per
   # "Keycloak console tasks" and DELETE the bootstrap account afterwards.
   KC_BOOTSTRAP_ADMIN_USERNAME=<console user>
   KC_BOOTSTRAP_ADMIN_PASSWORD=<openssl rand -base64 24>

   # Tailscale funnel (one-time auth key from the admin console)
   TS_AUTHKEY=tskey-auth-...

   # Keycloak public face (funnel, port 8443) — pins every token's `iss`;
   # the funnel path is root(/) so no path prefix appears in these URLs
   KC_PUBLIC_BASE_URL=https://reservation-qnap.<tailnet>.ts.net:8443
   JWT_ISSUER_URI=https://reservation-qnap.<tailnet>.ts.net:8443/realms/reservation

   # browser-facing legs (public, funnel)
   KEYCLOAK_AUTHORIZATION_URI=https://reservation-qnap.<tailnet>.ts.net:8443/realms/reservation/protocol/openid-connect/auth
   KEYCLOAK_END_SESSION_URI=https://reservation-qnap.<tailnet>.ts.net:8443/realms/reservation/protocol/openid-connect/logout

   # server legs (compose-internal, no funnel)
   KEYCLOAK_TOKEN_URI=http://keycloak:8080/realms/reservation/protocol/openid-connect/token
   KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/reservation/protocol/openid-connect/certs

   # pairs with the secret the NAS-side realm generation injects; NOT a
   # GitHub secret anymore — the only copy is here
   KC_WEB_CLIENT_SECRET=<openssl rand -base64 24>

   # APP_PUBLIC_BASE_URL / KC_WEB_REDIRECT_URI are intentionally ABSENT:
   # the callback URL derives from the request's visible scheme+host via
   # SERVER_FORWARD_HEADERS_STRATEGY (= the funnel ts.net URL), and the
   # realm's redirect URIs are generated from the QNAP_PUBLIC_APP_URL var.
   ```

7. **Realm secret rotation note**: changing `KC_WEB_CLIENT_SECRET` in the
   host `.env` → re-run Deploy (the NAS-side generation applies it). The
   realm import only re-applies on a fresh realm (delete the keycloak
   volume AND re-run deploy on a fresh DB to force one). Keycloak
   realm-internal secrets can also be rotated in its admin console.

8. **First main-branch deploy** — fire the workflow; the Post-deploy checks
   below must pass from a phone on LTE (true external vantage) at least
   once after provisioning.


## Keycloak console tasks (one-time, live realm)

The live realm predates the updated realm export/generation — the following
are set via the admin console once and persist in the kcdata volume. A
fresh-import deployment (delete kcdata → re-run Deploy) carries them from
the repo content instead.

1. **Permanent master-console admin** (removes the "temporary admin user"
   banner): log in at `…:8443` master realm as the bootstrap admin
   (`KC_BOOTSTRAP_ADMIN_USERNAME`) → Users →
   Add user (`kcadmin`, real email/name) → Credentials → set password with
   **Temporary OFF** → Role mapping → Assign role → filter client
   `realm-management` → assign `realm-admin`. Log out, log back in as
   `kcadmin` to verify, then delete the temporary bootstrap user.

2. **Console + login localization (EN/PL/EL)**: Realm settings →
   Localization → Internationalization **on**, supported locales
   `en, pl, el`, default `en` — in BOTH the master realm (governs the
   admin console) and the `reservation` realm (governs the login page).
   Keycloak ships PL and EL translations for these UIs; any string without
   a translation renders English. The app carries its own locale into the
   login page automatically: the OAuth2 redirect includes
   `ui_locales=<app locale cookie>` — see "Locale memory" below.

3. **Real users (seeded ones never ship to deployments)**: the QNAP realm
   generation REMOVES every demo user from the generated realm ("N demo
   user(s) removed" in the deploy log) — fresh imports start empty; the
   committed dev realm keeps `alice`/`bob`/`admin` ONLY as the stable
   contract for the e2e stack and local development, and visitor
   self-registration stays OPEN by design (anyone may sign up as a
   customer on the public URL). On the LIVE realm: delete the imported
   demo users `alice`/`bob`/`admin` (reservation realm → Users → … →
   Delete), then create the real ones: Users → Add user — one customer
   account, one admin (Role mapping → assign `ADMIN`) — credentials
   Temporary ON/OFF at your discretion. On first login the app
   auto-provisions the internal CustomerId, so any fresh console user
   works end-to-end without further wiring.

## Locale memory

The app's own `locale` cookie (365 days) is the single source of truth for
the visitor's language: set by the site's language selector (`?lang=`),
honored on every later visit (constraint-checked against the enabled
languages — a disabled/stale value falls back silently). The Keycloak
login screen follows the app locale via the `ui_locales` parameter on the
login redirect. A language switcher inside the Keycloak screen changes
only that Keycloak session and is deliberately NOT propagated back into
the app cookie.

## Deploy

GitHub → Actions → **Deploy to QNAP** → run with the image SHA you want
(the Release workflow publishes `:sha` images on every main push). The
workflow runs on the self-hosted runner ON the QNAP: generates the QNAP
realm locally (funnel URLs + real client secret from the host `.env`),
syncs `docker-compose.deploy.yml` + realm into the deploy dir, pulls the
exact SHA, boots the stack, health-checks, and smokes (public venue + 401
on availability on the LAN port, then both funnel doors + all containers
running). The runner must be online when you fire the action.

## Post-deploy manual checks

1. `https://reservation-qnap.<tailnet>.ts.net` — home page in the default
   language (funnel 443 → NAS 6680)
2. Language selector → Polish / Greek — copy + `lang` attribute switch
3. Log in as the CUSTOMER account YOU created in the console (a rotation
   screen appears only if you set its credentials Temporary) — redirected
   through `https://reservation-qnap.<tailnet>.ts.net:8443`, back to the
   app; the login screen should open in the language last chosen on the
   site; seeded demo users (alice/bob/admin) must NOT exist on this realm
4. Book a slot end to end; cancel it
5. Admin panel → /admin/content (per-language editor), /admin/map
6. Keycloak console at `https://reservation-qnap.<tailnet>.ts.net:8443`
   (master realm; `KC_BOOTSTRAP_ADMIN_USERNAME` credentials) — check the
   `reservation` realm users
7. First run after provisioning: do 1–4 from a phone on LTE (not the LAN
   Wi-Fi) — the funnel-door smoke in the workflow hairpins via the Tailscale
   relay, but an LTE check is the honest external vantage.

## Maintenance

- **Rollback**: re-run the Deploy workflow with the previous known-good SHA
  (compose recreates containers; images and volumes stay untouched). The
  workflow records the previously deployed tag into
  `/opt/reservation/.previous-image` before every overwrite — copy that
  value from the host as the rollback input.
- **Runner down?** Deploys queue until the runner reconnects (Actions job
  waits for the label). Check `docker logs reservation-runner` or just
  restart the runner container.
- **Funnel down?** Public URLs die but LAN + deploy keep working. Inspect
  `docker logs reservation-tailscale`, then `docker exec
  reservation-tailscale tailscale funnel status`.
- **Database backup** (run on the QNAP, schedule via cron):
  `docker exec reservation-postgres pg_dump -U reservation reservation | gzip > /opt/reservation/backup/reservation-$(date +%F).sql.gz`
  Restore: `gunzip -c <file> | docker exec -i reservation-postgres psql -U reservation -d reservation`.
- **Secret rotation**: change the value in the host `.env` → re-run Deploy.
  The realm import only re-applies on a fresh realm import (see the note
  above); Keycloak realm-internal secrets can also be rotated in its admin
  console (the realm persists across container recreations — a rotated
  password stays rotated).
- **TLS status (dev)**: TLS terminates at the funnel node (Tailscale-issued
  Let's Encrypt cert for the ts.net name); node → stack is plain HTTP on
  the NAS loopback. A custom-domain gateway remains a real-production
  concern. The Keycloak + app URLs the realm redirect URIs use must match
  the exposed funnel ports exactly.
- **Realm tightening notes**: demo realm has `directAccessGrantsEnabled` on
  `reservation-api` (password grant) — acceptable for the demo, disable for
  anything longer-lived. Seeded contract users never reach deployments
  (removed at realm generation); on the LIVE realm they are deleted and
  real users are created per the console-task section above.
