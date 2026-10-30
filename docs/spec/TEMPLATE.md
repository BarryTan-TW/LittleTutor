# Spec: <Feature name>

- **Status:** Draft | Implemented
- **Code:** `ui/<feature>/…`, `TutorViewModel.<functions>`
- **Related ADRs:** ADR-XXXX

## Goal / user story
As a <child | parent>, I want … so that …

## Behavior
Describe the flows step by step: entry points, the main path, and the empty, error and
loading states.

## State & data
New `LittleTutorUiState` fields, ViewModel actions and storage changes (keep them backward compatible).

## Layout
- Phone portrait:
- Phone landscape / tablet (`isWideLayout()`):

## Acceptance criteria
- [ ] Given … when … then …
- [ ] Works in phone portrait, phone landscape and on a tablet (emulator-verified)
- [ ] Destructive actions ask for confirmation; errors are shown inline

## Out of scope / open questions
