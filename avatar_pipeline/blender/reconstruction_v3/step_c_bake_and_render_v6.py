#!/usr/bin/env python3
"""Stage C v6 — Zero-Bleed Semantic Multi-Mesh PBR Texture Baker & Cycles Studio Renderer.

Bakes dedicated, razor-sharp, 100% zero-bleed PBR texture suites for each semantic part of
Professor Z and renders full orthographic turnaround comparisons + 360° turntable + close-ups.
"""

from __future__ import annotations

import math
import sys
from pathlib import Path

import bpy
import numpy as np
from mathutils import Vector
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
VENV_SITE = ROOT / ".venv/lib/python3.11/site-packages"
if str(VENV_SITE) not in sys.path:
    sys.path.append(str(VENV_SITE))

import cv2
from scipy import ndimage

WORK = ROOT / "avatar_pipeline/working/recon_v3"
MESH_DIR = WORK / "meshes_v6"
TEX_DIR = WORK / "textures_v6"
RENDER_DIR = ROOT / "avatar_pipeline/renders"
REF_PATH = ROOT / "avatar_pipeline/input/professor_z_turnaround_ref.png"


def rasterize_mesh_uv(mesh_obj: bpy.types.Object, tex_res: int) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Rasterize glTF-space `(X, Y, Z)` positions & unit normals onto `tex_res x tex_res` UV atlas."""
    me = mesh_obj.data
    me.calc_loop_triangles()
    uv_layer = me.uv_layers.active.data

    pos_img = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
    nrm_img = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
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

        ix0, ix1 = max(0, int(math.floor(min(x0, x1, x2))) - 1), min(tex_res - 1, int(math.ceil(max(x0, x1, x2))) + 1)
        iy0, iy1 = max(0, int(math.floor(min(y0, y1, y2))) - 1), min(tex_res - 1, int(math.ceil(max(y0, y1, y2))) + 1)
        if ix1 < ix0 or iy1 < iy0:
            continue

        denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(denom) < 1e-8:
            continue

        py_g, px_g = np.mgrid[iy0 : iy1 + 1, ix0 : ix1 + 1].astype(np.float32)
        w0 = ((y1 - y2) * (px_g - x2) + (x2 - x1) * (py_g - y2)) / denom
        w1 = ((y2 - y0) * (px_g - x2) + (x0 - x2) * (py_g - y2)) / denom
        w2 = 1.0 - w0 - w1

        inside = (w0 >= -0.02) & (w1 >= -0.02) & (w2 >= -0.02)
        if not np.any(inside):
            continue

        vi0, vi1, vi2 = tri.vertices
        w0c, w1c, w2c = np.clip(w0[inside], 0, 1)[:, None], np.clip(w1[inside], 0, 1)[:, None], np.clip(w2[inside], 0, 1)[:, None]
        ws = w0c + w1c + w2c
        w0c /= ws
        w1c /= ws
        w2c /= ws

        pos_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = w0c * v_co[vi0] + w1c * v_co[vi1] + w2c * v_co[vi2]
        n_int = w0c * v_no[vi0] + w1c * v_no[vi1] + w2c * v_no[vi2]
        n_int /= np.linalg.norm(n_int, axis=1, keepdims=True) + 1e-8
        nrm_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = n_int
        cov_img[iy0 : iy1 + 1, ix0 : ix1 + 1][inside] = True

    return pos_img, nrm_img, cov_img


def finish_and_save_maps(
    tag: str,
    cov_img: np.ndarray,
    col_valid: np.ndarray,
    rough_valid: np.ndarray,
    metal_valid: np.ndarray | None = None,
    emissive_valid: np.ndarray | None = None,
    bump_img: np.ndarray | None = None,
) -> tuple[Path, Path | None, Path, Path | None]:
    tex_res = cov_img.shape[0]
    ey, ex = np.where(cov_img)
    alb = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
    alb[ey, ex] = col_valid

    orm = np.dstack([np.ones((tex_res, tex_res), np.float32), np.full((tex_res, tex_res), 0.7, np.float32), np.zeros((tex_res, tex_res), np.float32)])
    orm[ey, ex, 1] = rough_valid
    if metal_valid is not None:
        orm[ey, ex, 2] = metal_valid

    _, uv_idx = ndimage.distance_transform_edt(~cov_img, return_indices=True)
    alb = np.where(cov_img[:, :, None], alb, alb[uv_idx[0], uv_idx[1]])
    orm = np.where(cov_img[:, :, None], orm, orm[uv_idx[0], uv_idx[1]])

    p_alb = TEX_DIR / f"{tag}_basecolor.png"
    p_orm = TEX_DIR / f"{tag}_orm.png"
    Image.fromarray(np.clip(alb, 0, 255).astype(np.uint8)).save(p_alb)
    Image.fromarray(np.clip(orm * 255.0, 0, 255).astype(np.uint8)).save(p_orm)

    p_nrm = None
    if bump_img is not None:
        gx = cv2.Sobel(bump_img, cv2.CV_32F, 1, 0, ksize=3) * 0.35
        gy = cv2.Sobel(bump_img, cv2.CV_32F, 0, 1, ksize=3) * 0.35
        nlen = np.sqrt(gx * gx + gy * gy + 1.0)
        nrm_rgb = np.stack([(-gx / nlen) * 0.5 + 0.5, (-gy / nlen) * 0.5 + 0.5, (1.0 / nlen) * 0.5 + 0.5], axis=2)
        p_nrm = TEX_DIR / f"{tag}_normal.png"
        Image.fromarray(np.clip(nrm_rgb * 255.0, 0, 255).astype(np.uint8)).save(p_nrm)

    p_emi = None
    if emissive_valid is not None:
        emi = np.zeros((tex_res, tex_res, 3), dtype=np.float32)
        emi[ey, ex] = emissive_valid
        emi = np.where(cov_img[:, :, None], emi, emi[uv_idx[0], uv_idx[1]])
        p_emi = TEX_DIR / f"{tag}_emissive.png"
        Image.fromarray(np.clip(emi, 0, 255).astype(np.uint8)).save(p_emi)

    return p_alb, p_nrm, p_orm, p_emi


def prepare_hairfree_face_plate(f_noglass: np.ndarray, f_mask: np.ndarray, P_lm: np.ndarray, scale: float, u0_f: float, v_sole: float) -> np.ndarray:
    """Create a 100% hair-free, background-free facial plate where dark curls outside eyes/brows are replaced by warm skin!

    Guarantees ZERO dark sideburn/hair blotches can EVER project onto `ProfZ_HeadSkin` cheeks, temples, or ears!
    """
    plate = f_noglass.copy()
    h, w, _ = plate.shape
    yy, xx = np.mgrid[0:h, 0:w]

    # Protect eyes + eyebrows strictly inside small radius around landmarks `[468, 473, 70..107, 300..336]`:
    protect = np.zeros((h, w), dtype=bool)
    keep_idx = [468, 473, 70, 63, 105, 66, 107, 300, 293, 334, 296, 336, 33, 133, 362, 263, 159, 145, 386, 374]
    for idx in keep_idx:
        ux = u0_f + P_lm[idx, 0] / scale
        vy = v_sole - P_lm[idx, 1] / scale
        r_px = 14.5 if idx in (468, 473) else 9.5
        protect |= ((xx - ux) ** 2 + (yy - vy) ** 2) <= (r_px**2)

    # Pure warm facial skin mask (`warm orange-peach pixels inside head rows 240..390`):
    r_ch = plate[:, :, 0].astype(np.float32)
    g_ch = plate[:, :, 1].astype(np.float32)
    b_ch = plate[:, :, 2].astype(np.float32)
    warm_skin = (f_mask > 0) & (yy >= 242) & (yy <= 388) & (r_ch > 95.0) & ((r_ch - b_ch) > 24.0)
    valid_face = warm_skin | (protect & (f_mask > 0))

    # Extrapolate pure warm cheek/forehead skin across ALL dark hair & background pixels via Voronoi + soft Gaussian!
    _, nn = ndimage.distance_transform_edt(~valid_face, return_indices=True)
    extrap = cv2.GaussianBlur(plate[nn[0], nn[1]].astype(np.float32), (15, 15), 4.0)
    out = np.where(valid_face[:, :, None], plate.astype(np.float32), extrap)
    return np.clip(out, 0, 255).astype(np.float32)


def bake_all_parts(imported: dict[str, bpy.types.Object], cal: dict) -> dict[str, tuple]:
    scale, u0_f, v_sole = float(cal["scale"]), float(cal["u0_f"]), float(cal["v_sole_f"])
    P_lm, R_h, c_h = cal["P_landmarks"], cal["R_head"], cal["c_head"]

    # 1. `ProfZ_HeadSkin` (`1024x1024` ultra-crisp facial features + seamless 360° warm Pixar ears, scalp & neck!):
    pos_h, nrm_h, cov_h = rasterize_mesh_uv(imported["head_skin"], 1024)
    ey, ex = np.where(cov_h)
    Ph, Nh = pos_h[ey, ex], nrm_h[ey, ex]
    face_plate = prepare_hairfree_face_plate(cal["f_rgb_noglass"], cal["f_mask"], P_lm, scale, u0_f, v_sole)

    vf = np.clip(v_sole - Ph[:, 1] / scale, 0.0, 1023.0)
    uf = np.clip(u0_f + Ph[:, 0] / scale, 0.0, face_plate.shape[1] - 1.001)
    col_front = np.column_stack([ndimage.map_coordinates(face_plate[:, :, c], [vf, uf], order=1, mode="nearest") for c in range(3)])

    # Seamless 3D warm Pixar skin base for ears, back of neck & jaw underside (`matched directly to Professor Z's exact skin tone`!):
    Q_loc = (Ph - c_h) @ R_h
    light_up = 0.5 + 0.5 * np.clip(Nh[:, 1] * 0.5 + Nh[:, 2] * 0.5, -1.0, 1.0)
    skin_base = (
        np.array([196.0, 122.0, 78.0])[None, :] * (1.0 - light_up[:, None])
        + np.array([235.0, 166.0, 118.0])[None, :] * light_up[:, None]
    )
    # Warm rosy ear helix & cheek subsurface scattering tint:
    on_ear = np.abs(Q_loc[:, 0]) > 0.088
    skin_base[on_ear] = skin_base[on_ear] * np.array([1.02, 0.94, 0.90])

    # Blend 100% pure projected `face_plate` across front face (`Nh_z > 0.15`, `Q_z > 0.01`), smoothly fading to `skin_base` around ears & neck!
    w_proj = np.clip((Nh[:, 2] - 0.08) / 0.42, 0.0, 1.0) * np.clip((Q_loc[:, 2] + 0.005) / 0.035, 0.0, 1.0)
    w_proj *= np.clip((Ph[:, 1] - 1.355) / 0.045, 0.25, 1.0)
    col_head = col_front * w_proj[:, None] + skin_base * (1.0 - w_proj[:, None])

    rough_h = np.full(len(Ph), 0.54, dtype=np.float32)
    for eye_i in (468, 473):
        ep = P_lm[eye_i]
        de = np.linalg.norm(Ph - ep, axis=1)
        rough_h = np.where(de < 0.0115, 0.08, rough_h)

    maps_head = finish_and_save_maps("head_skin", cov_h, col_head, rough_h)

    # 2. `ProfZ_ArmsSkin` (`512x512` spotless warm sunlit Pixar forearms & biceps — ZERO shirt bleed!):
    pos_a, nrm_a, cov_a = rasterize_mesh_uv(imported["arms_skin"], 512)
    eya, exa = np.where(cov_a)
    Pa, Na = pos_a[eya, exa], nrm_a[eya, exa]
    out_lit = np.clip(0.48 + 0.35 * np.abs(Na[:, 0]) + 0.22 * Na[:, 2] + 0.15 * Na[:, 1], 0.0, 1.0)
    sleeve_ao = np.clip((1.175 - Pa[:, 1]) / 0.055, 0.35, 1.0)  # subtle natural shadow inside short-sleeve cuff!
    pocket_ao = np.clip((Pa[:, 1] - 0.882) / 0.035, 0.55, 1.0)  # subtle shadow right inside jeans pocket lip!
    sh_a = out_lit * sleeve_ao * pocket_ao
    col_arms = np.array([186.0, 114.0, 72.0])[None, :] * (1.0 - sh_a[:, None]) + np.array([239.0, 174.0, 127.0])[None, :] * sh_a[:, None]
    maps_arms = finish_and_save_maps("arms_skin", cov_a, col_arms, np.full(len(Pa), 0.55, np.float32))

    # 3. `ProfZ_Hair` (`1024x1024` rich dark espresso Pixar curls with sculpted strand sheen & curl AO — ZERO skin blotches!):
    pos_hr, nrm_hr, cov_hr = rasterize_mesh_uv(imported["hair"], 1024)
    eyh, exh = np.where(cov_hr)
    Phr, Nhr = pos_hr[eyh, exh], nrm_hr[eyh, exh]
    Q_hr = (Phr - c_h) @ R_h
    rad_out = np.linalg.norm(Q_hr / np.array([0.10, 0.12, 0.11]), axis=1)
    curl_tip = np.clip((rad_out - 0.94) / 0.24, 0.0, 1.0)
    strand_wave = 0.5 + 0.5 * np.sin(np.arctan2(Q_hr[:, 2], Q_hr[:, 0]) * 26.0 + Q_hr[:, 1] * 140.0)
    crown_lit = np.clip(0.35 + 0.45 * Nhr[:, 1] + 0.20 * np.abs(Nhr[:, 2]), 0.0, 1.0)
    h_mix = np.clip(0.55 * curl_tip * crown_lit + 0.28 * strand_wave * curl_tip, 0.0, 1.0)
    col_hair = np.array([22.0, 17.5, 15.5])[None, :] * (1.0 - h_mix[:, None]) + np.array([58.0, 44.0, 36.0])[None, :] * h_mix[:, None]
    yy_hr, xx_hr = np.mgrid[0:1024, 0:1024].astype(np.float32)
    bump_hr = 0.65 * np.sin(xx_hr * 0.9 + 2.2 * np.sin(yy_hr * 0.15))
    maps_hair = finish_and_save_maps("hair", cov_hr, col_hair, np.full(len(Phr), 0.63, np.float32), bump_img=bump_hr)

    # 4. `ProfZ_TShirt` (`1024x1024` crew-neck washed black jersey cotton + exact `FRONT` Cosmic `"Z"` chest emblem!):
    pos_t, nrm_t, cov_t = rasterize_mesh_uv(imported["tshirt"], 1024)
    eyt, ext = np.where(cov_t)
    Pt, Nt = pos_t[eyt, ext], nrm_t[eyt, ext]
    # Base washed matte black cotton matching reference `RGB [31, 33, 37]` with subtle fabric fold shading:
    fold_sh = np.clip(0.45 + 0.35 * Nt[:, 1] + 0.25 * np.abs(Nt[:, 2]), 0.0, 1.0)
    col_ts = np.array([23.0, 24.5, 27.5])[None, :] * (1.0 - fold_sh[:, None]) + np.array([40.0, 42.5, 47.0])[None, :] * fold_sh[:, None]
    emi_ts = np.zeros_like(col_ts)

    # Project exact cosmic `"Z"` emblem cleanly onto front chest (`z > +0.035, Nz > 0.20, |x| < 0.118, y ∈ [1.045..1.300]`):
    on_chest = (Pt[:, 2] > 0.035) & (Nt[:, 2] > 0.20) & (np.abs(Pt[:, 0]) < 0.118) & (Pt[:, 1] > 1.045) & (Pt[:, 1] < 1.300)
    if np.any(on_chest):
        f_orig = cal["f_rgb_orig"].astype(np.float32)
        v_c = np.clip(v_sole - Pt[on_chest, 1] / scale, 0.0, 1023.0)
        u_c = np.clip(u0_f + Pt[on_chest, 0] / scale, 0.0, f_orig.shape[1] - 1.001)
        sampled_z = np.column_stack([ndimage.map_coordinates(f_orig[:, :, c], [v_c, u_c], order=1, mode="nearest") for c in range(3)])
        # Smooth alpha key for the bright cosmic cyan/violet/gold `"Z"` graphic + crisp border over black cotton:
        z_chroma = sampled_z.max(axis=1) - sampled_z.min(axis=1)
        z_bright = sampled_z.max(axis=1)
        w_logo = np.clip((np.maximum(z_chroma * 1.4, z_bright - 40.0)) / 32.0, 0.0, 1.0)
        col_ts[on_chest] = col_ts[on_chest] * (1.0 - w_logo[:, None]) + sampled_z * w_logo[:, None]
        emi_ts[on_chest] = sampled_z * (0.38 * w_logo[:, None])

    yy_t, xx_t = np.mgrid[0:1024, 0:1024].astype(np.float32)
    bump_ts = 0.26 * (np.sin(xx_t * 2.0) * np.cos(yy_t * 2.0))
    maps_ts = finish_and_save_maps("tshirt", cov_t, col_ts, np.full(len(Pt), 0.85, np.float32), emissive_valid=emi_ts, bump_img=bump_ts)

    # 5. `ProfZ_Jeans` (`1024x1024` slim dark indigo-charcoal washed denim + stitched pockets + grey-blue rolled cuffs!):
    pos_j, nrm_j, cov_j = rasterize_mesh_uv(imported["jeans"], 1024)
    eyj, exj = np.where(cov_j)
    Pj, Nj = pos_j[eyj, exj], nrm_j[eyj, exj]

    # Authentic dark charcoal-indigo denim base + subtle vertical knee/thigh whisker wash fade matching reference:
    leg_center_x = np.where(Pj[:, 0] < 0.0, -0.105, +0.095)
    center_fade = np.exp(-((Pj[:, 0] - leg_center_x) / 0.036) ** 2) * np.exp(-((Pj[:, 1] - 0.52) / 0.22) ** 2) * np.abs(Nj[:, 2])
    lit_j = np.clip(0.40 + 0.45 * np.abs(Nj[:, 2]) + 0.20 * np.abs(Nj[:, 0]), 0.0, 1.0)
    col_j = (
        np.array([29.0, 33.0, 38.0])[None, :] * (1.0 - lit_j[:, None])
        + np.array([45.0, 50.0, 57.0])[None, :] * lit_j[:, None]
        + np.array([15.0, 17.0, 20.0])[None, :] * center_fade[:, None]
    )

    # Subtle denim seam & pocket topstitching (`front fly, curved front scoop pockets, outseam, back patch pockets`):
    fly_seam = (Pj[:, 2] > 0.04) & (Pj[:, 1] > 0.72) & (Pj[:, 1] < 0.86) & (np.abs(Pj[:, 0] - 0.002) < 0.003)
    front_pock_arc = (
        (Pj[:, 2] > 0.03)
        & (Pj[:, 1] > 0.78)
        & (Pj[:, 1] < 0.86)
        & (np.abs(np.sqrt((np.abs(Pj[:, 0]) - 0.138) ** 2 + (Pj[:, 1] - 0.860) ** 2) - 0.056) < 0.0028)
    )
    outseam = (Pj[:, 1] > 0.173) & (np.abs(Nj[:, 0]) > 0.88) & (np.abs(Pj[:, 2] - 0.015) < 0.004)
    # Two back patch pockets on seat (`z < -0.025, y ∈ [0.735 .. 0.825], |x| ∈ [0.032 .. 0.112]`):
    in_bp_box = (Pj[:, 2] < -0.025) & (Pj[:, 1] >= 0.735) & (Pj[:, 1] <= 0.825) & (np.abs(Pj[:, 0]) >= 0.032) & (np.abs(Pj[:, 0]) <= 0.112)
    bp_border = in_bp_box & (
        (np.abs(Pj[:, 1] - 0.825) < 0.003)
        | (np.abs(Pj[:, 1] - 0.735) < 0.003)
        | (np.abs(np.abs(Pj[:, 0]) - 0.032) < 0.003)
        | (np.abs(np.abs(Pj[:, 0]) - 0.112) < 0.003)
    )
    col_j[in_bp_box] *= 0.94
    col_j[fly_seam | front_pock_arc | outseam | bp_border] = np.array([62.0, 67.0, 74.0])

    # Crisp turned-up reverse-weave grey-blue rolled denim cuffs (`y <= 0.1725 m` — matches `FRONT/SIDE/BACK` cuffs 100%!):
    on_cuff = Pj[:, 1] <= 0.1725
    cuff_shade = np.clip(0.55 + 0.45 * (np.abs(Nj[:, 2]) * 0.6 + np.abs(Nj[:, 0]) * 0.5), 0.0, 1.0)
    col_cuff = np.array([92.0, 99.0, 107.0])[None, :] * (1.0 - cuff_shade[:, None]) + np.array([138.0, 145.0, 153.0])[None, :] * cuff_shade[:, None]
    col_j[on_cuff] = col_cuff[on_cuff]
    # Top edge fold shadow & double chain-stitch line along top of rolled cuff:
    cuff_stitch = (np.abs(Pj[:, 1] - 0.1685) < 0.0018) & on_cuff
    col_j[cuff_stitch] = np.array([78.0, 84.0, 92.0])

    bump_j = 0.36 * np.sin((xx_t + yy_t * 1.5) * 1.55)
    maps_j = finish_and_save_maps("jeans", cov_j, col_j, np.full(len(Pj), 0.80, np.float32), bump_img=bump_j)

    # 6. `ProfZ_ShoeLeft` & `ProfZ_ShoeRight` (`1024x1024` High-Top Converse All-Stars):
    import step_c_uv_bake_and_render as c_prev
    c_prev.TEX_DIR = TEX_DIR
    sl_alb, sl_orm = c_prev.bake_converse_sneaker_2k(imported["shoe_l"], cal["ank_L"], math.radians(+15.0), "left", 1024)
    sr_alb, sr_orm = c_prev.bake_converse_sneaker_2k(imported["shoe_r"], cal["ank_R"], math.radians(-32.0), "right", 1024)

    return {
        "head_skin": maps_head,
        "arms_skin": maps_arms,
        "hair": maps_hair,
        "tshirt": maps_ts,
        "jeans": maps_j,
        "shoe_l": (sl_alb, None, sl_orm, None),
        "shoe_r": (sr_alb, None, sr_orm, None),
    }


def make_mat(
    name: str,
    maps: tuple | None = None,
    solid_rgba: tuple = (0.5, 0.5, 0.5, 1.0),
    metallic: float = 0.0,
    roughness: float = 0.5,
) -> bpy.types.Material:
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    nt.nodes.clear()
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
    bsdf.location = (120, 120)
    out.location = (420, 120)

    if maps is not None:
        p_alb, p_nrm, p_orm, p_emi = maps
        t_bc = nt.nodes.new("ShaderNodeTexImage")
        t_bc.image = bpy.data.images.load(str(p_alb))
        t_bc.location = (-450, 260)
        nt.links.new(t_bc.outputs["Color"], bsdf.inputs["Base Color"])

        t_orm = nt.nodes.new("ShaderNodeTexImage")
        t_orm.image = bpy.data.images.load(str(p_orm))
        t_orm.image.colorspace_settings.name = "Non-Color"
        t_orm.location = (-450, -30)
        sep = nt.nodes.new("ShaderNodeSeparateColor")
        sep.location = (-180, -30)
        nt.links.new(t_orm.outputs["Color"], sep.inputs["Color"])
        nt.links.new(sep.outputs["Green"], bsdf.inputs["Roughness"])
        nt.links.new(sep.outputs["Blue"], bsdf.inputs["Metallic"])

        if p_nrm is not None:
            t_n = nt.nodes.new("ShaderNodeTexImage")
            t_n.image = bpy.data.images.load(str(p_nrm))
            t_n.image.colorspace_settings.name = "Non-Color"
            t_n.location = (-450, -210)
            nm = nt.nodes.new("ShaderNodeNormalMap")
            nm.inputs["Strength"].default_value = 0.35
            nm.location = (-160, -210)
            nt.links.new(t_n.outputs["Color"], nm.inputs["Color"])
            nt.links.new(nm.outputs["Normal"], bsdf.inputs["Normal"])

        if p_emi is not None:
            t_e = nt.nodes.new("ShaderNodeTexImage")
            t_e.image = bpy.data.images.load(str(p_emi))
            t_e.location = (-450, -380)
            nt.links.new(t_e.outputs["Color"], bsdf.inputs["Emission Color"])
            bsdf.inputs["Emission Strength"].default_value = 1.0
    else:
        bsdf.inputs["Base Color"].default_value = solid_rgba
        bsdf.inputs["Metallic"].default_value = metallic
        bsdf.inputs["Roughness"].default_value = roughness

    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])
    return mat


def main() -> None:
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    RENDER_DIR.mkdir(parents=True, exist_ok=True)
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    sys.path.insert(0, str(WORK))
    cal = np.load(WORK / "calibration_v6.npz")

    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 40
    scene.cycles.use_denoising = True
    scene.cycles.denoiser = "OPENIMAGEDENOISE"
    scene.view_settings.view_transform = "Standard"

    part_specs = [
        ("head_skin", "part_head_skin.ply", 58.0),
        ("arms_skin", "part_arms_skin.ply", 62.0),
        ("hair", "part_hair.ply", 64.0),
        ("tshirt", "part_tshirt.ply", 60.0),
        ("jeans", "part_jeans.ply", 60.0),
        ("shoe_l", "part_shoe_left.ply", 62.0),
        ("shoe_r", "part_shoe_right.ply", 62.0),
        ("glasses", "part_glasses.ply", 66.0),
        ("watch", "part_watch_left.ply", 66.0),
    ]
    imported: dict[str, bpy.types.Object] = {}
    for key, fn, ang in part_specs:
        bpy.ops.wm.ply_import(filepath=str(MESH_DIR / fn), forward_axis="NEGATIVE_Z", up_axis="Y")
        obj = bpy.context.selected_objects[0]
        obj.name = f"ProfZ_{key}"
        bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
        bpy.ops.object.shade_smooth()
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.mode_set(mode="EDIT")
        bpy.ops.mesh.select_all(action="SELECT")
        bpy.ops.uv.smart_project(angle_limit=math.radians(ang), island_margin=0.003, correct_aspect=True)
        bpy.ops.object.mode_set(mode="OBJECT")
        imported[key] = obj

    print("Baking all semantic zero-bleed PBR maps...")
    baked = bake_all_parts(imported, cal)

    for key, maps in baked.items():
        mat = make_mat(f"Mat_{key}", maps=maps)
        imported[key].data.materials.clear()
        imported[key].data.materials.append(mat)

    mat_glasses = make_mat("Mat_glasses", solid_rgba=(0.135, 0.095, 0.062, 1.0), metallic=0.86, roughness=0.23)
    mat_watch = make_mat("Mat_watch", solid_rgba=(0.020, 0.021, 0.024, 1.0), metallic=0.30, roughness=0.20)
    imported["glasses"].data.materials.append(mat_glasses)
    imported["watch"].data.materials.append(mat_watch)

    bpy.ops.wm.save_as_mainfile(filepath=str(WORK / "professor_z_studio_v6.blend"))
    print("Saved master multi-mesh Blender scene -> professor_z_studio_v6.blend")

    # Warm, natural Pixar turnaround studio lighting + soft floor contact shadow:
    world = bpy.data.worlds.new("StudioCreamWorld")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (1.0, 0.985, 0.968, 1.0)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.72
    scene.render.film_transparent = True

    def add_light(loc, energy, size):
        ld = bpy.data.lights.new("Area", type="AREA")
        ld.energy = energy
        ld.size = size
        ld.color = (1.0, 0.985, 0.965)
        lo = bpy.data.objects.new("AreaObj", ld)
        scene.collection.objects.link(lo)
        lo.location = loc
        lo.rotation_euler = (Vector((0.0, 0.0, 1.05)) - Vector(loc)).to_track_quat("-Z", "Y").to_euler()

    add_light((-1.9, -3.2, 2.35), 110.0, 2.8)
    add_light((2.2, -2.5, 1.60), 55.0, 3.0)
    add_light((3.2, 0.8, 2.15), 100.0, 2.8)
    add_light((-3.2, 0.8, 2.15), 100.0, 2.8)
    add_light((0.2, 3.3, 2.25), 110.0, 2.8)

    bpy.ops.mesh.primitive_plane_add(size=4.0, location=(0.0, 0.0, 0.0))
    bpy.context.selected_objects[0].is_shadow_catcher = True

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

    def render_ortho(tag: str, loc_dir: tuple, w: int, u0_px: float, v_sole_px: float, s_mult: float = 1.0) -> np.ndarray:
        sc = scale * s_mult
        dx_cam = ((w - 1) * 0.5 - u0_px) * sc
        dz_cam = (v_sole_px - 511.5) * sc
        cam_data.type = "ORTHO"
        cam_data.ortho_scale = 1024.0 * sc
        cam.location = (loc_dir[0] + dx_cam * loc_dir[2], loc_dir[1] + dx_cam * loc_dir[3], dz_cam)
        tgt = Vector((dx_cam * loc_dir[2], dx_cam * loc_dir[3], dz_cam))
        cam.rotation_euler = (tgt - cam.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.resolution_x, scene.render.resolution_y = w, 1024
        p = WORK / f"v6_panel_{tag}.png"
        scene.render.filepath = str(p)
        bpy.ops.render.render(write_still=True)
        return comp_cream(p)

    # 1. Exact 1536x1024 Orthographic Turnaround Sheet + Direct Reference Comparison:
    sheet_3d = np.tile(cream_bg.astype(np.uint8)[None, None, :], (1024, 1536, 1))
    sheet_3d[:, 150:535] = render_ortho("front", (0.0, -3.5, 1.0, 0.0), 385, float(cal["u0_f"]), float(cal["v_sole_f"]), 1.0)
    sheet_3d[:, 615:965] = render_ortho("side",  (3.5,  0.0, 0.0, 1.0), 350, float(cal["u0_s"]), 973.0, 1.0)
    sheet_3d[:, 1075:1395] = render_ortho("back", (0.0,  3.5, -1.0, 0.0), 320, 133.5, 957.0, 795.0 / 775.0)

    ref_img = np.asarray(Image.open(REF_PATH).convert("RGB"))
    Image.fromarray(sheet_3d).save(RENDER_DIR / "professor_z_3d_turnaround_exact.png")
    Image.fromarray(np.vstack([ref_img, np.full((12, 1536, 3), [45, 48, 56], dtype=np.uint8), sheet_3d])).save(
        RENDER_DIR / "professor_z_3d_vs_ref_comparison.png"
    )

    # 2. Full-Body Uncropped `360°` Turntable (`dist=3.65m` so ZERO top curl crop!) + Macro Close-Ups:
    def render_persp(tag: str, angle_deg: float, dist: float, z_look: float, lens: float, w: int, h: int) -> np.ndarray:
        rad = math.radians(angle_deg)
        cam_data.type = "PERSP"
        cam_data.lens = lens
        cam.location = (dist * math.sin(rad), -dist * math.cos(rad), z_look + 0.03)
        cam.rotation_euler = (Vector((0.0, 0.0, z_look)) - cam.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.resolution_x, scene.render.resolution_y = w, h
        p = WORK / f"v6_card_{tag}.png"
        scene.render.filepath = str(p)
        bpy.ops.render.render(write_still=True)
        return comp_cream(p)

    row_360 = np.hstack(
        [
            render_persp("000_front", 0.0, 3.65, 0.875, 68.0, 380, 900),
            render_persp("038_3q_l", 38.0, 3.65, 0.875, 68.0, 380, 900),
            render_persp("090_side_l", 90.0, 3.65, 0.875, 68.0, 380, 900),
            render_persp("142_3q_b", 142.0, 3.65, 0.875, 68.0, 380, 900),
            render_persp("180_back", 180.0, 3.65, 0.875, 68.0, 380, 900),
            render_persp("318_3q_r", -42.0, 3.65, 0.875, 68.0, 380, 900),
        ]
    )
    hero_port_f = render_persp("hero_port_0", 6.0, 0.98, 1.515, 85.0, 760, 760)
    hero_port_3q = render_persp("hero_port_3q", 35.0, 1.02, 1.515, 85.0, 760, 760)
    hero_shoes = render_persp("hero_shoes", 26.0, 0.96, 0.165, 75.0, 760, 760)
    Image.fromarray(hero_port_f).save(RENDER_DIR / "professor_z_hero_portrait.png")

    row_closeups = np.hstack([hero_port_f, hero_port_3q, hero_shoes])
    Image.fromarray(np.vstack([row_360, np.full((10, 2280, 3), [45, 48, 56], dtype=np.uint8), row_closeups])).save(
        RENDER_DIR / "professor_z_360_turntable_sheet.png"
    )
    print("STAGE C v6 COMPLETE -> ALL ZERO-BLEED STUDIO RENDERS SAVED!")


if __name__ == "__main__":
    main()
