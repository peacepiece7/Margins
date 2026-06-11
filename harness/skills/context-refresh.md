# Skill: Context Reset And Reinjection

## Use When

Use after context compaction, context window reset, long-running work, repeated QA failure, major scope change, or before commit.

## Steps

1. Read `harness/agents/context-curator.md`.
2. Identify affected paths and their applicable `AGENTS.md` files.
3. Read `harness/process.md` and `harness/handoffs.md`.
4. Read relevant role and skill files for the next owner.
5. Read affected SDD/BDD files.
6. Read applicable `harness/owner/decisions/` files.
7. Read `harness/work/<task-id>/` files when a task id exists.
8. Inspect `git status --short`.
9. Create or update a task packet with loaded sources, current evidence, assumptions, and next owner.

## Context Packet Fields

- Objective
- Current phase
- Loaded sources
- Affected paths
- Acceptance criteria
- Current evidence
- Missing evidence
- Active assumptions
- Open decisions
- Applicable owner decision records
- Next owner sub-agent
- Task directory and next micro-step when present

## Done

- The next sub-agent can continue without relying on prior chat memory.
- All durable decisions point to repository documents.
- Weak or missing context is explicitly listed.
