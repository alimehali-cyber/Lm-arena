#!/usr/bin/env python3
"""Build a rigged, animated avatar GLB from a reference photo — the no-GPU route.

    python3 tools/avatar/build_avatar.py --photo front.jpg --name aram \
        --out out/avatars/aram.glb --work work/aram

Produces:
  out/avatars/aram.glb                     the avatar, 19 bones, five clips, vertex colours
  out/avatars/aram.glb.provenance.json     every licence behind it, plus the stats
  work/aram/front_cut.png                  the matte, for the record and for a second look
  work/aram/aram_brief.json                the measurements the build used
  out/avatars/aram_sheet.png               a four-pose contact sheet (unless --no-preview)

Input may be a raw photograph or an already-matted PNG with alpha. If a brief exists it is used as
given, so a character can be rebuilt bit-for-bit without re-measuring the photo.

This route builds a stylised low-poly humanoid whose *proportions and palette* come from the photo. It
is not a photogrammetric reconstruction; for a textured, high-fidelity likeness use AssetFurnace on a
GPU machine (skills/image-to-3d-avatar/SKILL.md).
"""

from __future__ import annotations

import argparse
import json
from datetime import date
from pathlib import Path

import numpy as np
from PIL import Image

from avatar_core import Proportions, build_avatar, default_palette
from glb_export import write_glb
from motion import CLIPS
import photo_ingest
from preview import contact_sheet


def proportions_from(brief: dict | None, height_m: float) -> Proportions:
    if not brief:
        return Proportions(height_m=height_m)
    allowed = Proportions.__dataclass_fields__
    values = {k: float(v) for k, v in brief.get("proportions", {}).items() if k in allowed}
    values["height_m"] = height_m
    base = Proportions()
    merged = {**base.__dict__, **values}
    # Keep the A-pose angles and depth ratio from the defaults: they are style, not measurement.
    merged["arm_spread"] = base.arm_spread
    merged["forearm_spread"] = base.forearm_spread
    merged["depth_ratio"] = base.depth_ratio
    return Proportions(**merged)


def palette_from(brief: dict | None) -> dict[str, tuple[float, float, float]]:
    if not brief:
        return default_palette()
    palette = default_palette()
    for key, value in brief.get("palette", {}).items():
        if key in palette and len(value) == 3:
            palette[key] = tuple(min(1.0, max(0.0, float(v))) for v in value)
    return palette


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--photo", type=Path, help="reference photo (or matted PNG)")
    parser.add_argument("--brief", type=Path, help="reuse a brief instead of measuring")
    parser.add_argument("--name", default=None, help="asset name, defaults to the photo's stem")
    parser.add_argument("--out", type=Path, default=None)
    parser.add_argument("--work", type=Path, default=Path("work/avatar"))
    parser.add_argument("--height", type=float, default=1.72, help="character height in metres")
    parser.add_argument("--no-preview", action="store_true")
    parser.add_argument("--route", default="authored-fallback", help="recorded in the provenance sidecar")
    args = parser.parse_args(argv)

    if not args.photo and not args.brief:
        parser.error("give --photo or --brief")

    name = args.name or (args.photo.stem if args.photo else args.brief.stem)
    args.work.mkdir(parents=True, exist_ok=True)
    out = args.out or Path("out/avatars") / f"{name}.glb"

    brief_path = args.brief
    if brief_path is None:
        brief_path = args.work / f"{name}_brief.json"
        matte_path = args.work / f"{name}_cut.png"
        brief = photo_ingest.ingest(args.photo, brief_path, matte_path)
        if brief["checks"]["confidence"] == "low":
            print("  warn: low-confidence measurements — the sheet will show it; check the palette by eye")
    else:
        brief = json.loads(brief_path.read_text())

    proportions = proportions_from(brief, args.height)
    palette = palette_from(brief)
    avatar = build_avatar(proportions, palette)
    stats = write_glb(avatar, out, CLIPS)

    sidecar = {
        "schema": "zig.avatar/1",
        "asset": name,
        "created": date.today().isoformat(),
        "route": args.route,
        "engine": {"name": "ZIG authored mesh + rig", "licence": "this repository"},
        "weights": [],
        "rig": {"tool": "tools/avatar (authored)", "bones": 19, "bind": "A-pose"},
        "motions": [{"name": clip.name, "seconds": clip.length, "licence": "this repository"} for clip in CLIPS],
        "source_photos": [brief.get("source")] if brief.get("source") else [],
        "consent": brief.get("consent", "not recorded — confirm before shipping"),
        "brief": str(brief_path),
        "stats": {
            "triangles": stats["triangles"],
            "vertices": stats["vertices"],
            "bones": stats["bones"],
            "bytes": stats["bytes"],
        },
    }
    sidecar_path = out.with_name(out.name + ".provenance.json")
    sidecar_path.write_text(json.dumps(sidecar, indent=2) + "\n")

    print(f"avatar  {out}  ({stats['bytes'] / 1024:.1f} KB, {stats['triangles']} triangles)")
    print(f"sidecar {sidecar_path}")
    if not args.no_preview:
        sheet = contact_sheet(avatar, out.with_name(f"{name}_sheet.png"))
        print(f"sheet   {sheet}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
