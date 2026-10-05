# Student invitation rollout

Not deployed automatically. Existing class IDs and enrolled students remain unchanged.

## Verification completed (2026-09-16)

- Seven policy and callable-contract tests passed.
- Firestore emulator rules tests passed: direct enrollment, privilege escalation, membership additions, private invitation reads/writes, rate-limit resets and removed-student reentry denied; existing student lesson-progress updates allowed.
- Server handlers tested with real Firestore emulator transactions: authorized generation, concurrent student joins, progress preservation, removed-student rejection, code rotation, archive rejection and disabling passed.
- Android Kotlin compilation passed; iOS Swift syntax checks passed. Full iOS build and device/UI tests remain required.
- Web automated tests passed. Actual invitation link/QR flows on installed native builds and deployed hosting remain to be tested before release.

## Release steps

1. Test `manageStudentInvitation` and `joinStudentClass` with the Firestore emulator: authorized instructor only; expired/disabled/replaced codes; archived/missing classroom or owner; removed students; concurrent joins; repeated guesses; preservation of existing progress. Test rules denying direct student enrollment and membership edits while allowing existing student progress updates.
2. Deploy the two callable functions from this functions directory. Update web, iOS, Android and the native invitation landing page together. New student setup calls the server; older clients only understand class IDs and cannot join after rule enforcement.
3. Generate invitations for existing active classrooms. Missing classroom documents or instructor ownership must be repaired by an authorized administrator, not inferred from students' profiles.
4. Deploy the authoritative rules at `apps/android/firebase/firestore.rules` only after updated clients are available. Do not deploy the stale web-worktree rules or functions configuration. Rules disable direct student profile creation and membership additions; server callables handle enrollment.
5. Verify a new test student can join by typed code and shared link/QR on all platforms. Verify existing students can sign in and save progress. Do not test on real student records.

Codes use 10 cryptographically random unambiguous characters, expire after 90 days, and are shared by a class. Anyone with a valid code can join; instructor approval is not part of this iteration. Regeneration or disabling invalidates the previous invitation but does not remove enrolled students. Server limits each signed-in account to ten join attempts per ten minutes. This is not a global/per-IP abuse limit; App Check enforcement and broader abuse protection remain separate rollout decisions.
