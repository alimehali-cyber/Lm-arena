#!/usr/bin/env python3
"""
Render the landscape's Paulownia from the same growth and organ rules the app uses.

This is NOT a screenshot. What it draws is the *design* that
`ui/rendering/PaulowniaTree.kt` grows and `ui/rendering/PaulowniaSprite.kt`
rasterises: the supplied `Paulownia.kt` tree (leaning trunk with root flare,
eight hand-placed scaffolds, recursive forks under an energy budget, the
flattened crown ellipsoid C=(0, 8.35, 0) / R=(5.90, 4.55, 3.35)) dressed in its
four seasonal wardrobes — spring panicles of lilac bells, summer cordate leaves,
autumn capsules and felted buds, winter wood, split capsules and leaf scars.

The python below mirrors the Kotlin function by function (same 32-bit LCG, same
consumption order, same crown clamp, same organ placement), so the panel you see
is the sprite the app bakes — one tree, four seasons. It exists so the tree can
be eyeballed and tuned without a device; the numbers were chosen against it.

Requires Pillow. Run from the repo root:
    python3 tools/paulownia/preview_paulownia.py
Writes docs/paulownia-tree.png.
"""

import math
import os

from PIL import Image, ImageDraw

# ---- constants shared with PaulowniaTree.kt ---------------------------------

HEIGHT_M = 13.8
SEED = 0x2E5A11
MAX_JOBS = 600
FILL_TWIGS = 130
MAX_LEAVES = 900

CROWN_CY = 8.35
CROWN_RX = 5.90
CROWN_RY = 4.55
CROWN_RZ = 3.35

TWO_PI = 6.2831855
HALF_PI = 1.5707964

SPRING, SUMMER, AUTUMN, WINTER = 0, 1, 2, 3
SEASON_NAMES = ["spring", "summer", "autumn", "winter"]

# The design's palette, at the landscape's daylight (PaulowniaSprite.kt).
BARK_GROOVE = (0x24, 0x1A, 0x12)
BARK = (0x4A, 0x38, 0x28)
BARK_RIDGE = (0x6B, 0x51, 0x36)
TWIG = (0x51, 0x3E, 0x2A)
LEAF_DEEP = (0x1B, 0x2A, 0x12)
LEAF_DARK = (0x25, 0x39, 0x1B)
LEAF_FRESH = (0x4B, 0x6B, 0x2A)
LEAF_BACK = (0x7C, 0x8A, 0x61)
VEIN = (0x6E, 0x73, 0x40)
FLOWER_TUBE = (0x5B, 0x46, 0x91)
FLOWER_RIM = (0xB7, 0x9C, 0xDB)
THROAT = (0xE6, 0xD6, 0x9B)
CALYX = (0x8A, 0x5A, 0x2E)
CAPSULE = (0x62, 0x76, 0x3A)
CAPSULE_RIPE = (0xA3, 0x83, 0x4C)
CAPSULE_OLD = (0x59, 0x42, 0x2F)
LEAF_BROWN = (0x6E, 0x4B, 0x28)
MASS_AUTUMN = (0x33, 0x27, 0x1A)  # the crown's interior shade in autumn
LEAF_OLIVE = (0x7D, 0x64, 0x34)
SCAR = (0xA4, 0x93, 0x7A)
PANICLE_STEM = (0x7C, 0x5C, 0x3A)


def hash01(seed, index):
    x = (seed * 1103515245 + index * 97531 + 12345) & 0xFFFFFFFF
    x ^= x >> 13
    x = (x * 0x5BD1E995) & 0xFFFFFFFF
    x ^= x >> 15
    return (x & 0x7FFFFFFF) / 2147483648.0


def cluster_polygon(r, squish, seed, index, lobes=7):
    # LandscapeProfile.clusterPolygon, mirrored exactly.
    n = max(5, min(9, lobes))
    phase = hash01(seed, index * 13 + 101) * 2 * math.pi
    lobe_freq = 2 + int(hash01(seed, index * 17 + 103) * 3)
    pts = []
    for k in range(n):
        jitter = (hash01(seed + k * 29, index * 19 + k * 7) - 0.5) * 0.30
        ang = phase + k / n * 2 * math.pi + jitter * 0.45
        lobe = 0.5 + 0.5 * math.sin(ang * lobe_freq + phase * 2.1)
        factor = 0.66 + 0.38 * lobe + (hash01(seed + k * 37, index * 23 + k * 11) - 0.5) * 0.36
        factor = max(0.62, min(1.20, factor))
        pts.append((math.cos(ang) * r * factor, math.sin(ang) * r * factor * squish))
    return pts


def shift(color, factor):
    f = max(0.0, min(1.60, factor))
    return tuple(max(0, min(255, int(c * f))) for c in color)


def blend(a, b, t):
    f = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * f) for i in range(3))


