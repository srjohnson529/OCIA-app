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

## Android — Gradle dependency setup blocked

- The Gradle project and wrapper are present.
- Local Android SDK configuration was restored into the ignored `apps/android/local.properties` file.
- Android Studio is installed at `/Applications/Android Studio.app`.
- Its bundled Java runtime was found and runs successfully at `Contents/jbr/Contents/Home`.
- Unit tests could not complete because the local Gradle 9.1 distribution/dependency cache is incomplete and the command-line environment could not resolve `services.gradle.org` to download the missing files.

Required next action: open `apps/android/` in Android Studio and allow the initial Gradle synchronization to finish while online. Then run the unit tests in Android Studio or run the following from `apps/android/`:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

## iOS — package linkage blocked

- Xcode recognizes the project and `IlluminedIOS` scheme.
- The locked Firebase Swift package checkouts and artifacts are available locally.
- Build planning reaches the application target.
- The build stops because Xcode reports missing package products: `FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore`, and `FirebaseMessaging`.

Required next action: open `apps/ios/IlluminedIOS.xcodeproj` in Xcode, reset or resolve package caches, and confirm that the four Firebase products remain attached to the application target. Then build with the local Firebase configuration present.

## Publication status

No application was published, uploaded, or pushed to GitHub during validation.
