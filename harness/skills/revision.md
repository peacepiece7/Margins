# Skill: Revision Loop

## Use When

Use when QA, review, integration, or user feedback reports a defect or mismatch after implementation. This includes redevelopment when a slice must be reworked instead of patched.

## Steps

1. Read `harness/agents/revision-engineer.md`.
2. Read the QA/review finding and affected SDD/BDD files.
3. Reproduce, inspect, or otherwise verify the finding.
4. Make the smallest correct fix that satisfies the documented behavior.
5. Update docs when the fix changes a durable contract.
6. Refresh context when repeated failures suggest stale assumptions.
7. Return the work to QA with commands rerun and remaining risks.

## Done

- Each finding has a fix, documented non-issue decision, or blocker.
- Relevant verification has been rerun.
- The task packet is ready for `qa-engineer`.
