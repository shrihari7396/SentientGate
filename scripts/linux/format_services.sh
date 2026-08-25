#!/bin/bash
# scripts/<os>/format_services.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "Formatting all services with Spotless..."

# Use each service's own Maven wrapper so no system-wide `mvn` install is required.
format_service() {
    local svc="$1"
    echo "----------------------------------------"
    echo "Formatting: $svc"
    echo "----------------------------------------"
    ( cd "$ROOT_DIR/$svc" && ./mvnw -q spotless:apply )
}

for svc in AIService ApiGateway Dummy EurekaServer LoggingService MCPService; do
    format_service "$svc"
done

echo "----------------------------------------"
echo "All services formatted!"
echo "----------------------------------------"
