# Discussion Log

## Task Id

- ai-assisted-discovery-improvements

## Discussion Status

- consensus reached

## Topic

- Improve book search, automatic persona generation, and automatic question generation through recursive planning, development, QA, and revision.

## Participants

- product-planner
- backend-engineer
- frontend-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: product-planner
- Role: scope framing
- Position: The first iteration should improve real user value without adding credential blockers.
- Assumptions: External Korean book APIs are useful later, but the first implementation should be reversible and non-secret.
- Proposed requirements: Ground book search in public catalog metadata; expose generated persona/question drafts before persistence.
- Risks: Adding too many providers or silently saving AI output would make the MVP harder to reason about.
- Questions for other agents: Can backend expose draft endpoints without schema changes?
- Owner 결정 필요: No

### Entry 2

- Agent: backend-engineer
- Role: API and service contract
- Position: Add a book search provider boundary and use Open Library first; extend `AiProvider` for persona/question drafts.
- Assumptions: Existing persistence tables can store selected personas and questions without migration.
- Proposed requirements: Preserve AI fallback for book search; preserve existing immediate question generation endpoint while adding preview.
- Risks: External API calls can fail; provider failure must degrade to existing AI/placeholder behavior.
- Questions for other agents: Should draft endpoints persist automatically? Consensus: no.
- Owner 결정 필요: No

### Entry 3

- Agent: frontend-engineer
- Role: UI behavior
- Position: Keep manual forms and add generated draft panels near existing controls.
- Assumptions: Drafts are transient client state until the reader saves.
- Proposed requirements: Add stable selectors for persona/question draft panels and actions.
- Risks: Long workbench UI can get crowded; keep draft panels compact.
- Owner 결정 필요: No

### Entry 4

- Agent: qa-engineer
- Role: verification
- Position: Backend tests should prove fallback and draft generation; frontend build/unit checks should prove contracts compile.
- Assumptions: Full-stack E2E can remain a later hardening step if focused tests cover the new contracts.
- Proposed requirements: Run backend tests, frontend unit/build, and docs audit.
- Risks: Existing uncommitted locale changes must not be reverted.
- Owner 결정 필요: No

## Consensus

- Proceed with Open Library as the first external book source because it is public and does not require credentials.
- Generated persona and question results should be reviewable drafts, not automatic writes.
- Existing manual and immediate-save flows must continue to work.

## Disagreements

- None after limiting the first iteration to reversible provider and draft endpoints.

## 요청할 Owner 결정

- None for this iteration.

## 이어서 반영할 요구사항

- Add backend provider/API contracts.
- Add frontend draft UI.
- Update SDD/BDD and tests.
