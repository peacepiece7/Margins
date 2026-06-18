# Requirements Brief

## Task Id

- ai-response-grounding-contract

## Source Query

- Continue recursive harness programming after Ask book / Debate personas separation by improving AI response quality and trust.

## Agreed Requirements

- Add a shared OpenAI grounding instruction for window answers, streamed answers, and persona debate.
- The instruction must require use of provided context, evidence references when possible, explicit uncertainty when context is insufficient, and no invented unavailable book details.
- Existing safety policy and reading boundary remain in the developer prompt.
- Tests inspect local fake OpenAI request bodies rather than calling the network.
- Docs and harness registry/report must be updated.

## Acceptance Criteria

- Provider helper exists and is used by all three answer paths.
- Targeted backend test proves answer and debate request bodies include grounding language.
- Back SDD/BDD and competitive analysis describe the contract.
- Harness validation, docs audit, and diff check pass.

## Out Of Scope

- RAG.
- External book APIs.
- New frontend evidence UI.
- JSON/structured response schema validation.

## 적용한 Owner 결정

- AI-owned report-first workflow.

## 열린 Owner 결정

- None.

## Agent 논의 요약

- The agents agreed this is a prompt-contract slice, not a persistence or UI slice.

## Next Micro-Steps

| Step | Owner | Output | Acceptance Check |
| --- | --- | --- | --- |
| Implement prompt helper | backend-engineer | Provider/test changes | Targeted backend test passes |
| Update docs | product-planner | Back/project docs and harness state | Docs audit passes |
| Verify | qa-engineer | Verification report | All commands pass |

