# Illumined Project Map

## Canonical workspace

The consolidated workspace is `/Users/stephenjohnson/Documents/Codex/Illumined`.

| Area | Location | Purpose |
| --- | --- | --- |
| iOS | `apps/ios/` | Full Swift/SwiftUI application |
| Android | `apps/android/` | Kotlin/Jetpack Compose application |
| HTML | `apps/html/` | Browser application |
| Curriculum | `curriculum/lessons.json` | Canonical lesson data |
| Human review | `curriculum/review/` | Readable curriculum exports |
| Documentation | `docs/` | Project and editorial procedures |
| Tools | `tools/` | Future export and synchronization utilities |
| Archive | `archive/` | Future home for explicitly retired snapshots |

## Sources used for the initial consolidation

The initial workspace was assembled non-destructively from these existing folders:

- iOS: `/Users/stephenjohnson/Desktop/IlluminedIOS/IlluminedIOS/`
- Android: `2026-07-22/i-w/`
- HTML: `2026-07-09/build-a-swift-ui-app-for/outputs/CatechismHTML_SharedLessons/`
- Curriculum: the iOS application’s `Application/Resources/lessons.json`

Generated Android build output, Gradle caches, dependency folders, and temporary work folders were excluded from the consolidated copy.

## Current status

This establishes a clean working layout, but does not yet retire the old dated folders. Before archival, each application should be opened and built from this workspace, configuration and credentials should be reviewed, and stakeholders should confirm that these are the desired product variants.

The consolidated iOS folder now contains the complete `IlluminedIOS.xcodeproj`, its Git history, application source, tests, resources, and the Desktop project’s uncommitted work. The earlier Swift-only source tree is preserved at `archive/ios-swift-source-2026-07-30/`. An exact 170-file copy of the current Desktop working project is preserved at `archive/legacy/ios-desktop-working-copy-2026-07-30/`; the Desktop original remains in place pending one direct build from the consolidated path in Xcode.

The Xcode target includes the root-level `apps/ios/lessons.json`. The additional `apps/ios/Resources/lessons.json` is not the lesson resource listed in the application target’s Resources build phase. Both are synchronized with `curriculum/lessons.json`. The divergent versions found during consolidation are preserved under `archive/curriculum-variants-2026-07-30/`.

The Android copy includes its Gradle project files. Generated build products, dependency folders, local machine configuration, IDE state, and debug logs were excluded from the working application. Local Android metadata found during consolidation was retained under `archive/android-local-metadata/` rather than deleted.

The HTML application has a normalized `apps/html/index.html` entry point. The identical file with the original `Catechism app.html` filename is preserved under `archive/html-original/`.
