# Optional profile and classroom images

Implemented in native Android and iOS. Backend deployed on September 20, 2026: `manageProfileImage`, `findClassrooms`, and `deleteOwnAccount`.

The default bucket was provisioned in US-CENTRAL1 with uniform bucket-level access and public-access prevention enforced. `firebase/storage.rules` denies all direct client access. The Functions runtime service account has bucket-scoped object administration. Unauthenticated Firebase Storage listing and direct object requests returned HTTP 403. Hosting and Firestore rules were not deployed. Real signed-in upload testing still needs to be performed in the rebuilt native app.

## User experience

- Native setup ends with an optional "Make It Yours" photo step after profile/class membership is confirmed. Students can add a personal photo; instructors can add a personal and classroom image. Approval-based students see the step after approval. Continue/Skip clears the local, account-scoped pending step; draft selections must be saved or canceled first. Existing configured accounts are not automatically enrolled into this step.
- Account → Your Photo: choose, preview, save, replace or remove a personal photo.
- Instructor Tools → Classes → each active classroom: equivalent classroom image controls.
- Home welcome card displays the classroom image and the signed-in user's own photo.
- Find My Classroom displays the optional image on enabled, active classroom listings.
- Missing images and read failures leave existing navigation and search usable.
- Personal images are owner-only in this release; roster/discussion photo sharing is not enabled.

## Storage and security

`manageProfileImage` checks Firebase authentication, profile existence, active membership and instructor status on every operation. A user can only read/write their own personal image. Active classmates can read the classroom image; only active instructors assigned to that classroom can modify it. Public discovery returns an image only after its existing enabled-listing and active-instructor checks succeed.

JPEG objects are stored at `profile-images/{user|classroom}/{sha256(target)}.jpg` in the project's default Storage bucket. Images have no download tokens or permanent public URLs. The callable returns base64 bytes. The server validates still image formats, limits pixels/input size, re-encodes, strips EXIF metadata, and bounds dimensions to 320×320 or 640×400. The clients preview their local selection; final aspect ratio is center-cropped on save. Writes are limited to 12 per user per ten minutes. Account deletion removes the personal image and photo rate-limit document.

## Deployment gates

1. Confirm the default bucket (`ocia-application.firebasestorage.app`) exists and the Functions service identity has object read/write/delete permission. Do not grant public access.
2. Inspect current Storage rules and bucket IAM before enabling this feature. The adjacent `firebase/profile-images.storage.rules` is a **review fragment**, not a blindly deployable replacement for existing rules. Ensure no overlapping wildcard rule grants client/public access to `profile-images/**`; `allow false` does not override another matching `allow true`. Preserve unrelated storage behavior. Test unauthenticated and cross-user storage access is denied.
3. Run Node 22 dependency installation and the policy/image tests. Deploy only `manageProfileImage`, updated `findClassrooms` and updated `deleteOwnAccount` after reviewing their full diffs (the working tree includes earlier changes).
4. Rebuild native apps. Before rollout, test real photo selection, rotation, replacement/removal, account switching, listed/unlisted classroom search, inactive users and deletion against the deployed service. An older backend shows an unavailable/retry message rather than pretending uploads succeeded.
5. Website photo editing is not included in this native-app change. Existing web search ignores the additive `image` field without changing membership behavior.

No Firestore schema migration, existing profile mutation or membership rule change is required. The existing Firestore default deny protects the server-only `profileImageLimits` collection.
