#!/usr/bin/env python3
"""
Stage 6: Headless Cycles + OpenImageDenoise Neutral Review, Cinematic Lighting,
and Direct Original-Reference-vs-3D Comparison Renderer (`render_review.py`).
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys
from typing import Any, Dict, List, Tuple

import bpy
from mathutils import Euler, Vector
from PIL import Image, ImageDraw, ImageOps


def setup_studio_lighting_and_cycles(cfg: Dict[str, Any]) -> Dict[str, bpy.types.Object]:
    scn = bpy.context.scene
    scn.render.engine = "CYCLES"
    scn.cycles.device = "CPU"
    scn.cycles.use_denoising = True
    scn.cycles.denoiser = "OPENIMAGEDENOISE"
    scn.cycles.max_bounces = 4
    scn.cycles.diffuse_bounces = 2
    scn.cycles.glossy_bounces = 2
    scn.render.image_settings.file_format = "PNG"
    scn.render.image_settings.color_mode = "RGBA"
    # Filmic / AgX prevents highlight clipping on cheekbones, nose bridge, and ivory shirt!
    try:
        scn.view_settings.view_transform = "AgX"
    except Exception:
        scn.view_settings.view_transform = "Filmic"
    scn.view_settings.look = "None"
    scn.view_settings.exposure = 0.0

    world = bpy.data.worlds.new("StudioWorld")
    scn.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (0.042, 0.052, 0.072, 1.0)
        bg.inputs["Strength"].default_value = 0.55

    bpy.ops.mesh.primitive_cylinder_add(vertices=64, radius=2.2, depth=0.04, location=(0.0, 0.0, -0.02))
    floor = bpy.context.active_object
    floor.name = "QA_StudioFloor"
    fmat = bpy.data.materials.new("Mat_StudioFloor")
    fmat.use_nodes = True
    bsdf = fmat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (0.115, 0.132, 0.162, 1.0)
    bsdf.inputs["Roughness"].default_value = 0.78
    floor.data.materials.append(fmat)

    def add_area(name: str, loc: Tuple[float, float, float], energy: float, size: float, col: Tuple[float, float, float]):
        ld = bpy.data.lights.new(name=name, type="AREA")
        ld.energy = energy
        ld.size = size
        ld.color = col
        lo = bpy.data.objects.new(name, ld)
        scn.collection.objects.link(lo)
        lo.location = Vector(loc)
        direction = Vector((0.0, 0.0, 1.32)) - lo.location
        lo.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
        return lo

    # Calibrated Neutral 3-Point Review Rig (zero blowout, crisp 3D facial & cloth form)
    lights = {
        "key":  add_area("KeyLight",  (-1.55, -2.05, 2.25), 72.0, 1.55, (1.00, 0.97, 0.92)),
        "fill": add_area("FillLight", ( 1.75, -1.85, 1.55), 26.0, 2.20, (0.88, 0.93, 1.00)),
        "rim":  add_area("RimLight",  ( 0.45,  2.15, 2.30), 54.0, 1.35, (1.00, 0.94, 0.86)),
    }
    return lights


def create_aimed_camera(name: str, loc: Tuple[float, float, float], target: Tuple[float, float, float], lens_mm: float = 65.0) -> bpy.types.Object:
    cd = bpy.data.cameras.new(name)
    cd.lens = lens_mm
    co = bpy.data.objects.new(name, cd)
    bpy.context.scene.collection.objects.link(co)
    co.location = Vector(loc)
    co.rotation_euler = (Vector(target) - co.location).to_track_quat("-Z", "Y").to_euler()
    return co


def render_to(cam: bpy.types.Object, out_path: pathlib.Path, res: Tuple[int, int], samples: int) -> str:
    scn = bpy.context.scene
    scn.camera = cam
    scn.render.resolution_x, scn.render.resolution_y = res
    scn.cycles.samples = samples
    out_path.parent.mkdir(parents=True, exist_ok=True)
    scn.render.filepath = str(out_path.resolve())
    bpy.ops.render.render(write_still=True)
    return str(out_path)


def activate_nla_clip(rig: bpy.types.Object, head_obj: bpy.types.Object, clip_name: str | None, frame: int = 1) -> None:
    for pb in rig.pose.bones:
        pb.location = (0, 0, 0)
        pb.rotation_euler = (0, 0, 0)
        pb.scale = (1, 1, 1)
    for kb in head_obj.data.shape_keys.key_blocks[1:]:
        kb.value = 0.0

    for anim_holder in (rig.animation_data, head_obj.data.shape_keys.animation_data):
        if anim_holder:
            for tr in anim_holder.nla_tracks:
                tr.mute = (tr.name != clip_name)
    bpy.context.scene.frame_set(frame)
    bpy.context.view_layer.update()


def compose_grid(tiles: List[Tuple[str, str]], cols: int, tile_px: int, out_path: pathlib.Path) -> str:
    rows = math.ceil(len(tiles) / float(cols))
    canvas = Image.new("RGB", (cols * tile_px, rows * tile_px), (28, 34, 46))
    draw = ImageDraw.Draw(canvas)
    for idx, (label, p) in enumerate(tiles):
        r, c = divmod(idx, cols)
        x0, y0 = c * tile_px, r * tile_px
        im = Image.open(p).convert("RGB").resize((tile_px, tile_px), Image.Resampling.LANCZOS)
        canvas.paste(im, (x0, y0))
        draw.rectangle((x0 + 8, y0 + 8, x0 + 235, y0 + 32), fill=(12, 16, 22))
        draw.text((x0 + 14, y0 + 14), label, fill=(238, 242, 248))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out_path)
    return str(out_path)


def compose_reference_vs_3d_comparison(
    views_dir: pathlib.Path,
    renders_map: Dict[str, str],
    out_path: pathlib.Path,
) -> str:
    """Compose direct side-by-side Original Reference Views (Row 1) vs Neutral 3D Reconstruction Views (Row 2)."""
    cols = [
        ("Hero / Front Portrait", views_dir / "hero_pose.png", renders_map["portrait_front"]),
        ("Turnaround Front (0 deg)", views_dir / "turnaround_front.png", renders_map["full_front"]),
        ("Turnaround 3/4 (45 deg)", views_dir / "turnaround_three_quarter.png", renders_map["full_3q"]),
        ("Turnaround Side (90 deg)", views_dir / "turnaround_side.png", renders_map["full_side"]),
        ("Turnaround Back (180 deg)", views_dir / "turnaround_rear.png", renders_map["full_rear"]),
    ]
    cw, ch = 320, 440
    canvas = Image.new("RGB", (cw * len(cols), ch * 2), (24, 29, 38))
    draw = ImageDraw.Draw(canvas)
    for ci, (title, ref_p, ren_p) in enumerate(cols):
        for ri, (tag, pth, bg_col) in enumerate((
            (f"REF: {title}", ref_p, (242, 237, 230)),
            (f"3D:  {title}", pathlib.Path(ren_p), (28, 34, 46)),
        )):
            x0, y0 = ci * cw, ri * ch
            if pth.is_file():
                im = ImageOps.pad(Image.open(pth).convert("RGB"), (cw - 8, ch - 38), color=bg_col)
                canvas.paste(im, (x0 + 4, y0 + 34))
            draw.rectangle((x0 + 4, y0 + 4, x0 + cw - 4, y0 + 30), fill=(14, 18, 26))
            draw.text((x0 + 10, y0 + 11), tag, fill=(240, 244, 250))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out_path)
    return str(out_path)


def run_review_renders(in_blend: pathlib.Path, out_dir: pathlib.Path, cfg: Dict[str, Any]) -> Dict[str, Any]:
    bpy.ops.wm.open_mainfile(filepath=str(in_blend.resolve()))
    anc = json.loads(bpy.context.scene["avatar_anchors_json"])
    rig = bpy.data.objects["AvatarRig"]
    head_obj = bpy.data.objects["Avatar_Head"]

    lights = setup_studio_lighting_and_cycles(cfg)
    rcfg = cfg["render"]
    pres = tuple(rcfg["portrait_res"])
    fres = tuple(rcfg["fullbody_res"])
    tres = tuple(rcfg["tile_res"])
    psmp = int(rcfg["portrait_samples"])
    fsmp = int(rcfg["fullbody_samples"])
    tsmp = int(rcfg["tile_samples"])

    H = anc["height_m"]
    head_mid_z = anc["chin_z"] + anc["head_h"] * 0.56
    tiles_dir = out_dir / "_tiles"

    cam_port_front = create_aimed_camera("Cam_PortraitFront", (0.0,  -1.08, head_mid_z + 0.015), (0.0, 0.0, head_mid_z - 0.012), 74.0)
    cam_port_3q    = create_aimed_camera("Cam_Portrait3Q",    (0.62, -0.92, head_mid_z + 0.025), (0.0, 0.0, head_mid_z - 0.012), 74.0)
    cam_port_side  = create_aimed_camera("Cam_PortraitSide",  (1.08, -0.04, head_mid_z + 0.015), (0.0, 0.0, head_mid_z - 0.012), 74.0)
    cam_fb_front   = create_aimed_camera("Cam_FullFront",     (0.0,  -3.55, H * 0.53), (0.0, 0.0, H * 0.505), 58.0)
    cam_fb_3q      = create_aimed_camera("Cam_Full3Q",        (2.35, -2.65, H * 0.53), (0.0, 0.0, H * 0.505), 58.0)
    cam_fb_side    = create_aimed_camera("Cam_FullSide",      (3.55,  0.0,  H * 0.53), (0.0, 0.0, H * 0.505), 58.0)
    cam_fb_rear    = create_aimed_camera("Cam_FullRear",      (0.0,  +3.55, H * 0.53), (0.0, 0.0, H * 0.505), 58.0)

    # Neutral Rest & Turnaround Views (`01..05` + `10_side_profile_portrait.png`)
    activate_nla_clip(rig, head_obj, "Idle_Breathing", frame=1)
    p01 = render_to(cam_port_front, out_dir / "01_portrait_front.png", pres, psmp)
    p02 = render_to(cam_port_3q,    out_dir / "02_portrait_three_quarter.png", pres, psmp)
    p10 = render_to(cam_port_side,  out_dir / "10_side_profile_portrait.png", pres, psmp)

    activate_nla_clip(rig, head_obj, None, frame=1)
    p03 = render_to(cam_fb_front, out_dir / "03_fullbody_front.png", fres, fsmp)
    p_fb3q = render_to(cam_fb_3q, tiles_dir / "fullbody_3q.png", fres, fsmp)
    p04 = render_to(cam_fb_side,  out_dir / "04_fullbody_side.png", fres, fsmp)
    p05 = render_to(cam_fb_rear,  out_dir / "05_fullbody_rear.png", fres, fsmp)

    # 06: 9-Panel Facial Expression & Viseme QA Sheet
    expr_specs = [
        ("1. Neutral Rest",       "Idle_Breathing",       1),
        ("2. Excited / Happy",    "Emote_Happy",          24),
        ("3. Surprised",          "Emote_Surprised",      20),
        ("4. Wink + Tongue Out",  "ProfZ_TongueOutWink",  24),
        ("5. Thinking Eye Gaze",  "Emote_Thinking",       26),
        ("6. Sleepy Stargazer",   "ProfZ_Sleepy",         30),
        ("7. Full Natural Blink", "Blink_Natural",        15),
        ("8. Viseme AA (JawOpen)","__VISEME_AA__",         1),
        ("9. Frown /Concerned",   "__FROWN__",             1),
    ]
    expr_tiles = []
    for idx, (label, cname, frm) in enumerate(expr_specs):
        if cname == "__VISEME_AA__":
            activate_nla_clip(rig, head_obj, None, 1)
            head_obj.data.shape_keys.key_blocks["viseme_aa"].value = 0.90
            head_obj.data.shape_keys.key_blocks["jawOpen"].value = 0.68
            bpy.context.view_layer.update()
        elif cname == "__FROWN__":
            activate_nla_clip(rig, head_obj, None, 1)
            head_obj.data.shape_keys.key_blocks["mouthFrownLeft"].value = 0.85
            head_obj.data.shape_keys.key_blocks["mouthFrownRight"].value = 0.85
            head_obj.data.shape_keys.key_blocks["browInnerUp"].value = 0.72
            bpy.context.view_layer.update()
        else:
            activate_nla_clip(rig, head_obj, cname, frm)
        tp = render_to(cam_port_front, tiles_dir / f"expr_{idx:02d}.png", tres, tsmp)
        expr_tiles.append((label, tp))
    p06 = compose_grid(expr_tiles, cols=3, tile_px=448, out_path=out_dir / "06_expression_sheet.png")

    # 07: 4-Panel Body Deformation & Signature Pose Sheet
    cam_pose = create_aimed_camera("Cam_Pose3Q", (1.25, -3.45, H * 0.56), (0.0, 0.0, H * 0.50), 56.0)
    pose_specs = [
        ("1. Hero Glasses Adjust", "ProfZ_HeroGlassesAdjust", 30, cam_fb_front),
        ("2. Disco Groove Point",  "ProfZ_DiscoGroove",       20, cam_fb_front),
        ("3. Walk Cycle Stride",   "Walk_Cycle",               9, cam_pose),
        ("4. Deep Squat Stress",   "__STRESS_SQUAT__",         1, cam_pose),
    ]
    pose_tiles = []
    for idx, (label, cname, frm, pcam) in enumerate(pose_specs):
        if cname == "__STRESS_SQUAT__":
            activate_nla_clip(rig, head_obj, None, 1)
            rig.pose.bones["Hips"].location = (0.0, -0.06, -0.265)
            rig.pose.bones["Hips"].rotation_euler = Euler((0.0, 0.0, math.radians(-12.0)), "XYZ")
            rig.pose.bones["Thigh_L"].rotation_euler = Euler((math.radians(-48.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["Thigh_R"].rotation_euler = Euler((math.radians(-48.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["Calf_L"].rotation_euler = Euler((math.radians(52.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["Calf_R"].rotation_euler = Euler((math.radians(52.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["Foot_L"].rotation_euler = Euler((math.radians(-4.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["Foot_R"].rotation_euler = Euler((math.radians(-4.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["UpperArm_L"].rotation_euler = Euler((math.radians(-45.0), 0.0, math.radians(-12.0)), "XYZ")
            rig.pose.bones["UpperArm_R"].rotation_euler = Euler((math.radians(-45.0), 0.0, math.radians(12.0)), "XYZ")
            rig.pose.bones["Forearm_L"].rotation_euler = Euler((math.radians(-45.0), 0.0, 0.0), "XYZ")
            rig.pose.bones["Forearm_R"].rotation_euler = Euler((math.radians(-45.0), 0.0, 0.0), "XYZ")
            head_obj.data.shape_keys.key_blocks["mouthSmileLeft"].value = 0.70
            head_obj.data.shape_keys.key_blocks["mouthSmileRight"].value = 0.70
            head_obj.data.shape_keys.key_blocks["jawOpen"].value = 0.30
            bpy.context.view_layer.update()
        else:
            activate_nla_clip(rig, head_obj, cname, frm)
        tp = render_to(pcam, tiles_dir / f"pose_{idx:02d}.png", (512, 512), tsmp)
        pose_tiles.append((label, tp))
    p07 = compose_grid(pose_tiles, cols=2, tile_px=512, out_path=out_dir / "07_body_deformation_sheet.png")

    # 08: 360 Turntable & Lip-Sync Frame Strip
    turn_tiles = []
    activate_nla_clip(rig, head_obj, None, 1)
    for idx, deg in enumerate((0, 45, 135, 225)):
        rig.rotation_euler = Euler((0.0, 0.0, math.radians(deg)), "XYZ")
        bpy.context.view_layer.update()
        tp = render_to(cam_fb_front, tiles_dir / f"turn_{idx:02d}.png", (384, 384), 24)
        turn_tiles.append((f"Turntable {deg} deg", tp))
    rig.rotation_euler = Euler((0.0, 0.0, 0.0), "XYZ")

    cam_lips = create_aimed_camera("Cam_Lips", (0.0, -0.82, head_mid_z + 0.01), (0.0, 0.0, head_mid_z - 0.012), 78.0)
    for idx, sfrm in enumerate((5, 13, 21, 35)):
        activate_nla_clip(rig, head_obj, "LipSync_Speech", sfrm)
        tp = render_to(cam_lips, tiles_dir / f"speech_{idx:02d}.png", (384, 384), 24)
        turn_tiles.append((f"LipSync f{sfrm:02d}", tp))
    p08 = compose_grid(turn_tiles, cols=4, tile_px=384, out_path=out_dir / "08_turntable_and_frames.png")

    # 09: 2x2 Material & PBR Texture Inspection Sheet
    tex_dir = in_blend.parent / "textures"
    mat_tiles = [
        ("Head Skin PBR Albedo",         str(tex_dir / "head_skin_albedo.png")),
        ("Procedural Iris + Catchlight", str(tex_dir / "eye_iris_albedo.png")),
        ("Garment 2x2 Albedo Atlas",     str(tex_dir / "garment_albedo.png")),
        ("Garment Tangent Normal Map",   str(tex_dir / "garment_normal.png")),
    ]
    p09 = compose_grid(mat_tiles, cols=2, tile_px=256, out_path=out_dir / "09_material_preview.png")

    # 11: Cinematic Stargazer Lighting Portrait (ONLY after neutral review renders complete!)
    lights["key"].data.color = (1.00, 0.86, 0.66)
    lights["key"].data.energy = 86.0
    lights["fill"].data.color = (0.42, 0.58, 1.00)
    lights["fill"].data.energy = 28.0
    lights["rim"].data.color = (1.00, 0.80, 0.48)
    lights["rim"].data.energy = 98.0
    bg = bpy.context.scene.world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (0.015, 0.022, 0.048, 1.0)
        bg.inputs["Strength"].default_value = 0.45
    activate_nla_clip(rig, head_obj, "ProfZ_HeroGlassesAdjust", frame=30)
    cam_cine = create_aimed_camera("Cam_CinematicHero", (0.38, -1.28, head_mid_z - 0.03), (0.0, 0.0, head_mid_z - 0.06), 70.0)
    p11 = render_to(cam_cine, out_dir / "11_cinematic_hero_portrait.png", pres, psmp + 8)

    # 12: Direct Side-by-Side Original Supplied Reference vs Neutral 3D Reconstruction Plate
    views_dir = in_blend.parent / "sheet_views"
    extra = [p10, p11]
    if (views_dir / "turnaround_front.png").is_file():
        p12 = compose_reference_vs_3d_comparison(
            views_dir,
            {
                "portrait_front": p01,
                "full_front": p03,
                "full_3q": p_fb3q,
                "full_side": p04,
                "full_rear": p05,
            },
            out_dir / "12_reference_vs_3d_turnaround.png",
        )
        extra.append(p12)

    return {"status": "renders_complete", "sheets": [p01, p02, p03, p04, p05, p06, p07, p08, p09], "extra_plates": extra}


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--in-blend", required=True)
    ap.add_argument("--out-dir", required=True)
    ap.add_argument("--config", required=True)
    args = ap.parse_args(argv)

    cfg = json.loads(pathlib.Path(args.config).read_text())
    print(json.dumps(run_review_renders(pathlib.Path(args.in_blend), pathlib.Path(args.out_dir), cfg), indent=2))


if __name__ == "__main__":
    main()
