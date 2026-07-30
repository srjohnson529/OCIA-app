# Illumined

Illumined is an Android app built with Kotlin, Jetpack Compose, and Material 3.

## Current starting point

- A working single-activity Android application
- An adult OCIA-focused visual direction
- Email/password sign-in and password-reset screens
- Firebase Authentication integration with session restoration and sign-out
- Firestore-backed learner overview using the existing `userProfiles`, `assignments`, `assignmentCompletions`, and `classSchedule` collections
- Assignment reader with instructions, readings, and progress completion synced to Firestore
- Primary navigation with Home, Lessons, Schedule, and Profile areas
- Read-only class Community area for shared discussion prompts and active prayer requests
- Android 8.0+ support (API 26)
- Play Store-ready application/version identifiers
- Release shrinking enabled from the outset

The current application ID is `com.illumined.app`. Confirm ownership before the first Play Console upload because an application ID cannot be changed for an existing store listing.

## Connect the existing Firebase project

Register an Android app with the package name `com.illumined.app` inside the same Firebase project used by the iOS and web apps. Download its `google-services.json` file and place it at `app/google-services.json`.

The configuration file is intentionally ignored by Git. Without it, the app remains buildable and shows a developer configuration notice instead of attempting authentication.

## Play Store bundle

Release credentials are never stored in source control. Copy `keystore.properties.example` to `keystore.properties`, point it to the private Play upload keystore, and replace the placeholder passwords. Then build with a monotonically increasing Play version:

```sh
./gradlew bundleRelease \
  -PILLUMINED_VERSION_CODE=2 \
  -PILLUMINED_VERSION_NAME=0.2.0
```

The signed bundle is generated at `app/build/outputs/bundle/release/app-release.aab`. Without `keystore.properties`, the release pipeline can still be compiled and shrunk for verification, but its bundle is not ready for Play upload.

Ensure Email/Password is enabled under Firebase Authentication → Sign-in method. Existing users in that Firebase project will then be able to use the same credentials across Android, iOS, and web.

## Open the project

Open this folder in Android Studio, allow the initial Gradle sync to finish, and run the `app` configuration on an emulator or Android device.

## Product decisions needed next

1. Whether OCIA participants self-register or receive accounts from a parish/admin
2. The formation structure: sessions, topics, calendar, or a guided sequence
3. Whether progress and notes should sync across Android, iOS, and web
4. Parish/cohort membership and leader permissions
5. Brand direction, logo, and final color palette
