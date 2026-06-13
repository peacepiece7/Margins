param(
  [string] $WorkRoot = "harness/work",
  [string] $RegistryPath = "harness/work/registry.md",
  [string] $OwnerReportsRoot = "harness/owner/reports",
  [string] $DocsRoot = "docs",
  [switch] $AllowEmptyUnregisteredDirectories
)

$ErrorActionPreference = "Stop"

$requiredWorkFiles = @(
  "task-packet.md",
  "work-status.md",
  "handoff-log.md",
  "verification-report.md",
  "discussion-log.md",
  "owner-decisions.md",
  "requirements-brief.md"
)

function Get-RegistryTaskRows {
  param([string] $Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    throw "Missing registry: $Path"
  }

  $rows = @()
  foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
    if ($line -notmatch '^\| `([^`]+)` \|') {
      continue
    }

    $cells = $line -split '\|'
    if ($cells.Count -lt 10) {
      continue
    }

    $taskId = $matches[1]
    $workDir = ($cells[6].Trim() -replace '^`|`$', '')
    $ownerReport = ($cells[9].Trim() -replace '^`|`$', '')
    $rows += [pscustomobject]@{
      TaskId = $taskId
      WorkDir = $workDir
      OwnerReport = $ownerReport
    }
  }

  return $rows
}

$failures = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

$registryRows = @(Get-RegistryTaskRows -Path $RegistryPath)
$registeredIds = @($registryRows | ForEach-Object { $_.TaskId })

$workDirs = @()
if (Test-Path -LiteralPath $WorkRoot) {
  $workDirs = @(Get-ChildItem -LiteralPath $WorkRoot -Directory | Sort-Object Name)
}

foreach ($dir in $workDirs) {
  $missing = @()
  foreach ($fileName in $requiredWorkFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $dir.FullName $fileName))) {
      $missing += $fileName
    }
  }

  if ($registeredIds -notcontains $dir.Name) {
    $childItems = @(Get-ChildItem -LiteralPath $dir.FullName -Force)
    if ($AllowEmptyUnregisteredDirectories -and $childItems.Count -eq 0) {
      $warnings.Add("Empty unregistered work directory: $($dir.FullName)") | Out-Null
    } else {
      $failures.Add("Work directory is not registered: $($dir.Name)") | Out-Null
    }
  }

  if ($missing.Count -gt 0) {
    $failures.Add("Work directory $($dir.Name) missing files: $($missing -join ', ')") | Out-Null
  }
}

foreach ($row in $registryRows) {
  if (-not (Test-Path -LiteralPath $row.WorkDir)) {
    $failures.Add("Registry row $($row.TaskId) points to missing work dir: $($row.WorkDir)") | Out-Null
  }

  if ($row.OwnerReport -and $row.OwnerReport -ne "none" -and -not (Test-Path -LiteralPath $row.OwnerReport)) {
    $failures.Add("Registry row $($row.TaskId) points to missing owner report: $($row.OwnerReport)") | Out-Null
  }
}

if (Test-Path -LiteralPath $OwnerReportsRoot) {
  foreach ($report in Get-ChildItem -LiteralPath $OwnerReportsRoot -Filter "*.md" -File | Where-Object { $_.Name -ne "README.md" }) {
    $text = Get-Content -LiteralPath $report.FullName -Raw -Encoding UTF8
    $taskMatch = [regex]::Match($text, '(?s)## Task ID\s*\r?\n\s*-\s*([^\r\n]+)')
    if (-not $taskMatch.Success) {
      $failures.Add("Owner report missing Task ID: $($report.FullName)") | Out-Null
      continue
    }

    $taskId = $taskMatch.Groups[1].Value.Trim()
    if (-not (Test-Path -LiteralPath (Join-Path $WorkRoot $taskId))) {
      $failures.Add("Owner report $($report.Name) points to missing task: $taskId") | Out-Null
    }
  }
}

if (Test-Path -LiteralPath $DocsRoot) {
  foreach ($domain in Get-ChildItem -LiteralPath $DocsRoot -Directory | Where-Object { $_.Name -ne "_templates" }) {
    foreach ($requiredDoc in @("sdd.md", "bdd.md")) {
      if (-not (Test-Path -LiteralPath (Join-Path $domain.FullName $requiredDoc))) {
        $failures.Add("Docs domain $($domain.Name) missing $requiredDoc") | Out-Null
      }
    }
  }
}

$projectMvpPath = Join-Path $DocsRoot "project/mvp.md"
if (Test-Path -LiteralPath $projectMvpPath) {
  $projectMvp = Get-Content -LiteralPath $projectMvpPath -Raw -Encoding UTF8
  foreach ($requiredText in @("Book search and add", "DB persistence for every meaningful conversation", "AI-proposed candidates", "Raspberry Pi is the deployment target")) {
    if (-not $projectMvp.Contains($requiredText)) {
      $failures.Add("Project MVP document is stale or unreadable; missing '$requiredText'.") | Out-Null
    }
  }

  $mojibakeMarkers = @(
    ([char]0xFFFD).ToString(),
    ([char]0xF9E2).ToString(),
    ([char]0x7B4C).ToString()
  )
  foreach ($mojibakeText in $mojibakeMarkers) {
    if ($projectMvp.Contains($mojibakeText)) {
      $failures.Add("Project MVP document appears to contain mojibake text: $mojibakeText") | Out-Null
    }
  }
}

$projectSddPath = Join-Path $DocsRoot "project/sdd.md"
$backSddPath = Join-Path $DocsRoot "back/sdd.md"
$authFilterPath = "back/src/main/java/com/margins/auth/filter/AuthTokenFilter.java"
$jwtServicePath = "back/src/main/java/com/margins/auth/service/JwtTokenService.java"
if ((Test-Path -LiteralPath $projectSddPath) -and (Test-Path -LiteralPath $backSddPath)) {
  $projectSdd = Get-Content -LiteralPath $projectSddPath -Raw -Encoding UTF8
  $backSdd = Get-Content -LiteralPath $backSddPath -Raw -Encoding UTF8
  $jwtImplementationExists = (Test-Path -LiteralPath $authFilterPath) -and (Test-Path -LiteralPath $jwtServicePath)

  if ($jwtImplementationExists -and $backSdd.Contains("HMAC-signed bearer JWT")) {
    foreach ($requiredText in @("/api/auth/login", "HMAC-signed bearer token", "/api/**")) {
      if (-not $projectSdd.Contains($requiredText)) {
        $failures.Add("Project SDD auth decision is stale or incomplete; missing '$requiredText' while JWT auth is implemented.") | Out-Null
      }
    }

    if ($projectSdd.Contains("JWT remains later-compatible")) {
      $failures.Add("Project SDD auth decision still says JWT remains later-compatible, but JWT auth is implemented.") | Out-Null
    }
  }
}

Write-Output "# Documentation Consistency Audit"
Write-Output ""
Write-Output "Work directories checked: $($workDirs.Count)"
Write-Output "Registry rows checked: $($registryRows.Count)"

if ($warnings.Count -gt 0) {
  Write-Output ""
  Write-Output "Warnings:"
  $warnings | ForEach-Object { Write-Output "- $_" }
}

if ($failures.Count -gt 0) {
  Write-Output ""
  Write-Output "Failures:"
  $failures | ForEach-Object { Write-Output "- $_" }
  exit 1
}

Write-Output ""
Write-Output "PASS: documentation indexes and work records are consistent."
