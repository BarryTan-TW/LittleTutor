# Spec: Score history

- **Status:** Implemented
- **Code:** `ui/history/ScoreHistoryDialog.kt`, `TutorViewModel.openScoreHistory` / `clearScoreHistory`
- **Related ADRs:** 0002, 0003

## Behavior
- Opened from a unit's menu (測試歷史) or from the round result (查看成績).
  The title is "成績・{unit}", or "全部成績" when no unit is given.
- Summary tiles: 測驗次數 (number of tests), 最近 (latest), 平均 (average), 最佳 (best).
- **考試紀錄** (records): newest first. Each card shows the date (`yyyy/MM/dd HH:mm`,
  zh-TW), the score, a progress bar and the accuracy color. Tap a card to expand it and see
  the words written wrong.
- **字詞分析** (word analysis): per-word attempts and error rate, sorted by error rate
  (descending), then by wrong count (descending), then by word. The color shows severity.
- 清除紀錄 appears only when records exist and asks for confirmation. When opened for a
  unit, it clears only that unit's records.
- Empty state: 📝 "還沒有考試紀錄", which says that results appear automatically after a test.

## Layout
- Phone: tabs (考試紀錄 / 字詞分析). Wide screens: two columns side by side.

## Acceptance criteria
- [ ] History follows unit renames and deletes (ADR-0003).
- [ ] Clearing the history of one unit leaves the other units' history untouched.
