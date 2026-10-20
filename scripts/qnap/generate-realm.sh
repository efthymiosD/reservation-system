#!/usr/bin/env bash
# QNAP realm generation — runs ON the QNAP (self-hosted runner).
# Transforms the committed dev realm: dev URLs -> public funnel hostnames,
# dev client secret -> the real one read from the HOST .env. The secret
# never transits GitHub and is never printed.
# Contract: runs from the repo checkout (realm source); reads the secret
# from $DEPLOY_DIR/.env.
set -euo pipefail
cd "${GITHUB_WORKSPACE:?must run inside the checkout workspace}"

APP_URL="${APP_URL:?APP_URL (public funnel app URL) required}"
DEPLOY_DIR="${DEPLOY_DIR:?DEPLOY_DIR required}"

ENV_FILE="$DEPLOY_DIR/.env"
test -f "$ENV_FILE" || { echo "ERROR: $ENV_FILE missing — see docs/RUNBOOK.md provisioning"; exit 1; }
# Defensive parse (no file sourcing): tolerate CRs, take the first match.
WEB_SECRET="$(grep -E '^KC_WEB_CLIENT_SECRET=' "$ENV_FILE" | head -n1 | cut -d= -f2- | tr -d '\r' || true)"
[ -n "$WEB_SECRET" ] || { echo "ERROR: KC_WEB_CLIENT_SECRET missing in $ENV_FILE"; exit 1; }

APP_URL="$APP_URL" WEB_SECRET="$WEB_SECRET" python3 -B - <<'PY'
import json
import os
import pathlib
import re

app_url = os.environ["APP_URL"].rstrip("/")
web_secret = os.environ["WEB_SECRET"]

src = pathlib.Path("keycloak/realm-export.json")
out = pathlib.Path("keycloak/realm-export.qnap.json")

raw = src.read_text(encoding="utf-8")
dev_secret_count = raw.count("dev-web-secret")
localhost_count = len(re.findall(r"http://localhost:\d+", raw))
realm = json.loads(raw)

for client in realm.get("clients", []):
    if client.get("secret") == "dev-web-secret":
        client["secret"] = web_secret
    client["redirectUris"] = [
        u.replace("http://localhost:8080", app_url) for u in client.get("redirectUris", [])
    ]
    client["webOrigins"] = [
        o.replace("http://localhost:8080", app_url) for o in client.get("webOrigins", [])
    ]
    logout_uris = client.get("attributes", {}).get("post.logout.redirect.uris")
    if isinstance(logout_uris, str):
        client["attributes"]["post.logout.redirect.uris"] = logout_uris.replace(
            "http://localhost:8080", app_url
        )

for client in realm.get("clients", []):
    uris = json.dumps(client.get("redirectUris", [])) + json.dumps(client.get("webOrigins", []))
    assert "localhost" not in uris, f"unreplaced localhost URI in client {client.get('clientId')}"
    assert client.get("secret") != "dev-web-secret", f"unreplaced dev secret in client {client.get('clientId')}"

# The public DEV box is internet-exposed: every demo user must rotate their
# password at first sign-in (UPDATE_PASSWORD required action via a temporary
# credential). The COMMITTED dev realm stays temporary:false on purpose —
# the e2e stack imports this exact file and its login helper must not hit
# the rotation screen.
rotated = 0
for user in realm.get("users", []):
    for cred in user.get("credentials", []):
        if cred.get("type") == "password":
            cred["temporary"] = True
            rotated += 1

tmp = out.with_suffix(".tmp")
tmp.write_text(json.dumps(realm, indent=2), encoding="utf-8")
json.loads(tmp.read_text(encoding="utf-8"))  # validate
tmp.rename(out)

# evidence WITHOUT secret contents
print(f"realm generated: {dev_secret_count} dev secret(s) replaced, "
      f"{localhost_count} dev URL reference(s) rewritten, "
      f"{rotated} demo password(s) set temporary (rotate at first sign-in), "
      f"output {out}")
PY

grep -q "$APP_URL" keycloak/realm-export.qnap.json \
  || { echo "ERROR: no localhost URI was replaced — check QNAP_PUBLIC_APP_URL"; exit 1; }
[ -s keycloak/realm-export.qnap.json ] || { echo "ERROR: empty generated realm"; exit 1; }
