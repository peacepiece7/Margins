$ErrorActionPreference = "Stop"

$scriptPath = "harness/scripts/run-fullstack-e2e.ps1"
$frontSddPath = "docs/front/sdd.md"
$frontBddPath = "docs/front/bdd.md"
$readinessPath = "docs/project/development-readiness.md"

$failures = New-Object System.Collections.Generic.List[string]

function Read-Text {
  param([string] $Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    $failures.Add("Missing required file: $Path") | Out-Null
    return ""
  }

  return Get-Content -LiteralPath $Path -Raw -Encoding UTF8
}

function Assert-Contains {
  param(
    [string] $Name,
    [string] $Text,
    [string[]] $Needles
  )

  foreach ($needle in $Needles) {
    if (-not $Text.Contains($needle)) {
      $failures.Add("$Name missing required text: $needle") | Out-Null
    }
  }
}

$runner = Read-Text $scriptPath
$frontSdd = Read-Text $frontSddPath
$frontBdd = Read-Text $frontBddPath
$readiness = Read-Text $readinessPath

Assert-Contains "run-fullstack-e2e" $runner @(
  '[int] $BackendPort = 18080',
  '[int] $FrontendPort = 15173',
  '[switch] $ReuseExistingServices',
  'function Test-TcpOpen',
  'Backend port $BackendPort is already in use',
  'Frontend port $FrontendPort is already in use',
  'pass -ReuseExistingServices when reuse is intentional',
  'Test-HttpOk -Url $backendHealthUrl',
  'Test-HttpOk -Url $frontendUrl',
  'PASS: full-stack E2E completed.'
)

Assert-Contains "front SDD" $frontSdd @(
  'isolated local ports `18080` and `15173`',
  '-ReuseExistingServices',
  'port is already in use'
)

Assert-Contains "front BDD" $frontBdd @(
  'Full-stack E2E runner starts isolated local services',
  'Full-stack E2E runner refuses accidental stale service reuse',
  '-ReuseExistingServices'
)

Assert-Contains "development readiness" $readiness @(
  'Full-stack E2E now defaults to isolated ports',
  'Current full-stack E2E stale-service guard',
  'occupied-port guard smoke'
)

Write-Output "# Full-Stack E2E Runner Audit"
Write-Output ""
Write-Output "Checked: isolated default ports, explicit reuse flag, occupied-port rejection, reuse health checks, and docs."

if ($failures.Count -gt 0) {
  Write-Output ""
  Write-Output "Failures:"
  $failures | ForEach-Object { Write-Output "- $_" }
  exit 1
}

Write-Output ""
Write-Output "PASS: full-stack E2E runner safety contract is documented and enforced."
