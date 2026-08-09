#!/bin/bash
# scripts/stop_local.sh
cd "$(dirname "$0")/.."

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🛑 Stopping SentientGate Services locally...${NC}"

echo -e "${BLUE}Stopping Microservices and UI processes...${NC}"
# Find and kill processes related to the project
for service in EurekaServer ApiGateway LoggingService MCPService AIService Dummy sentinel-gateway-ui; do
    if pgrep -f "$service" > /dev/null; then
        echo "Killing processes for $service..."
        pkill -f "$service"
    fi
done

# Catch-all for lingering gradlew or node development server processes associated with this project
if pgrep -f "gradlew bootRun" > /dev/null; then
    echo "Killing remaining gradlew bootRun processes..."
    pkill -f "gradlew bootRun"
fi

if pgrep -f "vite" > /dev/null; then
    echo "Killing vite development server..."
    pkill -f "vite"
fi

echo -e "${BLUE}🛑 Stopping Infrastructure (Postgres, Redis, Kafka, Kafka-UI)...${NC}"
if [ -d "TOOLS" ]; then
    cd TOOLS
    # Check if docker-compose.yml or docker-compose.yaml exists before running down
    if [ -f "docker-compose.yml" ] || [ -f "docker-compose.yaml" ]; then
        docker compose down
    else
        echo -e "${RED}docker-compose file not found in TOOLS. Skipping.${NC}"
    fi
    cd ..
else
    echo -e "${RED}TOOLS directory not found. Skipping docker compose down.${NC}"
fi

echo -e "${GREEN}✅ All services stopped successfully!${NC}"
