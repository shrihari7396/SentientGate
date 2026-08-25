# scripts/windows/generate_protos.ps1
# Regenerate the gRPC/protobuf sources for LoggingService and MCPService via the
# Maven protobuf plugin. Mirrors scripts/linux/generate_protos.sh.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Write-Host "Generating Protocol Buffer files..." -ForegroundColor Cyan

foreach ($svc in @('LoggingService', 'MCPService')) {
    Write-Host "-> Generating protos for $svc..." -ForegroundColor Cyan
    Push-Location (Join-Path $RepoRoot $svc)
    try {
        & '.\mvnw.cmd' -q protobuf:compile protobuf:compile-custom
        $code = $LASTEXITCODE
    }
    finally { Pop-Location }
    if ($code -ne 0) { Write-Host "Failed to generate protos for $svc!" -ForegroundColor Red; exit 1 }
    Write-Host "$svc protos generated successfully!" -ForegroundColor Green
}

Write-Host "All proto generation completed successfully!" -ForegroundColor Green
exit 0
