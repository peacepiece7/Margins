# Discussion Log

## Task Id

- ai-response-grounding-contract

## Discussion Status

- planned

## Topic

- AI answer grounding contract for book questions and persona debate.

## Participants

- product-planner, backend-engineer, qa-engineer

## Entries

### Entry 1

- Agent:
- product-planner
- Role: scope owner
- Position: Evidence snapshots and page boundaries are implemented, but answer prose still needs a direct contract that makes model behavior consistent.
- Assumptions: Plain-text prompt rules are sufficient for MVP; structured response schema can wait.
- Proposed requirements: Use one shared grounding instruction for answer, stream, and debate prompts; keep safety and page-boundary instructions.
- Risks: Prompt-only enforcement is weaker than schema validation, so tests should assert request-body contract and docs should record the limitation.
- Questions for other agents: none
- Owner 결정 필요: No

## Consensus

- Add a shared provider instruction for grounding, uncertainty, and no invention; verify through fake OpenAI request bodies.

## Disagreements

- None.

## 요청할 Owner 결정

- None.

## 이어서 반영할 요구사항

- Backend prompt helper, provider tests, backend BDD/SDD, competitive analysis, harness report.

