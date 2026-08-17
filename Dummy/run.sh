#!/bin/bash
cd "$(dirname "$0")"
echo "Starting Dummy..."
./gradlew bootRun -x test
