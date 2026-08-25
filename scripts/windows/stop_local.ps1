# scripts/windows/stop_local.ps1
# Stop locally-running SentientGate services and the infra stack.
# Mirrors scripts/linux/stop_local.sh (matches processes by command line).

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Write-Host "Stopping SentientGate Services locally..." -ForegroundColor Cyan

Write-Host "Stopping Microservices and UI processes..." -ForegroundColor Cyan
foreach ($pattern in @('spring-boot:run', 'vite')) {
    $procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine -like "*$pattern*" }
    foreach ($p in $procs) {
        Write-Host "Killing PID $($p.ProcessId) matching '$pattern'..."
        Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Stopping Infrastructure (Postgres, Redis, Kafka, Kafka-UI)..." -ForegroundColor Cyan
$tools = Join-Path $RepoRoot 'TOOLS'
if (Test-Path $tools) {
    if ((Test-Path (Join-Path $tools 'docker-compose.yml')) -or (Test-Path (Join-Path $tools 'docker-compose.yaml'))) {
        Push-Location $tools
        try { docker compose down } finally { Pop-Location }
    }
    else { Write-Host "docker-compose file not found in TOOLS. Skipping." -ForegroundColor Red }
}
else { Write-Host "TOOLS directory not found. Skipping docker compose down." -ForegroundColor Red }

Write-Host "All services stopped successfully!" -ForegroundColor Green
