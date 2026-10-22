#!/usr/bin/env python3
"""
Stage 7b - the review suite: expressions, animation frames, deformation, materials.

Each render is a check, not a beauty shot. The modes pose the armature themselves
instead of trusting the JSON reports, which is the only way to catch a rig whose
numbers look right and whose skin tears anyway.

  expressions  every named expression at its peak frame
  anim         one frame per clip, chosen to show the pose that matters
  deform       extreme poses that expose weight problems
  material     bright neutral studio, to judge albedo and roughness

Run: .venv/bin/python blender/review_suite.py --blend working/x/character_anim.blend \
        --outdir renders/x/suite --mode all --samples 20 --res 340
"""
from __future__ import annotations
import argparse, json, math, pathlib, sys
import bpy
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from scene_utils import reset_pose
from mathutils import Vector, Euler
from render_review import (enable_cycles, setup_world, key_light, make_camera,
                           frame, fit_distance)


def setup_render(res, samples):
    enable_cycles()
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.device = "CPU"
    sc.cycles.samples = samples
    sc.cycles.use_denoising = False
    sc.render.resolution_x = res
    sc.render.resolution_y = int(res * 1.25)
    sc.render.film_transparent = False
    sc.render.image_settings.file_format = "PNG"
    setup_world()
    return sc


def bounds():
    zs = []
    for o in bpy.data.objects:
        if o.type == "MESH":
            for c in o.bound_box:
                zs.append((o.matrix_world @ Vector(c)).z)
    return (min(zs), max(zs)) if zs else (0.0, 1.75)


def shoot(sc, cam, out, subject_h, centre_z, angle=0, label=""):
    dist = fit_distance(subject_h, lens=cam.data.lens)
    frame(cam, Vector((0, 0, centre_z)), dist, centre_z, angle)
    sc.render.filepath = str(out)
    bpy.ops.render.render(write_still=True)
    size = out.stat().st_size if out.exists() else 0
    print(f"[shot] {label or out.stem} -> {out.name} ({size} B)"
          f"  h={subject_h:.2f} cz={centre_z:.2f} cam={tuple(round(v,2) for v in cam.location)}")
    return {"shot": label or out.stem, "path": str(out), "bytes": size}


def set_action(arm, name, frame_no, mesh=None):
    """
    Assign an action and evaluate one frame.

    These actions are slotted: the bone curves live in the OB slot and every facial
    morph lives in a KE slot, and the two are evaluated by different IDs. Assigning the
    action only to the armature renders bone-only poses - every expression looked
    identical to neutral until the KE slot was bound to the shape keys as well.
    """
    # Switching actions does not clear channels the new action does not key, so a
    # clip that ignores `root` would inherit the previous clip's root pose. Reset first.
    reset_pose(arm)
    ad = arm.animation_data or arm.animation_data_create()
    act = bpy.data.actions.get(name)
    if act is None:
        return False
    ad.action = act
    if hasattr(ad, "action_slot"):
        for sl in act.slots:
            if (sl.identifier or "").startswith("OB"):
                try:
                    ad.action_slot = sl
                except Exception:
                    pass
                break
    if mesh is not None and mesh.data.shape_keys is not None:
        sk = mesh.data.shape_keys
        kad = sk.animation_data or sk.animation_data_create()
        kad.action = act
        for sl in act.slots:
            if (sl.identifier or "").startswith("KE"):
                try:
                    kad.action_slot = sl
                except Exception:
                    pass
                break
    bpy.context.scene.frame_set(int(frame_no))
    bpy.context.view_layer.update()
    return True


def posed_height(mesh_obj):
    """Evaluated (posed) vertical extent, used to catch a clip that collapses the rig."""
    dg = bpy.context.evaluated_depsgraph_get()
    me = mesh_obj.evaluated_get(dg).data
    zs = [(mesh_obj.matrix_world @ v.co).z for v in me.vertices]
    return (min(zs), max(zs)) if zs else (0.0, 0.0)


