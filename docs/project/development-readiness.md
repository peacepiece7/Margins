# Development Readiness

## 목적

MVP 요구사항을 실제 문서, 코드, 테스트, owner 결정 상태에 매핑한다. 이 문서는 다음 개발자가 context를 초기화해도 바로 다음 slice를 선택할 수 있게 하는 기준이다.

## 현재 결론

- Core CRUD/persistence skeleton은 구현되어 있다.
- DB schema와 metric-ready 구조는 MVP 기준으로 준비되어 있다.
- Frontend workbench와 full-stack smoke는 첫 흐름을 검증한다.
- 실제 OpenAI provider, streaming transport, refresh/reload 복구, Raspberry Pi 배포 자동화는 다음 개발 slice다.
- Raspberry Pi target과 secret은 owner 입력 없이는 진행하지 않는다.

## MVP 요구사항 매핑

| 요구사항 | 상태 | 파일 증거 | 남은 작업 |
| --- | --- | --- | --- |
| 책 검색 및 추가 | partial | `BookController.java`, `PlaceholderAiProvider.java`, `marginsRepository.ts` | OpenAI live provider 연결 |
| 책 기반 `ReadingSession` 생성 | implemented | `ReadingSessionController.java`, `ReadingSessionMapper.java`, `ReadingSessionBusinessPersistenceTest.java` | refresh/reload 조회 API |
| session 내부 `SessionWindow` 생성 | implemented | `SessionWindowController.java`, `SessionWindowMapper.java`, `SessionWindowBusinessPersistenceTest.java` | window list/detail 조회 API |
| window별 AI 질문/답변 | partial | `AiProvider.java`, `SessionWindowBusiness.java`, `MessageMapper.java` | OpenAI context assembly, streaming shape 검증 |
| 토론 window의 persona 기반 AI 응답 | partial | `DebateMessageRequest.java`, `SessionWindowBusiness.java`, `001_create_mvp_schema.sql` | persona prompt assembly와 OpenAI provider |
| 모든 대화/기록 DB 저장 | implemented | `MessageMapper.java`, `SessionWindowBusinessPersistenceTest.java`, `2026-06-12-mvp-backend-persistence-slice.md` | 조회/복구 API 확대 |
| metric/statistics 확장 DB 설계 | implemented | `docs/db/sdd.md`, `001_create_mvp_schema.sql`, `004_metric_sources.sql` | 실제 metric job은 후속 |
| 단일 사용자 또는 JWT 로그인 | partial | `AuthController.java`, `AuthBusiness.java`, `docs/back/sdd.md` | JWT filter/security hardening |
| AI streaming/socket 전달 | planned | `docs/back/sdd.md`, `docs/front/sdd.md` | SSE/WebSocket runtime 구현 |
| Raspberry Pi 배포 | blocked | `docs/infra/sdd.md`, `2026-06-12-runtime-secrets-and-deploy-target.md` | owner가 target/secret 제공 필요 |

## 다음 개발 순서

1. OpenAI provider slice
   - `OPENAI_API_KEY`가 있으면 real provider를 구현하고 live smoke를 추가한다.
   - secret이 없으면 provider interface와 context assembly unit test까지만 진행한다.

2. Session reload/read API slice
   - saved book/session/window/message를 조회하는 backend API를 추가한다.
   - frontend refresh recovery BDD를 실제 E2E로 검증한다.

3. Streaming transport slice
   - SSE 또는 WebSocket 중 하나를 선택해 `session.window.ai.delta/done/error`를 구현한다.
   - final assistant message persistence를 streaming completion과 연결한다.

4. Auth hardening slice
   - single-user mode를 유지하되 JWT 발급/검증 filter를 추가한다.
   - test reset endpoint는 local/test profile guard를 유지한다.

5. Deploy packaging slice
   - owner가 Raspberry Pi target을 제공하면 build artifact packaging, transfer, service restart script를 구현한다.

## Owner 판단 필요 영역

- `OPENAI_API_KEY` 제공 여부.
- Raspberry Pi hostname/IP, SSH user, deploy directory, service manager.
- streaming runtime 선택이 제품 UX나 infra 제약에 영향을 줄 경우 SSE vs WebSocket 선택.

현재 열린 요청은 `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md`에 있다.

## 검증 명령

- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-mvp-readiness.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/audit-doc-consistency.ps1`
- `powershell -NoProfile -ExecutionPolicy Bypass -File harness/scripts/validate-work-task.ps1 -TaskId planning-development-readiness-audit`
