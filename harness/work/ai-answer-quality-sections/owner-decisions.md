# Owner 결정

## Task ID

- ai-answer-quality-sections

## 열린 결정

- None.

## AI가 결정한 사항

이 결정은 owner 선택을 기다리며 작업을 멈추지 않고, 담당 sub-agent가 결정한 뒤 project owner에게 보고합니다.

### AI 결정 1

- 제목: Use text-section normalization before full structured JSON output.
- 단계: planning
- 결정 agent: backend-engineer
- 결정: Ensure final OpenAI content includes `Evidence:` and `Uncertainty:` sections.
- 근거: This improves auditability without breaking existing SSE streaming behavior.
- 증거: `OpenAiAiProviderFallbackTest`
- Owner 보고: `harness/owner/reports/2026-06-14-ai-answer-quality-sections.md`
- Status: decided

## 완료된 결정

- None.

