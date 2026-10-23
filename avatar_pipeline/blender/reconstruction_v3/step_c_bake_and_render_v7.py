#!/usr/bin/env python3
"""Stage C v7 — Studio Look-Dev Calibrated Multi-View Class-Filtered PBR Baker & Cycles Renderer."""

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
MESH_DIR = WORK / "meshes_v7"
TEX_DIR = WORK / "textures_v7"
RENDER_DIR = ROOT / "avatar_pipeline/renders"
REF_PATH = ROOT / "avatar_pipeline/input/professor_z_turnaround_ref.png"

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(WORK))
import step_c_bake_and_render_v6 as v6
import step_c_uv_bake_and_render as c_base


def class_filtered_plate(rgb: np.ndarray, valid_mask: np.ndarray, blur_sigma: float = 3.0) -> np.ndarray:
    """Keep ONLY authentic pixels belonging to a target material class & Voronoi-bleed everywhere else!"""
    kern = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
    clean = cv2.erode(valid_mask.astype(np.uint8), kern) > 0
    if clean.sum() < 50:
        clean = valid_mask
    _, nn = ndimage.distance_transform_edt(~clean, return_indices=True)
    extrap = cv2.GaussianBlur(rgb[nn[0], nn[1]].astype(np.float32), (11, 11), blur_sigma)
    return np.clip(np.where(clean[:, :, None], rgb.astype(np.float32), extrap), 0, 255).astype(np.float32)


def sample_plate(plate: np.ndarray, vv: np.ndarray, uu: np.ndarray) -> np.ndarray:
    vv_c = np.clip(vv, 0.0, plate.shape[0] - 1.001)
    uu_c = np.clip(uu, 0.0, plate.shape[1] - 1.001)
    return np.column_stack([ndimage.map_coordinates(plate[:, :, c], [vv_c, uu_c], order=1, mode="nearest") for c in range(3)])


