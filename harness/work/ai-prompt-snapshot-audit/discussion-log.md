# Discussion Log

## Task Id

- ai-prompt-snapshot-audit

## Discussion Status

- planned

## Topic

- Persist compact prompt policy metadata for generated messages.

## Participants

- product-planner, db-engineer, backend-engineer, front-engineer, qa-engineer

## Entries

### Entry 1

- Agent:
- product-planner
- Role: scope owner
- Position: Context snapshots show evidence sources, but prompt policy/version metadata is still missing from persisted AI messages.
- Assumptions: Full raw prompt/request retention is too large for this slice; a compact versioned JSON snapshot is enough for MVP auditability.
- Proposed requirements: Add nullable `messages.prompt_snapshot`, persist metadata on generated assistant/persona rows, expose it in timeline DTOs, document it.
- Risks: Snapshot version names become a contract; keep them stable and explicit.
- Questions for other agents: none
- Owner 결정 필요: No

## Consensus

- Implement compact prompt snapshot metadata, not raw prompt archival.

## Disagreements

- None.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- DB migration, backend model/mapper/business/DTO, frontend model, docs, harness report.