class Lcg:
    """The design's 32-bit LCG: same constants, same wrap, same order."""

    def __init__(self, seed):
        self.s = seed & 0xFFFFFFFF

    def next(self):
        self.s = (self.s * 1664525 + 1013904223) & 0xFFFFFFFF
        return ((self.s >> 8) & 0xFFFFFF) / 16777216.0

    def range(self, a, b):
        return a + (b - a) * self.next()


def nrm(x, y, z):
    l = math.sqrt(x * x + y * y + z * z)
    if l < 1e-6:
        return (0.0, 1.0, 0.0)
    return (x / l, y / l, z / l)


def tilt(d, phi, angle):
    rx, ry = (0.0, 1.0) if abs(d[1]) < 0.9 else (1.0, 0.0)
    ux, uy, uz = d[1] * 0 - d[2] * ry, d[2] * rx - d[0] * 0, d[0] * ry - d[1] * rx
    ul = math.sqrt(ux * ux + uy * uy + uz * uz)
    if ul > 1e-6:
        ux, uy, uz = ux / ul, uy / ul, uz / ul
    vx = d[1] * uz - d[2] * uy
    vy = d[2] * ux - d[0] * uz
    vz = d[0] * uy - d[1] * ux
    c, s = math.cos(phi), math.sin(phi)
    tx, ty, tz = ux * c + vx * s, uy * c + vy * s, uz * c + vz * s
    ca, sa = math.cos(angle), math.sin(angle)
    return nrm(d[0] * ca + tx * sa, d[1] * ca + ty * sa, d[2] * ca + tz * sa)


def crown_distance(x, y, z):
    nx = x / CROWN_RX
    ny = (y - CROWN_CY) / CROWN_RY
    nz = z / CROWN_RZ
    return math.sqrt(nx * nx + ny * ny + nz * nz)


def exposure(x, y, z):
    return max(0.0, min(1.15, 0.10 + 0.72 * crown_distance(x, y, z)
                     + 0.13 * max(-1.0, min(1.0, z / CROWN_RZ))
                     + 0.08 * max(0.0, min(1.0, (y - 4.0) / 9.0))))


def front(x, y, z):
    return max(0.0, min(1.0, 0.5 + z / (2 * CROWN_RZ)))


class Limb:
    def __init__(self, pts, rads, order):
        self.pts, self.rads, self.order = pts, rads, order


class Node:
    def __init__(self, x, y, z, order, r=0.01):
        self.x, self.y, self.z, self.order, self.r = x, y, z, order, r
        self.exposure = exposure(x, y, z)
        self.front = front(x, y, z)


