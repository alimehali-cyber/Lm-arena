#!/usr/bin/env python3
"""Offline, fail-closed structural audit for the Satellite Detail media manifest.

This checks recorded evidence and bundled file constraints; it cannot determine whether a
third-party copyright statement is true. Every approved asset still needs individual human review.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import re
import sys
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MANIFEST = ROOT / "app/src/main/assets/satellites/sat_media_manifest.json"
DEFAULT_ASSETS = ROOT / "app/src/main/assets/satellites/media"
REQUIRED = {"iss", "tiangong", "hubble", "starlink"}
BUNDLED_PREFIX = "file:///android_asset/satellites/media/"
LICENCE = re.compile(r"^(?:PUBLIC DOMAIN|PD|NASA PUBLIC DOMAIN|CC0(?: 1\.0)?|CC BY(?:-SA)? [1-4]\.0)$", re.I)


def valid_source(url: str) -> bool:
    p = urlparse(url)
    host = (p.hostname or "").lower()
    if p.scheme != "https" or p.username or p.password:
        return False
    if host == "nasa.gov" or host.endswith(".nasa.gov"):
        return bool(p.path.strip("/"))
    if host == "commons.wikimedia.org":
        return unquote(p.path).startswith("/wiki/File:")
    if host in {"flickr.com", "www.flickr.com"}:
        return re.match(r"^/photos/(?:europeanspaceagency|spacex)/[^/]+", p.path, re.I) is not None
    return False


def dimensions(path: Path) -> tuple[int, int]:
    try:
        from PIL import Image
    except ImportError as exc:
        raise ValueError("Pillow is required to inspect admitted bundled media") from exc
    try:
        with Image.open(path) as image:
            size = image.size
            image.verify()
            return size
    except Exception as exc:
        raise ValueError(f"cannot decode image {path.name}: {exc}") from exc


def validate(manifest: object, asset_root: Path) -> list[str]:
    errors: list[str] = []
    if not isinstance(manifest, dict) or manifest.get("schemaVersion") != 1:
        return ["schemaVersion must be 1"]
    spacecraft = manifest.get("spacecraft")
    if not isinstance(spacecraft, dict):
        return ["spacecraft must be an object"]
    for key in sorted(REQUIRED - spacecraft.keys()):
        errors.append(f"{key}: required TODO or VERIFIED entry is missing")
    for key, craft in spacecraft.items():
        if key not in REQUIRED and not re.fullmatch(r"[1-9][0-9]*", key):
            errors.append(f"{key}: use a numeric catalogue key, not a generic shape key")
        if not isinstance(craft, dict):
            errors.append(f"{key}: entry must be an object")
            continue
        status, images = craft.get("status"), craft.get("images")
        if status not in {"TODO", "VERIFIED"}:
            errors.append(f"{key}: status must be TODO or VERIFIED")
        if not isinstance(images, list):
            errors.append(f"{key}: images must be an array")
            continue
        if status == "TODO" and images:
            errors.append(f"{key}: TODO entries must not contain images")
        if status == "VERIFIED" and not 1 <= len(images) <= (1 if key == "starlink" else 6):
            errors.append(f"{key}: VERIFIED requires 1–{'1' if key == 'starlink' else '6'} images")
        for index, image in enumerate(images):
            label = f"{key}[{index}]"
            if not isinstance(image, dict):
                errors.append(f"{label}: image must be an object")
                continue
            for field in ("uri", "kind", "credit", "creditFa", "license", "sourceUrl", "placeholderColor", "altEn", "altFa"):
                if not isinstance(image.get(field), str) or not image[field].strip():
                    errors.append(f"{label}: missing {field}")
            if not isinstance(image.get("author"), str):
                errors.append(f"{label}: author must be recorded (empty only for a sole agency author)")
            if image.get("verified") is not True:
                errors.append(f"{label}: verified must be true after individual review")
            if image.get("kind") not in {"PHOTO", "RENDER", "ARTIST_CONCEPT"}:
                errors.append(f"{label}: invalid kind")
            if not LICENCE.fullmatch(str(image.get("license", "")).strip()):
                errors.append(f"{label}: licence is missing or outside the permitted PD/CC0/CC BY/CC BY-SA set")
            if not valid_source(str(image.get("sourceUrl", ""))):
                errors.append(f"{label}: sourceUrl must be an eligible individual NASA, Commons File, ESA or SpaceX page")
            if not re.fullmatch(r"#[0-9a-fA-F]{6}", str(image.get("placeholderColor", ""))):
                errors.append(f"{label}: placeholderColor must be #RRGGBB")
            focal = image.get("focal")
            if not (isinstance(focal, list) and len(focal) == 2 and all(
                isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v) and 0 <= v <= 1 for v in focal
            )):
                errors.append(f"{label}: focal must be two finite fractions in [0, 1]")
            uri = image.get("uri")
            if not isinstance(uri, str):
                continue
            if uri.startswith(BUNDLED_PREFIX):
                name = unquote(uri[len(BUNDLED_PREFIX):])
                if Path(name).name != name or name in {"", ".", ".."} or Path(name).suffix.lower() not in {".webp", ".avif"}:
                    errors.append(f"{label}: invalid bundled WebP/AVIF filename")
                    continue
                path = asset_root / name
                if not path.is_file() or path.is_symlink():
                    errors.append(f"{label}: bundled file missing or a symlink: {name}")
                    continue
                if path.stat().st_size > 150_000:
                    errors.append(f"{label}: bundled image exceeds 150,000 bytes")
                try:
                    width, height = dimensions(path)
                    if width <= 0 or height <= 0 or width > 1200:
                        errors.append(f"{label}: bundled image must be at most 1200 px wide")
                except ValueError as exc:
                    errors.append(f"{label}: {exc}")
            else:
                p = urlparse(uri)
                if key in REQUIRED:
                    errors.append(f"{label}: Tier 1 and shared Starlink media must be bundled")
                elif p.scheme != "https" or not p.hostname or p.username or p.password:
                    errors.append(f"{label}: remote media URI must be HTTPS")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--asset-root", type=Path, default=DEFAULT_ASSETS)
    args = parser.parse_args()
    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
        errors = validate(manifest, args.asset_root)
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    included = sum(len(c["images"]) for c in manifest["spacecraft"].values())
    print(f"PASS: manifest structure and local file checks ({included} included images).")
    print("NOTE: source pages and licence claims were not independently verified by this offline check.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
