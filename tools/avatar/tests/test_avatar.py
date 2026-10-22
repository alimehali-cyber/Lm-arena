"""Tests for the authored-avatar route. Standard library only, so they run anywhere:

    python3 -m unittest discover -s tools/avatar/tests -v
    python3 tools/avatar/tests/test_avatar.py

The rules this file enforces are the ones that actually break a character downstream: bone names,
weights, clip loops, the GLB's own bytes, and whether a photographed silhouette measures into a
plausible human being.
"""

from __future__ import annotations

import json
import math
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "avatar"))

import avatar_core  # noqa: E402
import glb_check  # noqa: E402
import glb_export  # noqa: E402
import make_test_photo  # noqa: E402
import motion  # noqa: E402
import photo_ingest  # noqa: E402
from avatar_core import BONE_NAMES, PARENTS, Proportions, build_avatar, build_skeleton, default_palette  # noqa: E402
from motion import CLIPS, CLIPS_BY_NAME, IDENTITY, q_from_to, q_mul, q_rotate  # noqa: E402


def avatar():
    return build_avatar(Proportions(), default_palette())


class SkeletonTest(unittest.TestCase):
    def test_bone_names_are_the_contract(self):
        bones = build_skeleton(Proportions())
        self.assertEqual(tuple(b.name for b in bones), BONE_NAMES)
        for bone in bones:
            if bone.parent is not None:
                self.assertIn(bone.parent, BONE_NAMES)
            self.assertLess(np.linalg.norm(bone.tail - bone.head), 0.6, bone.name)

    def test_only_the_hips_have_no_parent(self):
        self.assertEqual([name for name, parent in PARENTS.items() if parent is None], ["hips"])

    def test_bones_are_human_sized(self):
        bones = {b.name: b for b in build_skeleton(Proportions(height_m=1.72))}
        self.assertAlmostEqual(bones["head"].tail[1], 1.72, delta=0.02)
        self.assertLess(bones["head"].tail[1], 1.76)
        self.assertGreater(bones["hips"].head[1], bones["thigh.L"].head[1] - 1e-9)
        self.assertGreater(bones["shin.L"].tail[1], 0.0)
        # The character's left is +X.
        self.assertGreater(bones["thigh.L"].head[0], 0.0)
        self.assertLess(bones["thigh.R"].head[0], 0.0)

    def test_inverse_bind_matrices_invert_the_rest_pose(self):
        bones = build_skeleton(Proportions())
        ibm = avatar_core.inverse_bind_matrices(bones).reshape(len(bones), 4, 4).transpose(0, 2, 1)
        for bone, matrix in zip(bones, ibm):
            self.assertTrue(np.allclose(matrix[:3, :3], np.eye(3)))
            self.assertTrue(np.allclose(matrix[:3, 3], -bone.head))


class MeshTest(unittest.TestCase):
    def test_weights_are_normalised_and_joints_in_range(self):
        mesh = avatar().mesh
        sums = mesh.weights.sum(axis=1)
        self.assertLess(float(np.abs(sums - 1.0).max()), 1e-6)
        self.assertGreaterEqual(int(mesh.joints.min()), 0)
        self.assertLess(int(mesh.joints.max()), len(BONE_NAMES))
        self.assertTrue(np.all(mesh.weights >= 0.0))

    def test_mesh_is_inside_the_game_budget(self):
        mesh = avatar().mesh
        self.assertLessEqual(mesh.triangle_count, 12000)
        self.assertGreater(mesh.triangle_count, 400, "too coarse to read as a person")
        self.assertEqual(mesh.positions.shape[0], mesh.weights.shape[0])

    def test_normals_are_unit_length(self):
        mesh = avatar().mesh
        lengths = np.linalg.norm(mesh.normals, axis=1)
        self.assertTrue(np.allclose(lengths, 1.0, atol=1e-6))

    def test_every_vertex_follows_a_nearby_bone(self):
        # Regression guard. Culling degenerate faces once reindexed the mesh without reindexing the
        # per-vertex bone allowlists, and a thigh ended up skinned to the forearm: the character tore
        # open into a slab from hip to knee on every animated frame.
        mesh = avatar().mesh
        heads = {b.name: b.head for b in avatar().skeleton}
        worst = 0.0
        for i in range(0, mesh.positions.shape[0], 7):
            dominant = BONE_NAMES[int(mesh.joints[i][0])]
            gap = float(np.linalg.norm(mesh.positions[i] - heads[dominant]))
            worst = max(worst, gap)
        self.assertLess(worst, 0.42, f"a vertex is {worst:.2f} m from the bone that drives it")

    def test_no_degenerate_triangles_reach_the_exporter(self):
        mesh = avatar().mesh
        tri = mesh.indices.reshape(-1, 3)
        a, b, c = (mesh.positions[tri[:, k]] for k in range(3))
        area = 0.5 * np.linalg.norm(np.cross(b - a, c - a), axis=1)
        self.assertGreater(float(area.min()), 1e-9, "a zero-area triangle will render as an artefact")

    def test_the_figure_is_a_plausible_height_and_stands_on_the_ground(self):
        mesh = avatar().mesh
        self.assertAlmostEqual(float(mesh.positions[:, 1].max()), 1.72, delta=0.06)
        self.assertLess(float(mesh.positions[:, 1].min()), 0.03)
        # Wider than tall nowhere: a shoulder width over half the height would be a mistake.
        widths = mesh.positions[:, 0].max() - mesh.positions[:, 0].min()
        self.assertLess(float(widths), 0.9)

    def test_vertex_colours_are_albedo(self):
        mesh = avatar().mesh
        self.assertEqual(mesh.colors.shape, mesh.positions.shape)
        self.assertTrue(np.all(mesh.colors >= 0.0) and np.all(mesh.colors <= 1.0))


