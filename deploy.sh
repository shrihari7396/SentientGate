#!/bin/bash # Specifies the shell environment to run the script using bash
# Exit immediately if any command inside the script returns a non-zero status code
set -e
# Setup a handler function to capture and report errors if any command fails
trap 'echo "[-] Deployment failed at line $LINENO. Exiting."; exit 1' ERR
# Define the root workspace directory based on the absolute path of this script
ROOT_DIR="$(dirname "$(readlink -f "$0")")"
# Print a decorative banner to signal the start of the deployment sequence
echo "=============================================" # Printing top border for header
echo "Starting SentientGate Automated Deployment" # Printing title of the deployment task
echo "=============================================" # Printing bottom border for header
# Define a helper function to execute sub-deployments and print clear logs
run_deploy() { # Starting function declaration
  local service_name="$1" # Stores the first argument as service name
  local script_path="$2" # Stores the second argument as script path
  echo "[+] Deploying ${service_name}..." # Prints the deployment initialization status
  if [ -f "${script_path}" ]; then # Checks if the target script exists
    chmod +x "${script_path}" # Ensures the target script has execute permissions
    "${script_path}" # Executes the target script in place
    echo "[ok] ${service_name} deployed successfully." # Prints success status message
  else # Executes if script is not found
    echo "[-] Error: deploy.sh script not found at ${script_path}" # Prints error message
    exit 1 # Exits with error code 1
  fi # Ends the if statement
} # Ends the function declaration
# 1. Deploy Postgres database instance
run_deploy "PostgreSQL" "${ROOT_DIR}/Infrastructure/postgres/deploy/deploy.sh"
# 2. Deploy Redis cache instance
run_deploy "Redis" "${ROOT_DIR}/Infrastructure/redis/deploy/deploy.sh"
# 3. Deploy Zookeeper coordinator instance
run_deploy "Zookeeper" "${ROOT_DIR}/Infrastructure/zookeeper/deploy/deploy.sh"
# 4. Deploy Kafka message broker (depends on Zookeeper)
run_deploy "Kafka" "${ROOT_DIR}/Infrastructure/kafka/deploy/deploy.sh"
# 5. Deploy Eureka Server service registry
run_deploy "EurekaServer" "${ROOT_DIR}/EurekaServer/deploy/deploy.sh"
# 6. Deploy Logging Service (depends on Postgres, Kafka, Eureka)
run_deploy "LoggingService" "${ROOT_DIR}/LoggingService/deploy/deploy.sh"
# 7. Deploy Api Gateway (depends on Redis, Kafka, Eureka)
run_deploy "ApiGateway" "${ROOT_DIR}/ApiGateway/deploy/deploy.sh"
# 8. Deploy MCP Service (depends on Redis, Kafka, Eureka, Logging gRPC)
run_deploy "MCPService" "${ROOT_DIR}/MCPService/deploy/deploy.sh"
# 9. Deploy AI Service (depends on Eureka)
run_deploy "AIService" "${ROOT_DIR}/AIService/deploy/deploy.sh"
# 10. Deploy Dummy Service (depends on Eureka)
run_deploy "DummyService" "${ROOT_DIR}/Dummy/deploy/deploy.sh"
# 11. Deploy Sentinel UI frontend
run_deploy "SentinelUI" "${ROOT_DIR}/UI/deploy/deploy.sh"
# Print a success banner indicating that everything was executed successfully
echo "=============================================" # Printing top border for footer
echo "All SentientGate deployments completed successfully!" # Printing overall success message
echo "=============================================" # Printing bottom border for footer
