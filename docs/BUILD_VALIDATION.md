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

## Android — environment blocked

- The Gradle project and wrapper are present.
- Local Android SDK configuration was restored into the ignored `apps/android/local.properties` file.
- Unit tests could not start because no Java runtime or Android Studio installation was found on this computer.

Required next action: install Android Studio, or install/configure a compatible JDK, then run `./gradlew testDebugUnitTest` from `apps/android/`.

## iOS — package linkage blocked

- Xcode recognizes the project and `IlluminedIOS` scheme.
- The locked Firebase Swift package checkouts and artifacts are available locally.
- Build planning reaches the application target.
- The build stops because Xcode reports missing package products: `FirebaseCore`, `FirebaseAuth`, `FirebaseFirestore`, and `FirebaseMessaging`.

Required next action: open `apps/ios/IlluminedIOS.xcodeproj` in Xcode, reset or resolve package caches, and confirm that the four Firebase products remain attached to the application target. Then build with the local Firebase configuration present.

## Publication status

No application was published, uploaded, or pushed to GitHub during validation.

