# Feature specs

Each spec describes **current behavior** and its acceptance criteria. Keep them in sync
with the code: a behavior change and its spec update go in the same commit. Start new
features from [TEMPLATE.md](TEMPLATE.md).

| Spec | Area | Code |
|---|---|---|
| [app-shell.md](app-shell.md) | Top bar, back handling, dialogs, messages | `ui/LittleTutorApp.kt`, `ui/common/` |
| [home.md](home.md) | Unit card grid and per-unit actions | `ui/home/` |
| [unit-editor.md](unit-editor.md) | Add/edit unit, photo library | `ui/unit/`, `UnitInput` |
| [photo-ocr.md](photo-ocr.md) | Camera, OCR, tap/lasso selection | `ui/photo/`, `OcrTokenSplitter`, `PhotoBitmaps` |
| [writing-test.md](writing-test.md) | Dictation test, writing board, round result | `ui/writing/` |
| [history.md](history.md) | Score history and word analysis | `ui/history/` |
| [settings.md](settings.md) | Users, testing mode, about | `ui/settings/` |
