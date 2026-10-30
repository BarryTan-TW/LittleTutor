# ADR-0005: On-device ML Kit Chinese text recognition

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
Parents photograph textbook pages to build units, and the writing board needs to check a
child's handwriting. The app is used by children, so it must work offline and send no data
off the device.

## Decision
- Use `com.google.mlkit:text-recognition-chinese` (bundled, on-device) for both:
  1. **Photo OCR**: `PhotoBitmaps.decodeUpright` (EXIF rotation, downsampled to
     `OCR_MAX_DIMENSION` = 2048) runs on `Dispatchers.IO`. Recognized elements are split
     into word tokens by `OcrTokenSplitter`. Token boxes are in the coordinate space of the
     decoded bitmap. The user picks tokens by tap or lasso (`LassoGeometry`).
  2. **Writing board**: strokes are rendered to a bitmap. Recognition runs on
     `Dispatchers.Default`. The answer is correct if any candidate matches the target after
     normalisation (`isTargetMatched`). The candidate shown is the one with the smallest
     Levenshtein distance.
- Previews and thumbnails use `PREVIEW_MAX_DIMENSION` (1600) and `THUMBNAIL_MAX_DIMENSION`
  (320) to keep memory low.

## Consequences
- It works offline and protects privacy, but recognizing handwriting with a text
  recognizer is imperfect. That is why **manual confirm** stays the default testing mode.
- ML Kit Digital Ink would be a possible future upgrade for the writing board; it would
  need a new ADR.
