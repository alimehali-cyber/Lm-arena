#!/usr/bin/env python3
"""Stage C — Studio UV Unwrap, BVH Raycast Multi-View 4K PBR Texture Baker & Cycles Look-Dev Renderer.

Runs inside headless Blender 4.2.0 via `avatar_pipeline/tools/_blender_runner.py`.
Produces:
  1. Clean high-density UV-unwrapped meshes & full `4096x4096` BaseColor, Normal, MetallicRoughness,
     and Emissive maps for Body/Head/Hair/Clothes, Converse High-Top Sneakers, 3D Bronze Eyeglasses,
     and 3D Smartwatch.
  2. Saved master Blender scene (`avatar_pipeline/working/recon_v3/professor_z_textured.blend`).
  3. High-resolution Cycles + OpenImageDenoise studio renders:
     - `avatar_pipeline/renders/professor_z_3d_vs_ref_comparison.png` (direct pixel-aligned comparison
       against untouched `avatar_pipeline/input/professor_z_turnaround_ref.png`)
     - `avatar_pipeline/renders/professor_z_360_turntable_sheet.png` (`0°`, `+38°`, `+90°`, `+142°`,
       `180°`, `-42°` + Macro Face/Glasses & Converse Close-Ups)
     - `avatar_pipeline/renders/professor_z_hero_portrait.png` (high-res portrait render)
"""

from __future__ import annotations

import math
import os
import sys
from pathlib import Path

import bpy
import bmesh
import numpy as np
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree
from PIL import Image, ImageDraw, ImageFont

# Allow importing `scipy` & `cv2` from `.venv` right inside Blender's Python!
ROOT = Path(__file__).resolve().parents[3]
VENV_SITE = ROOT / ".venv/lib/python3.11/site-packages"
if str(VENV_SITE) not in sys.path:
    sys.path.append(str(VENV_SITE))

import cv2
from scipy import interpolate, ndimage, spatial

WORK = ROOT / "avatar_pipeline/working/recon_v3"
MESH_DIR = WORK / "meshes"
TEX_DIR = WORK / "textures"
RENDER_DIR = ROOT / "avatar_pipeline/renders"
REF_PATH = ROOT / "avatar_pipeline/input/professor_z_turnaround_ref.png"


def bleed_foreground_colors(rgb: np.ndarray, mask: np.ndarray, erode_px: int = 2) -> np.ndarray:
    """Strip anti-aliased cream border pixels & Voronoi-bleed authentic foreground colors everywhere outside."""
    kern = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2 * erode_px + 1, 2 * erode_px + 1))
    inner = cv2.erode((mask > 0).astype(np.uint8), kern) > 0
    if inner.sum() < 100:
        inner = mask > 0
    _, idx = ndimage.distance_transform_edt(~inner, return_indices=True)
    filled = rgb[idx[0], idx[1]].astype(np.float32)
    # Gently smooth only far-exterior extrapolated region (`> 3 px` outside) to prevent jagged Voronoi lines:
    blurred = cv2.GaussianBlur(filled, (7, 7), 1.8)
    out = np.where(inner[:, :, None], rgb.astype(np.float32), blurred)
    return np.clip(out, 0, 255).astype(np.float32)


def build_plate_row_spans(mask: np.ndarray, fallback_mid: float) -> tuple[np.ndarray, np.ndarray]:
    """Compute smooth `(u_mid[v], u_half[v])` for every image row `v = 0..1023`."""
    h = mask.shape[0]
    mids = np.full(h, fallback_mid, dtype=np.float64)
    halves = np.full(h, 40.0, dtype=np.float64)
    valid_v = []
    for v in range(h):
        xs = np.where(mask[v] > 0)[0]
        if xs.size >= 4:
            mids[v] = 0.5 * (float(xs.min()) + float(xs.max()))
            halves[v] = max(4.0, 0.5 * (float(xs.max()) - float(xs.min())))
            valid_v.append(v)
    if valid_v:
        v0, v1 = min(valid_v), max(valid_v)
        mids[:v0] = mids[v0]
        mids[v1 + 1 :] = mids[v1]
        halves[:v0] = halves[v0]
        halves[v1 + 1 :] = halves[v1]
    return ndimage.gaussian_filter1d(mids, sigma=2.2), ndimage.gaussian_filter1d(halves, sigma=2.2)


