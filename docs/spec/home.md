# Spec: Home

- **Status:** Implemented
- **Code:** `ui/home/HomeScreen.kt`
- **Related ADRs:** 0003, 0004, 0007

## Goal
Parent or child sees all units and can start a test with one tap.

## Behavior
- The header reads "我的課文（N）". A grid of unit cards uses `GridCells.Adaptive(320.dp)`:
  one column on phones, more on tablets and in landscape.
- Each card shows:
  - the title;
  - the words joined with 、 (ellipsized);
  - "N 個字詞";
  - "最近 xx%" from the latest record for that title, color coded;
  - a large **開始** button;
  - a ⋮ menu (更多操作) with 測試歷史, 編輯內容, 上移, 下移 and 刪除 (shown in red, with confirmation).
    上移 and 下移 are disabled at the ends of the list.
- An extended FAB labelled 新增課文 opens the unit editor.
- Empty state: 📚, "還沒有課文", an explanation and a large 新增課文單元 button.

## Acceptance criteria
- [ ] Starting a unit with no words shows a message instead of an empty test.
- [ ] The order of units is persisted after moving one up or down.
- [ ] The FAB never covers the last card's buttons (bottom content padding).