def grow(seed=SEED):
    rnd = Lcg(seed)
    limbs, tips, nodes, jobs = [], [], [], []

    limbs.append(Limb([
        (0.0, 0.0, 0.0), (-0.03, 0.38, 0.01), (0.02, 0.95, -0.02),
        (0.08, 1.65, 0.02), (0.03, 2.25, -0.03), (0.0, 2.82, 0.0), (0.01, 3.35, 0.0),
    ], [0.58, 0.39, 0.315, 0.30, 0.270, 0.205, 0.145], 0))

    for i in range(6):
        a = i / 6 * TWO_PI + rnd.range(-0.16, 0.16)
        cx, cz = math.cos(a), math.sin(a)
        reach = rnd.range(0.55, 0.82)
        limbs.append(Limb([
            (cx * 0.18, 0.30, cz * 0.18),
            (cx * reach * 0.58, 0.085, cz * reach * 0.58),
            (cx * reach, 0.002, cz * reach),
        ], [0.165, 0.110, 0.045], 1))

    scaffolds = [
        ((0.0, 2.80, 0.0), (0.05, 0.96, 0.02), 1.95, 15.5, 0.140, 0),
        ((-0.05, 2.35, 0.03), (-0.84, 0.28, 0.08), 1.70, 12.0, 0.115, 1),
        ((0.05, 2.75, -0.02), (0.87, 0.24, -0.03), 1.80, 12.4, 0.120, 2),
        ((-0.02, 3.95, -0.03), (-0.55, 0.70, -0.16), 1.70, 13.0, 0.090, 3),
        ((0.03, 4.25, 0.02), (0.58, 0.68, 0.13), 1.75, 13.2, 0.088, 4),
        ((0.0, 3.65, -0.06), (-0.10, 0.76, -0.50), 1.60, 12.2, 0.082, 5),
        ((-0.04, 3.05, 0.08), (-0.65, 0.34, 0.39), 1.55, 11.8, 0.095, 6),
        ((0.04, 3.35, 0.07), (0.67, 0.31, 0.36), 1.60, 12.0, 0.092, 7),
    ]
    for (o, d, ln, en, r, root) in scaffolds:
        jobs.append(dict(o=o, d=d, p=d, ln=ln, en=en, r=r, order=1, root=root))

    ji = 0
    branch_count = 0
    while ji < len(jobs) and branch_count < MAX_JOBS:
        job = jobs[ji]
        ji += 1
        branch_count += 1
        steps = max(3, min(8, int(math.floor(job["ln"] / (0.48 if job["order"] < 3 else 0.30) + 0.5))))
        step_len = job["ln"] / steps
        curve_phi = rnd.range(0, TWO_PI)
        curve_amt = rnd.range(0.045, 0.125)
        curve_dir = tilt(job["d"], curve_phi, HALF_PI)

        px, py, pz = job["o"]
        d = job["d"]
        pts = [(px, py, pz)]
        rads = [job["r"] * (1.22 if job["order"] > 1 else 1.06)]

        energy_left = job["en"] - job["ln"]
        terminal = energy_left <= 0.62 or job["r"] <= 0.007 or job["order"] >= 8
        if terminal:
            taper = 0.72
        elif job["order"] == 1:
            taper = 0.16
        elif job["order"] == 2:
            taper = 0.20
        elif job["order"] == 3:
            taper = 0.26
        elif job["order"] == 4:
            taper = 0.34
        else:
            taper = 0.45
        laterals = 0
        if job["order"] <= 2:
            max_laterals = 4
        elif job["order"] == 3:
            max_laterals = 3
        elif job["order"] <= 5:
            max_laterals = 2
        elif job["order"] == 6:
            max_laterals = 1
        else:
            max_laterals = 0

        for s in range(1, steps + 1):
            f = s / steps
            sway = curve_amt * (0.55 + 0.45 * math.sin(f * 4.2 + job["root"]))
            wx = d[0] * 0.76 + job["p"][0] * 0.24 + curve_dir[0] * sway
            wy = d[1] * 0.76 + job["p"][1] * 0.24 + curve_dir[1] * sway
            wz = d[2] * 0.76 + job["p"][2] * 0.24 + curve_dir[2] * sway
            if job["order"] >= 5:
                wy += 0.065
            if f > 0.76 and job["order"] >= 4:
                wy += 0.11
            if py < 4.8 and job["order"] <= 2:
                ox, _, oz = nrm(px, 0, pz)
                wx += ox * 0.07
                wz += oz * 0.07
            j = tilt(d, rnd.range(0, TWO_PI), rnd.range(0.015, 0.070))
            d = nrm(wx + j[0], wy + j[1], wz + j[2])

            nx2, ny2, nz2 = px + d[0] * step_len, py + d[1] * step_len, pz + d[2] * step_len
            cd = crown_distance(nx2, ny2, nz2)
            if cd > 0.93 and job["order"] > 1:
                g = nrm(nx2 / (CROWN_RX * CROWN_RX), (ny2 - CROWN_CY) / (CROWN_RY * CROWN_RY),
                        nz2 / (CROWN_RZ * CROWN_RZ))
                radial = d[0] * g[0] + d[1] * g[1] + d[2] * g[2]
                if radial > 0:
                    bend = 1.12 if cd > 1.05 else 0.72
                    d = nrm(d[0] - g[0] * radial * bend,
                            d[1] - g[1] * radial * bend + 0.025,
                            d[2] - g[2] * radial * bend)
                    shrink = 0.76 if cd > 1.05 else 0.92
                    nx2 = px + d[0] * step_len * shrink
                    ny2 = py + d[1] * step_len * shrink
                    nz2 = pz + d[2] * step_len * shrink
            if ny2 < 1.20:
                ny2 = 1.20 + 0.10 * f
            px, py, pz = nx2, ny2, nz2
            radius_at = max(0.0038, job["r"] * (1 - taper * f ** 0.90))
            pts.append((px, py, pz))
            rads.append(radius_at)
            if job["order"] >= 2:
                nodes.append(Node(px, py, pz, job["order"], radius_at))

            if 2 <= s < steps and laterals < max_laterals and job["order"] < 8 and len(jobs) < MAX_JOBS + 40:
                chance = {1: 0.45, 2: 0.42, 3: 0.36}.get(job["order"], 0.28)
                energy_at = job["en"] - job["ln"] * f
                if energy_at > 0.75 and rnd.next() < chance:
                    phi = rnd.range(0, TWO_PI)
                    pair = job["order"] >= 3 and rnd.next() < 0.24
                    kids = 2 if pair else 1
                    k = 0
                    while k < kids and laterals < max_laterals:
                        cd2 = tilt(d, phi + k * 3.1415927, rnd.range(0.55, 1.00))
                        outward = nrm(px * 0.8, 0.20, pz * 0.8)
                        cd2 = nrm(cd2[0] + outward[0] * 0.11,
                                  cd2[1] + outward[1] * 0.11 + 0.075,
                                  cd2[2] + outward[2] * 0.11)
                        ce = energy_at * rnd.range(0.30, 0.49)
                        cl = min(ce, 0.50 + ce * rnd.range(0.14, 0.22))
                        cr = max(0.0062, radius_at * rnd.range(0.58, 0.75))
                        jobs.append(dict(
                            o=(px - d[0] * step_len * 0.05, py - d[1] * step_len * 0.05, pz - d[2] * step_len * 0.05),
                            d=cd2, p=cd2, ln=cl, en=ce, r=cr, order=job["order"] + 1, root=job["root"]))
                        laterals += 1
                        k += 1

        end_r = rads[-1]
        continued = False
        if (not terminal) and energy_left > 0.62 and end_r > 0.0058 and job["order"] < 8 and len(jobs) < MAX_JOBS + 40:
            kids = 2 if job["order"] <= 2 else (2 if rnd.next() < 0.88 else 1)
            phi = rnd.range(0, TWO_PI)
            for k in range(kids):
                if job["order"] <= 2:
                    t = rnd.range(0.20, 0.48)
                else:
                    t = rnd.range(0.27, 0.62)
                phi_k = phi + k * 3.1415927 + rnd.range(-0.24, 0.24)
                cd2 = tilt(d, phi_k, t)
                outward = nrm(px * 0.65, 0.12, pz * 0.65)
                up = 0.115 if job["order"] >= 4 else 0.055
                cd2 = nrm(cd2[0] + outward[0] * 0.055,
                          cd2[1] + outward[1] * 0.055 + up,
                          cd2[2] + outward[2] * 0.055)
                if kids == 1:
                    ce = energy_left * rnd.range(0.95, 1.00)
                elif k == 0:
                    ce = energy_left * rnd.range(0.93, 0.99)
                else:
                    ce = energy_left * rnd.range(0.55, 0.75)
                cl = min(ce, 0.65 + ce * rnd.range(0.12, 0.20))
                cr = max(0.0052, end_r * (rnd.range(0.82, 0.94) if k == 0 else rnd.range(0.62, 0.80)))
                jobs.append(dict(
                    o=(px - d[0] * step_len * 0.04, py - d[1] * step_len * 0.04, pz - d[2] * step_len * 0.04),
                    d=cd2, p=cd2, ln=cl, en=ce, r=cr, order=job["order"] + 1, root=job["root"]))
                continued = True

        limbs.append(Limb(pts, rads, job["order"]))
        if not continued:
            tips.append(Node(px, py, pz, job["order"], end_r))

    pool = list(tips) + [n for n in nodes if n.order >= 3]
    if pool:
        for i in range(FILL_TWIGS):
            q = rnd.next()
            factor = rnd.range(0.35, 0.65) if q < 0.25 else (rnd.range(0.65, 0.92) if q < 0.80 else rnd.range(0.92, 1.05))
            zz = rnd.range(0.10, 1.0) if i % 3 == 0 else rnd.range(-1.0, 1.0)
            phi = rnd.range(0, TWO_PI)
            horiz = math.sqrt(max(0.0, 1 - zz * zz))
            tx = math.cos(phi) * horiz * CROWN_RX * factor
            ty = CROWN_CY + zz * CROWN_RY * factor
            tz = math.sin(phi) * horiz * CROWN_RZ * factor
            bends = rnd.range(0, TWO_PI)
            bend_amt = rnd.range(-0.12, 0.12)
            r0 = rnd.range(0.50, 0.70)
            jitter = rnd.range(0.68, 0.86)
            if ty < 3.05:
                continue
            best, best_d2 = None, 1e9
            for cand in pool:
                d2 = (tx - cand.x) ** 2 + (ty - cand.y) ** 2 + (tz - cand.z) ** 2
                if d2 < best_d2:
                    best_d2, best = d2, cand
            if best is None:
                continue
            dist = math.sqrt(best_d2)
            if dist < 0.22 or dist > 1.25:
                continue
            d = nrm(tx - best.x, ty - best.y, tz - best.z)
            g = nrm(best.x / (CROWN_RX ** 2), (best.y - CROWN_CY) / (CROWN_RY ** 2), best.z / (CROWN_RZ ** 2))
            aim = nrm(d[0] * 0.62 + g[0] * 0.16, d[1] * 0.62 + g[1] * 0.16 + 0.16, d[2] * 0.62 + g[2] * 0.16)
            ln = dist * jitter
            tangent = tilt(aim, bends, HALF_PI)
            twig_r = max(0.008, min(0.032, max(best.r, 0.012) * r0))
            twig_pts = [
                (best.x, best.y, best.z),
                (best.x + aim[0] * ln * 0.38 + tangent[0] * bend_amt * ln,
                 best.y + aim[1] * ln * 0.38 + tangent[1] * bend_amt * ln,
                 best.z + aim[2] * ln * 0.38 + tangent[2] * bend_amt * ln),
                (best.x + aim[0] * ln, best.y + aim[1] * ln + ln * 0.075, best.z + aim[2] * ln),
            ]
            limbs.append(Limb(twig_pts, [twig_r * 1.15, twig_r * 0.62, twig_r * 0.30], 6))
            tips.append(Node(twig_pts[2][0], twig_pts[2][1], twig_pts[2][2], 6, twig_r * 0.30))

    return limbs, tips, nodes


