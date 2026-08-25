# scripts/windows/test_local.ps1
# Generate protos, bring up infra, then run every service's test suite.
# Mirrors scripts/linux/test_local.sh. Exit code is non-zero if any suite fails.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
. (Join-Path $PSScriptRoot 'services.ps1')
Set-Location $RepoRoot

# Generate Protos — abort the whole run if codegen fails.
& (Join-Path $PSScriptRoot 'generate_protos.ps1')
if ($LASTEXITCODE -ne 0) { Write-Host "Proto generation failed. Aborting tests." -ForegroundColor Red; exit 1 }

Write-Host "Starting SentientGate Services Tests..." -ForegroundColor Cyan

# Start Infrastructure for integration tests
Write-Host "Starting Infrastructure (Postgres, Redis, Kafka, Zookeeper)..." -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot 'TOOLS')
try { docker compose up -d } finally { Pop-Location }

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

$Failed = @()

foreach ($s in $Services) {
    Write-Host "Testing $($s.Label)..." -ForegroundColor Cyan
    if (Invoke-TestService $s.Folder) {
        Write-Host "$($s.Label) tests passed!" -ForegroundColor Green
    }
    else {
        Write-Host "$($s.Label) tests failed!" -ForegroundColor Red
        $Failed += $s.Label
    }
}

if ($Failed.Count -gt 0) {
    Write-Host "Tests failed in the following services: $($Failed -join ', ')" -ForegroundColor Red
    exit 1
}
else {
    Write-Host "All services tested successfully!" -ForegroundColor Green
    exit 0
}
