# Requirements Brief

## Task Id

- ai-prompt-snapshot-audit

## Source Query

- Continue recursive harness programming after response grounding by making generated-message prompt policy auditable.

## Agreed Requirements

- Add nullable JSON `messages.prompt_snapshot` to schema and migration.
- Persist prompt snapshot JSON only for generated assistant/persona messages.
- Include `schemaVersion`, `promptContractVersion`, `responseType`, `provider`, `aiModel`, `streaming`, `safetyPolicyVersion`, `groundingPolicyVersion`, and `readingBoundaryPolicyVersion`.
- Expose `promptSnapshot` through backend timeline/message DTOs and frontend model types.
- Do not store full raw prompts or OpenAI request bodies in this slice.

## Acceptance Criteria

- Schema and migration include the column.
- Backend tests prove assistant and persona rows include prompt snapshot JSON.
- Timeline DTO mapping includes `promptSnapshot`.
- Docs and harness audits pass.

## Out Of Scope

- Raw prompt archival.
- Token usage parsing.
- Frontend visual rendering for prompt snapshots.
- New AI provider API.

## 적용한 Owner 결정

- AI-owned report-first workflow.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Agents agreed compact metadata is the smallest useful audit slice after grounding contract work.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Add schema | db-engineer | Base schema and migration | Schema docs align |
| Persist metadata | backend-engineer | Backend message model/mapper/business/tests | Targeted backend test passes |
| Expose model | front-engineer | Front session model | Front unit/build passes |
| Verify | qa-engineer | Verification report | All commands pass |