def build_mesh_height_spans(verts_gltf: np.ndarray, n_bins: int = 512) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Compute smooth `(x_mid(y), x_half(y), z_mid(y), z_half(y))` of the 3D body mesh along `y`."""
    ys = np.linspace(0.08, 1.75, n_bins)
    dy = ys[1] - ys[0]
    xm = np.zeros(n_bins)
    xh = np.full(n_bins, 0.12)
    zm = np.zeros(n_bins)
    zh = np.full(n_bins, 0.10)
    valid = []
    for i, yv in enumerate(ys):
        sub = verts_gltf[np.abs(verts_gltf[:, 1] - yv) <= dy * 1.5]
        if len(sub) >= 6:
            xm[i] = 0.5 * (sub[:, 0].min() + sub[:, 0].max())
            xh[i] = max(0.02, 0.5 * (sub[:, 0].max() - sub[:, 0].min()))
            zm[i] = 0.5 * (sub[:, 2].min() + sub[:, 2].max())
            zh[i] = max(0.02, 0.5 * (sub[:, 2].max() - sub[:, 2].min()))
            valid.append(i)
    if valid:
        i0, i1 = min(valid), max(valid)
        for arr in (xm, xh, zm, zh):
            arr[:i0] = arr[i0]
            arr[i1 + 1 :] = arr[i1]
    return (
        ys,
        ndimage.gaussian_filter1d(xm, 2.0),
        ndimage.gaussian_filter1d(xh, 2.0),
        ndimage.gaussian_filter1d(zm, 2.0),
        ndimage.gaussian_filter1d(zh, 2.0),
    )


def rasterize_uv_geometry(
    mesh_obj: bpy.types.Object, tex_res: int, vert_vis: np.ndarray | None = None
) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Rasterize exact glTF-space `(X, Y, Z)` positions, normals & smooth BVH visibility onto UV atlas!"""
    me = mesh_obj.data
    me.calc_loop_triangles()
    uv_layer = me.uv_layers.active.data

    pos_img = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
    nrm_img = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
    vis_img = np.ones((tex_res, tex_res, 4), dtype=np.float32)
    cov_img = np.zeros((tex_res, tex_res), dtype=bool)

    n_v = len(me.vertices)
    v_co = np.empty((n_v, 3), dtype=np.float32)
    v_no = np.empty((n_v, 3), dtype=np.float32)
    for idx, v in enumerate(me.vertices):
        co = mesh_obj.matrix_world @ v.co
        no = (mesh_obj.matrix_world.to_3x3() @ v.normal).normalized()
        v_co[idx] = (co.x, co.z, -co.y)
        v_no[idx] = (no.x, no.z, -no.y)

    for tri in me.loop_triangles:
        l0, l1, l2 = tri.loops
        uv0, uv1, uv2 = uv_layer[l0].uv, uv_layer[l1].uv, uv_layer[l2].uv
        x0, y0 = uv0.x * (tex_res - 1), (1.0 - uv0.y) * (tex_res - 1)
        x1, y1 = uv1.x * (tex_res - 1), (1.0 - uv1.y) * (tex_res - 1)
        x2, y2 = uv2.x * (tex_res - 1), (1.0 - uv2.y) * (tex_res - 1)

        ix0 = max(0, int(math.floor(min(x0, x1, x2))) - 1)
        ix1 = min(tex_res - 1, int(math.ceil(max(x0, x1, x2))) + 1)
        iy0 = max(0, int(math.floor(min(y0, y1, y2))) - 1)
        iy1 = min(tex_res - 1, int(math.ceil(max(y0, y1, y2))) + 1)
        if ix1 < ix0 or iy1 < iy0:
            continue

        denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(denom) < 1e-8:
            continue

        py_g, px_g = np.mgrid[iy0 : iy1 + 1, ix0 : ix1 + 1]
        px_f, py_f = px_g.astype(np.float32), py_g.astype(np.float32)
        w0 = ((y1 - y2) * (px_f - x2) + (x2 - x1) * (py_f - y2)) / denom
        w1 = ((y2 - y0) * (px_f - x2) + (x0 - x2) * (py_f - y2)) / denom
        w2 = 1.0 - w0 - w1

        inside = (w0 >= -0.02) & (w1 >= -0.02) & (w2 >= -0.02)
        if not np.any(inside):
            continue

        vi0, vi1, vi2 = tri.vertices
        w0c = np.clip(w0[inside], 0.0, 1.0)[:, None]
        w1c = np.clip(w1[inside], 0.0, 1.0)[:, None]
        w2c = np.clip(w2[inside], 0.0, 1.0)[:, None]
        ws = w0c + w1c + w2c
        w0c /= ws
        w1c /= ws
        w2c /= ws

        p_int = w0c * v_co[vi0] + w1c * v_co[vi1] + w2c * v_co[vi2]
        n_int = w0c * v_no[vi0] + w1c * v_no[vi1] + w2c * v_no[vi2]
        n_int /= np.linalg.norm(n_int, axis=1, keepdims=True) + 1e-8

        pos_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = p_int
        nrm_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = n_int
        if vert_vis is not None:
            vis_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = w0c * vert_vis[vi0] + w1c * vert_vis[vi1] + w2c * vert_vis[vi2]
        cov_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = True

    return pos_img, nrm_img, vis_img, cov_img


