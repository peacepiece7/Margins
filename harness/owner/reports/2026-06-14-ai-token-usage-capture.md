# Owner Report: AI Token Usage Capture

## Task ID

- ai-token-usage-capture

## Status

- reported

## Summary

- OpenAI `usage` metadata is now captured from JSON responses and streaming completion events when available.
- Generated assistant and persona message rows persist provider usage JSON in `messages.token_usage`.
- Immediate AI responses, timeline DTOs, and frontend curated models expose optional `tokenUsage`.
- Placeholder/fallback responses can leave token usage null.

## Owner Decisions Needed

- None.

## Verification

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests OpenAiAiProviderFallbackTest --tests SessionWindowBusinessPersistenceTest`: pass.
- `npm run test:unit` from `front/`: pass.
- `npm run build` from `front/`: pass.
- Harness validation, docs audit, and diff check are recorded in `harness/work/ai-token-usage-capture/verification-report.md`.

## Next Recommended Slice

- Add structured answer validation for citation and uncertainty fields, or build an internal AI audit/debug panel using `promptSnapshot`, `contextSnapshot`, and `tokenUsage`.
