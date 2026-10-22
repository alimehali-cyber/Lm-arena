#!/usr/bin/env python3
"""
Stage 3 - build the character: body, head, features, rig, skin weights, morphs.

Design decisions that matter, each one paid for with a failed attempt:

  * Lofted cross-sections, not stacked spheres. Ellipsoids always read as lumps no
    matter how much smoothing follows; a resampled profile is smooth by construction.
  * The arms are separate mesh islands, joined to the body *without welding*. A fused
    shell forces skin weights across the armpit crease, and any weight gradient over a
    3 mm crease shears into a sheet when the arm lifts (measured 55x edge stretch;
    with islands, 1.7x).
  * The armature reads the joints the body was actually built from. Recomputing them
    from separate constants once put the arm bones outside the arm skin, and every
    weight in the arm was wrong.

Run:
  .venv/bin/python blender/build_character.py --analysis working/x/01_analysis.json \
      --out working/x/character.blend --report working/x/03_build.json
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
from scene_utils import reset_pose  # noqa: E402


# --------------------------------------------------------------------------- helpers
def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def mat(name, hexcol, rough=0.55):
    """Reuse a material by name: joining objects must not create Skin.001, Skin.002..."""
    existing = bpy.data.materials.get(name)
    if existing is not None:
        return existing
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    c = hexcol.lstrip("#")
    r, g, b = (int(c[i:i + 2], 16) / 255.0 for i in (0, 2, 4))

    def to_linear(u):
        """sRGB hex is display-referred; Principled Base Color is linear."""
        return u / 12.92 if u <= 0.04045 else ((u + 0.055) / 1.055) ** 2.4

    r, g, b = (to_linear(u) for u in (r, g, b))
    if bsdf:
        bsdf.inputs["Base Color"].default_value = (r, g, b, 1.0)
        bsdf.inputs["Roughness"].default_value = rough
    m.diffuse_color = (r, g, b, 1.0)
    return m


def add_sphere(name, loc, scale, seg=24, ring=16):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=ring, radius=1.0, location=loc)
    o = bpy.context.object
    o.name = name
    o.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    return o


def add_cyl(name, p0, p1, r0, r1, verts=16):
    p0, p1 = Vector(p0), Vector(p1)
    d = p1 - p0
    bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r0, radius2=r1,
                                    depth=max(1e-4, d.length), location=(p0 + p1) / 2)
    o = bpy.context.object
    o.name = name
    o.rotation_mode = "QUATERNION"
    o.rotation_quaternion = d.to_track_quat("Z", "Y")
    return o


def join(objs, name):
    bpy.ops.object.select_all(action="DESELECT")
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    if len(objs) > 1:
        bpy.ops.object.join()
    o = bpy.context.object
    o.name = name
    return o


def loft(name, rings, ring=28, cap=True):
    """
    Bridge a stack of elliptical rings into one continuous surface.

    rings: [(z, half_width, half_depth, y_offset), ...] bottom to top.
    """
    import bmesh
    bm = bmesh.new()
    loops = []
    for (z, rx, ry, yoff) in rings:
        loop = []
        for i in range(ring):
            a = 2.0 * math.pi * i / ring
            loop.append(bm.verts.new((rx * math.cos(a), ry * math.sin(a) + yoff, z)))
        loops.append(loop)
    for a_ring, b_ring in zip(loops, loops[1:]):
        for i in range(ring):
            j = (i + 1) % ring
            try:
                bm.faces.new((a_ring[i], a_ring[j], b_ring[j], b_ring[i]))
            except ValueError:
                pass
    if cap:
        # triangle fans, not n-gons: the glTF exporter reports n-gon caps as invalid
        for r, flip in ((loops[0], True), (loops[-1], False)):
            c = sum((v.co for v in r), Vector((0, 0, 0))) / len(r)
            centre = bm.verts.new(c)
            for i in range(len(r)):
                j = (i + 1) % len(r)
                tri = (r[i], r[j], centre) if flip else (r[j], r[i], centre)
                try:
                    bm.faces.new(tri)
                except ValueError:
                    pass
    bm.normal_update()
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(ob)
    return ob


def smooth_profile(points, n=18):
    """
    Resample (z, half_width, half_depth, y_offset) with Catmull-Rom, clamped.

    Unclamped splines overshoot on uneven spacing, which turned the waist into a
    belly; clamping each channel to its bracketing control points keeps the
    silhouette inside the authored numbers.
    """
    pts = sorted(points, key=lambda p: p[0])
    if len(pts) < 2:
        return pts
    zs = [p[0] for p in pts]
    out = []
    for i in range(n):
        t = i / (n - 1)
        z = zs[0] + (zs[-1] - zs[0]) * t
        k = 0
        while k < len(zs) - 2 and z > zs[k + 1]:
            k += 1
        span = max(1e-9, zs[k + 1] - zs[k])
        u = (z - zs[k]) / span

        def cr(idx, chan):
            a = pts[max(0, idx - 1)][chan]
            b = pts[idx][chan]
            c = pts[min(len(pts) - 1, idx + 1)][chan]
            d = pts[min(len(pts) - 1, idx + 2)][chan]
            u2, u3 = u * u, u * u * u
            v = 0.5 * ((2 * b) + (-a + c) * u + (2 * a - 5 * b + 4 * c - d) * u2
                       + (-a + 3 * b - 3 * c + d) * u3)
            lo, hi = (b, c) if b <= c else (c, b)
            return max(lo, min(hi, v))

        out.append((z, cr(k, 1), cr(k, 2), cr(k, 3)))
    return out


# ----------------------------------------------------------------------------- body
def build_body(P, report, face=None):
    H = float(P["estimated_person_metres"])
    heads = float(P["heads_tall"])
    hh = H / heads

    # Vertical landmarks are fractions of the NON-HEAD height, not of the whole figure.
    #
    # The previous formulas ("waist = H - 3.05 heads") are calibrated for a 7-head figure
    # and silently invert on anything shorter: at 4.4 heads the hip landed at 76% of the
    # height, above the chest, and the rig folded into itself (skin stretch 29x). Expressing
    # the same numbers as fractions of the body below the head keeps every 7-head value
    # identical - body_H = 6/7 H there, so 0.5667 * 6/7 = 0.4857 = 3.4 heads exactly - and
    # degrades sensibly when the head is a quarter of the character.
    body_H = max(1e-3, H - hh)
    # Exact ratios (not rounded decimals) so a 7-head figure reproduces the previous mesh
    # exactly: hip 3.4 heads, waist H-3.05 heads, chest H-2.35, shoulder H-1.62, neck H-1.12.
    Z_ANKLE = 0.075
    Z_KNEE = (3.4 / 12.0) * body_H
    Z_HIP = (3.4 / 6.0) * body_H
    Z_WAIST = (3.95 / 6.0) * body_H
    Z_CHEST = (4.65 / 6.0) * body_H
    Z_SHLDR = (5.38 / 6.0) * body_H
    Z_NECK = 0.98 * body_H
    Z_HEAD = body_H + 0.44 * hh          # the head sits slightly into the neck, as before
    leg_len = Z_HIP

    # Shoulder span ~0.44 m for a 1.75 m adult. The analysis tends to over-read it, so
    # clamp into a believable band; everything else follows from this one measurement.
    sh_heads = max(1.45, min(1.75, float(P.get("shoulder_width_in_heads", 1.90))))
    WS = hh * sh_heads * 0.5
    WC = WS * 0.72
    WW = WS * 0.57
    WH = WS * 0.66
    D = 0.70

    parts = []

    # ---------------- torso ----------------
    torso_profile = [
        (Z_HIP - hh * 0.16, WH * 0.74, WH * D * 0.86, 0.0),
        (Z_HIP + hh * 0.02, WH * 1.00, WH * D * 1.00, 0.0),
        (Z_HIP + hh * 0.30, WH * 0.96, WH * D * 0.98, 0.0),
        (Z_WAIST - hh * 0.16, WH * 0.86, WH * D * 0.94, 0.0),
        (Z_WAIST + hh * 0.06, WW * 0.99, WW * D * 1.02, -hh * 0.004),
        (Z_WAIST + hh * 0.44, (WW + WC) * 0.55, WC * D * 0.98, -hh * 0.008),
        (Z_CHEST - hh * 0.14, WC * 0.94, WC * D * 1.02, -hh * 0.010),
        (Z_CHEST + hh * 0.04, WC * 1.00, WC * D * 1.00, -hh * 0.008),
        (Z_SHLDR - hh * 0.06, WC * 0.99, WC * D * 0.94, 0.0),
        (Z_SHLDR + hh * 0.20, WC * 0.74, WC * D * 0.82, hh * 0.004),
        (Z_NECK - hh * 0.02, hh * 0.115, hh * 0.108, hh * 0.006),
    ]
    parts.append(loft("torso", smooth_profile(torso_profile, n=20), ring=30))

    parts.append(add_cyl("neck", (0, hh * 0.012, Z_NECK - hh * 0.20),
                         (0, hh * 0.006, Z_HEAD - hh * 0.28), hh * 0.150, hh * 0.120, verts=20))

    joints = {}
    # 0.30 of the body below the head = 1.8 heads on a 7-head figure (unchanged), and
    # proportionally shorter arms on a stylised one, which is what concept art shows.
    arm_len = 0.30 * body_H
    for s_ in (-1, 1):
        tag = "L" if s_ > 0 else "R"
        # The shoulder sits outside the ribcage: if the arm axis is inside the torso
        # silhouette the union fuses arm and ribcage into one sheet.
        sh = Vector((s_ * WS * 0.80, 0, Z_SHLDR))
        el = sh + Vector((s_ * arm_len * 0.10, 0, -arm_len * 0.48))
        wr = el + Vector((s_ * arm_len * 0.04, 0, -arm_len * 0.46))
        hd = wr + Vector((0, 0, -hh * 0.41))
        joints.update({f"sh.{tag}": sh, f"el.{tag}": el, f"wr.{tag}": wr, f"hd.{tag}": hd})

        parts.append(add_sphere(f"deltoid.{s_}", sh + Vector((-s_ * hh * 0.075, 0, hh * 0.030)),
                                (hh * 0.175, hh * 0.150, hh * 0.170)))
        parts.append(loft(f"upperarm.{s_}", [
            (el.z, hh * 0.122, hh * 0.114, 0),
            (el.z + (sh.z - el.z) * 0.35, hh * 0.140, hh * 0.130, 0),
            (el.z + (sh.z - el.z) * 0.85, hh * 0.158, hh * 0.148, 0),
            (sh.z, hh * 0.176, hh * 0.160, 0),
        ], ring=16))
        parts.append(add_sphere(f"elbow.{s_}", el, (hh * 0.126, hh * 0.118, hh * 0.126)))
        parts.append(loft(f"forearm.{s_}", [
            (wr.z, hh * 0.086, hh * 0.078, 0),
            (wr.z + (el.z - wr.z) * 0.30, hh * 0.104, hh * 0.096, 0),
            (wr.z + (el.z - wr.z) * 0.75, hh * 0.130, hh * 0.120, 0),
            (el.z, hh * 0.132, hh * 0.122, 0),
        ], ring=16))
        bpy.data.objects[f"upperarm.{s_}"].location.x = (sh.x + el.x) / 2
        bpy.data.objects[f"forearm.{s_}"].location.x = (el.x + wr.x) / 2

        # hand: palm block, four fingers, thumb. A hand is ~0.19 m, about 0.75 head-units.
        palm = wr + Vector((0, 0, -hh * 0.20))
        parts.append(add_sphere(f"palm.{s_}", palm, (hh * 0.17, hh * 0.075, hh * 0.21)))
        for i, dx in enumerate((-0.52, -0.17, 0.17, 0.52)):
            knuck = palm + Vector((s_ * dx * hh * 0.17, 0, -hh * 0.19))
            tip = knuck + Vector((s_ * dx * hh * 0.03, hh * 0.01, -hh * 0.30))
            parts.append(add_cyl(f"finger{i}.{s_}", knuck, tip, hh * 0.052, hh * 0.042, verts=8))
            parts.append(add_sphere(f"knuck{i}.{s_}", knuck, (hh * 0.054,) * 3, seg=10, ring=6))
        thumb0 = palm + Vector((s_ * hh * 0.13, -hh * 0.05, hh * 0.03))
        thumb1 = palm + Vector((s_ * hh * 0.23, -hh * 0.09, -hh * 0.13))
        parts.append(add_cyl(f"thumb.{s_}", thumb0, thumb1, hh * 0.058, hh * 0.046, verts=8))

        # ---- leg ----
        hip = Vector((s_ * WH * 0.52, 0, Z_HIP + hh * 0.06))
        kn = Vector((s_ * WH * 0.46, 0, Z_KNEE))
        an = Vector((s_ * WH * 0.42, 0, Z_ANKLE))
        joints.update({f"hip.{tag}": hip, f"kn.{tag}": kn, f"an.{tag}": an,
                       f"toe.{tag}": Vector((an.x, -hh * 0.78, hh * 0.10))})
        parts.append(loft(f"thigh.{s_}", [
            (kn.z, hh * 0.140, hh * 0.140, 0),
            (kn.z + (hip.z - kn.z) * 0.30, hh * 0.170, hh * 0.168, -hh * 0.004),
            (kn.z + (hip.z - kn.z) * 0.70, hh * 0.205, hh * 0.198, 0),
            (hip.z, hh * 0.218, hh * 0.205, 0),
        ], ring=18))
        parts.append(loft(f"shin.{s_}", [
            (an.z, hh * 0.082, hh * 0.084, 0),
            (an.z + (kn.z - an.z) * 0.28, hh * 0.100, hh * 0.108, hh * 0.006),
            (an.z + (kn.z - an.z) * 0.72, hh * 0.128, hh * 0.130, 0),
            (kn.z, hh * 0.142, hh * 0.144, 0),
        ], ring=18))
        bpy.data.objects[f"thigh.{s_}"].location.x = (hip.x + kn.x) / 2
        bpy.data.objects[f"shin.{s_}"].location.x = (kn.x + an.x) / 2
        parts.append(add_sphere(f"knee.{s_}", kn + Vector((0, -hh * 0.012, 0)),
                                (hh * 0.145, hh * 0.150, hh * 0.150)))
        parts.append(add_sphere(f"ankle.{s_}", an, (hh * 0.090,) * 3))
        heel = Vector((an.x, hh * 0.22, hh * 0.16))
        ball = Vector((an.x, -hh * 0.50, hh * 0.11))
        toe = Vector((an.x, -hh * 0.78, hh * 0.10))
        parts.append(add_sphere(f"heel.{s_}", heel, (hh * 0.19, hh * 0.20, hh * 0.16)))
        parts.append(add_cyl(f"foot.{s_}", heel, ball, hh * 0.20, hh * 0.215, verts=14))
        parts.append(add_cyl(f"toe.{s_}", ball, toe, hh * 0.215, hh * 0.175, verts=14))
        parts.append(add_sphere(f"toebox.{s_}", toe, (hh * 0.185, hh * 0.13, hh * 0.105)))

    head_parts, HD = build_head_parts(face, dict(head_h=hh, head_center_z=Z_HEAD), report)
    parts.extend(head_parts)

    # ---- islands -----------------------------------------------------------
    ARM_PREFIXES = ("upperarm", "forearm", "palm", "finger", "knuck", "thumb")
    LEG_PREFIXES = ("thigh", "shin", "knee", "ankle", "foot", "heel", "toe")

    bpy.ops.object.select_all(action="DESELECT")
    for o in parts:
        o.select_set(True)
    bpy.context.view_layer.objects.active = parts[0]
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

    groups = {}
    part_labels = []
    for o in parts:
        bb = [o.matrix_world @ Vector(c) for c in o.bound_box]
        centre = sum(bb, Vector((0, 0, 0))) / 8.0
        scale = max(0.01, max((c - centre).length for c in bb))
        base = o.name.split(".")[0]
        side = "L" if centre.x > 0 else "R"
        if base in ("deltoid", "elbow") or base.startswith(ARM_PREFIXES):
            island, chain = f"arm.{side}", f"arm.{side}"
        elif base.startswith(LEG_PREFIXES):
            island, chain = "core", f"leg.{side}"
        else:
            island, chain = "core", "spine"
        groups.setdefault(island, []).append(o)
        part_labels.append((centre, scale, chain, base))

    target_tris = int(P.get("target_tris", 30000))
    budgets = {"core": 0.70, "arm.L": 0.15, "arm.R": 0.15}
    islands = []
    for gname, objs in groups.items():
        ob = join(objs, f"island_{gname.replace('.', '_')}")
        bpy.context.view_layer.objects.active = ob
        ob.data.remesh_voxel_size = hh * 0.014
        try:
            bpy.ops.object.voxel_remesh()
        except Exception as e:
            report["steps"].append(f"voxel_remesh_failed:{gname}:{e}")
        for _ in range(2):
            m = ob.modifiers.new("smooth", "SMOOTH")
            m.factor = 0.5
            m.iterations = 8
            bpy.ops.object.modifier_apply(modifier=m.name)
        cur = sum(len(p_.vertices) - 2 for p_ in ob.data.polygons)
        budget = int(target_tris * budgets.get(gname, 0.2))
        if cur > budget:
            dec = ob.modifiers.new("decimate", "DECIMATE")
            dec.decimate_type = "COLLAPSE"
            dec.ratio = max(0.02, budget / cur)
            bpy.ops.object.modifier_apply(modifier=dec.name)
        # hygiene per island: welding must never happen across islands
        bpy.ops.object.mode_set(mode="EDIT")
        bpy.ops.mesh.select_all(action="SELECT")
        bpy.ops.mesh.remove_doubles(threshold=hh * 0.0002)
        bpy.ops.mesh.normals_make_consistent(inside=False)
        bpy.ops.mesh.delete_loose()
        bpy.ops.object.mode_set(mode="OBJECT")
        ob.data.validate(verbose=False)
        tris = sum(len(p_.vertices) - 2 for p_ in ob.data.polygons)
        islands.append((gname, ob, len(ob.data.vertices), tris))
        report["steps"].append(f"island_{gname}:{len(ob.data.vertices)}v/{tris}t")

    bpy.ops.object.select_all(action="DESELECT")
    for _g, ob, _v, _t in islands:
        ob.select_set(True)
    bpy.context.view_layer.objects.active = islands[0][1]
    bpy.ops.object.join()
    body = bpy.context.object
    body.name = "Body"
    body.data.name = "CharacterMesh"
    report["islands"] = [{"name": g, "verts": v, "tris": t} for g, _o, v, t in islands]

    island_ranges = []
    off = 0
    for g, _o, v, _t in islands:
        island_ranges.append((g, off, off + v))
        off += v

    # feet to the floor
    zmin = min((body.matrix_world @ v.co).z for v in body.data.vertices)
    body.location.z -= zmin
    bpy.context.view_layer.objects.active = body
    bpy.ops.object.transform_apply(location=True, rotation=False, scale=False)
    dz = -zmin
    Z_HIP += dz; Z_NECK += dz; Z_CHEST += dz; Z_WAIST += dz; Z_SHLDR += dz; Z_HEAD += dz
    for k in joints:
        joints[k] = joints[k] + Vector((0, 0, dz))
    # part labels are in pre-shift coordinates; the mesh just moved, so move them too
    part_labels = [((c + Vector((0, 0, dz))), sc, chain, base)
                   for (c, sc, chain, base) in part_labels]
    silhouette = [(z + dz, hw_, hd_) for (z, hw_, hd_, _y) in torso_profile]

    report["body"] = {
        "verts": len(body.data.vertices), "polys": len(body.data.polygons),
        "dimensions": [round(v, 3) for v in body.dimensions],
        "head_unit_m": round(hh, 4), "feet_on_floor": round(zmin, 4),
        "height_m": round(max(v.co.z for v in body.data.vertices), 3),
    }
    HD = dict(HD or {})
    HD["head_h"] = hh
    HD["cz"] = Z_HEAD
    M = dict(head_h=hh, sh_w=WS * 2, hip_y=Z_HIP, chest_z=Z_CHEST, neck_z=Z_NECK,
             shoulder_z=Z_SHLDR, leg_len=leg_len, head_center_z=Z_HEAD, HD=HD,
             part_labels=part_labels, joints=joints, arm_len=arm_len,
             silhouette=silhouette, island_ranges=island_ranges)
    return body, M


# ----------------------------------------------------------------------------- head
def build_head_parts(face, M, report):
    """
    Anatomical head, sized in head-units:
      height 1.00 hh, width 0.55-0.70 hh, depth 1.16 x width
      eye line +0.05 hh, mouth -0.30 hh
    """
    hh = M["head_h"]
    cz = M["head_center_z"]
    ratio = (face["w"] / max(1.0, float(face["h"]))) if face else 0.78
    wide = max(0.55, min(0.70, 0.60 + (ratio - 0.78) * 0.18))
    hw = hh * wide * 0.5
    hd = hw * 1.16

    def P(x, y, z):
        return (x * hh, y * hh, cz + z * hh)

    parts = [
        add_sphere("cranium", P(0, 0.012, 0.15), (hw * 0.97, hd * 0.94, hh * 0.33)),
        add_sphere("face", P(0, -0.045, -0.02), (hw * 0.86, hd * 0.82, hh * 0.27)),
        add_sphere("jaw", P(0, -0.065, -0.245), (hw * 0.66, hd * 0.68, hh * 0.16)),
        add_sphere("chin", P(0, -0.175, -0.385), (hw * 0.40, hd * 0.38, hh * 0.095)),
        add_sphere("brow_ridge", P(0, -0.245, 0.110), (hw * 0.80, hh * 0.045, hh * 0.038)),
        add_cyl("nose_bridge", P(0, -0.275, 0.075), P(0, -0.335, -0.075),
                hh * 0.030, hh * 0.036, verts=14),
        add_sphere("nose_tip", P(0, -0.345, -0.080), (hh * 0.048, hh * 0.042, hh * 0.036)),
    ]
    for s_ in (-1, 1):
        parts.append(add_sphere(f"cheek.{s_}", P(s_ * 0.180, -0.185, -0.075),
                                (hh * 0.075, hh * 0.055, hh * 0.050)))
        parts.append(add_sphere(f"ear.{s_}", P(s_ * 0.305, 0.02, 0.0),
                                (hh * 0.026, hh * 0.062, hh * 0.085)))

    report["head"] = {
        "half_width_m": round(hw, 4), "half_depth_m": round(hd, 4),
        "centre_z": round(cz, 4), "part_count": len(parts),
        "height_m": round(hh, 4), "width_m": round(hw * 2, 4),
        "depth_m": round(hd * 2, 4), "width_over_height": round(hw * 2 / hh, 3),
    }
    return parts, dict(hw=hw, hd=hd, cz=cz, head_h=hh)


def build_features(HD, colours, report):
    """Eyes, lids, irises, brows and mouth sized in head-units, sitting on the face."""
    hh, hw, hd, cz = HD["head_h"], HD["hw"], HD["hd"], HD["cz"]
    eye_r = hh * 0.048
    eye_z = cz + hh * 0.050
    eye_x = hh * 0.130
    # The face surface at eye height is near -0.33 hh. Putting the eyeball CENTRE at the
    # surface pushes a full hemisphere out and the eyes read as blisters; 0.30 hh leaves
    # only the front cap visible, which is what an eye actually looks like.
    eye_y = -hh * 0.300
    objs = {}
    white = mat("EyeWhite", "#EFEDE8", 0.25)
    iris = mat("Iris", colours.get("iris", "#463127"), 0.22)
    skin = mat("Skin", colours.get("skin", "#D6B1A1"))
    brow_m = mat("Brow", colours.get("brow") or colours.get("hair_top", "#3A2A1E"), 0.7)
    lips = mat("Lips", colours.get("lips", "#B7816B"), 0.45)

    for s, tag in ((-1, "L"), (1, "R")):
        e = add_sphere(f"Eye_{tag}", (s * eye_x, eye_y, eye_z), (eye_r,) * 3, seg=24, ring=16)
        e.data.materials.append(white)
        objs[f"eye_{tag}"] = e
        # the iris must protrude past the sclera or it is invisible from the front
        ir = add_sphere(f"Iris_{tag}", (s * eye_x, eye_y - eye_r * 0.72, eye_z),
                        (eye_r * 0.44,) * 3, seg=20, ring=12)
        ir.data.materials.append(iris)
        objs[f"iris_{tag}"] = ir
        lid = add_sphere(f"Lid_{tag}", (s * eye_x, eye_y, eye_z + eye_r * 0.10),
                         (eye_r * 1.06, eye_r * 1.06, eye_r * 1.02), seg=20, ring=12)
        lid.data.materials.append(skin)
        bpy.context.view_layer.objects.active = lid
        bpy.ops.object.mode_set(mode="EDIT")
        bpy.ops.mesh.select_all(action="SELECT")
        bpy.ops.mesh.bisect(plane_co=(0, 0, eye_z + eye_r * 0.10), plane_no=(0, 0, 1),
                            clear_inner=True)
        bpy.ops.object.mode_set(mode="OBJECT")
        objs[f"lid_{tag}"] = lid
        br = add_sphere(f"Brow_{tag}", (s * eye_x, eye_y - eye_r * 0.30, eye_z + eye_r * 1.80),
                        (hh * 0.072, hh * 0.014, hh * 0.012), seg=16, ring=8)
        br.data.materials.append(brow_m)
        br.rotation_euler = (0, math.radians(-7 * s), 0)
        objs[f"brow_{tag}"] = br

    mo = add_sphere("Mouth", (0, -hh * 0.352, cz - hh * 0.300),
                    (hh * 0.072, hh * 0.018, hh * 0.019), seg=20, ring=10)
    mo.data.materials.append(lips)
    objs["mouth"] = mo

    report["features"] = sorted(objs)
    report["feature_geometry"] = {
        "eye_radius_m": round(eye_r, 4), "eye_spacing_m": round(eye_x * 2, 4),
        "eye_z": round(eye_z, 4), "mouth_z": round(cz - hh * 0.30, 4),
        "head_z_range": [round(cz - hh * 0.5, 4), round(cz + hh * 0.5, 4)],
    }
    return objs


def build_hair(HD, colours, report):
    hh, hw, hd, cz = HD["head_h"], HD["hw"], HD["hd"], HD["cz"]
    hair_mat = mat("Hair", colours.get("hair_top", "#3A2A1E"), 0.62)
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=18, radius=1.0,
                                         location=(0, hd * 0.02, cz + hh * 0.15))
    o = bpy.context.object
    o.name = "Hair"
    # scale to the cranium it sits on, not to the whole head, or the cap floats
    o.scale = (hw * 1.03, hd * 1.00, hh * 0.345)
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.bisect(plane_co=(0, 0, cz + hh * 0.235), plane_no=(0, 0, 1), clear_inner=True)
    bpy.ops.object.mode_set(mode="OBJECT")
    o.data.materials.append(hair_mat)
    return o


# ------------------------------------------------------------------------- armature
def build_armature(M, report):
    """
    Bones sit on the joints the body was built from.

    Arms are placed from the same `sh/el/wr/hd` vectors the mesh used, so a change to
    the arm geometry can never leave the bones outside the skin again.
    """
    hh = M["head_h"]
    hip = M["hip_y"]
    chest = M["chest_z"]
    J = M["joints"]
    HDd = M["HD"]
    vals = {
        "hip": hip, "spine0": hip + hh * 0.35, "spine1": chest - hh * 0.20,
        "chest_top": chest + hh * 0.30, "neck0": M["neck_z"] - hh * 0.10,
        "neck1": M["neck_z"] + hh * 0.10, "head_top": M["head_center_z"] + hh * 0.50,
        "eye_y": -HDd["hd"] * 0.90, "eye_y2": -HDd["hd"] * 1.40,
        "eye_z": HDd["cz"] + hh * 0.050,
        "jaw_y": -HDd["hd"] * 0.45, "jaw_z": HDd["cz"] - hh * 0.28,
        "jaw_y2": -HDd["hd"] * 0.85, "jaw_z2": HDd["cz"] - hh * 0.44,
    }
    for tag, sx in (("L", 1), ("R", -1)):
        for key in ("sh", "el", "wr", "hd", "hip", "kn", "an", "toe"):
            v = J[f"{key}.{tag}"]
            vals[f"{key}x_{tag}"] = v.x
            vals[f"{key}y_{tag}"] = v.y
            vals[f"{key}z_{tag}"] = v.z
        vals[f"clx_{tag}"] = sx * M["sh_w"] * 0.07
        vals[f"eyex_{tag}"] = HDd["hw"] * 0.52

    table = [
        ("root", (0, 0, 0.0), (0, 0, 0.12), None),
        ("pelvis", (0, 0, "@hip"), (0, 0, "@spine0"), "root"),
        ("spine", (0, 0, "@spine0"), (0, 0, "@spine1"), "pelvis"),
        ("chest", (0, 0, "@spine1"), (0, 0, "@chest_top"), "spine"),
        ("neck", (0, 0, "@neck0"), (0, 0, "@neck1"), "chest"),
        ("head", (0, 0, "@neck1"), (0, 0, "@head_top"), "neck"),
    ]
    for tag in ("L", "R"):
        table += [
            (f"clavicle.{tag}", ("@clx_" + tag, 0, "@shz_" + tag),
             ("@shx_" + tag, 0, "@shz_" + tag), "chest"),
            (f"upperarm.{tag}", ("@shx_" + tag, 0, "@shz_" + tag),
             ("@elx_" + tag, 0, "@elz_" + tag), f"clavicle.{tag}"),
            (f"forearm.{tag}", ("@elx_" + tag, 0, "@elz_" + tag),
             ("@wrx_" + tag, 0, "@wrz_" + tag), f"upperarm.{tag}"),
            (f"hand.{tag}", ("@wrx_" + tag, 0, "@wrz_" + tag),
             ("@hdx_" + tag, 0, "@hdz_" + tag), f"forearm.{tag}"),
            (f"eye.{tag}", ("@eyex_" + tag, "@eye_y", "@eye_z"),
             ("@eyex_" + tag, "@eye_y2", "@eye_z"), "head"),
        ]
    table += [("jaw", (0, "@jaw_y", "@jaw_z"), (0, "@jaw_y2", "@jaw_z2"), "head")]
    for tag in ("L", "R"):
        table += [
            (f"thigh.{tag}", ("@hipx_" + tag, 0, "@hip"), ("@knx_" + tag, 0, "@knz_" + tag), "pelvis"),
            (f"shin.{tag}", ("@knx_" + tag, 0, "@knz_" + tag),
             ("@anx_" + tag, 0, "@anz_" + tag), f"thigh.{tag}"),
            (f"foot.{tag}", ("@anx_" + tag, 0, "@anz_" + tag),
             ("@anx_" + tag, "@toey_" + tag, "@toez_" + tag), f"shin.{tag}"),
        ]

    def sub(coord):
        out = []
        for c in coord:
            if isinstance(c, str) and c.startswith("@"):
                out.append(vals[c[1:]])
            else:
                out.append(float(c))
        return Vector(out)

    bpy.ops.object.armature_add(enter_editmode=True, location=(0, 0, 0))
    arm = bpy.context.object
    arm.name = "RIG_Body"
    arm.data.name = "RIG_Body"
    eb = arm.data.edit_bones
    for b in list(eb):
        eb.remove(b)
    made = {}
    for name, a, b, parent in table:
        bone = eb.new(name)
        bone.head = sub(a)
        bone.tail = sub(b)
        if parent:
            bone.parent = made[parent]
        made[name] = bone
    bpy.ops.object.mode_set(mode="OBJECT")

    # control-only bones: excluding them from the skin keeps them out of the export and
    # stops them diluting weights at the shoulder
    # `root` is kept as a deform bone on purpose: export_mobile.py exports deform bones
    # only, so a non-deform root would vanish from the GLB and take the Turn clip's yaw
    # with it. A joint with no weights is legal and costs one matrix.
    for nm in ("clavicle.L", "clavicle.R"):
        b = arm.data.bones.get(nm)
        if b:
            b.use_deform = False

    report["bones"] = [b.name for b in arm.data.bones]
    report["bone_check"] = {"upperarm_L_x": round(made["upperarm.L"].head.x, 3),
                            "sh_w_half": round(M["sh_w"] * 0.5, 3)}
    return arm


def build_fingers(arm, M, report):
    """Four fingers and a thumb per hand, on the digits the palm mesh has."""
    hh = M["head_h"]
    J = M["joints"]
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode="EDIT")
    eb = arm.data.edit_bones
    made = 0
    for tag in ("L", "R"):
        sx = 1.0 if tag == "L" else -1.0
        wr = J[f"wr.{tag}"]
        palm = wr + Vector((0, 0, -hh * 0.20))
        for i, dx in enumerate((-0.52, -0.17, 0.17, 0.52)):
            knuck = palm + Vector((sx * dx * hh * 0.17, 0, -hh * 0.19))
            tip = knuck + Vector((sx * dx * hh * 0.03, hh * 0.01, -hh * 0.30))
            b = eb.new(f"finger{i}.{tag}")
            b.head, b.tail = knuck, tip
            b.parent = eb[f"hand.{tag}"]
            made += 1
        thumb0 = palm + Vector((sx * hh * 0.13, -hh * 0.05, hh * 0.03))
        thumb1 = palm + Vector((sx * hh * 0.23, -hh * 0.09, -hh * 0.13))
        b = eb.new(f"thumb.{tag}")
        b.head, b.tail = thumb0, thumb1
        b.parent = eb[f"hand.{tag}"]
        made += 1
    bpy.ops.object.mode_set(mode="OBJECT")
    report["fingers"] = made
    report["bone_count"] = len(arm.data.bones)
    report["deform_bones"] = sum(1 for b in arm.data.bones if b.use_deform)


# --------------------------------------------------------------------------- morphs
SHAPE_KEYS = [
    "jawOpen", "viseme_AA", "viseme_E", "viseme_I", "viseme_O", "viseme_U", "viseme_MBP",
    "mouthSmile_L", "mouthSmile_R", "mouthFrown", "cheekPuff",
    "browInnerUp", "browDown_L", "browDown_R", "eyeSquint_L", "eyeSquint_R", "Blink",
]


def add_shape_keys(head, HD, report, landmarks=None):
    """
    Jaw, visemes, brows, cheeks, squint and blink, all as real morph targets.

    `landmarks` overrides the default face geometry, in head-heights relative to the head
    centre. The defaults are the generic head's; a different character has a different
    brow height and mouth position, and using the wrong ones produces morphs that move
    nothing (five of them were dead on the first Professor Z build).
    """
    hh, hw, cz = HD["head_h"], HD["hw"], HD["cz"]
    L = {"mouth_z": -0.30, "mouth_w": 0.46, "brow_z": 0.125, "brow_w": 0.40,
         "brow_x": 0.130, "eye_z": 0.050, "eye_x": 0.130, "lid_z": 0.055,
         "jaw_top": -0.02, "jaw_y": 0.05}
    L.update(landmarks or {})
    head.shape_key_add(name="Basis", from_mix=False)

    def w_mouth(co):
        d = math.hypot((co.x) / (hw * L["mouth_w"]),
                       (co.z - (cz + hh * L["mouth_z"])) / (hh * 0.20))
        return max(0.0, 1.0 - d) ** 1.5

    def w_brow(co, side):
        if (co.x > 0) != (side > 0):
            return 0.0
        d = math.hypot((co.x - side * hh * L["brow_x"]) / (hw * L["brow_w"]),
                       (co.z - (cz + hh * L["brow_z"])) / (hh * 0.09))
        return max(0.0, 1.0 - d) ** 1.4

    def w_eye(co, side):
        if (co.x > 0) != (side > 0):
            return 0.0
        d = math.hypot((co.x - side * hh * L["eye_x"]) / (hw * 0.34),
                       (co.z - (cz + hh * L["eye_z"])) / (hh * 0.10))
        return max(0.0, 1.0 - d) ** 1.4

    def w_lid(co):
        d = math.hypot(co.x / (hw * 0.70), (co.z - (cz + hh * L["lid_z"])) / (hh * 0.07))
        return max(0.0, 1.0 - d) ** 2.0

    def make(name, fn):
        kb = head.shape_key_add(name=name, from_mix=False)
        for i, v in enumerate(head.data.vertices):
            dx, dy, dz = fn(v.co)
            kb.data[i].co = (v.co.x + dx, v.co.y + dy, v.co.z + dz)
        return kb

    # jaw + visemes: the mouth region moves, nothing else
    def jaw_drop(co):
        if co.z > cz + hh * L["jaw_top"] or co.y > -hh * L["jaw_y"]:
            return 0.0
        t = max(0.0, min(1.0, (cz + hh * L["jaw_top"] - co.z) / (hh * 0.55)))
        return t * w_mouth(co)

    make("jawOpen", lambda co: (0.0, 0.0, -hh * 0.085 * jaw_drop(co)))
    make("viseme_AA", lambda co: (0.0, -hh * 0.006 * w_mouth(co), -hh * 0.075 * w_mouth(co)))
    make("viseme_E", lambda co: (hh * 0.018 * w_mouth(co), 0.0, -hh * 0.028 * w_mouth(co)))
    make("viseme_I", lambda co: (hh * 0.026 * w_mouth(co), 0.0, hh * 0.006 * w_mouth(co)))
    make("viseme_O", lambda co: (-hh * 0.020 * w_mouth(co), -hh * 0.014 * w_mouth(co),
                                 -hh * 0.052 * w_mouth(co)))
    make("viseme_U", lambda co: (-hh * 0.028 * w_mouth(co), -hh * 0.010 * w_mouth(co),
                                 -hh * 0.030 * w_mouth(co)))
    make("viseme_MBP", lambda co: (0.0, hh * 0.006 * w_mouth(co), 0.0))

    # expressions: deliberately local, so the face does not move like a mask
    # A 3-4 mm displacement is under 5 px in the portrait review render and the smile was
    # invisible. Corner lift is now ~10 mm, with a little wrap back into the cheek so the
    # mouth line curves instead of sliding sideways.
    make("mouthSmile_L", lambda co: (hh * 0.026 * w_mouth(co) * (1 if co.x < 0 else 0),
                                     -hh * 0.008 * w_mouth(co) * (1 if co.x < 0 else 0),
                                     hh * 0.042 * w_mouth(co) * (1 if co.x < 0 else 0)))
    make("mouthSmile_R", lambda co: (-hh * 0.026 * w_mouth(co) * (1 if co.x > 0 else 0),
                                     -hh * 0.008 * w_mouth(co) * (1 if co.x > 0 else 0),
                                     hh * 0.042 * w_mouth(co) * (1 if co.x > 0 else 0)))
    make("mouthFrown", lambda co: (0.0, 0.0, -hh * 0.030 * w_mouth(co)))
    make("cheekPuff", lambda co: (0.0, -hh * 0.032 * w_mouth(co) * 0.5, 0.0))
    make("browInnerUp", lambda co: (0.0, 0.0, hh * 0.040 * max(w_brow(co, -1), w_brow(co, 1))))
    make("browDown_L", lambda co: (0.0, 0.0, -hh * 0.022 * w_brow(co, -1)))
    make("browDown_R", lambda co: (0.0, 0.0, -hh * 0.022 * w_brow(co, 1)))
    make("eyeSquint_L", lambda co: (0.0, 0.0, -hh * 0.010 * w_eye(co, -1)))
    make("eyeSquint_R", lambda co: (0.0, 0.0, -hh * 0.010 * w_eye(co, 1)))
    make("Blink", lambda co: (0.0, 0.0, -hh * 0.085 * w_lid(co)))

    # A morph that moves nothing is worse than no morph: it exports, it passes a name
    # check, and it silently does nothing at runtime. Measure the real displacement.
    base = [v.co.copy() for v in head.data.vertices]
    amps = {}
    for kb in head.data.shape_keys.key_blocks:
        if kb.name == "Basis":
            continue
        d = max(((kb.data[i].co - base[i]).length for i in range(len(base))), default=0.0)
        amps[kb.name] = round(d * 1000.0, 2)     # millimetres
    report["shape_key_displacement_mm"] = amps
    report["shape_key_dead"] = [k for k, v in amps.items() if v < 1.0]
    report["shape_keys"] = [k.name for k in head.data.shape_keys.key_blocks]
    report["shape_key_count"] = len(report["shape_keys"])


# -------------------------------------------------------------------------- skinning
CHAIN_OF = {}
for _side in ("L", "R"):
    for _b in (f"upperarm.{_side}", f"forearm.{_side}", f"hand.{_side}"):
        CHAIN_OF[_b] = f"arm.{_side}"
    for _b in (f"thigh.{_side}", f"shin.{_side}", f"foot.{_side}"):
        CHAIN_OF[_b] = f"leg.{_side}"
for _b in ("pelvis", "spine", "chest", "neck", "head", "jaw", "eye.L", "eye.R"):
    CHAIN_OF[_b] = "spine"

ISLAND_BONES = {
    "core": ("pelvis", "spine", "chest", "neck", "head", "jaw", "eye.L", "eye.R",
             "thigh.L", "shin.L", "foot.L", "thigh.R", "shin.R", "foot.R"),
    "arm.L": ("chest", "upperarm.L", "forearm.L", "hand.L", "thumb.L",
              "finger0.L", "finger1.L", "finger2.L", "finger3.L"),
    "arm.R": ("chest", "upperarm.R", "forearm.R", "hand.R", "thumb.R",
              "finger0.R", "finger1.R", "finger2.R", "finger3.R"),
}


def _dist_to_segment(p, a, b):
    ab = b - a
    denom = ab.length_squared
    if denom < 1e-12:
        return (p - a).length
    t = max(0.0, min(1.0, (p - a).dot(ab) / denom))
    return (p - (a + ab * t)).length


def weight_by_anatomy(mesh_obj, arm, M, report, island_ranges=None):
    """
    Distance-based weights, gated per island, then relaxed over the mesh graph.

    Bones come from their real positions, so "closest bone" is meaningful; the island
    limit stops a hand bone from reaching the ribcage; the Laplacian pass turns the
    per-island switch into a gradient. Blender's own operator for that smoothing needs
    a weight-paint viewport and fails headless, hence the explicit version below.
    """
    hh = M["head_h"]
    bones = {b.name: (b.head_local.copy(), b.tail_local.copy())
             for b in arm.data.bones if b.use_deform}
    for name in bones:
        if name not in mesh_obj.vertex_groups:
            mesh_obj.vertex_groups.new(name=name)

    def bones_for_index(i):
        if not island_ranges:
            return None
        for g, a, b_ in island_ranges:
            if a <= i < b_:
                allowed = ISLAND_BONES.get(g)
                return {n: bones[n] for n in allowed if n in bones} if allowed else None
        return None

    def smoothstep(a, b, x):
        t = max(0.0, min(1.0, (x - a) / max(1e-9, b - a)))
        return t * t * (3 - 2 * t)

    hip_z = M["hip_y"]
    counts = {}
    for v in mesh_obj.data.vertices:
        pool = bones_for_index(v.index) or bones
        # 0 above the hip line, 1 below it: the thigh must not own the pelvis, and the
        # pelvis must not own the thigh, or a 90-degree hip flex folds the crease double
        leg_f = smoothstep(hip_z + hh * 0.10, hip_z - hh * 0.12, v.co.z)
        cands = []
        for name, (h_co, t_co) in pool.items():
            d = _dist_to_segment(v.co, h_co, t_co)
            cands.append((d, name))
        cands.sort()
        cands = cands[:5]
        eps = 1e-4
        # The gate multiplies the *weight*, not the distance. Scaling distance created a
        # near-step wherever the gate crossed 0.5, which sheared the hip crease; a smooth
        # multiplier carries the influence across instead.
        gated = []
        for d, name in cands:
            base = name.split(".")[0]
            if base in ("thigh", "shin", "foot"):
                g = 0.10 + 0.90 * leg_f
            elif name == "pelvis":
                g = 1.0 - 0.85 * leg_f
            else:
                g = 1.0
            # Side gate. The feet nearly touch at the midline, so distance alone lets the
            # left foot bone claim right-foot vertices; when one leg swings, the other
            # foot is dragged apart (measured 17.8x edge stretch at the toes).
            if name.endswith(".L"):
                g *= smoothstep(-0.020, 0.020, v.co.x)
            elif name.endswith(".R"):
                g *= smoothstep(-0.020, 0.020, -v.co.x)
            gated.append(((1.0 / (d + eps)) ** 4.0) * g)
        total = sum(gated) or 1.0
        for (d, name), wr in zip(cands, gated):
            if wr / total >= 0.01:
                mesh_obj.vertex_groups[name].add([v.index], wr / total, "REPLACE")
        counts[cands[0][1].split(".")[0]] = counts.get(cands[0][1].split(".")[0], 0) + 1

    # ---- Laplacian relaxation over the mesh graph ----
    me = mesh_obj.data
    adj = [[] for _ in range(len(me.vertices))]
    for e in me.edges:
        a, b_ = e.vertices
        adj[a].append(b_)
        adj[b_].append(a)
    names = [vg.name for vg in mesh_obj.vertex_groups]
    W = [dict() for _ in range(len(me.vertices))]
    for v in me.vertices:
        for g in v.groups:
            if g.weight > 0:
                W[v.index][names[g.group]] = g.weight
    for _ in range(6):
        new = []
        for i in range(len(me.vertices)):
            acc = dict(W[i])
            n = len(adj[i])
            if n:
                for j in adj[i]:
                    for k, val in W[j].items():
                        acc[k] = acc.get(k, 0.0) + val / n
                for k in acc:
                    acc[k] *= 0.5
            new.append(acc)
        W = new
    for v in me.vertices:
        items = sorted(W[v.index].items(), key=lambda kv: -kv[1])[:4]
        total = sum(x for _k, x in items) or 1.0
        # Rigid extremities. A 3% weight difference across a 3 mm edge is harmless near
        # the hip but becomes 25 mm of stretch at the toe, because the lever arm is the
        # whole leg. Real feet and hands are rigid, so anything already dominated by one
        # bone is snapped to it; creases keep their gradient because they sit near 0.5.
        if items and items[0][1] / total >= 0.80:
            items = [(items[0][0], 1.0)] + [(k, x) for k, x in items[1:]]
            total = sum(x for _k, x in items)
        for vg_ in mesh_obj.vertex_groups:
            vg_.remove([v.index])
        for k, x in items:
            if x / total > 0.004:
                mesh_obj.vertex_groups[k].add([v.index], x / total, "REPLACE")

    report["skinning_method"] = "island_gated_distance+laplacian"
    report["vertex_counts_per_chain"] = counts
    mod = mesh_obj.modifiers.new("Armature", "ARMATURE")
    mod.object = arm
    mesh_obj.parent = arm


# ----------------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--analysis", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    a = ap.parse_args()

    A = json.loads(pathlib.Path(a.analysis).read_text())
    colours = A.get("colours", {})
    P = dict(A["proportions"])
    P["target_tris"] = A.get("target_tris", 30000)
    report = {"status": "running", "steps": [], "input_face": A.get("face") is not None,
              "warnings": []}

    reset()
    try:
        bpy.ops.preferences.addon_enable(module="cycles")
    except Exception:
        pass

    body, M = build_body(P, report, face=A.get("face"))
    HD = M["HD"]
    feats = build_features(HD, colours, report)
    hair = build_hair(HD, colours, report)
    arm = build_armature(M, report)
    build_fingers(arm, M, report)
    add_shape_keys(body, HD, report)

    # ---- accessories become skinned geometry, not bone-parented objects ----
    # Bone parenting anchors to the bone TAIL, which teleports the object; a vertex
    # group naming the bone keeps it in place and survives the join.
    bone_for = {"eye_L": "eye.L", "eye_R": "eye.R", "iris_L": "eye.L", "iris_R": "eye.R",
                "lid_L": "head", "lid_R": "head", "brow_L": "head", "brow_R": "head",
                "mouth": "jaw"}
    for name, o in feats.items():
        vg = o.vertex_groups.new(name=bone_for.get(name, "head"))
        vg.add(range(len(o.data.vertices)), 1.0, "REPLACE")
    vg = hair.vertex_groups.new(name="head")
    vg.add(range(len(hair.data.vertices)), 1.0, "REPLACE")

    bpy.ops.object.select_all(action="DESELECT")
    for o in [*feats.values(), hair, body]:
        o.select_set(True)
    bpy.context.view_layer.objects.active = body
    before = len(body.data.vertices)
    bpy.ops.object.join()
    body = bpy.context.object
    body.name = "Body"
    # Smooth shading on the merged mesh. Without it the glTF exporter splits normals per
    # face and duplicates almost every vertex (measured 92k verts for 34k triangles).
    bpy.ops.object.shade_smooth()
    report["steps"].append(f"accessories_joined:{before}->{len(body.data.vertices)}")

    # Materials: skin for head and neck, clothing for the body.
    # The clear() used to run AFTER the accessories were joined, which deleted the
    # eye, iris, brow, lip and hair slots and turned the whole face into blank skin.
    # Slots are now added only if missing, and the body/head split is applied only to
    # polygons that are not already on an accessory material.
    skin = mat("Skin", colours.get("skin", "#D6B1A1"))
    cloth = mat("Cloth", colours.get("clothing_chest", "#2A2F38"), 0.75)
    if cloth.name not in [m.name for m in body.data.materials if m]:
        body.data.materials.append(cloth)
    if skin.name not in [m.name for m in body.data.materials if m]:
        body.data.materials.append(skin)
    slot = {m.name: i for i, m in enumerate(body.data.materials) if m}
    keep = {"EyeWhite", "Iris", "Brow", "Lips", "Hair"}
    # Skin vs clothing is decided by which authored part each polygon came from, not by a
    # height threshold. A z-cut put the shirt's top ring and the chin on the wrong sides and
    # produced a jagged white collar (and, once "fixed" with a radius test, a red chin).
    # The parts are known before the join, so classify against them.
    clothing_bases = {"torso", "thigh", "shin", "knee", "ankle", "heel", "foot", "toe",
                      "toebox", "upperarm", "forearm", "deltoid", "elbow", "palm",
                      "finger0", "finger1", "finger2", "finger3", "knuck0", "knuck1",
                      "knuck2", "knuck3", "thumb"}
    labels = M.get("part_labels") or []
    for poly in body.data.polygons:
        current = body.data.materials[poly.material_index] if poly.material_index < len(body.data.materials) else None
        if current is not None and current.name in keep:
            continue
        co = [body.data.vertices[i].co for i in poly.vertices]
        c = sum(co, Vector((0, 0, 0))) / len(co)
        best, best_d = None, 1e9
        for (pc, pscale, _chain, base) in labels:
            d = (c - pc).length / pscale
            if d < best_d:
                best, best_d = base, d
        # Even with a correct part classification the boundary is a jagged line, because
        # voxel-remesh + decimate leave no clean edge loop there. Two rules make it read as
        # a collar rather than as spikes:
        #  1. the boundary sits high, on the narrow neck, not across the shoulders
        #  2. a polygon is skin only if ALL of its vertices are skin-side. Remesh and
        #     decimate produce long slivers that reach from the neck down onto the chest;
        #     a centroid test paints half of each sliver skin, which is exactly the white
        #     spike pattern in the review render.
        hh = M["head_h"]
        boundary = M["neck_z"] + hh * 0.02

        def skin_side(co_v):
            if co_v.z < boundary:
                return False
            if best in clothing_bases:
                if co_v.z < M["neck_z"] + hh * 0.18:
                    return False
                if co_v.z < M["neck_z"] + hh * 0.34 and math.hypot(co_v.x, co_v.y) > hh * 0.17:
                    return False
            return True

        poly.material_index = slot["Skin"] if all(skin_side(v_) for v_ in co) else slot["Cloth"]

    body.vertex_groups.clear()
    weight_by_anatomy(body, arm, M, report, island_ranges=M.get("island_ranges"))

    report["materials"] = sorted(m.name for m in bpy.data.materials)
    report["palette"] = {k: colours.get(k) for k in ("skin", "hair_top", "lips", "clothing_chest")}
    report["totals"] = {
        "objects": len(bpy.data.objects),
        "verts": sum(len(o.data.vertices) for o in bpy.data.objects if o.type == "MESH"),
        "polys": sum(len(o.data.polygons) for o in bpy.data.objects if o.type == "MESH"),
        "bones": len(arm.data.bones),
    }

    reset_pose(arm)
    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out))
    report["status"] = "ok"
    report["blend"] = str(out)
    report["blend_bytes"] = out.stat().st_size
    pathlib.Path(a.report).write_text(json.dumps(report, indent=2))
    print(json.dumps({"status": report["status"], "totals": report["totals"],
                      "shape_key_count": report.get("shape_key_count")}, indent=2))


if __name__ == "__main__":
    main()
