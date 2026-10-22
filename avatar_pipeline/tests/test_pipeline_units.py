#!/usr/bin/env python3
"""
Fast, Blender-free tests for the pipeline tools.

    cd avatar_pipeline
    .venv/bin/python -m unittest tests.test_pipeline_units -v

These cover the parts that can be checked without a 9-second Blender run:
GLB validation, lip-sync parsing, manifest hashing and the photo validator.
The heavy stages (build, skin, animate, export) are covered by
`run_pipeline.sh`, which fails loudly on any gate.

Every test is written to fail when the tool silently stops doing its job -
a validator that always returns "pass" is worse than no validator.
"""
from __future__ import annotations

import json
import struct
import subprocess
import sys
import tempfile
import unittest
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
PY = sys.executable

sys.path.insert(0, str(TOOLS))

import lipsync          # noqa: E402
import validate_glb     # noqa: E402

GLB = ROOT / "exports" / "test01" / "avatar.glb"
REQUIRED_CLIPS = [
    "Idle_Breathing", "Idle_LookAround", "Blink_Natural",
    "Expression_Happy", "Expression_Sad", "Expression_Surprised",
    "Expression_Thinking", "Expression_Curious", "Expression_Frustrated",
    "Gesture_Explain", "Gesture_Greet", "Gesture_Point", "Gesture_Emphasize",
    "Talk_Neutral", "Talk_Expressive", "Walk", "Turn",
    "React_Positive", "React_Negative",
]


def run(args, **kw):
    return subprocess.run([PY, *args], capture_output=True, text=True, **kw)


class TestGlb(unittest.TestCase):
    """The shipped GLB must still satisfy the contract the Android runtime assumes."""

    @classmethod
    def setUpClass(cls):
        if not GLB.exists():
            raise unittest.SkipTest(f"{GLB} missing - run run_pipeline.sh first")
        cls.data = GLB.read_bytes()
        cls.js, cls.bin, cls.nbytes = validate_glb.load_glb(GLB)

    def test_magic_and_version(self):
        self.assertEqual(self.data[:4], b"glTF")
        self.assertEqual(struct.unpack("<I", self.data[4:8])[0], 2)

    def test_single_skin_with_named_joints(self):
        js = self.js
        self.assertEqual(len(js["skins"]), 1)
        joints = js["skins"][0]["joints"]
        self.assertGreaterEqual(len(joints), 20)
        names = [js["nodes"][j].get("name", "") for j in joints]
        for must in ("root", "pelvis", "spine", "chest", "neck", "head"):
            self.assertIn(must, names, f"joint {must} missing from the runtime skeleton")

    def test_morph_targets(self):
        prims = [p for m in self.js["meshes"] for p in m["primitives"]]
        skinned = [p for p in prims if "JOINTS_0" in p["attributes"]]
        self.assertTrue(skinned, "no skinned primitive")
        for p in skinned:
            self.assertIn("targets", p)
            self.assertGreaterEqual(len(p["targets"]), 10)
            self.assertIn("POSITION", p["targets"][0])

    def test_all_clips_present_and_non_empty(self):
        anims = self.js.get("animations", [])
        names = {a.get("name") for a in anims}
        missing = [c for c in REQUIRED_CLIPS if c not in names]
        self.assertEqual(missing, [], f"clips missing from GLB: {missing}")
        for a in anims:
            self.assertGreater(len(a["channels"]), 0, f"{a.get('name')} has no channels")
            self.assertGreater(len(a["samplers"]), 0)

    def test_no_external_resources(self):
        js = self.js
        for key in ("buffers", "images"):
            for item in js.get(key, []):
                self.assertNotIn("uri", item,
                                 f"{key} entry references an external file - not shippable")

    def test_size_budget(self):
        self.assertLess(len(self.data), 4 * 1024 * 1024)

    def test_validator_reports_pass(self):
        with tempfile.TemporaryDirectory() as td:
            out = Path(td) / "v.json"
            r = run([str(TOOLS / "validate_glb.py"), "--glb", str(GLB), "--report", str(out)])
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
            report = json.loads(out.read_text())
            self.assertEqual(report["status"], "pass")
            self.assertEqual(report["failed"], [])

    def test_validator_rejects_truncated_file(self):
        """A validator that cannot fail is not a validator."""
        with tempfile.TemporaryDirectory() as td:
            bad = Path(td) / "bad.glb"
            bad.write_bytes(self.data[:600])
            r = run([str(TOOLS / "validate_glb.py"), "--glb", str(bad)])
            self.assertNotEqual(r.returncode, 0)