def place_organs(season, skel, seed=SEED):
    limbs, tips, nodes = skel
    rnd = Lcg(seed * 7919 + season * 131 + 17)
    out = []

    def seed_int():
        return int(rnd.next() * 1_000_000)

    def axis_to(x, y, z):
        sx, sy = x, -y
        l = math.sqrt(sx * sx + sy * sy)
        if l < 1e-4 or l < 0.22:
            return (0.0, -1.0)
        return (sx / l, sy / l)

    def add(kind, node, length, axis):
        out.append(dict(kind=kind, x=node.x, y=node.y, z=node.z, length=length,
                        ax=axis[0], ay=axis[1], exposure=node.exposure, front=node.front,
                        seed=seed_int()))

    def leaf(node, scale, kind):
        axis = axis_to(node.x * 0.25 + rnd.range(-0.55, 0.55),
                       rnd.range(0.55, 1.15),
                       node.z * 0.25 + rnd.range(-0.55, 0.55))
        add(kind, node, (0.26 + 0.16 * rnd.next()) * scale, axis)

    def panicle(node, scale):
        axis = axis_to(node.x * 0.35 + rnd.range(-0.3, 0.3),
                       rnd.range(0.55, 1.0),
                       node.z * 0.30 + rnd.range(-0.3, 0.3))
        add("panicle", node, (0.26 + 0.20 * rnd.next() * (0.79 + 0.34 * node.exposure)) * scale, axis)

    def capsule(node, kind):
        axis = axis_to(node.x * 0.6 + rnd.range(-0.2, 0.2),
                       rnd.range(0.25, 0.85),
                       node.z * 0.6 + rnd.range(-0.2, 0.2))
        add(kind, node, 0.036 + 0.022 * rnd.next(), axis)

    def buds(node):
        axis = axis_to(node.x * 0.5 + rnd.range(-0.25, 0.25),
                       rnd.range(0.6, 1.0),
                       node.z * 0.5 + rnd.range(-0.25, 0.25))
        add("buds", node, 0.16 + 0.09 * rnd.next(), axis)

    if season == SPRING:
        for tip in tips:
            if rnd.next() < 0.74:
                panicle(tip, 1.15)
        for node in nodes:
            if node.order >= 4 and rnd.next() < 0.46:
                panicle(node, 0.82)
            if node.order >= 6 and rnd.next() < 0.06:
                leaf(node, 0.62, "leaf")
        for _ in range(16):
            x, z = rnd.range(-5.6, 5.6), rnd.range(-3.2, 3.2)
            out.append(dict(kind="corolla", x=x, y=0.02, z=z, length=0.055, ax=0.0, ay=-1.0,
                            exposure=0.9, front=front(x, 0, z), seed=seed_int()))
    elif season == SUMMER:
        leaves = 0
        for tip in tips:
            leaf(tip, 1.62, "leaf")
            leaf(tip, 1.46, "leaf_back" if rnd.next() < 0.14 else "leaf")
            leaves += 2
            if rnd.next() < 0.16:
                capsule(tip, "capsule")
            if rnd.next() < 0.10:
                buds(tip)
        for node in nodes:
            if leaves >= MAX_LEAVES:
                break
            if node.order < 4:
                continue
            if rnd.next() < 0.62:
                leaf(node, 1.50, "leaf_back" if rnd.next() < 0.14 else "leaf")
                leaves += 1
    elif season == AUTUMN:
        leaves = 0
        for tip in tips:
            if rnd.next() < 0.42:
                leaf(tip, 1.42, "autumn")
                leaves += 1
            roll = rnd.next()
            if roll < 0.46:
                capsule(tip, "capsule_open")
            elif roll < 0.60:
                buds(tip)
        for node in nodes:
            if leaves >= MAX_LEAVES // 2:
                break
            if node.order < 5:
                continue
            if rnd.next() < 0.30:
                leaf(node, 1.30, "autumn")
                leaves += 1
            if rnd.next() < 0.22:
                capsule(node, "capsule_open")
    else:
        for tip in tips:
            roll = rnd.next()
            if roll < 0.40:
                capsule(tip, "capsule_open")
            elif roll < 0.72:
                buds(tip)
        for node in nodes:
            if node.order < 5:
                continue
            if rnd.next() < 0.30:
                buds(node)
            if rnd.next() < 0.22:
                add("scar", node, 0.022, (0.0, -1.0))
    return out


