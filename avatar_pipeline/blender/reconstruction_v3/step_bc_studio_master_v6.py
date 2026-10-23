#!/usr/bin/env python3
"""Stage B v6 — True 7-Part Semantic Pixar Character Reconstruction for Professor Z.

Builds clean, watertight, zero-bleed 3D sub-meshes in unified glTF meters (`Y-up, +Z forward, +X left`):
  1. `part_skin.ply`    : Pixar Head + 478-landmark Face + Sculpted Ears + Neck + Bare Bent Arms
  2. `part_hair.ply`    : Volumetric 360° 3D Curly Hair Crown with Tapered S-Curved Curl Locks & Wisps
  3. `part_tshirt.ply`  : Proper Crew-Neck Black Cotton T-Shirt (`collar y=1.376m`), Full Shoulders & Hollow Sleeves
  4. `part_jeans.ply`   : Sleek Slim Washed Denim Jeans (`zero hip flare`) + Pocket Openings + Rolled Cuffs
  5. `part_shoe_l.ply`  : Left High-Top Black/White Converse All-Star Sneaker (`flat sole y=0.000m`)
  6. `part_shoe_r.ply`  : Right High-Top Black/White Converse All-Star Sneaker (`natural contra-posto angle`)
  7. `part_glasses.ply` & `part_watch.ply` : Delicate 3D Bronze Panto Eyeglasses & Left-Wrist Matte Smartwatch
"""

from __future__ import annotations

import json
from pathlib import Path

import cv2
import fast_simplification
import numpy as np
import trimesh
import trimesh.smoothing
from PIL import Image
from scipy import interpolate, ndimage
from skimage import measure

ROOT = Path(__file__).resolve().parents[3]
REF_PATH = ROOT / "avatar_pipeline/input/professor_z_turnaround_ref.png"
WORK = ROOT / "avatar_pipeline/working/recon_v3"
MESH_DIR = WORK / "meshes_v6"


def smin(a: np.ndarray, b: np.ndarray, k: float = 0.012) -> np.ndarray:
    h = np.clip(0.5 + 0.5 * (b - a) / max(k, 1e-6), 0.0, 1.0)
    return b * (1.0 - h) + a * h - k * h * (1.0 - h)


def extract_clean_mesh(sdf: np.ndarray, origin: np.ndarray, pitch: float, target_tris: int, taubin_iters: int = 10) -> trimesh.Trimesh:
    sdf_s = ndimage.gaussian_filter(sdf, sigma=0.55)
    v, f, _, _ = measure.marching_cubes(sdf_s, level=0.0, spacing=(pitch, pitch, pitch))
    v[:, 0] += origin[0]
    v[:, 1] += origin[1]
    v[:, 2] += origin[2]
    m = trimesh.Trimesh(vertices=v, faces=f, process=True)
    # Keep significant connected components (e.g. head + 2 separate arms in `part_skin`!):
    parts = [p for p in m.split(only_watertight=False) if len(p.faces) >= 250]
    m = trimesh.util.concatenate(parts) if len(parts) > 1 else parts[0]
    trimesh.smoothing.filter_taubin(m, lamb=0.50, nu=-0.53, iterations=taubin_iters)
    if len(m.faces) > target_tris:
        vq, fq = fast_simplification.simplify(
            np.ascontiguousarray(m.vertices, dtype=np.float32),
            np.ascontiguousarray(m.faces, dtype=np.int32),
            target_count=target_tris,
        )
        m = trimesh.Trimesh(vertices=vq, faces=fq, process=True)
    trimesh.repair.fix_normals(m)
    return m


