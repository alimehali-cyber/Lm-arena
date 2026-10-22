"""Core of the authored-avatar route: skeleton, parametric humanoid mesh, skin weights.

This is the route that needs no GPU, no Blender and no downloaded weights — numpy and Pillow only.
It builds a stylised, low-poly humanoid from measurements taken off a reference photo, rigs it with
the 21-bone skeleton the app expects (see skills/zig-avatar-runtime/references/asset-contract.md),
and hands the result to `glb_export`.

Conventions: metres, Y up, +Z forward (the character faces the camera). The character's LEFT is +X
and therefore appears on the RIGHT of the screen. Origin between the feet.

Everything here is pure arithmetic on arrays, so it is unit-testable on a CPU.
"""

from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np

# ------------------------------------------------------------------------------------------------
# Skeleton
# ------------------------------------------------------------------------------------------------

BONE_NAMES = (
    "hips",
    "spine",
    "chest",
    "neck",
    "head",
    "shoulder.L", "upper_arm.L", "forearm.L", "hand.L",
    "shoulder.R", "upper_arm.R", "forearm.R", "hand.R",
    "thigh.L", "shin.L", "foot.L", "toe.L",
    "thigh.R", "shin.R", "foot.R", "toe.R",
)

PARENTS = {
    "hips": None,
    "spine": "hips",
    "chest": "spine",
    "neck": "chest",
    "head": "neck",
    "shoulder.L": "chest", "upper_arm.L": "shoulder.L", "forearm.L": "upper_arm.L", "hand.L": "forearm.L",
    "shoulder.R": "chest", "upper_arm.R": "shoulder.R", "forearm.R": "upper_arm.R", "hand.R": "forearm.R",
    "thigh.L": "hips", "shin.L": "thigh.L", "foot.L": "shin.L", "toe.L": "foot.L",
    "thigh.R": "hips", "shin.R": "thigh.R", "foot.R": "shin.R", "toe.R": "foot.R",
}


@dataclass
class Proportions:
    """Body measurements as fractions of total height. Defaults read as a stylised adult."""

    height_m: float = 1.72
    # Vertical landmarks, as a fraction of height above the ground.
    ankle: float = 0.045
    knee: float = 0.285
    hip: float = 0.530
    waist: float = 0.605
    chest: float = 0.720
    shoulder: float = 0.800
    neck: float = 0.845
    head_centre: float = 0.925
    # Horizontal half-widths, as a fraction of height.
    hip_half: float = 0.062
    waist_half: float = 0.055
    chest_half: float = 0.083
    shoulder_half: float = 0.108
    head_half: float = 0.048
    # Depth, as a fraction of the matching half-width.
    depth_ratio: float = 0.78
    # Limb lengths, as a fraction of height.
    upper_arm: float = 0.155
    forearm: float = 0.145
    hand: float = 0.055
    thigh: float = 0.245
    shin: float = 0.240
    foot: float = 0.085
    # A-pose: degrees away from straight down.
    arm_spread: float = 22.0
    forearm_spread: float = 10.0

    @property
    def h(self) -> float:
        return self.height_m

    def scaled(self, height_m: float) -> "Proportions":
        return Proportions(**{**self.__dict__, "height_m": height_m})


@dataclass(frozen=True)
class Bone:
    name: str
    parent: str | None
    head: np.ndarray
    tail: np.ndarray

    @property
    def index(self) -> int:
        return BONE_NAMES.index(self.name)


