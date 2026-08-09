#!/bin/bash
cd "$(dirname "$0")"
echo "Testing ApiGateway..."
./mvnw test