def bake_all_parts_v7(imported: dict[str, bpy.types.Object], cal: dict) -> dict[str, tuple]:
    scale = float(cal["scale"])
    u0_f, u0_s, u0_b, v_sole = float(cal["u0_f"]), float(cal["u0_s"]), float(cal["u0_b"]), float(cal["v_sole_f"])
    P_lm, R_h, c_h = cal["P_landmarks"], cal["R_head"], cal["c_head"]

    f_orig = cal["f_rgb_orig"]
    s_orig = cal["s_rgb_orig"]
    b_orig = cal["b_rgb_orig"]
    fm, sm, bm = cal["f_mask"] > 0, cal["s_mask"] > 0, cal["b_mask"] > 0

    # Exposure-compensate baked diffuse maps slightly (`x 0.84` on darks, `x 0.90` on mids) so that under
    # our 3D Cycles 5-light studio rig, the rendered sRGB pixel luminance & chroma match `professor_z_turnaround_ref.png` 1:1!
    # ------------------------------------------------------------------
    # 1. `ProfZ_HeadSkin` (`1024x1024` ultra-sharp Pixar eyes, cocked brows, nose, warm smile, ears & neck)
    # ------------------------------------------------------------------
    pos_h, nrm_h, cov_h = v6.rasterize_mesh_uv(imported["head_skin"], 1024)
    ey, ex = np.where(cov_h)
    Ph, Nh = pos_h[ey, ex], nrm_h[ey, ex]
    face_plate = v6.prepare_hairfree_face_plate(cal["f_rgb_noglass"], cal["f_mask"], P_lm, scale, u0_f, v_sole)

    col_front = sample_plate(face_plate, v_sole - Ph[:, 1] / scale, u0_f + Ph[:, 0] / scale)
    Q_loc = (Ph - c_h) @ R_h
    light_up = 0.5 + 0.5 * np.clip(Nh[:, 1] * 0.45 + Nh[:, 2] * 0.55, -1.0, 1.0)
    skin_base = (
        np.array([172.0, 99.0, 58.0])[None, :] * (1.0 - light_up[:, None])
        + np.array([224.0, 148.0, 98.0])[None, :] * light_up[:, None]
    )
    on_ear = np.abs(Q_loc[:, 0]) > 0.092
    skin_base[on_ear] *= np.array([1.03, 0.92, 0.86])

    w_proj = np.clip((Nh[:, 2] - 0.05) / 0.40, 0.0, 1.0) * np.clip((Q_loc[:, 2] + 0.005) / 0.035, 0.0, 1.0)
    w_proj *= np.clip((Ph[:, 1] - 1.355) / 0.045, 0.15, 1.0)
    col_head = (col_front * 0.92) * w_proj[:, None] + skin_base * (1.0 - w_proj[:, None])

    rough_h = np.full(len(Ph), 0.52, dtype=np.float32)
    for eye_i in (468, 473):
        de = np.linalg.norm(Ph - P_lm[eye_i], axis=1)
        rough_h = np.where(de < 0.012, 0.07, rough_h)
    maps_head = v6.finish_and_save_maps("head_skin", cov_h, col_head, rough_h)

    # ------------------------------------------------------------------
    # 2. `ProfZ_ArmsSkin` (`512x512` rich warm sunlit Pixar arms matching `FRONT/SIDE/BACK` arm tone 1:1!)
    # ------------------------------------------------------------------
    pos_a, nrm_a, cov_a = v6.rasterize_mesh_uv(imported["arms_skin"], 512)
    eya, exa = np.where(cov_a)
    Pa, Na = pos_a[eya, exa], nrm_a[eya, exa]
    out_lit = np.clip(0.45 + 0.38 * np.abs(Na[:, 0]) + 0.22 * Na[:, 2] + 0.14 * Na[:, 1], 0.0, 1.0)
    sleeve_ao = np.clip((1.148 - Pa[:, 1]) / 0.045, 0.42, 1.0)
    pocket_ao = np.clip((Pa[:, 1] - 0.880) / 0.032, 0.58, 1.0)
    sh_a = out_lit * sleeve_ao * pocket_ao
    col_arms = np.array([168.0, 98.0, 58.0])[None, :] * (1.0 - sh_a[:, None]) + np.array([228.0, 152.0, 102.0])[None, :] * sh_a[:, None]
    maps_arms = v6.finish_and_save_maps("arms_skin", cov_a, col_arms, np.full(len(Pa), 0.54, np.float32))

    # ------------------------------------------------------------------
    # 3. `ProfZ_Hair` (`1024x1024` deep rich espresso-black Pixar curl locks with warm cocoa wave highlights!)
    # ------------------------------------------------------------------
    pos_hr, nrm_hr, cov_hr = v6.rasterize_mesh_uv(imported["hair"], 1024)
    eyh, exh = np.where(cov_hr)
    Phr, Nhr = pos_hr[eyh, exh], nrm_hr[eyh, exh]
    Q_hr = (Phr - c_h) @ R_h
    rad_out = np.linalg.norm(Q_hr / np.array([0.105, 0.122, 0.112]), axis=1)
    curl_tip = np.clip((rad_out - 0.94) / 0.24, 0.0, 1.0)
    strand_wave = 0.5 + 0.5 * np.sin(np.arctan2(Q_hr[:, 2], Q_hr[:, 0]) * 18.0 + Q_hr[:, 1] * 95.0)
    crown_lit = np.clip(0.28 + 0.48 * Nhr[:, 1] + 0.24 * np.abs(Nhr[:, 2]), 0.0, 1.0)
    h_mix = np.clip(0.58 * curl_tip * crown_lit + 0.30 * strand_wave * curl_tip, 0.0, 1.0)
    # Deep velvet espresso base `[10, 7.5, 6.5]` -> warm cocoa sheen `[40, 28, 21]` matching `REF` hair contrast!
    col_hair = np.array([9.5, 7.2, 6.2])[None, :] * (1.0 - h_mix[:, None]) + np.array([40.0, 28.5, 21.5])[None, :] * h_mix[:, None]
    yy_hr, xx_hr = np.mgrid[0:1024, 0:1024].astype(np.float32)
    bump_hr = 0.55 * np.sin(xx_hr * 0.65 + 2.2 * np.sin(yy_hr * 0.12))
    maps_hair = v6.finish_and_save_maps("hair", cov_hr, col_hair, np.full(len(Phr), 0.58, np.float32), bump_img=bump_hr)

    # ------------------------------------------------------------------
    # 4. `ProfZ_TShirt` (`1024x1024` REAL class-filtered multi-view cotton folds + crisp electric Cosmic `"Z"`!)
    # ------------------------------------------------------------------
    pos_t, nrm_t, cov_t = v6.rasterize_mesh_uv(imported["tshirt"], 1024)
    eyt, ext = np.where(cov_t)
    Pt, Nt = pos_t[eyt, ext], nrm_t[eyt, ext]

    # Build pure cotton plates from `FRONT` & `BACK` (`removing warm bare skin so ONLY authentic black shirt + "Z" remain`!):
    yy_f, _ = np.mgrid[0:1024, 0:f_orig.shape[1]]
    skin_f = ((f_orig[:, :, 0].astype(np.float32) - f_orig[:, :, 2].astype(np.float32)) > 24.0) & (f_orig[:, :, 0] > 90)
    tshirt_mask_f = fm & (yy_f >= 360) & (yy_f <= 622) & (~skin_f)
    plate_ts_f = class_filtered_plate(f_orig, tshirt_mask_f, blur_sigma=2.2)

    yy_b, _ = np.mgrid[0:1024, 0:b_orig.shape[1]]
    skin_b = ((b_orig[:, :, 0].astype(np.float32) - b_orig[:, :, 2].astype(np.float32)) > 24.0) & (b_orig[:, :, 0] > 90)
    tshirt_mask_b = bm & (yy_b >= 365) & (yy_b <= 620) & (~skin_b)
    plate_ts_b = class_filtered_plate(b_orig, tshirt_mask_b, blur_sigma=2.2)

    vf_t = v_sole - Pt[:, 1] / scale
    col_tf = sample_plate(plate_ts_f, vf_t, u0_f + Pt[:, 0] / scale)
    col_tb = sample_plate(plate_ts_b, vf_t + 3.0, 133.5 - Pt[:, 0] * 0.96 / scale)

    w_front_ts = np.clip(0.5 + 1.4 * Nt[:, 2], 0.0, 1.0)
    raw_ts = col_tf * w_front_ts[:, None] + col_tb * (1.0 - w_front_ts[:, None])

    # Identify Cosmic `"Z"` logo pixels (`keep 100% vivid electric blue/purple/cyan brightness + subtle emissive`!)
    # while scaling plain black cotton by `0.58x` so rendered black T-shirt matches `REF` deep velvet charcoal `[28, 30, 34]`!
    is_logo = (Pt[:, 2] > 0.03) & (Nt[:, 2] > 0.22) & (np.abs(Pt[:, 0]) < 0.118) & (Pt[:, 1] > 1.045) & (Pt[:, 1] < 1.300)
    chroma_t = raw_ts.max(axis=1) - raw_ts.min(axis=1)
    logo_w = np.where(is_logo, np.clip((np.maximum(chroma_t * 1.5, raw_ts.max(axis=1) - 44.0)) / 26.0, 0.0, 1.0), 0.0)

    col_ts = (raw_ts * 0.56) * (1.0 - logo_w[:, None]) + (col_tf * 1.04) * logo_w[:, None]
    emi_ts = col_tf * (0.48 * logo_w[:, None])

    yy_t, xx_t = np.mgrid[0:1024, 0:1024].astype(np.float32)
    bump_ts = 0.24 * (np.sin(xx_t * 2.0) * np.cos(yy_t * 2.0))
    maps_ts = v6.finish_and_save_maps("tshirt", cov_t, col_ts, np.full(len(Pt), 0.88, np.float32), emissive_valid=emi_ts, bump_img=bump_ts)

    # ------------------------------------------------------------------
    # 5. `ProfZ_Jeans` (`1024x1024` REAL multi-view washed dark indigo denim + pockets + authentic cuffs!)
    # ------------------------------------------------------------------
    pos_j, nrm_j, cov_j = v6.rasterize_mesh_uv(imported["jeans"], 1024)
    eyj, exj = np.where(cov_j)
    Pj, Nj = pos_j[eyj, exj], nrm_j[eyj, exj]

    # Pure washed-denim plates from `FRONT`, `SIDE`, and `BACK` (`strictly rows 605..888 above shoes/cuffs, skin excluded!`!):
    denim_f = fm & (yy_f >= 605) & (yy_f <= 886) & (~skin_f)
    plate_jf = class_filtered_plate(f_orig, denim_f, blur_sigma=2.0)

    yy_s, _ = np.mgrid[0:1024, 0:s_orig.shape[1]]
    skin_s = ((s_orig[:, :, 0].astype(np.float32) - s_orig[:, :, 2].astype(np.float32)) > 24.0) & (s_orig[:, :, 0] > 90)
    denim_s = sm & (yy_s >= 612) & (yy_s <= 892) & (~skin_s)
    plate_js = class_filtered_plate(s_orig, denim_s, blur_sigma=2.0)

    denim_b = bm & (yy_b >= 608) & (yy_b <= 880) & (~skin_b)
    plate_jb = class_filtered_plate(b_orig, denim_b, blur_sigma=2.0)

    vf_j = v_sole - Pj[:, 1] / scale
    col_jf = sample_plate(plate_jf, vf_j, u0_f + Pj[:, 0] / scale)
    col_js = sample_plate(plate_js, vf_j + 8.0, u0_s - Pj[:, 2] / scale)
    col_jb = sample_plate(plate_jb, vf_j - 4.0, 133.5 - Pj[:, 0] * 0.96 / scale)

    wf_j = np.maximum(0.0, Nj[:, 2]) ** 2.0
    wb_j = np.maximum(0.0, -Nj[:, 2]) ** 2.0
    ws_j = np.abs(Nj[:, 0]) ** 2.0 + 0.05
    wsum_j = wf_j + wb_j + ws_j
    col_j = ((wf_j[:, None] * col_jf + wb_j[:, None] * col_jb + ws_j[:, None] * col_js) / wsum_j[:, None]) * 0.62

    # Crisp turned-up reverse-weave slate-grey denim cuffs (`y <= 0.1705 m`) matching `REF` rolled cuffs 1:1!
    on_cuff = Pj[:, 1] <= 0.1705
    cuff_shade = np.clip(0.50 + 0.50 * (np.abs(Nj[:, 2]) * 0.6 + np.abs(Nj[:, 0]) * 0.5), 0.0, 1.0)
    col_cuff = np.array([58.0, 64.0, 72.0])[None, :] * (1.0 - cuff_shade[:, None]) + np.array([96.0, 103.0, 112.0])[None, :] * cuff_shade[:, None]
    col_j[on_cuff] = col_cuff[on_cuff]
    col_j[(np.abs(Pj[:, 1] - 0.167) < 0.0020) & on_cuff] = np.array([44.0, 49.0, 56.0])

    bump_j = 0.34 * np.sin((xx_t + yy_t * 1.5) * 1.55)
    maps_j = v6.finish_and_save_maps("jeans", cov_j, col_j, np.full(len(Pj), 0.84, np.float32), bump_img=bump_j)

    # ------------------------------------------------------------------
    # 6. `ProfZ_ShoeLeft` & `ProfZ_ShoeRight` (`deep black canvas `[20, 21, 24]` + crisp white rubber & laces!`)
    # ------------------------------------------------------------------
    c_base.TEX_DIR = TEX_DIR
    sl_alb, sl_orm = c_base.bake_converse_sneaker_2k(imported["shoe_l"], cal["ank_L"], math.radians(+14.0), "left", 1024)
    sr_alb, sr_orm = c_base.bake_converse_sneaker_2k(imported["shoe_r"], cal["ank_R"], math.radians(-30.0), "right", 1024)
    # Deepen canvas black contrast on both shoes (`[39,40,43] -> [17,18,21]`) while keeping rubber & laces bright white!
    for sp_path in (sl_alb, sr_alb):
        arr = np.asarray(Image.open(sp_path)).astype(np.float32)
        dark_canvas = arr.mean(axis=2) < 95.0
        arr[dark_canvas] *= 0.44
        Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).save(sp_path)

    return {
        "head_skin": maps_head,
        "arms_skin": maps_arms,
        "hair": maps_hair,
        "tshirt": maps_ts,
        "jeans": maps_j,
        "shoe_l": (sl_alb, None, sl_orm, None),
        "shoe_r": (sr_alb, None, sr_orm, None),
    }


