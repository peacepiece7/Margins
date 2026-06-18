# Owner Decisions

## Task Id

- simplified-book-onboarding-ai-debate

## Open

- None.

## AI-Owned Defaults

### Default generated counts

- Decision: create 3 reflection questions and 3 generated personas for a new book session.
- Reason: the user requested `n`, but no exact number was supplied. Three gives variety without overwhelming the UI.
- Reversible: yes, can later be made user-configurable.

### First iteration session-persona model

- Decision: save generated book-context personas as active personas using the existing persona table.
- Reason: this avoids a schema migration while completing the requested flow.
- Risk: personas are global, not session-scoped. If this becomes confusing, next iteration should add session-persona scoping.
