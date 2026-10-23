"""Professor Z, built from the reference turnaround: a stylised young astronomer in a black tee.

Every number in `Body` is measured off the reference sheet — a pixel position divided by the figure's
height — so the build can be read against the drawing rather than guessed at. The character is one
skinned mesh with vertex colours: one primitive, one material, no texture pipeline.

At the size the Home scene draws him (~170 px) a character is silhouette and value: a wide curly mop
over a round head, glasses, a slim black tee with a glowing Z, dark jeans rolled at the ankle, and
chunky black-and-white sneakers. Those get the polygons; the rest only has to be right in the
turnaround.

Conventions are `avatar_core`'s: metres, Y up, +Z forward (the character faces the camera), the
character's left is +X (so it appears on the viewer's right), origin between the feet.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass, field, replace
from pathlib import Path

import numpy as np

from avatar_core import (
    Avatar,
    MeshBuilder,
    Proportions,
    build_skeleton,
    default_palette,
)
from motion import Rig

# ------------------------------------------------------------------------------------------------
# Spec
# ------------------------------------------------------------------------------------------------


@dataclass
class Features:
    """What the character has. Every one of these is geometry, not a texture."""

    hair_curls: int = 190
    hair_volume: float = 1.0
    fringe: int = 5
    glasses: bool = True
    sneakers: bool = True
    emblem: bool = True
    watch: bool = True
    rolled_cuffs: bool = True
    lashes: bool = True


@dataclass
class CharacterSpec:
    name: str
    height_m: float = 1.80
    proportions: dict = field(default_factory=dict)
    palette: dict[str, list[float]] = field(default_factory=dict)
    features: dict = field(default_factory=dict)

    def proportions_obj(self) -> Proportions:
        values = dict(Proportions().__dict__)
        for key, value in self.proportions.items():
            if key in Proportions.__dataclass_fields__:
                values[key] = float(value)
        values["height_m"] = self.height_m
        return Proportions(**values)

    def palette_obj(self) -> dict[str, tuple[float, float, float]]:
        merged = dict(default_palette())
        merged.update(DEFAULT_PALETTE)
        for key, value in self.palette.items():
            if len(value) == 3:
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


# Albedo, sampled from the reference and pulled down slightly: the preview's key light adds about a
# third of a stop, so a colour read straight off a lit pixel comes out too bright.
DEFAULT_PALETTE = {
    "skin": (0.880, 0.640, 0.480),
    "skin_shadow": (0.700, 0.450, 0.310),
    "hair": (0.075, 0.062, 0.052),
    "hair_mid": (0.135, 0.108, 0.088),
    "hair_hi": (0.225, 0.180, 0.145),
    "brow": (0.105, 0.082, 0.065),
    "eye_white": (0.94, 0.92, 0.89),
    "iris": (0.230, 0.135, 0.075),
    "pupil": (0.030, 0.022, 0.020),
    "mouth": (0.300, 0.120, 0.090),
    "lip": (0.620, 0.330, 0.250),
    "glasses": (0.100, 0.095, 0.092),
    "glasses_hi": (0.700, 0.720, 0.740),
    "shirt": (0.075, 0.075, 0.082),
    "shirt_dark": (0.042, 0.042, 0.047),
    "shirt_hi": (0.120, 0.120, 0.128),
    "emblem": (0.180, 0.080, 0.620),
    "emblem_glow": (0.420, 0.230, 1.000),
    "emblem_core": (0.760, 0.540, 1.000),
    "jeans": (0.190, 0.205, 0.225),
    "jeans_hi": (0.245, 0.262, 0.285),
    "jeans_dark": (0.120, 0.130, 0.145),
    "cuff": (0.265, 0.290, 0.320),
    "shoe": (0.062, 0.062, 0.065),
    "shoe_hi": (0.105, 0.105, 0.110),
    "sole": (0.900, 0.888, 0.862),
    "sole_dark": (0.500, 0.492, 0.475),
    "lace": (0.930, 0.920, 0.900),
    "watch": (0.045, 0.045, 0.050),
    "watch_rim": (0.470, 0.470, 0.490),
}


# ------------------------------------------------------------------------------------------------
# Primitives `avatar_core` does not have
# ------------------------------------------------------------------------------------------------


def add_torus(
    builder: MeshBuilder,
    centre: np.ndarray,
    ring_radius: float,
    tube_radius: float,
    colour,
    allowed: list[str],
    facing: str = "z",
    major: int = 20,
    minor: int = 6,
    shade: float = 1.0,
):
    """A ring: glasses rims, collars, watch bezels."""
    base = len(builder.positions)
    axis = np.array([0.0, 0.0, 1.0]) if facing == "z" else np.array([1.0, 0.0, 0.0])
    e1 = np.array([1.0, 0.0, 0.0]) if facing == "z" else np.array([0.0, 0.0, 1.0])
    e2 = np.cross(axis, e1)
    for i in range(major):
        theta = math.tau * i / major
        outward = e1 * math.cos(theta) + e2 * math.sin(theta)
        ring_centre = centre + outward * ring_radius
        for j in range(minor):
            phi = math.tau * j / minor
            offset = outward * (math.cos(phi) * tube_radius) + axis * (math.sin(phi) * tube_radius)
            builder.positions.append(ring_centre + offset)
            side = 0.86 + 0.14 * math.cos(phi)
            builder.colors.append(tuple(min(1.0, c * side * shade) for c in colour))
            builder._allowed.append(allowed)
    for i in range(major):
        i2 = (i + 1) % major
        for j in range(minor):
            j2 = (j + 1) % minor
            builder.add_quad(
                base + i * minor + j,
                base + i2 * minor + j,
                base + i2 * minor + j2,
                base + i * minor + j2,
            )


def add_box(
    builder: MeshBuilder,
    centre: np.ndarray,
    half: tuple[float, float, float],
    colour,
    allowed: list[str],
    yaw: float = 0.0,
    roll: float = 0.0,
    shade: float = 1.0,
):
    """A box, rotated about Z (`roll`) then Y (`yaw`): brows, lace bars, the Z's strokes."""
    hx, hy, hz = half
    base = len(builder.positions)
    for sx, sy, sz in (
        (-1, -1, -1), (1, -1, -1), (1, 1, -1), (-1, 1, -1),
        (-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1),
    ):
        local = np.array([sx * hx, sy * hy, sz * hz])
        cr, sr = math.cos(roll), math.sin(roll)
        local = np.array([local[0] * cr - local[1] * sr, local[0] * sr + local[1] * cr, local[2]])
        cy, sy_ = math.cos(yaw), math.sin(yaw)
        local = np.array([local[0] * cy - local[2] * sy_, local[1], local[0] * sy_ + local[2] * cy])
        builder.positions.append(centre + local)
        brightness = 1.0 if sz > 0 else 0.80
        side = 1.0 if sx > 0 else 0.92
        builder.colors.append(tuple(min(1.0, c * brightness * side * shade) for c in colour))
        builder._allowed.append(allowed)
    # Wound so every face points out of the box.
    for quad in ((0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7)):
        builder.add_quad(*(base + i for i in quad))


