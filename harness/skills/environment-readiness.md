# Skill: Environment Readiness

## Use When

Use when tests, builds, local services, Docker, MySQL, browser verification, or generated tooling are needed before implementation or QA can complete.

## Steps

1. Read the affected domain docs and scripts before inventing new environment behavior.
2. Check required tools with direct commands, for example `java -version`, `docker info`, `docker compose version`, or repository scripts.
3. If a tool exists but a service is stopped, start it only when the action is safe, local, and reversible.
4. If a port is occupied and the script supports an override, use the documented override and record it.
5. If a build tool is missing, prefer repository-local scripts and ignored caches over committing ad hoc binaries.
6. Re-run the original failing command after remediation.
7. Document first-run and cached/retry behavior when relevant.
8. Update SDD/BDD when the runtime behavior becomes durable project knowledge.
9. Route real source, schema, or test failures to `revision-engineer`; do not misclassify them as environment blockers.
10. Escalate to the owner only for credential policy, production-impacting service changes, destructive data operations, or machine policy choices.

## Done

- Required runtime command passes, or the blocker is proven with exact evidence.
- Safe remediation is documented in the task verification report.
- Local caches and generated outputs are ignored when they should not be committed.
- The next sub-agent can reproduce the environment path from files alone.