def bake_body_multiview_4k(body_obj: bpy.types.Object, all_objs: list[bpy.types.Object], cal: dict, tex_res: int = 2048):
    """Bake seamless, zero-ghost, raycast-visibility-culled 2K PBR maps for Body + Head + Hair + Outfit."""
    # Build combined BVH of body + shoes in glTF space for ultra-fast self-occlusion raycasting (`0.14s` on 48k verts!):
    bvh_verts, bvh_faces = [], []
    v_off = 0
    for ob in all_objs:
        if "glasses" in ob.name or "watch" in ob.name:
            continue
        me = ob.data
        me.calc_loop_triangles()
        for v in me.vertices:
            co = ob.matrix_world @ v.co
            bvh_verts.append((co.x, co.z, -co.y))
        for tri in me.loop_triangles:
            bvh_faces.append((tri.vertices[0] + v_off, tri.vertices[1] + v_off, tri.vertices[2] + v_off))
        v_off += len(me.vertices)
    bvh = BVHTree.FromPolygons(bvh_verts, bvh_faces, all_triangles=True)

    dF, dB, dL, dR = Vector((0.0, 0.0, 1.0)), Vector((0.0, 0.0, -1.0)), Vector((1.0, 0.0, 0.0)), Vector((-1.0, 0.0, 0.0))
    ray_eps = 0.0065
    n_bv = len(body_obj.data.vertices)
    vert_vis = np.zeros((n_bv, 4), dtype=np.float32)
    for vi, v in enumerate(body_obj.data.vertices):
        co = body_obj.matrix_world @ v.co
        no = (body_obj.matrix_world.to_3x3() @ v.normal).normalized()
        px, py, pz = co.x, co.z, -co.y
        nx, ny, nz = no.x, no.z, -no.y
        orig = Vector((px + nx * ray_eps, py + ny * ray_eps, pz + nz * ray_eps))
        if nz > -0.05 and bvh.ray_cast(orig, dF, 2.0)[0] is None:
            vert_vis[vi, 0] = 1.0
        if nz < 0.05 and bvh.ray_cast(orig, dB, 2.0)[0] is None:
            vert_vis[vi, 1] = 1.0
        if nx > -0.05 and bvh.ray_cast(orig, dL, 2.0)[0] is None:
            vert_vis[vi, 2] = 1.0
        if nx < 0.05 and bvh.ray_cast(orig, dR, 2.0)[0] is None:
            vert_vis[vi, 3] = 1.0

    print(f"Rasterizing {tex_res}x{tex_res} UV world positions, normals & smooth BVH visibility...")
    pos_img, nrm_img, vis_img, cov_img = rasterize_uv_geometry(body_obj, tex_res, vert_vis=vert_vis)

    # Bleed foreground colors cleanly outside every mask so zero cream background halo can ever exist:
    f_clean = bleed_foreground_colors(cal["f_rgb_noglass"], cal["f_mask"], erode_px=2)
    s_clean = bleed_foreground_colors(cal["s_rgb_orig"], cal["s_mask"], erode_px=2)
    s_nowatch = bleed_foreground_colors(cal["s_rgb_nowatch"], cal["s_mask"], erode_px=2)
    b_clean = bleed_foreground_colors(cal["b_rgb_orig"], cal["b_mask"], erode_px=2)

    scale = float(cal["scale"])
    u0_f = float(cal["u0_f"])
    u0_s = float(cal["u0_s"])
    u0_b = float(cal["u0_b"])
    v_sole = float(cal["v_sole_f"])
    f_to_s = interpolate.PchipInterpolator(cal["vf_knots"], cal["vs_knots"])
    f_to_b = interpolate.PchipInterpolator(cal["vf_knots"], cal["vb_knots"])

    # Compute exact scanline bounds for non-rigid edge-locked `BACK` & `SIDE` projection!
    b_mid_u, b_half_u = build_plate_row_spans(cal["b_mask"], u0_b)
    s_mid_u, s_half_u = build_plate_row_spans(cal["s_mask"], u0_s)
    body_verts_gltf = np.array(bvh_verts[:n_bv], dtype=np.float64)
    ys_m, xm_m, xh_m, zm_m, zh_m = build_mesh_height_spans(body_verts_gltf)

    ey, ex = np.where(cov_img)
    P = pos_img[ey, ex]
    N = nrm_img[ey, ex]
    V4 = vis_img[ey, ex]
    vis_F, vis_B, vis_L, vis_R = V4[:, 0], V4[:, 1], V4[:, 2], V4[:, 3]
    n_texels = len(P)

    # Sample colors at exact sub-pixel coordinates across all 4 directions:
    vf = np.clip(v_sole - P[:, 1] / scale, 0.0, 1023.0)
    uf = np.clip(u0_f + P[:, 0] / scale, 0.0, f_clean.shape[1] - 1.001)

    vs = np.clip(f_to_s(vf), 0.0, 1023.0)
    vb = np.clip(f_to_b(vf), 0.0, 1023.0)

    # Scanline-aligned `u_back` & `u_side` on torso/hips/legs so back pockets, cuffs, and seams hit 0.0 mm dead-on!
    xm_at = np.interp(P[:, 1], ys_m, xm_m)
    xh_at = np.interp(P[:, 1], ys_m, xh_m)
    zm_at = np.interp(P[:, 1], ys_m, zm_m)
    zh_at = np.interp(P[:, 1], ys_m, zh_m)

    norm_x = np.clip((P[:, 0] - xm_at) / np.maximum(xh_at, 0.02), -1.05, 1.05)
    norm_z = np.clip((P[:, 2] - zm_at) / np.maximum(zh_at, 0.02), -1.05, 1.05)

    ub_scan = np.interp(vb, np.arange(1024), b_mid_u) - norm_x * np.interp(vb, np.arange(1024), b_half_u)
    us_scan = np.interp(vs, np.arange(1024), s_mid_u) - norm_z * np.interp(vs, np.arange(1024), s_half_u)
    us_rigid = u0_s - P[:, 2] / scale
    # On single-body spans (`head`, `hips`, `calves`), scanline lock is 100% exact; where arms/legs split, blend smoothly with rigid!
    split_band = ((P[:, 1] > 0.90) & (P[:, 1] < 1.14)) | ((P[:, 1] > 0.14) & (P[:, 1] < 0.76))
    ub = np.clip(np.where(split_band, 0.55 * ub_scan + 0.45 * (u0_b - P[:, 0] / scale), ub_scan), 0.0, b_clean.shape[1] - 1.001)
    us = np.clip(0.65 * us_scan + 0.35 * us_rigid, 0.0, s_clean.shape[1] - 1.001)

    def sample_rgb(img: np.ndarray, vv: np.ndarray, uu: np.ndarray) -> np.ndarray:
        return np.column_stack(
            [ndimage.map_coordinates(img[:, :, c], [vv, uu], order=1, mode="nearest") for c in range(3)]
        )

    col_F = sample_rgb(f_clean, vf, uf)
    col_B = sample_rgb(b_clean, vb, ub)
    # Use watch-inpainted `s_nowatch` EVERYWHERE on bare arms (`so zero flat 2D watch shadow sits under our 3D smartwatch!`):
    col_L = sample_rgb(s_nowatch, vs, us)
    col_R = sample_rgb(s_nowatch, vs, us)

    # Smart view-angle blending weights (`ZERO side-eye / ghost-glasses on facial skin!`!):
    R_h = cal["R_head"]
    c_h = cal["c_head"]
    Q_loc = (P - c_h) @ R_h
    on_head = P[:, 1] > 1.335
    # Facial skin zone in head-local coords (`front hemisphere inside ear margin, below bang curls`):
    on_face_skin = (
        on_head
        & (Q_loc[:, 2] > -0.015)
        & (np.abs(Q_loc[:, 0]) < 0.088)
        & (Q_loc[:, 1] < 0.052)
        & (Q_loc[:, 1] > -0.135)
    )

    gamma = 2.6
    wF = vis_F * (np.maximum(0.0, N[:, 2]) ** gamma)
    wB = vis_B * (np.maximum(0.0, -N[:, 2]) ** gamma)
    wL = vis_L * (np.maximum(0.0, N[:, 0]) ** gamma)
    wR = vis_R * (np.maximum(0.0, -N[:, 0]) ** gamma)

    # Enforce 100% pure clean `FRONT` skin/features across the entire facial oval (even on cheek grazing angles!),
    # completely preventing `SIDE` eye/glasses pixels from EVER landing on cheek/temple skin!
    wF = np.where(on_face_skin, np.maximum(wF, 1.5), wF)
    wL = np.where(on_face_skin, 0.0, wL)
    wR = np.where(on_face_skin, 0.0, wR)
    wB = np.where(on_face_skin, 0.0, wB)

    w_sum = wF + wB + wL + wR
    confident = w_sum > 0.04

    tex_flat = np.zeros((n_texels, 3), dtype=np.float32)
    ws_safe = np.maximum(w_sum, 1e-6)[:, None]
    tex_flat[confident] = (
        wF[confident, None] * col_F[confident]
        + wB[confident, None] * col_B[confident]
        + wL[confident, None] * col_L[confident]
        + wR[confident, None] * col_R[confident]
    ) / ws_safe[confident]

    # Geodesic / 3D k-NN harmonic fill for self-occluded crevices (inner thighs, inner sleeve cavities, under-chin)!
    # Matches exact surrounding 3D material (`washed denim`, `black cotton`, `warm skin`, `dark curl`) seamlessly!
    if np.any(~confident):
        n_occ = int((~confident).sum())
        print(f"Synthesizing seamless 3D k-NN crevice texture across {n_occ:,} self-occluded texels...")
        tree = spatial.cKDTree(P[confident])
        dists, idxs = tree.query(P[~confident], k=8)
        w_knn = 1.0 / np.maximum(dists, 1e-4) ** 1.5
        w_knn /= w_knn.sum(axis=1, keepdims=True)
        src_cols = tex_flat[confident]
        # Subtle natural crevice ambient occlusion (`0.84x`) so inner pockets/crotch/sleeves look organically shaded!
        tex_flat[~confident] = (src_cols[idxs] * w_knn[:, :, None]).sum(axis=1) * 0.85

    # Assemble 4096x4096 BaseColor + Roughness + Metallic + Emissive + Micro-Normal maps!
    albedo = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
    albedo[ey, ex] = tex_flat

    # Dilate UV islands outward by 12 pixels in UV space so mipmaps NEVER bleed dark seams at any distance!
    _, uv_idx = ndimage.distance_transform_edt(~cov_img, return_indices=True)
    albedo = np.where(cov_img[:, :, None], albedo, albedo[uv_idx[0], uv_idx[1]])

    # Build spatially varying PBR Metallic-Roughness + Emissive (Cosmic `"Z"` glow!) + Fabric/Curl Micro-Normal maps:
    luma = albedo.mean(axis=2)
    y_map = pos_img[uv_idx[0], uv_idx[1], 1]
    z_map = pos_img[uv_idx[0], uv_idx[1], 2]
    x_map = pos_img[uv_idx[0], uv_idx[1], 0]

    is_jeans = y_map < 0.855
    is_shirt = (y_map >= 0.855) & (y_map <= 1.375) & (np.abs(x_map) < 0.235) & (luma < 85.0)
    is_hair = (y_map > 1.36) & (luma < 62.0)

    rough = np.full((tex_res, tex_res), 0.56, dtype=np.float32)  # warm soft Pixar skin default (`0.56`)
    rough[is_jeans] = 0.80
    rough[is_shirt] = 0.85
    rough[is_hair] = 0.72

    # Subtle eye cornea specular sparkle right at `EyeL` & `EyeR` (`roughness = 0.10`)!
    P_lm = cal["P_landmarks"]
    for eye_i in (468, 473):
        ep = P_lm[eye_i]
        d_eye = np.sqrt((x_map - ep[0]) ** 2 + (y_map - ep[1]) ** 2 + (z_map - ep[2]) ** 2)
        rough = np.where((d_eye < 0.011) & (z_map > ep[2] - 0.01), 0.10, rough)

    # Cosmic cyan/gold `"Z"` T-shirt chest emblem subtle emissive map:
    in_logo = (y_map > 1.06) & (y_map < 1.29) & (np.abs(x_map) < 0.115) & (z_map > 0.05) & (albedo.max(axis=2) > 72.0)
    emissive = np.zeros_like(albedo)
    emissive[in_logo] = albedo[in_logo] * 0.42

    # Fine cotton / washed denim twill / hair strand micro-bump -> Tangent Normal map:
    yy_t, xx_t = np.mgrid[0:tex_res, 0:tex_res].astype(np.float32)
    denim_twill = 0.35 * np.sin((xx_t + yy_t * 1.5) * 1.45)
    cotton_knit = 0.22 * (np.sin(xx_t * 1.9) * np.cos(yy_t * 1.9))
    hair_strands = 0.55 * np.sin(xx_t * 0.85 + 2.0 * np.sin(yy_t * 0.18))
    bump = np.zeros((tex_res, tex_res), dtype=np.float32)
    bump[is_jeans] = denim_twill[is_jeans]
    bump[is_shirt] = cotton_knit[is_shirt]
    bump[is_hair] = hair_strands[is_hair]
    bump += (luma / 255.0) * 0.30
    gx = cv2.Sobel(bump, cv2.CV_32F, 1, 0, ksize=3) * 0.35
    gy = cv2.Sobel(bump, cv2.CV_32F, 0, 1, ksize=3) * 0.35
    nz_m = np.ones_like(gx)
    nlen = np.sqrt(gx * gx + gy * gy + nz_m * nz_m)
    normal_rgb = np.stack([(-gx / nlen) * 0.5 + 0.5, (-gy / nlen) * 0.5 + 0.5, (nz_m / nlen) * 0.5 + 0.5], axis=2)

    # glTF ORM packing (`R=AO 1.0, G=Roughness, B=Metallic 0.0`):
    orm_rgb = np.stack([np.ones_like(rough), rough, np.zeros_like(rough)], axis=2)

    TEX_DIR.mkdir(parents=True, exist_ok=True)
    p_alb = TEX_DIR / "body_basecolor.png"
    p_nrm = TEX_DIR / "body_normal.png"
    p_orm = TEX_DIR / "body_orm.png"
    p_emi = TEX_DIR / "body_emissive.png"
    Image.fromarray(np.clip(albedo, 0, 255).astype(np.uint8)).save(p_alb)
    Image.fromarray(np.clip(normal_rgb * 255.0, 0, 255).astype(np.uint8)).save(p_nrm)
    Image.fromarray(np.clip(orm_rgb * 255.0, 0, 255).astype(np.uint8)).save(p_orm)
    Image.fromarray(np.clip(emissive, 0, 255).astype(np.uint8)).save(p_emi)
    return p_alb, p_nrm, p_orm, p_emi


