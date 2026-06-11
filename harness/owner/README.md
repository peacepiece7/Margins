# Owner Decision And Report Area

## Purpose

This area stores project-owner-facing records for decisions and results.

## Layout

```text
harness/owner/
  requests/   # Owner judgment needed before irreversible work.
  decisions/  # Owner decisions that future agents must follow.
  reports/    # PR-like result reports after AI-owned work is completed.
```

## Rules

- Check `harness/owner/decisions/` before planning, design, development, QA, revision, or commit.
- A recorded owner decision is binding until a newer owner decision supersedes it.
- Use `requests/` only when the decision genuinely needs owner judgment.
- For AI-owned decisions, proceed after gates pass and write a report in `reports/` when durable owner visibility is needed.
- Reports must include scope, files changed, evidence, risks, owner-visible outcomes, and follow-up options.
- Do not store secrets, credentials, private endpoints, or machine-local access details.
