# Illumined Final Organization Inventory

Snapshot date: 2026-07-30

Sizes below are allocated sizes reported by macOS. Cloud placeholders can change size when macOS downloads them.

## Canonical workspace

The active Illumined workspace is `/Users/stephenjohnson/Documents/Codex/Illumined`.

| Area | Size | Status |
| --- | ---: | --- |
| `apps/ios/` | 2.6 MB | Canonical iOS app; confirmed functioning in Xcode |
| `apps/android/` | 128 MB | Canonical Android app; 140 tests and debug build passed; size includes generated build output |
| `apps/html/` | 860 KB | Canonical HTML app; browser runtime validation passed |
| `curriculum/` | 1.3 MB | Canonical lesson source and human-review exports |
| `archive/` | 26 MB | Verified historical source, evidence, and variants; intentionally ignored by Git |
| `work/` | 3.4 GB | Generated validation caches only; ignored by Git and eligible for controlled cleanup |

The canonical lesson SHA-256 is `1040a1f5fc4914e85a3a5777423a4354ca4dadd1f44452bfc3140452ac2d1610`. Every active runtime lesson copy matches it.

## Remaining generated data

These locations are not canonical source and can be regenerated:

| Location | Size | Notes |
| --- | ---: | --- |
| `work/SourcePackages/` | 1.2 GB | Firebase Swift package checkouts and binary artifacts |
| `work/ios-validation/` | 1.2 GB | Temporary package/build validation clone |
| `work/android-gradle-download-attempt/` | 987 MB | Temporary Gradle download and partial-copy attempt |
| `work/XcodeCache/` | 60 MB | Redirected Xcode and Swift package caches |
| `work/DerivedData/` | 1.5 MB | Temporary Xcode build data |
| `apps/android/app/build/` | 122 MB | Generated Android build products |
| `apps/android/.gradle/` | 3.7 MB | Generated project cache |
| `apps/android/.kotlin/` | 368 KB | Generated Kotlin compiler state |

The ignored `work/` folders were created or reused during validation. They should be removed only as exact targets, without touching `apps/`, `curriculum/`, `archive/`, or `docs/`.

## Remaining dated material

| Location | Size | Reason retained |
| --- | ---: | --- |
| `2026-07-22/i-w/` | 1.4 GB | Legacy Android folder; source and evidence are archived, but two cloud-sensitive Gradle caches remain in `work/` |
| `2026-07-09/ana/` | 20 KB | Historical HTML-improvement experiment and analysis placeholders; not an active app |
| `2026-07-09/build-a-swift-ui-app-for/` | 132 KB | Two guide/test documents, five transformation scripts, and Finder metadata; active app outputs and defective curriculum were retired |

No active legacy iOS or standalone HTML application remains on the Desktop or in the dated output folders.

## Curriculum safety

The one known defective catalog is isolated at `archive/legacy/defective-curriculum-2026-07-09/lessons.json`. It has SHA-256 `1c9b07ff305230bfadc2c4889eed0d5fcfea876d9a04cb5687aefee02ce6de9f` and must not be synchronized into an app.

## Recommended next cleanup

1. Remove the generated validation folders under the canonical `work/` directory.
2. Remove canonical Android build output only when another APK is not immediately needed.
3. Handle the two legacy Android Gradle caches with a cloud-safe method that does not hydrate placeholders first.
4. Archive the remaining small HTML experiment and transformation documents/scripts if they still have historical value.
5. Keep Trash unemptied until the consolidated applications have been used normally for an agreed observation period.
