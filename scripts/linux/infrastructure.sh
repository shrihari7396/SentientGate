#!/bin/bash
# scripts/<os>/infrastructure.sh
# Bring up the local infrastructure stack (Postgres, Redis, Kafka, Kafka-UI, ...)
# defined in TOOLS/docker-compose.yml.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$ROOT_DIR/TOOLS"
docker compose up -d
