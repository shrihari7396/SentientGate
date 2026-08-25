# scripts/windows/deploy.ps1
# Deploy SentientGate to a Kubernetes cluster (installs KEDA if missing).
# Mirrors scripts/linux/deploy.sh.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $RepoRoot

Write-Host "============================================="
Write-Host "   SentientGate Kubernetes Deployment"
Write-Host "============================================="
Write-Host ""

Write-Host "[+] Checking Kubernetes cluster..."
kubectl cluster-info *> $null
if ($LASTEXITCODE -ne 0) { Write-Host "Kubernetes cluster is not reachable." -ForegroundColor Red; exit 1 }
Write-Host "Kubernetes cluster is available." -ForegroundColor Green
Write-Host ""

Write-Host "[+] Checking for KEDA (required for autoscaling)..."
kubectl get crd scaledobjects.keda.sh *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "KEDA not found. Installing KEDA..."
    kubectl apply --server-side -f https://github.com/kedacore/keda/releases/download/v2.15.1/keda-2.15.1.yaml
    if ($LASTEXITCODE -ne 0) { Write-Host "KEDA install failed." -ForegroundColor Red; exit 1 }
    Write-Host "Waiting for KEDA to be ready..."
    Start-Sleep -Seconds 10
    kubectl wait --for=condition=ready pod -l app=keda-operator -n keda --timeout=120s
}
else { Write-Host "KEDA is already installed." -ForegroundColor Green }
Write-Host ""

Write-Host "[+] Kubernetes manifests:"
Get-ChildItem -Path 'k8s' -Recurse -Include *.yaml, *.yml | Sort-Object FullName | ForEach-Object { $_.FullName }
Write-Host ""

$confirm = Read-Host "Deploy all Kubernetes manifests? [y/N]"
if ($confirm -notmatch '^[Yy]$') { Write-Host "Deployment cancelled."; exit 0 }

Write-Host ""
Write-Host "============================================="
Write-Host "Applying Kubernetes manifests..."
Write-Host "============================================="
Write-Host ""

kubectl apply -R -f k8s/
if ($LASTEXITCODE -ne 0) { Write-Host "kubectl apply failed." -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "Deployment completed successfully!" -ForegroundColor Green
Write-Host ""

Write-Host "[+] Current Pods:"
kubectl get pods -n sentientgate
Write-Host ""
Write-Host "[+] Current Services:"
kubectl get services -n sentientgate
Write-Host ""
Write-Host "[+] Current Deployments:"
kubectl get deployments -n sentientgate
Write-Host ""
Write-Host "============================================="
Write-Host "SentientGate is deployed!"
Write-Host "============================================="
