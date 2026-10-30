# ADR-0001: Single ViewModel with one immutable UI state

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
The app is small: one activity, a home screen, a test screen and a few full-screen dialogs.
State is shared across features. For example, a unit's latest score appears on Home, the
current user affects every list, and finishing a test refreshes history.

## Decision
- `TutorViewModel` (an `AndroidViewModel`) owns one `MutableStateFlow<LittleTutorUiState>`.
  The UI collects it with `collectAsStateWithLifecycle()` in `LittleTutorApp`.
- `LittleTutorUiState` is an immutable data class. Each feature adds fields to it with a
  prefix (`writing*`, `preview*`, `editingUnit*`, `isXxxOpen`, …).
- State changes only happen in ViewModel functions, via `_uiState.update { it.copy(...) }`.
- Screen composables take **values and callbacks** and never the ViewModel.
  `LittleTutorApp.kt` connects them.
- Pure logic (parsing, geometry, splitting, name rules) lives in small `object`s or a
  `companion object`, so JVM unit tests can cover it without Android.

## Consequences
- It is easy to follow and to preview. Screens stay stateless except for purely visual
  local state such as strokes, tabs and expanded cards.
- `TutorViewModel` is large. If it grows a lot more, split it into per-feature
  controllers that share one state holder. Record that in a new ADR first.
