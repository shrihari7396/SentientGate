#!/bin/bash
cd ..

# Base directory
BASE_DIR=$(pwd)

echo "Formatting all services with Spotless..."

format_maven_service() {
    SERVICE_NAME=$1
    SERVICE_PATH=$BASE_DIR/$SERVICE_NAME

    echo "----------------------------------------"
    echo "Formatting Maven Service: $SERVICE_NAME..."
    echo "----------------------------------------"
    cd $SERVICE_PATH || exit
    mvn spotless:apply
}

# Format Maven services (all services are Maven)
format_maven_service AIService
format_maven_service ApiGateway
format_maven_service Dummy
format_maven_service EurekaServer
format_maven_service LoggingService
format_maven_service MCPService

echo "----------------------------------------"
echo "All services formatted!"
echo "----------------------------------------"
