#!/usr/bin/env python3
"""Build a character from a spec, render a review sheet, and (optionally) install it into the app.

    python3 tools/avatar/build_character.py --spec tools/avatar/specs/professor_z.json \
        --out out/avatars/professor_z.glb --install

`--install` copies the GLB and its provenance sidecar into `app/src/main/assets/avatar/`, where the
Android runtime loads it; without it, the build stays in `out/` for review.
"""

from __future__ import annotations

import argparse
import json
import shutil
from datetime import date
from pathlib import Path

from character import CharacterSpec, build_character
from glb_export import write_glb
from motion import CLIPS
from preview import contact_sheet, render

ROOT = Path(__file__).resolve().parents[2]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets" / "avatar"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--spec", type=Path, required=True)
    parser.add_argument("--out", type=Path, default=None)
    parser.add_argument("--sheet", type=Path, default=None)
    parser.add_argument("--install", action="store_true", help="copy into app/src/main/assets/avatar/")
    parser.add_argument("--no-sheet", action="store_true")
    args = parser.parse_args(argv)

    spec = CharacterSpec.load(args.spec)
    avatar = build_character(spec)
    out = args.out or Path("out/avatars") / f"{spec.name}.glb"
    stats = write_glb(avatar, out, CLIPS)

    sidecar = {
        "schema": "zig.avatar/1",
        "asset": spec.name,
        "display_name": json.loads(args.spec.read_text()).get("display_name", spec.name),
        "created": date.today().isoformat(),
        "route": "authored-fallback",
        "engine": {"name": "ZIG authored character builder (tools/avatar/character.py)", "licence": "this repository"},
        "spec": str(args.spec),
        "rig": {"tool": "tools/avatar (authored)", "bones": stats["bones"], "bind": "A-pose"},
        "motions": [{"name": clip.name, "seconds": clip.length, "licence": "this repository"} for clip in CLIPS],
        "source_reference": "user-supplied character catalogue sheet",
        "consent": "user-supplied character reference; owned by the requester",
        "stats": {k: stats[k] for k in ("triangles", "vertices", "bones", "bytes")},
    }
    sidecar_path = out.with_name(out.name + ".provenance.json")
    sidecar_path.write_text(json.dumps(sidecar, indent=2) + "\n")

    print(f"character {out}  {stats['bytes'] / 1024:.0f} KB · {stats['triangles']} triangles · {stats['bones']} bones")

    if not args.no_sheet:
        sheet_path = args.sheet or out.with_name(f"{spec.name}_sheet.png")
        contact_sheet(avatar, sheet_path)
        print(f"sheet     {sheet_path}")
        for name, time_s in (("front", 0.0), ("threequarter", 0.0)):
            pass

    if args.install:
        ASSET_DIR.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(out, ASSET_DIR / out.name)
        shutil.copyfile(sidecar_path, ASSET_DIR / sidecar_path.name)
        print(f"installed {ASSET_DIR / out.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
