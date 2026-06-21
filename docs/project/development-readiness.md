# Development Readiness

## Purpose

This document maps MVP requirements to implementation evidence, remaining gaps, and the next development slice. It is the first place to check when deciding what to build after a context reset.

## Current Summary

- Core MVP reading flows are implemented end to end: book candidate search, book save, reading session creation, session windows, persisted questions, messages, highlights, tags, insights, persona debate, completion, review export, and metrics snapshots.
- OpenAI is wired through `OpenAiAiProvider` when `margins.ai.provider=openai`; configured window-message streams use OpenAI Responses API `stream=true`, and missing API keys or pre-stream provider failures fall back to deterministic placeholder behavior.
- Runtime AI streaming uses SSE through `POST /api/session-windows/{id}/messages/stream` with `message.start`, provider-backed `message.delta`, `message.done`, and `message.error`.
- JWT login and bearer-token filtering are implemented for `/api/**`, while the business model remains single-user compatible for MVP speed.
- Frontend logout is covered by E2E and clears both `margins.auth` and `margins.selectedSessionId`, so a later login cannot accidentally restore the previous selected session from browser storage.
- JWT validation now rejects signed tokens with unexpected JWT header algorithm or type, in addition to malformed, tampered, expired, wrong-secret, or wrong-issuer tokens.
- Backend validation and domain errors now use the common `ApiResponse` failure envelope, so frontend and E2E tooling can parse `success=false` and `message` consistently.
- Frontend repository helpers now preserve backend `ApiResponse.message` for JSON failures, so visible workbench and login errors can show domain reasons instead of generic HTTP status text.
- Raspberry Pi live transfer/restart and post-deploy health smoke are verified against the configured target. The deploy runner now uses a generated SSH key through `MARGINS_DEPLOY_SSH_KEY`; the full completion gate transferred `infra/artifacts/margins-release.zip`, switched `/opt/margins/current`, restarted `margins-back`, and received HTTP 200 from `http://172.30.1.21/api/health`.

## MVP Requirement Map

| Requirement | Status | Evidence | Remaining work |
| --- | --- | --- | --- |
| Book search and add | implemented | `BookController.java`, `OpenLibraryBookSearchProvider.java`, `OpenAiAiProvider.java`, `BookBusinessPersistenceTest.java`, `session-workbench.spec.ts` | None for MVP. |
| Create `ReadingSession` from a book | implemented | `ReadingSessionController.java`, `ReadingSessionMapper.java`, `ReadingSessionBusinessPersistenceTest.java` | Multi-user ownership is later work. |
| Create `SessionWindow` inside a session | implemented | `SessionWindowController.java`, `SessionWindowMapper.java`, `SessionWindowBusinessPersistenceTest.java` | None for MVP. |
| Window-level AI question and answer | implemented | `AiProvider.java`, `OpenAiAiProvider.java`, `SessionWindowBusiness.java`, `MessageMapper.java`, `SessionControllerValidationTest.java`, `OpenAiAiProviderFallbackTest.java` | None for MVP. |
| Persona-based debate window | implemented | `PersonaMapper.java`, `SessionWindowBusiness.java`, `OpenAiAiProvider.java`, `session-workbench.spec.ts` | Multi-turn persona orchestration can be added later. |
| Persist all conversations and records | implemented | `MessageMapper.java`, `SessionWindowBusinessPersistenceTest.java`, `ReadingSessionBusinessPersistenceTest.java` | None for MVP. |
| Metric/statistics-ready DB design | implemented | `docs/db/sdd.md`, `db/schema/*.sql`, `MetricBusinessTest.java` | Scheduled metric jobs remain later work. |
| Single-user or simple JWT auth | implemented | `AuthController.java`, `JwtTokenService.java`, `AuthTokenFilter.java`, `AuthTokenFilterTest.java` | Social login remains deferred. |
| AI streaming/socket delivery | implemented | `SessionWindowController.java`, `marginsRepository.ts`, `SessionControllerValidationTest.java`, `session-workbench.spec.ts` | WebSocket remains deferred until multi-client real-time delivery is needed. |
| Raspberry Pi deploy flow | implemented | `docs/infra/sdd.md`, `infra/scripts/*`, `infra/artifacts/margins-release.zip`, `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md` | None for MVP. |

## Recently Verified Slice

Reader session brief polish is implemented and verified:

