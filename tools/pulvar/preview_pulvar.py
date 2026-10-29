#!/usr/bin/env python3
"""
Pulvar River 3D — the supplied `PulvarRiver3D.kt` design, mirrored for the flat landscape.

This is the tuning mirror of `ui/rendering/PulvarRiver.kt` + `PulvarRiverSprite.kt`: the same
hydraulic layout (224 m reach, pool-riffle-run sequence, asymmetric thalweg, gravel point bars,
boulders with wakes, cobbles, reeds, grass), the same deterministic RNG and consumption order, and
the same shading model (the design's GLSL `shadeOpaque` / `shadeWater` ported to Python with the
same ACES + gamma tone map).

What differs from the design is only the *projection*, and that is the point of the port:

  * the design renders through a fixed 3D side camera into a GLSurfaceView;
  * this landscape is a flat Compose canvas, so the corridor is projected the way the rest of the
    landscape already draws it — downstream x across the canvas, the channel's lateral axis down
    the canvas, height into colour and small relief. `Frame` below is that projection.

Run from the repo root:
    python3 tools/pulvar/preview_pulvar.py
Writes docs/pulvar-river.png (a four-up of one reach at noon / golden hour / night, plus the
composition inside the Marvdasht landscape band).
"""

import math
import os
import sys

from PIL import Image, ImageDraw

PI = 3.1415927

# ============================================================================
# Deterministic RNG (mulberry32) and integer value noise — the design's own.
# ============================================================================


class Rng:
    def __init__(self, seed):
        self.s = seed & 0xFFFFFFFF

    def rnd(self):
        self.s = (self.s + 0x6D2B79F5) & 0xFFFFFFFF
        t = ((self.s ^ (self.s >> 15)) * (1 | self.s)) & 0xFFFFFFFF
        t = ((t + (((t ^ (t >> 7)) * (61 | t)) & 0xFFFFFFFF)) ^ t) & 0xFFFFFFFF
        return ((t ^ (t >> 14)) & 0xFFFFFFFF) / 4294967296.0

    def rr(self, a, b):
        return a + (b - a) * self.rnd()


def hash3(x, y, z):
    h = (x * 374761393 + y * 668265263 + z * 1274126177) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1103515245) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFFFF) / 4294967295.0


def lerp(a, b, t):
    return a + (b - a) * t


def clamp(x, a, b):
    return a if x < a else (b if x > b else x)


