# Build Validation

Last checked: 2026-07-30

## Shared curriculum — passed

- Canonical catalog parsed successfully.
- 73 lessons and 73 unique lesson IDs.
- iOS, Android, and HTML runtime copies matched the canonical SHA-256 checksum.

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

## iOS — package linkage blocked

- Xcode recognizes the project and `IlluminedIOS` scheme.
- The locked Firebase Swift package checkouts and artifacts are available locally.
- Build planning reaches the application target.
- The build stops because Xcode reports missing package products: `FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore`, and `FirebaseMessaging`.

Required next action: open `apps/ios/IlluminedIOS.xcodeproj` in Xcode, reset or resolve package caches, and confirm that the four Firebase products remain attached to the application target. Then build with the local Firebase configuration present.

## Publication status

No application was published, uploaded, or pushed to GitHub during validation.
