# Spec: App shell

- **Status:** Implemented
- **Code:** `ui/LittleTutorApp.kt`, `ui/common/Components.kt`, `MainActivity.kt`
- **Related ADRs:** 0001, 0004, 0007

## Behavior
- The app is edge-to-edge. The top bar shows "小小家教" and a 設定 (settings) icon.
  A compact bar is used during a test.
- The screen shown depends on state: Home, or the writing test when `isWritingTestActive`.
  Secondary flows open as `FullScreenDialog`s.
- `BackHandler`:
  - closes the topmost dialog first;
  - during an unfinished test, asks "結束測試？" ("目前的作答進度將不會保存。");
  - from a finished round, returns Home.
- Deleting a unit asks "刪除「{title}」？" and says that its records are deleted too.
- Short feedback uses the Scaffold snackbar (`showMessage`). Dialogs show their own inline feedback.

## Acceptance criteria
- [ ] Back never exits the app while a dialog or a test is open.
- [ ] No content is hidden under the status or navigation bar (gesture or 3-button) on phone or tablet.
