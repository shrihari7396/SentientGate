# scripts/windows/build_and_push_images.ps1
#
# Usage:
#   build_and_push_images.ps1 [build|push|all]
#
#   build  - build every image locally (no push)
#   push   - tag + push every previously-built image
#   all    - build every image, then push them (default)
#
# The build phase aborts on the FIRST failure, so a service that fails to
# build can never result in a partial set of pushed images. Mirrors
# scripts/linux/build_and_push_images.sh.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $RepoRoot

$Phase = if ($args.Count -ge 1) { $args[0] } else { 'all' }

$RegistryUser = 'shrihari7396'
$Tag = 'latest'

# Format: Folder -> final image name
$Services = @(
    @{ Folder = 'EurekaServer';            Image = 'eureka-server' }
    @{ Folder = 'ApiGateway';              Image = 'api-gateway' }
    @{ Folder = 'LoggingService';          Image = 'logging-service' }
    @{ Folder = 'MCPService';              Image = 'mcp-server' }
    @{ Folder = 'AIService';               Image = 'ai-service' }
    @{ Folder = 'Dummy';                   Image = 'dummy-service' }
    @{ Folder = 'UI/sentinel-gateway-ui';  Image = 'sentinel-ui' }
)

function Invoke-BuildAll {
    Write-Host "=== Building all images ===" -ForegroundColor Cyan
    foreach ($s in $Services) {
        $localTag = "sentientgate_$($s.Image)"
        if (-not (Test-Path (Join-Path $RepoRoot $s.Folder))) { Write-Host "Folder not found: $($s.Folder)" -ForegroundColor Red; exit 1 }
        Write-Host "Building $($s.Folder) -> $localTag..." -ForegroundColor Cyan
        docker build -t $localTag $s.Folder
        if ($LASTEXITCODE -ne 0) { Write-Host "Build failed: $($s.Folder)" -ForegroundColor Red; exit 1 }
        Write-Host "Built: $localTag" -ForegroundColor Green
    }
    Write-Host "All images built successfully." -ForegroundColor Green
}

function Invoke-PushAll {
    Write-Host "=== Pushing all images ===" -ForegroundColor Cyan
    foreach ($s in $Services) {
        $localTag = "sentientgate_$($s.Image)"
        $remoteTag = "$RegistryUser/$($s.Image):$Tag"
        Write-Host "Tagging $localTag -> $remoteTag" -ForegroundColor Cyan
        docker tag $localTag $remoteTag
        if ($LASTEXITCODE -ne 0) { Write-Host "Tag failed: $localTag" -ForegroundColor Red; exit 1 }
        Write-Host "Pushing $remoteTag..." -ForegroundColor Cyan
        docker push $remoteTag
        if ($LASTEXITCODE -ne 0) { Write-Host "Push failed: $remoteTag" -ForegroundColor Red; exit 1 }
        Write-Host "Pushed: $remoteTag" -ForegroundColor Green
    }
    Write-Host "All images pushed successfully!" -ForegroundColor Green
}

Write-Host "SentientGate images - phase: $Phase" -ForegroundColor Cyan
switch ($Phase) {
    'build' { Invoke-BuildAll }
    'push'  { Invoke-PushAll }
    'all'   { Invoke-BuildAll; Invoke-PushAll }
    default { Write-Host "Unknown phase: $Phase (use: build | push | all)" -ForegroundColor Red; exit 2 }
}
