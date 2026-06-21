$ErrorActionPreference = "Stop"

function Invoke-Audit {
  param(
    [string] $Name,
    [string] $Path
  )

  Write-Host ""
  Write-Host "## $Name"
  $output = & powershell -NoProfile -ExecutionPolicy Bypass -File $Path
  if ($LASTEXITCODE -ne 0) {
    $output | ForEach-Object { Write-Host $_ }
    throw "$Name failed with exit code $LASTEXITCODE"
  }
  $output | ForEach-Object { Write-Host $_ }
  return ,$output
}

function Assert-Text {
  param(
    [System.Collections.Generic.List[string]] $Failures,
    [string] $Name,
    [string] $Text,
    [string[]] $Needles
  )

  foreach ($needle in $Needles) {
    if (-not $Text.Contains($needle)) {
      $Failures.Add("$Name missing required text: $needle") | Out-Null
    }
  }
}

$failures = New-Object System.Collections.Generic.List[string]

Write-Output "# Final Acceptance Audit"
Write-Output ""
Write-Output "This audit verifies the current MVP acceptance boundary. It does not replace full-stack E2E or live Raspberry Pi smoke runs."

$readinessOutput = Invoke-Audit -Name "MVP readiness audit" -Path "harness\scripts\audit-mvp-readiness.ps1"
$docOutput = Invoke-Audit -Name "Documentation consistency audit" -Path "harness\scripts\audit-doc-consistency.ps1"
$dbOutput = Invoke-Audit -Name "DB contract audit" -Path "harness\scripts\audit-db-contract.ps1"
$deployOutput = Invoke-Audit -Name "Deploy dry-run audit" -Path "harness\scripts\audit-deploy-dry-run.ps1"
$liveDeployGuardOutput = Invoke-Audit -Name "Live deploy guard audit" -Path "harness\scripts\audit-live-deploy-guard.ps1"
$artifactSecretGuardOutput = Invoke-Audit -Name "Artifact secret guard audit" -Path "harness\scripts\audit-artifact-secret-guard.ps1"
$ciWorkflowOutput = Invoke-Audit -Name "CI workflow audit" -Path "harness\scripts\audit-ci-workflow.ps1"
$completionCommandOutput = Invoke-Audit -Name "Completion command audit" -Path "harness\scripts\audit-completion-command.ps1"
$qualityGateCompositionOutput = Invoke-Audit -Name "Quality gate composition audit" -Path "harness\scripts\audit-quality-gate-composition.ps1"
$acceptanceTraceabilityOutput = Invoke-Audit -Name "Acceptance traceability audit" -Path "harness\scripts\audit-acceptance-traceability.ps1"
$fullStackE2eRunnerOutput = Invoke-Audit -Name "Full-stack E2E runner audit" -Path "harness\scripts\audit-fullstack-e2e-runner.ps1"

$readinessText = $readinessOutput -join "`n"
$docText = $docOutput -join "`n"
$dbText = $dbOutput -join "`n"
$deployText = $deployOutput -join "`n"
$liveDeployGuardText = $liveDeployGuardOutput -join "`n"
$artifactSecretGuardText = $artifactSecretGuardOutput -join "`n"
$ciWorkflowText = $ciWorkflowOutput -join "`n"
$completionCommandText = $completionCommandOutput -join "`n"
$qualityGateCompositionText = $qualityGateCompositionOutput -join "`n"
$acceptanceTraceabilityText = $acceptanceTraceabilityOutput -join "`n"
$fullStackE2eRunnerText = $fullStackE2eRunnerOutput -join "`n"

Assert-Text $failures "MVP readiness audit" $readinessText @(
  "PASS: MVP readiness evidence paths and required text are present.",
  "## raspberry-pi-deploy",
  "- Status: implemented"
)
Assert-Text $failures "Documentation consistency audit" $docText @(
  "PASS: documentation indexes and work records are consistent."
)
Assert-Text $failures "DB contract audit" $dbText @(
  "PASS: DB schema, seed, query, and reset contracts are consistent."
)
Assert-Text $failures "Deploy dry-run audit" $deployText @(
  "PASS: Raspberry Pi deploy dry-run output contract is consistent."
)
Assert-Text $failures "Live deploy guard audit" $liveDeployGuardText @(
  "PASS: live deploy smoke guard prevents accidental Raspberry Pi transfer/restart."
)
Assert-Text $failures "Artifact secret guard audit" $artifactSecretGuardText @(
  "PASS: artifact secret guard rejects secret-like release contents."
)
Assert-Text $failures "CI workflow audit" $ciWorkflowText @(
  "SSH actions",
  "deploy env vars",
  "PASS: CI workflow gates are explicit and do not perform live Raspberry Pi deployment."
)
Assert-Text $failures "Completion command audit" $completionCommandText @(
  "PASS: final deployment completion command is consistent across docs and scripts."
)
Assert-Text $failures "Quality gate composition audit" $qualityGateCompositionText @(
  "PASS: quality gate composition is aligned across local, CI, final acceptance, and docs."
)
Assert-Text $failures "Acceptance traceability audit" $acceptanceTraceabilityText @(
  "PASS: MVP acceptance traceability is connected across planning, design, BDD, implementation, and tests."
)
Assert-Text $failures "Full-stack E2E runner audit" $fullStackE2eRunnerText @(
  "PASS: full-stack E2E runner safety contract is documented and enforced."
)

if ($readinessText.Contains("- Status: partial") -or $readinessText.Contains("- Status: planned")) {
  $failures.Add("MVP readiness still contains partial or planned requirements.") | Out-Null
}

