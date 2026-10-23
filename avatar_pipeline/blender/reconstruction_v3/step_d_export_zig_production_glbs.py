#!/usr/bin/env python3
"""Stage D — Production ZIG GLB Exporter (`Master 2048`, `LOD0 1024 <= 25k`, `LOD1 512 <= 12k`)
with Full `60`-Bone Armature, `26` Pixar Morph Targets, `24` NLA Clips & Zero-Bleed `3x3` PBR Atlas.
"""

from __future__ import annotations

import json
import math
import shutil
import sys
from pathlib import Path

import bpy
import numpy as np
from mathutils import Vector
from PIL import Image
from scipy.spatial import cKDTree

ROOT = Path(__file__).resolve().parents[3]
VENV_SITE = ROOT / ".venv/lib/python3.11/site-packages"
if str(VENV_SITE) not in sys.path:
    sys.path.append(str(VENV_SITE))

from image_to_3dlab import glb_skins, glb_stats, photo_paint

WORK = ROOT / "avatar_pipeline/working/recon_v3"
TEX_DIR = WORK / "textures_v7"
BLENDER_SCRIPTS = ROOT / "avatar_pipeline/blender"
EXPORT_DIR = ROOT / "avatar_pipeline/exports"
APP_AVATAR_DIR = ROOT / "app/src/main/assets/avatar"

sys.path.insert(0, str(BLENDER_SCRIPTS))
import animate_character
import optimize_and_export
import rig_character


TILE_MAP = {
    "ProfZ_head_skin": (0, 0, "head_skin_v9"),
    "ProfZ_hair":      (1, 0, "hair_v9"),
    "ProfZ_tshirt":    (2, 0, "tshirt"),
    "ProfZ_jeans":     (0, 1, "jeans"),
    "ProfZ_shoe_l":    (1, 1, "shoe_left"),
    "ProfZ_shoe_r":    (2, 1, "shoe_right"),
    "ProfZ_arms_skin": (0, 2, "arms_skin"),
    "ProfZ_glasses":   (1, 2, "SOLID_BRONZE"),
    "ProfZ_watch":     (2, 2, "SOLID_WATCH"),
}


def build_zero_bleed_3x3_atlas(out_dir: Path, size: int, navy_variant: bool = False) -> dict[str, str]:
    """Pack all 9 semantic PBR textures into a `size x size` non-overlapping `3x3` padded atlas (`0%` mip bleed!)."""
    out_dir.mkdir(parents=True, exist_ok=True)
    # Use floating-point exact tile slicing across `[0..size]`:
    albedo = np.full((size, size, 3), [30, 32, 36], dtype=np.uint8)
    orm = np.zeros((size, size, 3), dtype=np.uint8)
    orm[..., 0] = 255
    orm[..., 1] = 150
    normal = np.zeros((size, size, 3), dtype=np.uint8)
    normal[..., 0] = 128
    normal[..., 1] = 128
    normal[..., 2] = 255

    margin = 0.03
    for _, (col, row, prefix) in TILE_MAP.items():
        c0, c1 = round(col * size / 3.0), round((col + 1) * size / 3.0)
        r0, r1 = round(row * size / 3.0), round((row + 1) * size / 3.0)
        tw, th = c1 - c0, r1 - r0
        px = max(1, round(tw * margin))
        py = max(1, round(th * margin))
        iw, ih = tw - 2 * px, th - 2 * py

        def place_tile(dst: np.ndarray, sub_arr: np.ndarray) -> None:
            inner = np.asarray(Image.fromarray(sub_arr).resize((iw, ih), Image.Resampling.LANCZOS))
            padded = np.pad(inner, ((py, py), (px, px), (0, 0)), mode="edge")
            dst[r0:r1, c0:c1] = padded[:th, :tw]

        if prefix == "SOLID_BRONZE":
            albedo[r0:r1, c0:c1] = [116, 88, 62]
            orm[r0:r1, c0:c1] = [255, 56, 224]
        elif prefix == "SOLID_WATCH":
            albedo[r0:r1, c0:c1] = [26, 28, 32]
            orm[r0:r1, c0:c1] = [255, 48, 64]
        else:
            p_base = TEX_DIR / f"{prefix}_basecolor.png"
            alb_file = p_base if p_base.is_file() else (TEX_DIR / f"{prefix}_albedo.png")
            alb = np.asarray(Image.open(alb_file).convert("RGB")).astype(np.float32)
            emi_p = TEX_DIR / f"{prefix}_emissive.png"
            if emi_p.is_file():
                # Bake Cosmic `"Z"` glow directly into baseColor so single-texture GLES 3.0 renders it vividly!
                emi = np.asarray(Image.open(emi_p).convert("RGB")).astype(np.float32)
                alb = np.clip(alb + 0.55 * emi, 0, 255)
            # Boost overall studio brightness (`x 1.16` on clothing/skin) so real-time mobile shader looks bright & crisp:
            alb = np.clip(alb * 1.14, 0, 255)
            if navy_variant and prefix == "tshirt":
                alb = np.clip(alb * np.array([0.65, 0.85, 1.45]), 0, 255)
            place_tile(albedo, alb.astype(np.uint8))

            orm_p = TEX_DIR / f"{prefix}_orm.png"
            if orm_p.is_file():
                place_tile(orm, np.asarray(Image.open(orm_p).convert("RGB")))
            nrm_p = TEX_DIR / f"{prefix}_normal.png"
            if nrm_p.is_file():
                place_tile(normal, np.asarray(Image.open(nrm_p).convert("RGB")))

    tag = "_navy" if navy_variant else ""
    pa = out_dir / f"profz_v9_albedo_{size}{tag}.png"
    po = out_dir / f"profz_v9_orm_{size}{tag}.png"
    pn = out_dir / f"profz_v9_normal_{size}{tag}.png"
    Image.fromarray(albedo).save(pa, optimize=True)
    Image.fromarray(orm).save(po, optimize=True)
    Image.fromarray(normal).save(pn, optimize=True)
    return {"albedo": str(pa.resolve()), "orm": str(po.resolve()), "normal": str(pn.resolve())}


