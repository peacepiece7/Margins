# Discussion Log

## Task Id

- mvp-test-reset-runtime

## Discussion Status

- completed

## Topic

- Make backend test reset real now that persistence writes test-owned rows.

## Participants

- agent-council
- backend-engineer
- db-engineer
- qa-engineer
- commit-manager

## Entries

### Entry 1

- Agent: agent-council
- Role: sequencing
- Position: Persistence writes are verified, so a real reset endpoint is the next blocker for recursive E2E work.
- Assumptions: Reset remains local/test only.
- Proposed requirements: Replace placeholder response with real DB reset.
- Risks: Reset must not be available in production profiles.
- Questions for other agents: Does this require owner choice?
- Owner decision needed: No

### Entry 2

- Agent: backend-engineer
- Role: implementation
- Position: Add `TestDataResetExecutor` and JDBC implementation so business guard stays separate from SQL execution.
- Assumptions: Seed SQL remains the DB source of truth.
- Proposed requirements: Delete `is_test_data=true` rows and reapply seed script.
- Risks: Seed script path must be configurable.
- Questions for other agents: None.
- Owner decision needed: No

### Entry 3

- Agent: qa-engineer
- Role: verification
- Position: Verify both guard behavior and runtime DB row counts.
- Assumptions: MySQL runtime on `3307` is available.
- Proposed requirements: Create a test row, call reset, confirm seed counts.
- Risks: Ensure dev server is stopped after verification.
- Questions for other agents: None.
- Owner decision needed: No

## Consensus

- Implement JDBC reset executor and verify through unit and runtime tests.

## Disagreements

- None.

## Owner Decisions To Request

- None.

## Requirements To Carry Forward

- Reset endpoint is local/test only.
- Reset must restore deterministic seed state.
