param(
  [string] $EnvPath = ".env",
  [string] $ArtifactPath = "infra/artifacts/margins-release.zip"
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$loadEnv = Join-Path $scriptRoot "load-env.ps1"
& $loadEnv -EnvPath (Join-Path $repoRoot $EnvPath)

foreach ($name in @("MARGINS_DEPLOY_HOST", "MARGINS_DEPLOY_USER", "MARGINS_DEPLOY_DIR", "MARGINS_SERVICE_MANAGER")) {
  if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
    throw "Missing required environment variable: $name"
  }
}

$artifact = Resolve-Path -LiteralPath (Join-Path $repoRoot $ArtifactPath)
$target = "$env:MARGINS_DEPLOY_USER@$env:MARGINS_DEPLOY_HOST"
$remoteZip = "$env:MARGINS_DEPLOY_DIR/margins-release.zip"
$backendService = if ($env:MARGINS_BACKEND_SERVICE) { $env:MARGINS_BACKEND_SERVICE } else { "margins-back" }
$frontendService = $env:MARGINS_FRONTEND_SERVICE

$sshOptions = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new")
if ($env:MARGINS_DEPLOY_SSH_KEY) {
  $sshOptions += @("-i", $env:MARGINS_DEPLOY_SSH_KEY)
}

ssh @sshOptions $target "mkdir -p '$env:MARGINS_DEPLOY_DIR'"
if ($LASTEXITCODE -ne 0) {
  throw "SSH connection or remote directory preparation failed"
}

scp @sshOptions $artifact.Path "${target}:$remoteZip"
if ($LASTEXITCODE -ne 0) {
  throw "Artifact transfer failed"
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
rm -rf current
mkdir -p current
unzip -oq margins-release.zip -d current
$restartCommand
"@

ssh @sshOptions $target $remoteCommand
if ($LASTEXITCODE -ne 0) {
  throw "Remote deploy command failed"
}

Write-Output "Deployed artifact to Raspberry Pi target."
