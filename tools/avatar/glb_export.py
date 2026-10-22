"""Write a skinned, animated GLB — one mesh, one skin, one material, five clips.

Binary glTF 2.0, written directly: no Blender, no gltf library, no GPU. The output satisfies
skills/zig-avatar-runtime/references/asset-contract.md and is verified by `glb_check.py`.

Layout: node 0 is the Armature; nodes 1..n are the bones in BONE_NAMES order; the last node carries
the skinned mesh. Every bone's rest rotation is identity, so animation is a pure rotation channel and
the inverse bind matrices are translations.
"""

from __future__ import annotations

import json
import struct
from pathlib import Path

import numpy as np

from avatar_core import BONE_NAMES, PARENTS, Avatar
from motion import CLIPS, Clip, sample_clip

GENERATOR = "ZIG authored-avatar route (tools/avatar)"

FLOAT = 5126
USHORT = 5123
UINT = 5125
ARRAY_BUFFER = 34962
ELEMENT_ARRAY_BUFFER = 34963


class _Buffer:
    def __init__(self) -> None:
        self.blob = bytearray()
        self.views: list[dict] = []

    def view(self, data: bytes, target: int | None = None) -> int:
        while len(self.blob) % 4:
            self.blob.append(0)
        offset = len(self.blob)
        self.blob.extend(data)
        entry: dict = {"buffer": 0, "byteOffset": offset, "byteLength": len(data)}
        if target is not None:
            entry["target"] = target
        self.views.append(entry)
        return len(self.views) - 1


class _Accessors:
    def __init__(self, buffer: _Buffer) -> None:
        self.buffer = buffer
        self.entries: list[dict] = []

    def add(
        self,
        array: np.ndarray,
        component: int,
        kind: str,
        target: int | None = None,
        min_max: bool = False,
        normalized: bool = False,
    ) -> int:
        data = np.ascontiguousarray(array).tobytes()
        entry = {
            "bufferView": self.buffer.view(data, target),
            "componentType": component,
            "count": int(array.shape[0]),
            "type": kind,
        }
        if normalized:
            entry["normalized"] = True
        if min_max:
            if kind == "SCALAR":
                entry["min"] = [float(np.min(array))]
                entry["max"] = [float(np.max(array))]
            else:
                entry["min"] = [float(v) for v in np.min(array, axis=0)]
                entry["max"] = [float(v) for v in np.max(array, axis=0)]
        self.entries.append(entry)
        return len(self.entries) - 1


def _node_tree(avatar: Avatar) -> list[dict]:
    """Armature at 0, bones at 1..20 in BONE_NAMES order, mesh node last."""
    bones = {bone.name: bone for bone in avatar.skeleton}
    index_of = {name: i + 1 for i, name in enumerate(BONE_NAMES)}

    nodes: list[dict] = [{"name": "Armature", "children": [index_of["hips"]]}]
    for name in BONE_NAMES:
        bone = bones[name]
        parent = PARENTS[name]
        offset = bone.head - (bones[parent].head if parent else np.zeros(3))
        entry: dict = {
            "name": name,
            "translation": [round(float(v), 6) for v in offset],
        }
        children = [index_of[child] for child, p in PARENTS.items() if p == name]
        if children:
            entry["children"] = children
        nodes.append(entry)
    return nodes


