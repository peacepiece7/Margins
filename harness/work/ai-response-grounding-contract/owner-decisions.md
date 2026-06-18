# Owner 결정

## Task ID

- ai-response-grounding-contract

## 열린 결정

- None.

## AI가 결정한 사항

이 결정은 owner 선택을 기다리며 작업을 멈추지 않고, 담당 sub-agent가 결정한 뒤 project owner에게 보고합니다.

### AI 결정 1

- 제목: Use prompt-level grounding contract before structured response schema.
- 단계: planning
- 결정 agent: product-planner
- 결정: Add shared OpenAI developer-prompt instructions for evidence grounding, uncertainty, and no invention.
- 근거: MVP already persists context snapshots and page boundaries; this slice improves answer quality without changing API or UI contracts.
- 증거: `OpenAiAiProviderFallbackTest`
- Owner 보고: `harness/owner/reports/2026-06-14-ai-response-grounding-contract.md`
- Status: decided

## 완료된 결정

- None.

