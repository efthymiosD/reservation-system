#!/usr/bin/env bash
# QNAP smoke tests — runs ON the QNAP in the self-hosted runner. The LAN-side
# app port is 6680 on the NAS (host side) because the QTS web admin owns
# 8080. The funnel-door checks probe the two public ts.net URLs (443 app /
# 8443 keycloak); that traffic hairpins via the Tailscale edge, exercising
# the whole public path (public DNS -> relay -> funnel -> local service).
set -euo pipefail
FUTURE_DATE="${FUTURE_DATE:?FUTURE_DATE required}"
PUBLIC_APP_URL="${PUBLIC_APP_URL:?PUBLIC_APP_URL (funnel, 443) required}"
PUBLIC_KC_URL="${PUBLIC_KC_URL:?PUBLIC_KC_URL (funnel, 8443) required}"
# Compose pins container_name; names are stable. LAN base overridable for a
# bridge-mode runner container (default assumes host networking).
BASE="${LAN_APP_URL:-http://localhost:6680}"
APP_LOGS=reservation-api

echo "== Waiting for health (LAN: $BASE) =="
ok=""
for i in $(seq 1 40); do
  if curl -sf "$BASE/actuator/health" | grep -q '"status":"UP"'; then ok=yes; break; fi
  sleep 3
done
[ -n "$ok" ] || { echo "HEALTH CHECK FAILED"; docker logs "$APP_LOGS" --tail 100; exit 1; }
echo "health: UP"

echo "== Smoke: public venue endpoint =="
curl -sf "$BASE/api/public/venue" | grep -q . || { echo "SMOKE FAILED: /api/public/venue"; exit 1; }

echo "== Smoke: availability requires authentication =="
# dynamic future date — never staleness-affected
code=$(curl -s -o /dev/null -w '%{http_code}' \
  "$BASE/api/availability?date=$FUTURE_DATE&start=10:00&durationMinutes=60")
[ "$code" = "401" ] || { echo "SMOKE FAILED: /api/availability returned $code, expected 401"; exit 1; }

echo "== Smoke: funnel door — app ($PUBLIC_APP_URL) =="
door=""
for i in $(seq 1 10); do
  if curl -sf -m 15 "$PUBLIC_APP_URL/actuator/health" | grep -q '"status":"UP"'; then door=yes; break; fi
  sleep 3
done
[ -n "$door" ] || { echo "FUNNEL DOOR FAILED: $PUBLIC_APP_URL unreachable (tailscale up? funnel on? certs?)"; exit 1; }
echo "funnel door (app): UP"

echo "== Smoke: funnel door — keycloak ($PUBLIC_KC_URL) =="
door=""
for i in $(seq 1 10); do
  if curl -sf -m 15 "$PUBLIC_KC_URL/realms/reservation" | grep -q '"realm"'; then door=yes; break; fi
  sleep 3
done
[ -n "$door" ] || { echo "FUNNEL DOOR FAILED: $PUBLIC_KC_URL/realms/reservation unreachable"; exit 1; }
echo "funnel door (keycloak): UP"

echo "== Smoke: stack containers healthy =="
for c in reservation-postgres reservation-keycloak reservation-tailscale; do
  if docker ps --format '{{.Names}}' | grep -qx "$c"; then
    echo "$c: running"
  else
    echo "SMOKE FAILED: $c is not running"; exit 1
  fi
done

echo "smoke tests passed"
