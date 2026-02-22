#!/bin/bash

# SentientGate: Unit Test Runner
# This script runs tests for all microservices except ApiGateway.

# Colors for better output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "🚀 Starting SentientGate Unit Test Suite..."

# Function to run gradle tests
run_gradle_test() {
    local dir=$1
    echo -e "Testing ${GREEN}${dir}${NC} (Gradle)..."
    if [ -d "$dir" ]; then
        cd "$dir"
        chmod +x gradlew
        ./gradlew test
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ $dir tests passed!${NC}"
        else
            echo -e "${RED}❌ $dir tests failed!${NC}"
        fi
        cd ..
    else
        echo -e "${RED}⚠️  Directory $dir not found!${NC}"
    fi
    echo "----------------------------------------"
}

# Function to run maven tests
run_maven_test() {
    local dir=$1
    echo -e "Testing ${GREEN}${dir}${NC} (Maven)..."
    if [ -d "$dir" ]; then
        cd "$dir"
        chmod +x mvnw
        ./mvnw test
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ $dir tests passed!${NC}"
        else
            echo -e "${RED}❌ $dir tests failed!${NC}"
        fi
        cd ..
    else
        echo -e "${RED}⚠️  Directory $dir not found!${NC}"
    fi
    echo "----------------------------------------"
}

# Run tests for each service
run_gradle_test "MCPService"
run_gradle_test "LogingService"
run_gradle_test "DummyService"
run_maven_test "AIService"

echo "🎉 All tests completed!"
