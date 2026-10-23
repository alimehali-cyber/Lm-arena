#!/usr/bin/env python3
"""
PROFESSOR Z - built to the supplied turnaround (front / side / back).

Every number below is read off the sheet. The front view is 797 px tall from the hair
crown (y=168) to the sole (y=964); with the figure at 1.70 m the scale is

    S = 1.70 / 797 = 0.002133 m/px        z(y) = (964 - y) * S

LANDMARKS (sheet pixels -> metres)
    hair crown   168 -> 1.698      eye line    278.5 -> 1.462
    skull top    225 -> 1.576      nose tip    301   -> 1.414
    brow         255 -> 1.512      mouth       314   -> 1.386
    chin         340 -> 1.331      collar      353   -> 1.303
    shoulder top 425 -> 1.150      armpit      470   -> 1.054
    tee hem      596 -> 0.785      crotch      660   -> 0.648
    knee         755 -> 0.446      cuff        858   -> 0.226
    sole         964 -> 0.000

    head width 0.200, head height 0.245, ear span 0.275, hair width 0.303,
    shoulder span (sleeve to sleeve) 0.490, torso 0.288, hip 0.331
    eye width 0.040, pupil separation 0.083, mouth width 0.068

That is a 4.6-head stylised youth: a big head, a short thick neck, narrow shoulders
carrying a loose tee, skinny jeans and chunky high-tops.

WHY THE FIRST ATTEMPT LOOKED WRONG - so it is not repeated
    It was a tweed-suited old man assembled from spheres and slabs, painted by a nearest
    label pass after a voxel remesh. This version shapes the surface instead:
      * the head is ONE lofted surface with independent front/back radii, then displaced
        by smooth fields (sockets, brow ridge, cheekbones, jaw, chin, nose);
      * the eyes sit in those sockets under real lids, with an iris, a pupil and a
        catch-light - not a ball with a dot;
      * limbs are swept tubes with a radius curve and a frame transported along them;
      * hair is a shell with the face cut out plus ~130 small tori: a curl is a ring;
      * every part carries its own material from birth - no paint-by-nearest-label.

Run:
  .venv/bin/python blender/build_professor_z.py \
      --out working/profz/character.blend --report working/profz/03_build.json
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import random
import sys

import bpy
from mathutils import Vector

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import build_character as BC          # noqa: E402
import char_lib as C                  # noqa: E402
from scene_utils import reset_pose    # noqa: E402

H = 1.70
S = H / 797.0


def z(y_px: float) -> float:
    """Height in metres for a row of the sheet."""
    return (964.0 - y_px) * S


def w(px: float) -> float:
    return px * S


Z = {
    "hair_top": z(168), "skull_top": z(225), "brow": z(255), "eye": z(278.5),
    "nose": z(301), "mouth": z(314), "chin": z(340), "neck": z(353),
    "shoulder_top": z(425), "armpit": z(470), "chest": z(500), "waist": z(560),
    "hem": z(596), "hip": z(620), "crotch": z(660), "knee": z(755),
    "cuff_top": z(858), "cuff_bot": z(890), "ankle": z(900),
}

HEAD_CZ = 1.4537           # (chin 1.331 + crown 1.576) / 2
HEAD_HH = 0.1290
# Measured again, this time against the SKIN, not the silhouette: the face is 108 px
# across at the cheekbones (0.230 m) and the hair only 138 px (0.294 m). The first pass
# used 94 px for the head, which made it too narrow and every feature on it too big.
HEAD_HW = 0.0965
HEAD_HD = 0.1105

EYE_X = 0.0362
EYE_R = 0.0222
IRIS_R = 0.0134
PUPIL_R = 0.0060
EYE_Z = 1.4615
MOUTH_HW = 0.0338
MOUTH_Z = 1.3765        # crescent: corners y≈306 (z 1.404), centre y≈319
NOSE_HW = 0.0235
BROW_HW = 0.0272
BROW_X = 0.0452
BROW_Z = 1.5075

PALETTE = {
    "Skin":     ("#DE9C63", 0.62, 0.0, None, 0.0),
    "SkinDeep": ("#B26C3C", 0.64, 0.0, None, 0.0),
    "Hair":     ("#1A1411", 0.58, 0.0, None, 0.0),
    "Brow":     ("#1E1713", 0.52, 0.0, None, 0.0),
    "EyeWhite": ("#F7F1E9", 0.18, 0.0, None, 0.0),
    "Iris":     ("#6B3C1C", 0.20, 0.0, None, 0.0),
    "Pupil":    ("#080606", 0.16, 0.0, None, 0.0),
    "Lips":     ("#C0705C", 0.42, 0.0, None, 0.0),
    "Tee":      ("#17181A", 0.88, 0.0, None, 0.0),
    "TeeDark":  ("#0D0E10", 0.88, 0.0, None, 0.0),
    "Denim":    ("#1D2128", 0.82, 0.0, None, 0.0),
    "Cuff":     ("#5A626C", 0.86, 0.0, None, 0.0),
    "Canvas":   ("#131416", 0.80, 0.0, None, 0.0),
    "Sole":     ("#EAE5DB", 0.56, 0.0, None, 0.0),
    "Lace":     ("#F3EEE5", 0.70, 0.0, None, 0.0),
    "Gold":     ("#C9A24A", 0.26, 1.0, None, 0.0),
    "Watch":    ("#0F0F11", 0.34, 0.25, None, 0.0),
    "ZGlow":    ("#8E6BFF", 0.30, 0.0, "#8E6BFF", 1.35),
    "ZCore":    ("#33207A", 0.30, 0.0, "#4B33B8", 0.55),
}


def materials():
    return {n: C.material(n, c, r, m, e, s) for n, (c, r, m, e, s) in PALETTE.items()}


# --------------------------------------------------------------------------- head
# (t from chin to crown, half width, front depth, back depth) as fractions
HEAD_PROF = [
    (0.000, 0.30, 0.60, 0.52),
    (0.055, 0.44, 0.74, 0.68),
    (0.120, 0.60, 0.84, 0.82),
    (0.215, 0.80, 0.92, 0.91),
    (0.340, 0.91, 0.97, 0.97),
    (0.480, 0.96, 0.99, 1.00),
    (0.620, 1.00, 1.00, 1.00),
    (0.760, 0.995, 0.97, 0.99),
    (0.870, 0.930, 0.90, 0.94),
    (0.945, 0.780, 0.76, 0.80),
    (1.000, 0.430, 0.42, 0.44),
]


def head_t(z_abs):
    """Parameter t in [0,1] for an absolute height, clamped."""
    t = (z_abs - Z["chin"]) / (Z["skull_top"] - Z["chin"])
    return max(0.0, min(1.0, t))


def head_radii(t):
    for i in range(len(HEAD_PROF) - 1):
        t0, w0, f0, b0 = HEAD_PROF[i]
        t1, w1, f1, b1 = HEAD_PROF[i + 1]
        if t0 <= t <= t1:
            u = (t - t0) / (t1 - t0)
            return (HEAD_HW * (w0 + (w1 - w0) * u),
                    HEAD_HD * (f0 + (f1 - f0) * u),
                    HEAD_HD * (b0 + (b1 - b0) * u))
    return HEAD_HW, HEAD_HD, HEAD_HD


def face_point(x, z_abs, out=0.0):
    """
    The front surface of the head at (x, z_abs), pushed `out` metres forward.

    Every facial feature is placed through this. The first build placed the mouth at a
    fixed y and it ended up INSIDE the head, invisible in every render - the mouth is
    10 mm behind the cheek surface at that height.
    """
    t = head_t(z_abs)
    rx, ryf, _ = head_radii(t)
    k = max(0.0, 1.0 - (x / max(rx, 1e-6)) ** 2)
    y = -ryf * math.sqrt(k)
    return Vector((x, y - out, z_abs))


def build_head(mat, parts):
    rings = []
    for (t, fw, ff, fb) in HEAD_PROF:
        rings.append((Z["chin"] + (Z["skull_top"] - Z["chin"]) * t,
                      HEAD_HW * fw, HEAD_HD * ff, HEAD_HD * fb, 0.0))
    ob = C.egg_loft("Head", rings, ring=56)
    c = Vector((0, 0, HEAD_CZ))
    C.sculpt(ob, [
        # Broad fields only: sigma >= 0.03, or the Gaussian lands on single vertices
        # and the surface grows spikes (the nose came out as an M-shaped ridge).
        (+EYE_X, -0.092, EYE_Z + 0.004, 0.030, -0.0035),
        (-EYE_X, -0.092, EYE_Z + 0.004, 0.030, -0.0035),
        (+BROW_X, -0.098, BROW_Z - 0.002, 0.036, +0.0050),
        (-BROW_X, -0.098, BROW_Z - 0.002, 0.036, +0.0050),
        (0.0, -0.090, EYE_Z + 0.080, 0.060, +0.0030),
        (+0.070, -0.078, Z["nose"] - 0.010, 0.046, +0.0065),
        (-0.070, -0.078, Z["nose"] - 0.010, 0.046, +0.0065),
        (+0.062, -0.086, Z["mouth"] + 0.044, 0.040, +0.0030),
        (-0.062, -0.086, Z["mouth"] + 0.044, 0.040, +0.0030),
        (+0.078, -0.030, Z["chin"] + 0.046, 0.038, +0.0022),
        (-0.078, -0.030, Z["chin"] + 0.046, 0.038, +0.0022),
        (0.0, -0.090, Z["chin"] + 0.018, 0.034, +0.0070),
    ], centre=c)
    C.shade_smooth(ob)
    parts.append(C.apply_material(ob, mat["Skin"]))
    return ob


def build_ears(mat, parts):
    for s, tag in ((1, "L"), (-1, "R")):
        shell = C.sphere(f"Ear_{tag}", (s * 0.1000, 0.014, 1.4185),
                         (0.0175, 0.0155, 0.0255), seg=22, ring=16,
                         rot=(0, math.radians(-8 * s), math.radians(10 * s)))
        C.shade_smooth(shell)
        parts.append(C.apply_material(shell, mat["Skin"]))
        bowl = C.sphere(f"EarBowl_{tag}", (s * 0.1120, 0.013, 1.4135),
                        (0.0050, 0.0085, 0.0140), seg=16, ring=12)
        parts.append(C.apply_material(bowl, mat["Skin"]))
        rim = C.torus(f"EarRim_{tag}", (s * 0.1100, 0.014, 1.4200), 0.0170, 0.0026,
                      rot=(0, math.radians(90), math.radians(4 * s)),
                      major=18, minor=6, arc=math.pi * 1.15)
        parts.append(C.apply_material(rim, mat["Skin"]))


def build_face(mat, parts):
    import bmesh

    # The socket is sculpted 7.1 mm deep, so the surface there is at face_point + 7.1 mm.
    # The eyeball's front pole is placed 1.5 mm proud of that plane: enough to read as an
    # eye, not enough to bulge. Placing it at a fixed y put it behind the skin.
    SOCKET_DEPTH = 0.0071
    for s, tag in ((1, "L"), (-1, "R")):
        socket_y = face_point(s * EYE_X, EYE_Z).y + SOCKET_DEPTH
        centre = Vector((s * EYE_X, socket_y - 0.0015 + EYE_R * 0.86, EYE_Z))

            # ---- eye: an almond disc set 3 mm proud of the face, with the iris and pupil
        # on its front. A sphere pushed into a socket was tried first; the skin plane is
        # curved, the socket is not deep enough for a sphere, and only a sliver of white
        # showed. A shallow almond is how the reference itself is drawn.
        surf = face_point(s * EYE_X, EYE_Z)
        eye_y = surf.y - 0.0030
        white = C.superellipsoid(f"Eye_{tag}", (s * EYE_X, eye_y, EYE_Z),
                                 (EYE_R * 1.06, 0.0075, EYE_R * 0.80), n=2.15,
                                 seg=32, rings=18)
        C.shade_smooth(white)
        parts.append(C.apply_material(white, mat["EyeWhite"]))
        iris = C.superellipsoid(f"Iris_{tag}", (s * EYE_X, eye_y - 0.0060, EYE_Z - 0.0022),
                                (IRIS_R, 0.0035, IRIS_R), n=2.1, seg=26, rings=16)
        C.shade_smooth(iris)
        parts.append(C.apply_material(iris, mat["Iris"]))
        pupil = C.sphere(f"Pupil_{tag}", (s * EYE_X, eye_y - 0.0086, EYE_Z - 0.0022),
                         (PUPIL_R, 0.0022, PUPIL_R), seg=20, ring=12)
        parts.append(C.apply_material(pupil, mat["Pupil"]))
        spark = C.sphere(f"Spark_{tag}", (s * EYE_X + IRIS_R * 0.40,
                                          eye_y - 0.0098, EYE_Z + IRIS_R * 0.42),
                         (0.0032, 0.0016, 0.0032), seg=12, ring=8)
        parts.append(C.apply_material(spark, mat["EyeWhite"]))

        # upper lid: a heavy skin hood over the top of the almond
        hood = C.superellipsoid(f"Lid_{tag}", (s * EYE_X, eye_y - 0.0012,
                                               EYE_Z + EYE_R * 0.74),
                                (EYE_R * 1.14, 0.0078, EYE_R * 0.46), n=2.3,
                                seg=30, rings=16)
        C.shade_smooth(hood)
        parts.append(C.apply_material(hood, mat["Skin"]))

        # lash line, following the upper lid edge
        lash = C.torus(f"Lash_{tag}", (s * EYE_X, eye_y - 0.0048, EYE_Z + EYE_R * 0.14),
                       EYE_R * 1.01, 0.0018, rot=(math.radians(90), 0, 0),
                       major=24, minor=6, arc=math.pi)
        parts.append(C.apply_material(lash, mat["Hair"]))

        # ---- brow: thick, straight, on the face surface
        pts = []
        for u in (0.0, 0.32, 0.66, 1.0):
            x = s * (BROW_X - BROW_HW + 2 * BROW_HW * u)
            # the sheet's brows are nearly flat, dipping only at the outer end
            # inner end low, gentle peak two thirds out, thin tail: the sheet's brow
            zz = BROW_Z + 0.0040 * u + 0.0058 * math.sin(math.pi * u) - 0.0055 * u * u
            pts.append(face_point(x, zz, out=0.0042))
        brow = C.tube(f"Brow_{tag}", pts, [0.0068, 0.0100, 0.0092, 0.0036],
                      ring=12, samples=6, squash=[(1.0, 0.52)] * 4)
        C.shade_smooth(brow)
        parts.append(C.apply_material(brow, mat["Hair"]))

        # ---- glasses: thin gold round rim
        rim_r = 0.0368
        rim = C.torus(f"Rim_{tag}", (centre.x, -0.1085, EYE_Z), rim_r, 0.0024,
                      rot=(math.radians(90), 0, 0), major=34, minor=7)
        parts.append(C.apply_material(rim, mat["Gold"]))
        inner = s * (EYE_X - rim_r * 0.98)
        outer = s * (EYE_X + rim_r * 0.98)
        parts.append(C.apply_material(
            C.cylinder(f"Temple_{tag}", (outer, -0.1050, EYE_Z + 0.004),
                       (s * 0.1155, 0.020, EYE_Z + 0.008), 0.0017, 0.0017, verts=8),
            mat["Gold"]))
    parts.append(C.apply_material(
        C.cylinder("Bridge", (-0.0045, -0.1085, EYE_Z + 0.004),
                   (0.0045, -0.1085, EYE_Z + 0.004), 0.0019, 0.0019, verts=8),
        mat["Gold"]))

    # ---- nose: a small soft wedge on the surface, tip at Z["nose"]
    nose_pts = [face_point(0.0, Z["nose"] + 0.048, out=-0.006),
                face_point(0.0, Z["nose"] + 0.022, out=0.002),
                face_point(0.0, Z["nose"] + 0.004, out=0.006)]
    nose = C.tube("NoseBridge", nose_pts, [0.0050, 0.0078, 0.0105], ring=12, samples=5,
                  squash=[(1.0, 0.75)] * 3)
    C.shade_smooth(nose)
    parts.append(C.apply_material(nose, mat["Skin"]))
    tip = C.sphere("NoseTip", face_point(0.0, Z["nose"] + 0.003, out=0.0086),
                   (NOSE_HW * 0.92, 0.0125, 0.0098), seg=22, ring=14)
    C.shade_smooth(tip)
    parts.append(C.apply_material(tip, mat["Skin"]))
    for s in (-1, 1):
        parts.append(C.apply_material(
            C.sphere(f"Nostril_{s}", face_point(s * 0.0115, Z["nose"] - 0.0080, out=0.0020),
                     (0.0034, 0.0028, 0.0024), seg=10, ring=8), mat["Skin"]))

    # ---- mouth: a lens-shaped patch laid on the face, edge to edge.
    # A tube following the smile curve broke into pieces at the corners: the cheek turns
    # away faster than a tube of constant radius can follow.
    def smile_curve(u):
        zz = MOUTH_Z + 0.0230 * (u * u)
        return face_point(MOUTH_HW * u, zz, out=0.0)

    mouth = C.conformal_patch("Smile", smile_curve,
                              lambda u: 0.0034 * (1.0 - abs(u) ** 2.2) ** 0.55 + 0.0007,
                              lambda u, v: 0.0050,
                              u_steps=34, v_steps=8)
    C.shade_smooth(mouth)
    parts.append(C.apply_material(mouth, mat["Lips"]))

    # lower lip: a shallow pad under the centre of the smile
    def lip_curve(u):
        zz = MOUTH_Z - 0.0085 + 0.0190 * (u * u)
        return face_point(MOUTH_HW * u * 0.86, zz, out=0.0)

    low = C.conformal_patch("LowerLip", lip_curve,
                            lambda u: 0.0032 * (1.0 - abs(u) ** 2.0) ** 0.6 + 0.0006,
                            lambda u, v: 0.0048,
                            u_steps=30, v_steps=8)
    C.shade_smooth(low)
    parts.append(C.apply_material(low, mat["Lips"]))


# --------------------------------------------------------------------------- hair
def hair_allowed(p):
    """The hair volume: open over the face, solid back, sides above the ears."""
    x, y, zz = p.x, p.y, p.z
    if y > 0.020:
        return zz > 1.352                    # nape
    if abs(x) > 0.090:
        return zz > 1.492                    # above the ears
    # front: the fringe line, dipping at the temples
    edge = (1.5275 - 0.022 * min(1.0, (abs(x) / 0.100) ** 2)
            + 0.0050 * math.sin(41.0 * x) + 0.0032 * math.sin(74.0 * x + 1.7))
    return zz > edge


def build_hair(mat, parts, seed=11):
    rnd = random.Random(seed)
    import bmesh

    # Hair radius by height, measured off the sheet's silhouette mask. The mop is a
    # MUSHROOM: widest at z 1.56 (0.147 half) and narrower at the ears (0.128), which is
    # the opposite of the ellipsoid used before - that one was 24 px narrow at the crown
    # and 18 px wide at the temples at the same time.
    HAIR_PROFILE = [
        (1.316, 0.006), (1.336, 0.062), (1.366, 0.086), (1.396, 0.091),
        (1.426, 0.103), (1.456, 0.114), (1.486, 0.120), (1.516, 0.123),
        (1.546, 0.126), (1.576, 0.127), (1.606, 0.108), (1.636, 0.076),
        (1.661, 0.045), (1.681, 0.021), (1.694, 0.006),
    ]

    def hair_r(zz):
        for i in range(len(HAIR_PROFILE) - 1):
            z0, r0 = HAIR_PROFILE[i]
            z1, r1 = HAIR_PROFILE[i + 1]
            if z0 <= zz <= z1:
                return r0 + (r1 - r0) * (zz - z0) / (z1 - z0)
        return 0.0

    cy = 0.012
    rings = []
    n = 34
    z_lo, z_hi = HAIR_PROFILE[0][0], HAIR_PROFILE[-1][0]
    for i in range(n + 1):
        zz = z_lo + (z_hi - z_lo) * i / n
        r = hair_r(zz)
        if r <= 0.004:
            continue
        rings.append((zz, r, r * 0.97, r * 0.97, cy))
    shell = C.egg_loft("HairShell", [r for r in rings if r[1] > 0.003], ring=56)
    bm = bmesh.new()
    bm.from_mesh(shell.data)
    # Break the dome: a smooth shell under the curls read as a swim cap. Every vertex is
    # pushed out by a small random amount so the mass itself is lumpy.
    bm.normal_update()
    for v in bm.verts:
        n = v.normal.copy()
        h = hash((round(v.co.x, 4), round(v.co.y, 4), round(v.co.z, 4), 7)) % 1000
        v.co += n * (0.0010 + 0.0055 * (h / 999.0))
    kill = []
    for f in bm.faces:
        c = sum((v.co for v in f.verts), Vector()) / len(f.verts)
        if not hair_allowed(c):
            kill.append(f)
    bmesh.ops.delete(bm, geom=kill, context="FACES")
    bm.to_mesh(shell.data)
    bm.free()
    C.shade_smooth(shell)
    parts.append(C.apply_material(shell, mat["Hair"]))

    placed = 0
    golden = math.pi * (3.0 - math.sqrt(5.0))
    count = 430
    for i in range(count):
        yy = 1.0 - (i / (count - 1)) * 2.0
        rad = math.sqrt(max(0.0, 1.0 - yy * yy))
        theta = golden * i
        # horizontal direction on the surface, normalised: using the raw (sin, cos) of the
        # sphere shortened the radius by sqrt(1-z^2), which sank every curl in the upper
        # half into the shell and left a smooth dome.
        h = Vector((math.cos(theta), math.sin(theta) * 0.97, 0.0)).normalized()
        zz = 1.500 + yy * 0.185
        r = hair_r(zz)
        if r < 0.035:
            continue
        slope = (hair_r(min(zz + 0.004, 1.695)) - hair_r(max(zz - 0.004, 1.317))) / 0.008
        n = Vector((h.x, h.y, -slope)).normalized()
        sc = rnd.uniform(1.05, 1.70) if i % 3 == 0 else rnd.uniform(0.60, 1.05)
        crad = 0.0150 * sc
        pos = Vector((h.x, h.y, 0.0)) * r + Vector((0.0, cy, zz))
        if not hair_allowed(pos):
            continue
        pos += n * (0.0030 + 0.40 * crad + rnd.uniform(0.0, 0.0035))
        # More than a full turn: the coil closes on itself into a twisted lock instead of
        # reading as an open ring, which is what a single turn of tube looked like.
        curl = C.coil(f"Curl{i}", pos, n, radius=crad, thick=0.0064 * sc,
                      turns=rnd.uniform(1.55, 2.10), phase=rnd.uniform(0, math.pi * 2),
                      ring=5, rise=0.0009)
        C.shade_smooth(curl)
        parts.append(C.apply_material(curl, mat["Hair"]))
        placed += 1

    for i, (x, y, zz, sc) in enumerate([
            (-0.092, -0.078, 1.528, 0.95), (-0.056, -0.104, 1.552, 1.05),
            (-0.018, -0.116, 1.560, 1.10), (0.022, -0.114, 1.556, 1.05),
            (0.062, -0.100, 1.545, 1.00), (0.094, -0.070, 1.520, 0.92)]):
        curl = C.coil(f"Fringe{i}", (x, y, zz), (0.0, -0.88, 0.47),
                      radius=0.0110 * sc, thick=0.0052 * sc, turns=1.25,
                      phase=rnd.uniform(0, 6.28), ring=5, rise=0.0012)
        C.shade_smooth(curl)
        parts.append(C.apply_material(curl, mat["Hair"]))
        placed += 1

    for s in (-1, 1):
        parts.append(C.apply_material(
            C.sphere(f"Sideburn_{s}", (s * 0.1045, -0.018, 1.4820),
                     (0.0085, 0.0150, 0.0210), seg=14, ring=10), mat["Hair"]))
    return placed


# --------------------------------------------------------------------------- body
def build_body(mat, parts, J):
    # ---- neck: SHORT. The sheet's neckline is at y=352 (z=1.306) and the chin at
    # y=340 (z=1.331), so only 2.5 cm of the neck is visible. The first build put the
    # neckline at the shoulder line, 16 cm lower, and the character was a giraffe.
    neck = C.tube("Neck", [(0, 0.006, 1.268), (0, 0.006, 1.305), (0, 0.004, 1.338)],
                  [0.0455, 0.0445, 0.0440], ring=20, samples=4)
    C.shade_smooth(neck)
    parts.append(C.apply_material(neck, mat["Skin"]))

    # ---- tee
    # The tee is the whole torso: shoulders at 1.15, neckline at 1.306, armpit at 1.054.
    # Its widest point (0.152 half-width) is the deltoid, where the sleeve begins.
    torso = C.egg_loft("Tee", [
        (0.778, 0.1660, 0.1195, 0.1175, 0.000),
        (0.806, 0.1600, 0.1160, 0.1145, 0.000),
        (0.850, 0.1530, 0.1125, 0.1110, 0.000),
        (0.900, 0.1490, 0.1105, 0.1090, 0.000),
        (0.950, 0.1475, 0.1105, 0.1090, 0.000),
        (1.010, 0.1470, 0.1125, 0.1110, 0.000),
        (1.055, 0.1480, 0.1130, 0.1115, 0.000),
        (1.100, 0.1500, 0.1120, 0.1105, 0.000),
        (1.150, 0.2140, 0.1300, 0.1285, 0.000),
        (1.195, 0.2100, 0.1290, 0.1275, 0.001),
        (1.235, 0.1920, 0.1200, 0.1185, 0.002),
        (1.252, 0.1860, 0.1180, 0.1165, 0.003),
        (1.272, 0.1700, 0.1110, 0.1095, 0.003),
        (1.284, 0.1520, 0.1030, 0.1015, 0.004),
        (1.292, 0.1260, 0.0920, 0.0905, 0.004),
        (1.298, 0.0950, 0.0790, 0.0780, 0.005),
        (1.303, 0.0620, 0.0590, 0.0580, 0.005),
        (1.307, 0.0445, 0.0440, 0.0440, 0.006),
    ], ring=40)
    C.shade_smooth(torso)
    parts.append(C.apply_material(torso, mat["Tee"]))

    collar = C.torus("Collar", (0, 0.006, 1.3055), 0.0450, 0.0052, rot=(0, 0, 0),
                     major=28, minor=8)
    collar.scale = (1.0, 0.96, 1.0)
    parts.append(C.apply_material(collar, mat["Canvas"]))

    hem = C.torus("Hem", (0, 0, 0.7785), 0.1620, 0.0045, rot=(0, 0, 0), major=34, minor=8)
    hem.scale = (1.02, 0.72, 1.0)
    parts.append(C.apply_material(hem, mat["Canvas"]))

    # ---- glowing Z on the chest
    zc = 0.940
    front = -0.1130
    for tag, k, key, depth in (("Outer", 1.00, "ZGlow", 0.0028),
                               ("Core", 0.86, "ZCore", 0.0042)):
        parts.append(C.apply_material(
            C.superellipsoid(f"ZTop_{tag}", (0, front - depth, zc + 0.082),
                             (0.1250 * k, 0.0035, 0.0190 * k), n=3.0, seg=24, rings=12),
            mat[key]))
        parts.append(C.apply_material(
            C.superellipsoid(f"ZBot_{tag}", (0, front - depth, zc - 0.082),
                             (0.1250 * k, 0.0035, 0.0190 * k), n=3.0, seg=24, rings=12),
            mat[key]))
        parts.append(C.apply_material(
            C.superellipsoid(f"ZDiag_{tag}", (0, front - depth, zc),
                             (0.1460 * k, 0.0035, 0.0175 * k), n=3.0, seg=24, rings=12,
                             rot=(0, math.radians(-44), 0)),
            mat[key]))

    # ---- arms
    for s, tag in ((1, "L"), (-1, "R")):
        # Measured from the width profile: the widest point of the whole figure is
        # 236 px at z = 0.99 (the sleeve hem), then the bare arm angles IN to 179 px at
        # z = 0.83 and the hand disappears into the front pocket.
        sh = Vector((s * 0.150, 0.0, 1.148))
        el = Vector((s * 0.206, 0.008, 0.930))
        wr = Vector((s * 0.162, -0.010, 0.802))
        hd = Vector((s * 0.132, -0.052, 0.786))
        J[f"sh.{tag}"], J[f"el.{tag}"], J[f"wr.{tag}"], J[f"hd.{tag}"] = sh, el, wr, hd

        # The sleeve caps the deltoid: its top is at the shoulder (z 1.15), not at the
        # collar. Starting it at 1.24 made the shoulders 43 px too wide at the neck.
        # Sloped, broad shoulders: 0.192 half-width at z 1.24, 0.244 at z 1.09.
        # A tube's flat end cap is a disc: at the shoulder it sticks out as a triangular
        # fin. The sleeve is now a rounded stub - small radius at the top, widest at the
        # deltoid, tapering to the hem.
        # A tube whose FIRST ring is already wide turns its flat end cap into a triangular
        # fin above the shoulder - that is exactly the wedge that showed on both sides.
        # Start inside the torso with a small radius and let it swell at the deltoid.
        sl = [(s * 0.060, 0.000, 1.246), (s * 0.110, 0.001, 1.226),
              (s * 0.152, 0.002, 1.178), (s * 0.184, 0.003, 1.110),
              (s * 0.190, 0.004, 1.045)]
        sleeve = C.tube(f"Sleeve_{tag}", sl,
                        [0.020, 0.052, 0.062, 0.058, 0.052], ring=24, samples=4)
        C.shade_smooth(sleeve)
        parts.append(C.apply_material(sleeve, mat["Tee"]))
        # hem band, aligned to the sleeve's own axis instead of a guessed angle
        d = (Vector(sl[-1]) - Vector(sl[-2])).normalized()
        parts.append(C.apply_material(
            C.torus(f"SleeveCuff_{tag}", sl[-1], 0.0535, 0.0036,
                    rot=(0, math.atan2(d.x, d.z), 0), major=22, minor=6),
            mat["Canvas"]))

        arm = C.tube(f"Arm_{tag}",
                     [(s * 0.188, 0.0, 1.070), (s * 0.204, 0.002, 1.000),
                      (s * 0.196, 0.004, 0.930), (s * 0.172, 0.000, 0.862),
                      (s * 0.160, -0.006, 0.800)],
                     [0.0400, 0.0420, 0.0380, 0.0330, 0.0290], ring=20, samples=6)
        C.shade_smooth(arm)
        parts.append(C.apply_material(arm, mat["Skin"]))

        # ---- hand: a rounded fist tucked at the pocket mouth. The sheet hides both
        # hands in the pockets; modelling fingers put a claw in front of the thigh.
        palm_c = wr + Vector((-s * 0.004, 0.004, -0.030))
        fist = C.sphere(f"Palm_{tag}", palm_c, (0.0205, 0.0205, 0.0270), seg=20, ring=14)
        C.shade_smooth(fist)
        parts.append(C.apply_material(fist, mat["Skin"]))
        thumb = C.sphere(f"Thumb_{tag}", palm_c + Vector((s * 0.014, -0.012, 0.004)),
                         (0.0090, 0.0105, 0.0125), seg=14, ring=10)
        C.shade_smooth(thumb)
        parts.append(C.apply_material(thumb, mat["Skin"]))

        # watch: left wrist only, on the outside of the cuff, facing front
        if tag == "L":
            band = C.torus("WatchBand", (wr.x, wr.y, wr.z + 0.030), 0.0262, 0.0056,
                           rot=(0, math.radians(2), 0), major=20, minor=7)
            parts.append(C.apply_material(band, mat["Canvas"]))
            parts.append(C.apply_material(
                C.cylinder("WatchCase", (wr.x, wr.y - 0.0290, wr.z + 0.033),
                           (wr.x, wr.y - 0.0190, wr.z + 0.033), 0.0172, 0.0172, verts=22),
                mat["Canvas"]))
            parts.append(C.apply_material(
                C.cylinder("WatchFace", (wr.x, wr.y - 0.0305, wr.z + 0.033),
                           (wr.x, wr.y - 0.0288, wr.z + 0.033), 0.0138, 0.0138, verts=22),
                mat["Iris"]))

    # ---- hips + jeans
    pelvis = C.egg_loft("Hips", [
        (0.630, 0.1040, 0.0900, 0.0880, 0.0),
        (0.660, 0.1220, 0.0980, 0.0960, 0.0),
        (0.700, 0.1440, 0.1080, 0.1060, 0.0),
        (0.730, 0.1550, 0.1150, 0.1130, 0.0),
        (0.770, 0.1540, 0.1140, 0.1120, 0.0),
        (0.800, 0.1400, 0.1080, 0.1060, 0.0),
    ], ring=36)
    C.shade_smooth(pelvis)
    parts.append(C.apply_material(pelvis, mat["Denim"]))

    # fly seam and back pockets: geometry, because a flat colour shows nothing
    fly = C.tube("Fly", [(0, -0.1160, 0.700), (0, -0.1180, 0.670), (0.006, -0.1150, 0.646)],
                 [0.0022, 0.0022, 0.0018], ring=6, samples=4)
    parts.append(C.apply_material(fly, mat["Canvas"]))
    # Front pocket openings: two short stitched lines, angled, in a denim-stitch tone.
    for s in (-1, 1):
        pts = [(s * 0.030, -0.1150, 0.742), (s * 0.062, -0.1140, 0.734),
               (s * 0.092, -0.1075, 0.726)]
        seam = C.tube(f"PocketSeam_{s}", pts, [0.0022, 0.0024, 0.0018], ring=6, samples=4)
        parts.append(C.apply_material(seam, mat["Cuff"]))

    for s, tag in ((1, "L"), (-1, "R")):
        hip = Vector((s * 0.0985, 0.0, 0.734))
        kn = Vector((s * 0.1075, 0.002, 0.446))
        an = Vector((s * 0.1280, 0.006, 0.150))
        J[f"hip.{tag}"], J[f"kn.{tag}"], J[f"an.{tag}"] = hip, kn, an
        J[f"toe.{tag}"] = Vector((an.x, -0.180, 0.030))

        leg = C.tube(f"Leg_{tag}",
                     [(s * 0.0990, 0.000, 0.790), (s * 0.1010, 0.000, 0.700),
                      (s * 0.1040, 0.001, 0.600), (s * 0.1065, 0.002, 0.500),
                      (s * 0.1075, 0.002, 0.446), (s * 0.1160, 0.004, 0.330),
                      (s * 0.1265, 0.006, 0.232)],
                     [0.0790, 0.0745, 0.0690, 0.0625, 0.0595, 0.0545, 0.0475],
                     ring=22, samples=5)
        C.shade_smooth(leg)
        parts.append(C.apply_material(leg, mat["Denim"]))

        cuff = C.tube(f"JeanCuff_{tag}",
                      [(s * 0.1265, 0.006, 0.246), (s * 0.1272, 0.006, 0.192),
                       (s * 0.1280, 0.006, 0.158)],
                      [0.0500, 0.0520, 0.0500], ring=20, samples=3)
        C.shade_smooth(cuff)
        parts.append(C.apply_material(cuff, mat["Cuff"]))

        build_shoe(mat, parts, an, s, tag)


def build_shoe(mat, parts, ankle, s, tag):
    """Chunky black canvas high-top: white sole, white toe cap, laces."""
    ax = ankle.x
    # Toes out. A negative angle on the +x side turned the toes inward.
    ang = math.radians(30 * s)
    ca, sa = math.cos(ang), math.sin(ang)

    def P(y, zz):
        """A point in the shoe's frame: y forward is negative, rotated outward."""
        return (ax - y * sa, y * ca, zz)

    sole = C.superellipsoid(f"Sole_{tag}", P(-0.048, 0.0225), (0.0575, 0.1450, 0.0230),
                            n=3.8, seg=30, rings=14, rot=(0, 0, ang))
    C.shade_smooth(sole)
    parts.append(C.apply_material(sole, mat["Sole"]))
    stripe = C.superellipsoid(f"SoleStripe_{tag}", P(-0.048, 0.0340),
                              (0.0525, 0.1450, 0.0050), n=3.8, seg=26, rings=10,
                              rot=(0, 0, ang))
    parts.append(C.apply_material(stripe, mat["Canvas"]))

    upper = C.tube(f"ShoeUpper_{tag}",
                   [P(0.072, 0.058), P(0.040, 0.064), P(-0.010, 0.072),
                    P(-0.070, 0.072), P(-0.122, 0.064), P(-0.158, 0.048)],
                   [0.0470, 0.0500, 0.0520, 0.0505, 0.0440, 0.0325],
                   ring=20, samples=5, squash=[(1.0, 0.94)] * 6)
    C.shade_smooth(upper)
    parts.append(C.apply_material(upper, mat["Canvas"]))

    collar = C.tube(f"ShoeCollar_{tag}", [P(0.030, 0.080), P(0.028, 0.114), P(0.026, 0.150)],
                    [0.0570, 0.0562, 0.0550], ring=18, samples=3)
    C.shade_smooth(collar)
    parts.append(C.apply_material(collar, mat["Canvas"]))

    toe = C.superellipsoid(f"ToeCap_{tag}", P(-0.148, 0.0450), (0.0420, 0.0345, 0.0300),
                           n=2.6, seg=22, rings=14, rot=(0, 0, ang))
    C.shade_smooth(toe)
    parts.append(C.apply_material(toe, mat["Sole"]))

    for i in range(5):
        yy = -0.012 - i * 0.0175
        zz = 0.0850 - i * 0.0045
        a = P(yy, zz)
        lace = C.cylinder(f"Lace{i}_{tag}", (a[0] - 0.0255, a[1], a[2]),
                          (a[0] + 0.0255, a[1], a[2]), 0.0030, 0.0030, verts=8)
        lace.rotation_euler = (0, 0, ang)
        parts.append(C.apply_material(lace, mat["Sole"]))

    tongue = C.superellipsoid(f"Tongue_{tag}", P(-0.060, 0.0880), (0.0245, 0.0340, 0.0090),
                              n=3.0, seg=16, rings=10, rot=(math.radians(-18), 0, ang))
    parts.append(C.apply_material(tongue, mat["Canvas"]))


