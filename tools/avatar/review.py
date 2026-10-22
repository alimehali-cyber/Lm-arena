#!/usr/bin/env python3
"""Review sheet for a character: the four turnaround views, a head close-up, and the Home-scale tile.

    python3 tools/avatar/review.py --spec tools/avatar/specs/professor_z.json --out out/avatars/z_review.png

This is the QA artifact an animator actually looks at: does the silhouette read, do the glasses and
the bow tie land where they should, is the hair a head of hair or a helmet. It renders off the same
skinned mesh the GLB is written from, so a fix here is a fix in the shipped character.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw

from character import CharacterSpec, build_character
from motion import CLIPS_BY_NAME
from preview import _camera, _frame, _project, render, scene_scale_tile, skin, bone_world_transforms
import numpy as np


def head_crop(avatar, clip, yaw_deg: float, size: int = 340) -> Image.Image:
    """A close-up of the head, framed on the head's own world position."""
    pose = clip.pose(0.0)
    world = bone_world_transforms(avatar, pose)
    head = world["head"][0:3, 3]
    full = render(avatar, clip, 0.0, width=size, height=size * 2, yaw_deg=yaw_deg)
    # The head sits at `head` height; the render frames the whole body, so crop proportionally.
    eye, right, up, forward, focal, distance = _camera(avatar, size, size * 2, yaw_deg)
    point = _project(head, eye, right, up, forward, focal, size, size * 2)
    if point is None:
        return full.crop((0, 0, size, size))
    cx, cy = point
    half = size * 0.30
    box = (int(cx - half), int(cy - half * 1.15), int(cx + half), int(cy + half * 0.85))
    crop = full.crop(box)
    return crop.resize((size, size), Image.LANCZOS)


def build_sheet(avatar, path: Path) -> Path:
    clip = CLIPS_BY_NAME["idle_breathe"]
    tiles: list[tuple[str, Image.Image]] = []
    for label, yaw in (("front", 0.0), ("3/4", 28.0), ("side", 78.0), ("back", 168.0)):
        tiles.append((label, render(avatar, clip, 0.0, width=300, height=460, yaw_deg=yaw)))
    tiles.append(("head", head_crop(avatar, clip, 14.0, size=300)))
    tiles.append(("Home scale", scene_scale_tile(avatar, CLIPS_BY_NAME["night_point"], 1.6, width=300, height=460)))

    gap = 8
    header = 26
    width = sum(t.width for _, t in tiles) + gap * (len(tiles) + 1)
    height = tiles[0][1].height + header + gap * 2
    sheet = Image.new("RGB", (width, height), (10, 14, 22))
    draw = ImageDraw.Draw(sheet)
    x = gap
    for label, tile in tiles:
        sheet.paste(tile, (x, header))
        draw.text((x + 6, 8), label, fill=(228, 238, 250))
        x += tile.width + gap
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--spec", type=Path, required=True)
    parser.add_argument("--out", type=Path, default=Path("out/avatars/review.png"))
    args = parser.parse_args(argv)
    avatar = build_character(CharacterSpec.load(args.spec))
    print(build_sheet(avatar, args.out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
