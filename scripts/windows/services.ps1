# scripts/windows/services.ps1
#
# Single source of truth for the SentientGate service set and how each service
# is run and tested locally on Windows. Dot-sourced by run_local.ps1 and
# test_local.ps1 so the per-service invocation lives in exactly one place.
#
# $Services lists every service as an object with:
#   Folder - path to the service directory, relative to the repo root
#   Label  - friendly name used for log filenames and test output
#
# The first entry (EurekaServer) is the service registry and must start before
# the others; run_local.ps1 relies on this ordering.

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

$Services = @(
    @{ Folder = 'EurekaServer';            Label = 'EurekaServer' }
    @{ Folder = 'ApiGateway';              Label = 'ApiGateway' }
    @{ Folder = 'LoggingService';          Label = 'LoggingService' }
    @{ Folder = 'MCPService';              Label = 'MCPService' }
    @{ Folder = 'AIService';               Label = 'AIService' }
    @{ Folder = 'services';                Label = 'services' }
    @{ Folder = 'UI/sentinel-gateway-ui';  Label = 'SentinelUI' }
)

# Invoke-RunService <folder>
# Start one service in the FOREGROUND (blocks). run_local.ps1 launches this in a
# hidden child PowerShell and redirects its output to a per-service log file.
function Invoke-RunService {
    param([Parameter(Mandatory)][string]$Folder)
    Push-Location (Join-Path $RepoRoot $Folder)
    try {
        switch -Wildcard ($Folder) {
            'UI/*'  { npm install; npm run dev; break }
            'services' { & '.\mvnw.cmd' -pl dummy-service -am spring-boot:run -DskipTests; break }
            default { & '.\mvnw.cmd' spring-boot:run -DskipTests; break }
        }
    }
    finally { Pop-Location }
}

# Invoke-TestService <folder>
# Run one service's test suite in the foreground; returns $true if it passed.
# `| Out-Host` streams the tool output to the console without leaking it into
# the function's return value (only the trailing boolean is returned).
function Invoke-TestService {
    param([Parameter(Mandatory)][string]$Folder)
    Push-Location (Join-Path $RepoRoot $Folder)
    try {
        if ($Folder -like 'UI/*') {
            npm install | Out-Host
            if ($LASTEXITCODE -ne 0) { return $false }
            npm test | Out-Host
            return ($LASTEXITCODE -eq 0)
        }
        else {
            & '.\mvnw.cmd' test | Out-Host
            return ($LASTEXITCODE -eq 0)
        }
    }
    finally { Pop-Location }
}
