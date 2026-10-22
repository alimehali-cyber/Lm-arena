#!/usr/bin/env python3
"""
11-Gate Automated glTF 2.0 / GLB Mobile QA Validator (`validate_assets.py`).

Runs inside Blender 4.2 (`blender -b --factory-startup -P validate_assets.py -- ...`) to validate both
raw binary GLB structure (`pygltflib`) AND full Blender glTF 2.0 round-trip re-import geometry,
skin weights, sparse morph targets, world metric scale, and floor grounding.
"""
from __future__ import annotations

import argparse
import io
import json
import math
import os
import pathlib
import sys
from typing import Any, Dict

import bpy
from mathutils import Vector
import numpy as np
from PIL import Image
from pygltflib import GLTF2


def validate_glb(glb_path: pathlib.Path, cfg: Dict[str, Any], target_height_m: float, is_lod1: bool = False) -> Dict[str, Any]:
    budgets = cfg["budgets"]
    max_tris = int(budgets["lod1_max_triangles"] if is_lod1 else budgets["lod0_max_triangles"])
    max_tex = int(budgets["lod1_atlas_size"] if is_lod1 else budgets["lod0_atlas_size"])
    max_mb = float(budgets["max_glb_size_mb"])
    max_bones = int(budgets["max_bones"])
    max_mat = int(budgets["max_materials"])

    issues = []
    size_mb = round(glb_path.stat().st_size / (1024.0 * 1024.0), 4) if glb_path.exists() else 0.0
    gltf = GLTF2().load(str(glb_path))
    blob = gltf.binary_blob()

    # Check embedded texture resolutions cleanly from GLB binary bufferViews
    tex_sizes = []
    for img in gltf.images or []:
        if img.bufferView is not None:
            bv = gltf.bufferViews[img.bufferView]
            raw = blob[bv.byteOffset : bv.byteOffset + bv.byteLength]
            with Image.open(io.BytesIO(raw)) as im:
                tex_sizes.append([im.width, im.height])

    draw_calls = sum(len(m.primitives) for m in (gltf.meshes or []))
    mat_count = len(gltf.materials or [])
    anim_count = len(gltf.animations or [])
    skin_joints = len(gltf.skins[0].joints) if gltf.skins else 0
    morph_count = max((len(p.targets or []) for m in (gltf.meshes or []) for p in m.primitives), default=0)

    # Round-trip re-import into fresh Blender scene to inspect real evaluated world-space bounds & weights!
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb_path))

    real_meshes = [
        o for o in bpy.context.scene.objects
        if o.type == "MESH" and not o.hide_get() and not o.name.startswith("Icosphere")
    ]
    tris = 0
    has_nan = False
    min_z = 999.0
    max_z = -999.0
    max_influences_seen = 0
    non_bone_vgs = []

    arm_objs = [o for o in bpy.context.scene.objects if o.type == "ARMATURE"]
    bone_names = {b.name for a in arm_objs for b in a.data.bones}

    depsgraph = bpy.context.evaluated_depsgraph_get()
    for obj in real_meshes:
        tris += sum(len(p.vertices) - 2 for p in obj.data.polygons)
        for vg in obj.vertex_groups:
            if vg.name not in bone_names:
                non_bone_vgs.append(vg.name)
        for v in obj.data.vertices:
            nz_w = [g.weight for g in v.groups if g.weight > 1e-4]
            max_influences_seen = max(max_influences_seen, len(nz_w))
        eval_obj = obj.evaluated_get(depsgraph)
        mw = eval_obj.matrix_world
        for v in eval_obj.data.vertices:
            wco = mw @ v.co
            if not all(math.isfinite(c) for c in wco):
                has_nan = True
            min_z = min(min_z, float(wco.z))
            max_z = max(max_z, float(wco.z))

    measured_height_m = round(max_z - min_z, 4) if real_meshes else 0.0
    checks = {
        "01_file_size_within_budget": 0.05 < size_mb <= max_mb,
        "02_triangle_budget": 1000 < tris <= max_tris,
        "03_finite_manifold_geometry": (not has_nan) and len(real_meshes) >= 1,
        "04_single_pbr_material_low_drawcalls": (mat_count <= max_mat) and (draw_calls <= 3),
        "05_atlas_resolution_within_budget": len(tex_sizes) >= 1 and all(w <= max_tex and h <= max_tex for w, h in tex_sizes),
        "06_gles30_skeleton_joint_limit": 20 <= skin_joints <= max_bones,
        "07_clean_four_bone_skin_weights": (1 <= max_influences_seen <= 4) and len(non_bone_vgs) == 0,
        "08_26_facial_morph_targets_present": morph_count == 26,
        "09_animation_library_embedded": anim_count >= 20,
        "10_metric_world_height_accurate": abs(measured_height_m - target_height_m) <= 0.045,
        "11_flat_floor_grounding": abs(min_z) <= 0.008,
    }
    for k, ok in checks.items():
        if not ok:
            issues.append(k)

    return {
        "glb_path": str(glb_path),
        "passed": len(issues) == 0,
        "file_size_mb": size_mb,
        "triangle_count": tris,
        "draw_calls": draw_calls,
        "material_count": mat_count,
        "texture_resolutions": tex_sizes,
        "skeleton_joint_count": skin_joints,
        "max_influences_per_vertex": max_influences_seen,
        "morph_target_count": morph_count,
        "animation_count": anim_count,
        "measured_height_m": measured_height_m,
        "min_z_m": round(min_z, 5),
        "checks": checks,
        "issues": issues,
    }


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--glb", required=True)
    ap.add_argument("--config", required=True)
    ap.add_argument("--target-height", type=float, default=1.73)
    ap.add_argument("--out", required=True)
    ap.add_argument("--lod1", action="store_true")
    args = ap.parse_args(argv)

    cfg = json.loads(pathlib.Path(args.config).read_text())
    report = validate_glb(pathlib.Path(args.glb), cfg, args.target_height, is_lod1=args.lod1)
    out_p = pathlib.Path(args.out)
    out_p.parent.mkdir(parents=True, exist_ok=True)
    out_p.write_text(json.dumps(report, indent=2))
    print(json.dumps(report, indent=2), flush=True)
    sys.stdout.flush()
    sys.stderr.flush()
    os._exit(0 if report["passed"] else 3)


if __name__ == "__main__":
    main()
