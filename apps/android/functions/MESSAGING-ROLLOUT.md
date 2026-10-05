# Classroom messaging and shared instructor inbox

Deployment update (October 3, 2026): canonical rules, web Hosting, all three messaging notification triggers, and updated account-deletion cleanup are deployed. All four targeted functions report ACTIVE. All 32 previous functions remain present; the other 31 retain their prior deployed source versions. Native app-store releases are not included.

Release verification: 108 web tests, 4 notification-policy/account-cleanup tests, and 3 emulator suites pass. The emulator suites cover both canonical rule copies, legacy iOS/Android/web group-message payloads (including a classroom without a classroom document), private inbox privacy, and browser push-token registration. Every non-chat production rule is logically unchanged from the backed-up live rules. Published web assets and live rule contents were verified against local files. Actual delivery to test devices/accounts is still pending; no test messages were sent to production students.

## Behavior

- Existing `chatMessages` remain the group conversation. Replies reference message IDs; reactions are per-user (`🙏`, `❤️`, `👍`). Authors may edit or delete their own messages. Assigned instructors may delete, but cannot rewrite student messages.
- Private conversations use `instructorConversations/{classId}__{studentId}/messages/{messageId}`. Only the student and current, active instructors in that class can read them. The interface explicitly says all classroom instructors have access. These are not student-to-student DMs or end-to-end encrypted conversations.
- Students or assigned instructors can start conversations. Instructors use New message → Choose a student; the picker includes only active student accounts in the current classroom. Existing conversations reopen instead of creating duplicates. New conversation rules independently verify the recipient's current membership, student role, and name. Instructors see a shared list and their own unread markers. All platforms use the same records, 4,000-character cap, and atomic conversation/message writes. Private history loads the latest 100 messages with a load-earlier control.
- Web and native apps show private-conversation unread indicators; Android also tracks unread classroom messages. Read state is per device/account, not synchronized read receipts. Attachments, typing indicators, and recipient read receipts are not included.
- New backend triggers `notifyClassroomMessage`, `notifyPrivateMessage`, and `notifyChatReaction` send device push alerts for classroom messages, replies, private messages, and added/changed reactions. No alerts are sent for edits, deletes, removed reactions, or your own actions. Private recipients are only the student and currently active instructors, excluding the sender. All push copy is generic (no private text or student names), localized to English/Spanish, and respects `notificationsEnabled` (plus `notificationMessages` if explicitly disabled). Existing global notification controls apply; there is no separate message preference switch yet.
- Notification taps validate the recipient account and active classroom before opening classroom chat or the private inbox. They do not jump to a specific message. The web includes in-app unread badges and opt-in browser push registration/service-worker support with the configured public VAPID key.
- Failed sends retain the current draft. Drafts are in memory and clear on sign-out/class changes; they are not permanent saved drafts.
- Private messages are append-only in this first version. Group message tools do not apply to private conversations.

## Security and compatibility

The new inbox requires a non-archived classroom document and current membership. Removed, inactive, archived, signed-out, peer-student, and unrelated-instructor access is denied. Administrator status alone does not grant private inbox access. This is enforced by Firestore rules, not just interface controls.

The preexisting group-chat eligibility for legacy classrooms is retained. Those classes need a proper classroom record before the new private inbox can be used. Do not relax the new inbox rules to work around missing records. Review/migrate the affected classes with approval before release; no production records were changed.

The current full rules suite has an older self-enrollment test that expects a client to create a legacy-class profile. That profile write is already denied by the preexisting classroom-discovery rules. It is unrelated to messaging. The dedicated messaging tests use approved profiles and verify the student and instructor query shapes against both rule files.

`removeInstructorInboxData` is called by the existing fresh-authentication account deletion flow: student-owned conversations and their messages are recursively deleted; a departing instructor's authored private messages are removed from the classrooms tracked on their profile. Deploy this cleanup with the feature.

## Before publishing

1. Review the existing unrelated worktree changes before committing. Do not publish the entire dirty mobile tree indiscriminately.
2. Run messaging rule tests using the local Firestore emulator on port 8199. The full canonical rule file is synchronized between `apps/android/firebase/firestore.rules` and the web worktree's `firestore.rules`. Do not restore the older, less restrictive web rules.
3. Deploy the canonical Firestore rules and updated `deleteOwnAccount` function first. The web functions folder is a separate legacy codebase; the account-deletion function lives in `apps/android/functions`.
   Deploy the three messaging notification triggers from the same mobile functions codebase. Do not deploy the unrelated legacy web functions. Trigger delivery is at-least-once; duplicate pushes are possible, as with the existing notification triggers.
4. Build and test all clients with two instructors, two students, and two classrooms, including real sign-in/out and removal. Do not test by sending to production students.
5. Publish Hosting and new Android/iOS versions only after validation. Old clients still send the existing message format; new reply/reaction fields are optional for decoding.
6. Verify push delivery on real iOS/Android devices in foreground, background, and cold start. Test a muted recipient, removed instructor, class/account switch, self-send, and private-message privacy. No live pushes have been sent during local validation. Full iOS build verification remains required.

## Checks performed

Notification extension checks: 103 web tests and 3 backend recipient/reaction-policy tests pass. Final Android app Kotlin compilation passes. Full Android unit-test compilation is blocked by preexisting duplicate `%202.kt` test classes (schedule selection, active class, account deletion, invite link); these files were not changed. New notification-routing unit tests are included but not certified as run. Swift parsing and both localization files pass; full iOS build and actual push delivery are not verified. Changes are local only.

- Android debug Kotlin compilation passed.
- iOS Swift parsing and Spanish strings validation passed. Full Xcode build is still blocked: package resolution cannot write the SwiftPM Firebase manifest cache, and the additional permission was not granted. This leaves Firebase package products unresolved; it has not been certified as a successful iOS build.
- Web unit/regression suite: 100 passing tests, including instructor initiation, existing-thread reuse, recipient draft isolation, Spanish picker labels, and stale roster callbacks.
- Dedicated emulator tests cover both web and mobile rules: instructor-initiated conversations, student receipt and co-instructor access, recipient-role/membership validation, roster queries, query privacy, co-instructor replies, sender spoofing, peer/outsider denial, message length, ownership edits, reactions, moderator deletion, cross-class reply references, revoked membership, and archived classrooms.
- Local browser fixture checked conversation list, privacy notice, unread indicator, message bubbles, and composer. The fixture is removed after QA.