def smooth(a, b, x):
    t = clamp((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def mix3(a, b, t):
    return (lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t))


def noise3(x, y, z):
    xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
    xf, yf, zf = x - xi, y - yi, z - zi
    u = xf * xf * (3 - 2 * xf)
    v = yf * yf * (3 - 2 * yf)
    w = zf * zf * (3 - 2 * zf)
    xi, yi, zi = int(xi), int(yi), int(zi)
    a = lerp(hash3(xi, yi, zi), hash3(xi + 1, yi, zi), u)
    b = lerp(hash3(xi, yi + 1, zi), hash3(xi + 1, yi + 1, zi), u)
    c = lerp(hash3(xi, yi, zi + 1), hash3(xi + 1, yi, zi + 1), u)
    d = lerp(hash3(xi, yi + 1, zi + 1), hash3(xi + 1, yi + 1, zi + 1), u)
    return lerp(lerp(a, b, v), lerp(c, d, v), w)


def fbm3(x, y, z, oct):
    s, amp, norm = 0.0, 0.5, 0.0
    fx, fy, fz = x, y, z
    for _ in range(oct):
        s += amp * noise3(fx, fy, fz)
        norm += amp
        fx = fx * 2.03 + 11.7
        fy = fy * 2.01 + 7.3
        fz = fz * 2.05 + 5.1
        amp *= 0.5
    return s / norm


def noise2(x, z):
    return noise3(x, 0.0, z)


def fbm2(x, z, oct):
    return fbm3(x, 0.0, z, oct)


# ============================================================================
# Albedo palette (the design's, sRGB-ish; the shader linearises with ^2.2)
# ============================================================================

PAL = {
    "BED_SAND": (0.48, 0.40, 0.29),
    "BED_PEBBLE": (0.34, 0.32, 0.28),
    "BED_ALGAE": (0.20, 0.31, 0.18),
    "BED_DARK": (0.16, 0.19, 0.15),
    "WET_EARTH": (0.20, 0.16, 0.12),
    "GRAVEL": (0.46, 0.43, 0.36),
    "SAND_DRY": (0.55, 0.48, 0.35),
    "EARTH": (0.32, 0.24, 0.16),
    "GRASS_LUSH": (0.32, 0.40, 0.18),
    "GRASS_DEEP": (0.22, 0.31, 0.13),
    "GRASS_DRY": (0.55, 0.52, 0.26),
    "SEDGE": (0.35, 0.43, 0.20),
    "ROCK_LIGHT": (0.46, 0.45, 0.41),
    "ROCK_MID": (0.29, 0.29, 0.27),
    "ROCK_DARK": (0.15, 0.16, 0.15),
    "ROCK_MOSS": (0.24, 0.36, 0.18),
    "REED_BASE": (0.22, 0.34, 0.15),
    "REED_TIP": (0.39, 0.46, 0.20),
    "REED_SEED": (0.58, 0.46, 0.27),
    "VEG_MUTE": (0.33, 0.36, 0.24),
}

# ============================================================================
# Hydraulic layout — the design's reach table and channel functions, verbatim.
# ============================================================================

X0, X1 = -112.0, 112.0
LEN = X1 - X0

#              x       depth  speed  foam  width×  waterY
REACH = [
    (-112.0, 1.70, 0.48, 0.05, 1.02, 3.00),
    (-92.0, 2.75, 0.27, 0.02, 1.14, 2.84),
    (-72.0, 0.66, 1.28, 0.58, 0.82, 2.30),
    (-50.0, 1.50, 0.72, 0.12, 0.98, 2.12),
    (-28.0, 2.45, 0.34, 0.04, 1.11, 1.96),
    (-8.0, 0.54, 1.58, 0.88, 0.78, 1.34),
    (12.0, 1.42, 0.76, 0.18, 0.96, 1.16),
    (34.0, 2.95, 0.25, 0.02, 1.17, 1.02),
    (58.0, 0.80, 1.16, 0.47, 0.86, 0.62),
    (78.0, 1.60, 0.66, 0.10, 1.00, 0.48),
    (100.0, 2.25, 0.36, 0.03, 1.09, 0.36),
    (112.0, 1.80, 0.50, 0.05, 1.01, 0.28),
]


class Reach:
    __slots__ = ("depth", "speed", "foam", "width", "water_y")

    def __init__(self, depth, speed, foam, width, waterY):
        self.depth, self.speed, self.foam, self.width, self.water_y = depth, speed, foam, width, waterY


def reach_at(x):
    r = REACH
    if x <= r[0][0]:
        return Reach(*r[0][1:])
    for i in range(len(r) - 1):
        if x <= r[i + 1][0]:
            raw = (x - r[i][0]) / (r[i + 1][0] - r[i][0])
            t = raw * raw * (3 - 2 * raw)
            return Reach(*[lerp(r[i][k], r[i + 1][k], t) for k in range(1, 6)])
    return Reach(*r[-1][1:])


def progress(x):
    return (x - X0) / LEN


def bend_phase(x):
    return math.sin(progress(x) * 4.6 + 0.7)


def center_z(x):
    p = progress(x)
    return (-3.0 + (p - 0.5) * 9.0 + 10.5 * math.sin(p * 4.6 + 0.7)
            + 5.0 * math.sin(p * 9.3 - 1.15) + 1.8 * math.sin(p * 17.7 + 2.1))


def half_width(x):
    p = progress(x)
    reach = reach_at(x)
    base = 11.3 + 1.5 * math.sin(p * 3.1 - 0.4) + 0.8 * math.sin(p * 7.7 + 1.8)
    return clamp(base * reach.width, 8.2, 15.9)


def water_y(x):
    return reach_at(x).water_y


def thalweg_t(x):
    return 0.46 * bend_phase(x) + 0.08 * math.sin(progress(x) * 11.0 + 0.4)


def profile_depth(x, t):
    reach = reach_at(x)
    t0 = thalweg_t(x)
    denom = (1.0 + t0) if t < t0 else (1.0 - t0)
    q = clamp(1.0 - abs(t - t0) / max(denom, 0.05), 0.0, 1.0)
    return reach.depth * (0.05 + 0.95 * math.sin(q * PI * 0.5) ** 1.28)


def point_bar_raise(x, t, depth):
    bend = bend_phase(x)
    inner = -1.0 if bend >= 0 else 1.0
    if t * inner <= 0:
        return 0.0
    reach = reach_at(x)
    bend_strength = smooth(0.38, 1.0, abs(bend))
    pool_strength = smooth(1.15, 2.5, reach.depth)
    lat = math.exp(-(((abs(t) - 0.74) ** 2) / (2 * 0.20 * 0.20)))
    patch = 0.78 + 0.28 * fbm2(x * 0.045 + 4.0, t * 2.0, 2)
    return bend_strength * pool_strength * lat * patch * (depth + 0.48) * 1.08


def bed_height(x, t):
    wy = water_y(x)
    depth = profile_depth(x, t)
    bar = point_bar_raise(x, t, depth)
    half = half_width(x)
    bed_noise = (fbm2(x * 0.31 + 8.0, t * half * 0.31 - 3.0, 3) - 0.5) * 0.16
    edge_calm = smooth(1.0, 0.82, abs(t))
    return wy - depth + bar + bed_noise * (0.25 + 0.75 * edge_calm)


def terrain_point(x, q):
    """World (x, y, z) of the corridor surface at downstream x and normalised lateral q."""
    half = half_width(x)
    wx = x
    wz = center_z(x) + q * half
    wy = water_y(x)
    aq = abs(q)
    if aq <= 1.0:
        y = bed_height(x, q)
    else:
        sign = -1.0 if q < 0 else 1.0
        edge = bed_height(x, sign)
        bend = bend_phase(x)
        outer = smooth(0.2, 0.9, abs(bend)) if sign == (1.0 if bend >= 0 else -1.0) else 0.0
        base_bank = 0.82 if sign < 0 else 0.58
        bank_h = base_bank + 0.28 * outer - 0.08 * (1.0 - outer)
        immediate = (0.15 + 0.30 * outer) * smooth(1.0, 1.20, aq)
        rise = smooth(1.02, 1.75, aq) ** 1.25
        bank = wy - 0.04 + immediate + bank_h * rise
        bank += (fbm2(wx * 0.035 + 2.0, wz * 0.035 - 6.0, 3) - 0.5) * 0.38 * smooth(1.2, 3.2, aq)
        bank += (fbm2(wx * 0.13 - 4.0, wz * 0.13 + 1.0, 2) - 0.5) * 0.12 * smooth(1.0, 1.5, aq)
        y = lerp(edge, bank, smooth(1.0, 1.28, aq))
    return (wx, y, wz)


def terrain_colour(x, q, p):
    wy = water_y(x)
    reach = reach_at(x)
    aq = abs(q)
    submerged = max(0.0, wy - p[1])
    above = max(0.0, p[1] - wy)
    n1 = fbm2(p[0] * 0.16 + 7.0, p[2] * 0.16 - 2.0, 3)
    n2 = noise2(p[0] * 0.85 - 3.0, p[2] * 0.85 + 5.0)
    if aq <= 1.03 and submerged > 0.03:
        col = mix3(PAL["BED_PEBBLE"], PAL["BED_SAND"], n1)
        algae = (smooth(0.12, 0.7, submerged) * (1.0 - smooth(1.2, 2.6, submerged))
                 * (1.0 - reach.foam * 0.75))
        col = mix3(col, PAL["BED_ALGAE"], algae * (0.35 + 0.45 * n1) * 0.50)
        col = mix3(col, PAL["BED_DARK"], smooth(1.0, 3.0, submerged) * 0.72)
    elif aq <= 1.10:
        col = mix3(PAL["GRAVEL"], PAL["SAND_DRY"], n1)
        col = mix3(col, PAL["WET_EARTH"], (1.0 - smooth(0.02, 0.18, above)) * 0.68)
        if n2 > 0.80:
            col = mix3(col, PAL["BED_PEBBLE"], 0.45)
        col = mix3(col, PAL["SEDGE"],
                   smooth(0.58, 0.88, n1) * smooth(0.05, 0.22, above) * 0.38)
    else:
        col = mix3(PAL["GRAVEL"], PAL["EARTH"], smooth(1.10, 1.34, aq) * 0.60)
        grass = mix3(PAL["SEDGE"], PAL["GRASS_LUSH"], n1)
        grass = mix3(grass, PAL["GRASS_DEEP"], smooth(0.45, 0.80, n1) * 0.62)
        dry = fbm2(p[0] * 0.028 - 6.0, p[2] * 0.028 + 3.0, 3)
        grass = mix3(grass, PAL["GRASS_DRY"], smooth(0.46, 0.78, dry) * 0.55)
        grass = mix3(grass, PAL["EARTH"],
                     smooth(0.72, 0.94, fbm2(p[0] * 0.075 + 8.0, p[2] * 0.075, 2)) * 0.22)
        col = mix3(col, grass, smooth(1.16, 1.48, aq))
        if n2 > 0.86 and aq < 1.75:
            col = mix3(col, PAL["EARTH"], 0.38)
    v = 0.90 + 0.20 * n2
    return (col[0] * v, col[1] * v, col[2] * v)


def surface_speed01(x, t):
    reach = reach_at(x)
    t0 = thalweg_t(x)
    width = lerp(0.46, 0.78, reach.foam)
    cross = 0.38 + 0.78 * math.exp(-(((t - t0) ** 2) / (2 * width * width)))
    return clamp(reach.speed * cross / 1.72, 0.03, 1.0)


# ============================================================================
# Boulders, cobbles, reeds, grass — the design's builders, same seed and order.
# ============================================================================


class Rock:
    __slots__ = ("x", "q", "radius", "channel", "wx", "wy", "wz", "rx", "ry", "rz",
                 "yaw", "seed", "level", "water_y")


def rock_descriptors():
    rng = Rng(-1317134298)
    rocks = []

    def add_rock(x, q, radius, channel):
        ry = radius * rng.rr(0.52, 0.86)
        rz = radius * rng.rr(0.65, 1.25)
        yaw = rng.rr(0.0, PI * 2)
        seed = rng.rr(0.0, 100.0)
        level = 3 if radius > 0.92 else 2
        wy = water_y(x)
        if channel:
            center_y = bed_height(x, q) + ry * 0.22
            if radius > 1.15:
                center_y = max(center_y, wy - ry * 0.34)
        else:
            center_y = terrain_point(x, q)[1] + ry * 0.02
        r = Rock()
        r.x, r.q, r.radius, r.channel = x, q, radius, channel
        r.wx, r.wy, r.wz = x, center_y, center_z(x) + q * half_width(x)
        r.rx, r.ry, r.rz = radius, ry, rz
        r.yaw, r.seed, r.level, r.water_y = yaw, seed, level, wy
        rocks.append(r)

    riffles = ((-72.0, 14), (-8.0, 21), (58.0, 13))
    for cx, count in riffles:
        for _ in range(count):
            spread = (rng.rnd() + rng.rnd() + rng.rnd()) / 3.0 - 0.5
            x = cx + spread * 2.0 * 11.0
            big = rng.rnd()
            radius = rng.rr(1.7, 2.7) if big > 0.86 else rng.rr(0.45, 1.55)
            add_rock(x, rng.rr(-0.86, 0.86), radius, True)
    for _ in range(16):
        add_rock(rng.rr(-106.0, 106.0), rng.rr(-0.82, 0.82), rng.rr(0.5, 1.5), True)
    for _ in range(24):
        side = -1.0 if rng.rnd() < 0.5 else 1.0
        add_rock(rng.rr(-108.0, 108.0), side * rng.rr(0.96, 1.32), rng.rr(0.65, 2.6), False)
    return rocks


class Pebble:
    __slots__ = ("wx", "wy", "wz", "rx", "ry", "rz", "yaw", "seed", "water_y")


class Clump:
    __slots__ = ("blades", "base_x", "base_y", "base_z")


class Blade:
    __slots__ = ("x", "y", "z", "yaw", "lean", "height", "width", "tip_dry", "seed",
                 "seed_head", "moisture")


def scatter_details(rocks):
    """Cobbles, reeds and grass, in the design's exact RNG order."""
    rng = Rng(-1317134298)
    # the design consumes the rock pass first
    riffles = ((-72.0, 14), (-8.0, 21), (58.0, 13))
    for _cx, count in riffles:
        for _ in range(count):
            rng.rnd(); rng.rnd(); rng.rnd(); rng.rnd(); rng.rnd()
            rng.rnd()  # radius (one draw either way)
            rng.rnd()  # q
    for _ in range(16):
        for _k in range(7):  # x, q, radius, ry, rz, yaw, seed
            rng.rnd()
    for _ in range(24):
        rng.rnd(); rng.rnd(); rng.rnd()  # side, x, q
        for _k in range(4):  # radius, ry, rz, yaw, seed -> 5 draws
            rng.rnd()
        rng.rnd()
    pebbles = []
    for _ in range(320):
        x = rng.rr(-109.0, 109.0)
        side = -1.0 if rng.rnd() < 0.5 else 1.0
        mode = rng.rnd()
        q = side * rng.rr(0.68, 1.04) if mode < 0.46 else side * rng.rr(1.02, 1.58)
        ground = terrain_point(x, q)
        radius = rng.rr(0.17, 0.62)
        ry = radius * rng.rr(0.5, 0.82)
        seed = rng.rr(0.0, 100.0)
        yaw = rng.rr(0.0, PI * 2)
        rz = radius * rng.rr(0.7, 1.25)
        p = Pebble()
        p.wx, p.wy, p.wz = ground[0], ground[1] + ry * 0.28, ground[2]
        p.rx, p.ry, p.rz = radius, ry, rz
        p.yaw, p.seed, p.water_y = yaw, seed, water_y(x)
        pebbles.append(p)
    reeds = []
    for _ in range(230):
        x = rng.rr(-110.0, 110.0)
        reach = reach_at(x)
        if rng.rnd() < reach.foam * 0.72:
            continue
        side = 1.0 if rng.rnd() < 0.58 else -1.0
        q = side * rng.rr(0.94, 1.16)
        base = terrain_point(x, q)
        if base[1] < water_y(x) - 0.48:
            continue
        blades = 7 + int(math.floor(rng.rnd() * 6.0))
        clump = Clump()
        clump.blades = []
        clump.base_x, clump.base_y, clump.base_z = base
        for _j in range(blades):
            b = Blade()
            b.x = base[0] + rng.rr(-0.58, 0.58)
            b.z = base[2] + rng.rr(-0.58, 0.58)
            b.y = base[1] - 0.05
            b.yaw = rng.rr(0.0, PI * 2)
            b.lean = rng.rr(-0.38, 0.38)
            b.height = rng.rr(0.92, 2.15)
            b.width = rng.rr(0.075, 0.135)
            b.seed_head = rng.rnd() < 0.20
            b.seed = rng.rnd()
            b.tip_dry = False
            b.moisture = clamp(1.1 - (b.y - water_y(x)), 0.25, 1.0)
            clump.blades.append(b)
        reeds.append(clump)
    grass = []
    for _ in range(260):
        x = rng.rr(-110.0, 110.0)
        side = 1.0 if rng.rnd() < 0.55 else -1.0
        q = side * (rng.rr(1.12, 2.95) if side > 0 else rng.rr(1.12, 1.82))
        base = terrain_point(x, q)
        blades = 3 + int(math.floor(rng.rnd() * 4.0))
        for _j in range(blades):
            b = Blade()
            b.x = base[0] + rng.rr(-0.28, 0.28)
            b.z = base[2] + rng.rr(-0.28, 0.28)
            b.y = base[1] - 0.03
            b.yaw = rng.rr(0.0, PI * 2)
            b.lean = rng.rr(-0.28, 0.28)
            b.height = rng.rr(0.24, 0.62)
            b.width = rng.rr(0.045, 0.082)
            b.tip_dry = rng.rnd() < 0.28
            b.seed = rng.rnd()
            b.moisture = 0.55
            b.seed_head = False
            grass.append(b)
    return pebbles, reeds, grass


def rock_wake(x, t, wx, wz, rocks):
    foam, lift = 0.0, 0.0
    for r in rocks:
        if not r.channel:
            continue
        dx = wx - r.wx
        if dx < -r.radius * 2.0 or dx > r.radius * 10.0:
            continue
        dz = wz - r.wz
        if abs(dz) > r.radius * 4.5 + max(0.0, dx) * 0.45:
            continue
        speed = surface_speed01(x, t)
        strength = clamp(0.35 + speed * 0.95, 0.35, 1.25)
        rr = math.sqrt(dx * dx + dz * dz)
        collar = math.exp(-((max(0.0, rr - r.radius * 0.72) ** 2)
                            / (2 * (r.radius * 0.42) ** 2)))
        foam += collar * 0.85 * strength
        if dx > 0.0:
            voff = r.radius * 0.58 + dx * 0.30
            band = math.exp(-(((abs(dz) - voff) ** 2) / (2 * (0.30 + dx * 0.055) ** 2)))
            decay = math.exp(-(dx / (r.radius * 5.2)))
            foam += band * decay * 0.72 * strength
            centre = (math.exp(-(dz * dz) / (2 * (r.radius * 0.62) ** 2))
                      * math.exp(-(dx / (r.radius * 3.1))))
            foam += centre * 0.58 * strength
            lift += (math.exp(-(((dx - r.radius * 1.35) ** 2) / (2 * (r.radius * 0.85) ** 2)))
                     * math.exp(-(dz * dz) / (2 * (r.radius * 0.95) ** 2)) * strength)
        else:
            lift += collar * 0.35 * strength
    return clamp(foam, 0.0, 1.15), clamp(lift, 0.0, 1.2)


# ============================================================================
# Shading — the design's `shadeOpaque` / `shadeWater`, at a reference daylight.
# ============================================================================


def aces_tone(c):
    out = []
    for v in c:
        x = clamp(v * 1.02, 0.0, 60.0)
        t = clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0)
        out.append(t)
    lum = 0.2126 * out[0] + 0.7152 * out[1] + 0.0722 * out[2]
    return tuple(clamp(lerp(out[i], lum, 0.10), 0.0, 1.0) ** (1.0 / 2.2) for i in range(3))


