$ErrorActionPreference = "Stop"

function New-MinimalRelease {
  param(
    [string] $Root
  )

  New-Item -ItemType Directory -Force -Path (Join-Path $Root "back") | Out-Null
  New-Item -ItemType Directory -Force -Path (Join-Path $Root "front\dist\assets") | Out-Null
  New-Item -ItemType Directory -Force -Path (Join-Path $Root "runtime") | Out-Null
  New-Item -ItemType Directory -Force -Path (Join-Path $Root "runtime\systemd") | Out-Null
  New-Item -ItemType Directory -Force -Path (Join-Path $Root "runtime\nginx") | Out-Null

  Set-Content -LiteralPath (Join-Path $Root "back\margins-back.jar") -Value "jar-placeholder" -Encoding ASCII
  Set-Content -LiteralPath (Join-Path $Root "front\dist\index.html") -Value "<html></html>" -Encoding ASCII
  Set-Content -LiteralPath (Join-Path $Root "front\dist\assets\index.js") -Value "console.log('ok')" -Encoding ASCII
  Set-Content -LiteralPath (Join-Path $Root "mysql-compose.yml") -Value "services: {}" -Encoding ASCII
  Set-Content -LiteralPath (Join-Path $Root "README.md") -Value "# Test Release" -Encoding ASCII
  @(
    "builtAt=2026-06-12T00:00:00Z"
    "sourceCommit=test"
    "backendJar=back/margins-back.jar"
    "frontendDist=front/dist"
    "mysqlCompose=mysql-compose.yml"
    "runtimeEnv=runtime/env.example"
    "runtimeSystemd=runtime/systemd/margins-back.service.example"
    "runtimeNginx=runtime/nginx/margins.conf.example"
  ) | Set-Content -LiteralPath (Join-Path $Root "manifest.txt") -Encoding ASCII
  @(
    "MARGINS_DB_URL=jdbc:mysql://localhost:3306/margins"
    "MARGINS_MYSQL_USER=margins"
    "MARGINS_MYSQL_PASSWORD=change-me"
    "MARGINS_AUTH_JWT_SECRET=change-me"
    "OPENAI_API_KEY="
    "OPENAI_MODEL=gpt-5.5"
    "SPRING_PROFILES_ACTIVE=prod"
  ) | Set-Content -LiteralPath (Join-Path $Root "runtime\env.example") -Encoding ASCII
  @(
    "[Service]"
    "EnvironmentFile=/opt/margins/.env"
    "ExecStart=/usr/bin/java -jar /opt/margins/current/back/margins-back.jar"
    "Restart=on-failure"
  ) | Set-Content -LiteralPath (Join-Path $Root "runtime\systemd\margins-back.service.example") -Encoding ASCII
  @(
    "server {"
    "  root /opt/margins/current/front/dist;"
    "  location /api/ { proxy_pass http://127.0.0.1:8080; }"
    "  location / { try_files `$uri /index.html; }"
    "}"
  ) | Set-Content -LiteralPath (Join-Path $Root "runtime\nginx\margins.conf.example") -Encoding ASCII
}

function Update-Checksums {
  param(
    [string] $Root
  )

  $rootPath = (Resolve-Path -LiteralPath $Root).Path.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
  $rootUri = [Uri]$rootPath
  $lines = Get-ChildItem -LiteralPath $Root -Recurse -File |
    Where-Object { $_.Name -ne "checksums.sha256" } |
    Sort-Object FullName |
    ForEach-Object {
      $relative = [Uri]::UnescapeDataString($rootUri.MakeRelativeUri([Uri]$_.FullName).ToString())
      $hash = Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
      "$($hash.Hash.ToLowerInvariant())  $($relative -replace '\\', '/')"
    }
  $lines | Set-Content -LiteralPath (Join-Path $Root "checksums.sha256") -Encoding ASCII
}