- Frontend derives a compact `session-brief` from persisted timeline state.
- The brief summarizes active focus, page progress, latest quote evidence, answer count, persona reply count, and next action.
- Vitest covers empty-session defaults and developed-session priority rules.
- E2E asserts the brief updates after question selection, progress save, quote edit, answer persistence, and persona replies.
- Playwright screenshots capture desktop and mobile workbench layouts through `npm run screenshots:workbench` or `harness/scripts/verify-local-quality.ps1 -VisualScreenshots`, with the brief using a readable two-column desktop layout.
- Full-stack Playwright verification can now be run from a cold local process state with `harness/scripts/run-fullstack-e2e.ps1`; it uses isolated default backend/frontend ports, starts MySQL with schema, starts backend and Vite, runs E2E, cleans up processes it started, and refuses accidental reuse of occupied service ports unless `-ReuseExistingServices` is explicit and the existing service answers the expected HTTP 2xx check.
- Lower workbench controls are grouped into `message-composer` and `persona-composer` regions so message answering, persona creation, and debate actions remain readable in long sessions.
- Completed review screenshots are captured by the same script, and the review panel is grouped into overview, insight, and evidence regions.
- Reflection prompts now use compact full-row selection with inline answered/open status and a separated delete action for unanswered prompts.
- Completed closeout summaries render read-only and relabel the completion action as `Completed`, reducing form ambiguity after a session is finished.
- Active sessions now include a session-area jump control for questions, progress, quotes, messages, and review. It uses a two-row mobile layout so long mobile reading sessions do not require cramped one-row controls to return to repeated work areas.
- Narrow screens now show lower `composer-mode-tabs` so message answering and persona debate do not stack into one dense block; desktop keeps both composers visible side by side.
- Mobile composer tabs now expose standard tab accessibility state, blank message/debate submits stay disabled, and next-action persona jumps open the Persona composer before focusing the debate input.
- Initial workbench hydration failure now stops after the failed request, shows the error, and gives the reader an explicit Retry action instead of repeatedly firing restore requests while the backend is unavailable.
- Stale selected-session browser storage now recovers by falling back to the latest available session and replacing the stale `margins.selectedSessionId`, so an archived or missing stored session does not block workbench entry.
- Markdown export filenames now preserve Unicode book/session title characters, so non-English reading records do not degrade to generic `session-*.md` downloads.
- Direct window-message SQL lookup now joins non-deleted `session_windows`, so archived windows stay hidden from direct message recovery queries as well as timeline reads.
- User message edit/delete now returns a `404` domain failure envelope when the message is missing, deleted, assistant-owned, concurrently removed, or scoped to an archived session/window.
- Reading session archive, pin, title, progress, and complete mutations now return a `404` domain failure envelope when no session row is updated, so direct API calls against missing or archived sessions cannot appear successful.
- Highlight, tag, and insight child-record mutations now return `404` domain failure envelopes when the parent session or child row is missing, so direct API calls cannot silently refresh a timeline after no child record changed.
- Session summary and metric source counts now exclude questions and messages scoped to archived windows, keeping library totals and metric snapshots aligned with user-facing timelines.
- Backend session-window archive now rejects deleting the last non-deleted window, so sessions cannot be left without a usable work area even if the API is called outside the frontend guard.
- Backend session-window title/archive and question delete mutations now verify the final update count and return `404` failure envelopes if the row was already missing or concurrently archived/deleted.
- Backend reading-session creation now verifies the saved book exists before insert, so missing or archived book ids return a `404` failure envelope instead of surfacing a database FK failure.
- Backend session-window creation now verifies the parent reading session exists before insert, so missing or archived parent sessions return a `404` failure envelope instead of surfacing a database FK failure.
- Backend metric snapshots now reject missing, archived, or wrong-owner reading sessions with a `404` failure envelope before inserting a metric row.
- Backend book candidate search now removes blank AI suggestions and trims/caps candidate identifiers, titles, and authors to the `/api/books` save contract, so displayed AI candidates can be saved without tripping column-length validation.
- Backend message writes now validate optional `questionId` ownership before persistence, so answers cannot be linked to missing, archived, or cross-window questions through direct API calls.
- Backend message and debate writes now ignore client-supplied `userId` values and persist rows with the session-window owner, so direct API calls cannot spoof the conversation owner.
- Backend OpenAI streaming now preserves nested provider error messages after partial deltas, so the SSE failure path can show the actionable upstream reason instead of a generic stream failure.
- Backend single-persona debate now validates that the requested `personaId` is active and non-deleted before creating prompt or response messages.
- Frontend debate persona selection now stays aligned with the active persona list, preserving a valid selection, falling back to the first active persona after refresh, and disabling single-persona Debate when no valid persona remains.
- All-persona debate now uses a content-only request contract; persona ids are selected by the backend from active personas instead of requiring the frontend to send a sentinel `personaId`.
- Full-stack E2E now defaults to isolated ports and rejects accidental reuse of occupied backend/frontend ports, so stale local processes cannot silently verify the wrong build.
- Release artifact verification now rejects non-placeholder JWT secret and DB password values in packaged text files, in addition to `.env`, private-key, and OpenAI key markers.
- Raspberry Pi deploy dry-run now validates a configured `MARGINS_DEPLOY_SSH_KEY` points to an existing local file before any SSH/SCP step, while redacting the key path from dry-run output.
- Reader draft inputs for messages, highlights, personas, questions, windows, tags, and insights now clear only after the save succeeds, so transient backend or stream failures do not discard unsaved reading notes.
- Frontend new-session creation now recovers after partial default-window or library refresh failures by loading the created session timeline, selecting any created usable window, preserving available state, and surfacing a warning instead of leaving the created session hidden.
- Selected reflection questions are now preserved across timeline refreshes when the selected question still belongs to the active window, preventing later-question answers from silently falling back to the first prompt.
- Progress and highlight page drafts now allow only blank values or non-negative safe integers before submit, so decimal, signed, non-numeric, or unsafe page values do not reach the backend page-field contract.
- Frontend text drafts for session/window titles, progress notes, highlights, tags, review insight metadata, and custom persona display fields now use shared `inputLimits` that mirror backend request and column limits before submit.
- Frontend-generated reading-session titles now use the same limit helper, so a maximum-length saved book title does not produce an overlong `POST /api/reading-sessions` payload when the app appends the initial `reflection` suffix.
- Frontend SSE stream parsing now accepts both LF and CRLF event block delimiters, so `message.delta` and `message.done` remain readable through runtimes or proxies that normalize line endings.
- Backend JDBC test reset now re-enables MySQL foreign-key checks in a `finally` path if cleanup fails, so repeated E2E runs cannot inherit a pooled connection with constraints disabled.

