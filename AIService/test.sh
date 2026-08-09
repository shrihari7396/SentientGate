#!/bin/bash
cd "$(dirname "$0")"
echo "Testing AIService..."
./mvnw test
