# Build Validation

Last checked: 2026-08-01

## Shared curriculum — passed

- Canonical catalog parsed successfully.
- 73 lessons and 73 unique lesson IDs.
- iOS, Android, and HTML runtime copies matched the canonical SHA-256 checksum.

## Shared spiritual formation — passed

- `curriculum/spiritual_formation.json` is the canonical Formation catalog.
- The catalog parsed successfully with 27 common prayers, 20 Rosary mysteries, unique identifiers, and sequential order values.
- iOS, Android, and HTML runtime copies matched the canonical SHA-256 checksum.
- The Xcode application target contains a pre-build validation phase that rejects invalid or unsynchronized Formation content before compilation.
- The Xcode project file passed property-list validation after the build phase was added.

## HTML — passed

- Served locally and loaded in a browser.
- Page title: `Illumined: Catholic Faith Formation App`.
- Sign-in interface rendered successfully.
- `lessons.json` and `discussion_prompts.json` returned successfully.
- No browser console warnings or errors were recorded.
- The optional `/favicon.ico` request returned 404; this does not prevent the application from running.

## Android — passed

- The Gradle project and wrapper are present.
- Local Android SDK configuration was restored into the ignored `apps/android/local.properties` file.
- Android Studio is installed at `/Applications/Android Studio.app`.
- Its bundled Java runtime was found and runs successfully at `Contents/jbr/Contents/Home`.
- Android Studio opened and synchronized the consolidated `apps/android/` project.
- The debug application and unit-test sources compiled successfully.
- `testDebugUnitTest` passed 140 tests with 0 failures, 0 errors, and 0 skipped tests.
- `assembleDebug` completed successfully and produced `apps/android/app/build/outputs/apk/debug/app-debug.apk`.
- The APK SHA-256 at validation time was `1554fa1c6900ca5f4053541cd0496b3ab9e61e75b532e9c31b360726c46266f2`.
- The local debug keystore required by the build was restored to the ignored `apps/android/work/debug.keystore` path.

Repeat the unit tests from `apps/android/` with:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

One source warning remains: the Firebase Messaging token API usage in `NotificationRegistrar.kt` is deprecated. It does not fail the build or tests.

## iOS — passed

- The consolidated and Desktop `project.pbxproj` files are identical.
- The application target explicitly links `FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore`, and `FirebaseMessaging`.
- `Package.resolved` pins Firebase iOS SDK 12.16.0 and all 13 locked package checkouts and binary artifacts are present in the existing Xcode cache.
- Every application source/project file matches the Desktop working project. Only the two lesson JSON files differ, intentionally, because the consolidated files contain the canonical curriculum.
- The Desktop working project produced `Debug-iphonesimulator/IlluminedIOS.app` on 2026-07-30 at 10:35:36; its corresponding build log contains no compile errors.
- A direct command-line build from the consolidated path could not complete inside the Codex filesystem sandbox. Xcode requires write access to its Swift package diagnostic cache and invokes a nested manifest sandbox; those permissions were not granted. This was an execution-environment restriction, not a missing Firebase product reference.
- The consolidated project was subsequently opened in Xcode and confirmed functioning by the user on 2026-07-30.

The consolidated iOS verification hold is cleared.

## Publication status

No application was published, uploaded, or pushed to GitHub during validation.
