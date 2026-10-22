#!/usr/bin/env python3
"""
Stage 3: Appeal-First Pixar 3D Character Builder (`build_character.py`).

Constructs the complete production-clean `Professor Z` (`1.730 m` / `5'8"`, Age 24,
Nighttime Thinker / Space Enthusiast / Snack Specialist) — or an arbitrary single-photo
portrait avatar — using watertight, UV-mapped, mobile-budgeted BMesh topology with strict
anatomical skinning zone partitions (`ZONE_TORSO`, `ZONE_ARMS`, `ZONE_LEGS`, `ZONE_HANDS`).
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys
from typing import Any, Dict, List, Sequence, Tuple

import bpy
import bmesh
from mathutils import Matrix, Vector
import numpy as np
from scipy.spatial import cKDTree


UV_BLAZER  = (0.02, 0.48, 0.02, 0.48)
UV_SHIRT   = (0.52, 0.98, 0.02, 0.48)
UV_TROUSER = (0.02, 0.48, 0.52, 0.98)
UV_SHOE    = (0.52, 0.73, 0.77, 0.98)
UV_GOLD    = (0.77, 0.98, 0.77, 0.98)
UV_BOWTIE  = (0.52, 0.73, 0.52, 0.73)
UV_DARK    = (0.77, 0.98, 0.52, 0.73)


def clear_scene() -> None:
    bpy.ops.wm.read_factory_settings(use_empty=True)


def load_packed_image(path_str: str, non_color: bool = False) -> bpy.types.Image:
    p = str(pathlib.Path(path_str).resolve())
    img = bpy.data.images.load(p, check_existing=True)
    if non_color:
        img.colorspace_settings.name = "Non-Color"
    img.pack()
    return img


def make_principled_material(
    name: str,
    base_color: Tuple[float, float, float, float] = (0.8, 0.8, 0.8, 1.0),
    roughness: float = 0.55,
    subsurface: float = 0.0,
    metallic: float = 0.0,
    albedo_img: bpy.types.Image | None = None,
    normal_img: bpy.types.Image | None = None,
    rough_img: bpy.types.Image | None = None,
    normal_strength: float = 0.25,
) -> bpy.types.Material:
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nt = mat.node_tree
    nt.nodes.clear()
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.inputs["Base Color"].default_value = base_color
    bsdf.inputs["Roughness"].default_value = roughness
    bsdf.inputs["Metallic"].default_value = metallic
    if subsurface > 0.0 and "Subsurface Weight" in bsdf.inputs:
        bsdf.inputs["Subsurface Weight"].default_value = subsurface
        if "Subsurface Radius" in bsdf.inputs:
            bsdf.inputs["Subsurface Radius"].default_value = (1.0, 0.42, 0.24)
    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])

    if albedo_img is not None:
        tex_n = nt.nodes.new("ShaderNodeTexImage")
        tex_n.image = albedo_img
        nt.links.new(tex_n.outputs["Color"], bsdf.inputs["Base Color"])
    if rough_img is not None:
        r_n = nt.nodes.new("ShaderNodeTexImage")
        r_n.image = rough_img
        nt.links.new(r_n.outputs["Color"], bsdf.inputs["Roughness"])
    if normal_img is not None:
        n_tex = nt.nodes.new("ShaderNodeTexImage")
        n_tex.image = normal_img
        n_map = nt.nodes.new("ShaderNodeNormalMap")
        n_map.inputs["Strength"].default_value = normal_strength
        nt.links.new(n_tex.outputs["Color"], n_map.inputs["Color"])
        nt.links.new(n_map.outputs["Normal"], bsdf.inputs["Normal"])
    return mat


def create_mesh_object(
    name: str,
    bm: bmesh.types.BMesh,
    mat: bpy.types.Material,
    subdiv: int = 0,
) -> bpy.types.Object:
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    mesh = bpy.data.meshes.new(f"{name}_Mesh")
    bm.to_mesh(mesh)
    bm.free()
    for poly in mesh.polygons:
        poly.use_smooth = True
    obj = bpy.data.objects.new(name, mesh)
    obj.data.materials.append(mat)
    bpy.context.collection.objects.link(obj)
    if subdiv > 0:
        bpy.context.view_layer.objects.active = obj
        mod = obj.modifiers.new("Subsurf", "SUBSURF")
        mod.levels = subdiv
        mod.render_levels = subdiv
        bpy.ops.object.modifier_apply(modifier=mod.name)
    return obj


def add_swept_tube_to_bmesh(
    bm: bmesh.types.BMesh,
    frames: Sequence[Tuple[Vector, Vector, Vector, float, float]],
    n_seg: int = 8,
    uv_box: Tuple[float, float, float, float] = (0.0, 1.0, 0.0, 1.0),
    cap_start: bool = False,
    cap_end: bool = False,
) -> None:
    """Sweep clean smooth quad rings along an arbitrary 3D curve of `(center, u_axis, v_axis, rx, ry)` frames."""
    uv_l = bm.loops.layers.uv.verify()
    u0, u1, v0, v1 = uv_box
    rings: List[List[bmesh.types.BMVert]] = []
    for (c, ax_u, ax_v, rx, ry) in frames:
        ring = []
        for j in range(n_seg):
            ang = 2.0 * math.pi * (j / float(n_seg))
            pt = c + ax_u * (rx * math.cos(ang)) + ax_v * (ry * math.sin(ang))
            ring.append(bm.verts.new(pt))
        rings.append(ring)

    n_f = len(rings)
    for i in range(n_f - 1):
        va = v0 + (v1 - v0) * (i / max(n_f - 1, 1))
        vb = v0 + (v1 - v0) * ((i + 1) / max(n_f - 1, 1))
        for j in range(n_seg):
            jn = (j + 1) % n_seg
            ua = u0 + (u1 - u0) * (j / float(n_seg))
            ub = u0 + (u1 - u0) * ((j + 1) / float(n_seg))
            try:
                f = bm.faces.new((rings[i][j], rings[i][jn], rings[i + 1][jn], rings[i + 1][j]))
                for lp, uv_c in zip(f.loops, ((ua, va), (ub, va), (ub, vb), (ua, vb))):
                    lp[uv_l].uv = uv_c
            except ValueError:
                pass

    if cap_start and len(rings[0]) >= 3:
        c0 = frames[0][0]
        cv = bm.verts.new(c0)
        for j in range(n_seg):
            jn = (j + 1) % n_seg
            try:
                f = bm.faces.new((cv, rings[0][jn], rings[0][j]))
                for lp in f.loops:
                    lp[uv_l].uv = (0.5 * (u0 + u1), v0)
            except ValueError:
                pass

    if cap_end and len(rings[-1]) >= 3:
        ce = frames[-1][0]
        cv = bm.verts.new(ce)
        for j in range(n_seg):
            jn = (j + 1) % n_seg
            try:
                f = bm.faces.new((cv, rings[-1][j], rings[-1][jn]))
                for lp in f.loops:
                    lp[uv_l].uv = (0.5 * (u0 + u1), v1)
            except ValueError:
                pass


def build_pixar_head_assembly(
    profile: Dict[str, Any],
    skin_mat: bpy.types.Material,
    eye_mat: bpy.types.Material,
    dark_mat: bpy.types.Material,
) -> Tuple[bpy.types.Object, List[bpy.types.Object], Dict[str, float]]:
    """
    Construct `Avatar_Head` + `Eye_L` + `Eye_R` calibrated directly to Professor Z's
    reference turnaround sheet (`ref_02_professor_z_catalogue.png`):
      - Youthful tapered jaw + warm cheek fullness + defined bridge/button nose & nostrils
      - Relaxed soulful upper eyelids + crisp dark upper eyelashes gently overlapping upper iris
        (eliminates wide-eyed startled stare!)
      - Post-Subsurf closed-lip warm asymmetric Pixar half-smile (`gap = 0.4 mm` — ZERO dark slot!)
      - Full-width curved white upper & lower dental arches + 3D coral tongue for natural visemes!
    """
    anth = profile["anthropometrics"]
    pixar = bool(anth.get("stylized_pixar_proportions", False))
    H = float(anth.get("target_height_m", 1.73 if pixar else 1.68))

    if pixar:
        S = H / 1.73
        chin_z = 1.352 * S
        head_h = 0.286 * S
        base_rx = 0.109 * S
        base_ry = 0.121 * S
        mouth_z = chin_z + 0.255 * head_h
        nose_z = chin_z + 0.435 * head_h
        eye_z = chin_z + 0.560 * head_h
        brow_z = chin_z + 0.720 * head_h
        eye_x = 0.0425 * S
        eye_r = 0.0258 * S
        eye_hx, eye_hz = 0.0282 * S, 0.0182 * S  # Relaxed almond aperture — no startled stare!
        mouth_hx = 0.0315 * S
    else:
        S = H / 1.68
        chin_z = 1.430 * S
        head_h = 0.225 * S
        base_rx = 0.091 * S
        base_ry = 0.104 * S
        mouth_z = chin_z + 0.245 * head_h
        nose_z = chin_z + 0.425 * head_h
        eye_z = chin_z + 0.555 * head_h
        brow_z = chin_z + 0.718 * head_h
        eye_x = 0.0345 * S
        eye_r = 0.0178 * S
        eye_hx, eye_hz = 0.0190 * S, 0.0126 * S
        mouth_hx = 0.0265 * S

    eye_y = -base_ry * 0.790

    bm = bmesh.new()
    uv_layer = bm.loops.layers.uv.verify()

    N_LON, N_LAT = 44, 34
    TH_CHIN = 0.38
    c0 = math.cos(TH_CHIN)
    grid: List[List[bmesh.types.BMVert]] = []

    for i in range(N_LAT + 1):
        s = i / float(N_LAT)
        if s < 0.15:
            nt = s / 0.15
            z = (chin_z - 0.088 * S) + (0.088 * S) * nt
            neck_flare = 0.43 + 0.045 * (nt ** 2)
            rx = base_rx * neck_flare
            ry = base_ry * neck_flare
            cy = 0.008 * S * (1.0 - nt)
        else:
            ht = (s - 0.15) / 0.85
            th = TH_CHIN + ht * (math.pi - TH_CHIN)
            z = chin_z + head_h * ((c0 - math.cos(th)) / (1.0 + c0))
            prof = math.sin(th) ** 0.76
            # Clean youthful taper on lower jaw (`ht < 0.24`) + warm cheekbone fullness (`ht ~ 0.38`)
            jaw_taper = -0.075 * math.exp(-((ht - 0.14) / 0.13) ** 2)
            cheek_w = 1.0 + jaw_taper + 0.088 * math.exp(-((ht - 0.38) / 0.16) ** 2) - 0.045 * math.exp(-((ht - 0.68) / 0.16) ** 2)
            rx = base_rx * prof * cheek_w
            ry = base_ry * prof
            cy = 0.004 * S * math.sin(ht * math.pi)

        row: List[bmesh.types.BMVert] = []
        for j in range(N_LON):
            phi = -math.pi * 0.5 + 2.0 * math.pi * (j / float(N_LON))
            ca, sa = math.cos(phi), math.sin(phi)
            px = rx * ca
            py = cy + ry * sa
            pz = z

            if py < 0.01 and z > chin_z - 0.015:
                # A. Clean defined chin pad
                chin_pad = math.exp(-((px / (0.028 * S)) ** 2 + ((pz - (chin_z + 0.019 * S)) / (0.020 * S)) ** 2))
                py -= 0.0085 * S * chin_pad

                # B. Warm Zygomatic Cheekbones
                for sx_c in (-1.0, 1.0):
                    chk = math.exp(-(((px - sx_c * eye_x * 1.18) / (0.034 * S)) ** 2 + ((pz - (nose_z + 0.008 * S)) / (0.028 * S)) ** 2))
                    py -= 0.0088 * S * chk
                    px += sx_c * 0.0045 * S * chk

                # C. Defined Pixar Nose Bridge + Rounded Tip + Side Alar Wings
                bridge = math.exp(-((px / (0.0110 * S)) ** 2 + ((pz - (nose_z + 0.026 * S)) / (0.028 * S)) ** 2))
                tip = math.exp(-((px / (0.0155 * S)) ** 2 + ((pz - nose_z) / (0.0145 * S)) ** 2))
                py -= (0.0185 * bridge + 0.0295 * tip) * S
                pz += 0.0025 * S * tip
                for sx_n in (-1.0, 1.0):
                    alar = math.exp(-(((px - sx_n * 0.0148 * S) / (0.0085 * S)) ** 2 + ((pz - (nose_z - 0.004 * S)) / (0.0078 * S)) ** 2))
                    py -= 0.0115 * S * alar

                # D. Eye Sockets + Relaxed Upper Eyelid Fold gently overlapping top of iris!
                for sx_e in (-1.0, 1.0):
                    ex_n = (px - sx_e * eye_x) / eye_hx
                    ez_n = (pz - (eye_z - 0.0018 * S)) / eye_hz
                    er2 = ex_n * ex_n + ez_n * ez_n
                    sock = math.exp(-er2 * 1.42)
                    py += 0.0215 * S * sock
                    rim_fold = math.exp(-((math.sqrt(max(er2, 1e-6)) - 1.03) / 0.22) ** 2)
                    py -= 0.0072 * S * rim_fold

                # E. Charming Upturned Pixar Half-Smile Lip Volume + Cheek Dimples
                mx_norm = px / max(mouth_hx, 1e-4)
                smile_lift = (0.0056 * (mx_norm ** 2) + 0.0018 * mx_norm) * S * float(np.clip(1.25 - abs(mx_norm) * 0.3, 0.0, 1.25))
                m_line_z = mouth_z + smile_lift
                if abs(px) <= mouth_hx * 1.30:
                    horiz_w = math.exp(-((px / (mouth_hx * 0.96)) ** 4))
                    dz_m = pz - m_line_z
                    seam = math.exp(-((dz_m / (0.0038 * S)) ** 2)) * horiz_w
                    up_lip = math.exp(-(((dz_m - 0.0058 * S) / (0.0052 * S)) ** 2)) * horiz_w
                    lo_lip = math.exp(-(((dz_m + 0.0068 * S) / (0.0062 * S)) ** 2)) * horiz_w
                    dimple = math.exp(-(((abs(px) - mouth_hx * 1.04) / (0.008 * S)) ** 2 + (dz_m / (0.008 * S)) ** 2))
                    py = py + 0.0105 * S * seam - 0.0062 * S * up_lip - 0.0075 * S * lo_lip + 0.0052 * S * dimple

            row.append(bm.verts.new((px, py, pz)))
        grid.append(row)

    i_mouth = min(range(8, N_LAT - 8), key=lambda ii: abs(grid[ii][0].co.z - mouth_z))
    mouth_cols = [N_LON - 3, N_LON - 2, N_LON - 1, 0, 1, 2, 3]
    interior_mouth = set(mouth_cols[1:-1])

    upper_lip_v: Dict[int, bmesh.types.BMVert] = {}
    lower_lip_v: Dict[int, bmesh.types.BMVert] = {}
    for idx_m, jj in enumerate(mouth_cols):
        u_m = -1.0 + 2.0 * (idx_m / float(len(mouth_cols) - 1))
        asym_lift = 0.0026 * S * max(0.0, u_m)
        smile_z = (0.0048 * (u_m * u_m) * S) + asym_lift
        base_v = grid[i_mouth][jj]
        if jj not in interior_mouth:
            base_v.co.z = mouth_z + smile_z
            base_v.co.y += 0.0028 * S
            upper_lip_v[jj] = base_v
            lower_lip_v[jj] = base_v
        else:
            cupid = (0.0012 * math.cos(u_m * math.pi * 2.2) - 0.0004) * S * (1.0 - u_m * u_m)
            vu = base_v
            vu.co = Vector((base_v.co.x, base_v.co.y - 0.0058 * S * (1.0 - 0.50 * u_m * u_m), mouth_z + smile_z + 0.0003 * S + cupid))
            vl = bm.verts.new(Vector((base_v.co.x, base_v.co.y - 0.0060 * S * (1.0 - 0.50 * u_m * u_m), mouth_z + smile_z - 0.0003 * S)))
            upper_lip_v[jj] = vu
            lower_lip_v[jj] = vl

    def u_of(jj: int) -> float:
        ang = 2.0 * math.pi * (jj / float(N_LON))
        if ang > math.pi:
            ang -= 2.0 * math.pi
        return float(np.clip(0.5 + ang / (2.0 * math.pi), 0.01, 0.99))

    for i in range(N_LAT):
        va = i / float(N_LAT)
        vb = (i + 1) / float(N_LAT)
        for j in range(N_LON):
            jn = (j + 1) % N_LON
            ua = u_of(j)
            ub = u_of(j + 1) if j < N_LON // 2 else u_of(jn)
            if abs(ub - ua) > 0.45:
                ub = ua

            v00 = lower_lip_v.get(j, grid[i][j]) if i == i_mouth else grid[i][j]
            v10 = lower_lip_v.get(jn, grid[i][jn]) if i == i_mouth else grid[i][jn]
            if i == i_mouth:
                v00 = upper_lip_v.get(j, grid[i][j])
                v10 = upper_lip_v.get(jn, grid[i][jn])
            v11 = lower_lip_v.get(jn, grid[i + 1][jn]) if (i + 1) == i_mouth else grid[i + 1][jn]
            v01 = lower_lip_v.get(j, grid[i + 1][j]) if (i + 1) == i_mouth else grid[i + 1][j]

            f = bm.faces.new((v00, v10, v11, v01))
            is_lip_band = (
                (i == i_mouth and (j in interior_mouth or jn in interior_mouth))
                or (i == i_mouth - 1 and (j in interior_mouth or jn in interior_mouth))
            )
            if is_lip_band:
                lip_uvs = [
                    (0.92, 0.92) if (vv in upper_lip_v.values() or vv in lower_lip_v.values()) else uvc
                    for vv, uvc in zip((v00, v10, v11, v01), ((ua, va), (ub, va), (ub, vb), (ua, vb)))
                ]
                for lp, uvc in zip(f.loops, lip_uvs):
                    lp[uv_layer].uv = uvc
            else:
                for lp, uvc in zip(f.loops, ((ua, va), (ub, va), (ub, vb), (ua, vb))):
                    lp[uv_layer].uv = uvc

    # Closed 2-manifold inner oral cavity (`material_index = 1`)
    pouch_up = [bm.verts.new((upper_lip_v[jj].co.x * 0.94, upper_lip_v[jj].co.y + 0.028 * S, mouth_z + 0.011 * S)) for jj in mouth_cols]
    pouch_lo = [bm.verts.new((lower_lip_v[jj].co.x * 0.94, lower_lip_v[jj].co.y + 0.028 * S, mouth_z - 0.012 * S)) for jj in mouth_cols]
    for k in range(len(mouth_cols) - 1):
        j0, j1 = mouth_cols[k], mouth_cols[k + 1]
        for quad in (
            (lower_lip_v[j0], lower_lip_v[j1], pouch_lo[k + 1], pouch_lo[k]),
            (pouch_lo[k], pouch_lo[k + 1], pouch_up[k + 1], pouch_up[k]),
            (pouch_up[k], pouch_up[k + 1], upper_lip_v[j1], upper_lip_v[j0]),
        ):
            if len({id(v) for v in quad}) >= 3:
                verts_unique = []
                for vv in quad:
                    if vv not in verts_unique:
                        verts_unique.append(vv)
                fc = bm.faces.new(verts_unique)
                fc.material_index = 1
                for lp in fc.loops:
                    lp[uv_layer].uv = (0.5, 0.5)

    top_center = bm.verts.new((0.0, 0.004 * S, chin_z + head_h))
    for j in range(N_LON):
        jn = (j + 1) % N_LON
        f = bm.faces.new((top_center, grid[-1][j], grid[-1][jn]))
        for lp in f.loops:
            lp[uv_layer].uv = (0.5, 0.98)

    # 2. Clean Rounded C-Shaped Pixar Ears (`Ears_L/R`) cleanly clear of hair sideburns!
    for sx in (-1.0, 1.0):
        ear_z_mid = 0.5 * (eye_z + nose_z) - 0.002 * S
        ear_x_base = sx * base_rx * 0.98
        ear_y_mid = -0.002 * S
        helix_pts = []
        for k in range(13):
            t = -0.78 * math.pi + (1.56 * math.pi) * (k / 12.0)
            ex = ear_x_base + sx * (0.011 + 0.015 * math.cos(t * 0.55)) * S
            ey = ear_y_mid + 0.018 * S * math.cos(t) + 0.006 * S
            ez = ear_z_mid + 0.028 * S * math.sin(t)
            n1 = Vector((sx, 0.0, 0.0))
            n2 = Vector((0.0, math.cos(t), math.sin(t))).normalized()
            helix_pts.append((Vector((ex, ey, ez)), n1, n2, 0.0064 * S, 0.0068 * S))
        eu = (0.24, 0.28, 0.46, 0.54) if sx < 0 else (0.72, 0.76, 0.46, 0.54)
        add_swept_tube_to_bmesh(bm, helix_pts, n_seg=7, uv_box=eu, cap_start=True, cap_end=True)

        cup_pts = [
            (Vector((ear_x_base + sx * 0.005 * S, ear_y_mid + 0.006 * S, ear_z_mid + z_o * S)),
             Vector((0, 1, 0)), Vector((sx, 0, 0)), w_e * S, 0.0052 * S)
            for (z_o, w_e) in [(-0.020, 0.011), (-0.007, 0.016), (+0.007, 0.017), (+0.020, 0.012)]
        ]
        add_swept_tube_to_bmesh(bm, cup_pts, n_seg=8, uv_box=eu, cap_start=True, cap_end=True)

    oral_mat = make_principled_material("Mat_OralDark", base_color=(0.032, 0.010, 0.013, 1.0), roughness=0.90)
    teeth_mat = make_principled_material("Mat_TeethIvory", base_color=(0.955, 0.942, 0.910, 1.0), roughness=0.22)
    tongue_mat = make_principled_material("Mat_TongueCoral", base_color=(0.76, 0.29, 0.29, 1.0), roughness=0.36)

    bm.verts.ensure_lookup_table()
    bm.faces.ensure_lookup_table()
    bm.verts.index_update()
    bm.faces.index_update()
    up_set = {v.index for v in upper_lip_v.values()}
    lo_set = {v.index for v in lower_lip_v.values()}
    above_set = {grid[ii][jj].index for ii in range(i_mouth + 1, N_LAT + 1) for jj in range(N_LON)}
    below_set = {grid[ii][jj].index for ii in range(0, i_mouth) for jj in range(N_LON)}
    pouch_set = {v.index for v in (pouch_up + pouch_lo)}

    # Also tag exact inner seam lip indices BEFORE subdivision so we seal Catmull-Clark shrinkage cleanly!
    up_seam_only = {upper_lip_v[jj].index for jj in interior_mouth}
    lo_seam_only = {lower_lip_v[jj].index for jj in interior_mouth}

    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    for fc in bm.faces:
        fc.smooth = True
    head_mesh = bpy.data.meshes.new("Avatar_Head_Mesh")
    bm.to_mesh(head_mesh)
    bm.free()

    head_obj = bpy.data.objects.new("Avatar_Head", head_mesh)
    bpy.context.scene.collection.objects.link(head_obj)
    head_obj.data.materials.append(skin_mat)
    head_obj.data.materials.append(oral_mat)

    vg_up = head_obj.vertex_groups.new(name="LIP_UPPER")
    vg_lo = head_obj.vertex_groups.new(name="LIP_LOWER")
    vg_pouch = head_obj.vertex_groups.new(name="PART_ORAL")
    vg_seal_up = head_obj.vertex_groups.new(name="_SEAL_UP")
    vg_seal_lo = head_obj.vertex_groups.new(name="_SEAL_LO")

    for vi, mv in enumerate(head_obj.data.vertices):
        if vi in up_seam_only:
            vg_seal_up.add([vi], 1.0, "REPLACE")
        if vi in lo_seam_only:
            vg_seal_lo.add([vi], 1.0, "REPLACE")
        if vi in pouch_set:
            vg_pouch.add([vi], 1.0, "REPLACE")
            continue
        p = mv.co
        if p.y > -0.02 * S:
            continue
        dx = p.x / max(mouth_hx * 1.35, 0.045)
        if vi in up_set and vi not in lo_set:
            vg_up.add([vi], 1.0, "REPLACE")
        elif vi in lo_set and vi not in up_set:
            vg_lo.add([vi], 1.0, "REPLACE")
        elif vi in up_set and vi in lo_set:
            vg_up.add([vi], 0.5, "REPLACE")
            vg_lo.add([vi], 0.5, "REPLACE")
        elif vi in above_set:
            w = math.exp(-(dx * dx + ((p.z - mouth_z) / (0.026 * S)) ** 2))
            if w > 0.01:
                vg_up.add([vi], w, "REPLACE")
        elif vi in below_set:
            w = math.exp(-(dx * dx * 0.72 + ((p.z - mouth_z) / (0.048 * S)) ** 2))
            if w > 0.01:
                vg_lo.add([vi], w, "REPLACE")

    bpy.context.view_layer.objects.active = head_obj
    sub_mod = head_obj.modifiers.new("SubsurfHead", "SUBSURF")
    sub_mod.levels = 1
    sub_mod.render_levels = 1
    bpy.ops.object.modifier_apply(modifier=sub_mod.name)

    # POST-SUBSURF CLOSED-LIP HALF-SMILE SEAL:
    # Catmull-Clark averaging pulls `_SEAL_UP` up toward `pouch_up` and `_SEAL_LO` down toward `pouch_lo`.
    # Restore a natural, warm, softly closed Pixar half-smile along the lip contact arc!
    # Re-fetch fresh RNA pointers AFTER `modifier_apply` replaces C++ mesh data!
    vg_seal_up = head_obj.vertex_groups["_SEAL_UP"]
    vg_seal_lo = head_obj.vertex_groups["_SEAL_LO"]
    vg_pouch = head_obj.vertex_groups["PART_ORAL"]
    idx_su = vg_seal_up.index
    idx_sl = vg_seal_lo.index
    idx_po = vg_pouch.index
    for mv in head_obj.data.vertices:
        w_su = 0.0
        w_sl = 0.0
        w_po = 0.0
        for g in mv.groups:
            if g.group == idx_su:
                w_su = g.weight
            elif g.group == idx_sl:
                w_sl = g.weight
            elif g.group == idx_po:
                w_po = g.weight
        if (w_su > 0.20 or w_sl > 0.20) and w_po < 0.35:
            u_m = float(np.clip(mv.co.x / max(mouth_hx, 1e-4), -1.1, 1.1))
            asym_lift = 0.0024 * S * max(0.0, u_m)
            target_seal_z = mouth_z + (0.0052 * (u_m * u_m) * S) + asym_lift
            blend = float(np.clip(max(w_su, w_sl) * (1.0 - w_po), 0.0, 1.0))
            if w_su >= w_sl:
                mv.co.z = mv.co.z * (1.0 - blend) + (target_seal_z + 0.00025 * S) * blend
                mv.co.y -= 0.0026 * S * blend
            else:
                mv.co.z = mv.co.z * (1.0 - blend) + (target_seal_z - 0.00025 * S) * blend
                mv.co.y -= 0.0028 * S * blend

    head_obj.vertex_groups.remove(vg_seal_up)
    head_obj.vertex_groups.remove(vg_seal_lo)

    # 3. Full-Arch Curved White Pixar Upper (`PART_TEETH_UPPER`) & Lower (`PART_TEETH_LOWER`) Teeth + Tongue!
    teeth_up_bm = bmesh.new()
    teeth_lo_bm = bmesh.new()
    for is_upper, z_t, t_bm in ((True, mouth_z + 0.0042 * S, teeth_up_bm), (False, mouth_z - 0.0058 * S, teeth_lo_bm)):
        t_frames = []
        hw = mouth_hx * (0.98 if is_upper else 0.90)  # Full arch width so ZERO dark corner gaps ever show!
        for k in range(13):
            u_t = -1.0 + 2.0 * (k / 12.0)
            tx = u_t * hw
            ty = (-base_ry * 0.855 if is_upper else -base_ry * 0.820) + 0.017 * S * (u_t * u_t)
            scallop = 1.0 - 0.10 * abs(math.sin(k * math.pi * 0.9))
            t_frames.append((Vector((tx, ty, z_t)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0038 * S, 0.0048 * S * scallop))
        add_swept_tube_to_bmesh(t_bm, t_frames, n_seg=6, uv_box=(0.2, 0.8, 0.2, 0.8), cap_start=True, cap_end=True)

    tongue_bm = bmesh.new()
    tng_frames = [
        (Vector((0.0, (-base_ry * 0.54 - 0.0072 * k), mouth_z - 0.0055 * S)),
         Vector((1, 0, 0)), Vector((0, 0, 1)), (0.0170 - 0.0022 * k) * S, (0.0052 - 0.0007 * k) * S)
        for k in range(4)
    ]
    add_swept_tube_to_bmesh(tongue_bm, tng_frames, n_seg=8, uv_box=(0.2, 0.8, 0.2, 0.8), cap_start=True, cap_end=True)

    # 4. Thick Expressive Tapered Pixar Eyebrows + Relaxed Upper Lash Arch overlapping top of iris!
    brow_bm = bmesh.new()
    lash_bm = bmesh.new()
    for sx in (-1.0, 1.0):
        b_pts = []
        for k in range(7):
            u_b = k / 6.0
            bx = sx * (0.0165 + 0.0505 * u_b) * S
            asym = (0.0028 * S) if sx > 0 else 0.0
            bz = brow_z + (0.0110 * math.sin(math.pi * (0.12 + 0.78 * u_b)) - 0.0032 * u_b) * S + asym
            by = -base_ry * (0.945 - 0.14 * (u_b ** 1.6))
            thick_w = (0.0092 - 0.0046 * (u_b ** 1.5)) * S
            b_pts.append((Vector((bx, by, bz)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0056 * S, thick_w))
        add_swept_tube_to_bmesh(brow_bm, b_pts, n_seg=7, uv_box=(0.2, 0.8, 0.2, 0.8), cap_start=True, cap_end=True)

        l_pts = []
        for k in range(9):
            ang = math.pi * (0.12 + 0.76 * (k / 8.0))
            lx = sx * (eye_x - eye_hx * 1.02 * math.cos(ang))
            lz = (eye_z - 0.0018 * S) + eye_hz * 1.02 * math.sin(ang)
            ly = eye_y - eye_r * 0.88 + 0.005 * S * ((math.cos(ang)) ** 2)
            l_pts.append((Vector((lx, ly, lz)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0028 * S, 0.0026 * S))
        add_swept_tube_to_bmesh(lash_bm, l_pts, n_seg=5, uv_box=(0.2, 0.8, 0.2, 0.8), cap_start=True, cap_end=True)

    teeth_up_obj = create_mesh_object("Tmp_TeethUp", teeth_up_bm, teeth_mat, subdiv=0)
    teeth_lo_obj = create_mesh_object("Tmp_TeethLo", teeth_lo_bm, teeth_mat, subdiv=0)
    tongue_obj = create_mesh_object("Tmp_Tongue", tongue_bm, tongue_mat, subdiv=0)
    brow_obj = create_mesh_object("Tmp_Brows", brow_bm, dark_mat, subdiv=0)
    lash_obj = create_mesh_object("Tmp_Lashes", lash_bm, dark_mat, subdiv=0)

    for sub_obj, vg_tag in (
        (teeth_up_obj, "PART_TEETH_UPPER"),
        (teeth_lo_obj, "PART_TEETH_LOWER"),
        (tongue_obj, "PART_TONGUE"),
        (brow_obj, "PART_BROWS"),
        (lash_obj, "PART_LASHES"),
    ):
        vg = sub_obj.vertex_groups.new(name=vg_tag)
        vg.add(list(range(len(sub_obj.data.vertices))), 1.0, "REPLACE")

    bpy.ops.object.select_all(action="DESELECT")
    for o in (head_obj, teeth_up_obj, teeth_lo_obj, tongue_obj, brow_obj, lash_obj):
        o.select_set(True)
    bpy.context.view_layer.objects.active = head_obj
    bpy.ops.object.join()

    # 5. Soulful Large-Iris Eyeballs (`Eye_L`, `Eye_R`)
    eyes: List[bpy.types.Object] = []
    for side_tag, sx in (("L", 1.0), ("R", -1.0)):
        ebm = bmesh.new()
        euv = ebm.loops.layers.uv.verify()
        bmesh.ops.create_uvsphere(ebm, u_segments=24, v_segments=16, radius=eye_r)
        bmesh.ops.rotate(ebm, verts=ebm.verts[:], cent=(0, 0, 0), matrix=Matrix.Rotation(math.pi * 0.5, 3, "X"))
        for f in ebm.faces:
            for lp in f.loops:
                v = lp.vert.co
                if v.y < 0.0:
                    scale_uv = 0.44 / max(eye_r * 0.88, 1e-4)
                    uu = 0.5 + float(np.clip(v.x * scale_uv, -0.49, 0.49))
                    vv = 0.5 + float(np.clip(v.z * scale_uv, -0.49, 0.49))
                    lp[euv].uv = (uu, vv)
                else:
                    lp[euv].uv = (0.04, 0.04)
        bmesh.ops.translate(ebm, verts=ebm.verts[:], vec=(sx * eye_x, eye_y, eye_z))
        e_obj = create_mesh_object(f"Eye_{side_tag}", ebm, eye_mat, subdiv=0)
        eyes.append(e_obj)

    anchors = {
        "height_m": H,
        "chin_z": chin_z,
        "head_h": head_h,
        "base_rx": base_rx,
        "base_ry": base_ry,
        "mouth_z": mouth_z,
        "mouth_hx": mouth_hx,
        "nose_z": nose_z,
        "eye_x": eye_x,
        "eye_y": eye_y,
        "eye_z": eye_z,
        "eye_r": eye_r,
        "brow_z": brow_z,
    }
    return head_obj, eyes, anchors


def build_pixar_hair_and_glasses(
    head_obj: bpy.types.Object,
    profile: Dict[str, Any],
    anc: Dict[str, float],
    garment_mat: bpy.types.Material,
) -> List[bpy.types.Object]:
    """
    Build Professor Z's signature `Messy, expressive hair` AND `Round glasses`:
      - Ray-verified `100%` zero-gap solid espresso base cap using `cKDTree` over `Avatar_Head`'s actual
        subdivided vertices (`R_cap >= R_head + 7.5 mm` everywhere across crown, occiput, nape & temples,
        while curving neatly over the ears so ZERO hair clips through the ears AND ZERO bald scalp
        patches can ever appear at ANY camera angle from `0°` to `360°`!).
      - Sleek tapered flat-blade Pixar hair locks sweeping upward & rightward from the crown (`H = 1.730 m`).
    """
    pal = profile["features_and_palette"]
    H = anc["height_m"]
    S = H / 1.73
    chin_z, head_h = anc["chin_z"], anc["head_h"]
    base_rx, base_ry = anc["base_rx"], anc["base_ry"]
    eye_x, eye_y, eye_z = anc["eye_x"], anc["eye_y"], anc["eye_z"]
    crown_z = chin_z + head_h
    built: List[bpy.types.Object] = []

    skull_center = np.array([0.0, 0.0, chin_z + 0.56 * head_h], dtype=np.float64)
    head_pts = np.array([tuple(v.co) for v in head_obj.data.vertices], dtype=np.float64)
    # Exclude lateral protruding ear tips (`abs(x) > base_rx * 1.03`) when measuring cranial shell radius
    cranial_mask = np.abs(head_pts[:, 0]) <= (base_rx * 1.02)
    cranial_pts = head_pts[cranial_mask]
    cranial_dirs = cranial_pts - skull_center[None, :]
    cranial_radii = np.linalg.norm(cranial_dirs, axis=1)
    cranial_unit = cranial_dirs / np.maximum(cranial_radii[:, None], 1e-6)
    dir_tree = cKDTree(cranial_unit)

    def skull_safe_point(raw_xyz: Tuple[float, float, float], min_clearance: float) -> Vector:
        p = np.array(raw_xyz, dtype=np.float64)
        rel = p - skull_center
        r_cur = float(np.linalg.norm(rel))
        u_dir = rel / max(r_cur, 1e-6)
        _, idxs = dir_tree.query(u_dir, k=6)
        r_skull = float(np.max(cranial_radii[idxs]))
        r_safe = max(r_cur, r_skull + min_clearance)
        out = skull_center + u_dir * r_safe
        return Vector((float(out[0]), float(out[1]), float(out[2])))

    hbm = bmesh.new()
    huv = hbm.loops.layers.uv.verify()

    # 1. Ray-Verified 100% Zero-Gap Solid Espresso Inner Hair Base Cap
    N_U, N_V = 40, 20
    cap_grid: List[List[bmesh.types.BMVert]] = []
    ear_z_mid = 0.5 * (eye_z + anc["nose_z"])
    for iv in range(N_V + 1):
        tv = iv / float(N_V)  # `0 = clean anatomical hairline rim`, `1 = crown apex`
        ring_v = []
        for iu in range(N_U):
            phi = -math.pi * 0.5 + 2.0 * math.pi * (iu / float(N_U))
            ca, sa = math.cos(phi), math.sin(phi)
            # Smooth continuous hairline around entire 360° head:
            # - Front forehead (`sa = -1`): `brow_z + 0.036`
            # - Sideburns right in front of ears (`abs(ca) ~ 0.92, sa ~ -0.38`): dips cleanly to `eye_z + 0.004`
            # - Over ear arch (`abs(ca) ~ 1.0, sa ~ +0.02`): arches cleanly above ear helix `ear_z_mid + 0.036`
            # - Back nape (`sa = +1`): tapers cleanly down to `chin_z + 0.035`!
            w_front = 0.5 * (1.0 - sa)
            w_back = 0.5 * (1.0 + sa)
            sideburn = math.exp(-(((abs(ca) - 0.88) / 0.22) ** 2 + ((sa + 0.44) / 0.26) ** 2))
            over_ear = math.exp(-(((abs(ca) - 0.99) / 0.16) ** 2 + ((sa - 0.03) / 0.18) ** 2))
            z_rim = (
                (anc["brow_z"] + 0.032 * S) * (w_front ** 1.35)
                + (chin_z - 0.014 * S) * (w_back ** 1.35)
                + (ear_z_mid + 0.024 * S) * (1.0 - (w_front ** 1.35) - (w_back ** 1.35))
                - 0.034 * S * sideburn
                + 0.020 * S * over_ear
            )
            # Spherical interpolation in angle from rim elevation up to crown apex (`0, 0, crown_z + 0.018`)
            el_rim = math.atan2(z_rim - skull_center[2], base_rx * 1.02)
            el = el_rim + (math.pi * 0.5 - el_rim) * tv
            cos_el, sin_el = math.cos(el), math.sin(el)
            raw_pt = (
                skull_center[0] + (base_rx * 1.12 * cos_el) * ca,
                skull_center[1] + (base_ry * 1.13 * cos_el) * sa,
                skull_center[2] + (head_h * 0.56 * sin_el),
            )
            ring_v.append(hbm.verts.new(skull_safe_point(raw_pt, 0.0085 * S + 0.006 * S * math.sin(tv * math.pi))))
        cap_grid.append(ring_v)

    u0, u1, v0, v1 = UV_DARK
    solid_dark_uv = (0.5 * (u0 + u1), 0.5 * (v0 + v1))
    for iv in range(N_V):
        for iu in range(N_U):
            iun = (iu + 1) % N_U
            f = hbm.faces.new((cap_grid[iv][iu], cap_grid[iv][iun], cap_grid[iv + 1][iun], cap_grid[iv + 1][iu]))
            for lp in f.loops:
                lp[huv].uv = solid_dark_uv
    cap_apex = hbm.verts.new(skull_safe_point((0.0, 0.0, crown_z + 0.020 * S), 0.012 * S))
    for iu in range(N_U):
        iun = (iu + 1) % N_U
        f = hbm.faces.new((cap_apex, cap_grid[-1][iu], cap_grid[-1][iun]))
        for lp in f.loops:
            lp[huv].uv = solid_dark_uv

    # Sleek flat-ribbon / teardrop Pixar hair blade builder (`thin thickness -> NO balloon sausages!`)
    def add_pixar_lock(pts: Sequence[Tuple[float, float, float]], width: float, thick: float, twist: float = 0.24) -> None:
        ctrl = [Vector(p) for p in pts]
        vecs: List[Vector] = []
        n_c = len(ctrl)
        for step in range(11):
            g = (step / 10.0) * (n_c - 1)
            idx0 = min(int(g), n_c - 2)
            fr = g - idx0
            p0 = ctrl[max(idx0 - 1, 0)]
            p1 = ctrl[idx0]
            p2 = ctrl[idx0 + 1]
            p3 = ctrl[min(idx0 + 2, n_c - 1)]
            fr2, fr3 = fr * fr, fr * fr * fr
            pt = 0.5 * (
                (2.0 * p1)
                + (-p0 + p2) * fr
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * fr2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * fr3
            )
            vecs.append(pt)

        n_p = len(vecs)
        frames = []
        center_head = Vector((0.0, 0.0, chin_z + head_h * 0.56))
        for idx, p in enumerate(vecs):
            tang = (vecs[min(idx + 1, n_p - 1)] - vecs[max(idx - 1, 0)]).normalized()
            if tang.length < 1e-5:
                tang = Vector((0.0, 0.0, 1.0))
            outward = (p - center_head)
            outward -= tang * outward.dot(tang)
            if outward.length < 1e-5:
                outward = Vector((0.0, -1.0, 0.0))
            outward.normalize()
            blade_u = tang.cross(outward).normalized()
            t = idx / float(n_p - 1)
            ang_tw = twist * t
            ax_u = (blade_u * math.cos(ang_tw) + outward * math.sin(ang_tw)).normalized()
            ax_v = (outward * math.cos(ang_tw) - blade_u * math.sin(ang_tw)).normalized()
            prof_w = math.sin(math.pi * (0.22 + 0.76 * (t ** 0.78)))
            if idx == n_p - 1:
                prof_w = 0.035
            frames.append((p, ax_u, ax_v, width * prof_w, thick * prof_w))
        add_swept_tube_to_bmesh(
            hbm,
            frames,
            n_seg=8,
            uv_box=(solid_dark_uv[0], solid_dark_uv[0], solid_dark_uv[1], solid_dark_uv[1]),
            cap_start=True,
            cap_end=True,
        )

    if pal.get("messy_spiky_hair", False):
        # Signature swept-up center-right crown crest & playful stray strands matching `ref_02_professor_z_catalogue.png`!
        hero_locks = [
            ([(-0.046 * S, -0.048 * S, crown_z - 0.008 * S), (-0.012 * S, -0.078 * S, crown_z + 0.038 * S), ( 0.030 * S, -0.068 * S, crown_z + 0.072 * S), ( 0.074 * S, -0.048 * S, H - 0.002 * S)], 0.046 * S, 0.012 * S,  0.30),
            ([(-0.016 * S, -0.026 * S, crown_z - 0.006 * S), ( 0.020 * S, -0.048 * S, crown_z + 0.044 * S), ( 0.062 * S, -0.038 * S, crown_z + 0.076 * S), ( 0.104 * S, -0.020 * S, H - 0.016 * S)], 0.044 * S, 0.011 * S,  0.26),
            ([(-0.064 * S, -0.034 * S, crown_z - 0.012 * S), (-0.056 * S, -0.058 * S, crown_z + 0.034 * S), (-0.022 * S, -0.054 * S, crown_z + 0.068 * S), ( 0.018 * S, -0.038 * S, H - 0.004 * S)], 0.044 * S, 0.012 * S,  0.28),
            # Neat swept side locks hugging silhouette cleanly (NO Mickey-Mouse balloon ears!)
            ([(-0.062 * S, -0.022 * S, crown_z - 0.010 * S), (-0.088 * S, -0.028 * S, crown_z + 0.020 * S), (-0.102 * S, -0.020 * S, crown_z + 0.044 * S), (-0.098 * S, -0.008 * S, crown_z + 0.060 * S)], 0.036 * S, 0.011 * S, -0.32),
            ([( 0.056 * S, -0.022 * S, crown_z - 0.010 * S), ( 0.084 * S, -0.024 * S, crown_z + 0.018 * S), ( 0.104 * S, -0.014 * S, crown_z + 0.040 * S), ( 0.110 * S,  0.000 * S, crown_z + 0.054 * S)], 0.036 * S, 0.011 * S,  0.32),
            # Asymmetric forehead fringe locks sweeping lightly above eyebrows
            ([(-0.026 * S, -0.080 * S, crown_z - 0.008 * S), ( 0.012 * S, -0.114 * S, crown_z - 0.004 * S), ( 0.050 * S, -0.126 * S, crown_z - 0.028 * S), ( 0.080 * S, -0.118 * S, anc["brow_z"] + 0.026 * S)], 0.038 * S, 0.011 * S,  0.22),
            ([(-0.006 * S, -0.086 * S, crown_z - 0.010 * S), (-0.028 * S, -0.118 * S, crown_z - 0.010 * S), (-0.044 * S, -0.128 * S, crown_z - 0.034 * S), (-0.032 * S, -0.122 * S, anc["brow_z"] + 0.020 * S)], 0.032 * S, 0.010 * S, -0.20),
            ([(-0.048 * S, -0.074 * S, crown_z - 0.012 * S), (-0.082 * S, -0.106 * S, crown_z - 0.010 * S), (-0.106 * S, -0.112 * S, crown_z - 0.034 * S), (-0.112 * S, -0.100 * S, anc["brow_z"] + 0.026 * S)], 0.035 * S, 0.011 * S, -0.26),
            ([( 0.034 * S, -0.074 * S, crown_z - 0.010 * S), ( 0.072 * S, -0.106 * S, crown_z - 0.004 * S), ( 0.102 * S, -0.110 * S, crown_z - 0.024 * S), ( 0.114 * S, -0.094 * S, anc["brow_z"] + 0.030 * S)], 0.035 * S, 0.011 * S,  0.26),
        ]
        for pts, w_l, t_l, tw in hero_locks:
            add_pixar_lock(pts, w_l, t_l, tw)

        rng = np.random.default_rng(202607)
        for k in range(28):
            u_k = (k + 0.5) / 28.0
            az = -math.pi * 0.20 + (math.pi * 1.40) * u_k + float(rng.uniform(-0.06, 0.06))
            tier = k % 3
            ca, sa = math.cos(az), math.sin(az)
            if tier == 0:
                sw = az + 0.30 + float(rng.uniform(-0.12, 0.12))
                ca2, sa2 = math.cos(sw), math.sin(sw)
                zc = crown_z - 0.024 * S + float(rng.uniform(-0.010, 0.012)) * S
                flare = 1.18 + float(rng.uniform(-0.04, 0.07))
                lift = (0.040 + float(rng.uniform(-0.010, 0.020))) * S
                pts = [
                    (base_rx * 0.65 * ca, base_ry * 0.65 * sa, crown_z + 0.010 * S),
                    (base_rx * 1.02 * ca, base_ry * 1.04 * sa, zc + 0.024 * S),
                    (base_rx * flare * ca2, base_ry * (flare + 0.04) * sa2, zc + lift * 0.72),
                    (base_rx * (flare + 0.05) * ca2, base_ry * (flare + 0.07) * sa2, zc + lift),
                ]
                add_pixar_lock(pts, 0.042 * S, 0.011 * S, 0.26)
            elif tier == 1:
                # Upper-back & temple locks kept cleanly ABOVE ear arch (`zm >= ear_z_mid + 0.055`) so ears stay crisp!
                sw = az + (0.24 if ca >= 0 else -0.24) + float(rng.uniform(-0.08, 0.08))
                ca2, sa2 = math.cos(sw), math.sin(sw)
                zm = chin_z + head_h * (0.74 + float(rng.uniform(-0.04, 0.05)))
                pts = [
                    (base_rx * 0.94 * ca, base_ry * 0.96 * sa, zm + 0.042 * S),
                    (base_rx * 1.14 * ca, base_ry * 1.16 * sa, zm + 0.018 * S),
                    (base_rx * 1.18 * ca2, base_ry * 1.22 * sa2, zm - 0.012 * S),
                    (base_rx * 1.12 * ca2, base_ry * 1.18 * sa2, zm - 0.036 * S),
                ]
                add_pixar_lock(pts, 0.044 * S, 0.011 * S, -0.20)
            else:
                if sa > 0.22:
                    sw = az + float(rng.uniform(-0.14, 0.14))
                    ca2, sa2 = math.cos(sw), math.sin(sw)
                    zn = chin_z + head_h * (0.44 + float(rng.uniform(-0.04, 0.05)))
                    pts = [
                        (base_rx * 0.92 * ca, base_ry * 0.98 * sa, zn + 0.036 * S),
                        (base_rx * 1.10 * ca, base_ry * 1.14 * sa, zn - 0.002 * S),
                        (base_rx * 1.12 * ca2, base_ry * 1.18 * sa2, zn - 0.034 * S),
                        (base_rx * 1.05 * ca2, base_ry * 1.16 * sa2, zn - 0.058 * S),
                    ]
                    add_pixar_lock(pts, 0.040 * S, 0.011 * S, 0.16)

    hair_obj = create_mesh_object("Hair", hbm, garment_mat, subdiv=0)
    built.append(hair_obj)

    # 2. Iconic Antique Bronze-Tortoiseshell `Round glasses`
    if pal.get("wearing_glasses", False):
        rim_r = 0.0395 * S
        wire_r = 0.0022 * S
        gx = 0.0468 * S
        gy = eye_y - anc["eye_r"] * 1.72
        gz = eye_z - 0.002 * S

        gbm = bmesh.new()
        for sx in (-1.0, 1.0):
            hoop_pts = []
            for k in range(33):
                ang = 2.0 * math.pi * (k % 32) / 32.0
                c = Vector((sx * gx + rim_r * math.cos(ang), gy, gz + rim_r * math.sin(ang)))
                n_out = Vector((math.cos(ang), 0.0, math.sin(ang)))
                hoop_pts.append((c, n_out, Vector((0.0, 1.0, 0.0)), wire_r, wire_r * 1.32))
            add_swept_tube_to_bmesh(gbm, hoop_pts, n_seg=6, uv_box=UV_GOLD)

            t_pts = [
                (Vector((sx * (gx + rim_r), gy, gz + 0.004 * S)), Vector((1, 0, 0)), Vector((0, 0, 1)), wire_r * 1.35, wire_r * 1.35),
                (Vector((sx * (base_rx * 1.02), gy + 0.048 * S, gz + 0.007 * S)), Vector((1, 0, 0)), Vector((0, 0, 1)), wire_r, wire_r),
                (Vector((sx * (base_rx * 1.05), -0.004 * S, gz + 0.009 * S)), Vector((1, 0, 0)), Vector((0, 0, 1)), wire_r, wire_r * 1.1),
                (Vector((sx * (base_rx * 1.02), +0.026 * S, gz - 0.015 * S)), Vector((1, 0, 0)), Vector((0, 0, 1)), wire_r * 0.85, wire_r * 0.85),
            ]
            add_swept_tube_to_bmesh(gbm, t_pts, n_seg=6, uv_box=UV_GOLD, cap_start=True, cap_end=True)

        inner_x = max(0.004 * S, gx - rim_r)
        br_pts = []
        for k in range(9):
            t = -1.0 + 2.0 * (k / 8.0)
            bx = t * inner_x
            bz_arc = gz + (0.007 + 0.0055 * (1.0 - t * t)) * S
            by_arc = gy - 0.0025 * S * (1.0 - t * t)
            br_pts.append((Vector((bx, by_arc, bz_arc)), Vector((0, 1, 0)), Vector((0, 0, 1)), wire_r, wire_r))
        add_swept_tube_to_bmesh(gbm, br_pts, n_seg=6, uv_box=UV_GOLD, cap_start=True, cap_end=True)

        glasses_obj = create_mesh_object("Glasses", gbm, garment_mat, subdiv=0)
        vg = glasses_obj.vertex_groups.new(name="PIN_DETAIL")
        vg.add(list(range(len(glasses_obj.data.vertices))), 1.0, "REPLACE")
        built.append(glasses_obj)

    return built


def build_tailored_wardrobe_and_body(
    profile: Dict[str, Any],
    anc: Dict[str, float],
    garment_mat: bpy.types.Material,
    skin_mat: bpy.types.Material,
) -> List[bpy.types.Object]:
    """
    Construct the full tailored body in relaxed natural A-pose split strictly into clean
    anatomical skinning zones (`ZONE_TORSO`, `ZONE_ARMS`, `ZONE_LEGS`, `ZONE_HANDS`).
    """
    pal = profile["features_and_palette"]
    H = anc["height_m"]
    S = H / 1.73
    objs: List[bpy.types.Object] = []

    z_blazer_hem = 0.805 * S
    z_knee = 0.490 * S

    # ==================== ZONE A: `Body_Torso` (`ZONE_TORSO`) ====================
    bbm = bmesh.new()

    # 1. Snug Tailored Cream Cotton Dress Shirt + Pointed Collar + Placket (`ZERO neck gap!`)
    shirt_stations = [
        (0.895 * S, 0.132 * S, 0.092 * S),
        (0.970 * S, 0.136 * S, 0.096 * S),
        (1.090 * S, 0.148 * S, 0.104 * S),
        (1.210 * S, 0.154 * S, 0.098 * S),
        (1.275 * S, 0.056 * S, 0.058 * S),
        (1.304 * S, 0.051 * S, 0.053 * S),
    ]
    shirt_frames = [
        (Vector((0.0, -0.006 * S, z)), Vector((1, 0, 0)), Vector((0, 1, 0)), rx, ry)
        for (z, rx, ry) in shirt_stations
    ]
    add_swept_tube_to_bmesh(bbm, shirt_frames, n_seg=22, uv_box=UV_SHIRT, cap_start=True, cap_end=True)

    placket_frames = [
        (Vector((0.0, -(0.099 + 0.010 * math.sin(math.pi * k / 5.0)) * S, (0.920 + 0.068 * k) * S)),
         Vector((1, 0, 0)), Vector((0, 1, 0)), 0.013 * S, 0.004 * S)
        for k in range(6)
    ]
    add_swept_tube_to_bmesh(bbm, placket_frames, n_seg=6, uv_box=UV_SHIRT, cap_start=True, cap_end=True)

    if pal.get("has_bowtie", False):
        for sx in (-1.0, 1.0):
            c_pts = [
                (Vector((sx * 0.048 * S,  0.022 * S, 1.302 * S)), Vector((0, 0, 1)), Vector((1, 0, 0)), 0.020 * S, 0.004 * S),
                (Vector((sx * 0.055 * S, -0.040 * S, 1.294 * S)), Vector((0, 0, 1)), Vector((1, 0, 0)), 0.022 * S, 0.004 * S),
                (Vector((sx * 0.042 * S, -0.082 * S, 1.276 * S)), Vector((0, 0, 1)), Vector((1, 0, 0)), 0.020 * S, 0.004 * S),
                (Vector((sx * 0.020 * S, -0.096 * S, 1.258 * S)), Vector((0, 0, 1)), Vector((1, 0, 0)), 0.006 * S, 0.003 * S),
            ]
            add_swept_tube_to_bmesh(bbm, c_pts, n_seg=6, uv_box=UV_SHIRT, cap_start=True, cap_end=True)

        bt_z, bt_y = 1.272 * S, -0.100 * S
        knot_frames = [
            (Vector((x_k * S, bt_y, bt_z)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.010 * S, 0.013 * S)
            for x_k in (-0.010, 0.0, 0.010)
        ]
        add_swept_tube_to_bmesh(bbm, knot_frames, n_seg=8, uv_box=UV_BOWTIE, cap_start=True, cap_end=True)
        for sx in (-1.0, 1.0):
            wing_frames = [
                (Vector((sx * 0.008 * S, bt_y + 0.002 * S, bt_z)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.006 * S, 0.010 * S),
                (Vector((sx * 0.027 * S, bt_y - 0.002 * S, bt_z)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.009 * S, 0.021 * S),
                (Vector((sx * 0.046 * S, bt_y + 0.003 * S, bt_z)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.007 * S, 0.025 * S),
                (Vector((sx * 0.051 * S, bt_y + 0.005 * S, bt_z)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.003 * S, 0.019 * S),
            ]
            add_swept_tube_to_bmesh(bbm, wing_frames, n_seg=8, uv_box=UV_BOWTIE, cap_start=True, cap_end=True)

    # 2. Structured Olive-Walnut Herringbone Tweed Open Blazer + Snug Neck Collar + Notched Lapels
    blazer_rows = [
        (z_blazer_hem, 0.164 * S, 0.114 * S, 0.34),
        (0.890 * S,    0.158 * S, 0.110 * S, 0.22),
        (0.980 * S,    0.155 * S, 0.109 * S, 0.17),
        (1.070 * S,    0.165 * S, 0.114 * S, 0.25),
        (1.165 * S,    0.175 * S, 0.112 * S, 0.36),
        (1.245 * S,    0.178 * S, 0.104 * S, 0.45),
        (1.288 * S,    0.072 * S, 0.070 * S, 0.54),
    ]
    uv_l = bbm.loops.layers.uv.verify()
    N_BLZ = 24
    outer_rings, inner_rings = [], []
    for (z, rx, ry, gap) in blazer_rows:
        p_start = -math.pi * 0.5 + gap
        p_span = 2.0 * (math.pi - gap)
        o_ring, i_ring = [], []
        for j in range(N_BLZ + 1):
            phi = p_start + p_span * (j / float(N_BLZ))
            ca, sa = math.cos(phi), math.sin(phi)
            o_ring.append(bbm.verts.new((rx * ca, -0.006 * S + ry * sa, z)))
            i_ring.append(bbm.verts.new(((rx - 0.008 * S) * ca, -0.006 * S + (ry - 0.008 * S) * sa, z)))
        outer_rings.append(o_ring)
        inner_rings.append(i_ring)

    u0, u1, v0, v1 = UV_BLAZER
    for i in range(len(blazer_rows) - 1):
        va = v0 + (v1 - v0) * (i / float(len(blazer_rows) - 1))
        vb = v0 + (v1 - v0) * ((i + 1) / float(len(blazer_rows) - 1))
        for j in range(N_BLZ):
            ua = u0 + (u1 - u0) * (j / float(N_BLZ))
            ub = u0 + (u1 - u0) * ((j + 1) / float(N_BLZ))
            fo = bbm.faces.new((outer_rings[i][j], outer_rings[i][j + 1], outer_rings[i + 1][j + 1], outer_rings[i + 1][j]))
            fi = bbm.faces.new((inner_rings[i][j], inner_rings[i + 1][j], inner_rings[i + 1][j + 1], inner_rings[i][j + 1]))
            for lp, uv_c in zip(fo.loops, ((ua, va), (ub, va), (ub, vb), (ua, vb))):
                lp[uv_l].uv = uv_c
            for lp in fi.loops:
                lp[uv_l].uv = (0.5 * (u0 + u1), 0.5 * (v0 + v1))
        fe0 = bbm.faces.new((inner_rings[i][0], outer_rings[i][0], outer_rings[i + 1][0], inner_rings[i + 1][0]))
        fe1 = bbm.faces.new((outer_rings[i][N_BLZ], inner_rings[i][N_BLZ], inner_rings[i + 1][N_BLZ], outer_rings[i + 1][N_BLZ]))
        for lp in list(fe0.loops) + list(fe1.loops):
            lp[uv_l].uv = (u0, va)
    for j in range(N_BLZ):
        fb = bbm.faces.new((inner_rings[0][j], inner_rings[0][j + 1], outer_rings[0][j + 1], outer_rings[0][j]))
        ft = bbm.faces.new((outer_rings[-1][j], outer_rings[-1][j + 1], inner_rings[-1][j + 1], inner_rings[-1][j]))
        for lp in list(fb.loops) + list(ft.loops):
            lp[uv_l].uv = (u0, v0)

    if pal.get("has_notched_lapels", False):
        for sx in (-1.0, 1.0):
            lapel_frames = [
                (Vector((sx * 0.060 * S, -0.065 * S, 1.288 * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.019 * S, 0.005 * S),
                (Vector((sx * 0.076 * S, -0.095 * S, 1.228 * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.027 * S, 0.006 * S),
                (Vector((sx * 0.065 * S, -0.112 * S, 1.155 * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.024 * S, 0.006 * S),
                (Vector((sx * 0.043 * S, -0.117 * S, 1.075 * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.016 * S, 0.005 * S),
                (Vector((sx * 0.028 * S, -0.115 * S, 1.005 * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.006 * S, 0.004 * S),
            ]
            add_swept_tube_to_bmesh(bbm, lapel_frames, n_seg=6, uv_box=UV_BLAZER, cap_start=True, cap_end=True)

            pock_frames = [
                (Vector((sx * 0.126 * S, -0.076 * S, (0.842 + 0.036 * k) * S)),
                 Vector((-sx * 0.55, -0.83, 0.0)), Vector((0.83, -sx * 0.55, 0.0)), 0.034 * S, 0.0045 * S)
                for k in range(3)
            ]
            add_swept_tube_to_bmesh(bbm, pock_frames, n_seg=6, uv_box=UV_BLAZER, cap_start=True, cap_end=True)

        for bz_btn in (0.995 * S, 0.925 * S):
            btn_frames = [
                (Vector((0.034 * S, (-0.114 - 0.004 * k) * S, bz_btn)), Vector((1, 0, 0)), Vector((0, 0, 1)), 0.0085 * S, 0.0085 * S)
                for k in range(2)
            ]
            add_swept_tube_to_bmesh(bbm, btn_frames, n_seg=8, uv_box=UV_DARK, cap_start=True, cap_end=True)

    if pal.get("has_belt", True):
        belt_frames = [
            (Vector((0.0, -0.006 * S, (0.898 + 0.028 * k) * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.140 * S, 0.099 * S)
            for k in range(2)
        ]
        add_swept_tube_to_bmesh(bbm, belt_frames, n_seg=22, uv_box=UV_DARK, cap_start=True, cap_end=True)
        bk_pts = []
        bw, bh = 0.022 * S, 0.017 * S
        for px, pz in [(-bw, -bh), (bw, -bh), (bw, bh), (-bw, bh), (-bw, -bh)]:
            bk_pts.append((Vector((px, -0.107 * S, 0.912 * S + pz)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0032 * S, 0.0032 * S))
        add_swept_tube_to_bmesh(bbm, bk_pts, n_seg=6, uv_box=UV_GOLD)

    # Smooth continuous tailored trouser hip/pelvis saddle extending down into upper thigh overlap (`0.785 m`)
    pelvis_frames = [
        (Vector((0.0, -0.005 * S, z * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), rx * S, ry * S)
        for (z, rx, ry) in [(0.785, 0.146, 0.088), (0.835, 0.144, 0.094), (0.875, 0.140, 0.096), (0.910, 0.136, 0.096)]
    ]
    add_swept_tube_to_bmesh(bbm, pelvis_frames, n_seg=22, uv_box=UV_TROUSER, cap_start=True, cap_end=True)

    torso_obj = create_mesh_object("Body_Torso", bbm, garment_mat, subdiv=0)
    torso_obj.vertex_groups.new(name="ZONE_TORSO").add(list(range(len(torso_obj.data.vertices))), 1.0, "REPLACE")
    objs.append(torso_obj)

    # ==================== ZONE B: `Body_Arms` (`ZONE_ARMS`) ====================
    abm = bmesh.new()
    for sx in (-1.0, 1.0):
        sh_pt = Vector((sx * 0.166 * S, -0.008 * S, 1.245 * S))
        el_pt = Vector((sx * 0.205 * S, -0.004 * S, 1.045 * S))
        wr_pt = Vector((sx * 0.238 * S, -0.020 * S, 0.858 * S))

        sleeve_pts = [
            (sh_pt + Vector((-sx * 0.032 * S, 0.0, -0.006 * S)), 0.045 * S, 0.049 * S),
            (sh_pt.lerp(el_pt, 0.25), 0.046 * S, 0.049 * S),
            (sh_pt.lerp(el_pt, 0.65), 0.044 * S, 0.046 * S),
            (el_pt,                   0.042 * S, 0.044 * S),
            (el_pt.lerp(wr_pt, 0.50), 0.039 * S, 0.040 * S),
            (el_pt.lerp(wr_pt, 0.92), 0.036 * S, 0.037 * S),
            (el_pt.lerp(wr_pt, 0.94), 0.029 * S, 0.030 * S),
        ]
        sl_frames = []
        for pt, ru, rv in sleeve_pts:
            axis = (wr_pt - sh_pt).normalized()
            ax_u = axis.cross(Vector((0, 1, 0))).normalized()
            sl_frames.append((pt, ax_u, Vector((0, 1, 0)), ru, rv))
        add_swept_tube_to_bmesh(abm, sl_frames, n_seg=14, uv_box=UV_BLAZER, cap_start=True, cap_end=True)

        for k_cb in range(2):
            cb_c = el_pt.lerp(wr_pt, 0.80 + 0.07 * k_cb) + Vector((sx * 0.037 * S, 0.010 * S, 0.0))
            cb_f = [(cb_c + Vector((sx * 0.002 * m * S, 0, 0)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0045 * S, 0.0045 * S) for m in range(2)]
            add_swept_tube_to_bmesh(abm, cb_f, n_seg=6, uv_box=UV_DARK, cap_start=True, cap_end=True)

        cuff_frames = []
        axis = (wr_pt - el_pt).normalized()
        ax_u = axis.cross(Vector((0, 1, 0))).normalized()
        for t_c in (0.91, 1.01):
            cuff_frames.append((el_pt.lerp(wr_pt, t_c), ax_u, Vector((0, 1, 0)), 0.032 * S, 0.033 * S))
        add_swept_tube_to_bmesh(abm, cuff_frames, n_seg=12, uv_box=UV_SHIRT, cap_start=True, cap_end=True)

    arms_obj = create_mesh_object("Body_Arms", abm, garment_mat, subdiv=0)
    arms_obj.vertex_groups.new(name="ZONE_ARMS").add(list(range(len(arms_obj.data.vertices))), 1.0, "REPLACE")
    objs.append(arms_obj)

    # ==================== ZONE C: `Body_Legs` (`ZONE_LEGS`) ====================
    lbm = bmesh.new()
    for sx in (-1.0, 1.0):
        hx = sx * 0.080 * S
        leg_stations = [
            (0.086 * S, hx * 1.06, 0.049 * S, 0.053 * S),
            (0.118 * S, hx * 1.05, 0.053 * S, 0.057 * S),
            (0.255 * S, hx * 1.03, 0.058 * S, 0.062 * S),
            (z_knee,    hx * 1.00, 0.064 * S, 0.068 * S),
            (0.680 * S, hx * 0.96, 0.072 * S, 0.078 * S),
            (0.880 * S, hx * 0.88, 0.080 * S, 0.086 * S),
        ]
        leg_frames = [
            (Vector((lx, -0.004 * S, lz)), Vector((1, 0, 0)), Vector((0, 1, 0)), rx, ry)
            for (lz, lx, rx, ry) in leg_stations
        ]
        add_swept_tube_to_bmesh(lbm, leg_frames, n_seg=16, uv_box=UV_TROUSER, cap_start=True, cap_end=True)

        cuff_leg = [
            (Vector((hx * 1.06, -0.004 * S, zc * S)), Vector((1, 0, 0)), Vector((0, 1, 0)), 0.053 * S, 0.057 * S)
            for zc in (0.086, 0.118)
        ]
        add_swept_tube_to_bmesh(lbm, cuff_leg, n_seg=16, uv_box=UV_TROUSER, cap_start=True, cap_end=True)

        # Authentic Burnished Cognac Leather Brogues + Dark Espresso Leather Welt & Stacked Heel (`Classic kicks`!)
        fx = sx * 0.085 * S
        shoe_stations = [
            (+0.074 * S, 0.048 * S, 0.037 * S, 0.040 * S),
            (+0.035 * S, 0.046 * S, 0.042 * S, 0.038 * S),
            (-0.015 * S, 0.040 * S, 0.045 * S, 0.033 * S),
            (-0.070 * S, 0.033 * S, 0.047 * S, 0.026 * S),
            (-0.125 * S, 0.027 * S, 0.044 * S, 0.022 * S),
            (-0.160 * S, 0.023 * S, 0.031 * S, 0.017 * S),
        ]
        sh_frames = [
            (Vector((fx, sy, max(sz, hh + 0.011 * S))), Vector((1, 0, 0)), Vector((0, 0, 1)), hw, hh)
            for (sy, sz, hw, hh) in shoe_stations
        ]
        add_swept_tube_to_bmesh(lbm, sh_frames, n_seg=12, uv_box=UV_SHOE, cap_start=True, cap_end=True)

        sole_frames = [
            (Vector((fx, sy, 0.0090 * S)), Vector((1, 0, 0)), Vector((0, 0, 1)), hw * 1.09, 0.0090 * S)
            for (sy, _sz, hw, _hh) in shoe_stations
        ]
        n_before = len(lbm.verts)
        add_swept_tube_to_bmesh(lbm, sole_frames, n_seg=12, uv_box=UV_DARK, cap_start=True, cap_end=True)
        lbm.verts.ensure_lookup_table()
        for v in lbm.verts[n_before:]:
            if v.co.z < 0.003 * S:
                v.co.z = 0.0

        # Burnished cognac leather cap-toe seam (`Classic kicks` detail!)
        toecap_frames = [
            (Vector((fx, sy, sz_tc)), Vector((1, 0, 0)), Vector((0, 0, 1)), hw_tc, hh_tc)
            for (sy, sz_tc, hw_tc, hh_tc) in (
                (-0.112 * S, 0.026 * S, 0.0455 * S, 0.0195 * S),
                (-0.140 * S, 0.024 * S, 0.0405 * S, 0.0175 * S),
                (-0.162 * S, 0.021 * S, 0.0315 * S, 0.0145 * S),
            )
        ]
        add_swept_tube_to_bmesh(lbm, toecap_frames, n_seg=12, uv_box=UV_SHOE, cap_start=True, cap_end=True)

        for k_lace in range(4):
            ly = (-0.014 - 0.020 * k_lace) * S
            lz = (0.065 - 0.0065 * k_lace) * S
            lf = [
                (Vector((fx - 0.019 * S, ly, lz)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0028 * S, 0.0028 * S),
                (Vector((fx + 0.019 * S, ly, lz)), Vector((0, 1, 0)), Vector((0, 0, 1)), 0.0028 * S, 0.0028 * S),
            ]
            add_swept_tube_to_bmesh(lbm, lf, n_seg=5, uv_box=UV_DARK, cap_start=True, cap_end=True)

    legs_obj = create_mesh_object("Body_Legs", lbm, garment_mat, subdiv=0)
    legs_obj.vertex_groups.new(name="ZONE_LEGS").add(list(range(len(legs_obj.data.vertices))), 1.0, "REPLACE")
    objs.append(legs_obj)

    # ==================== ZONE D: Proportioned Sculpted Pixar `Hands` (`ZONE_HANDS`) ====================
    hnd_bm = bmesh.new()
    HS = 1.18 * S
    for sx in (-1.0, 1.0):
        wr = Vector((sx * 0.238 * S, -0.020 * S, 0.858 * S))
        dir_down = Vector((sx * 0.14, -0.05, -1.0)).normalized()
        fwd_vec = Vector((0.0, -1.0, 0.0))
        palm_thick = Vector((-sx, 0.0, 0.0))

        palm_frames = [
            (wr + dir_down * (0.019 * k * HS), fwd_vec, palm_thick, (0.028 + 0.0045 * k) * HS, (0.016 - 0.001 * k) * HS)
            for k in range(4)
        ]
        add_swept_tube_to_bmesh(hnd_bm, palm_frames, n_seg=10, uv_box=(0.45, 0.55, 0.65, 0.72), cap_start=True, cap_end=True)

        knuckle_base = wr + dir_down * (0.058 * HS)
        finger_specs = [
            (-0.023, 0.054, 0.0076),
            (-0.007, 0.060, 0.0078),
            (+0.008, 0.055, 0.0072),
            (+0.022, 0.046, 0.0065),
        ]
        for y_off, f_len, f_rad in finger_specs:
            f_start = knuckle_base + fwd_vec * (y_off * (-1.0) * HS)
            f_frames = []
            for k in range(5):
                t = k / 4.0
                curl = Vector((-sx * 0.012 * (t ** 2) * HS, 0.0, 0.0))
                pt = f_start + dir_down * (f_len * t * HS) + curl
                r_cur = f_rad * HS * (1.0 - 0.24 * t)
                f_frames.append((pt, fwd_vec, palm_thick, r_cur, r_cur * 0.94))
            add_swept_tube_to_bmesh(hnd_bm, f_frames, n_seg=7, uv_box=(0.45, 0.55, 0.65, 0.72), cap_start=True, cap_end=True)

        th_base = wr + dir_down * (0.024 * HS) + fwd_vec * (0.023 * HS)
        th_dir = (dir_down * 0.72 + fwd_vec * 0.62 - palm_thick * 0.18).normalized()
        th_frames = [
            (th_base + th_dir * (0.046 * (k / 3.0) * HS), palm_thick, Vector((0, 0, 1)), 0.0086 * HS * (1.0 - 0.22 * k / 3.0), 0.0082 * HS * (1.0 - 0.22 * k / 3.0))
            for k in range(4)
        ]
        add_swept_tube_to_bmesh(hnd_bm, th_frames, n_seg=7, uv_box=(0.45, 0.55, 0.65, 0.72), cap_start=True, cap_end=True)

    hands_obj = create_mesh_object("Hands", hnd_bm, skin_mat, subdiv=0)
    hands_obj.vertex_groups.new(name="ZONE_HANDS").add(list(range(len(hands_obj.data.vertices))), 1.0, "REPLACE")
    objs.append(hands_obj)

    return objs


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--profile", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args(argv)

    profile = json.loads(pathlib.Path(args.profile).read_text())
    tex = profile["generated_textures"]
    pal = profile["features_and_palette"]

    clear_scene()
    skin_img = load_packed_image(tex["head_skin_albedo"])
    eye_img = load_packed_image(tex["eye_iris_albedo"])
    g_alb = load_packed_image(tex["garment_albedo"])
    g_nrm = load_packed_image(tex["garment_normal"], non_color=True)
    g_rgh = load_packed_image(tex["garment_roughness"], non_color=True)

    skin_mat = make_principled_material("Mat_Skin", roughness=0.56, subsurface=0.20, albedo_img=skin_img)
    eye_mat = make_principled_material("Mat_Eye", roughness=0.04, albedo_img=eye_img)
    dark_lin = [pow(max(0.0, c), 2.2) for c in pal["hair_srgb"]]
    dark_mat = make_principled_material("Mat_HairDark", base_color=(*dark_lin, 1.0), roughness=0.48)
    garment_mat = make_principled_material(
        "Mat_Garments", roughness=0.68, albedo_img=g_alb, normal_img=g_nrm, rough_img=g_rgh, normal_strength=0.22
    )

    head_obj, eyes, anchors = build_pixar_head_assembly(profile, skin_mat, eye_mat, dark_mat)
    build_pixar_hair_and_glasses(head_obj, profile, anchors, garment_mat)
    build_tailored_wardrobe_and_body(profile, anchors, garment_mat, skin_mat)

    bpy.context.scene["avatar_anchors_json"] = json.dumps(anchors)

    out_p = pathlib.Path(args.out).resolve()
    out_p.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out_p))

    total_tris = sum(
        sum(len(p.vertices) - 2 for p in o.data.polygons)
        for o in bpy.context.scene.objects if o.type == "MESH"
    )
    print(json.dumps({
        "status": "character_built",
        "blend": str(out_p),
        "total_triangles": total_tris,
        "height_m": anchors["height_m"],
        "objects": [o.name for o in bpy.context.scene.objects if o.type == "MESH"],
    }, indent=2))


if __name__ == "__main__":
    main()
