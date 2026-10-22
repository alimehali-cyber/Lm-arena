#!/usr/bin/env python3
"""
Stage 5 - skinning QA. Poses the rig and measures the SKIN, not the intent.

  max edge stretch       a torn seam shows up as edges stretched many times over
  weight discontinuity   neighbouring vertices with barely-overlapping bone sets
  p99 stretch            what the bulk of the mesh does, so one bad edge is visible

This gate is what caught the armpit shear: a fused body scored 55x edge stretch on an
arm raise. A run fails when more than 0.5% of edges exceed the limit.

Run: .venv/bin/python blender/check_skinning.py --blend working/x/character.blend \
        --report working/x/05_skin.json
"""
from __future__ import annotations
import argparse, json, math, pathlib, sys
import bpy
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from scene_utils import reset_pose
from mathutils import Euler

# Two classes, deliberately separate:
#
#   GATE   poses inside the motion range of the shipped clip library. More than 0.5% of
#          edges over the stretch limit fails the build, because it would show on screen.
#   STRESS poses beyond that range, kept as information. They fail, and the report says
#          so: linear blend skinning cannot hold a 90-degree hip crease on this topology.
#          Silently dropping them would hide a real limitation; gating on them would
#          block a build for a pose nothing ever plays.
CASES = {
    "arm_raise":   ({"upperarm.L": (0, 0, -95)}, "gate"),
    "arm_forward": ({"upperarm.L": (-85, 0, 0), "forearm.L": (-70, 0, 0)}, "gate"),
    "elbow_fold":  ({"forearm.L": (-130, 0, 0)}, "gate"),
    "knee_bend":   ({"thigh.R": (-70, 0, 0), "shin.R": (95, 0, 0)}, "gate"),
    "spine_twist": ({"spine": (0, 35, 0), "chest": (0, 30, 0)}, "gate"),
    "head_turn":   ({"head": (25, 0, 55)}, "gate"),
    "hip_flex":    ({"thigh.L": (-55, 0, 0)}, "gate"),
    "hip_90_stress": ({"thigh.L": (-90, 0, 0)}, "stress"),
}


def evaluated(obj):
    dg = bpy.context.evaluated_depsgraph_get()
    return [v.co.copy() for v in obj.evaluated_get(dg).data.vertices]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--report", required=True)
    ap.add_argument("--stretch_limit", type=float, default=2.0)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    body = next(o for o in bpy.data.objects if o.type == "MESH")
    me = body.data
    reset_pose(arm)
    rest = evaluated(body)
    edges = [(e.vertices[0], e.vertices[1],
              (rest[e.vertices[0]] - rest[e.vertices[1]]).length) for e in me.edges]
    edges = [e for e in edges if e[2] > 1e-6]

    gi = {vg.index: vg.name for vg in body.vertex_groups}
    sets = [{gi[g.group] for g in v.groups if g.weight > 0.05} for v in me.vertices]
    discontinuous = 0
    for i, j, L in edges:
        if L < 0.01 and sets[i] and sets[j]:
            inter = len(sets[i] & sets[j])
            union = len(sets[i] | sets[j])
            if union and inter / union < 0.34:
                discontinuous += 1

    results = {"blend": a.blend, "verts": len(me.vertices), "edges": len(edges),
               "weight_discontinuous_edges": discontinuous, "cases": {}, "warnings": []}
    failed = []
    for name, (spec, kind) in CASES.items():
        reset_pose(arm)
        for bone, rot in spec.items():
            pb = arm.pose.bones.get(bone)
            if pb is None:
                results["warnings"].append(f"missing bone {bone}")
                continue
            pb.rotation_mode = "XYZ"
            pb.rotation_euler = Euler([math.radians(v) for v in rot], "XYZ")
        bpy.context.view_layer.update()
        posed = evaluated(body)
        ratios = sorted((((posed[i] - posed[j]).length / L) for i, j, L in edges), reverse=True)
        mx = ratios[0] if ratios else 0.0
        over = sum(1 for r in ratios if r > a.stretch_limit)
        p99 = ratios[min(len(ratios) - 1, int(len(ratios) * 0.01))] if ratios else 0.0
        results["cases"][name] = {"max_stretch": round(mx, 2), "p99_stretch": round(p99, 2),
                                  "edges_over_limit": over, "class": kind,
                                  "fraction_over_limit": round(over / max(1, len(edges)), 4)}
        if kind == "gate" and over > len(edges) * 0.005:
            failed.append(f"{name}: {over} edges over {a.stretch_limit}x (max {mx:.2f}x)")
    reset_pose(arm)

    results["status"] = "fail" if failed else "pass"
    results["failed_cases"] = failed
    results["known_limitations"] = [
        f"{k}: {v['edges_over_limit']} edges over the limit (max {v['max_stretch']}x) - "
        "outside the shipped motion range, reported not gated"
        for k, v in results["cases"].items()
        if v["class"] == "stress" and v["edges_over_limit"] > 0
    ]
    results["criteria"] = {
        "max_stretch_limit": a.stretch_limit,
        "allowed_fraction_over_limit": 0.005,
        "note": "A torn seam produces thousands of over-stretched edges; a handful at an "
                "extreme joint angle is normal for linear blend skinning.",
    }
    pathlib.Path(a.report).write_text(json.dumps(results, indent=2))
    print(json.dumps({"status": results["status"], "failed": failed,
                      "max_stretch": {k: v["max_stretch"] for k, v in results["cases"].items()}},
                     indent=2))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
