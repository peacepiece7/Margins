param(
  [Parameter(Mandatory = $true)]
  [string] $TaskId,

  [string] $OutputRoot = "harness/work"
)

$ErrorActionPreference = "Stop"

$safeTaskId = ($TaskId -replace '[^A-Za-z0-9._-]', '-').Trim('-')
if ([string]::IsNullOrWhiteSpace($safeTaskId)) {
  throw "TaskId must contain at least one filename-safe character."
}

$taskDir = Join-Path $OutputRoot $safeTaskId
New-Item -ItemType Directory -Force -Path $taskDir | Out-Null

$templates = @{
  "task-packet.md" = "harness/templates/task-packet.md"
  "work-status.md" = "harness/templates/work-status.md"
  "handoff-log.md" = "harness/templates/handoff-log.md"
  "verification-report.md" = "harness/templates/verification-report.md"
  "discussion-log.md" = "harness/templates/discussion-log.md"
  "owner-decisions.md" = "harness/templates/owner-decision-request.md"
  "requirements-brief.md" = "harness/templates/requirements-brief.md"
}

foreach ($entry in $templates.GetEnumerator()) {
  if (-not (Test-Path -LiteralPath $entry.Value)) {
    throw "Missing template: $($entry.Value)"
  }

  $destination = Join-Path $taskDir $entry.Key
  if (-not (Test-Path -LiteralPath $destination)) {
    Copy-Item -LiteralPath $entry.Value -Destination $destination
  }

  $content = Get-Content -LiteralPath $destination -Raw -Encoding UTF8
  $content = $content -replace "(?m)(^## Task Id\r?\n\r?\n)-[^\S\r\n]*$", "`${1}- $safeTaskId"
  Set-Content -LiteralPath $destination -Value $content -Encoding UTF8
}

Write-Output $taskDir