def wrap(nd, w):
    return clamp((nd + w) / (1.0 + w), 0.0, 1.0)


class Light:
    """Reference daylight, the design's full-day stops."""

    def __init__(self, sun_sign=1.0, altitude=52.0):
        d = max(0.0, math.sin(math.radians(max(altitude, 0.0))))
        c = (1.0, 0.96, 0.88) if altitude >= 25 else (1.0, 0.88, 0.66)
        s = 2.9 * (0.12 + 0.88 * d ** 0.65)
        self.sun = (c[0] * s, c[1] * s, c[2] * s)
        # the design's sun direction at az 205°, alt 52°, mirrored by the Sun's side
        self.sun_dir = normalize((-0.262 * sun_sign, 0.788, 0.558))
        self.sky = (0.312, 0.436, 0.605)
        self.ground = (0.16, 0.17, 0.13)
        self.view = normalize((0.0, 0.34, 1.0))  # the design's camera elevation


def normalize(v):
    l = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return (v[0] / l, v[1] / l, v[2] / l)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def shade_opaque(light, albedo, normal, world, moisture, submerged, mat):
    """Port of the design's shadeOpaque(); returns finished sRGB 0..1."""
    lin = tuple(clamp(c, 0.0, 1.0) ** 2.2 for c in albedo)
    n_mid = fbm2(world[0] * 1.25 + 3.0, world[2] * 1.25 + 7.0, 4)
    n_micro = noise2(world[0] * 7.0 + world[1] * 3.0, world[2] * 7.0 + world[1] * 3.0)
    n_fine = noise2(world[0] * 23.0 - world[1] * 5.0, world[2] * 23.0 - world[1] * 5.0)
    k = 0.84 + 0.24 * n_mid + 0.10 * n_micro
    lin = tuple(min(1.6, c * k) for c in lin)
    if mat == 0:
        pebble = smooth(0.68, 0.82, noise2(world[0] * 3.1 + 11.0, world[2] * 3.1 + 11.0))
        lin = mix3(lin, (0.16, 0.15, 0.13), pebble * 0.22 * (0.35 + moisture))
        green = smooth(0.015, 0.09, lin[1] - max(lin[0], lin[2]))
        streak = fbm2(world[0] * 0.72, world[2] * 0.72, 4) * 0.55 + \
            noise2(world[0] * 3.1, world[2] * 3.1) * 0.45
        lin = tuple(c * (1.0 + green * (streak - 0.5) * 0.16) for c in lin)
        gpa = fbm2(world[0] * 0.33 + 4.0, world[2] * 0.33 + 4.0, 4)
        gpb = noise2(world[0] * 2.6 - 2.0, world[2] * 2.6 - 2.0)
        lin = mix3(lin, (0.018, 0.052, 0.010), green * smooth(0.42, 0.72, gpa) * 0.55)
        lin = mix3(lin, (0.105, 0.115, 0.032), green * smooth(0.48, 0.82, gpb) * 0.42)
        lin = mix3(lin, tuple(c * 0.78 for c in lin), clamp(submerged * 0.45, 0.0, 0.55))
    elif mat == 1:
        strata = math.sin(world[1] * 4.2 + n_mid * 4.0) * 0.5 + 0.5
        lin = tuple(c * (0.92 + strata * 0.13) for c in lin)
        speck = smooth(0.74, 0.92, n_fine)
        lin = mix3(lin, (0.28, 0.27, 0.23), speck * 0.28)
        lin = tuple(c * (1.0 - moisture * 0.34) for c in lin)
    else:
        lin = tuple(c * (0.82 + 0.30 * n_micro + 0.12 * n_fine) for c in lin)
        dry = smooth(0.03, 0.14, lin[0] - lin[2])
        lin = mix3(lin, (lin[0] * 1.05, lin[1] * 0.96, lin[2] * 0.72), dry * n_mid * 0.35)

    ny = normal[1]
    amb = mix3(light.ground, light.sky, ny * 0.5 + 0.5)
    col = tuple(lin[i] * amb[i] for i in range(3))
    w = 0.38 if mat > 1 else 0.08
    nds = wrap(dot(normal, light.sun_dir), w)
    col = tuple(col[i] + lin[i] * light.sun[i] * nds for i in range(3))

    gloss = clamp(moisture * 0.75 + submerged * 0.5, 0.0, 1.0) if mat < 1.5 else 0.06
    hv = normalize((light.sun_dir[0] + light.view[0],
                    light.sun_dir[1] + light.view[1],
                    light.sun_dir[2] + light.view[2]))
    col = tuple(col[i] + light.sun[i] * (max(dot(normal, hv), 0.0) ** 72.0) * gloss * 0.16
                for i in range(3))
    if submerged > 0.01:
        col = tuple(col[i] * math.exp(-submerged * (0.22, 0.09, 0.06)[i]) for i in range(3))
    if mat > 1.5:
        back = max(dot(light.view, (-light.sun_dir[0], -light.sun_dir[1], -light.sun_dir[2])), 0.0) ** 3
        col = tuple(col[i] + lin[i] * light.sun[i] * back * 0.30 for i in range(3))
    return aces_tone(col)


