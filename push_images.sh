#!/bin/bash

# Configuration
USERNAME="shrihari7396"
TAG="latest"

# List of services
SERVICES=(
    "sentientgate_eureka-server:eureka-server"
    "sentientgate_api-gateway:api-gateway"
    "sentientgate_logging-service:logging-service"
    "sentientgate_mcp-server:mcp-server"
    "sentientgate_ai-service:ai-service"
    "sentientgate_dummy-service:dummy-service"
    "sentientgate_sentinel-ui:sentinel-ui"
)

echo "Starting Docker Push Process..."

# Loop through services
for service_info in "${SERVICES[@]}"; do
    LOCAL_IMAGE="${service_info%%:*}"
    REMOTE_NAME="${service_info##*:}"
    
    REMOTE_IMAGE="$USERNAME/$REMOTE_NAME:$TAG"
    
    echo "------------------------------------------------"
    echo "Tagging $LOCAL_IMAGE as $REMOTE_IMAGE..."
    docker tag $LOCAL_IMAGE $REMOTE_IMAGE
    
    echo "Pushing $REMOTE_IMAGE..."
    docker push $REMOTE_IMAGE
done

echo "------------------------------------------------"
echo "All images pushed successfully!"
