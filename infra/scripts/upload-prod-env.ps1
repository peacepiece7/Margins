param(
  [string] $DeployEnvPath = ".env",
  [string] $RuntimeEnvPath = ".env.production",
  [string] $RemoteEnvPath = $(if ($env:MARGINS_REMOTE_ENV_PATH) { $env:MARGINS_REMOTE_ENV_PATH } else { "/opt/margins/.env" }),
  [switch] $NoBackup
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$loadEnv = Join-Path $scriptRoot "load-env.ps1"
& $loadEnv -EnvPath (Join-Path $repoRoot $DeployEnvPath)

$runtimeEnv = Resolve-Path -LiteralPath (Join-Path $repoRoot $RuntimeEnvPath)

foreach ($name in @("MARGINS_DEPLOY_HOST", "MARGINS_DEPLOY_USER")) {
  if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
    throw "Missing required deployment environment variable: $name"
  }
}

if ($RemoteEnvPath -notmatch "^/[A-Za-z0-9_./-]+$" -or $RemoteEnvPath -eq "/") {
  throw "RemoteEnvPath must be a safe absolute remote file path"
}

$sshOptions = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new")
if ($env:MARGINS_DEPLOY_SSH_KEY) {
  if (-not (Test-Path -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY -PathType Leaf)) {
    throw "MARGINS_DEPLOY_SSH_KEY must point to an existing private key file"
  }
  $sshOptions += @("-i", (Resolve-Path -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY).Path)
}

$target = "${env:MARGINS_DEPLOY_USER}@${env:MARGINS_DEPLOY_HOST}"
$remoteTmp = "/tmp/margins.env.$([Guid]::NewGuid().ToString("N"))"
$remoteDir = $RemoteEnvPath -replace "/[^/]+$", ""
if (-not $remoteDir -or $remoteDir -eq $RemoteEnvPath) {
  throw "RemoteEnvPath must include a directory"
}

scp @sshOptions $runtimeEnv.Path "${target}:$remoteTmp"
if ($LASTEXITCODE -ne 0) {
  throw "Runtime env upload failed"
}

$backupFlag = if ($NoBackup) { "0" } else { "1" }
$remoteCommand = @"
set -e
remote_env='$RemoteEnvPath'
remote_tmp='$remoteTmp'
remote_dir='$remoteDir'
backup_enabled='$backupFlag'
mkdir -p "`$remote_dir"
if [ "`$backup_enabled" = "1" ] && [ -f "`$remote_env" ]; then
  cp "`$remote_env" "`$remote_env.backup.`$(date +%Y%m%d%H%M%S)"
fi
mv "`$remote_tmp" "`$remote_env"
chmod 600 "`$remote_env"
printf 'runtime_env=updated\n'
printf 'runtime_env_path=%s\n' "`$remote_env"
printf 'runtime_env_keys='
sed -n 's/^\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p' "`$remote_env" | sort | paste -sd ',' -
printf '\nruntime_env_perms='
stat -c '%a %U:%G' "`$remote_env"
printf '\n'
"@

ssh @sshOptions $target $remoteCommand
if ($LASTEXITCODE -ne 0) {
  throw "Remote runtime env install failed"
}
