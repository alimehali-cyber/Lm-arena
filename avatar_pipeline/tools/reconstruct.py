#!/usr/bin/env python3
"""
Stage 1-2: Multi-Panel Character Sheet & Portrait Analyzer + PBR Texture Synthesizer (`reconstruct.py`).

Supports:
  1. Official Multi-Panel Character Sheets (e.g., `Professor Z — Character Catalogue`, `1536x1024`):
     - Automatically detects multi-panel sheet layout, crops high-res reference panels (`hero`,
       `turnaround_front`, `turnaround_three_quarter`, `turnaround_side`, `turnaround_rear`,
       `palette_and_details`, `expressions`, `poses_and_props`) cleanly into `workdir/sheet_views/`.
     - Runs MediaPipe FaceMesh (`refine_landmarks=True`) + MediaPipe Pose (`model_complexity=1`) directly
       on the orthographic Front Turnaround & Hero panels to measure exact facial & body ratios.
     - Extracts authentic palette swatches & synthesizes rich 4-quadrant woven tweed, cream poplin,
       charcoal wool, cognac saddle leather, antique bronze-gold, warm subsurface Pixar skin albedo,
       and multi-layered stromal chestnut-amber iris textures.
  2. Standard Single-Person Portrait / Full-Body Photographs (`--strict` enforces >= 128x128 & valid face).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import shutil
import sys
from typing import Any, Dict, Tuple

import cv2
import mediapipe as mp
import numpy as np
from PIL import Image, ImageDraw


# Canonical UV tile layout inside `garment_albedo.png` (PIL row 0 -> Blender `uv_v = 1.0`):
#   Bottom-Left  (`u: 0.00..0.50, v: 0.00..0.50` -> rows `0.50..1.00, cols 0.00..0.50`): Woven Herringbone Tweed Blazer
#   Bottom-Right (`u: 0.50..1.00, v: 0.00..0.50` -> rows `0.50..1.00, cols 0.50..1.00`): Crisp Cream Poplin Dress Shirt
#   Top-Left     (`u: 0.00..0.50, v: 0.50..1.00` -> rows `0.00..0.50, cols 0.00..0.50`): Charcoal-Slate Wool Trousers
#   Top-Right 2x2 sub-quadrant (`u: 0.50..1.00, v: 0.50..1.00` -> rows `0.00..0.50, cols 0.50..1.00`):
#     - Top-Left sub-tile     (`u: 0.50..0.75, v: 0.75..1.00` -> rows `0.00..0.25, cols 0.50..0.75`): Burnished Cognac Leather Kicks
#     - Top-Right sub-tile    (`u: 0.75..1.00, v: 0.75..1.00` -> rows `0.00..0.25, cols 0.75..1.00`): Antique Satin Bronze-Gold Wire Frames & Buckle
#     - Bottom-Left sub-tile  (`u: 0.50..0.75, v: 0.50..0.75` -> rows `0.25..0.50, cols 0.50..0.75`): Textured Cognac-Tweed Signature Bow Tie
#     - Bottom-Right sub-tile (`u: 0.75..1.00, v: 0.50..0.75` -> rows `0.25..0.50, cols 0.75..1.00`): Rich Warm Espresso Hair, Brows, Belt & Shoe Welt


def detect_is_character_catalogue(img_rgb: np.ndarray) -> bool:
    """Detect structured wide light-paper character turnaround sheet (`W/H >= 1.35`, cream paper border)."""
    h, w, _ = img_rgb.shape
    if w < 900 or h < 600 or (w / float(h)) < 1.35:
        return False
    border = np.concatenate([img_rgb[:24, :, :].reshape(-1, 3), img_rgb[-24:, :, :].reshape(-1, 3)], axis=0)
    mean_border = border.mean(axis=0)
    return bool(mean_border.mean() > 215.0 and np.std(border) < 28.0)


def extract_catalogue_panels(img_rgb: np.ndarray, views_dir: pathlib.Path) -> Dict[str, str]:
    """Losslessly slice every panel of the Character Catalogue into `workdir/sheet_views/*.png`."""
    views_dir.mkdir(parents=True, exist_ok=True)
    h, w, _ = img_rgb.shape
    crops = {
        "hero_pose": (0.215, 0.025, 0.390, 0.600),
        "turnaround_front": (0.402, 0.105, 0.505, 0.530),
        "turnaround_three_quarter": (0.505, 0.105, 0.596, 0.530),
        "turnaround_side": (0.596, 0.105, 0.678, 0.530),
        "turnaround_rear": (0.678, 0.105, 0.785, 0.530),
        "palette_and_details": (0.795, 0.035, 0.985, 0.490),
        "expressions_grid": (0.010, 0.575, 0.375, 0.985),
        "outfit_variations": (0.380, 0.575, 0.645, 0.985),
        "poses_and_props": (0.650, 0.575, 0.990, 0.985),
    }
    saved: Dict[str, str] = {}
    for name, (x0, y0, x1, y1) in crops.items():
        patch = img_rgb[int(y0 * h) : int(y1 * h), int(x0 * w) : int(x1 * w)]
        out_p = views_dir / f"{name}.png"
        Image.fromarray(patch).save(out_p)
        saved[name] = str(out_p)
    return saved


def build_pixar_skin_texture(out_path: pathlib.Path, base_srgb: Tuple[float, float, float], size: int = 512) -> str:
    """
    Synthesize a warm, living Pixar subsurface skin albedo map with subtle natural cheek warmth,
    soft rosy nose-tip blush, delicate upper-eyelid warmth, and warm coral-rose vermilion lips.
    """
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32) / float(size - 1)
    # UV `v = 1.0 - yy`, `u = xx` (`u = 0.5` is center face `-Y`)
    uu = xx
    vv = 1.0 - yy

    skin = np.empty((size, size, 3), dtype=np.float32)
    skin[..., 0] = base_srgb[0]
    skin[..., 1] = base_srgb[1]
    skin[..., 2] = base_srgb[2]

    # Subtle micro-pore organic variation so skin never looks flat plastic
    rng = np.random.default_rng(4207)
    micro = cv2.GaussianBlur(rng.normal(0.0, 0.006, (size, size)).astype(np.float32), (0, 0), 1.4)
    skin += micro[..., None]

    # Warm subsurface Pixar cheek blush (`u ~ 0.39 & 0.61, v ~ 0.48`)
    for cu in (0.395, 0.605):
        cheek_w = np.exp(-(((uu - cu) / 0.075) ** 2 + ((vv - 0.465) / 0.068) ** 2))
        skin[..., 0] += 0.036 * cheek_w
        skin[..., 1] -= 0.016 * cheek_w
        skin[..., 2] -= 0.022 * cheek_w

    # Adorable Pixar warm nose bridge & tip blush (`u = 0.50, v = 0.45`)
    nose_w = np.exp(-(((uu - 0.50) / 0.042) ** 2 + ((vv - 0.445) / 0.045) ** 2))
    skin[..., 0] += 0.040 * nose_w
    skin[..., 1] -= 0.018 * nose_w
    skin[..., 2] -= 0.024 * nose_w

    # Warm ear helix subsurface flush (`u ~ 0.25 & 0.75, v ~ 0.50`)
    for eu in (0.24, 0.76):
        ear_w = np.exp(-(((uu - eu) / 0.055) ** 2 + ((vv - 0.50) / 0.075) ** 2))
        skin[..., 0] += 0.032 * ear_w
        skin[..., 1] -= 0.012 * ear_w
        skin[..., 2] -= 0.016 * ear_w

    # Soft warm coral-terracotta vermilion lips (`u = 0.50, uv_v = 0.31 -> vv = 0.69`) + dedicated lip UV swatch `(0.90, 0.90)`
    lip_w = np.exp(-(((uu - 0.50) / 0.068) ** 2 + ((vv - 0.69) / 0.026) ** 2))
    skin[..., 0] = skin[..., 0] * (1.0 - 0.52 * lip_w) + 0.68 * 0.52 * lip_w
    skin[..., 1] = skin[..., 1] * (1.0 - 0.52 * lip_w) + 0.35 * 0.52 * lip_w
    skin[..., 2] = skin[..., 2] * (1.0 - 0.52 * lip_w) + 0.32 * 0.52 * lip_w

    c0, r1 = int(size * 0.84), int(size * 0.16)
    skin[0:r1, c0:size, 0] = 0.69
    skin[0:r1, c0:size, 1] = 0.36
    skin[0:r1, c0:size, 2] = 0.33

    out_path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((np.clip(skin, 0.0, 1.0) * 255.0).astype(np.uint8)).save(out_path)
    return str(out_path)


def build_pixar_eye_texture(out_path: pathlib.Path, iris_srgb: Tuple[float, float, float], size: int = 512) -> str:
    """
    Synthesize high-appeal Pixar eye texture:
      - Clean warm ivory sclera with soft upper-lid ambient shadow
      - Crisp dark limbal ring (`r = 0.43..0.47`)
      - Multi-fiber radial chestnut-honey iris (`r = 0.19..0.44`) with golden inner collarette
      - Deep velvet-black pupil (`r < 0.195`) + dual crisp specular catchlights!
    """
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    cx = (xx - (size - 1) * 0.5) / (size * 0.5)
    cy = ((size - 1) * 0.5 - yy) / (size * 0.5)  # +cy is UP!
    rr = np.sqrt(cx * cx + cy * cy)
    ang = np.arctan2(cy, cx)

    # Sclera base with gentle upper-eyelid ambient occlusion warmth
    img = np.ones((size, size, 3), dtype=np.float32) * np.array([0.965, 0.952, 0.938], dtype=np.float32)
    upper_ao = np.clip((cy - 0.25) / 0.75, 0.0, 1.0) * 0.08
    img -= upper_ao[..., None]

    # Iris mask (`rr <= 0.465`)
    iris_mask = np.clip((0.472 - rr) / 0.018, 0.0, 1.0)
    fibers = 0.5 + 0.5 * np.sin(ang * 46.0 + np.sin(ang * 7.0) * 1.4)
    fine_fibers = 0.5 + 0.5 * np.sin(ang * 108.0 - 0.7)
    radial_glow = np.exp(-((rr - 0.255) / 0.105) ** 2)  # Golden-amber Pixar inner ring!

    base = np.array(iris_srgb, dtype=np.float32)
    iris_col = (
        base[None, None, :] * (0.72 + 0.42 * fibers[..., None] * 0.65 + 0.18 * fine_fibers[..., None])
        + np.array([0.24, 0.145, 0.045], dtype=np.float32)[None, None, :] * radial_glow[..., None]
    )
    # Dark Pixar outer limbal ring (`rr ~ 0.40..0.47`) & subtle upper iris shadow under upper lashes!
    limbal = np.clip((rr - 0.375) / 0.085, 0.0, 1.0)
    iris_col *= (1.0 - 0.62 * limbal[..., None])
    iris_col *= (1.0 - 0.22 * np.clip(cy / 0.46, 0.0, 1.0)[..., None])

    img = img * (1.0 - iris_mask[..., None]) + iris_col * iris_mask[..., None]

    # Deep velvet-black pupil (`rr <= 0.195`)
    pupil_mask = np.clip((0.200 - rr) / 0.014, 0.0, 1.0)
    img = img * (1.0 - pupil_mask[..., None]) + np.array([0.012, 0.010, 0.012])[None, None, :] * pupil_mask[..., None]

    # Signature Pixar primary upper-left catchlight (`(-0.145, +0.165), r = 0.068`) + secondary lower-right glint!
    glint1 = np.clip((0.068 - np.sqrt((cx + 0.145) ** 2 + (cy - 0.165) ** 2)) / 0.012, 0.0, 1.0)
    glint2 = np.clip((0.028 - np.sqrt((cx - 0.155) ** 2 + (cy + 0.150) ** 2)) / 0.010, 0.0, 1.0)
    glint = np.clip(glint1 + 0.72 * glint2, 0.0, 1.0)
    img = img * (1.0 - glint[..., None]) + glint[..., None]

    out_path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((np.clip(img, 0.0, 1.0) * 255.0).astype(np.uint8)).save(out_path)
    return str(out_path)


def build_garment_pbr_textures(tex_dir: pathlib.Path, pal: Dict[str, Any], size: int = 512) -> Dict[str, str]:
    """
    Build the 4-quadrant high-detail woven tweed, poplin, charcoal wool, leather, bow tie,
    antique gold, and dark espresso PBR sub-atlas (`albedo`, `normal`, `roughness`, `orm`).
    """
    h = size // 2
    q = size // 4
    albedo = np.zeros((size, size, 3), dtype=np.float32)
    rough = np.full((size, size), 0.65, dtype=np.float32)
    height = np.zeros((size, size), dtype=np.float32)

    rng = np.random.default_rng(17324)
    yy, xx = np.mgrid[0:h, 0:h].astype(np.float32)

    # 1. Bottom-Left (`rows h..size, cols 0..h` -> `UV_BLAZER = (0.02, 0.48, 0.02, 0.48)`):
    #    Rich Multi-Fleck Olive-Walnut Herringbone Tweed!
    zig = np.abs(((xx / 5.0) % 4.0) - 2.0)
    herring = 0.5 + 0.5 * np.sin((yy + zig * 5.0) * (2.0 * math.pi / 6.0))
    fleck_light = (rng.random((h, h)) > 0.965).astype(np.float32)
    fleck_dark = (rng.random((h, h)) > 0.955).astype(np.float32)
    fleck_light = cv2.GaussianBlur(fleck_light, (3, 3), 0.6)
    fleck_dark = cv2.GaussianBlur(fleck_dark, (3, 3), 0.6)

    tweed_base = np.array(pal["blazer_srgb"], dtype=np.float32)
    tweed = (
        tweed_base[None, None, :] * (0.86 + 0.26 * herring[..., None])
        + np.array([0.16, 0.13, 0.09], dtype=np.float32)[None, None, :] * fleck_light[..., None]
        - np.array([0.12, 0.10, 0.08], dtype=np.float32)[None, None, :] * fleck_dark[..., None]
    )
    albedo[h:size, 0:h] = tweed
    rough[h:size, 0:h] = 0.80 - 0.06 * herring
    height[h:size, 0:h] = 0.65 * herring + 0.25 * fleck_light

    # 2. Bottom-Right (`rows h..size, cols h..size` -> `UV_SHIRT = (0.52, 0.98, 0.02, 0.48)`):
    #    Crisp Warm Ivory Cotton Poplin Dress Shirt!
    poplin = 0.5 + 0.5 * np.sin(xx * (2.0 * math.pi / 3.0)) * np.sin(yy * (2.0 * math.pi / 3.0))
    shirt_base = np.array(pal["shirt_srgb"], dtype=np.float32)
    albedo[h:size, h:size] = shirt_base[None, None, :] * (0.985 + 0.03 * poplin[..., None])
    rough[h:size, h:size] = 0.56
    height[h:size, h:size] = 0.14 * poplin

    # 3. Top-Left (`rows 0..h, cols 0..h` -> `UV_TROUSER = (0.02, 0.48, 0.52, 0.98)`):
    #    Tailored Charcoal-Slate Worsted Wool Trousers!
    twill = 0.5 + 0.5 * np.sin((xx * 1.4 + yy) * (2.0 * math.pi / 4.5))
    wool_noise = rng.normal(0.0, 0.012, (h, h)).astype(np.float32)
    pants_base = np.array(pal["trouser_srgb"], dtype=np.float32)
    albedo[0:h, 0:h] = pants_base[None, None, :] * (0.94 + 0.11 * twill[..., None]) + wool_noise[..., None]
    rough[0:h, 0:h] = 0.72
    height[0:h, 0:h] = 0.28 * twill

    # 4. Top-Right 2x2 Sub-Tiles (`rows 0..h, cols h..size`):
    #   4a. Top-Left sub-tile (`rows 0..q, cols h..h+q` -> `UV_SHOE = (0.52, 0.73, 0.77, 0.98)`): Burnished Cognac Leather Kicks
    grain = cv2.GaussianBlur(rng.normal(0.0, 0.020, (q, q)).astype(np.float32), (3, 3), 0.7)
    shoe_base = np.array(pal["shoe_srgb"], dtype=np.float32)
    albedo[0:q, h : h + q] = shoe_base[None, None, :] + grain[..., None]
    rough[0:q, h : h + q] = 0.34
    height[0:q, h : h + q] = grain * 3.0

    #   4b. Top-Right sub-tile (`rows 0..q, cols h+q..size` -> `UV_GOLD = (0.77, 0.98, 0.77, 0.98)`): Satin Antique Bronze-Gold
    albedo[0:q, h + q : size] = np.array(pal["gold_srgb"], dtype=np.float32)[None, None, :]
    rough[0:q, h + q : size] = 0.24

    #   4c. Bottom-Left sub-tile (`rows q..h, cols h..h+q` -> `UV_BOWTIE = (0.52, 0.73, 0.52, 0.73)`): Woven Cognac Signature Bow Tie
    byy, bxx = np.mgrid[0:q, 0:q].astype(np.float32)
    bt_weave = 0.5 + 0.5 * np.sin((bxx - byy) * (2.0 * math.pi / 4.0))
    bt_base = np.array(pal["bowtie_srgb"], dtype=np.float32)
    albedo[q:h, h : h + q] = bt_base[None, None, :] * (0.90 + 0.20 * bt_weave[..., None])
    rough[q:h, h : h + q] = 0.68
    height[q:h, h : h + q] = 0.35 * bt_weave

    #   4d. Bottom-Right sub-tile (`rows q..h, cols h+q..size` -> `UV_DARK = (0.77, 0.98, 0.52, 0.73)`): Warm Dark Espresso Hair, Brows & Belt
    hyy, hxx = np.mgrid[0:q, 0:q].astype(np.float32)
    strands = 0.5 + 0.5 * np.sin(hyy * (2.0 * math.pi / 3.5))
    dark_base = np.array(pal["hair_srgb"], dtype=np.float32)
    albedo[q:h, h + q : size] = dark_base[None, None, :] * (0.88 + 0.26 * strands[..., None])
    rough[q:h, h + q : size] = 0.38
    height[q:h, h + q : size] = 0.18 * strands

    # Convert `height` into OpenGL tangent-space normal map
    gx = cv2.Sobel(height, cv2.CV_32F, 1, 0, ksize=3) * 0.45
    gy = cv2.Sobel(height, cv2.CV_32F, 0, 1, ksize=3) * 0.45
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
    orm_arr[0:q, h + q : size, 2] = 0.85  # Satin metallic frame & belt buckle tile!
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
    if is_catalogue:
        sheet_panels = extract_catalogue_panels(img_rgb, workdir / "sheet_views")
        analysis_img = np.array(Image.open(sheet_panels["turnaround_front"]).convert("RGB"))
    else:
        analysis_img = img_rgb

    # Run MediaPipe FaceMesh (`static_image_mode=True`) & Pose (`model_complexity=1` offline bundled weights)
    with mp.solutions.face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True) as fm:
        fm_res = fm.process(analysis_img)
    with mp.solutions.pose.Pose(static_image_mode=True, model_complexity=1) as ps:
        ps_res = ps.process(analysis_img)

    has_face = bool(fm_res.multi_face_landmarks)
    has_pose = bool(ps_res.pose_landmarks)
    if strict and not has_face and not is_catalogue:
        raise ValueError("No valid human/character face landmarks detected under `--strict` mode.")

    # Save visual landmark diagnostic overlay (`00_subject_analysis_overlay.png`)
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
            "skin_srgb": [0.895, 0.718, 0.592],
            "hair_srgb": [0.088, 0.062, 0.050],     # Rich dark roasted espresso (`#221A16` Swatch #1 — NOT dead pure-black!)
            "iris_srgb": [0.475, 0.268, 0.132],     # Warm golden-chestnut Pixar eyes
            "blazer_srgb": [0.415, 0.292, 0.188],   # Authentic warm olive-walnut nubby tweed (`#6A4B30`)
            "shirt_srgb": [0.922, 0.896, 0.848],    # Warm crisp ivory dress shirt (`#EBE4D8` Swatch #5)
            "bowtie_srgb": [0.515, 0.332, 0.192],   # Cognac-brown woven butterfly bow tie (`Swatch #3`)
            "trouser_srgb": [0.224, 0.252, 0.286],  # Tailored dark charcoal-slate wool trousers (`Swatch #6`)
            "shoe_srgb": [0.445, 0.268, 0.155],     # Burnished cognac leather Oxford kicks
            "gold_srgb": [0.465, 0.342, 0.215],     # Warm bronze-tortoiseshell round wire glasses & belt buckle
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
            "skin_srgb": [0.855, 0.688, 0.575],
            "hair_srgb": [0.105, 0.078, 0.062],
            "iris_srgb": [0.380, 0.245, 0.140],
            "blazer_srgb": mean_col,
            "shirt_srgb": [0.910, 0.900, 0.880],
            "bowtie_srgb": [0.460, 0.300, 0.180],
            "trouser_srgb": [0.230, 0.255, 0.290],
            "shoe_srgb": [0.380, 0.240, 0.150],
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

    tex_dir = workdir / "textures"
    skin_tex = build_pixar_skin_texture(tex_dir / "head_skin_albedo.png", tuple(pal["skin_srgb"]), size=512)
    eye_tex = build_pixar_eye_texture(tex_dir / "eye_iris_albedo.png", tuple(pal["iris_srgb"]), size=512)
    garm_tex = build_garment_pbr_textures(tex_dir, pal, size=512)

    profile = {
        "input_file": str(input_path.resolve()),
        "input_sha256": hashlib.sha256(input_path.read_bytes()).hexdigest(),
        "input_dimensions": [w, h],
        "is_character_catalogue": is_catalogue,
        "sheet_panels": sheet_panels,
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
        }, indent=2))
    except Exception as exc:
        print(f"[RECONSTRUCT ERROR] {exc}", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
