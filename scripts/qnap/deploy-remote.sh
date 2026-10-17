#!/usr/bin/env bash
# QNAP deploy — runs ON the QNAP via SSH (appleboy forwards IMAGE_TAG).
# Contract: the deployment directory is the current working directory.
set -euo pipefail

IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG (40-char SHA) required}"

# One-time setup expectations; never overwrite the host .env.
test -f .env || { echo "ERROR: .env missing — see docs/RUNBOOK.md provisioning"; exit 1; }
mkdir -p keycloak

# record the currently deployed image for the rollback procedure
cp .env.image .previous-image 2>/dev/null || true

echo "RESERVATION_IMAGE=ghcr.io/efthymiosd/reservation-system:$IMAGE_TAG" > .env.image
mv keycloak/realm-export.qnap.json keycloak/realm-export.json
docker compose -f docker-compose.deploy.yml --env-file .env --env-file .env.image pull app
docker compose -f docker-compose.deploy.yml --env-file .env --env-file .env.image up -d
echo "deployed: $(cat .env.image)"
