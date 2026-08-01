# Curriculum tools

Run these commands from anywhere using Python 3.

## Check synchronization

```sh
python3 tools/curriculum/sync_lessons.py --check
```

This validates `curriculum/lessons.json`, rejects duplicate lesson IDs or missing required fields, and confirms that every application resource matches it. It does not change files.

## Synchronize applications and review documents

```sh
python3 tools/curriculum/sync_lessons.py
```

This copies the canonical catalog to iOS, Android, and HTML; regenerates the Markdown and plain-text review editions; and verifies SHA-256 checksums.

Always edit `curriculum/lessons.json`, never an application copy.

## Spiritual formation

Check the canonical Formation catalog and all three application copies without changing files:

```sh
python3 tools/curriculum/sync_spiritual_formation.py --check
```

After editing `curriculum/spiritual_formation.json`, validate it and synchronize iOS, Android, and HTML:

```sh
python3 tools/curriculum/sync_spiritual_formation.py
```

Always edit `curriculum/spiritual_formation.json`, never an application copy. The iOS build automatically runs the read-only check and stops with a clear error if the catalog is malformed or any application copy is out of sync.
