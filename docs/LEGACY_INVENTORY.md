# Legacy Illumined Inventory and Archive Plan

Inventory date: 2026-07-30

This inventory covers Illumined-related material found under the accessible Desktop and `/Users/stephenjohnson/Documents/Codex` folders. No legacy source was deleted or moved during this inventory.

## Verification holds

These folders are original working applications. Keep them unchanged until the corresponding consolidated application has passed its build validation.

| Existing location | Approximate size | Classification | Planned disposition |
| --- | ---: | --- | --- |
| `/Users/stephenjohnson/Desktop/IlluminedIOS/IlluminedIOS` | 3.4 MB | Retired original iOS Xcode project | Verification hold cleared after the consolidated project was confirmed functioning in Xcode. The exact 170-file archive remains under `archive/legacy/ios-desktop-working-copy-2026-07-30/`; the Desktop original was moved recoverably to `~/.Trash/IlluminedIOS-Desktop-original-2026-07-30/`. |
| `/Users/stephenjohnson/Documents/Codex/2026-07-22/i-w` | 1.2 GB after macOS hydrated cloud cache placeholders during cleanup | Original Android project plus generated data | Verification hold cleared: the consolidated Android project synchronized, passed 140 unit tests, and built a debug APK. The source and selected work evidence are archived; the two large Gradle cache folders remain in place pending a cloud-safe cleanup method. |
| `/Users/stephenjohnson/Documents/Codex/2026-07-09/build-a-swift-ui-app-for/outputs/CatechismHTML_SharedLessons` | 860 KB | Retired shared-curriculum HTML output | All five files matched `apps/html/` byte-for-byte and no separate hosting configuration was present. An exact archive exists under `archive/legacy/html-standalone-output-2026-07-09/`; the original output was moved recoverably to Trash. |

## Retired standalone HTML output

The five-file `CatechismHTML_SharedLessons` output was compared with `apps/html/` on 2026-07-30. Its four JSON resources were identical, and `Catechism app.html` was identical to the normalized `apps/html/index.html`. No Firebase Hosting, Netlify, Vercel, GitHub Pages, or other deployment configuration was found in the output or its surrounding work tree. The Firebase project identity and client configuration are embedded in the HTML application itself.

An exact archive is preserved at `archive/legacy/html-standalone-output-2026-07-09/`. After verification, the original output folder was moved to `~/.Trash/CatechismHTML_SharedLessons-original-2026-07-09/`; Trash was not emptied. The surrounding dated project was not moved or modified.

## Historical iOS starters

The current Desktop iOS working project was copied without modification to `archive/legacy/ios-desktop-working-copy-2026-07-30/` on 2026-07-30. The archive contains 170 files, including its local Git metadata and uncommitted working state. A recursive byte comparison found no differences. The consolidated project was subsequently confirmed functioning in Xcode. After a final comparison, the original was moved to `~/.Trash/IlluminedIOS-Desktop-original-2026-07-30/`; Trash was not emptied. The now-empty outer Desktop container was moved separately to `~/.Trash/IlluminedIOS-Desktop-empty-container-2026-07-30/`.

These are small starter variants, not the current full application.

| Original location | Approximate size | Classification | Completed disposition |
| --- | ---: | --- | --- |
| `2026-07-09/ana/work/IlluminedIOS` | 60 KB | Starter source workspace | Archived under `archive/legacy/ios-starter-variants-2026-07-09/ana-work-IlluminedIOS/`. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v1` | 92 KB after hydration | Edited expanded starter output | Archived under `archive/legacy/ios-starter-variants-2026-07-09/expanded-starter-v1/`. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v1.zip` | 16 KB | Starter snapshot | Archived under `archive/legacy/ios-starter-variants-2026-07-09/zip-snapshots/`. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v2.zip` | 16 KB | Starter snapshot | Archived under `archive/legacy/ios-starter-variants-2026-07-09/zip-snapshots/`. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v3.zip` | 16 KB | Starter snapshot | Archived under `archive/legacy/ios-starter-variants-2026-07-09/zip-snapshots/`. |
| `2026-07-09/ana/work/IlluminedIOS-DerivedData` | 0 bytes | Empty build artifact | Moved to Trash with the retired variants. |

All three ZIP files have different checksums and contain meaningful code revisions. Version 2 changes the authentication, chat, and profile services from version 1; version 3 changes `RootView.swift` from version 2. The expanded v1 folder also differs from the v1 ZIP in `RootView.swift` and the three service files, so it was preserved separately. After exact archive comparisons, all originals were moved to `~/.Trash/Illumined-historical-variants-2026-07-30/`; Trash was not emptied.

## Swift and shared-lesson transformation copies