def shade_water(light, depth, turb, foam_base, ndv, speed):
    """Port of the design's shadeWater(); returns (r, g, b, alpha) in finished sRGB."""
    fres = 0.02 + 0.98 * (1.0 - clamp(ndv, 0.0, 1.0)) ** 5
    ry = clamp(0.86 - 0.50 * (1.0 - clamp(ndv, 0.0, 1.0)), -0.12, 1.0)
    sky_ref = mix3(tuple(c * 0.95 for c in light.ground),
                   tuple(c * 1.22 for c in light.sky),
                   smooth(-0.08, 0.58, ry))
    sigma = mix3((0.20, 0.075, 0.050), (0.38, 0.16, 0.10), turb * 0.55)
    refr_cos = math.sqrt(max(0.05, 1.0 - (1.0 - ndv * ndv) / 1.7689))
    path = depth / max(refr_cos, 0.55)
    trans = tuple(math.exp(-sigma[i] * path) for i in range(3))
    deep = 1.0 - math.exp(-depth * 0.62)
    scatter = mix3((0.008, 0.045, 0.046), (0.0008, 0.014, 0.032), deep)
    water_light = tuple(light.sky[i] * 1.30 + light.ground[i] * 0.34
                        + light.sun[i] * max(light.sun_dir[1], 0.0) * 0.30 for i in range(3))
    body = tuple(scatter[i] * water_light[i] * 1.22 for i in range(3))
    col = tuple(body[i] * (1.0 - fres) + sky_ref[i] * fres for i in range(3))
    # specular glitter (the Sun's column) is added by the live pass, not the bake
    foam_alb = (0.52, 0.55, 0.54)
    foam_col = tuple(foam_alb[i] * (light.sky[i] * 0.60 + light.ground[i] * 0.24
                                    + light.sun[i] * wrap(light.sun_dir[1], 0.42) * 0.58)
                     for i in range(3))
    tone = aces_tone(col)
    tone_foam = aces_tone(foam_col)
    foam = smooth(0.60, 0.82, foam_base + 0.0)
    mixd = tuple(lerp(tone[i], tone_foam[i], foam * 0.92) for i in range(3))
    trans_avg = sum(trans) / 3.0
    alpha = (1.0 - trans_avg) * 0.88 + fres * 0.82
    alpha *= 0.52 + 0.48 * smooth(0.0, 0.30, depth)
    alpha = max(alpha, 0.30 + 0.55 * smooth(0.35, 1.20, depth))
    alpha = max(alpha, foam * 0.96)
    return mixd[0], mixd[1], mixd[2], clamp(alpha, 0.18, 0.97)


# ============================================================================
# The band: the projection, the mesh, and the bake. Mirrors PulvarRiver.Frame
# and PulvarRiverSprite.bake() so this preview shows what the app bakes.
# ============================================================================

import numpy as np

SWING_FRACTION = 0.0185
HALF_WIDTH_FRACTION = 0.0280
PX_PER_METRE_Y_FRACTION = 0.0036
FAR_EDGE_Q = 1.85
NEAR_EDGE_Q = 1.62

Q_BASE = [-1.85, -1.68, -1.5, -1.34, -1.21, -1.12, -1.02, -0.94, -0.8, -0.62,
          -0.42, -0.22, 0.0, 0.22, 0.42, 0.62, 0.8, 0.94, 1.02, 1.12,
          1.21, 1.34, 1.44, 1.54, 1.62]
T_BASE = [-1.0, -0.93, -0.84, -0.73, -0.60, -0.46, -0.32, -0.18, -0.06,
          0.06, 0.18, 0.32, 0.46, 0.60, 0.73, 0.84, 0.93, 1.0]


def _subdivide(nodes):
    out = []
    for i, n in enumerate(nodes):
        out.append(n)
        if i < len(nodes) - 1:
            out.append((n + nodes[i + 1]) * 0.5)
    return out


Q_NODES = _subdivide(Q_BASE)
T_NODES = _subdivide(T_BASE)
TERRAIN_COLS = 240
WATER_COLS = 320
EDGE_SAMPLES = 96


_HW_TABLE = []


def _hw_table():
    if not _HW_TABLE:
        xs = np.linspace(X0, X1, 513)
        _HW_TABLE.append(xs)
        _HW_TABLE.append(np.array([half_width(float(x)) for x in xs]))
    return _HW_TABLE


class Frame:
    def __init__(self, width, height, anchor):
        self.W, self.H = float(width), float(height)
        self.center_line = min(max(anchor, self.H * 0.905), self.H * 0.950)
        self.swing = self.H * SWING_FRACTION
        self.ppm_x = self.W / LEN
        self.ppm_z = self.H * HALF_WIDTH_FRACTION / 11.3
        self.ppm_y = self.H * PX_PER_METRE_Y_FRACTION
        zs = [center_z(X0 + LEN * i / 96.0) for i in range(97)]
        self.mean_z = sum(zs) / len(zs)
        self.peak_z = max(1.0, max(abs(z - self.mean_z) for z in zs))

    def world_x(self, cx):
        return X0 + (np.asarray(cx, dtype=np.float64) / self.W) * LEN

    def canvas_x(self, wx):
        v = (np.asarray(wx, dtype=np.float64) - X0) / LEN * self.W
        return float(v) if np.ndim(v) == 0 else v

    def center_y(self, cx):
        return self.center_y_world(self.world_x(cx))

    def center_y_world(self, wx):
        p = (np.asarray(wx, dtype=np.float64) - X0) / LEN
        z = (-3.0 + (p - 0.5) * 9.0 + 10.5 * np.sin(p * 4.6 + 0.7)
             + 5.0 * np.sin(p * 9.3 - 1.15) + 1.8 * np.sin(p * 17.7 + 2.1))
        v = self.center_line + (z - self.mean_z) / self.peak_z * self.swing
        return float(v) if np.ndim(v) == 0 else v

    def half_px(self, cx):
        xs, ys = _hw_table()
        wx = self.world_x(cx)
        v = np.interp(wx, xs, ys) * self.ppm_z
        return float(v) if np.ndim(v) == 0 else v

    def corridor_top(self):
        return self.center_line - self.swing - FAR_EDGE_Q * self.ppm_z * 11.3

    def corridor_bottom(self):
        return self.center_line + self.swing + NEAR_EDGE_Q * self.ppm_z * 11.3


# ---- adapters: the head's scalar helpers in the Kotlin port's shapes -------------

def center_dzdx(x):
    p = progress(x)
    return (9.0 + 10.5 * 4.6 * math.cos(p * 4.6 + 0.7) + 5.0 * 9.3 * math.cos(p * 9.3 - 1.15)
            + 1.8 * 17.7 * math.cos(p * 17.7 + 2.1)) / LEN


