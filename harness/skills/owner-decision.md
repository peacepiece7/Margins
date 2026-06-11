# Skill: Project Owner Decision Options

## Use When

Use whenever a process stage finds a choice that affects product scope, UX, architecture, data, cost, risk, verification depth, or high-risk commit exceptions. Normal commit scope and timing are AI-owned by `commit-manager` and reported to the project owner.

## Steps

1. Read `harness/templates/owner-decision-request.md`.
2. State why the decision is needed now.
3. Provide 2-3 mutually exclusive options.
4. Mark a recommended option.
5. Explain tradeoffs, domain impact, reversibility, and consequence of delay.
6. Record the final owner choice in `owner-decisions.md`.
7. Create or update a binding record under `harness/owner/decisions/`.
8. Copy durable product/technical implications into the relevant `docs/<domain>/sdd.md` or `bdd.md`.

## Done

- The project owner can choose only the decisions that genuinely require owner judgment without decoding agent debate.
- The chosen option is recorded before irreversible work proceeds.
- Sub-agents can resume using the recorded decision.
