$ErrorActionPreference = "Stop"

$workflowPath = ".github/workflows/ci.yml"
if (-not (Test-Path -LiteralPath $workflowPath)) {
  throw "Missing CI workflow: $workflowPath"
}

$text = Get-Content -LiteralPath $workflowPath -Raw -Encoding UTF8
$lines = Get-Content -LiteralPath $workflowPath -Encoding UTF8

function Assert-Contains {
  param(
    [string] $Needle
  )

  if (-not $text.Contains($Needle)) {
    throw "CI workflow missing required text: $Needle"
  }
}

function Get-LineNumber {
  param(
    [string] $Needle
  )

  for ($index = 0; $index -lt $lines.Count; $index++) {
    if ($lines[$index].Contains($Needle)) {
      return $index + 1
    }
  }

  return -1
}

foreach ($needle in @(
  "on:",
  "pull_request:",
  "branches:",
  "- main",
  "Node harness quality audit",
  "npm run quality:full -- -SkipBackend -SkipFrontendBuild",
  "Build release artifact",
  "npm run deploy:build -- --skip-tests",
  "Verify release artifact",
  "npm run deploy:verify",
  "Upload release artifact",
  "actions/upload-artifact@v4"
)) {
  Assert-Contains -Needle $needle
}

$orderedNeedles = @(
  "Node harness quality audit",
  "Build release artifact",
  "Verify release artifact",
  "Upload release artifact"
)

$previousLine = 0
foreach ($needle in $orderedNeedles) {
  $line = Get-LineNumber -Needle $needle
  if ($line -le $previousLine) {
    throw "CI workflow step order is invalid around: $needle"
  }
  $previousLine = $line
}

foreach ($forbidden in @(
  "deploy-raspberry-pi.ps1",
  " -SshPreflight",
  " -LiveDeploySmoke",
  "MARGINS_DEPLOY_SSH_KEY"
)) {
  if ($text.Contains($forbidden)) {
    throw "CI workflow contains forbidden deploy execution text: $forbidden"
  }
}

$forbiddenLinePatterns = @(
  '^\s*run:\s*.*\bssh(\.exe)?\b',
  '^\s*run:\s*.*\bscp(\.exe)?\b',
  '^\s*uses:\s*.*ssh',
  '^\s*uses:\s*.*scp',
  'MARGINS_DEPLOY_(HOST|USER|DIR|SSH_KEY|HEALTH_URL)',
  'DEPLOY_SSH'
)

for ($index = 0; $index -lt $lines.Count; $index++) {
  $line = $lines[$index]
  foreach ($pattern in $forbiddenLinePatterns) {
    if ($line -match $pattern) {
      throw "CI workflow contains forbidden deploy pattern on line $($index + 1): $pattern"
    }
  }
}

Write-Output "# CI Workflow Audit"
Write-Output ""
Write-Output "Checked: CI runs the consolidated Node harness quality audit before artifact build and verification."
Write-Output "Checked: CI builds, verifies, and uploads the release artifact."
Write-Output "Checked: CI workflow does not run SSH/SCP, SSH actions, deploy env vars, or Raspberry Pi deploy commands."
Write-Output ""
Write-Output "PASS: CI workflow gates use the Node harness and do not perform live Raspberry Pi deployment."
