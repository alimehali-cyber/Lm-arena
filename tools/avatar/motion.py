"""The clips the Home scene plays. Authored, not generated: five performances, 24 fps, seamless.

Arms are posed by **aiming** them at world directions (a tiny two-bone solver), not by guessing Euler
triples: an angle that raises the arm at rest does not raise it once the chest has turned. The spine,
neck and head keep Euler triples, which read naturally for a torso.

Every clip is a pure function of time, so it is unit-testable and the loops can be proved seamless
(pose(0) == pose(length)). Angles are degrees.
"""

from __future__ import annotations

import math
from dataclasses import dataclass

import numpy as np

FPS = 24

REST = {
    ".L": (math.sin(math.radians(22.0)), -math.cos(math.radians(22.0)), 0.0),
    ".R": (-math.sin(math.radians(22.0)), -math.cos(math.radians(22.0)), 0.0),
}
FORE = {
    ".L": (math.sin(math.radians(10.0)), -math.cos(math.radians(10.0)), 0.0),
    ".R": (-math.sin(math.radians(10.0)), -math.cos(math.radians(10.0)), 0.0),
}

IDENTITY = np.array([0.0, 0.0, 0.0, 1.0])


# ------------------------------------------------------------------------------------------------
# Quaternions
# ------------------------------------------------------------------------------------------------


def q_axis(axis: str, degrees: float) -> np.ndarray:
    half = math.radians(degrees) * 0.5
    s = math.sin(half)
    if axis == "x":
        return np.array([s, 0.0, 0.0, math.cos(half)])
    if axis == "y":
        return np.array([0.0, s, 0.0, math.cos(half)])
    return np.array([0.0, 0.0, s, math.cos(half)])


def q_mul(a: np.ndarray, b: np.ndarray) -> np.ndarray:
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return np.array([
        aw * bx + ax * bw + ay * bz - az * by,
        aw * by - ax * bz + ay * bw + az * bx,
        aw * bz + ax * by - ay * bx + az * bw,
        aw * bw - ax * bx - ay * by - az * bz,
    ])


def q_conj(q: np.ndarray) -> np.ndarray:
    return np.array([-q[0], -q[1], -q[2], q[3]])


def q_rotate(q: np.ndarray, v) -> np.ndarray:
    """Rotate a 3-vector by a unit quaternion."""
    x, y, z, w = q
    v = np.asarray(v, dtype=np.float64)
    t = 2.0 * np.cross(np.array([x, y, z]), v)
    return v + w * t + np.cross(np.array([x, y, z]), t)


def q_from_to(a, b) -> np.ndarray:
    """Shortest-arc rotation taking direction a onto direction b."""
    a = np.asarray(a, dtype=np.float64)
    b = np.asarray(b, dtype=np.float64)
    a = a / (np.linalg.norm(a) or 1.0)
    b = b / (np.linalg.norm(b) or 1.0)
    dot = float(np.clip(a @ b, -1.0, 1.0))
    if dot > 0.999999:
        return IDENTITY.copy()
    if dot < -0.999999:
        axis = np.cross(a, np.array([0.0, 0.0, 1.0]))
        if np.linalg.norm(axis) < 1e-6:
            axis = np.cross(a, np.array([0.0, 1.0, 0.0]))
        axis /= np.linalg.norm(axis)
        return np.array([axis[0], axis[1], axis[2], 0.0])
    axis = np.cross(a, b)
    axis /= np.linalg.norm(axis)
    angle = math.acos(dot) * 0.5
    return np.array([axis[0] * math.sin(angle), axis[1] * math.sin(angle), axis[2] * math.sin(angle), math.cos(angle)])


def q_euler(rx: float, ry: float, rz: float) -> np.ndarray:
    q = q_mul(q_mul(q_axis("z", rz), q_axis("x", rx)), q_axis("y", ry))
    n = np.linalg.norm(q)
    return q / (n or 1.0)


def q_slerp(a: np.ndarray, b: np.ndarray, t: float) -> np.ndarray:
    dot = float(a @ b)
    if dot < 0.0:
        b = -b
        dot = -dot
    if dot > 0.9995:
        out = a + (b - a) * t
        return out / (np.linalg.norm(out) or 1.0)
    theta = math.acos(max(-1.0, min(1.0, dot)))
    sin_theta = math.sin(theta)
    return (a * (math.sin((1.0 - t) * theta) / sin_theta)) + (b * (math.sin(t * theta) / sin_theta))


