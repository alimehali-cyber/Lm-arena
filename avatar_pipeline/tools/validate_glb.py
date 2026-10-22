#!/usr/bin/env python3
"""
Stage 9 - validate the exported GLB with no external dependency.

Checks chosen for an Android runtime:
  * container and chunk integrity, accessors inside their buffer views
  * exactly one skin, joints named (the app addresses bones by name), bind matrices
  * every primitive skinned, morph target names matching the target count
  * every required clip present and non-empty, with both bone and morph channels
  * a size budget for a decorative asset

Exits non-zero on a hard failure so the pipeline stops.
"""
from __future__ import annotations
import argparse, json, pathlib, struct, sys

COMP = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}
NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT2": 4, "MAT3": 9, "MAT4": 16}

REQUIRED = {
    "Idle_Breathing", "Idle_LookAround", "Blink_Natural",
    "Expression_Happy", "Expression_Sad", "Expression_Surprised",
    "Expression_Thinking", "Expression_Curious", "Expression_Frustrated",
    "Gesture_Explain", "Gesture_Greet", "Gesture_Point", "Gesture_Emphasize",
    "Talk_Neutral", "Talk_Expressive", "Walk", "Turn",
    "React_Positive", "React_Negative",
}


def load_glb(path: pathlib.Path):
    data = path.read_bytes()
    magic, _ver, length = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        raise SystemExit("not a GLB")
    off, js, bin_ = 12, None, b""
    while off < length:
        clen, ctype = struct.unpack_from("<II", data, off)
        chunk = data[off + 8: off + 8 + clen]
        if ctype == 0x4E4F534A:
            js = json.loads(chunk.decode("utf-8"))
        elif ctype == 0x004E4942:
            bin_ = chunk
        off += 8 + clen + ((4 - clen % 4) % 4 if clen % 4 else 0)
    return js, bin_, len(data)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--glb", required=True)
    ap.add_argument("--report", required=True)
    ap.add_argument("--max-mb", type=float, default=4.0)
    a = ap.parse_args()

    js, bin_, total = load_glb(pathlib.Path(a.glb))
    checks = []
    hard_fail = False

    def ok(name, cond, detail=""):
        nonlocal hard_fail
        checks.append({"check": name, "pass": bool(cond), "detail": detail})
        if not cond:
            hard_fail = True

    ok("json_chunk", js is not None)
    ok("bin_chunk", len(bin_) > 0, f"{len(bin_)} bytes")
    ok("generator", bool(js.get("asset", {}).get("generator")))
    ok("no_external_uris", not any("uri" in b for b in js.get("buffers", [])))

    bad = []
    for i, acc in enumerate(js.get("accessors", [])):
        if "bufferView" not in acc:
            continue
        bv = js["bufferViews"][acc["bufferView"]]
        start = bv.get("byteOffset", 0) + acc.get("byteOffset", 0)
        size = acc["count"] * NCOMP[acc["type"]] * COMP[acc["componentType"]]
        if start + size > len(bin_):
            bad.append(i)
    ok("accessor_bounds", not bad, f"{len(bad)} out of range")

    skins = js.get("skins", [])
    ok("single_skin", len(skins) == 1, f"{len(skins)}")
    joints = skins[0].get("joints", []) if skins else []
    ok("joint_count", 15 <= len(joints) <= 80, f"{len(joints)}")
    ok("inverse_bind_matrices", "inverseBindMatrices" in (skins[0] if skins else {}))
    nodes = js.get("nodes", [])
    unnamed = [j for j in joints if not nodes[j].get("name")]
    ok("all_joints_named", not unnamed, f"{len(unnamed)} unnamed")

    prims = [p for m in js.get("meshes", []) for p in m.get("primitives", [])]
    ok("primitives", 0 < len(prims) <= 16, f"{len(prims)} draw calls")
    missing = [i for i, p in enumerate(prims) if "WEIGHTS_0" not in p.get("attributes", {})]
    ok("skinned_primitives", not missing, f"{len(missing)} without WEIGHTS_0")
    targets = [len(p.get("targets", [])) for p in prims]
    ok("morph_targets_match", len(set(targets)) <= 1, f"{set(targets)}")
    mesh = js["meshes"][0]
    tnames = (mesh.get("extras", {}).get("targetNames")
              or mesh["primitives"][0].get("extras", {}).get("targetNames") or [])
    ok("morph_names_recorded", bool(tnames), f"{len(tnames)} names")
    if tnames and targets:
        ok("morph_name_count", len(tnames) == targets[0], f"{len(tnames)} vs {targets[0]}")

    anims = js.get("animations", [])
    names = [x.get("name", "") for x in anims]
    ok("animation_count", len(anims) >= 19, f"{len(anims)}")
    missing_clips = sorted(REQUIRED - set(names))
    ok("all_required_clips", not missing_clips,
       f"missing {missing_clips}" if missing_clips else "19 named clips present")

    durations, empty = {}, []
    for x in anims:
        ins = [js["accessors"][s["input"]] for s in x["samplers"]]
        if not ins or not x.get("channels"):
            empty.append(x.get("name"))
            continue
        durations[x["name"]] = round(max((acc.get("max", [0])[0] for acc in ins), default=0), 3)
    ok("no_empty_animations", not empty, f"{empty[:4]}")

    paths = {}
    for x in anims:
        for ch in x.get("channels", []):
            paths[ch["target"]["path"]] = paths.get(ch["target"]["path"], 0) + 1
    ok("bone_and_morph_channels", paths.get("rotation", 0) > 0 and paths.get("weights", 0) > 0,
       str(paths))
    # The ZIG runtime turns the whole character by rotating the root joint (the
    # "walk in from the edge" action). If root is missing from the skin, or the Turn
    # clip does not target it, that motion silently does nothing on device.
    joint_names = [nodes[j].get("name", "") for j in joints]
    ok("root_joint_exported", "root" in joint_names, f"{len(joint_names)} joints")
    root_idx = [j for j, n in enumerate(nodes) if n.get("name") == "root"]
    turn = next((x for x in anims if x.get("name") == "Turn"), None)
    turn_root = False
    if turn and root_idx:
        turn_root = any(ch["target"].get("node") == root_idx[0]
                        and ch["target"]["path"] in ("rotation", "translation")
                        for ch in turn.get("channels", []))
    ok("turn_moves_root", turn_root, "Turn must key the root node")
    ok("size_budget", total < a.max_mb * 1e6, f"{total / 1e6:.2f} MB")

    report = {
        "glb": a.glb, "bytes": total, "mb": round(total / 1e6, 3),
        "status": "fail" if hard_fail else "pass",
        "failed": [c["check"] for c in checks if not c["pass"]],
        "bones": [nodes[j].get("name") for j in joints],
        "durations_s": durations,
        "channel_paths": paths,
        "checks": checks,
    }
    pathlib.Path(a.report).write_text(json.dumps(report, indent=2))
    print(json.dumps({"status": report["status"], "failed": report["failed"],
                      "mb": report["mb"], "clips": len(durations)}, indent=2))
    sys.exit(1 if hard_fail else 0)


if __name__ == "__main__":
    main()
