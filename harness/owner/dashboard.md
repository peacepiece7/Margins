# Owner Dashboard

## 목적

이 문서는 project owner가 가장 먼저 확인하는 진입점입니다. 결정 요청, 대기 중인 요청, AI가 선처리한 작업 결과를 한 곳에서 확인합니다.

## 사용 방법

- 먼저 **Owner 조치 필요**를 확인합니다.
- **최근 보고**에서 AI가 먼저 처리하고 이후 보고한 작업을 검토합니다.
- **활성 결정**에서 이후 agent가 반드시 따라야 하는 기준을 확인합니다.
- 전체 작업 이력은 `harness/work/registry.md`에서 확인합니다.

## Owner 조치 필요

| 요청 | 상태 | 필요한 시점 | 요약 |
| --- | --- | --- | --- |
| `harness/owner/requests/2026-06-12-runtime-secrets-and-deploy-target.md` | open | Raspberry Pi 원격 배포 재시도 | `.env`에 OpenAI key와 Raspberry Pi target은 확인되었습니다. 원격 transfer/restart를 위해 `MARGINS_DEPLOY_SSH_KEY` 또는 현재 사용자 SSH 인증 설정이 필요합니다. |

## 최근 보고

| 보고서 | 상태 | 요약 |
| --- | --- | --- |
| `harness/owner/reports/2026-06-12-local-deploy-run-verification.md` | blocked | OpenAI key, artifact build, local backend/frontend 실행은 검증되었습니다. Raspberry Pi 배포는 SSH 인증 부재로 transfer 전 중단되었습니다. |
| `harness/owner/reports/2026-06-12-recursive-feature-review-fixes.md` | reported | 전체 MVP 기능 표면을 추가 review하고 validation, message ordering, frontend async append, generated output ignore 문제를 수정했습니다. |
| `harness/owner/reports/2026-06-12-planning-development-readiness-audit.md` | reported | MVP 요구사항과 구현 증거, 다음 개발 slice, owner input 필요 항목을 분리했습니다. |
| `harness/owner/reports/2026-06-12-docs-consistency-audit.md` | reported | 문서 정합성을 전면 점검하고, 빈 orphan work directory를 정리했으며, 반복 가능한 audit script를 추가했습니다. |
| `harness/owner/reports/2026-06-12-mvp-fullstack-e2e-smoke.md` | reported | Playwright full-stack smoke가 통과했고, `5b633cc`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-mvp-frontend-skeleton.md` | reported | Frontend skeleton build와 dev server 응답을 검증했고, `f17f7c0`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-mvp-test-reset-runtime.md` | reported | Test reset endpoint가 JDBC로 seed data를 복구하도록 구현했고, `570a749`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-harness-autonomy-upgrade.md` | reported | 작업 조율과 환경 준비도 지원을 검증했고, `c809937`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-mvp-backend-persistence-slice.md` | reported | Backend persistence slice를 test, runtime API flow, SQL 증거, seed 복구로 검증했고, `43c3fef`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-mvp-infra-mysql-runtime.md` | reported | MySQL runtime을 port `3307`에서 schema/seed 적용 상태로 검증했고, `d93d797`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-mvp-build-tooling.md` | reported | Backend test script가 최초 실행과 cached Gradle 실행에 통과했고, `9a308ec`에 commit되었습니다. |
| `harness/owner/reports/2026-06-12-mvp-backend-skeleton.md` | reported | Backend skeleton을 `9124315`에 commit했습니다. Gradle 또는 wrapper 준비 전까지 test 실행은 보류 상태입니다. |
| `harness/owner/reports/2026-06-12-mvp-db-schema.md` | reported | MVP DB schema, seed/reset/query script, 관련 문서를 `7224f15`에 commit했습니다. |
| `harness/owner/reports/2026-06-12-harness-owner-area.md` | reported | Owner 결정/보고 영역을 추가하고, AI 선처리 후보고 workflow를 연결했습니다. |

## 활성 결정

| 결정 | 상태 | 요약 |
| --- | --- | --- |
| `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md` | active | 일반 작업은 AI가 먼저 처리하고 이후 결과를 보고합니다. owner escalation은 고위험 예외 상황에만 사용합니다. |

## 작업 이력

- `harness/work/registry.md`
