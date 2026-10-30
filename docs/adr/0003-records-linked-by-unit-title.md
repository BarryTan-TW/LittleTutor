# ADR-0003: Test records link to units by title; titles are unique

- **Status:** Accepted
- **Date:** 2026-10-30

## Context
`records.csv` stores the unit **title**, not the unit id. That is historical and must stay
compatible with existing files.

## Decision
- Unit titles are unique per user after trimming. `UnitInput.titleError` shows the
  inline error "已有同名課文，請換一個名稱", and `UserSpaceManager.isUnitTitleTaken` enforces it.
- Renaming a unit rewrites the title in `records.csv` (`renameTitleInRecords`).
- Deleting a unit deletes its records and result files (`deleteRecordsByIncludedTitles`),
  after a confirmation dialog.
- Practice rounds (wrong words only) are **not** saved, so they never skew history.

## Consequences
- History and "最近 xx%" on Home stay correct through renames and deletes.
- Any new feature that creates or renames units must go through these helpers.
- User names are unique too, ignoring case (`TutorViewModel.isDuplicateUserName`).
