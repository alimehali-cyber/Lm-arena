#!/usr/bin/env python3
"""
Stage 7: Master `2048x2048` + Mobile `1024x1024` (`LOD0`) + `512x512` (`LOD1`) PBR Exporter,
`AssetFurnace / image-to-3dlab` `KHR_materials_variants` & `glb_stats` Post-Processor, and
Clean Blender 4.2 glTF 2.0 Round-Trip Re-Import Verifier (`optimize_and_export.py`).
"""
from __future__ import annotations

import argparse
import io
import json
import pathlib
import shutil
import sys
from typing import Any, Dict

import bpy
import numpy as np
from PIL import Image
from scipy.spatial import cKDTree

from image_to_3dlab import glb_skins, glb_stats, photo_paint


def build_master_avatar_atlas(
    profile: Dict[str, Any],
    out_dir: pathlib.Path,
    size: int = 1024,
    navy_variant: bool = False,
) -> Dict[str, str]:
    """
    Pack all 4 character quadrants into a power-of-two `size x size` PBR atlas:
      - Top-Left     (`u: 0.0..0.5, v: 0.5..1.0`): Head Skin Subsurface Albedo
      - Top-Right    (`u: 0.5..1.0, v: 0.5..1.0`): Glossy Eye Sclera + Soulful Chestnut Iris
      - Bottom-Left  (`u: 0.0..0.5, v: 0.0..0.5`): Garment 2x2 Sub-Atlas (`Tweed/Shirt/Wool/Shoe/Bronze`)
      - Bottom-Right (`u: 0.5..1.0, v: 0.0..0.5`): `HairDark`, `TeethIvory`, `TongueCoral`, `OralDark`
    """
    tex = profile["generated_textures"]
    pal = profile["features_and_palette"]
    h = size // 2
    q = size // 4

    albedo = np.zeros((size, size, 3), dtype=np.uint8)
    normal = np.zeros((size, size, 3), dtype=np.uint8)
    normal[..., 0] = 128
    normal[..., 1] = 128
    normal[..., 2] = 255
    orm = np.zeros((size, size, 3), dtype=np.uint8)
    orm[..., 0] = 248
    orm[..., 1] = 140
    orm[..., 2] = 0

    head_im = np.array(Image.open(tex["head_skin_albedo"]).convert("RGB").resize((h, h), Image.Resampling.LANCZOS))
    eye_im  = np.array(Image.open(tex["eye_iris_albedo"]).convert("RGB").resize((h, h), Image.Resampling.LANCZOS))
    garm_alb = np.array(Image.open(tex["albedo"]).convert("RGB").resize((h, h), Image.Resampling.LANCZOS))
    garm_nrm = np.array(Image.open(tex["normal"]).convert("RGB").resize((h, h), Image.Resampling.BILINEAR))
    garm_orm = np.array(Image.open(tex["orm"]).convert("RGB").resize((h, h), Image.Resampling.BILINEAR))

    if navy_variant:
        # Second switchable outfit variant inspired by `ref_01_professor_z_lineup.png` (`Midnight Lab Navy Z-Suit`)
        hh2 = h // 2
        garm_alb[hh2:h, 0:hh2] = np.clip(garm_alb[hh2:h, 0:hh2].astype(np.float32) * np.array([0.38, 0.68, 1.85]), 0, 255).astype(np.uint8)
        garm_alb[hh2:h, hh2:h] = np.clip(garm_alb[hh2:h, hh2:h].astype(np.float32) * np.array([0.24, 0.34, 0.56]), 0, 255).astype(np.uint8)

    # Top-Left: Natural Matte-Subsurface Skin (`roughness = 0.56`)
    albedo[0:h, 0:h] = head_im
    orm[0:h, 0:h, 1] = int(0.56 * 255)

    # Top-Right: Wet Glossy Cornea & Iris (`roughness = 0.04`)
    albedo[0:h, h:size] = eye_im
    orm[0:h, h:size, 1] = int(0.04 * 255)

    # Bottom-Left: Garments + Leather Brogues + Satin Bronze Frames
    garm_orm[0 : h // 4, (h * 3) // 4 : h, 2] = 220
    garm_orm[0 : h // 4, (h * 3) // 4 : h, 1] = 68
    albedo[h:size, 0:h] = garm_alb
    normal[h:size, 0:h] = garm_nrm
    orm[h:size, 0:h] = garm_orm

    def fill_swatch(r0: int, r1: int, c0: int, c1: int, rgb01, rough_val: float):
        albedo[r0:r1, c0:c1] = np.clip(np.array(rgb01) * 255.0, 0, 255).astype(np.uint8)
        orm[r0:r1, c0:c1, 1] = int(rough_val * 255)

    fill_swatch(h, h + q, h, h + q, pal["hair_srgb"], 0.48)
    fill_swatch(h, h + q, h + q, size, [0.955, 0.942, 0.910], 0.22)
    fill_swatch(h + q, size, h, h + q, [0.76, 0.29, 0.29], 0.36)
    fill_swatch(h + q, size, h + q, size, [0.032, 0.010, 0.013], 0.88)

    out_dir.mkdir(parents=True, exist_ok=True)
    tag = "_navy_variant" if navy_variant else ""
    p_a = out_dir / f"avatar_master_albedo_{size}{tag}.png"
    p_n = out_dir / f"avatar_master_normal_{size}{tag}.png"
    p_o = out_dir / f"avatar_master_orm_{size}{tag}.png"
    Image.fromarray(albedo).save(p_a)
    Image.fromarray(normal).save(p_n)
    Image.fromarray(orm).save(p_o)
    return {"albedo": str(p_a.resolve()), "normal": str(p_n.resolve()), "orm": str(p_o.resolve())}


def create_unified_gltf_pbr_material(atlas: Dict[str, str]) -> bpy.types.Material:
    mat = bpy.data.materials.new(name="Mat_AvatarMobile_PBR")
    mat.use_nodes = True
    nt = mat.node_tree
    nt.nodes.clear()

    out = nt.nodes.new("ShaderNodeOutputMaterial")
    bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
    nt.links.new(bsdf.outputs["BSDF"], out.inputs["Surface"])

    alb_img = bpy.data.images.load(atlas["albedo"], check_existing=True)
    alb_img.pack()
    nrm_img = bpy.data.images.load(atlas["normal"], check_existing=True)
    nrm_img.colorspace_settings.name = "Non-Color"
    nrm_img.pack()
    orm_img = bpy.data.images.load(atlas["orm"], check_existing=True)
    orm_img.colorspace_settings.name = "Non-Color"
    orm_img.pack()

    t_alb = nt.nodes.new("ShaderNodeTexImage")
    t_alb.image = alb_img
    nt.links.new(t_alb.outputs["Color"], bsdf.inputs["Base Color"])

    t_orm = nt.nodes.new("ShaderNodeTexImage")
    t_orm.image = orm_img
    sep = nt.nodes.new("ShaderNodeSeparateColor")
    nt.links.new(t_orm.outputs["Color"], sep.inputs["Color"])
    nt.links.new(sep.outputs["Green"], bsdf.inputs["Roughness"])
    nt.links.new(sep.outputs["Blue"], bsdf.inputs["Metallic"])

    t_nrm = nt.nodes.new("ShaderNodeTexImage")
    t_nrm.image = nrm_img
    nmap = nt.nodes.new("ShaderNodeNormalMap")
    nmap.inputs["Strength"].default_value = 0.25
    nt.links.new(t_nrm.outputs["Color"], nmap.inputs["Color"])
    nt.links.new(nmap.outputs["Normal"], bsdf.inputs["Normal"])
    return mat


def remap_object_uvs_to_single_atlas(obj: bpy.types.Object, unified_mat: bpy.types.Material) -> None:
    uv_layer = obj.data.uv_layers.active.data
    slot_names = [ms.material.name if ms.material else "" for ms in obj.material_slots]

    for poly in obj.data.polygons:
        mname = slot_names[poly.material_index] if poly.material_index < len(slot_names) else ""
        for li in poly.loop_indices:
            u, v = float(uv_layer[li].uv.x), float(uv_layer[li].uv.y)
            u = min(max(u, 0.004), 0.996)
            v = min(max(v, 0.004), 0.996)
            if "Skin" in mname:
                nu, nv = 0.5 * u, 0.5 + 0.5 * v
            elif "Eye" in mname:
                nu, nv = 0.5 + 0.5 * u, 0.5 + 0.5 * v
            elif "Garments" in mname:
                nu, nv = 0.5 * u, 0.5 * v
            elif "HairDark" in mname:
                nu, nv = 0.52 + 0.21 * u, 0.27 + 0.21 * v
            elif "Teeth" in mname:
                nu, nv = 0.77 + 0.21 * u, 0.27 + 0.21 * v
            elif "Tongue" in mname:
                nu, nv = 0.52 + 0.21 * u, 0.02 + 0.21 * v
            else:
                nu, nv = 0.77 + 0.21 * u, 0.02 + 0.21 * v
            uv_layer[li].uv = (nu, nv)
        poly.material_index = 0

    obj.data.materials.clear()
    obj.data.materials.append(unified_mat)


def clamp_and_normalize_skin_weights(obj: bpy.types.Object, valid_bones: set[str], max_inf: int = 4) -> None:
    for vg in list(obj.vertex_groups):
        if vg.name not in valid_bones:
            obj.vertex_groups.remove(vg)

    fallback_vg = obj.vertex_groups.get("Hips") or obj.vertex_groups[0]
    for v in obj.data.vertices:
        pairs = [(g.group, g.weight) for g in v.groups if g.weight > 1e-4]
        if not pairs:
            fallback_vg.add([v.index], 1.0, "REPLACE")
            continue
        pairs.sort(key=lambda x: x[1], reverse=True)
        keep = pairs[:max_inf]
        keep_ids = {gid for gid, _ in keep}
        for gid, _ in pairs:
            if gid not in keep_ids:
                obj.vertex_groups[gid].remove([v.index])
        total = sum(w for _, w in keep) or 1.0
        for gid, w in keep:
            obj.vertex_groups[gid].add([v.index], w / total, "REPLACE")


def verify_roundtrip_blender_reimport(glb_path: pathlib.Path) -> Dict[str, Any]:
    """Re-import exported `.glb` into an empty Blender scene and verify geometry, UVs, textures, rig, morphs & animations."""
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb_path.resolve()))
    meshes = [o for o in bpy.data.objects if o.type == "MESH"]
    arms = [o for o in bpy.data.objects if o.type == "ARMATURE"]
    tris = sum(sum(len(p.vertices) - 2 for p in m.data.polygons) for m in meshes)
    verts = sum(len(m.data.vertices) for m in meshes)
    has_uvs = all(len(m.data.uv_layers) >= 1 for m in meshes)
    n_bones = sum(len(a.data.bones) for a in arms)
    morphs = max((len(m.data.shape_keys.key_blocks) - 1 for m in meshes if m.data.shape_keys), default=0)
    n_images = len(bpy.data.images)
    n_actions = len(bpy.data.actions)
    af_stats = glb_stats.file_stats(glb_path.resolve())
    return {
        "reimport_ok": bool(len(meshes) >= 2 and len(arms) == 1 and has_uvs and n_bones == 60 and morphs == 26 and n_images >= 3),
        "reimported_mesh_objects": [m.name for m in meshes],
        "reimported_vertices": verts,
        "reimported_triangles": tris,
        "uv_layers_present_all_meshes": has_uvs,
        "reimported_bones": n_bones,
        "reimported_morph_targets": morphs,
        "reimported_packed_images": n_images,
        "reimported_blender_actions": n_actions,
        "assetfurnace_glb_stats": af_stats,
    }


