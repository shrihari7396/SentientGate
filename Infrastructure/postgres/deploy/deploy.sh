#!/bin/bash # Shell execution directive using bash shell interpreter
# Exit immediately if any command in the script returns a non-zero exit status
set -e
# Retrieve the directory path of the active script to ensure relative paths resolve correctly
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"
# Apply the Secret resources to initialize database credentials in the Kubernetes cluster
kubectl apply -f "${SCRIPT_DIR}/secret.yml"
# Apply the ConfigMap configurations to initialize database configurations in the Kubernetes cluster
kubectl apply -f "${SCRIPT_DIR}/config.yml"
# Apply the PersistentVolumeClaim resource to request and provision persistent storage
kubectl apply -f "${SCRIPT_DIR}/pvc.yml"
# Apply the Deployment and Service configurations to run and expose the PostgreSQL container
kubectl apply -f "${SCRIPT_DIR}/deployment.yml"
