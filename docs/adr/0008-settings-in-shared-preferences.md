# ADR-0008: App-wide preferences in SharedPreferences

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
The testing mode and speech rate reset on every launch, which annoyed parents.

## Decision
- App-wide (device-level, not per-user) preferences are stored in SharedPreferences
  `little_tutor_settings`:
  - `testing_mode`: `TestingMode.name` (default `MANUAL_CONFIRM`)
  - `speech_rate`: float, clamped to 0.5–2.0 (default 1.0)
- They are loaded in the `TutorViewModel` `init` before `reloadFromStorage()` and written in
  `setTestingMode` / `updateWritingSpeechRate`. Unknown values fall back to the defaults.
- Per-user data stays in files ([ADR-0002](0002-file-based-per-user-storage.md)).

## Consequences
- This is simple and enough for a handful of keys. If preferences become per-user or
  structured, move them to DataStore in a new ADR.