# --------------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    a = ap.parse_args()

    report = {"status": "running", "character": "Professor Z",
              "source": "turnaround sheet (front / side / back), measured", "warnings": []}
    BC.reset()
    try:
        bpy.ops.preferences.addon_enable(module="cycles")
    except Exception:
        pass

    mat = materials()
    head_parts, body_parts = [], []
    J = {}

    build_head(mat, head_parts)
    build_ears(mat, head_parts)
    build_face(mat, head_parts)
    curls = build_hair(mat, head_parts)
    build_body(mat, body_parts, J)

    report["curls"] = curls
    report["landmarks_m"] = {k: round(v, 4) for k, v in Z.items()}

    def is_arm(o):
        return o.name.startswith(("Sleeve", "Arm_", "Palm_", "Finger", "Thumb_", "Watch"))

    core, arms = [], {"L": [], "R": []}
    for o in body_parts + head_parts:
        if is_arm(o):
            arms["L" if o.matrix_world.translation.x > 0 else "R"].append(o)
        else:
            core.append(o)

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
    arm_ob = {side: join_group(arms[side], f"PZ_arm_{side}") for side in ("L", "R")}

    island_ranges = [("core", 0, len(core_ob.data.vertices))]
    off = len(core_ob.data.vertices)
    for side in ("L", "R"):
        n = len(arm_ob[side].data.vertices)
        bpy.ops.object.select_all(action="DESELECT")
        arm_ob[side].select_set(True)
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

    zmin = min(v.co.z for v in body.data.vertices)
    for v in body.data.vertices:
        v.co.z -= zmin
    J = {k: v + Vector((0, 0, -zmin)) for k, v in J.items()}
    cz = HEAD_CZ - zmin

    M = dict(head_h=HEAD_HH * 2, sh_w=0.28, hip_y=J["hip.L"].z, chest_z=Z["chest"] - zmin,
             neck_z=Z["neck"] - zmin, shoulder_z=Z["shoulder_top"] - zmin,
             leg_len=J["hip.L"].z, head_center_z=cz, arm_len=0.30,
             HD=dict(hw=HEAD_HW, hd=HEAD_HD, cz=cz, head_h=HEAD_HH * 2),
             joints=J, island_ranges=island_ranges, part_labels=[], silhouette=[])

    arm = BC.build_armature(M, report)
    BC.build_fingers(arm, M, report)
    BC.add_shape_keys(body, dict(M["HD"]), report, landmarks={
        "mouth_z": (Z["mouth"] - cz) / (HEAD_HH * 2),
        "mouth_w": 0.34,
        "brow_z": (Z["brow"] - cz) / (HEAD_HH * 2),
        "brow_w": 0.36,
        "brow_x": 0.0420 / (HEAD_HH * 2),
        "eye_z": (Z["eye"] - cz) / (HEAD_HH * 2),
        "eye_x": EYE_X / (HEAD_HH * 2),
        "lid_z": (Z["eye"] - cz) / (HEAD_HH * 2),
        "jaw_top": (Z["mouth"] - 0.020 - cz) / (HEAD_HH * 2),
        "jaw_y": 0.30,
    })
    body.vertex_groups.clear()
    BC.weight_by_anatomy(body, arm, M, report, island_ranges=island_ranges)

    report["body"] = {
        "verts": len(body.data.vertices), "polys": len(body.data.polygons),
        "tris": sum(len(p.vertices) - 2 for p in body.data.polygons),
        "height_m": round(max(v.co.z for v in body.data.vertices), 3),
        "dimensions": [round(v, 3) for v in body.dimensions],
    }
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
    pathlib.Path(a.report).write_text(json.dumps(report, indent=2))
    print(json.dumps({"status": "ok", "body": report["body"], "totals": report["totals"],
                      "curls": curls, "dead_morphs": report.get("shape_key_dead")}, indent=2))


if __name__ == "__main__":
    main()
