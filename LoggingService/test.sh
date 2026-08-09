#!/bin/bash
cd "$(dirname "$0")"
echo "Testing LoggingService..."
./gradlew test