def export_lod_variant(
    master_blend: pathlib.Path,
    profile: Dict[str, Any],
    out_glb: pathlib.Path,
    atlas_dir: pathlib.Path,
    atlas_size: int,
    target_max_tris: int | None,
    max_inf: int,
    draco: bool,
    attach_khr_variants: bool = False,
) -> Dict[str, Any]:
    bpy.ops.wm.open_mainfile(filepath=str(master_blend.resolve()))
    rig = bpy.data.objects["AvatarRig"]
    head = bpy.data.objects["Avatar_Head"]
    body = bpy.data.objects["Avatar_Body"]

    for o in list(bpy.data.objects):
        if o not in (rig, head, body):
            bpy.data.objects.remove(o, do_unlink=True)

    atlas_paths = build_master_avatar_atlas(profile, atlas_dir, size=atlas_size, navy_variant=False)
    unified_mat = create_unified_gltf_pbr_material(atlas_paths)
    remap_object_uvs_to_single_atlas(head, unified_mat)
    remap_object_uvs_to_single_atlas(body, unified_mat)

    cur_tris = sum(sum(len(p.vertices) - 2 for p in o.data.polygons) for o in (head, body))
    if target_max_tris is not None and cur_tris > target_max_tris:
        ratio = max(0.26, min(0.88, (target_max_tris * 0.66) / float(cur_tris)))

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
        hdec = head.modifiers.new("DecimateHeadLOD", "DECIMATE")
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
        dec = body.modifiers.new("DecimateLOD", "DECIMATE")
        dec.ratio = ratio
        if "PIN_DETAIL" in body.vertex_groups:
            dec.vertex_group = "PIN_DETAIL"
            dec.invert_vertex_group = True
        while body.modifiers.find(dec.name) > 0:
            bpy.ops.object.modifier_move_up(modifier=dec.name)
        bpy.ops.object.modifier_apply(modifier=dec.name)

    valid_bones = {b.name for b in rig.data.bones}
    clamp_and_normalize_skin_weights(head, valid_bones, max_inf)
    clamp_and_normalize_skin_weights(body, valid_bones, max_inf)

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
        export_draco_mesh_compression_enable=draco,
    )

    final_tris = sum(sum(len(p.vertices) - 2 for p in o.data.polygons) for o in (head, body))

    # On High-Resolution Master GLB, embed `KHR_materials_variants` (`Catalogue Tweed` + `Midnight Lab Navy`)
    # using live `AssetFurnace / image-to-3dlab` (`glb_skins.with_variants`)!
    variants_attached = []
    if attach_khr_variants:
        navy_paths = build_master_avatar_atlas(profile, atlas_dir, size=atlas_size, navy_variant=True)
        buf1 = pathlib.Path(navy_paths["albedo"]).read_bytes()
        raw_glb = out_glb.read_bytes()
        navy_glb = photo_paint.replace_base_colour(raw_glb, buf1)
        skinned_glb = glb_skins.with_variants(
            raw_glb,
            [("Midnight Lab Navy Z-Suit", navy_glb)],
            original="Catalogue Tweed Blazer",
        )
        out_glb.write_bytes(skinned_glb)
        variants_attached = ["Catalogue Tweed Blazer", "Midnight Lab Navy Z-Suit"]

    roundtrip = verify_roundtrip_blender_reimport(out_glb)
    return {
        "glb": str(out_glb.resolve()),
        "size_mb": round(out_glb.stat().st_size / (1024.0 * 1024.0), 3),
        "triangles": final_tris,
        "atlas_size": atlas_size,
        "khr_materials_variants": variants_attached,
        "blender_roundtrip_reimport": roundtrip,
    }


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--in-blend", required=True)
    ap.add_argument("--profile", required=True)
    ap.add_argument("--export-dir", required=True)
    ap.add_argument("--config", required=True)
    args = ap.parse_args(argv)

    cfg = json.loads(pathlib.Path(args.config).read_text())
    b = cfg["budgets"]
    profile = json.loads(pathlib.Path(args.profile).read_text())
    exp_dir = pathlib.Path(args.export_dir)
    atlas_dir = exp_dir / "atlases"
    master_blend = pathlib.Path(args.in_blend)

    # 1. Separate High-Resolution Master Asset (`2048x2048` PBR + `KHR_materials_variants` + Master `.blend`)
    shutil.copy2(master_blend, exp_dir / "avatar_master.blend")
    master = export_lod_variant(
        master_blend,
        profile,
        exp_dir / "avatar_master_2048.glb",
        atlas_dir,
        2048,
        None,
        int(b["max_skin_weights_per_vertex"]),
        False,
        attach_khr_variants=True,
    )

    # 2. Mobile Android GLB `LOD0` (`1024x1024` single PBR material for GLES 3.0)
    lod0 = export_lod_variant(
        master_blend,
        profile,
        exp_dir / "avatar_mobile.glb",
        atlas_dir,
        int(b["lod0_atlas_size"]),
        int(b["lod0_max_triangles"]),
        int(b["max_skin_weights_per_vertex"]),
        bool(b["use_draco"]),
        attach_khr_variants=False,
    )

    # 3. Mobile Android GLB `LOD1` (`512x512` single PBR material decimated with `PIN_DETAIL` protection)
    lod1 = export_lod_variant(
        master_blend,
        profile,
        exp_dir / "avatar_mobile_lod1.glb",
        atlas_dir,
        int(b["lod1_atlas_size"]),
        int(b["lod1_max_triangles"]),
        int(b["max_skin_weights_per_vertex"]),
        bool(b["use_draco"]),
        attach_khr_variants=False,
    )

    summary = {"status": "exported", "master": master, "lod0": lod0, "lod1": lod1}
    (exp_dir / "export_summary.json").write_text(json.dumps(summary, indent=2))
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
