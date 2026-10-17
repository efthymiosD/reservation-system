#!/usr/bin/env bash
# QNAP smoke tests — runs ON the QNAP via SSH; appleboy forwards FUTURE_DATE
# (computed on the runner, GNU date — busybox dialects differ on QTS).
# The LAN-side app port is 6680 on the NAS (host side) because the QTS web
# admin owns 8080. The	public-door check runs from the GitHub runner.
set -euo pipefail
FUTURE_DATE="${FUTURE_DATE:?FUTURE_DATE required}"
BASE=http://localhost:6680
# Compose pins container_name; names are stable.
APP_LOGS=reservation-api

echo "== Waiting for health =="
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

echo "== Smoke: stack containers healthy =="
for c in reservation-postgres reservation-keycloak; do
  if docker ps --format '{{.Names}}' | grep -qx "$c"; then
    echo "$c: running"
  else
    echo "SMOKE FAILED: $c is not running"; exit 1
  fi
done

echo "smoke tests passed"
