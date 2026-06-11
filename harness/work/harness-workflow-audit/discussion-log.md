# Discussion Log

## Task Id

- harness-workflow-audit

## Discussion Status

- closed

## Topic

- Verify and strengthen whether the harness can answer even simple queries through deep multi-agent discussion, produce requirements from that discussion, and route only necessary owner decisions to the project owner.

## Participants

- context-curator
- agent-council
- product-planner
- designer
- db-engineer
- backend-engineer
- frontend-engineer
- qa-engineer
- revision-engineer
- commit-manager

## Entries

### Entry 1

- Agent: agent-council
- Role: discussion facilitator
- Position: The harness needs an explicit discussion-first path, not only sequential handoff.
- Assumptions: Simple queries can still affect product behavior, architecture, data, UI, QA, cost, risk, or commit safety.
- Proposed requirements: Add discussion log, requirements brief, owner decision request, and agent-council role.
- Risks: Without a facilitator and durable discussion files, context reset can lose why requirements were chosen.
- Questions for other agents: Which stages must check owner decisions?
- Owner decision needed: No

### Entry 2

- Agent: qa-engineer
- Role: verification
- Position: The harness should mechanically validate the presence of discussion and decision files.
- Assumptions: Documentation-only rules are easy to skip during long work.
- Proposed requirements: Add `validate-work-task.ps1` and require all work-state files.
- Risks: Validation should not block early discussion when open decisions are expected.
- Questions for other agents: Should open decisions fail by default?
- Owner decision needed: No

### Entry 3

- Agent: commit-manager
- Role: commit policy
- Position: Normal commit scope and timing should be AI-owned; the owner should receive a result report.
- Assumptions: The project owner wants to minimize blocking decisions and delegate operational choices.
- Proposed requirements: Commit-manager decides normal commit scope/timing after QA and recursive verification; owner escalation only for high-risk exceptions.
- Risks: The commit manager must still protect unrelated user changes and high-risk decisions.
- Questions for other agents: Which exceptions require owner escalation?
- Owner decision needed: No, user already clarified policy.

## Consensus

- Add `agent-council` as the facilitator for multi-agent requirements discussion.
- Discussion artifacts are required for deep analysis: `discussion-log.md`, `requirements-brief.md`, and `owner-decisions.md`.
- Every stage must identify owner-needed options, but normal commit scope and timing are AI-owned by `commit-manager`.
- Owner escalation is limited to destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous decisions.
- Validation should check required work files and fail on real open owner decisions unless explicitly allowed for pre-irreversible work.

## Disagreements

- None remaining.

## Owner Decisions To Request

- None for current harness policy. The user clarified that commit scope and timing are AI-owned.

## Requirements To Carry Forward

- Maintain discussion-first workflow in README, process, handoffs, sub-agent contract, skills, templates, and project SDD.
- Keep owner decisions as option requests only when owner judgment is genuinely required.
- Ensure validation script supports checking work-state completeness.
