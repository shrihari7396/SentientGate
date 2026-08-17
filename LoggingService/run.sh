#!/bin/bash
cd "$(dirname "$0")"
echo "Starting LoggingService..."
./gradlew bootRun -x test
