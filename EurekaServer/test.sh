#!/bin/bash
cd "$(dirname "$0")"
echo "Testing EurekaServer..."
./gradlew test
