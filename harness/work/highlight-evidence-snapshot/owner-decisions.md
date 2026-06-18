# Owner 결정

## Task ID

- highlight-evidence-snapshot

## 열린 결정

- None.

## AI가 결정한 사항

### AI 결정 1

- 제목: saved quote evidence는 기존 context snapshot에 추가한다.
- 단계: development
- 결정 agent: backend-engineer
- 결정: `references.highlights[]`를 `messages.context_snapshot` JSON에 추가한다.
- 근거: Existing DB column is versioned evidence metadata and avoids unnecessary migration.
- 증거: `SessionWindowBusinessPersistenceTest`, `aiEvidence.test.ts`
- Owner 보고: `harness/owner/reports/2026-06-14-highlight-evidence-snapshot.md`
- Status: decided

## 완료된 결정

- None.
