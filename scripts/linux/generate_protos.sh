#!/bin/bash
# scripts/<os>/generate_protos.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_DIR"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔨 Generating Protocol Buffer files...${NC}"

for svc in LoggingService MCPService; do
    echo -e "${BLUE}➡️  Generating protos for ${svc}...${NC}"
    if ( cd "$svc" && ./mvnw -q protobuf:compile protobuf:compile-custom ); then
        echo -e "${GREEN}✅ ${svc} protos generated successfully!${NC}"
    else
        echo -e "${RED}❌ Failed to generate protos for ${svc}!${NC}"
        exit 1
    fi
done

echo -e "${GREEN}🎉 All proto generation completed successfully!${NC}"
exit 0