def lerp_colour(a, b, t: float):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


# ------------------------------------------------------------------------------------------------
# Measurements
# ------------------------------------------------------------------------------------------------


class Body:
    """Professor Z's measurements, in fractions of his height, off the reference turnaround.

    Vertical landmarks are the drawing's pixel rows, converted: the figure is 814 px from the top of
    the hair to the sole, so his chin is at row 366 → (964 − 366) / 814 = 0.735.
    """

    # Vertical landmarks.
    ground = 0.0
    sole_top = 0.018
    ankle = 0.070
    shoe_top = 0.079
    cuff_top = 0.108
    knee = 0.245
    crotch = 0.386
    shirt_hem = 0.459
    hip = 0.478
    waistband = 0.478
    waist = 0.560
    chest = 0.645
    armpit = 0.650
    shoulder = 0.688
    shoulder_drop = 0.028
    collar = 0.7160
    # The feature stack, measured off the sheet's front view: chin 355 px, mouth 318, nose tip 302,
    # eye 278, brow 255 (of 814 from the sole).
    chin = 0.7580
    mouth = 0.7955
    nose = 0.8150
    eye = 0.8420
    brow = 0.8600
    ear_top = 0.8660
    hairline = 0.8780
    skull_top = 0.9000
    hair_top = 0.998

    # Half-widths.
    head_half = 0.0745
    head_front = 0.0680
    head_back = 0.0730
    neck_half = 0.0400
    shoulder_half = 0.1330
    chest_half = 0.0920
    waist_half = 0.0925
    hip_half = 0.0880
    thigh_half = 0.0545
    knee_half = 0.0425
    shin_half = 0.0335
    leg_centre = 0.0518

    # Depths.
    chest_depth = 0.0700
    waist_depth = 0.0685
    hip_depth = 0.0705

    # Limbs.
    arm_spread = 6.0
    forearm_spread = -19.0
    upper_arm = 0.115
    forearm = 0.105
    hand = 0.040
    foot = 0.145


