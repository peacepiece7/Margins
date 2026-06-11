# Core Domain Model

## Domain Ownership

| Domain | Purpose | Persistence Required |
| --- | --- | --- |
| `User` | Owner of sessions and records | Yes |
| `Book` | Selected book metadata and AI candidate source | Yes |
| `ReadingSession` | A user's reading/reflection workspace for one book | Yes |
| `SessionWindow` | A focused area inside a session, such as question or debate | Yes |
| `Message` | User, AI, system, and persona conversation records | Yes |
| `Persona` | Debate participant definition and prompt profile | Yes |
| `Question` | AI-generated or curated prompt attached to a window/session | Yes |
| `Metric` | Future aggregate or derived analysis value | Yes |

## Relationship Sketch

```text
User 1 --- n ReadingSession
Book 1 --- n ReadingSession
ReadingSession 1 --- n SessionWindow
SessionWindow 1 --- n Message
SessionWindow 1 --- n Question
Persona 1 --- n Message
User 1 --- n Metric
Book 1 --- n Metric
ReadingSession 1 --- n Metric
```

## Message Requirements

- Every user note, AI answer, persona debate response, and system prompt decision that matters for replay must be persisted.
- Messages need role, source window, ordering, timestamps, and optional persona/question linkage.
- Store enough context metadata to calculate future metrics without parsing free text first.

## Metric Design Constraints

- Metrics should be appendable or recomputable.
- Metric tables must support user/book/session/window scope.
- Avoid hardcoding only one metric type. Use a metric type/code plus measured value or JSON details when needed.
