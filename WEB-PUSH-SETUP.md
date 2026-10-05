# Browser push setup

The user-supplied public VAPID key is configured in `public/web-push-config.js`. Hosting and canonical Firestore rules were deployed on October 3, 2026, and live asset/rule contents verified. Actual push delivery testing with test accounts is still pending. No private key is stored in the website.

1. Open Firebase Console → `ocia-application` → Project settings → Cloud Messaging → Web Push certificates. Copy the public key from the key pair (generate a pair there only if none exists). Never copy a private key into the website.
2. Set `vapidKey` in `public/web-push-config.js`. Ensure the FCM Registration API is enabled for the project if registration reports it disabled.
3. Deploy Hosting and the messaging triggers from `apps/android/functions` after review. This web worktree's `ocia_functions_code` is a different legacy codebase; do not substitute it for those triggers. Always target specific functions to preserve other deployed services.
4. On the HTTPS website, sign in and choose More → Notifications → Enable browser notifications. Accept the browser prompt. The choice is opt-in; there is no automatic permission prompt on page load.
5. Test with test accounts only: foreground alert, closed-tab/background push, opening a private message, blocked permission, sign-out, account switch, inactive/removed membership, and browser-only disable. Existing device notification settings are not switched off by browser disable.

The service worker handles notification clicks before loading Firebase and lets Firebase display background notification payloads exactly once. No private content is cached. Links carry only account/class/type routing metadata in the URL fragment. The app rechecks membership and recipient identity. For another assigned classroom, the user is asked to switch class before opening the alert.

The web-app manifest supports installation to the Home Screen. On iOS/iPadOS, browser push requires a compatible OS and a Home Screen web app. Browser support is detected before requesting permission.

Reference: https://firebase.google.com/docs/cloud-messaging/web/get-started and https://firebase.google.com/docs/cloud-messaging/web/receive-messages