def build_character(spec: CharacterSpec) -> Avatar:
    p = spec.proportions_obj()
    palette = spec.palette_obj()
    features = spec.features_obj()
    h = p.h
    b = Body()
    bones = build_skeleton(p)
    builder = MeshBuilder(bones, palette)

    def H(fraction: float) -> float:
        return fraction * h

    skin = palette["skin"]
    skin_shadow = palette["skin_shadow"]
    hair = palette["hair"]
    jeans = palette["jeans"]
    jeans_dark = palette["jeans_dark"]
    jeans_hi = palette["jeans_hi"]
    shirt = palette["shirt"]

    # --- legs and jeans ---------------------------------------------------------------------
    for label, sign in (("L", 1.0), ("R", -1.0)):
        hip_x = sign * H(b.leg_centre)
        leg = [
            (np.array([hip_x, H(b.crotch + 0.084), 0.0]), H(b.thigh_half * 0.94), H(b.thigh_half * 0.92)),
            (np.array([hip_x, H(b.crotch + 0.004), H(0.002)]), H(b.thigh_half), H(b.thigh_half * 0.97)),
            (np.array([hip_x * 0.99, H(b.knee + 0.045), H(0.004)]), H(b.knee_half + 0.003), H(b.knee_half + 0.002)),
            (np.array([hip_x * 0.98, H(b.knee), H(0.002)]), H(b.knee_half), H(b.knee_half * 0.97)),
            (np.array([hip_x * 0.97, H(b.cuff_top + 0.035), 0.0]), H(b.shin_half), H(b.shin_half * 0.95)),
        ]
        builder.add_tube(
            [station[0] for station in leg],
            [station[1] for station in leg],
            [station[2] for station in leg],
            jeans,
            [f"thigh.{label}", f"shin.{label}"],
            segments=16,
            cap_start=True,
            cap_end=False,
        )
        # Rolled cuff: the turned-up hem, wider than the leg, in the reference's lighter denim.
        if features.rolled_cuffs:
            builder.add_tube(
                [
                    np.array([hip_x * 0.97, H(b.cuff_top + 0.028), 0.0]),
                    np.array([hip_x * 0.97, H(b.cuff_top + 0.008), 0.0]),
                    np.array([hip_x * 0.97, H(b.cuff_top - 0.014), 0.0]),
                ],
                [H(b.shin_half + 0.001), H(b.shin_half + 0.007), H(b.shin_half + 0.004)],
                [H(b.shin_half + 0.001), H(b.shin_half + 0.006), H(b.shin_half + 0.003)],
                palette["cuff"],
                [f"shin.{label}"],
                segments=16,
                cap_start=False,
                cap_end=True,
            )
        # The ankle: whatever the cuff does not cover, the sneaker's collar does. Without this the leg
        # ends in mid-air and the figure appears to float above its own shoes.
        builder.add_tube(
            [
                np.array([hip_x * 0.97, H(b.cuff_top - 0.006), 0.0]),
                np.array([hip_x * 0.97, H(0.090), 0.0]),
                np.array([hip_x * 0.97, H(0.070), -H(0.002)]),
            ],
            [H(b.shin_half * 0.84), H(b.shin_half * 0.80), H(b.shin_half * 0.78)],
            [H(b.shin_half * 0.82), H(b.shin_half * 0.78), H(b.shin_half * 0.80)],
            palette["shoe"],
            [f"shin.{label}", f"foot.{label}"],
            segments=12,
            cap_start=False,
            cap_end=False,
        )
        if features.sneakers:
            build_sneaker(builder, palette, H, hip_x * 0.97, label, yaw=sign * math.radians(24.0))

    # (No separate seat: the thighs meet above the crotch and the tee's hem is closed, so a seat
    # layer here could only show as a lighter wedge between the legs.)

    # The waistband, a shade darker and a millimetre proud of the seat.
    builder.add_loft(
        [
            (np.array([0.0, H(b.waistband - 0.006), 0.0]), H(b.hip_half * 0.968), H(b.hip_depth * 0.948), H(b.hip_depth * 0.948), 0.80),
            (np.array([0.0, H(b.waistband + 0.008), 0.0]), H(b.hip_half * 0.958), H(b.hip_depth * 0.938), H(b.hip_depth * 0.938), 0.80),
        ],
        jeans,
        ["hips"],
        segments=22,
        cap_start=False,
        cap_end=False,
    )
    # Fly seam, pocket seams and the bulge of two hands pushed into the pockets.
    add_box(builder, np.array([0.0, H(0.4450), H(0.0385)]), (H(0.0016), H(0.0210), H(0.0018)), jeans_dark, ["hips"])
    for sign in (-1.0, 1.0):
        add_box(
            builder,
            np.array([sign * H(0.042), H(b.hip - 0.004), H(b.hip_depth * 0.84)]),
            (H(0.022), H(0.0018), H(0.002)),
            jeans_dark,
            ["hips"],
            roll=sign * -0.34,
        )
        # (No pocket seam here: at Home size a dark tab on the hip reads as an object in the pocket.)

    # --- torso: the tee -----------------------------------------------------------------------
    tee = [
        (b.shirt_hem - 0.002, 0.0955, 0.0715, 0.0720, 0.84),
        (b.shirt_hem + 0.020, 0.0952, 0.0712, 0.0718, 0.84),
        (b.waist, 0.0938, 0.0700, 0.0706, 0.84),
        (b.waist + 0.035, 0.0940, 0.0702, 0.0710, 0.84),
        (b.chest, 0.0946, 0.0706, 0.0712, 0.84),
        (b.chest + 0.014, 0.0952, 0.0692, 0.0702, 0.84),
        (0.658, 0.0950, 0.0690, 0.0702, 0.86),
        (0.672, 0.0962, 0.0684, 0.0698, 0.88),
        (0.683, 0.0968, 0.0672, 0.0688, 0.90),
        (0.692, 0.0924, 0.0640, 0.0658, 0.92),
        (0.7080, 0.0790, 0.0580, 0.0602, 0.94),
        (0.7180, 0.0560, 0.0470, 0.0494, 0.96),
        (0.7260, 0.0480, 0.0424, 0.0450, 0.98),
    ]
    builder.add_loft(
        [(np.array([0.0, H(y), 0.0]), H(rx), H(zf), H(zb), e) for y, rx, zf, zb, e in tee],
        shirt,
        ["hips", "spine", "chest", "neck"],
        segments=26,
        cap_start=True,
        cap_end=False,
    )
    # Ribbed crew collar.
    collar_ring = []
    for i in range(24):
        theta = math.tau * i / 24
        collar_ring.append(
            (np.array([math.sin(theta) * H(0.0410), H(0.7255), math.cos(theta) * H(0.0355)]), 0.0)
        )
    builder.add_tube(
        [point for point, _ in collar_ring] + [collar_ring[0][0]],
        [H(0.0062)] * 25,
        [H(0.0062)] * 25,
        palette["shirt_dark"],
        ["chest", "neck"],
        segments=8,
        cap_start=False,
        cap_end=False,
    )
    # A short neck: the head sits low on the shoulders, the way it does in the reference.
    builder.add_tube(
        [
            np.array([0.0, H(0.706), -H(0.006)]),
            np.array([0.0, H(0.726), -H(0.008)]),
            np.array([0.0, H(b.chin + 0.004), -H(0.010)]),
        ],
        [H(b.neck_half * 0.94), H(b.neck_half * 0.88), H(b.neck_half * 0.84)],
        [H(b.neck_half * 0.80), H(b.neck_half * 0.76), H(b.neck_half * 0.72)],
        skin,
        ["neck", "head", "chest"],
        segments=14,
        cap_start=False,
    )
    if features.emblem:
        build_emblem(builder, palette, H, b)

    # --- arms ---------------------------------------------------------------------------------
    for label, sign in (("L", 1.0), ("R", -1.0)):
        build_arm(builder, palette, features, H, b, label, sign)

    # --- head ---------------------------------------------------------------------------------
    head = HeadGeometry(H, b)
    head.build(builder, palette, features)

    mesh = builder.build()
    bones = scale_to_height(mesh, bones, spec.height_m)
    # The performances aim at this character's own landmarks. A 5.4-head figure keeps his brow a long
    # way from where a 7-head one keeps it, and a hand that hovers over the forehead instead of resting
    # on it reads as a mistake at any size.
    rig = replace(
        Rig.from_bones(bones, p),
        chest_y=b.chest,
        head_y=(b.chin + b.skull_top) / 2.0,
        brow_y=b.brow,
        eye_y=b.eye,
        head_z=b.head_front,
        hip_y=b.hip,
    )
    return Avatar(p, bones, mesh, palette, rig)


# ------------------------------------------------------------------------------------------------
# Sneakers
# ------------------------------------------------------------------------------------------------


