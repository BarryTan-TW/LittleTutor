# ADR-0009: Development workflow — feature by feature, emulator validation

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
The app was refactored for stability and UX one feature at a time. The developer's
physical phone is often connected over USB and must not be touched by automated tools.

## Decision
- Work **one feature per commit**. Message format: `Area: short summary`, then a bullet
  list body, ending with the `Co-authored-by` trailer when an AI agent helps.
- Every commit must pass `bash gradlew testDebugUnitTest assembleDebug`.
- UI is validated on **emulators only**:
  - Prefer throwaway AVDs, for example a phone at 1080x2400 and a tablet at 2560x1600.
    Delete them afterwards.
  - Always target emulators explicitly with `adb -s emulator-<port>`. Never run bare `adb`
    or `adb devices`, and never install on a hardware device.
  - Don't seed or overwrite data in the developer's existing AVDs.
  - Check phone portrait, phone landscape and tablet for each changed screen.
- Don't push. The developer reviews and pushes.

## Consequences
- History is easy to review and bisect, and the developer's devices are never affected.