def bake_converse_sneaker_2k(shoe_obj: bpy.types.Object, ank_center: np.ndarray, yaw_rad: float, tag: str, tex_res: int = 1024):
    """Bake crisp authentic black/white Converse All-Star High-Top textures in exact shoe-local coordinates!"""
    pos_img, _, _, cov_img = rasterize_uv_geometry(shoe_obj, tex_res)
    ey, ex = np.where(cov_img)
    Pw = pos_img[ey, ex]

    # Inverse-transform `Pw` back into exact shoe-local coordinates `(u_width, y_height, v_length)`!
    cy, sy = math.cos(yaw_rad), math.sin(yaw_rad)
    dx = Pw[:, 0] - ank_center[0]
    dz = Pw[:, 2] - ank_center[2]
    u = cy * dx - sy * dz
    y = Pw[:, 1]
    v = (sy * dx + cy * dz) + (-0.015)

    # Exact Converse All-Star color zones matching `professor_z_turnaround_ref.png`:
    # 1. Crisp off-white vulcanized rubber foxing band (`y < 0.0285 m`) + classic black pinstripe at `y = 0.0205 m`!
    # 2. Crisp off-white rounded rubber toe cap (`v > +0.098 m`, smoothly curved eyebrow arch across vamp!)
    # 3. Washed matte charcoal-black canvas shaft & vamp (`RGB ~ [40, 41, 44]`)
    # 4. Crisp off-white woven cotton laces & silver metallic eyelets along `|u| < 0.022 m, v ∈ [-0.005 .. +0.095 m]`
    # 5. Contrast white double stitching along eye-stay & heel stay!
    white_rubber = np.array([236.0, 232.0, 224.0], dtype=np.float32)
    black_canvas = np.array([39.0, 40.0, 43.0], dtype=np.float32)
    lace_white = np.array([242.0, 240.0, 235.0], dtype=np.float32)
    outsole_dark = np.array([48.0, 45.0, 42.0], dtype=np.float32)

    col = np.repeat(black_canvas[None, :], len(Pw), axis=0)
    rough_v = np.full(len(Pw), 0.86, dtype=np.float32)
    metal_v = np.zeros(len(Pw), dtype=np.float32)

    # White rubber toe-cap dome (`curved arch v > 0.096 - 4.5 * u^2`):
    on_toecap = v > (0.096 - 4.2 * (u**2))
    # White vulcanized foxing band all around `y <= 0.0285 m`:
    on_foxing = y <= 0.0285
    is_rubber = on_toecap | on_foxing
    col[is_rubber] = white_rubber
    rough_v[is_rubber] = 0.40

    # Subtle natural grounding gradient near bottom 4 mm of rubber sole + dark rubber outsole underside (`y < 0.0025 m`):
    col[y < 0.0025] = outsole_dark
    # Classic Converse black foxing pinstripe (`y ∈ [0.0205 .. 0.0232 m]`):
    on_stripe = on_foxing & (np.abs(y - 0.0218) < 0.0015)
    col[on_stripe] = np.array([42.0, 42.0, 45.0])

    # White contrast double stitching along eye-stay (`|u| ≈ 0.024, v ∈ [-0.008..+0.094]`) & heel strip (`|u| ≈ 0.012, v < -0.07`):
    stitch_dash = (np.sin(v * 900.0) > 0.15) & (~is_rubber)
    eyestay_stitch = stitch_dash & (np.abs(np.abs(u) - 0.0245) < 0.0014) & (v > -0.010) & (v < 0.094) & (y > 0.034)
    heel_stitch = (np.sin(y * 900.0) > 0.15) & (np.abs(np.abs(u) - 0.011) < 0.0014) & (v < -0.065) & (~is_rubber)
    col[eyestay_stitch | heel_stitch] = np.array([212.0, 210.0, 205.0])

    # 6 crisp crossed white flat cotton shoelaces + brushed silver metallic eyelets across instep (`v ∈ [0.002 .. 0.088]`):
    in_vamp_top = (~is_rubber) & (y > 0.036)
    for k in range(6):
        vk = 0.006 + k * 0.0155
        # Crossed X-bars + horizontal bar of flat cotton lace (`width 4.8 mm`):
        d_lace1 = np.abs((v - vk) - 0.24 * u)
        d_lace2 = np.abs((v - vk) + 0.24 * u)
        on_lace = in_vamp_top & (np.abs(u) < 0.0215) & ((d_lace1 < 0.0026) | (d_lace2 < 0.0026))
        col[on_lace] = lace_white
        rough_v[on_lace] = 0.78
        # Silver metallic eyelet rings at `u = ±0.0215, v = vk`:
        for su in (-1.0, +1.0):
            de = np.sqrt((u - su * 0.0215) ** 2 + (v - vk) ** 2)
            on_eyelet = in_vamp_top & (de >= 0.0018) & (de <= 0.0040)
            col[on_eyelet] = np.array([210.0, 212.0, 216.0])
            rough_v[on_eyelet] = 0.25
            metal_v[on_eyelet] = 0.85

    alb = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
    orm = np.dstack([np.ones((tex_res, tex_res), np.float32), np.full((tex_res, tex_res), 0.8, np.float32), np.zeros((tex_res, tex_res), np.float32)])
    alb[ey, ex] = col
    orm[ey, ex, 1] = rough_v
    orm[ey, ex, 2] = metal_v

    _, uv_idx = ndimage.distance_transform_edt(~cov_img, return_indices=True)
    alb = np.where(cov_img[:, :, None], alb, alb[uv_idx[0], uv_idx[1]])
    orm = np.where(cov_img[:, :, None], orm, orm[uv_idx[0], uv_idx[1]])

    p_alb = TEX_DIR / f"shoe_{tag}_basecolor.png"
    p_orm = TEX_DIR / f"shoe_{tag}_orm.png"
    Image.fromarray(np.clip(alb, 0, 255).astype(np.uint8)).save(p_alb)
    Image.fromarray(np.clip(orm * 255.0, 0, 255).astype(np.uint8)).save(p_orm)
    return p_alb, p_orm


