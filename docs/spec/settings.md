# Spec: Settings

- **Status:** Implemented
- **Code:** `ui/settings/SettingsDialog.kt`, `TutorViewModel` (`addUser`, `switchUser`, `deleteUser`, `setTestingMode`, `isDuplicateUserName`)
- **Related ADRs:** 0002, 0007, 0008

## Behavior
- A full-screen, scrollable dialog with content width capped at 640dp. 完成 (done) or ×
  closes it.
- **使用者** (users):
  - users are radio cards; tapping one switches to it, and the current user shows 使用中;
  - adding a user (name field + 新增, or IME Done) creates the user **and switches to them**;
  - duplicate names (case-insensitive, trimmed) show "已經有這個名字了" and 新增 is disabled;
  - the 🗑 icon opens a confirmation that all of that user's units, photos and scores
    will be deleted;
  - when only one user remains, the icon is hidden and "至少要保留一位使用者。" is shown;
  - deleting the current user switches to the first remaining user.
- **聽寫方式** (testing mode): radio cards 自行確認 (default) and 畫板書寫, each with a
  one-line explanation. The choice is persisted.
- **關於** (about): "LittleTutor 小老師 v{versionName}", 作者 Barry Tan, Email.

## Acceptance criteria
- [ ] The testing mode and speech rate survive an app restart.
- [ ] The last user cannot be deleted, either from the UI or through the ViewModel guard.
- [ ] All sections are reachable by scrolling on a phone in landscape.
