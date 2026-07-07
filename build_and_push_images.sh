#!/bin/bash

# Configuration
REGISTRY_USER="shrihari7396"
TAG="latest"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting Build and Push Process for SentientGate Services...${NC}"

# Define services with their folder name and final image name
# Format: "FOLDER_NAME:IMAGE_NAME"
SERVICES=(
    "EurekaServer:eureka-server"
    "ApiGateway:api-gateway"
    "LoggingService:logging-service"
    "MCPService:mcp-server"
    "AIService:ai-service"
    "Dummy:dummy-service"
    "UI/sentinel-gateway-ui:sentinel-ui"
)

# Function to build and push a service
build_and_push() {
    local folder=$1
    local image=$2
    local local_tag="sentientgate_${image}"
    local remote_tag="${REGISTRY_USER}/${image}:${TAG}"

    echo -e "\n${BLUE}------------------------------------------------${NC}"
    echo -e "${BLUE}🔨 Building service in: ${folder} (Local Tag: ${local_tag})...${NC}"
    echo -e "${BLUE}------------------------------------------------${NC}"

    if [ -d "$folder" ]; then
        # Build image
        docker build -t "$local_tag" "$folder"
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Successfully built local image: ${local_tag}${NC}"
            
            # Tag image
            echo -e "${BLUE}🏷️  Tagging ${local_tag} as ${remote_tag}...${NC}"
            docker tag "$local_tag" "$remote_tag"
            
            # Push image
            echo -e "${BLUE}📤 Pushing ${remote_tag} to registry...${NC}"
            docker push "$remote_tag"
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ Successfully pushed: ${remote_tag}${NC}"
            else
                echo -e "${RED}❌ Failed to push: ${remote_tag}${NC}"
            fi
        else
            echo -e "${RED}❌ Failed to build service in folder: ${folder}${NC}"
        fi
    else
        echo -e "${RED}⚠️  Folder not found: ${folder}${NC}"
    fi
}

# Run the loop
for service in "${SERVICES[@]}"; do
    FOLDER="${service%%:*}"
    IMAGE="${service##*:}"
    build_and_push "$FOLDER" "$IMAGE"
done

echo -e "\n${GREEN}🎉 All build and push operations completed!${NC}"