def aces(x):
    x = min(max(x * 1.02, 0.0), 60.0)
    return min(max((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0), 1.0)


_tp_head = terrain_point
_tc_head = terrain_colour
_rw_head = rock_wake


def terrain_point(x, q, out=None):
    v = _tp_head(x, q)
    if out is not None:
        out[0], out[1], out[2] = v
    return v


def terrain_colour(x, q, p, season_mix, out):
    c = _tc_head(x, q, p)
    if season_mix > 0:
        straw = tuple(lerp(c[i], PAL["GRASS_DRY"][i], (0.78, 0.72, 0.78)[i]) for i in range(3))
        c = tuple(lerp(c[i], straw[i], season_mix) for i in range(3))
    out[0], out[1], out[2] = c
    return c


def rock_wake_v(x, t, wx, wz, rocks):
    return _rw_head(x, t, wx, wz, rocks)


# ---- the Kotlin port of shadeOpaque / shadeWater, as data -------------------

class Light:
    def __init__(self, sun_az=205.0, sun_alt=52.0, moon_az=128.0, moon_alt=34.0,
                 sun_sign=1.0, moon_scale=0.0):
        d = dir_from_az_alt(sun_az, sun_alt)
        if sun_sign < 0:
            d = (-d[0], d[1], d[2])
        self.sun_dir = d
        self.sun = tuple(c for c in sun_colour_for(sun_alt))
        md = dir_from_az_alt(moon_az, moon_alt)
        self.moon_dir = md
        self.moon = tuple(c * moon_scale for c in moon_colour_for(moon_alt, 0.72))
        self.sky = (0.30, 0.42, 0.58)
        self.ground = (0.16, 0.17, 0.13)
        self.view = (0.0, 0.322, 0.947)


RIPPLE_SCALE = 0.14


def water_gradient(u, v, speed, turb):
    u, v = u * RIPPLE_SCALE, v * RIPPLE_SCALE
    gx = gz = 0.0
    ph = (u * 1.0 + v * 0.18) * 2.05
    gx += 1.0 * (0.036 * 2.05 * math.cos(ph))
    gz += 0.18 * (0.036 * 2.05 * math.cos(ph))
    ph = (u * 1.0 - v * 0.33) * 3.65
    gx += 1.0 * (0.020 * 3.65 * math.cos(ph))
    gz += -0.33 * (0.020 * 3.65 * math.cos(ph))
    ph = (u * 0.86 + v * 0.51) * 7.4
    gx += 0.86 * (0.009 * 7.4 * math.cos(ph))
    gz += 0.51 * (0.009 * 7.4 * math.cos(ph))
    ph = (u * 0.94 - v * 0.62) * 12.8
    gx += 0.94 * (0.0045 * 12.8 * math.cos(ph))
    gz += -0.62 * (0.0045 * 12.8 * math.cos(ph))
    e = 0.20
    n0 = fbm2(u * 0.55, v * 1.05, 4)
    k = 0.022 * (0.45 + turb)
    gx += (fbm2(u * 0.55 + e, v * 1.05, 4) - n0) / e * k
    gz += (fbm2(u * 0.55, v * 1.05 + e, 4) - n0) / e * k
    m0 = noise2(u * 3.1, v * 5.0)
    fine = 0.0035 / 0.14
    gx += (noise2(u * 3.1 + 0.14, v * 5.0) - m0) * fine
    gz += (noise2(u * 3.1, v * 5.0 + 0.14) - m0) * fine
    inv = 1.0 / RIPPLE_SCALE
    return gx * inv, gz * inv


def shade_water_vert(u, v, depth, foam_base, speed, turb, view_y, light):
    gx, gz = water_gradient(u, v, speed, turb)
    gs = 0.10 + 0.20 * turb
    nx, ny, nz = -gx * gs, 1.0, -gz * gs
    nl = math.sqrt(nx * nx + ny * ny + nz * nz)
    nx, ny, nz = nx / nl, ny / nl, nz / nl
    vz = math.sqrt(max(0.0, 1.0 - view_y * view_y))
    ndv = max(ny * view_y + nz * vz, 0.0)
    fres = 0.02 + 0.98 * (1.0 - ndv) ** 5
    rx, ry, rz = 2 * ndv * nx, -view_y + 2 * ndv * ny, -vz + 2 * ndv * nz
    sh = smooth(-0.08, 0.58, min(max(ry, -0.12), 1.0))
    sky = [lerp(light.ground[i] * 0.95, light.sky[i] * (1.30, 1.26, 1.18)[i], sh) for i in range(3)]
    sunr = max(rx * light.sun_dir[0] + ry * light.sun_dir[1] + rz * light.sun_dir[2], 0.0) ** 16
    moonr = max(rx * light.moon_dir[0] + ry * light.moon_dir[1] + rz * light.moon_dir[2], 0.0) ** 16
    for i in range(3):
        sky[i] += light.sun[i] * sunr * 0.16 + light.moon[i] * moonr * 0.22
    tk = turb * 0.55
    sig = (lerp(0.20, 0.38, tk), lerp(0.075, 0.16, tk), lerp(0.050, 0.10, tk))
    refr = math.sqrt(max(0.05, 1.0 - (1.0 - ndv * ndv) / 1.7689))
    path = depth / max(refr, 0.55)
    trans = [math.exp(-sig[i] * path) for i in range(3)]
    deep = 1.0 - math.exp(-depth * 0.62)
    sc = (lerp(0.008, 0.0008, deep), lerp(0.045, 0.014, deep), lerp(0.046, 0.032, deep))
    wl = [light.sky[i] * 1.30 + light.ground[i] * 0.34
          + light.sun[i] * max(light.sun_dir[1], 0.0) * 0.30
          + light.moon[i] * max(light.moon_dir[1], 0.0) * 0.30 for i in range(3)]
    body = [sc[i] * wl[i] * 1.22 for i in range(3)]
    streak = 0.98 + 0.045 * (noise2(u * 0.10, v * 1.05) * 0.62 + noise2(u * 0.38, v * 2.7) * 0.38)
    col = [(body[i] * (1 - fres) + sky[i] * fres) * streak for i in range(3)]
    hs = (light.sun_dir[0], light.sun_dir[1] + view_y, light.sun_dir[2] + vz)
    hl = math.sqrt(sum(c * c for c in hs))
    if hl > 1e-9:
        hs = tuple(c / hl for c in hs)
        ndhs = max(nx * hs[0] + ny * hs[1] + nz * hs[2], 0.0)
        spec = ndhs ** 54 * 0.08 * fres * 1.55
        for i in range(3):
            col[i] += light.sun[i] * spec
    crest = smooth(0.30, 0.86, math.sqrt(gx * gx + gz * gz) * (0.75 + turb))
    fn = fbm2(u * 0.62, v * 1.45, 4) * 0.66 + noise2(u * 1.95, v * 3.65) * 0.34
    field = foam_base * (0.72 + 0.58 * fn) + crest * foam_base * 0.22
    foam = smooth(0.60, 0.82, field + (fn - 0.5) * 0.36)
    if foam > 0.001:
        ndsw = clamp((nx * light.sun_dir[0] + ny * light.sun_dir[1] + nz * light.sun_dir[2] + 0.42) / 1.42, 0.0, 1.0)
        fcol = [(light.sky[i] * 0.60 + light.ground[i] * 0.24 + light.sun[i] * ndsw * 0.58)
                * (0.52, 0.55, 0.54)[i] for i in range(3)]
        for i in range(3):
            col[i] = lerp(col[i], fcol[i], foam * 0.92)
    ta = sum(trans) / 3.0
    alpha = (1 - ta) * 0.88 + fres * 0.82
    alpha *= 0.52 + 0.48 * smooth(0.0, 0.30, depth)
    alpha = max(alpha, 0.30 + 0.55 * smooth(0.35, 1.20, depth))
    alpha = max(alpha, foam * 0.96)
    alpha = min(max(alpha, 0.18), 0.97)
    return tone_water(col) + (alpha,)


def shade_opaque_vert(alb, normal, world, mat, moisture, submerged, light):
    if mat > 1:
        alb = tuple(lerp(alb[i], PAL["VEG_MUTE"][i], 0.38) for i in range(3))
    a = [min(max(c, 0.0), 1.0) ** 2.2 for c in alb]
    n_mid = fbm2(world[0] * 1.25 + 3.0, world[2] * 1.25 + 7.0, 4)
    n_micro = noise2(world[0] * 7.0 + world[1] * 3.0, world[2] * 7.0 + world[1] * 3.0)
    n_fine = noise2(world[0] * 23.0 - world[1] * 5.0, world[2] * 23.0 - world[1] * 5.0)
    k = 0.84 + 0.24 * n_mid + 0.10 * n_micro
    a = [min(1.6, c * k) for c in a]
    if mat == 0:
        pebble = smooth(0.68, 0.82, noise2(world[0] * 3.1 + 11.0, world[2] * 3.1 + 11.0))
        pk = pebble * 0.22 * (0.35 + moisture)
        a = [lerp(a[i], (0.16, 0.15, 0.13)[i], pk) for i in range(3)]
        green = smooth(0.015, 0.09, a[1] - max(a[0], a[2]))
        streak = fbm2(world[0] * 0.72, world[2] * 0.72, 4) * 0.55 + noise2(world[0] * 3.1, world[2] * 3.1) * 0.45
        gk = 1.0 + green * (streak - 0.5) * 0.16
        a = [c * gk for c in a]
        gpa = fbm2(world[0] * 0.33 + 4.0, world[2] * 0.33 + 4.0, 4)
        gpb = noise2(world[0] * 2.6 - 2.0, world[2] * 2.6 - 2.0)
        p1 = green * smooth(0.42, 0.72, gpa) * 0.55
        a = [lerp(a[i], (0.018, 0.052, 0.010)[i], p1) for i in range(3)]
        p2 = green * smooth(0.48, 0.82, gpb) * 0.42
        a = [lerp(a[i], (0.105, 0.115, 0.032)[i], p2) for i in range(3)]
        sk = min(max(submerged * 0.45, 0.0), 0.55)
        a = [lerp(a[i], a[i] * (0.72, 0.82, 0.78)[i], sk) for i in range(3)]
    elif mat == 1:
        strata = math.sin(world[1] * 4.2 + n_mid * 4.0) * 0.5 + 0.5
        a = [c * (0.92 + strata * 0.13) for c in a]
        sp = smooth(0.74, 0.92, n_fine) * 0.28
        a = [lerp(a[i], (0.28, 0.27, 0.23)[i], sp) for i in range(3)]
        a = [c * (1.0 - moisture * 0.34) for c in a]
    else:
        p0 = 0.82 + 0.30 * n_micro + 0.12 * n_fine
        a = [c * p0 for c in a]
        dm = smooth(0.03, 0.14, a[0] - a[2]) * n_mid * 0.35
        a = [lerp(a[i], a[i] * (1.05, 0.96, 0.72)[i], dm) for i in range(3)]
    nl = math.sqrt(sum(c * c for c in normal)) or 1.0
    N = (normal[0] / nl, normal[1] / nl, normal[2] / nl)
    w = 0.38 if mat > 1 else 0.08
    amb = [lerp(light.ground[i], light.sky[i], N[1] * 0.5 + 0.5) for i in range(3)]
    nds = clamp((sum(N[i] * light.sun_dir[i] for i in range(3)) + w) / (1 + w), 0.0, 1.0)
    ndm = clamp((sum(N[i] * light.moon_dir[i] for i in range(3)) + w) / (1 + w), 0.0, 1.0)
    col = [a[i] * amb[i] + a[i] * light.sun[i] * nds + a[i] * light.moon[i] * ndm for i in range(3)]
    gloss = clamp(moisture * 0.75 + submerged * 0.5, 0.0, 1.0) if mat < 2 else 0.06
    for d, cc, g in ((light.sun_dir, light.sun, gloss * 0.16), (light.moon_dir, light.moon, gloss * 0.20)):
        h = [d[i] + light.view[i] for i in range(3)]
        hl = math.sqrt(sum(c * c for c in h)) or 1.0
        ndh = max(sum(N[i] * h[i] / hl for i in range(3)), 0.0) ** 72 * g
        for i in range(3):
            col[i] += cc[i] * ndh
    if submerged > 0.01:
        col = [col[i] * math.exp(-submerged * (0.22, 0.09, 0.06)[i]) for i in range(3)]
    if mat == 0 and submerged > 0.01:
        c1 = noise2(world[0] * 1.05, world[2] * 1.05)
        c2 = noise2(world[0] * 1.95, world[2] * 1.95)
        caust = clamp(c1 + c2 - 0.92, 0.0, 1.0) ** 2
        fade = smooth(0.02, 0.25, submerged) * (1 - smooth(1.1, 2.8, submerged))
        for i in range(3):
            col[i] += light.sun[i] * caust * fade * 0.018 * max(N[1], 0.0)
            col[i] += light.moon[i] * caust * fade * 0.05 * max(N[1], 0.0)
    if mat > 1:
        bs = max(-sum(light.sun_dir[i] * light.view[i] for i in range(3)), 0.0) ** 3 * 0.30
        bm = max(-sum(light.moon_dir[i] * light.view[i] for i in range(3)), 0.0) ** 3 * 0.22
        for i in range(3):
            col[i] += a[i] * light.sun[i] * bs + a[i] * light.moon[i] * bm
    return tone(col)


def tone(col):
    t = [aces(c) for c in col]
    lum = 0.2126 * t[0] + 0.7152 * t[1] + 0.0722 * t[2]
    return tuple(min(max(lerp(t[i], lum, 0.10), 0.0), 1.0) ** (1 / 2.2) for i in range(3))


def tone_water(col):
    t = [aces(c) for c in col]
    lum = 0.2126 * t[0] + 0.7152 * t[1] + 0.0722 * t[2]
    return tuple(min(max(lerp(t[i], lum, 0.17), 0.0), 1.0) ** (1 / 2.2) for i in range(3))


def dir_from_az_alt(az, alt):
    a = math.radians(az)
    e = math.radians(alt)
    return (math.sin(a) * math.cos(e), math.sin(e), -math.cos(a) * math.cos(e))


def sun_colour_for(alt):
    if alt < 0:
        return (0.0, 0.0, 0.0)
    d = math.sin(math.radians(max(alt, 0.0)))
    if alt < 8:
        c = _l3((1, 0.36, 0.10), (1, 0.62, 0.28), alt / 8)
    elif alt < 25:
        c = _l3((1, 0.62, 0.28), (1, 0.88, 0.66), (alt - 8) / 17)
    else:
        c = _l3((1, 0.88, 0.66), (1, 0.96, 0.88), min(1.0, (alt - 25) / 35))
    s = 2.9 * (0.12 + 0.88 * d ** 0.65)
    return tuple(x * s for x in c)


def moon_colour_for(alt, phase):
    if alt < 0:
        return (0.0, 0.0, 0.0)
    i = 0.34 * phase * (0.25 + 0.75 * math.sin(math.radians(max(alt, 0.0))))
    return (0.62 * i, 0.72 * i, 0.95 * i)


def _l3(a, b, f):
    return tuple(lerp(a[i], b[i], f) for i in range(3))


def season_mix_for(season):
    return 0.0 if season in ("spring", "summer") else 1.0


# ---- vertex grids ----------------------------------------------------------

def terrain_grid(frame, light, season_mix):
    q = Q_NODES
    cols = TERRAIN_COLS
    grid = np.zeros(((cols + 1) * len(q), 4), dtype=np.float32)
    p = [0.0, 0.0, 0.0]
    alb = [0.0, 0.0, 0.0]
    for i in range(cols + 1):
        x = X0 + LEN * i / cols
        half = half_width(x)
        wy = water_y(x)
        for j, qq in enumerate(q):
            terrain_point(x, qq, p)
            terrain_colour(x, qq, p, season_mix, alb)
            sub = max(0.0, wy - p[1])
            moist = clamp(1.12 - max(0.0, p[1] - wy) * 1.05 - max(0.0, abs(qq) - 1.0) * 0.16, 0.0, 1.0)
            ya = terrain_point_y(x, qq - 0.04, p)
            yb = terrain_point_y(x, qq + 0.04, p)
            slope = (yb - ya) / (2 * 0.04 * half)
            rgb = shade_opaque_vert(alb, (0.0, 1.0, -slope), p, 0, moist, sub, light)
            fade_far = 1.0 - smooth(FAR_EDGE_Q - 0.18, FAR_EDGE_Q, -qq)
            fade_near = 1.0 - smooth(NEAR_EDGE_Q - 0.24, NEAR_EDGE_Q, qq)
            grid[i * len(q) + j, :3] = rgb
            grid[i * len(q) + j, 3] = min(fade_far, fade_near)
    return grid.reshape(cols + 1, len(q), 4)


def water_grid_data(frame, light, rocks):
    t = T_NODES
    cols = WATER_COLS
    grid = np.zeros(((cols + 1) * len(t), 4), dtype=np.float32)
    for i in range(cols + 1):
        x = X0 + LEN * i / cols
        reach = reach_at(x)
        half = half_width(x)
        m = center_dzdx(x)
        inv = 1.0 / math.sqrt(1.0 + m * m)
        for j, tt in enumerate(t):
            lat = tt * half
            wx = x - m * inv * lat
            wz = center_z(x) + inv * lat
            bed = bed_height(x, tt)
            depth = max(0.012, reach.water_y - bed)
            wake = rock_wake_v(x, tt, wx, wz, rocks)
            u = x * 1.015 + center_z(x) * 0.07
            edge_env = max(0.0, math.sin((tt + 1.0) * PI * 0.5)) ** 0.65
            amp = (0.010 + 0.082 * reach.foam + 0.105 * wake[1]) * edge_env
            phase = u * 2.35 - abs(lat) * 0.48 + 1.7 * (noise2(u * 0.11, lat * 0.11) - 0.5)
            crest = clamp((math.sin(phase) - 0.25) * 1.4, 0.0, 1.0)
            foam = reach.foam * (0.08 + 0.25 * crest + 0.12 * surface_speed01(x, tt))
            foam += (1.0 - smooth(0.03, 0.30, depth)) * 0.05
            foam += wake[0] * 0.34
            foam *= 0.72 + 0.28 * fbm2(u * 0.20 + 3.0, lat * 0.20, 2)
            sp = surface_speed01(x, tt)
            turb = clamp(reach.foam * 1.15 + sp * 0.35 + wake[0] * 0.20, 0.0, 1.0)
            view_y = lerp(0.07, 0.58, (tt + 1.0) * 0.5)
            r, g, b, a = shade_water_vert(u, lat, depth, clamp(foam, 0.0, 1.0), sp, turb, view_y, light)
            grid[i * len(t) + j, :3] = (r, g, b)
            grid[i * len(t) + j, 3] = a
    return grid.reshape(cols + 1, len(t), 4)


def terrain_point_y(x, q, p):
    terrain_point(x, q, p)
    return p[1]





# ---- raster: Gouraud over the mesh, exactly as drawVertices would ----------

def _sample(grid, cols, nodes, frame, rows, weld):
    """Bilinear sample of a (cols+1, nodes, 4) vertex grid at canvas rows/cols."""
    W = len(weld)
    wx = frame.world_x(weld)
    cen = frame.center_y_world(wx)
    half = np.maximum(frame.half_px(weld), 0.5)
    q = (rows[:, None] - cen[None, :]) / half[None, :]

    ci = weld / frame.W * cols
    c0 = np.clip(np.floor(ci).astype(int), 0, cols)
    c1 = np.clip(c0 + 1, 0, cols)
    cf = (ci - c0)[None, :, None]

    n = np.asarray(nodes, dtype=np.float64)
    jf = np.clip(np.interp(q, n, np.arange(len(n))), 0, len(n) - 1.001)
    j0 = np.floor(jf).astype(int)
    j1 = np.clip(j0 + 1, 0, len(n) - 1)
    jfrac = (jf - j0)[:, :, None]

    a = grid[c0[None, :], j0] * (1 - jfrac) + grid[c0[None, :], j1] * jfrac
    b = grid[c1[None, :], j0] * (1 - jfrac) + grid[c1[None, :], j1] * jfrac
    return a * (1 - cf) + b * cf


def raster_band(frame, terr, wtr, top, bottom, bg):
    W = int(round(frame.W))
    H = int(round(frame.H))
    top_i = int(math.floor(top))
    bot_i = int(min(math.ceil(bottom), H))
    rows = np.arange(top_i, bot_i) + 0.5
    weld = np.arange(W) + 0.5

    ter = _sample(terr, TERRAIN_COLS, Q_NODES, frame, rows, weld)
    can = np.zeros((len(rows), W, 4), dtype=np.float32)
    a = ter[:, :, 3:4]
    can[:, :, :3] = ter[:, :, :3] * a + bg[:3][None, None, :] * (1 - a)
    can[:, :, 3] = 1.0

    wat = _sample(wtr, WATER_COLS, T_NODES, frame, rows, weld)
    wa = wat[:, :, 3:4]
    can[:, :, :3] = wat[:, :, :3] * wa + can[:, :, :3] * (1 - wa)

    out = Image.fromarray(np.clip(can * 255.0, 0, 255).astype(np.uint8), "RGBA")
    return out, top_i


# ---- details: boulders, cobbles, reeds, grass ------------------------------

_SPHERES = {}


def icosphere(level):
    if level in _SPHERES:
        return _SPHERES[level]
    verts = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
    faces = [(0, 2, 4), (4, 2, 1), (1, 2, 5), (5, 2, 0), (4, 3, 0), (1, 3, 4), (5, 3, 1), (0, 3, 5)]
    for _ in range(level):
        cache = {}

        def mid(a, b):
            key = (a, b) if a < b else (b, a)
            if key in cache:
                return cache[key]
            v = [(verts[a][i] + verts[b][i]) * 0.5 for i in range(3)]
            l = math.sqrt(sum(c * c for c in v)) or 1.0
            verts.append(tuple(c / l for c in v))
            cache[key] = len(verts) - 1
            return cache[key]

        nxt = []
        for f in faces:
            ab, bc, ca = mid(f[0], f[1]), mid(f[1], f[2]), mid(f[2], f[0])
            nxt += [(f[0], ab, ca), (ab, f[1], bc), (ca, bc, f[2]), (ab, bc, ca)]
        faces = nxt
    _SPHERES[level] = verts
    return verts


def convex_hull(pts):
    pts = sorted(set(pts))
    if len(pts) < 3:
        return pts

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    lower = []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)
    upper = []
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)
    return lower[:-1] + upper[:-1]