def build_skeleton(p: Proportions) -> list[Bone]:
    """Rest skeleton in A-pose. Local rotations are identity, so animation angles are in body axes."""
    h = p.h
    x_hip = p.hip_half * h
    x_sh = p.shoulder_half * h
    z = p.depth_ratio

    def v(x: float, y: float, zed: float = 0.0) -> np.ndarray:
        return np.array([x, y * h, zed], dtype=np.float64)

    arm = math.radians(p.arm_spread)
    farm = math.radians(p.forearm_spread)
    elbow = np.array([
        x_sh * 0.92 + math.sin(arm) * p.upper_arm * h,
        p.shoulder * h - math.cos(arm) * p.upper_arm * h,
        0.0,
    ])
    wrist = np.array([
        elbow[0] + math.sin(farm) * p.forearm * h,
        elbow[1] - math.cos(farm) * p.forearm * h,
        0.0,
    ])
    hand_end = np.array([
        wrist[0] + math.sin(farm) * p.hand * h,
        wrist[1] - math.cos(farm) * p.hand * h,
        0.0,
    ])

    def side(sign: float) -> dict[str, tuple[np.ndarray, np.ndarray]]:
        sx = lambda a: np.array([a[0] * sign, a[1], a[2]])
        sx_rev = lambda a: np.array([a[0] * sign, a[1], a[2]])
        sh = v(x_sh * 0.30 * sign, p.shoulder)
        ua = v(x_sh * 0.86 * sign, p.shoulder - 0.006 * h)
        return {
            "shoulder": (sh, ua),
            "upper_arm": (ua, sx_rev(elbow)),
            "forearm": (sx(elbow), sx_rev(wrist)),
            "hand": (sx(wrist), sx_rev(hand_end)),
        }

    bones: list[Bone] = []

    def add(name: str, head: np.ndarray, tail: np.ndarray) -> None:
        bones.append(Bone(name, PARENTS[name], head, tail))

    hips_head = v(0.0, p.hip)
    add("hips", hips_head, v(0.0, p.waist))
    add("spine", v(0.0, p.waist), v(0.0, p.chest))
    add("chest", v(0.0, p.chest), v(0.0, p.shoulder))
    add("neck", v(0.0, p.neck), v(0.0, p.head_centre - 0.045))
    add("head", v(0.0, p.head_centre - 0.045), v(0.0, p.head_centre + 0.075))

    for label, sign in (("L", 1.0), ("R", -1.0)):
        parts = side(sign)
        add(f"shoulder.{label}", parts["shoulder"][0], parts["shoulder"][1])
        add(f"upper_arm.{label}", parts["upper_arm"][0], parts["upper_arm"][1])
        add(f"forearm.{label}", parts["forearm"][0], parts["forearm"][1])
        add(f"hand.{label}", parts["hand"][0], parts["hand"][1])

    for label, sign in (("L", 1.0), ("R", -1.0)):
        hip_j = v(x_hip * sign, p.hip)
        knee_j = v(x_hip * 0.86 * sign, p.knee)
        ankle_j = v(x_hip * 0.80 * sign, p.ankle)
        ball = np.array([ankle_j[0], p.ankle * h * 0.62, p.foot * h * 0.55])
        toe = np.array([ankle_j[0], p.ankle * h * 0.50, p.foot * h * 0.95])
        add(f"thigh.{label}", hip_j, knee_j)
        add(f"shin.{label}", knee_j, ankle_j)
        add(f"foot.{label}", ankle_j, ball)
        add(f"toe.{label}", ball, toe)

    return bones


def bone_lookup(bones: list[Bone]) -> dict[str, int]:
    return {b.name: b.index for b in bones}


def inverse_bind_matrices(bones: list[Bone]) -> np.ndarray:
    """Column-major 4x4 per bone. Rest rotations are identity, so these are pure translations."""
    out = np.zeros((len(bones), 16), dtype=np.float64)
    for i, bone in enumerate(bones):
        m = np.eye(4)
        m[0:3, 3] = -bone.head
        out[i] = m.T.reshape(16)  # glTF wants column-major
    return out


# ------------------------------------------------------------------------------------------------
# Mesh
# ------------------------------------------------------------------------------------------------


