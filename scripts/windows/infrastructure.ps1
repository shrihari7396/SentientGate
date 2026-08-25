# scripts/windows/infrastructure.ps1
# Bring up the local infrastructure stack (Postgres, Redis, Kafka, Kafka-UI, ...)
# defined in TOOLS/docker-compose.yml. Mirrors scripts/linux/infrastructure.sh.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Push-Location (Join-Path $RepoRoot 'TOOLS')
try {
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { Write-Host "docker compose up failed." -ForegroundColor Red; exit 1 }
}
finally { Pop-Location }