def draw_rocks(img, frame, light, rocks, sun_sign, oy):
    d = ImageDraw.Draw(img, "RGBA")
    for r in rocks:
        if r.q < -FAR_EDGE_Q or r.q > NEAR_EDGE_Q:
            continue
        verts = icosphere(2 if r.radius > 0.92 else 1)
        cy = frame.center_y_world(r.x)
        center_y = cy + (r.wz - center_z(r.x)) * frame.ppm_z
        cyaw, syaw = math.cos(r.yaw), math.sin(r.yaw)
        pts = []
        for v in verts:
            d1 = fbm3(v[0] * 1.25 + r.seed, v[1] * 1.25 + r.seed * 0.7, v[2] * 1.25 - r.seed * 0.4, 3)
            facet = math.floor(d1 * 5) / 5
            d2 = noise3(v[0] * 4.6 - r.seed, v[1] * 4.6, v[2] * 4.6 + r.seed)
            rad = 0.69 + 0.38 * facet + 0.17 * d2
            lx = v[0] * rad * r.rx
            ly = v[1] * rad * r.ry
            lz = v[2] * rad * r.rz
            lx += ly * ((r.seed % 1.0) - 0.5) * 0.22
            if ly < -r.ry * 0.62:
                ly = -r.ry * 0.62
            wx = lx * cyaw - lz * syaw
            wz = lx * syaw + lz * cyaw
            pts.append((frame.canvas_x(r.x + wx),
                        center_y + wz * frame.ppm_z - (ly + r.ry) * frame.ppm_y - oy))
        hull = convex_hull(pts)
        if len(hull) < 3:
            continue
        moist = 1.0 - smooth(r.water_y - 0.04, r.water_y + 0.40, r.wy)
        sub = max(0.0, r.water_y - r.wy)
        tone = r.seed % 1.0
        base = mix3(PAL["ROCK_MID"], PAL["ROCK_LIGHT"], 0.10 + 0.80 * tone)
        base = mix3(base, PAL["ROCK_DARK"], 0.26 * (1.0 - tone))
        if r.wy + r.ry > r.water_y + 0.05:
            base = mix3(base, PAL["ROCK_MOSS"], 0.10 + 0.18 * tone)
        lit = shade_opaque_vert(base, (-0.45 * sun_sign, 0.82, 0.35), (r.wx, r.wy + r.ry * 0.6, r.wz),
                                1, moist * 0.4, sub, light)
        dark = shade_opaque_vert(tuple(c * 0.62 for c in base), (0.5 * sun_sign, 0.25, 0.3),
                                 (r.wx, r.wy - r.ry * 0.5, r.wz), 1, moist, sub, light)
        top_c, bot_c = (lit, dark) if sun_sign >= 0 else (dark, lit)
        xs = [q[0] for q in hull]
        ys = [q[1] for q in hull]
        x0, x1 = min(xs), max(xs)
        y0, y1 = min(ys), max(ys)
        w = max(1, int(round(x1 - x0)) + 1)
        h = max(1, int(round(y1 - y0)) + 1)
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).polygon([(q[0] - x0, q[1] - y0) for q in hull], fill=255)
        grad = Image.new("RGB", (1, h))
        for yy in range(h):
            t = yy / max(1, h - 1)
            grad.putpixel((0, yy), tuple(int(255 * lerp(top_c[i], bot_c[i], t)) for i in range(3)))
        img.paste(grad.resize((w, h)), (int(round(x0)), int(round(y0))), mask)


