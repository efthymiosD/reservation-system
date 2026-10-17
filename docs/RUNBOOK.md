# QNAP Deployment Runbook

DEV environment on the QNAP, publicly reachable via **myQNAPcloud DDNS +
router port forwards** (Cloudflare tunnel deferred until a real domain and a
TLS gateway exist — the real production deployment is deferred anyway):
Postgres + Keycloak + the app. The stack runs the app with the prod Spring
profile on purpose — the box is internet-exposed, so real OAuth2/JWT
enforcement applies despite the environment label. All public traffic is
plain HTTP on nonstandard ports for now (`6680` app / `6681` keycloak) —
documented, dev-grade. The dedicated SSH route is the ONLY public
management port.

```
Internet ─ router (port forwards 22, 8080, 8081) ─ QNAP (DukesCloud, dukescloud.myqnapcloud.com)
   ├─ :8080 → compose `app`         (reservation site/API; LAN + DDNS)
   ├─ :8081 → compose `keycloak`    (browser OIDC redirects; LAN + DDNS)
   └─ :22   → QTS sshd              (CI deploy path via the DDNS; keys only)
```


## NAS safety box (hard rules)

- Never delete `/container-station-data`, reinstall QTS, or touch unrelated
  services (e.g. QuMagie's `system-docker`) from this runbook.
- Never run `docker compose down -v` against the QNAP deployment — data
  (postgres volume, keycloak data) survives routine deploys.
- Never paste database passwords, client secrets, SSH private keys or
  diagnostic-backup contents into logs/chat (the deploy workflow is written
  to fail loudly without printing the injected realm secret).
- Diagnostic backups exist at
  `/share/CACHEDEV1_DATA/Container-Station-diagnostic-backup-*` — read-only
  reference.


## One-time host provisioning

1. **Deployment directory** — create the dir the workflow deploys to
   (default `/opt/reservation`; set the `QNAP_DEPLOY_DIR` GitHub variable to
   deviate). The SSH user needs a Docker-capable account (QNAP "admin" group
   or a dedicated docker user).

2. **SSH for the deploy workflow** — the runner connects via the DDNS name;
   no Cloudflare involvement:
   - generate a dedicated keypair (`ssh-keygen -t ed25519 -f qnap-deploy-key
     -N ""`) and put the public key in the QNAP user's `~/.ssh/authorized_keys`
     (STRICT host keys over the first connect: `accept-new` is workflow-set)
   - router: forward an EXTERNAL port to the QNAP:22 (browse: forward 8080
     and 8081 too); a non-key-accessible host must be reachable by the GitHub
     runner
   - HARDENING (QTS → Control Panel → Telnet / SSH): **disable SSH password
     logins**; key-only. Optionally restrict sshd attempts/QTS IP-block.
   - GitHub secrets: `QNAP_DEPLOY_HOST` (= `dukescloud.myqnapcloud.com`),
     `QNAP_DEPLOY_USER` (= `technical_user`), `QNAP_DEPLOY_SSH_KEY` (= the
     private key file content)

3. **GitHub variables**
   - `QNAP_PUBLIC_APP_URL` — the app's public DDNS URL
     (e.g. `http://dukescloud.myqnapcloud.com:8080`)
   - `QNAP_PUBLIC_BASE_URL` — the Keycloak public DDNS URL
     (e.g. `http://dukescloud.myqnapcloud.com:8081`)
   - `QNAP_DEPLOY_DIR` — deployment directory
     (e.g. `/share/CACHEDEV1_DATA/reservation`)

4. **GitHub secret** — `KC_WEB_CLIENT_SECRET`: generate strongly
   (`openssl rand -base64 24`). The deploy workflow substitutes it into the
   deployed realm (`dev-web-secret` never carries to the QNAP).

5. **Router port forwards** (needed for public access + the CI deploy):
   - `8080` → QNAP:6680 (app; the NAS-side bind avoids QTS's admin 8080)
   - `8081` → QNAP:6681 (Keycloak public entry; same reasoning)
   - `22` → QNAP:22 (deploy SSH; keys-only enforced in QTS)
   With myQNAPcloud DDNS the public URLs keep :8080 / :8081 (mapped to
   NAS-side 6680 / 6681 by the router).

6. **Host env file** — `/opt/reservation/.env` (mode 600):

   ```bash
   # data plane
   POSTGRES_PASSWORD=<openssl rand -base64 24>

   # Keycloak console bootstrap (NOT admin/admin — the realm-imported admin
   # user is seeded temporary:true and rotates at first sign-in)
   KEYCLOAK_ADMIN=<console user>
   KEYCLOAK_ADMIN_PASSWORD=<openssl rand -base64 24>

   # Keycloak public face (through the tunnel) — pins every token's `iss`
   KC_PUBLIC_BASE_URL=https://auth.example.com
   JWT_ISSUER_URI=https://auth.example.com/realms/reservation

   # browser-facing legs (public, tunnel)
   KEYCLOAK_AUTHORIZATION_URI=https://auth.example.com/realms/reservation/protocol/openid-connect/auth
   KEYCLOAK_END_SESSION_URI=https://auth.example.com/realms/reservation/protocol/openid-connect/logout

   # server legs (compose-internal, no tunnel)
   KEYCLOAK_TOKEN_URI=http://keycloak:8081/realms/reservation/protocol/openid-connect/token
   KEYCLOAK_JWK_SET_URI=http://keycloak:8081/realms/reservation/protocol/openid-connect/certs

   # pairs with the secret the deploy injects into the realm
   KC_WEB_CLIENT_SECRET=<same value as the GitHub secret>

   # public app hostname (redirect derivation + tunnel smoke)
   # optional: the app resolves {baseUrl} from forwarded headers by default
   APP_PUBLIC_BASE_URL=https://reservation.example.com
   KC_WEB_REDIRECT_URI=https://reservation.example.com/login/oauth2/code/keycloak
   ```

7. **GHCR pull (private package, B-2)** — one-time on the QNAP: create a
   fine-grained PAT with `read:packages`, then
   `docker login ghcr.io -u <github-username>` using it. Potentially re-login
   when the PAT expires.

## Deploy

GitHub → Actions → **Deploy to QNAP** → run with the image SHA you want
(the Release workflow publishes `:sha` images on every main push). The
workflow: generates the QNAP realm (tunnel URLs + real client secret),
syncs `docker-compose.deploy.yml` + realm, pulls the exact SHA, boots the
stack, health-checks, and smokes (public venue + 401 on availability).

## Post-deploy manual checks

1. `http://dukescloud.myqnapcloud.com:8080` — home page in the default language (this lands on NAS :6680 via the router)
2. Language selector → Polish / Greek — copy + `lang` attribute switch
3. Log in (alice / alice → first sign-in forces a password rotation) —
   redirected through `https://<auth-host>`, back to the app
4. Book a slot end to end; cancel it
5. Admin panel → /admin/content (per-language editor), /admin/map
6. Keycloak console at the DDNS host's 8081 (master realm;
   `KEYCLOAK_ADMIN` credentials) — check the `reservation` realm users

## Maintenance

- **Rollback**: re-run the Deploy workflow with the previous known-good SHA
  (compose recreates containers; images and volumes stay untouched). The
  workflow records the previously deployed tag into
  `/opt/reservation/.previous-image` before every overwrite — copy that
  value from the host as the rollback input.
- **Database backup** (run on the QNAP, schedule via cron):
  `docker exec reservation-postgres pg_dump -U reservation reservation | gzip > /opt/reservation/backup/reservation-$(date +%F).sql.gz`
  Restore: `gunzip -c <file> | docker exec -i reservation-postgres psql -U reservation -d reservation`.
- **Secret rotation**: change the value in the host `.env` (and the GitHub
  `KC_WEB_CLIENT_SECRET` for the realm injection) → re-run Deploy. Keycloak
  realm-internal secrets can also be rotated in its admin console (the
  realm persists across recreations now — a rotated password stays rotated).
  Note: changing the realm inject happens on fresh realm imports only; to
  force one, delete the keycloak volume AND re-run deploy on a fresh DB.
- **TLS status (dev)**: everything routes plain HTTP on forwarded ports; a
  TLS termination gateway (Cloudflare/domain or QNAP reverse proxy + LE cert)
  comes with the real production environment. The Keycloak + app URLs the
  realm redirect URIs use must match the exposed ports exactly.
- **Realm tightening notes**: demo realm has `directAccessGrantsEnabled` on
  `reservation-api` (password grant) — acceptable for the demo, disable for
  anything longer-lived; demo users are seeded `temporary:true`.
