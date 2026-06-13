param(
  [string] $EnvPath = ".env",
  [string] $ArtifactPath = "infra/artifacts/margins-release.zip",
  [string] $SmokeHealthUrl = $env:MARGINS_DEPLOY_HEALTH_URL,
  [int] $SmokeAttempts = 12,
  [int] $SmokeDelaySeconds = 5,
  [int] $ReleaseRetainCount = -1,
  [switch] $Rollback,
  [string] $RollbackReleaseId = "",
  [switch] $DryRun,
  [switch] $SshPreflight
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$loadEnv = Join-Path $scriptRoot "load-env.ps1"
& $loadEnv -EnvPath (Join-Path $repoRoot $EnvPath)

if (-not $SmokeHealthUrl -and $env:MARGINS_DEPLOY_HEALTH_URL) {
  $SmokeHealthUrl = $env:MARGINS_DEPLOY_HEALTH_URL
}

foreach ($name in @("MARGINS_DEPLOY_HOST", "MARGINS_DEPLOY_USER", "MARGINS_DEPLOY_DIR", "MARGINS_SERVICE_MANAGER")) {
  if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
    throw "Missing required environment variable: $name"
  }
}

function Assert-SafeDeployValue {
  param(
    [string] $Name,
    [string] $Value,
    [string] $Pattern,
    [string] $Description
  )

  if ($Value -notmatch $Pattern) {
    throw "$Name must be $Description"
  }
}

Assert-SafeDeployValue -Name "MARGINS_DEPLOY_HOST" -Value $env:MARGINS_DEPLOY_HOST -Pattern "^[A-Za-z0-9_.-]+$" -Description "a hostname or IP containing only letters, numbers, dot, underscore, or hyphen"
Assert-SafeDeployValue -Name "MARGINS_DEPLOY_USER" -Value $env:MARGINS_DEPLOY_USER -Pattern "^[A-Za-z0-9_.-]+$" -Description "an SSH username containing only letters, numbers, dot, underscore, or hyphen"
Assert-SafeDeployValue -Name "MARGINS_DEPLOY_DIR" -Value $env:MARGINS_DEPLOY_DIR -Pattern "^/[A-Za-z0-9_./-]+$" -Description "an absolute remote path containing only letters, numbers, slash, dot, underscore, or hyphen"

if ($env:MARGINS_DEPLOY_DIR -eq "/") {
  throw "MARGINS_DEPLOY_DIR must not be the filesystem root"
}

if ($env:MARGINS_SERVICE_MANAGER -notin @("systemd", "manual", "artifact")) {
  throw "MARGINS_SERVICE_MANAGER must be one of: systemd, manual, artifact"
}

if ($SmokeAttempts -lt 1) {
  throw "SmokeAttempts must be at least 1"
}

if ($SmokeDelaySeconds -lt 0) {
  throw "SmokeDelaySeconds must be zero or greater"
}

if ($ReleaseRetainCount -lt 0) {
  $ReleaseRetainCount = if ($env:MARGINS_RELEASE_RETAIN_COUNT) { [int] $env:MARGINS_RELEASE_RETAIN_COUNT } else { 5 }
}

if ($ReleaseRetainCount -lt 1) {
  throw "ReleaseRetainCount must be at least 1"
}

if ($RollbackReleaseId -and $RollbackReleaseId -notmatch "^\d{14}$") {
  throw "RollbackReleaseId must be a 14 digit release timestamp such as 20260612091530"
}

if (-not $Rollback) {
  $artifact = Resolve-Path -LiteralPath (Join-Path $repoRoot $ArtifactPath)
}

$target = "$env:MARGINS_DEPLOY_USER@$env:MARGINS_DEPLOY_HOST"
$remoteZip = "$env:MARGINS_DEPLOY_DIR/margins-release.zip"
$backendService = if ($env:MARGINS_BACKEND_SERVICE) { $env:MARGINS_BACKEND_SERVICE } else { "margins-back" }
$frontendService = $env:MARGINS_FRONTEND_SERVICE
$sshKeyPath = $null

Assert-SafeDeployValue -Name "MARGINS_BACKEND_SERVICE" -Value $backendService -Pattern "^[A-Za-z0-9_.@-]+$" -Description "a systemd service name containing only letters, numbers, dot, underscore, at sign, or hyphen"
if ($frontendService) {
  Assert-SafeDeployValue -Name "MARGINS_FRONTEND_SERVICE" -Value $frontendService -Pattern "^[A-Za-z0-9_.@-]+$" -Description "a systemd service name containing only letters, numbers, dot, underscore, at sign, or hyphen"
}

if ($env:MARGINS_DEPLOY_SSH_KEY) {
  if (-not (Test-Path -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY -PathType Leaf)) {
    throw "MARGINS_DEPLOY_SSH_KEY must point to an existing private key file"
  }

  $sshKeyPath = (Resolve-Path -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY).Path
}

$sshOptions = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new")
if ($sshKeyPath) {
  $sshOptions += @("-i", $sshKeyPath)
}

$restartCommand = if ($env:MARGINS_SERVICE_MANAGER -eq "systemd") {
  $commands = @("sudo systemctl restart '$backendService'")
  if ($frontendService) {
    $commands += "sudo systemctl restart '$frontendService'"
  }
  $commands -join "`n"
} elseif ($env:MARGINS_SERVICE_MANAGER -eq "manual") {
  "printf 'manual-service-manager'"
} else {
  "printf 'artifact-deployed'"
}

