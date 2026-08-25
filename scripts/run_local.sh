#!/bin/bash
# scripts/run_local.sh
cd "$(dirname "$0")/.."
mkdir -p logs

# Service set + per-service run/test dispatch (single source of truth).
source scripts/services.sh

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting SentientGate Services locally (without Docker for services)...${NC}"

# Step 1: Start Infrastructure services
echo -e "${BLUE}📦 Starting Infrastructure (Postgres, Redis, Kafka, Kafka-UI)...${NC}"
cd TOOLS
docker compose up -d postgres redis kafka kafka-ui
cd ..

# Step 2: Wait for Postgres to be healthy
echo -e "${BLUE}⏳ Waiting for Postgres database to be healthy...${NC}"
until docker exec postgres-db pg_isready -U postgres >/dev/null 2>&1 || docker exec sentient-postgres pg_isready -U postgres >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ Postgres is healthy!${NC}"

# Step 3: Start Eureka Server first (service registry) — every other service depends on it.
echo -e "${BLUE}📦 Starting Eureka Server locally...${NC}"
run_service EurekaServer > logs/EurekaServer.log 2>&1 &

# Step 4: Wait for Eureka Server port 8761 to open
echo -e "${BLUE}⏳ Waiting for Eureka Server to listen on port 8761...${NC}"
until curl -s http://localhost:8761/eureka/apps >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ Eureka Registry is up and running!${NC}"

# Step 5: Start all downstream microservices and the frontend.
echo -e "${BLUE}📦 Starting Microservices (ApiGateway, LoggingService, MCPServer, AIService, Dummy, SentinelUI)...${NC}"
for service in "${SERVICES[@]}"; do
    folder="${service%%:*}"
    label="${service##*:}"
    [ "$folder" = "EurekaServer" ] && continue   # already started in Step 3
    run_service "$folder" > "logs/${label}.log" 2>&1 &
done

echo -e "\n${GREEN}🎉 All services started successfully in the background!${NC}"
echo -e "${BLUE}Logs are being written to the 'logs/' directory.${NC}"
echo -e "${BLUE}Press Ctrl+C to stop all services (the background processes).${NC}"

# Wait for all background jobs (the services) to finish, or trap Ctrl-C to kill them.
trap 'kill $(jobs -p)' EXIT
wait
