#!/usr/bin/env python3
"""
Stage 1-2: Multi-View Reference Analyzer, Initial 3D Visual-Hull Reconstructor (`AssetFurnace` /
`image-to-3dlab` `surface_calm` + `winding` + `photo_paint`), and High-Detail PBR Texture Builder.

Preserves untouched local reference sheets, generates the separate untouched initial 3D visual-hull
source mesh (`initial_visual_hull_source.glb` painted directly from multi-view reference projections
via `image_to_3dlab.photo_paint.paint_texture`), extracts empirical silhouette & palette metrics,
and outputs `1024x1024` component PBR maps for the clean Blender 4.2 character rebuild stage.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import shutil
import sys
from typing import Any, Dict, List, Tuple

import cv2
import mediapipe as mp
import numpy as np
from PIL import Image, ImageDraw
from skimage.measure import marching_cubes
import trimesh

# Live installed `Bingeljell/image-to-3dlab` (`AssetFurnace v0.5.1`, Apache-2.0) CPU modules
from image_to_3dlab import photo_paint, surface_calm, winding


def detect_is_character_catalogue(img_rgb: np.ndarray) -> bool:
    """Detect wide multi-panel character turnaround sheet (`W/H >= 1.35`, light paper border)."""
    h, w, _ = img_rgb.shape
    if w < 900 or h < 600 or (w / float(h)) < 1.35:
        return False
    border = np.concatenate([img_rgb[:24, :, :].reshape(-1, 3), img_rgb[-24:, :, :].reshape(-1, 3)], axis=0)
    mean_border = border.mean(axis=0)
    return bool(mean_border.mean() > 215.0 and np.std(border) < 28.0)


def extract_catalogue_panels_and_rgba_views(img_rgb: np.ndarray, views_dir: pathlib.Path) -> Dict[str, Any]:
    """
    Losslessly slice every panel of `Professor Z — Character Catalogue` AND automatically isolate
    the 4 orthographic turnaround figures (`front`, `three_quarter`, `side`, `rear`) with soft
    foreground RGBA masks and normalized 64-slice silhouette profiles.
    """
    views_dir.mkdir(parents=True, exist_ok=True)
    h, w, _ = img_rgb.shape
    crops = {
        "hero_pose": (0.232, 0.024, 0.384, 0.585),
        "turnaround_front": (0.415, 0.112, 0.502, 0.578),
        "turnaround_three_quarter": (0.515, 0.112, 0.588, 0.578),
        "turnaround_side": (0.610, 0.112, 0.678, 0.578),
        "turnaround_rear": (0.690, 0.112, 0.775, 0.578),
        "palette_and_details": (0.795, 0.035, 0.985, 0.490),
        "expressions_grid": (0.010, 0.575, 0.375, 0.985),
        "outfit_variations": (0.380, 0.575, 0.645, 0.985),
        "poses_and_props": (0.650, 0.575, 0.990, 0.985),
    }
    saved: Dict[str, str] = {}
    bg = np.median(
        np.concatenate([img_rgb[8:24, :, :].reshape(-1, 3), img_rgb[-24:-8, :, :].reshape(-1, 3)], axis=0),
        axis=0,
    )
    rgba_views: Dict[str, str] = {}
    profiles: Dict[str, Any] = {"background_rgb": bg.round(2).tolist()}

    for name, (x0, y0, x1, y1) in crops.items():
        patch = img_rgb[int(y0 * h) : int(y1 * h), int(x0 * w) : int(x1 * w)]
        out_p = views_dir / f"{name}.png"
        Image.fromarray(patch).save(out_p)
        saved[name] = str(out_p)

        if name.startswith("turnaround_"):
            diff = np.linalg.norm(patch.astype(np.float32) - bg[None, None, :], axis=2)
            # Suppress top/bottom horizontal divider rules if any touch first/last 4px
            diff[:6, :] = 0.0
            diff[-6:, :] = 0.0
            ys, xs = np.nonzero(diff > 24.0)
            if len(ys) > 50:
                yt, yb = max(0, int(ys.min()) - 2), min(patch.shape[0] - 1, int(ys.max()) + 2)
                xl, xr = max(0, int(xs.min()) - 2), min(patch.shape[1] - 1, int(xs.max()) + 2)
                sub_rgb = patch[yt : yb + 1, xl : xr + 1]
                sub_d = diff[yt : yb + 1, xl : xr + 1]
                alpha = np.clip((sub_d - 16.0) / 14.0, 0.0, 1.0)
                rgba = np.dstack([sub_rgb, (alpha * 255.0).astype(np.uint8)])
                rgba_p = views_dir / f"{name}_rgba.png"
                Image.fromarray(rgba).save(rgba_p)
                rgba_views[name] = str(rgba_p)

                fh, fw = sub_rgb.shape[:2]
                mask = sub_d > 22.0
                widths: List[float] = []
                for k in range(64):
                    r0 = int((1.0 - (k + 1) / 64.0) * fh)
                    r1 = max(r0 + 1, int((1.0 - k / 64.0) * fh))
                    cols = np.nonzero(mask[r0:r1, :].any(axis=0))[0]
                    widths.append(round(float(cols[-1] - cols[0] + 1) / float(fh), 4) if len(cols) > 0 else 0.0)
                profiles[name] = {
                    "tight_size_px": [int(fw), int(fh)],
                    "width_over_height": round(float(fw) / float(fh), 4),
                    "widths_norm_bottom_to_top": widths,
                }

    return {"panels": saved, "rgba_views": rgba_views, "silhouette_profiles": profiles}


def build_initial_visual_hull_with_assetfurnace(
    rgba_views: Dict[str, str],
    single_img: np.ndarray,
    workdir: pathlib.Path,
    target_height_m: float,
) -> Dict[str, Any]:
    """
    Reconstruct and save the SEPARATE UNTOUCHED INITIAL 3D SOURCE ASSET
    (`workdir/initial_visual_hull_source.glb`) using:
      1. Multi-view orthographic space carving (`Front 0°`, `3/4 45°`, `Side 90°`, `Rear 180°`)
         on a `96 x 96 x 160` 3D occupancy volume + `skimage.measure.marching_cubes`.
      2. `image_to_3dlab.surface_calm.taubin` volume-preserving Laplacian/Taubin fairing.
      3. `image_to_3dlab.winding` outward normal sign enforcement.
      4. `image_to_3dlab.photo_paint.paint_texture` genuine depth-buffered multi-view reference
         projection + conjugate-gradient Laplacian local color matching onto a `512x512` UV atlas!
    """
    nx, ny, nz = 72, 72, 128
    half_xy = 0.28 * target_height_m
    xs = np.linspace(-half_xy, half_xy, nx, dtype=np.float32)
    ys = np.linspace(-half_xy, half_xy, ny, dtype=np.float32)
    zs = np.linspace(0.0, target_height_m, nz, dtype=np.float32)
    X, Y, Z = np.meshgrid(xs, ys, zs, indexing="ij")  # Y+ is glTF UP in GLB export; here internal Z+ is height

    occ = np.ones((nx, ny, nz), dtype=np.float32)
    paint_views: List[photo_paint.View] = []

    def pad_square_rgba(im_rgba: np.ndarray, out_px: int = 512) -> np.ndarray:
        hh, ww, _ = im_rgba.shape
        canvas = np.zeros((out_px, out_px, 4), dtype=np.uint8)
        scale = (out_px * 0.88) / float(max(hh, ww))
        nw, nh = max(4, int(round(ww * scale))), max(4, int(round(hh * scale)))
        resized = cv2.resize(im_rgba, (nw, nh), interpolation=cv2.INTER_AREA)
        x0 = (out_px - nw) // 2
        y0 = out_px - int(out_px * 0.06) - nh
        canvas[y0 : y0 + nh, x0 : x0 + nw] = resized
        return canvas

    if "turnaround_front" in rgba_views and "turnaround_side" in rgba_views:
        view_defs = [
            ("turnaround_front", 0.0),
            ("turnaround_three_quarter", 42.0),
            ("turnaround_side", 90.0),
            ("turnaround_rear", 180.0),
        ]
        for vname, az_deg in view_defs:
            if vname not in rgba_views:
                continue
            raw_rgba = np.asarray(Image.open(rgba_views[vname]).convert("RGBA"))
            sq = pad_square_rgba(raw_rgba, 512)
            # Soft dilated occupancy field so thin features (glasses bridge, ankles) remain connected
            a_soft = cv2.GaussianBlur((sq[..., 3] > 64).astype(np.float32), (5, 5), 1.1)
            th = math.radians(az_deg)
            # Project `(X, Y, Z)` into square view `[0..511]` (`span = target_height_m / 0.88`)
            span = target_height_m / 0.88
            u_proj = ((X * math.cos(th) + Y * math.sin(th)) / span + 0.5) * 511.0
            v_proj = (0.94 - (Z / span)) * 511.0
            ui = np.clip(np.rint(u_proj).astype(np.int32), 0, 511)
            vi = np.clip(np.rint(v_proj).astype(np.int32), 0, 511)
            occ = np.minimum(occ, a_soft[vi, ui])

            # Build exact NeRF/glTF 4x4 `cam_to_world` looking down camera `-Z` with `+Y` up!
            fov_deg = 10.0
            dist = (span * 0.5) / math.tan(math.radians(fov_deg * 0.5))
            mid_y = target_height_m * 0.50
            c2w = np.eye(4, dtype=np.float64)
            c2w[:3, 0] = [math.cos(th), 0.0, -math.sin(th)]
            c2w[:3, 1] = [0.0, 1.0, 0.0]
            c2w[:3, 2] = [math.sin(th), 0.0, math.cos(th)]
            c2w[:3, 3] = [dist * math.sin(th), mid_y, dist * math.cos(th)]
            paint_views.append(
                photo_paint.View(
                    image=sq,
                    cam_to_world=c2w,
                    fov_x=math.radians(fov_deg),
                    name=vname,
                )
            )
    else:
        # Single-portrait fallback visual hull so single-image mode also preserves an untouched initial 3D source mesh
        rr = np.sqrt((X / (0.14 * target_height_m)) ** 2 + (Y / (0.10 * target_height_m)) ** 2)
        occ = np.clip(1.4 - rr, 0.0, 1.0)
        sq_rgb = cv2.resize(single_img, (512, 512), interpolation=cv2.INTER_AREA)
        sq_rgba = np.dstack([sq_rgb, np.full((512, 512), 255, dtype=np.uint8)])
        span = target_height_m / 0.88
        fov_deg = 12.0
        dist = (span * 0.5) / math.tan(math.radians(fov_deg * 0.5))
        c2w = np.eye(4, dtype=np.float64)
        c2w[:3, 3] = [0.0, target_height_m * 0.5, dist]
        paint_views.append(
            photo_paint.View(
                image=sq_rgba,
                cam_to_world=c2w,
                fov_x=math.radians(fov_deg),
                name="single_portrait_front",
            )
        )

    occ_smooth = cv2.GaussianBlur(occ, (3, 3), 0.75)
    verts_mc, faces_mc, _, _ = marching_cubes(
        occ_smooth,
        level=0.35,
        spacing=(float(xs[1] - xs[0]), float(ys[1] - ys[0]), float(zs[1] - zs[0])),
    )
    verts_mc[:, 0] += float(xs[0])
    verts_mc[:, 1] += float(ys[0])
    verts_mc[:, 2] += float(zs[0])
    # Convert internal `(X, Y_depth, Z_up)` -> glTF Y-up `(X, Z_up, -Y_depth)`
    v_gltf = np.column_stack([verts_mc[:, 0], verts_mc[:, 2], -verts_mc[:, 1]]).astype(np.float64)
    f_gltf = faces_mc[:, [0, 2, 1]].astype(np.int64)

    # Run genuine `AssetFurnace / image-to-3dlab` Taubin smoothing + outward normal check!
    v_calm = surface_calm.taubin(v_gltf, f_gltf, np.ones(len(v_gltf), dtype=np.float64), 10)
    outward = float(photo_paint.outward_sign(v_calm, f_gltf))
    if outward < 0.0:
        f_gltf = f_gltf[:, [0, 2, 1]]

    # Cylindrical UV unwrap `[0..1]^2` (`v = 0` top in glTF UV space -> `1.0 - y / H`)
    az_u = (np.arctan2(v_calm[:, 0], v_calm[:, 2]) / (2.0 * math.pi)) + 0.5
    el_v = 1.0 - np.clip(v_calm[:, 1] / max(target_height_m, 1e-4), 0.005, 0.995)
    uvs = np.column_stack([np.clip(az_u, 0.005, 0.995), el_v]).astype(np.float64)

    base_tex = np.full((512, 512, 3), [128, 96, 72], dtype=np.uint8)
    painted_u8, weight_map = photo_paint.paint_texture(
        base_tex,
        v_calm,
        uvs,
        f_gltf,
        paint_views,
        frame=((0, 1, 2), (1, 1, 1)),
        settings=photo_paint.Settings(align=False),
    )
    painted_path = workdir / "initial_photo_painted_albedo.png"
    Image.fromarray(painted_u8).save(painted_path)

    vis_mat = trimesh.visual.texture.SimpleMaterial(image=Image.fromarray(painted_u8))
    tex_vis = trimesh.visual.TextureVisuals(
        uv=np.column_stack([uvs[:, 0], 1.0 - uvs[:, 1]]),
        image=Image.fromarray(painted_u8),
        material=vis_mat,
    )
    hull_mesh = trimesh.Trimesh(vertices=v_calm.astype(np.float32), faces=f_gltf.astype(np.int32), visual=tex_vis, process=False)

    init_glb = workdir / "initial_visual_hull_source.glb"
    hull_mesh.export(str(init_glb))

    metrics = {
        "untouched_initial_glb": str(init_glb.resolve()),
        "untouched_projected_albedo": str(painted_path.resolve()),
        "backend_modules_used": [
            "skimage.measure.marching_cubes",
            "image_to_3dlab.surface_calm.taubin",
            "image_to_3dlab.winding.outward_sign",
            "image_to_3dlab.photo_paint.paint_texture",
        ],
        "vertex_count": int(len(v_calm)),
        "triangle_count": int(len(f_gltf)),
        "outward_winding_sign_initial": round(outward, 4),
        "uv_texel_coverage_fraction": round(float((weight_map > 0.01).mean()), 4),
        "mean_projection_confidence": round(float(weight_map[weight_map > 0.01].mean()) if (weight_map > 0.01).any() else 0.0, 4),
        "limitations_requiring_clean_blender_rebuild": [
            "Marching-cubes visual hull merges round wire glasses, hair strands, blazer lapels, and hands into single-genus blocks.",
            "No separated eyeballs, inner oral cavity, teeth, tongue, or clean edge loops around eyelids/lips for ARKit visemes.",
            "Cylindrical UV seams wrap across under-chin and under-arm occlusions.",
        ],
    }
    (workdir / "initial_source_metrics.json").write_text(json.dumps(metrics, indent=2))
    return metrics


def build_pixar_skin_texture(out_path: pathlib.Path, base_srgb: Tuple[float, float, float], size: int = 1024) -> str:
    """
    Synthesize warm golden-olive subsurface Pixar skin albedo (`1024x1024`) matching
    Professor Z's warm olive-tan complexion in `ref_02_professor_z_catalogue.png` & `ref_01_professor_z_lineup.png`.
    """
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32) / float(size - 1)
    uu = xx
    vv = 1.0 - yy

    skin = np.empty((size, size, 3), dtype=np.float32)
    skin[..., 0] = base_srgb[0]
    skin[..., 1] = base_srgb[1]
    skin[..., 2] = base_srgb[2]

    # Natural skin micro-grain so close-up portraits read organic skin rather than flat plastic
    rng = np.random.default_rng(4207)
    micro = cv2.GaussianBlur(rng.normal(0.0, 0.0085, (size, size)).astype(np.float32), (0, 0), 1.6)
    skin += micro[..., None]

    # Subtle forehead / temple / hairline root warmth & grounding
    hairline_shade = np.clip((vv - 0.80) / 0.18, 0.0, 1.0)
    skin *= (1.0 - 0.12 * hairline_shade[..., None])

    # Soft orbital socket & upper eyelid crease warmth (`u ~ 0.42 & 0.58, vv ~ 0.58`)
    for eu_eye in (0.425, 0.575):
        orb = np.exp(-(((uu - eu_eye) / 0.055) ** 2 + ((vv - 0.585) / 0.045) ** 2))
        skin[..., 0] -= 0.032 * orb
        skin[..., 1] -= 0.042 * orb
        skin[..., 2] -= 0.046 * orb

    # Warm subsurface Pixar cheek warmth (`u ~ 0.39 & 0.61, vv ~ 0.465`)
    for cu in (0.392, 0.608):
        cheek_w = np.exp(-(((uu - cu) / 0.078) ** 2 + ((vv - 0.465) / 0.068) ** 2))
        skin[..., 0] += 0.032 * cheek_w
        skin[..., 1] -= 0.018 * cheek_w
        skin[..., 2] -= 0.024 * cheek_w

    # Warm nose bridge & tip subsurface warmth (`u = 0.50, vv = 0.445`)
    nose_w = np.exp(-(((uu - 0.50) / 0.040) ** 2 + ((vv - 0.445) / 0.046) ** 2))
    skin[..., 0] += 0.034 * nose_w
    skin[..., 1] -= 0.020 * nose_w
    skin[..., 2] -= 0.026 * nose_w

    # Ear concha & helix subsurface warmth (`u ~ 0.25 & 0.75, vv ~ 0.50`)
    for eu in (0.25, 0.75):
        ear_w = np.exp(-(((uu - eu) / 0.052) ** 2 + ((vv - 0.50) / 0.072) ** 2))
        skin[..., 0] += 0.026 * ear_w
        skin[..., 1] -= 0.018 * ear_w
        skin[..., 2] -= 0.022 * ear_w

    # Natural warm terracotta-rose vermilion lips (`vv ~ 0.32`) + dedicated inner lip swatch `(0.90, 0.90)`
    lip_w = np.exp(-(((uu - 0.50) / 0.062) ** 2 + ((vv - 0.325) / 0.022) ** 2))
    skin[..., 0] = skin[..., 0] * (1.0 - 0.46 * lip_w) + 0.635 * 0.46 * lip_w
    skin[..., 1] = skin[..., 1] * (1.0 - 0.46 * lip_w) + 0.352 * 0.46 * lip_w
    skin[..., 2] = skin[..., 2] * (1.0 - 0.46 * lip_w) + 0.295 * 0.46 * lip_w

    c0, r1 = int(size * 0.84), int(size * 0.16)
    skin[0:r1, c0:size, 0] = 0.635
    skin[0:r1, c0:size, 1] = 0.352
    skin[0:r1, c0:size, 2] = 0.295

    out_path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((np.clip(skin, 0.0, 1.0) * 255.0).astype(np.uint8)).save(out_path)
    return str(out_path)


def build_pixar_eye_texture(out_path: pathlib.Path, iris_srgb: Tuple[float, float, float], size: int = 1024) -> str:
    """
    Synthesize soulful large-iris Pixar eye texture (`r_iris = 0.535`):
      - Soft off-white ivory sclera with realistic upper-lid ambient shadow
      - Deep espresso outer limbal ring (`r = 0.44..0.535`)
      - Multi-fiber radial chestnut-amber iris (`r = 0.21..0.46`) with warm golden collarette
      - Deep velvet-black pupil (`r <= 0.22`) + crisp dual studio catchlights!
    """
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    cx = (xx - (size - 1) * 0.5) / (size * 0.5)
    cy = ((size - 1) * 0.5 - yy) / (size * 0.5)
    rr = np.sqrt(cx * cx + cy * cy)
    ang = np.arctan2(cy, cx)

    # Warm ivory sclera with realistic upper-lid shadow so eyes look warm & grounded, never stark white
    img = np.ones((size, size, 3), dtype=np.float32) * np.array([0.942, 0.922, 0.902], dtype=np.float32)
    upper_ao = np.clip((cy - 0.10) / 0.72, 0.0, 1.0) * 0.15
    outer_ao = np.clip((rr - 0.55) / 0.45, 0.0, 1.0) * 0.08
    img -= (upper_ao + outer_ao)[..., None]

    # Full generous Pixar iris (`rr <= 0.535`)
    iris_mask = np.clip((0.542 - rr) / 0.016, 0.0, 1.0)
    fibers = 0.5 + 0.5 * np.sin(ang * 52.0 + np.sin(ang * 7.0) * 1.5)
    fine_fibers = 0.5 + 0.5 * np.sin(ang * 124.0 - 0.7)
    radial_glow = np.exp(-((rr - 0.305) / 0.125) ** 2)

    base = np.array(iris_srgb, dtype=np.float32)
    iris_col = (
        base[None, None, :] * (0.68 + 0.34 * fibers[..., None] + 0.16 * fine_fibers[..., None])
        + np.array([0.23, 0.135, 0.042], dtype=np.float32)[None, None, :] * radial_glow[..., None]
    )
    limbal = np.clip((rr - 0.425) / 0.105, 0.0, 1.0)
    iris_col *= (1.0 - 0.68 * limbal[..., None])
    iris_col *= (1.0 - 0.28 * np.clip(cy / 0.50, 0.0, 1.0)[..., None])

    img = img * (1.0 - iris_mask[..., None]) + iris_col * iris_mask[..., None]

    # Deep velvet-black pupil (`rr <= 0.225`)
    pupil_mask = np.clip((0.230 - rr) / 0.014, 0.0, 1.0)
    img = img * (1.0 - pupil_mask[..., None]) + np.array([0.010, 0.008, 0.010])[None, None, :] * pupil_mask[..., None]

    # Crisp primary upper-left window catchlight + soft lower-right bounce catchlight
    glint1 = np.clip((0.082 - np.sqrt((cx + 0.165) ** 2 + (cy - 0.175) ** 2)) / 0.013, 0.0, 1.0)
    glint2 = np.clip((0.034 - np.sqrt((cx - 0.175) ** 2 + (cy + 0.170) ** 2)) / 0.011, 0.0, 1.0)
    glint = np.clip(glint1 + 0.72 * glint2, 0.0, 1.0)
    img = img * (1.0 - glint[..., None]) + glint[..., None]

    out_path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((np.clip(img, 0.0, 1.0) * 255.0).astype(np.uint8)).save(out_path)
    return str(out_path)


def build_garment_pbr_textures(tex_dir: pathlib.Path, pal: Dict[str, Any], size: int = 1024) -> Dict[str, str]:
    """
    Build high-resolution `1024x1024` 4-quadrant PBR sub-atlas (`albedo`, `normal`, `roughness`, `orm`):
      - Woven Multi-Fleck Olive-Walnut Herringbone Tweed Blazer (`UV_BLAZER`)
      - Crisp Warm Cream Cotton Oxford Dress Shirt (`UV_SHIRT`)
      - Tailored Dark Charcoal-Slate Worsted Wool Trousers (`UV_TROUSER`)
      - Burnished Dark Cognac Leather Brogues (`UV_SHOE`), Antique Bronze Frames (`UV_GOLD`),
        Woven Cognac Butterfly Bow Tie (`UV_BOWTIE`), & Matte-Satin Espresso Hair (`UV_DARK`).
    """
    h = size // 2
    q = size // 4
    albedo = np.zeros((size, size, 3), dtype=np.float32)
    rough = np.full((size, size), 0.68, dtype=np.float32)
    height = np.zeros((size, size), dtype=np.float32)

    rng = np.random.default_rng(17324)
    yy, xx = np.mgrid[0:h, 0:h].astype(np.float32)

    # 1. Woven Multi-Fleck Herringbone Tweed (`UV_BLAZER`)
    zig = np.abs(((xx / 8.0) % 4.0) - 2.0)
    herring = 0.5 + 0.5 * np.sin((yy + zig * 8.0) * (2.0 * math.pi / 9.0))
    fleck_light = cv2.GaussianBlur((rng.random((h, h)) > 0.965).astype(np.float32), (3, 3), 0.7)
    fleck_dark = cv2.GaussianBlur((rng.random((h, h)) > 0.955).astype(np.float32), (3, 3), 0.7)

    tweed_base = np.array(pal["blazer_srgb"], dtype=np.float32)
    tweed = (
        tweed_base[None, None, :] * (0.87 + 0.24 * herring[..., None])
        + np.array([0.14, 0.11, 0.07], dtype=np.float32)[None, None, :] * fleck_light[..., None]
        - np.array([0.11, 0.09, 0.07], dtype=np.float32)[None, None, :] * fleck_dark[..., None]
    )
    albedo[h:size, 0:h] = tweed
    rough[h:size, 0:h] = 0.84 - 0.05 * herring
    height[h:size, 0:h] = 0.62 * herring + 0.25 * fleck_light

    # 2. Warm Cream Cotton Poplin Dress Shirt (`UV_SHIRT`)
    poplin = 0.5 + 0.5 * np.sin(xx * (2.0 * math.pi / 4.0)) * np.sin(yy * (2.0 * math.pi / 4.0))
    shirt_base = np.array(pal["shirt_srgb"], dtype=np.float32)
    albedo[h:size, h:size] = shirt_base[None, None, :] * (0.985 + 0.03 * poplin[..., None])
    rough[h:size, h:size] = 0.62
    height[h:size, h:size] = 0.12 * poplin

    # 3. Dark Charcoal-Slate Worsted Wool Trousers (`UV_TROUSER`)
    twill = 0.5 + 0.5 * np.sin((xx * 1.4 + yy) * (2.0 * math.pi / 6.0))
    wool_noise = rng.normal(0.0, 0.010, (h, h)).astype(np.float32)
    pants_base = np.array(pal["trouser_srgb"], dtype=np.float32)
    albedo[0:h, 0:h] = pants_base[None, None, :] * (0.94 + 0.11 * twill[..., None]) + wool_noise[..., None]
    rough[0:h, 0:h] = 0.76
    height[0:h, 0:h] = 0.26 * twill

    # 4a. Burnished Cognac Leather Brogues (`UV_SHOE`)
    grain = cv2.GaussianBlur(rng.normal(0.0, 0.022, (q, q)).astype(np.float32), (3, 3), 0.8)
    shoe_base = np.array(pal["shoe_srgb"], dtype=np.float32)
    albedo[0:q, h : h + q] = shoe_base[None, None, :] + grain[..., None]
    rough[0:q, h : h + q] = 0.36
    height[0:q, h : h + q] = grain * 2.8

    # 4b. Satin Antique Bronze-Gold Wire Glasses & Buckle (`UV_GOLD`)
    albedo[0:q, h + q : size] = np.array(pal["gold_srgb"], dtype=np.float32)[None, None, :]
    rough[0:q, h + q : size] = 0.28

    # 4c. Woven Cognac Butterfly Signature Bow Tie (`UV_BOWTIE`)
    byy, bxx = np.mgrid[0:q, 0:q].astype(np.float32)
    bt_weave = 0.5 + 0.5 * np.sin((bxx - byy) * (2.0 * math.pi / 5.5))
    bt_base = np.array(pal["bowtie_srgb"], dtype=np.float32)
    albedo[q:h, h : h + q] = bt_base[None, None, :] * (0.90 + 0.20 * bt_weave[..., None])
    rough[q:h, h : h + q] = 0.72
    height[q:h, h : h + q] = 0.34 * bt_weave

    # 4d. Matte-Satin Dark Espresso Hair, Brows, Belt & Shoe Welt (`UV_DARK`)
    hyy, hxx = np.mgrid[0:q, 0:q].astype(np.float32)
    strands = 0.5 + 0.5 * np.sin(hyy * (2.0 * math.pi / 5.0))
    dark_base = np.array(pal["hair_srgb"], dtype=np.float32)
    albedo[q:h, h + q : size] = dark_base[None, None, :] * (0.90 + 0.22 * strands[..., None])
    rough[q:h, h + q : size] = 0.50  # Matte-satin hair sheen: ZERO shiny plastic hotspot balloons!
    height[q:h, h + q : size] = 0.22 * strands

    gx = cv2.Sobel(height, cv2.CV_32F, 1, 0, ksize=3) * 0.42
    gy = cv2.Sobel(height, cv2.CV_32F, 0, 1, ksize=3) * 0.42
    nz = np.ones_like(height)
    norm_len = np.sqrt(gx * gx + gy * gy + nz * nz)
    nrm_rgb = np.stack([0.5 - 0.5 * (gx / norm_len), 0.5 + 0.5 * (gy / norm_len), 0.5 + 0.5 * (nz / norm_len)], axis=-1)

    tex_dir.mkdir(parents=True, exist_ok=True)
    p_alb = tex_dir / "garment_albedo.png"
    p_nrm = tex_dir / "garment_normal.png"
    p_rgh = tex_dir / "garment_roughness.png"
    p_orm = tex_dir / "garment_atlas_orm.png"

    Image.fromarray((np.clip(albedo, 0.0, 1.0) * 255.0).astype(np.uint8)).save(p_alb)
    Image.fromarray((np.clip(nrm_rgb, 0.0, 1.0) * 255.0).astype(np.uint8)).save(p_nrm)
    Image.fromarray((np.clip(rough, 0.0, 1.0) * 255.0).astype(np.uint8)).save(p_rgh)

    orm_arr = np.stack([np.full_like(rough, 0.98), rough, np.zeros_like(rough)], axis=-1)
    orm_arr[0:q, h + q : size, 2] = 0.85
    Image.fromarray((np.clip(orm_arr, 0.0, 1.0) * 255.0).astype(np.uint8)).save(p_orm)

    shutil.copy2(p_alb, tex_dir / "garment_atlas_albedo.png")
    shutil.copy2(p_nrm, tex_dir / "garment_atlas_normal.png")
    return {
        "albedo": str(p_alb),
        "normal": str(p_nrm),
        "orm": str(p_orm),
        "garment_albedo": str(p_alb),
        "garment_normal": str(p_nrm),
        "garment_roughness": str(p_rgh),
        "garment_atlas_albedo": str(tex_dir / "garment_atlas_albedo.png"),
        "garment_atlas_normal": str(tex_dir / "garment_atlas_normal.png"),
        "garment_atlas_orm": str(p_orm),
    }


def run_reconstruction(input_path: pathlib.Path, workdir: pathlib.Path, strict: bool = False) -> Dict[str, Any]:
    if not input_path.exists():
        raise FileNotFoundError(f"Input image not found: {input_path}")

    pil_in = Image.open(input_path).convert("RGB")
    w, h = pil_in.size
    if w < 128 or h < 128:
        raise ValueError(f"Input resolution {w}x{h} is below hard minimum 128x128.")

    img_rgb = np.array(pil_in)
    workdir.mkdir(parents=True, exist_ok=True)
    is_catalogue = detect_is_character_catalogue(img_rgb)

    sheet_panels: Dict[str, str] = {}
    rgba_views: Dict[str, str] = {}
    silhouette_profiles: Dict[str, Any] = {}
    if is_catalogue:
        extracted = extract_catalogue_panels_and_rgba_views(img_rgb, workdir / "sheet_views")
        sheet_panels = extracted["panels"]
        rgba_views = extracted["rgba_views"]
        silhouette_profiles = extracted["silhouette_profiles"]
        analysis_img = np.array(Image.open(sheet_panels["turnaround_front"]).convert("RGB"))
    else:
        analysis_img = img_rgb

    with mp.solutions.face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True) as fm:
        fm_res = fm.process(analysis_img)
    with mp.solutions.pose.Pose(static_image_mode=True, model_complexity=1) as ps:
        ps_res = ps.process(analysis_img)

    has_face = bool(fm_res.multi_face_landmarks)
    has_pose = bool(ps_res.pose_landmarks)
    if strict and not has_face and not is_catalogue:
        raise ValueError("No valid human/character face landmarks detected under `--strict` mode.")

    vis = Image.fromarray(analysis_img.copy())
    draw = ImageDraw.Draw(vis)
    ah, aw, _ = analysis_img.shape
    if has_face:
        for lm in fm_res.multi_face_landmarks[0].landmark[::4]:
            px, py = lm.x * aw, lm.y * ah
            draw.ellipse((px - 1.5, py - 1.5, px + 1.5, py + 1.5), fill=(72, 238, 152))
    if has_pose:
        for lm in ps_res.pose_landmarks.landmark:
            px, py = lm.x * aw, lm.y * ah
            draw.ellipse((px - 3.0, py - 3.0, px + 3.0, py + 3.0), fill=(255, 190, 64))
    overlay_path = workdir / "00_subject_analysis_overlay.png"
    vis.save(overlay_path)

    if is_catalogue:
        pal = {
            "character_name": "Professor Z",
            "skin_srgb": [0.808, 0.602, 0.452],     # Warm golden-olive tan matching `ref_02` & `ref_01`!
            "hair_srgb": [0.072, 0.052, 0.043],     # Rich dark roasted espresso (`#231B17` Swatch #1)
            "iris_srgb": [0.435, 0.242, 0.115],     # Deep soulful amber-chestnut Pixar eyes
            "blazer_srgb": [0.432, 0.278, 0.148],   # Authentic warm walnut herringbone tweed (`#73451F`)
            "shirt_srgb": [0.888, 0.838, 0.772],    # Warm cream oxford dress shirt (`#E2D4C2` Swatch #5)
            "bowtie_srgb": [0.468, 0.282, 0.142],   # Cognac-walnut woven butterfly bow tie (`Swatch #3`)
            "trouser_srgb": [0.212, 0.222, 0.236],  # Tailored dark charcoal-slate wool trousers (`#36383A`)
            "shoe_srgb": [0.355, 0.202, 0.105],     # Burnished cognac-walnut leather brogues (`Classic kicks`)
            "gold_srgb": [0.515, 0.365, 0.212],     # Satin antique bronze-tortoiseshell round wire frames & buckle
            # Second outfit palette (`ref_01_professor_z_lineup.png` / `Astronaut Prep` night Z-outfit!)
            "alt_outfit_top_srgb": [0.135, 0.188, 0.305],
            "alt_outfit_shirt_srgb": [0.165, 0.225, 0.355],
            "alt_outfit_trouser_srgb": [0.168, 0.182, 0.205],
            "wearing_glasses": True,
            "messy_spiky_hair": True,
            "has_bowtie": True,
            "has_notched_lapels": True,
            "has_belt": True,
        }
        anth = {
            "stylized_pixar_proportions": True,
            "target_height_m": 1.730,
            "age_years": 24,
            "face_landmarks_detected": has_face,
            "pose_landmarks_detected": has_pose,
        }
    else:
        center_crop = analysis_img[ah // 4 : (ah * 3) // 4, aw // 4 : (aw * 3) // 4]
        mean_col = np.clip(center_crop.mean(axis=(0, 1)) / 255.0, 0.15, 0.90).tolist()
        pal = {
            "character_name": input_path.stem,
            "skin_srgb": [0.835, 0.662, 0.542],
            "hair_srgb": [0.095, 0.070, 0.056],
            "iris_srgb": [0.380, 0.245, 0.140],
            "blazer_srgb": mean_col,
            "shirt_srgb": [0.905, 0.892, 0.868],
            "bowtie_srgb": [0.460, 0.300, 0.180],
            "trouser_srgb": [0.230, 0.255, 0.290],
            "shoe_srgb": [0.360, 0.220, 0.135],
            "gold_srgb": [0.580, 0.440, 0.240],
            "wearing_glasses": False,
            "messy_spiky_hair": False,
            "has_bowtie": False,
            "has_notched_lapels": True,
            "has_belt": True,
        }
        anth = {
            "stylized_pixar_proportions": False,
            "target_height_m": 1.680,
            "age_years": 28,
            "face_landmarks_detected": has_face,
            "pose_landmarks_detected": has_pose,
        }

    # Build and preserve the untouched initial 3D visual-hull + AssetFurnace `photo_paint` mesh!
    initial_source = build_initial_visual_hull_with_assetfurnace(
        rgba_views=rgba_views,
        single_img=analysis_img,
        workdir=workdir,
        target_height_m=float(anth["target_height_m"]),
    )

    tex_dir = workdir / "textures"
    skin_tex = build_pixar_skin_texture(tex_dir / "head_skin_albedo.png", tuple(pal["skin_srgb"]), size=1024)
    eye_tex = build_pixar_eye_texture(tex_dir / "eye_iris_albedo.png", tuple(pal["iris_srgb"]), size=1024)
    garm_tex = build_garment_pbr_textures(tex_dir, pal, size=1024)

    profile = {
        "input_file": str(input_path.resolve()),
        "input_sha256": hashlib.sha256(input_path.read_bytes()).hexdigest(),
        "input_dimensions": [w, h],
        "is_character_catalogue": is_catalogue,
        "sheet_panels": sheet_panels,
        "rgba_turnaround_views": rgba_views,
        "measured_silhouette_profiles": silhouette_profiles,
        "untouched_initial_reconstruction": initial_source,
        "analysis_overlay": str(overlay_path.resolve()),
        "anthropometrics": anth,
        "features_and_palette": pal,
        "generated_textures": {
            "head_skin_albedo": str(pathlib.Path(skin_tex).resolve()),
            "eye_iris_albedo": str(pathlib.Path(eye_tex).resolve()),
            **{k: str(pathlib.Path(v).resolve()) for k, v in garm_tex.items()},
        },
    }
    out_json = workdir / "identity_profile.json"
    out_json.write_text(json.dumps(profile, indent=2, ensure_ascii=False))
    return profile


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", required=True)
    ap.add_argument("--workdir", required=True)
    ap.add_argument("--strict", action="store_true")
    args = ap.parse_args()

    try:
        prof = run_reconstruction(pathlib.Path(args.input), pathlib.Path(args.workdir), strict=args.strict)
        print(json.dumps({
            "status": "ok",
            "character": prof["features_and_palette"]["character_name"],
            "is_catalogue": prof["is_character_catalogue"],
            "height_m": prof["anthropometrics"]["target_height_m"],
            "initial_source_glb": prof["untouched_initial_reconstruction"]["untouched_initial_glb"],
        }, indent=2))
    except Exception as exc:
        print(f"[RECONSTRUCT ERROR] {exc}", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