class TestLipsync(unittest.TestCase):

    def _wav(self, td, seconds=1.0, rate=16000, amp=0.35, silent=False):
        path = Path(td) / "voice.wav"
        with wave.open(str(path), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(rate)
            frames = bytearray()
            n = int(rate * seconds)
            for i in range(n):
                # amplitude-modulated tone: an envelope with four clear bumps
                env = 0.0 if silent else 0.5 * (1.0 + (i // 2000) % 2)
                v = int(amp * env * 32767 * (1 if (i // 40) % 2 else -1))
                frames += struct.pack("<h", v)
            w.writeframes(bytes(frames))
        return path

    def test_energy_mode_segments(self):
        with tempfile.TemporaryDirectory() as td:
            wav = self._wav(td)
            out = Path(td) / "v.json"
            r = run([str(TOOLS / "lipsync.py"), "--audio", str(wav), "--out", str(out)])
            self.assertEqual(r.returncode, 0, r.stderr)
            data = json.loads(out.read_text())
            self.assertEqual(data["mode"], "energy")
            self.assertGreater(data["viseme_count"], 2)
            self.assertEqual(data["viseme_count"], len(data["keys"]))
            shapes = set(lipsync.RHUBARB_MAP.values())
            last_t = -1.0
            for k in data["keys"]:
                self.assertIn(k["viseme"], shapes)
                self.assertGreaterEqual(k["t"], 0.0)
                self.assertLessEqual(k["t"], data["duration_s"] + 1e-6)
                self.assertGreaterEqual(k["weight"], 0.0)
                self.assertLessEqual(k["weight"], 1.0)
                self.assertGreater(k["t"], last_t, "timeline must be monotonic")
                last_t = k["t"]

    def test_silence_does_not_produce_visemes(self):
        with tempfile.TemporaryDirectory() as td:
            wav = self._wav(td, silent=True)
            out = Path(td) / "v.json"
            r = run([str(TOOLS / "lipsync.py"), "--audio", str(wav), "--out", str(out)])
            data = json.loads(out.read_text())
            self.assertEqual(data["keys"], [])
            self.assertEqual(data["viseme_count"], 0)
            self.assertTrue(data["neutral_fallback"])

    def test_rhubarb_json_maps_every_shape(self):
        payload = {
            "mouthCues": [
                {"start": 0.0, "end": 0.12, "value": "A"},
                {"start": 0.12, "end": 0.30, "value": "E"},
                {"start": 0.30, "end": 0.42, "value": "X"},
            ]
        }
        with tempfile.TemporaryDirectory() as td:
            src = Path(td) / "r.json"
            src.write_text(json.dumps(payload))
            out = Path(td) / "v.json"
            r = run([str(TOOLS / "lipsync.py"), "--rhubarb-json", str(src), "--out", str(out)])
            self.assertEqual(r.returncode, 0, r.stderr)
            data = json.loads(out.read_text())
            self.assertEqual([k["viseme"] for k in data["keys"]],
                             ["viseme_AA", "viseme_O"])
            self.assertIn("english", (data.get("note") or "").lower())
            # "X" is silence and must not become a morph with weight 0
            self.assertEqual(data["rest_intervals_dropped"], 1)
            self.assertEqual(data["mode"], "rhubarb")

    def test_rhubarb_tsv(self):
        with tempfile.TemporaryDirectory() as td:
            src = Path(td) / "r.txt"
            src.write_text("0\t0.2\tB\n0.2\t0.4\tC\n")
            out = Path(td) / "v.json"
            r = run([str(TOOLS / "lipsync.py"), "--rhubarb-json", str(src), "--out", str(out)])
            self.assertEqual(r.returncode, 0, r.stderr)
            data = json.loads(out.read_text())
            self.assertEqual([k["viseme"] for k in data["keys"]],
                             ["viseme_MBP", "viseme_E"])
            self.assertEqual(data["mode"], "rhubarb")


class TestManifest(unittest.TestCase):

    def test_hashes_match_and_redaction(self):
        out = ROOT / "exports" / "test01" / "manifest.json"
        if not out.exists():
            self.skipTest("manifest not generated yet")
        m = json.loads(out.read_text())
        self.assertIn("artefacts", m)
        self.assertGreaterEqual(len(m["artefacts"]), 5)
        for name, a in m["artefacts"].items():
            p = ROOT / a["path"]
            self.assertTrue(p.exists(), f"{name} -> {a['path']} missing")
            self.assertEqual(len(a["sha256"]), 64)
        blob = json.dumps(m)
        self.assertNotIn("ghp_", blob)
        self.assertNotIn("github_pat_", blob)
        # the honesty fields the report relies on must be present and pessimistic
        self.assertTrue(m["status"]["android_runtime"].startswith("NOT TESTED"))
        self.assertTrue(m["status"]["photo_identity_match"].startswith("NO"))
        self.assertEqual(m["status"]["glb_validation"], "pass")


class TestAnalyzePhoto(unittest.TestCase):
    """The validator must reject unusable input rather than invent a face."""

    def _write_png(self, path, w=64, h=64, colour=(120, 120, 120)):
        import zlib
        raw = b""
        for _ in range(h):
            raw += b"\x00" + bytes(colour) * w
        def chunk(tag, data):
            c = tag + data
            return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(raw))
        png += chunk(b"IEND", b"")
        Path(path).write_bytes(png)

    def test_rejects_featureless_image(self):
        with tempfile.TemporaryDirectory() as td:
            img = Path(td) / "flat.png"
            self._write_png(img)
            out = Path(td) / "a.json"
            r = run([str(TOOLS / "analyze_photo.py"), "--image", str(img), "--out", str(out)])
            self.assertNotEqual(r.returncode, 0,
                                "an unusable photograph must fail loudly, not build a character")
            self.assertTrue(out.exists(), "the report must still be written so the reason is visible")
            data = json.loads(out.read_text())
            self.assertFalse(all(data["validation"].values()))
            self.assertTrue(data["limitations"])

    def test_rejects_missing_file(self):
        r = run([str(TOOLS / "analyze_photo.py"), "--image", "/nonexistent.jpg"])
        self.assertNotEqual(r.returncode, 0)


class TestArtInput(unittest.TestCase):
    """
    Concept art is not a photograph. The cascade is trained on faces and will either miss
    a drawing or hallucinate one, so the analyser must be able to fall back to the
    silhouette and say so, and must let the caller state the head count outright.
    """

    def _figure(self, path, alpha=False, w=420, h=760):
        import numpy as np
        import cv2
        img = np.full((h, w, 3), 245, np.uint8)
        skin, cloth, hair = (150, 190, 235), (90, 80, 180), (40, 45, 70)
        cx = w // 2
        cv2.circle(img, (cx, 120), 72, hair, -1)
        cv2.circle(img, (cx, 132), 62, skin, -1)
        cv2.rectangle(img, (cx - 58, 190), (cx + 58, 330), cloth, -1)
        cv2.rectangle(img, (cx - 95, 200), (cx - 62, 340), cloth, -1)
        cv2.rectangle(img, (cx + 62, 200), (cx + 95, 340), cloth, -1)
        cv2.rectangle(img, (cx - 48, 330), (cx - 8, 600), cloth, -1)
        cv2.rectangle(img, (cx + 8, 330), (cx + 48, 600), cloth, -1)
        cv2.ellipse(img, (cx - 24, 120), (10, 6), 0, 0, 360, (60, 60, 60), -1)
        cv2.ellipse(img, (cx + 24, 120), (10, 6), 0, 0, 360, (60, 60, 60), -1)
        if alpha:
            a = np.where(img.min(axis=2) < 240, 255, 0).astype(np.uint8)
            cv2.imwrite(str(path), np.dstack([img, a]))
        else:
            cv2.imwrite(str(path), img)
        return path

    def _analyse(self, img, *extra):
        with tempfile.TemporaryDirectory() as td:
            out = Path(td) / "a.json"
            r = run([str(TOOLS / "analyze_photo.py"), "--image", str(img),
                     "--out", str(out), *extra])
            self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
            return json.loads(out.read_text())

    def test_flat_background_art_uses_silhouette(self):
        with tempfile.TemporaryDirectory() as td:
            img = self._figure(Path(td) / "art.png")
            d = self._analyse(img)
            self.assertTrue(all(d["validation"].values()))
            self.assertIn(d["face_source"], ("silhouette", "haar"))
            self.assertEqual(d["silhouette"]["figure_type"], "full_body")
            self.assertGreater(d["silhouette"]["bbox"][3], 400)

    def test_cascade_disagreement_is_recorded_not_silently_trusted(self):
        with tempfile.TemporaryDirectory() as td:
            img = self._figure(Path(td) / "art.png")
            d = self._analyse(img)
            if d["face_source"] == "silhouette":
                self.assertTrue(d["rejected_faces"], "a rejected face must be explained")
                self.assertTrue(any("proportion" in r["reason"] or "height" in r["reason"]
                                    for r in d["rejected_faces"]))
                self.assertEqual(d["proportions"]["head_count_source"], "silhouette")
            self.assertGreaterEqual(d["proportions"]["heads_tall"], 2.5)
            self.assertLessEqual(d["proportions"]["heads_tall"], 9.0)

    def test_alpha_png_does_not_crash(self):
        """Regression: an `alpha` local once shadowed the argparse namespace."""
        with tempfile.TemporaryDirectory() as td:
            img = self._figure(Path(td) / "art_alpha.png", alpha=True)
            d = self._analyse(img)
            self.assertTrue(all(d["validation"].values()))
            self.assertIn(d["silhouette"]["source"], ("alpha", "background"))

    def test_head_count_override_wins(self):
        with tempfile.TemporaryDirectory() as td:
            img = self._figure(Path(td) / "art.png")
            d = self._analyse(img, "--heads-tall", "3.5", "--height-m", "1.2")
            prop = d["proportions"]
            self.assertEqual(prop["heads_tall"], 3.5)
            self.assertEqual(prop["head_count_source"], "caller")
            self.assertAlmostEqual(prop["estimated_person_metres"], 1.2, places=3)

    def test_face_box_override(self):
        with tempfile.TemporaryDirectory() as td:
            img = self._figure(Path(td) / "art.png")
            d = self._analyse(img, "--face-box", "168,60,84,144")
            self.assertEqual(d["face_source"], "manual")
            self.assertEqual(d["face"]["w"], 84)

    def test_photograph_still_uses_the_cascade(self):
        """The photo path must not regress while the art path is being added."""
        photo = ROOT / "tests" / "fixtures" / "portrait-astronaut.jpg"
        if not photo.exists():
            self.skipTest("fixture missing")
        d = self._analyse(photo)
        self.assertEqual(d["face_source"], "haar")
        self.assertEqual(d["silhouette"]["source"], "uneven_background")
        self.assertEqual(d["proportions"]["heads_tall"], 7.0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
