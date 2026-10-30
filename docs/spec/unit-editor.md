# Spec: Unit editor (add / edit)

- **Status:** Implemented
- **Code:** `ui/unit/UnitDialogs.kt`, `ui/tutor/UnitInput.kt`, `TutorViewModel` (`openAddUnitDialog`, `openEditUnit`, `saveNewUnit`/`saveEditedUnit`, …)
- **Related ADRs:** 0002, 0003

## Behavior
- A full-screen dialog titled 新增課文 or 編輯課文 with:
  - 課文名稱 (example "第一課 春天來了");
  - 字詞 (multi-line). Separators are 、 ， , ； ; and newline. Words are trimmed and
    de-duplicated by `UnitInput.parseWords`. A live count is shown.
- Title errors appear inline: 請輸入課文名稱, or 已有同名課文，請換一個名稱.
  儲存 is disabled until the title is valid and there is at least one word.
- When adding, the "拍課本，直接圈選字詞" section offers 拍照 (camera into the user's
  `photos/` folder; the OCR preview opens automatically) and a grid of earlier photos
  (`Adaptive(112.dp)`). Tapping a photo opens the OCR preview. Photos can be multi-selected
  for deletion with confirmation ("已建立的字詞不受影響").
- Renaming a unit updates the title in history (ADR-0003).

## Acceptance criteria
- [ ] Duplicate titles cannot be saved. Editing a unit keeps its own title valid.
- [ ] Words typed by hand are kept if the user then adds words from a photo.
- [ ] The form scrolls and stays above the keyboard on phones in landscape.