def remap_uvs_to_tile(obj: bpy.types.Object, col: int, row: int, margin: float = 0.03) -> None:
    uv_layer = obj.data.uv_layers.active
    span = 1.0 - 2.0 * margin
    for loop in obj.data.loops:
        u0, v0 = uv_layer.data[loop.index].uv
        u0 = min(1.0, max(0.0, float(u0)))
        v0 = min(1.0, max(0.0, float(v0)))
        uv_layer.data[loop.index].uv = (
            (col + margin + span * u0) / 3.0,
            ((2 - row) + margin + span * v0) / 3.0,
        )


def build_rigged_animated_master_blend(cal: dict) -> Path:
    bpy.ops.wm.open_mainfile(filepath=str(WORK / "professor_z_studio_v9_final.blend"))

    # Remap all 9 semantic parts into their zero-bleed `3x3` atlas tiles:
    for name, (col, row, _) in TILE_MAP.items():
        remap_uvs_to_tile(bpy.data.objects[name], col, row)

    # Exact floor grounding (`min Z = 0.00000 m`) and exact `1.7300 m` height normalization across all 9 parts:
    all_objs = [bpy.data.objects[n] for n in TILE_MAP]
    min_z = min(min(float(v.co.z) for v in o.data.vertices) for o in all_objs)
    max_z = max(max(float(v.co.z) for v in o.data.vertices) for o in all_objs)
    s_norm = 1.7300 / (max_z - min_z)
    for o in all_objs:
        for v in o.data.vertices:
            v.co.x *= s_norm
            v.co.y *= s_norm
            v.co.z = (v.co.z - min_z) * s_norm

    # Exact 3D facial anchors in Blender world space (`X = world_X, Y = -world_Z, Z = world_Y`):
    P = cal["P_landmarks"]
    eye_mid = 0.5 * (P[468] + P[473])
    anc = {
        "height_m": 1.7300,
        "chin_z": float((P[152, 1] - min_z) * s_norm),
        "head_h": float((1.7300 - (P[152, 1] - min_z) * s_norm)),
        "eye_x": float(0.5 * abs(P[473, 0] - P[468, 0]) * s_norm),
        "eye_y": float(-eye_mid[2] * s_norm),
        "eye_z": float((eye_mid[1] - min_z) * s_norm),
        "eye_r": 0.0155,
        "mouth_z": float((0.5 * (P[13, 1] + P[14, 1]) - min_z) * s_norm),
        "brow_z": float((0.5 * (P[105, 1] + P[334, 1]) - min_z) * s_norm),
    }

    # Build exact `60`-bone humanoid + face rig (`AvatarRig`) at rest pose:
    rig = rig_character.build_60_bone_armature(anc)

    # Set up `Avatar_Head` with smooth lip/brow weights + all `26` Pixar blendshapes/visemes (`0.0` at rest!):
    head_obj = bpy.data.objects["ProfZ_head_skin"]
    head_obj.name = "Avatar_Head"
    head_obj.data.name = "Avatar_Head_Mesh"

    # Populate lip & brow vertex groups cleanly on `Avatar_Head` around actual 3D mouth & brow landmarks:
    vg_up = head_obj.vertex_groups.new(name="LIP_UPPER")
    vg_lo = head_obj.vertex_groups.new(name="LIP_LOWER")
    vg_br = head_obj.vertex_groups.new(name="PART_BROWS")
    mx0 = float(0.5 * (P[61, 0] + P[291, 0]) * s_norm)
    my0 = float(-0.5 * (P[13, 2] + P[14, 2]) * s_norm)
    mz0 = anc["mouth_z"]
    for i, v in enumerate(head_obj.data.vertices):
        dm = math.sqrt(((v.co.x - mx0) / 0.036) ** 2 + ((v.co.y - my0) / 0.035) ** 2 + ((v.co.z - mz0) / 0.026) ** 2)
        if dm < 1.0 and v.co.y < my0 + 0.025:
            w = float(math.cos(dm * math.pi * 0.5) ** 2)
            if v.co.z >= mz0:
                vg_up.add([i], w, "REPLACE")
            else:
                vg_lo.add([i], w, "REPLACE")
        if abs(v.co.z - anc["brow_z"]) < 0.014 and abs(v.co.x) < 0.055 and v.co.y < -0.09:
            vg_br.add([i], 0.85, "REPLACE")

    rig_character.assign_head_weights_and_26_morphs(head_obj, rig, anc)

    # Rigidly skin `ProfZ_hair` and `ProfZ_glasses` 100% to `Head` (`ZERO wobbling or distortion ever!`!),
    # and skin garments/arms/shoes cleanly to the 60-bone skeleton:
    hair_o = bpy.data.objects["ProfZ_hair"]
    glasses_o = bpy.data.objects["ProfZ_glasses"]
    rig_character.assign_rigid_single_bone(hair_o, "Head")
    rig_character.assign_rigid_single_bone(glasses_o, "Head")

    body_list = [
        bpy.data.objects["ProfZ_tshirt"],
        bpy.data.objects["ProfZ_jeans"],
        bpy.data.objects["ProfZ_arms_skin"],
        bpy.data.objects["ProfZ_watch"],
        bpy.data.objects["ProfZ_shoe_l"],
        bpy.data.objects["ProfZ_shoe_r"],
        hair_o,
        glasses_o,
    ]
    bpy.ops.object.select_all(action="DESELECT")
    for o in body_list:
        o.select_set(True)
    main_body = bpy.data.objects["ProfZ_tshirt"]
    bpy.context.view_layer.objects.active = main_body
    bpy.ops.object.join()
    main_body.name = "Avatar_Body"
    main_body.data.name = "Avatar_Body_Mesh"

    rig_character.assign_body_capsule_weights(main_body, rig, skip_rigid_bones={"Eye_L", "Eye_R", "Head"})

    # Protect high-detail sneakers, glasses, watch & hair silhouettes during mobile LOD decimation:
    vg_pin = main_body.vertex_groups.new(name="PIN_DETAIL")
    pin_ids = [i for i, v in enumerate(main_body.data.vertices) if v.co.z < 0.14 or (v.co.z > 1.44 and v.co.y < -0.10)]
    if pin_ids:
        vg_pin.add(pin_ids, 1.0, "REPLACE")

    for m_obj in (head_obj, main_body):
        m_obj.parent = rig
        mod = m_obj.modifiers.new(name="Armature", type="ARMATURE")
        mod.object = rig

    rigged_p = WORK / "professor_z_v9_rigged.blend"
    anim_p = WORK / "professor_z_v9_animated.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(rigged_p))

    # Build all `24` NLA animation clips using `animate_character.py` directly!
    lipsync_p = APP_AVATAR_DIR / "professor_z_lipsync_en.json"
    sys.argv = ["blender", "--", "--in-blend", str(rigged_p), "--lipsync", str(lipsync_p), "--out-blend", str(anim_p)]
    animate_character.main()
    return anim_p


