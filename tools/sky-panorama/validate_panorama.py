#!/usr/bin/env python3
"""
Deterministic asset-level validation for the ZIG runtime sky panorama.

Rejects the runtime JPEG when any of the following holds (no subjective image comparison):
  * dimensions are not exactly 4096 x 2048 (2:1 equirectangular, not resized)
  * the file is not a JPEG, or has no embedded sRGB ICC profile
  * the SHA-256 of the JPEG does not match --expected-sha256 (when given)
  * with --source: the JPEG does not match the deterministic re-conversion of the original EXR.
    The pixel pipeline is fixed, so the JPEG must decode within a small JPEG-compression
    tolerance of the re-derived pixels. If it instead matches a vertically flipped, horizontally
    mirrored, or 180-degree rotated version, the file is rejected with that specific reason.

Usage:
  validate_panorama.py app/src/main/assets/sky/panorama/milkyway_2020_4k.jpg \
      --expected-sha256 <sha256 from provenance> [--source milkyway_2020_4k.exr]

Exit status: 0 = accepted, 1 = rejected, 2 = usage / unreadable input.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

EXPECTED_WIDTH = 4096
EXPECTED_HEIGHT = 2048
# JPEG (q92, 4:4:4) error budget against the exact re-derived pixels. Calibrated on synthetic
# fixtures in test_convert_milkyway.py; a flip or mirror differs by tens of levels on any
# non-trivial image, so these bounds separate the cases without depending on image content.
MAX_MEAN_ABS_DIFF = 1.5
MIN_PSNR_DB = 38.0


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def psnr(a: np.ndarray, b: np.ndarray) -> float:
    mse = np.mean((a.astype(np.float64) - b.astype(np.float64)) ** 2)
    if mse == 0:
        return float("inf")
    return 10.0 * np.log10(255.0 * 255.0 / mse)


def validate(jpeg_path: str, expected_sha256: str | None = None, source_exr: str | None = None,
             exposure_ev: float = 0.0) -> list[str]:
    """Returns a list of rejection reasons. Empty list means accepted."""
    from PIL import Image, ImageCms

    reasons: list[str] = []
    with open(jpeg_path, "rb") as f:
        raw = f.read()

    if expected_sha256 and sha256_bytes(raw) != expected_sha256.lower():
        reasons.append("file SHA-256 does not match the expected conversion hash")

    try:
        image = Image.open(io.BytesIO(raw))
    except Exception as exc:  # noqa: BLE001 - any decode failure is a rejection
        return reasons + [f"not a readable image: {exc}"]

    if image.format != "JPEG":
        reasons.append(f"format is {image.format}, expected JPEG")
    if (image.width, image.height) != (EXPECTED_WIDTH, EXPECTED_HEIGHT):
        reasons.append(f"dimensions are {image.width}x{image.height}, expected "
                       f"{EXPECTED_WIDTH}x{EXPECTED_HEIGHT} (resized or wrong projection)")
    if image.mode != "RGB":
        reasons.append(f"mode is {image.mode}, expected RGB")

    icc = image.info.get("icc_profile")
    if not icc:
        reasons.append("no embedded ICC profile: colour space is not tagged as sRGB")
    else:
        try:
            desc = ImageCms.getProfileDescription(ImageCms.ImageCmsProfile(io.BytesIO(icc))).strip()
            if "srgb" not in desc.lower():
                reasons.append(f"embedded ICC profile is '{desc}', not sRGB")
        except Exception as exc:  # noqa: BLE001
            reasons.append(f"embedded ICC profile is unreadable: {exc}")

    if reasons and (image.width, image.height) != (EXPECTED_WIDTH, EXPECTED_HEIGHT):
        return reasons  # no point comparing pixels of the wrong geometry

    if source_exr is not None:
        from convert_milkyway_exr import convert_to_uint8

        reference = convert_to_uint8(source_exr, exposure_ev)
        decoded = np.asarray(image.convert("RGB"))
        if decoded.shape != reference.shape:
            return reasons + [f"decoded shape {decoded.shape} != reference {reference.shape}"]

        candidates = {
            "identity": reference,
            "vertical flip (north/south swapped)": reference[::-1, :, :],
            "horizontal mirror (east/west swapped)": reference[:, ::-1, :],
            "180-degree rotation": reference[::-1, ::-1, :],
        }
        scores = {}
        for name, ref in candidates.items():
            mad = float(np.mean(np.abs(decoded.astype(np.int16) - ref.astype(np.int16))))
            scores[name] = (mad, psnr(decoded, ref))

        mad, p = scores["identity"]
        if mad > MAX_MEAN_ABS_DIFF or p < MIN_PSNR_DB:
            better = [n for n, (m, q) in scores.items() if n != "identity" and m <= MAX_MEAN_ABS_DIFF and q >= MIN_PSNR_DB]
            if better:
                reasons.append(f"pixels match a {better[0]} of the source, not the correct orientation")
            else:
                reasons.append(f"pixels do not match the re-derived conversion (mean |diff| {mad:.2f}, "
                               f"PSNR {p:.1f} dB)")
    return reasons


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("jpeg")
    parser.add_argument("--expected-sha256")
    parser.add_argument("--source", help="Original EXR, for the deterministic pixel comparison")
    parser.add_argument("--exposure-ev", type=float, default=0.0)
    args = parser.parse_args(argv)
    if not os.path.isfile(args.jpeg):
        print(f"missing file: {args.jpeg}", file=sys.stderr)
        return 2
    reasons = validate(args.jpeg, args.expected_sha256, args.source, args.exposure_ev)
    if reasons:
        print("REJECTED")
        for r in reasons:
            print(" - " + r)
        return 1
    print("ACCEPTED: " + args.jpeg)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
