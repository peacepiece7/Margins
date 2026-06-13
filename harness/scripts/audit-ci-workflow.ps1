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
  "Project readiness audit",
  "Documentation consistency audit",
  "DB contract audit",
  "Live deploy guard audit",
  "./harness/scripts/audit-live-deploy-guard.ps1",
  "Artifact secret guard audit",
  "./harness/scripts/audit-artifact-secret-guard.ps1",
  "CI workflow audit",
  "./harness/scripts/audit-ci-workflow.ps1",
  "Completion command audit",
  "./harness/scripts/audit-completion-command.ps1",
  "Quality gate composition audit",
  "./harness/scripts/audit-quality-gate-composition.ps1",
  "Acceptance traceability audit",
  "./harness/scripts/audit-acceptance-traceability.ps1",
  "Final acceptance boundary audit",
  "./harness/scripts/audit-final-acceptance.ps1",
  "Build release artifact",
  "./infra/scripts/build-artifacts.ps1 -SkipTests",
  "Verify release artifact",
  "./infra/scripts/verify-artifacts.ps1",
  "Upload release artifact",
  "actions/upload-artifact@v4"
)) {
  Assert-Contains -Needle $needle
}

$orderedNeedles = @(
  "Live deploy guard audit",
  "Artifact secret guard audit",
  "CI workflow audit",
  "Completion command audit",
  "Quality gate composition audit",
  "Acceptance traceability audit",
  "Final acceptance boundary audit",
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
Write-Output "Checked: CI runs readiness, docs, DB, live-deploy guard, artifact-secret guard, CI workflow, completion command, quality composition, acceptance traceability, and final acceptance audits."
Write-Output "Checked: CI builds, verifies, and uploads the release artifact."
Write-Output "Checked: CI workflow does not run SSH/SCP, SSH actions, deploy env vars, or Raspberry Pi deploy commands."
Write-Output ""
Write-Output "PASS: CI workflow gates are explicit and do not perform live Raspberry Pi deployment."
