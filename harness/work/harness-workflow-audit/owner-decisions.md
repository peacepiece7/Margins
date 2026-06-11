# Owner Decisions

## Task Id

- harness-workflow-audit

## Open Decisions

- None.

## AI-Owned Decisions

### AI Decision 1

- Title: Normal commit scope and timing
- Stage: commit
- Deciding agent: commit-manager
- Decision: `commit-manager` decides normal commit scope and timing after QA and recursive verification.
- Rationale: The project owner clarified that AI should decide commit scope and timing and the owner should mostly receive the result report.
- Evidence: `harness/process.md`, `harness/agents/commit-manager.md`, `harness/skills/commit.md`, `docs/project/sdd.md`.
- Owner report: report commit scope, timing rationale, checks, commit message, and commit hash after commit.
- Status: decided

## Resolved Decisions

### Resolved Decision 1

- Title: Owner role in commit timing
- Stage: commit
- Chosen option: AI-owned normal commit scope/timing with owner report.
- Decision date: 2026-06-12
- Owner rationale: "Commit scope and timing also let AI decide; owner mostly receives the result."
- Tradeoffs accepted: Faster autonomous flow; owner escalation remains for high-risk exceptions.
- Follow-up actions: Update commit gate, commit-manager role, commit skill, owner decision skill, and project SDD.
- Recorded in: `docs/project/sdd.md`
- Status: resolved
