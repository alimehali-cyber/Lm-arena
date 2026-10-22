#!/usr/bin/env python3
"""Turn a reference photo into a measurable brief: matte, proportions, palette, quality checks.

    python3 tools/avatar/photo_ingest.py front.jpg --out work/front_brief.json --matte work/front_cut.png

What it does, in order:

1. Auto-orient (EXIF), flatten alpha, and downscale anything larger than 1600 px on the long edge.
2. Matte by flooding from the frame border with a tolerance, then a small open/close so the silhouette
   loses specks and keeps its fingers. A busy background fails loudly rather than silently becoming
   part of the character.
3. Measure the subject: box, head, shoulders, waist, hips, and a handful of quality checks.
4. Sample a five-colour palette from the actual pixels (median of small patches, masked).
5. Write a JSON brief — the build reads the brief, so a character is reproducible from it alone.

Everything here is a guess about a photograph, so it reports its own confidence and the build clamps
it. Nothing about it requires a GPU.
"""

from __future__ import annotations

import argparse
import json
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageOps

SENTINEL = (255, 0, 255)
MAX_EDGE = 1600
DEFAULT_HEIGHT_M = 1.72


def load(path: Path) -> Image.Image:
    image = Image.open(path)
    image = ImageOps.exif_transpose(image)
    if image.mode in ("RGBA", "LA", "P"):
        image = image.convert("RGBA")
        canvas = Image.new("RGBA", image.size, (255, 255, 255, 255))
        canvas.alpha_composite(image)
        image = canvas
    image = image.convert("RGB")
    if max(image.size) > MAX_EDGE:
        scale = MAX_EDGE / max(image.size)
        image = image.resize((int(image.width * scale), int(image.height * scale)), Image.LANCZOS)
    return image


