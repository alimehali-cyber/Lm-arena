#!/usr/bin/env python3
"""
Stage 4: `60`-Bone GLES-Safe Humanoid + Face Rig & `26` Pixar Morph Target Builder (`rig_character.py`).

Guarantees:
  - Exact `60` deform joints (`<= 64` OpenGL ES 3.0 `uniform mat4 uJoints[64]` hardware budget):
    10 central spine/neck/head/jaw/eye bones + 2 * 25 bilateral arm/twist/5-finger/leg bones.
  - Strict anatomical partition skinning (`ZONE_TORSO`, `ZONE_ARMS`, `ZONE_LEGS`, `ZONE_HANDS`)
    plus 100% rigid attachment for `Eye_L`, `Eye_R`, `Hair`, and `Glasses` (`ZERO` cross-talk ever!).
  - Full `26` appealing Pixar facial & viseme morph targets on `Avatar_Head`, moving both upper lid AND
    `PART_LASHES` seamlessly during `Blink` & `Wink`!
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys
from typing import Dict, List, Set, Tuple

import bpy
from mathutils import Vector
import numpy as np


def build_60_bone_armature(anc: Dict[str, float]) -> bpy.types.Object:
    H = anc["height_m"]
    S = H / 1.73
    chin_z = anc["chin_z"]
    eye_x, eye_y, eye_z = anc["eye_x"], anc["eye_y"], anc["eye_z"]
    mouth_z = anc["mouth_z"]

    arm_data = bpy.data.armatures.new("AvatarRig_Armature")
    rig = bpy.data.objects.new("AvatarRig", arm_data)
    bpy.context.collection.objects.link(rig)
    bpy.context.view_layer.objects.active = rig
    bpy.ops.object.mode_set(mode="EDIT")
    eb = arm_data.edit_bones

    def add_b(
        name: str,
        head: Tuple[float, float, float],
        tail: Tuple[float, float, float],
        parent_name: str | None = None,
        connect: bool = False,
    ):
        b = eb.new(name)
        b.head = Vector(head)
        b.tail = Vector(tail)
        if (b.tail - b.head).length < 1e-4:
            b.tail = b.head + Vector((0.0, 0.0, 0.03))
        if parent_name and parent_name in eb:
            b.parent = eb[parent_name]
            b.use_connect = connect
        return b

    # 10 Central Spine, Neck, Head & Face Joints
    add_b("Root",    (0.0, 0.0, 0.0),              (0.0, 0.0, 0.12 * S))
    add_b("Hips",    (0.0, -0.006 * S, 0.855 * S), (0.0, -0.006 * S, 0.945 * S), "Root")
    add_b("Spine01", (0.0, -0.006 * S, 0.945 * S), (0.0, -0.008 * S, 1.040 * S), "Hips", True)
    add_b("Spine02", (0.0, -0.008 * S, 1.040 * S), (0.0, -0.008 * S, 1.135 * S), "Spine01", True)
    add_b("Chest",   (0.0, -0.008 * S, 1.135 * S), (0.0, -0.006 * S, 1.265 * S), "Spine02", True)
    add_b("Neck",    (0.0, -0.006 * S, 1.265 * S), (0.0, -0.004 * S, chin_z + 0.015), "Chest", True)
    add_b("Head",    (0.0, -0.004 * S, chin_z + 0.015), (0.0, -0.004 * S, chin_z + anc["head_h"]), "Neck", True)
    add_b("Jaw",     (0.0, -0.035 * S, mouth_z + 0.012), (0.0, -0.095 * S, chin_z + 0.008), "Head")
    add_b("Eye_L",   ( eye_x, eye_y, eye_z), ( eye_x, eye_y - 0.035, eye_z), "Head")
    add_b("Eye_R",   (-eye_x, eye_y, eye_z), (-eye_x, eye_y - 0.035, eye_z), "Head")

    # 2 * 25 = 50 Bilateral Joints -> Exact `60` Joints Total (`<= 64` GLES limit!)
    for tag, sx in (("L", 1.0), ("R", -1.0)):
        sh = Vector((sx * 0.166 * S, -0.008 * S, 1.245 * S))
        el = Vector((sx * 0.205 * S, -0.004 * S, 1.045 * S))
        wr = Vector((sx * 0.238 * S, -0.020 * S, 0.858 * S))
        ua_mid = sh.lerp(el, 0.5)
        fa_mid = el.lerp(wr, 0.5)

        add_b(f"Clavicle_{tag}",      (sx * 0.042 * S, -0.012 * S, 1.252 * S), tuple(sh), "Chest")
        add_b(f"UpperArm_{tag}",      tuple(sh), tuple(ua_mid), f"Clavicle_{tag}", True)
        add_b(f"UpperArmTwist_{tag}", tuple(ua_mid), tuple(el), f"UpperArm_{tag}", True)
        add_b(f"Forearm_{tag}",       tuple(el), tuple(fa_mid), f"UpperArmTwist_{tag}", True)
        add_b(f"ForearmTwist_{tag}",  tuple(fa_mid), tuple(wr), f"Forearm_{tag}", True)

        dir_down = Vector((sx * 0.14, -0.05, -1.0)).normalized()
        knuckle = wr + dir_down * (0.058 * S)
        add_b(f"Hand_{tag}", tuple(wr), tuple(knuckle), f"ForearmTwist_{tag}", True)

        th0 = wr + dir_down * (0.024 * S) + Vector((0.0, -0.023 * S, 0.0))
        th_dir = (dir_down * 0.72 + Vector((0.0, -0.62, 0.0)) - Vector((-sx * 0.18, 0.0, 0.0))).normalized()
        for k in range(3):
            p0 = th0 + th_dir * (0.046 * (k / 3.0) * S)
            p1 = th0 + th_dir * (0.046 * ((k + 1) / 3.0) * S)
            par = f"Hand_{tag}" if k == 0 else f"Thumb{k}_{tag}"
            add_b(f"Thumb{k+1}_{tag}", tuple(p0), tuple(p1), par, k > 0)

        for fname, y_off, flen in (("Index", 0.023, 0.054), ("Middle", 0.007, 0.060), ("Ring", -0.008, 0.055), ("Pinky", -0.022, 0.046)):
            f0 = knuckle + Vector((0.0, -y_off * S, 0.0))
            for k in range(3):
                p0 = f0 + dir_down * (flen * (k / 3.0) * S)
                p1 = f0 + dir_down * (flen * ((k + 1) / 3.0) * S)
                par = f"Hand_{tag}" if k == 0 else f"{fname}{k}_{tag}"
                add_b(f"{fname}{k+1}_{tag}", tuple(p0), tuple(p1), par, k > 0)

        hx = sx * 0.082 * S
        fx = sx * 0.087 * S
        add_b(f"Thigh_{tag}", (hx * 0.95, -0.004 * S, 0.855 * S), (hx, -0.004 * S, 0.490 * S), "Hips")
        add_b(f"Calf_{tag}",  (hx, -0.004 * S, 0.490 * S),         (fx, -0.004 * S, 0.088 * S), f"Thigh_{tag}", True)
        add_b(f"Foot_{tag}",  (fx, -0.004 * S, 0.088 * S),         (fx, -0.075 * S, 0.028 * S), f"Calf_{tag}", True)
        add_b(f"Toe_{tag}",   (fx, -0.075 * S, 0.028 * S),         (fx, -0.155 * S, 0.018 * S), f"Foot_{tag}", True)

    bpy.ops.object.mode_set(mode="OBJECT")
    assert len(rig.data.bones) == 60, f"Expected 60 bones, built {len(rig.data.bones)}"
    return rig


def get_part_indices(mesh_obj: bpy.types.Object, vg_name: str) -> Set[int]:
    vg = mesh_obj.vertex_groups.get(vg_name)
    if vg is None:
        return set()
    gi = vg.index
    out: Set[int] = set()
    for i, v in enumerate(mesh_obj.data.vertices):
        for g in v.groups:
            if g.group == gi and g.weight > 0.5:
                out.add(i)
    return out


def assign_rigid_single_bone(obj: bpy.types.Object, bone_name: str) -> None:
    vg = obj.vertex_groups.get(bone_name) or obj.vertex_groups.new(name=bone_name)
    vg.add(list(range(len(obj.data.vertices))), 1.0, "REPLACE")


def assign_body_capsule_weights(obj: bpy.types.Object, rig: bpy.types.Object, skip_rigid_bones: Set[str]) -> None:
    torso_ids = get_part_indices(obj, "ZONE_TORSO")
    arms_ids = get_part_indices(obj, "ZONE_ARMS")
    legs_ids = get_part_indices(obj, "ZONE_LEGS")
    hands_ids = get_part_indices(obj, "ZONE_HANDS")

    for helper in ("ZONE_TORSO", "ZONE_ARMS", "ZONE_LEGS", "ZONE_HANDS"):
        if helper in obj.vertex_groups:
            obj.vertex_groups.remove(obj.vertex_groups[helper])

    torso_set = {"Hips", "Spine01", "Spine02", "Chest", "Neck"}
    arms_L = {"Chest", "Clavicle_L", "UpperArm_L", "UpperArmTwist_L", "Forearm_L", "ForearmTwist_L"}
    arms_R = {"Chest", "Clavicle_R", "UpperArm_R", "UpperArmTwist_R", "Forearm_R", "ForearmTwist_R"}
    legs_L = {"Hips", "Thigh_L", "Calf_L", "Foot_L", "Toe_L"}
    legs_R = {"Hips", "Thigh_R", "Calf_R", "Toe_R", "Foot_R"}
    hands_L = {
        "ForearmTwist_L", "Hand_L", "Thumb1_L", "Thumb2_L", "Thumb3_L",
        "Index1_L", "Index2_L", "Index3_L", "Middle1_L", "Middle2_L", "Middle3_L",
        "Ring1_L", "Ring2_L", "Ring3_L", "Pinky1_L", "Pinky2_L", "Pinky3_L",
    }
    hands_R = {
        "ForearmTwist_R", "Hand_R", "Thumb1_R", "Thumb2_R", "Thumb3_R",
        "Index1_R", "Index2_R", "Index3_R", "Middle1_R", "Middle2_R", "Middle3_R",
        "Ring1_R", "Ring2_R", "Ring3_R", "Pinky1_R", "Pinky2_R", "Pinky3_R",
    }

    bones = [b for b in rig.data.bones if b.name not in ("Root", "Jaw", "Eye_L", "Eye_R", "Head")]
    segs = {b.name: (np.array(b.head_local, dtype=np.float64), np.array(b.tail_local, dtype=np.float64)) for b in bones}

    rigid_vgs = {rig_b: obj.vertex_groups.get(rig_b) for rig_b in skip_rigid_bones}
    rigid_vert_ids: Set[int] = set()
    for vg in rigid_vgs.values():
        if vg is not None:
            for i, v in enumerate(obj.data.vertices):
                if any(g.group == vg.index and g.weight > 0.5 for g in v.groups):
                    rigid_vert_ids.add(i)

    vgs = {b.name: (obj.vertex_groups.get(b.name) or obj.vertex_groups.new(name=b.name)) for b in rig.data.bones}

    for i, v in enumerate(obj.data.vertices):
        if i in rigid_vert_ids:
            continue
        p = np.array(v.co, dtype=np.float64)
        is_left = p[0] >= 0.0
        if i in torso_ids:
            allowed = torso_set
        elif i in arms_ids:
            allowed = arms_L if is_left else arms_R
        elif i in legs_ids:
            allowed = legs_L if is_left else legs_R
        elif i in hands_ids:
            allowed = hands_L if is_left else hands_R
        else:
            allowed = set(segs.keys())

        dists: List[Tuple[float, str]] = []
        for bname in allowed:
            a, b = segs[bname]
            ab = b - a
            t = float(np.clip(np.dot(p - a, ab) / max(float(np.dot(ab, ab)), 1e-9), 0.0, 1.0))
            proj = a + t * ab
            dists.append((float(np.linalg.norm(p - proj)), bname))
        dists.sort(key=lambda x: x[0])
        top = dists[: min(4, len(dists))]
        sigma = max(top[0][0] * 0.45 + 0.015, 0.015)
        raw_w = [math.exp(-((d - top[0][0]) ** 2) / (2.0 * sigma * sigma)) for d, _ in top]
        s_w = sum(raw_w) or 1.0
        for (_, bname), w in zip(top, raw_w):
            nw = w / s_w
            if nw > 0.005:
                vgs[bname].add([i], nw, "REPLACE")


def assign_head_weights_and_26_morphs(head_obj: bpy.types.Object, rig: bpy.types.Object, anc: Dict[str, float]) -> None:
    chin_z = anc["chin_z"]
    mouth_z = anc["mouth_z"]
    mouth_hx = anc.get("mouth_hx", 0.035)
    eye_x, eye_z = anc["eye_x"], anc["eye_z"]
    brow_z = anc["brow_z"]

    oral_ids = get_part_indices(head_obj, "PART_ORAL")
    teeth_up_ids = get_part_indices(head_obj, "PART_TEETH_UPPER")
    teeth_lo_ids = get_part_indices(head_obj, "PART_TEETH_LOWER")
    teeth_ids = teeth_up_ids | teeth_lo_ids
    tongue_ids = get_part_indices(head_obj, "PART_TONGUE")
    brow_ids = get_part_indices(head_obj, "PART_BROWS")
    lash_ids = get_part_indices(head_obj, "PART_LASHES")
    internal_ids = oral_ids | teeth_ids | tongue_ids

    # Read smooth C2-interpolated topological `LIP_UPPER` & `LIP_LOWER` weights BEFORE cleaning helper groups!
    vg_lu = head_obj.vertex_groups.get("LIP_UPPER")
    vg_ll = head_obj.vertex_groups.get("LIP_LOWER")
    up_weights = [0.0] * len(head_obj.data.vertices)
    lo_weights = [0.0] * len(head_obj.data.vertices)
    for i, mv in enumerate(head_obj.data.vertices):
        for g in mv.groups:
            if vg_lu is not None and g.group == vg_lu.index:
                up_weights[i] = float(g.weight)
            elif vg_ll is not None and g.group == vg_ll.index:
                lo_weights[i] = float(g.weight)

    for helper_name in ("PART_ORAL", "PART_TEETH_UPPER", "PART_TEETH_LOWER", "PART_TONGUE", "PART_BROWS", "PART_LASHES", "LIP_UPPER", "LIP_LOWER"):
        if helper_name in head_obj.vertex_groups:
            head_obj.vertex_groups.remove(head_obj.vertex_groups[helper_name])

    vg_neck = head_obj.vertex_groups.new(name="Neck")
    vg_head = head_obj.vertex_groups.new(name="Head")
    vg_jaw = head_obj.vertex_groups.new(name="Jaw")

    verts = head_obj.data.vertices
    for i, v in enumerate(verts):
        if i in tongue_ids:
            vg_jaw.add([i], 0.85, "REPLACE")
            vg_head.add([i], 0.15, "REPLACE")
        elif i in teeth_lo_ids:
            vg_jaw.add([i], 0.90, "REPLACE")
            vg_head.add([i], 0.10, "REPLACE")
        elif i in teeth_up_ids or i in brow_ids or i in lash_ids or i in oral_ids:
            vg_head.add([i], 1.0, "REPLACE")
        else:
            w_neck = float(np.clip((chin_z + 0.022 - v.co.z) / 0.080, 0.0, 1.0))
            w_head = 1.0 - w_neck
            if w_neck > 0.005:
                vg_neck.add([i], w_neck, "REPLACE")
            if w_head > 0.005:
                vg_head.add([i], w_head, "REPLACE")

    head_obj.shape_key_add(name="Basis", from_mix=False)
    base_co = [v.co.copy() for v in verts]

    def add_sk(name: str, fn) -> None:
        sk = head_obj.shape_key_add(name=name, from_mix=False)
        for idx, p0 in enumerate(base_co):
            sk.data[idx].co = p0 + fn(idx, p0)

    def eye_lid_mask(idx: int, p: Vector, sx: float) -> float:
        if idx in internal_ids or idx in brow_ids or p.y > -0.02:
            return 0.0
        if p.x * sx < -0.004:
            return 0.0
        dx = (p.x - sx * eye_x) / max(anc["eye_r"] * 1.48, 1e-4)
        dz = (p.z - eye_z) / max(anc["eye_r"] * 1.38, 1e-4)
        return math.exp(-(dx * dx + dz * dz))

    def brow_mask(idx: int, p: Vector, sx: float | None = None) -> float:
        if idx in internal_ids or idx in lash_ids or p.y > -0.02:
            return 0.0
        if sx is not None and p.x * sx < -0.005:
            return 0.0
        w = 1.0 if idx in brow_ids else math.exp(-((p.z - brow_z) / 0.022) ** 2) * 0.52
        if sx is not None:
            w *= float(np.clip(abs(p.x) / (eye_x * 0.35), 0.25, 1.0))
        return w

    def mouth_lip_mask(idx: int, p: Vector) -> Tuple[float, float]:
        if idx in brow_ids or idx in lash_ids:
            return 0.0, 0.0
        if idx in oral_ids:
            return (0.85, +1.0) if p.z >= mouth_z else (0.85, -1.0)
        if idx in teeth_lo_ids:
            return 0.42, -1.0
        if idx in teeth_up_ids:
            return 0.0, +1.0
        if idx in tongue_ids:
            return 0.42, -1.0
        wu, wl = up_weights[idx], lo_weights[idx]
        if wu > 1e-4 or wl > 1e-4:
            if wu >= wl:
                return wu, +1.0
            return wl, -1.0
        return 0.0, 0.0

    # 1..6: Eyelids + Upper Lashes (`Blink` & `Wink` close 100% cleanly across entire eyeball!)
    for sk_n, sx in (("eyeBlinkLeft", 1.0), ("eyeBlinkRight", -1.0)):
        def blink_fn(i: int, p: Vector, s: float = sx) -> Vector:
            if i in lash_ids and p.x * s > 0.0:
                return Vector((0.0, -0.0055, -0.029 * S if "S" in locals() else -0.029))
            m = eye_lid_mask(i, p, s)
            if m <= 1e-4:
                return Vector((0.0, 0.0, 0.0))
            if p.z >= eye_z - 0.001:
                return Vector((0.0, -0.0165 * m, -0.048 * m))
            return Vector((0.0, -0.0075 * m, +0.014 * m))
        add_sk(sk_n, blink_fn)
    for sk_n, sx in (("eyeWideLeft", 1.0), ("eyeWideRight", -1.0)):
        add_sk(sk_n, lambda i, p, s=sx: Vector((0.0, 0.0, (+0.0085 if p.z >= eye_z else -0.004) * eye_lid_mask(i, p, s))))
    for sk_n, sx in (("eyeSquintLeft", 1.0), ("eyeSquintRight", -1.0)):
        add_sk(sk_n, lambda i, p, s=sx: Vector((0.0, -0.002 * eye_lid_mask(i, p, s), (+0.0115 if p.z < eye_z else -0.0055) * eye_lid_mask(i, p, s))))

    # 7..11: Expressive Pixar Brows
    add_sk("browInnerUp", lambda i, p: Vector((0.0, -0.001, 0.017 * brow_mask(i, p) * math.exp(-(p.x / (eye_x * 1.25)) ** 2))))
    add_sk("browDownLeft", lambda i, p: Vector((-0.003 * brow_mask(i, p, 1.0), -0.002 * brow_mask(i, p, 1.0), -0.014 * brow_mask(i, p, 1.0))))
    add_sk("browDownRight", lambda i, p: Vector((+0.003 * brow_mask(i, p, -1.0), -0.002 * brow_mask(i, p, -1.0), -0.014 * brow_mask(i, p, -1.0))))
    add_sk("browOuterUpLeft", lambda i, p: Vector((0.0, 0.0, 0.017 * brow_mask(i, p, 1.0) * float(np.clip(abs(p.x) / eye_x, 0.0, 1.2)))))
    add_sk("browOuterUpRight", lambda i, p: Vector((0.0, 0.0, 0.017 * brow_mask(i, p, -1.0) * float(np.clip(abs(p.x) / eye_x, 0.0, 1.2)))))

    # 12..19: Mouth Smile, Frown, Pucker, Funnel, JawOpen & Signature `tongueOut`!
    for sk_n, sx in (("mouthSmileLeft", 1.0), ("mouthSmileRight", -1.0)):
        def smile_fn(i: int, p: Vector, s: float = sx) -> Vector:
            w, _ = mouth_lip_mask(i, p)
            side = float(np.clip((p.x * s + 0.006) / 0.032, 0.0, 1.0))
            if i in teeth_ids or i in tongue_ids:
                return Vector((0, 0, 0))
            return Vector((s * 0.012 * w * side, +0.003 * w * side, +0.0125 * w * side))
        add_sk(sk_n, smile_fn)

    for sk_n, sx in (("mouthFrownLeft", 1.0), ("mouthFrownRight", -1.0)):
        def frown_fn(i: int, p: Vector, s: float = sx) -> Vector:
            w, _ = mouth_lip_mask(i, p)
            side = float(np.clip((p.x * s + 0.006) / 0.032, 0.0, 1.0))
            if i in teeth_ids or i in tongue_ids:
                return Vector((0, 0, 0))
            return Vector((0.0, 0.0, -0.0105 * w * side))
        add_sk(sk_n, frown_fn)

    def pucker_fn(i: int, p: Vector) -> Vector:
        w, sgn = mouth_lip_mask(i, p)
        if i in teeth_ids or i in tongue_ids:
            return Vector((0, 0, 0))
        return Vector((-0.44 * p.x * w, -0.008 * w, sgn * 0.0018 * w))
    add_sk("mouthPucker", pucker_fn)

    def funnel_fn(i: int, p: Vector) -> Vector:
        w, sgn = mouth_lip_mask(i, p)
        center = math.exp(-(p.x / 0.026) ** 2)
        if i in teeth_ids or i in tongue_ids:
            return Vector((0.0, 0.0, -0.007 * w))
        return Vector((-0.34 * p.x * w, -0.0075 * w, (0.0068 if sgn > 0 else -0.012) * w * center))
    add_sk("mouthFunnel", funnel_fn)

    def jaw_open_fn(i: int, p: Vector) -> Vector:
        w, sgn = mouth_lip_mask(i, p)
        center = 0.42 + 0.58 * math.exp(-(p.x / 0.034) ** 2)
        if sgn > 0:
            return Vector((0.0, 0.0, +0.0048 * w * center))
        return Vector((0.0, +0.004 * w, -0.024 * w * center))
    add_sk("jawOpen", jaw_open_fn)

    def tongue_out_fn(i: int, p: Vector) -> Vector:
        if i in tongue_ids:
            return Vector((0.0, -0.038, -0.009))
        w, sgn = mouth_lip_mask(i, p)
        center = 0.42 + 0.58 * math.exp(-(p.x / 0.034) ** 2)
        return Vector((0.0, 0.0, (+0.0045 if sgn > 0 else -0.018) * w * center))
    add_sk("tongueOut", tongue_out_fn)

    # 20..22: Cheeks
    add_sk("cheekPuff", lambda i, p: Vector((math.copysign(0.0085, p.x), -0.0065, 0.002)) * (
        0.0 if i in internal_ids else math.exp(-(((abs(p.x) - eye_x * 1.1) / 0.032) ** 2 + ((p.z - (mouth_z + 0.02)) / 0.032) ** 2))
    ))
    for sk_n, sx in (("cheekSquintLeft", 1.0), ("cheekSquintRight", -1.0)):
        add_sk(sk_n, lambda i, p, s=sx: Vector((0.0, -0.004, +0.0075)) * (
            0.0 if (i in internal_ids or p.x * s < 0.0) else math.exp(-(((p.x - s * eye_x * 1.05) / 0.032) ** 2 + ((p.z - (eye_z - 0.034)) / 0.028) ** 2))
        ))

    # 23..26: Core Speech Visemes (`PP`, `FF`, `EE`, `aa`)
    add_sk("viseme_PP", lambda i, p: Vector((0.0, -0.003 * mouth_lip_mask(i, p)[0], -0.002 * mouth_lip_mask(i, p)[1] * mouth_lip_mask(i, p)[0])) if i not in teeth_ids else Vector((0, 0, 0)))
    add_sk("viseme_FF", lambda i, p: Vector((0.0, (+0.004 if mouth_lip_mask(i, p)[1] < 0 else 0.0) * mouth_lip_mask(i, p)[0], (+0.0048 if mouth_lip_mask(i, p)[1] > 0 else +0.001) * mouth_lip_mask(i, p)[0])) if i not in teeth_ids else Vector((0, 0, 0)))
    add_sk("viseme_EE", lambda i, p: Vector((math.copysign(0.0085, p.x) * mouth_lip_mask(i, p)[0], 0.0, (+0.0052 if mouth_lip_mask(i, p)[1] > 0 else -0.0085) * mouth_lip_mask(i, p)[0])))
    add_sk("viseme_aa", lambda i, p: Vector((0.0, 0.0, (+0.0060 if mouth_lip_mask(i, p)[1] > 0 else -0.0225) * mouth_lip_mask(i, p)[0] * (0.45 + 0.55 * math.exp(-(p.x / 0.034) ** 2)))))

    assert len(head_obj.data.shape_keys.key_blocks) - 1 == 26


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--in-blend", required=True)
    ap.add_argument("--out-blend", required=True)
    args = ap.parse_args(argv)

    bpy.ops.wm.open_mainfile(filepath=str(pathlib.Path(args.in_blend).resolve()))
    anc = json.loads(bpy.context.scene["avatar_anchors_json"])

    rig = build_60_bone_armature(anc)

    head_obj = bpy.data.objects["Avatar_Head"]
    assign_head_weights_and_26_morphs(head_obj, rig, anc)

    body_parts: List[bpy.types.Object] = []
    for name, rigid_bone in (("Eye_L", "Eye_L"), ("Eye_R", "Eye_R"), ("Hair", "Head"), ("Glasses", "Head")):
        o = bpy.data.objects.get(name)
        if o is not None:
            assign_rigid_single_bone(o, rigid_bone)
            body_parts.append(o)

    for name in ("Body_Torso", "Body_Arms", "Body_Legs", "Hands"):
        o = bpy.data.objects.get(name)
        if o is not None:
            body_parts.append(o)

    bpy.ops.object.select_all(action="DESELECT")
    for o in body_parts:
        o.select_set(True)
    main_body = bpy.data.objects["Body_Torso"]
    bpy.context.view_layer.objects.active = main_body
    bpy.ops.object.join()
    main_body.name = "Avatar_Body"
    main_body.data.name = "Avatar_Body_Mesh"

    assign_body_capsule_weights(main_body, rig, skip_rigid_bones={"Eye_L", "Eye_R", "Head"})

    for m_obj in (head_obj, main_body):
        m_obj.parent = rig
        mod = m_obj.modifiers.new(name="Armature", type="ARMATURE")
        mod.object = rig

    out_p = pathlib.Path(args.out_blend).resolve()
    out_p.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out_p))
    print(json.dumps({
        "status": "character_rigged",
        "blend": str(out_p),
        "bones": len(rig.data.bones),
        "morph_targets": len(head_obj.data.shape_keys.key_blocks) - 1,
    }, indent=2))


if __name__ == "__main__":
    main()
