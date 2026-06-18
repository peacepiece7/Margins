# Requirements Brief

## Task Id

- competitive-reading-product-gap-analysis

## Source Query

- Research services similar to Margins, document their UI and features in detail, create the current project's problem list, and proceed through harness programming from those findings.

## Agreed Requirements

- Compare Margins with mainstream reading trackers, social reading communities, highlight/PKM tools, and AI reading assistants.
- Do not copy tracker-heavy product direction unless it supports the user's requested low-management reading flow.
- Treat the core Margins product as an AI reading room created from one book registration.
- Convert research into a prioritized problem list and implementation backlog.
- Keep decisions durable in project docs and harness work files.

## Acceptance Criteria

- Research document names comparable services, UI patterns, core features, and lessons.
- Problems are grouped into P0, P1, and P2.
- Backlog slices include goal, scope, and acceptance checks.
- SDD/BDD reference the research.
- Harness task state is complete enough for another agent to resume.

## Out Of Scope

- Creating accounts in third-party services.
- Implementing the next UI/AI changes in this task.
- Replacing Open Library or adding RAG.
- Changing production deployment or secrets.

## 적용한 Owner 결정

- Existing AI-owned workflow decision applies: normal analysis, planning, documentation, and report-first work proceed without waiting for owner approval.

## 열린 Owner 결정

- None for this task.

## Agent 논의 요약

- Product planning: Margins should not become another Goodreads/StoryGraph clone; it should make the AI reading room primary.
- Design: the first viewport must explain the full flow through structure, not help text.
- Frontend: generated questions, persona cast, capture, and discussion should live in one workspace.
- Backend/AI: next slices need response evidence, role taxonomy, and progress boundary contracts.
- QA: each slice needs direct BDD/E2E evidence from book registration to prepared reading room.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Research competitors | product-planner | `docs/project/competitive-analysis.md` | Services, UI, features, and source links documented |
| Record project contract | product-planner | `docs/project/sdd.md`, `docs/project/bdd.md` | SDD/BDD point to research and planning behavior |
| Complete harness record | work-coordinator | task files, registry, dashboard, owner report | Work task validation passes |
| Verify docs | qa-engineer | verification report | Doc audit and diff checks pass |
