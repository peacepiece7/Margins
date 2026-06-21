param(
  [string] $ArtifactPath = "infra/artifacts/margins-release.zip"
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$artifact = Resolve-Path -LiteralPath (Join-Path $repoRoot $ArtifactPath)
$verifyRoot = Join-Path $repoRoot ("infra\artifacts\verify\" + [Guid]::NewGuid().ToString("N"))

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($artifact.Path)
try {
  foreach ($entry in $zip.Entries) {
    if ($entry.FullName.Contains("\")) {
      throw "Artifact zip entry must use forward slash separators for Linux unzip compatibility: $($entry.FullName)"
    }
  }
}
finally {
  $zip.Dispose()
}

if (Test-Path -LiteralPath $verifyRoot) {
  Remove-Item -LiteralPath $verifyRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $verifyRoot | Out-Null

try {
  Expand-Archive -LiteralPath $artifact.Path -DestinationPath $verifyRoot -Force
  $artifactFiles = @(Get-ChildItem -LiteralPath $verifyRoot -Recurse -File)
  $verifyRootPath = (Resolve-Path -LiteralPath $verifyRoot).Path.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
  $verifyRootUri = [Uri]$verifyRootPath

  $required = @(
    "back/margins-back.jar",
    "front/dist/index.html",
    "mysql-compose.yml",
    "runtime/env.example",
    "runtime/systemd/margins-back.service.example",
    "runtime/nginx/margins.conf.example",
    "manifest.txt",
    "checksums.sha256",
    "README.md"
  )

  foreach ($relative in $required) {
    $path = Join-Path $verifyRoot ($relative -replace '/', [IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path -LiteralPath $path)) {
      throw "Missing artifact entry: $relative"
    }
  }

  $forbiddenNamePatterns = @(
    '(^|[\\/])\.env($|\.)',
    '\.pem$',
    '\.p8$',
    '\.ppk$',
    'id_rsa$',
    'id_ed25519$'
  )

  foreach ($file in $artifactFiles) {
    $relative = [Uri]::UnescapeDataString($verifyRootUri.MakeRelativeUri([Uri]$file.FullName).ToString()) -replace '\\', '/'
    foreach ($pattern in $forbiddenNamePatterns) {
      if ($relative -match $pattern) {
        throw "Artifact contains forbidden secret-like file: $relative"
      }
    }
  }

  $backendJar = Join-Path $verifyRoot "back\margins-back.jar"
  if ((Get-Item -LiteralPath $backendJar).Length -le 0) {
    throw "Backend jar is empty: back/margins-back.jar"
  }

  $frontendAssets = @(Get-ChildItem -LiteralPath (Join-Path $verifyRoot "front\dist\assets") -File -ErrorAction SilentlyContinue)
  if ($frontendAssets.Count -eq 0) {
    throw "Frontend dist assets are missing under front/dist/assets"
  }

  $manifestText = Get-Content -LiteralPath (Join-Path $verifyRoot "manifest.txt") -Raw -Encoding UTF8
  foreach ($needle in @("builtAt=", "sourceCommit=", "backendJar=back/margins-back.jar", "frontendDist=front/dist", "mysqlCompose=mysql-compose.yml", "runtimeEnv=runtime/env.example", "runtimeSystemd=runtime/systemd/margins-back.service.example", "runtimeNginx=runtime/nginx/margins.conf.example")) {
    if (-not $manifestText.Contains($needle)) {
      throw "Manifest is missing required entry: $needle"
    }
  }

  $systemdText = Get-Content -LiteralPath (Join-Path $verifyRoot "runtime\systemd\margins-back.service.example") -Raw -Encoding UTF8
  foreach ($needle in @("EnvironmentFile=/opt/margins/.env", "ExecStart=/usr/bin/java -jar /opt/margins/current/back/margins-back.jar", "Restart=on-failure")) {
    if (-not $systemdText.Contains($needle)) {
      throw "Systemd example is missing required text: $needle"
    }
  }

  $nginxText = Get-Content -LiteralPath (Join-Path $verifyRoot "runtime\nginx\margins.conf.example") -Raw -Encoding UTF8
  foreach ($needle in @("root /opt/margins/current/front/dist;", "proxy_pass http://127.0.0.1:8080;", 'try_files $uri /index.html;')) {
    if (-not $nginxText.Contains($needle)) {
      throw "Nginx example is missing required text: $needle"
    }
  }

  $envText = Get-Content -LiteralPath (Join-Path $verifyRoot "runtime\env.example") -Raw -Encoding UTF8
  foreach ($needle in @("MARGINS_DB_URL=", "MARGINS_MYSQL_USER=", "MARGINS_MYSQL_PASSWORD=", "MARGINS_AUTH_JWT_SECRET=", "OPENAI_API_KEY=", "OPENAI_MODEL=", "MARGINS_BOOK_SEARCH_AI_FALLBACK_ENABLED=", "MARGINS_BOOK_SEARCH_PROVIDER=", "SPRING_PROFILES_ACTIVE=prod")) {
    if (-not $envText.Contains($needle)) {
      throw "Runtime env example is missing required variable: $needle"
    }
  }

  foreach ($needle in @("MARGINS_MYSQL_PASSWORD=change-me", "MARGINS_AUTH_JWT_SECRET=change-me", "OPENAI_API_KEY=")) {
    if (-not $envText.Contains($needle)) {
      throw "Runtime env example is missing safe placeholder: $needle"
    }
  }

  if ($envText -match 'OPENAI_API_KEY=sk-[A-Za-z0-9_-]+') {
    throw "Runtime env example appears to contain an OpenAI API key"
  }

  $textFileExtensions = @(".css", ".html", ".js", ".json", ".md", ".txt", ".xml", ".yml", ".yaml", ".example")
  $secretMarkers = @(
    "BEGIN OPENSSH PRIVATE KEY",
    "BEGIN RSA PRIVATE KEY",
    "BEGIN EC PRIVATE KEY",
    "OPENAI_API_KEY=sk-"
  )
  $secretLinePatterns = @(
    '^MARGINS_AUTH_JWT_SECRET=(?!change-me\s*$).+',
    '^MARGINS_MYSQL_PASSWORD=(?!change-me\s*$).+'
  )

  foreach ($file in $artifactFiles) {
    if ($textFileExtensions -notcontains $file.Extension.ToLowerInvariant()) {
      continue
    }

    $relative = [Uri]::UnescapeDataString($verifyRootUri.MakeRelativeUri([Uri]$file.FullName).ToString()) -replace '\\', '/'
    $fileText = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    foreach ($marker in $secretMarkers) {
      if ($fileText.Contains($marker)) {
        throw "Artifact text file contains forbidden secret marker: $relative"
      }
    }
    foreach ($pattern in $secretLinePatterns) {
      if ($fileText -match "(?m)$pattern") {
        throw "Artifact text file contains forbidden secret value: $relative"
      }
    }
  }

  $expectedChecksumTargets = @($artifactFiles |
    Where-Object { $_.Name -ne "checksums.sha256" }).Count
  $actualChecksumTargets = 0

  Get-Content -LiteralPath (Join-Path $verifyRoot "checksums.sha256") | ForEach-Object {
    if (-not $_.Trim()) {
      return
    }

    $parts = $_ -split '\s+', 2
    if ($parts.Count -ne 2) {
      throw "Invalid checksum line: $_"
    }

    $expected = $parts[0].Trim().ToLowerInvariant()
    $relative = $parts[1].Trim()
    $path = Join-Path $verifyRoot ($relative -replace '/', [IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path -LiteralPath $path)) {
      throw "Checksum target is missing: $relative"
    }

    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
      throw "Checksum mismatch for ${relative}: expected $expected but found $actual"
    }

    $actualChecksumTargets += 1
  }

  if ($actualChecksumTargets -ne $expectedChecksumTargets) {
    throw "Checksum target count mismatch: expected $expectedChecksumTargets entries but found $actualChecksumTargets"
  }

  Write-Output "Artifact verified: $($artifact.Path)"
}
finally {
  if (Test-Path -LiteralPath $verifyRoot) {
    Remove-Item -LiteralPath $verifyRoot -Recurse -Force
  }
}