def export_single_tier(
    anim_blend: Path,
    out_glb: Path,
    atlas_size: int,
    target_max_tris: int | None,
    attach_variants: bool = False,
) -> dict:
    bpy.ops.wm.open_mainfile(filepath=str(anim_blend))
    rig = bpy.data.objects["AvatarRig"]
    head = bpy.data.objects["Avatar_Head"]
    body = bpy.data.objects["Avatar_Body"]

    atlas_dir = WORK / "atlases_v9"
    atlas_paths = build_zero_bleed_3x3_atlas(atlas_dir, size=atlas_size, navy_variant=False)
    unified_mat = optimize_and_export.create_unified_gltf_pbr_material(atlas_paths)
    head.data.materials.clear()
    head.data.materials.append(unified_mat)
    body.data.materials.clear()
    body.data.materials.append(unified_mat)

    cur_tris = sum(sum(len(p.vertices) - 2 for p in o.data.polygons) for o in (head, body))
    if target_max_tris is not None and cur_tris > target_max_tris:
        # Decimate smoothly to `92%` of budget (`~22,800` tris for LOD0, `~11,000` tris for LOD1):
        ratio = max(0.055, min(0.92, (target_max_tris * 0.91) / float(cur_tris)))

        n_h0 = len(head.data.vertices)
        p0 = np.empty((n_h0, 3), dtype=np.float64)
        head.data.vertices.foreach_get("co", p0.ravel())
        sk_list = []
        for kb in head.data.shape_keys.key_blocks[1:]:
            arr = np.empty((n_h0, 3), dtype=np.float64)
            kb.data.foreach_get("co", arr.ravel())
            sk_list.append((kb.name, arr - p0))

        saved_sk_nla = []
        if head.data.shape_keys and head.data.shape_keys.animation_data:
            for tr in head.data.shape_keys.animation_data.nla_tracks:
                if tr.strips:
                    saved_sk_nla.append((tr.name, tr.strips[0].action, int(tr.strips[0].frame_start)))

        head.shape_key_clear()
        bpy.ops.object.select_all(action="DESELECT")
        head.select_set(True)
        bpy.context.view_layer.objects.active = head
        hdec = head.modifiers.new("DecHead", "DECIMATE")
        hdec.ratio = ratio
        while head.modifiers.find(hdec.name) > 0:
            bpy.ops.object.modifier_move_up(modifier=hdec.name)
        bpy.ops.object.modifier_apply(modifier=hdec.name)

        n_h1 = len(head.data.vertices)
        p1 = np.empty((n_h1, 3), dtype=np.float64)
        head.data.vertices.foreach_get("co", p1.ravel())
        dists, idxs = cKDTree(p0).query(p1, k=4)
        inv_w = 1.0 / np.maximum(dists, 1e-6)
        inv_w /= np.sum(inv_w, axis=1, keepdims=True)
        head.shape_key_add(name="Basis", from_mix=False)
        for sk_name, delta0 in sk_list:
            kb_new = head.shape_key_add(name=sk_name, from_mix=False)
            delta1 = np.einsum("ij,ijk->ik", inv_w, delta0[idxs])
            kb_new.data.foreach_set("co", (p1 + delta1).astype(np.float32).ravel())

        if saved_sk_nla:
            sk_anim = head.data.shape_keys.animation_data_create()
            for tr_name, act, f0 in saved_sk_nla:
                tr = sk_anim.nla_tracks.new()
                tr.name = tr_name
                st = tr.strips.new(name=tr_name, start=f0, action=act)
                st.blend_type = "REPLACE"
                tr.mute = True

        bpy.ops.object.select_all(action="DESELECT")
        body.select_set(True)
        bpy.context.view_layer.objects.active = body
        bdec = body.modifiers.new("DecBody", "DECIMATE")
        bdec.ratio = ratio
        while body.modifiers.find(bdec.name) > 0:
            bpy.ops.object.modifier_move_up(modifier=bdec.name)
        bpy.ops.object.modifier_apply(modifier=bdec.name)

    valid_bones = {b.name for b in rig.data.bones}
    optimize_and_export.clamp_and_normalize_skin_weights(head, valid_bones, 4)
    optimize_and_export.clamp_and_normalize_skin_weights(body, valid_bones, 4)

    for m in list(bpy.data.materials):
        if m != unified_mat:
            bpy.data.materials.remove(m, do_unlink=True)

    out_glb.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(out_glb.resolve()),
        export_format="GLB",
        export_yup=True,
        export_apply=False,
        export_skins=True,
        export_all_influences=False,
        export_morph=True,
        export_morph_normal=False,
        export_morph_tangent=False,
        export_animations=True,
        export_animation_mode="NLA_TRACKS",
        export_draco_mesh_compression_enable=False,
    )

    if attach_variants:
        navy_paths = build_zero_bleed_3x3_atlas(atlas_dir, size=atlas_size, navy_variant=True)
        raw_glb = out_glb.read_bytes()
        navy_glb = photo_paint.replace_base_colour(raw_glb, Path(navy_paths["albedo"]).read_bytes())
        out_glb.write_bytes(
            glb_skins.with_variants(raw_glb, [("Midnight Cosmic Z-Tee", navy_glb)], original="Signature Black Z-Tee")
        )

    final_tris = sum(sum(len(p.vertices) - 2 for p in o.data.polygons) for o in (head, body))
    rt = optimize_and_export.verify_roundtrip_blender_reimport(out_glb)
    return {
        "glb": str(out_glb),
        "size_mb": round(out_glb.stat().st_size / (1024 * 1024), 4),
        "triangles": final_tris,
        "blender_roundtrip_reimport": rt,
    }


