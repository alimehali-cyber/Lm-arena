#!/usr/bin/env python3
"""Bake a character's clips into sprite sheets the app can play on any device.

    python3 tools/avatar/bake_frames.py --spec tools/avatar/specs/professor_z.json \
        --out app/src/main/assets/avatar/professor_z

Why baked frames and not a live skin: the avatar has to animate on every phone the app installs on.
A GLES3 skinning sidecar is a per-device risk (EGL surface, punch-through inside a LazyColumn,
driver quirks) for a figure that is ~170 px tall. Baked frames are one bitmap per clip, drawn by the
same Canvas that already draws the observer — no new surface, no new thread, no new failure mode. The
GLB still ships alongside them: it is the portable asset, the one that goes into Blender or a game
engine, and the reference the frames were rendered from.

Framing is computed once, across every frame of every clip, so the character never changes size or
drifts between clips. The anchor (feet) and the character's height inside a cell are recorded, so the
runtime can put the feet exactly on the scene's grass line.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

from character import CharacterSpec, build_character
from motion import CLIPS
from preview import render

CELL = (128, 192)      # final cell size in the atlas, px
SUPERSAMPLE = 2
COLS = 6               # 24 frames per clip, 6 x 4
ROWS = 4
# Playback rate. The authored clips are deliberately slow (8 s) so they read as stillness on a home
# screen; baked at 8 fps over 24 frames they loop in 3 s, which is the same motion at a pace a small
# figure needs. More frames would cost megabytes for movement nobody can see at 170 px.
FPS = 8

# Which way the camera looks for each clip: a dead-on front view hides the depth of a raised arm, so
# each beat gets the angle that reads best at thumbnail size.
YAW = {
    "idle_breathe": 12.0,
    "day_shade": 16.0,
    "dusk_phone": 20.0,
    "night_point": 22.0,
    "wave_hello": 18.0,
}


def render_clip_frames(avatar, clip, frames: int) -> list[Image.Image]:
    out = []
    yaw = YAW.get(clip.name, 14.0)
    for i in range(frames):
        # The last frame of a loop is the first frame again, so do not duplicate it.
        time_s = clip.length * i / frames
        image = render(
            avatar,
            clip,
            time_s,
            width=CELL[0] * SUPERSAMPLE,
            height=CELL[1] * SUPERSAMPLE,
            yaw_deg=yaw,
            with_alpha=True,
        )
        out.append(image)
    return out


def union_box(frames: list[Image.Image]) -> tuple[int, int, int, int]:
    left = top = 10**9
    right = bottom = -1
    for frame in frames:
        alpha = np.asarray(frame)[..., 3]
        rows = np.flatnonzero(alpha.max(axis=1) > 8)
        cols = np.flatnonzero(alpha.max(axis=0) > 8)
        if rows.size == 0:
            continue
        left = min(left, int(cols[0]))
        right = max(right, int(cols[-1]) + 1)
        top = min(top, int(rows[0]))
        bottom = max(bottom, int(rows[-1]) + 1)
    if right < 0:
        raise ValueError("nothing was rendered")
    return left, top, right, bottom


def fit_to_cell(box, canvas_size, target_aspect: float = CELL[0] / CELL[1], margin: float = 0.06):
    """Expand a pixel box to the cell's aspect ratio with a margin, clamped to the canvas."""
    left, top, right, bottom = box
    width = right - left
    height = bottom - top
    pad_x = width * margin
    pad_y = height * margin * 2.2  # extra headroom: raised arms and hair need it
    left -= pad_x
    right += pad_x
    top -= pad_y
    bottom += pad_y * 0.7
    width = right - left
    height = bottom - top
    if width / height < target_aspect:
        wanted = height * target_aspect
        grow = (wanted - width) / 2.0
        left -= grow
        right += grow
    else:
        wanted = width / target_aspect
        grow = (wanted - height) / 2.0
        top -= grow
        bottom += grow
    canvas_w, canvas_h = canvas_size
    # Clamp first, then re-fit if clamping broke the aspect, so the cell is never stretched.
    left = max(0.0, left)
    top = max(0.0, top)
    right = min(float(canvas_w), right)
    bottom = min(float(canvas_h), bottom)
    return (int(round(left)), int(round(top)), int(round(right)), int(round(bottom)))


