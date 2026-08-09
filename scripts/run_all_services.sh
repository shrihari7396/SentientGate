#!/bin/bash
cd ..

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting SentientGate Services Orchestration...${NC}"

# Step 1: Start Infrastructure services
echo -e "${BLUE}📦 Starting Infrastructure (Postgres, Redis, Kafka, Zookeeper)...${NC}"
docker compose up -d postgres redis zookeeper kafka

# Step 2: Wait for Postgres to be healthy
echo -e "${BLUE}⏳ Waiting for Postgres database to be healthy...${NC}"
until docker exec sentient-postgres pg_isready -U postgres >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ Postgres is healthy!${NC}"

# Step 3: Start Eureka Server
echo -e "${BLUE}📦 Starting Eureka Server...${NC}"
docker compose up -d eureka-server

# Step 4: Wait for Eureka Server port 8761 to open
echo -e "${BLUE}⏳ Waiting for Eureka Server to listen on port 8761...${NC}"
until curl -s http://localhost:8761/eureka/apps >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ Eureka Registry is up and running!${NC}"

# Step 5: Start all downstream microservices and frontend
echo -e "${BLUE}📦 Starting Microservices (ApiGateway, LoggingService, MCPServer, AIService, Dummy, SentinelUI)...${NC}"
docker compose up -d api-gateway logging-service mcp-server ai-service dummy-service sentinel-ui

echo -e "\n${GREEN}🎉 All services started successfully!${NC}"
echo -e "${BLUE}Access points:${NC}"
echo -e " - ${GREEN}Sentinel Overwatch UI:${NC} http://localhost:5173"
echo -e " - ${GREEN}Eureka Dashboard:${NC}      http://localhost:8761"
echo -e " - ${GREEN}ApiGateway Base Endpoint:${NC} http://localhost:8079"
echo -e "\nTo view running containers: ${BLUE}docker compose ps${NC}"
echo -e "To tail service logs:       ${BLUE}docker compose logs -f <service-name>${NC}"
