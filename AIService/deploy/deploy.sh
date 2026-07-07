#!/bin/bash # Shell execution directive using bash shell interpreter
# Exit immediately if any command in the script returns a non-zero exit status
set -e
# Retrieve the directory path of the active script to ensure relative paths resolve correctly
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
# Apply the ConfigMap configurations to initialize AI service settings in the cluster
kubectl apply -f "${SCRIPT_DIR}/config.yml"
# Apply the Deployment and Service configurations to run and expose the AI Service container
kubectl apply -f "${SCRIPT_DIR}/deployment.yml"
# Apply the HorizontalPodAutoscaler to enable dynamic autoscaling for the AI Service
kubectl apply -f "${SCRIPT_DIR}/hpa.yml"
