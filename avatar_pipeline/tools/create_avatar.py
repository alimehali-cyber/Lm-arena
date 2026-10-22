#!/usr/bin/env python3
"""
Single Deterministic End-to-End Orchestrator (`create_avatar.py`).

Executes all 8 stages with SHA-256 input+script checkpoint caching (`--resume`):
  1. `reconstruct`       -> `identity_profile.json`, `initial_visual_hull_source.glb` (`AssetFurnace`), & `1024x1024` PBR sub-maps
  2. `lipsync`           -> `lipsync_active.json`
  3. `build_character`   -> `02_character_built.blend`
  4. `rig_character`     -> `03_character_rigged.blend` (`60` joints, `26` morph targets)
  5. `animate_character` -> `04_character_animated.blend` (`24` NLA clips)
  6. `render_review`     -> Neutral Studio `01..09_*.png` + `10_side_profile_portrait.png` + `11_cinematic_hero_portrait.png` + `12_reference_vs_3d_turnaround.png`
  7. `optimize_export`   -> Separate Master (`2048x2048` + `KHR_materials_variants`) + Mobile `LOD0` (`1024x1024`) + `LOD1` (`512x512`) `.glb` with live Blender round-trip re-import & `image_to_3dlab.glb_stats`
  8. `validate_qa`       -> 11-gate validation + `<name>_manifest.json` + `<name>_report.md`
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import time
from typing import Any, Callable, Dict, List


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
VENV_PYTHON = REPO_ROOT / ".venv/bin/python"
X11_STUBS_DIR = REPO_ROOT / "avatar_pipeline/tools/_x11_stubs"


def sha256_files(paths: List[pathlib.Path]) -> str:
    h = hashlib.sha256()
    for p in paths:
        if p.is_file():
            h.update(p.read_bytes())
    return h.hexdigest()


def run_cmd(cmd: List[str]) -> str:
    env = os.environ.copy()
    env["LD_LIBRARY_PATH"] = f"{X11_STUBS_DIR}:/home/user/.local/lib:{env.get('LD_LIBRARY_PATH', '')}"
    env["PATH"] = f"/home/user/.local/bin:{env.get('PATH', '')}"
    env["BAS_SKIP_GPU"] = "1"
    proc = subprocess.run(
        cmd,
        cwd=str(REPO_ROOT),
        env=env,
        text=True,
        encoding="utf-8",
        errors="replace",
        capture_output=True,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            f"Command failed (exit {proc.returncode}): {' '.join(cmd)}\nSTDOUT:\n{proc.stdout}\nSTDERR:\n{proc.stderr}"
        )
    return proc.stdout


def blender_python_cmd(_blender_bin: pathlib.Path, script_rel: str, script_args: List[str]) -> List[str]:
    """Invoke snapshot-persisted headless `bpy==4.2.0` runner (`os._exit(0)` avoids C++ teardown segfault)."""
    return [str(VENV_PYTHON), "avatar_pipeline/tools/_blender_runner.py", "-b", "--factory-startup", "-P", script_rel, "--", *script_args]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--input", required=True)
    ap.add_argument("--audio", default="")
    ap.add_argument("--phonemes", default="")
    ap.add_argument("--lang", default="en")
    ap.add_argument("--name", default="")
    ap.add_argument("--config", default="avatar_pipeline/config.json")
    ap.add_argument("--resume", action="store_true")
    ap.add_argument("--skip-renders", action="store_true")
    args = ap.parse_args()

    in_path = (REPO_ROOT / args.input if not pathlib.Path(args.input).is_absolute() else pathlib.Path(args.input)).resolve()
    cfg_path = (REPO_ROOT / args.config if not pathlib.Path(args.config).is_absolute() else pathlib.Path(args.config)).resolve()
    cfg = json.loads(cfg_path.read_text())

    if cfg.get("strict_commercial_licenses_only", True) and cfg.get("allow_noncommercial_bfm", False):
        raise RuntimeError("SECURITY/LICENSE VIOLATION: Non-commercial BFM weights are forbidden when strict_commercial_licenses_only=true.")

    blender_bin = pathlib.Path(cfg.get("blender_binary", "/home/user/.local/bin/blender"))
    char_slug = args.name.strip() or in_path.stem

    workdir = REPO_ROOT / "avatar_pipeline/working" / char_slug
    render_dir = REPO_ROOT / "avatar_pipeline/renders"
    stage_export_dir = REPO_ROOT / "avatar_pipeline/exports" / char_slug
    canon_export_dir = REPO_ROOT / "avatar_pipeline/exports"
    reports_dir = REPO_ROOT / "avatar_pipeline/reports"
    for d in (workdir, render_dir, stage_export_dir, canon_export_dir, reports_dir):
        d.mkdir(parents=True, exist_ok=True)

    state_file = workdir / "pipeline_state.json"
    tracked_state_file = reports_dir / f"{char_slug}_pipeline_state.json"
    if args.resume and state_file.is_file():
        state: Dict[str, Any] = json.loads(state_file.read_text())
    elif args.resume and tracked_state_file.is_file():
        state = json.loads(tracked_state_file.read_text())
    else:
        state = {}

    def save_state() -> None:
        txt = json.dumps(state, indent=2)
        state_file.write_text(txt)
        tracked_state_file.write_text(txt)

    named_init_probe   = canon_export_dir / f"{char_slug}_initial_visual_hull_source.glb"
    named_master_probe = canon_export_dir / f"{char_slug}_master.glb"
    named_lod0_probe   = canon_export_dir / f"{char_slug}.glb"
    named_lod1_probe   = canon_export_dir / f"{char_slug}_lod1.glb"
    named_lip_probe    = canon_export_dir / f"{char_slug}_lipsync.json"
    manifest_probe     = reports_dir / f"{char_slug}_manifest.json"

    all_code_deps = [
        in_path,
        cfg_path,
        REPO_ROOT / "avatar_pipeline/tools/reconstruct.py",
        REPO_ROOT / "avatar_pipeline/tools/rhubarb_bridge.py",
        REPO_ROOT / "avatar_pipeline/blender/build_character.py",
        REPO_ROOT / "avatar_pipeline/blender/rig_character.py",
        REPO_ROOT / "avatar_pipeline/blender/animate_character.py",
        REPO_ROOT / "avatar_pipeline/blender/render_review.py",
        REPO_ROOT / "avatar_pipeline/blender/optimize_and_export.py",
        REPO_ROOT / "avatar_pipeline/tools/validate_assets.py",
    ]
    if args.phonemes:
        all_code_deps.append((REPO_ROOT / args.phonemes if not pathlib.Path(args.phonemes).is_absolute() else pathlib.Path(args.phonemes)).resolve())
    elif args.audio:
        all_code_deps.append((REPO_ROOT / args.audio if not pathlib.Path(args.audio).is_absolute() else pathlib.Path(args.audio)).resolve())
    full_source_sig = sha256_files(all_code_deps)

    tracked_deliverables_ready = all(
        p.is_file()
        for p in (named_init_probe, named_master_probe, named_lod0_probe, named_lod1_probe, named_lip_probe, manifest_probe)
    ) and (
        args.skip_renders
        or all(
            (render_dir / r).is_file()
            for r in ("01_portrait_front.png", "09_material_preview.png", "11_cinematic_hero_portrait.png")
        )
    )

    if args.resume and tracked_deliverables_ready and state.get("_full_source_sig") == full_source_sig:
        stage_names = ["reconstruct", "lipsync", "build_character", "rig_character", "animate_character"]
        if not args.skip_renders:
            stage_names.append("render_review")
        stage_names.extend(["optimize_export", "validate_qa"])
        for sname in stage_names:
            print(f"[CACHE HIT] {sname}", flush=True)
        prev = json.loads(manifest_probe.read_text())
        qa0 = prev["qa_validation_lod0"]
        qa1 = prev["qa_validation_lod1"]
        exp_summary = prev["export_summary_and_roundtrip_reimport"]
        print(json.dumps({
            "pipeline_status": prev["pipeline_status"],
            "master_glb": str(named_master_probe),
            "lod0_glb": str(named_lod0_probe),
            "lod1_glb": str(named_lod1_probe),
            "manifest": str(manifest_probe),
            "report_md": str(reports_dir / f"{char_slug}_report.md"),
            "master_size_mb": exp_summary["master"]["size_mb"],
            "lod0_size_mb": qa0["file_size_mb"],
            "lod0_triangles": qa0["triangle_count"],
            "lod1_size_mb": qa1["file_size_mb"],
            "lod1_triangles": qa1["triangle_count"],
            "bones": qa0["skeleton_joint_count"],
            "morph_targets": qa0["morph_target_count"],
            "animations": qa0["animation_count"],
        }, indent=2))
        return

    def run_stage(name: str, deps: List[pathlib.Path], outputs: List[pathlib.Path], fn: Callable[[], None]) -> None:
        sig = sha256_files(deps)
        if args.resume and state.get(name) == sig and all(o.exists() for o in outputs):
            print(f"[CACHE HIT] {name}", flush=True)
            return
        print(f"[RUNNING]   {name}...", flush=True)
        t0 = time.time()
        fn()
        state[name] = sig
        save_state()
        print(f"[DONE]      {name} ({round(time.time() - t0, 2)}s)", flush=True)

    profile_json = workdir / "identity_profile.json"
    init_src_glb = workdir / "initial_visual_hull_source.glb"
    lipsync_json = workdir / "lipsync_active.json"
    built_blend  = workdir / "02_character_built.blend"
    rigged_blend = workdir / "03_character_rigged.blend"
    master_blend = workdir / "04_character_animated.blend"
    master_stage = stage_export_dir / "avatar_master_2048.glb"
    lod0_stage   = stage_export_dir / "avatar_mobile.glb"
    lod1_stage   = stage_export_dir / "avatar_mobile_lod1.glb"
    qa_lod0_json = workdir / "qa_validation_lod0.json"
    qa_lod1_json = workdir / "qa_validation_lod1.json"

    run_stage(
        "reconstruct",
        [in_path, REPO_ROOT / "avatar_pipeline/tools/reconstruct.py"],
        [profile_json, init_src_glb],
        lambda: run_cmd([str(VENV_PYTHON), "avatar_pipeline/tools/reconstruct.py", "--input", str(in_path), "--workdir", str(workdir), "--strict"]),
    )

    lip_cmd = [str(VENV_PYTHON), "avatar_pipeline/tools/rhubarb_bridge.py", "--lang", args.lang, "--out", str(lipsync_json)]
    lip_deps = [REPO_ROOT / "avatar_pipeline/tools/rhubarb_bridge.py"]
    if args.phonemes:
        pp = (REPO_ROOT / args.phonemes if not pathlib.Path(args.phonemes).is_absolute() else pathlib.Path(args.phonemes)).resolve()
        lip_cmd += ["--phonemes", str(pp)]
        lip_deps.append(pp)
    elif args.audio:
        ap_wav = (REPO_ROOT / args.audio if not pathlib.Path(args.audio).is_absolute() else pathlib.Path(args.audio)).resolve()
        lip_cmd += ["--audio", str(ap_wav)]
        lip_deps.append(ap_wav)
    run_stage("lipsync", lip_deps, [lipsync_json], lambda: run_cmd(lip_cmd))

    run_stage(
        "build_character",
        [profile_json, REPO_ROOT / "avatar_pipeline/blender/build_character.py"],
        [built_blend],
        lambda: run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/blender/build_character.py", ["--profile", str(profile_json), "--out", str(built_blend)])),
    )

    run_stage(
        "rig_character",
        [built_blend, REPO_ROOT / "avatar_pipeline/blender/rig_character.py"],
        [rigged_blend],
        lambda: run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/blender/rig_character.py", ["--in-blend", str(built_blend), "--out-blend", str(rigged_blend)])),
    )

    run_stage(
        "animate_character",
        [rigged_blend, lipsync_json, REPO_ROOT / "avatar_pipeline/blender/animate_character.py"],
        [master_blend],
        lambda: run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/blender/animate_character.py", ["--in-blend", str(rigged_blend), "--lipsync", str(lipsync_json), "--out-blend", str(master_blend)])),
    )

    if not args.skip_renders:
        run_stage(
            "render_review",
            [master_blend, cfg_path, REPO_ROOT / "avatar_pipeline/blender/render_review.py"],
            [render_dir / "01_portrait_front.png", render_dir / "09_material_preview.png", render_dir / "11_cinematic_hero_portrait.png"],
            lambda: run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/blender/render_review.py", ["--in-blend", str(master_blend), "--out-dir", str(render_dir), "--config", str(cfg_path)])),
        )

    run_stage(
        "optimize_export",
        [master_blend, profile_json, cfg_path, REPO_ROOT / "avatar_pipeline/blender/optimize_and_export.py"],
        [master_stage, lod0_stage, lod1_stage],
        lambda: run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/blender/optimize_and_export.py", ["--in-blend", str(master_blend), "--profile", str(profile_json), "--export-dir", str(stage_export_dir), "--config", str(cfg_path)])),
    )

    canon_lod0   = canon_export_dir / "avatar_mobile.glb"
    canon_lod1   = canon_export_dir / "avatar_mobile_lod1.glb"
    canon_lip    = canon_export_dir / "avatar_mobile_lipsync.json"
    named_init   = canon_export_dir / f"{char_slug}_initial_visual_hull_source.glb"
    named_master = canon_export_dir / f"{char_slug}_master.glb"
    named_blend  = canon_export_dir / f"{char_slug}_master.blend"
    named_lod0   = canon_export_dir / f"{char_slug}.glb"
    named_lod1   = canon_export_dir / f"{char_slug}_lod1.glb"
    named_lip    = canon_export_dir / f"{char_slug}_lipsync.json"
    lang_lip     = canon_export_dir / f"{char_slug}_lipsync_{args.lang}.json"

    shutil.copy2(init_src_glb, named_init)
    shutil.copy2(master_blend, named_blend)
    shutil.copy2(master_stage, named_master)
    shutil.copy2(lod0_stage, canon_lod0)
    shutil.copy2(lod1_stage, canon_lod1)
    shutil.copy2(lipsync_json, canon_lip)
    shutil.copy2(lod0_stage, named_lod0)
    shutil.copy2(lod1_stage, named_lod1)
    shutil.copy2(lipsync_json, named_lip)
    shutil.copy2(lipsync_json, lang_lip)

    profile_data = json.loads(profile_json.read_text())
    target_h = float(profile_data["anthropometrics"]["target_height_m"])

    run_stage(
        "validate_qa",
        [canon_lod0, canon_lod1, cfg_path, REPO_ROOT / "avatar_pipeline/tools/validate_assets.py"],
        [qa_lod0_json, qa_lod1_json],
        lambda: (
            run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/tools/validate_assets.py", ["--glb", str(canon_lod0), "--config", str(cfg_path), "--target-height", str(target_h), "--out", str(qa_lod0_json)])),
            run_cmd(blender_python_cmd(blender_bin, "avatar_pipeline/tools/validate_assets.py", ["--glb", str(canon_lod1), "--config", str(cfg_path), "--target-height", str(target_h), "--lod1", "--out", str(qa_lod1_json)])),
        ),
    )

    qa0 = json.loads(qa_lod0_json.read_text())
    qa1 = json.loads(qa_lod1_json.read_text())
    exp_summary = json.loads((stage_export_dir / "export_summary.json").read_text())

    manifest = {
        "pipeline_status": "PASS" if (qa0["passed"] and qa1["passed"]) else "FAIL",
        "character_slug": char_slug,
        "character_name": profile_data["features_and_palette"]["character_name"],
        "untouched_initial_reconstruction": profile_data["untouched_initial_reconstruction"],
        "exports": {
            "master_blend": str(named_blend),
            "master_glb": str(named_master),
            "lod0_glb": str(named_lod0),
            "lod1_glb": str(named_lod1),
            "canonical_lod0_glb": str(canon_lod0),
            "canonical_lod1_glb": str(canon_lod1),
            "lipsync_json": str(named_lip),
        },
        "export_summary_and_roundtrip_reimport": exp_summary,
        "qa_validation_lod0": qa0,
        "qa_validation_lod1": qa1,
    }
    manifest_path = reports_dir / f"{char_slug}_manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2))
    (reports_dir / "avatar_manifest.json").write_text(json.dumps(manifest, indent=2))
    state["_full_source_sig"] = full_source_sig
    save_state()

    report_md = reports_dir / f"{char_slug}_report.md"
    report_md.write_text(
        f"# 3D Mobile Avatar Build & QA Report — {profile_data['features_and_palette']['character_name']}\n\n"
        f"- **Pipeline Status**: `{manifest['pipeline_status']}`\n"
        f"- **Untouched Initial Source GLB (`initial_visual_hull_source.glb`)**: `{profile_data['untouched_initial_reconstruction']['triangle_count']}` tris, AssetFurnace `photo_paint` coverage `{profile_data['untouched_initial_reconstruction']['uv_texel_coverage_fraction']}`\n"
        f"- **Master Asset (`{named_master.name}`)**: `{exp_summary['master']['size_mb']} MB`, `{exp_summary['master']['triangles']}` tris, `2048x2048` PBR + `KHR_materials_variants` (`{', '.join(exp_summary['master']['khr_materials_variants'])}`)\n"
        f"- **Mobile LOD0 Asset (`{named_lod0.name}`)**: `{qa0['file_size_mb']} MB`, `{qa0['triangle_count']}` tris, `{qa0['material_count']}` material (`{qa0['draw_calls']}` draw calls)\n"
        f"- **Mobile LOD1 Asset (`{named_lod1.name}`)**: `{qa1['file_size_mb']} MB`, `{qa1['triangle_count']}` tris, `512x512` atlas\n"
        f"- **Rig & Animation**: `{qa0['skeleton_joint_count']}` joints (`<= 64` GLES limit), `{qa0['morph_target_count']}` morph targets, `{qa0['animation_count']}` animations\n"
        f"- **Blender Round-Trip Re-Import Verified**: `Master={exp_summary['master']['blender_roundtrip_reimport']['reimport_ok']}`, `LOD0={exp_summary['lod0']['blender_roundtrip_reimport']['reimport_ok']}`, `LOD1={exp_summary['lod1']['blender_roundtrip_reimport']['reimport_ok']}`\n"
        f"- **Metric Scale & Grounding**: height `{qa0['measured_height_m']} m` (target `{target_h} m`), floor `min_z = {qa0['min_z_m']} m`\n"
    )

    print(json.dumps({
        "pipeline_status": manifest["pipeline_status"],
        "master_glb": str(named_master),
        "lod0_glb": str(named_lod0),
        "lod1_glb": str(named_lod1),
        "manifest": str(manifest_path),
        "report_md": str(report_md),
        "master_size_mb": exp_summary["master"]["size_mb"],
        "lod0_size_mb": qa0["file_size_mb"],
        "lod0_triangles": qa0["triangle_count"],
        "lod1_size_mb": qa1["file_size_mb"],
        "lod1_triangles": qa1["triangle_count"],
        "bones": qa0["skeleton_joint_count"],
        "morph_targets": qa0["morph_target_count"],
        "animations": qa0["animation_count"],
    }, indent=2))


if __name__ == "__main__":
    main()
