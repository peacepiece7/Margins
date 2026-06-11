# Owner 결정 및 보고 영역

## 목적

이 영역은 project owner가 확인해야 하는 결정, 요청, 결과 보고를 보관합니다.

## 구조

```text
harness/owner/
  dashboard.md # Owner 진입점.
  requests/   # 되돌리기 어렵거나 민감한 작업 전 owner 판단이 필요한 요청.
  decisions/  # 이후 agent가 따라야 하는 owner 결정.
  reports/    # AI 선처리 작업 이후 작성하는 PR 유사 결과 보고.
```

## 규칙

- planning, design, development, QA, revision, commit 전에 `harness/owner/decisions/`를 확인합니다.
- 기록된 owner 결정은 더 새로운 owner 결정으로 대체되기 전까지 유효합니다.
- `requests/`는 실제로 owner 판단이 필요한 경우에만 사용합니다.
- AI가 판단 가능한 작업은 gate 통과 후 진행하고, owner가 지속적으로 확인해야 할 결과는 `reports/`에 남깁니다.
- 보고서는 scope, 변경 파일, 검증 증거, risk, owner가 볼 결과, 후속 선택지를 포함해야 합니다.
- `harness/owner/dashboard.md`는 owner-facing 진입점으로 계속 최신 상태를 유지합니다.
- secret, credential, private endpoint, machine-local access detail은 저장하지 않습니다.
