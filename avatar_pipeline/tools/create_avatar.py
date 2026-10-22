#!/usr/bin/env python3
"""
Single Deterministic End-to-End Orchestrator (`create_avatar.py`).

Executes all 8 stages with SHA-256 input+script checkpoint caching (`--resume`):
  1. `reconstruct`       -> `identity_profile.json` + PBR sub-textures
  2. `lipsync`           -> `lipsync_active.json`
  3. `build_character`   -> `02_character_built.blend`
  4. `rig_character`     -> `03_character_rigged.blend` (`60` joints, `26` morph targets)
  5. `animate_character` -> `04_character_animated.blend` (`24` clips)
  6. `render_review`     -> `renders/01..09_*.png` (Cycles + OpenImageDenoise)
  7. `optimize_export`   -> `exports/avatar_mobile.glb` (`LOD0`) + `avatar_mobile_lod1.glb` (`LOD1`)
  8. `validate_qa`       -> 11-gate round-trip glTF 2.0 validation + `<name>_manifest.json` + `<name>_report.md`
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


def sha256_files(paths: List[pathlib.Path]) -> str:
    h = hashlib.sha256()
    for p in paths:
        if p.is_file():
            h.update(p.read_bytes())
    return h.hexdigest()


def run_cmd(cmd: List[str]) -> str:
    env = os.environ.copy()
    env["LD_LIBRARY_PATH"] = f"/home/user/.local/lib:{env.get('LD_LIBRARY_PATH', '')}"
    env["PATH"] = f"/home/user/.local/bin:{env.get('PATH', '')}"
    env["BAS_SKIP_GPU"] = "1"
    proc = subprocess.run(cmd, cwd=str(REPO_ROOT), env=env, text=True, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError(
            f"Command failed (exit {proc.returncode}): {' '.join(cmd)}\nSTDOUT:\n{proc.stdout}\nSTDERR:\n{proc.stderr}"
        )
    return proc.stdout


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
    state: Dict[str, Any] = json.loads(state_file.read_text()) if (args.resume and state_file.is_file()) else {}

    def run_stage(name: str, deps: List[pathlib.Path], outputs: List[pathlib.Path], fn: Callable[[], None]) -> None:
        sig = sha256_files(deps)
        if args.resume and state.get(name) == sig and all(o.exists() for o in outputs):
            print(f"[CACHE HIT] {name}", flush=True)
            return
        print(f"[RUNNING]   {name}...", flush=True)
        t0 = time.time()
        fn()
        state[name] = sig
        state_file.write_text(json.dumps(state, indent=2))
        print(f"[DONE]      {name} ({round(time.time() - t0, 2)}s)", flush=True)

    profile_json = workdir / "identity_profile.json"
    lipsync_json = workdir / "lipsync_active.json"
    built_blend  = workdir / "02_character_built.blend"
    rigged_blend = workdir / "03_character_rigged.blend"
    master_blend = workdir / "04_character_animated.blend"
    lod0_stage   = stage_export_dir / "avatar_mobile.glb"
    lod1_stage   = stage_export_dir / "avatar_mobile_lod1.glb"
    qa_lod0_json = workdir / "qa_validation_lod0.json"
    qa_lod1_json = workdir / "qa_validation_lod1.json"

    # Stage 1: Reconstruct
    run_stage(
        "reconstruct",
        [in_path, REPO_ROOT / "avatar_pipeline/tools/reconstruct.py"],
        [profile_json],
        lambda: run_cmd([str(VENV_PYTHON), "avatar_pipeline/tools/reconstruct.py", "--input", str(in_path), "--workdir", str(workdir), "--strict"]),
    )

    # Stage 2: Lip-Sync
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

    # Stage 3: Build Character
    run_stage(
        "build_character",
        [profile_json, REPO_ROOT / "avatar_pipeline/blender/build_character.py"],
        [built_blend],
        lambda: run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/blender/build_character.py", "--", "--profile", str(profile_json), "--out", str(built_blend)]),
    )

    # Stage 4: Rig Character (`60` bones + `26` morph targets)
    run_stage(
        "rig_character",
        [built_blend, REPO_ROOT / "avatar_pipeline/blender/rig_character.py"],
        [rigged_blend],
        lambda: run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/blender/rig_character.py", "--", "--in-blend", str(built_blend), "--out-blend", str(rigged_blend)]),
    )

    # Stage 5: Animate Character (`24` NLA clips)
    run_stage(
        "animate_character",
        [rigged_blend, lipsync_json, REPO_ROOT / "avatar_pipeline/blender/animate_character.py"],
        [master_blend],
        lambda: run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/blender/animate_character.py", "--", "--in-blend", str(rigged_blend), "--lipsync", str(lipsync_json), "--out-blend", str(master_blend)]),
    )

    # Stage 6: 9-Sheet Cycles Visual QA Review Renders
    if not args.skip_renders:
        run_stage(
            "render_review",
            [master_blend, cfg_path, REPO_ROOT / "avatar_pipeline/blender/render_review.py"],
            [render_dir / "01_portrait_front.png", render_dir / "09_material_preview.png"],
            lambda: run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/blender/render_review.py", "--", "--in-blend", str(master_blend), "--out-dir", str(render_dir), "--config", str(cfg_path)]),
        )

    # Stage 7: Optimize Single PBR Atlas & Export `LOD0` + `LOD1` `.glb`
    run_stage(
        "optimize_export",
        [master_blend, profile_json, cfg_path, REPO_ROOT / "avatar_pipeline/blender/optimize_and_export.py"],
        [lod0_stage, lod1_stage],
        lambda: run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/blender/optimize_and_export.py", "--", "--in-blend", str(master_blend), "--profile", str(profile_json), "--export-dir", str(stage_export_dir), "--config", str(cfg_path)]),
    )

    # Copy to canonical mobile export paths & character-named paths
    canon_lod0 = canon_export_dir / "avatar_mobile.glb"
    canon_lod1 = canon_export_dir / "avatar_mobile_lod1.glb"
    canon_lip  = canon_export_dir / "avatar_mobile_lipsync.json"
    named_lod0 = canon_export_dir / f"{char_slug}.glb"
    named_lod1 = canon_export_dir / f"{char_slug}_lod1.glb"
    named_lip  = canon_export_dir / f"{char_slug}_lipsync.json"
    lang_lip   = canon_export_dir / f"{char_slug}_lipsync_{args.lang}.json"
    shutil.copy2(lod0_stage, canon_lod0)
    shutil.copy2(lod1_stage, canon_lod1)
    shutil.copy2(lipsync_json, canon_lip)
    shutil.copy2(lod0_stage, named_lod0)
    shutil.copy2(lod1_stage, named_lod1)
    shutil.copy2(lipsync_json, named_lip)
    shutil.copy2(lipsync_json, lang_lip)

    profile_data = json.loads(profile_json.read_text())
    target_h = float(profile_data["anthropometrics"]["target_height_m"])

    # Stage 8: 11-Gate Round-Trip Validation QA
    run_stage(
        "validate_qa",
        [canon_lod0, canon_lod1, cfg_path, REPO_ROOT / "avatar_pipeline/tools/validate_assets.py"],
        [qa_lod0_json, qa_lod1_json],
        lambda: (
            run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/tools/validate_assets.py", "--", "--glb", str(canon_lod0), "--config", str(cfg_path), "--target-height", str(target_h), "--out", str(qa_lod0_json)]),
            run_cmd([str(blender_bin), "-b", "--factory-startup", "-P", "avatar_pipeline/tools/validate_assets.py", "--", "--glb", str(canon_lod1), "--config", str(cfg_path), "--target-height", str(target_h), "--lod1", "--out", str(qa_lod1_json)]),
        ),
    )

    qa0 = json.loads(qa_lod0_json.read_text())
    qa1 = json.loads(qa_lod1_json.read_text())

    manifest = {
        "pipeline_status": "PASS" if (qa0["passed"] and qa1["passed"]) else "FAIL",
        "character_slug": char_slug,
        "character_name": profile_data["features_and_palette"]["character_name"],
        "exports": {
            "lod0_glb": str(named_lod0),
            "lod1_glb": str(named_lod1),
            "canonical_lod0_glb": str(canon_lod0),
            "canonical_lod1_glb": str(canon_lod1),
            "lipsync_json": str(named_lip),
        },
        "qa_validation_lod0": qa0,
        "qa_validation_lod1": qa1,
    }
    manifest_path = reports_dir / f"{char_slug}_manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2))
    (reports_dir / "avatar_manifest.json").write_text(json.dumps(manifest, indent=2))

    report_md = reports_dir / f"{char_slug}_report.md"
    report_md.write_text(
        f"# 3D Mobile Avatar Build & QA Report — {profile_data['features_and_palette']['character_name']}\n\n"
        f"- **Pipeline Status**: `{manifest['pipeline_status']}`\n"
        f"- **LOD0 Asset (`{named_lod0.name}`)**: `{qa0['file_size_mb']} MB`, `{qa0['triangle_count']}` tris, `{qa0['material_count']}` material (`{qa0['draw_calls']}` draw calls)\n"
        f"- **LOD1 Asset (`{named_lod1.name}`)**: `{qa1['file_size_mb']} MB`, `{qa1['triangle_count']}` tris, `512x512` atlas\n"
        f"- **Rig & Animation**: `{qa0['skeleton_joint_count']}` joints (`<= 64` GLES limit), `{qa0['morph_target_count']}` morph targets, `{qa0['animation_count']}` animations\n"
        f"- **Metric Scale & Grounding**: height `{qa0['measured_height_m']} m` (target `{target_h} m`), floor `min_z = {qa0['min_z_m']} m`\n"
    )

    print(json.dumps({
        "pipeline_status": manifest["pipeline_status"],
        "lod0_glb": str(named_lod0),
        "lod1_glb": str(named_lod1),
        "manifest": str(manifest_path),
        "report_md": str(report_md),
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