def build_sneaker(builder: MeshBuilder, palette, H, ankle_x: float, label: str, yaw: float = 0.0) -> None:
    """A low-top canvas sneaker, built where it stands, then turned out on the ankle.

    The reference's shoes are black canvas over a thick cream midsole, with a rounded white toe cap,
    white laces, and feet splayed about twenty degrees. That splay is not decoration: without it a
    front view only ever shows the toe box, and the laces — the one thing that says "sneaker" at a
    glance — disappear.
    """
    bones = [f"foot.{label}", f"toe.{label}"]
    sole = palette["sole"]
    shoe = palette["shoe"]
    lace = palette["lace"]

    zoff = H(0.010)
    def ring(y, rx, zf, zb, e=0.58):
        return (np.array([ankle_x, H(y), zoff]), H(rx), H(zf), H(zb), e)

    base = len(builder.positions)

    # Midsole: chunky, widest at the ball, and lipped out past the canvas so the white reads from
    # every angle. It is the value cue that keeps a black shoe off a dark ground.
    builder.add_loft(
        [
            ring(0.0000, 0.0290, 0.0690, 0.0545, 0.66),
            ring(0.0055, 0.0345, 0.0830, 0.0635, 0.60),
            ring(0.0155, 0.0370, 0.0865, 0.0660, 0.56),
            ring(0.0270, 0.0366, 0.0855, 0.0650, 0.56),
            ring(0.0335, 0.0344, 0.0822, 0.0622, 0.58),
        ],
        sole,
        bones,
        segments=26,
        cap_start=True,
        cap_end=False,
    )
    # The canvas upper, from the sole line up over the instep to the ankle collar.
    builder.add_loft(
        [
            ring(0.0328, 0.0290, 0.0770, 0.0582, 0.60),
            ring(0.0460, 0.0296, 0.0735, 0.0574, 0.62),
            ring(0.0600, 0.0292, 0.0645, 0.0546, 0.64),
            ring(0.0760, 0.0284, 0.0495, 0.0502, 0.68),
            ring(0.0910, 0.0272, 0.0325, 0.0458, 0.74),
            ring(0.1055, 0.0258, 0.0210, 0.0398, 0.84),
        ],
        shoe,
        bones,
        segments=26,
        cap_start=False,
        cap_end=False,
    )
    # White toe cap: the rubber bumper, wrapped round the front of the canvas. Its rings reach
    # only a little way back, so it hugs the toe instead of swelling off it.
    builder.add_loft(
        [
            ring(0.0000, 0.0345, 0.0850, 0.0360, 0.62),
            ring(0.0150, 0.0365, 0.0868, 0.0360, 0.58),
            ring(0.0305, 0.0362, 0.0862, 0.0330, 0.58),
            ring(0.0420, 0.0348, 0.0842, 0.0268, 0.60),
            ring(0.0515, 0.0324, 0.0776, 0.0205, 0.66),
            ring(0.0585, 0.0286, 0.0686, 0.0158, 0.72),
            ring(0.0630, 0.0232, 0.0596, 0.0124, 0.80),
        ],
        sole,
        bones,
        segments=22,
        cap_start=False,
        cap_end=False,
        shade=0.99,
    )
    # Tongue, just proud of the lacing.
    add_box(builder, np.array([ankle_x, H(0.0790), zoff + H(0.0262)]), (H(0.0140), H(0.0195), H(0.0062)), shoe, bones)
    # Four criss-crossed laces climbing the instep.
    for i in range(4):
        t = i / 3.0
        y = H(0.0530 + t * 0.0385)
        z = zoff + H(0.0520 - t * 0.0300)
        half = H(0.0112 - t * 0.0010)
        for side in (-1.0, 1.0):
            add_box(
                builder,
                np.array([ankle_x, y, z + H(0.0022)]),
                (half, H(0.0018), H(0.0018)),
                lace,
                bones,
                roll=side * 0.46,
            )
    # Heel tab, and the round ankle patch on the outside face.
    add_box(builder, np.array([ankle_x, H(0.0985), zoff - H(0.0395)]), (H(0.0092), H(0.0062), H(0.0026)), lace, bones)
    outer = 1.0 if label == "L" else -1.0
    add_torus(
        builder,
        np.array([ankle_x + outer * H(0.0322), H(0.0575), zoff - H(0.0080)]),
        H(0.0072),
        H(0.0015),
        lace,
        bones,
        facing="x",
        major=10,
        minor=4,
    )

    if yaw and len(builder.positions) > base:
        c, sn = math.cos(yaw), math.sin(yaw)
        pivot = np.array([ankle_x, 0.0, zoff])
        for i in range(base, len(builder.positions)):
            local = builder.positions[i] - pivot
            builder.positions[i] = np.array(
                [local[0] * c + local[2] * sn, local[1], -local[0] * sn + local[2] * c]
            ) + pivot


# ------------------------------------------------------------------------------------------------
# The Z
# ------------------------------------------------------------------------------------------------


def build_emblem(builder: MeshBuilder, palette, H, b: Body) -> None:
    """The neon Z on the chest.

    A Z is two bars and a diagonal, and the diagonal must run from the top-right corner to the
    bottom-left one: three nested copies of that shape — a dark bed, a violet glow, a bright core —
    give the sign a lit edge without a texture.
    """
    cy = H(0.5640)
    front = H(b.chest_depth)
    half_w = H(0.0480)
    half_h = H(0.0545)

    def z(colour, inset: float, depth: float, thickness: float, lift: float):
        w = half_w - inset
        hh = half_h - inset
        t = thickness
        y0 = cy + hh - t * 0.5
        y1 = cy - hh + t * 0.5
        for y in (y0, y1):
            add_box(
                builder,
                np.array([0.0, y, front + lift]),
                (w, t * 0.5, depth),
                colour,
                ["chest"],
                shade=1.10,
            )
        # The diagonal joins the inner ends of the two bars.
        ax, ay = w - t * 0.5, y0 - t * 0.5
        bx, by = -w + t * 0.5, y1 + t * 0.5
        span = math.hypot(ax - bx, ay - by)
        add_box(
            builder,
            np.array([(ax + bx) * 0.5, (ay + by) * 0.5, front + lift]),
            (span * 0.5, t * 0.5, depth),
            colour,
            ["chest"],
            roll=math.atan2(by - ay, bx - ax),
            shade=1.10,
        )

    # A dark bed, slightly proud of the tee, so the glow has an edge to sit against.
    z(palette["emblem_bed"], -H(0.0042), H(0.0010), H(0.0150), H(0.0004))
    z(palette["emblem"], H(0.0016), H(0.0016), H(0.0118), H(0.0018))
    z(palette["emblem_glow"], H(0.0056), H(0.0018), H(0.0078), H(0.0028))
    z(palette["emblem_core"], H(0.0110), H(0.0020), H(0.0036), H(0.0036))


# ------------------------------------------------------------------------------------------------
# Arms
# ------------------------------------------------------------------------------------------------


