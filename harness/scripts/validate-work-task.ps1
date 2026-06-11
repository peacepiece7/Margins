param(
  [Parameter(Mandatory = $true)]
  [string] $TaskId,

  [string] $WorkRoot = "harness/work",
  [switch] $AllowOpenDecisions
)

$ErrorActionPreference = "Stop"

$safeTaskId = ($TaskId -replace '[^A-Za-z0-9._-]', '-').Trim('-')
if ([string]::IsNullOrWhiteSpace($safeTaskId)) {
  throw "TaskId must contain at least one filename-safe character."
}

$taskDir = Join-Path $WorkRoot $safeTaskId
if (-not (Test-Path -LiteralPath $taskDir)) {
  throw "Missing task directory: $taskDir"
}

$required = @(
  "task-packet.md",
  "work-status.md",
  "handoff-log.md",
  "verification-report.md",
  "discussion-log.md",
  "owner-decisions.md",
  "requirements-brief.md"
)

$missing = @()
foreach ($name in $required) {
  $path = Join-Path $taskDir $name
  if (-not (Test-Path -LiteralPath $path)) {
    $missing += $name
  }
}

if ($missing.Count -gt 0) {
  throw "Missing required work files: $($missing -join ', ')"
}

$ownerDecisionsPath = Join-Path $taskDir "owner-decisions.md"
$ownerDecisions = Get-Content -LiteralPath $ownerDecisionsPath -Raw
if (-not $AllowOpenDecisions -and $ownerDecisions -match "(?m)^\s*-\s*Status:\s*open\s*$") {
  throw "Open owner decisions remain in $ownerDecisionsPath. Use -AllowOpenDecisions only before irreversible work."
}

Write-Output "PASS: $taskDir"
