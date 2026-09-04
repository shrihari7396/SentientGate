# scripts/windows/run_local.ps1
# Start the full SentientGate stack locally on Windows (infra in Docker, services
# via the Maven wrappers / npm). Mirrors scripts/linux/run_local.sh.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$ServicesPs1 = Join-Path $PSScriptRoot 'services.ps1'
. $ServicesPs1

Set-Location $RepoRoot
$LogDir = Join-Path $RepoRoot 'logs'
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

Write-Host "Starting SentientGate Services locally (without Docker for services)..." -ForegroundColor Cyan

# Step 1: Start Infrastructure services
Write-Host "Starting Infrastructure (Postgres, Redis, Kafka, Kafka-UI)..." -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot 'TOOLS')
try { docker compose up -d postgres redis kafka kafka-ui } finally { Pop-Location }

# Step 2: Wait for Postgres to be healthy (bounded)
Write-Host "Waiting for Postgres database to be healthy..." -ForegroundColor Cyan
$attempt = 0
while ($true) {
    docker exec postgres-db pg_isready -U postgres *> $null
    $ok = ($LASTEXITCODE -eq 0)
    if (-not $ok) { docker exec sentient-postgres pg_isready -U postgres *> $null; $ok = ($LASTEXITCODE -eq 0) }
    if ($ok) { break }
    $attempt++
    if ($attempt -ge 60) { Write-Host "Postgres did not become healthy after 120s. Aborting." -ForegroundColor Red; exit 1 }
    Start-Sleep -Seconds 2
}
Write-Host "Postgres is healthy!" -ForegroundColor Green

$Procs = @()

# Launch one service in a hidden child PowerShell, logging stdout/stderr to files.
function Start-ServiceProcess {
    param([string]$Folder, [string]$Label)
    $out = Join-Path $LogDir "$Label.log"
    $err = Join-Path $LogDir "$Label.err.log"
    $cmd = ". '$ServicesPs1'; Invoke-RunService '$Folder'"
    return Start-Process -FilePath 'powershell' `
        -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $cmd) `
        -RedirectStandardOutput $out -RedirectStandardError $err `
        -WindowStyle Hidden -PassThru
}

# Step 3: Start Eureka Server first (service registry).
Write-Host "Starting Eureka Server locally..." -ForegroundColor Cyan
$Procs += Start-ServiceProcess -Folder 'EurekaServer' -Label 'EurekaServer'

# Step 4: Wait for Eureka Server port 8761 to open (bounded)
Write-Host "Waiting for Eureka Server to listen on port 8761..." -ForegroundColor Cyan
$attempt = 0
while ($true) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8761/eureka/apps' -TimeoutSec 3 *> $null
        break
    }
    catch {
        $attempt++
        if ($attempt -ge 60) {
            Write-Host "Eureka Server did not come up after 120s. Aborting." -ForegroundColor Red
            foreach ($p in $Procs) { if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue } }
            exit 1
        }
        Start-Sleep -Seconds 2
    }
}
Write-Host "Eureka Registry is up and running!" -ForegroundColor Green

# Step 5: Start all downstream microservices and the frontend.
Write-Host "Starting Microservices (ApiGateway, LoggingService, MCPServer, AIService, services, SentinelUI)..." -ForegroundColor Cyan
foreach ($s in $Services) {
    if ($s.Folder -eq 'EurekaServer') { continue }   # already started in Step 3
    $Procs += Start-ServiceProcess -Folder $s.Folder -Label $s.Label
}

Write-Host "All services started successfully in the background!" -ForegroundColor Green
Write-Host "Logs are being written to the 'logs/' directory." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop all services (the background processes)." -ForegroundColor Cyan

# Wait for the background services; on Ctrl+C (or exit) stop them all.
try {
    Wait-Process -Id ($Procs | ForEach-Object { $_.Id })
}
finally {
    Write-Host "`nStopping background services..." -ForegroundColor Cyan
    foreach ($p in $Procs) { if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue } }
}
