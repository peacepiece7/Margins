param(
  [string[]] $Domains = @("project"),
  [string] $TaskId,
  [string] $WorkRoot = "harness/work",
  [switch] $IncludeStatus
)

$ErrorActionPreference = "Stop"

$sources = New-Object System.Collections.Generic.List[string]
$sources.Add("AGENTS.md")
$sources.Add("harness/AGENTS.md")
$sources.Add("harness/README.md")
$sources.Add("harness/process.md")
$sources.Add("harness/handoffs.md")
$sources.Add("harness/sub-agents.md")
$sources.Add("harness/owner/README.md")
$sources.Add("harness/owner/dashboard.md")
$sources.Add("harness/work/registry.md")

$expandedDomains = foreach ($domainValue in $Domains) {
  $domainValue -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ }
}

foreach ($domain in $expandedDomains) {
  $sdd = "docs/$domain/sdd.md"
  $bdd = "docs/$domain/bdd.md"
  if (Test-Path -LiteralPath $sdd) { $sources.Add($sdd) }
  if (Test-Path -LiteralPath $bdd) { $sources.Add($bdd) }
}

if (-not [string]::IsNullOrWhiteSpace($TaskId)) {
  $safeTaskId = ($TaskId -replace '[^A-Za-z0-9._-]', '-').Trim('-')
  $taskDir = Join-Path $WorkRoot $safeTaskId
  foreach ($name in @("task-packet.md", "work-status.md", "handoff-log.md", "verification-report.md", "discussion-log.md", "owner-decisions.md", "requirements-brief.md")) {
    $path = Join-Path $taskDir $name
    if (Test-Path -LiteralPath $path) { $sources.Add($path) }
  }
}

foreach ($ownerDir in @("harness/owner/decisions", "harness/owner/requests", "harness/owner/reports")) {
  if (Test-Path -LiteralPath $ownerDir) {
    Get-ChildItem -LiteralPath $ownerDir -Filter "*.md" -File | ForEach-Object {
      $sources.Add($_.FullName)
    }
  }
}

Write-Output "# Context Sources"
$sources | Sort-Object -Unique | ForEach-Object { Write-Output "- $_" }

if ($IncludeStatus) {
  Write-Output ""
  Write-Output "# Git Status"
  git status --short
}