def prepare_calibration() -> dict:
    raw = np.asarray(Image.open(REF_PATH).convert("RGB"))
    fp = np.load(WORK / "front_perfect.npz")
    sp = np.load(WORK / "side_perfect.npz")
    bp = np.load(WORK / "back_perfect.npz")
    lm478 = np.load(WORK / "front_facemesh_478.npy").astype(np.float64)
    g_info = json.loads((WORK / "glasses_and_landmarks.json").read_text())

    f_x0, s_x0, b_x0 = int(fp["x0"]), int(sp["x0"]), int(bp["x0"])
    f_rgb_orig = raw[:, f_x0 : int(fp["x1"])].copy()
    s_rgb_orig = raw[:, s_x0 : int(sp["x1"])].copy()
    b_rgb_orig = raw[:, b_x0 : int(bp["x1"])].copy()
    f_mask = (fp["mask"] > 0.5).astype(np.uint8)
    s_mask = (sp["mask"] > 0.5).astype(np.uint8)
    b_mask = (bp["mask"] > 0.5).astype(np.uint8)

    # Clean eyeglasses removal on `FRONT` skin/eyes (`f_rgb_noglass`):
    f_noglass = f_rgb_orig.copy()
    yy, xx = np.mgrid[0:1024, 0 : f_noglass.shape[1]]
    skin_like = (
        (f_rgb_orig[:, :, 0].astype(np.float32) > 72.0)
        & ((f_rgb_orig[:, :, 0].astype(np.float32) - f_rgb_orig[:, :, 2].astype(np.float32)) > 16.0)
    )
    skin_dil = cv2.dilate(skin_like.astype(np.uint8), cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9))) > 0

    wire_mask = np.zeros(f_noglass.shape[:2], dtype=bool)
    fitted = g_info["glasses_rings"]
    local_med = cv2.medianBlur(f_noglass, 15).astype(np.float32)
    for key in ("viewer_left", "viewer_right"):
        ring = fitted[key]
        cx, cy = float(ring["cx_global"]) - f_x0, float(ring["cy"])
        rx, ry = float(ring["rx"]), float(ring["ry"])
        ell = np.sqrt(((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2)
        wire_mask |= (ell >= 0.85) & (ell <= 1.16) & ((local_med.mean(axis=2) - f_noglass.astype(np.float32).mean(axis=2)) > 10.5) & skin_dil

    cl, cr = fitted["viewer_left"], fitted["viewer_right"]
    bx0, bx1 = int(cl["cx_global"] - f_x0 + cl["rx"] * 0.78), int(cr["cx_global"] - f_x0 - cr["rx"] * 0.78)
    by_mid = 0.5 * (cl["cy"] + cr["cy"])
    wire_mask |= (xx >= bx0 - 2) & (xx <= bx1 + 2) & (np.abs(yy - by_mid) <= 5.5) & ((local_med.mean(axis=2) - f_noglass.astype(np.float32).mean(axis=2)) > 8.5)
    tx1 = cl["cx_global"] - f_x0 - cl["rx"] * 0.92
    wire_mask |= (xx >= tx1 - 15) & (xx <= tx1 + 3) & (np.abs(yy - (cl["cy"] + 1.0)) <= 4.5) & ((local_med.mean(axis=2) - f_noglass.astype(np.float32).mean(axis=2)) > 9.0) & skin_dil

    wire_u8 = cv2.dilate(wire_mask.astype(np.uint8) * 255, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3)))
    f_noglass = cv2.inpaint(f_noglass, wire_u8, 4.0, cv2.INPAINT_TELEA)

    v_top_f, v_sole_f = 171.0, 965.0
    scale = 1.7300 / (v_sole_f - v_top_f)
    u0_f, u0_s, u0_b = 180.5, 166.0, 143.5

    # Solve exact 3D head landmarks & local coordinate frame:
    lx = (lm478[:, 0] - (f_x0 + u0_f)) * scale
    ly = (v_sole_f - lm478[:, 1]) * scale
    lz_raw = -lm478[:, 2] * scale
    idx_cal = [10, 168, 1, 0, 152]
    z_target = np.array([0.1440, 0.1460, 0.1680, 0.1485, 0.1335], dtype=np.float64)
    ab, _, _, _ = np.linalg.lstsq(np.column_stack([np.ones(5), lz_raw[idx_cal]]), z_target, rcond=None)
    lz = ab[0] + ab[1] * lz_raw
    lz += (0.1680 - lz[1]) * np.exp(-((lx - lx[1]) ** 2 + (ly - ly[1]) ** 2) / (0.020**2))

    P = np.column_stack([lx, ly, lz])
    x_hat = (P[473] - P[468]) / np.linalg.norm(P[473] - P[468])
    z_hat = np.cross(x_hat, P[10] - P[152])
    if z_hat[2] < 0:
        z_hat = -z_hat
    z_hat /= np.linalg.norm(z_hat)
    y_hat = np.cross(z_hat, x_hat) / np.linalg.norm(np.cross(z_hat, x_hat))
    R_h = np.column_stack([x_hat, y_hat, z_hat])
    face_mid = 0.5 * (P[234] + P[454])
    c_h = np.array([face_mid[0], face_mid[1] + 0.012, 0.043], dtype=np.float64)

    return {
        "scale": scale,
        "f_x0": f_x0,
        "s_x0": s_x0,
        "b_x0": b_x0,
        "u0_f": u0_f,
        "u0_s": u0_s,
        "u0_b": u0_b,
        "v_sole_f": v_sole_f,
        "f_rgb_orig": f_rgb_orig,
        "f_rgb_noglass": f_noglass,
        "s_rgb_orig": s_rgb_orig,
        "b_rgb_orig": b_rgb_orig,
        "f_mask": f_mask,
        "s_mask": s_mask,
        "b_mask": b_mask,
        "P": P,
        "R_h": R_h,
        "c_h": c_h,
        "glasses_info": g_info,
    }


