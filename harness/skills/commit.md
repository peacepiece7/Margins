# Skill: Commit Preparation

## Use When

Use after QA passes and commit is the next process step. The commit manager chooses normal commit scope and timing, then reports the result to the project owner.

## Steps

1. Read `harness/agents/commit-manager.md`.
2. Run context refresh.
3. Confirm recursive verification passed.
4. Run `git status --short`.
5. Inspect the final diff for intended scope.
6. Decide commit scope and timing.
7. Escalate only destructive, security-sensitive, credential-related, production-impacting, or explicitly ambiguous commit decisions to the project owner.
8. Confirm required docs and tests are present or documented as unavailable.
9. Create a concise commit message.
10. Commit only the intended files.
11. Write or update an owner result report when durable owner visibility is needed.

## Done

- Commit contains only intended changes.
- Commit message describes the outcome.
- Commit hash is reported.
