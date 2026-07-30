#!/usr/bin/env python3
"""Validate and synchronize the Illumined lesson catalog across all applications."""

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
CANONICAL = PROJECT_ROOT / "curriculum" / "lessons.json"
DESTINATIONS = {
    "iOS active target": PROJECT_ROOT / "apps" / "ios" / "lessons.json",
    "iOS secondary resource": PROJECT_ROOT / "apps" / "ios" / "Resources" / "lessons.json",
    "Android": PROJECT_ROOT / "apps" / "android" / "app" / "src" / "main" / "res" / "raw" / "lessons.json",
    "HTML": PROJECT_ROOT / "apps" / "html" / "lessons.json",
}
EXPORTER = Path(__file__).with_name("export_lessons.py")


def checksum(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_catalog(path: Path) -> tuple[int, str]:
    if not path.is_file():
        raise ValueError(f"Catalog not found: {path}")

    try:
        catalog = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Catalog is not valid JSON: {error}") from error

    if not isinstance(catalog, dict) or not isinstance(catalog.get("lessons"), list):
        raise ValueError("Catalog must contain a top-level lessons array")

    lessons = catalog["lessons"]
    required = {"id", "category", "title", "contentHTML", "quiz"}
    missing = []
    ids = []
    for index, lesson in enumerate(lessons, start=1):
        if not isinstance(lesson, dict):
            raise ValueError(f"Lesson {index} is not an object")
        absent = sorted(required - lesson.keys())
        if absent:
            missing.append(f"lesson {index}: {', '.join(absent)}")
        lesson_id = lesson.get("id")
        if not isinstance(lesson_id, str) or not lesson_id.strip():
            raise ValueError(f"Lesson {index} has no valid ID")
        ids.append(lesson_id)

    if missing:
        raise ValueError("Required fields are missing: " + "; ".join(missing))

    duplicates = sorted({lesson_id for lesson_id in ids if ids.count(lesson_id) > 1})
    if duplicates:
        raise ValueError("Duplicate lesson IDs: " + ", ".join(duplicates))

    return len(lessons), str(catalog.get("version", "unversioned"))


def report_status(canonical_hash: str) -> bool:
    all_match = True
    for label, destination in DESTINATIONS.items():
        if not destination.is_file():
            print(f"MISSING  {label}: {destination}")
            all_match = False
            continue
        destination_hash = checksum(destination)
        state = "MATCH" if destination_hash == canonical_hash else "DIFF"
        print(f"{state:7} {label}: {destination}")
        all_match = all_match and destination_hash == canonical_hash
    return all_match


def synchronize() -> None:
    for label, destination in DESTINATIONS.items():
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(CANONICAL, destination)
        print(f"UPDATED {label}: {destination}")

    subprocess.run([sys.executable, str(EXPORTER)], check=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate and compare files without changing them.",
    )
    args = parser.parse_args()

    try:
        lesson_count, version = validate_catalog(CANONICAL)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    canonical_hash = checksum(CANONICAL)
    print(f"Canonical catalog: version {version}, {lesson_count} lessons")
    print(f"SHA-256: {canonical_hash}")

    if args.check:
        matches = report_status(canonical_hash)
        return 0 if matches else 2

    synchronize()
    matches = report_status(canonical_hash)
    if not matches:
        print("ERROR: synchronization verification failed", file=sys.stderr)
        return 2

    print("Curriculum synchronization complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

