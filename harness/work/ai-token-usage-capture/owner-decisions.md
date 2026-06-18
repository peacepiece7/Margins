# Owner 결정

## Task ID

- ai-token-usage-capture

## 열린 결정

- None.

## AI가 결정한 사항

이 결정은 owner 선택을 기다리며 작업을 멈추지 않고, 담당 sub-agent가 결정한 뒤 project owner에게 보고합니다.

### AI 결정 1

- 제목: Store provider usage JSON as-is when present.
- 단계: planning
- 결정 agent: backend-engineer
- 결정: Persist optional usage JSON without provider-normalized columns.
- 근거: This captures audit/cost evidence now without committing to billing semantics.
- 증거: `OpenAiAiProviderFallbackTest`, `SessionWindowBusinessPersistenceTest`
- Owner 보고: `harness/owner/reports/2026-06-14-ai-token-usage-capture.md`
- Status: decided

## 완료된 결정

- None.