$blockedMatches = [regex]::Matches($readinessText, "- Status: blocked")
if ($blockedMatches.Count -ne 0) {
  $failures.Add("MVP readiness must not contain blocked requirements after live Raspberry Pi deploy smoke, but found $($blockedMatches.Count).") | Out-Null
}

$readinessDoc = Get-Content -LiteralPath "docs/project/development-readiness.md" -Raw -Encoding UTF8
$readinessEvidenceSection = $readinessDoc
$lastVerificationIndex = $readinessDoc.IndexOf("## Last Local Verification")
if ($lastVerificationIndex -ge 0) {
  $readinessEvidenceSection = $readinessDoc.Substring($lastVerificationIndex)
  $finalBoundaryIndex = $readinessEvidenceSection.IndexOf("## Final Acceptance Boundary")
  if ($finalBoundaryIndex -gt 0) {
    $readinessEvidenceSection = $readinessEvidenceSection.Substring(0, $finalBoundaryIndex)
  }
}
foreach ($forbiddenEvidenceText in @(
  "pending verification",
  "weak evidence",
  "Missing or weak",
  "unverified"
)) {
  if ($readinessEvidenceSection.Contains($forbiddenEvidenceText)) {
    $failures.Add("development-readiness verification evidence contains unverified text: $forbiddenEvidenceText") | Out-Null
  }
}

foreach ($requiredPath in @(
  "harness/scripts/verify-local-quality.ps1",
  "harness/scripts/run-fullstack-e2e.ps1",
  "harness/scripts/audit-final-acceptance.ps1",
  "harness/scripts/audit-artifact-secret-guard.ps1",
  "harness/scripts/audit-ci-workflow.ps1",
  "harness/scripts/audit-completion-command.ps1",
  "harness/scripts/audit-quality-gate-composition.ps1",
  "harness/scripts/audit-acceptance-traceability.ps1",
  "harness/scripts/audit-fullstack-e2e-runner.ps1",
  "harness/scripts/audit-release-artifact-runtime.ps1",
  "harness/scripts/audit-release-artifact-frontend.ps1",
  "harness/scripts/audit-live-deploy-guard.ps1",
  "infra/scripts/build-artifacts.ps1",
  "infra/scripts/verify-artifacts.ps1",
  "infra/scripts/deploy-raspberry-pi.ps1",
  "infra/scripts/upload-prod-env.ps1",
  "infra/scripts/upload-prod-env.sh",
  "docs/project/development-readiness.md",
  "harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md"
)) {
  if (-not (Test-Path -LiteralPath $requiredPath)) {
    $failures.Add("Missing final acceptance path: $requiredPath") | Out-Null
  }
}

Assert-Text $failures "development-readiness" $readinessDoc @(
  "Core MVP reading flows are implemented end to end",
  "Raspberry Pi live transfer/restart and post-deploy health smoke are verified",
  "Local quality gate:",
  "Deployment preflight:",
  "Current deployment preflight gate:",
  "verify-local-quality.ps1 -DeploymentPreflight -SkipBackend -SkipFrontendBuild",
  "Current artifact runtime smoke gate:",
  "Current artifact frontend smoke gate:",
  "Current combined artifact smoke gate:",
  "verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SkipBackend -SkipFrontendBuild",
  "verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke",
  "verify-local-quality.ps1 -DeploymentPreflight -ArtifactFrontendSmoke",
  "upload-prod-env.ps1 -RuntimeEnvPath .env.production",
  "verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl",
  "Full local quality with E2E:",
  "Current full-stack E2E gate:",
  "verify-local-quality.ps1 -FullStackE2E -SkipBackend -SkipFrontendBuild",
  "audit-fullstack-e2e-runner.ps1",
  "full-stack E2E runner audits",
  "rejects accidental stale-service reuse"
)

Write-Output ""
Write-Output "## Acceptance Boundary"
Write-Output "- Implemented MVP slices have repeatable evidence audits."
Write-Output "- Current full-stack E2E evidence is recorded in docs/project/development-readiness.md."
Write-Output "- Current deployment preflight evidence is recorded in docs/project/development-readiness.md."
Write-Output "- Optional release artifact runtime and frontend smokes are documented for local verification before Raspberry Pi transfer."
Write-Output "- Local quality and deployment preflight commands are documented in docs/project/development-readiness.md."
Write-Output "- Live deploy smoke cannot run unless deployment preflight, SSH preflight, and health URL are all explicit."
Write-Output "- Release artifact validation rejects secret-like files and private-key markers."
Write-Output "- CI gates verify release artifacts without running live Raspberry Pi deploy commands."
Write-Output "- The final Raspberry Pi completion command is consistent across docs and scripts."
Write-Output "- Local, CI, final acceptance, and documentation quality-gate composition stays aligned."
Write-Output "- MVP acceptance traceability connects planning, design, BDD, implementation, and tests."
Write-Output "- Full-stack E2E runner safety is audited so stale local services cannot silently verify the wrong build."
Write-Output "- Development readiness evidence cannot contain pending, weak, or unverified verification claims."
Write-Output "- MVP readiness has no blocked requirements after the live Raspberry Pi deploy smoke passed."
Write-Output "- Final completion can be claimed when this audit and the full live deploy quality gate pass."

if ($failures.Count -gt 0) {
  Write-Output ""
  Write-Output "Failures:"
  $failures | ForEach-Object { Write-Output "- $_" }
  exit 1
}

Write-Output ""
Write-Output "PASS: final acceptance boundary is explicit and current evidence is consistent."
