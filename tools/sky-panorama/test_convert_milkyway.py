#!/usr/bin/env python3
"""
Deterministic checks for the sky-panorama conversion and validation tools.

These tests use SYNTHETIC EXR fixtures generated inside the test run. They verify that the
converter and validator preserve orientation, reject flips/mirrors/resizes/untagged output and
mismatched hashes. They do NOT verify NASA imagery; that requires the real source file and the
validator's --source mode (see provenance/milkyway_2020_4k.source.json).

Run:  python3 -m unittest -v tools/sky-panorama/test_convert_milkyway.py
Needs: OpenEXR, numpy, pillow (offline development machine only).
"""

from __future__ import annotations

import io
import os
import tempfile
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
import sys

sys.path.insert(0, HERE)

import convert_milkyway_exr as conv  # noqa: E402
import validate_panorama as val  # noqa: E402

W, H = conv.EXPECTED_WIDTH, conv.EXPECTED_HEIGHT


def write_synthetic_exr(path: str) -> None:
    """Asymmetric synthetic panorama: smooth dark gradient, an off-centre bright blob, and a marker
    pixel placed at a known row/column so that flips and mirrors are unambiguous."""
    import Imath
    import OpenEXR

    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    base = 0.003 + 0.05 * (y / H)                       # darker towards the top (row 0)
    blob = 0.9 * np.exp(-(((x - 3000.0) / 120.0) ** 2 + ((y - 600.0) / 90.0) ** 2))
    r = base + blob
    g = base * 0.8 + 0.6 * blob
    b = base * 1.2 + 0.3 * blob
    # Marker: a 16x16 patch at row 1500, column 500, unlike anything else in the image.
    for chan in (r, g, b):
        chan[1500:1516, 500:516] = 0.5
    r[1500:1516, 500:516] = 0.9
    g[1500:1516, 500:516] = 0.1
    b[1500:1516, 500:516] = 0.1

    header = OpenEXR.Header(W, H)
    header["channels"] = {c: Imath.Channel(Imath.PixelType(Imath.PixelType.HALF)) for c in "RGB"}
    out = OpenEXR.OutputFile(path, header)
    out.writePixels({
        "R": r.astype(np.float16).tobytes(),
        "G": g.astype(np.float16).tobytes(),
        "B": b.astype(np.float16).tobytes(),
    })
    out.close()


class ConversionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.tmp = tempfile.TemporaryDirectory()
        cls.exr = os.path.join(cls.tmp.name, "synthetic_4k.exr")
        write_synthetic_exr(cls.exr)
        cls.exr_sha = conv.sha256_file(cls.exr)
        cls.pixels = conv.convert_to_uint8(cls.exr, 0.0)
        cls.jpg = os.path.join(cls.tmp.name, "panorama.jpg")
        rc = conv.main([cls.exr, "--out", cls.jpg, "--expected-sha256", cls.exr_sha])
        assert rc == 0, "converter should accept the matching hash"

    @classmethod
    def tearDownClass(cls) -> None:
        cls.tmp.cleanup()

    def test_output_geometry_is_exactly_4096_by_2048(self) -> None:
        self.assertEqual(self.pixels.shape, (H, W, 3))

    def test_marker_stays_at_its_source_row_and_column(self) -> None:
        # Identity mapping: the marker must remain at row 1500..1515, column 500..515.
        # A flipped image would place it at row H-1516.., a mirrored one at column W-516..
        patch = self.pixels[1500:1516, 500:516, 0].astype(int)
        self.assertGreater(patch.mean(), 200)          # red marker is bright
        outside = self.pixels[1500:1516, 1000:1016, 0].astype(int)
        self.assertLess(outside.mean(), 100)

    def test_no_resampling_when_dimensions_are_correct(self) -> None:
        # Converter must not resize or crop: source and output share the same grid.
        self.assertEqual(self.pixels.shape[:2], (H, W))

    def test_converter_refuses_mismatched_source_hash(self) -> None:
        rc = conv.main([self.exr, "--out", os.path.join(self.tmp.name, "x.jpg"),
                        "--expected-sha256", "0" * 64])
        self.assertEqual(rc, 2)

    def test_output_is_tagged_srgb(self) -> None:
        from PIL import Image, ImageCms
        with Image.open(self.jpg) as img:
            self.assertEqual(img.format, "JPEG")
            icc = img.info["icc_profile"]
        self.assertIn("srgb", ImageCms.getProfileDescription(
            ImageCms.ImageCmsProfile(io.BytesIO(icc))).lower())


class ValidatorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.tmp = tempfile.TemporaryDirectory()
        cls.exr = os.path.join(cls.tmp.name, "synthetic_4k.exr")
        write_synthetic_exr(cls.exr)
        cls.exr_sha = conv.sha256_file(cls.exr)
        cls.good = os.path.join(cls.tmp.name, "good.jpg")
        conv.main([cls.exr, "--out", cls.good])
        from PIL import Image
        cls.Image = Image

    @classmethod
    def tearDownClass(cls) -> None:
        cls.tmp.cleanup()

    def _write_variant(self, name: str, array: np.ndarray, icc: bool = True) -> str:
        from PIL import ImageCms
        path = os.path.join(self.tmp.name, name)
        kwargs = {"icc_profile": ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB")).tobytes()} if icc else {}
        self.Image.fromarray(array, mode="RGB").save(path, format="JPEG", quality=92, subsampling=0, **kwargs)
        return path

    def test_accepts_correct_conversion_against_source(self) -> None:
        self.assertEqual(val.validate(self.good, None, self.exr), [])

    def test_accepts_matching_expected_hash(self) -> None:
        with open(self.good, "rb") as fh:
            digest = val.sha256_bytes(fh.read())
        self.assertEqual(val.validate(self.good, digest), [])

    def test_rejects_wrong_expected_hash(self) -> None:
        reasons = val.validate(self.good, "0" * 64)
        self.assertTrue(any("SHA-256" in r for r in reasons))

    def test_rejects_vertically_flipped_panorama(self) -> None:
        pixels = conv.convert_to_uint8(self.exr, 0.0)[::-1, :, :].copy()
        reasons = val.validate(self._write_variant("flip.jpg", pixels), None, self.exr)
        self.assertTrue(any("vertical flip" in r for r in reasons), reasons)

    def test_rejects_horizontally_mirrored_panorama(self) -> None:
        pixels = conv.convert_to_uint8(self.exr, 0.0)[:, ::-1, :].copy()
        reasons = val.validate(self._write_variant("mirror.jpg", pixels), None, self.exr)
        self.assertTrue(any("horizontal mirror" in r for r in reasons), reasons)

    def test_rejects_resized_panorama(self) -> None:
        small = np.asarray(self.Image.open(self.good).convert("RGB").resize((2048, 1024), self.Image.LANCZOS))
        reasons = val.validate(self._write_variant("small.jpg", small), None, None)
        self.assertTrue(any("dimensions" in r for r in reasons), reasons)

    def test_rejects_untagged_output_without_srgb_profile(self) -> None:
        pixels = conv.convert_to_uint8(self.exr, 0.0)
        reasons = val.validate(self._write_variant("untagged.jpg", pixels, icc=False), None, None)
        self.assertTrue(any("ICC" in r for r in reasons), reasons)

    def test_rejects_non_srgb_icc_profile(self) -> None:
        from PIL import ImageCms
        pixels = conv.convert_to_uint8(self.exr, 0.0)
        path = os.path.join(self.tmp.name, "xyz.jpg")
        # A non-sRGB profile: XYZ D50 colour space, built into LittleCMS.
        xyz = ImageCms.ImageCmsProfile(ImageCms.createProfile("XYZ")).tobytes()
        self.Image.fromarray(pixels, mode="RGB").save(path, format="JPEG", quality=92, subsampling=0, icc_profile=xyz)
        reasons = val.validate(path, None, None)
        self.assertTrue(any("not sRGB" in r for r in reasons), reasons)


if __name__ == "__main__":
    unittest.main(verbosity=2)
