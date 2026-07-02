param(
  [switch] $FullStackE2E,
  [switch] $VisualScreenshots,
  [switch] $DeploymentPreflight,
  [switch] $ArtifactRuntimeSmoke,
  [switch] $ArtifactFrontendSmoke,
  [switch] $SshPreflight,
  [switch] $LiveDeploySmoke,
  [string] $DeploySmokeHealthUrl = $env:MARGINS_DEPLOY_HEALTH_URL,
  [switch] $SkipBackend,
  [switch] $SkipFrontendBuild
)

$ErrorActionPreference = "Stop"

if ($SshPreflight -and -not $DeploymentPreflight) {
  throw "-SshPreflight requires -DeploymentPreflight so the release artifact is built and verified first."
}

if ($ArtifactRuntimeSmoke -and -not $DeploymentPreflight) {
  throw "-ArtifactRuntimeSmoke requires -DeploymentPreflight so the release artifact is built first."
}

if ($ArtifactFrontendSmoke -and -not $DeploymentPreflight) {
  throw "-ArtifactFrontendSmoke requires -DeploymentPreflight so the release artifact is built first."
}

if ($LiveDeploySmoke -and -not ($DeploymentPreflight -and $SshPreflight)) {
  throw "-LiveDeploySmoke requires -DeploymentPreflight -SshPreflight so artifact verification and SSH auth run first."
}

if ($LiveDeploySmoke -and -not $DeploySmokeHealthUrl) {
  throw "-LiveDeploySmoke requires -DeploySmokeHealthUrl or MARGINS_DEPLOY_HEALTH_URL."
}

function Invoke-Step {
  param(
    [string] $Name,
    [scriptblock] $Command
  )

  Write-Output ""
  Write-Output "## $Name"
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Name failed with exit code $LASTEXITCODE"
  }
}

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
$frontRoot = Join-Path $repoRoot "front"

Push-Location $repoRoot
try {
  Invoke-Step "MVP readiness audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-mvp-readiness.ps1"
  }

  Invoke-Step "Documentation consistency audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-doc-consistency.ps1"
  }

  Invoke-Step "DB contract audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-db-contract.ps1"
  }

  Invoke-Step "Live deploy safety guard audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-live-deploy-guard.ps1"
  }

  Invoke-Step "Artifact secret guard audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-artifact-secret-guard.ps1"
  }

  Invoke-Step "CI workflow audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-ci-workflow.ps1"
  }

  Invoke-Step "Completion command audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-completion-command.ps1"
  }

  Invoke-Step "Quality gate composition audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-quality-gate-composition.ps1"
  }

  Invoke-Step "Acceptance traceability audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-acceptance-traceability.ps1"
  }

  Invoke-Step "Full-stack E2E runner audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-fullstack-e2e-runner.ps1"
  }

  Invoke-Step "Cross-platform script audit" {
    node "harness/scripts/audit-cross-platform-scripts.mjs"
  }

  Invoke-Step "Final acceptance boundary audit" {
    Invoke-PowerShellFile -Path "harness/scripts/audit-final-acceptance.ps1"
  }

  if (-not $SkipBackend) {
    Invoke-Step "Backend tests" {
      Invoke-PowerShellFile -Path "back/scripts/test.ps1"
    }
  }

  Push-Location $frontRoot
  try {
    Invoke-Step "Frontend unit tests" {
      npm run test:unit
    }

    if (-not $SkipFrontendBuild) {
      Invoke-Step "Frontend production build" {
        npm run build
      }

      Invoke-Step "Frontend production selector check" {
        npm run verify:production-selectors
      }
    }

    if ($FullStackE2E) {
      Invoke-Step "Frontend full-stack E2E" {
        Pop-Location
        try {
          Invoke-PowerShellFile -Path "harness/scripts/run-fullstack-e2e.ps1"
        }
        finally {
          Push-Location $frontRoot
        }
      }
    }

    if ($VisualScreenshots) {
      Invoke-Step "Frontend visual screenshots" {
        npm run screenshots:workbench
      }
    }
  }
  finally {
    Pop-Location
  }

  if ($DeploymentPreflight) {
    Invoke-Step "Build release artifact" {
      Invoke-PowerShellFile -Path "infra/scripts/build-artifacts.ps1" -Arguments @("-SkipTests")
    }

    Invoke-Step "Release artifact verification" {
      Invoke-PowerShellFile -Path "infra/scripts/verify-artifacts.ps1"
    }

    Invoke-Step "Raspberry Pi deploy dry-run" {
      Invoke-PowerShellFile -Path "harness/scripts/audit-deploy-dry-run.ps1" -Arguments @("-ArtifactPath", "infra/artifacts/margins-release.zip")
    }

    if ($ArtifactRuntimeSmoke) {
      Invoke-Step "Release artifact runtime smoke" {
        Invoke-PowerShellFile -Path "harness/scripts/audit-release-artifact-runtime.ps1" -Arguments @("-ArtifactPath", "infra/artifacts/margins-release.zip")
      }
    }

    if ($ArtifactFrontendSmoke) {
      Invoke-Step "Release artifact frontend smoke" {
        Invoke-PowerShellFile -Path "harness/scripts/audit-release-artifact-frontend.ps1" -Arguments @("-ArtifactPath", "infra/artifacts/margins-release.zip")
      }
    }

    if ($SshPreflight) {
      Invoke-Step "Raspberry Pi SSH preflight" {
        Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @("-ArtifactPath", "infra/artifacts/margins-release.zip", "-SshPreflight")
      }
    }

    if ($LiveDeploySmoke) {
      Invoke-Step "Raspberry Pi live deploy smoke" {
        Invoke-PowerShellFile -Path "infra/scripts/deploy-raspberry-pi.ps1" -Arguments @(
          "-ArtifactPath", "infra/artifacts/margins-release.zip",
          "-SmokeHealthUrl", $DeploySmokeHealthUrl
        )
      }
    } elseif ($DeploySmokeHealthUrl -and -not $LiveDeploySmoke) {
      Write-Output "Raspberry Pi deploy smoke URL is configured, but live deploy smoke is skipped without -LiveDeploySmoke."
    }
  }

  Write-Output ""
  Write-Output "PASS: local quality gate completed."
}
finally {
  Pop-Location
}
