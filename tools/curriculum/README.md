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
