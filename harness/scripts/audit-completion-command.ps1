$ErrorActionPreference = "Stop"

function Assert-Text {
  param(
    [string] $Name,
    [string] $Text,
    [string[]] $Needles
  )

  foreach ($needle in $Needles) {
    if (-not $Text.Contains($needle)) {
      throw "$Name missing required text: $needle"
    }
  }
}

$qualityGate = Get-Content -LiteralPath "harness/scripts/verify-local-quality.ps1" -Raw -Encoding UTF8
$deployScript = Get-Content -LiteralPath "infra/scripts/deploy-raspberry-pi.ps1" -Raw -Encoding UTF8
$readinessDoc = Get-Content -LiteralPath "docs/project/development-readiness.md" -Raw -Encoding UTF8
$infraSdd = Get-Content -LiteralPath "docs/infra/sdd.md" -Raw -Encoding UTF8
$infraBdd = Get-Content -LiteralPath "docs/infra/bdd.md" -Raw -Encoding UTF8

$completionCommand = "verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <"

Assert-Text "verify-local-quality" $qualityGate @(
  "[switch] `$DeploymentPreflight",
  "[switch] `$ArtifactRuntimeSmoke",
  "[switch] `$ArtifactFrontendSmoke",
  "[switch] `$SshPreflight",
  "[switch] `$LiveDeploySmoke",
  "[string] `$DeploySmokeHealthUrl = `$env:MARGINS_DEPLOY_HEALTH_URL",
  "-LiveDeploySmoke requires -DeploymentPreflight -SshPreflight",
  "-LiveDeploySmoke requires -DeploySmokeHealthUrl or MARGINS_DEPLOY_HEALTH_URL.",
  "-ArtifactRuntimeSmoke requires -DeploymentPreflight",
  "-ArtifactFrontendSmoke requires -DeploymentPreflight",
  "Release artifact runtime smoke",
  "Release artifact frontend smoke",
  "Raspberry Pi SSH preflight",
  "-SshPreflight",
  "Raspberry Pi live deploy smoke",
  "-SmokeHealthUrl `$DeploySmokeHealthUrl"
)

Assert-Text "deploy-raspberry-pi" $deployScript @(
  "[string] `$SmokeHealthUrl = `$env:MARGINS_DEPLOY_HEALTH_URL",
  "[switch] `$SshPreflight",
  "SSH preflight passed for Raspberry Pi target.",
  "scp @sshOptions",
  "Deploy smoke passed for configured health URL.",
  "Deploy smoke failed for configured health URL"
)

Assert-Text "development-readiness" $readinessDoc @(
  $completionCommand,
  "artifact verification, dry-run audit, backend artifact runtime smoke, frontend artifact render smoke, SSH auth, full transfer/restart, and health smoke are captured by one command"
)

Assert-Text "infra-sdd" $infraSdd @(
  $completionCommand,
  "explicit full Raspberry Pi completion gate",
  "builds and verifies the artifact",
  "artifact runtime smoke",
  "artifact frontend smoke",
  "transfers the artifact",
  "runs the configured HTTP health smoke"
)

Assert-Text "infra-bdd" $infraBdd @(
  $completionCommand,
  "full transfer, service restart, and HTTP health smoke"
)

Write-Output "# Completion Command Audit"
Write-Output ""
Write-Output "Checked: final Raspberry Pi completion command is documented."
Write-Output "Checked: local quality gate includes artifact runtime/frontend smokes before SSH and live deploy smoke."
Write-Output "Checked: local quality gate maps DeploySmokeHealthUrl to deploy script SmokeHealthUrl."
Write-Output ""
Write-Output "PASS: final deployment completion command is consistent across docs and scripts."
