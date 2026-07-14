#!/usr/bin/env python3
"""Regenerates app/src/main/assets/device_names.json from Google Play's public
supported-devices list (https://storage.googleapis.com/play_public/supported_devices.csv).

Maps Build.MODEL -> consumer marketing name ("<Retail Branding> <Marketing Name>", deduped
when the marketing name already starts with the brand). Models with more than one distinct
marketing name across the CSV (common among generic/white-label hardware) are dropped rather
than guessed at; the app falls back to the raw model string for those.

Usage: python scripts/generate_device_names.py
"""
import csv
import json
import urllib.request
from collections import defaultdict
from pathlib import Path

CSV_URL = "https://storage.googleapis.com/play_public/supported_devices.csv"
OUTPUT_PATH = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "device_names.json"


def build_display_name(brand: str, marketing_name: str) -> str:
    if not brand or marketing_name.lower().startswith(brand.lower()):
        return marketing_name
    return f"{brand} {marketing_name}"


def main() -> None:
    with urllib.request.urlopen(CSV_URL) as response:
        text = response.read().decode("utf-16le")

    rows = csv.reader(text.splitlines())
    next(rows)  # header: Retail Branding, Marketing Name, Device, Model

    names_by_model: dict[str, set[str]] = defaultdict(set)
    for row in rows:
        if len(row) < 4:
            continue
        brand, marketing_name, _device, model = (value.strip() for value in row[:4])
        if not model or not marketing_name:
            continue
        names_by_model[model].add(build_display_name(brand, marketing_name))

    result = {model: next(iter(names)) for model, names in names_by_model.items() if len(names) == 1}

    OUTPUT_PATH.write_text(
        json.dumps(result, ensure_ascii=False, separators=(",", ":"), sort_keys=True),
        encoding="utf-8",
    )
    print(f"{len(names_by_model)} distinct models, {len(result)} unambiguous -> {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
