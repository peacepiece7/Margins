param(
  [string] $GradleVersion = "8.10.2",
  [string] $Task = "test"
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backRoot = Resolve-Path (Join-Path $scriptRoot "..")
$repoRoot = Resolve-Path (Join-Path $backRoot "..")
$loadEnv = Join-Path $repoRoot "infra\scripts\load-env.ps1"
$toolsDir = Join-Path $repoRoot ".tools"
$distributionName = "gradle-$GradleVersion"
$distributionUrl = "https://services.gradle.org/distributions/$distributionName-bin.zip"
$zipPath = Join-Path $toolsDir "$distributionName-bin.zip"
$gradleRoot = Join-Path $toolsDir $distributionName
$gradleBat = Join-Path $gradleRoot "bin\gradle.bat"

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

if (Test-Path -LiteralPath $loadEnv) {
  & $loadEnv -EnvPath (Join-Path $repoRoot ".env")
}

if (-not (Test-Path -LiteralPath $gradleBat)) {
  if (-not (Test-Path -LiteralPath $zipPath)) {
    Write-Host "Downloading $distributionUrl"
    Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath
  }

  Write-Host "Extracting $zipPath"
  Expand-Archive -LiteralPath $zipPath -DestinationPath $toolsDir -Force
}

if (-not (Test-Path -LiteralPath $gradleBat)) {
  throw "Gradle executable was not found after setup: $gradleBat"
}

Push-Location $backRoot
try {
  & $gradleBat --no-daemon $Task
  if ($LASTEXITCODE -ne 0) {
    throw "Gradle task failed with exit code $LASTEXITCODE"
  }
}
finally {
  Pop-Location
}
