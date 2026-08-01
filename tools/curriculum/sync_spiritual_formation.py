#!/usr/bin/env python3
"""Validate and synchronize Illumined spiritual-formation content."""

import argparse
import hashlib
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
CANONICAL = PROJECT_ROOT / "curriculum" / "spiritual_formation.json"
DESTINATIONS = {
    "iOS": PROJECT_ROOT / "apps" / "ios" / "spiritual_formation.json",
    "Android": PROJECT_ROOT / "apps" / "android" / "app" / "src" / "main" / "res" / "raw" / "spiritual_formation.json",
    "HTML": PROJECT_ROOT / "apps" / "html" / "spiritual_formation.json",
}


def checksum(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_object(value: object, label: str) -> dict:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be an object")
    return value


def require_list(value: object, label: str) -> list:
    if not isinstance(value, list):
        raise ValueError(f"{label} must be an array")
    return value


def require_text(item: dict, key: str, label: str) -> str:
    value = item.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{label}.{key} must be non-empty text")
    return value


def require_integer(item: dict, key: str, label: str) -> int:
    value = item.get(key)
    if not isinstance(value, int) or isinstance(value, bool):
        raise ValueError(f"{label}.{key} must be an integer")
    return value


def require_unique(values: list[str], label: str) -> None:
    duplicates = sorted(value for value, count in Counter(values).items() if count > 1)
    if duplicates:
        raise ValueError(f"Duplicate {label}: {', '.join(duplicates)}")


def validate_catalog(path: Path) -> tuple[int, int, str]:
    if not path.is_file():
        raise ValueError(f"Catalog not found: {path}")

    try:
        catalog = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Catalog is not valid JSON: {error}") from error

    catalog = require_object(catalog, "Catalog")
    required_root_fields = {
        "version",
        "source",
        "commonPrayers",
        "rosary",
        "lectioDivina",
        "liturgyOfTheHours",
        "examinationOfConscience",
        "spiritualPractices",
    }
    missing_root_fields = sorted(required_root_fields - catalog.keys())
    if missing_root_fields:
        raise ValueError("Missing top-level fields: " + ", ".join(missing_root_fields))

    version = require_text(catalog, "version", "Catalog")
    require_text(catalog, "source", "Catalog")

    prayers = require_list(catalog["commonPrayers"], "commonPrayers")
    prayer_ids: list[str] = []
    prayer_orders: list[int] = []
    for index, value in enumerate(prayers):
        label = f"commonPrayers[{index}]"
        prayer = require_object(value, label)
        prayer_ids.append(require_text(prayer, "id", label))
        require_text(prayer, "title", label)
        require_text(prayer, "text", label)
        prayer_orders.append(require_integer(prayer, "sortOrder", label))
    require_unique(prayer_ids, "common prayer IDs")
    require_unique([str(order) for order in prayer_orders], "common prayer sort orders")
    if prayer_orders != list(range(len(prayers))):
        raise ValueError("Common prayer sortOrder values must be sequential and match array order")

    rosary = require_object(catalog["rosary"], "rosary")
    rosary_prayers = require_object(rosary.get("prayers"), "rosary.prayers")
    for key in (
        "signOfTheCross",
        "apostlesCreed",
        "ourFather",
        "hailMary",
        "gloryBe",
        "fatimaPrayer",
        "hailHolyQueen",
        "concludingPrayer",
    ):
        require_text(rosary_prayers, key, "rosary.prayers")

    mystery_sets = require_list(rosary.get("mysteries"), "rosary.mysteries")
    mystery_set_ids: list[str] = []
    mystery_ids: list[str] = []
    for set_index, value in enumerate(mystery_sets):
        set_label = f"rosary.mysteries[{set_index}]"
        mystery_set = require_object(value, set_label)
        mystery_set_ids.append(require_text(mystery_set, "id", set_label))
        require_text(mystery_set, "title", set_label)
        require_text(mystery_set, "name", set_label)
        require_text(mystery_set, "descriptionHTML", set_label)
        mysteries = require_list(mystery_set.get("mysteries"), f"{set_label}.mysteries")
        for mystery_index, mystery_value in enumerate(mysteries):
            label = f"{set_label}.mysteries[{mystery_index}]"
            mystery = require_object(mystery_value, label)
            mystery_ids.append(require_text(mystery, "id", label))
            require_text(mystery, "title", label)
            require_text(mystery, "scripture", label)
    require_unique(mystery_set_ids, "Rosary mystery-set IDs")
    require_unique(mystery_ids, "Rosary mystery IDs")

    lectio = require_object(catalog["lectioDivina"], "lectioDivina")
    require_text(lectio, "title", "lectioDivina")
    require_text(lectio, "contentHTML", "lectioDivina")
    if "steps" in lectio:
        steps = require_list(lectio["steps"], "lectioDivina.steps")
        for index, value in enumerate(steps):
            require_object(value, f"lectioDivina.steps[{index}]")

    hours_section = require_object(catalog["liturgyOfTheHours"], "liturgyOfTheHours")
    require_text(hours_section, "title", "liturgyOfTheHours")
    require_text(hours_section, "description", "liturgyOfTheHours")
    hours = require_list(hours_section.get("hours"), "liturgyOfTheHours.hours")
    hour_ids: list[str] = []
    for index, value in enumerate(hours):
        label = f"liturgyOfTheHours.hours[{index}]"
        hour = require_object(value, label)
        hour_ids.append(require_text(hour, "id", label))
        require_text(hour, "title", label)
        require_text(hour, "description", label)
    require_unique(hour_ids, "Liturgy of the Hours IDs")

    examination = require_object(catalog["examinationOfConscience"], "examinationOfConscience")
    require_text(examination, "title", "examinationOfConscience")
    require_text(examination, "contentHTML", "examinationOfConscience")

    practices = require_list(catalog["spiritualPractices"], "spiritualPractices")
    practice_ids: list[str] = []
    practice_orders: list[int] = []
    for index, value in enumerate(practices):
        label = f"spiritualPractices[{index}]"
        practice = require_object(value, label)
        practice_ids.append(require_text(practice, "id", label))
        require_text(practice, "title", label)
        require_text(practice, "contentHTML", label)
        practice_orders.append(require_integer(practice, "sortOrder", label))
    require_unique(practice_ids, "spiritual-practice IDs")
    require_unique([str(order) for order in practice_orders], "spiritual-practice sort orders")
    if practice_orders != list(range(len(practices))):
        raise ValueError("Spiritual-practice sortOrder values must be sequential and match array order")

    return len(prayers), len(mystery_ids), version


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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate and compare files without changing them.",
    )
    args = parser.parse_args()

    try:
        prayer_count, mystery_count, version = validate_catalog(CANONICAL)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    canonical_hash = checksum(CANONICAL)
    print(
        f"Canonical formation: version {version}, "
        f"{prayer_count} common prayers, {mystery_count} mysteries"
    )
    print(f"SHA-256: {canonical_hash}")

    if args.check:
        matches = report_status(canonical_hash)
        return 0 if matches else 2

    synchronize()
    if not report_status(canonical_hash):
        print("ERROR: synchronization verification failed", file=sys.stderr)
        return 2

    print("Spiritual-formation synchronization complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
