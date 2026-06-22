#!/bin/bash # Shell execution directive using bash shell interpreter
# Exit immediately if any command in the script returns a non-zero exit status
set -e
# Retrieve the directory path of the active script to ensure relative paths resolve correctly
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
# Apply the Deployment and Service configurations to run and expose the UI containers
kubectl apply -f "${SCRIPT_DIR}/deployment.yml"
# Apply the HorizontalPodAutoscaler to enable dynamic autoscaling for the UI service
kubectl apply -f "${SCRIPT_DIR}/hpa.yml"