def draw_cobbles(d, frame, light, cobbles, oy):
    for c in cobbles:
        q = (c.wz - center_z(c.wx)) / max(half_width(c.wx), 0.1)
        if q < -FAR_EDGE_Q or q > NEAR_EDGE_Q:
            continue
        sub = max(0.0, c.water_y - c.wy)
        moist = 1.0 - smooth(c.water_y - 0.03, c.water_y + 0.22, c.wy)
        alb = mix3(PAL["BED_PEBBLE"], PAL["GRAVEL"], c.seed / 100.0)
        if c.seed / 100.0 > 0.78:
            alb = mix3(alb, PAL["BED_SAND"], 0.5)
        alb = mix3(alb, PAL["ROCK_DARK"], moist * 0.5)
        rgb = shade_opaque_vert(alb, (0.2, 0.94, 0.28), (c.wx, c.wy, c.wz), 1, moist, sub, light)
        cx = frame.canvas_x(c.wx)
        cy = frame.center_y_world(c.wx) + q * half_width(c.wx) * frame.ppm_z
        rx = max(0.6, c.rx * frame.ppm_x)
        ry = max(0.5, c.ry * frame.ppm_y)
        d.ellipse([cx - rx, cy - ry - oy, cx + rx, cy + ry - oy],
                  fill=tuple(int(v * 255) for v in rgb) + (255,))