function New-TestZip {
  param(
    [string] $SourceRoot,
    [string] $ZipPath
  )

  if (Test-Path -LiteralPath $ZipPath) {
    Remove-Item -LiteralPath $ZipPath -Force
  }

  Add-Type -AssemblyName System.IO.Compression
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $sourceRootPath = (Resolve-Path -LiteralPath $SourceRoot).Path.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
  $sourceRootUri = [Uri]$sourceRootPath
  $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
  try {
    Get-ChildItem -LiteralPath $SourceRoot -Recurse -File |
      Sort-Object FullName |
      ForEach-Object {
        $relative = [Uri]::UnescapeDataString($sourceRootUri.MakeRelativeUri([Uri]$_.FullName).ToString()) -replace '\\', '/'
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
}

function Invoke-ExpectedArtifactFailure {
  param(
    [string] $Name,
    [string] $ZipPath,
    [string] $ExpectedText
  )

  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $output = & powershell -NoProfile -ExecutionPolicy Bypass -File "infra\scripts\verify-artifacts.ps1" -ArtifactPath $ZipPath 2>&1
    $exitCode = $LASTEXITCODE
  }
  finally {
    $ErrorActionPreference = $previousErrorActionPreference
  }

  $text = $output -join "`n"
  if ($exitCode -eq 0) {
    throw "$Name unexpectedly passed"
  }

  if (-not $text.Contains($ExpectedText)) {
    throw "$Name did not include expected guard text: $ExpectedText"
  }
}

function Get-RepoRelativePath {
  param(
    [string] $Path
  )

  $repoRootPath = (Resolve-Path -LiteralPath $repoRoot).Path.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
  $repoRootUri = [Uri]$repoRootPath
  return [Uri]::UnescapeDataString($repoRootUri.MakeRelativeUri([Uri](Resolve-Path -LiteralPath $Path).Path).ToString()) -replace '\\', '/'
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$auditRoot = Join-Path $repoRoot ("harness\artifacts\artifact-secret-guard\" + [Guid]::NewGuid().ToString("N"))

if (Test-Path -LiteralPath $auditRoot) {
  Remove-Item -LiteralPath $auditRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $auditRoot | Out-Null

try {
  $envLeakRoot = Join-Path $auditRoot "env-leak"
  New-MinimalRelease -Root $envLeakRoot
  Set-Content -LiteralPath (Join-Path $envLeakRoot ".env") -Value "OPENAI_API_KEY=sk-test" -Encoding ASCII
  Update-Checksums -Root $envLeakRoot
  $envLeakZip = Join-Path $auditRoot "env-leak.zip"
  New-TestZip -SourceRoot $envLeakRoot -ZipPath $envLeakZip
  Invoke-ExpectedArtifactFailure `
    -Name "Artifact .env leak" `
    -ZipPath (Get-RepoRelativePath -Path $envLeakZip) `
    -ExpectedText "Artifact contains forbidden secret-like file"

  $keyMarkerRoot = Join-Path $auditRoot "key-marker"
  New-MinimalRelease -Root $keyMarkerRoot
  Set-Content -LiteralPath (Join-Path $keyMarkerRoot "README.md") -Value "BEGIN OPENSSH PRIVATE KEY" -Encoding ASCII
  Update-Checksums -Root $keyMarkerRoot
  $keyMarkerZip = Join-Path $auditRoot "key-marker.zip"
  New-TestZip -SourceRoot $keyMarkerRoot -ZipPath $keyMarkerZip
  Invoke-ExpectedArtifactFailure `
    -Name "Artifact private key marker" `
    -ZipPath (Get-RepoRelativePath -Path $keyMarkerZip) `
    -ExpectedText "Artifact text file contains forbidden secret marker"

  $openAiKeyRoot = Join-Path $auditRoot "openai-key"
  New-MinimalRelease -Root $openAiKeyRoot
  @(
    "MARGINS_DB_URL=jdbc:mysql://localhost:3306/margins"
    "MARGINS_MYSQL_USER=margins"
    "MARGINS_MYSQL_PASSWORD=change-me"
    "MARGINS_AUTH_JWT_SECRET=change-me"
    "OPENAI_API_KEY=sk-test-secret"
    "OPENAI_MODEL=gpt-5.5"
    "SPRING_PROFILES_ACTIVE=prod"
  ) | Set-Content -LiteralPath (Join-Path $openAiKeyRoot "runtime\env.example") -Encoding ASCII
  Update-Checksums -Root $openAiKeyRoot
  $openAiKeyZip = Join-Path $auditRoot "openai-key.zip"
  New-TestZip -SourceRoot $openAiKeyRoot -ZipPath $openAiKeyZip
  Invoke-ExpectedArtifactFailure `
    -Name "Artifact OpenAI key marker" `
    -ZipPath (Get-RepoRelativePath -Path $openAiKeyZip) `
    -ExpectedText "Runtime env example appears to contain an OpenAI API key"

  $jwtSecretRoot = Join-Path $auditRoot "jwt-secret"
  New-MinimalRelease -Root $jwtSecretRoot
  Set-Content -LiteralPath (Join-Path $jwtSecretRoot "README.md") -Value "MARGINS_AUTH_JWT_SECRET=real-jwt-secret" -Encoding ASCII
  Update-Checksums -Root $jwtSecretRoot
  $jwtSecretZip = Join-Path $auditRoot "jwt-secret.zip"
  New-TestZip -SourceRoot $jwtSecretRoot -ZipPath $jwtSecretZip
  Invoke-ExpectedArtifactFailure `
    -Name "Artifact JWT secret value" `
    -ZipPath (Get-RepoRelativePath -Path $jwtSecretZip) `
    -ExpectedText "Artifact text file contains forbidden secret value"

  $dbPasswordRoot = Join-Path $auditRoot "db-password"
  New-MinimalRelease -Root $dbPasswordRoot
  Set-Content -LiteralPath (Join-Path $dbPasswordRoot "README.md") -Value "MARGINS_MYSQL_PASSWORD=real-db-password" -Encoding ASCII
  Update-Checksums -Root $dbPasswordRoot
  $dbPasswordZip = Join-Path $auditRoot "db-password.zip"
  New-TestZip -SourceRoot $dbPasswordRoot -ZipPath $dbPasswordZip
  Invoke-ExpectedArtifactFailure `
    -Name "Artifact DB password value" `
    -ZipPath (Get-RepoRelativePath -Path $dbPasswordZip) `
    -ExpectedText "Artifact text file contains forbidden secret value"

  $privateKeyFileRoot = Join-Path $auditRoot "private-key-file"
  New-MinimalRelease -Root $privateKeyFileRoot
  Set-Content -LiteralPath (Join-Path $privateKeyFileRoot "id_rsa") -Value "not-a-real-key" -Encoding ASCII
  Update-Checksums -Root $privateKeyFileRoot
  $privateKeyFileZip = Join-Path $auditRoot "private-key-file.zip"
  New-TestZip -SourceRoot $privateKeyFileRoot -ZipPath $privateKeyFileZip
  Invoke-ExpectedArtifactFailure `
    -Name "Artifact private key filename" `
    -ZipPath (Get-RepoRelativePath -Path $privateKeyFileZip) `
    -ExpectedText "Artifact contains forbidden secret-like file"

  Write-Output "# Artifact Secret Guard Audit"
  Write-Output ""
  Write-Output "Checked: release artifact verification rejects packaged .env files."
  Write-Output "Checked: release artifact verification rejects private-key text markers."
  Write-Output "Checked: release artifact verification rejects OpenAI API key markers."
  Write-Output "Checked: release artifact verification rejects JWT secret value markers."
  Write-Output "Checked: release artifact verification rejects DB password value markers."
  Write-Output "Checked: release artifact verification rejects private-key filenames."
  Write-Output ""
  Write-Output "PASS: artifact secret guard rejects secret-like release contents."
}
finally {
  if (Test-Path -LiteralPath $auditRoot) {
    Remove-Item -LiteralPath $auditRoot -Recurse -Force
  }
}

exit 0
