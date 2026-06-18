# Owner 결정

## Task ID

- spoiler-progress-boundary

## 열린 결정

- None.

## AI가 결정한 사항

### AI 결정 1

- 제목: spoiler boundary를 backend prompt context부터 적용한다.
- 단계: development
- 결정 agent: backend-engineer
- 결정: `SessionWindowContext`에 progress fields를 추가하고 OpenAI context에 reading boundary instruction을 넣는다.
- 근거: Existing progress columns are already authoritative and this improves AI behavior without new UX burden.
- 증거: `OpenAiAiProviderFallbackTest`
- Owner 보고: `harness/owner/reports/2026-06-13-spoiler-progress-boundary.md`
- Status: decided

## 완료된 결정

- None.
