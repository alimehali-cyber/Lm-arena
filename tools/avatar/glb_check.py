#!/usr/bin/env python3
"""Check an avatar GLB against the app contract, by reading the file itself.

    python3 tools/avatar/glb_check.py out/avatars/<name>.glb
    python3 tools/avatar/glb_check.py <name>.glb --min-bones 18 --require-clip idle_breathe --strict

Reads the JSON and the BIN chunk, decodes the accessors it needs and asserts the things that actually
break a skinned mesh in a renderer: joint indices in range, weights summing to one, inverse bind
matrices present and the right count, animation inputs sorted, targets existing, bone names matching
the contract, height in range, triangle budget. No GPU, no Blender, no third-party glTF library.
"""

from __future__ import annotations

import argparse
import json
import struct
import sys
from pathlib import Path

import numpy as np

CONTRACT_BONES = {
    "hips", "spine", "chest", "neck", "head",
    "shoulder.L", "upper_arm.L", "forearm.L", "hand.L",
    "shoulder.R", "upper_arm.R", "forearm.R", "hand.R",
    "thigh.L", "shin.L", "foot.L",
    "thigh.R", "shin.R", "foot.R",
}
REQUIRED_CLIPS = ("idle_breathe", "day_shade", "dusk_phone", "night_point")
COMPONENT = {5120: ("b", 1), 5121: ("B", 1), 5122: ("h", 2), 5123: ("H", 2), 5125: ("I", 4), 5126: ("f", 4)}
WIDTH = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


def parse_glb(data: bytes) -> tuple[dict, bytes]:
    if len(data) < 12:
        raise ValueError("file is shorter than a GLB header")
    magic, version, length = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        raise ValueError("not a GLB (bad magic)")
    if version != 2:
        raise ValueError(f"glTF version {version}, expected 2")
    if length != len(data):
        raise ValueError(f"header says {length} bytes, file is {len(data)}")
    offset = 12
    document = None
    binary = b""
    while offset < len(data):
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        payload = data[offset + 8: offset + 8 + chunk_length]
        if chunk_type == 0x4E4F534A:
            document = json.loads(payload.decode("utf-8"))
        elif chunk_type == 0x004E4942:
            binary = payload
        offset += 8 + chunk_length
    if document is None:
        raise ValueError("no JSON chunk")
    return document, binary


def read_accessor(document: dict, binary: bytes, index: int) -> np.ndarray:
    accessor = document["accessors"][index]
    view = document["bufferViews"][accessor["bufferView"]]
    fmt, size = COMPONENT[accessor["componentType"]]
    width = WIDTH[accessor["type"]]
    count = accessor["count"]
    stride = view.get("byteStride") or size * width
    start = view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
    if start + stride * (count - 1) + size * width > len(binary):
        raise ValueError(f"accessor {index} runs past the end of the buffer")
    out = np.zeros((count, width), dtype=np.float64)
    for i in range(count):
        chunk = struct.unpack_from("<" + fmt * width, binary, start + i * stride)
        out[i] = chunk
    return out


