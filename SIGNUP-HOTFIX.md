# Web student signup hotfix — September 11, 2026

Fixed missing activeClassId in saveProfile. The field now matches classId and classIds. Local post-save state includes both fields too.

Published to illumined.net as hosting version a9ef5152ed7e8994, release 1789176659851000. This was a focused hotfix cloned from the live version ae1ba41cde8c221d; only /Catechism app.html changed. All 28 existing files and hosting configuration were preserved. Pending Student Details and other local web changes were NOT included. No rules or functions changed.

Rollback source: projects/ocia-application/sites/ocia-application/versions/ae1ba41cde8c221d.

Verified the public page contains the corrected field. Emulator tests rejected the old payload, accepted the fixed payload, and continued to reject class-code reentry for a removed student. Tests are in signup-profile.test.cjs (optional SIGNUP_RULES_TEST_PACKAGE points to a package with Firebase rules-testing dependencies; emulator port 8098).

The HTML fix is included in this branch so subsequent releases retain the production hotfix.