@dataclass
class Mesh:
    positions: np.ndarray          # (n, 3) float64
    colors: np.ndarray             # (n, 3) float64 in 0..1 (albedo)
    indices: np.ndarray            # (m*3,) int32
    weights: np.ndarray            # (n, 4) float64
    joints: np.ndarray             # (n, 4) int32
    normals: np.ndarray = field(default_factory=lambda: np.zeros((0, 3)))

    @property
    def triangle_count(self) -> int:
        return int(self.indices.size // 3)

    def with_normals(self) -> "Mesh":
        self.normals = vertex_normals(self.positions, self.indices)
        return self


def vertex_normals(positions: np.ndarray, indices: np.ndarray) -> np.ndarray:
    normals = np.zeros_like(positions)
    tri = indices.reshape(-1, 3)
    a, b, c = positions[tri[:, 0]], positions[tri[:, 1]], positions[tri[:, 2]]
    face = np.cross(b - a, c - a)
    for k in range(3):
        np.add.at(normals, tri[:, k], face)
    lengths = np.linalg.norm(normals, axis=1, keepdims=True)
    # Degenerate geometry can leave a vertex with no usable normal. Those fall back to the radial
    # direction from the body axis, which is always outward and always unit length after this.
    empty = lengths[:, 0] < 1e-9
    if empty.any():
        radial = positions[empty] - np.array([0.0, positions[empty][:, 1].mean(), 0.0])
        radial_len = np.linalg.norm(radial, axis=1, keepdims=True)
        radial_len[radial_len < 1e-9] = 1.0
        normals[empty] = np.array([0.0, 1.0, 0.0]) + radial / radial_len
    lengths = np.linalg.norm(normals, axis=1, keepdims=True)
    lengths[lengths < 1e-12] = 1.0
    return normals / lengths


class MeshBuilder:
    def __init__(self, bones: list[Bone], palette: dict[str, tuple[float, float, float]]):
        self.bones = bones
        self.palette = palette
        self.positions: list[np.ndarray] = []
        self.colors: list[tuple[float, float, float]] = []
        self.faces: list[tuple[int, int, int]] = []
        self._allowed: list[list[str]] = []

    # -- primitives -------------------------------------------------------------------------

    def add_quad(self, a: int, b: int, c: int, d: int) -> None:
        self.faces.append((a, b, c))
        self.faces.append((a, c, d))

    def add_tube(
        self,
        centres: list[np.ndarray],
        half_widths: list[float],
        depths: list[float],
        colour: tuple[float, float, float],
        allowed: list[str],
        segments: int = 10,
        cap_start: bool = True,
        cap_end: bool = True,
        shade: float = 1.0,
    ) -> None:
        """A lofted elliptical tube through `centres`. Rings are perpendicular to the local axis."""
        base = len(self.positions)
        n = len(centres)
        axes: list[tuple[np.ndarray, np.ndarray]] = []
        for i in range(n):
            if i == 0:
                tangent = centres[1] - centres[0]
            elif i == n - 1:
                tangent = centres[-1] - centres[-2]
            else:
                tangent = centres[i + 1] - centres[i - 1]
            tangent = tangent / (np.linalg.norm(tangent) or 1.0)
            # A stable frame: x along world X projected, z along the tangent's world-Z remainder.
            right = np.cross(tangent, np.array([0.0, 0.0, 1.0]))
            if np.linalg.norm(right) < 1e-6:
                right = np.array([1.0, 0.0, 0.0])
            right /= np.linalg.norm(right)
            forward = np.cross(right, tangent)
            forward /= np.linalg.norm(forward) or 1.0
            axes.append((right, forward))

        for i, centre in enumerate(centres):
            right, forward = axes[i]
            for s in range(segments):
                theta = 2.0 * math.pi * s / segments
                offset = right * (math.cos(theta) * half_widths[i]) + forward * (math.sin(theta) * depths[i])
                # Soft side shading baked into the albedo, the way a painter darkens a turning form.
                side = 0.90 + 0.10 * math.cos(theta)
                self.positions.append(centre + offset)
                self.colors.append(tuple(min(1.0, c * side * shade) for c in colour))
                self._allowed.append(allowed)

        for i in range(n - 1):
            for s in range(segments):
                s2 = (s + 1) % segments
                a = base + i * segments + s
                b = base + i * segments + s2
                c = base + (i + 1) * segments + s2
                d = base + (i + 1) * segments + s
                self.add_quad(a, b, c, d)

        if cap_start:
            centre_index = len(self.positions)
            self.positions.append(centres[0] - axes[0][1] * 0.0)
            self.colors.append(tuple(min(1.0, c * shade * 0.94) for c in colour))
            self._allowed.append(allowed)
            for s in range(segments):
                s2 = (s + 1) % segments
                self.faces.append((centre_index, base + s2, base + s))
        if cap_end:
            ring = base + (n - 1) * segments
            centre_index = len(self.positions)
            self.positions.append(centres[-1])
            self.colors.append(tuple(min(1.0, c * shade * 0.96) for c in colour))
            self._allowed.append(allowed)
            for s in range(segments):
                s2 = (s + 1) % segments
                self.faces.append((centre_index, ring + s, ring + s2))

    def add_ellipsoid(
        self,
        centre: np.ndarray,
        radii: tuple[float, float, float],
        colour: tuple[float, float, float],
        allowed: list[str],
        rings: int = 8,
        segments: int = 12,
        shade: float = 1.0,
        theta_min: float = 0.0,
        theta_max: float = math.pi,
        phi_from: float = 0.0,
        phi_to: float = math.tau,
    ) -> None:
        base = len(self.positions)
        rx, ry, rz = radii
        rows = rings + 1
        for i in range(rows):
            theta = theta_min + (theta_max - theta_min) * i / rings
            for s in range(segments):
                phi = phi_from + (phi_to - phi_from) * s / max(segments - 1, 1)
                normal = np.array(
                    [math.sin(theta) * math.cos(phi), math.cos(theta), math.sin(theta) * math.sin(phi)]
                )
                self.positions.append(
                    centre + np.array([normal[0] * rx, normal[1] * ry, normal[2] * rz])
                )
                side = 0.90 + 0.10 * math.cos(phi)
                self.colors.append(tuple(min(1.0, c * side * shade) for c in colour))
                self._allowed.append(allowed)
        # A full ellipsoid wraps its last ring segment back to the first. A partial wedge must not: the
        # wrap would stitch the two cut edges together and produce a pinched, zero-area sliver across
        # the whole wedge, which shows up as black or unlit triangles in any renderer.
        full_turn = abs((phi_to - phi_from) - math.tau) < 1e-9
        span = segments if full_turn else max(segments - 1, 1)
        for i in range(rings):
            for s in range(span):
                s2 = (s + 1) % segments
                a = base + i * segments + s
                b = base + i * segments + s2
                c = base + (i + 1) * segments + s2
                d = base + (i + 1) * segments + s
                self.add_quad(a, b, c, d)

    # -- finish -----------------------------------------------------------------------------

    def build(self, falloff: float = 0.055) -> Mesh:
        positions = np.array(self.positions, dtype=np.float64)
        colours = np.array(self.colors, dtype=np.float64)
        indices = np.array([i for f in self.faces for i in f], dtype=np.int32)

        # No zero-area triangle leaves this builder. A collapsed cap or a pinched seam is invisible in
        # the mesh and poisonous downstream: it has no normal, so it shades black, and some engines
        # reject the primitive outright. Cull the faces and the vertices they were the only users of.
        tri = indices.reshape(-1, 3)
        a, b, c = positions[tri[:, 0]], positions[tri[:, 1]], positions[tri[:, 2]]
        keep = np.linalg.norm(np.cross(b - a, c - a), axis=1) > 1e-12
        if not keep.all():
            tri = tri[keep]
            used = np.unique(tri)
            remap = np.full(len(positions), -1, dtype=np.int64)
            remap[used] = np.arange(len(used))
            positions = positions[used]
            colours = colours[used]
            # The per-vertex bone allowlists must be remapped with the vertices, or every part after the
            # first culled face is skinned to the wrong limb (a thigh that follows the forearm).
            self._allowed = [self._allowed[i] for i in used]
            indices = remap[tri].ravel().astype(np.int32)

        weights, joints = self._skin(positions, falloff)
        return Mesh(positions, colours, indices, weights, joints).with_normals()

    def _skin(self, positions: np.ndarray, falloff: float) -> tuple[np.ndarray, np.ndarray]:
        """Distance-to-bone weights, restricted to the bones a part is allowed to follow."""
        segments = [(b.head, b.tail, b.index) for b in self.bones]
        weights = np.zeros((len(positions), 4), dtype=np.float64)
        joints = np.zeros((len(positions), 4), dtype=np.int32)

        for i, position in enumerate(positions):
            allowed = set(self._allowed[i])
            candidates: list[tuple[float, int]] = []
            for head, tail, index in segments:
                name = BONE_NAMES[index]
                if name not in allowed:
                    continue
                d = _distance_to_segment(position, head, tail)
                candidates.append((d, index))
            if not candidates:
                candidates = [(_distance_to_segment(position, h, t), idx) for h, t, idx in segments]
            candidates.sort()
            candidates = candidates[:4]
            scored = [(1.0 / (d + falloff) ** 3, idx) for d, idx in candidates]
            total = sum(s for s, _ in scored)
            scaled = sorted(((s / total, idx) for s, idx in scored), reverse=True)
            scaled = [(s, idx) for s, idx in scaled if s >= 0.02] or [scaled[0]]
            total = sum(s for s, _ in scaled)
            for slot, (s, idx) in enumerate(scaled[:4]):
                weights[i, slot] = s / total
                joints[i, slot] = idx
        return weights, joints


def _distance_to_segment(point: np.ndarray, head: np.ndarray, tail: np.ndarray) -> float:
    direction = tail - head
    length_sq = float(direction @ direction)
    if length_sq < 1e-12:
        return float(np.linalg.norm(point - head))
    t = float((point - head) @ direction / length_sq)
    t = max(0.0, min(1.0, t))
    return float(np.linalg.norm(point - (head + t * direction)))


# ------------------------------------------------------------------------------------------------
# The avatar
# ------------------------------------------------------------------------------------------------


@dataclass
class Avatar:
    proportions: Proportions
    skeleton: list[Bone]
    mesh: Mesh
    palette: dict[str, tuple[float, float, float]]


def default_palette() -> dict[str, tuple[float, float, float]]:
    """A calm, slightly stylised palette — the fallback when a photo gives us nothing usable."""
    return {
        "skin": (0.85, 0.66, 0.52),
        "hair": (0.20, 0.15, 0.13),
        "top": (0.24, 0.33, 0.48),
        "trousers": (0.20, 0.22, 0.28),
        "shoes": (0.11, 0.12, 0.15),
    }


def build_avatar(proportions: Proportions, palette: dict[str, tuple[float, float, float]]) -> Avatar:
    """A low-poly humanoid in A-pose, rigged by construction."""
    p = proportions
    h = p.h
    bones = build_skeleton(p)
    builder = MeshBuilder(bones, palette)

    skin = palette["skin"]
    top = palette["top"]
    trousers = palette["trousers"]
    shoes = palette["shoes"]
    hair = palette["hair"]

    torso_bones = ["hips", "spine", "chest"]
    # Torso: pelvis to shoulders, elliptical rings.
    rings_y = [p.hip - 0.02, p.hip + 0.01, p.waist, (p.waist + p.chest) / 2, p.chest, p.shoulder]
    rings_half = [
        p.hip_half * 1.02, p.hip_half, p.waist_half, (p.waist_half + p.chest_half) / 2,
        p.chest_half, p.shoulder_half * 0.92,
    ]
    centres = [np.array([0.0, y * h, 0.0]) for y in rings_y]
    widths = [w * h for w in rings_half]
    depths = [w * p.depth_ratio for w in widths]
    builder.add_tube(centres, widths, depths, top, torso_bones, segments=12, shade=1.0)

    # Pelvis / trouser seat, so the top reads as a garment over trousers.
    seat_centres = [np.array([0.0, (p.hip - 0.055) * h, 0.0]), np.array([0.0, (p.hip + 0.005) * h, 0.0])]
    builder.add_tube(
        seat_centres,
        [p.hip_half * 0.98 * h, p.hip_half * 0.99 * h],
        [p.hip_half * 0.95 * p.depth_ratio * h] * 2,
        trousers,
        ["hips", "thigh.L", "thigh.R"],
        segments=12,
    )

    # Neck.
    neck_centres = [
        np.array([0.0, (p.shoulder - 0.005) * h, 0.0]),
        np.array([0.0, (p.neck + 0.01) * h, 0.0]),
    ]
    builder.add_tube(neck_centres, [0.030 * h, 0.028 * h], [0.026 * h, 0.024 * h], skin, ["neck", "chest"], segments=10)

    # Head and hair.
    head_centre = np.array([0.0, p.head_centre * h, 0.004 * h])
    head_rx = p.head_half * h
    head_ry = 0.066 * h
    head_rz = p.head_half * 0.92 * h
    builder.add_ellipsoid(head_centre, (head_rx, head_ry, head_rz), skin, ["head", "neck"], rings=9, segments=14)
    # Hair: a cap over the crown and the back, tilted back a little.
    hair_centre = head_centre + np.array([0.0, 0.008 * h, -0.006 * h])
    builder.add_ellipsoid(
        hair_centre,
        (head_rx * 1.035, head_ry * 1.02, head_rz * 1.045),
        hair,
        ["head"],
        rings=6,
        segments=14,
        theta_min=0.0,
        theta_max=math.radians(108.0),
        shade=0.98,
    )
    # Back of the hair, down the nape.
    builder.add_ellipsoid(
        hair_centre + np.array([0.0, -0.012 * h, -0.012 * h]),
        (head_rx * 0.98, head_ry * 0.92, head_rz * 0.72),
        hair,
        ["head"],
        rings=5,
        segments=12,
        theta_min=math.radians(70.0),
        theta_max=math.radians(150.0),
        shade=0.94,
    )

    # Arms and legs, mirrored.
    for label, sign in (("L", 1.0), ("R", -1.0)):
        shoulder_x = sign * p.shoulder_half * h * 0.86
        shoulder_y = (p.shoulder - 0.006) * h
        spread = math.radians(p.arm_spread)
        elbow = np.array([
            shoulder_x + sign * math.sin(spread) * p.upper_arm * h,
            shoulder_y - math.cos(spread) * p.upper_arm * h,
            0.0,
        ])
        fore = math.radians(p.forearm_spread)
        wrist = np.array([
            elbow[0] + sign * math.sin(fore) * p.forearm * h,
            elbow[1] - math.cos(fore) * p.forearm * h,
            0.0,
        ])
        hand_end = np.array([
            wrist[0] + sign * math.sin(fore) * p.hand * h * 0.9,
            wrist[1] - math.cos(fore) * p.hand * h * 0.9,
            0.0,
        ])
        upper_bones = [f"shoulder.{label}", f"upper_arm.{label}", f"forearm.{label}", f"hand.{label}"]
        arm_centres = [np.array([shoulder_x, shoulder_y, 0.0]), elbow, wrist]
        arm_widths = [0.039 * h, 0.029 * h, 0.023 * h]
        builder.add_tube(
            arm_centres,
            arm_widths,
            [w * 0.92 for w in arm_widths],
            top,
            upper_bones[:3],
            segments=10,
        )
        builder.add_ellipsoid(
            np.array([shoulder_x * 0.97, shoulder_y - 0.010 * h, 0.0]),
            (0.033 * h, 0.030 * h, 0.032 * h),
            top,
            [f"shoulder.{label}", f"upper_arm.{label}"],
            rings=6,
            segments=10,
            shade=1.02,
        )
        builder.add_ellipsoid(
            (wrist + hand_end) / 2.0,
            (0.026 * h, 0.033 * h, 0.021 * h),
            skin,
            [f"hand.{label}", f"forearm.{label}"],
            rings=6,
            segments=10,
            shade=1.0,
        )

        hip_j = np.array([sign * p.hip_half * h, p.hip * h, 0.0])
        knee_j = np.array([sign * p.hip_half * 0.86 * h, p.knee * h, 0.004 * h])
        ankle_j = np.array([sign * p.hip_half * 0.80 * h, p.ankle * h, 0.0])
        leg_bones = [f"thigh.{label}", f"shin.{label}", f"foot.{label}"]
        leg_widths = [0.057 * h, 0.041 * h, 0.030 * h]
        builder.add_tube(
            [hip_j, knee_j, ankle_j],
            leg_widths,
            [w * 0.95 for w in leg_widths],
            trousers,
            leg_bones,
            segments=10,
        )
        # Shoe: a low lofted last from heel to toe, flat on the grass line.
        heel = np.array([ankle_j[0], 0.030 * h, -0.028 * h])
        mid = np.array([ankle_j[0], 0.028 * h, 0.052 * h])
        toe = np.array([ankle_j[0], 0.026 * h, 0.108 * h])
        builder.add_tube(
            [heel, mid, toe],
            [0.029 * h, 0.031 * h, 0.023 * h],
            [0.028 * h, 0.026 * h, 0.019 * h],
            shoes,
            [f"foot.{label}", f"shin.{label}"],
            segments=10,
        )

    return Avatar(p, bones, builder.build(), palette)
