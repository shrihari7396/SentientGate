#!/bin/bash
# scripts/<os>/stop_local.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_DIR"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🛑 Stopping SentientGate Services locally...${NC}"

echo -e "${BLUE}Stopping Microservices and UI processes...${NC}"
# Find and kill processes related to the project
for service in EurekaServer ApiGateway LoggingService MCPService AIService services sentinel-gateway-ui; do
    if pgrep -f "$service" > /dev/null; then
        echo "Killing processes for $service..."
        pkill -f "$service"
    fi
done

# Catch-all for lingering Maven spring-boot:run or node dev-server processes.
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "Killing remaining spring-boot:run processes..."
    pkill -f "spring-boot:run"
fi

if pgrep -f "vite" > /dev/null; then
    echo "Killing vite development server..."
    pkill -f "vite"
fi

echo -e "${BLUE}🛑 Stopping Infrastructure (Postgres, Redis, Kafka, Kafka-UI)...${NC}"
if [ -d "TOOLS" ]; then
    if [ -f "TOOLS/docker-compose.yml" ] || [ -f "TOOLS/docker-compose.yaml" ]; then
        ( cd TOOLS && docker compose down )
    else
        echo -e "${RED}docker-compose file not found in TOOLS. Skipping.${NC}"
    fi
else
    echo -e "${RED}TOOLS directory not found. Skipping docker compose down.${NC}"
fi

echo -e "${GREEN}✅ All services stopped successfully!${NC}"