def pose_bones(arm, spec):
    reset_pose(arm)
    for bone, rot in spec.items():
        pb = arm.pose.bones.get(bone)
        if pb is None:
            continue
        pb.rotation_mode = "XYZ"
        pb.rotation_euler = Euler([math.radians(v) for v in rot], "XYZ")
    bpy.context.view_layer.update()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--outdir", required=True)
    ap.add_argument("--mode", default="all",
                    choices=["all", "expressions", "anim", "deform", "material"])
    ap.add_argument("--samples", type=int, default=20)
    ap.add_argument("--res", type=int, default=340)
    ap.add_argument("--report", default=None)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    sc = setup_render(a.res, a.samples)
    outdir = pathlib.Path(a.outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    zmin, zmax = bounds()
    arm = next((o for o in bpy.data.objects if o.type == "ARMATURE"), None)
    reset_pose(arm)
    head_z = ((arm.matrix_world @ arm.data.bones["head"].head_local).z
              if arm and "head" in arm.data.bones else zmax - 0.18)
    key_light(zmax)
    cam = make_camera()
    sc.camera = cam
    body_h = zmax - zmin
    head_h = min(0.34, body_h * 0.45)
    mesh = next((o for o in bpy.data.objects if o.type == "MESH"), None)
    report = {"blend": a.blend, "modes": {}, "warnings": [],
              "resolution": [sc.render.resolution_x, sc.render.resolution_y]}
    shots = []

    if a.mode in ("all", "expressions"):
        names = ["Expression_Happy", "Expression_Sad", "Expression_Surprised",
                 "Expression_Thinking", "Expression_Curious", "Expression_Frustrated"]
        got = []
        for nm in names:
            if set_action(arm, nm, 15, mesh):  # peak frame of clip_expression_single
                shots.append(shoot(sc, cam, outdir / f"expr_{nm.replace('Expression_','').lower()}.png",
                                   head_h, head_z + 0.10, 18, nm))
                got.append(nm)
            else:
                report["warnings"].append(f"missing action {nm}")
        report["modes"]["expressions"] = got

    if a.mode in ("all", "anim"):
        picks = [("Idle_Breathing", 168, "body"), ("Walk", 19, "body"), ("Walk", 37, "body"),
                 ("Turn", 20, "body"), ("Gesture_Greet", 30, "body"),
                 ("Talk_Expressive", 60, "head"), ("React_Positive", 12, "body"),
                 ("Gesture_Explain", 24, "body"), ("Idle_LookAround", 60, "head")]
        got = []
        for nm, fr, part in picks:
            if not set_action(arm, nm, fr, mesh):
                report["warnings"].append(f"missing action {nm}")
                continue
            if mesh is not None:
                z0, z1 = posed_height(mesh)
                rest_h = body_h
                if (z1 - z0) < rest_h * 0.85 or z0 < -0.05:
                    report["warnings"].append(
                        f"{nm}@{fr} deforms the body to {z0:.2f}..{z1:.2f} m "
                        f"(rest {rest_h:.2f} m) - clip collapses the rig")
            if part == "head":
                shots.append(shoot(sc, cam, outdir / f"anim_{nm.lower()}_{fr}.png",
                                   head_h, head_z + 0.10, 22, f"{nm}@{fr}"))
            else:
                shots.append(shoot(sc, cam, outdir / f"anim_{nm.lower()}_{fr}.png",
                                   body_h, (zmin + zmax) / 2, 22, f"{nm}@{fr}"))
            got.append(f"{nm}@{fr}")
        report["modes"]["anim"] = got

    if a.mode in ("all", "deform"):
        cases = {
            "arm_raise_L": {"upperarm.L": (0, 0, -95), "forearm.L": (-25, 0, 0)},
            "arm_forward_L": {"upperarm.L": (-85, 0, 0), "forearm.L": (-70, 0, 0)},
            "knee_bend_R": {"thigh.R": (-70, 0, 0), "shin.R": (95, 0, 0)},
            "spine_twist": {"spine": (0, 35, 0), "chest": (0, 30, 0), "head": (0, -20, 0)},
            "head_extremes": {"head": (25, 0, 55)},
            "elbow_fold": {"forearm.L": (-130, 0, 0)},
        }
        for name, spec in cases.items():
            pose_bones(arm, spec)
            shots.append(shoot(sc, cam, outdir / f"deform_{name}.png",
                               body_h * 0.92, (zmin + zmax) / 2, 28, name))
        reset_pose(arm)
        report["modes"]["deform"] = list(cases)

    if a.mode in ("all", "material"):
        reset_pose(arm)
        setup_world(strength=1.6, colour=(0.35, 0.38, 0.45))   # the key rig is moody
        for L in [o for o in bpy.data.objects if o.type == "LIGHT"]:
            L.data.energy *= 1.7
        shots.append(shoot(sc, cam, outdir / "material_portrait.png", head_h,
                           head_z + 0.10, 12, "material_portrait"))
        shots.append(shoot(sc, cam, outdir / "material_body.png", body_h,
                           (zmin + zmax) / 2, 0, "material_body"))
        report["modes"]["material"] = ["material_portrait", "material_body"]

    report["shots"] = shots
    report["bounds_z"] = [round(zmin, 3), round(zmax, 3)]
    rp = pathlib.Path(a.report) if a.report else outdir / f"suite_{a.mode}.json"
    rp.write_text(json.dumps(report, indent=2))
    print(json.dumps({"shots": len(shots), "warnings": report["warnings"]}, indent=2))


if __name__ == "__main__":
    main()
