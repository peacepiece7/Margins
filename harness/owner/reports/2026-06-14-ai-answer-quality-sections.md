# Owner Report: AI Answer Quality Sections

## Task ID

- ai-answer-quality-sections

## Status

- reported

## Summary

- OpenAI answer and persona debate prompts now include response-structure instructions for `Evidence:` and `Uncertainty:` sections.
- Final OpenAI answer/debate content is normalized by `AiAnswerQualityPolicy` so missing sections are appended before persistence.
- SSE deltas remain provider text while the final `message.done` content is normalized.

## Owner Decisions Needed

- None.

## Verification

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests OpenAiAiProviderFallbackTest`: pass.
- Harness validation, docs audit, and diff check are recorded in `harness/work/ai-answer-quality-sections/verification-report.md`.

## Next Recommended Slice

- Full JSON structured output validation, or an internal AI audit/debug panel using prompt, context, token, and quality metadata.