def unit(v) -> np.ndarray:
    v = np.asarray(v, dtype=np.float64)
    return v / (np.linalg.norm(v) or 1.0)


@dataclass(frozen=True)
class Rig:
    """The handful of landmarks the performances aim at, in units of body height.

    A clip that hard-codes "the hand goes to y = 0.952" only works for the character it was tuned on.
    Every target below is expressed off this rig instead, so the same five performances fit a 1.55 m
    child, this 1.80 m astronomer, or anything else the pipeline builds.
    """

    shoulder_y: float = 0.794
    shoulder_x: float = 0.0929
    upper: float = 0.155
    fore: float = 0.145
    hand: float = 0.052
    chest_y: float = 0.720
    head_y: float = 0.925
    brow_y: float = 0.945
    eye_y: float = 0.925
    head_z: float = 0.037   # half-depth of the head, for reaching in front of the face
    hip_y: float = 0.530
    arm_spread: float = 22.0
    forearm_spread: float = 10.0

    def rest_direction(self, side: str, forearm: bool = False) -> np.ndarray:
        """The direction the bone points at rest, per the skeleton's own spread angles."""
        angle = math.radians(self.forearm_spread if forearm else self.arm_spread)
        sign = 1.0 if side == ".L" else -1.0
        return np.array([sign * math.sin(angle), -math.cos(angle), 0.0])

    @staticmethod
    def from_bones(bones, p) -> "Rig":
        """Read the rig off a real skeleton, so the IK aims with the bones that will actually move.

        Measuring beats recomputing: the arm's rest length is whatever the skeleton made it, and the
        socket is wherever the shoulder bone really ends. A rig derived from the numbers instead of
        from the bones is off by a centimetre or two, which is the difference between a hand that
        rests on the brow and one that hovers over it.
        """
        by_name = {bone.name: bone for bone in bones}

        def length(name: str) -> float:
            bone = by_name[name]
            return float(np.linalg.norm(bone.tail - bone.head))

        head = by_name["head"]
        return Rig(
            shoulder_y=float(by_name["upper_arm.R"].head[1] / p.h),
            shoulder_x=float(abs(by_name["upper_arm.R"].head[0]) / p.h),
            upper=length("upper_arm.R") / p.h,
            fore=length("forearm.R") / p.h,
            hand=length("hand.R") / p.h,
            chest_y=float(p.chest),
            head_y=float(head.head[1] / p.h),
            brow_y=float(p.head_centre) + float(p.head_half) * 0.42,
            eye_y=float(p.head_centre),
            head_z=float(p.head_half) * float(p.depth_ratio),
            hip_y=float(p.hip),
            arm_spread=float(p.arm_spread),
            forearm_spread=float(p.forearm_spread),
        )


DEFAULT_RIG = Rig()


# ------------------------------------------------------------------------------------------------
# Aiming
# ------------------------------------------------------------------------------------------------


def aim_chain(
    upper: str,
    upper_to,
    fore_to,
    hand_to=None,
    roll: float = 0.0,
    rig: Rig = DEFAULT_RIG,
) -> dict[str, np.ndarray]:
    """Local quaternions that point a two-bone arm at resting-space directions.

    `upper_to` is where the upper arm should point in body space, `fore_to` the forearm, and so on.
    The forearm's local rotation is computed against whatever the upper arm just did, so a turned
    chest does not throw the hand off the brow.
    """
    side = ".L" if upper.endswith(".L") else ".R"
    out: dict[str, np.ndarray] = {}
    parent = q_axis("z", roll) if roll else IDENTITY.copy()

    for bone, rest, target in (
        (upper, rig.rest_direction(side), upper_to),
        (upper.replace("upper_arm", "forearm"), rig.rest_direction(side, forearm=True), fore_to),
        (upper.replace("upper_arm", "hand"), rig.rest_direction(side, forearm=True), fore_to if hand_to is None else hand_to),
    ):
        world_rest = q_rotate(parent, rest)
        delta = q_from_to(world_rest, unit(target))
        local = q_mul(q_mul(q_conj(parent), delta), parent)
        out[bone] = local / (np.linalg.norm(local) or 1.0)
        parent = q_mul(parent, out[bone])
    return out


