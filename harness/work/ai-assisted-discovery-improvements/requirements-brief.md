# Requirements Brief

## Task Id

- ai-assisted-discovery-improvements

## Source Query

- "재귀적으로 기능에 대해서 기획 - 개발 - 토론 - 테스트 - 재개발 과정을 반복적으로 수행해서 목표를 완성해줘"

## Agreed Requirements

- Book search should try a real catalog source before falling back to AI-only candidates.
- Book search must still work without new secrets or owner credentials.
- Persona auto generation should produce selectable drafts based on the active reading context.
- Question auto generation should offer a preview/draft mode before persisted save.
- Existing manual persona creation, existing question creation, and existing persisted question generation must remain usable.
- Docs and tests must describe and verify the new behavior.

## Acceptance Criteria

- `POST /api/books/search-candidates` can return Open Library candidates when the provider responds.
- If Open Library returns no usable candidates or fails, existing AI/placeholder candidate fallback still returns results.
- A persona draft API returns generated persona candidates without writing rows to `personas`.
- A question draft API returns generated question candidates without writing rows to `questions`.
- Frontend exposes generate/select controls for persona and question drafts.
- SDD/BDD reflect the new preview and provider behavior.
- Verification commands pass or any failure is routed to revision with evidence.

## Out Of Scope

- Mandatory Naver/Kakao API credentials.
- Full RAG or external book content ingestion.
- Social login or multi-user ownership changes.
- Automatic background generation without reader action.

## 적용한 Owner 결정

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Use public Open Library first to avoid credential blockers.
- Use AI/placeholder for persona and question drafts.
- Keep generated drafts transient until the reader chooses to save.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Backend provider/API | backend-engineer | Open Library provider, persona/question draft endpoints | Backend tests compile and pass |
| Frontend draft UI | frontend-engineer | Draft panels and repository methods | Frontend unit/build pass |
| Docs and QA | qa-engineer | SDD/BDD updates and verification report | Docs audit and test commands pass |
| Recursive revision | revision-engineer | Fixes for failed or weak evidence | Re-run failed checks |
