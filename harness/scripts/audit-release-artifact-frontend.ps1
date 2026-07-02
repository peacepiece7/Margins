param(
  [string] $ArtifactPath = "infra/artifacts/margins-release.zip",
  [switch] $SkipArtifactVerify
)

$ErrorActionPreference = "Stop"

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

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..")
$artifact = Resolve-Path -LiteralPath (Join-Path $repoRoot $ArtifactPath)
$auditRoot = Join-Path $repoRoot ("harness\artifacts\release-artifact-frontend\" + [Guid]::NewGuid().ToString("N"))

New-Item -ItemType Directory -Force -Path $auditRoot | Out-Null

$oldDistDir = $env:MARGINS_DIST_DIR

Push-Location $repoRoot
try {
  if (-not $SkipArtifactVerify) {
    Invoke-PowerShellFile -Path "infra/scripts/verify-artifacts.ps1" -Arguments @("-ArtifactPath", $ArtifactPath)
    if ($LASTEXITCODE -ne 0) {
      throw "Release artifact verification failed with exit code $LASTEXITCODE"
    }
  }

  Expand-Archive -LiteralPath $artifact.Path -DestinationPath $auditRoot -Force
  $distDir = Join-Path $auditRoot "front\dist"
  if (-not (Test-Path -LiteralPath (Join-Path $distDir "index.html"))) {
    throw "Expanded artifact is missing front/dist/index.html"
  }

  $env:MARGINS_DIST_DIR = $distDir
  Push-Location (Join-Path $repoRoot "front")
  try {
    node "scripts\verify-production-selectors.mjs"
    if ($LASTEXITCODE -ne 0) {
      throw "Release artifact frontend production smoke failed with exit code $LASTEXITCODE"
    }
  }
  finally {
    Pop-Location
  }

  Write-Output "# Release Artifact Frontend Audit"
  Write-Output ""
  Write-Output "Artifact checked: $($artifact.Path)"
  Write-Output "Frontend dist rendered from expanded release artifact."
  Write-Output "Bundled asset references exist and production data selectors are stripped."
  Write-Output ""
  Write-Output "PASS: release artifact frontend production smoke passed."
}
finally {
  Pop-Location

  if ($null -eq $oldDistDir) {
    Remove-Item -Path "env:MARGINS_DIST_DIR" -ErrorAction SilentlyContinue
  } else {
    $env:MARGINS_DIST_DIR = $oldDistDir
  }

  if (Test-Path -LiteralPath $auditRoot) {
    Remove-Item -LiteralPath $auditRoot -Recurse -Force
  }
}
