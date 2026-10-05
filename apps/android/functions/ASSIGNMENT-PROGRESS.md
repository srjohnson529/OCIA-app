# Assignment completion rollout

The mobile and web discussion screens no longer require readings or lessons to be completed before opening a discussion. Posting is item-level progress, not whole-assignment completion.

The shared backend derives completion from current class-scoped data: every reading receipt, every linked completed lesson (including the existing quiz completion workflow), and every required linked discussion response. Optional/inactive discussions do not block completion. Assignment-specific prompts retain precedence over legacy lesson-linked prompts. Instructions-only assignments retain their manual completion button.

## Deployment required before releasing clients

Deploy only these functions to `ocia-application` from `apps/android`:

```
firebase deploy --project ocia-application --only functions:updateAssignmentAfterReading,functions:updateAssignmentAfterResponse,functions:updateAssignmentAfterLesson,functions:updateAssignmentAfterRequirements,functions:updateAssignmentAfterPrompt,functions:refreshAssignmentProgress,functions:reopenAssignmentWhenDiscussionResponseDeleted
```

Do not deploy unrelated functions, hosting, or security rules as part of this rollout. No rules changes are included. New clients call the authenticated refresh function at sign-in to reconcile existing records for that user. Source changes then reconcile automatically. Backend transactions read current source records, rather than trusting event ordering or a client-computed complete flag. Legacy clients may briefly write a premature complete flag; the completion trigger corrects it. Release updated clients promptly.

## Verification

- `node --test assignment-progress.test.js`: ten policy tests.
- Test on a signed-in student in a test class after deployment: discussion first must remain incomplete; finish readings and every lesson and confirm completion; undo a reading or delete a required response and confirm incomplete; add another required part and confirm reopening.
- Check all required discussions are shown, optional discussions do not block completion, instructor counts update, and a second class/student cannot satisfy requirements.
- Confirm instructions-only assignments still support manual completion.
- Full iOS build and device/browser acceptance testing are still needed.

Deployed to `ocia-application` on September 10, 2026 after user approval: all seven scoped functions created successfully in `us-central1`. Hosting, security rules, and unrelated functions were not deployed. Updated mobile builds and the web client still need release; signed-in acceptance testing remains outstanding.
