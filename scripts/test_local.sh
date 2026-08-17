#!/bin/bash
# scripts/test_local.sh
cd "$(dirname "$0")/.."

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting SentientGate Services Tests...${NC}"

# Start Infrastructure for integration tests
echo -e "${BLUE}📦 Starting Infrastructure (Postgres, Redis, Kafka, Zookeeper)...${NC}"
cd TOOLS
docker compose up -d 

cd ..
cd UI
cd sentinel-gateway-ui  
npm install
cd ..
cd ..

echo -e "${BLUE}⏳ Waiting for Postgres database to be healthy...${NC}"
until docker exec postgres-db pg_isready -U postgres >/dev/null 2>&1 || docker exec sentient-postgres pg_isready -U postgres >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e "\n${GREEN}✅ Postgres is healthy!${NC}"

# Array of services to test
SERVICES=("EurekaServer" "ApiGateway" "LoggingService" "MCPService" "AIService" "Dummy" "UI/sentinel-gateway-ui")
FAILED_SERVICES=()

for SERVICE in "${SERVICES[@]}"; do
    echo -e "${BLUE}🧪 Testing $SERVICE...${NC}"
    if ./$SERVICE/test.sh; then
        echo -e "${GREEN}✅ $SERVICE tests passed!${NC}"
    else
        echo -e "${RED}❌ $SERVICE tests failed!${NC}"
        FAILED_SERVICES+=("$SERVICE")
    fi
done

if [ ${#FAILED_SERVICES[@]} -ne 0 ]; then
    echo -e "${RED}❌ Tests failed in the following services: ${FAILED_SERVICES[*]}${NC}"
    exit 1
else
    echo -e "${GREEN}🎉 All services tested successfully!${NC}"
    exit 0
fi
