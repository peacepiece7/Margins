param(
  [switch] $Volumes
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$composeFile = Join-Path $repoRoot "infra\docker\mysql-compose.yml"

$args = @("compose", "-f", $composeFile, "down")
if ($Volumes) {
  $args += "--volumes"
}

docker @args
if ($LASTEXITCODE -ne 0) {
  throw "docker compose down failed with exit code $LASTEXITCODE"
}