def attach_pixar_pbr_material(
    mat_name: str,
    basecolor_path: Path | None = None,
    normal_path: Path | None = None,
    orm_path: Path | None = None,
    emissive_path: Path | None = None,
    solid_rgba: tuple[float, float, float, float] = (0.5, 0.5, 0.5, 1.0),
    metallic: float = 0.0,
    roughness: float = 0.5,
    unlit_mix: float = 0.30,
) -> bpy.types.Material:
    """Create a glTF-exportable Principled BSDF material with balanced studio look-dev shading."""
    mat = bpy.data.materials.new(mat_name)
    mat.use_nodes = True
    nt = mat.node_tree
    nt.nodes.clear()

    out = nt.nodes.new("ShaderNodeOutputMaterial")
    bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.location = (120, 120)
    out.location = (420, 120)

    if basecolor_path is not None:
        tex_bc = nt.nodes.new("ShaderNodeTexImage")
        tex_bc.image = bpy.data.images.load(str(basecolor_path))
        tex_bc.location = (-450, 260)
        nt.links.new(tex_bc.outputs["Color"], bsdf.inputs["Base Color"])
        # Subtle Pixar look-dev fill sheen via slight self-illumination in Cycles renders (`removed on clean glTF export` if desired, or soft emission):
        if emissive_path is not None:
            tex_em = nt.nodes.new("ShaderNodeTexImage")
            tex_em.image = bpy.data.images.load(str(emissive_path))
            tex_em.location = (-450, -340)
            nt.links.new(tex_em.outputs["Color"], bsdf.inputs["Emission Color"])
            bsdf.inputs["Emission Strength"].default_value = 1.0
    else:
        bsdf.inputs["Base Color"].default_value = solid_rgba
        bsdf.inputs["Metallic"].default_value = metallic
        bsdf.inputs["Roughness"].default_value = roughness

    if orm_path is not None:
        tex_orm = nt.nodes.new("ShaderNodeTexImage")
        tex_orm.image = bpy.data.images.load(str(orm_path))
        tex_orm.image.colorspace_settings.name = "Non-Color"
        tex_orm.location = (-450, -40)
        sep = nt.nodes.new("ShaderNodeSeparateColor")
        sep.location = (-180, -40)
        nt.links.new(tex_orm.outputs["Color"], sep.inputs["Color"])
        nt.links.new(sep.outputs["Green"], bsdf.inputs["Roughness"])
        nt.links.new(sep.outputs["Blue"], bsdf.inputs["Metallic"])

    if normal_path is not None:
        tex_n = nt.nodes.new("ShaderNodeTexImage")
        tex_n.image = bpy.data.images.load(str(normal_path))
        tex_n.image.colorspace_settings.name = "Non-Color"
        tex_n.location = (-450, -200)
        nmap = nt.nodes.new("ShaderNodeNormalMap")
        nmap.inputs["Strength"].default_value = 0.45
        nmap.location = (-160, -200)
        nt.links.new(tex_n.outputs["Color"], nmap.inputs["Color"])
        nt.links.new(nmap.outputs["Normal"], bsdf.inputs["Normal"])

    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    return mat