def check(path: Path, min_bones: int, require_clips: tuple[str, ...], strict: bool) -> tuple[bool, list[str]]:
    problems: list[str] = []
    notes: list[str] = []
    document, binary = parse_glb(path.read_bytes())

    meshes = document.get("meshes", [])
    if len(meshes) != 1 or len(meshes[0].get("primitives", [])) != 1:
        problems.append("expected exactly one mesh with one primitive")
    primitive = meshes[0]["primitives"][0] if meshes else {}
    attributes = primitive.get("attributes", {})
    for attribute in ("POSITION", "NORMAL", "JOINTS_0", "WEIGHTS_0"):
        if attribute not in attributes:
            problems.append(f"missing vertex attribute {attribute}")

    positions = read_accessor(document, binary, attributes["POSITION"]) if "POSITION" in attributes else np.zeros((1, 3))
    height = float(positions[:, 1].max() - positions[:, 1].min())
    if not 1.55 <= height <= 1.95:
        notes.append(f"height {height:.2f} m is outside the 1.55-1.95 m contract")
        if strict:
            problems.append(f"height {height:.2f} m is outside the contract")

    joints = read_accessor(document, binary, attributes["JOINTS_0"]) if "JOINTS_0" in attributes else np.zeros((1, 4))
    weights = read_accessor(document, binary, attributes["WEIGHTS_0"]) if "WEIGHTS_0" in attributes else np.ones((1, 4))
    skins = document.get("skins", [])
    if len(skins) != 1:
        problems.append("expected exactly one skin")
    else:
        joints_used = len(skins[0]["joints"])
        if int(joints.max()) >= joints_used:
            problems.append(f"joint index {int(joints.max())} out of range (skin has {joints_used})")
        if joints_used < min_bones:
            problems.append(f"{joints_used} bones, contract needs at least {min_bones}")
        if "inverseBindMatrices" not in skins[0]:
            problems.append("skin has no inverseBindMatrices")
        else:
            ibm = read_accessor(document, binary, skins[0]["inverseBindMatrices"])
            if ibm.shape[0] != joints_used:
                problems.append(f"{ibm.shape[0]} inverse bind matrices for {joints_used} joints")

    sums = weights.sum(axis=1)
    if float(np.abs(sums - 1.0).max()) > 1e-3:
        problems.append(f"skin weights are not normalised (worst {float(np.abs(sums - 1.0).max()):.4f})")

    nodes = document.get("nodes", [])
    bone_names = {n.get("name") for n in nodes}
    missing = CONTRACT_BONES - bone_names
    if missing:
        problems.append(f"missing contract bones: {', '.join(sorted(missing))}")

    animations = document.get("animations", [])
    names = [a.get("name", "") for a in animations]
    for clip in require_clips:
        if clip not in names:
            problems.append(f"missing clip {clip!r}")
    for animation in animations:
        for channel in animation["channels"]:
            target = channel["target"]["node"]
            if target >= len(nodes):
                problems.append(f"clip {animation.get('name')} targets node {target}, which does not exist")
        for sampler in animation["samplers"]:
            times = read_accessor(document, binary, sampler["input"]).ravel()
            if len(times) < 2:
                problems.append(f"clip {animation.get('name')} has a sampler with {len(times)} keys")
            elif float(np.min(np.diff(times))) <= 0.0:
                problems.append(f"clip {animation.get('name')} has non-increasing key times")

    index_accessor = primitive.get("indices")
    triangles = 0
    if index_accessor is not None:
        indices = read_accessor(document, binary, index_accessor).ravel()
        triangles = len(indices) // 3
        if int(indices.max()) >= positions.shape[0]:
            problems.append("an index points past the last vertex")
    if triangles > 20000:
        problems.append(f"{triangles} triangles exceeds the 20,000 hard cap")

    stats = {
        "file": path.name,
        "bytes": path.stat().st_size,
        "vertices": int(positions.shape[0]),
        "triangles": triangles,
        "bones": len(skins[0]["joints"]) if skins else 0,
        "height_m": round(height, 3),
        "clips": ", ".join(
            f"{a.get('name')}({len(a['channels'])} ch)"
            for a in animations
        ),
        "materials": len(document.get("materials", [])),
    }
    print(f"GLB  {stats['file']}  {stats['bytes'] / 1024:.1f} KB")
    print(f"     {stats['triangles']} triangles · {stats['vertices']} vertices · {stats['bones']} bones · {stats['height_m']} m")
    print(f"     clips: {stats['clips']}")
    for note in notes:
        print(f"  note: {note}")
    for problem in problems:
        print(f"  FAIL: {problem}")
    if not problems:
        print("  OK: satisfies the avatar asset contract")
    return not problems, problems


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("glb", type=Path)
    parser.add_argument("--min-bones", type=int, default=19)
    parser.add_argument("--require-clip", action="append", default=None)
    parser.add_argument("--strict", action="store_true", help="treat contract notes as failures")
    args = parser.parse_args(argv)
    required = tuple(args.require_clip) if args.require_clip else REQUIRED_CLIPS
    ok, _ = check(args.glb, args.min_bones, required, args.strict)
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
