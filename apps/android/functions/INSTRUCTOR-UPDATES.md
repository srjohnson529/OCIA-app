# Instructor updates

## Extended management

Admin Tools → Instructor Updates → Update Management adds private drafts, the blue/gold preview, publication scheduling, optional startup-card expiration, cancellation and withdrawal. The web uses the same management link in Instructor Updates. Date/time controls use the device's local zone and store absolute Unix milliseconds. Scheduled publication runs every five minutes; this is not a guarantee of delivery at the exact selected minute. Expiration affects the startup card, not the retained inbox record. Withdrawal stops future startup presentation; already-delivered pushes or a card already being read cannot be recalled.

Drafts are stored separately in server-only instructorUpdateDrafts, inaccessible to any client (including direct admin reads). Admins access them through manageInstructorUpdates. Revision checks prevent overwriting another administrator's edits. Published text is immutable: withdraw and create a new draft for a correction. Publishing uses the draft ID as its idempotency key, so simultaneous or repeated publish attempts cannot broadcast twice. A scheduler rechecks the scheduling administrator's role. Expired schedules are skipped. Publication failures remain visible for investigation; uncertain pushes are not automatically resent.

Results count saved Got it receipts among currently registered instructors. FCM accepted/attempted counts are device-token counts, not unique users and not proof of delivery or reading. No acknowledgment names are disclosed in the summary. The latest 100 management records are shown.

Release also requires manageInstructorUpdates and publishScheduledInstructorUpdates, plus the revised publishInstructorUpdate and updated clients. Existing startup clients must be updated to honor expiresAtMs. No deployments or live sends were performed during implementation. Local update-management.emulator.test.cjs verifies privacy boundaries at callables, revision conflicts, concurrent publication, scheduled/not-yet-due/canceled/expired paths, withdrawal and acknowledgment results; rules tests verify drafts remain private.

Startup cards: the admin composer has independent Show at instructor startup and Send push notification switches. Only instructor profiles see the latest update at startup, and only when that update explicitly enables it. Older messages are not queued. Existing updates without the flag remain inbox-only at startup. Daily Formation is presented first, without changing its content/settings. Got it writes an account-scoped dismissal receipt shared by all platforms; Close for now closes only this session. Blue backgrounds, white reading text and gold accents follow Daily Formation's platform layouts. No automatic translation of admin-written content is provided. Deploy the instructorUpdateReceipts rules alongside the updated callable before releasing these clients.

Admin Tools → Instructor Updates on iOS/Android, or Instructor Updates in the web navigation (administrator accounts see the composer). Instructor accounts can read the latest 100 updates in More → Instructor Updates; the web uses the navigation button. Students cannot access this collection.

Title: 120 characters; message: 2,000. The sender reviews and confirms the broadcast. The message is published verbatim; UI controls support English/Spanish, but message text is not automatically translated. There are no external links, HTML rendering, or student recipients added by this feature.

The callable verifies the profile's server-controlled isAdmin flag, saves an inbox record, and sends push requests to profiles with isInstructor exactly true and notificationsEnabled not false. Device permission and a registered FCM token are required. The existing class-update notification channel is reused. Tapping a push opens the app; the inbox is under More. The web inbox works without browser push support.

Delivery counts are FCM accepted requests, not confirmed receipt or reading. A partial/sending status must not be interpreted as a failed inbox publication. Request IDs are claimed transactionally before sending; retrying the same ID will not broadcast again. This favors avoiding duplicates over automatic retry of uncertain delivery. There is no live automatic retry job.

## Release requirements

Deploy publishInstructorUpdate in us-central1 and the instructorUpdates rule block, then release the native builds/web changes. Do not blindly deploy every unrelated pending Firestore change: this working tree also contains an enrollment rules migration that needs its own coordinated rollout. No function, rule, hosting deployment, or production broadcast was performed for this feature during implementation.

## Checks

- node --test instructor-updates.test.js: authorization, content validation, audience preferences, token deduplication, paging, bounded batches, repeat requests, failed delivery.
- firebase/rules-tests/instructor-updates.rules.test.cjs: emulator verifies instructor/admin reads, student/anonymous denial, and denial of direct writes for all clients (including admins).
- Web regression tests and instructor-updates.test.cjs.
- Android compileDebugKotlin; iOS Swift syntax check. Device delivery and the iOS full build still need release testing.

Before a real broadcast, use an isolated staging project with an admin, an instructor with notifications on, an instructor with notifications off, and a student. Verify inbox visibility, confirmation cancellation, successful publication, background/foreground push delivery, and that a repeated request ID never duplicates the push.
