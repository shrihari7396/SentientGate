#!/bin/bash

set -e

trap 'echo -e "\n❌ Deployment failed at line $LINENO. Exiting."; exit 1' ERR

echo "============================================="
echo "   🚀 SentientGate Kubernetes Deployment"
echo "============================================="
echo

echo "[+] Moving to project root..."
cd ..

echo "[+] Current directory:"
pwd
echo

echo "[+] Checking Kubernetes cluster..."

if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Kubernetes cluster is not reachable."
    exit 1
fi

echo "✅ Kubernetes cluster is available."
echo

echo "[+] Checking for KEDA (required for autoscaling)..."
if ! kubectl get crd scaledobjects.keda.sh >/dev/null 2>&1; then
    echo "📦 KEDA not found. Installing KEDA..."
    kubectl apply --server-side -f https://github.com/kedacore/keda/releases/download/v2.15.1/keda-2.15.1.yaml
    echo "⏳ Waiting for KEDA to be ready..."
    sleep 10
    kubectl wait --for=condition=ready pod -l app=keda-operator -n keda --timeout=120s
else
    echo "✅ KEDA is already installed."
fi
echo

echo "[+] Kubernetes manifests:"
find k8s/ -type f \( -name "*.yaml" -o -name "*.yml" \) | sort
echo

read -r -p "Deploy all Kubernetes manifests? [y/N]: " CONFIRM

if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo "🚫 Deployment cancelled."
    exit 0
fi

echo
echo "============================================="
echo "📦 Applying Kubernetes manifests..."
echo "============================================="
echo

kubectl apply -R -f k8s/

echo
echo "============================================="
echo "✅ Deployment completed successfully!"
echo "============================================="
echo

echo "[+] Current Pods:"
kubectl get pods -n sentientgate

echo
echo "[+] Current Services:"
kubectl get services -n sentientgate

echo
echo "[+] Current Deployments:"
kubectl get deployments -n sentientgate

echo
echo "============================================="
echo "🎉 SentientGate is deployed!"
echo "============================================="