#!/bin/bash
cd "$(dirname "$0")"
echo "Starting MCPService..."
./gradlew bootRun -x test
