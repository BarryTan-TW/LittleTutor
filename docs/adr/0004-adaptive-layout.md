# ADR-0004: Adaptive layout without a navigation library

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
The app must be comfortable on small phones, phones in landscape and large tablets.
It has only two top-level screens: Home and the writing test.

## Decision
- `MainActivity` calls `enableEdgeToEdge()`. Screens handle insets through Scaffold.
- In `ui/common/Components.kt`:
  - `currentWidthClass()` returns COMPACT (<600dp), MEDIUM (<840dp) or EXPANDED.
  - `isWideLayout()` is true when the width is not COMPACT **or** the screen is landscape.
    Use it to choose between a single column and two panes or more grid columns.
- Secondary flows (unit editor, photo picker, OCR preview, history, settings) use
  `FullScreenDialog(title, onDismiss, actions) { contentModifier -> … }`.
  - You **must** apply `contentModifier`. It carries the top-bar padding and the
    nav-bar overlap fix. In Compose 1.6, dialogs receive no WindowInsets.
  - On wide screens, cap content width (for example `widthIn(max = 640.dp)`) and center it.
- Small confirmations use `ConfirmDialog` (destructive style by default).
- Screen switching is plain state (`isWritingTestActive`, `isXxxOpen`) plus `BackHandler`
  in `LittleTutorApp`. There is no Navigation component.
- Long content scrolls, so landscape phones never clip buttons.

## Consequences
- Every new screen is checked on phone portrait, phone landscape and tablet.
- The main snackbar is hidden behind full-screen dialogs. Show feedback inside the dialog.
- If deep links or many screens are ever needed, adopting Navigation needs a new ADR.
