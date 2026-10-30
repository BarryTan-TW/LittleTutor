# AGENTS.md — Guide for AI agents and new contributors

Read this file first, then the ADRs in [`docs/adr/`](docs/adr/README.md) and the
feature spec for the area you touch in [`docs/spec/`](docs/spec/README.md).
These documents describe how the app works today and the decisions that must stay
consistent. If you change behavior, update the matching spec in the same commit.

## Product in one paragraph
LittleTutor (小小家教) is an Android dictation (聽寫) helper for elementary-school
children and their parents. A parent creates **units** (課文) of words, typed by hand
or picked from a textbook photo via on-device OCR. The child takes a **writing test**:
the app speaks each word (TTS) and the child writes it on paper (parent confirms ✓/✗)
or on a **writing board** that is checked by OCR. Results are saved to **history**
with per-word analysis. Several children can share a device (**users**).

## Tech & commands
- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06, Material3 1.2), AGP 8.5.2, Gradle 9.0.
- minSdk 26, targetSdk 35. Only *core* Material icons are available (no extended icons).
- ML Kit Chinese text recognition (on-device), Android TextToSpeech.
- Build + unit tests (the `gradlew` file is not executable, so run it through `bash`):
  ```bash
  export ANDROID_HOME=$HOME/Android/Sdk
  bash gradlew testDebugUnitTest assembleDebug
  ```

## Code map (`app/src/main/java/com/littletutor/app/`)
| Path | Responsibility |
|---|---|
| `MainActivity.kt` | Edge-to-edge host; sets `LittleTutorApp()` |
| `ui/LittleTutorApp.kt` | App shell: Scaffold, top bar, snackbar, BackHandler, which screen or dialog is shown |
| `ui/tutor/TutorViewModel.kt` | **Single** ViewModel: `LittleTutorUiState`, all actions, OCR, TTS, scoring |
| `ui/tutor/UserSpaceManager.kt` | File storage per user (units, photos, records) |
| `ui/tutor/UnitInput.kt` | Word parsing and unit-title validation (pure, tested) |
| `ui/tutor/OcrTokenSplitter.kt`, `PhotoBitmaps.kt` | OCR token splitting, EXIF-aware decoding (pure/tested) |
| `ui/common/Components.kt` | Shared UI: `WidthClass`, `isWideLayout()`, `FullScreenDialog`, `ConfirmDialog`, `ZhuyinText` |
| `ui/home/` | Home: unit card grid |
| `ui/unit/` | Add/edit unit dialogs, photo library |
| `ui/photo/` | OCR preview, tap/lasso word picking (`LassoGeometry`) |
| `ui/writing/` | Writing test screen, writing board, round result |
| `ui/history/` | Score history (records + word analysis) |
| `ui/settings/` | Users, testing mode, about |

Unit tests live in `app/src/test/java/...` (JUnit4 + Truth). Pure logic gets tests; UI is
verified on emulators.

## Rules that must not be broken
1. All state lives in `LittleTutorUiState` and changes only through `TutorViewModel`
   ([ADR-0001](docs/adr/0001-single-viewmodel-state.md)). Composables receive plain values and
   callbacks; screens never take the ViewModel as a parameter.
2. File I/O goes through `UserSpaceManager` using `writeTextAtomic`, keeping the existing
   formats backward compatible ([ADR-0002](docs/adr/0002-file-based-per-user-storage.md)).
3. Unit titles are unique per user, because history links to units **by title**
   ([ADR-0003](docs/adr/0003-records-linked-by-unit-title.md)).
4. Every screen must work on phone portrait, phone landscape and tablet. Use `isWideLayout()`
   and `FullScreenDialog` ([ADR-0004](docs/adr/0004-adaptive-layout.md)).
5. Heavy work (decoding, OCR, recognition) runs off the main thread. Async results must be
   ignored if the user has moved on ([ADR-0006](docs/adr/0006-writing-test-round-lifecycle.md)).
6. UI text is hardcoded Traditional Chinese, written for kids and parents
   ([ADR-0007](docs/adr/0007-ux-principles.md)). Destructive actions always ask for
   confirmation and name what will be deleted.
7. Validate on **emulators only**. Never run commands against the developer's physical device:
   always pass `adb -s emulator-XXXX`, and never run bare `adb` or `adb devices`
   ([ADR-0009](docs/adr/0009-development-workflow.md)).

## How to add a feature
1. Write or extend a spec in `docs/spec/` (copy `docs/spec/TEMPLATE.md`), including acceptance criteria.
2. If you make a lasting technical choice, add an ADR (copy `docs/adr/TEMPLATE.md`, next number).
3. Add state to `LittleTutorUiState` and actions to `TutorViewModel`. Put pure logic in a
   small object with unit tests.
4. Build the UI in `ui/<feature>/`, reusing `Components.kt`, and wire it in `LittleTutorApp.kt`.
5. Build and run the tests. Check phone portrait and landscape and a tablet on emulators.
6. Make one commit per feature, with the message `Area: summary` and a bullet body.
   Update the README (EN + 繁中) when users can see the change.

## Known limitations / backlog
- `ZhuyinHelper` covers only some characters, so zhuyin shows for only some words.
- Strings are not in `strings.xml` (no localization).
- The snackbar belongs to the main Scaffold, so it is hidden behind full-screen dialogs.
  Show inline messages inside dialogs instead.
- `TutorViewModel` is large (~1000 lines). Splitting it per feature is a possible future ADR.