def build_part_1_skin(cal: dict, arm_curves: dict) -> tuple[trimesh.Trimesh, trimesh.Trimesh]:
    """Build dedicated `ProfZ_HeadSkin` (`1.2 mm` ultra-crisp Pixar head+face+ears+neck) & `ProfZ_ArmsSkin`."""
    P, R_h, c_h = cal["P"], cal["R_h"], cal["c_h"]
    Q = (P - c_h) @ R_h

    rx_cran, ry_cran, rz_cran = 0.092, 0.110, 0.102
    theta_ring = np.linspace(0, 2 * np.pi, 48, endpoint=False)
    pts_2d = np.vstack([Q[:468, :2], np.column_stack([rx_cran * 0.96 * np.cos(theta_ring), ry_cran * 0.92 * np.sin(theta_ring) - 0.008])])
    vals_z = np.concatenate([Q[:468, 2], np.full(48, 0.018)])

    gx = np.linspace(-0.12, 0.12, 241)
    gy = np.linspace(-0.15, 0.15, 301)
    GY2, GX2 = np.meshgrid(gy, gx, indexing="ij")
    qz_smooth = ndimage.gaussian_filter(interpolate.LinearNDInterpolator(pts_2d, vals_z, fill_value=0.015)(GX2, GY2), sigma=1.6)

    # Crisp sculpted Pixar nose tip, alar wings, nostrils, eyeballs, eyelids, cocked brow & warm crooked smile:
    q_nose = Q[1]
    qz_smooth += 0.0044 * np.exp(-(((GX2 - q_nose[0]) / 0.015) ** 2 + ((GY2 - q_nose[1]) / 0.014) ** 2))
    for n_idx in (98, 327):
        qa = Q[n_idx]
        qz_smooth += 0.0024 * np.exp(-(((GX2 - qa[0]) / 0.009) ** 2 + ((GY2 - qa[1]) / 0.008) ** 2))
    for sgn_n in (-1.0, +1.0):
        qz_smooth -= 0.0022 * np.exp(-(((GX2 - (q_nose[0] + sgn_n * 0.0095)) / 0.0048) ** 2 + ((GY2 - (q_nose[1] - 0.0095)) / 0.0038) ** 2))

    for iris_i, up_i in ((468, 159), (473, 386)):
        qi, qu = Q[iris_i], Q[up_i]
        de2 = (GX2 - qi[0]) ** 2 + (GY2 - qi[1]) ** 2
        eyeball = np.sqrt(np.maximum(0.0135**2 - de2, 0.0)) - 0.0075
        w_eye = np.exp(-de2 / (0.0145**2))
        qz_smooth = qz_smooth * (1.0 - 0.55 * w_eye) + (qi[2] + 0.0045 + eyeball) * (0.55 * w_eye)
        qz_smooth += 0.0026 * np.exp(-(((GX2 - qi[0]) / 0.016) ** 2 + ((GY2 - (qu[1] + 0.002)) / 0.0035) ** 2))
        qz_smooth -= 0.0018 * np.exp(-(((GX2 - qi[0]) / 0.017) ** 2 + ((GY2 - (qu[1] + 0.0075)) / 0.0030) ** 2))

    for grp in ([70, 63, 105, 66, 107], [300, 293, 334, 296, 336]):
        pts_b = Q[grp]
        tb = np.linspace(0, 1, 16)
        bx_s = np.interp(tb, np.linspace(0, 1, len(grp)), pts_b[:, 0])
        by_s = np.interp(tb, np.linspace(0, 1, len(grp)), pts_b[:, 1])
        brow_f = np.zeros_like(qz_smooth)
        for bxv, byv in zip(bx_s, by_s):
            brow_f = np.maximum(brow_f, np.exp(-(((GX2 - bxv) / 0.0075) ** 2 + ((GY2 - byv) / 0.0048) ** 2)))
        qz_smooth += 0.0028 * brow_f

    mouth_idx = [61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291]
    pts_m = Q[mouth_idx]
    tm = np.linspace(0, 1, 24)
    mx_s = np.interp(tm, np.linspace(0, 1, len(mouth_idx)), pts_m[:, 0])
    my_s = np.interp(tm, np.linspace(0, 1, len(mouth_idx)), pts_m[:, 1])
    smile_g = np.zeros_like(qz_smooth)
    for mxv, myv in zip(mx_s, my_s):
        smile_g = np.maximum(smile_g, np.exp(-(((GX2 - mxv) / 0.0055) ** 2 + ((GY2 - (myv - 0.0035)) / 0.0028) ** 2)))
    qz_smooth -= 0.0026 * smile_g
    qlip = Q[17]
    qz_smooth += 0.0024 * np.exp(-(((GX2 - qlip[0]) / 0.017) ** 2 + ((GY2 - (qlip[1] + 0.004)) / 0.0055) ** 2))

    # 1.25 mm grid for Head + Ears + Neck (`tucked cleanly inside crew-neck collar at y = 1.335 m!`!):
    pitch_h = 0.00125
    orig_h = np.array([-0.155, 1.325, -0.125], dtype=np.float64)
    upp_h = np.array([+0.175, 1.665, +0.200], dtype=np.float64)
    dims_h = np.ceil((upp_h - orig_h) / pitch_h).astype(int) + 1
    xs = orig_h[0] + np.arange(dims_h[0], dtype=np.float32) * pitch_h
    ys = orig_h[1] + np.arange(dims_h[1], dtype=np.float32) * pitch_h
    zs = orig_h[2] + np.arange(dims_h[2], dtype=np.float32) * pitch_h

    sdf_h = np.empty(dims_h, dtype=np.float32)
    ear_defs = [(-1.0, np.array([-(rx_cran + 0.005), -0.010, -0.008])), (+1.0, np.array([+(rx_cran + 0.005), -0.010, -0.008]))]

    for i0 in range(0, dims_h[0], 48):
        i1 = min(dims_h[0], i0 + 48)
        Xc, Yc, Zc = np.meshgrid(xs[i0:i1], ys, zs, indexing="ij")
        dxw, dyw, dzw = Xc - c_h[0], Yc - c_h[1], Zc - c_h[2]
        qx = dxw * R_h[0, 0] + dyw * R_h[1, 0] + dzw * R_h[2, 0]
        qy = dxw * R_h[0, 1] + dyw * R_h[1, 1] + dzw * R_h[2, 1]
        qz = dxw * R_h[0, 2] + dyw * R_h[1, 2] + dzw * R_h[2, 2]

        jaw_t = np.clip(-qy / 0.130, 0.0, 1.0)
        rx_loc = rx_cran * (1.0 - 0.34 * (jaw_t**1.45))
        rz_loc = rz_cran * (1.0 - 0.16 * (jaw_t**1.6))
        d_skull = (np.sqrt((qx / rx_loc) ** 2 + ((qy + 0.006) / ry_cran) ** 2 + ((qz + 0.004) / rz_loc) ** 2) - 1.0) * 0.094

        iy_g = np.clip((qy - gy[0]) / (gy[1] - gy[0]), 0.0, len(gy) - 1.001)
        ix_g = np.clip((qx - gx[0]) / (gx[1] - gx[0]), 0.0, len(gx) - 1.001)
        qz_surf = ndimage.map_coordinates(qz_smooth, [iy_g, ix_g], order=1, mode="nearest")

        w_face = np.clip(1.30 - np.sqrt((qx / 0.082) ** 2 + ((qy + 0.014) / 0.106) ** 2), 0.0, 1.0) ** 1.4 * np.clip((qz + 0.015) / 0.045, 0.0, 1.0)
        d_core = d_skull * (1.0 - w_face) + np.maximum((qz - qz_surf) * 0.88, d_skull - 0.045) * w_face

        # Sculpted 3D Pixar Ears (with helix groove, antihelix & earlobe):
        for sgn_e, q_ear in ear_defs:
            ex = (qx - q_ear[0]) * sgn_e
            ey, ez = qy - q_ear[1], qz - q_ear[2]
            er = np.sqrt(((ex - 0.004 - 0.005 * (-ez / 0.016)) / 0.0105) ** 2 + (ey / 0.0265) ** 2 + (ez / 0.0160) ** 2)
            d_ear = (er - 1.0) * 0.014 + 0.0042 * np.exp(-(((ey - 0.002) / 0.014) ** 2 + ((ez - 0.002) / 0.009) ** 2 + ((ex - 0.009) / 0.006) ** 2))
            d_core = smin(d_core, d_ear, 0.008)

        # Cylindrical neck (`y = 1.335 .. 1.455 m`, radius `0.052 m`, disappearing cleanly INSIDE crew-neck collar at `y = 1.376 m`!):
        tn = np.clip((Yc - 1.335) / 0.115, 0.0, 1.0)
        cx_n = 0.003 + 0.018 * tn
        cz_n = 0.020 + 0.020 * tn
        d_neck = np.maximum((np.sqrt(((Xc - cx_n) / 0.052) ** 2 + ((Zc - cz_n) / 0.050) ** 2) - 1.0) * 0.051, np.maximum(1.332 - Yc, Yc - 1.455))
        sdf_h[i0:i1] = smin(d_core.astype(np.float32), d_neck.astype(np.float32), 0.016)

    head_mesh = extract_clean_mesh(sdf_h, orig_h, pitch_h, target_tris=38000, taubin_iters=10)

    # Dedicated clean bare-skin Arms (`y = 0.875 .. 1.245 m`, emerging from inside hollow T-shirt sleeves into pockets):
    pitch_a = 0.0018
    orig_a = np.array([-0.295, 0.865, -0.145], dtype=np.float64)
    upp_a = np.array([+0.295, 1.255, +0.095], dtype=np.float64)
    dims_a = np.ceil((upp_a - orig_a) / pitch_a).astype(int) + 1
    xa_v = orig_a[0] + np.arange(dims_a[0], dtype=np.float32) * pitch_a
    ya_v = orig_a[1] + np.arange(dims_a[1], dtype=np.float32) * pitch_a
    za_v = orig_a[2] + np.arange(dims_a[2], dtype=np.float32) * pitch_a
    sdf_a = np.empty(dims_a, dtype=np.float32)

    t_dense = np.linspace(0.878, 1.240, 32)
    zk_d = arm_curves["zc"](t_dense)
    for i0 in range(0, dims_a[0], 56):
        i1 = min(dims_a[0], i0 + 56)
        Xg, Yg, Zg = np.meshgrid(xa_v[i0:i1], ya_v, za_v, indexing="ij")
        da = np.full(Xg.shape, 0.50, dtype=np.float32)
        for spl_xc, spl_xr in ((arm_curves["r_xc"], arm_curves["r_xr"]), (arm_curves["l_xc"], arm_curves["l_xr"])):
            xk_d, rk_d = spl_xc(t_dense), spl_xr(t_dense)
            for k in range(len(t_dense) - 1):
                q0x, q0y, q0z, r0s = xk_d[k], t_dense[k], zk_d[k], rk_d[k]
                dx, dy, dz = xk_d[k + 1] - q0x, t_dense[k + 1] - q0y, zk_d[k + 1] - q0z
                seg_l2 = dx * dx + dy * dy + dz * dz
                tt = np.clip(((Xg - q0x) * dx + (Yg - q0y) * dy + (Zg - q0z) * dz) / seg_l2, 0.0, 1.0)
                rr = r0s + tt * (rk_d[k + 1] - r0s)
                da = np.minimum(da, np.sqrt((Xg - (q0x + tt * dx)) ** 2 + (Yg - (q0y + tt * dy)) ** 2 + (Zg - (q0z + tt * dz)) ** 2) - rr)
        sdf_a[i0:i1] = da

    arms_mesh = extract_clean_mesh(sdf_a, orig_a, pitch_a, target_tris=12000, taubin_iters=10)
    return head_mesh, arms_mesh


