#!/bin/bash
cd "$(dirname "$0")"
echo "Starting ApiGateway..."
./mvnw spring-boot:run -DskipTests