def draw_blades(d, frame, light, clumps, grasses, autumn, oy):
    groups = [(clumps, True), (grasses, False)]
    for group, is_reed in groups:
        for item in group:
            blades = item.blades if is_reed else [item]
            for b in blades:
                hw = half_width(b.x)
                q = (b.z - center_z(b.x)) / max(hw, 0.1)
                if q < -FAR_EDGE_Q or q > NEAR_EDGE_Q:
                    continue
                base_col = PAL["REED_BASE"] if is_reed else PAL["GRASS_DEEP"]
                if is_reed:
                    tip_col = PAL["REED_TIP"]
                else:
                    tip_col = PAL["GRASS_DRY"] if b.tip_dry else PAL["GRASS_LUSH"]
                rgb = shade_opaque_vert(mix3(base_col, tip_col, 0.45), (0.0, 0.9, 0.3),
                                        (b.x, b.y + b.height * 0.5, b.z), 2, b.moisture, 0.0, light)
                bx = frame.canvas_x(b.x)
                by = frame.center_y_world(b.x) + q * hw * frame.ppm_z - oy
                height_px = b.height * frame.ppm_y
                width_px = max(0.55, b.width * frame.ppm_x)
                if height_px < 0.9:
                    d.point((bx, by), fill=tuple(int(v * 255) for v in rgb))
                    continue
                lean_px = b.lean * b.height * frame.ppm_x
                segs = 3
                pts = []
                for k in range(segs + 1):
                    f = k / segs
                    pts.append((bx + lean_px * f * f,
                                by - height_px * f + height_px * 0.16 * f * f * abs(b.lean)))
                col = tuple(int(v * 255) for v in rgb)
                for k in range(segs):
                    f = k / segs
                    w0 = width_px * (1 - f * 0.88) * 0.5
                    w1 = width_px * (1 - (f + 1 / segs) * 0.88) * 0.5
                    d.polygon([(pts[k][0] - w0, pts[k][1]), (pts[k][0] + w0, pts[k][1]),
                               (pts[k + 1][0] + w1, pts[k + 1][1]), (pts[k + 1][0] - w1, pts[k + 1][1])],
                              fill=col + (235,))
                if is_reed and getattr(b, "seed_head", False) and autumn < 0.62:
                    tipx, tipy = pts[-1]
                    w = max(0.7, 0.44 * 0.16 * frame.ppm_x)
                    h = max(1.2, 0.44 * frame.ppm_y)
                    seed = shade_opaque_vert(PAL["REED_SEED"], (0, 0.9, 0.3),
                                             (b.x, b.y + b.height, b.z), 2, b.moisture, 0.0, light)
                    d.ellipse([tipx - w, tipy - h, tipx + w, tipy],
                              fill=tuple(int(v * 255) for v in seed) + (235,))


# ---- composition -----------------------------------------------------------

def tint_for(day, light_level, sun_alt):
    lit = clamp(light_level, 0.0, 1.0)
    tint = [lerp(0x2C / 255.0, 1.0, lit), lerp(0x33 / 255.0, 1.0, lit), lerp(0x40 / 255.0, 1.0, lit)]
    golden = (1.0 - clamp(sun_alt, 0.0, 20.0) / 20.0) * day
    if golden > 0.01:
        tint = [lerp(tint[i], (0xFF / 255.0, 0xE3 / 255.0, 0xB4 / 255.0)[i], 0.30 * golden)
                for i in range(3)]
    return tint


def render_river_band(frame, light, autumn, bg=(0.55, 0.50, 0.42), season_mix=0.0):
    """Returns (RGBA band image, top). Mirrors PulvarRiverSprite.bake()."""
    rocks = rock_descriptors()
    cobbles, reeds, grasses = scatter_details(rocks)
    terr = terrain_grid(frame, light, season_mix)
    wtr = water_grid_data(frame, light, rocks)
    band, top = raster_band(frame, terr, wtr, frame.corridor_top() - 3,
                            frame.corridor_bottom() + 3, np.array(bg, dtype=np.float32))
    d = ImageDraw.Draw(band, "RGBA")
    draw_cobbles(d, frame, light, cobbles, top)
    draw_rocks(band, frame, light, rocks, 1.0, top)
    draw_blades(d, frame, light, reeds, grasses, 1.0 if autumn else 0.0, top)
    return band, top


def backdrop(W, H, sky_top, sky_bot, ground, horizon=0.79):
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    hy = int(H * horizon)
    for row in range(hy):
        t = row / max(1, hy - 1)
        d.line([(0, row), (W, row)], fill=tuple(int(255 * lerp(sky_top[i], sky_bot[i], t)) for i in range(3)))
    for row in range(hy, H):
        t = (row - hy) / max(1, H - hy - 1)
        d.line([(0, row), (W, row)], fill=tuple(int(255 * lerp(ground[i] * 1.18, ground[i] * 0.82, t)) for i in range(3)))
    return img


def main():
    W, H = 1080, 1260
    frame = Frame(W, H, 1011.4 + H * 0.062)
    panels = []
    states = [
        ("noon", Light(205, 52, sun_sign=1), 1.0, 1.0, 52.0, 0.0,
         (0.36, 0.47, 0.62), (0.66, 0.64, 0.55), (0.46, 0.41, 0.31)),
        ("golden hour", Light(205, 12, sun_sign=1), 0.62, 0.62, 12.0, 0.0,
         (0.30, 0.33, 0.45), (0.86, 0.62, 0.42), (0.44, 0.36, 0.25)),
        ("autumn", Light(205, 34, sun_sign=1), 0.85, 0.85, 34.0, 1.0,
         (0.34, 0.42, 0.54), (0.70, 0.62, 0.50), (0.44, 0.38, 0.27)),
        ("night (moon)", Light(205, 52, sun_sign=-1), 0.10, 0.10, -8.0, 0.0,
         (0.03, 0.05, 0.11), (0.09, 0.12, 0.20), (0.10, 0.11, 0.10)),
    ]
    for label, light, day, light_level, sun_alt, autumn, sky_t, sky_b, ground in states:
        img = backdrop(W, H, sky_t, sky_b, ground)
        band, top = render_river_band(frame, light, autumn > 0.5, bg=ground,
                                      season_mix=season_mix_for("autumn" if autumn > 0.5 else "summer"))
        tint = tint_for(day, light_level, sun_alt)
        r, g, b, a = band.split()
        r = r.point(lambda v: int(v * tint[0]))
        g = g.point(lambda v: int(v * tint[1]))
        b = b.point(lambda v: int(v * tint[2]))
        band = Image.merge("RGBA", (r, g, b, a))
        img = img.convert("RGBA")
        img.alpha_composite(band, (0, top))
        d = ImageDraw.Draw(img)
        d.text((16, 24), label, fill=(250, 250, 250))
        panels.append(img.convert("RGB").crop((0, int(H * 0.72), W, H)))

    ph = panels[0].height
    out = Image.new("RGB", (W, ph * len(panels)), (16, 16, 18))
    for i, p in enumerate(panels):
        out.paste(p, (0, i * ph))
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "pulvar-river.png")
    out.save(path)
    print("wrote", os.path.abspath(path), out.size)


if __name__ == "__main__":
    main()