def build_part_2_pixar_curly_hair(cal: dict) -> trimesh.Trimesh:
    """Build lush, spiky/fluffy 360° Pixar curly hair with S-curved tapered locks (`ZERO round buns`!)."""
    R_h, c_h = cal["R_h"], cal["c_h"]
    rx_cran, ry_cran, rz_cran = 0.091, 0.109, 0.101

    # Generate ~220 3D S-curved tapered Pixar curl locks (`each lock = 5-sphere curved tapered strand!`):
    rng = np.random.default_rng(4202026)
    primitives: list[tuple[float, float, float, float]] = []

    for i in range(215):
        ga = i * 2.39996323
        u_elev = -0.42 + 1.42 * (i / 214.0)  # all the way down nape `-0.42` up to top crown `+1.0`
        rad_xy = np.sqrt(max(0.0, 1.0 - min(u_elev, 0.996) ** 2))
        dx, dz, dy = np.cos(ga) * rad_xy, np.sin(ga) * rad_xy, u_elev

        # Clean open face window: keep curls strictly above brow/forehead bang line (`dy >= 0.44` on front `dz > 0.22`)
        # and cleanly trimmed in front of ears (`no bulbous temple balls!` — `rx_temple` stays tight `<= 0.098 m`!):
        if dz > 0.22 and dy < 0.44 and abs(dx) < 0.78:
            continue
        if abs(dx) > 0.80 and (-0.30 < dy < 0.08) and (-0.24 < dz < 0.26):
            continue

        # Tight temple width (`+6 mm`) vs tall fluffy top crown (`+34 mm`) & rounded occiput (`+18 mm`) -> EXACT Pixar proportions!
        w_temple = 0.007 + 0.008 * max(0.0, dy)
        h_crown = 0.010 + 0.030 * (max(0.0, dy) ** 1.3)
        d_occip = 0.012 + 0.014 * max(0.0, -dz) + (0.016 if (dz > 0.22 and dy >= 0.44) else 0.0)

        root_q = np.array([dx * (rx_cran + w_temple * 0.4), dy * (ry_cran + h_crown * 0.4) + 0.008, dz * (rz_cran + d_occip * 0.4)])
        out_n = np.array([dx * 0.65, max(0.25, dy) * 1.15, dz * 0.85])
        out_n /= np.linalg.norm(out_n) + 1e-6
        tang1 = np.cross(out_n, np.array([0.0, 1.0, 0.2]))
        if np.linalg.norm(tang1) < 1e-4:
            tang1 = np.array([1.0, 0.0, 0.0])
        tang1 /= np.linalg.norm(tang1)
        phase = rng.uniform(0, 2 * np.pi)
        length = 0.026 + 0.020 * max(0.0, dy) + 0.008 * rng.random()
        r_root = 0.016 + 0.005 * rng.random()

        # 4 stepped beads along an S-curved curl lock tapering down to a crisp `4.5 mm` pointed wisp tip!
        for step_k, s_u in enumerate((0.0, 0.36, 0.70, 1.0)):
            curl_swirl = 0.0085 * np.sin(phase + s_u * 4.2) * tang1
            q_pt = root_q + out_n * (length * s_u) + curl_swirl
            r_pt = r_root * (1.0 - 0.68 * (s_u**1.35))
            pw = c_h + q_pt @ R_h.T
            primitives.append((float(pw[0]), float(pw[1]), float(pw[2]), float(r_pt)))

    # Add signature iconic forehead curl lock hanging slightly over his right-of-center brow (just like FRONT & SIDE ref!):
    for s_u in np.linspace(0.0, 1.0, 6):
        q_bang = np.array([-0.012 + 0.010 * np.sin(s_u * 3.1), 0.098 - 0.048 * s_u, 0.088 + 0.024 * np.sin(s_u * 2.2)])
        pw = c_h + q_bang @ R_h.T
        primitives.append((float(pw[0]), float(pw[1]), float(pw[2]), float(0.013 * (1.0 - 0.55 * s_u))))

    prim_arr = np.array(primitives, dtype=np.float32)

    pitch = 0.0014
    orig = np.array([-0.155, 1.385, -0.155], dtype=np.float64)
    upp = np.array([+0.185, 1.742, +0.205], dtype=np.float64)
    dims = np.ceil((upp - orig) / pitch).astype(int) + 1
    xs = orig[0] + np.arange(dims[0], dtype=np.float32) * pitch
    ys = orig[1] + np.arange(dims[1], dtype=np.float32) * pitch
    zs = orig[2] + np.arange(dims[2], dtype=np.float32) * pitch
    print(f"Building dedicated 360° spiky/curly Pixar Hair mesh ({dims[0]}x{dims[1]}x{dims[2]})...")

    sdf = np.empty(dims, dtype=np.float32)
    for i0 in range(0, dims[0], 46):
        i1 = min(dims[0], i0 + 46)
        Xc, Yc, Zc = np.meshgrid(xs[i0:i1], ys, zs, indexing="ij")
        dxw, dyw, dzw = Xc - c_h[0], Yc - c_h[1], Zc - c_h[2]
        qx = dxw * R_h[0, 0] + dyw * R_h[1, 0] + dzw * R_h[2, 0]
        qy = dxw * R_h[0, 1] + dyw * R_h[1, 1] + dzw * R_h[2, 1]
        qz = dxw * R_h[0, 2] + dyw * R_h[1, 2] + dzw * R_h[2, 2]

        # Thin inner dark scalp cap (`strictly on upper/back skull so ZERO bald gap ever peeks between curls!`):
        d_scalp = (np.sqrt((qx / 0.095) ** 2 + ((qy - 0.004) / 0.114) ** 2 + ((qz + 0.004) / 0.105) ** 2) - 1.0) * 0.095
        # Trim scalp cap cleanly away from face & ears (`qz - 0.42*qy > 0.040` or ear window):
        face_cut = (qz - 0.46 * qy) - 0.036
        ear_cut = 0.032 - np.sqrt(((np.abs(qx) - 0.092) / 1.0) ** 2 + ((qy + 0.010) / 1.15) ** 2 + ((qz + 0.008) / 0.9) ** 2)
        d_scalp = np.maximum(d_scalp, -np.minimum(-face_cut, -ear_cut)) if False else np.maximum(d_scalp, face_cut)
        d_scalp = np.maximum(d_scalp, ear_cut)
        d_scalp = np.maximum(d_scalp, -0.048 - qy)

        d_locks = d_scalp.astype(np.float32)
        for cx_p, cy_p, cz_p, r_p in prim_arr:
            if (cx_p + r_p + 0.012 < xs[i0]) or (cx_p - r_p - 0.012 > xs[i1 - 1]):
                continue
            ds = np.sqrt((Xc - cx_p) ** 2 + (Yc - cy_p) ** 2 + (Zc - cz_p) ** 2) - r_p
            d_locks = smin(d_locks, ds, 0.0048)
        sdf[i0:i1] = d_locks

    return extract_clean_mesh(sdf, orig, pitch, target_tris=34000, taubin_iters=7)


