param(
  [string] $ArtifactPath = "harness/artifacts/deploy-dry-run/margins-release.zip"
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$resolvedArtifactPath = Join-Path $repoRoot $ArtifactPath
$artifactDir = Split-Path -Parent $resolvedArtifactPath

function Get-PowerShellExecutable {
  $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
  if ($pwsh) { return $pwsh.Source }

  $windowsPowerShell = Get-Command powershell -ErrorAction SilentlyContinue
  if ($windowsPowerShell) { return $windowsPowerShell.Source }

  throw "PowerShell executable not found. On macOS, use the Node npm commands documented in README.md; this legacy script is optional."
}

function Invoke-PowerShellFile {
  param(
    [string] $Path,
    [string[]] $Arguments = @()
  )

  $powerShell = Get-PowerShellExecutable
  $powerShellArgs = @("-NoProfile")
  if ((Split-Path -Leaf $powerShell) -ieq "powershell.exe" -or (Split-Path -Leaf $powerShell) -ieq "powershell") {
    $powerShellArgs += @("-ExecutionPolicy", "Bypass")
  }
  $powerShellArgs += @("-File", $Path)
  $powerShellArgs += $Arguments

  & $powerShell @powerShellArgs
}

New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null
if (-not (Test-Path -LiteralPath $resolvedArtifactPath)) {
  Set-Content -LiteralPath $resolvedArtifactPath -Value "dry-run-placeholder" -Encoding ASCII
}

$envNames = @(
  "MARGINS_DEPLOY_HOST",
  "MARGINS_DEPLOY_USER",
  "MARGINS_DEPLOY_DIR",
  "MARGINS_SERVICE_MANAGER",
  "MARGINS_BACKEND_SERVICE",
  "MARGINS_FRONTEND_SERVICE",
  "MARGINS_DEPLOY_HEALTH_URL",
  "MARGINS_RELEASE_RETAIN_COUNT",
  "MARGINS_DEPLOY_SSH_KEY"
)
$previous = @{}
foreach ($name in $envNames) {
  $previous[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
}

try {
  $env:MARGINS_DEPLOY_HOST = "dry-run.local"
  $env:MARGINS_DEPLOY_USER = "margins"
  $env:MARGINS_DEPLOY_DIR = "/opt/margins"
  $env:MARGINS_SERVICE_MANAGER = "systemd"
  $env:MARGINS_BACKEND_SERVICE = "margins-back"
  $env:MARGINS_FRONTEND_SERVICE = "margins-front"
  $env:MARGINS_DEPLOY_HEALTH_URL = "https://dry-run.local/api/health"
  $env:MARGINS_DEPLOY_SSH_KEY = Join-Path $repoRoot "harness\artifacts\deploy-dry-run\dry-run-key"
  Set-Content -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY -Value "dry-run-placeholder-key" -Encoding ASCII

  $output = Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
    "-EnvPath", ".env.does-not-exist",
    "-ArtifactPath", $ArtifactPath,
    "-SmokeAttempts", "3",
    "-SmokeDelaySeconds", "1",
    "-ReleaseRetainCount", "4",
    "-DryRun"
  )

  if ($LASTEXITCODE -ne 0) {
    throw "Deploy dry-run failed with exit code $LASTEXITCODE"
  }

  $text = $output -join "`n"
  if ($text.Contains($env:MARGINS_DEPLOY_HEALTH_URL)) {
    throw "Deploy dry-run output leaked the configured smoke health URL"
  }
  if ($text.Contains($env:MARGINS_DEPLOY_SSH_KEY)) {
    throw "Deploy dry-run output leaked the configured SSH key path"
  }

  foreach ($needle in @(
    "Dry run passed.",
    "Artifact:",
    "Target: margins@dry-run.local",
    "SSH key: configured",
    "Remote zip: /opt/margins/margins-release.zip",
    "Service manager: systemd",
    "Backend service: margins-back",
    "Frontend service: margins-front",
    "Release retain count: 4",
    "Smoke health URL: configured",
    "Smoke attempts: 3",
    "Smoke delay seconds: 1",
    "Remote command:",
    "set -e",
    "cd '/opt/margins'",
    'release_id=$(date +%Y%m%d%H%M%S)',
    'release_dir="releases/$release_id"',
    "release_retain_count=4",
    "mkdir -p releases",
    'rm -rf "$release_dir"',
    'mkdir -p "$release_dir"',
    'unzip -oq margins-release.zip -d "$release_dir"',
    "if [ -e current ] && [ ! -L current ]; then",
    'legacy_dir="releases/legacy-$release_id"',
    'mv current "$legacy_dir"',
    'ln -sfn "$release_dir" current',
    "sudo systemctl restart 'margins-back'",
    "sudo systemctl restart 'margins-front'",
    "find releases -mindepth 1 -maxdepth 1 -type d | sort -r | awk -v retain=",
    'rm -rf "$old_release"'
  )) {
    if (-not $text.Contains($needle)) {
      throw "Deploy dry-run output missing required text: $needle"
    }
  }

  $rollbackOutput = Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
    "-EnvPath", ".env.does-not-exist",
    "-Rollback",
    "-DryRun"
  )

  if ($LASTEXITCODE -ne 0) {
    throw "Rollback dry-run failed with exit code $LASTEXITCODE"
  }

  $rollbackText = $rollbackOutput -join "`n"
  if ($rollbackText.Contains($env:MARGINS_DEPLOY_HEALTH_URL)) {
    throw "Rollback dry-run output leaked the configured smoke health URL"
  }
  if ($rollbackText.Contains($env:MARGINS_DEPLOY_SSH_KEY)) {
    throw "Rollback dry-run output leaked the configured SSH key path"
  }

  if ($rollbackText.Contains("Artifact:") -or $rollbackText.Contains("Remote zip:")) {
    throw "Rollback dry-run should not require or print artifact transfer details"
  }

  foreach ($needle in @(
    "Dry run passed.",
    "Mode: rollback",
    "Target: margins@dry-run.local",
    "SSH key: configured",
    "Service manager: systemd",
    "Backend service: margins-back",
    "Frontend service: margins-front",
    "Rollback release: previous",
    "Remote command:",
    "set -e",
    "cd '/opt/margins'",
    'current_target=$(readlink current || true)',
    'rollback_dir=$(find releases -mindepth 1 -maxdepth 1 -type d | sort -r | awk -v current="$current_target"',
    'test -n "$rollback_dir"',
    'test -d "$rollback_dir"',
    "if [ -e current ] && [ ! -L current ]; then",
    'legacy_dir="releases/legacy-$(date +%Y%m%d%H%M%S)"',
    'mv current "$legacy_dir"',
    'ln -sfn "$rollback_dir" current',
    "sudo systemctl restart 'margins-back'",
    "sudo systemctl restart 'margins-front'"
  )) {
    if (-not $rollbackText.Contains($needle)) {
      throw "Rollback dry-run output missing required text: $needle"
    }
  }

  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $invalidRollbackOutput = Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
      "-EnvPath", ".env.does-not-exist",
      "-Rollback",
      "-RollbackReleaseId", "bad",
      "-DryRun"
    ) 2>&1
  }
  finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  if ($LASTEXITCODE -eq 0) {
    throw "Rollback dry-run accepted an invalid RollbackReleaseId"
  }

  if (-not (($invalidRollbackOutput | Out-String).Contains("RollbackReleaseId must be a 14 digit release timestamp"))) {
    throw "Rollback dry-run invalid id failure did not report the expected validation text"
  }

  $env:MARGINS_DEPLOY_DIR = "/"
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $invalidDirOutput = Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
      "-EnvPath", ".env.does-not-exist",
      "-DryRun"
    ) 2>&1
  }
  finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  if ($LASTEXITCODE -eq 0) {
    throw "Deploy dry-run accepted an unsafe MARGINS_DEPLOY_DIR"
  }

  if (-not (($invalidDirOutput | Out-String).Contains("MARGINS_DEPLOY_DIR must be an absolute remote path"))) {
    throw "Deploy dry-run invalid deploy dir failure did not report the expected validation text"
  }

  $env:MARGINS_DEPLOY_DIR = "/opt/margins"
  $env:MARGINS_BACKEND_SERVICE = "bad'name"
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $invalidServiceOutput = Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
      "-EnvPath", ".env.does-not-exist",
      "-ArtifactPath", $ArtifactPath,
      "-DryRun"
    ) 2>&1
  }
  finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  if ($LASTEXITCODE -eq 0) {
    throw "Deploy dry-run accepted an unsafe MARGINS_BACKEND_SERVICE"
  }

  if (-not (($invalidServiceOutput | Out-String).Contains("MARGINS_BACKEND_SERVICE must be a systemd service name"))) {
    throw "Deploy dry-run invalid backend service failure did not report the expected validation text"
  }

  $env:MARGINS_BACKEND_SERVICE = "margins-back"
  $env:MARGINS_DEPLOY_SSH_KEY = Join-Path $repoRoot "harness\artifacts\deploy-dry-run\missing-key"
  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $invalidSshKeyOutput = Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
      "-EnvPath", ".env.does-not-exist",
      "-ArtifactPath", $ArtifactPath,
      "-DryRun"
    ) 2>&1
  }
  finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  if ($LASTEXITCODE -eq 0) {
    throw "Deploy dry-run accepted a missing MARGINS_DEPLOY_SSH_KEY file"
  }

  if (-not (($invalidSshKeyOutput | Out-String).Contains("MARGINS_DEPLOY_SSH_KEY must point to an existing private key file"))) {
    throw "Deploy dry-run missing SSH key failure did not report the expected validation text"
  }

  Write-Output "# Deploy Dry-Run Audit"
  Write-Output ""
  Write-Output "Artifact checked: $ArtifactPath"
  Write-Output "Service manager checked: systemd"
  Write-Output "Smoke health checked: configured without printing URL"
  Write-Output "Remote command checked: unzip and backend/frontend restart"
  Write-Output "Release directory checked: new release path and current symlink switch"
  Write-Output "Legacy current checked: existing current directory is preserved before symlink switch"
  Write-Output "Release retention checked: old release cleanup keeps configured count"
  Write-Output "Rollback checked: previous release symlink switch without artifact transfer"
  Write-Output "Rollback validation checked: release id must be a 14 digit timestamp"
  Write-Output "Deploy input validation checked: unsafe deploy directory and service name are rejected"
  Write-Output "SSH key validation checked: configured key path must exist and dry-run output does not print it"
  Write-Output ""
  Write-Output "PASS: Raspberry Pi deploy dry-run output contract is consistent."
}
finally {
  foreach ($name in $envNames) {
    [Environment]::SetEnvironmentVariable($name, $previous[$name], "Process")
  }
}

exit 0
