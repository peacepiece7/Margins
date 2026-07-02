$ErrorActionPreference = "Stop"

$files = @{
  LocalQuality = "harness/scripts/verify-local-quality.ps1"
  FinalAcceptance = "harness/scripts/audit-final-acceptance.ps1"
  CiWorkflow = ".github/workflows/ci.yml"
  CiAudit = "harness/scripts/audit-ci-workflow.ps1"
  ReadinessAudit = "harness/scripts/audit-mvp-readiness.ps1"
  ReadinessDoc = "docs/project/development-readiness.md"
  InfraSdd = "docs/infra/sdd.md"
  InfraBdd = "docs/infra/bdd.md"
  UploadProdEnvPs = "infra/scripts/upload-prod-env.ps1"
  UploadProdEnvSh = "infra/scripts/upload-prod-env.sh"
}

$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in $files.GetEnumerator()) {
  if (-not (Test-Path -LiteralPath $entry.Value)) {
    $failures.Add("Missing required file: $($entry.Value)") | Out-Null
  }
}

if ($failures.Count -gt 0) {
  $failures | ForEach-Object { Write-Output "- $_" }
  exit 1
}

function Read-Text {
  param([string] $Path)
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

function Assert-Order {
  param(
    [string] $Name,
    [string] $Text,
    [string[]] $Needles
  )

  $previous = -1
  foreach ($needle in $Needles) {
    $index = $Text.IndexOf($needle)
    if ($index -lt 0) {
      $failures.Add("$Name missing ordered text: $needle") | Out-Null
      continue
    }
    if ($index -le $previous) {
      $failures.Add("$Name order is invalid around: $needle") | Out-Null
    }
    $previous = $index
  }
}

$localQuality = Read-Text $files.LocalQuality
$finalAcceptance = Read-Text $files.FinalAcceptance
$ciWorkflow = Read-Text $files.CiWorkflow
$ciAudit = Read-Text $files.CiAudit
$readinessAudit = Read-Text $files.ReadinessAudit
$readinessDoc = Read-Text $files.ReadinessDoc
$infraSdd = Read-Text $files.InfraSdd
$infraBdd = Read-Text $files.InfraBdd
$uploadProdEnvPs = Read-Text $files.UploadProdEnvPs
$uploadProdEnvSh = Read-Text $files.UploadProdEnvSh

$coreAudits = @(
  "audit-mvp-readiness.ps1",
  "audit-doc-consistency.ps1",
  "audit-db-contract.ps1",
  "audit-live-deploy-guard.ps1",
  "audit-artifact-secret-guard.ps1",
  "audit-ci-workflow.ps1",
  "audit-completion-command.ps1",
  "audit-quality-gate-composition.ps1",
  "audit-acceptance-traceability.ps1",
  "audit-fullstack-e2e-runner.ps1",
  "audit-cross-platform-scripts.mjs",
  "audit-final-acceptance.ps1"
)

$deployPreflightAudits = @(
  "build-artifacts.ps1",
  "verify-artifacts.ps1",
  "audit-deploy-dry-run.ps1"
)

Assert-Contains "verify-local-quality" $localQuality ($coreAudits + $deployPreflightAudits + @(
  "LiveDeploySmoke requires -DeploymentPreflight -SshPreflight",
  "LiveDeploySmoke requires -DeploySmokeHealthUrl",
  "Raspberry Pi SSH preflight",
  "Raspberry Pi live deploy smoke",
  "Release artifact runtime smoke",
  "Release artifact frontend smoke",
  "-ArtifactRuntimeSmoke requires -DeploymentPreflight",
  "-ArtifactFrontendSmoke requires -DeploymentPreflight",
  "audit-release-artifact-runtime.ps1",
  "audit-release-artifact-frontend.ps1",
  '"-SmokeHealthUrl", $DeploySmokeHealthUrl'
))

Assert-Order "verify-local-quality default audits" $localQuality @(
  "MVP readiness audit",
  "Documentation consistency audit",
  "DB contract audit",
  "Live deploy safety guard audit",
  "Artifact secret guard audit",
  "CI workflow audit",
  "Completion command audit",
  "Quality gate composition audit",
  "Acceptance traceability audit",
  "Full-stack E2E runner audit",
  "Cross-platform script audit",
  "Final acceptance boundary audit"
)

Assert-Order "verify-local-quality deployment preflight" $localQuality @(
  "Build release artifact",
  "Release artifact verification",
  "Raspberry Pi deploy dry-run",
  "Release artifact runtime smoke",
  "Release artifact frontend smoke",
  "Raspberry Pi SSH preflight",
  "Raspberry Pi live deploy smoke"
)

Assert-Contains "final acceptance" $finalAcceptance ($coreAudits + @(
  "Quality gate composition audit",
  "PASS: final acceptance boundary is explicit and current evidence is consistent."
))

Assert-Contains "ci workflow" $ciWorkflow @(
  "Node harness quality audit",
  "npm run quality:full -- -SkipBackend -SkipFrontendBuild",
  "Build release artifact",
  "npm run deploy:build -- --skip-tests",
  "Verify release artifact",
  "npm run deploy:verify",
  "Upload release artifact"
)

Assert-Order "ci workflow gates" $ciWorkflow @(
  "Node harness quality audit",
  "Build release artifact",
  "Verify release artifact",
  "Upload release artifact"
)

Assert-Contains "ci workflow audit" $ciAudit @(
  "Node harness quality audit",
  "npm run quality:full -- -SkipBackend -SkipFrontendBuild",
  "does not run SSH/SCP, SSH actions, deploy env vars"
)

Assert-Contains "readiness audit" $readinessAudit @(
  "audit-fullstack-e2e-runner.ps1",
  "Full-Stack E2E Runner Audit",
  "PASS: full-stack E2E runner safety contract is documented and enforced.",
  "audit-quality-gate-composition.ps1",
  "Quality Gate Composition Audit",
  "PASS: quality gate composition is aligned across local, CI, final acceptance, and docs."
)

Assert-Contains "development readiness doc" $readinessDoc @(
  "harness/scripts/audit-fullstack-e2e-runner.ps1",
  "harness/scripts/audit-cross-platform-scripts.mjs",
  "harness/scripts/audit-quality-gate-composition.ps1",
  "harness/scripts/audit-acceptance-traceability.ps1",
  "harness/scripts/audit-release-artifact-runtime.ps1",
  "harness/scripts/audit-release-artifact-frontend.ps1",
  "quality gate composition",
  "verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl"
)

Assert-Contains "infra sdd" $infraSdd @(
  "scripts/harness.mjs",
  "npm run quality:full",
  "local quality gate, CI workflow, documentation pairs, MVP DB contract, artifact secret verifier contract, acceptance traceability, and deployment documentation",
  "npm run deploy:upload-env",
  "/opt/margins/.env"
)

Assert-Contains "infra bdd" $infraBdd @(
  "Quality gate composition stays aligned",
  "npm run quality:full -- --skip-backend --skip-frontend-build",
  "Node quality gate executes",
  "Production runtime env is uploaded before deploy"
)

Assert-Contains "upload-prod-env.ps1" $uploadProdEnvPs @(
  "/opt/margins/.env",
  "runtime_env=updated",
  "chmod 600"
)

Assert-Contains "upload-prod-env.sh" $uploadProdEnvSh @(
  "/opt/margins/.env",
  "runtime_env=updated",
  "chmod 600"
)

foreach ($forbidden in @(
  "deploy-raspberry-pi.ps1",
  " -SshPreflight",
  " -LiveDeploySmoke",
  "MARGINS_DEPLOY_SSH_KEY",
  "MARGINS_DEPLOY_HEALTH_URL"
)) {
  if ($ciWorkflow.Contains($forbidden)) {
    $failures.Add("CI workflow contains forbidden live deploy text: $forbidden") | Out-Null
  }
}

Write-Output "# Quality Gate Composition Audit"
Write-Output ""
Write-Output "Checked: local quality gate, CI workflow, final acceptance audit, and docs share the same required audit set."
Write-Output "Checked: deployment preflight remains ordered before SSH preflight and live deploy smoke."
Write-Output "Checked: full-stack E2E runner safety audit is part of the local quality gate."
Write-Output "Checked: cross-platform script audit is part of the local quality gate and CI."
Write-Output "Checked: CI exposes quality composition directly without live Raspberry Pi deploy text."

if ($failures.Count -gt 0) {
  Write-Output ""
  Write-Output "Failures:"
  $failures | ForEach-Object { Write-Output "- $_" }
  exit 1
}

Write-Output ""
Write-Output "PASS: quality gate composition is aligned across local, CI, final acceptance, and docs."
