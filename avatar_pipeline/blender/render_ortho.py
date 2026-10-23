#!/usr/bin/env python3
"""
Orthographic front/side render at the sheet's own framing, for direct comparison.

The review renders use a perspective camera and a fitted distance, which is fine for
"does it look like a person" and useless for "is the hem 4 mm too low". This renders
with an orthographic camera, 1 px per 0.002133 m, framed exactly like the sheet crop,
so the two images can be subtracted.

Run:
  .venv/bin/python blender/render_ortho.py --blend working/profz/character_anim.blend \
      --outdir renders/profz/ortho --views front,side,back --samples 24
"""
from __future__ import annotations

import argparse
import math
import pathlib
import sys

import bpy
from mathutils import Vector

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import render_review as RR       # noqa: E402
from scene_utils import reset_pose  # noqa: E402

# The sheet: 797 px from crown to sole, 1.70 m at the current scale.
PIXELS_PER_METRE = 797.0 / 1.70
SHEET_W, SHEET_H = 266, 797


def setup(samples, width, height):
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.samples = samples
    sc.cycles.device = "CPU"
    sc.cycles.use_denoising = False
    sc.render.resolution_x = width
    sc.render.resolution_y = height
    sc.render.resolution_percentage = 100
    sc.render.film_transparent = True
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGBA"
    sc.view_settings.view_transform = "Standard"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--outdir", required=True)
    ap.add_argument("--views", default="front,side,back")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--res", type=int, default=797)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    RR.enable_cycles()
    height = a.res
    width = int(round(height * SHEET_W / SHEET_H))
    setup(a.samples, width, height)
    RR.setup_world(strength=0.45, colour=(0.42, 0.44, 0.47))

    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    body = bpy.data.objects["Body"]
    reset_pose(arm)

    cd = bpy.data.cameras.new("Ortho")
    cd.type = "ORTHO"
    cd.ortho_scale = height / PIXELS_PER_METRE        # vertical extent in metres
    cam = bpy.data.objects.new("Ortho", cd)
    bpy.context.collection.objects.link(cam)
    bpy.context.scene.camera = cam

    zs = [(body.matrix_world @ v.co).z for v in body.data.vertices]
    lo, hi = min(zs), max(zs)
    mid = (lo + hi) / 2
    RR.key_light(mid)

    out = pathlib.Path(a.outdir)
    out.mkdir(parents=True, exist_ok=True)
    angles = {"front": 0.0, "side": 90.0, "back": 180.0, "q34": 35.0}
    made = {}
    for name in a.views.split(","):
        ang = math.radians(angles[name])
        dist = 4.0
        cam.location = Vector((math.sin(ang) * dist, -math.cos(ang) * dist, mid))
        d = Vector((0, 0, mid)) - cam.location
        cam.rotation_euler = d.to_track_quat("-Z", "Y").to_euler()
        path = out / f"ortho_{name}.png"
        bpy.context.scene.render.filepath = str(path)
        bpy.ops.render.render(write_still=True)
        made[name] = str(path)
        print(f"[ortho] {name} -> {path}")
    print({"views": made, "width": width, "height": height,
           "px_per_m": round(PIXELS_PER_METRE, 2)})


if __name__ == "__main__":
    main()
