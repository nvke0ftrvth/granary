<#
.SYNOPSIS
    Rotates JWT_SECRET on Railway without forcing all users to re-log in.

.DESCRIPTION
    Moves the current JWT_SECRET into JWT_SECRET_PREVIOUS, generates a new
    JWT_SECRET, and stamps JWT_SECRET_ROTATED_AT (unix seconds, UTC) so the
    jwt-secret-cleanup GitHub Action knows when it's safe to remove
    JWT_SECRET_PREVIOUS (24h later, once old tokens have expired).

    Requires the Railway CLI (https://docs.railway.com/cli) logged in
    (`railway login`) with access to the target project.

.PARAMETER Service
    Railway service name. Defaults to "granary".

.PARAMETER RailwayEnvironment
    Railway environment name (e.g. "production"). Required.

.EXAMPLE
    ./scripts/rotate-jwt-secret.ps1 -RailwayEnvironment production
#>
param(
    [string]$Service = "granary",
    [Parameter(Mandatory = $true)][string]$RailwayEnvironment
)

$ErrorActionPreference = "Stop"

function Invoke-Railway {
    param([string[]]$Args)
    & railway @Args
    if ($LASTEXITCODE -ne 0) {
        throw "railway $($Args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$newSecret = [Convert]::ToBase64String($bytes)

$varsJson = & railway variable list --service $Service --environment $RailwayEnvironment --json
if ($LASTEXITCODE -ne 0) {
    throw "Could not read current Railway variables (exit code $LASTEXITCODE)."
}
$vars = $varsJson | ConvertFrom-Json
$currentSecret = $vars.JWT_SECRET

if ([string]::IsNullOrWhiteSpace($currentSecret)) {
    throw "Could not read the current JWT_SECRET from Railway. Aborting rotation to avoid locking everyone out."
}

$rotatedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

Write-Host "Preserving current secret as JWT_SECRET_PREVIOUS..."
Invoke-Railway @("variable", "set", "JWT_SECRET_PREVIOUS=$currentSecret", "--service", $Service, "--environment", $RailwayEnvironment, "--skip-deploys")

Write-Host "Stamping rotation time (JWT_SECRET_ROTATED_AT=$rotatedAt)..."
Invoke-Railway @("variable", "set", "JWT_SECRET_ROTATED_AT=$rotatedAt", "--service", $Service, "--environment", $RailwayEnvironment, "--skip-deploys")

Write-Host "Setting new JWT_SECRET and redeploying..."
Invoke-Railway @("variable", "set", "JWT_SECRET=$newSecret", "--service", $Service, "--environment", $RailwayEnvironment)

Write-Host ""
Write-Host "Rotation complete. Tokens signed with the old secret keep validating via JWT_SECRET_PREVIOUS."
Write-Host "The jwt-secret-cleanup GitHub Action will remove JWT_SECRET_PREVIOUS automatically ~24h from now."
