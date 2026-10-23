#!/usr/bin/env python3
"""
Render Professor Z's head at exactly the sheet's head framing.

Judging a face from a 797 px full-body sprite is guessing. This renders 266 x 200 px of
head at the sheet's own 468.82 px/m, so the result can be pasted next to the sheet crop
and compared feature by feature.

  .venv/bin/python tools/render_head_ab.py --blend working/profz/character.blend \
      --out renders/profz/head/head_ab.png
"""
import argparse, math, pathlib, sys
import bpy
from mathutils import Vector

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent / "blender"))
import render_review as RR       # noqa: E402
from scene_utils import reset_pose  # noqa: E402

PX = 468.82          # px per metre, the sheet's scale
W, H = 266, 200      # the sheet's head crop: x 183..449, y 168..368
Z_BOTTOM = 1.274     # bottom edge of that crop


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--z-bottom", type=float, default=Z_BOTTOM)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    RR.enable_cycles()
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.samples = a.samples
    sc.cycles.device = "CPU"
    sc.cycles.use_denoising = False
    sc.render.resolution_x, sc.render.resolution_y = W, H
    sc.render.film_transparent = True
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGBA"
    sc.view_settings.view_transform = "Standard"

    RR.setup_world(strength=0.45, colour=(0.42, 0.44, 0.47))
    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    reset_pose(arm)
    mid = a.z_bottom + (H / 2) / PX
    RR.key_light(mid)

    cd = bpy.data.cameras.new("OrthoHead")
    cd.type = "ORTHO"
    cd.ortho_scale = W / PX
    cam = bpy.data.objects.new("OrthoHead", cd)
    bpy.context.collection.objects.link(cam)
    sc.camera = cam
    cam.location = Vector((0.0, -4.0, mid))
    cam.rotation_euler = (math.radians(90), 0.0, 0.0)

    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    sc.render.filepath = str(out)
    bpy.ops.render.render(write_still=True)
    print({"head": str(out), "px_per_m": PX, "z": [a.z_bottom, a.z_bottom + H / PX]})


if __name__ == "__main__":
    main()
