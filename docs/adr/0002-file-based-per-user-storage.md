# ADR-0002: File-based per-user storage with atomic writes

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
The data is tiny and personal: a few dozen units and records per child. It must stay on
the device, and existing installs must keep working.

## Decision
`UserSpaceManager(rootDir = filesDir/user_spaces)` is the only class that touches files.

```
user_spaces/
  users.txt                 id|displayName           (one per line)
  current_user.txt          id
  <userId>/
    profile.txt
    test_units.txt          b64(id)|b64(title)|b64(w1),b64(w2)…|b64(path1),…
    photos/                 *.jpg captured by the camera
    test_records/
      records.csv           id|yyyy-MM-dd HH:mm:ss|b64(unitTitle)|total|correct
      <recordId>_results.txt  word|1-or-0 (one per line)
```

- On first launch, a default user `guest` named 訪客 is created.
- Free text is stored as Base64 UTF-8, so `|`, `,` and newlines are safe.
- Every write uses `File.writeTextAtomic` (write a `.tmp` file, then rename).
- Readers skip malformed lines instead of crashing.
- Paths received from the UI are canonicalised and must lie inside the user's directory
  before anything is deleted.

## Consequences
- No migrations or database are needed, and it is easy to inspect with `run-as`.
- **Format changes must be backward compatible**: only add trailing fields and keep
  readers tolerant. Never rewrite existing files into a new format without a migration
  plan recorded in an ADR.
- Not designed for large data or concurrent writers. If that changes, consider Room.
