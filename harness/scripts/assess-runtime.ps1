param(
  [switch] $CheckDocker,
  [switch] $CheckMySql,
  [switch] $CheckBackendTests
)

$ErrorActionPreference = "Continue"

function Write-Result {
  param(
    [string] $Name,
    [string] $Status,
    [string] $Detail
  )

  Write-Output ("{0}: {1} - {2}" -f $Name, $Status, $Detail)
}

function Test-Command {
  param(
    [string] $Name,
    [string] $Command
  )

  $output = Invoke-Expression "$Command 2>&1"
  if ($LASTEXITCODE -eq 0) {
    Write-Result $Name "pass" (($output | Select-Object -First 1) -join " ")
    return
  }

  Write-Result $Name "fail" (($output | Select-Object -First 1) -join " ")
}

Test-Command "java" "java -version"
Test-Command "git" "git --version"

if ($CheckDocker -or $CheckMySql) {
  Test-Command "docker-cli" "docker --version"
  Test-Command "docker-compose" "docker compose version"
  Test-Command "docker-daemon" "docker info"
}

if ($CheckMySql) {
  $mysql = docker ps --filter "name=margins-mysql" --format "{{.Names}} {{.Status}} {{.Ports}}" 2>&1
  if ($LASTEXITCODE -eq 0 -and $mysql) {
    Write-Result "margins-mysql" "pass" ($mysql -join " ")
  }
  else {
    Write-Result "margins-mysql" "warn" "container is not running or Docker daemon is unavailable"
  }
}

if ($CheckBackendTests) {
  $script = "back\scripts\test.ps1"
  if (Test-Path -LiteralPath $script) {
    Write-Result "backend-test-script" "pass" $script
  }
  else {
    Write-Result "backend-test-script" "fail" "$script not found"
  }
}
