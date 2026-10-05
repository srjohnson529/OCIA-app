# Refreshment sign-up rollout

## Deployed manual-name extension

Instructors can choose “Enter a name manually” in the volunteer picker on web, Android and iOS. A trimmed name (1–120 characters) is stored as `volunteerName` with an empty `volunteerId`. This is still an occupied slot: students, including older clients, cannot claim/cancel it. Only current classroom instructors may create or manage manual entries. Selecting a real account replaces the manual name with the account's verified profile name. A name and account ID cannot be submitted together. Manual entries do not receive automatic reminders; every instructor editor explains that the name is not linked to an app account. No email or phone number is collected.

This extension was deployed October 3, 2026 to the `classroomRefreshments` callable and web Hosting. All 37 functions remain present; only the intended callable changed. Live assets match the tested files and the endpoint rejects anonymous requests. Validation: 116 web tests, 3 policy tests, and 3 emulator integration suites pass; Android Kotlin compilation and Swift syntax checks pass. Full iOS compilation and a signed-in production UI test remain unverified. No rules/indexes or reminder functions changed. Native clients require new releases.

## Previously deployed baseline

Deployed October 3, 2026: web Hosting, canonical Firestore rules, `classroomRefreshments`, `sendRefreshmentReminders`, and the account-deletion cleanup. All three targeted functions report ACTIVE. All 35 previously deployed functions remain present, with no source changes outside the intended account-deletion update. Live rules and website assets were verified; the live callable correctly rejects anonymous access. Native apps still need new releases. Actual signed-in end-to-end/push delivery testing remains pending; no test messages were sent to production students.

Classroom Management now includes an instructor-only “Refreshment sign-up” switch on web, Android and iOS. Its state is stored in `classrooms/{classId}/settings/refreshments.enabled` (missing means enabled, preserving existing behavior). The callable `configure` action requires current active instructor membership. Direct settings writes are blocked. When disabled, the entire dashboard row is hidden, open sheets close, student reads and all signup writes are denied, and reminder scans/claims stop. Existing entries are retained and reappear when enabled again. Clients listen to the setting and reconnect signup listeners after re-enabling. Already delivered notifications cannot be recalled.

Each classroom/date has one slot in `refreshmentSignups/{classId}__{yyyy-MM-dd}`. Multiple topics on that date share the slot. Dates come from `classSchedule`, grouped in the parish time zone (`classrooms/{classId}/settings/dailyFormation.timeZone`, falling back to America/New_York, consistent with existing reminders). Classes require an active classroom record. No production schedule or profile records were modified.

`classroomRefreshments` lists future/current dates and performs transactional claim/edit/cancel operations. Students can claim vacant spots and edit/cancel themselves only. Active instructors can assign any active classroom member and edit/cancel entries. Names are resolved from the volunteer's profile, never trusted from client input. A revision check rejects stale writes and concurrent claims. Cancel keeps an empty slot with a new revision. Notes have a 240-character limit. Clients cannot directly write slot records, including instructors; rules allow only active class members to read them.

Web, Android and iOS show a secondary row inside the upcoming-topic card opening the shared sheet. English and Spanish are supported. The instructor picker appears only for instructors. Sign-out/class changes reset client state; failed writes retain drafts. Device push taps are account/membership checked and open the sheet; web taps retain the existing classroom-switch safety check.

`sendRefreshmentReminders` runs every 15 minutes, sending at or after 9 a.m. on the calendar day before a scheduled class. Only the current active volunteer is targeted. Global `notificationsEnabled` is honored. No alert is sent for empty slots, removed/inactive users, deleted class dates, or archived classrooms. Volunteers need a registered device/browser token and OS notification permission. The per-date/per-user claim ledger avoids overlapping/duplicate successful scheduled runs; cancellation/reassignment is checked again before sending. A process crash after FCM delivery but before acknowledgement can still cause a retry, as with other at-least-once notifications. Reassigning the slot to a different person allows a reminder to that person. Date-only schedules retain existing timestamp/timezone interpretation; check imported dates in the parish zone before publishing.

Account deletion releases the user's slots and removes their reminder ledger records. Slots attached to removed dates remain hidden and send no reminders; recreating the same calendar date restores its slot. A rescheduled date has its own slot, so the instructor must arrange the new volunteer.

## Validation

- 114 web tests pass, including own signup, peer read-only controls, instructor selection, retained drafts, Spanish labels, listener cleanup, disabled-dashboard hiding/re-enabling, and setting-save rollback.
- Three pure policy tests pass (ownership, date grouping and DST/calendar-day reminder timing).
- Emulator integration covers racing claims, stale revisions, instructor reassignment, removed users, schedule deletion and targeted/deduplicated reminders with a mocked sender. No production messages are sent by these tests.
- Five emulator rule suites pass: existing inbox/discovery coverage plus refreshment reads/direct-write denial against both canonical rule copies.
- Android debug Kotlin compilation passes. Swift syntax checks pass; a full Xcode build remains unverified because the requested Xcode/SwiftPM cache permissions were not granted. Real device push delivery and full signed-in device UI checks remain required.
- Local browser fixture visually checked the compact card link and sign-up dialog. Fixture data is not production data.

## Deployment order (completed for web/backend)

1. From `apps/android`, deploy only `firestore:rules,functions:classroomRefreshments,functions:sendRefreshmentReminders,functions:deleteOwnAccount` to `ocia-application`. The rule copies are synchronized. Existing settings permissions are unchanged except that the new `refreshments` settings document is callable-write-only. No new composite index is required.
2. From `work/github-main-html-safe`, deploy Hosting only. Do not substitute its legacy functions codebase or mobile minimal Hosting directory.
3. Test with controlled instructor/student accounts and a configured schedule. Verify two concurrent claims, reassignment, cancellation, an actual next-day reminder, and notification taps. Never test by messaging real students.
4. Build/test and publish Android and iOS updates separately. Preserve all other deployed functions and existing notification behavior.
