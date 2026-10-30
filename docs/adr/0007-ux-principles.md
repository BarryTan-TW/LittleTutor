# ADR-0007: UX principles and UI language

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
The users are elementary-school children (writing tests) and parents (managing units,
users and history), mostly in Taiwan.

## Decision
- UI text is **Traditional Chinese, hardcoded in the composables** (no `strings.xml` yet),
  with short, friendly sentences. Use 「」 for quotes and full-width punctuation.
- Touch targets are at least 48dp. Primary test actions are large; manual ✓/✗ buttons are 72dp.
- Every list has an **empty state** (emoji + title + one line of what to do next).
- **Destructive actions** (delete unit, photos, user, history; exit a test) use
  `ConfirmDialog`. Its message names what will be lost and says it cannot be undone.
  The last user cannot be deleted.
- Validation errors appear **inline** under the field (`isError` + `supportingText`).
  Save buttons are disabled while input is invalid.
- Progress and feedback are always visible: progress bars, "正在辨識文字…", saved notes,
  and emoji on results.
- Selectable options use cards with radio semantics (`selectable` + `Role.RadioButton`).
- Icons have Chinese `contentDescription` values, so accessibility services read them and
  UI automation can find them.

## Consequences
- Adding localization later means extracting strings. Until then, keep all text in Chinese.
- New screens follow the same patterns, so the app feels consistent.
