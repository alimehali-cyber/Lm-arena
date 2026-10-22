#!/usr/bin/env python3
"""
Professor Z - built from the supplied character sheet, as a real character rather than a
recolour of the generic blockout.

WHY THIS FILE EXISTS SEPARATELY
    `build_character.py` extrapolates a body from a photograph's face box. Professor Z is an
    authored design with a stated height, a stylised 3.7-head build, clothes and props. So
    the mesh is built here from measured landmarks, and the rig, skin weights, morph targets
    and the 21-clip animation library are the shared, already-verified ones.

DESIGN DECISIONS THAT WERE PAID FOR
  * No voxel remesh. The first attempt reused the generic pipeline's remesh, which unions
    every shell into one skin and then paints materials by nearest author label. On a bare
    figure that is fine; on a clothed character it welded the jacket to the shirt and
    smeared labels across the body (the render came out as a nude figure with random
    patches). Here each part keeps its own closed surface and its own material, joined
    without welding, which is also how the shared pipeline already handles eyes and hair.
  * Silhouette parts are lofted profiles, not sphere soup. A sphere cannot be made to have
    a jaw; a stack of elliptical rings can.
  * Vertical landmarks are fractions of total height measured off the sheet's front view,
    so the model is 3.7 heads tall like the drawing, not 7 like a photograph.
  * Materials are assigned per source object *before* the join; Blender remaps the slots
    correctly and the boundaries stay exact. Re-deriving them afterwards never worked.

MEASURED FROM THE SHEET (front view, 418 px tall, 133 px wide)
    hair top   0.000      eye line 0.152     chin     0.258
    shoulder   0.302      waist    0.520     hip      0.585
    knee       0.780      ankle    0.930     ground   1.000
    shoulder width 97 px / 418 px = 0.232 of height = 0.402 m at 1.73 m

Run:
  .venv/bin/python blender/build_professor_z.py \
      --out working/profz/character.blend --report working/profz/03_build.json
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
import build_character as BC          # noqa: E402
from scene_utils import reset_pose    # noqa: E402

H = 1.73                              # 5'8", stated on the sheet

F = {                                 # fractions of total height, top-down
    "hair_top": 0.000, "skull_top": 0.062, "eye": 0.152, "nose": 0.198,
    "chin": 0.258, "neck_base": 0.290, "shoulder": 0.302, "chest": 0.380,
    "waist": 0.520, "hip": 0.585, "crotch": 0.610, "knee": 0.780,
    "ankle": 0.930, "ground": 1.000,
}


def zf(frac: float) -> float:
    """Height above the ground for a top-down fraction of the figure."""
    return H * (1.0 - frac)


# Head geometry: skull from chin to crown, hair above it.
HEAD_BOTTOM = zf(F["chin"])
HEAD_TOP = zf(F["skull_top"])
HH = HEAD_TOP - HEAD_BOTTOM            # 0.339 m
HEAD_CZ = (HEAD_TOP + HEAD_BOTTOM) / 2
HW = 0.145                            # half width, 70 px of 418 * 1.73 / 2
HD = 0.165                            # half depth (side view is deeper than wide)

PALETTE = {
    "Skin":      ("#B87A4C", 0.55, 0.0),
    "Hair":      ("#241B13", 0.62, 0.0),
    "Brow":      ("#2A1E14", 0.70, 0.0),
    "EyeWhite":  ("#F4EEE6", 0.16, 0.0),
    "Iris":      ("#6B4426", 0.22, 0.0),
    "Pupil":     ("#0E0B08", 0.14, 0.0),
    "Lips":      ("#A45B45", 0.45, 0.0),
    "Jacket":    ("#6E4B2C", 0.78, 0.0),
    "JacketDark": ("#5A3B21", 0.80, 0.0),
    "Shirt":     ("#F0E8DB", 0.72, 0.0),
    "Tie":       ("#7A4C25", 0.55, 0.0),
    "Trousers":  ("#26262B", 0.80, 0.0),
    "Shoe":      ("#4B2F1B", 0.42, 0.0),
    "Glasses":   ("#2E2E30", 0.28, 0.75),
}


def make_materials():
    out = {}
    for name, (hexcol, rough, metal) in PALETTE.items():
        m = BC.mat(name, hexcol, rough)
        if metal and m.use_nodes:
            bsdf = m.node_tree.nodes.get("Principled BSDF")
            if bsdf:
                bsdf.inputs["Metallic"].default_value = metal
        out[name] = m
    return out


def set_material(o, material):
    """Give an object exactly one material slot. Join keeps these assignments exact."""
    o.data.materials.clear()
    o.data.materials.append(material)
    for p in o.data.polygons:
        p.material_index = 0
    return o


# --------------------------------------------------------------------------- head
def head_profile():
    """
    (t, half_width, half_depth, y_offset) from chin (t=0) to crown (t=1).

    The sheet's head is a wide cranium over a narrow jaw with soft cheeks; the profile is
    where that lives. Values are relative to HW / HD.
    """
    return [
        (0.00, 0.36, 0.34, -0.12),   # chin point, pushed forward
        (0.06, 0.50, 0.47, -0.06),
        (0.14, 0.63, 0.58, -0.02),
        (0.24, 0.75, 0.70, 0.00),
        (0.36, 0.85, 0.81, 0.00),
        (0.48, 0.93, 0.90, 0.00),
        (0.60, 0.98, 0.96, 0.01),
        (0.72, 1.00, 1.00, 0.02),
        (0.83, 0.96, 0.97, 0.03),
        (0.92, 0.82, 0.85, 0.04),
        (0.97, 0.58, 0.63, 0.04),
        (1.00, 0.26, 0.30, 0.04),
    ]


def build_skull(materials):
    rings = []
    for (t, rx, ry, yoff) in head_profile():
        rings.append((HEAD_BOTTOM + HH * t, HW * rx, HD * ry, yoff * HD))
    skull = BC.loft("Skull", BC.smooth_profile(rings, n=22), ring=32)
    return set_material(skull, materials["Skin"])


def build_face(materials, parts):
    """Eyes, iris, pupil, catch-light, brows, nose, ears, mouth, glasses."""
    eye_z = zf(F["eye"])
    eye_x = HW * 0.42
    # face surface at eye height: profile half-depth at t=(eye_z-bottom)/HH
    t_eye = (eye_z - HEAD_BOTTOM) / HH
    eye_y = -HD * (0.80 if t_eye < 0.6 else 0.86)
    eye_r = 0.0375                      # big cartoon eyes, ~0.11 HH

    for s, tag in ((1, "L"), (-1, "R")):
        # sclera: a sphere mostly inside the skull, only the front cap showing
        e = BC.add_sphere(f"Eye_{tag}", (s * eye_x, eye_y + eye_r * 0.86, eye_z),
                          (eye_r, eye_r * 0.86, eye_r * 1.02), seg=26, ring=18)
        parts.append(set_material(e, materials["EyeWhite"]))
        ir = BC.add_sphere(f"Iris_{tag}", (s * eye_x, eye_y - eye_r * 0.18, eye_z),
                           (eye_r * 0.52, eye_r * 0.24, eye_r * 0.52), seg=22, ring=14)
        parts.append(set_material(ir, materials["Iris"]))
        pu = BC.add_sphere(f"Pupil_{tag}", (s * eye_x, eye_y - eye_r * 0.30, eye_z),
                           (eye_r * 0.26, eye_r * 0.15, eye_r * 0.26), seg=16, ring=10)
        parts.append(set_material(pu, materials["Pupil"]))
        gl = BC.add_sphere(f"Catch_{tag}", (s * eye_x + eye_r * 0.22, eye_y - eye_r * 0.38,
                                            eye_z + eye_r * 0.26),
                           (eye_r * 0.12, eye_r * 0.08, eye_r * 0.12), seg=10, ring=8)
        parts.append(set_material(gl, materials["EyeWhite"]))

        # brow: thick, slightly arched, sits just above the rim of the glasses
        br = BC.add_sphere(f"Brow_{tag}", (s * eye_x, eye_y - eye_r * 0.10,
                                           eye_z + eye_r * 1.55),
                           (HW * 0.36, HD * 0.045, HH * 0.018), seg=20, ring=10)
        br.rotation_euler = (math.radians(-4), math.radians(-7 * s), math.radians(4 * s))
        parts.append(set_material(br, materials["Brow"]))

        # ear: flattened disc, high and slightly back
        ear = BC.add_sphere(f"Ear_{tag}", (s * HW * 0.93, HD * 0.40, eye_z - HH * 0.10),
                            (HW * 0.07, HD * 0.10, HH * 0.095), seg=18, ring=12)
        parts.append(set_material(ear, materials["Skin"]))

        # round wire glasses
        rim_r = eye_r * 1.30
        bpy.ops.mesh.primitive_torus_add(
            major_radius=rim_r, minor_radius=0.0035, major_segments=28, minor_segments=8,
            location=(s * eye_x, eye_y - eye_r * 0.30, eye_z))
        rim = bpy.context.object
        rim.name = f"Rim_{tag}"
        rim.rotation_euler = (math.radians(90), 0, 0)
        parts.append(set_material(rim, materials["Glasses"]))
        a = Vector((s * (eye_x + rim_r * 0.96), eye_y - eye_r * 0.30, eye_z))
        b = Vector((s * HW * 1.00, HD * 0.30, eye_z + HH * 0.05))
        parts.append(set_material(
            BC.add_cyl(f"Temple_{tag}", a, b, 0.0032, 0.0032, verts=8), materials["Glasses"]))

    bridge = BC.add_cyl("Bridge", (-eye_x + eye_r * 1.26, eye_y - eye_r * 0.30, eye_z),
                        (eye_x - eye_r * 1.26, eye_y - eye_r * 0.30, eye_z),
                        0.0032, 0.0032, verts=8)
    parts.append(set_material(bridge, materials["Glasses"]))

    # nose: small button, from the sheet it is tiny and rounded
    nose_z = zf(F["nose"])
    nose = BC.add_sphere("Nose", (0, -HD * 0.76, nose_z), (0.022, 0.020, 0.017), seg=18, ring=12)
    parts.append(set_material(nose, materials["Skin"]))
    bridge_n = BC.add_sphere("NoseBridge", (0, -HD * 0.70, nose_z + 0.022),
                             (0.010, 0.012, 0.020), seg=14, ring=10)
    parts.append(set_material(bridge_n, materials["Skin"]))

    # mouth: a soft, wide, closed smile. Scale matters - the first pass was 26 mm
    # across and vanished under the nose. The sheet's mouth is ~0.20 of head width.
    mouth_z = HEAD_BOTTOM + HH * 0.140
    mouth = BC.add_sphere("Mouth", (0, -HD * 0.700, mouth_z),
                          (HW * 0.27, 0.010, HH * 0.011), seg=24, ring=12)
    parts.append(set_material(mouth, materials["Lips"]))
    # a lower-lip shelf gives the smile a shape instead of a decal look
    lowlip = BC.add_sphere("LowerLip", (0, -HD * 0.685, mouth_z - HH * 0.016),
                           (HW * 0.19, 0.009, HH * 0.009), seg=20, ring=10)
    parts.append(set_material(lowlip, materials["Lips"]))
    for s_ in (-1, 1):
        ck = BC.add_sphere(f"Cheek_{'L' if s_ > 0 else 'R'}",
                           (s_ * HW * 0.52, -HD * 0.34, eye_z - HH * 0.12),
                           (HW * 0.24, HD * 0.26, HH * 0.115), seg=20, ring=12)
        parts.append(set_material(ck, materials["Skin"]))
    for s_ in (-1, 1):
        corner = BC.add_sphere(f"MouthCorner_{'L' if s_ > 0 else 'R'}",
                               (s_ * HW * 0.255, -HD * 0.66, mouth_z + HH * 0.005),
                               (HW * 0.045, 0.009, HH * 0.007), seg=12, ring=8)
        parts.append(set_material(corner, materials["Lips"]))

    return dict(eye_z=eye_z, eye_x=eye_x, eye_y=eye_y, eye_r=eye_r, mouth_z=mouth_z)


def build_hair(materials, parts):
    """
    Tousled dark hair.

    The shell is lofted from the skull's own profile, scaled 5.5 %, with an angular window
    cut out of the front so a face exists: the window is ~58 degrees either side of the
    front at jaw level, closes to nothing above the forehead, and leaves the ears clear.
    An earlier version used a plain sphere for the scalp and separate clumps for the
    silhouette; every clump below the sphere read as a detached lobe and the character
    ended up with hair earmuffs. Conforming the shell first and clumping only on the
    outline fixed it.
    """
    import bmesh

    top = zf(F["hair_top"])
    eye_z = zf(F["eye"])
    bottom = HEAD_BOTTOM + HH * 0.10
    # The hair dome is its own ellipsoid, not the skull offset: the skull's profile pulls
    # in hard above the temples, and hair that followed it produced a bowl cut with a
    # saucer on top. Half-depth 0.60 HH, crown exactly at F["hair_top"].
    rz = HH * 0.60
    zc = top - rz
    rx, ry = HW * 1.10, HD * 1.14
    n_rows, ring = 26, 44
    bm = bmesh.new()
    rows = []
    for i in range(n_rows):
        u = i / (n_rows - 1)                    # 0 bottom, 1 top
        z = bottom + (top - bottom) * u
        q = max(0.0, 1.0 - ((z - zc) / rz) ** 2) ** 0.5
        # Window, keyed on height relative to the eye line: closed above the hairline,
        # widest at the jaw. The ear zone gets its own plateau - a single ramp buried the
        # ears under the hair and they rendered as skin lobes inside the hair mass.
        h = (z - eye_z) / HH                     # -0.45 jaw .. +0.77 crown
        if h < -0.22:
            cut = 104.0
        elif h < -0.12:
            v = (h + 0.22) / 0.10
            cut = 104.0 + (76.0 - 104.0) * (v * v * (3 - 2 * v))
        elif h < 0.06:
            cut = 76.0 + (60.0 - 76.0) * (h + 0.12) / 0.18
        elif h < 0.26:
            v = (h - 0.06) / 0.20
            cut = 60.0 * (1.0 - (v * v * (3 - 2 * v)))
        else:
            cut = 0.0
        a0 = math.radians(-90.0 + cut)
        a1 = math.radians(270.0 - cut)
        closed = cut < 1e-6
        # An open window means the ring ends are real edges; a closed ring must wrap, or
        # the hair has a slit down the front of the skull (which rendered as a notch in
        # the hairline).
        n_pts = ring if closed else ring - 1
        row = []
        for k in range(n_pts):
            ang = a0 + (2 * math.pi if closed else (a1 - a0)) * k / (ring - 1)
            row.append(bm.verts.new((rx * q * math.cos(ang),
                                     ry * q * math.sin(ang) + HD * 0.10,
                                     z)))
        rows.append((row, closed))
    for (ra, ca), (rb, cb) in zip(rows, rows[1:]):
        n = len(ra)
        span = n if (ca and cb) else n - 1
        for k in range(span):
            k2 = (k + 1) % n
            try:
                bm.faces.new((ra[k], ra[k2], rb[k2], rb[k]))
            except ValueError:
                pass
    bm.normal_update()
    me = bpy.data.meshes.new("HairShell")
    bm.to_mesh(me)
    bm.free()
    shell = bpy.data.objects.new("HairShell", me)
    bpy.context.collection.objects.link(shell)
    bpy.ops.object.select_all(action="DESELECT")
    shell.select_set(True)
    bpy.context.view_layer.objects.active = shell
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.delete_loose()
    bpy.ops.object.mode_set(mode="OBJECT")
    parts.append(set_material(shell, materials["Hair"]))

    # clumps: (x, y, height above head centre, rx, ry, rz, tilt) in head units.
    # They sit ON the shell and break its outline; none may rise above the cap.
    clumps = [
        (-0.66, -0.72, 0.24, 0.20, 0.13, 0.11, 18), (-0.22, -0.82, 0.29, 0.19, 0.12, 0.10, -6),
        (0.24, -0.80, 0.25, 0.20, 0.13, 0.11, 10), (0.64, -0.70, 0.22, 0.19, 0.13, 0.11, -20),
        (-0.44, -0.30, 0.42, 0.20, 0.19, 0.13, 14), (0.20, -0.22, 0.45, 0.20, 0.19, 0.13, -10),
        (-0.96, 0.04, 0.02, 0.13, 0.16, 0.16, 26), (0.96, 0.06, 0.01, 0.13, 0.16, 0.16, -28),
        (-0.80, 0.42, 0.12, 0.16, 0.16, 0.14, 18), (0.78, 0.44, 0.11, 0.16, 0.16, 0.14, -20),
        (0.02, 0.60, 0.24, 0.22, 0.16, 0.14, 4), (-0.40, 0.66, 0.02, 0.16, 0.14, 0.15, -8),
        (0.42, 0.64, 0.01, 0.16, 0.14, 0.15, 8),
        # errant tufts, for asymmetry
        (-0.52, -0.34, 0.40, 0.11, 0.10, 0.10, -26), (0.56, -0.22, 0.38, 0.10, 0.10, 0.11, 22),
    ]
    for i, (x, y, zr, rx, ry, rz, tilt) in enumerate(clumps):
        o = BC.add_sphere(f"HairClump{i}", (x * HW * 0.86, y * HD * 0.88, HEAD_CZ + HH * zr),
                          (rx * HW * 0.94, ry * HD * 0.94, rz * HH * 0.94), seg=18, ring=12)
        o.rotation_euler = (math.radians(tilt * 0.5), math.radians(tilt),
                            math.radians(tilt * 0.7))
        parts.append(set_material(o, materials["Hair"]))


# --------------------------------------------------------------------------- body
def build_body(materials, parts, J):
    """
    Jacket, shirt, bow tie, trousers, shoes, hands.

    Widths come from the sheet's width profile, corrected for the fact that the profile
    includes the arms: the jacket is a boxy blazer, ~0.19 m half-width at the chest, and
    the arms hang at its outer edge - not outside it, which is what made the first build
    look like a mannequin with detached shoulders.
    """
    sh_z = zf(F["shoulder"])          # 1.208
    chest_z = zf(F["chest"])          # 1.073
    waist_z = zf(F["waist"])          # 0.830
    hip_z = zf(F["hip"])              # 0.718
    knee_z = zf(F["knee"])            # 0.381
    ankle_z = zf(F["ankle"])          # 0.121
    hem_z = hip_z - 0.02              # blazer ends just below the hip line

    torso_hw, torso_hd = 0.170, 0.128

    # ---- neck ----
    neck = BC.add_cyl("Neck", (0, 0.004, sh_z - 0.035), (0, 0.004, HEAD_BOTTOM + 0.010),
                      0.052, 0.049, verts=20)
    parts.append(set_material(neck, materials["Skin"]))

    # ---- jacket ----
    torso = BC.loft("Jacket", BC.smooth_profile([
        (hem_z,          torso_hw * 1.03, torso_hd * 1.02, 0.0),
        (hip_z + 0.02,   torso_hw * 1.00, torso_hd * 1.00, 0.0),
        (waist_z,        torso_hw * 0.97, torso_hd * 0.97, 0.0),
        (chest_z - 0.05, torso_hw * 0.99, torso_hd * 1.00, -0.003),
        (chest_z + 0.06, torso_hw * 1.00, torso_hd * 0.99, -0.002),
        (sh_z - 0.050,   torso_hw * 1.00, torso_hd * 0.98, 0.0),
        (sh_z - 0.010,   torso_hw * 0.92, torso_hd * 0.95, 0.002),
        (sh_z + 0.022,   torso_hw * 0.74, torso_hd * 0.80, 0.006),
        (sh_z + 0.045,   torso_hw * 0.50, torso_hd * 0.56, 0.008),
    ], n=22), ring=32)
    parts.append(set_material(torso, materials["Jacket"]))

    # ---- shirt: a tapered panel standing proud of the jacket front ----
    shirt = BC.loft("ShirtFront", BC.smooth_profile([
        (chest_z - 0.13, 0.022, 0.016, -torso_hd * 0.93),
        (chest_z - 0.05, 0.036, 0.016, -torso_hd * 0.95),
        (chest_z + 0.02, 0.044, 0.016, -torso_hd * 0.96),
        (chest_z + 0.09, 0.042, 0.015, -torso_hd * 0.92),
        (sh_z + 0.040,   0.028, 0.014, -torso_hd * 0.78),
    ], n=14), ring=20)
    parts.append(set_material(shirt, materials["Shirt"]))

    # collar band around the neck, wide enough to be seen above the jacket
    collar = BC.loft("Collar", [
        (sh_z + 0.030, 0.071, 0.064, 0.004),
        (sh_z + 0.062, 0.063, 0.057, 0.004),
        (sh_z + 0.088, 0.055, 0.050, 0.004),
    ], ring=22, cap=False)
    parts.append(set_material(collar, materials["Shirt"]))

    # lapels: thin plates framing the shirt, angled into a V
    for s_ in (-1, 1):
        lap = BC.add_sphere(f"Lapel_{'L' if s_ > 0 else 'R'}",
                            (s_ * 0.072, -torso_hd * 0.90, chest_z + 0.075),
                            (0.052, 0.026, 0.120), seg=18, ring=12)
        lap.rotation_euler = (0, math.radians(9 * s_), math.radians(-15 * s_))
        parts.append(set_material(lap, materials["JacketDark"]))

    # ---- bow tie: the sheet's is wide, nearly the width of the collar ----
    tie_z = sh_z + 0.038
    for s_ in (-1, 1):
        w = BC.add_sphere(f"Bow_{'L' if s_ > 0 else 'R'}",
                          (s_ * 0.042, -0.064, tie_z), (0.046, 0.017, 0.028),
                          seg=18, ring=12)
        w.rotation_euler = (0, 0, math.radians(-10 * s_))
        parts.append(set_material(w, materials["Tie"]))
    knot = BC.add_sphere("BowKnot", (0, -0.070, tie_z), (0.015, 0.015, 0.018),
                         seg=14, ring=10)
    parts.append(set_material(knot, materials["Tie"]))

    # ---- arms ----
    for s_, tag in ((1, "L"), (-1, "R")):
        sh = Vector((s_ * 0.163, 0, sh_z - 0.020))
        el = Vector((s_ * 0.188, 0.004, sh_z - 0.300))
        wr = Vector((s_ * 0.196, 0.004, sh_z - 0.565))
        hd = Vector((s_ * 0.196, 0.004, sh_z - 0.660))
        J[f"sh.{tag}"], J[f"el.{tag}"], J[f"wr.{tag}"], J[f"hd.{tag}"] = sh, el, wr, hd

        parts.append(set_material(
            BC.add_sphere(f"Deltoid_{tag}", (s_ * 0.166, 0, sh_z - 0.005),
                          (0.062, 0.058, 0.064), seg=20, ring=14), materials["Jacket"]))
        arm = BC.loft(f"Sleeve_{tag}", [
            (wr.z,                        0.029, 0.028, 0),
            (wr.z + (el.z - wr.z) * 0.30, 0.033, 0.032, 0),
            (wr.z + (el.z - wr.z) * 0.75, 0.038, 0.037, 0),
            (el.z,                        0.040, 0.039, 0),
            (el.z + (sh.z - el.z) * 0.35, 0.045, 0.044, 0),
            (el.z + (sh.z - el.z) * 0.80, 0.052, 0.050, 0),
            (sh.z,                        0.056, 0.053, 0),
        ], ring=20)
        arm.location.x = (sh.x + el.x) / 2
        parts.append(set_material(arm, materials["Jacket"]))

        cuff = BC.add_cyl(f"Cuff_{tag}", wr + Vector((0, 0, 0.022)), wr + Vector((0, 0, -0.002)),
                          0.031, 0.030, verts=16)
        parts.append(set_material(cuff, materials["Shirt"]))

        # hand: compact palm, short fingers - the first pass grew claws
        palm = wr + Vector((0, 0, -0.042))
        parts.append(set_material(BC.add_sphere(f"Palm_{tag}", palm, (0.034, 0.021, 0.041),
                                                seg=20, ring=14), materials["Skin"]))
        for i, dx in enumerate((-0.50, -0.17, 0.17, 0.50)):
            knuck = palm + Vector((s_ * dx * 0.028, -0.002, -0.030))
            tip = knuck + Vector((s_ * dx * 0.004, 0.001, -0.036))
            parts.append(set_material(
                BC.add_cyl(f"Finger{i}_{tag}", knuck, tip, 0.0095, 0.0075, verts=8),
                materials["Skin"]))
            parts.append(set_material(
                BC.add_sphere(f"Knuckle{i}_{tag}", knuck, (0.0085, 0.0085, 0.0085), seg=10, ring=8),
                materials["Skin"]))
        th0 = palm + Vector((s_ * 0.024, -0.008, 0.004))
        th1 = palm + Vector((s_ * 0.038, -0.014, -0.026))
        parts.append(set_material(BC.add_cyl(f"Thumb_{tag}", th0, th1, 0.010, 0.008, verts=8),
                                  materials["Skin"]))

    # ---- legs ----
    for s_, tag in ((1, "L"), (-1, "R")):
        hip = Vector((s_ * 0.078, 0, hip_z))
        kn = Vector((s_ * 0.074, 0, knee_z))
        an = Vector((s_ * 0.072, 0, ankle_z))
        J[f"hip.{tag}"], J[f"kn.{tag}"], J[f"an.{tag}"] = hip, kn, an
        J[f"toe.{tag}"] = Vector((an.x, -0.175, 0.025))

        leg_ob = BC.loft(f"Leg_{tag}", [
            (an.z,                         0.037, 0.039, 0),
            (an.z + (kn.z - an.z) * 0.30, 0.042, 0.044, 0),
            (an.z + (kn.z - an.z) * 0.75, 0.048, 0.050, 0),
            (kn.z,                         0.051, 0.053, 0),
            (kn.z + (hip.z - kn.z) * 0.35, 0.058, 0.060, 0),
            (kn.z + (hip.z - kn.z) * 0.75, 0.067, 0.069, 0),
            (hip.z + 0.030,                0.072, 0.074, 0),
        ], ring=20)
        leg_ob.location.x = (hip.x + kn.x) / 2
        parts.append(set_material(leg_ob, materials["Trousers"]))

        shoe = BC.loft(f"Shoe_{tag}", [
            (0.000, 0.038, 0.112, -0.044),
            (0.018, 0.045, 0.126, -0.048),
            (0.042, 0.048, 0.120, -0.042),
            (0.072, 0.044, 0.094, -0.024),
            (0.100, 0.038, 0.064, -0.008),
            (0.116, 0.033, 0.046, 0.000),
        ], ring=22)
        shoe.location.x = an.x
        parts.append(set_material(shoe, materials["Shoe"]))

    return dict(torso_hw=torso_hw, torso_hd=torso_hd, sh_half=0.163,
                chest_z=chest_z, hip_z=hip_z)


# --------------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    a = ap.parse_args()

    report = {"status": "running", "steps": [], "character": "Professor Z",
              "source": "character sheet front view, measured", "warnings": []}
    BC.reset()
    try:
        bpy.ops.preferences.addon_enable(module="cycles")
    except Exception:
        pass

    materials = make_materials()

    head_parts, body_parts = [], []
    skull = build_skull(materials)
    head_parts.append(skull)
    face = build_face(materials, head_parts)
    build_hair(materials, head_parts)
    J = {}
    geo = build_body(materials, body_parts, J)

    # ---- island split: arms are their own shell, as in the shared pipeline ----
    def is_arm(o):
        return o.name.startswith(("Deltoid", "Sleeve", "Cuff",
                                  "Palm", "Finger", "Knuckle", "Thumb"))

    core = [skull] + body_parts
    arms = {"L": [], "R": []}
    for o in head_parts[1:] + body_parts:
        if is_arm(o):
            arms["L" if o.matrix_world.translation.x > 0 else "R"].append(o)

    core = [o for o in ([skull] + head_parts[1:] + body_parts) if not is_arm(o)]

    def join_group(objs, name):
        bpy.ops.object.select_all(action="DESELECT")
        for o in objs:
            o.select_set(True)
        bpy.context.view_layer.objects.active = objs[0]
        bpy.ops.object.join()
        ob = bpy.context.object
        ob.name = name
        return ob

    core_ob = join_group(core, "PZ_core")
    arm_ob = {}
    for side in ("L", "R"):
        if arms[side]:
            arm_ob[side] = join_group(arms[side], f"PZ_arm_{side}")

    # Join arms into the core one at a time so the vertex ranges are deterministic:
    # a join appends the other objects' vertices after the active object's.
    island_ranges, off = [], 0
    n_core = len(core_ob.data.vertices)
    island_ranges.append(("core", 0, n_core))
    off = n_core
    for side in ("L", "R"):
        ob = arm_ob.get(side)
        if not ob:
            continue
        n = len(ob.data.vertices)
        bpy.ops.object.select_all(action="DESELECT")
        ob.select_set(True)
        core_ob.select_set(True)
        bpy.context.view_layer.objects.active = core_ob
        bpy.ops.object.join()
        island_ranges.append((f"arm.{side}", off, off + n))
        off += n

    body = bpy.context.object
    body.name = "Body"
    body.data.name = "ProfessorZ"
    bpy.ops.object.shade_smooth()
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.normals_make_consistent(inside=False)
    bpy.ops.object.mode_set(mode="OBJECT")

    # feet on the floor
    zmin = min(v.co.z for v in body.data.vertices)
    for v in body.data.vertices:
        v.co.z -= zmin
    for k in J:
        J[k] = J[k] + Vector((0, 0, -zmin))
    cz = HEAD_CZ - zmin

    report["face"] = {k: round(v, 4) for k, v in face.items()}
    report["geo"] = {k: round(v, 4) for k, v in geo.items()}
    report["islands"] = island_ranges

    M = dict(head_h=HH, sh_w=geo["sh_half"] * 2, hip_y=J["hip.L"].z,
             chest_z=geo["chest_z"] - zmin, neck_z=zf(F["neck_base"]) - zmin,
             shoulder_z=zf(F["shoulder"]) - zmin, leg_len=J["hip.L"].z,
             head_center_z=cz, HD=dict(hw=HW, hd=HD, cz=cz, head_h=HH),
             joints=J, arm_len=0.30, silhouette=[], island_ranges=island_ranges,
             part_labels=[])

    arm = BC.build_armature(M, report)
    BC.build_fingers(arm, M, report)
    BC.add_shape_keys(body, M["HD"], report, landmarks={
        "mouth_z": (face["mouth_z"] - cz) / HH,
        "mouth_w": 0.34,
        "brow_z": (face["eye_z"] + face["eye_r"] * 1.62 - cz) / HH,
        "brow_w": 0.34,
        "brow_x": face["eye_x"] / HH,
        "eye_z": (face["eye_z"] - cz) / HH,
        "eye_x": face["eye_x"] / HH,
        "lid_z": (face["eye_z"] - cz) / HH,
        "jaw_top": (face["mouth_z"] - cz) / HH + 0.07,
        "jaw_y": 0.10,
    })
    body.vertex_groups.clear()
    BC.weight_by_anatomy(body, arm, M, report, island_ranges=island_ranges)

    report["body"] = {
        "verts": len(body.data.vertices), "polys": len(body.data.polygons),
        "dimensions": [round(v, 3) for v in body.dimensions],
        "height_m": round(max(v.co.z for v in body.data.vertices), 3),
        "head_unit_m": round(HH, 4),
    }
    report["materials"] = sorted({m.name for m in body.data.materials if m})
    report["totals"] = {
        "objects": len(bpy.data.objects),
        "verts": sum(len(o.data.vertices) for o in bpy.data.objects if o.type == "MESH"),
        "polys": sum(len(o.data.polygons) for o in bpy.data.objects if o.type == "MESH"),
        "bones": len(arm.data.bones)}
    reset_pose(arm)

    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out))
    report["status"] = "ok"
    report["blend"] = str(out)
    report["blend_bytes"] = out.stat().st_size
    pathlib.Path(a.report).write_text(json.dumps(report, indent=2))
    print(json.dumps({"status": "ok", "totals": report["totals"],
                      "height_m": report["body"]["height_m"],
                      "materials": report["materials"],
                      "dead_morphs": report.get("shape_key_dead")}, indent=2))


if __name__ == "__main__":
    main()
