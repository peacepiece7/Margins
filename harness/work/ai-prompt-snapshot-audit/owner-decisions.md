# Owner 결정

## Task ID

- ai-prompt-snapshot-audit

## 열린 결정

- None.

## AI가 결정한 사항

이 결정은 owner 선택을 기다리며 작업을 멈추지 않고, 담당 sub-agent가 결정한 뒤 project owner에게 보고합니다.

### AI 결정 1

- 제목: Store compact prompt policy metadata, not raw prompt text.
- 단계: planning
- 결정 agent: product-planner
- 결정: Add versioned JSON prompt snapshots to generated messages with policy metadata only.
- 근거: This gives audit traceability while avoiding large raw prompt bodies and sensitive context duplication.
- 증거: `SessionWindowBusinessPersistenceTest`
- Owner 보고: `harness/owner/reports/2026-06-14-ai-prompt-snapshot-audit.md`
- Status: decided

## 완료된 결정

- None.

