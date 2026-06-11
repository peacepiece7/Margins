param(
  [Parameter(Mandatory = $true)]
  [string] $Name,

  [string] $OutputDir = "harness/work"
)

$ErrorActionPreference = "Stop"

$templatePath = Join-Path "harness/templates" "verification-report.md"
if (-not (Test-Path -LiteralPath $templatePath)) {
  throw "Missing template: $templatePath"
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$safeName = ($Name -replace '[^A-Za-z0-9._-]', '-').Trim('-')
if ([string]::IsNullOrWhiteSpace($safeName)) {
  throw "Name must contain at least one filename-safe character."
}

$outputPath = Join-Path $OutputDir "$safeName.verification-report.md"
Copy-Item -LiteralPath $templatePath -Destination $outputPath -Force
Write-Output $outputPath