## Recommended Next Slices

1. Product polish pass:
   - Continue screenshot-led review of remaining mobile spacing and repeated-control density after the session-area jump control.
   - Split any over-dense panels only when the change improves repeated reading-session use.
2. Deployment follow-up:
   - Keep Raspberry Pi runtime secrets and service units maintained outside release artifacts.
   - Re-run the live build-transfer-restart health smoke path after deployment script or runtime configuration changes.

## Verification Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/run-fullstack-e2e.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -FullStackE2E`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -VisualScreenshots` with MySQL ready, backend running with `SPRING_PROFILES_ACTIVE=local`, and Vite running
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke` when Docker/MySQL is available and release artifact runtime boot should be verified before Raspberry Pi transfer.
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactFrontendSmoke` when the packaged frontend artifact should be rendered and selector-stripping verified before Raspberry Pi transfer.
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -SshPreflight` to re-check Raspberry Pi SSH authentication without transfer/restart.
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <health-url>` for the final release artifact smoke plus Raspberry Pi transfer/restart health smoke.
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-mvp-readiness.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-db-contract.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-deploy-dry-run.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-release-artifact-runtime.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-release-artifact-frontend.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-quality-gate-composition.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-acceptance-traceability.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-fullstack-e2e-runner.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-final-acceptance.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File back/scripts/test.ps1`
- `npm run build` from `front/`
- `npm test` from `front/` when MySQL, backend, and Vite are already running; otherwise prefer `harness/scripts/run-fullstack-e2e.ps1`.

## Last Local Verification

Verified on 2026-06-13:

- Local quality gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1` passed after adding the full-stack E2E runner safety audit, the readiness evidence text guard, and the no-blocked-requirements guard.
- Final acceptance boundary audit: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-final-acceptance.ps1` passed.
- Deployment preflight: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight` passed.
- Current deployment preflight gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -SkipBackend -SkipFrontendBuild` passed.
- Current deploy release-directory gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -SkipBackend -SkipFrontendBuild` passed after verifying timestamped release extraction, `/opt/margins/current` symlink switching, release README layout guidance, artifact verification, and dry-run restart commands.
- Current deploy release-retention gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-deploy-dry-run.ps1` passed after verifying `ReleaseRetainCount`, old release cleanup command generation, and health URL redaction.
- Current deploy rollback dry-run gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-deploy-dry-run.ps1` passed after verifying previous-release rollback command generation, no artifact transfer details in rollback mode, rollback release id validation, and service restart commands.
- Current deploy input-validation gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-deploy-dry-run.ps1` passed after verifying unsafe deploy directories and unsafe service names are rejected before SSH.
- Current deploy SSH-key validation gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-deploy-dry-run.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after verifying configured SSH key paths must exist before SSH and dry-run output does not print the key path.
- Current deploy legacy-current migration gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-deploy-dry-run.ps1` passed after verifying a real pre-symlink `current` directory would be moved into `releases/legacy-<timestamp>` before creating the symlink.
- Current artifact runtime smoke gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SkipBackend -SkipFrontendBuild` passed after launching the packaged backend jar from `infra/artifacts/margins-release.zip` and polling `/api/health`.
- Current artifact frontend smoke gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SkipBackend -SkipFrontendBuild` passed after rendering packaged `front/dist`, confirming bundled asset references exist, and verifying production selectors are stripped.
- Current combined artifact smoke gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke` passed after full audits, backend tests, frontend unit/build/selector checks, release artifact build, artifact verification, deploy dry-run, packaged backend jar `/api/health` smoke, and packaged frontend render smoke.
- Current Raspberry Pi SSH preflight boundary: `powershell -NoProfile -ExecutionPolicy Bypass -File infra/scripts/deploy-raspberry-pi.ps1 -SshPreflight` passed with the generated `MARGINS_DEPLOY_SSH_KEY` for `peacepiece@172.30.1.21`.
- Current Raspberry Pi live deploy gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl http://172.30.1.21/api/health` passed after full audits, backend tests, frontend unit/build/selector checks, release artifact verification, deploy dry-run, packaged backend/frontend artifact smokes, SSH preflight, artifact transfer, `/opt/margins/current` symlink switch, `margins-back` restart, and post-deploy HTTP health smoke.
- Current Raspberry Pi schema apply: `infra/scripts/apply-raspberry-pi-schema.ps1 -ApplySeed` passed against the remote `margins-mysql` Docker container after providing the remote MySQL password through process environment, applying every `db/schema/*.sql` file and seed data without printing the password.
- Current Raspberry Pi browser smoke: Playwright opened `http://172.30.1.21`, logged in as `test-reader`, rendered `Seed reflection session`, `Active session brief`, and `The Left Hand of Darkness`, detected no framework overlay, and saved `harness/artifacts/pi-production-smoke.png`.
- Current artifact secret value guard: `infra/scripts/verify-artifacts.ps1 -ArtifactPath infra/artifacts/margins-release.zip`, `harness/scripts/audit-artifact-secret-guard.ps1`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after extending release artifact verification to reject non-placeholder JWT secret and DB password values in packaged text files.
- Self-starting full-stack E2E runner: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/run-fullstack-e2e.ps1` passed.
- Isolated-port full-stack E2E runner: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/run-fullstack-e2e.ps1` passed with default backend port `18080` and frontend port `15173`.
- Full local quality with E2E: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -FullStackE2E` passed.
- Current full-stack E2E gate: `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/verify-local-quality.ps1 -FullStackE2E -SkipBackend -SkipFrontendBuild` passed.
- Current session-area jump polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/run-fullstack-e2e.ps1`, and `npm run screenshots:workbench` passed.
- Current mobile composer density polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/run-fullstack-e2e.ps1`, and `npm run screenshots:workbench` passed after adding narrow-screen `composer-mode-tabs`.
- Current mobile composer accessibility polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after adding tab accessibility state, disabled blank message/debate submits, and E2E coverage for mobile next-action Persona composer focus.
- Current initial-load retry polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after adding non-looping initial hydration failure handling and visible Retry.
- Current logout storage cleanup coverage: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, `harness/scripts/audit-final-acceptance.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after extending E2E to assert logout clears `margins.auth` and `margins.selectedSessionId`.
- Current stale selected-session recovery polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after adding latest-session fallback for failed stored selected-session restore.
- Current Unicode export filename polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after extracting export filename helpers and adding unit coverage.
- Current JWT header validation hardening: `back/scripts/test.ps1` and `harness/scripts/audit-doc-consistency.ps1` passed after rejecting signed tokens with unexpected `alg` or `typ` headers.
- Current backend failure-envelope hardening: `back/scripts/test.ps1` and `harness/scripts/audit-doc-consistency.ps1` passed after adding `ApiExceptionHandler` for validation and `ResponseStatusException` failures.
- Current frontend failure-message preservation: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after teaching repository JSON and pre-stream failures to throw backend `ApiResponse.message`.
- Current full-context API failure-envelope contract: `back/scripts/test.ps1` and `harness/scripts/audit-doc-consistency.ps1` passed after extending `OpenApiContractTest` to assert 401 and validation failure bodies.
- Current direct window-message query soft-delete hardening: `harness/scripts/audit-db-contract.ps1` and `harness/scripts/audit-doc-consistency.ps1` passed after requiring `002_window_messages.sql` to exclude archived windows in DB contract audit.
- Current message mutation not-found hardening: `back/scripts/test.ps1`, `harness/scripts/audit-db-contract.ps1`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after converting missing editable-message and zero-row delete failures to `404` `ApiResponse` envelopes.
- Current reading-session mutation not-found hardening: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after converting missing archive, pin, title, progress, and complete session mutations to `404` `ApiResponse` envelopes.
- Current child-record mutation not-found hardening: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after converting missing highlight, tag, and insight mutations to `404` `ApiResponse` envelopes.
- Current summary/metric archived-window count hardening: `back/scripts/test.ps1`, `harness/scripts/audit-db-contract.ps1`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after excluding archived-window questions and messages from session summaries and metric source counts.
- Current last-window archive guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after rejecting `DELETE /api/session-windows/{id}` when it would archive the final active window.
- Current session-window/question mutation not-found hardening: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after converting zero-row window title/archive and question delete mutations to `404` `ApiResponse` envelopes.
- Current reading-session book guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after rejecting reading-session creation for missing or archived saved books before insert.
- Current book/persona insert row-count guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after failing save-book and custom-persona creation when the database reports zero inserted rows instead of returning a fake successful result.
- Current book/persona DTO length guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after rejecting saved-book and custom-persona payloads that exceed MySQL VARCHAR column limits before business logic.
- Current session/window DTO length guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after rejecting reading-session titles, session-window types, and session-window titles that exceed MySQL VARCHAR column limits before business logic.
- Current persisted-record insert row-count guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after failing reading-session, session-window, question, message, highlight, tag, insight, and metric snapshot flows when the mapper reports zero inserted rows instead of returning fake successful ids.
- Current session-window parent guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after rejecting session-window creation for missing or archived parent reading sessions before insert.
- Current metric snapshot session guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after rejecting metric snapshots for missing or archived reading sessions before insert.
- Current book candidate save-compatibility guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after normalizing external and AI-fallback book candidates to the saved-book DTO length and blank-field contract before returning them to the frontend.
- Current message question-link guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after validating `questionId` belongs to the target non-deleted session window before inserting message rows.
- Current message/debate owner guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after ignoring client-supplied `userId` values for message, single-persona debate, and all-persona debate persistence.
- Current OpenAI stream error-message preservation: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after preserving nested provider stream errors once deltas have already been emitted.
- Current debate persona guard: `back/scripts/test.ps1`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after validating single-persona debate `personaId` before inserting prompt and response message rows.
- Current frontend debate persona selection guard: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after preventing stale persona selections from sending single-persona debate requests.
- Current debate-all request contract cleanup: `back/scripts/test.ps1`, `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/run-fullstack-e2e.ps1 -BackendPort 18080 -FrontendPort 15173` passed after replacing the all-persona `personaId: 0` request body with a content-only DTO.
- Current full-stack E2E stale-service guard: `harness/scripts/run-fullstack-e2e.ps1`, occupied-port guard smoke, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/audit-mvp-readiness.ps1` passed after making isolated ports the default and requiring `-ReuseExistingServices` before reusing occupied backend/frontend ports.
- Current full-stack E2E runner audit gate: `harness/scripts/audit-fullstack-e2e-runner.ps1`, `harness/scripts/audit-quality-gate-composition.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, `harness/scripts/audit-final-acceptance.ps1`, and `harness/scripts/verify-local-quality.ps1 -SkipBackend -SkipFrontendBuild` passed after adding the runner safety audit to local quality and final acceptance checks.
- Current reader draft preservation polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after extending failed-save E2E coverage from message/highlight drafts to custom question, session tag, and persona drafts.
- Current partial new-session recovery polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after extracting `createDefaultSessionPatch` and adding Vitest coverage for loading the created session when a later default-window request or library refresh fails.
- Current selected-question refresh polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after preserving later selected reflection questions across timeline refresh.
- Current page-number draft validation polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/run-fullstack-e2e.ps1` passed after adding frontend utility coverage and E2E assertions that invalid progress/highlight page drafts disable submit actions before API calls.
- Current frontend text-limit validation polish: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after adding shared `inputLimits` coverage and wiring session/window/progress/highlight/tag/insight/persona text drafts to backend-compatible max lengths before API calls.
- Current generated session-title limit guard: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after fitting repository-generated reading-session titles inside the backend title limit even when the saved book title is already at that limit.
- Current SSE parser line-ending hardening: `npm run test:unit`, `npm run build`, `npm run verify:production-selectors`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-mvp-readiness.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after adding CRLF-delimited stream coverage to the frontend repository tests.
- Current JDBC reset FK recovery hardening: `back/scripts/test.ps1`, `harness/scripts/audit-db-contract.ps1`, `harness/scripts/audit-doc-consistency.ps1`, `harness/scripts/audit-acceptance-traceability.ps1`, and `harness/scripts/audit-final-acceptance.ps1` passed after forcing FK checks back on when test-data cleanup fails.

## Final Acceptance Boundary

- `harness/scripts/audit-final-acceptance.ps1` is the lightweight completion-boundary check after context reset. It runs readiness, docs, DB, deploy dry-run, deploy safety, artifact safety, CI, completion-command, quality-composition, acceptance-traceability, and full-stack E2E runner audits together, then prints each sub-audit result before the acceptance boundary summary.
- `harness/scripts/audit-live-deploy-guard.ps1` is part of the local quality gate and proves `-LiveDeploySmoke` cannot run from partial flag combinations.
- `harness/scripts/audit-artifact-secret-guard.ps1` is part of the local quality gate and proves release artifact verification rejects `.env` files and private-key markers before deployment.
- `harness/scripts/audit-ci-workflow.ps1` is part of the local quality gate and proves CI verifies artifacts without running live Raspberry Pi deploy commands.
- `harness/scripts/audit-completion-command.ps1` is part of the local quality gate and proves the documented final Raspberry Pi completion command matches script parameters.
- `harness/scripts/audit-quality-gate-composition.ps1` is part of the local quality gate and proves quality gate composition stays aligned across local quality, CI, final acceptance, and deployment docs.
- `harness/scripts/audit-acceptance-traceability.ps1` is part of the local quality gate and proves MVP requirements connect planning, design, BDD, implementation, and test evidence before final acceptance passes.
- `harness/scripts/audit-fullstack-e2e-runner.ps1` is part of the local quality gate and final acceptance audit. It proves the self-starting E2E runner uses isolated default ports, rejects accidental stale-service reuse, and documents the explicit `-ReuseExistingServices` escape hatch.
- `harness/scripts/audit-release-artifact-runtime.ps1` is an optional deployment preflight smoke that launches the packaged backend jar from the release zip and polls `/api/health` before Raspberry Pi transfer.
- `harness/scripts/audit-release-artifact-frontend.ps1` is an optional deployment preflight smoke that renders packaged `front/dist`, checks referenced assets, and verifies production selectors are stripped before Raspberry Pi transfer.
- The final acceptance audit requires the latest current full-stack E2E gate evidence to be recorded in this document.
- The final acceptance audit requires the latest current deployment preflight gate evidence to be recorded in this document.
- The final acceptance audit fails if this document contains `pending verification`, `weak evidence`, `Missing or weak`, or `unverified` evidence text; every current evidence line must point to a command or external blocker.
- The final acceptance audit fails if MVP readiness reports any blocked requirement after the live Raspberry Pi deploy smoke has passed.
- Live Raspberry Pi completion evidence should use `verify-local-quality.ps1 -DeploymentPreflight -ArtifactRuntimeSmoke -ArtifactFrontendSmoke -SshPreflight -LiveDeploySmoke -DeploySmokeHealthUrl <health-url>` so artifact verification, dry-run audit, backend artifact runtime smoke, frontend artifact render smoke, SSH auth, full transfer/restart, and health smoke are captured by one command.
- `harness/scripts/verify-local-quality.ps1` and `.github/workflows/ci.yml` run the final acceptance boundary audit so routine local and CI gates expose readiness regressions.
- The audit is expected to pass with no blocked MVP requirements.
- Full project completion can be claimed after the final local quality gate with live Raspberry Pi build-transfer-restart smoke succeeds.
