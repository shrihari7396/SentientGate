#!/bin/bash

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

format_gradle_service() {
    SERVICE_NAME=$1
    SERVICE_PATH=$BASE_DIR/$SERVICE_NAME

    echo "----------------------------------------"
    echo "Formatting Gradle Service: $SERVICE_NAME..."
    echo "----------------------------------------"
    cd $SERVICE_PATH || exit
    ./gradlew spotlessApply
}

# Format Maven services
format_maven_service AIService
format_maven_service ApiGateway

# Format Gradle services
format_gradle_service DummyService
format_gradle_service EurekaServer
format_gradle_service LogingService
format_gradle_service MCPService

echo "----------------------------------------"
echo "All services formatted!"
echo "----------------------------------------"