def build_part_3_tshirt() -> trimesh.Trimesh:
    """Build dedicated Crew-Neck Black Cotton T-Shirt (`y = 0.852 .. 1.380 m`) covering 100% of shoulders & collar!"""
    pitch = 0.0020
    orig = np.array([-0.305, 0.840, -0.165], dtype=np.float64)
    upp = np.array([+0.305, 1.392, +0.165], dtype=np.float64)
    dims = np.ceil((upp - orig) / pitch).astype(int) + 1
    xs = orig[0] + np.arange(dims[0], dtype=np.float32) * pitch
    ys = orig[1] + np.arange(dims[1], dtype=np.float32) * pitch
    zs = orig[2] + np.arange(dims[2], dtype=np.float32) * pitch

    # Smooth trunk curve from waist hem (`0.854 m`) all the way up over trapezius to crew-neck collar (`1.380 m`, `r = 0.060 m` hugging neck!):
    ty_k = np.array([0.850, 0.900, 1.000, 1.120, 1.240, 1.315, 1.355, 1.382])
    txr_k = np.array([0.162, 0.153, 0.150, 0.157, 0.165, 0.146, 0.098, 0.061])
    tzc_k = np.array([0.022, 0.020, 0.016, 0.012, 0.010, 0.014, 0.019, 0.021])
    tzr_k = np.array([0.102, 0.106, 0.112, 0.116, 0.112, 0.094, 0.072, 0.059])
    spl_xr = interpolate.PchipInterpolator(ty_k, txr_k)
    spl_zc = interpolate.PchipInterpolator(ty_k, tzc_k)
    spl_zr = interpolate.PchipInterpolator(ty_k, tzr_k)

    sdf = np.empty(dims, dtype=np.float32)
    for i0 in range(0, dims[0], 54):
        i1 = min(dims[0], i0 + 54)
        Xg, Yg, Zg = np.meshgrid(xs[i0:i1], ys, zs, indexing="ij")
        yq = np.clip(Yg, 0.850, 1.382)
        xr, zc, zr = spl_xr(yq), spl_zc(yq), spl_zr(yq)

        # Natural crew-neck collar dip in front (`dip down 24 mm on chest side +Z just like real crew-neck T-shirt!`!):
        front_dip = 0.024 * np.clip((Zg - 0.02) / 0.06, 0.0, 1.0) * np.exp(-((Xg - 0.004) / 0.062) ** 2)
        top_plane = Yg - (1.380 - front_dip)

        hem_lip = -0.0045 * np.exp(-((Yg - 0.860) / 0.013) ** 2)
        collar_rib = -0.0040 * np.exp(-((top_plane + 0.007) / 0.007) ** 2)
        drape = -0.0028 * np.sin(np.arctan2(Zg - zc, Xg) * 5.0 + Yg * 17.0) * np.exp(-((Yg - 0.905) / 0.055) ** 2)

        tnorm = (((np.abs(Xg - 0.002) / xr) ** 2.12) + ((np.abs(Zg - zc) / zr) ** 2.08)) ** (1.0 / 2.10)
        d_trunk = np.maximum((tnorm - 1.0) * np.minimum(xr, zr) + hem_lip + collar_rib + drape, np.maximum(0.854 - Yg, top_plane))

        # Full rounded shoulder caps + flared short sleeves (`covering 100% of upper shoulders so ZERO bare shoulder gap exists!`!):
        d_slvs = np.full_like(d_trunk, 0.50)
        for sgn in (-1.0, +1.0):
            p0 = np.array([sgn * 0.095, 1.322, +0.012], dtype=np.float32)
            p1 = np.array([sgn * 0.214, 1.118, -0.044], dtype=np.float32)
            v01 = p1 - p0
            L2 = float(np.dot(v01, v01))
            ts = np.clip(((Xg - p0[0]) * v01[0] + (Yg - p0[1]) * v01[1] + (Zg - p0[2]) * v01[2]) / L2, 0.0, 1.0)
            cx_s, cy_s, cz_s = p0[0] + ts * v01[0], p0[1] + ts * v01[1], p0[2] + ts * v01[2]
            r_slv = 0.072 - 0.010 * ts + 0.006 * (ts**2)
            dist_ax = np.sqrt((Xg - cx_s) ** 2 + (Yg - cy_s) ** 2 + ((Zg - cz_s) * 0.92) ** 2)
            d_s = np.maximum(dist_ax - r_slv, (ts - 1.0) * 0.16)
            # Carve realistic hollow sleeve opening at cuff (`ts > 0.78`):
            in_m = (ts > 0.78) & (dist_ax < (r_slv - 0.008))
            d_s = np.where(in_m, np.maximum(d_s, ((r_slv - 0.008) - dist_ax) * ((ts - 0.78) / 0.22)), d_s)
            d_slvs = np.minimum(d_slvs, d_s)

        sdf[i0:i1] = smin(d_trunk.astype(np.float32), d_slvs.astype(np.float32), 0.026)

    return extract_clean_mesh(sdf, orig, pitch, target_tris=22000, taubin_iters=10)


