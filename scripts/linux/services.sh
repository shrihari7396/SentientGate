#!/bin/bash
# scripts/<os>/services.sh
#
# Single source of truth for the SentientGate service set and how each service
# is run and tested locally. Sourced by run_local.sh and test_local.sh so the
# per-service invocation lives in exactly one place.
#
# SERVICES lists every service as "FOLDER:LABEL":
#   FOLDER - path to the service directory, relative to the repo root
#   LABEL  - friendly name used for log filenames and test output
#
# The first entry (EurekaServer) is the service registry and must start before
# the others; run_local.sh relies on this ordering.
#
# Callers cd into the repo root before invoking run_service/test_service, so the
# FOLDER paths below resolve relative to that root.
SERVICES=(
    "EurekaServer:EurekaServer"
    "ApiGateway:ApiGateway"
    "LoggingService:LoggingService"
    "MCPService:MCPService"
    "AIService:AIService"
    "Dummy:Dummy"
    "UI/sentinel-gateway-ui:SentinelUI"
)

# run_service <folder>
# Start one service in the foreground. Callers background it and redirect output.
# Runs in a subshell so the `cd` never leaks into the caller's working directory.
run_service() {
    local folder="$1"
    (
        cd "$folder" || exit 1
        case "$folder" in
            UI/*)
                npm install && npm run dev
                ;;
            Dummy)
                # Aggregator reactor: run the one bootable module, building its deps first.
                ./mvnw -pl dummy-service -am spring-boot:run -DskipTests
                ;;
            *)
                ./mvnw spring-boot:run -DskipTests
                ;;
        esac
    )
}

# test_service <folder>
# Run one service's test suite in the foreground; exit status reflects the suite.
# Runs in a subshell so consecutive calls in a loop keep a stable working directory.
test_service() {
    local folder="$1"
    (
        cd "$folder" || exit 1
        case "$folder" in
            UI/*)
                npm install && npm test
                ;;
            *)
                ./mvnw test
                ;;
        esac
    )
}
