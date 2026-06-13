$ErrorActionPreference = "Stop"

$previousHealthUrl = [Environment]::GetEnvironmentVariable("MARGINS_DEPLOY_HEALTH_URL", "Process")
[Environment]::SetEnvironmentVariable("MARGINS_DEPLOY_HEALTH_URL", $null, "Process")

function Invoke-ExpectedFailure {
  param(
    [string] $Name,
    [string[]] $Arguments,
    [string] $ExpectedText
  )

  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $output = & powershell -NoProfile -ExecutionPolicy Bypass -File "harness\scripts\verify-local-quality.ps1" @Arguments 2>&1
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

try {
  Invoke-ExpectedFailure `
    -Name "LiveDeploySmoke without preflight" `
    -Arguments @("-LiveDeploySmoke", "-SkipBackend", "-SkipFrontendBuild") `
    -ExpectedText "-LiveDeploySmoke requires -DeploymentPreflight -SshPreflight"

  Invoke-ExpectedFailure `
    -Name "LiveDeploySmoke without health URL" `
    -Arguments @("-DeploymentPreflight", "-SshPreflight", "-LiveDeploySmoke", "-SkipBackend", "-SkipFrontendBuild") `
    -ExpectedText "-LiveDeploySmoke requires -DeploySmokeHealthUrl or MARGINS_DEPLOY_HEALTH_URL."

  Write-Output "# Live Deploy Guard Audit"
  Write-Output ""
  Write-Output "Checked: -LiveDeploySmoke cannot run without deployment preflight and SSH preflight."
  Write-Output "Checked: -LiveDeploySmoke cannot run without a configured health URL."
  Write-Output "Checked: process MARGINS_DEPLOY_HEALTH_URL is cleared during guard checks."
  Write-Output ""
  Write-Output "PASS: live deploy smoke guard prevents accidental Raspberry Pi transfer/restart."
}
finally {
  [Environment]::SetEnvironmentVariable("MARGINS_DEPLOY_HEALTH_URL", $previousHealthUrl, "Process")
}

exit 0
