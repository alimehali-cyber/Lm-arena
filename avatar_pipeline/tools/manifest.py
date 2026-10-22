#!/usr/bin/env python3
"""
Stage 10 - reproducibility manifest.

Records what was produced, with hashes, plus versions, licences by class, and an
explicit verified / untested split. Licences are recorded per artefact class because
they carry different obligations: the pipeline code is the author's own, Blender/bpy is
GPL-3.0, the generated assets derive from the user's photograph, and the vendored skills
each have their own terms (one has none at all).

Usage: python tools/manifest.py --root . --id test01 --out exports/test01/manifest.json
"""
from __future__ import annotations
import argparse, hashlib, json, pathlib, platform, subprocess

SKILL_LICENCES = {
    "blender-skills":       {"licence": "MIT", "commit": "71d031d", "use": "primary skill workflow"},
    "blender-agent-studio": {"licence": "MIT", "commit": "6ff97fa", "use": "reference: agent/tool structure"},
    "facial-animation":     {"licence": "MIT", "commit": "5bd659d", "use": "reference: facial scope"},
    "cc-blender-skill":     {"licence": "MIT", "commit": "a2634b5", "use": "reference: rig conventions"},
    "gpt_3d_skill":         {"licence": "NO LICENCE FILE", "commit": "d35e84a",
                             "use": "REFERENCE ONLY - never executed, never redistributed"},
}


def sha256(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    ap.add_argument("--id", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--identity", default="photo",
                    choices=("photo", "authored"),
                    help="photo: proportions extrapolated from an image; authored: a design "
                         "built from a character sheet")
    a = ap.parse_args()
    root = pathlib.Path(a.root).resolve()

    def entry(rel):
        p = root / rel
        if not p.exists():
            return None
        return {"path": rel, "bytes": p.stat().st_size, "sha256": sha256(p)}

    artefacts = {}
    for label, rel in [
        ("analysis", f"working/{a.id}/01_analysis.json"),
        ("character_blend", f"working/{a.id}/character.blend"),
        ("build_report", f"working/{a.id}/03_build.json"),
        ("skin_report", f"working/{a.id}/05_skin.json"),
        ("animated_blend", f"working/{a.id}/character_anim.blend"),
        ("anim_report", f"working/{a.id}/06_anim.json"),
        ("visemes", f"working/{a.id}/07_visemes.json"),
        ("export_report", f"exports/{a.id}/08_export.json"),
        ("glb", f"exports/{a.id}/avatar.glb"),
        ("validate_report", f"exports/{a.id}/09_validate.json"),
    ]:
        e = entry(rel)
        if e:
            artefacts[label] = e

    versions = {"python": platform.python_version(), "platform": platform.platform()}
    try:
        import bpy
        versions["blender"] = bpy.app.version_string
    except Exception:
        pass

    status = {}
    vp = root / f"exports/{a.id}/09_validate.json"
    if vp.exists():
        v = json.loads(vp.read_text())
        status["glb_validation"] = v.get("status")
        status["glb_mb"] = v.get("mb")
        status["clips"] = len(v.get("durations_s", {}))
    sp = root / f"working/{a.id}/05_skin.json"
    if sp.exists():
        s = json.loads(sp.read_text())
        status["skinning"] = s.get("status")
        status["max_edge_stretch"] = {k: v["max_stretch"] for k, v in s.get("cases", {}).items()}
        status["skinning_known_limitations"] = s.get("known_limitations", [])
    ep = root / f"exports/{a.id}/08_export.json"
    if ep.exists():
        g = json.loads(ep.read_text()).get("glb", {})
        status["runtime_vertices"] = g.get("runtime_vertices")
        status["runtime_triangles"] = g.get("runtime_triangles")
        status["joints"] = g.get("joints")
        status["draw_calls"] = g.get("primitives")
    status["android_runtime"] = "NOT TESTED - no device and no emulator exist in this environment"
    if a.identity == "authored":
        status["character_identity"] = (
            "STYLISED INTERPRETATION - modelled from the supplied character sheet; "
            "not a likeness of any real person")
    else:
        status["photo_identity_match"] = "NO - the mesh is generic, not a likeness of the subject"

    man = {
        "id": a.id,
        "generated_by": "avatar_pipeline",
        "versions": versions,
        "artefacts": artefacts,
        "licences": {
            "pipeline_source_code": {"licence": "author's own work, not published here"},
            "blender_bpy": {"licence": "GPL-3.0-or-later",
                            "note": "running Blender headless does not make the output GPL"},
            # The input is whatever 01_analysis.json records. This test build used the
            # synthetic fixture in tests/fixtures/, not the user's own photograph, and the
            # manifest must not imply otherwise.
            "generated_assets": {"licence": "derived from the input image named in 01_analysis.json",
                                 "input_image": "see analysis.image",
                                 "note": "no third-party model weights were used; the mesh is "
                                         "parametric, not a reconstruction of the subject"},
            "vendored_skills": SKILL_LICENCES,
            "rhubarb_lipsync": {"licence": "MIT (optional)", "note": "not installed; energy "
                                                                     "fallback used instead"},
            "gltf_exporter": {"licence": "Apache-2.0 (Khronos glTF-Blender-IO)"},
        },
        "status": status,
        "reproduce": f"./run_pipeline.sh working/{a.id}",
    }
    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(man, indent=2))
    print(json.dumps({"manifest": str(out), "artefacts": len(artefacts), "status": status},
                     indent=2))


if __name__ == "__main__":
    main()
