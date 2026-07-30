# Illumined Final Organization Inventory

Snapshot date: 2026-07-30

Sizes below are allocated sizes reported by macOS. Cloud placeholders can change size when macOS downloads them.

## Canonical workspace

The active Illumined workspace is `/Users/stephenjohnson/Documents/Codex/Illumined`.

| Area | Size | Status |
| --- | ---: | --- |
| `apps/ios/` | 2.6 MB | Canonical iOS app; confirmed functioning in Xcode |
| `apps/android/` | 2.2 MB | Canonical Android source/configuration; 140 tests and debug build passed; generated output is no longer in the app tree |
| `apps/html/` | 860 KB | Canonical HTML app; browser runtime validation passed |
| `curriculum/` | 1.3 MB | Canonical lesson source and human-review exports |
| `archive/` | 26 MB | Verified historical source, evidence, and variants; intentionally ignored by Git |
| `work/` | 1.1 GB | One ignored cloud-sensitive cache-retirement group; no active source |

The canonical lesson SHA-256 is `1040a1f5fc4914e85a3a5777423a4354ca4dadd1f44452bfc3140452ac2d1610`. Every active runtime lesson copy matches it.

## Remaining generated data

These locations are not canonical source and can be regenerated. Fully local caches were removed from their active paths on 2026-07-30:

| Location | Size | Notes |
| --- | ---: | --- |
| `~/.Trash/Illumined-local-generated-caches-2026-07-30/` | 2.5 GB | Recoverable group containing local Swift packages, iOS validation data, Xcode caches/DerivedData, and Android project caches; Trash was not emptied |
| `work/cloud-sensitive-cache-retirement/android-gradle-download-attempt/` | approximately 991 MB | Temporary Gradle download attempt containing 54 cloud-only placeholders; grouped by metadata-only move |
| `work/cloud-sensitive-cache-retirement/android-app-build/` | approximately 122 MB | Generated Android build products containing 9 cloud-only placeholders; removed from the active app tree without hydration |
| `2026-07-22/i-w/work/cloud-sensitive-cache-retirement/` | 1.4 GB | Both legacy Gradle homes, containing 11,494 cloud-only placeholders; grouped without hydration |

The cloud-sensitive folders remain allocated in their current cloud locations because an ordinary Trash move would download their placeholders first. They are now clearly isolated from active source. They should be removed only with a File Provider-aware method that does not hydrate them, without touching `apps/`, `curriculum/`, `archive/`, `docs/`, the legacy Android evidence, or the debug keystore.

## Remaining dated material

| Location | Size | Reason retained |
| --- | ---: | --- |
| `2026-07-22/i-w/` | 1.4 GB | Legacy Android folder; source and evidence are archived, and both Gradle caches are isolated under `work/cloud-sensitive-cache-retirement/` |
| `2026-07-09/ana/` | 20 KB | Historical HTML-improvement experiment and analysis placeholders; not an active app |
| `2026-07-09/build-a-swift-ui-app-for/` | 132 KB | Two guide/test documents, five transformation scripts, and Finder metadata; active app outputs and defective curriculum were retired |

No active legacy iOS or standalone HTML application remains on the Desktop or in the dated output folders.

## Curriculum safety

The one known defective catalog is isolated at `archive/legacy/defective-curriculum-2026-07-09/lessons.json`. It has SHA-256 `1c9b07ff305230bfadc2c4889eed0d5fcfea876d9a04cb5687aefee02ce6de9f` and must not be synchronized into an app.

## Recommended next cleanup

1. Keep Trash unemptied until the consolidated applications have been used normally for an agreed observation period.
2. Then permanently remove the specifically labeled generated-cache Trash group, or empty Trash only after reviewing every other retained recovery item.
3. Remove the two `cloud-sensitive-cache-retirement/` groups with a File Provider-aware method that does not hydrate placeholders first.
4. Archive the remaining small HTML experiment and transformation documents/scripts if they still have historical value.
