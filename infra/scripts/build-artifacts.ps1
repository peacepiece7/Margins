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
$runtimeDir = Join-Path $releaseDir "runtime"
$systemdDir = Join-Path $runtimeDir "systemd"
$nginxDir = Join-Path $runtimeDir "nginx"

New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null

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

if (Test-Path -LiteralPath $releaseDir) {
  Remove-Item -LiteralPath $releaseDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $backendDir | Out-Null
New-Item -ItemType Directory -Force -Path $frontendDir | Out-Null
New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
New-Item -ItemType Directory -Force -Path $systemdDir | Out-Null
New-Item -ItemType Directory -Force -Path $nginxDir | Out-Null

Push-Location $repoRoot
try {
  if ($SkipTests) {
    Invoke-PowerShellFile -Path "back/scripts/test.ps1" -Arguments @("-Task", "bootJar")
  } else {
    Invoke-PowerShellFile -Path "back/scripts/test.ps1"
    Invoke-PowerShellFile -Path "back/scripts/test.ps1" -Arguments @("-Task", "bootJar")
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

  @(
    "# Margins Release"
    ""
    "## Contents"
    ""
    "- back/margins-back.jar"
    "- front/dist/"
    "- mysql-compose.yml"
    "- runtime/env.example"
    "- runtime/systemd/margins-back.service.example"
    "- runtime/nginx/margins.conf.example"
    "- manifest.txt"
    "- checksums.sha256"
    ""
    "## Backend"
    ""
    "Copy runtime/env.example to /opt/margins/.env, fill real values outside the release directory, then run:"
    ""
    '```powershell'
    "java -jar back/margins-back.jar"
    '```'
    ""
    "For systemd, adapt runtime/systemd/margins-back.service.example and set EnvironmentFile to the secret-bearing env file outside the release artifact."
    ""
    "## Raspberry Pi Layout"
    ""
    "deploy-raspberry-pi.ps1 extracts each zip into /opt/margins/releases/<timestamp> and then switches /opt/margins/current to that release. Keep service units and nginx roots pointed at /opt/margins/current."
    ""
    "If /opt/margins/current already exists as a real directory, the deploy script preserves it under /opt/margins/releases/legacy-<timestamp> before creating the current symlink."
    ""
    "The deploy script keeps five release directories by default. Set MARGINS_RELEASE_RETAIN_COUNT or pass -ReleaseRetainCount to retain a different number."
    ""
    "To roll back without transferring a new artifact, run deploy-raspberry-pi.ps1 -Rollback. By default it selects the newest retained release that is not current; pass -RollbackReleaseId <timestamp> to choose a specific retained release."
    ""
    "## Frontend"
    ""
    "Serve front/dist with the Raspberry Pi's chosen static file server or reverse proxy. runtime/nginx/margins.conf.example is a starting point for nginx."
    ""
    "## Integrity"
    ""
    "Run infra/scripts/verify-artifacts.ps1 against margins-release.zip before transfer or after download."
  ) | Set-Content -LiteralPath (Join-Path $releaseDir "README.md") -Encoding UTF8

  @(
    "MARGINS_DB_URL=jdbc:mysql://localhost:3306/margins?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    "MARGINS_MYSQL_USER=margins"
    "MARGINS_MYSQL_PASSWORD=change-me"
    "MARGINS_AUTH_JWT_SECRET=change-me"
    "MARGINS_SINGLE_USER_USERNAME=reader"
    "MARGINS_SINGLE_USER_PASSWORD=change-me"
    "OPENAI_API_KEY="
    "OPENAI_MODEL=gpt-5.5"
    "MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=false"
    "MARGINS_BOOK_SEARCH_PROVIDER=kakao"
    "SPRING_PROFILES_ACTIVE=prod"
  ) | Set-Content -LiteralPath (Join-Path $runtimeDir "env.example") -Encoding UTF8

  @(
    "[Unit]"
    "Description=Margins backend"
    "After=network-online.target"
    "Wants=network-online.target"
    ""
    "[Service]"
    "Type=simple"
    "User=margins"
    "Group=margins"
    "WorkingDirectory=/opt/margins/current"
    "EnvironmentFile=/opt/margins/.env"
    "ExecStart=/usr/bin/java -jar /opt/margins/current/back/margins-back.jar"
    "Restart=on-failure"
    "RestartSec=5"
    ""
    "[Install]"
    "WantedBy=multi-user.target"
  ) | Set-Content -LiteralPath (Join-Path $systemdDir "margins-back.service.example") -Encoding UTF8

  @(
    "server {"
    "  listen 80;"
    "  server_name _;"
    ""
    "  root /opt/margins/current/front/dist;"
    "  index index.html;"
    ""
    "  location /api/ {"
    "    proxy_pass http://127.0.0.1:8080;"
    "    proxy_set_header Host `$host;"
    "    proxy_set_header X-Real-IP `$remote_addr;"
    "    proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;"
    "    proxy_set_header X-Forwarded-Proto `$scheme;"
    "  }"
    ""
    "  location / {"
    "    try_files `$uri /index.html;"
    "  }"
    "}"
  ) | Set-Content -LiteralPath (Join-Path $nginxDir "margins.conf.example") -Encoding UTF8

  $manifest = @(
    "builtAt=$(Get-Date -Format o)"
    "sourceCommit=$(git -C $repoRoot rev-parse --short HEAD 2>$null)"
    "backendJar=back/margins-back.jar"
    "frontendDist=front/dist"
    "mysqlCompose=mysql-compose.yml"
    "runtimeEnv=runtime/env.example"
    "runtimeSystemd=runtime/systemd/margins-back.service.example"
    "runtimeNginx=runtime/nginx/margins.conf.example"
  )
  $manifest | Set-Content -LiteralPath (Join-Path $releaseDir "manifest.txt") -Encoding UTF8

  $releaseRootPath = (Resolve-Path -LiteralPath $releaseDir).Path.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
  $releaseRootUri = [Uri]::new($releaseRootPath)
  $checksumLines = Get-ChildItem -LiteralPath $releaseDir -Recurse -File -Force |
    Where-Object { $_.Name -ne "checksums.sha256" } |
    Sort-Object FullName |
    ForEach-Object {
      $relative = [Uri]::UnescapeDataString($releaseRootUri.MakeRelativeUri([Uri]::new($_.FullName)).ToString())
      $hash = Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
      "$($hash.Hash.ToLowerInvariant())  $($relative -replace '\\', '/')"
    }
  $checksumLines | Set-Content -LiteralPath (Join-Path $releaseDir "checksums.sha256") -Encoding ASCII

  $zipPath = Join-Path $artifactRoot "margins-release.zip"
  if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
  }

  Add-Type -AssemblyName System.IO.Compression
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zip = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
  try {
    Get-ChildItem -LiteralPath $releaseDir -Recurse -File -Force |
      Sort-Object FullName |
      ForEach-Object {
        $relative = [Uri]::UnescapeDataString($releaseRootUri.MakeRelativeUri([Uri]::new($_.FullName)).ToString()) -replace '\\', '/'
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
          $zip,
          $_.FullName,
          $relative,
          [System.IO.Compression.CompressionLevel]::Optimal
        ) | Out-Null
      }
  }
  finally {
    $zip.Dispose()
  }

  Write-Output $zipPath
}
finally {
  Pop-Location
}