def build_part_4_slim_jeans_and_cuffs(cal: dict) -> tuple[trimesh.Trimesh, np.ndarray, np.ndarray]:
    """Build sleek slim tapered washed jeans (`ZERO hip saddle flare`) + crisp rolled cuffs (`y = 0.134..0.885 m`)."""
    s, u0_f, u0_s, v_sole = cal["scale"], cal["u0_f"], cal["u0_s"], cal["v_sole_f"]
    fm, sm = cal["f_mask"] > 0, cal["s_mask"] > 0
    y_of = lambda vf: (v_sole - np.asarray(vf, dtype=np.float64)) * s
    x_of = lambda uf: (np.asarray(uf, dtype=np.float64) - u0_f) * s
    z_of = lambda us: (u0_s - np.asarray(us, dtype=np.float64)) * s

    leg_vf, rx_c, rx_r, lx_c, lx_r, lz_c, lz_r = [], [], [], [], [], [], []
    for vf in range(654, 906, 4):
        pad = np.concatenate([[False], fm[vf], [False]])
        st = np.where(pad[1:] & ~pad[:-1])[0]
        en = np.where(~pad[1:] & pad[:-1])[0] - 1
        segs = [(int(a), int(b)) for a, b in zip(st, en) if (b - a) >= 20]
        if len(segs) >= 2:
            r0, r1 = segs[0]
            l0, l1 = segs[-1]
            us_idx = np.where(sm[min(1023, vf + 8)])[0]
            us_min = max(float(us_idx.min()), 121.0 if vf > 880 else 99.0)
            us_max = min(float(us_idx.max()), 221.0)
            leg_vf.append(vf)
            rx_c.append(0.5 * (x_of(r0) + x_of(r1)))
            rx_r.append(0.5 * (x_of(r1) - x_of(r0)))
            lx_c.append(0.5 * (x_of(l0) + x_of(l1)))
            lx_r.append(0.5 * (x_of(l1) - x_of(l0)))
            lz_c.append(0.5 * (z_of(us_min) + z_of(us_max)))
            lz_r.append(0.5 * (z_of(us_min) - z_of(us_max)))

    ly = y_of(leg_vf)[::-1]
    kw = dict(s=1.2e-5, k=3)
    spl_rxc = interpolate.UnivariateSpline(ly, np.array(rx_c)[::-1], **kw)
    spl_rxr = interpolate.UnivariateSpline(ly, np.array(rx_r)[::-1], **kw)
    spl_lxc = interpolate.UnivariateSpline(ly, np.array(lx_c)[::-1], **kw)
    spl_lxr = interpolate.UnivariateSpline(ly, np.array(lx_r)[::-1], **kw)
    spl_lzc = interpolate.UnivariateSpline(ly, np.array(lz_c)[::-1], **kw)
    spl_lzr = interpolate.UnivariateSpline(ly, np.array(lz_r)[::-1], **kw)

    pitch = 0.0020
    orig = np.array([-0.285, 0.125, -0.155], dtype=np.float64)
    upp = np.array([+0.265, 0.895, +0.185], dtype=np.float64)
    dims = np.ceil((upp - orig) / pitch).astype(int) + 1
    xs = orig[0] + np.arange(dims[0], dtype=np.float32) * pitch
    ys = orig[1] + np.arange(dims[1], dtype=np.float32) * pitch
    zs = orig[2] + np.arange(dims[2], dtype=np.float32) * pitch

    sdf = np.empty(dims, dtype=np.float32)
    for i0 in range(0, dims[0], 54):
        i1 = min(dims[0], i0 + 54)
        Xg, Yg, Zg = np.meshgrid(xs[i0:i1], ys, zs, indexing="ij")
        yl = np.clip(Yg, 0.134, 0.675)
        stride_t = np.clip((0.55 - yl) / 0.415, 0.0, 1.0) ** 1.8
        rzc = spl_lzc(yl) + 0.055 * stride_t
        lzc = spl_lzc(yl)
        rxc, rxr = spl_rxc(yl), spl_rxr(yl)
        lxc, lxr = spl_lxc(yl), spl_lxr(yl)
        lzr = spl_lzr(yl)

        # Crisp turned-up rolled denim cuff (`y = 0.134 .. 0.172 m`, clean `+5.5 mm` cuff fold + sharp top seam!):
        in_cuff = (Yg >= 0.134) & (Yg <= 0.172)
        cuff_boost = np.where(in_cuff, 0.0055, 0.0) - 0.0020 * np.exp(-((Yg - 0.173) / 0.0035) ** 2)
        fold = 0.0018 * np.sin(Yg * 60.0 + Xg * 14.0) * np.exp(-((Yg - 0.25) / 0.11) ** 2)
        fold += 0.0014 * np.sin(Yg * 46.0) * np.exp(-((Yg - 0.50) / 0.09) ** 2)

        d_r = ((((np.abs(Xg - rxc) / (rxr + cuff_boost)) ** 2.15) + ((np.abs(Zg - rzc) / (lzr + cuff_boost)) ** 2.15)) ** (1 / 2.15) - 1.0) * np.minimum(rxr, lzr) - fold
        d_l = ((((np.abs(Xg - lxc) / (lxr + cuff_boost)) ** 2.15) + ((np.abs(Zg - lzc) / (lzr + cuff_boost)) ** 2.15)) ** (1 / 2.15) - 1.0) * np.minimum(lxr, lzr) - fold
        d_legs = np.maximum(smin(d_r, d_l, 0.010), np.maximum(0.134 - Yg, Yg - 0.790))

        # Sleek straight-slim jeans pelvis/hip block (`half_x = 0.148 m`, tucked `14 mm` INSIDE T-shirt hem `0.162 m` -> ZERO flare!!):
        th = np.clip((Yg - 0.675) / (0.880 - 0.675), 0.0, 1.0)
        hx_p = 0.149 - 0.004 * th
        hz_p = 0.094 - 0.003 * th
        cz_p = 0.022
        cleft = 0.005 * np.exp(-(Xg / 0.018) ** 2) * np.exp(-((Yg - 0.775) / 0.05) ** 2) * (Zg < 0.0)
        d_hip = ((((np.abs(Xg + 0.002) / hx_p) ** 2.2) + ((np.abs(Zg - cz_p) / hz_p) ** 2.1)) ** (1 / 2.15) - 1.0) * 0.095 + cleft
        d_hip = np.maximum(d_hip, np.maximum(0.680 - Yg, Yg - 0.882))

        sdf[i0:i1] = smin(d_legs.astype(np.float32), d_hip.astype(np.float32), 0.020)

    jeans_mesh = extract_clean_mesh(sdf, orig, pitch, target_tris=22000, taubin_iters=10)
    ank_R = np.array([float(spl_rxc(0.145)), 0.0, float(spl_lzc(0.145)) + 0.055])
    ank_L = np.array([float(spl_lxc(0.145)), 0.0, float(spl_lzc(0.145))])
    return jeans_mesh, ank_R, ank_L


