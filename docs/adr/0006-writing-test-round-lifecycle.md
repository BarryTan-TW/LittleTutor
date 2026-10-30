# ADR-0006: Writing test round lifecycle

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
Earlier versions lost results when the child forgot to press "save". Late OCR results
could also be scored against the wrong word, and fast double taps skipped words.

## Decision
- `beginWritingRound(words, isPractice)` resets all round state. `startUnitTest`,
  `restartWritingRound` (all unit words) and `retryWrongWords` (practice) go through it.
  Starting a unit with no words shows a message instead.
- Recognition captures `index` and `word` before launching. If the round, index or
  finished flag changed by the time it completes, the result is dropped.
- Manual ✓/✗ (`confirmTestResult`) is guarded by `writingIsAdvancing` and advances after
  400 ms, so double taps cannot skip words.
- `cancelWritingJobs()` runs on next, exit and restart.
- `finishWritingRound` **auto-saves** a `TestRecord` unless `writingIsPracticeRound`.
  The result screen shows "成績已自動儲存" or "錯字練習不列入成績", and offers 練習錯字,
  再測一輪, 查看成績 and 回到首頁.
- TTS status is tracked as `TtsStatus` (INITIALIZING / READY / UNAVAILABLE). Voices are
  tried in the order zh-TW → zh-CN → zh. If TTS is unavailable, a warning is shown and the
  test still works.
- Leaving a test that is in progress asks "結束測試？".

## Consequences
- A finished round is never lost, and scores always match the right word.
- New test modes must reuse `beginWritingRound` / `finishWritingRound` and keep the
  stale-result guards.
