# Curriculum Workflow

## Source of truth

The authoritative lesson catalog is:

`curriculum/lessons.json`

Do not make independent editorial changes to the application copies. That creates silent differences between platforms.

## Review sequence

1. Edit the canonical catalog.
2. Generate the readable review editions in `curriculum/review/`.
3. Record clergy, theological, and instructional review decisions.
4. Assign a new catalog version after approval.
5. Copy the approved catalog into each application’s runtime resource location.
6. Verify lesson count, identifiers, titles, quizzes, and catalog checksum across all three applications.
7. Build and test iOS, Android, and HTML before release.

## Commands

From the project root, check the current state without changing files:

```sh
python3 tools/curriculum/sync_lessons.py --check
```

After editing and reviewing the canonical catalog, update every application and regenerate the review editions:

```sh
python3 tools/curriculum/sync_lessons.py
```

## Application resource locations

- iOS: `apps/ios/lessons.json` (the resource currently included in the Xcode application target)
- Android: `apps/android/app/src/main/res/raw/lessons.json`
- HTML: `apps/html/lessons.json`

These three files should match `curriculum/lessons.json` exactly after synchronization.

The imported iOS project also contains `apps/ios/Resources/lessons.json`. It is not the copy included in the application target, but it is synchronized for consistency. Its necessity should be evaluated after the iOS project builds successfully.

## Spiritual-formation content

The authoritative Formation catalog is:

`curriculum/spiritual_formation.json`

It supplies the common prayers, Rosary content, Lectio Divina, Liturgy of the Hours, examination of conscience, and spiritual-practice content used across the applications.

Check validity and synchronization without changing files:

```sh
python3 tools/curriculum/sync_spiritual_formation.py --check
```

After editing the canonical Formation catalog, synchronize the runtime resources:

```sh
python3 tools/curriculum/sync_spiritual_formation.py
```

The synchronized destinations are:

- iOS: `apps/ios/spiritual_formation.json`
- Android: `apps/android/app/src/main/res/raw/spiritual_formation.json`
- HTML: `apps/html/spiritual_formation.json`

The iOS application target runs the read-only synchronization check before compilation. Invalid JSON, missing required fields, duplicate identifiers or order values, non-sequential ordering, and differences between application copies stop the build.
