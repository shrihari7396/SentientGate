#!/bin/bash
cd "$(dirname "$0")"
echo "Testing MCPService..."
./gradlew test