def write_glb(avatar: Avatar, path: Path, clips: tuple[Clip, ...] = CLIPS) -> dict:
    buffer = _Buffer()
    accessors = _Accessors(buffer)
    mesh = avatar.mesh

    position_accessor = accessors.add(mesh.positions.astype(np.float32), FLOAT, "VEC3", ARRAY_BUFFER, min_max=True)
    normal_accessor = accessors.add(mesh.normals.astype(np.float32), FLOAT, "VEC3", ARRAY_BUFFER)
    colour_accessor = accessors.add(mesh.colors.astype(np.float32), FLOAT, "VEC3", ARRAY_BUFFER)
    joints_accessor = accessors.add(mesh.joints.astype(np.uint16), USHORT, "VEC4", ARRAY_BUFFER)
    weights_accessor = accessors.add(mesh.weights.astype(np.float32), FLOAT, "VEC4", ARRAY_BUFFER)
    index_accessor = accessors.add(mesh.indices.astype(np.uint32), UINT, "SCALAR", ELEMENT_ARRAY_BUFFER)

    from avatar_core import inverse_bind_matrices

    ibm = inverse_bind_matrices(avatar.skeleton).astype(np.float32)
    ibm_accessor = accessors.add(ibm, FLOAT, "MAT4")

    nodes = _node_tree(avatar)
    joint_indices = [i + 1 for i in range(len(BONE_NAMES))]
    mesh_node_index = len(nodes)
    nodes.append({
        "name": "AvatarMesh",
        "mesh": 0,
        "skin": 0,
    })

    # Animations.
    animations = []
    for clip in clips:
        times, tracks = sample_clip(clip, BONE_NAMES)
        time_accessor = accessors.add(times.astype(np.float32), FLOAT, "SCALAR", min_max=True)
        samplers = []
        channels = []
        for bone, keys in tracks.items():
            quats = keys.astype(np.float32)
            # Shortest-arc continuity: flip a key whose dot product with the previous one is negative.
            for i in range(1, len(quats)):
                if float(np.dot(quats[i], quats[i - 1])) < 0.0:
                    quats[i] = -quats[i]
            output = accessors.add(quats, FLOAT, "VEC4")
            samplers.append({"input": time_accessor, "output": output, "interpolation": "LINEAR"})
            channels.append({
                "sampler": len(samplers) - 1,
                "target": {"node": BONE_NAMES.index(bone) + 1, "path": "rotation"},
            })
        animations.append({"name": clip.name, "samplers": samplers, "channels": channels})

    document = {
        "asset": {
            "version": "2.0",
            "generator": GENERATOR,
            "copyright": "ZIG avatar — see the .provenance.json sidecar",
        },
        "scene": 0,
        "scenes": [{"name": "Avatar", "nodes": [0, mesh_node_index]}],
        "nodes": nodes,
        "meshes": [{
            "name": "AvatarBody",
            "primitives": [{
                "attributes": {
                    "POSITION": position_accessor,
                    "NORMAL": normal_accessor,
                    "COLOR_0": colour_accessor,
                    "JOINTS_0": joints_accessor,
                    "WEIGHTS_0": weights_accessor,
                },
                "indices": index_accessor,
                "material": 0,
                "mode": 4,
            }],
        }],
        "skins": [{
            "name": "AvatarRig",
            "joints": joint_indices,
            "inverseBindMatrices": ibm_accessor,
            "skeleton": 0,
        }],
        "materials": [{
            "name": "AvatarSkin",
            "pbrMetallicRoughness": {
                "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
                "metallicFactor": 0.0,
                "roughnessFactor": 0.72,
            },
            "doubleSided": False,
        }],
        "animations": animations,
        "accessors": accessors.entries,
        "bufferViews": buffer.views,
        "buffers": [{"byteLength": len(buffer.blob)}],
    }

    json_bytes = json.dumps(document, separators=(",", ":"), allow_nan=False).encode("utf-8")
    while len(json_bytes) % 4:
        json_bytes += b" "
    binary = bytes(buffer.blob)
    while len(binary) % 4:
        binary += b"\x00"

    total = 12 + 8 + len(json_bytes) + 8 + len(binary)
    glb = bytearray()
    glb += struct.pack("<III", 0x46546C67, 2, total)
    glb += struct.pack("<II", len(json_bytes), 0x4E4F534A)
    glb += json_bytes
    glb += struct.pack("<II", len(binary), 0x004E4942)
    glb += binary

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(glb)

    return {
        "path": str(path),
        "bytes": len(glb),
        "triangles": mesh.triangle_count,
        "vertices": int(mesh.positions.shape[0]),
        "bones": len(BONE_NAMES),
        "clips": [{"name": c.name, "seconds": c.length} for c in clips],
    }
