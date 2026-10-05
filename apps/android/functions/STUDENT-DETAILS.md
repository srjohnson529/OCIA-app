# Student Details and instructor-only restoration

Status: server functions and authoritative Firestore rules deployed to ocia-application on September 11, 2026. All nine scoped functions verified ACTIVE in us-central1. Web hosting and mobile builds were not released by this deployment.

## Behavior
- Student Progress is now Student Details on iOS, Android, and web.
- Search name/email; filter Active, Inactive, Removed, or All.
- Existing progress remains visible, with a contact-email link.
- Inactive retains access/progress, but is excluded from active progress totals and class push notifications.
- Remove revokes only the selected classroom membership. Account, progress, and other classroom memberships remain.
- Removed students remain in the instructor's Removed filter. Only the class instructor can restore them; class-code reentry is blocked by Firestore rules.
- Staff/self targets are protected. All changes require confirmation and use the authenticated manageStudentRoster callable, with transaction rechecks and a private rosterEvents audit record.
- No live accounts were modified in tests.

## Server release prerequisite
Deploy server changes before testing/releasing these client updates. Do not deploy automatically without user approval.

Use the Android Firebase project directory and project ocia-application:
1. Deploy the authoritative rules at apps/android/firebase/firestore.rules.
2. Deploy manageStudentRoster and every function using the class notification filtering:
   sendDailyFormationNow, createClassAnnouncement, notifyNewPrayerRequest,
   notifyPrayerReaction, notifyNewAssignment, notifyDiscussionReply,
   sendAssignmentDueReminders, sendDailyFormationReminders.
3. Verify deployment, then release updated web/native clients.

Do NOT deploy the web worktree's older firestore.rules; it is not the authoritative rules file.
Do not deploy unrelated functions or hosting as part of a rules-only release.

## Verification
- Android compileDebugKotlin passed.
- iOS Swift syntax parsing passed. Full Xcode verification is still required; access to local Xcode caches was denied.
- 8 pure roster/notification tests passed, plus 10 existing assignment policy tests.
- 6 Firestore emulator tests passed: status tampering, inactive access, removed reentry, scoped removed roster queries, restored access, legacy profiles.
- Callable emulator test passed: unauthenticated/outsider/student denial, protected staff, remove/restore, progress preservation, audit records, archived classroom denial.
- 3 mocked web UI tests passed, plus 10 existing guide/assignment tests; HTML inline JavaScript and new script parsed.

## Repeatable tests
From apps/android/functions:
- node --test student-roster.test.js assignment-progress.test.js
- With Firestore emulator at 127.0.0.1:8098: FIRESTORE_EMULATOR_HOST=127.0.0.1:8098 node --test student-roster.emulator.test.js
  The callable test skips without that exact emulator host and uses demo-roster only.

From apps/android:
- ROSTER_TEST_PACKAGE=/path/to/isolated/test/package.json node --test firebase/rules-tests/student-roster.rules.test.mjs
  Install @firebase/rules-unit-testing and firebase into that isolated package. Run a Firestore emulator on port 8098; project demo-roster.

From work/github-main-html-safe:
- node --test student-details.test.cjs assignment-progress-indicators.test.cjs rite-preparation.test.cjs rite-guide-templates.test.cjs

## Manual checks before release
Use test accounts on each platform, English and Spanish:
1. Mark inactive: appears under Inactive, access/progress remain, active totals decrease.
2. Restore inactive: returns to Active.
3. Remove: class content disappears for student; code rejoin fails; other classes still work.
4. Open Removed as instructor, review progress, restore.
5. Remove last class: student can still manage their profile and join a different class.
6. Cancel confirmations and verify no changes.
7. Switch classes during loading and verify no stale roster is presented.
