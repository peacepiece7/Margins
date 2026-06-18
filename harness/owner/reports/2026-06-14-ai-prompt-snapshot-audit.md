# Owner Report: AI Prompt Snapshot Audit

## Task ID

- ai-prompt-snapshot-audit

## Status

- reported

## Summary

- Generated assistant and persona response rows now persist compact prompt policy metadata in `messages.prompt_snapshot`.
- The snapshot records prompt contract, response type, provider, model, streaming flag, safety policy, grounding policy, and reading-boundary policy versions.
- Backend timeline/immediate AI DTOs and frontend curated models expose `promptSnapshot`.
- Raw prompt retention and token usage parsing remain deferred.

## Owner Decisions Needed

- None.

## Verification

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests SessionWindowBusinessPersistenceTest`: pass.
- `npm run test:unit` from `front/`: pass.
- `npm run build` from `front/`: pass.
- Harness validation, docs audit, and diff check are recorded in `harness/work/ai-prompt-snapshot-audit/verification-report.md`.

## Next Recommended Slice

- Add token usage capture from OpenAI responses or introduce structured answer validation for citation/uncertainty fields.