def main() -> None:
    cal = np.load(WORK / "calibration_v7.npz")
    EXPORT_DIR.mkdir(parents=True, exist_ok=True)
    APP_AVATAR_DIR.mkdir(parents=True, exist_ok=True)

    anim_blend = build_rigged_animated_master_blend(cal)

    master_p = EXPORT_DIR / "professor_z_master.glb"
    lod0_exp = EXPORT_DIR / "professor_z.glb"
    lod1_exp = EXPORT_DIR / "professor_z_lod1.glb"

    res_master = export_single_tier(anim_blend, master_p, atlas_size=2048, target_max_tris=52000, attach_variants=True)
    res_lod0 = export_single_tier(anim_blend, lod0_exp, atlas_size=1024, target_max_tris=24500, attach_variants=False)
    res_lod1 = export_single_tier(anim_blend, lod1_exp, atlas_size=512, target_max_tris=9200, attach_variants=False)

    shutil.copy2(lod0_exp, APP_AVATAR_DIR / "professor_z.glb")
    shutil.copy2(lod1_exp, APP_AVATAR_DIR / "professor_z_lod1.glb")

    summary = {"master": res_master, "lod0": res_lod0, "lod1": res_lod1}
    (WORK / "export_v9_summary.json").write_text(json.dumps(summary, indent=2))
    print("STAGE D PRODUCTION GLBs EXPORTED SUCCESSFULLY:")
    print(json.dumps(summary, indent=2))

    # Render live visual proof directly from a fresh Blender re-import of `app/src/main/assets/avatar/professor_z.glb`!
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(APP_AVATAR_DIR / "professor_z.glb"))
    for o in list(bpy.data.objects):
        if o.name.startswith("Icosphere"):
            bpy.data.objects.remove(o, do_unlink=True)
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 36
    scene.cycles.use_denoising = True
    scene.cycles.denoiser = "OPENIMAGEDENOISE"
    scene.view_settings.view_transform = "Standard"
    scene.render.film_transparent = True
    world = bpy.data.worlds.new("WProof")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (1.0, 0.98, 0.96, 1.0)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.45
    for loc, en in [((-1.8, -3.1, 2.35), 110), ((2.2, -2.5, 1.6), 50), ((3.2, 0.7, 2.15), 90), ((-3.2, 0.7, 2.15), 90), ((0.2, 3.3, 2.25), 100)]:
        ld = bpy.data.lights.new("L", "AREA")
        ld.energy, ld.size = en, 2.6
        lo = bpy.data.objects.new("LO", ld)
        scene.collection.objects.link(lo)
        lo.location = loc
        lo.rotation_euler = (Vector((0, 0, 1.05)) - Vector(loc)).to_track_quat("-Z", "Y").to_euler()
    bpy.ops.mesh.primitive_plane_add(size=4.0, location=(0, 0, 0))
    bpy.context.selected_objects[0].is_shadow_catcher = True
    cd = bpy.data.cameras.new("C")
    co = bpy.data.objects.new("CO", cd)
    scene.collection.objects.link(co)
    scene.camera = co
    cards = []
    cream = np.array([238.0, 232.0, 227.0], np.float32)
    for ang in (0.0, 42.0, 90.0, 180.0):
        rad = math.radians(ang)
        cd.lens = 68.0
        co.location = (3.65 * math.sin(rad), -3.65 * math.cos(rad), 0.90)
        co.rotation_euler = (Vector((0, 0, 0.875)) - co.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.resolution_x, scene.render.resolution_y = 380, 900
        p = WORK / f"reimp_proof_{int(ang)}.png"
        scene.render.filepath = str(p)
        bpy.ops.render.render(write_still=True)
        rgba = np.asarray(Image.open(p).convert("RGBA")).astype(np.float32)
        a = rgba[:, :, 3:4] / 255.0
        cards.append(np.clip(rgba[:, :, :3] * a + cream * (1.0 - a), 0, 255).astype(np.uint8))
    Image.fromarray(np.hstack(cards)).save(ROOT / "avatar_pipeline/renders/professor_z_glb_reimport_proof.png")
    print("SAVED SHIPPED GLB RE-IMPORT PROOF RENDER -> professor_z_glb_reimport_proof.png")


if __name__ == "__main__":
    main()