# ---- raster (mirrors PaulowniaSprite.bake) ----------------------------------


def bezier(p0, p1, p2, p3, steps=10):
    out = []
    for i in range(steps + 1):
        t = i / steps
        mt = 1 - t
        out.append((mt ** 3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t ** 3 * p3[0],
                    mt ** 3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t ** 3 * p3[1]))
    return out


def leaf_outline(length):
    w = length * 0.36
    pts = []
    pts += bezier((0, -length * 0.04), (-w * 0.30, 0), (-w * 0.78, -length * 0.02), (-w * 1.00, -length * 0.26), 6)
    pts += bezier((-w * 1.00, -length * 0.26), (-w * 1.20, -length * 0.54), (-w * 0.72, -length * 0.82), (0, -length), 6)
    pts += bezier((0, -length), (w * 0.72, -length * 0.82), (w * 1.20, -length * 0.54), (w * 1.00, -length * 0.26), 6)
    pts += bezier((w * 1.00, -length * 0.26), (w * 0.78, -length * 0.02), (w * 0.30, 0), (0, -length * 0.04), 6)
    return pts


def rotate(pts, deg):
    a = math.radians(deg)
    ca, sa = math.cos(a), math.sin(a)
    return [(x * ca - y * sa, x * sa + y * ca) for x, y in pts]


