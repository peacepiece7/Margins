# Requirements Brief

## Task Id

- reading-room-first-ui

## Source Query

- Continue harness programming from the competitive gap analysis and implement the next recommended UI slice.

## Agreed Requirements

- The active session must immediately look like a prepared reading room.
- Generated questions, persona cast, quote capture, and discussion status must be visible before window/admin controls.
- Existing persisted controls remain authoritative; the new board only summarizes and navigates.
- Missing AI setup should have a retry affordance.

## Acceptance Criteria

- `reading-room-board` renders for active sessions.
- Board cards use existing state for questions, personas, highlights, and persona responses.
- Board actions jump to existing work areas or composer inputs.
- SDD/BDD document the behavior.
- Frontend and harness validation pass.

## Out Of Scope

- Adding citation/evidence metadata to AI responses.
- Changing backend APIs or DB schema.
- Replacing all existing session controls.
- Running hands-on competitor account tests.

## 적용한 Owner 결정

- AI-owned report-first workflow applies.
- Competitive analysis recommends `Reading Room First UI` as the next implementation slice.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Frontend should avoid a large rewrite of `SessionWorkbench` in this slice.
- The board should reduce first-screen cognitive load without moving persistence responsibility into a new store.
- Follow-up work should handle structured AI evidence traceability.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Add board UI | frontend-engineer | `SessionWorkbench.tsx` | Board renders before window controls |
| Update docs | product-planner | `docs/front/sdd.md`, `docs/front/bdd.md` | Behavior is documented |
| Verify | qa-engineer | verification report | Commands pass |
