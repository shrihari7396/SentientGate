#!/bin/bash
cd "$(dirname "$0")"
echo "Starting AIService..."
./mvnw spring-boot:run -DskipTests
