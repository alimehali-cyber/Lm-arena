#!/usr/bin/env python3
"""
Stage 7: Single-Atlas PBR Consolidation, `PIN_DETAIL` LOD Decimation & Clean glTF 2.0 Exporter
(`optimize_and_export.py`).

Consolidates all multi-material character sub-surfaces into **ONE unified `Mat_AvatarMobile_PBR`**
(`Albedo + Tangent Normal + Occlusion/Roughness/Metallic`) so the entire animated 3D character renders
in just **2 OpenGL ES 3.0 draw calls** (`Avatar_Head` with sparse morphs + `Avatar_Body` skinned mesh).
"""
from __future__ import annotations

import argparse
import json
import pathlib
import sys
from typing import Any, Dict, Tuple

import bpy
import numpy as np
from PIL import Image
from scipy.spatial import cKDTree


def build_master_avatar_atlas(profile: Dict[str, Any], out_dir: pathlib.Path, size: int = 1024) -> Dict[str, str]:
    """
    Pack all 4 character quadrants into a single power-of-two `size x size` PBR atlas (`PIL row 0 -> uv_v = 1.0`):
      - Top-Left     (`u: 0.0..0.5, v: 0.5..1.0` -> `rows 0..h, cols 0..h`): Head Skin Subsurface Albedo
      - Top-Right    (`u: 0.5..1.0, v: 0.5..1.0` -> `rows 0..h, cols h..s`): Glossy Eye Sclera + Chestnut Iris
      - Bottom-Left  (`u: 0.0..0.5, v: 0.0..0.5` -> `rows h..s, cols 0..h`): Garment 2x2 Sub-Atlas (`Tweed/Shirt/Wool/Shoe/Gold`)
      - Bottom-Right (`u: 0.5..1.0, v: 0.0..0.5` -> `rows h..s, cols h..s`): 4 Crisp Solid/Micro Swatches (`HairDark`, `TeethIvory`, `TongueCoral`, `OralDark`)
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
    orm[..., 1] = 128
    orm[..., 2] = 0

    head_im = np.array(Image.open(tex["head_skin_albedo"]).convert("RGB").resize((h, h), Image.Resampling.LANCZOS))
    eye_im  = np.array(Image.open(tex["eye_iris_albedo"]).convert("RGB").resize((h, h), Image.Resampling.LANCZOS))
    garm_alb = np.array(Image.open(tex["albedo"]).convert("RGB").resize((h, h), Image.Resampling.LANCZOS))
    garm_nrm = np.array(Image.open(tex["normal"]).convert("RGB").resize((h, h), Image.Resampling.BILINEAR))
    garm_orm = np.array(Image.open(tex["orm"]).convert("RGB").resize((h, h), Image.Resampling.BILINEAR))

    # Top-Left: Head Skin (`roughness = 0.46`)
    albedo[0:h, 0:h] = head_im
    orm[0:h, 0:h, 1] = int(0.46 * 255)

    # Top-Right: Eye Sclera + Iris (`roughness = 0.045`)
    albedo[0:h, h:size] = eye_im
    orm[0:h, h:size, 1] = int(0.045 * 255)

    # Bottom-Left: Garments + Sneakers + Metallic-Gold Glasses tile!
    garm_orm[0 : h // 4, (h * 3) // 4 : h, 2] = 225  # Metallic = 0.88 on Gold Glasses & Buckle!
    garm_orm[0 : h // 4, (h * 3) // 4 : h, 1] = 56   # Satin Roughness = 0.22
    albedo[h:size, 0:h] = garm_alb
    normal[h:size, 0:h] = garm_nrm
    orm[h:size, 0:h] = garm_orm

    # Bottom-Right: `HairDark` (TL), `TeethIvory` (TR), `TongueCoral` (BL), `OralDark` (BR)
    def fill_swatch(r0: int, r1: int, c0: int, c1: int, rgb01, rough_val: float):
        albedo[r0:r1, c0:c1] = np.clip(np.array(rgb01) * 255.0, 0, 255).astype(np.uint8)
        orm[r0:r1, c0:c1, 1] = int(rough_val * 255)

    fill_swatch(h, h + q, h, h + q, pal["hair_srgb"], 0.38)
    fill_swatch(h, h + q, h + q, size, [0.955, 0.942, 0.910], 0.20)
    fill_swatch(h + q, size, h, h + q, [0.74, 0.28, 0.28], 0.36)
    fill_swatch(h + q, size, h + q, size, [0.035, 0.011, 0.014], 0.85)

    out_dir.mkdir(parents=True, exist_ok=True)
    p_a = out_dir / f"avatar_master_albedo_{size}.png"
    p_n = out_dir / f"avatar_master_normal_{size}.png"
    p_o = out_dir / f"avatar_master_orm_{size}.png"
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
    """Remap per-polygon UVs from each source material slot into exact quadrants of `Mat_AvatarMobile_PBR`."""
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
    # Remove any non-bone helper vertex groups FIRST (`PIN_DETAIL`, `ZONE_*`, `PART_*`)
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


def export_lod_variant(
    master_blend: pathlib.Path,
    profile: Dict[str, Any],
    out_glb: pathlib.Path,
    atlas_dir: pathlib.Path,
    atlas_size: int,
    target_max_tris: int,
    max_inf: int,
    draco: bool,
) -> Dict[str, Any]:
    bpy.ops.wm.open_mainfile(filepath=str(master_blend.resolve()))
    rig = bpy.data.objects["AvatarRig"]
    head = bpy.data.objects["Avatar_Head"]
    body = bpy.data.objects["Avatar_Body"]

    # Remove studio review helpers if any exist
    for o in list(bpy.data.objects):
        if o not in (rig, head, body):
            bpy.data.objects.remove(o, do_unlink=True)

    atlas_paths = build_master_avatar_atlas(profile, atlas_dir, size=atlas_size)
    unified_mat = create_unified_gltf_pbr_material(atlas_paths)
    remap_object_uvs_to_single_atlas(head, unified_mat)
    remap_object_uvs_to_single_atlas(body, unified_mat)

    # Decimate `Avatar_Head` (preserving all 26 morph targets via cKDTree 4-NN interpolation!) & `Avatar_Body` (`PIN_DETAIL`-safe) for `LOD1`!
    cur_tris = sum(sum(len(p.vertices) - 2 for p in o.data.polygons) for o in (head, body))
    if cur_tris > target_max_tris:
        ratio = max(0.26, min(0.88, (target_max_tris * 0.66) / float(cur_tris)))

        # 1. Capture `Avatar_Head` basis + all 26 morph target deltas BEFORE removing shape keys for `DECIMATE`
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

        # Re-bind all 26 morph targets + all 24 NLA strips cleanly onto decimated `Avatar_Head`!
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

        # 2. Decimate `Avatar_Body` while protecting `PIN_DETAIL` (`Round glasses`)!
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

    # Purge orphan materials so exported `.glb` contains strictly ONE shared PBR material!
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
    return {
        "glb": str(out_glb.resolve()),
        "size_mb": round(out_glb.stat().st_size / (1024.0 * 1024.0), 3),
        "triangles": final_tris,
        "atlas_size": atlas_size,
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

    lod0 = export_lod_variant(
        master_blend, profile, exp_dir / "avatar_mobile.glb", atlas_dir,
        int(b["lod0_atlas_size"]), int(b["lod0_max_triangles"]), int(b["max_skin_weights_per_vertex"]), bool(b["use_draco"])
    )
    lod1 = export_lod_variant(
        master_blend, profile, exp_dir / "avatar_mobile_lod1.glb", atlas_dir,
        int(b["lod1_atlas_size"]), int(b["lod1_max_triangles"]), int(b["max_skin_weights_per_vertex"]), bool(b["use_draco"])
    )
    print(json.dumps({"status": "exported", "lod0": lod0, "lod1": lod1}, indent=2))


if __name__ == "__main__":
    main()
