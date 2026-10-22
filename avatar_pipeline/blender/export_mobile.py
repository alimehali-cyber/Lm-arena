#!/usr/bin/env python3
"""
Stage 8 - export the mobile GLB and measure it.

Exports the deforming skeleton only, one skinned mesh, every morph target and every
clip (as NLA tracks, one animation per track).

Two settings that each halved the file and are worth remembering:
  * `export_texcoords=False` - the character is solid-coloured, so UVs were 0.7 MB of
    dead weight;
  * smooth shading before export - otherwise normals split per face and the exporter
    duplicates vertices for almost every triangle (92k verts for 34k triangles).

Run: .venv/bin/python blender/export_mobile.py --blend working/x/character_anim.blend \
        --out exports/x/avatar.glb --report exports/x/08_export.json
"""
from __future__ import annotations
import argparse, json, pathlib, struct, sys
import bpy
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from scene_utils import reset_pose


def glb_container(path: pathlib.Path) -> dict:
    """Parse the GLB container and its JSON chunk without any external library."""
    data = path.read_bytes()
    magic, version, length = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        return {"error": "not a GLB"}
    off, js, bin_bytes = 12, None, 0
    while off < length:
        clen, ctype = struct.unpack_from("<II", data, off)
        chunk = data[off + 8: off + 8 + clen]
        if ctype == 0x4E4F534A:
            js = json.loads(chunk.decode("utf-8"))
        elif ctype == 0x004E4942:
            bin_bytes = clen
        off += 8 + clen + ((4 - clen % 4) % 4 if clen % 4 else 0)
    if js is None:
        return {"error": "no JSON chunk"}

    meshes = js.get("meshes", [])
    prims = [p for m in meshes for p in m.get("primitives", [])]
    verts = sum(js["accessors"][p["attributes"]["POSITION"]]["count"] for p in prims
                if "POSITION" in p.get("attributes", {}))
    tris = sum(js["accessors"][p["indices"]]["count"] // 3 for p in prims if "indices" in p)
    return {
        "glb_bytes": len(data), "bin_bytes": bin_bytes,
        "generator": js.get("asset", {}).get("generator"),
        "nodes": len(js.get("nodes", [])),
        "meshes": len(meshes), "primitives": len(prims),
        "materials": len(js.get("materials", [])),
        "textures": len(js.get("textures", [])), "images": len(js.get("images", [])),
        "skins": len(js.get("skins", [])),
        "joints": len(js["skins"][0]["joints"]) if js.get("skins") else 0,
        "runtime_vertices": verts, "runtime_triangles": tris,
        "animations": [a.get("name") for a in js.get("animations", [])],
        "animation_channels": [len(a.get("channels", [])) for a in js.get("animations", [])],
        "morph_targets_per_primitive": [len(p.get("targets", [])) for p in prims],
        "extensions_used": js.get("extensionsUsed", []),
        "accessors": len(js.get("accessors", [])),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    arm = next((o for o in bpy.data.objects if o.type == "ARMATURE"), None)
    mesh = next((o for o in bpy.data.objects if o.type == "MESH"), None)
    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    reset_pose(arm)

    report = {"source_blend": a.blend, "status": "running"}
    if mesh:
        report["source_vertices"] = len(mesh.data.vertices)
        report["source_triangles"] = sum(len(p.vertices) - 2 for p in mesh.data.polygons)

    bpy.ops.object.select_all(action="SELECT")
    kwargs = dict(
        filepath=str(out),
        export_format="GLB",
        use_selection=True,
        export_apply=False,
        export_animations=True,
        export_animation_mode="NLA_TRACKS",
        export_nla_strips=True,
        export_bake_animation=False,
        # glTF has no bezier. The exporter's fallback for the curves it bakes from the
        # sparse authoring keys is STEP, i.e. held frames, which reads as judder on a
        # 60 Hz phone. Verified after export: every channel that actually changes is
        # LINEAR, while constant channels keep two STEP keys (smaller, and correct).
        export_sampling_interpolation_fallback="LINEAR",
        export_optimize_animation_size=True,
        export_optimize_animation_keep_anim_armature=True,
        export_anim_single_armature=True,
        export_morph=True,
        export_morph_normal=False,      # normal deltas double morph cost for no gain here
        export_morph_tangent=False,
        export_skins=True,
        export_def_bones=True,          # deforming joints only: smaller runtime skeleton
        export_yup=True,
        export_texcoords=False,         # solid colours: UVs are dead weight
        export_normals=True,
        export_materials="EXPORT",
        export_image_format="AUTO",
        export_extras=True,
    )
    dropped = []
    while True:                          # tolerate API drift across Blender versions
        try:
            bpy.ops.export_scene.gltf(**kwargs)
            break
        except TypeError as e:
            msg = str(e)
            bad = next((k for k in list(kwargs)
                        if k in msg and k not in ("filepath", "export_format")), None)
            if bad is None:
                report["export_error"] = msg
                break
            dropped.append(bad)
            kwargs.pop(bad)

    if dropped:
        report["dropped_export_options"] = dropped
    if out.exists():
        report["glb"] = glb_container(out)
        report["status"] = "ok"
    else:
        report["status"] = "failed"

    pathlib.Path(a.report).write_text(json.dumps(report, indent=2))
    print(json.dumps({k: report.get(k) for k in ("status", "glb", "dropped_export_options")},
                     indent=2))


if __name__ == "__main__":
    main()