def build_arm(builder: MeshBuilder, palette, features, H, b: Body, label: str, sign: float) -> None:
    skin = palette["skin"]
    socket_x = sign * H(b.shoulder_half * 0.80)
    # The joint is below the top of the shoulder, the way a real one is: the tee's shoulder line is
    # above it, so a tube hung from the joint reads as a sleeve rather than a pauldron.
    joint_y = H(b.shoulder - b.shoulder_drop)
    spread = math.radians(b.arm_spread)
    elbow = np.array([
        socket_x + sign * math.sin(spread) * H(b.upper_arm),
        joint_y - math.cos(spread) * H(b.upper_arm),
        H(0.004),
    ])
    fore = math.radians(b.forearm_spread)
    wrist = np.array([
        elbow[0] + sign * math.sin(fore) * H(b.forearm),
        elbow[1] - math.cos(fore) * H(b.forearm),
        H(0.008),
    ])
    upper_bones = [f"shoulder.{label}", f"upper_arm.{label}", f"forearm.{label}"]

    # One cap sleeve: its top edge is the tee's shoulder line, sloping down from the collar to the
    # shoulder point, and its first ring is buried inside the chest so no opening is ever visible.
    sleeve_t = 0.62
    sleeve = np.array([
        socket_x + sign * math.sin(spread) * H(b.upper_arm) * sleeve_t,
        joint_y - math.cos(spread) * H(b.upper_arm) * sleeve_t,
        H(0.003),
    ])
    builder.add_tube(
        [
            np.array([sign * H(0.0300), H(b.shoulder + 0.006), 0.0]),
            np.array([sign * H(0.0620), H(b.shoulder - 0.002), H(0.001)]),
            np.array([sign * H(0.0900), H(b.shoulder - 0.014), H(0.002)]),
            np.array([socket_x, H(b.shoulder - 0.024), H(0.002)]),
            sleeve,
        ],
        [H(0.0140), H(0.0220), H(0.0280), H(0.0300), H(0.0280)],
        [H(0.0300), H(0.0330), H(0.0330), H(0.0320), H(0.0280)],
        palette["shirt"],
        [f"chest", f"shoulder.{label}", f"upper_arm.{label}"],
        segments=18,
        cap_start=False,
        cap_end=False,
    )
    # Sleeve hem: a band along the arm's own axis, slightly proud of the sleeve.
    along = (elbow - sleeve) / max(np.linalg.norm(elbow - sleeve), 1e-6)
    builder.add_tube(
        [sleeve - along * H(0.004), sleeve + along * H(0.006)],
        [H(0.0316), H(0.0298)],
        [H(0.0310), H(0.0292)],
        palette["shirt_hi"],
        upper_bones,
        segments=16,
        cap_start=False,
        cap_end=False,
    )
    # Bare arm.
    builder.add_tube(
        [
            sleeve,
            elbow,
            wrist - (wrist - elbow) * 0.10,
        ],
        [H(0.0272), H(0.0244), H(0.0212)],
        [H(0.0266), H(0.0240), H(0.0208)],
        skin,
        [f"upper_arm.{label}", f"forearm.{label}"],
        segments=16,
        cap_start=False,
        cap_end=False,
    )
    if features.watch and label == "L":
        watch_y = wrist[1] - H(0.006)
        builder.add_tube(
            [
                np.array([wrist[0], watch_y + H(0.008), wrist[2]]),
                np.array([wrist[0], watch_y - H(0.008), wrist[2]]),
            ],
            [H(0.0195), H(0.0195)],
            [H(0.0190), H(0.0190)],
            palette["watch"],
            [f"forearm.{label}", f"hand.{label}"],
            segments=16,
            cap_start=False,
            cap_end=False,
        )
        add_torus(
            builder,
            np.array([wrist[0], watch_y, wrist[2] + H(0.0035)]),
            H(0.0120),
            H(0.0022),
            palette["watch_rim"],
            [f"forearm.{label}"],
            facing="z",
            major=14,
            minor=5,
        )
        builder.add_ellipsoid(
            np.array([wrist[0], watch_y, wrist[2] + H(0.0030)]),
            (H(0.0102), H(0.0102), H(0.0030)),
            (0.09, 0.10, 0.12),
            [f"forearm.{label}"],
            rings=5,
            segments=10,
        )
    # Hand: palm, four fingers and a thumb. Eight pixels at Home size, but it is against the sky in
    # the night and wave clips, where a mitten reads as a paddle.
    direction = (wrist - elbow) / max(np.linalg.norm(wrist - elbow), 1e-6)
    palm = wrist + direction * H(0.016)
    hand_bones = [f"hand.{label}", f"forearm.{label}"]
    builder.add_ellipsoid(palm, (H(0.0125), H(0.0165), H(0.0098)), skin, hand_bones, rings=6, segments=12)
    for i in range(4):
        offset = (i - 1.5) * H(0.0055)
        length = H(0.0105) * (1.0 - abs(i - 1.3) * 0.10)
        base = palm + direction * H(0.0110) + np.array([sign * offset, 0.0, 0.0])
        tip = base + direction * length + np.array([sign * offset * 0.20, H(0.0004), H(0.0020)])
        builder.add_tube([base, tip], [H(0.0042), H(0.0038)], [H(0.0038), H(0.0034)], skin, hand_bones, segments=7)
    thumb_base = palm - direction * H(0.002) + np.array([-sign * H(0.0125), H(0.0035), H(0.0035)])
    thumb_tip = thumb_base + np.array([-sign * H(0.0065), -H(0.0105), H(0.0055)])
    builder.add_tube([thumb_base, thumb_tip], [H(0.0054), H(0.0038)], [H(0.0050), H(0.0034)], skin, hand_bones, segments=7)


# ------------------------------------------------------------------------------------------------
# Head
# ------------------------------------------------------------------------------------------------


