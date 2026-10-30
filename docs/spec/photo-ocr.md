# Spec: Photo OCR word picking

- **Status:** Implemented
- **Code:** `ui/photo/PhotoOcr.kt`, `ui/photo/LassoGeometry.kt`, `ui/tutor/OcrTokenSplitter.kt`, `ui/tutor/PhotoBitmaps.kt`, `TutorViewModel.openPhotoPreview` / `savePreviewSelectionToUnit`
- **Related ADRs:** 0005

## Behavior
- Opening a photo shows "圈選字詞". Decoding (EXIF-upright) and OCR run off the main
  thread while "正在辨識文字…" is shown.
- Recognized words are drawn as boxes over the photo. **Tap** toggles a word. **Drawing a
  loop** (lasso) selects every word whose center is inside it. The hint shows "已選 N 個".
- Toolbar: 全選, 反選, 清除, 復原圈選 (undo the last lasso).
- Selected words are listed as chips ("已選字詞（N）"); tap × to remove one.
- 存到課文:
  - type a new title, or pick an existing unit from the dropdown;
  - saving appends the words, de-duplicated, to that unit, or creates the unit;
  - the snackbar says "已加入 N 個字詞到「…」" or "已新增「…」".
- Errors: if the photo can't be opened, show 無法開啟此照片. If there is no text, show the
  "請在光線充足處把課本拍清楚" hint.
- Layout: on wide screens, the photo and the selection panel sit side by side. On phones,
  the photo is on top and the panel below.

## Acceptance criteria
- [ ] Photos taken in any orientation are shown upright, and the boxes line up with the text.
- [ ] Large photos do not cause OOM (downsampled to 2048 for OCR and 1600 for preview).
- [ ] Words of several characters are split into separate tokens when the gap between them is large.
