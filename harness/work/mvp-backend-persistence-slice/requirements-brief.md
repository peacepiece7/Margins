# Requirements Brief

## Task Id

- mvp-backend-persistence-slice

## Source Query

- 정리된 task와 문서를 기반으로 다음 작업을 이어갈 수 있게 준비한다.

## Agreed Requirements

- First backend persistence slice writes `books`, `reading_sessions`, `session_windows`, and `messages`.
- MyBatis mappers must return generated IDs instead of fixed placeholder IDs.
- User identity may use seed single-user id `1` until auth work expands.
- Message persistence must store user and AI response messages with session/window context.
- No RAG, no social login, no external book API, and no socket runtime in this slice.

## Acceptance Criteria

- Placeholder fixed IDs are removed from persisted create paths.
- Mapper insert methods exist and match DB columns.
- Tests pass through `back/scripts/test.ps1`.
- If DB-backed verification is used, MySQL runtime uses documented env overrides.
- Back SDD/BDD describe implemented persistence behavior.

## Out Of Scope

- Real OpenAI network integration.
- Socket streaming runtime.
- Metrics/statistics generation.
- JWT/social auth expansion.
- Full front/back Docker Compose.

## Owner Decisions Applied

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## Open Owner Decisions

- None.

## Agent Discussion Summary

- Agents agreed persistence is the next task because schema, backend skeleton, test tooling, and MySQL runtime are ready.
- The first slice should be narrow enough to verify deeply.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Design mapper parameter objects | backend-engineer | model/mapper plan | Columns match DB schema |
| Implement insert paths | backend-engineer | mapper/business changes | Generated IDs returned |
| Add tests | backend-engineer | tests | `back/scripts/test.ps1` passes |
| Verify against docs | qa-engineer | verification report | requirements map to file evidence |
| Commit scoped persistence work | commit-manager | git commit | unrelated changes excluded |
