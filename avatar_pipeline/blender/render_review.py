#!/usr/bin/env python3
"""
Stage 7 - deterministic review renders. Cycles on the CPU; there is no GPU here.

The camera rig is fixed and documented so two runs are comparable. Two framing bugs
worth remembering, both fixed here:
  * a fixed camera distance cropped a 1.75 m body (a 70 mm lens at 2.4 m sees 1.23 m);
    the distance is now solved from the actual bounds;
  * Blender's AUTO sensor fit uses the LARGER render dimension, so a portrait render
    needs sensor_height - mixing a 36 mm width with the 24 mm default height framed
    everything ~35% too tight.

Run: .venv/bin/python blender/render_review.py --blend working/x/character_anim.blend \
        --outdir renders/x --shots body_front,portrait_front --samples 24 --res 420
"""
from __future__ import annotations
import argparse, json, math, pathlib, sys
import bpy
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from scene_utils import reset_pose
from mathutils import Vector


def enable_cycles():
    try:
        bpy.ops.preferences.addon_enable(module="cycles")
    except Exception:
        pass


def setup_world(strength=0.9, colour=(0.20, 0.23, 0.29)):
    w = bpy.data.worlds.get("World") or bpy.data.worlds.new("World")
    bpy.context.scene.world = w
    w.use_nodes = True
    bg = w.node_tree.nodes.get("Background")
    if bg:
        bg.inputs[0].default_value = (*colour, 1.0)
        bg.inputs[1].default_value = strength


def key_light(target_z):
    lights = []
    for name, loc, energy, size, col in [
        # 900 W clipped every skin highlight to white and the face read as a blank
        # mask. These values keep the sRGB albedo visible on the mid-tones.
        ("Key", (2.2, -3.4, target_z * 1.62), 300, 2.2, (1.0, 0.96, 0.90)),
        ("Fill", (-3.0, -2.2, target_z * 1.05), 45, 4.0, (0.82, 0.88, 1.0)),
        ("Rim", (-1.6, 3.4, target_z * 1.75), 130, 2.5, (0.80, 0.88, 1.0)),
    ]:
        d = bpy.data.lights.new(name, "AREA")
        d.energy = energy
        d.size = size
        d.color = col
        o = bpy.data.objects.new(name, d)
        bpy.context.collection.objects.link(o)
        o.location = loc
        direction = Vector((0, 0, target_z * 0.6)) - o.location
        o.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
        lights.append(o)
    return lights


def make_camera():
    cd = bpy.data.cameras.new("Cam")
    cd.lens = 70
    cd.sensor_fit = "VERTICAL"
    # VERTICAL fit reads sensor_HEIGHT (default 24 mm); keep both at 36 mm so the
    # framing maths below matches the lens.
    cd.sensor_width = 36.0
    cd.sensor_height = 36.0
    o = bpy.data.objects.new("Cam", cd)
    bpy.context.collection.objects.link(o)
    bpy.context.scene.camera = o
    return o


def frame(cam, target, distance, height, angle_deg):
    a = math.radians(angle_deg)
    cam.location = Vector((math.sin(a) * distance, -math.cos(a) * distance, height))
    d = target - cam.location
    cam.rotation_euler = d.to_track_quat("-Z", "Y").to_euler()


def fit_distance(subject_height, lens=70.0, sensor=36.0, aspect=1.0, margin=1.10):
    """
    Distance at which `subject_height` metres fills the frame vertically.

    The camera is set to sensor_fit="VERTICAL", which pins the vertical field of view to
    sensor_height / lens no matter what the render aspect is. An earlier version also
    divided by the aspect here, which widened the vertical FOV that the camera does not
    actually have and cropped the head and feet off every body shot.
    """
    half_v = math.atan((sensor / 2) / lens)
    return (subject_height * margin * 0.5) / math.tan(half_v)


SHOTS = {
    "portrait_front": dict(angle=0, part="head"),
    "portrait_34": dict(angle=35, part="head"),
    "portrait_side": dict(angle=90, part="head"),
    "body_front": dict(angle=0, part="body"),
    "body_side": dict(angle=90, part="body"),
    "body_rear": dict(angle=180, part="body"),
    "body_34": dict(angle=40, part="body"),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--outdir", required=True)
    ap.add_argument("--shots", default=",".join(SHOTS))
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--res", type=int, default=420)
    ap.add_argument("--report", default=None)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    enable_cycles()
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.device = "CPU"
    sc.cycles.samples = a.samples
    sc.cycles.use_denoising = False
    sc.render.resolution_x = a.res
    sc.render.resolution_y = int(a.res * 1.3)
    sc.render.film_transparent = False
    sc.render.image_settings.file_format = "PNG"
    setup_world()

    zs = []
    for o in bpy.data.objects:
        if o.type == "MESH":
            for c in o.bound_box:
                zs.append((o.matrix_world @ Vector(c)).z)
    zmin, zmax = (min(zs), max(zs)) if zs else (0.0, 1.75)

    arm = next((o for o in bpy.data.objects if o.type == "ARMATURE"), None)
    reset_pose(arm)
    head_z = ((arm.matrix_world @ arm.data.bones["head"].head_local).z
              if arm and "head" in arm.data.bones else zmax - 0.18)
    print(f"[bounds] z {zmin:.2f}..{zmax:.2f}  head_z {head_z:.2f}")

    key_light(zmax)
    cam = make_camera()
    aspect = sc.render.resolution_x / sc.render.resolution_y
    outdir = pathlib.Path(a.outdir)
    outdir.mkdir(parents=True, exist_ok=True)

    shots = []
    for name in a.shots.split(","):
        s = SHOTS[name]
        if s["part"] == "head":
            # frame the whole head including the crown: head_z is the neck joint, not
            # the top, so a 0.34 m window centred on it cut the top of the skull
            centre = head_z + 0.10
            subject_h = min(0.46, (zmax - zmin) * 0.45)
        else:
            centre = (zmin + zmax) / 2
            subject_h = zmax - zmin
        dist = fit_distance(subject_h, lens=cam.data.lens, aspect=aspect)
        frame(cam, Vector((0, 0, centre)), dist,
              centre + (0.02 if s["part"] == "head" else 0.0), s["angle"])
        f = outdir / f"{name}.png"
        sc.render.filepath = str(f)
        bpy.ops.render.render(write_still=True)
        shots.append({"shot": name, "path": str(f),
                      "bytes": f.stat().st_size if f.exists() else 0})
        print(f"[render] {name} -> {f.name} ({shots[-1]['bytes']} B)")

    rep = {"blend": a.blend, "engine": "CYCLES/CPU", "samples": a.samples,
           "resolution": [sc.render.resolution_x, sc.render.resolution_y],
           "shots": shots, "bounds_z": [round(zmin, 3), round(zmax, 3)]}
    rp = pathlib.Path(a.report) if a.report else outdir / "shotlist.json"
    rp.write_text(json.dumps(rep, indent=2))
    print(json.dumps({"shots": len(shots), "outdir": str(outdir)}, indent=2))


if __name__ == "__main__":
    main()
