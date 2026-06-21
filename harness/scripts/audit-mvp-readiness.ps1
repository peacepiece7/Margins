param(
  [switch] $Strict
)

$ErrorActionPreference = "Stop"

function Evidence {
  param(
    [string] $Path,
    [string[]] $Contains = @()
  )

  return [pscustomobject]@{
    Path = $Path
    Contains = $Contains
  }
}

function Test-Evidence {
  param(
    [pscustomobject] $Evidence
  )

  $issues = New-Object System.Collections.Generic.List[string]
  if (-not (Test-Path -LiteralPath $Evidence.Path)) {
    $issues.Add("missing path: $($Evidence.Path)") | Out-Null
    return $issues
  }

  if ($Evidence.Contains.Count -gt 0) {
    $text = Get-Content -LiteralPath $Evidence.Path -Raw -Encoding UTF8
    foreach ($needle in $Evidence.Contains) {
      if (-not $text.Contains($needle)) {
        $issues.Add("missing text in $($Evidence.Path): $needle") | Out-Null
      }
    }
  }

  return $issues
}

$checks = @(
  @{
    Id = "book-search-add"
    Requirement = "AI book candidate search and book add"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/project/mvp.md" @("external or AI-fallback candidates")
      Evidence "docs/back/sdd.md" @("/api/books/search-candidates", "/api/books", "Book could not be saved", "blank title, author, or candidate identifier suggestions are removed")
      Evidence "back/src/main/java/com/margins/book/controller/BookController.java" @('@RequestMapping("/api/books")', '@PostMapping("/search-candidates")')
      Evidence "back/src/main/java/com/margins/book/dto/SaveBookRequest.java" @("@Size(max = 255)")
      Evidence "back/src/main/java/com/margins/book/business/BookBusiness.java" @("sanitizeCandidate", "SAVE_TEXT_LIMIT", "Book could not be saved")
      Evidence "back/src/test/java/com/margins/BookBusinessPersistenceTest.java" @("saveBookRejectsZeroRowInsert", "searchCandidatesReturnsOnlySaveCompatibleCandidates")
      Evidence "back/src/test/java/com/margins/BookControllerValidationTest.java" @("saveRejectsOverlongTitleBeforeBusinessLogic")
      Evidence "back/src/main/java/com/margins/ai/OpenAiAiProvider.java" @("suggestBooks")
      Evidence "front/src/repository/marginsRepository.ts" @("searchCandidates", "/api/books/search-candidates", "saveBook", "/api/books", "fitTextWithSuffix")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-search-input", "book-candidate-save", "manual-book-submit")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "reading-session"
    Requirement = "Book-based ReadingSession creation"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/back/sdd.md" @("/api/reading-sessions", "Book not found", "Reading session could not be saved", "Reading session not found", "Session highlight not found", "Session highlight could not be saved", "Session tag not found", "Session tag could not be saved", "Session insight not found", "Session insight could not be saved")
      Evidence "back/src/main/java/com/margins/session/controller/ReadingSessionController.java" @('@PostMapping', "public ApiResponse<CreateReadingSessionResponse> create")
      Evidence "back/src/main/java/com/margins/session/dto/CreateReadingSessionRequest.java" @("@Size(max = 255)")
      Evidence "back/src/main/java/com/margins/session/mapper/ReadingSessionMapper.java" @("countActiveBookById")
      Evidence "back/src/main/java/com/margins/session/mapper/ReadingSessionMapper.java" @("insert")
      Evidence "back/src/test/java/com/margins/ReadingSessionBusinessPersistenceTest.java" @("CreateReadingSessionRequest", "createRejectsMissingBookBeforeInsert", "createRejectsZeroRowInsert", "createHighlightRejectsZeroRowInsert", "createTagRejectsZeroRowInsert", "createInsightRejectsZeroRowInsert", "missingSessionMutationsReturnNotFound", "childRecordMutationsReturnNotFoundWhenRowsAreMissing", "childRecordWritesReturnNotFoundWhenParentSessionIsMissing")
      Evidence "back/src/test/java/com/margins/SessionControllerValidationTest.java" @("readingSessionRejectsOverlongTitleBeforeBusinessLogic", "readingSessionCreateMissingBookUsesApiResponseFailureShape", "missingReadingSessionMutationUsesApiResponseFailureShape", "missingReadingSessionChildMutationUsesApiResponseFailureShape")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-start-review", "/api/reading-sessions")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "session-window"
    Requirement = "SessionWindow creation inside a session"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/back/sdd.md" @("/api/session-windows", "/api/session-windows/{id}", "Session window not found", "Session window could not be saved", "Question not found", "Question could not be saved")
      Evidence "back/src/main/java/com/margins/session/controller/SessionWindowController.java" @('@RequestMapping("/api/session-windows")', "public ApiResponse<CreateSessionWindowResponse> create", "public ApiResponse<CreateSessionWindowResponse> archive")
      Evidence "back/src/main/java/com/margins/session/dto/CreateSessionWindowRequest.java" @("@Size(max = 40)", "@Size(max = 255)")
      Evidence "back/src/main/java/com/margins/session/mapper/SessionWindowMapper.java" @("countActiveSessionById")
      Evidence "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java" @("CreateSessionWindowRequest", "createRejectsMissingReadingSessionBeforeInsert", "createRejectsZeroRowInsert", "generateQuestionsRejectsZeroRowQuestionInsert", "windowMutationsReturnNotFoundWhenRowsAreMissing", "deleteQuestionReturnsNotFoundWhenDeleteRowIsMissing")
      Evidence "back/src/test/java/com/margins/SessionControllerValidationTest.java" @("sessionWindowRejectsOverlongWindowTypeBeforeBusinessLogic", "sessionWindowRejectsOverlongTitleBeforeBusinessLogic", "sessionWindowCreateMissingReadingSessionUsesApiResponseFailureShape", "missingSessionWindowMutationUsesApiResponseFailureShape")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-start-review", "debate-enter-submit")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "ai-question-answer"
    Requirement = "AI question/answer per window"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/back/sdd.md" @("/api/session-windows/{id}/questions/generate", "/api/session-windows/{id}/messages/stream", "Message could not be saved", "Question could not be saved", "client-supplied userId")
      Evidence "back/src/main/java/com/margins/ai/AiProvider.java" @("suggestQuestions", "answerWindowMessage")
      Evidence "back/src/main/java/com/margins/session/business/SessionWindowBusiness.java" @("generateQuestions", "sendMessage", "ignoredRequestUserId", "Message could not be saved", "Question could not be saved")
      Evidence "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java" @("sendMessageIgnoresClientSuppliedUserId")
      Evidence "back/src/main/java/com/margins/message/mapper/MessageMapper.java" @("insert")
      Evidence "front/src/repository/marginsRepository.ts" @("generateQuestions", "streamMessage")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("book-generate-questions", "/messages/stream", "question-answer-history")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "persona-debate"
    Requirement = "Persona-based AI response in debate window"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/back/sdd.md" @("/api/session-windows/{id}/debate", "/api/session-windows/{id}/debate/all", "Persona could not be saved", "client-supplied userId")
      Evidence "db/schema/001_create_mvp_schema.sql" @("CREATE TABLE IF NOT EXISTS personas", "persona_id")
      Evidence "back/src/main/java/com/margins/persona/mapper/PersonaMapper.java" @("findActive")
      Evidence "back/src/main/java/com/margins/persona/dto/CreatePersonaRequest.java" @("@Size(max = 120)")
      Evidence "back/src/main/java/com/margins/persona/business/PersonaBusiness.java" @("Persona could not be saved")
      Evidence "back/src/test/java/com/margins/PersonaBusinessTest.java" @("createRejectsZeroRowInsert")
      Evidence "back/src/test/java/com/margins/SessionControllerValidationTest.java" @("personaRejectsOverlongDisplayNameBeforeBusinessLogic")
      Evidence "back/src/main/java/com/margins/session/business/SessionWindowBusiness.java" @("public AiMessageResponse debate", "public AiMessageListResponse debateAll", "ignoredRequestUserId")
      Evidence "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java" @("debateIgnoresClientSuppliedUserId", "debateAllIgnoresClientSuppliedUserId")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("debate-topic-input", "debate-session-submit", "debate-message-list")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "conversation-persistence"
    Requirement = "All conversations and records saved to DB"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/db/sdd.md" @("messages", "session_windows", "questions")
      Evidence "db/schema/001_create_mvp_schema.sql" @("CREATE TABLE IF NOT EXISTS messages", "parent_message_id", "question_id")
      Evidence "back/src/main/java/com/margins/message/mapper/MessageMapper.java" @("findBySessionId", "softDelete")
      Evidence "back/src/test/java/com/margins/MessageBusinessPersistenceTest.java" @("deleteReturnsNotFoundWhenSoftDeleteUpdatesNoRows")
      Evidence "back/src/test/java/com/margins/SessionWindowBusinessPersistenceTest.java" @("sendMessageRejectsZeroRowUserMessageInsertBeforeAiCall", "message")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("page.reload()", "message-list")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "metric-ready-schema"
    Requirement = "Metric/statistics-ready DB design"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/db/sdd.md" @("metrics", "Metric")
      Evidence "docs/back/sdd.md" @("/api/reading-sessions/{id}/metrics/snapshot", "message=Reading session not found", "Metric snapshot could not be saved")
      Evidence "docs/back/bdd.md" @("Missing reading session metric snapshot returns not found")
      Evidence "db/schema/001_create_mvp_schema.sql" @("CREATE TABLE IF NOT EXISTS metrics", "metric_details")
      Evidence "db/queries/004_metric_sources.sql" @("message_count", "answered_question_count")
      Evidence "harness/scripts/audit-db-contract.ps1" @("DB Contract Audit", "required MVP table", "is_test_data", "answered_question_count", "deleted_at IS NULL")
      Evidence "back/src/test/java/com/margins/MetricBusinessTest.java" @("createSessionSnapshot", "createSessionSnapshotRejectsMissingReadingSessionBeforeInsert", "createSessionSnapshotRejectsZeroRowInsert")
      Evidence "back/src/test/java/com/margins/SessionControllerValidationTest.java" @("missingMetricSnapshotSessionUsesApiResponseFailureShape")
      Evidence "front/src/components/views/SessionWorkbench.tsx" @("metric-snapshot-submit", "lastMetricSnapshot.metricName")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "auth"
    Requirement = "Single-user or simple JWT initial auth"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/back/sdd.md" @("POST /api/auth/login", "AuthTokenFilter")
      Evidence "back/src/main/java/com/margins/auth/controller/AuthController.java" @('@PostMapping("/login")')
      Evidence "back/src/main/java/com/margins/auth/service/JwtTokenService.java" @("HmacSHA256")
      Evidence "back/src/main/java/com/margins/auth/filter/AuthTokenFilter.java" @("HttpHeaders.AUTHORIZATION", "Bearer")
      Evidence "back/src/test/java/com/margins/AuthTokenFilterTest.java" @("HttpHeaders.AUTHORIZATION", "Bearer")
      Evidence "back/src/test/java/com/margins/OpenApiContractTest.java" @("protectedApiRejectsAnonymousRequestsInFullContext", "loginIssuedTokenPassesFullContextAuthFilter", "/api/reading-sessions/latest", "/api/protected-contract-probe", "isUnauthorized", "isNotFound")
      Evidence "front/src/components/views/LoginGate.tsx" @("margins.auth", "login-submit")
      Evidence "front/src/components/views/LoginGate.tsx" @("logout-submit", "margins.auth", "margins.selectedSessionId")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "socket-streaming"
    Requirement = "Socket/SSE for AI streaming and real-time session-window response delivery"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/back/sdd.md" @("message.start", "message.delta", "message.done", "message.error", "error.message")
      Evidence "docs/front/sdd.md" @("message.start", "message.delta", "message.done", "message.error", "CRLF event block delimiters")
      Evidence "back/src/main/java/com/margins/session/controller/SessionWindowController.java" @("TEXT_EVENT_STREAM_VALUE", "message.delta", "message.done")
      Evidence "back/src/main/java/com/margins/ai/OpenAiAiProvider.java" @('"stream"', "response.output_text.delta", "streamWindowMessage", "streamErrorMessage")
      Evidence "back/src/test/java/com/margins/SessionControllerValidationTest.java" @("messages/stream")
      Evidence "back/src/test/java/com/margins/OpenAiAiProviderFallbackTest.java" @("streamsOpenAiResponseDeltasWhenConfigured", "preservesNestedOpenAiStreamErrorMessageAfterProviderDelta", '\"stream\":true')
      Evidence "front/src/repository/marginsRepository.ts" @("text/event-stream", "message.delta", "message.done", "message.error")
      Evidence "front/src/repository/marginsRepository.test.ts" @("message.delta", "message.done", "message.error", "OpenAI stream failed", "parses CRLF-delimited SSE events")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("/messages/stream")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "frontend-quality-gates"
    Requirement = "Frontend build and E2E entry points are explicit"
    Status = "implemented"
    Evidence = @(
      Evidence "front/package.json" @('"test": "npm run test:unit && npm run e2e"', '"test:unit": "vitest run src"', '"pree2e": "node scripts/check-e2e-prereqs.mjs"', '"build": "tsc -b && vite build"', '"screenshots:workbench": "node scripts/capture-workbench-screenshots.mjs"', '"verify:production-selectors": "node scripts/verify-production-selectors.mjs"')
      Evidence "front/scripts/pull-openapi.mjs" @("Margins API", "/api/reading-sessions/{id}/metrics/snapshot", "/api/session-windows/{id}/messages/stream")
      Evidence "front/scripts/check-e2e-prereqs.mjs" @("/api/health", "/api/test/reset", "SPRING_PROFILES_ACTIVE=local or test")
      Evidence "front/scripts/capture-workbench-screenshots.mjs" @("MARGINS_BACKEND_URL", "MARGINS_FRONT_URL", "session-workbench-desktop.png", "session-review-mobile.png")
      Evidence "front/scripts/verify-production-selectors.mjs" @("MARGINS_DIST_DIR", "Production build rendered app root and no data-testid attributes")
      Evidence "front/src/utils/inputLimits.ts" @("inputLimits", "personaDisplayName", "sessionWindowTitle", "fitTextWithSuffix")
      Evidence "front/src/utils/inputLimits.test.ts" @("matches backend column limits", "validates non-blank values", "fits generated labels")
      Evidence "front/src/store/sessionFlowStore.ts" @("createDefaultSessionPatch", "Session started, but default windows could not all be created", "Session started, but library summaries could not be refreshed")
      Evidence "front/src/store/sessionFlowStore.test.ts" @("loads the created session when a default window create fails", "loads the created session when library refresh fails after creation")
      Evidence "front/src/repository/marginsRepository.test.ts" @("keeps generated reading-session titles within the backend limit")
      Evidence "front/src/components/views/SessionWorkbench.tsx" @("maxLength={inputLimits.personaDisplayName}", "maxLength={inputLimits.sessionWindowTitle}", "maxLength={inputLimits.highlightQuote}")
      Evidence "front/src/utils/testAttrs.test.ts" @("strips stable selectors in production", "emits stable selectors outside production")
      Evidence "front/vite.config.ts" @("MARGINS_BACKEND_URL", "MARGINS_FRONTEND_PORT")
      Evidence "front/playwright.config.ts" @("MARGINS_FRONT_URL", "MARGINS_FRONTEND_PORT")
      Evidence "front/tests/e2e/session-workbench.spec.ts" @("MARGINS_BACKEND_URL", "api/test/reset")
      Evidence "harness/scripts/run-fullstack-e2e.ps1" @("BackendPort = 18080", "FrontendPort = 15173", "ReuseExistingServices", "Test-TcpOpen", "already in use", "mysql-up.ps1", "SERVER_PORT", "MARGINS_BACKEND_URL", "MARGINS_FRONTEND_PORT", "MARGINS_FRONT_URL", "npm run e2e", "PASS: full-stack E2E completed.")
      Evidence "docs/front/sdd.md" @("frontend test entry point", "scripts/check-e2e-prereqs.mjs", "self-starting full-stack gate", "MARGINS_BACKEND_URL", "inputLimits", "createDefaultSessionPatch")
      Evidence ".gitignore" @("harness/artifacts/")
      Evidence ".github/workflows/ci.yml" @("Project readiness audit", "Documentation consistency audit", "DB contract audit", "Live deploy guard audit", "audit-live-deploy-guard.ps1", "Artifact secret guard audit", "audit-artifact-secret-guard.ps1", "CI workflow audit", "audit-ci-workflow.ps1", "Completion command audit", "audit-completion-command.ps1", "Quality gate composition audit", "audit-quality-gate-composition.ps1", "Acceptance traceability audit", "audit-acceptance-traceability.ps1", "Final acceptance boundary audit", "audit-final-acceptance.ps1", "Frontend unit tests", "npm run test:unit", "Frontend build", "Install Playwright Chromium", "npx playwright install chromium", "Frontend production selector check", "npm run verify:production-selectors")
      Evidence "harness/scripts/audit-ci-workflow.ps1" @("CI Workflow Audit", "forbidden deploy pattern", "does not run SSH/SCP, SSH actions, deploy env vars", "deploy-raspberry-pi.ps1", "PASS: CI workflow gates are explicit and do not perform live Raspberry Pi deployment.")
      Evidence "harness/scripts/audit-completion-command.ps1" @("Completion Command Audit", "DeploySmokeHealthUrl", "SmokeHealthUrl", "PASS: final deployment completion command is consistent across docs and scripts.")
      Evidence "harness/scripts/audit-quality-gate-composition.ps1" @("Quality Gate Composition Audit", "verify-local-quality", "ci workflow gates", "final acceptance", "PASS: quality gate composition is aligned across local, CI, final acceptance, and docs.")
      Evidence "harness/scripts/audit-acceptance-traceability.ps1" @("Acceptance Traceability Audit", "planning, design, BDD, and verification evidence", "PASS: MVP acceptance traceability is connected across planning, design, BDD, implementation, and tests.")
      Evidence "harness/scripts/verify-local-quality.ps1" @("DB contract audit", "Final acceptance boundary audit", "CI workflow audit", "Completion command audit", "Quality gate composition audit", "Acceptance traceability audit", "Full-stack E2E runner audit", "Frontend unit tests", "Frontend production build", "Frontend production selector check", "FullStackE2E", "run-fullstack-e2e.ps1", "VisualScreenshots", "DeploymentPreflight")
      Evidence "harness/scripts/audit-fullstack-e2e-runner.ps1" @("Full-Stack E2E Runner Audit", "isolated default ports", "explicit reuse flag", "occupied-port rejection", "PASS: full-stack E2E runner safety contract is documented and enforced.")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "reader-review-readiness"
    Requirement = "Reader can see review readiness from persisted session state"
    Status = "implemented"
    Evidence = @(
      Evidence "front/src/utils/sessionReadiness.ts" @("buildSessionReadiness", "answeredQuestionCount", "personaResponseCount")
      Evidence "front/src/utils/sessionReadiness.test.ts" @("buildSessionReadiness", "not ready", "marks each review area ready")
      Evidence "front/src/components/views/SessionWorkbench.tsx" @("review-readiness", "review-readiness-score")
      Evidence "front/src/components/views/SessionWorkbench.tsx" @("review-readiness", "review-readiness-score")
      Evidence "docs/front/bdd.md" @("Reader sees review readiness from persisted session state")
      Evidence "docs/front/sdd.md" @("src/utils/sessionReadiness.ts")
    )
    Gap = ""
    NextSlice = ""
  },
  @{
    Id = "raspberry-pi-deploy"
    Requirement = "Raspberry Pi deploy flow"
    Status = "implemented"
    Evidence = @(
      Evidence "docs/infra/sdd.md" @("Raspberry Pi", "deploy-raspberry-pi.ps1")
      Evidence "infra/docker/mysql-compose.yml" @("mysql:8.4")
      Evidence "infra/scripts/build-artifacts.ps1" @("margins-release")
      Evidence "infra/scripts/verify-artifacts.ps1" @("Guid", "Artifact zip entry must use forward slash separators", "Backend jar is empty", "Frontend dist assets are missing", "Manifest is missing required entry", "Runtime env example is missing required variable", "Runtime env example is missing safe placeholder", "Systemd example is missing required text", "Nginx example is missing required text", "Artifact contains forbidden secret-like file", "Artifact text file contains forbidden secret marker", "Artifact text file contains forbidden secret value", "Checksum target count mismatch")
      Evidence "harness/scripts/audit-artifact-secret-guard.ps1" @("Artifact Secret Guard Audit", "packaged .env files", "private-key text markers", "OpenAI API key markers", "JWT secret value markers", "DB password value markers", "private-key filenames", "PASS: artifact secret guard rejects secret-like release contents.")
      Evidence "infra/scripts/deploy-raspberry-pi.ps1" @("MARGINS_DEPLOY_HOST", "MARGINS_DEPLOY_USER", "MARGINS_DEPLOY_DIR", "DryRun", "SshPreflight", "SmokeHealthUrl", "MARGINS_DEPLOY_HEALTH_URL", "MARGINS_DEPLOY_SSH_KEY must point to an existing private key file", "MARGINS_RELEASE_RETAIN_COUNT", "ReleaseRetainCount", "Rollback", "RollbackReleaseId", "Deploy smoke passed for configured health URL", "SSH authentication preflight failed before artifact transfer")
      Evidence "infra/scripts/apply-raspberry-pi-schema.ps1" @("MARGINS_REMOTE_MYSQL_PASSWORD", "db\schema", "db\seed\001_seed_mvp_data.sql", "docker exec -i", "Raspberry Pi schema apply completed.")
      Evidence "harness/scripts/audit-deploy-dry-run.ps1" @("Deploy Dry-Run Audit", "Deploy dry-run output leaked the configured smoke health URL", "Deploy dry-run output leaked the configured SSH key path", "Remote zip: /opt/margins/margins-release.zip", "SSH key: configured", "Smoke health URL: configured", "Release retain count: 4", 'release_dir="releases/$release_id"', 'ln -sfn "$release_dir" current', "Legacy current checked: existing current directory is preserved before symlink switch", "Release retention checked: old release cleanup keeps configured count", "Rollback checked: previous release symlink switch without artifact transfer", "Rollback validation checked: release id must be a 14 digit timestamp", "Deploy input validation checked: unsafe deploy directory and service name are rejected", "SSH key validation checked", "sudo systemctl restart 'margins-back'", "PASS: Raspberry Pi deploy dry-run output contract is consistent.")
      Evidence "harness/scripts/audit-live-deploy-guard.ps1" @("Live Deploy Guard Audit", "LiveDeploySmoke without preflight", "LiveDeploySmoke without health URL", "MARGINS_DEPLOY_HEALTH_URL", "PASS: live deploy smoke guard prevents accidental Raspberry Pi transfer/restart.")
      Evidence "harness/scripts/audit-release-artifact-runtime.ps1" @("Release Artifact Runtime Audit", "margins-back.jar", "api/health", "PASS: release artifact backend runtime smoke passed.")
      Evidence "harness/scripts/audit-release-artifact-frontend.ps1" @("Release Artifact Frontend Audit", "MARGINS_DIST_DIR", "PASS: release artifact frontend production smoke passed.")
      Evidence "harness/scripts/verify-local-quality.ps1" @("Build release artifact", "build-artifacts.ps1", "Release artifact verification", "Raspberry Pi deploy dry-run", "audit-deploy-dry-run.ps1", "Release artifact runtime smoke", "ArtifactRuntimeSmoke", "Release artifact frontend smoke", "ArtifactFrontendSmoke", "Raspberry Pi SSH preflight", "Raspberry Pi live deploy smoke", "LiveDeploySmoke requires -DeploymentPreflight -SshPreflight", "LiveDeploySmoke requires -DeploySmokeHealthUrl")
      Evidence "infra/scripts/upload-prod-env.ps1" @("RemoteEnvPath", "/opt/margins/.env", "runtime_env=updated", "chmod 600")
      Evidence "infra/scripts/upload-prod-env.sh" @("REMOTE_ENV_PATH", "/opt/margins/.env", "runtime_env=updated", "chmod 600")
      Evidence "docs/infra/bdd.md" @("Raspberry Pi SSH authentication is preflighted", "Full Raspberry Pi deployment can smoke-test health", "Local quality gate can run explicit live deploy smoke", "Raspberry Pi deployment dry-run validates local inputs", "Raspberry Pi deployment dry-run output is audited")
      Evidence "harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md" @("Raspberry Pi")
    )
    Gap = ""
    NextSlice = ""
  }
)

$failed = $false
Write-Output "# MVP Readiness Audit"
Write-Output ""
foreach ($check in $checks) {
  $issues = New-Object System.Collections.Generic.List[string]
  foreach ($item in $check.Evidence) {
    $itemIssues = @(Test-Evidence -Evidence $item)
    foreach ($issue in $itemIssues) {
      $issues.Add($issue) | Out-Null
    }
  }

  if ($issues.Count -gt 0) {
    $failed = $true
  }

  Write-Output "## $($check.Id)"
  Write-Output "- Requirement: $($check.Requirement)"
  Write-Output "- Status: $($check.Status)"
  if ($check.Gap) { Write-Output "- Gap: $($check.Gap)" }
  if ($check.NextSlice) { Write-Output "- Next slice: $($check.NextSlice)" }
  if ($issues.Count -gt 0) {
    Write-Output "- Evidence failures:"
    $issues | ForEach-Object { Write-Output "  - $_" }
  } else {
    Write-Output "- Evidence: paths and required text present"
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

Write-Output "PASS: MVP readiness evidence paths and required text are present."