def solve_arm(side: str, target, pole, reach: float = 0.985, rig: Rig = DEFAULT_RIG) -> dict[str, np.ndarray]:
    """Two-bone IK: put the wrist at `target`, with the elbow pushed towards `pole`.

    All arguments are in units of body height, so the same target works for any character height.
    The chain is clamped just inside full extension, which keeps the elbow from locking into a
    perfectly straight pipe (the classic tell of a badly posed arm).
    """
    shoulder = np.array([rig.shoulder_x if side == ".L" else -rig.shoulder_x, rig.shoulder_y, 0.0])
    goal = np.array(target, dtype=np.float64)
    d = goal - shoulder
    distance = float(np.linalg.norm(d))
    if distance < 1e-6:
        d = np.array([0.0, -1.0, 0.0])
        distance = 1e-6
    direction = d / distance
    upper, fore = rig.upper, rig.fore
    span = upper + fore
    distance = min(distance, span * reach)
    distance = max(distance, abs(upper - fore) + 1e-3)

    # Law of cosines: how far along the shoulder→goal line the elbow projects.
    along = (upper ** 2 - fore ** 2 + distance ** 2) / (2.0 * distance)
    height_sq = max(upper ** 2 - along ** 2, 0.0)
    bend = math.sqrt(height_sq)

    pole = np.asarray(pole, dtype=np.float64)
    side_component = pole - direction * float(pole @ direction)
    if np.linalg.norm(side_component) < 1e-6:
        side_component = np.cross(direction, np.array([0.0, 0.0, 1.0]))
        if np.linalg.norm(side_component) < 1e-6:
            side_component = np.array([1.0, 0.0, 0.0])
    side_component /= np.linalg.norm(side_component)

    elbow = shoulder + direction * along + side_component * bend
    wrist = shoulder + direction * distance
    hand_dir = wrist - elbow
    return aim_chain(
        f"upper_arm{side}",
        elbow - shoulder,
        wrist - elbow,
        hand_dir,  # the hand continues the forearm
        rig=rig,
    )


# ------------------------------------------------------------------------------------------------
# Clips
# ------------------------------------------------------------------------------------------------

Pose = dict[str, np.ndarray]  # bone -> local quaternion


@dataclass(frozen=True)
class Clip:
    name: str
    length: float
    pose: object
    loop: bool = True

    @property
    def frames(self) -> int:
        return int(round(self.length * FPS)) + 1

    def at(self, time_s: float, rig: Rig | None = None) -> Pose:
        """The pose at `time_s` for a rig. Clamped to the clip, so callers can hand it any clock."""
        return self.pose(min(max(time_s, 0.0), self.length), rig or DEFAULT_RIG)


def _sin(t: float, period: float) -> float:
    return math.sin(2.0 * math.pi * t / period)


def _cos(t: float, period: float) -> float:
    return math.cos(2.0 * math.pi * t / period)


def _torso(breath: float, shift: float, look: float, bend: float = 0.0) -> Pose:
    return {
        "hips": q_euler(0.0, shift * 0.8, shift * 0.7),
        "spine": q_euler(-0.6 + breath * 0.7 + bend, shift * 0.4, shift * 0.3),
        "chest": q_euler(breath * 1.1 + bend * 0.6, shift * 0.2, -shift * 0.4),
        "neck": q_euler(-breath * 0.4 - bend * 0.3, look * 1.5, 0.0),
        "head": q_euler(-1.2 + breath * 0.5 - bend * 0.5, look * 3.0, -shift * 0.6),
        "shoulder.L": q_euler(0.0, 0.0, breath * 0.9),
        "shoulder.R": q_euler(0.0, 0.0, -breath * 0.9),
        "thigh.L": q_euler(0.0, 0.0, -shift * 0.5),
        "thigh.R": q_euler(0.0, 0.0, -shift * 0.5),
        "hand.L": q_euler(2.0, 0.0, 1.0),
        "hand.R": q_euler(2.0, 0.0, -1.0),
    }


