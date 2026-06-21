param(
  [switch] $ApplySchema,
  [int] $TimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$composeFile = Join-Path $repoRoot "infra\docker\mysql-compose.yml"
$schemaDir = Join-Path $repoRoot "db\schema"
$seedFile = Join-Path $repoRoot "db\seed\001_seed_mvp_data.sql"

$containerName = "margins-mysql"
$database = if ($env:MARGINS_MYSQL_DATABASE) { $env:MARGINS_MYSQL_DATABASE } else { "margins" }
$rootPassword = if ($env:MARGINS_MYSQL_ROOT_PASSWORD) { $env:MARGINS_MYSQL_ROOT_PASSWORD } else { "margins-root" }
$port = if ($env:MARGINS_MYSQL_PORT) { $env:MARGINS_MYSQL_PORT } else { "3306" }

docker compose -f $composeFile up -d mysql
if ($LASTEXITCODE -ne 0) {
  throw "docker compose failed with exit code $LASTEXITCODE"
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
  $health = docker inspect --format "{{.State.Health.Status}}" $containerName 2>$null
  if ($LASTEXITCODE -eq 0 -and $health -eq "healthy") {
    break
  }

  if ((Get-Date) -ge $deadline) {
    throw "Timed out waiting for $containerName to become healthy. Last health: $health"
  }

  Start-Sleep -Seconds 2
} while ($true)

Write-Host "$containerName is healthy"

if ($ApplySchema) {
  $schemaFiles = Get-ChildItem -LiteralPath $schemaDir -Filter "*.sql" | Sort-Object Name | Select-Object -ExpandProperty FullName

  foreach ($file in @($schemaFiles + $seedFile)) {
    if (-not (Test-Path -LiteralPath $file)) {
      throw "Missing SQL file: $file"
    }

    Write-Host "Applying $file"
    $containerFile = "/tmp/margins-sql/$([IO.Path]::GetFileName($file))"
    docker exec $containerName sh -c "mkdir -p /tmp/margins-sql"
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to prepare SQL directory in $containerName"
    }

    docker cp $file "${containerName}:$containerFile"
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to copy SQL file into $containerName`: $file"
    }

    docker exec `
      -e "MYSQL_PWD=$rootPassword" `
      -e "MARGINS_SQL_FILE=$containerFile" `
      -e "MARGINS_MYSQL_DATABASE=$database" `
      $containerName `
      sh -c 'mysql --user=root --default-character-set=utf8mb4 "$MARGINS_MYSQL_DATABASE" < "$MARGINS_SQL_FILE"'
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to apply $file"
    }
  }
}

Write-Host "MySQL is ready on port $port"