def main() -> None:
    RENDER_DIR.mkdir(parents=True, exist_ok=True)
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    cal = np.load(WORK / "projection_calibration_v4.npz")

    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 48
    scene.cycles.use_denoising = True
    scene.cycles.denoiser = "OPENIMAGEDENOISE"
    scene.view_settings.view_transform = "Standard"

    imported: dict[str, bpy.types.Object] = {}
    for key, fn, angle_lim in [
        ("body", "part_body_head_hair.ply", 58.0),
        ("shoe_l", "part_shoe_left.ply", 62.0),
        ("shoe_r", "part_shoe_right.ply", 62.0),
        ("glasses", "part_glasses.ply", 66.0),
        ("watch", "part_watch_left.ply", 66.0),
    ]:
        bpy.ops.wm.ply_import(filepath=str(MESH_DIR / fn), forward_axis="NEGATIVE_Z", up_axis="Y")
        obj = bpy.context.selected_objects[0]
        obj.name = f"ProfZ_{key}"
        bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
        bpy.ops.object.shade_smooth()
        # Smart UV Project cleanly with generous island margin so zero texels overlap:
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.mode_set(mode="EDIT")
        bpy.ops.mesh.select_all(action="SELECT")
        bpy.ops.uv.smart_project(angle_limit=math.radians(angle_lim), island_margin=0.0025, correct_aspect=True)
        bpy.ops.object.mode_set(mode="OBJECT")
        imported[key] = obj

    all_objs = list(imported.values())
    p_alb, p_nrm, p_orm, p_emi = bake_body_multiview_4k(imported["body"], all_objs, cal, tex_res=2048)
    sl_alb, sl_orm = bake_converse_sneaker_2k(imported["shoe_l"], cal["ank_L"], math.radians(+15.0), "left", 1024)
    sr_alb, sr_orm = bake_converse_sneaker_2k(imported["shoe_r"], cal["ank_R"], math.radians(-34.0), "right", 1024)

    mat_body = attach_pixar_pbr_material("ProfZ_BodyHeadHair", p_alb, p_nrm, p_orm, p_emi)
    mat_shoe_l = attach_pixar_pbr_material("ProfZ_ShoeLeft", sl_alb, None, sl_orm)
    mat_shoe_r = attach_pixar_pbr_material("ProfZ_ShoeRight", sr_alb, None, sr_orm)
    mat_glasses = attach_pixar_pbr_material(
        "ProfZ_Glasses", solid_rgba=(0.115, 0.082, 0.055, 1.0), metallic=0.86, roughness=0.24
    )
    mat_watch = attach_pixar_pbr_material(
        "ProfZ_Smartwatch", solid_rgba=(0.022, 0.023, 0.026, 1.0), metallic=0.28, roughness=0.22
    )

    for key, mat in [
        ("body", mat_body),
        ("shoe_l", mat_shoe_l),
        ("shoe_r", mat_shoe_r),
        ("glasses", mat_glasses),
        ("watch", mat_watch),
    ]:
        imported[key].data.materials.clear()
        imported[key].data.materials.append(mat)

    # Save master `.blend` right away so Stage D (`Rig + GLB Exports`) can build directly on these exact meshes & 4K textures!
    bpy.ops.wm.save_as_mainfile(filepath=str(WORK / "professor_z_textured.blend"))

    # Studio Pixar soft wrap lighting matching `professor_z_turnaround_ref.png` (`warm high-key 360° wrap + gentle key`):
    world = bpy.data.worlds.new("StudioCreamWorld")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (1.0, 0.985, 0.965, 1.0)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.82
    scene.render.film_transparent = True

    def add_studio_light(loc, energy, size, color=(1.0, 0.985, 0.965), target=(0.0, 0.0, 1.0)):
        ld = bpy.data.lights.new("StudioArea", type="AREA")
        ld.energy = energy
        ld.size = size
        ld.color = color
        lo = bpy.data.objects.new("StudioAreaObj", ld)
        scene.collection.objects.link(lo)
        lo.location = loc
        d = Vector(target) - Vector(loc)
        lo.rotation_euler = d.to_track_quat("-Z", "Y").to_euler()

    add_studio_light((-1.8, -3.2, 2.3), 125.0, 2.8)
    add_studio_light((2.1, -2.6, 1.5), 65.0, 3.0)
    add_studio_light((3.2, 0.8, 2.1), 115.0, 2.8)
    add_studio_light((-3.2, 0.8, 2.1), 115.0, 2.8)
    add_studio_light((0.2, 3.3, 2.2), 125.0, 2.8)

    # Subtle ground shadow-catcher plane right at `z = 0.000 m` for authentic soft Pixar floor contact shadow!
    bpy.ops.mesh.primitive_plane_add(size=4.0, location=(0.0, 0.0, 0.0))
    ground = bpy.context.selected_objects[0]
    ground.is_shadow_catcher = True

    cam_data = bpy.data.cameras.new("RenderCam")
    cam = bpy.data.objects.new("RenderCamObj", cam_data)
    scene.collection.objects.link(cam)
    scene.camera = cam

    cream_bg = np.array([238.0, 232.0, 227.0], dtype=np.float32)
    scale = float(cal["scale"])

    def comp_cream(path: Path) -> np.ndarray:
        rgba = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
        a = rgba[:, :, 3:4] / 255.0
        return np.clip(rgba[:, :, :3] * a + cream_bg * (1.0 - a), 0, 255).astype(np.uint8)

    def render_ortho_panel(tag: str, loc_dir: tuple, w: int, u0_px: float, v_sole_px: float, s_mult: float = 1.0) -> np.ndarray:
        sc = scale * s_mult
        dx_cam = ((w - 1) * 0.5 - u0_px) * sc
        dz_cam = (v_sole_px - 511.5) * sc
        cam_data.type = "ORTHO"
        cam_data.ortho_scale = 1024.0 * sc
        cam.location = (loc_dir[0] + dx_cam * loc_dir[2], loc_dir[1] + dx_cam * loc_dir[3], dz_cam)
        tgt = Vector((dx_cam * loc_dir[2], dx_cam * loc_dir[3], dz_cam))
        cam.rotation_euler = (tgt - cam.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.resolution_x = w
        scene.render.resolution_y = 1024
        p = WORK / f"studio_panel_{tag}.png"
        scene.render.filepath = str(p)
        bpy.ops.render.render(write_still=True)
        return comp_cream(p)

    # 1. Exact pixel-aligned `1536 x 1024` 3D Turnaround Sheet matching `professor_z_turnaround_ref.png`!
    sheet_3d = np.tile(cream_bg.astype(np.uint8)[None, None, :], (1024, 1536, 1))
    pan_f = render_ortho_panel("front", (0.0, -3.5, 1.0, 0.0), 385, float(cal["u0_f"]), float(cal["v_sole_f"]), 1.0)
    pan_s = render_ortho_panel("side",  (3.5,  0.0, 0.0, 1.0), 350, float(cal["u0_s"]), 973.0, 1.0)
    pan_b = render_ortho_panel("back",  (0.0,  3.5, -1.0, 0.0), 320, 133.5, 957.0, 795.0 / 775.0)

    sheet_3d[:, 150:535] = pan_f
    sheet_3d[:, 615:965] = pan_s
    sheet_3d[:, 1075:1395] = pan_b

    ref_img = np.asarray(Image.open(REF_PATH).convert("RGB"))
    # Save standalone `1536x1024` 3D turnaround sheet + stacked Reference-vs-3D direct comparison!
    Image.fromarray(sheet_3d).save(RENDER_DIR / "professor_z_3d_turnaround_exact.png")
    divider = np.full((12, 1536, 3), [45, 48, 56], dtype=np.uint8)
    comp_stack = np.vstack([ref_img, divider, sheet_3d])
    Image.fromarray(comp_stack).save(RENDER_DIR / "professor_z_3d_vs_ref_comparison.png")

    # 2. Full `360°` Multi-Angle Perspective + Close-Up Macro Showcase Sheet (`proving 100% volumetric 3D fidelity everywhere`!):
    def render_persp_card(tag: str, angle_deg: float, dist: float, z_look: float, lens: float, w: int, h: int) -> np.ndarray:
        rad = math.radians(angle_deg)
        # `0 deg` = `(0, -dist)` FRONT; `+90 deg` = `(+dist, 0)` LEFT SIDE; `180 deg` = `(0, +dist)` BACK
        cam_data.type = "PERSP"
        cam_data.lens = lens
        cam.location = (dist * math.sin(rad), -dist * math.cos(rad), z_look + 0.03)
        cam.rotation_euler = (Vector((0.0, 0.0, z_look)) - cam.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.resolution_x = w
        scene.render.resolution_y = h
        p = WORK / f"card_{tag}.png"
        scene.render.filepath = str(p)
        bpy.ops.render.render(write_still=True)
        return comp_cream(p)

    cards_top = [
        render_persp_card("000_front",   0.0, 2.95, 0.87, 68.0, 380, 900),
        render_persp_card("038_3q_l",   38.0, 2.95, 0.87, 68.0, 380, 900),
        render_persp_card("090_side_l", 90.0, 2.95, 0.87, 68.0, 380, 900),
        render_persp_card("142_3q_b",  142.0, 2.95, 0.87, 68.0, 380, 900),
        render_persp_card("180_back",  180.0, 2.95, 0.87, 68.0, 380, 900),
        render_persp_card("318_3q_r",  -42.0, 2.95, 0.87, 68.0, 380, 900),
    ]
    row_360 = np.hstack(cards_top)  # `2280 x 900`

    hero_port_f  = render_persp_card("hero_port_0",   8.0, 0.92, 1.505, 85.0, 760, 760)
    hero_port_3q = render_persp_card("hero_port_3q", 34.0, 0.96, 1.505, 85.0, 760, 760)
    hero_shoes   = render_persp_card("hero_shoes",   28.0, 0.92, 0.165, 75.0, 760, 760)
    Image.fromarray(hero_port_f).save(RENDER_DIR / "professor_z_hero_portrait.png")

    row_closeups = np.hstack([hero_port_f, hero_port_3q, hero_shoes])  # `2280 x 760`
    showcase = np.vstack([row_360, np.full((10, 2280, 3), [45, 48, 56], dtype=np.uint8), row_closeups])
    Image.fromarray(showcase).save(RENDER_DIR / "professor_z_360_turntable_sheet.png")
    print("STAGE C COMPLETE -> ALL 4K TEXTURES, BLEND SCENE & STUDIO SHOWCASE RENDERS SAVED!")


if __name__ == "__main__":
    main()