class HeadGeometry:
    """The head as a queryable surface, so every feature sits *on* the face instead of near it."""

    def __init__(self, H, b: Body):
        self.H = H
        self.b = b
        self.chin = b.chin
        self.top = b.skull_top
        self.height = b.skull_top - b.chin
        self.half = b.head_half
        self.front = b.head_front
        self.back = b.head_back
        # (t up the head, half-width, front depth, back depth, squareness)
        self.profile = [
            (0.00, 0.46, 0.44, 0.54, 0.99),
            (0.06, 0.62, 0.58, 0.66, 0.90),
            (0.14, 0.75, 0.70, 0.78, 0.81),
            (0.26, 0.87, 0.81, 0.89, 0.80),
            (0.38, 0.94, 0.90, 0.95, 0.87),
            (0.50, 1.00, 0.97, 1.00, 0.86),
            (0.60, 1.00, 1.00, 1.02, 0.86),
            (0.72, 0.97, 0.98, 1.00, 0.88),
            (0.84, 0.90, 0.90, 0.94, 0.90),
            (0.93, 0.76, 0.74, 0.82, 0.93),
            (0.98, 0.50, 0.48, 0.56, 0.97),
            (1.00, 0.18, 0.20, 0.24, 1.00),
        ]

    def y(self, fraction: float) -> float:
        """World height of a fraction of the head: 0 is the chin, 1 the top of the skull."""
        return self.H(self.chin + fraction * self.height)

    def ring(self, fraction: float):
        ts = np.array([p[0] for p in self.profile])
        i = int(np.clip(np.searchsorted(ts, fraction) - 1, 0, len(self.profile) - 2))
        t0, t1 = ts[i], ts[i + 1]
        mix = 0.0 if t1 == t0 else (fraction - t0) / (t1 - t0)
        a, c = self.profile[i], self.profile[i + 1]
        return tuple(a[k] + (c[k] - a[k]) * mix for k in range(1, 5))

    def surface(self, world_y: float, x: float) -> float:
        """Front z of the face at a world height and an x offset."""
        fraction = (world_y / self.H(1.0) - self.chin) / self.height
        rx, zf, _, exponent = self.ring(max(0.0, min(1.0, fraction)))
        nx = min(1.0, abs(x) / max(self.H(self.half * rx), 1e-6))
        power = 2.0 / exponent
        return self.H(self.front * zf) * (max(0.0, 1.0 - nx ** power) ** (1.0 / power))

    def build(self, builder: MeshBuilder, palette, features) -> None:
        H = self.H
        skin = palette["skin"]
        head_bones = ["head"]
        builder.add_loft(
            [
                (
                    np.array([0.0, self.y(t), self.H(0.001)]),
                    H(self.half * rx),
                    H(self.front * zf),
                    H(self.back * zb),
                    e,
                )
                for t, rx, zf, zb, e in self.profile
            ],
            skin,
            head_bones,
            segments=28,
        )
        self.build_eyes(builder, palette)
        self.build_brows(builder, palette)
        self.build_nose(builder, palette)
        self.build_mouth(builder, palette)
        self.build_ears(builder, palette)
        if features.glasses:
            self.build_glasses(builder, palette)
        build_hair(builder, palette, features, H, self)

    # -- eyes -------------------------------------------------------------------------------
    @property
    def eye_y(self) -> float:
        return self.H(self.b.eye)

    @property
    def eye_x(self) -> float:
        return self.H(0.0335)

    def build_eyes(self, builder, palette) -> None:
        """A big, calm eye: a white almond, a wide brown iris, a dark pupil and one glint.

        Measured off the sheet: each lens is nearly half the head's width, and the eye inside it
        almost fills the glass. That scale is what makes the face read as a character rather than a
        mannequin, so the eye is built large and set shallow into the socket.
        """
        H = self.H
        eye_y = self.eye_y
        for sign in (-1.0, 1.0):
            x = sign * self.eye_x
            front = self.surface(eye_y, x)
            centre = np.array([x, eye_y, front - H(0.0022)])
            # The white of the eye, a shallow lens set into the socket.
            builder.add_ellipsoid(
                centre,
                (H(0.0200), H(0.0132), H(0.0052)),
                palette["eye_white"],
                ["head"],
                rings=8,
                segments=18,
            )
            iris = centre + np.array([-sign * H(0.0006), H(0.0004), H(0.0034)])
            builder.add_ellipsoid(
                iris,
                (H(0.0122), H(0.0122), H(0.0022)),
                palette["iris"],
                ["head"],
                rings=8,
                segments=18,
            )
            builder.add_ellipsoid(
                iris + np.array([0.0, 0.0, H(0.0012)]),
                (H(0.0058), H(0.0058), H(0.0014)),
                palette["pupil"],
                ["head"],
                rings=6,
                segments=12,
            )
            builder.add_ellipsoid(
                iris + np.array([-sign * H(0.0034), H(0.0038), H(0.0016)]),
                (H(0.0024), H(0.0025), H(0.0010)),
                (1.0, 0.99, 0.97),
                ["head"],
                rings=4,
                segments=8,
            )
            # The upper lid: a thin skin crease over the top of the eye, and the lash line that gives
            # the eye its shape. No mass: at this size a lid big enough to cover the eye swells.
            arc = []
            for i in range(7):
                t = (i - 3) / 3.0
                ax = x + sign * t * H(0.0205)
                ay = eye_y + H(0.0112) * (1.0 - 0.22 * t * t)
                arc.append(np.array([ax, ay, self.surface(ay, ax) - H(0.0012)]))
            builder.add_tube(
                arc,
                [H(0.0012), H(0.0016), H(0.0019), H(0.0020), H(0.0019), H(0.0016), H(0.0012)],
                [H(0.0011), H(0.0014), H(0.0016), H(0.0017), H(0.0016), H(0.0014), H(0.0011)],
                palette["brow"],
                ["head"],
                segments=6,
                cap_start=False,
                cap_end=False,
            )
            # A soft crease under the eye: one pale line, which is all a stylised face gets.
            add_box(
                builder,
                centre + np.array([0.0, -H(0.0148), H(0.0018)]),
                (H(0.0150), H(0.0008), H(0.0010)),
                palette["skin_shadow"],
                ["head"],
                shade=1.05,
            )

    # -- brows ------------------------------------------------------------------------------
    def build_brows(self, builder, palette) -> None:
        """Thick, arched brows. In the reference they are the strongest line on the face."""
        H = self.H
        brow_y = self.H(self.b.brow)
        for sign in (-1.0, 1.0):
            # Three stations along the brow: heavy at the inner end, arched, then tapering to the
            # temple. One tapered tube reads as hair; three boxes read as stickers.
            stations = []
            for t in (0.0, 0.28, 0.55, 0.80, 1.0):
                bx = sign * H(0.0075 + t * 0.0385)
                by = brow_y + H(0.0016) * math.sin(t * math.pi) - H(0.0032) * t
                stations.append(np.array([bx, by, self.surface(by, bx) - H(0.0012)]))
            builder.add_tube(
                stations,
                [H(0.0064), H(0.0068), H(0.0058), H(0.0046), H(0.0028)],
                [H(0.0044), H(0.0048), H(0.0040), H(0.0032), H(0.0020)],
                palette["brow"],
                ["head"],
                segments=8,
                cap_start=False,
                cap_end=False,
            )

    # -- nose -------------------------------------------------------------------------------
    def build_nose(self, builder, palette) -> None:
        """A small, rounded nose: a bridge that rises off the face, a tip, two soft wings."""
        H = self.H
        nose_y = self.H(self.b.nose)
        z = self.surface(nose_y, 0.0)
        builder.add_tube(
            [
                np.array([0.0, self.H(self.b.eye) - H(0.004), z - H(0.0030)]),
                np.array([0.0, nose_y + H(0.010), z - H(0.0055)]),
                np.array([0.0, nose_y + H(0.001), z - H(0.0075)]),
            ],
            [H(0.0042), H(0.0056), H(0.0072)],
            [H(0.0034), H(0.0050), H(0.0064)],
            palette["skin"],
            ["head"],
            segments=12,
            cap_start=False,
            cap_end=False,
            shade=1.02,
        )
        # The tip: one soft mass, wider than it is tall, sitting just proud of the face.
        builder.add_ellipsoid(
            np.array([0.0, nose_y + H(0.0010), z - H(0.0026)]),
            (H(0.0092), H(0.0098), H(0.0080)),
            palette["skin"],
            ["head"],
            rings=6,
            segments=14,
            shade=1.02,
        )
        # Two nostril shadows, not two balls.
        for sign in (-1.0, 1.0):
            add_box(
                builder,
                np.array([sign * H(0.0046), nose_y - H(0.0068), z - H(0.0008)]),
                (H(0.0018), H(0.0011), H(0.0014)),
                palette["skin_shadow"],
                ["head"],
                roll=sign * 0.25,
                shade=0.92,
            )

    # -- mouth ------------------------------------------------------------------------------
    def build_mouth(self, builder, palette) -> None:
        """A closed, slightly upturned smile: a dark line, a lit lower lip, a shadow beneath.

        No lipstick: the reference's mouth is a line with a soft lower lip, and any red at all turns
        into a blob at Home size.
        """
        H = self.H
        mouth_y = self.H(self.b.mouth)
        # A closed smile: the line rises at the corners, and the middle of the lower lip is the
        # lowest point of the mouth. Two arcs, not one straight dash.
        points = []
        for i in range(11):
            t = (i - 5) / 5.0
            x = t * H(0.0215)
            y = mouth_y - H(0.0036) * (1.0 - t * t) + H(0.0062) * (t * t * t)
            points.append(np.array([x, y, self.surface(y, x) - H(0.0022)]))
        builder.add_tube(
            points,
            [H(0.0019)] * 11,
            [H(0.0015)] * 11,
            palette["mouth"],
            ["head"],
            segments=6,
            cap_start=True,
            cap_end=True,
            shade=0.92,
        )
        # The lower lip: a soft, lit roll under the line, widest at the middle.
        builder.add_ellipsoid(
            np.array([0.0, mouth_y - H(0.0090), self.surface(mouth_y, 0.0) - H(0.0046)]),
            (H(0.0122), H(0.0036), H(0.0042)),
            lerp_colour(palette["skin"], palette["lip"], 0.16),
            ["head"],
            rings=5,
            segments=14,
            shade=1.0,
        )
        # Shadow under the lower lip.
        add_box(
            builder,
            np.array([0.0, mouth_y - H(0.0098), self.surface(mouth_y, 0.0) - H(0.0044)]),
            (H(0.0088), H(0.0012), H(0.0018)),
            palette["skin_shadow"],
            ["head"],
            shade=0.98,
        )

    # -- ears -------------------------------------------------------------------------------
    def build_ears(self, builder, palette) -> None:
        """An ear is a rim with a hollow behind it: a shell, a ring, and a shadow inside the ring."""
        H = self.H
        ear_y = self.H(self.b.ear_top) - H(0.0175)
        for sign in (-1.0, 1.0):
            x = sign * H(self.half * 0.955)
            centre = np.array([x, ear_y, -H(0.0060)])
            builder.add_ellipsoid(
                centre,
                (H(0.0058), H(0.0165), H(0.0105)),
                palette["skin"],
                ["head"],
                rings=7,
                segments=12,
                shade=1.01,
            )
            builder.add_ellipsoid(
                centre + np.array([sign * H(0.0022), -H(0.0006), -H(0.0012)]),
                (H(0.0016), H(0.0090), H(0.0052)),
                lerp_colour(palette["skin"], palette["skin_shadow"], 0.38),
                ["head"],
                rings=5,
                segments=10,
                shade=1.04,
            )

    # -- glasses ----------------------------------------------------------------------------
    def build_glasses(self, builder, palette) -> None:
        H = self.H
        eye_y = self.eye_y + H(0.0015)
        rim = H(0.0272)
        frame = palette["glasses"]
        for sign in (-1.0, 1.0):
            x = sign * self.eye_x
            z = self.surface(eye_y, x) + H(0.0050)
            centre = np.array([x, eye_y, z])
            add_torus(builder, centre, rim, H(0.0011), frame, ["head"], major=28, minor=6)
            # A lens highlight: a short, dim dash inside the top of each lens.
            add_box(
                builder,
                centre + np.array([-sign * rim * 0.30, rim * 0.40, -H(0.0002)]),
                (rim * 0.24, H(0.0008), H(0.0006)),
                palette["glasses_hi"],
                ["head"],
                roll=-sign * 0.5,
            )
            # Temple arm back to the ear.
            # The temple: off the rim, back along the skull to the ear. It follows the head's own
            # width, because a straight bar to the widest point sticks out like a wing.
            temple = []
            for t in (0.0, 0.35, 1.0):
                tx = sign * (self.eye_x + rim * 0.90 + t * (self.H(self.half * 0.96) - self.eye_x - rim * 0.90))
                ty = eye_y + H(0.0010) + t * H(0.0016)
                if t < 1.0:
                    tz = self.surface(ty, abs(tx)) - H(0.0012)
                else:
                    tz = -H(0.028)
                temple.append(np.array([tx, ty, tz]))
            builder.add_tube(
                temple,
                [H(0.0011), H(0.0011), H(0.0011)],
                [H(0.0011), H(0.0011), H(0.0011)],
                frame,
                ["head"],
                segments=6,
                cap_start=False,
            )
        # Bridge over the nose: a shallow arc, thin as the rims.
        z = self.surface(eye_y, 0.0) + H(0.0040)
        builder.add_tube(
            [
                np.array([-self.eye_x + rim * 0.90, eye_y + H(0.0040), z - H(0.0020)]),
                np.array([0.0, eye_y + H(0.0072), z]),
                np.array([self.eye_x - rim * 0.90, eye_y + H(0.0040), z - H(0.0020)]),
            ],
            [H(0.0016)] * 3,
            [H(0.0014)] * 3,
            frame,
            ["head"],
            segments=6,
            cap_start=False,
            cap_end=False,
        )


