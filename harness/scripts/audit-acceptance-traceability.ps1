$ErrorActionPreference = "Stop"

function Evidence {
  param(
    [string] $Path,
    [string[]] $Needles
  )

  return @{
    Path = $Path
    Needles = $Needles
  }
}

function Test-Evidence {
  param(
    [hashtable] $Evidence
  )

  $issues = New-Object System.Collections.Generic.List[string]
  $path = $Evidence.Path

  if (-not (Test-Path -LiteralPath $path)) {
    $issues.Add("Missing evidence path: $path") | Out-Null
    return $issues
  }

  $text = Get-Content -LiteralPath $path -Raw -Encoding UTF8
  foreach ($needle in $Evidence.Needles) {
    if (-not $text.Contains($needle)) {
      $issues.Add("$path missing required text: $needle") | Out-Null
    }
  }

  return $issues
}

$traces = @(
  @{
    Id = "book-search-add"
    Requirement = "Book search and add"
    Evidence = @(
      Evidence "docs/project/mvp.md" @("Book search and add", "A user can add a book from external or AI-fallback candidates")
      Evidence "docs/project/bdd.md" @("User completes the first useful reading record", "searches for a book", "selects one external or AI-fallback candidate")
      Evidence "docs/back/sdd.md" @("/api/books/search-candidates", "OpenLibraryBookSearchProvider", "/api/books")
      Evidence "docs/front/sdd.md" @("GET /api/books", "book-search")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-search-input", "book-candidate-save")
      Evidence "back/src/test/java/com/margins/BookBusinessPersistenceTest.java" @("SaveBookRequest", "saveBook")
    )
  },
  @{
    Id = "reading-session"
    Requirement = "Book-based ReadingSession creation"
    Evidence = @(
      Evidence "docs/project/mvp.md" @('Book-based `ReadingSession` creation', "A user can start a reading session for a saved book")
      Evidence "docs/project/bdd.md" @("starts a reading session for that book", "book, session, window")
      Evidence "docs/back/sdd.md" @("/api/reading-sessions", "Create session for a book")
      Evidence "docs/front/sdd.md" @("reading session", "ReadingPortal")
      Evidence "back/src/test/java/com/margins/ReadingSessionBusinessPersistenceTest.java" @("CreateReadingSessionRequest", "ReadingSession")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-start-review", "api/reading-sessions")
    )
  },
  @{
    Id = "session-window"
    Requirement = "SessionWindow creation inside a session"
    Evidence = @(
      Evidence "docs/project/mvp.md" @('`SessionWindow` creation inside a session', "multiple windows")
      Evidence "docs/back/sdd.md" @("/api/session-windows", "Create session window")
      Evidence "docs/front/bdd.md" @("session window", "reflection window")
      Evidence "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java" @("CreateSessionWindowRequest", "SessionWindow")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-start-review", "debate-enter-submit")
    )
  },
  @{
    Id = "ai-question-answer"
    Requirement = "AI question and answer flow per window"
    Evidence = @(
      Evidence "docs/project/mvp.md" @("AI question and answer flow per window", "AI questions and answers are stored as messages")
      Evidence "docs/project/bdd.md" @("answers an AI-generated question in a session window", "AI response are persisted")
      Evidence "docs/back/sdd.md" @("/api/session-windows/{id}/questions/generate", "/api/session-windows/{id}/messages/stream")
      Evidence "docs/front/sdd.md" @("message.delta", "message.done")
      Evidence "back/src/test/java/com/margins/SessionControllerValidationTest.java" @("messages/stream", "message.delta")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-generate-questions", "question-answer-form")
    )
  },
  @{
    Id = "persona-debate"
    Requirement = "Persona-based AI response in a debate window"
    Evidence = @(
      Evidence "docs/project/mvp.md" @('`Persona`-based AI response in a debate window', "Persona debate messages")
      Evidence "docs/project/bdd.md" @("Persona Debate Flow", "persona response is stored")
      Evidence "docs/back/sdd.md" @("/api/session-windows/{id}/debate", "/api/personas")
      Evidence "docs/front/bdd.md" @("persona", "debate")
      Evidence "back/src/test/java/com/margins/PersonaBusinessTest.java" @("persona", "Persona")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("debate-session-submit", "debate-message-list")
    )
  },
  @{
    Id = "conversation-persistence"
    Requirement = "Persist every meaningful conversation and reading record"
    Evidence = @(
      Evidence "docs/project/mvp.md" @("DB persistence for every meaningful conversation and reading record")
      Evidence "docs/project/bdd.md" @("book, session, window, user message, and AI response are persisted", "same record can be reloaded later")
      Evidence "docs/db/sdd.md" @("messages", "session_windows", "questions")
      Evidence "docs/back/bdd.md" @("final user and assistant messages are persisted")
      Evidence "back/src/test/java/com/margins/MessageBusinessPersistenceTest.java" @("message", "streamingStatus")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("page.reload()", "message-list")
    )
  },
  @{
    Id = "metric-ready-schema"
    Requirement = "Metric/statistics-ready DB design"
    Evidence = @(
      Evidence "docs/project/mvp.md" @('future `Metric` and statistics expansion', "metric period")
      Evidence "docs/project/bdd.md" @("Metric-Ready Data", "store metric output without changing raw messages")
      Evidence "docs/db/sdd.md" @("metrics", "metric_details")
      Evidence "db/schema/001_create_mvp_schema.sql" @("CREATE TABLE IF NOT EXISTS metrics", "metric_details")
      Evidence "db/queries/004_metric_sources.sql" @("answered_question_count", "message_count")
      Evidence "back/src/test/java/com/margins/MetricBusinessTest.java" @("createSessionSnapshot", "session_snapshot")
    )
  },
  @{
    Id = "simple-jwt-auth"
    Requirement = "Single-user-compatible JWT auth"
    Evidence = @(
      Evidence "docs/project/mvp.md" @("single-user mode or simple JWT login", "Social login")
      Evidence "docs/project/sdd.md" @("simple JWT login", "social login remains out of MVP scope")
      Evidence "docs/back/sdd.md" @("POST /api/auth/login", "AuthTokenFilter")
      Evidence "back/src/test/java/com/margins/AuthTokenFilterTest.java" @("Bearer", "HttpHeaders.AUTHORIZATION")
      Evidence "back/src/test/java/com/margins/OpenApiContractTest.java" @("protectedApiRejectsAnonymousRequestsInFullContext", "loginIssuedTokenPassesFullContextAuthFilter")
      Evidence "front/src/components/views/LoginGate.tsx" @("login-submit", "margins.auth")
    )
  },
  @{
    Id = "ai-streaming"
    Requirement = "Streaming AI delivery"
    Evidence = @(
      Evidence "docs/project/mvp.md" @("Streaming should be supported", "session-window response delivery")
      Evidence "docs/project/sdd.md" @('SSE over `POST /api/session-windows/{id}/messages/stream`')
      Evidence "docs/back/sdd.md" @("message.start", "message.delta", "message.done", "message.error")
      Evidence "docs/front/sdd.md" @("message.start", "message.delta", "message.done", "message.error")
      Evidence "back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java" @('\"stream\":true', "streamsOpenAiResponseDeltasWhenConfigured")
      Evidence "front/src/repository/marginsRepository.test.ts" @("message.delta", "message.done", "message.error")
    )
  },
  @{
    Id = "e2e-reset"
    Requirement = "Deterministic E2E reset"
    Evidence = @(
      Evidence "docs/project/bdd.md" @("E2E Reset", "test-owned records are removed or restored to seed state")
      Evidence "docs/db/bdd.md" @("reset", "is_test_data")
      Evidence "db/reset/001_reset_test_data.sql" @("is_test_data", "DELETE")
      Evidence "back/src/main/java/com/margins/testsupport/business/JdbcTestDataResetExecutor.java" @("001_seed_mvp_data.sql", "DELETE FROM users WHERE is_test_data = TRUE", "finally", "SET FOREIGN_KEY_CHECKS = 1")
      Evidence "back/src/test/java/com/margins/TestResetBusinessTest.java" @("jdbcResetReenablesForeignKeyChecksWhenDeleteFails")
      Evidence "front/scripts/check-e2e-prereqs.mjs" @("/api/test/reset", "SPRING_PROFILES_ACTIVE=local or test")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("api/test/reset")
    )
  },
  @{
    Id = "deployment-acceptance"
    Requirement = "Raspberry Pi completion boundary"
    Evidence = @(
      Evidence "docs/project/development-readiness.md" @("Raspberry Pi live transfer/restart", "verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl")
      Evidence "docs/infra/sdd.md" @("Raspberry Pi", "Deployment Preflight Boundary")
      Evidence "docs/infra/bdd.md" @("Local quality gate can run explicit live deploy smoke", "Quality gate composition stays aligned")
      Evidence "harness/scripts/audit-release-artifact-runtime.ps1" @("Release Artifact Runtime Audit", "PASS: release artifact backend runtime smoke passed.")
      Evidence "harness/scripts/audit-release-artifact-frontend.ps1" @("Release Artifact Frontend Audit", "PASS: release artifact frontend production smoke passed.")
      Evidence "infra/scripts/deploy-raspberry-pi.ps1" @("SshPreflight", "SmokeHealthUrl", "Deploy smoke passed for configured health URL")
      Evidence "harness/scripts/audit-completion-command.ps1" @("Completion Command Audit", "DeploySmokeHealthUrl")
      Evidence "harness/scripts/audit-live-deploy-guard.ps1" @("Live Deploy Guard Audit", "LiveDeploySmoke without preflight")
    )
  }
)

$failed = $false

Write-Output "# Acceptance Traceability Audit"
Write-Output ""

foreach ($trace in $traces) {
  $issues = New-Object System.Collections.Generic.List[string]
  foreach ($item in $trace.Evidence) {
    $itemIssues = @(Test-Evidence -Evidence $item)
    foreach ($issue in $itemIssues) {
      $issues.Add($issue) | Out-Null
    }
  }

  if ($issues.Count -gt 0) {
    $failed = $true
  }

  Write-Output "## $($trace.Id)"
  Write-Output "- Requirement: $($trace.Requirement)"
  if ($issues.Count -gt 0) {
    Write-Output "- Trace failures:"
    $issues | ForEach-Object { Write-Output "  - $_" }
  } else {
    Write-Output "- Trace: planning, design, BDD, and verification evidence present"
  }
  Write-Output ""
}

if ($failed) {
  Write-Output "FAIL: acceptance traceability has missing or weak evidence."
  exit 1
}

Write-Output "PASS: MVP acceptance traceability is connected across planning, design, BDD, implementation, and tests."
