# Illumined iOS

Native SwiftUI starter app for the Illumined OCIA/Catechism app.

This app is designed to share Firebase data with the HTML app:

- `userProfiles/{uid}`
- `chatMessages/{messageId}`
- `classrooms/{classId}`
- `announcements/{announcementId}`

## What is included

- Email/password sign in and account creation
- Profile setup using `displayName`, `classIds`, `completedLessons`, `earnedBadges`, and `completedMysteries`
- Dashboard reading progress from `userProfiles`
- Class chat using the same `chatMessages` collection as the HTML app
- Shared model types for lessons, quizzes, badges, profiles, chat, classrooms, and announcements

## Xcode setup

1. Open Xcode.
2. Create a new iOS App project named `IlluminedIOS`.
3. Use SwiftUI and Swift.
4. Copy the `IlluminedIOS` source folder from this package into the Xcode project.
5. Add Firebase using Swift Package Manager:
   - `https://github.com/firebase/firebase-ios-sdk`
   - Add products:
     - `FirebaseAuth`
     - `FirebaseFirestore`
     - `FirebaseCore`
6. In Firebase Console, add an iOS app to the same Firebase project used by the HTML app.
7. Download `GoogleService-Info.plist`.
8. Add `GoogleService-Info.plist` to the Xcode project target.
9. Make sure Email/Password sign-in is enabled in Firebase Authentication.
10. Publish Firestore rules compatible with the shared collections.

## Optional XcodeGen setup

If you use XcodeGen, this folder includes `project.yml`.

```bash
cd /path/to/IlluminedIOS
xcodegen generate
open IlluminedIOS.xcodeproj
```

Then add `GoogleService-Info.plist` to the app target.

## Important note

The HTML app currently keeps lesson content inside the HTML file. This starter app shares accounts, profiles, progress, achievements, and chat immediately. To share lessons too, the best next step is to move lesson content into Firestore or a shared JSON format.
