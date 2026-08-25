# scripts/windows/format_services.ps1
# Apply Spotless formatting to every Maven service. Mirrors
# scripts/linux/format_services.sh (uses each service's own Maven wrapper).

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Write-Host "Formatting all services with Spotless..."

foreach ($svc in @('AIService', 'ApiGateway', 'Dummy', 'EurekaServer', 'LoggingService', 'MCPService')) {
    Write-Host ('-' * 40)
    Write-Host "Formatting: $svc"
    Write-Host ('-' * 40)
    Push-Location (Join-Path $RepoRoot $svc)
    try {
        & '.\mvnw.cmd' -q spotless:apply
        $code = $LASTEXITCODE
    }
    finally { Pop-Location }
    if ($code -ne 0) { Write-Host "Spotless failed for $svc!" -ForegroundColor Red; exit 1 }
}

Write-Host ('-' * 40)
Write-Host "All services formatted!"
Write-Host ('-' * 40)
