# Classroom-first native enrollment

Implemented for iOS and Android. The web login is unchanged.

## Flow
- Signed-out launch offers Find My Classroom, QR invitation entry, existing-account sign-in, and an instructor setup path. No email/password fields appear on the initial screen.
- Search asks for parish name (partial, accent/case insensitive) and city (full, accent/case insensitive).
- Only instructor-enabled listings with a live classroom and active owner appear. Public results contain class ID, parish, city and classroom name only, not roster information, emails or invite codes.
- Selection persists locally through account creation. Profile setup submits a separate pending request; no profile membership is granted yet.
- Student Details shows the instructor an approval queue with confirmation for Approve/Decline. Approval adds student membership transactionally while preserving existing progress. A waiting student is notified by the database listener and enters the app once approved.
- Cancelling or declining never grants membership. A declined student must contact the instructor (who can share a valid invitation) or choose another classroom.
- Existing instructor-shared student links/QR codes retain direct enrollment through the existing, expiring invitation-code validator. Scanning never automatically grants a role. Co-instructor and parish setup invitations still use their existing privileged-code checks.
- iOS scans live using the system scanner (requires a supported camera device). Android captures a QR photo using the camera app and decodes it locally. Both have manual invitation-code entry as a fallback; Android does not store the photo.

## Instructor setup
New parish setup collects parish name and city, not a manual class ID. The authenticated `startParishClass` callable validates and consumes the startup code and creates the classroom/profile atomically. IDs use lowercase parish-city (for example `holy rosary-steubenville`), with numeric suffixes on collisions; existing classroom IDs are unchanged. Invitation links preserve ID casing. Existing and new instructors open Classroom Codes, fill parish/city/classroom name, and enable Show in classroom search. Discovery is opt-in; existing classrooms are not made public automatically. Student Details contains Requests to Join.

## Deployment (not performed by this change)
Deploy from apps/android to the intended Firebase project:

```
firebase deploy --only functions:startParishClass,functions:manageClassroomListing,functions:findClassrooms,functions:requestClassroomEnrollment,functions:reviewClassroomEnrollment,firestore:rules
```

Rebuild both native clients. Deploy the backend/rules before distributing those builds or the new calls will return NOT FOUND. The directory uses a single-field cityKey query and does not require a new composite index. Search is capped at 100 listings per city / 20 matched results; revisit pagination for larger rollouts.

## Security and operations
- Directory writes, enrollment requests and membership changes are server-only. Rules permit request reads by that student or the assigned instructor only.
- Server checks active classroom ownership, instructor assignment, removed/archived membership and role restrictions at mutation time.
- Search rate limit: 30 per ten minutes per authenticated UID or hashed connection IP. Enrollment: 10 per ten minutes per UID. For a large public rollout, consider App Check/edge abuse protection; throttling alone is not comprehensive DDoS protection.
- Request review records reviewer and timestamp. Search limits keep a hashed identifier, not the raw IP. expiresAt can be configured as Firestore TTL for classroomDiscoveryLimits; TTL is not automatically enabled.
- Pending requests hold student name/email for instructor review. Include classroomJoinRequests in account-erasure/retention maintenance when those policies are expanded.

## Verification
- `node --test classroom-discovery.test.js student-invitations-handler.test.js`
- Start the local Firestore emulator on 127.0.0.1:8199, then run `node --test classroom-discovery.emulator.test.js` from functions and `node --test classroom-discovery.rules.test.cjs` from firebase/rules-tests.
- Android compileDebugKotlin; iOS syntax/plist validation. Full iOS build and live camera checks must also be completed in Xcode/on devices before release.
