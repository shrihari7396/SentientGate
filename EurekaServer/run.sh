#!/bin/bash
cd "$(dirname "$0")"
echo "Starting EurekaServer..."
./gradlew bootRun -x test
