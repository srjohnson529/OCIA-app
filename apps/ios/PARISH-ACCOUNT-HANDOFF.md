# Shared-account native handoff (unpublished)

iOS and Android profile setup call `getParishAccess` for the authenticated Firebase account. A `ready` response selects New Parish, hides the startup-code field and permits completion using the server-held activation. Normal classroom/profile listeners continue to route already configured accounts into the app. Code activation and payment remain server responsibilities; no client grants itself instructor access.

Loading/error/retry states are included. Checks are scoped to the authenticated UID; canceled or disposed views cannot apply a prior account's response. Access is not persisted locally as an entitlement. If the endpoint is unavailable, explain the failure and retain the manual code route; never infer access from a network error. Pending student requests and co-instructor invitations retain their existing paths.

Release dependency: deploy the canonical `getParishAccess` and updated `startParishClass` functions together before distributing these clients. Nothing is deployed by this change.

Validation before release: finish web activation, sign in with the same credentials on each native platform, confirm no code prompt, create the classroom and reopen on another device. Repeat with an existing completed classroom, a non-activated account, failed/offline access lookup, sign-out during lookup, and pending student enrollment. A native setup still asks the instructor to enable search afterward in Classroom Codes (the web setup includes that option directly).
