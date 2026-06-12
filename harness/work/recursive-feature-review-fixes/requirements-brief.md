# Requirements Brief

## Task Id

- recursive-feature-review-fixes

## Source Query

- "모든 기능에 대한 재귀적인 리뷰(refactoring, debug, potentail error)와 수정 계획을 세우고 수정을 재귀적으로 진행해줘 에러가 없을떄까지"

## Agreed Requirements

- Review all MVP feature surfaces for concrete potential errors.
- Fix issues that can be safely handled without owner secret/production target input.
- Add or update tests for changed backend behavior.
- Re-run backend, frontend, E2E, and documentation consistency gates.
- Document what remains out of scope because it needs owner input or a future slice.

## Acceptance Criteria

- Backend tests pass.
- Frontend production build passes.
- Full-stack E2E smoke passes.
- DB seed reset succeeds after E2E.
- SDD/BDD reflect changed behavior.
- Owner report and registry/dashboard are updated.

## Out Of Scope

- Real OpenAI provider live verification.
- SSE/WebSocket streaming runtime.
- Session read/reload API implementation.
- Raspberry Pi deployment automation.

## 적용한 Owner 결정

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`

## 열린 Owner 결정

- none for this fix batch.

## Agent 논의 요약

- backend-engineer found request validation and window message ordering gaps.
- frontend-engineer found stale async state append risk.
- qa-engineer required recursive test/build/E2E/doc gates.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| backend fixes | backend-engineer | validation/order changes and tests | backend tests pass |
| frontend fix | frontend-engineer | latest-state append | frontend build and E2E pass |
| documentation | work-coordinator | SDD/BDD/report/task updates | doc audit passes |
| commit/push | commit-manager | commit pushed to origin | branch up to date |
