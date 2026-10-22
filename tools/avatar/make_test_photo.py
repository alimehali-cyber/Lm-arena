#!/usr/bin/env python3
"""Draw a stand-in reference photo, so the whole photo -> avatar chain can be run before a real shoot.

    python3 tools/avatar/make_test_photo.py --out work/test_card.png

It is deliberately honest about what it is: a flat test card in A-pose with five distinct colours and
a plain background — the same things the intake rules ask a real photograph for. If the pipeline can
measure and rebuild this, it can measure a photograph; if it cannot, the bug is in the pipeline.
"""

from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw

HEIGHT = 1100
WIDTH = 760
BACKGROUND = (232, 232, 230)
SKIN = (214, 160, 122)
HAIR = (58, 40, 33)
TOP = (58, 92, 138)
TROUSERS = (48, 52, 66)
SHOES = (30, 30, 36)


def draw(path: Path) -> Path:
    image = Image.new("RGB", (WIDTH, HEIGHT), BACKGROUND)
    d = ImageDraw.Draw(image)
    cx = WIDTH / 2
    h = 980.0
    ground = 1050

    def y(fraction: float) -> float:
        return ground - h * fraction

    # Legs.
    for sign in (-1, 1):
        hip = (cx + sign * 52, y(0.52))
        knee = (cx + sign * 58, y(0.285))
        ankle = (cx + sign * 62, y(0.045))
        d.line([hip, knee, ankle], fill=TROUSERS, width=62, joint="curve")
        d.ellipse([ankle[0] - 34, ankle[1] - 12, ankle[0] + 34, ankle[1] + 30], fill=SHOES)

    # Torso.
    d.polygon([
        (cx - 96, y(0.54)), (cx - 118, y(0.80)), (cx - 40, y(0.845)),
        (cx + 40, y(0.845)), (cx + 118, y(0.80)), (cx + 96, y(0.54)),
    ], fill=TOP)
    d.rounded_rectangle([cx - 78, y(0.845), cx + 78, y(0.80)], radius=16, fill=TOP)

    # Arms in A-pose, ~22 degrees out.
    for sign in (-1, 1):
        shoulder = (cx + sign * 100, y(0.795))
        angle = math.radians(22)
        elbow = (shoulder[0] + sign * math.sin(angle) * 168, shoulder[1] + math.cos(angle) * 168)
        angle2 = math.radians(12)
        wrist = (elbow[0] + sign * math.sin(angle2) * 150, elbow[1] + math.cos(angle2) * 150)
        d.line([shoulder, elbow, wrist], fill=TOP, width=44, joint="curve")
        d.ellipse([wrist[0] - 22, wrist[1] - 18, wrist[0] + 22, wrist[1] + 34], fill=SKIN)

    # Neck and head.
    d.rounded_rectangle([cx - 24, y(0.855), cx + 24, y(0.80)], radius=10, fill=SKIN)
    d.ellipse([cx - 54, y(0.995), cx + 54, y(0.845)], fill=SKIN)
    # Hair: crown and back.
    d.chord([cx - 58, y(1.005), cx + 58, y(0.905)], 180, 360, fill=HAIR)
    d.ellipse([cx - 56, y(0.955), cx + 56, y(0.875)], fill=HAIR)
    d.ellipse([cx - 50, y(0.952), cx + 50, y(0.892)], fill=SKIN)
    # Eyes, so a face is readable in the head crop.
    for sign in (-1, 1):
        d.ellipse([cx + sign * 18 - 6, y(0.918) - 5, cx + sign * 18 + 6, y(0.918) + 5], fill=(40, 34, 30))

    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", type=Path, default=Path("work/test_card.png"))
    args = parser.parse_args(argv)
    print(draw(args.out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
