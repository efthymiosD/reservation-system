# QNAP Deployment Runbook

DEV environment on the QNAP (publicly reachable via the Cloudflare tunnel;
the real production deployment is deferred): Postgres + Keycloak + the app,
public access exclusively through the tunnel (no public host ports; the
`8080:8080` LAN binding remains for operator smoke checks only). The stack
runs the app with the prod Spring profile on purpose — the box is
internet-exposed, so real OAuth2/JWT enforcement applies despite the
environment label.

```
Cloudflare edge (TLS)
   └─ cloudflared (container, tunnel token) — remotely-managed ingress
        ├─ https://<app-host>  → http://app:8080
        └─ https://<auth-host> → http://keycloak:8081
```

## NAS safety box (hard rules)

- Never delete `/container-station-data`, reinstall QTS, or touch unrelated
  services (e.g. QuMagie's `system-docker`) from this runbook.
- Never run `docker compose down -v` against the QNAP deployment — data
  (postgres volume, keycloak data) survives routine deploys.
- Never paste database passwords, client secrets, SSH keys, tunnel tokens or
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

2. **SSH access** — generate a dedicated keypair; public key into the QNAP
   user's `authorized_keys`. Store in GitHub secrets as
   `QNAP_DEPLOY_SSH_KEY` (+ `QNAP_DEPLOY_HOST`, `QNAP_DEPLOY_USER`).

3. **GitHub variables**
   - `QNAP_PUBLIC_APP_URL` — the app's tunnel hostname
     (e.g. `https://reservation.example.com`)
   - `QNAP_PUBLIC_BASE_URL` — the Keycloak tunnel hostname
     (e.g. `https://auth.example.com`)
   - optional `QNAP_DEPLOY_DIR`

4. **GitHub secret** — `KC_WEB_CLIENT_SECRET`: generate strongly
   (`openssl rand -base64 24`). The deploy workflow substitutes it into the
   deployed realm (`dev-web-secret` never carries to the QNAP).

5. **Cloudflare tunnel**
   - Zero Trust dashboard → Networks → Tunnels → Create tunnel
     (Cloudflared connector; copy the tunnel token)
   - Public hostnames (remotely-managed ingress) on the tunnel:
     - `<app-host>` → service `http://app:8080`
     - `<auth-host>` → service `http://keycloak:8081`
     (the service names resolve because cloudflared joins the compose network)
   - Store the token on the host: `CLOUDFLARED_TUNNEL_TOKEN=...` in `.env`

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
   CLOUDFLARED_TUNNEL_TOKEN=<from the tunnel creation>

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

1. `https://<app-host>` — home page in the default language
2. Language selector → Polish / Greek — copy + `lang` attribute switch
3. Log in (alice / alice → first sign-in forces a password rotation) —
   redirected through `https://<auth-host>`, back to the app
4. Book a slot end to end; cancel it
5. Admin panel → /admin/content (per-language editor), /admin/map
6. Keycloak console at `https://auth.example.com` (master realm;
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
- **Tunnel incidents**: `docker logs reservation-cloudflared` on the QNAP;
  the connector is stateless — recreating it with the same token reconnects.
- **Realm tightening notes**: demo realm has `directAccessGrantsEnabled` on
  `reservation-api` (password grant) — acceptable for the demo, disable for
  anything longer-lived; demo users are seeded `temporary:true`.
