# Requirements Brief

## Task Id

- highlight-evidence-snapshot

## Source Query

- Continue recursively and close remaining evidence/spoiler UX gaps.

## Agreed Requirements

- Saved quotes should appear in AI evidence trace when available.
- Existing evidence chip UI should render quote references without a new UI pattern.
- Missing current page should be shown as a small capture-card warning, not a large tracker requirement.

## Acceptance Criteria

- Backend snapshot includes highlight references.
- Frontend parser extracts highlight references.
- Capture card renders `reading-boundary-warning` when current page is absent.
- Docs and harness records are updated.
- Verification commands pass.

## Out Of Scope

- Full citation ranking.
- Live OpenAI smoke.
- Screenshot automation.
- New DB migration.

## 적용한 Owner 결정

- AI-owned report-first workflow.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Backend uses existing `SessionHighlightMapper`.
- Frontend extends the existing evidence parser and Workbench board.
- DB docs clarify `context_snapshot` can include highlight ids and text.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Add highlight trace | backend-engineer | backend business/test | backend tests pass |
| Render highlight trace and warning | frontend-engineer | parser/workbench | unit/build pass |
| Verify and record | qa-engineer | report/registry | audits pass |