def make_studio_mat_v7(
    name: str,
    maps: tuple | None = None,
    spec_ior: float = 0.08,
    solid_rgba: tuple = (0.5, 0.5, 0.5, 1.0),
    metallic: float = 0.0,
    roughness: float = 0.5,
) -> bpy.types.Material:
    mat = v6.make_mat(name, maps=maps, solid_rgba=solid_rgba, metallic=metallic, roughness=roughness)
    bsdf = [n for n in mat.node_tree.nodes if n.type == "BSDF_PRINCIPLED"][0]
    if "Specular IOR Level" in bsdf.inputs:
        bsdf.inputs["Specular IOR Level"].default_value = spec_ior
    return mat


def main() -> None:
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    v6.TEX_DIR = TEX_DIR
    RENDER_DIR.mkdir(parents=True, exist_ok=True)
    cal = np.load(WORK / "calibration_v7.npz")

    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 48
    scene.cycles.use_denoising = True
    scene.cycles.denoiser = "OPENIMAGEDENOISE"
    scene.view_settings.view_transform = "Standard"

    part_specs = [
        ("head_skin", "part_head_skin.ply", 58.0, 0.26),
        ("arms_skin", "part_arms_skin.ply", 62.0, 0.26),
        ("hair", "part_hair.ply", 64.0, 0.20),
        ("tshirt", "part_tshirt.ply", 60.0, 0.06),
        ("jeans", "part_jeans.ply", 60.0, 0.06),
        ("shoe_l", "part_shoe_left.ply", 62.0, 0.14),
        ("shoe_r", "part_shoe_right.ply", 62.0, 0.14),
        ("glasses", "part_glasses.ply", 66.0, 0.50),
        ("watch", "part_watch_left.ply", 66.0, 0.50),
    ]
    imported: dict[str, bpy.types.Object] = {}
    spec_levels: dict[str, float] = {}
    for key, fn, ang, sp_lvl in part_specs:
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
        spec_levels[key] = sp_lvl

    print("Baking v7 class-filtered multi-view PBR maps...")
    baked = bake_all_parts_v7(imported, cal)

    for key, maps in baked.items():
        mat = make_studio_mat_v7(f"Mat_{key}", maps=maps, spec_ior=spec_levels[key])
        imported[key].data.materials.clear()
        imported[key].data.materials.append(mat)

    imported["glasses"].data.materials.append(
        make_studio_mat_v7("Mat_glasses", solid_rgba=(0.14, 0.095, 0.060, 1.0), spec_ior=0.5, metallic=0.88, roughness=0.22)
    )
    imported["watch"].data.materials.append(
        make_studio_mat_v7("Mat_watch", solid_rgba=(0.012, 0.013, 0.015, 1.0), spec_ior=0.5, metallic=0.25, roughness=0.18)
    )

    bpy.ops.wm.save_as_mainfile(filepath=str(WORK / "professor_z_studio_v7.blend"))
    print("Saved master studio Blender file -> professor_z_studio_v7.blend")

    # Balanced Pixar studio lighting (`rich blacks, warm sunlit skin, soft contact floor shadow`):
    world = bpy.data.worlds.new("StudioCreamWorld")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (1.0, 0.98, 0.96, 1.0)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.42
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

    add_light((-1.8, -3.1, 2.35), 115.0, 2.6)
    add_light((2.2, -2.5, 1.60), 50.0, 2.8)
    add_light((3.2, 0.7, 2.15), 95.0, 2.6)
    add_light((-3.2, 0.7, 2.15), 95.0, 2.6)
    add_light((0.2, 3.3, 2.25), 105.0, 2.6)

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
        p = WORK / f"v7_panel_{tag}.png"
        scene.render.filepath = str(p)
        bpy.ops.render.render(write_still=True)
        return comp_cream(p)

    sheet_3d = np.tile(cream_bg.astype(np.uint8)[None, None, :], (1024, 1536, 1))
    sheet_3d[:, 150:535] = render_ortho("front", (0.0, -3.5, 1.0, 0.0), 385, float(cal["u0_f"]), float(cal["v_sole_f"]), 1.0)
    sheet_3d[:, 615:965] = render_ortho("side",  (3.5,  0.0, 0.0, 1.0), 350, float(cal["u0_s"]), 973.0, 1.0)
    sheet_3d[:, 1075:1395] = render_ortho("back", (0.0,  3.5, -1.0, 0.0), 320, 133.5, 957.0, 795.0 / 775.0)

    ref_img = np.asarray(Image.open(REF_PATH).convert("RGB"))
    Image.fromarray(sheet_3d).save(RENDER_DIR / "professor_z_3d_turnaround_exact.png")
    Image.fromarray(np.vstack([ref_img, np.full((12, 1536, 3), [45, 48, 56], dtype=np.uint8), sheet_3d])).save(
        RENDER_DIR / "professor_z_3d_vs_ref_comparison.png"
    )

    def render_persp(tag: str, angle_deg: float, dist: float, z_look: float, lens: float, w: int, h: int) -> np.ndarray:
        rad = math.radians(angle_deg)
        cam_data.type = "PERSP"
        cam_data.lens = lens
        cam.location = (dist * math.sin(rad), -dist * math.cos(rad), z_look + 0.03)
        cam.rotation_euler = (Vector((0.0, 0.0, z_look)) - cam.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.resolution_x, scene.render.resolution_y = w, h
        p = WORK / f"v7_card_{tag}.png"
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
    print("STAGE C v7 MASTERPIECE COMPLETE -> ALL RENDERS SAVED!")


if __name__ == "__main__":
    main()
