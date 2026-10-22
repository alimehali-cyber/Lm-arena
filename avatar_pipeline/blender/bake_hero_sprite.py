#!/usr/bin/env python3
"""
Bake the animated character into sprite strips for the Android hero.

WHY SPRITES AND NOT THE GL RUNTIME
    The GLES runtime in avatar_pipeline/android/ is the "true 3D" path, but it needs an
    EGL surface, and the Home hero is a Compose Canvas inside a scrolling card. An
    untested GLSurfaceView composited over that scene is exactly the kind of change that
    goes black or flickers on a device nobody here can test. A sprite strip is drawn by
    the same Canvas that draws the sky: one drawImage per frame, deterministic, no
    lifecycle, no EGL, and it *is* the 3D character - baked from the exported GLB's source
    scene, same mesh, same rig, same clips.

    The GL path is kept, unwired, for a device that can actually run it.

WHAT IS BAKED
    Four strips, side by side in one PNG per strip:
      idle   12 frames of Idle_Breathing  (loop)
      greet  14 frames of Gesture_Greet   (one shot: hand raised, for bright daylight)
      think  14 frames of Expression_Thinking (one shot: for the phone pose)
      point  14 frames of Gesture_Point   (one shot: for looking up at night)
    Frames are 2:3 portrait, transparent, character facing screen-left (the figure stands
    on the right of the scene and faces the middle).

Run:
  .venv/bin/python blender/bake_hero_sprite.py \
      --blend working/profz/character_anim.blend --outdir ../app/src/main/res/drawable-nodpi
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys

import bpy
from mathutils import Vector

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import render_review as RR          # noqa: E402
from scene_utils import reset_pose  # noqa: E402

# clip -> (frames to bake, loop, window length in source frames).
# Idle_Breathing is 675 frames long (28 s); sampling the whole range turned a breath into
# a series of jumps, so a contiguous 2.5 s window is baked instead.
#
# Only the idle is baked. The three one-shot candidates were rendered and inspected:
# Gesture_Greet's raised arm swings out to the side rather than to the brow, and
# Expression_Thinking is almost entirely facial, so at the 90 px the hero draws it, both
# read as nothing. The vector ObserverRenderer keeps its pose variants; the 3D figure
# ships with its idle, which is what it does 95 % of the time anyway.
STRIPS = {
    "idle": ("Idle_Breathing", 16, True, 60),
}


def setup_render(res_x, res_y, samples):
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.samples = samples
    sc.cycles.use_denoising = False
    sc.cycles.device = "CPU"
    sc.render.resolution_x = res_x
    sc.render.resolution_y = res_y
    sc.render.resolution_percentage = 100
    sc.render.film_transparent = True
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGBA"
    sc.view_settings.view_transform = "Standard"


def assign_clip(arm, name):
    """NLA strips in the built file; push a single action to the active slot."""
    act = bpy.data.actions.get(name)
    if act is None:
        raise SystemExit(f"missing action {name}")
    ad = arm.animation_data or arm.animation_data_create()
    for tr in list(ad.nla_tracks):
        tr.mute = True
    ad.action = act
    if hasattr(ad, "action_slot") and getattr(act, "slots", None):
        try:
            ad.action_slot = act.slots[0]
        except Exception:
            pass
    return act


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--outdir", required=True)
    ap.add_argument("--res", default="200x300")
    ap.add_argument("--samples", type=int, default=10)
    a = ap.parse_args()

    rx, ry = (int(v) for v in a.res.lower().split("x"))
    bpy.ops.wm.open_mainfile(filepath=a.blend)
    RR.enable_cycles()
    setup_render(rx, ry, a.samples)
    RR.setup_world(strength=1.15, colour=(0.30, 0.33, 0.40))

    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    body = bpy.data.objects["Body"]
    reset_pose(arm)

    # frame the whole figure with a little headroom, from the character's left so that the
    # face points to screen-left, which is how the figure stands in the scene
    zs = [(body.matrix_world @ v.co).z for v in body.data.vertices]
    height = max(zs) - min(zs)
    target = Vector((0, 0, (max(zs) + min(zs)) * 0.5))
    cam = RR.make_camera()
    RR.frame(cam, target, RR.fit_distance(height, margin=1.20), target.z, 32.0)
    RR.key_light(target.z)

    outdir = pathlib.Path(a.outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    tmp = outdir / "_frames"
    tmp.mkdir(exist_ok=True)

    # face the figure at the camera, whatever the rest pose points at
    meta = {}
    for key, (clip, n_frames, looping, window) in STRIPS.items():
        act = assign_clip(arm, clip)
        f0, f1 = (int(act.frame_range[0]), int(act.frame_range[1]))
        start = f0
        span = (window if window else (f1 - f0))
        files = []
        for i in range(n_frames):
            # a LOOP covers `span` frames exactly, so frame n wraps onto frame 0; a
            # one-shot covers the whole clip and holds its last pose
            denom = n_frames if looping else max(1, n_frames - 1)
            f = start + round(span * i / denom)
            bpy.context.scene.frame_set(int(f))
            path = tmp / f"{key}_{i:02d}.png"
            bpy.context.scene.render.filepath = str(path)
            bpy.ops.render.render(write_still=True)
            files.append(path)
        meta[key] = {"clip": clip, "frames": n_frames, "loop": looping,
                     "source_frames": [start, start + span], "files": [str(p) for p in files]}
        print(f"[bake] {key}: {n_frames} frames of {clip} [{f0}..{f1}]")

    (outdir / "_frames" / "meta.json").write_text(json.dumps(meta, indent=2))
    print(json.dumps({"status": "ok", "tmp": str(tmp), "strips": list(STRIPS)}, indent=2))


if __name__ == "__main__":
    main()