# ------------------------------------------------------------------------------------------------
# Hair
# ------------------------------------------------------------------------------------------------


def build_hair(builder: MeshBuilder, palette, features, H, head: HeadGeometry) -> None:
    """The mop: a thin scalp, and the volume built out of curls that overlap.

    Curly hair is a mass of locks, not a smooth cap. A smooth cap on top of a round head reads as a
    helmet at any size — which is exactly what a shell with a few bumps on it looks like. So the shell
    here is only a scalp (barely wider than the skull, and it stops at the hairline), and every bit of
    the silhouette is made by ~80 overlapping curls, each a tapered lock that leaves the scalp, curls
    out and up, and ends in a rounded tip.

    Placement is a Fibonacci spiral on the skull's own ellipsoid with a fixed seed, so a rebuild is
    byte-identical and the mass can be tuned by two numbers.
    """
    hair = palette["hair"]
    mid = palette["hair_mid"]
    hi = palette["hair_hi"]
    volume = features.hair_volume

    # The scalp: a thin cap, from just above the ears up over the crown. Its front edge is the
    # hairline, so it must stay shallow at the brow and reach further at the temples.
    shell = [
        (0.8300, 0.0560, 0.0100, 0.0620, 0.95),
        (0.8420, 0.0592, 0.0190, 0.0648, 0.95),
        (0.856, 0.0608, 0.0280, 0.0640, 0.95),
        (0.872, 0.0636, 0.0380, 0.0672, 0.95),
        (0.888, 0.0650, 0.0470, 0.0690, 0.95),
        (0.902, 0.0648, 0.0515, 0.0685, 0.95),
        (0.916, 0.0632, 0.0530, 0.0670, 0.95),
        (0.930, 0.0600, 0.0520, 0.0640, 0.95),
        (0.944, 0.0540, 0.0490, 0.0580, 0.95),
        (0.956, 0.0440, 0.0420, 0.0480, 0.95),
        (0.964, 0.0280, 0.0280, 0.0320, 0.95),
    ]
    builder.add_loft(
        [(np.array([0.0, H(y), -H(0.004)]), H(rx * volume), H(zf * volume), H(zb * volume), e) for y, rx, zf, zb, e in shell],
        hair,
        ["head"],
        segments=26,
        cap_start=False,
        cap_end=True,
    )

    rng = np.random.default_rng(20240611)
    centre = np.array([0.0, H(0.893), -H(0.004)])
    radius = np.array([H(0.0778), H(0.0672), H(0.0755)]) * volume

    count = max(12, int(features.hair_curls))
    for index in range(count):
        # A Fibonacci sphere, held to the upper two thirds, plus a band at the nape.
        u = (index + 0.5) / count
        theta = math.acos(1.0 - 1.72 * u)          # 0 at the crown, ~2.6 rad at the nape
        phi = math.pi * (1.0 + 5.0 ** 0.5) * index
        ny, ring = math.cos(theta), math.sin(theta)
        nx, nz = ring * math.cos(phi), ring * math.sin(phi)
        # Taper toward the crown, so the mass peaks instead of swelling into a dome.
        crown = 1.0 - 0.42 * max(0.0, ny - 0.45) ** 1.3
        point = centre + np.array([nx * radius[0] * crown, ny * radius[1], nz * radius[2] * crown])
        height = point[1] / H(1.0)
        # The nape and the sides carry hair down to the ear line; the face does not.
        floor = 0.846 if nz < -0.30 else (0.860 if abs(nz) < 0.30 else 0.872)
        if height < floor:
            continue
        # Nothing in front of the brow, and nothing hanging over the glasses.
        if nz > 0.15 and height < 0.888:
            continue
        if nz > 0.55 and height < 0.900:
            continue

        normal = np.array([nx, ny * 1.0, nz])
        normal /= np.linalg.norm(normal) or 1.0
        # Curls leave the scalp outwards, lift, and twist round the skull.
        twist = rng.normal() * 0.5
        tangent = np.array([-normal[2], 0.0, normal[0]]) * (1.0 if index % 2 else -1.0)
        direction = normal * 1.0 + np.array([0.0, 0.55 + 0.5 * rng.random(), 0.0]) + tangent * (0.25 + twist * 0.4)
        direction /= np.linalg.norm(direction)
        length = H(0.022 + 0.016 * rng.random()) * volume
        root_r = H(0.0100 + 0.0048 * rng.random()) * volume

        path = [
            point - normal * H(0.004),
            point + direction * length * 0.32,
            point + direction * length * 0.66 + tangent * length * 0.24,
            point + direction * length * 0.90 + tangent * length * 0.52,
            point + direction * length * 1.02 + tangent * length * 0.78,
        ]
        widths = [root_r * 0.78, root_r, root_r * 0.90, root_r * 0.62, root_r * 0.10]
        depths = [root_r * 0.68, root_r * 0.86, root_r * 0.78, root_r * 0.54, root_r * 0.09]
        colour = hair if rng.random() < 0.38 else (mid if rng.random() < 0.74 else hi)
        builder.add_tube(
            path,
            widths,
            depths,
            colour,
            ["head"],
            segments=7,
            cap_start=False,
            cap_end=True,
            shade=0.95 + 0.13 * rng.random(),
        )

    # Fringe: six locks off the crown sweeping forward and down over the forehead, tips curling up
    # off the brow — the reference's hair never quite touches the glasses.
    fringe = max(0, int(features.fringe)) + 1
    for i in range(fringe):
        t = i / max(fringe - 1, 1)
        x = (t - 0.5) * H(0.108)
        root = np.array([x * 0.7, H(0.914 - 0.012 * (t - 0.5) ** 2), H(0.038)])
        tip = np.array([
            x * 1.06,
            H(0.8940 - 0.004 * math.sin(t * math.pi)),
            H(0.062 + 0.004 * math.sin(t * math.pi)),
        ])
        mid_point = (root + tip) * 0.5 + np.array([0.0, H(0.008), H(0.008)])
        r = H(0.0125)
        builder.add_tube(
            [root, mid_point, tip + np.array([0.0, H(0.006), -H(0.004)]), tip],
            [r * 0.9, r, r * 0.66, r * 0.16],
            [r * 0.8, r * 0.9, r * 0.58, r * 0.14],
            hair if i % 2 else mid,
            ["head"],
            segments=8,
            cap_start=False,
            cap_end=True,
        )

    # Sideburns in front of the ears: the only hair below the scalp. A slim tapered lock that lies
    # on the temple, tucked behind the glasses' temple arm.
    for sign in (-1.0, 1.0):
        base = np.array([sign * H(head.half * 0.90), H(0.8760), H(0.0080)])
        tip = np.array([sign * H(head.half * 0.855), H(0.8500), H(0.0180)])
        builder.add_tube(
            [base, (base + tip) * 0.5 + np.array([0.0, 0.0, H(0.0020)]), tip],
            [H(0.0038), H(0.0034), H(0.0016)],
            [H(0.0022), H(0.0020), H(0.0010)],
            mid,
            ["head"],
            segments=6,
            cap_start=False,
            cap_end=True,
            shade=0.98,
        )


