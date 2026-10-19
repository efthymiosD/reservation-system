#!/usr/bin/env bash
# One-time (idempotent) Tailscale Funnel setup for the QNAP DEV environment.
# Runs ON the QNAP after:
#   docker compose -f docker-compose.tailscale.yml --env-file .env up -d
# First run: tailscale prints an approval URL and blocks — open it in a
# browser, log in to the tailnet and approve enabling Funnel (this also
# grants the funnel nodeAttr to the tailnet policy). Later runs re-assert
# the same config without any interaction.
# Prerequisites (docs/RUNBOOK.md): MagicDNS + HTTPS certificates enabled on
# the tailnet. Mind Let's Encrypt rate limits — do NOT toggle funnel on/off
# repeatedly (up to 34h lockout).
#
# If your installed tailscale version rejects the flags below, consult
# `tailscale funnel --help` for its exact syntax and adjust.
set -euo pipefail

TS="docker exec reservation-tailscale tailscale"
# Loopback targets hit the NAS host bindings published by
# docker-compose.deploy.yml (host-networking carrier). Override both for the
# userspace-networking fallback (bridge network + NAS LAN IP).
TARGET_APP="${FUNNEL_APP_TARGET:-http://127.0.0.1:6680}"
TARGET_KC="${FUNNEL_KC_TARGET:-http://127.0.0.1:6681}"

echo "== Waiting for tailscaled =="
ok=""
for i in $(seq 1 30); do
  if $TS status >/dev/null 2>&1; then ok=yes; break; fi
  sleep 2
done
[ -n "$ok" ] || { echo "FAILED: tailscaled not up — check TS_AUTHKEY and 'docker logs reservation-tailscale'"; exit 1; }
echo "tailscaled: up"

echo "== Funnel: app 443 -> $TARGET_APP =="
# First-ever run blocks here until the funnel approval URL is opened.
$TS funnel --bg --https=443 "$TARGET_APP"

echo "== Funnel: keycloak 8443 -> $TARGET_KC =="
$TS funnel --bg --https=8443 "$TARGET_KC"

echo
echo "== Funnel status =="
$TS funnel status
echo
echo "Record both public URLs (https://<node>.<tailnet>.ts.net and ...:8443)"
echo "into the QNAP_PUBLIC_* GitHub variables — see docs/RUNBOOK.md."
