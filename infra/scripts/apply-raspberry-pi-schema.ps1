param(
  [string] $EnvPath = ".env",
  [string] $RemoteMysqlContainer = $(if ($env:MARGINS_REMOTE_MYSQL_CONTAINER) { $env:MARGINS_REMOTE_MYSQL_CONTAINER } else { "margins-mysql" }),
  [string] $MysqlDatabase = $(if ($env:MARGINS_REMOTE_MYSQL_DATABASE) { $env:MARGINS_REMOTE_MYSQL_DATABASE } elseif ($env:MARGINS_MYSQL_DATABASE) { $env:MARGINS_MYSQL_DATABASE } else { "margins" }),
  [string] $MysqlUser = $(if ($env:MARGINS_REMOTE_MYSQL_USER) { $env:MARGINS_REMOTE_MYSQL_USER } elseif ($env:MARGINS_MYSQL_USER) { $env:MARGINS_MYSQL_USER } else { "margins" }),
  [string] $MysqlPassword = $(if ($env:MARGINS_REMOTE_MYSQL_PASSWORD) { $env:MARGINS_REMOTE_MYSQL_PASSWORD } else { $env:MARGINS_MYSQL_PASSWORD }),
  [switch] $ApplySeed
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$loadEnv = Join-Path $scriptRoot "load-env.ps1"
& $loadEnv -EnvPath (Join-Path $repoRoot $EnvPath)

if (-not $RemoteMysqlContainer -and $env:MARGINS_REMOTE_MYSQL_CONTAINER) {
  $RemoteMysqlContainer = $env:MARGINS_REMOTE_MYSQL_CONTAINER
}
if (-not $MysqlDatabase) {
  $MysqlDatabase = if ($env:MARGINS_REMOTE_MYSQL_DATABASE) { $env:MARGINS_REMOTE_MYSQL_DATABASE } elseif ($env:MARGINS_MYSQL_DATABASE) { $env:MARGINS_MYSQL_DATABASE } else { "margins" }
}
if (-not $MysqlUser) {
  $MysqlUser = if ($env:MARGINS_REMOTE_MYSQL_USER) { $env:MARGINS_REMOTE_MYSQL_USER } elseif ($env:MARGINS_MYSQL_USER) { $env:MARGINS_MYSQL_USER } else { "margins" }
}
if (-not $MysqlPassword) {
  $MysqlPassword = if ($env:MARGINS_REMOTE_MYSQL_PASSWORD) { $env:MARGINS_REMOTE_MYSQL_PASSWORD } else { $env:MARGINS_MYSQL_PASSWORD }
}

foreach ($name in @("MARGINS_DEPLOY_HOST", "MARGINS_DEPLOY_USER")) {
  if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
    throw "Missing required environment variable: $name"
  }
}

if (-not $MysqlPassword) {
  throw "Missing required environment variable: MARGINS_REMOTE_MYSQL_PASSWORD or MARGINS_MYSQL_PASSWORD"
}

function Assert-SafeShellToken {
  param(
    [string] $Name,
    [string] $Value
  )

  if ($Value -notmatch "^[A-Za-z0-9_.-]+$") {
    throw "$Name may contain only letters, numbers, dot, underscore, or hyphen"
  }
}

function Escape-SingleQuotedShellValue {
  param([string] $Value)
  return $Value.Replace("'", "'\''")
}

Assert-SafeShellToken -Name "MARGINS_DEPLOY_HOST" -Value $env:MARGINS_DEPLOY_HOST
Assert-SafeShellToken -Name "MARGINS_DEPLOY_USER" -Value $env:MARGINS_DEPLOY_USER
Assert-SafeShellToken -Name "RemoteMysqlContainer" -Value $RemoteMysqlContainer
Assert-SafeShellToken -Name "MysqlDatabase" -Value $MysqlDatabase
Assert-SafeShellToken -Name "MysqlUser" -Value $MysqlUser

$sshOptions = @("-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "-o", "StrictHostKeyChecking=accept-new")
if ($env:MARGINS_DEPLOY_SSH_KEY) {
  if (-not (Test-Path -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY -PathType Leaf)) {
    throw "MARGINS_DEPLOY_SSH_KEY must point to an existing private key file"
  }

  $sshOptions += @("-i", (Resolve-Path -LiteralPath $env:MARGINS_DEPLOY_SSH_KEY).Path)
}

$target = "$env:MARGINS_DEPLOY_USER@$env:MARGINS_DEPLOY_HOST"
$schemaDir = Join-Path $repoRoot "db\schema"
$sqlFiles = @(Get-ChildItem -LiteralPath $schemaDir -Filter "*.sql" | Sort-Object Name | Select-Object -ExpandProperty FullName)
if ($ApplySeed) {
  $sqlFiles += Join-Path $repoRoot "db\seed\001_seed_mvp_data.sql"
}

$password = Escape-SingleQuotedShellValue -Value $MysqlPassword
$remoteCommand = "docker exec -i -e MYSQL_PWD='$password' '$RemoteMysqlContainer' mysql -u '$MysqlUser' '$MysqlDatabase'"

foreach ($file in $sqlFiles) {
  if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
    throw "Missing SQL file: $file"
  }

  $relative = Resolve-Path -LiteralPath $file -Relative
  Write-Output "Applying $relative to Raspberry Pi MySQL container $RemoteMysqlContainer"
  Get-Content -LiteralPath $file -Raw | ssh @sshOptions $target $remoteCommand
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to apply $relative"
  }
}

Write-Output "Raspberry Pi schema apply completed."
