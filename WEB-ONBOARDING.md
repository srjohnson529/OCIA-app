# Unpublished shared-account onboarding

The main webapp now opens with Find My Classroom, QR image entry, sign-in, invitation entry, and instructor/parish registration. Existing marketing content remains below. Blue/gold styling lives in `public/web-onboarding.css`; isolated UI logic is in `public/web-onboarding.js`.

## Account and activation

All authentication uses the webapp's existing Firebase project, `ocia-application`, also used by the native clients. No second user database or payment password is created. Existing classroom members sign in directly. Parish registration creates only a Firebase Auth account; it does not grant an instructor role. With a valid startup code, `activateParishAccess` reserves that code exclusively for the authenticated UID. `startParishClass` uses the server-held activation, creates profile/classroom atomically, and consumes the code. A retry returns the same classroom. A classroom created on the web is available on native devices using the same credentials.

The user can enable parish/city search during setup (checked by default, with explicit disclosure). Search-selected students request approval; no client profile write grants membership. Instructor invitation codes remain a separate native pathway. Web QR support uses BarcodeDetector where supported; manual invitation entry remains available everywhere. QR images are not uploaded.

## Payment scaffold — intentionally disabled

The activation screen clearly says payment is unavailable, shows a disabled payment button, and offers code activation/contact. The authenticated `createParishCheckout` endpoint always returns failed-precondition. There is no provider SDK, price, card form, redirect, fake success, payment collection, or webhook deployed by this change.

Future integration contract:

1. Choose provider, price/currency, refund/access policy and purchase terms. Do not accept a client-supplied price or UID.
2. Implement authenticated checkout using Firebase UID as server-side purchase metadata; require verified email before payment. Reuse provider customers and checkout idempotency keys.
3. Add a server webhook with signature verification and event-ID deduplication. Check paid amount/currency/product and matching UID. Browser success URLs must never grant access.
4. Have the verified webhook create a one-time setup code and bind it to `parishAccess/{uid}` with status `ready`, setupCode, provider identifiers and audit timestamps. Code must be reserved (`claimedBy: uid`, `isActive: false`, no usedBy) in the same transaction. Email is informational; the signed-in account can resume without re-entering the code.
5. Handle retries, delayed payment, cancellation, refunds and disputes explicitly. Do not erase classrooms/progress automatically. Never store card data in Firestore.
6. Add provider sandbox tests before enabling checkout. The backend capability flag and UI must change together.

`parishAccess` is server-only under default-deny rules. `getParishAccess` exposes only the caller's access status, class ID and enrollment status; never the stored code or billing records. No client can self-assert payment or instructor access.

## Release prerequisites (nothing published)

- Backend source is **apps/android/functions**, not this web worktree's older `ocia_functions_code`. Review and deploy the new access endpoints plus the classroom discovery endpoints from the canonical backend only when authorized.
- Use the canonical Android Firebase rules including classroom approval; do not deploy this worktree's older rules over them.
- Test full integration in Firebase emulators/staging, including activation races, pending approval and sign-in on an actual device.
- Native clients can open a classroom created on the web. Their unfinished-setup screens still need a future UI update to discover a ready account-linked activation without asking for a code; the server already accepts code-free completion for an activated UID.
- New interface copy is currently English. Add Spanish translations before the multilingual release.
- Nothing here constitutes a live checkout or billing system. Hosting, functions and rules have not been deployed.
