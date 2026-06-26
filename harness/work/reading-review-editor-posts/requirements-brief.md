# Requirements Brief

## Task Id

- reading-review-editor-posts

## Source Query

- 독후감 페이지는 editor 영역으로 텍스트 이미지 등 실제 포스트를 올릴 수 있게 수정할거야 editor 무료 버전을 쓰고 저장, 수정 기능이 있어야해. 환경변수 없이 실행 계획은 나중에 작업한다.

## Agreed Requirements

- The reading review page must provide a rich editor area rather than a plain summary textarea.
- The editor must be free/open-source or locally self-hosted without paid cloud dependency.
- The editor must support text formatting and image insertion.
- Review post data must persist through backend/database storage.
- The user must be able to save a new review post and edit the saved post.
- Environment-variable-independent execution planning is out of scope for this task.

## Acceptance Criteria

- Timeline response includes persisted review post data.
- Save/update API accepts review post title and editor HTML.
- Frontend repository/store expose a save/update review action.
- Session workbench renders the editor state and a saved preview/edit loop.
- Docs identify schema/API/UI behavior and verification limits.

## Out Of Scope

- Binary image upload and file hosting.
- Paid editor services.
- RAG, social login, or unrelated AI changes.
- Runtime execution in this environment.

## 적용한 Owner 결정

- `harness/owner/decisions/2026-06-12-ai-owned-report-first-workflow.md`: proceed with ordinary AI-owned planning and implementation, then report results.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- Current app has session summaries, insights, highlights, and messages, but no dedicated review post. A one-review-per-session contract keeps the implementation aligned with the reading session domain.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement persisted review post contract | backend-engineer/db-engineer | DTOs, mapper, schema, business/controller endpoint | Save/update returns updated timeline |
| Implement frontend editor | frontend-engineer | Editor component/state/repository/store updates | User can save and edit formatted text/image URL content |
| Update docs and static verification | qa-engineer | SDD/BDD and verification report | Evidence maps to acceptance criteria |
