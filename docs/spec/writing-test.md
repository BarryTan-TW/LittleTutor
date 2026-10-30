# Spec: Writing (dictation) test

- **Status:** Implemented
- **Code:** `ui/writing/WritingTestScreen.kt`, `TutorViewModel` (writing functions, TTS)
- **Related ADRs:** 0005, 0006, 0008

## Behavior
- **Header:**
  - × (結束測试) to exit, the unit title (plus "・錯字練習" in practice rounds), a progress
    bar and "i / n";
  - one "？" box per character, with zhuyin where available;
  - 🔊 再聽一次 and a speech-rate slider (0.5–2.0×, persisted);
  - a warning if TTS is unavailable.
- Each word is spoken automatically when it appears.
- **Manual confirm** (default): the child writes on paper. The parent can use 看答案 /
  隱藏答案 (show / hide answer) and presses the large ✓ 正確 or ✗ 錯誤 buttons (72dp).
  The app advances after 400 ms; double taps are ignored.
- **Writing board**:
  - a 田字格 guide; strokes are captured without touch slop, so dots count;
  - ↶ 復原 (undo) and 清空 (clear) buttons;
  - recognition runs off the main thread after **every stroke** (and after undo), showing
    "辨識中…" and then "辨識：…" with "✓ 寫對了！" or "再試試看";
  - "下一題 →" advances (完成 on the last word);
  - the board clears on every new word and on rotation.
- **Round result:**
  - "本輪完成！", an emoji, and "答對 c / n（p%）";
  - "成績已自動儲存", or "錯字練習不列入成績" in practice rounds;
  - lists of 需要加強（n） and 答對（n）, or "全部答對，太棒了！";
  - buttons 練習錯字（n） (only if there are mistakes), 再測一輪, 查看成績 and 回到首頁.
- Exiting before the end asks for confirmation.

## Layout
- Phone portrait: the header is above the board or buttons.
- Wide: two panes, with the header and controls on the left and the board on the right.
- The result screen uses a FlowRow of buttons, 2 per row on compact screens and 4 on wide ones.

## Acceptance criteria
- [ ] A late recognition result is never applied to a different word.
- [ ] Normal rounds are saved exactly once. Practice rounds are never saved.
- [ ] 再測一輪 after a practice round uses all of the unit's words.
- [ ] The test is usable without TTS.
