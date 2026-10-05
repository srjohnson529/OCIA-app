# Parish & Account Support

Available under Admin Tools on iOS/Android and the admin-only Parish & Account Support navigation button on web. Both callables require userProfiles/{uid}.isAdmin exactly true; mutations recheck it inside the transaction.

- Directory searches classroom names/parish names/IDs and account names/emails/IDs. Pages return up to 20 matches and scan at most 1,000 existing documents per request. Continue search / Load more resumes from the returned cursor; an empty page is not necessarily an exhausted search. No search-index migration is needed. Directory results deliberately exclude tokens, quiz answers, and other profile details.
- Classroom summaries include active student counts, assigned instructors, current owner and a missing-owner flag. Archive is reversible and does not delete classroom content, accounts or progress. Admins may archive the only active classroom; affected instructors receive an empty active-class selection until a class is restored or selected. Instructor archive lists/active selections are updated atomically, capped at 400 instructors per classroom.
- Restore access only restores an existing or removed membership and clears its inactive/removed/archive flags. It never promotes a student or grants access to an unrelated classroom. Restore an archived classroom first. Admin-profile restoration is not offered.
- Ownership transfers require a currently assigned active instructor and an unchanged expected owner. The previous owner stays an instructor; createdBy remains historical metadata. The archive/restore ownership check in index.js now uses instructorId, falling back to createdBy only on legacy ownerless records.
- Student invitations reuse an active, unexpired code; support does not rotate it or invalidate existing links. When missing/expired, the classroom instructor must generate a new code using Classroom Codes. Admins may create a new one-use instructor invitation, using the existing invitation format. Links are copied/shared manually; no email is sent automatically.
- Every mutation requires a reason, explicit client confirmation and an idempotency request ID. adminSupportEvents records the action, actor, target, previous owner, reason and result. These events are server-only under the existing deny-by-default rules; no public audit-data access was added.

## Release

Deploy adminDirectory and adminClassSupport. Also deploy archiveClass and restoreClass to apply the transferred-owner safeguard. Release the native changes and web HTML plus public/admin-support.js. These changes are not deployed by local implementation/testing. Do not deploy unrelated pending enrollment-rule changes. No production accounts, classrooms, invitations or memberships were modified during testing.

## Verification

admin-support.test.js exercises admin authorization, minimal directory data, preserved progress, restored membership bounds, archive/restore, transfer target checks and stale owners, idempotency, invitation status and audit records. admin-support.emulator.test.cjs runs real local Firestore transactions, including simultaneous requests for the same invitation. Web tests validate gating and syntax. Android compilation and iOS syntax checks are separate from release-device testing.