class QuaternionTest(unittest.TestCase):
    def test_from_to_maps_the_first_direction_onto_the_second(self):
        a = np.array([0.0, -1.0, 0.0])
        for b in ([0.0, 1.0, 0.0], [1.0, 0.0, 0.0], [0.0, 0.0, -1.0], [0.3, 0.5, 0.8]):
            rotated = q_rotate(q_from_to(a, b), a)
            self.assertTrue(np.allclose(rotated, np.array(b) / np.linalg.norm(b), atol=1e-6), b)

    def test_identity_is_neutral(self):
        self.assertTrue(np.allclose(q_mul(IDENTITY, IDENTITY), IDENTITY))


class MotionTest(unittest.TestCase):
    def test_every_clip_poses_every_bone(self):
        for clip in CLIPS:
            pose = clip.pose(clip.length * 0.5)
            for bone in BONE_NAMES:
                q = pose.get(bone, IDENTITY)
                self.assertEqual(q.shape, (4,), f"{clip.name}/{bone}")
                self.assertAlmostEqual(float(np.linalg.norm(q)), 1.0, places=5, msg=f"{clip.name}/{bone}")

    def test_loops_are_seamless(self):
        for clip in CLIPS:
            if not clip.loop:
                continue
            self.assertLess(motion.loop_error(clip, BONE_NAMES), 0.5, clip.name)

    def test_clip_lengths_and_names_are_the_contract(self):
        self.assertEqual(
            [c.name for c in CLIPS],
            ["idle_breathe", "day_shade", "dusk_phone", "night_point", "wave_hello"],
        )
        for clip in CLIPS:
            self.assertLessEqual(clip.length, 8.0)
            self.assertEqual(clip.frames, int(round(clip.length * motion.FPS)) + 1)

    def test_the_night_pose_raises_the_right_hand_above_the_head(self):
        """The one pose a viewer will judge instantly: is the arm actually up at the sky?"""
        import preview

        av = avatar()
        for clip_name, check in (
            ("night_point", lambda hand, head: hand[1] > head[1] + 0.12),
            ("day_shade", lambda hand, head: abs(hand[0] - head[0]) < 0.16 and hand[1] > head[1] + 0.03),
        ):
            clip = CLIPS_BY_NAME[clip_name]
            pose = clip.pose(1.0)
            world = preview.bone_world_transforms(av, pose)
            hand = world["hand.R"][0:3, 3]
            head = world["head"][0:3, 3]
            self.assertTrue(check(hand, head), f"{clip_name}: hand {hand}, head {head}")

    def test_the_phone_pose_brings_both_hands_together_in_front(self):
        import preview

        av = avatar()
        world = preview.bone_world_transforms(av, CLIPS_BY_NAME["dusk_phone"].pose(1.0))
        left = world["hand.L"][0:3, 3]
        right = world["hand.R"][0:3, 3]
        self.assertLess(np.linalg.norm(left - right), 0.24, "hands are meant to meet around a phone")
        self.assertGreater(left[2], 0.05, "the hands should be in front of the body, +Z")


class ExportTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / "avatar.glb"
        self.stats = glb_export.write_glb(avatar(), self.path, CLIPS)

    def tearDown(self):
        self.tmp.cleanup()

    def test_the_file_is_a_glb_and_parses(self):
        data = self.path.read_bytes()
        self.assertEqual(data[:4], b"glTF")
        document, binary = glb_check.parse_glb(data)
        self.assertEqual(document["asset"]["version"], "2.0")
        self.assertGreater(len(binary), 1000)

    def test_it_passes_our_own_contract_check(self):
        ok, problems = glb_check.check(self.path, 21, glb_check.REQUIRED_CLIPS, strict=True)
        self.assertTrue(ok, problems)

    def test_the_scene_points_at_the_armature_and_the_mesh(self):
        document, _ = glb_check.parse_glb(self.path.read_bytes())
        nodes = document["nodes"]
        self.assertEqual(nodes[0]["name"], "Armature")
        mesh_node = next(n for n in nodes if "mesh" in n)
        self.assertIn("skin", mesh_node)
        self.assertEqual([n.get("name") for n in nodes[1:1 + len(BONE_NAMES)]], list(BONE_NAMES))

    def test_animation_targets_are_bones_and_times_are_sorted(self):
        document, binary = glb_check.parse_glb(self.path.read_bytes())
        for animation in document["animations"]:
            for channel in animation["channels"]:
                self.assertIn(channel["target"]["node"], range(1, 1 + len(BONE_NAMES)))
                self.assertEqual(channel["target"]["path"], "rotation")
            for sampler in animation["samplers"]:
                times = glb_check.read_accessor(document, binary, sampler["input"]).ravel()
                self.assertTrue(np.all(np.diff(times) > 0))

    def test_a_truncated_file_is_reported_as_not_a_glb(self):
        broken = Path(self.tmp.name) / "broken.glb"
        broken.write_bytes(self.path.read_bytes()[:64])
        with self.assertRaises(ValueError):
            glb_check.check(broken, 21, (), False)


class IntakeTest(unittest.TestCase):
    def test_a_test_card_measures_like_a_person(self):
        with tempfile.TemporaryDirectory() as tmp:
            card = make_test_photo.draw(Path(tmp) / "card.png")
            brief = photo_ingest.ingest(card, Path(tmp) / "brief.json", Path(tmp) / "cut.png")
        p = brief["proportions"]
        self.assertGreater(brief["checks"]["subject_height_fraction"], 0.6)
        self.assertTrue(brief["checks"]["border_is_background"])
        self.assertTrue(brief["checks"]["arms_clear_of_body"])
        # Ordered landmarks, top to bottom.
        self.assertGreater(p["head_centre"], p["neck"])
        self.assertGreater(p["neck"], p["shoulder"])
        self.assertGreater(p["shoulder"], p["chest"])
        self.assertGreater(p["chest"], p["waist"])
        self.assertGreater(p["waist"], p["hip"])
        self.assertGreater(p["hip"], p["knee"])
        self.assertGreater(p["knee"], p["ankle"])
        # Widths are halved widths: a shoulder is wider than a head, hips wider than a waist.
        self.assertGreater(p["shoulder_half"], p["head_half"])
        self.assertGreater(p["hip_half"], p["waist_half"] * 0.8)
        self.assertLess(p["head_half"], 0.07)

    def test_the_palette_matches_the_card(self):
        with tempfile.TemporaryDirectory() as tmp:
            card = make_test_photo.draw(Path(tmp) / "card.png")
            brief = photo_ingest.ingest(card, Path(tmp) / "brief.json", Path(tmp) / "cut.png")
        self.assertEqual(list(brief["palette_rgb"]["top"]), list(make_test_photo.TOP))
        self.assertEqual(list(brief["palette_rgb"]["hair"]), list(make_test_photo.HAIR))
        skin = brief["palette_rgb"]["skin"]
        self.assertTrue(all(abs(a - b) < 24 for a, b in zip(skin, make_test_photo.SKIN)), skin)

    def test_a_blank_image_fails_loudly(self):
        from PIL import Image

        with tempfile.TemporaryDirectory() as tmp:
            blank = Path(tmp) / "blank.png"
            Image.new("RGB", (200, 400), (240, 240, 240)).save(blank)
            with self.assertRaises(ValueError):
                photo_ingest.ingest(blank, Path(tmp) / "b.json", None)


class PaletteTest(unittest.TestCase):
    def test_the_brief_palette_is_clamped_to_a_usable_albedo(self):
        from build_avatar import palette_from

        palette = palette_from({"palette": {"skin": [1.4, -0.2, 0.5], "nonsense": [1, 1, 1]}})
        self.assertEqual(set(palette), {"skin", "hair", "top", "trousers", "shoes"})
        for colour in palette.values():
            for channel in colour:
                self.assertGreaterEqual(channel, 0.0)
                self.assertLessEqual(channel, 1.0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
