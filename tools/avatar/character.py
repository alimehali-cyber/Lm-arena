"""Spec-driven character builder: proportions, palette and features in, a rigged mesh out.

The earlier `build_avatar` makes a generic humanoid from five colours. This module makes a *specific*
character: a spec adds features that a silhouette needs in order to be recognisable in the Home scene
at ~170 px — glasses, a bow tie, lapels, a belt, cuffed boots, a messy head of hair, ears, a nose, a
mouth and irises.

Everything is still one skinned mesh with vertex colours, so it is one primitive, one material and one
draw call, and it needs no texture pipeline. Detail lives in the silhouette, which is what survives
being drawn 170 px tall.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np

from avatar_core import (
    BONE_NAMES,
    PARENTS,
    Avatar,
    Bone,
    MeshBuilder,
    Proportions,
    build_skeleton,
    default_palette,
)

# ------------------------------------------------------------------------------------------------
# Spec
# ------------------------------------------------------------------------------------------------


@dataclass
class Features:
    """What the character has, not how they look. Each one is geometry, not a texture."""

    glasses: bool = True
    bow_tie: bool = True
    lapels: bool = True
    belt: bool = True
    cuffs: bool = True
    boots: bool = True
    hair_spikes: int = 9
    hair_volume: float = 1.10
    ears: bool = True
    nose: bool = True
    mouth: str = "smile"  # smile | neutral | open
    brows: str = "playful"  # playful | flat | raised
    jacket_open: bool = True  # shows a shirt panel between the lapels


@dataclass
class CharacterSpec:
    name: str
    height_m: float = 1.80
    proportions: dict = field(default_factory=dict)
    palette: dict[str, list[float]] = field(default_factory=dict)
    features: dict = field(default_factory=dict)

    def proportions_obj(self) -> Proportions:
        base = Proportions()
        values = {**base.__dict__}
        for key, value in self.proportions.items():
            if key in Proportions.__dataclass_fields__:
                values[key] = float(value)
        values["height_m"] = self.height_m
        return Proportions(**values)

    def palette_obj(self) -> dict[str, tuple[float, float, float]]:
        merged = dict(default_palette())
        merged.update({
            "skin_shadow": (0.72, 0.52, 0.42),
            "shirt": (0.93, 0.91, 0.87),
            "tie": (0.42, 0.24, 0.14),
            "jacket_dark": (0.24, 0.16, 0.10),
            "belt": (0.26, 0.18, 0.12),
            "sole": (0.30, 0.22, 0.16),
            "glasses": (0.16, 0.13, 0.11),
            "eye_white": (0.96, 0.95, 0.93),
            "iris": (0.24, 0.16, 0.10),
            "pupil": (0.06, 0.05, 0.05),
            "mouth": (0.42, 0.20, 0.20),
            "hair_hi": (0.20, 0.15, 0.13),
            "brow": (0.13, 0.10, 0.09),
        })
        for key, value in self.palette.items():
            if key in merged and len(value) == 3:
                merged[key] = tuple(min(1.0, max(0.0, float(v))) for v in value)
        return merged

    def features_obj(self) -> Features:
        allowed = Features.__dataclass_fields__
        return Features(**{k: v for k, v in self.features.items() if k in allowed})

    @staticmethod
    def load(path: Path) -> "CharacterSpec":
        data = json.loads(Path(path).read_text())
        return CharacterSpec(
            name=data.get("name", "character"),
            height_m=float(data.get("height_m", 1.80)),
            proportions=data.get("proportions", {}),
            palette=data.get("palette", {}),
            features=data.get("features", {}),
        )


# ------------------------------------------------------------------------------------------------
# Extra primitives
# ------------------------------------------------------------------------------------------------


def add_torus(
    builder: MeshBuilder,
    centre: np.ndarray,
    ring_radius: float,
    tube_radius: float,
    colour: tuple[float, float, float],
    allowed: list[str],
    facing: str = "z",
    major: int = 18,
    minor: int = 6,
    arc: float = math.tau,
) -> None:
    """A ring — glasses rims, and anything else circular that has to read at a glance."""
    base = len(builder.positions)
    axis = np.array([0.0, 0.0, 1.0]) if facing == "z" else np.array([1.0, 0.0, 0.0])
    e1 = np.array([1.0, 0.0, 0.0]) if facing == "z" else np.array([0.0, 0.0, 1.0])
    e2 = np.cross(axis, e1)
    for i in range(major + 1):
        theta = arc * i / major
        outward = e1 * math.cos(theta) + e2 * math.sin(theta)
        ring_centre = centre + outward * ring_radius
        for j in range(minor):
            phi = math.tau * j / minor
            offset = outward * (math.cos(phi) * tube_radius) + axis * (math.sin(phi) * tube_radius)
            builder.positions.append(ring_centre + offset)
            side = 0.88 + 0.12 * math.cos(phi)
            builder.colors.append(tuple(min(1.0, c * side) for c in colour))
            builder._allowed.append(allowed)
    for i in range(major):
        for j in range(minor):
            j2 = (j + 1) % minor
            a = base + i * minor + j
            b = base + i * minor + j2
            c = base + (i + 1) * minor + j2
            d = base + (i + 1) * minor + j
            builder.add_quad(a, b, c, d)


def add_box(
    builder: MeshBuilder,
    centre: np.ndarray,
    half: tuple[float, float, float],
    colour: tuple[float, float, float],
    allowed: list[str],
    yaw: float = 0.0,
    shade: float = 1.0,
) -> None:
    """An axis-aligned (optionally yawed) box: brows, mouth bars, tie wings, bag straps."""
    hx, hy, hz = half
    corners = []
    for sx, sy, sz in (
        (-1, -1, -1), (1, -1, -1), (1, 1, -1), (-1, 1, -1),
        (-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1),
    ):
        local = np.array([sx * hx, sy * hy, sz * hz])
        cos, sin = math.cos(yaw), math.sin(yaw)
        rotated = np.array([local[0] * cos - local[2] * sin, local[1], local[0] * sin + local[2] * cos])
        corners.append(centre + rotated)
    base = len(builder.positions)
    for index, corner in enumerate(corners):
        builder.positions.append(corner)
        # Cheap face shading: the front and top faces read brighter than the sides.
        brightness = 1.0 if index in (4, 5, 6, 7) else (0.86 if index in (2, 3, 6, 7) else 0.74)
        builder.colors.append(tuple(min(1.0, c * brightness * shade) for c in colour))
        builder._allowed.append(allowed)
    for quad in ((0, 1, 2, 3), (4, 7, 6, 5), (0, 4, 5, 1), (1, 5, 6, 2), (2, 6, 7, 3), (3, 7, 4, 0)):
        builder.add_quad(*(base + i for i in quad))


def _lerp_colour(a, b, t: float):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


# ------------------------------------------------------------------------------------------------
# The character
# ------------------------------------------------------------------------------------------------


def build_character(spec: CharacterSpec) -> Avatar:
    p = spec.proportions_obj()
    palette = spec.palette_obj()
    features = spec.features_obj()
    h = p.h
    bones = build_skeleton(p)
    builder = MeshBuilder(bones, palette)

    skin = palette["skin"]
    skin_shadow = palette["skin_shadow"]
    hair = palette["hair"]
    hair_hi = palette["hair_hi"]
    jacket = palette["top"]
    jacket_dark = palette["jacket_dark"]
    shirt = palette["shirt"]
    tie = palette["tie"]
    trousers = palette["trousers"]
    boots = palette["shoes"]
    sole = palette["sole"]
    glasses = palette["glasses"]

    torso_bones = ["hips", "spine", "chest"]

    # --- torso: jacket ---------------------------------------------------------------
    rings_y = [p.hip - 0.055, p.hip - 0.02, p.waist, (p.waist + p.chest) / 2, p.chest, p.chest + 0.035, p.shoulder]
    rings_half = [
        p.hip_half * 1.12, p.hip_half * 1.06, p.waist_half * 1.04, (p.waist_half + p.chest_half) / 2,
        p.chest_half * 1.04, p.chest_half * 0.98, p.shoulder_half * 0.72,
    ]
    centres = [np.array([0.0, y * h, 0.0]) for y in rings_y]
    widths = [w * h for w in rings_half]
    depths = [w * p.depth_ratio for w in widths]
    builder.add_tube(centres, widths, depths, jacket, torso_bones, segments=14)

    # Shirt panel in the V of the open jacket. A thin wedge only: it has to break the jacket surface
    # enough to read as an opening at 170 px, and not one millimetre more.
    if features.jacket_open:
        v_top = p.shoulder - 0.020
        v_bottom = p.chest - 0.052
        v = [
            np.array([0.0, v_bottom * h, (p.chest_half * 0.90) * h]),
            np.array([0.0, (v_bottom + (v_top - v_bottom) * 0.55) * h, (p.chest_half * 0.94) * h]),
            np.array([0.0, v_top * h, (p.chest_half * 0.72) * h]),
        ]
        builder.add_tube(
            v,
            [0.010 * h, 0.026 * h, 0.032 * h],
            [0.012 * h, 0.016 * h, 0.014 * h],
            shirt,
            ["chest", "neck"],
            segments=8,
            cap_start=False,
            cap_end=False,
        )
        # Lapels: two wedges framing the V. They stay below the shoulder line.
        for sign in (-1.0, 1.0):
            add_box(
                builder,
                np.array([sign * 0.047 * h, (p.chest - 0.012) * h, (p.chest_half * 0.80) * h]),
                (0.013 * h, 0.038 * h, 0.004 * h),
                _lerp_colour(jacket, jacket_dark, 0.35),
                ["chest"],
                yaw=sign * 0.42,
            )
        # Button line down the jacket front, below the V.
        for i in range(2):
            add_box(
                builder,
                np.array([0.004 * h, (p.chest - 0.105 - i * 0.050) * h, p.chest_half * 0.84 * h]),
                (0.0042 * h, 0.0042 * h, 0.004 * h),
                jacket_dark,
                ["chest" if i == 0 else "spine"],
            )
        # Collar band.
        builder.add_tube(
            [np.array([0.0, (p.shoulder - 0.016) * h, 0.0]), np.array([0.0, (p.shoulder + 0.004) * h, 0.0])],
            [0.050 * h, 0.044 * h],
            [0.040 * h, 0.036 * h],
            jacket_dark,
            ["chest", "neck"],
            segments=12,
        )

    # --- bow tie ---------------------------------------------------------------------
    if features.bow_tie:
        knot_y = (p.neck - 0.048) * h
        add_box(builder, np.array([0.0, knot_y, 0.058 * h]), (0.008 * h, 0.009 * h, 0.007 * h), tie, ["neck"])
        for sign in (-1.0, 1.0):
            add_box(
                builder,
                np.array([sign * 0.022 * h, knot_y, 0.057 * h]),
                (0.016 * h, 0.014 * h, 0.005 * h),
                _lerp_colour(tie, (1.0, 1.0, 1.0), 0.08),
                ["neck"],
                yaw=sign * 0.22,
            )

    # --- belt ------------------------------------------------------------------------
    if features.belt:
        builder.add_tube(
            [np.array([0.0, (p.hip - 0.028) * h, 0.0]), np.array([0.0, (p.hip - 0.008) * h, 0.0])],
            [p.hip_half * 1.08 * h, p.hip_half * 1.08 * h],
            [p.hip_half * 1.10 * p.depth_ratio * h] * 2,
            palette["belt"],
            ["hips"],
            segments=14,
        )
        add_box(
            builder,
            np.array([0.0, (p.hip - 0.018) * h, p.hip_half * p.depth_ratio * 1.02 * h]),
            (0.009 * h, 0.008 * h, 0.005 * h),
            (0.62, 0.52, 0.28),
            ["hips"],
        )

    # --- hips / trousers seat --------------------------------------------------------
    builder.add_tube(
        [np.array([0.0, (p.hip - 0.075) * h, 0.0]), np.array([0.0, (p.hip - 0.010) * h, 0.0])],
        [p.hip_half * 0.99 * h, p.hip_half * 1.00 * h],
        [p.hip_half * 0.96 * p.depth_ratio * h] * 2,
        trousers,
        ["hips", "thigh.L", "thigh.R"],
        segments=14,
    )

    # --- neck and head ---------------------------------------------------------------
    builder.add_tube(
        [np.array([0.0, (p.shoulder - 0.010) * h, 0.0]), np.array([0.0, (p.neck + 0.012) * h, 0.002 * h])],
        [0.030 * h, 0.027 * h],
        [0.028 * h, 0.025 * h],
        skin_def := skin,
        ["neck", "chest"],
        segments=12,
    )

    head_centre = np.array([0.0, p.head_centre * h, 0.004 * h])
    head_rx = p.head_half * h
    head_ry = p.head_half * 1.12 * h
    head_rz = p.head_half * 1.02 * h
    builder.add_ellipsoid(head_centre, (head_rx, head_ry, head_rz), skin, ["head"], rings=14, segments=22)
    # Jaw and chin: a stylised face tapers to a small chin, which is most of what stops a head reading
    # as a ball.
    builder.add_ellipsoid(
        head_centre + np.array([0.0, -head_ry * 0.06, -0.004 * h]),
        (head_rx * 0.94, head_ry * 1.00, head_rz * 0.94),
        skin,
        ["head"],
        rings=8,
        segments=18,
        theta_min=math.radians(62.0),
        theta_max=math.radians(178.0),
        shade=0.97,
    )

    def surface_z(y_world: float) -> float:
        """Front of the head at a given world height — features are placed against this, not guessed."""
        local = (y_world - head_centre[1]) / head_ry
        return head_centre[2] + head_rz * math.sqrt(max(0.0, 1.0 - min(1.0, abs(local)) ** 2))

    eye_y = head_centre[1] + 0.014 * h
    eye_x = 0.034 * h
    eye_z = surface_z(eye_y)

    # Ears.
    if features.ears:
        for sign in (-1.0, 1.0):
            builder.add_ellipsoid(
                np.array([sign * head_rx * 0.99, head_centre[1] - 0.004 * h, head_centre[2] - 0.006 * h]),
                (0.011 * h, 0.022 * h, 0.014 * h),
                skin_shadow,
                ["head"],
                rings=6,
                segments=8,
            )

    # Eyes: white, iris, pupil, each stepped forward so the stack reads at any size.
    for sign in (-1.0, 1.0):
        eye_centre = np.array([sign * eye_x, eye_y, eye_z - 0.008 * h])
        builder.add_ellipsoid(eye_centre, (0.0105 * h, 0.0115 * h, 0.0090 * h), palette["eye_white"], ["head"], rings=6, segments=10)
        builder.add_ellipsoid(
            eye_centre + np.array([0.0, 0.0, 0.0060 * h]),
            (0.0060 * h, 0.0066 * h, 0.0040 * h),
            palette["iris"],
            ["head"],
            rings=5,
            segments=10,
        )
        builder.add_ellipsoid(
            eye_centre + np.array([0.0, 0.0, 0.0086 * h]),
            (0.0030 * h, 0.0032 * h, 0.0020 * h),
            palette["pupil"],
            ["head"],
            rings=4,
            segments=8,
        )
        builder.add_ellipsoid(
            eye_centre + np.array([-sign * 0.0026 * h, 0.0032 * h, 0.0094 * h]),
            (0.0016 * h, 0.0016 * h, 0.0012 * h),
            (1.0, 1.0, 1.0),
            ["head"],
            rings=3,
            segments=6,
        )

    # Brows, sitting on the brow ridge above the glasses.
    brow_colour = palette["brow"]
    brow_y = eye_y + 0.0255 * h
    for sign in (-1.0, 1.0):
        tilt = 0.18 if features.brows == "playful" else (0.0 if features.brows == "flat" else -0.14)
        add_box(
            builder,
            np.array([sign * eye_x, brow_y, surface_z(brow_y) - 0.004 * h]),
            (0.0140 * h, 0.0030 * h, 0.0040 * h),
            brow_colour,
            ["head"],
            yaw=-sign * 0.16,
        )
        if features.brows != "flat":
            # Outer end lifted: the half-raised look of someone who just had an idea.
            add_box(
                builder,
                np.array([sign * (eye_x + 0.0125 * h), brow_y + 0.0035 * h + tilt * 0.004 * h, surface_z(brow_y) - 0.011 * h]),
                (0.0055 * h, 0.0024 * h, 0.0032 * h),
                brow_colour,
                ["head"],
                yaw=-sign * 0.55,
            )

    # Nose.
    nose_y = eye_y - 0.020 * h
    if features.nose:
        builder.add_ellipsoid(
            np.array([0.0, nose_y, surface_z(nose_y) - 0.004 * h]),
            (0.0090 * h, 0.0125 * h, 0.0100 * h),
            skin,
            ["head"],
            rings=6,
            segments=10,
            shade=1.04,
        )

    # Mouth.
    mouth_y = nose_y - 0.022 * h
    mouth_colour = palette["mouth"]
    if features.mouth == "smile":
        for i in range(5):
            t = (i - 2) / 2.0
            add_box(
                builder,
                np.array([t * 0.0130 * h, mouth_y + (t * t) * 0.0040 * h, surface_z(mouth_y) - 0.0030 * h]),
                (0.0034 * h, 0.0022 * h, 0.0024 * h),
                mouth_colour,
                ["head"],
            )
    elif features.mouth == "open":
        builder.add_ellipsoid(
            np.array([0.0, mouth_y, surface_z(mouth_y) - 0.0020 * h]),
            (0.0110 * h, 0.0075 * h, 0.0050 * h),
            mouth_colour,
            ["head"],
            rings=5,
            segments=10,
        )
    else:
        add_box(builder, np.array([0.0, mouth_y, surface_z(mouth_y) - 0.0020 * h]), (0.0110 * h, 0.0018 * h, 0.0030 * h), mouth_colour, ["head"])

    # --- glasses ---------------------------------------------------------------------
    if features.glasses:
        rim_r = 0.0235 * h
        for sign in (-1.0, 1.0):
            add_torus(
                builder,
                np.array([sign * eye_x, eye_y, eye_z - 0.002 * h]),
                rim_r,
                0.0019 * h,
                glasses,
                ["head"],
                major=20,
                minor=6,
            )
            # Temple arm, back towards the ear.
            builder.add_tube(
                [
                    np.array([sign * (eye_x + rim_r), eye_y + 0.002 * h, eye_z - 0.010 * h]),
                    np.array([sign * head_rx * 1.02, eye_y + 0.004 * h, head_centre[2] - 0.010 * h]),
                ],
                [0.0016 * h, 0.0016 * h],
                [0.0016 * h, 0.0016 * h],
                glasses,
                ["head"],
                segments=6,
            )
        # Bridge, with the small nose-pad dip the reference has.
        builder.add_tube(
            [
                np.array([-eye_x + rim_r * 0.8, eye_y + 0.004 * h, eye_z - 0.002 * h]),
                np.array([0.0, eye_y + 0.006 * h, eye_z + 0.001 * h]),
                np.array([eye_x - rim_r * 0.8, eye_y + 0.004 * h, eye_z - 0.002 * h]),
            ],
            [0.0018 * h] * 3,
            [0.0018 * h] * 3,
            glasses,
            ["head"],
            segments=6,
        )

    # --- hair ---
    # Three parts and nothing clever: a cap that follows the skull, a raised mass over the crown (the
    # quiff), and flat tufts that break the outline. The cap stops above the glasses.
    hair_centre = head_centre + np.array([0.0, 0.010 * h, -0.004 * h])
    volume = features.hair_volume
    builder.add_ellipsoid(
        hair_centre,
        (head_rx * 1.075 * volume, head_ry * 1.04 * volume, head_rz * 1.075 * volume),
        hair,
        ["head"],
        rings=7,
        segments=18,
        theta_min=0.0,
        theta_max=math.radians(72.0),
        shade=0.97,
    )
    # Quiff: the raised mass, tipped forward over the brow.
    builder.add_ellipsoid(
        hair_centre + np.array([0.0, head_ry * 0.80, head_rz * 0.02]),
        (head_rx * 1.00 * volume, head_ry * 0.55 * volume, head_rz * 0.95 * volume),
        hair,
        ["head"],
        rings=8,
        segments=16,
        shade=1.0,
    )
    # Back of the head, down to the nape.
    builder.add_ellipsoid(
        hair_centre + np.array([0.0, -head_ry * 0.16, -head_rz * 0.10]),
        (head_rx * 1.06 * volume, head_ry * 0.92 * volume, head_rz * 0.92 * volume),
        hair,
        ["head"],
        rings=6,
        segments=16,
        theta_min=0.0,
        theta_max=math.radians(120.0),
        shade=0.93,
        phi_from=math.radians(95.0),
        phi_to=math.radians(265.0),
    )
    # Temples and sideburns: they cover the cap's hard lower edge, which otherwise reads as a band.
    for sign in (-1.0, 1.0):
        builder.add_ellipsoid(
            hair_centre + np.array([sign * head_rx * 0.86, -head_ry * 0.16, -head_rz * 0.20]),
            (head_rx * 0.24, head_ry * 0.52, head_rz * 0.42),
            hair,
            ["head"],
            rings=6,
            segments=10,
            shade=0.94,
        )
    # Tufts: flat-bladed, leaning outward and up, deterministic.
    rng = np.random.default_rng(11)
    spikes = max(0, int(features.hair_spikes))
    for i in range(spikes):
        phi = math.tau * i / max(spikes, 1) + 0.45
        lean = 0.55 + 0.25 * math.sin(i * 1.9)
        length = (0.040 + 0.018 * rng.random()) * h * volume
        ring = 0.68 if i % 4 else 0.40
        base = hair_centre + np.array([
            math.cos(phi) * head_rx * ring,
            head_ry * (1.02 - 0.34 * (ring < 0.5)),
            math.sin(phi) * head_rz * ring,
        ])
        tip = base + np.array([
            math.cos(phi) * length * (lean + 0.45),
            length * (0.90 + 0.35 * rng.random()),
            math.sin(phi) * length * (lean + 0.45) - length * 0.25,
        ])
        builder.add_tube(
            [base, base * 0.4 + tip * 0.6, tip],
            [0.020 * h, 0.013 * h, 0.004 * h],
            [0.014 * h, 0.009 * h, 0.003 * h],
            hair_hi if i % 3 == 0 else hair,
            ["head"],
            segments=6,
            cap_start=False,
        )

    # --- arms ------------------------------------------------------------------------
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
            wrist[0] + sign * math.sin(fore) * p.hand * h * 0.85,
            wrist[1] - math.cos(fore) * p.hand * h * 0.85,
            0.0,
        ])
        arm_bones = [f"shoulder.{label}", f"upper_arm.{label}", f"forearm.{label}", f"hand.{label}"]
        # Sleeve, starting inside the torso so the shoulder is one continuous form.
        arm_widths = [0.036 * h, 0.027 * h, 0.021 * h]
        builder.add_tube(
            [
                np.array([shoulder_x * 0.70, shoulder_y + 0.006 * h, 0.0]),
                np.array([shoulder_x * 0.94, shoulder_y - 0.030 * h, 0.0]),
                elbow,
                wrist,
            ],
            [arm_widths[0] * 0.80, arm_widths[0], arm_widths[1], arm_widths[2]],
            [w * 0.95 for w in (arm_widths[0] * 0.80, arm_widths[0], arm_widths[1], arm_widths[2])],
            jacket,
            arm_bones[:3],
            segments=10,
            cap_start=False,
        )
        # Cuff and hand.
        if features.cuffs:
            builder.add_tube(
                [wrist + (wrist - elbow) * -0.045, wrist + (wrist - elbow) * 0.010],
                [0.0225 * h, 0.0225 * h],
                [0.0215 * h, 0.0215 * h],
                shirt,
                [f"forearm.{label}", f"hand.{label}"],
                segments=10,
            )
        builder.add_ellipsoid(
            (wrist + hand_end) / 2.0,
            (0.021 * h, 0.030 * h, 0.016 * h),
            skin,
            [f"hand.{label}", f"forearm.{label}"],
            rings=6,
            segments=10,
        )

        # --- legs --------------------------------------------------------------------
        hip_j = np.array([sign * p.hip_half * h, p.hip * h, 0.0])
        knee_j = np.array([sign * p.hip_half * 0.86 * h, p.knee * h, 0.004 * h])
        ankle_j = np.array([sign * p.hip_half * 0.80 * h, p.ankle * h, 0.0])
        leg_widths = [0.046 * h, 0.036 * h, 0.027 * h]
        builder.add_tube(
            [hip_j, knee_j, ankle_j],
            leg_widths,
            [w * 0.96 for w in leg_widths],
            trousers,
            [f"thigh.{label}", f"shin.{label}", f"foot.{label}"],
            segments=12,
        )
        if features.boots:
            boot_y = 0.030 * h
            # Ankle shaft, then the foot: one last from heel to toe, flat on the ground.
            builder.add_tube(
                [
                    np.array([ankle_j[0] * 0.92, (p.ankle + 0.075) * h, -0.004 * h]),
                    np.array([ankle_j[0] * 0.95, (p.ankle + 0.020) * h, -0.004 * h]),
                ],
                [0.030 * h, 0.032 * h],
                [0.030 * h, 0.032 * h],
                boots,
                [f"shin.{label}", f"foot.{label}"],
                segments=12,
                cap_start=False,
            )
            builder.add_tube(
                [
                    np.array([ankle_j[0] * 0.95, 0.034 * h, -0.026 * h]),
                    np.array([ankle_j[0] * 0.97, 0.030 * h, 0.030 * h]),
                    np.array([ankle_j[0], 0.026 * h, 0.085 * h]),
                    np.array([ankle_j[0], 0.030 * h, 0.112 * h]),
                ],
                [0.031 * h, 0.034 * h, 0.033 * h, 0.026 * h],
                [0.032 * h, 0.034 * h, 0.030 * h, 0.022 * h],
                boots,
                [f"foot.{label}", f"toe.{label}", f"shin.{label}"],
                segments=12,
            )
            # Sole slab: the part that makes a boot read as heavy.
            builder.add_tube(
                [
                    np.array([ankle_j[0] * 0.95, 0.012 * h, -0.026 * h]),
                    np.array([ankle_j[0], 0.010 * h, 0.116 * h]),
                ],
                [0.031 * h, 0.022 * h],
                [0.013 * h, 0.011 * h],
                sole,
                [f"foot.{label}", f"toe.{label}"],
                segments=10,
            )

    mesh = builder.build()
    return Avatar(p, bones, mesh, palette)