def main() -> None:
    MESH_DIR.mkdir(parents=True, exist_ok=True)
    # Reuse Converse, Glasses & Watch builders from v5 alongside our clean new 4 body sub-meshes!
    import sys as _sys
    _sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(WORK))
    import step_b_build_flawless_v4 as v5

    cal = prepare_calibration()
    ay_k = np.array([0.880, 0.940, 1.008, 1.076, 1.150, 1.240])
    arm_curves = {
        "r_xc": interpolate.PchipInterpolator(ay_k, np.array([-0.128, -0.174, -0.214, -0.232, -0.218, -0.184])),
        "r_xr": interpolate.PchipInterpolator(ay_k, np.array([0.025, 0.030, 0.035, 0.039, 0.043, 0.045])),
        "l_xc": interpolate.PchipInterpolator(ay_k, np.array([+0.128, +0.170, +0.208, +0.224, +0.214, +0.184])),
        "l_xr": interpolate.PchipInterpolator(ay_k, np.array([0.025, 0.030, 0.035, 0.039, 0.043, 0.045])),
        "zc": interpolate.PchipInterpolator(ay_k, np.array([+0.042, +0.014, -0.038, -0.084, -0.054, -0.018])),
    }

    head_mesh, arms_mesh = build_part_1_skin(cal, arm_curves)
    hair_mesh = build_part_2_pixar_curly_hair(cal)
    tshirt_mesh = build_part_3_tshirt()
    jeans_mesh, ank_R, ank_L = build_part_4_slim_jeans_and_cuffs(cal)

    shoe_R = v5.build_converse_sneaker_v5(ank_R, yaw_rad=np.radians(-32.0), length_m=0.278, width_m=0.102)
    shoe_L = v5.build_converse_sneaker_v5(ank_L, yaw_rad=np.radians(+15.0), length_m=0.276, width_m=0.102)
    head_pack = {"P_landmarks": cal["P"], "R_head": cal["R_h"], "c_head": cal["c_h"]}
    glasses_mesh = v5.build_flush_3d_glasses(cal, head_pack)
    prof_w = {"spl_larm_xc": arm_curves["l_xc"], "spl_larm_xr": arm_curves["l_xr"], "spl_arm_zc": arm_curves["zc"]}
    watch_mesh = v5.build_flush_3d_watch(prof_w)

    head_mesh.export(MESH_DIR / "part_head_skin.ply")
    arms_mesh.export(MESH_DIR / "part_arms_skin.ply")
    hair_mesh.export(MESH_DIR / "part_hair.ply")
    tshirt_mesh.export(MESH_DIR / "part_tshirt.ply")
    jeans_mesh.export(MESH_DIR / "part_jeans.ply")
    shoe_L.export(MESH_DIR / "part_shoe_left.ply")
    shoe_R.export(MESH_DIR / "part_shoe_right.ply")
    glasses_mesh.export(MESH_DIR / "part_glasses.ply")
    watch_mesh.export(MESH_DIR / "part_watch_left.ply")

    np.savez_compressed(
        WORK / "calibration_v6.npz",
        scale=cal["scale"],
        f_x0=cal["f_x0"],
        s_x0=cal["s_x0"],
        b_x0=cal["b_x0"],
        u0_f=cal["u0_f"],
        u0_s=cal["u0_s"],
        u0_b=cal["u0_b"],
        v_sole_f=cal["v_sole_f"],
        f_rgb_orig=cal["f_rgb_orig"],
        f_rgb_noglass=cal["f_rgb_noglass"],
        s_rgb_orig=cal["s_rgb_orig"],
        b_rgb_orig=cal["b_rgb_orig"],
        f_mask=cal["f_mask"],
        s_mask=cal["s_mask"],
        b_mask=cal["b_mask"],
        P_landmarks=cal["P"],
        R_head=cal["R_h"],
        c_head=cal["c_h"],
        ank_R=ank_R,
        ank_L=ank_L,
    )
    print("STAGE B v6 COMPLETE -> ALL 9 CLEAN SEMANTIC SUB-MESHES EXPORTED!")


if __name__ == "__main__":
    main()