def head_ring_at(shell, height: float):
    """Interpolate the hair shell's (half-width, front, back) at a world-height fraction."""
    if height <= shell[0][0]:
        return shell[0][1], shell[0][2], shell[0][3], shell[0][4]
    if height >= shell[-1][0]:
        return shell[-1][1], shell[-1][2], shell[-1][3], shell[-1][4]
    for (y0, a0, b0, c0, e0), (y1, a1, b1, c1, e1) in zip(shell, shell[1:]):
        if y0 <= height <= y1:
            mix = (height - y0) / (y1 - y0)
            return a0 + (a1 - a0) * mix, b0 + (b1 - b0) * mix, c0 + (c1 - c0) * mix, e0
    return shell[-1][1], shell[-1][2], shell[-1][3], shell[-1][4]


# ------------------------------------------------------------------------------------------------
# Height
# ------------------------------------------------------------------------------------------------


def scale_to_height(mesh, bones, target_height: float):
    """Scale a mesh and its skeleton so the figure measures exactly `target_height` metres.

    Feet go to y = 0 first, so the scale is also what puts the character on the ground. Bones are
    frozen dataclasses, so they are rebuilt rather than mutated.
    """
    ys = mesh.positions[:, 1]
    span = float(ys.max() - ys.min())
    if span <= 1e-6:
        return bones
    factor = target_height / span
    origin = float(ys.min())
    if abs(factor - 1.0) < 1e-6 and abs(origin) < 1e-6:
        return bones

    def fit(point):
        return (np.asarray(point, dtype=np.float64) - np.array([0.0, origin, 0.0])) * factor

    mesh.positions = fit(mesh.positions)
    return [replace(bone, head=fit(bone.head), tail=fit(bone.tail)) for bone in bones]
