# Legacy Illumined Inventory and Archive Plan

Inventory date: 2026-07-30

This inventory covers Illumined-related material found under the accessible Desktop and `/Users/stephenjohnson/Documents/Codex` folders. No legacy source was deleted or moved during this inventory.

## Verification holds

These folders are original working applications. Keep them unchanged until the corresponding consolidated application has passed its build validation.

| Existing location | Approximate size | Classification | Planned disposition |
| --- | ---: | --- | --- |
| `/Users/stephenjohnson/Desktop/IlluminedIOS/IlluminedIOS` | 3.4 MB | Original active iOS Xcode project | Keep until `apps/ios/` resolves Firebase packages and builds successfully; then archive as the pre-consolidation iOS source. |
| `/Users/stephenjohnson/Documents/Codex/2026-07-22/i-w` | 1.2 GB after macOS hydrated cloud cache placeholders during cleanup | Original Android project plus generated data | Verification hold cleared: the consolidated Android project synchronized, passed 140 unit tests, and built a debug APK. The source and selected work evidence are archived; the two large Gradle cache folders remain in place pending a cloud-safe cleanup method. |
| `/Users/stephenjohnson/Documents/Codex/2026-07-09/build-a-swift-ui-app-for/outputs/CatechismHTML_SharedLessons` | 860 KB | Original shared-curriculum HTML output | Keep until the consolidated HTML application’s deployment/hosting origin is documented. The local runtime test already passes. |

## Historical iOS starters

These are small starter variants, not the current full application.

| Existing location | Approximate size | Classification | Planned disposition |
| --- | ---: | --- | --- |
| `2026-07-09/ana/work/IlluminedIOS` | 60 KB | Starter source workspace | Archive after a final file comparison. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v1` | 16 KB | Expanded starter output | Archive or retain only its matching ZIP. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v1.zip` | 16 KB | Starter snapshot | Retain in historical archive. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v2.zip` | 16 KB | Starter snapshot | Retain in historical archive. |
| `2026-07-09/ana/outputs/IlluminedIOS-starter-v3.zip` | 16 KB | Starter snapshot | Retain in historical archive. |
| `2026-07-09/ana/work/IlluminedIOS-DerivedData` | 0 bytes | Empty build artifact | Safe removal candidate. |

The three ZIP files have different checksums and should be treated as distinct historical snapshots unless their contents are compared and documented.

## Swift and shared-lesson transformation copies

| Existing location | Approximate size | Classification | Planned disposition |
| --- | ---: | --- | --- |
| `2026-07-09/build-a-swift-ui-app-for/work/existing-swift/illumined copy` | 1.6 MB | Full Swift-only source variant | Archive; an unchanged consolidation-era copy already exists under `Illumined/archive/ios-swift-source-2026-07-30/`. |
| `2026-07-09/build-a-swift-ui-app-for/outputs/IlluminedIOS_SharedLessons` | 1.5 MB | Generated full-iOS transformation output | Archive as generated historical output. |
| `2026-07-09/build-a-swift-ui-app-for/outputs/IlluminedIOS_StarterSharedLessons` | 596 KB | Generated starter transformation output | Archive as generated historical output. |
| `2026-07-09/build-a-swift-ui-app-for/work/catechism-html` | 0 bytes | Empty work folder | Safe removal candidate. |

The lesson catalogs in the full Swift copy, both generated iOS outputs, and the HTML output match the current canonical catalog exactly.

## Divergent legacy curriculum

`2026-07-09/build-a-swift-ui-app-for/work/shared-lessons/lessons.json` is not canonical. It contains 73 lessons but only 72 unique IDs because both “Freedom and Choice” and “Freedom in Christ” use `morals-Freedom`.

It contains no additional lesson titles. Preserve it only as a historical defective variant; do not synchronize it into an application.

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
- Deferred `work/gradle-home` and `work/gradle-home-parity`. Moving the first cache caused macOS to download cloud placeholders, increasing its local footprint from approximately 150 MB to 345 MB. The operation was stopped cleanly before either cache moved; both remain at the original location.

The recoverable Trash folder currently occupies approximately 118 MB. The two deferred Gradle caches occupy approximately 345 MB and 811 MB after cloud hydration. They contain regenerable dependency caches, but should be cleaned with a method that does not first download every cloud placeholder.

## Recommended archive sequence

1. Resolve the consolidated iOS Firebase package linkage and complete an iOS build.
2. Complete Android Studio Gradle Sync, unit tests, and an Android build. **Completed 2026-07-30.**
3. Identify the HTML hosting/deployment source.
4. Copy verification-hold projects into a dated external or consolidated archive if a second recovery copy is desired.
5. Move small historical source variants into one clearly labeled archive.
6. Remove only confirmed generated caches, build output, empty folders, and duplicate expanded outputs.
7. Re-run the inventory and record the recovered disk space.

No legacy material should be deleted solely because its name looks duplicated; its classification and replacement must be confirmed first.
