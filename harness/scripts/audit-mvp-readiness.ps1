param(
  [switch] $Strict
)

$ErrorActionPreference = "Stop"

$checks = @(
  @{
    Id = "book-search-add"
    Requirement = "AI book candidate search and book add"
    Status = "partial"
    Evidence = @(
      "docs/project/mvp.md",
      "docs/back/sdd.md",
      "back/src/main/java/com/margins/book/controller/BookController.java",
      "back/src/main/java/com/margins/ai/PlaceholderAiProvider.java",
      "front/src/repository/marginsRepository.ts",
      "front/src/components/views/SessionWorkbench.tsx"
    )
    Gap = "OpenAI live provider is not wired; placeholder provider is used."
    NextSlice = "openai-provider"
  },
  @{
    Id = "reading-session"
    Requirement = "Book-based ReadingSession creation"
    Status = "implemented"
    Evidence = @(
      "back/src/main/java/com/margins/session/controller/ReadingSessionController.java",
      "back/src/main/java/com/margins/session/mapper/ReadingSessionMapper.java",
      "back/src/test/java/com/margins/ReadingSessionBusinessPersistenceTest.java",
      "front/tests/e2e/session-workbench.spec.ts"
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "session-window"
    Requirement = "SessionWindow creation inside a session"
    Status = "implemented"
    Evidence = @(
      "back/src/main/java/com/margins/session/controller/SessionWindowController.java",
      "back/src/main/java/com/margins/session/mapper/SessionWindowMapper.java",
      "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java",
      "front/tests/e2e/session-workbench.spec.ts"
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "ai-question-answer"
    Requirement = "AI question/answer per window"
    Status = "partial"
    Evidence = @(
      "back/src/main/java/com/margins/ai/AiProvider.java",
      "back/src/main/java/com/margins/session/business/SessionWindowBusiness.java",
      "back/src/main/java/com/margins/message/mapper/MessageMapper.java",
      "front/src/repository/marginsRepository.ts",
      "front/tests/e2e/session-workbench.spec.ts"
    )
    Gap = "Responses are persisted through the AiProvider boundary, but runtime OpenAI and streaming transport are deferred."
    NextSlice = "openai-provider-streaming-shape"
  },
  @{
    Id = "persona-debate"
    Requirement = "Persona-based AI response in debate window"
    Status = "partial"
    Evidence = @(
      "db/schema/001_create_mvp_schema.sql",
      "back/src/main/java/com/margins/session/dto/DebateMessageRequest.java",
      "back/src/main/java/com/margins/session/business/SessionWindowBusiness.java",
      "front/tests/e2e/session-workbench.spec.ts"
    )
    Gap = "Persona id is persisted and displayed, but prompt assembly still uses placeholder AI provider."
    NextSlice = "persona-context-openai-provider"
  },
  @{
    Id = "conversation-persistence"
    Requirement = "All conversations and records saved to DB"
    Status = "implemented"
    Evidence = @(
      "db/schema/001_create_mvp_schema.sql",
      "back/src/main/java/com/margins/message/mapper/MessageMapper.java",
      "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java",
      "harness/owner/reports/2026-06-12-mvp-backend-persistence-slice.md"
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "metric-ready-schema"
    Requirement = "Metric/statistics-ready DB design"
    Status = "implemented"
    Evidence = @(
      "docs/db/sdd.md",
      "db/schema/001_create_mvp_schema.sql",
      "db/queries/004_metric_sources.sql",
      "harness/owner/reports/2026-06-12-mvp-db-schema.md"
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "auth"
    Requirement = "Single-user or simple JWT initial auth"
    Status = "partial"
    Evidence = @(
      "docs/back/sdd.md",
      "back/src/main/java/com/margins/auth/controller/AuthController.java",
      "back/src/main/java/com/margins/auth/business/AuthBusiness.java"
    )
    Gap = "First runnable slice uses single-user mode; durable JWT security filter is not implemented."
    NextSlice = "jwt-auth-hardening"
  },
  @{
    Id = "socket-streaming"
    Requirement = "Socket for AI streaming and real-time session window delivery"
    Status = "planned"
    Evidence = @(
      "docs/back/sdd.md",
      "docs/front/sdd.md"
    )
    Gap = "Socket/SSE runtime is not implemented; only event and DTO shape are documented."
    NextSlice = "streaming-transport"
  },
  @{
    Id = "raspberry-pi-deploy"
    Requirement = "Raspberry Pi deploy flow"
    Status = "blocked"
    Evidence = @(
      "docs/infra/sdd.md",
      "infra/docker/mysql-compose.yml",
      "harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md"
    )
    Gap = "Raspberry Pi target, SSH user, deployment directory, and service manager are owner-provided inputs."
    NextSlice = "deploy-packaging-after-owner-input"
  }
)

$failed = $false
Write-Output "# MVP Readiness Audit"
Write-Output ""
foreach ($check in $checks) {
  $missing = @()
  foreach ($path in $check.Evidence) {
    if (-not (Test-Path -LiteralPath $path)) {
      $missing += $path
    }
  }

  if ($missing.Count -gt 0) {
    $failed = $true
  }

  Write-Output "## $($check.Id)"
  Write-Output "- Requirement: $($check.Requirement)"
  Write-Output "- Status: $($check.Status)"
  if ($check.Gap) { Write-Output "- Gap: $($check.Gap)" }
  if ($check.NextSlice) { Write-Output "- Next slice: $($check.NextSlice)" }
  if ($missing.Count -gt 0) {
    Write-Output "- Missing evidence:"
    $missing | ForEach-Object { Write-Output "  - $_" }
  } else {
    Write-Output "- Evidence: present"
  }
  Write-Output ""
}

if ($Strict) {
  $notReady = @($checks | Where-Object { $_.Status -in @("partial", "planned", "blocked") })
  if ($notReady.Count -gt 0) {
    $failed = $true
    Write-Output "Strict readiness failed for: $($notReady.Id -join ', ')"
  }
}

if ($failed) {
  exit 1
}

Write-Output "PASS: MVP readiness evidence paths are present."
