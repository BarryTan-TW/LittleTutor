# Architecture Decision Records

Each ADR records one lasting decision. Don't edit an accepted ADR's decision. Write a new
ADR that supersedes it, and update the old ADR's status.

| # | Decision | Status |
|---|---|---|
| [0001](0001-single-viewmodel-state.md) | Single `TutorViewModel` with one immutable `LittleTutorUiState` | Accepted |
| [0002](0002-file-based-per-user-storage.md) | File-based per-user storage with atomic writes | Accepted |
| [0003](0003-records-linked-by-unit-title.md) | Test records link to units by title; titles are unique | Accepted |
| [0004](0004-adaptive-layout.md) | Adaptive layout via `isWideLayout()` and `FullScreenDialog`, no Navigation | Accepted |
| [0005](0005-on-device-ocr.md) | On-device ML Kit Chinese text recognition for photos and the writing board | Accepted |
| [0006](0006-writing-test-round-lifecycle.md) | Writing test round lifecycle: auto-save, practice rounds, stale-result guards | Accepted |
| [0007](0007-ux-principles.md) | Kid/parent UX principles and hardcoded Traditional Chinese UI | Accepted |
| [0008](0008-settings-in-shared-preferences.md) | App-wide preferences in SharedPreferences | Accepted |
| [0009](0009-development-workflow.md) | Feature-by-feature commits, validated on emulators only | Accepted |