def matte(image: Image.Image, tolerance: int = 34) -> Image.Image:
    """'L' mask, 255 on the subject. Flood the border; whatever the flood never reaches is the subject."""
    marked = image.copy()
    draw = ImageDraw.Draw(marked)
    w, h = image.size
    seeds = [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (w // 2, 0), (w // 2, h - 1), (0, h // 2), (w - 1, h // 2)]
    for seed in seeds:
        try:
            ImageDraw.floodfill(marked, seed, SENTINEL, thresh=tolerance)
        except Exception:
            continue
    array = np.asarray(marked)
    background = np.all(np.abs(array.astype(int) - np.array(SENTINEL)) <= 2, axis=2)
    mask = Image.fromarray(np.where(background, 0, 255).astype(np.uint8), mode="L")
    # Open then close: drop strays in the background, keep the silhouette continuous.
    mask = mask.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.MaxFilter(3))
    mask = mask.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
    return mask


def border_background_ratio(mask: Image.Image) -> float:
    array = np.asarray(mask)
    border = np.concatenate([array[0, :], array[-1, :], array[:, 0], array[:, -1]])
    return float((border == 0).mean())


def subject_box(mask: Image.Image) -> tuple[int, int, int, int]:
    array = np.asarray(mask) > 127
    rows = np.flatnonzero(array.any(axis=1))
    cols = np.flatnonzero(array.any(axis=0))
    if rows.size == 0 or cols.size == 0:
        raise ValueError("the matte is empty: no subject found (is the background the same as the subject?)")
    return int(cols[0]), int(rows[0]), int(cols[-1] + 1), int(rows[-1] + 1)


def row_runs(mask: Image.Image, row: int) -> list[tuple[int, int]]:
    """Connected runs of subject pixels in one row, as (start, end) pixel pairs."""
    line = (np.asarray(mask)[row] > 127).astype(np.int8)
    edges = np.flatnonzero(np.diff(np.concatenate(([0], line, [0]))))
    return [(int(edges[i]), int(edges[i + 1])) for i in range(0, len(edges), 2)]


def body_width(mask: Image.Image, row: int) -> int:
    """Width of the body in a row, ignoring detached arms.

    With arms held away there are three runs (arm, torso, arm) and the middle one is the body. With
    arms against the torso there is one run and the measurement includes them — which is why the
    intake rules ask for an A-pose with a clear gap.
    """
    runs = row_runs(mask, row)
    if not runs:
        return 0
    if len(runs) == 1:
        return runs[0][1] - runs[0][0]
    if len(runs) == 2:
        return max(r[1] - r[0] for r in runs)
    middle = runs[len(runs) // 2]
    return middle[1] - middle[0]


def measure(mask: Image.Image, box: tuple[int, int, int, int]) -> dict:
    left, top, right, bottom = box
    height = max(bottom - top, 1)
    rows = list(range(top, bottom))
    widths = np.array([body_width(mask, r) for r in rows], dtype=np.float64)

    def index_at(fraction_from_top: float) -> int:
        return int(min(height - 1, max(0, round(height * fraction_from_top))))

    def width_at(fraction_from_top: float) -> float:
        i = index_at(fraction_from_top)
        lo, hi = max(0, i - 2), min(height, i + 3)
        return float(np.median(widths[lo:hi]))

    # Neck: the narrowest row under the head — hair and shoulders are both wider.
    lo, hi = index_at(0.11), index_at(0.21)
    neck_index = lo + int(np.argmin(widths[lo:hi])) if hi > lo else index_at(0.15)
    head_half = float(widths[: max(neck_index, 2)].max()) / 2.0 / height

    # Shoulders sit a fixed distance below the neck on a standing figure; measuring there avoids the
    # arms, which join the silhouette just under the shoulder line in an A-pose.
    neck_fraction = neck_index / height
    shoulder_fraction = min(0.30, max(0.18, neck_fraction + 0.055))
    shoulder_index = index_at(shoulder_fraction)
    shoulder_half = width_at(shoulder_fraction) / 2.0 / height

    # Crotch: once the silhouette stays split into two legs, the hips are just above it.
    crotch_index = index_at(0.55)
    two_run_streak = 0
    for i in range(index_at(0.42), index_at(0.72)):
        two_run_streak = two_run_streak + 1 if len(row_runs(mask, top + i)) == 2 else 0
        if two_run_streak >= 8:
            crotch_index = i - 7
            break

    hip_half = float(widths[max(0, crotch_index - 6)]) / 2.0 / height if crotch_index > 6 else 0.062
    waist_half = width_at(0.43) / 2.0 / height
    chest_half = width_at(0.33) / 2.0 / height

    def above(fraction_from_top: float) -> float:
        return round(1.0 - fraction_from_top, 4)

    proportions = {
        "height_m": DEFAULT_HEIGHT_M,
        "head_centre": round(1.0 - (neck_index * 0.55 + index_at(0.03) * 0.45) / height, 4),
        "neck": round(1.0 - neck_index / height, 4),
        "shoulder": round(1.0 - shoulder_index / height, 4),
        "chest": round(1.0 - (shoulder_fraction + 0.06), 4),
        "waist": above(0.44),
        "hip": round(1.0 - crotch_index / height + 0.02, 4),
        "knee": above(0.735),
        "ankle": above(0.955),
        "head_half": round(head_half, 4),
        "shoulder_half": round(shoulder_half, 4),
        "chest_half": round(chest_half, 4),
        "waist_half": round(waist_half, 4),
        "hip_half": round(hip_half, 4),
    }
    proportions = _sane(proportions)

    separated_rows = sum(
        1 for f in (0.33, 0.36, 0.39, 0.42, 0.45)
        if len(row_runs(mask, top + index_at(f))) >= 3
    )
    checks = {
        "subject_height_fraction": round(height / mask.height, 3),
        "background_removed_fraction": round(1.0 - float((np.asarray(mask)[top:bottom, left:right] > 127).mean()), 3),
        "border_is_background": border_background_ratio(mask) > 0.5,
        "arms_clear_of_body": separated_rows >= 1,
        "separated_rows": separated_rows,
        "legs_separate_below_hips": len(row_runs(mask, top + index_at(0.80))) >= 2,
        "feet_inside_frame": bottom < mask.height - 1,
        "head_to_height_ratio": round(neck_index / height, 3),
        "confidence": "high" if border_background_ratio(mask) > 0.5 and height / mask.height > 0.5 else "low",
    }
    return {"proportions": proportions, "checks": checks, "box": [left, top, right, bottom]}


def _sane(p: dict) -> dict:
    """Clamp measurements into the range a human silhouette can plausibly occupy."""
    limits = {
        "neck": (0.72, 0.90), "shoulder": (0.68, 0.86), "chest": (0.60, 0.78), "waist": (0.52, 0.68),
        "hip": (0.44, 0.60), "knee": (0.22, 0.36), "ankle": (0.02, 0.10), "head_centre": (0.85, 0.96),
        "head_half": (0.032, 0.062), "shoulder_half": (0.070, 0.150), "chest_half": (0.055, 0.130),
        "waist_half": (0.040, 0.110), "hip_half": (0.045, 0.115),
    }
    out = dict(p)
    for key, (lo, hi) in limits.items():
        if key in out:
            out[key] = round(min(hi, max(lo, float(out[key]))), 4)
    return out


def region_colour(rgb: np.ndarray, mask: np.ndarray, cx: float, cy: float, radius: int = 7) -> tuple[int, int, int] | None:
    height, width = mask.shape
    x, y = int(cx * width), int(cy * height)
    patch = rgb[max(0, y - radius): y + radius + 1, max(0, x - radius): x + radius + 1]
    patch_mask = mask[max(0, y - radius): y + radius + 1, max(0, x - radius): x + radius + 1]
    if patch.size == 0:
        return None
    pixels = patch[patch_mask]
    if pixels.shape[0] < 12:
        return None
    median = np.median(pixels, axis=0)
    return tuple(int(v) for v in median)


def palette(image: Image.Image, mask: Image.Image, box: tuple[int, int, int, int]) -> dict:
    left, top, right, bottom = box
    crop = image.crop(box)
    crop_mask = mask.crop(box)
    rgb = np.asarray(crop).astype(np.float64)
    alpha = np.asarray(crop_mask) > 127
    probes = {
        "hair": (0.50, 0.035),
        "skin": (0.50, 0.105),
        "top": (0.50, 0.34),
        "trousers": (0.50, 0.70),
        "shoes": (0.50, 0.965),
    }
    found: dict[str, tuple[int, int, int]] = {}
    for name, (cx, cy) in probes.items():
        colour = region_colour(rgb, alpha, cx, cy)
        if colour is not None:
            found[name] = colour
    defaults = {"skin": (217, 168, 133), "hair": (51, 38, 33), "top": (61, 84, 122), "trousers": (51, 56, 71), "shoes": (28, 31, 38)}
    return {name: found.get(name, defaults[name]) for name in defaults}


def to_unit(colour: tuple[int, int, int]) -> tuple[float, float, float]:
    return tuple(round(c / 255.0, 4) for c in colour)


def ingest(path: Path, out: Path, matte_path: Path | None, tolerance: int = 34) -> dict:
    image = load(path)
    mask = matte(image, tolerance)
    box = subject_box(mask)
    measured = measure(mask, box)
    colours = palette(image, mask, box)

    # Write the cutout and a head crop for the build and for the record.
    if matte_path is not None:
        rgba = image.convert("RGBA")
        rgba.putalpha(mask)
        matte_path.parent.mkdir(parents=True, exist_ok=True)
        rgba.save(matte_path)
    head = image.crop(box)
    head_height = int(head.height * 0.22)
    head_crop = head.crop((0, 0, head.width, max(head_height, 16)))

    brief = {
        "schema": "zig.avatar.brief/1",
        "source": str(path),
        "source_size": [image.width, image.height],
        "matte": str(matte_path) if matte_path else None,
        "subject_box": measured["box"],
        "proportions": measured["proportions"],
        "palette_rgb": colours,
        "palette": {k: list(to_unit(v)) for k, v in colours.items()},
        "checks": measured["checks"],
        "notes": [
            "Proportions are measured from the silhouette and clamped; unusual poses will be clamped too.",
            "The palette is a median sample of five regions; check it against the photograph before shipping.",
        ],
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(brief, indent=2) + "\n")
    if matte_path is not None:
        head_crop_path = matte_path.with_name(matte_path.stem + "_head.png")
        head_crop.save(head_crop_path)
        brief["head_crop"] = str(head_crop_path)
        out.write_text(json.dumps(brief, indent=2) + "\n")
    return brief


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("photo", type=Path)
    parser.add_argument("--out", type=Path, default=Path("work/brief.json"))
    parser.add_argument("--matte", type=Path, default=None, help="write the cut-out PNG here")
    parser.add_argument("--tolerance", type=int, default=34, help="flood-fill tolerance, 0-120")
    args = parser.parse_args(argv)

    brief = ingest(args.photo, args.out, args.matte, args.tolerance)
    checks = brief["checks"]
    print(f"brief   {args.out}")
    if brief.get("matte"):
        print(f"matte   {brief['matte']}")
    print(f"subject {brief['subject_box'][2] - brief['subject_box'][0]} x {brief['subject_box'][3] - brief['subject_box'][1]} px"
          f"  ({checks['subject_height_fraction'] * 100:.0f} % of the frame height)")
    print("palette " + "  ".join(f"{k}:{v}" for k, v in brief["palette_rgb"].items()))
    warnings = []
    if not checks["border_is_background"]:
        warnings.append("the frame border is not background: the matte is unreliable")
    if checks["subject_height_fraction"] < 0.55:
        warnings.append("subject is small in frame; ask for a tighter shot")
    if not checks["arms_clear_of_body"]:
        warnings.append("no clear gap between arms and body: ask for an A-pose")
    if not checks["feet_inside_frame"]:
        warnings.append("feet are cut off at the bottom edge")
    for warning in warnings:
        print(f"  warn: {warning}")
    print(f"confidence {checks['confidence']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
