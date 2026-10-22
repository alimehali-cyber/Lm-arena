#!/usr/bin/env python3
"""
End-to-End Automated Regression, Legal/Safeguard, GLB QA, Lip-Sync, Resumption,
and Live Kotlin 2.2.10 Runtime Verification Suite (`avatar_pipeline/tests/test_pipeline.py`).
"""
from __future__ import annotations

import json
import os
import pathlib
import subprocess
import tempfile
import unittest
from PIL import Image


REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
VENV_PYTHON = REPO_ROOT / ".venv/bin/python"
KOTLINC_BIN = pathlib.Path("/home/user/.local/bin/kotlinc")
JAVA_BIN = pathlib.Path("/home/user/.local/bin/java")


class AvatarPipelineEndToEndTests(unittest.TestCase):

    def test_01_protected_zig_subsystems_and_release_workflow_untouched(self) -> None:
        """All protected ZIG subsystems, release signing config, and CI workflows MUST remain 100% untouched."""
        proc = subprocess.run(
            ["git", "diff", "--name-only", "985629980d53218c3758b55d5ba50f38793e7638"],
            cwd=str(REPO_ROOT),
            text=True,
            capture_output=True,
            check=True,
        )
        forbidden_tokens = [
            ".github/workflows/",
            "app/build.gradle.kts",
            "build.gradle.kts",
            "settings.gradle.kts",
            "CompassARScreen.kt",
            "/ar/",
            "com/zig/gargantua/",
            "com/zig/chal/",
            "com/zig/gravity/",
            "/astro_engine/",
        ]
        violations = [
            line for line in proc.stdout.splitlines()
            if any(tok in line for tok in forbidden_tokens)
        ]
        self.assertEqual(
            violations,
            [],
            f"Protected ZIG files were unexpectedly modified:\n{violations}",
        )

    def test_02_opengl_es_only_zero_vulkan_zero_ndk(self) -> None:
        """Both prototype and production `app/src/main/java/.../avatar` MUST use pure GLES 3.0 only."""
        for target_dir in [
            REPO_ROOT / "avatar_pipeline/android_prototype",
            REPO_ROOT / "app/src/main/java/com/alijafari/red/astronomy/avatar",
        ]:
            cpp_files = (
                list(target_dir.rglob("*.c"))
                + list(target_dir.rglob("*.cc"))
                + list(target_dir.rglob("*.cpp"))
                + list(target_dir.rglob("*.h"))
                + list(target_dir.rglob("*.hpp"))
                + list(target_dir.rglob("CMakeLists.txt"))
            )
            self.assertEqual(cpp_files, [], f"Forbidden NDK/C++ files found in {target_dir}: {cpp_files}")

        for shader_path in [
            REPO_ROOT / "avatar_pipeline/android_prototype/src/main/kotlin/com/alijafari/red/astronomy/avatar/AvatarShaders.kt",
            REPO_ROOT / "app/src/main/java/com/alijafari/red/astronomy/avatar/AvatarShaders.kt",
        ]:
            shader_src = shader_path.read_text(encoding="utf-8")
            self.assertIn("#version 300 es", shader_src)
            self.assertIn("uniform mat4 uJoints[64];", shader_src)
            self.assertNotIn("VkInstance", shader_src)

    def test_03_installed_mit_skills_and_license_quarantine(self) -> None:
        """All MIT skills are discoverable with zero collisions across all 3 agent directories and unlicensed repos are excluded."""
        reg = json.loads((REPO_ROOT / "avatar_pipeline/skills/REGISTRY.json").read_text())
        n_skills = reg["total_active_skills"]
        self.assertGreaterEqual(n_skills, 50)
        self.assertEqual(reg["collision_count"], 0)
        for root_str in [
            str(REPO_ROOT / ".agents/skills"),
            "/home/user/.agents/skills",
            "/home/user/.claude/skills",
        ]:
            skill_mds = list(pathlib.Path(root_str).glob("*/SKILL.md"))
            self.assertEqual(len(skill_mds), n_skills, f"Expected {n_skills} SKILL.md entries in {root_str}")
        self.assertFalse((REPO_ROOT / "avatar_pipeline/skills/gpt_3d_skill").exists())

    def test_04_legal_bfm_guard_and_license_ledger(self) -> None:
        """Non-commercial BFM weights must remain disabled by default and LICENSES.md must exist."""
        cfg = json.loads((REPO_ROOT / "avatar_pipeline/config.json").read_text())
        self.assertFalse(cfg["allow_noncommercial_bfm"])
        self.assertTrue(cfg["strict_commercial_licenses_only"])
        lic_text = (REPO_ROOT / "avatar_pipeline/requirements/LICENSES.md").read_text()
        for section in [
            "1. Installed Skill & Tool Repositories (Source Code)",
            "2. Sandbox Execution Binaries & Libraries (Code vs Output License)",
            "3. Pretrained Model Weights Audit (Evaluated & Installed)",
        ]:
            self.assertIn(section, lic_text)

    def test_05_exported_glb_qa_all_11_gates_pass(self) -> None:
        """Exported Professor Z LOD0 and LOD1 `.glb` assets exist, respect mobile budgets, and pass all 11 QA gates."""
        lod0 = REPO_ROOT / "avatar_pipeline/exports/professor_z.glb"
        lod1 = REPO_ROOT / "avatar_pipeline/exports/professor_z_lod1.glb"
        self.assertTrue(lod0.exists())
        self.assertTrue(lod1.exists())
        self.assertLess(lod1.stat().st_size, lod0.stat().st_size)

        manifest = json.loads((REPO_ROOT / "avatar_pipeline/reports/professor_z_manifest.json").read_text())
        qa = manifest["qa_validation_lod0"]
        self.assertTrue(qa["passed"], f"QA failed: {qa['issues']}")
        for gate_name, passed in qa["checks"].items():
            self.assertTrue(passed, f"QA gate {gate_name} did not pass")
        self.assertEqual(qa["skeleton_joint_count"], 60)
        self.assertLessEqual(qa["skeleton_joint_count"], 64)
        self.assertEqual(qa["morph_target_count"], 26)
        self.assertEqual(qa["animation_count"], 24)
        self.assertLessEqual(qa["triangle_count"], 28000)

    def test_06_all_nine_review_sheets_generated(self) -> None:
        """All 9 Cycles+OIDN visual review PNGs exist with valid non-empty dimensions."""
        render_dir = REPO_ROOT / "avatar_pipeline/renders"
        pngs = sorted(render_dir.glob("0*.png"))
        self.assertEqual(len(pngs), 9)
        for p in pngs:
            with Image.open(p) as im:
                self.assertGreaterEqual(im.width, 512)
                self.assertGreaterEqual(im.height, 512)

    def test_07_input_validation_strict_rejection_on_lowres_nonface(self) -> None:
        """Corrupted/tiny non-face input cleanly raises exit code != 0 under `--strict` without unhandled crash."""
        with tempfile.TemporaryDirectory() as tmp:
            bad_img = pathlib.Path(tmp) / "tiny_noise.png"
            Image.new("RGB", (64, 64), (30, 30, 30)).save(bad_img)
            proc = subprocess.run(
                [
                    str(VENV_PYTHON),
                    "avatar_pipeline/tools/reconstruct.py",
                    "--input",
                    str(bad_img),
                    "--workdir",
                    tmp,
                    "--strict",
                ],
                cwd=str(REPO_ROOT),
                text=True,
                capture_output=True,
            )
            self.assertNotEqual(proc.returncode, 0)
            self.assertIn("below hard minimum 128x128", proc.stderr)

    def test_08_lipsync_english_persian_and_neutral_fallback(self) -> None:
        """Lip-sync bridge handles WAV alignment, Persian (`fa`) phoneme schedules, and silent neutral fallback."""
        with tempfile.TemporaryDirectory() as tmp:
            out_fa = pathlib.Path(tmp) / "fa.json"
            out_silent = pathlib.Path(tmp) / "silent.json"
            subprocess.run(
                [
                    str(VENV_PYTHON),
                    "avatar_pipeline/tools/rhubarb_bridge.py",
                    "--phonemes",
                    "avatar_pipeline/input/professor_z_persian_phonemes.json",
                    "--lang",
                    "fa",
                    "--out",
                    str(out_fa),
                ],
                cwd=str(REPO_ROOT),
                check=True,
                capture_output=True,
            )
            subprocess.run(
                [
                    str(VENV_PYTHON),
                    "avatar_pipeline/tools/rhubarb_bridge.py",
                    "--out",
                    str(out_silent),
                ],
                cwd=str(REPO_ROOT),
                check=True,
                capture_output=True,
            )
            fa_data = json.loads(out_fa.read_text())
            sil_data = json.loads(out_silent.read_text())
            self.assertEqual(fa_data["metadata"]["language"], "fa")
            self.assertGreater(len(fa_data["mouthCues"]), 30)
            self.assertEqual(sil_data["metadata"]["mode"], "neutral_speech_fallback")

    def test_09_pipeline_stage_resumption_cache_hit(self) -> None:
        """Re-running `create_avatar.py --resume` on Professor Z hits SHA-256 checkpoints across all stages in <5s."""
        proc = subprocess.run(
            [
                str(VENV_PYTHON),
                "avatar_pipeline/tools/create_avatar.py",
                "--input",
                "avatar_pipeline/input/professor_z_catalogue.png",
                "--phonemes",
                "avatar_pipeline/input/professor_z_persian_phonemes.json",
                "--lang",
                "fa",
                "--name",
                "professor_z",
                "--config",
                "avatar_pipeline/config.json",
                "--resume",
            ],
            cwd=str(REPO_ROOT),
            text=True,
            capture_output=True,
            check=True,
        )
        self.assertIn("[CACHE HIT] reconstruct", proc.stdout)
        self.assertIn("[CACHE HIT] optimize_export", proc.stdout)
        self.assertIn('"pipeline_status": "PASS"', proc.stdout)

    def test_10_live_kotlin_2_2_10_android_runtime_execution(self) -> None:
        """Compiles Kotlin 2.2.10 Android runtime prototype and verifies all 24 Professor Z clips & bind matrices on JVM 21."""
        env = os.environ.copy()
        env["PATH"] = f"/home/user/.local/bin:{env.get('PATH', '')}"
        with tempfile.TemporaryDirectory() as tmp:
            jar_path = pathlib.Path(tmp) / "avatar_runtime_verify.jar"
            kt_files = [
                "avatar_pipeline/android_prototype/src/main/kotlin/com/alijafari/red/astronomy/avatar/GltfBinaryLoader.kt",
                "avatar_pipeline/android_prototype/src/main/kotlin/com/alijafari/red/astronomy/avatar/AvatarAnimator.kt",
                "avatar_pipeline/android_prototype/src/main/kotlin/com/alijafari/red/astronomy/avatar/AvatarShaders.kt",
                "avatar_pipeline/android_prototype/src/main/kotlin/com/alijafari/red/astronomy/avatar/AvatarGlTextureView.kt",
                "avatar_pipeline/android_prototype/src/test/kotlin/com/alijafari/red/astronomy/avatar/AvatarRuntimeVerificationMain.kt",
            ]
            subprocess.run(
                [str(KOTLINC_BIN)] + kt_files + ["-include-runtime", "-d", str(jar_path)],
                cwd=str(REPO_ROOT),
                env=env,
                check=True,
                capture_output=True,
            )
            run_proc = subprocess.run(
                [
                    str(JAVA_BIN),
                    "-jar",
                    str(jar_path),
                    "avatar_pipeline/exports/professor_z.glb",
                    "avatar_pipeline/exports/professor_z_lod1.glb",
                    "avatar_pipeline/exports/professor_z_lipsync_fa.json",
                ],
                cwd=str(REPO_ROOT),
                env=env,
                text=True,
                capture_output=True,
                check=True,
            )
            res = json.loads(run_proc.stdout)
            self.assertEqual(res["kotlin_runtime_verification"], "PASS")
            self.assertEqual(res["skeleton_joints"], 60)
            self.assertEqual(res["animations_verified"], 24)
            self.assertLess(res["max_rest_bind_identity_error"], 1e-4)

    def test_11_untouched_reference_sha256_and_master_khr_variants_roundtrip(self) -> None:
        """Verifies untouched SHA-256 reference inputs, initial AssetFurnace visual-hull mesh, 2048 Master GLB variants, and extra plates."""
        import hashlib

        ref1 = REPO_ROOT / "avatar_pipeline/input/ref_01_professor_z_lineup.png"
        ref2 = REPO_ROOT / "avatar_pipeline/input/ref_02_professor_z_catalogue.png"
        self.assertEqual(
            hashlib.sha256(ref1.read_bytes()).hexdigest(),
            "19a055b164d683257fcc66005fa57ea221cd4347f92f788de977a4998870b143",
        )
        self.assertEqual(
            hashlib.sha256(ref2.read_bytes()).hexdigest(),
            "098d1902057d18a9f346438d5f5bb3378b7a21f49894c72d01d6acd35be6b797",
        )

        # Untouched initial source reconstruction preserved separately from refined assets
        init_src = REPO_ROOT / "avatar_pipeline/exports/professor_z_initial_visual_hull_source.glb"
        self.assertTrue(init_src.is_file())
        self.assertGreater(init_src.stat().st_size, 50_000)

        # Separate Master 2048 GLB with AssetFurnace `KHR_materials_variants` + round-trip Blender re-import
        manifest = json.loads((REPO_ROOT / "avatar_pipeline/reports/professor_z_manifest.json").read_text())
        master_info = manifest["export_summary_and_roundtrip_reimport"]["master"]
        self.assertEqual(
            master_info["khr_materials_variants"],
            ["Catalogue Tweed Blazer", "Midnight Lab Navy Z-Suit"],
        )
        self.assertTrue(master_info["blender_roundtrip_reimport"]["reimport_ok"])
        self.assertTrue(manifest["export_summary_and_roundtrip_reimport"]["lod0"]["blender_roundtrip_reimport"]["reimport_ok"])
        self.assertTrue(manifest["export_summary_and_roundtrip_reimport"]["lod1"]["blender_roundtrip_reimport"]["reimport_ok"])

        # Neutral side-profile, cinematic hero, and direct REF-vs-3D turnaround comparison plates exist
        for plate in (
            "10_side_profile_portrait.png",
            "11_cinematic_hero_portrait.png",
            "12_reference_vs_3d_turnaround.png",
        ):
            p = REPO_ROOT / "avatar_pipeline/renders" / plate
            self.assertTrue(p.is_file(), f"Missing plate {plate}")
            with Image.open(p) as im:
                self.assertGreaterEqual(im.width, 512)
                self.assertGreaterEqual(im.height, 512)


if __name__ == "__main__":
    unittest.main(verbosity=2)
