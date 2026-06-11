# Discussion Log

## Task Id

- mvp-fullstack-e2e-smoke

## Discussion Status

- completed

## Topic

- Add first full-stack browser smoke after frontend skeleton.

## Participants

- agent-council
- frontend-engineer
- qa-engineer
- environment-engineer
- commit-manager

## Entries

### Entry 1

- Agent: agent-council
- Role: sequencing
- Position: Browser E2E was the main weak evidence after frontend skeleton.
- Assumptions: Backend persistence and reset are available.
- Proposed requirements: Add a smoke that runs through the MVP workbench.
- Risks: Browser binaries may be missing.
- Questions for other agents: Is browser installation an owner decision?
- Owner decision needed: No

### Entry 2

- Agent: frontend-engineer
- Role: implementation
- Position: The UI must show the user's message as well as the persisted assistant/persona responses.
- Assumptions: User messages can be displayed optimistically while backend returns the final AI response.
- Proposed requirements: Add display message view-model and E2E selectors.
- Risks: Refresh recovery remains later work.
- Questions for other agents: None.
- Owner decision needed: No

### Entry 3

- Agent: environment-engineer
- Role: runtime readiness
- Position: Missing Playwright browser is a local environment issue.
- Assumptions: Installing Chromium is safe and local.
- Proposed requirements: Run `npx playwright install chromium` and retry.
- Risks: Browser download is large.
- Questions for other agents: None.
- Owner decision needed: No

### Entry 4

- Agent: qa-engineer
- Role: verification
- Position: Start backend and frontend, run E2E, stop servers, restore DB seed.
- Assumptions: MySQL remains available on port `3307`.
- Proposed requirements: Record exact pass/fail and cleanup.
- Risks: E2E server orchestration should be automated later.
- Questions for other agents: None.
- Owner decision needed: No

## Consensus

- Add Playwright E2E smoke now.
- Treat browser install as environment remediation.

## Disagreements

- None.

## Owner Decisions To Request

- None.

## Requirements To Carry Forward

- E2E should reset data before the test and DB seed should be restored after verification.