def ribbon(points, rads):
    n = len(rads)
    nx, ny = [0.0] * n, [0.0] * n
    for i in range(n):
        a, b = max(0, i - 1), min(n - 1, i + 1)
        dx, dy = points[b][0] - points[a][0], points[b][1] - points[a][1]
        l = math.hypot(dx, dy)
        if l < 1e-3:
            dx, dy = 0.0, 1.0
        else:
            dx, dy = dx / l, dy / l
        nx[i], ny[i] = -dy, dx
    left = [(points[i][0] + nx[i] * rads[i], points[i][1] + ny[i] * rads[i]) for i in range(n)]
    right = [(points[i][0] - nx[i] * rads[i], points[i][1] - ny[i] * rads[i]) for i in reversed(range(n))]
    return left + right


def oval(draw, cx, cy, rx, ry, color):
    draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=color)


class Sprite:
    """One baked render plus the frame maths `PaulowniaSprite.draw` needs."""

    def __init__(self, img, span_fraction, base_x_fraction, base_y_fraction):
        self.img = img
        self.span_fraction = span_fraction        # sprite height / the design's 13.8 m
        self.base_x_fraction = base_x_fraction    # trunk foot, fraction of the width
        self.base_y_fraction = base_y_fraction    # ground line, fraction of the height

    def at(self, tree_height_px, flip=False, tint=(255, 255, 255), alpha=1.0):
        """Project the one render at one planting (mirror + multiply, no re-render)."""
        h = max(2, int(round(tree_height_px * self.span_fraction)))
        w = max(1, int(round(h * self.img.width / self.img.height)))
        img = self.img.resize((w, h), Image.LANCZOS)
        if flip:
            img = img.transpose(Image.FLIP_LEFT_RIGHT)
        if tint != (255, 255, 255) or alpha < 0.999:
            r, g, b = tint
            a = img.getchannel("A")
            if alpha < 0.999:
                a = a.point(lambda v: int(v * alpha))
            img = Image.merge("RGBA", [
                img.getchannel(0).point(lambda v: v * r // 255),
                img.getchannel(1).point(lambda v: v * g // 255),
                img.getchannel(2).point(lambda v: v * b // 255),
                a,
            ])
        return img


def render(season, span_px=520, sky=(0xC9, 0xD3, 0xDC)):
    """Rasterise one season exactly as PaulowniaSprite.bake does.

    `sky=None` returns RGBA with a transparent background — the sprite the landscape blits.
    """
    ss = 4  # supersample, the way the real bake is downscaled on screen
    limbs, tips, nodes = grow()
    organs = place_organs(season, (limbs, tips, nodes))

    xs, ys = [], []
    for limb in limbs:
        for (x, y, _), r in zip(limb.pts, limb.rads):
            xs += [x - r, x + r]
            ys += [y - r, y + r]
    for o in organs:
        # The organ's own extent, exactly as PaulowniaSprite.bake includes it: the anchor, the tip
        # along the axis, and the two perpendicular extremes at 45 % of the length.
        ex, ey = o["ax"] * o["length"], o["ay"] * o["length"]
        px, py = -o["ay"] * o["length"] * 0.45, o["ax"] * o["length"] * 0.45
        xs += [o["x"], o["x"] + ex, o["x"] + px, o["x"] - px]
        ys += [o["y"], o["y"] + ey, o["y"] + py, o["y"] - py]
    min_x, max_x = min(xs) - 0.10, max(xs) + 0.10
    min_y, max_y = min(ys) - 0.05, max(ys) + 0.10
    span_m = max(0.5, max_y - min_y)
    span_x = max(0.5, max_x - min_x)
    k = span_px * ss / span_m
    width_px = int(math.ceil(span_x * k))

    img = Image.new("RGBA" if sky is None else "RGB", (width_px, span_px * ss),
                    (0, 0, 0, 0) if sky is None else sky)
    draw = ImageDraw.Draw(img, "RGBA")

    def sx(v):
        return (v - min_x) * k

    def sy(v):
        return (max_y - v) * k

    prims = []
    if season in (SUMMER, AUTUMN):
        mass_alpha = 214 if season == SUMMER else 196
        mass_color = LEAF_DEEP if season == SUMMER else MASS_AUTUMN
        every = 3 if season == SUMMER else 2
        n_leaf = 0
        for o in organs:
            if o["kind"] not in ("leaf", "leaf_back", "autumn"):
                continue
            n = n_leaf
            n_leaf += 1
            if n % every:
                continue
            rad = (o["length"] * 0.62 + 0.05) * k
            cx, cy = sx(o["x"]), sy(o["y"]) + rad * 0.10
            pts = cluster_polygon(rad, 0.86, 4409 + n, n, 9)
            prims.append(
                (o["z"] - 0.55, "mass", [(cx + x, cy + y) for x, y in pts], mass_color, mass_alpha)
            )

    for limb in limbs:
        n = len(limb.rads)
        pts = [(sx(limb.pts[i][0]), sy(limb.pts[i][1])) for i in range(n)]
        rads = [limb.rads[i] * k for i in range(n)]
        mid = n // 2
        z = limb.pts[mid][2]
        exp = 0.55 + 0.55 * min(1.15, max(0.0, exposure(*limb.pts[mid])))
        color = BARK if limb.order <= 1 else TWIG
        poly = ribbon(pts, rads)
        prims.append((z, "path", poly, shift(color, exp), None))
        if limb.order <= 1 and rads[0] > 4 * ss:
            for f in (-0.45, -0.05, 0.48):
                col = shift(BARK_GROOVE, exp * 0.9) if f < 0 else shift(BARK_RIDGE, exp)
                prims.append((z + 0.01, "stroke", pts, col, (f, rads)))

    for o in organs:
        ox, oy = sx(o["x"]), sy(o["y"])
        length = max(1.2, o["length"] * k)
        deg = math.degrees(math.atan2(o["ax"], -o["ay"]))
        expo = min(1.15, o["exposure"] * 1.15)
        front_f = 0.80 + 0.30 * o["front"]
        z = o["z"] + o["length"] * 0.05
        kind = o["kind"]
        if kind in ("leaf", "autumn", "leaf_back"):
            if kind == "leaf_back":
                colour = blend(LEAF_BACK, LEAF_FRESH, 0.15 * expo)
            elif kind == "autumn":
                colour = blend(LEAF_BROWN, LEAF_OLIVE, 0.35 + 0.45 * expo)
            else:
                colour = blend(LEAF_DARK, LEAF_FRESH, expo)
            prims.append((z, "leaf", (ox, oy, deg, length), shift(colour, front_f * (0.86 + 0.22 * expo)), None))
        elif kind == "panicle":
            prims.append((z, "panicle", (ox, oy, deg, length), None, o))
        elif kind in ("capsule", "capsule_open"):
            open_ = kind == "capsule_open"
            colour = CAPSULE_OLD if open_ else blend(CAPSULE, CAPSULE_RIPE, 0.55)
            prims.append((z, "capsule", (ox, oy, deg, length), shift(colour, front_f * (0.80 + 0.30 * expo)), (open_, o)))
        elif kind == "buds":
            prims.append((z, "buds", (ox, oy, deg, length), None, o))
        elif kind == "scar":
            prims.append((z, "scar", (ox, oy, length), SCAR + (170,), None))
        elif kind == "corolla":
            prims.append((z - 5, "corolla", (ox, oy, deg, length), FLOWER_RIM + (200,), None))

    prims.sort(key=lambda p: p[0])
    for (_z, kind, geom, colour, extra) in prims:
        if kind == "mass":
            # The crown under-mass: one lobed cluster under every Nth leaf (PaulowniaSprite.bake).
            draw.polygon(geom, fill=colour + (extra,))
        elif kind == "path":
            draw.polygon(geom, fill=colour + (255,))
        elif kind == "stroke":
            pts, col, (f, rads) = geom, colour, extra
            n = len(pts)
            for i in range(n):
                a, b = max(0, i - 1), min(n - 1, i + 1)
                dx, dy = pts[b][0] - pts[a][0], pts[b][1] - pts[a][1]
                l = math.hypot(dx, dy) or 1.0
                x = pts[i][0] - (dy / l) * rads[i] * f
                y = pts[i][1] + (dx / l) * rads[i] * f
                r = max(0.7, rads[0] * 0.13)
                oval(draw, x, y, r, r, col + (255,))
        elif kind == "leaf":
            ox, oy, deg, length = geom
            poly = [(ox + x, oy + y) for x, y in rotate(leaf_outline(length), deg)]
            draw.polygon(poly, fill=colour + (255,))
        elif kind == "panicle":
            o = extra
            ox, oy, deg, length = geom
            ax, ay = o["ax"], o["ay"]
            pxn, pyn = -ay, ax
            stem = shift(PANICLE_STEM, 0.80 + 0.30 * o["front"])
            tube = shift(FLOWER_TUBE, (0.80 + 0.30 * o["front"]) * (0.80 + 0.28 * expo))
            rim = shift(FLOWER_RIM, (0.80 + 0.30 * o["front"]) * (0.86 + 0.24 * expo))
            draw.line([(ox, oy), (ox + ax * length, oy + ay * length)], fill=stem + (255,),
                      width=max(1, int(length * 0.03)))
            bells = 5 + (o["seed"] % 3)
            for b in range(bells):
                t = 0.26 + 0.74 * (b + 0.5) / bells
                wob = (((b * 37 + o["seed"]) % 100) / 100 - 0.5) * length * 0.30
                bx, by = ox + ax * length * t + pxn * wob, oy + ay * length * t + pyn * wob
                bl = length * (0.32 - 0.10 * t)
                c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
                oval(draw, bx, by - bl * 0.5, max(0.4, bl * 0.20), bl * 0.5, tube + (255,))
                oval(draw, bx + s * bl * 0.0, by - bl * 0.92, max(0.4, bl * 0.15), max(0.4, bl * 0.15), rim + (255,))
        elif kind == "capsule":
            ox, oy, deg, length = geom
            open_ = extra[0]
            poly = [(ox + x, oy + y) for x, y in rotate(
                [(math.cos(a) * length * 0.26, -length * 0.5 + math.sin(a) * length * 0.5)
                 for a in [i / 16 * 2 * math.pi for i in range(17)]], deg)]
            draw.polygon(poly, fill=colour + (255,))
        elif kind == "buds":
            o = extra
            ox, oy, deg, length = geom
            ax, ay = o["ax"], o["ay"]
            pxn, pyn = -ay, ax
            bud = shift(CALYX, (0.80 + 0.30 * o["front"]) * (0.82 + 0.26 * expo))
            stem = shift(PANICLE_STEM, (0.80 + 0.30 * o["front"]) * 0.9)
            draw.line([(ox, oy), (ox + ax * length, oy + ay * length)], fill=stem + (255,),
                      width=max(1, int(length * 0.03)))
            count = 6 + (o["seed"] % 3)
            for b in range(count):
                t = 0.18 + 0.82 * (b + 0.5) / count
                wob = (((b * 53 + o["seed"]) % 100) / 100 - 0.5) * length * 0.34
                bx, by = ox + ax * length * t + pxn * wob, oy + ay * length * t + pyn * wob
                oval(draw, bx, by, max(0.5, length * 0.085), max(0.5, length * 0.085), bud + (255,))
        elif kind == "scar":
            ox, oy, length = geom
            oval(draw, ox, oy, max(1.0, length * 0.3), max(1.0, length * 0.3), colour)
        elif kind == "corolla":
            ox, oy, deg, length = geom
            poly = [(ox + x, oy + y) for x, y in rotate(
                [(math.cos(a) * length * 0.5, math.sin(a) * length * 0.22)
                 for a in [i / 12 * 2 * math.pi for i in range(13)]], deg)]
            draw.polygon(poly, fill=colour)

    img = img.resize((width_px // ss, span_px), Image.LANCZOS)
    return Sprite(
        img,
        span_fraction=span_m / HEIGHT_M,
        base_x_fraction=(0.0 - min_x) / span_x,
        base_y_fraction=(max_y - 0.0) / span_m,
    )


def main():
    panel_h = 520
    panels = [render(season, panel_h).img for season in (SPRING, SUMMER, AUTUMN, WINTER)]
    gap = 24
    margin = 28
    label_h = 28
    total_w = sum(p.width for p in panels) + gap * (len(panels) - 1) + margin * 2
    total_h = panel_h + label_h + margin * 2
    sheet = Image.new("RGB", (total_w, total_h), (0x1A, 0x20, 0x26))
    x = margin
    for p in panels:
        sheet.paste(p, (x, margin))
        x += p.width + gap
    draw = ImageDraw.Draw(sheet)
    x = margin
    for season, p in zip(SEASON_NAMES, panels):
        draw.text((x + 4, margin + panel_h + 6), season, fill=(0xD8, 0xDF, 0xE6))
        x += p.width + gap
    out = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                       "docs", "paulownia-tree.png")
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main()
