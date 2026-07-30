# Version Control

## Repository model

Illumined uses one local Git repository rooted at `/Users/stephenjohnson/Documents/Codex/Illumined` for the iOS, Android, HTML, curriculum, documentation, and shared tools.

No GitHub remote has been configured and nothing has been pushed externally.

## Preserved iOS history

The imported iOS application originally had its own nested Git repository. Its metadata and a portable Git bundle are preserved locally under:

`archive/git-history/`

The archive is intentionally excluded from the unified repository. The application’s current working files, including its previously uncommitted changes, are represented in the unified repository baseline.

## Local configuration

Firebase application configuration, signing files, Android local settings, IDE state, build output, dependencies, backups, and local archives are excluded through `.gitignore`.

Fresh checkouts will need their own authorized copies of:

- `apps/ios/GoogleService-Info.plist`
- `apps/android/app/google-services.json`
- Android `local.properties`
- Any future signing or keystore configuration

Do not commit credentials or production signing material.

## Normal workflow

1. Make changes inside the consolidated workspace.
2. Run `python3 tools/curriculum/sync_lessons.py --check` for curriculum-related work.
3. Review `git status` and the actual diff.
4. Build or test the affected application.
5. Commit related changes together with a descriptive message.
6. Push only after a GitHub repository and remote have been deliberately selected.