$remoteCommand = @"
set -e
cd '$env:MARGINS_DEPLOY_DIR'
release_id=`$(date +%Y%m%d%H%M%S)
release_dir="releases/`$release_id"
release_retain_count=$ReleaseRetainCount
mkdir -p releases
rm -rf "`$release_dir"
mkdir -p "`$release_dir"
unzip -oq margins-release.zip -d "`$release_dir"
if [ -e current ] && [ ! -L current ]; then
  legacy_dir="releases/legacy-`$release_id"
  rm -rf "`$legacy_dir"
  mv current "`$legacy_dir"
fi
ln -sfn "`$release_dir" current
$restartCommand
find releases -mindepth 1 -maxdepth 1 -type d | sort -r | awk -v retain="`$release_retain_count" 'NR > retain' | while IFS= read -r old_release; do
  rm -rf "`$old_release"
done
"@

$rollbackCommand = if ($RollbackReleaseId) {
@"
set -e
cd '$env:MARGINS_DEPLOY_DIR'
rollback_dir="releases/$RollbackReleaseId"
test -d "`$rollback_dir"
if [ -e current ] && [ ! -L current ]; then
  legacy_dir="releases/legacy-`$(date +%Y%m%d%H%M%S)"
  rm -rf "`$legacy_dir"
  mv current "`$legacy_dir"
fi
ln -sfn "`$rollback_dir" current
$restartCommand
"@
} else {
@"
set -e
cd '$env:MARGINS_DEPLOY_DIR'
current_target=`$(readlink current || true)
rollback_dir=`$(find releases -mindepth 1 -maxdepth 1 -type d | sort -r | awk -v current="`$current_target" '`$0 != current { print; exit }')
test -n "`$rollback_dir"
test -d "`$rollback_dir"
if [ -e current ] && [ ! -L current ]; then
  legacy_dir="releases/legacy-`$(date +%Y%m%d%H%M%S)"
  rm -rf "`$legacy_dir"
  mv current "`$legacy_dir"
fi
ln -sfn "`$rollback_dir" current
$restartCommand
"@
}

$remoteCommandToRun = if ($Rollback) { $rollbackCommand } else { $remoteCommand }

if ($DryRun) {
  Write-Output "Dry run passed."
  Write-Output "Mode: $(if ($Rollback) { "rollback" } else { "deploy" })"
  if (-not $Rollback) {
    Write-Output "Artifact: $($artifact.Path)"
  }
  Write-Output "Target: $target"
  Write-Output "SSH key: $(if ($sshKeyPath) { "configured" } else { "default agent or SSH config" })"
  if (-not $Rollback) {
    Write-Output "Remote zip: $remoteZip"
  }
  Write-Output "Service manager: $env:MARGINS_SERVICE_MANAGER"
  Write-Output "Backend service: $backendService"
  if ($Rollback) {
    Write-Output "Rollback release: $(if ($RollbackReleaseId) { $RollbackReleaseId } else { "previous" })"
  } else {
    Write-Output "Release retain count: $ReleaseRetainCount"
  }
  if ($frontendService) {
    Write-Output "Frontend service: $frontendService"
  }
  if ($SmokeHealthUrl) {
    Write-Output "Smoke health URL: configured"
    Write-Output "Smoke attempts: $SmokeAttempts"
    Write-Output "Smoke delay seconds: $SmokeDelaySeconds"
  } else {
    Write-Output "Smoke health URL: not configured"
  }
  Write-Output "Remote command:"
  Write-Output $remoteCommandToRun
  exit 0
}

if ($SshPreflight) {
  ssh @sshOptions $target "printf 'margins-ssh-ok'"
  if ($LASTEXITCODE -ne 0) {
    throw "SSH authentication preflight failed before artifact transfer"
  }

  Write-Output "SSH preflight passed for Raspberry Pi target."
  exit 0
}

if (-not $Rollback) {
  ssh @sshOptions $target "mkdir -p '$env:MARGINS_DEPLOY_DIR'"
  if ($LASTEXITCODE -ne 0) {
    throw "SSH connection or remote directory preparation failed"
  }

  scp @sshOptions $artifact.Path "${target}:$remoteZip"
  if ($LASTEXITCODE -ne 0) {
    throw "Artifact transfer failed"
  }
}

ssh @sshOptions $target $remoteCommandToRun
if ($LASTEXITCODE -ne 0) {
  throw "$(if ($Rollback) { "Remote rollback command failed" } else { "Remote deploy command failed" })"
}

if ($SmokeHealthUrl) {
  $smokePassed = $false
  $lastSmokeError = ""
  for ($attempt = 1; $attempt -le $SmokeAttempts; $attempt++) {
    try {
      $response = Invoke-WebRequest -Uri $SmokeHealthUrl -UseBasicParsing -TimeoutSec 10
      if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
        $smokePassed = $true
        break
      }
      $lastSmokeError = "HTTP status $($response.StatusCode)"
    } catch {
      $lastSmokeError = $_.Exception.GetType().Name
    }

    if ($attempt -lt $SmokeAttempts -and $SmokeDelaySeconds -gt 0) {
      Start-Sleep -Seconds $SmokeDelaySeconds
    }
  }

  if (-not $smokePassed) {
    throw "Deploy smoke failed for configured health URL after $SmokeAttempts attempt(s): $lastSmokeError"
  }

  Write-Output "Deploy smoke passed for configured health URL."
}

if ($Rollback) {
  Write-Output "Rolled back Raspberry Pi target to configured release."
} else {
  Write-Output "Deployed artifact to Raspberry Pi target."
}
