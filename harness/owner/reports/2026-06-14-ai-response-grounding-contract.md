# Owner Report: AI Response Grounding Contract

## Task ID

- ai-response-grounding-contract

## Status

- reported

## Summary

- OpenAI book answers, streamed book answers, and persona debate replies now share a response grounding contract.
- The contract requires use of provided session context, quote/note/message/question evidence when possible, uncertainty when context is insufficient, no invented unavailable book details, and reading-boundary respect.
- This remains a prompt-level MVP contract; structured response schema validation is deferred.

## Owner Decisions Needed

- None.

## Verification

- `powershell -NoProfile -ExecutionPolicy Bypass -File back\scripts\test.ps1 --tests OpenAiAiProviderFallbackTest`: pass.
- Harness validation, docs audit, and diff check are recorded in `harness/work/ai-response-grounding-contract/verification-report.md`.

## Next Recommended Slice

- Add prompt/version audit metadata to persisted AI messages, or add stricter structured output validation if prompt-only grounding proves insufficient.
