$ErrorActionPreference = "Stop"

$previousHealthUrl = [Environment]::GetEnvironmentVariable("MARGINS_DEPLOY_HEALTH_URL", "Process")
[Environment]::SetEnvironmentVariable("MARGINS_DEPLOY_HEALTH_URL", $null, "Process")

function Get-PowerShellExecutable {
  $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
  if ($pwsh) { return $pwsh.Source }

  $windowsPowerShell = Get-Command powershell -ErrorAction SilentlyContinue
  if ($windowsPowerShell) { return $windowsPowerShell.Source }

  throw "PowerShell executable not found. On macOS, use the Node npm commands documented in README.md; this legacy script is optional."
}

function Invoke-ExpectedFailure {
  param(
    [string] $Name,
    [string[]] $Arguments,
    [string] $ExpectedText
  )

  $previousErrorActionPreference = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $powerShell = Get-PowerShellExecutable
    $powerShellArgs = @("-NoProfile")
    if ((Split-Path -Leaf $powerShell) -ieq "powershell.exe" -or (Split-Path -Leaf $powerShell) -ieq "powershell") {
      $powerShellArgs += @("-ExecutionPolicy", "Bypass")
    }
    $powerShellArgs += @("-File", "harness/scripts/verify-local-quality.ps1")
    $powerShellArgs += $Arguments

    $output = & $powerShell @powerShellArgs 2>&1
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
    -ExpectedText "-LiveDeploySmoke requires -DeploySmokeHealthUrl"

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
