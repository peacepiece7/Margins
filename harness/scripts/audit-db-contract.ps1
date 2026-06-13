$ErrorActionPreference = "Stop"

$schemaRoot = "db/schema"
$queryRoot = "db/queries"
$resetPath = "db/reset/001_reset_test_data.sql"
$seedPath = "db/seed/001_seed_mvp_data.sql"
$jdbcResetExecutorPath = "back/src/main/java/com/margins/testsupport/business/JdbcTestDataResetExecutor.java"

function Read-RequiredText {
  param([string] $Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    throw "Missing required file: $Path"
  }

  return Get-Content -LiteralPath $Path -Raw -Encoding UTF8
}

function Assert-Contains {
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

if (-not (Test-Path -LiteralPath $schemaRoot)) {
  throw "Missing schema directory: $schemaRoot"
}
if (-not (Test-Path -LiteralPath $queryRoot)) {
  throw "Missing query directory: $queryRoot"
}

$schemaText = (Get-ChildItem -LiteralPath $schemaRoot -Filter "*.sql" | Sort-Object Name | ForEach-Object {
  Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
}) -join "`n"
$resetText = Read-RequiredText -Path $resetPath
$seedText = Read-RequiredText -Path $seedPath
$jdbcResetExecutorText = Read-RequiredText -Path $jdbcResetExecutorPath
$timelineQuery = Read-RequiredText -Path (Join-Path $queryRoot "001_session_timeline.sql")
$windowMessagesQuery = Read-RequiredText -Path (Join-Path $queryRoot "002_window_messages.sql")
$metricQuery = Read-RequiredText -Path (Join-Path $queryRoot "004_metric_sources.sql")
$personaQuery = Read-RequiredText -Path (Join-Path $queryRoot "003_persona_trace.sql")

$requiredTables = @(
  "users",
  "books",
  "book_candidates",
  "reading_sessions",
  "session_windows",
  "personas",
  "questions",
  "messages",
  "metrics",
  "session_highlights",
  "session_tags",
  "session_insights"
)

foreach ($table in $requiredTables) {
  if ($schemaText -notmatch "CREATE TABLE IF NOT EXISTS\s+$table\b") {
    $failures.Add("Schema does not create required MVP table: $table") | Out-Null
  }
  if (-not $resetText.Contains("DELETE FROM $table WHERE is_test_data = TRUE;")) {
    $failures.Add("Reset script must delete only is_test_data rows for table: $table") | Out-Null
  }
}

Assert-Contains $failures "schema" $schemaText @(
  "deleted_at TIMESTAMP NULL",
  "is_test_data BOOLEAN NOT NULL DEFAULT FALSE",
  "parent_message_id BIGINT NULL",
  "persona_id BIGINT NULL",
  "question_id BIGINT NULL",
  "metric_details JSON NULL",
  "KEY idx_metrics_period",
  "reading_goal",
  "current_page",
  "target_page",
  "pinned BOOLEAN NOT NULL DEFAULT FALSE"
)

Assert-Contains $failures "reset" $resetText @(
  "SET FOREIGN_KEY_CHECKS = 0;",
  "SET FOREIGN_KEY_CHECKS = 1;",
  "SOURCE db/seed/001_seed_mvp_data.sql;"
)

Assert-Contains $failures "JDBC reset executor" $jdbcResetExecutorText @(
  "statement.execute(""SET FOREIGN_KEY_CHECKS = 0"")",
  "finally",
  "statement.execute(""SET FOREIGN_KEY_CHECKS = 1"")",
  "ScriptUtils.executeSqlScript"
)

Assert-Contains $failures "seed" $seedText @(
  "INSERT INTO personas",
  "INSERT INTO questions",
  "INSERT INTO messages",
  "INSERT INTO metrics",
  "is_test_data"
)

Assert-Contains $failures "timeline query" $timelineQuery @(
  "rs.deleted_at IS NULL",
  "sw.deleted_at IS NULL",
  "m.deleted_at IS NULL",
  "h.deleted_at IS NULL",
  "persona_display_name",
  "question_id"
)

Assert-Contains $failures "window messages query" $windowMessagesQuery @(
  "JOIN session_windows sw ON sw.id = m.window_id AND sw.deleted_at IS NULL",
  "m.window_id = ?",
  "m.deleted_at IS NULL",
  "persona_display_name",
  "question_id",
  "ORDER BY m.message_order, m.id"
)

Assert-Contains $failures "persona trace query" $personaQuery @(
  "persona_id",
  "persona_name",
  "persona_display_name",
  "system_prompt"
)

Assert-Contains $failures "metric source query" $metricQuery @(
  "rs.deleted_at IS NULL",
  "b.deleted_at IS NULL",
  "sw.deleted_at IS NULL",
  "q.deleted_at IS NULL",
  "FROM session_windows qsw",
  "qsw.id = q.window_id",
  "qsw.deleted_at IS NULL",
  "h.deleted_at IS NULL",
  "m.deleted_at IS NULL",
  "FROM session_windows msw",
  "msw.id = m.window_id",
  "msw.deleted_at IS NULL",
  "window_count",
  "question_count",
  "answered_question_count",
  "highlight_count",
  "message_count",
  "persona_count",
  "pages_read_estimate"
)

Write-Output "# DB Contract Audit"
Write-Output ""
Write-Output "Schema files checked: $((Get-ChildItem -LiteralPath $schemaRoot -Filter "*.sql").Count)"
Write-Output "Required MVP tables checked: $($requiredTables.Count)"
Write-Output "Query files checked: 4"
Write-Output "Reset script checked: $resetPath"
Write-Output "JDBC reset executor checked: $jdbcResetExecutorPath"

if ($failures.Count -gt 0) {
  Write-Output ""
  Write-Output "Failures:"
  $failures | ForEach-Object { Write-Output "- $_" }
  exit 1
}

Write-Output ""
Write-Output "PASS: DB schema, seed, query, and reset contracts are consistent."
