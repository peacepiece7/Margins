param(
  [string] $OutputDir = "infra/artifacts",
  [switch] $SkipTests
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$artifactRoot = Join-Path $repoRoot $OutputDir
$releaseDir = Join-Path $artifactRoot "margins-release"
$backendDir = Join-Path $releaseDir "back"
$frontendDir = Join-Path $releaseDir "front"

New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null

if (Test-Path -LiteralPath $releaseDir) {
  Remove-Item -LiteralPath $releaseDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $backendDir | Out-Null
New-Item -ItemType Directory -Force -Path $frontendDir | Out-Null

Push-Location $repoRoot
try {
  if ($SkipTests) {
    powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1 -Task bootJar
  } else {
    powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1
    powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1 -Task bootJar
  }
  if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed with exit code $LASTEXITCODE"
  }

  Push-Location (Join-Path $repoRoot "front")
  try {
    npm run build
    if ($LASTEXITCODE -ne 0) {
      throw "Frontend build failed with exit code $LASTEXITCODE"
    }
  }
  finally {
    Pop-Location
  }

  $jar = Get-ChildItem -LiteralPath (Join-Path $repoRoot "back/build/libs") -Filter "*.jar" |
    Where-Object { $_.Name -notmatch '-plain\.jar$' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
  if (-not $jar) {
    throw "Backend jar was not found under back/build/libs"
  }

  Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $backendDir "margins-back.jar")
  Copy-Item -LiteralPath (Join-Path $repoRoot "front/dist") -Destination $frontendDir -Recurse
  Copy-Item -LiteralPath (Join-Path $repoRoot "infra/docker/mysql-compose.yml") -Destination $releaseDir

  $manifest = @(
    "builtAt=$(Get-Date -Format o)"
    "backendJar=back/margins-back.jar"
    "frontendDist=front/dist"
    "mysqlCompose=mysql-compose.yml"
  )
  $manifest | Set-Content -LiteralPath (Join-Path $releaseDir "manifest.txt") -Encoding UTF8

  $zipPath = Join-Path $artifactRoot "margins-release.zip"
  if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
  }

  Compress-Archive -Path (Join-Path $releaseDir "*") -DestinationPath $zipPath
  Write-Output $zipPath
}
finally {
  Pop-Location
}