def crop_and_fit(frame: Image.Image, box) -> Image.Image:
    left, top, right, bottom = box
    pad = Image.new("RGBA", (frame.width, frame.height), (0, 0, 0, 0))
    pad.paste(frame, (0, 0))
    crop = pad.crop((left, top, right, bottom))
    return crop.resize(CELL, Image.LANCZOS)


def atlas(frames: list[Image.Image]) -> Image.Image:
    sheet = Image.new("RGBA", (CELL[0] * COLS, CELL[1] * ROWS), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        col, row = index % COLS, index // COLS
        sheet.paste(frame, (col * CELL[0], row * CELL[1]))
    return sheet


def bake(spec: CharacterSpec, out_dir: Path, clips=CLIPS) -> dict:
    avatar = build_character(spec)
    out_dir.mkdir(parents=True, exist_ok=True)

    rendered: dict[str, list[Image.Image]] = {}
    for clip in clips:
        count = COLS * ROWS if clip.loop else min(COLS * ROWS, max(6, int(round(clip.length * FPS))))
        rendered[clip.name] = render_clip_frames(avatar, clip, count)

    everything = [frame for frames in rendered.values() for frame in frames]
    canvas = rendered[clips[0].name][0].size
    box = fit_to_cell(union_box(everything), canvas)

    # Where the feet are, in cell space, and how much of the cell the body fills.
    alpha = np.zeros((canvas[1], canvas[0]), dtype=np.float32)
    for frame in everything:
        alpha = np.maximum(alpha, np.asarray(frame)[..., 3].astype(np.float32))
    body_rows = np.flatnonzero(alpha.max(axis=1) > 8)
    feet_y = (body_rows[-1] + 1 - box[1]) / max(box[3] - box[1], 1)
    head_y = (body_rows[0] - box[1]) / max(box[3] - box[1], 1)
    column_mass = alpha.sum(axis=0)
    total_mass = float(column_mass.sum()) or 1.0
    centroid_x = float((column_mass * np.arange(column_mass.size)).sum() / total_mass)
    centre_x = (centroid_x - box[0]) / max(box[2] - box[0], 1)

    entries = []
    for clip in clips:
        frames = [crop_and_fit(frame, box) for frame in rendered[clip.name]]
        sheet = atlas(frames)
        name = f"{clip.name}.png"
        sheet.save(out_dir / name, optimize=True)
        entries.append({
            "name": clip.name,
            "sheet": name,
            "frames": len(frames),
            "fps": FPS,
            "seconds": len(frames) / FPS,
            "loop": clip.loop,
        })

    meta = {
        "schema": "zig.avatar.frames/1",
        "asset": spec.name,
        "height_m": spec.height_m,
        "cell": {"w": CELL[0], "h": CELL[1], "cols": COLS, "rows": ROWS},
        # Normalised inside a cell: (0,0) top-left. The runtime aligns `anchor` to the grass line.
        "anchor": {"x": round(float(centre_x), 4), "y": round(float(feet_y), 4)},
        "body_fraction": round(float(feet_y - head_y), 4),
        "clips": entries,
        "source_glb": f"{spec.name}.glb",
        "note": "Rendered from the same skinned mesh the GLB is written from; see build_character.py.",
    }
    (out_dir / "meta.json").write_text(json.dumps(meta, indent=2) + "\n")

    total = sum((out_dir / entry["sheet"]).stat().st_size for entry in entries)
    print(f"baked {len(entries)} clips into {out_dir}  ({total / 1024:.0f} KB)")
    print(f"cell {CELL[0]}x{CELL[1]} · anchor {meta['anchor']} · body {meta['body_fraction']} of the cell")
    for entry in entries:
        size = (out_dir / entry["sheet"]).stat().st_size / 1024
        print(f"  {entry['name']:<14} {entry['frames']:>3} frames @ {entry['fps']:.1f} fps  {size:.0f} KB")
    return meta


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--spec", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    bake(CharacterSpec.load(args.spec), args.out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