def _arms_rest(breath: float, shift: float) -> Pose:
    """Arms hanging in the A-pose, with the breath moving them a degree or two."""
    return {
        "upper_arm.L": q_euler(breath * 0.5, 0.0, 1.6 + shift * 0.5),
        "upper_arm.R": q_euler(breath * 0.5, 0.0, -1.6 - shift * 0.5),
        "forearm.L": q_euler(0.0, 0.0, 2.0 + breath * 0.4),
        "forearm.R": q_euler(0.0, 0.0, -2.0 - breath * 0.4),
    }


def idle_breathe(t: float, rig: Rig = DEFAULT_RIG) -> Pose:
    """Breath on 4 s, weight shift on 8 s: different periods, so the loop never reads as a loop."""
    breath, shift, look = _sin(t, 4.0), _sin(t, 8.0), _cos(t, 8.0)
    pose = _torso(breath, shift, look)
    pose.update(_arms_rest(breath, shift))
    return pose


def day_shade(t: float, rig: Rig = DEFAULT_RIG) -> Pose:
    """Noon: hand up to the brow like a visor, weight on the left leg, other hand on the hip."""
    breath, sway = _sin(t, 4.0), _sin(t, 8.0)
    pose = _torso(breath, sway * 0.8, 0.0, bend=-1.0)
    pose["hips"] = q_euler(0.0, -2.5 + sway * 0.8, 1.4)
    pose["chest"] = q_euler(breath * 0.8 - 1.0, -3.0, -1.0)
    pose["neck"] = q_euler(-1.0, -2.0, 0.0)
    pose["head"] = q_euler(-3.0 + breath * 0.4, -4.0 + sway * 1.5, 1.5)
    pose["shoulder.R"] = q_euler(0.0, 0.0, -8.0)
    pose["shoulder.L"] = q_euler(0.0, 0.0, 4.0)
    # Right wrist to the brow, elbow pushed out and forward: a visor against the noon sun.
    # Just above the brow, and only a hand's depth in front of the face: the hand is a visor, not a
    # wave, and a hand that floats at temple height in mid-air reads as neither.
    brow = rig.brow_y + 0.012
    pose.update(solve_arm(
        ".R",
        (-rig.shoulder_x * 0.62 + sway * 0.004, brow, rig.head_z * 1.35),
        (-1.0, -0.35, 0.30),
        rig=rig,
    ))
    # Left hand rests on the hip.
    pose.update(solve_arm(
        ".L",
        (rig.shoulder_x * 0.95, rig.hip_y + 0.075, rig.head_z * 0.8),
        (1.0, 0.10, 0.55),
        reach=0.93,
        rig=rig,
    ))
    pose["thigh.R"] = q_euler(-2.0, 0.0, -3.0)
    pose["shin.R"] = q_euler(3.0, 0.0, 0.0)
    return pose


def dusk_phone(t: float, rig: Rig = DEFAULT_RIG) -> Pose:
    """Twilight: both elbows in, head down to the screen, a slow glance up at the horizon."""
    breath = _sin(t, 4.0)
    glance = max(0.0, _sin(t, 8.0) - 0.55) / 0.45
    pose = _torso(breath, _sin(t, 8.0) * 0.5, 0.0, bend=1.6)
    pose["neck"] = q_euler(4.0 - glance * 3.0, 0.0, 0.0)
    pose["head"] = q_euler(9.0 - glance * 15.0 + breath * 0.4, 3.0 * _cos(t, 8.0), -1.5)
    pose["shoulder.R"] = q_euler(0.0, 0.0, -4.0)
    pose["shoulder.L"] = q_euler(0.0, 0.0, 4.0)
    # Both wrists meet in front of the chest, as if around a phone; elbows tucked in.
    phone_y = rig.chest_y - 0.020
    phone_z = rig.head_z * 4.0
    pose.update(solve_arm(".R", (-0.030, phone_y, phone_z), (-1.0, -0.55, 0.15), rig=rig))
    pose.update(solve_arm(".L", (0.030, phone_y, phone_z), (1.0, -0.55, 0.15), rig=rig))
    return pose


