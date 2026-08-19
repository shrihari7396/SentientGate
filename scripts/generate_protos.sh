#!/bin/bash
# scripts/generate_protos.sh
cd "$(dirname "$0")/.."

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔨 Generating Protocol Buffer files...${NC}"

# LoggingService
echo -e "${BLUE}➡️ Generating protos for LoggingService...${NC}"
cd LoggingService
./gradlew generateProto
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ LoggingService protos generated successfully!${NC}"
else
    echo -e "${RED}❌ Failed to generate protos for LoggingService!${NC}"
    exit 1
fi
cd ..

# MCPService
echo -e "${BLUE}➡️ Generating protos for MCPService...${NC}"
cd MCPService
./gradlew generateProto
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ MCPService protos generated successfully!${NC}"
else
    echo -e "${RED}❌ Failed to generate protos for MCPService!${NC}"
    exit 1
fi
cd ..

echo -e "${GREEN}🎉 All proto generation completed successfully!${NC}"
exit 0
