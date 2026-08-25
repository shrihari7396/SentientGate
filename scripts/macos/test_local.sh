#!/bin/bash
# scripts/<os>/test_local.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_DIR"

# Service set + per-service run/test dispatch (single source of truth).
source "$SCRIPT_DIR/services.sh"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Generate Protos — abort the whole run if codegen fails (otherwise the failure
# is only surfaced later as an opaque compile error).
if ! "$SCRIPT_DIR/generate_protos.sh"; then
    echo -e "${RED}❌ Proto generation failed. Aborting tests.${NC}"
    exit 1
fi

echo -e "${BLUE}🚀 Starting SentientGate Services Tests...${NC}"

# Start Infrastructure for integration tests
echo -e "${BLUE}📦 Starting Infrastructure (Postgres, Redis, Kafka, Zookeeper)...${NC}"
( cd TOOLS && docker compose up -d )

echo -e "${BLUE}⏳ Waiting for Postgres database to be healthy...${NC}"
attempt=0
until docker exec postgres-db pg_isready -U postgres >/dev/null 2>&1 || docker exec sentient-postgres pg_isready -U postgres >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -ge 60 ]; then
        echo -e "\n${RED}❌ Postgres did not become healthy after 120s. Aborting.${NC}"
        exit 1
    fi
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ Postgres is healthy!${NC}"

FAILED_SERVICES=()

for service in "${SERVICES[@]}"; do
    folder="${service%%:*}"
    label="${service##*:}"
    echo -e "${BLUE}🧪 Testing $label...${NC}"
    if test_service "$folder"; then
        echo -e "${GREEN}✅ $label tests passed!${NC}"
    else
        echo -e "${RED}❌ $label tests failed!${NC}"
        FAILED_SERVICES+=("$label")
    fi
done

if [ ${#FAILED_SERVICES[@]} -ne 0 ]; then
    echo -e "${RED}❌ Tests failed in the following services: ${FAILED_SERVICES[*]}${NC}"
    exit 1
else
    echo -e "${GREEN}🎉 All services tested successfully!${NC}"
    exit 0
fi
