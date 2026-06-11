# Discussion Log

## Task Id

- mvp-db-schema

## Discussion Status

- closed for initial schema implementation

## Topic

- Define MVP DB requirements for Margins reading sessions, AI Q/A, persona debate, persistent records, and future metrics.

## Participants

- agent-council
- product-planner
- db-engineer
- backend-engineer
- frontend-engineer
- qa-engineer

## Entries

### Entry 1

- Agent: product-planner
- Role: product scope
- Position: The schema must cover MVP persistence without introducing RAG, social login, or external book API assumptions.
- Assumptions: Initial auth is single-user compatible but should not prevent JWT users later.
- Proposed requirements: Include `users`, `books`, `book_candidates`, sessions, windows, personas, questions, messages, and metrics.
- Risks: Overfitting schema to first UI can block statistics later.
- Questions for other agents: What metric dimensions should be preserved now?
- Owner decision needed: No

### Entry 2

- Agent: db-engineer
- Role: DB design
- Position: Use raw SQL scripts for the first slice and keep metric records appendable.
- Assumptions: MySQL 8 compatible JSON columns are available.
- Proposed requirements: Use BIGINT ids, typed status/scope columns, JSON snapshots for flexible AI context/token/metric payloads, and seed/reset scripts.
- Risks: Migration tool can be added later; raw SQL needs disciplined script naming.
- Questions for other agents: Should soft delete be universal?
- Owner decision needed: No

### Entry 3

- Agent: backend-engineer
- Role: backend integration
- Position: The backend needs columns that map cleanly to controller/service/business/mapper layers and OpenAPI DTOs.
- Assumptions: MyBatis will use explicit SQL mappings.
- Proposed requirements: Add indexes for session timeline, window message ordering, persona message joins, and metric source grouping.
- Risks: Missing indexes will make timeline reconstruction slow.
- Questions for other agents: What ordering field should messages use?
- Owner decision needed: No

### Entry 4

- Agent: qa-engineer
- Role: verification
- Position: Reset and seed scripts must be deterministic before E2E depends on persisted data.
- Assumptions: Test data can be marked with `is_test_data`.
- Proposed requirements: Seed a local user, book, session, question window, debate window, personas, question, messages, and sample metric.
- Risks: Reset scripts that delete unmarked data are unsafe.
- Questions for other agents: Should reset delete only test-owned rows?
- Owner decision needed: No

## Consensus

- Proceed with raw SQL scripts.
- Use `is_test_data` to make seed/reset deterministic.
- Preserve raw messages and use appendable `metrics`.
- Use both typed metric columns and JSON details for future flexibility.
- Use soft-delete-style `deleted_at` on user-facing durable records while preserving source records.

## Disagreements

- None blocking.

## Owner Decisions To Request

- None. Decisions are AI-owned and reversible enough for MVP DB bootstrap.

## Requirements To Carry Forward

- Implement schema/seed/reset/query scripts and update DB SDD/BDD.