| Original location | Approximate size | Classification | Completed disposition |
| --- | ---: | --- | --- |
| `2026-07-09/build-a-swift-ui-app-for/work/existing-swift/illumined copy` | 1.6 MB | Full Swift-only source variant | Existing source archive verified at `archive/ios-swift-source-2026-07-30/`; only `.DS_Store` metadata was omitted. |
| `2026-07-09/build-a-swift-ui-app-for/outputs/IlluminedIOS_SharedLessons` | 1.6 MB | Generated full-iOS transformation output | Archived under `archive/legacy/shared-lesson-transformations-2026-07-09/`. |
| `2026-07-09/build-a-swift-ui-app-for/outputs/IlluminedIOS_StarterSharedLessons` | 2.5 MB after hydration | Generated starter transformation output | Archived under `archive/legacy/shared-lesson-transformations-2026-07-09/`. |
| `2026-07-09/build-a-swift-ui-app-for/work/catechism-html` | 620 KB after hydration | Pre-shared-curriculum HTML work copy | Archived under `archive/legacy/shared-lesson-transformations-2026-07-09/catechism-html-work/`. |

The lesson catalogs in the full Swift copy and both generated iOS outputs match the current canonical catalog exactly. The older HTML work copy differs from the current consolidated HTML entry point and was retained as historical transformation evidence. After exact archive comparisons, all original transformation copies were moved into the same recoverable Trash group as the starter variants.

## Divergent legacy curriculum

The retired `2026-07-09/build-a-swift-ui-app-for/work/shared-lessons/lessons.json` is not canonical. It contains 73 lessons but only 72 unique IDs because both “Freedom and Choice” and “Freedom in Christ” use `morals-Freedom`.

It contains no additional lesson titles. An exact copy is isolated under `archive/legacy/defective-curriculum-2026-07-09/` with a prominent warning and checksum record. The original one-file work folder was moved recoverably to `~/.Trash/Illumined-defective-shared-lessons-2026-07-09/`; Trash was not emptied. Do not synchronize the archived file into an application.

## Android generated material

Most of the original Android folder’s size is regenerable build and tool data:

| Existing location | Approximate size | Classification |
| --- | ---: | --- |
| `2026-07-22/i-w/work` | 364 MB | Temporary Gradle homes, caches, and task work |
| `2026-07-22/i-w/app/build` | 38 MB | Generated Android build output |
| `2026-07-22/i-w/.gradle` | 19 MB | Local Gradle project cache |
| `2026-07-22/i-w/.kotlin` | 352 KB | Kotlin compiler state and error logs |
| `2026-07-22/i-w/.idea` | 164 KB | Android Studio local workspace state |
| Debug log files in the project root | approximately 100 KB | Firebase and Firestore emulator/debug logs |

The consolidated Android project has synchronized, passed its unit tests, and built successfully. Cleanup was explicitly approved and began on 2026-07-30.

### Android archive and cleanup record — 2026-07-30

- Archived 185 source files to `archive/legacy/android-i-w-source-2026-07-22/`, excluding generated caches, build output, Android Studio state, and task-work data.
- Archived 33 selected work-evidence files to `archive/legacy/android-i-w-work-evidence-2026-07-22/`, including the debug keystore, quarantined Kotlin source, backend-reference reports, screenshots, UI hierarchy captures, and root Firebase/Firestore logs.
- Verified matching SHA-256 checksums for the original and archived `debug.keystore` and `quarantine/IlluminedApp 2.kt` files.
- Moved `app/build`, `.gradle`, `.kotlin`, the temporary Android SDK/user-home folders, and the root Firebase/Firestore logs to the recoverable Trash folder `~/.Trash/Illumined-Android-generated-2026-07-30/`. The Trash folder was not emptied.
- Kept `.idea`, all remaining legacy source, task evidence, the debug keystore, and the quarantine folder in their original locations.
- An ordinary Trash move of `work/gradle-home` and `work/gradle-home-parity` caused macOS to download cloud placeholders, so it was stopped cleanly. A later scan found 11,494 remaining cloud-only files. Both caches were then grouped without hydration under `work/cloud-sensitive-cache-retirement/`, away from the retained evidence and keystore.

The initial recoverable Android-generated Trash folder occupies approximately 118 MB. The two isolated legacy Gradle caches now occupy approximately 418 MB and 1.0 GB. They contain regenerable dependency caches, but should be removed with a File Provider-aware method that does not first download every cloud placeholder.

## Recommended archive sequence

1. Resolve the consolidated iOS Firebase package linkage and complete an iOS build. **Completed 2026-07-30.**
2. Complete Android Studio Gradle Sync, unit tests, and an Android build. **Completed 2026-07-30.**
3. Identify the HTML hosting/deployment source. **Completed locally: no separate deployment configuration was present.**
4. Copy verification-hold projects into a dated external or consolidated archive if a second recovery copy is desired. **Completed for iOS, Android, and HTML.**
5. Move small historical source variants into one clearly labeled archive. **Completed 2026-07-30.**
6. Remove only confirmed generated caches, build output, empty folders, and duplicate expanded outputs. **Fully local caches were moved recoverably to Trash; cloud-sensitive caches are isolated in labeled retirement folders.**
7. Re-run the inventory and record the recovered disk space. **Completed 2026-07-30; see `docs/FINAL_INVENTORY.md`.**

No legacy material should be deleted solely because its name looks duplicated; its classification and replacement must be confirmed first.
