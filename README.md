# Illumined

This is the consolidated working home for the Illumined project.

## Applications

- `apps/ios/` — Swift/SwiftUI application.
- `apps/android/` — Kotlin/Jetpack Compose application.
- `apps/html/` — Browser-based HTML application.

## Shared curriculum

`curriculum/lessons.json` is the canonical lesson catalog. Edit lesson content here first. Copies inside individual applications are runtime resources and should be refreshed from this file rather than edited independently.

`curriculum/review/` contains human-readable exports intended for clergy, catechetical leaders, and instructors.

## Documentation

- `docs/PROJECT_MAP.md` explains the folder layout and identifies the source used for each consolidated application.
- `docs/CURRICULUM_WORKFLOW.md` explains how lesson changes should flow into all three applications.
- `docs/VERSION_CONTROL.md` explains the unified Git repository and local configuration exclusions.
- `docs/BUILD_VALIDATION.md` records which consolidated applications currently build or run and any remaining blockers.
- `docs/LEGACY_INVENTORY.md` classifies older Illumined folders and defines the non-destructive archive sequence.

## Historical material

The consolidated applications have been validated and their former working copies have been retired recoverably. Verified historical snapshots are grouped under `archive/legacy/`; generated cache groups awaiting cloud-safe removal are documented in `docs/FINAL_INVENTORY.md`. The `archive/` and `work/` folders are local and intentionally ignored by Git.