def night_point(t: float, rig: Rig = DEFAULT_RIG) -> Pose:
    """Astronomical night: right arm up to the sky, head tipped back, chest open."""
    breath, sweep = _sin(t, 4.0), _sin(t, 8.0)
    pose = _torso(breath, -0.8 + breath * 0.3, 0.0, bend=-2.0)
    pose["hips"] = q_euler(0.0, 1.5, -0.8 + breath * 0.3)
    pose["spine"] = q_euler(-2.0 + breath * 0.6, 1.0, 0.0)
    pose["chest"] = q_euler(-3.0 + breath * 0.9, 1.5, 0.0)
    pose["neck"] = q_euler(-6.0, 2.0, 0.0)
    pose["head"] = q_euler(-13.0 + breath * 0.5, 6.0 * sweep, 1.0)
    pose["shoulder.R"] = q_euler(0.0, 0.0, -10.0)
    pose["shoulder.L"] = q_euler(0.0, 0.0, 4.0)
    # Left arm hangs; right wrist is above the head, pointing at the sky, elbow out to the side.
    pose.update(_arms_rest(breath, 1.0))
    pose.update(solve_arm(
        ".R",
        (-rig.shoulder_x * 2.2 + sweep * 0.012, rig.brow_y + 0.115, rig.head_z * 1.35),
        (-1.0, -0.20, 0.35),
        rig=rig,
    ))
    return pose


def wave_hello(t: float, rig: Rig = DEFAULT_RIG) -> Pose:
    """The one generic clip worth keeping: a friendly wave, hand above the shoulder."""
    breath = _sin(t, 4.0)
    wave = math.sin(2.0 * math.pi * 2.0 * t / 3.0)
    pose = _torso(breath, 0.6, 0.0)
    pose["chest"] = q_euler(breath * 0.8, 3.0, -1.0)
    pose["neck"] = q_euler(-1.0, 3.0, 0.0)
    pose["head"] = q_euler(-2.0, 5.0, 2.0)
    pose["shoulder.R"] = q_euler(0.0, 0.0, -12.0)
    pose.update(_arms_rest(breath, 0.0))
    # Right hand up beside the head, waving from the wrist via a moving elbow pole.
    pose.update(solve_arm(
        ".R",
        (-rig.shoulder_x * 2.6 + wave * 0.020, rig.brow_y + 0.065, rig.head_z * 2.4),
        (-1.0, -0.10, 0.30 + wave * 0.25),
        rig=rig,
    ))
    return pose


# All three scene beats run 8 s: slow enough to read as stillness on a home screen, and long enough
# that every underlying period (breath 4 s, weight shift 8 s) divides it exactly, so each clip loops.
CLIPS: tuple[Clip, ...] = (
    Clip("idle_breathe", 8.0, idle_breathe),
    Clip("day_shade", 8.0, day_shade),
    Clip("dusk_phone", 8.0, dusk_phone),
    Clip("night_point", 8.0, night_point),
    Clip("wave_hello", 3.0, wave_hello, loop=False),
)

CLIPS_BY_NAME = {clip.name: clip for clip in CLIPS}


def sample_clip(clip: Clip, bone_names: tuple[str, ...], rig: Rig | None = None) -> tuple[np.ndarray, dict[str, np.ndarray]]:
    """(times, {bone: (frames, 4) quaternions}) sampled at FPS, last frame inclusive."""
    frames = clip.frames
    times = np.array([i / FPS for i in range(frames)], dtype=np.float64)
    out: dict[str, np.ndarray] = {}
    for bone in bone_names:
        keys = np.zeros((frames, 4), dtype=np.float64)
        for i, time in enumerate(times):
            pose = clip.at(float(time), rig)
            keys[i] = pose.get(bone, IDENTITY)
        out[bone] = keys
    return times, out


def loop_error(clip: Clip, bone_names: tuple[str, ...], rig: Rig | None = None) -> float:
    """Largest angular gap, in degrees, between the first and last sampled pose of a looping clip."""
    _, keys = sample_clip(clip, bone_names, rig)
    worst = 0.0
    for track in keys.values():
        a, b = track[0], track[-1]
        dot = max(-1.0, min(1.0, abs(float(a @ b))))
        worst = max(worst, math.degrees(2.0 * math.acos(dot)))
    return worst
