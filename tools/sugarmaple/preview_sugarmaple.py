#!/usr/bin/env python3
"""Sugar maple — the landscape's second species, grown once and rendered once.

This mirrors, statement for statement, the Kotlin pair that ships in the app:
`ui/rendering/SugarMapleTree.kt` (the design's growth + organ placement) and
`ui/rendering/SugarMapleSprite.kt` (the raster the landscape blits at every
planting). It exists so the tree can be seen and tuned without a device —
the app itself never runs this file.

The design is the supplied `SugarMaple.kt`: *Acer saccharum*, about 19.6 m
tall, a 0.94 m trunk at breast height with root flare, ten hand-placed
scaffold limbs staggered between 3.35 and 7.40 m, recursive opposite forking
under an energy budget, current-year shoots, crown-gap fill twigs, and a broad
oval crown centred at (0, 11.05, 0) with radii (7.55, 7.35, 4.55).

What the app keeps: the geometry, the proportions, the organ sites.
What the app drops: the 800k-triangle GL meshes. The landscape climbs this
tree at 20–95 px, so it is rasterised once per foliage look and display size
and projected at every planting, exactly like the Paulownia.

Foliage looks (the app maps the Persian calendar onto them):
    GREEN   — spring + summer
    AUTUMN  — autumn + winter, yellow → gold → orange → scarlet

Usage:
    python3 tools/sugarmaple/preview_sugarmaple.py        # docs/sugar-maple.png
"""

import math
import os

from PIL import Image, ImageDraw

# ---------------------------------------------------------------------------
# The design's measured frame
# ---------------------------------------------------------------------------

HEIGHT_M = 19.6          # crown top
TRUNK_DBH_M = 0.94       # 0.47 m radius at breast height
CROWN_C = (0.0, 11.05, 0.0)
CROWN_R = (7.55, 7.35, 4.55)

SEED = 0x5ACE2026
MAX_BRANCHES = 2300      # the design's own budget (breadth-first, so it must be complete)
# The design's canopy is ~15,400 leaves and ~6,300 interior clusters. None of
# that survives at 20–95 px on canvas, so the sprite keeps a deterministic
# stride of them (uniform over the whole crown, never a truncated prefix) and
# enlarges what it keeps: leaves at 2.2×, one lobed cluster per kept cluster.
LEAF_STRIDE = 1 / 0.16
CLUSTER_STRIDE = 1 / 0.26
FILL_TWIGS = 850

GREEN, AUTUMN = 0, 1


# ---------------------------------------------------------------------------
# Deterministic growth (the design's own LCG, same constants, same order)
# ---------------------------------------------------------------------------

class Rnd:
    """32-bit LCG: `s = s * 1664525 + 1013904223`, read from bits 8..31."""

    MASK = 0xFFFFFFFF

    def __init__(self, seed):
        self.s = seed & self.MASK

    def next(self):
        self.s = (self.s * 1664525 + 1013904223) & self.MASK
        return ((self.s >> 8) & 0xFFFFFF) / 16777216.0

    def range(self, a, b):
        return a + (b - a) * self.next()


def add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def mul(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def nrm(a):
    l = math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2])
    if l == 0.0:
        l = 1.0
    return (a[0] / l, a[1] / l, a[2] / l)


def clamp(x, a, b):
    return a if x < a else (b if x > b else x)


def basis(d):
    up = (0.0, 1.0, 0.0) if abs(d[1]) < 0.9 else (1.0, 0.0, 0.0)
    e1 = nrm(cross(up, d))
    return e1, cross(d, e1)


def tilt(d, phi, angle):
    e1, e2 = basis(d)
    side = add(mul(e1, math.cos(phi)), mul(e2, math.sin(phi)))
    return nrm(add(mul(d, math.cos(angle)), mul(side, math.sin(angle))))


def crown_d(p):
    x = (p[0] - CROWN_C[0]) / CROWN_R[0]
    y = (p[1] - CROWN_C[1]) / CROWN_R[1]
    z = (p[2] - CROWN_C[2]) / CROWN_R[2]
    return math.sqrt(x * x + y * y + z * z)


def exposure(p):
    return clamp(
        0.10 + 0.72 * crown_d(p)
        + 0.13 * clamp(p[2] / CROWN_R[2], -1.0, 1.0)
        + 0.08 * clamp((p[1] - 4.0) / 15.0, 0.0, 1.0),
        0.0, 1.15,
    )


def front(p):
    return clamp(0.5 + p[2] / (2.0 * CROWN_R[2]), 0.0, 1.0)


class Node:
    __slots__ = ("p", "d", "r", "order", "root", "exp", "front")

    def __init__(self, p, d, r, order, root):
        self.p, self.d, self.r, self.order, self.root = p, d, r, order, root
        self.exp = exposure(p)
        self.front = front(p)


class Job:
    __slots__ = ("o", "d", "pref", "l", "e", "r", "order", "root")

    def __init__(self, o, d, l, e, r, order, root):
        self.o, self.d, self.pref, self.l, self.e, self.r = o, d, d, l, e, r
        self.order, self.root = order, root


class Limb:
    __slots__ = ("pts", "rads", "order")

    def __init__(self, pts, rads, order):
        self.pts, self.rads, self.order = pts, rads, order


def smooth_path(pts, rads, iters):
    p = [tuple(q) for q in pts]
    r = list(rads)
    for _ in range(iters):
        out_p, out_r = [p[0]], [r[0]]
        for i in range(len(p) - 1):
            a, b = p[i], p[i + 1]
            out_p.append(tuple(a[j] * 0.75 + b[j] * 0.25 for j in range(3)))
            out_r.append(r[i] * 0.75 + r[i + 1] * 0.25)
            out_p.append(tuple(a[j] * 0.25 + b[j] * 0.75 for j in range(3)))
            out_r.append(r[i] * 0.25 + r[i + 1] * 0.75)
        out_p.append(p[-1])
        out_r.append(r[-1])
        p, r = out_p, out_r
    return p, r


def js_round(x):
    return int(math.floor(x + 0.5))


def grow(seed=SEED):
    """The design's `growSugarMaple`, with the mesh calls replaced by path records."""
    rnd = Rnd(seed)
    rr = rnd.range
    limbs, tips, nodes, jobs = [], [], [], []

    trunk_pts = [
        (0.0, 0.0, 0.0), (-0.02, 0.45, 0.01), (0.03, 1.10, -0.01), (0.05, 2.00, 0.015),
        (-0.01, 3.00, -0.02), (0.02, 4.10, 0.0), (0.0, 5.15, 0.0),
    ]
    tp, tr = smooth_path(trunk_pts, [0.82, 0.56, 0.475, 0.455, 0.430, 0.370, 0.290], 2)
    limbs.append(Limb(tp, tr, 0))
    for i in range(6):
        a = i / 6.0 * math.pi * 2.0 + rr(-0.16, 0.16)
        d = (math.cos(a), 0.0, math.sin(a))
        reach = rr(0.80, 1.15)
        limbs.append(Limb(
            [(d[0] * 0.18, 0.30, d[2] * 0.18),
             (d[0] * reach * 0.58, 0.085, d[2] * reach * 0.58),
             (d[0] * reach, 0.002, d[2] * reach)],
            [0.210, 0.130, 0.050], 0,
        ))

    def scaffold(o, d, l, e, r, root):
        jobs.append(Job(o, nrm(d), l, e, r, 1, root))

    scaffold((0.0, 5.00, 0.0), (-0.03, 0.99, 0.01), 2.40, 21.0, 0.250, 0)
    scaffold((-0.04, 3.35, 0.03), (-0.82, 0.38, 0.08), 2.20, 21.5, 0.205, 1)
    scaffold((0.04, 3.55, -0.02), (0.86, 0.34, -0.02), 2.30, 19.5, 0.200, 2)
    scaffold((-0.02, 5.20, -0.03), (-0.55, 0.72, -0.16), 2.10, 21.5, 0.160, 3)
    scaffold((0.03, 5.55, 0.02), (0.55, 0.70, 0.15), 2.15, 19.0, 0.148, 4)
    scaffold((0.0, 4.70, -0.06), (-0.10, 0.73, -0.50), 2.00, 19.5, 0.148, 5)
    scaffold((-0.03, 4.05, 0.06), (-0.58, 0.38, 0.42), 2.05, 20.5, 0.165, 6)
    scaffold((0.03, 4.45, 0.05), (0.58, 0.36, 0.39), 2.05, 18.5, 0.152, 7)
    scaffold((-0.01, 7.00, -0.01), (-0.48, 0.82, -0.04), 1.90, 19.0, 0.115, 8)
    scaffold((0.01, 7.40, 0.01), (0.36, 0.87, 0.04), 1.85, 16.5, 0.104, 9)

    branch_count = 0
    ji = 0
    while ji < len(jobs) and branch_count < MAX_BRANCHES:
        job = jobs[ji]
        ji += 1
        branch_count += 1
        steps = max(3, min(8, js_round(job.l / (0.62 if job.order < 3 else 0.38))))
        step_len = job.l / steps
        curve_phi = rr(0.0, math.pi * 2.0)
        curve_amt = rr(0.080, 0.200)
        curve_dir = tilt(job.d, curve_phi, math.pi / 2.0)
        p = job.o
        d = job.d
        pts, rads = [p], [job.r * (1.22 if job.order > 1 else 1.06)]
        energy_left = job.e - job.l
        terminal = energy_left <= 0.62 or job.r <= 0.007 or job.order >= 8
        if terminal:
            taper = 0.72
        elif job.order == 1:
            taper = 0.28
        elif job.order == 2:
            taper = 0.32
        elif job.order == 3:
            taper = 0.38
        elif job.order == 4:
            taper = 0.44
        else:
            taper = 0.52
        laterals = 0
        if job.order <= 2:
            max_laterals = 4
        elif job.order == 3:
            max_laterals = 3
        elif job.order <= 5:
            max_laterals = 2
        elif job.order == 6:
            max_laterals = 1
        else:
            max_laterals = 0
        for s in range(1, steps + 1):
            f = s / steps
            wish = add(mul(d, 0.76), add(mul(job.pref, 0.24),
                                         mul(curve_dir, curve_amt * (0.55 + 0.45 * math.sin(f * 4.2 + job.root)))))
            if job.order >= 5:
                wish = add(wish, (0.0, 0.065, 0.0))
            if f > 0.76 and job.order >= 4:
                wish = add(wish, (0.0, 0.11, 0.0))
            if p[1] < 6.2 and job.order <= 2:
                wish = add(wish, mul(nrm((p[0], 0.0, p[2])), 0.07))
            jitter_phi = rr(0.0, math.pi * 2.0)
            jitter_amt = rr(0.030, 0.100)
            wish = add(wish, mul(tilt(d, jitter_phi, math.pi / 2.0), jitter_amt))
            d = nrm(wish)
            nxt = add(p, mul(d, step_len))
            cd = crown_d(nxt)
            if cd > 0.94 and job.order > 1:
                grad = nrm((
                    (nxt[0] - CROWN_C[0]) / (CROWN_R[0] * CROWN_R[0]),
                    (nxt[1] - CROWN_C[1]) / (CROWN_R[1] * CROWN_R[1]),
                    (nxt[2] - CROWN_C[2]) / (CROWN_R[2] * CROWN_R[2]),
                ))
                radial = dot(d, grad)
                if radial > 0.0:
                    d = nrm(add(sub(d, mul(grad, radial * (1.12 if cd > 1.05 else 0.72))), (0.0, 0.025, 0.0)))
                    nxt = add(p, mul(d, step_len * (0.76 if cd > 1.05 else 0.92)))
            if nxt[1] < 2.75:
                nxt = (nxt[0], 2.75 + 0.10 * f, nxt[2])
            p = nxt
            radius_at = max(0.0038, job.r * (1.0 - taper * f ** 0.90))
            pts.append(p)
            rads.append(radius_at)
            nodes.append(Node(p, d, radius_at, job.order, job.root))
            if (s >= 2 and s < steps and laterals < max_laterals and job.order < 8
                    and len(jobs) < MAX_BRANCHES + 90):
                if job.order == 1:
                    chance = 0.50
                elif job.order == 2:
                    chance = 0.48
                elif job.order == 3:
                    chance = 0.42
                else:
                    chance = 0.34
                energy_at = job.e - job.l * f
                if energy_at > 0.75 and rnd.next() < chance:
                    phi = rr(0.0, math.pi * 2.0)
                    pair = job.order >= 2 and rnd.next() < 0.48
                    n_kids = 2 if pair else 1
                    k = 0
                    while k < n_kids and laterals < max_laterals:
                        t = rr(0.42, 0.78)
                        cd2 = tilt(d, phi + k * math.pi, t)
                        outward = nrm((p[0] * 0.8, 0.20, p[2] * 0.8))
                        cd2 = nrm(add(add(cd2, mul(outward, 0.11)), (0.0, 0.075, 0.0)))
                        ce = energy_at * rr(0.30, 0.49)
                        cl = min(ce, 0.50 + ce * rr(0.14, 0.22))
                        cr = max(0.0062, radius_at * rr(0.50, 0.68))
                        jobs.append(Job(add(p, mul(d, -step_len * 0.05)), cd2, cl, ce, cr,
                                        job.order + 1, job.root))
                        laterals += 1
                        k += 1
        end_r = rads[-1]
        continued = False
        if (not terminal and energy_left > 0.62 and end_r > 0.0058 and job.order < 8
                and len(jobs) < MAX_BRANCHES + 90):
            if job.order <= 2:
                n_kids = 2
            else:
                n_kids = 2 if rnd.next() < 0.88 else 1
            phi = rr(0.0, math.pi * 2.0)
            for k in range(n_kids):
                t = rr(0.16, 0.36) if job.order <= 2 else rr(0.20, 0.48)
                phi_k = phi + k * math.pi + rr(-0.24, 0.24)
                cd2 = tilt(d, phi_k, t)
                outward = nrm((p[0] * 0.65, 0.12, p[2] * 0.65))
                cd2 = nrm(add(add(cd2, mul(outward, 0.055)),
                             (0.0, 0.160 if job.order >= 4 else 0.075, 0.0)))
                if n_kids == 1:
                    ce = energy_left * rr(0.95, 1.00)
                elif k == 0:
                    ce = energy_left * rr(0.93, 0.99)
                else:
                    ce = energy_left * rr(0.55, 0.75)
                cl = min(ce, 0.65 + ce * rr(0.12, 0.20))
                cr = max(0.0052, end_r * (rr(0.74, 0.86) if k == 0 else rr(0.55, 0.72)))
                jobs.append(Job(add(p, mul(d, -step_len * 0.04)), cd2, cl, ce, cr,
                                job.order + 1, job.root))
                continued = True
        sm_p, sm_r = smooth_path(pts, rads, 1)
        limbs.append(Limb(sm_p, sm_r, job.order))
        if not continued:
            tips.append(Node(p, d, end_r, job.order, job.root))

    # ---- current-year shoots -------------------------------------------------
    def add_shoot(origin, parent_dir, parent_r, root, order):
        grad = nrm((
            (origin[0] - CROWN_C[0]) / (CROWN_R[0] * CROWN_R[0]),
            (origin[1] - CROWN_C[1]) / (CROWN_R[1] * CROWN_R[1]),
            (origin[2] - CROWN_C[2]) / (CROWN_R[2] * CROWN_R[2]),
        ))
        grad = (grad[0], max(grad[1], -0.04), grad[2])
        phi = rr(0.0, math.pi * 2.0)
        tangent = tilt(parent_dir, phi, math.pi / 2.0)
        tangent_amt = rr(-0.36, 0.36)
        up_amt = rr(0.14, 0.34)
        d = nrm(add(add(add(mul(grad, 0.48), mul(parent_dir, 0.34)),
                         mul(tangent, tangent_amt)), (0.0, up_amt, 0.0)))
        ln = rr(0.30, 0.68) * (0.68 if order > 8 else 1.0)
        r0 = clamp(parent_r * rr(0.26, 0.40), 0.0028, 0.0095)
        side_bend = rr(-0.05, 0.05)
        p0 = add(origin, mul(d, -0.035))
        p1 = add(add(origin, mul(d, ln * 0.48)), add(mul(tangent, side_bend * ln), (0.0, ln * 0.035, 0.0)))
        p2 = add(add(origin, mul(d, ln)), (0.0, ln * 0.105, 0.0))
        rr(0.0, 30.0)  # tube seed, consumed exactly as the design consumes it
        limbs.append(Limb([p0, p1, p2], [r0 * 1.18, r0 * 0.72, r0 * 0.38], order))
        end_dir = nrm(sub(p2, p1))
        nodes.append(Node(p1, end_dir, r0 * 0.72, order, root))
        tips.append(Node(p2, end_dir, r0 * 0.38, order, root))
        if order == 8 and rnd.next() < 0.24:
            add_shoot(p1, end_dir, r0, root, 9)

    base_tips = list(tips)
    base_nodes = list(nodes)
    for tip in base_tips:
        if tip.exp > 0.58:
            add_shoot(tip.p, tip.d, max(tip.r, 0.012), tip.root, 8)
            if tip.exp > 0.90 and rnd.next() < 0.35:
                add_shoot(tip.p, tip.d, max(tip.r, 0.012), tip.root, 8)
    for node in base_nodes:
        if node.order >= 5 and rnd.next() < clamp(0.10 + 0.34 * node.exp, 0.0, 0.46):
            add_shoot(node.p, node.d, max(node.r, 0.012), node.root, 8)
            if node.exp > 0.88 and rnd.next() < 0.16:
                add_shoot(node.p, node.d, max(node.r, 0.012), node.root, 8)
    for node in base_nodes:
        if node.order in (2, 3) and rnd.next() < 0.28 + 0.22 * node.exp:
            add_shoot(node.p, node.d, max(node.r, 0.014), node.root, 8)
    for node in base_nodes:
        if node.order == 1 and rnd.next() < 0.38:
            add_shoot(node.p, node.d, max(node.r, 0.014), node.root, 8)

    # ---- crown gap fill ------------------------------------------------------
    fill_pool = list(base_tips) + list(base_nodes)
    wedge_from = int(FILL_TWIGS * 0.66)
    for i in range(FILL_TWIGS):
        if i >= wedge_from:
            yy = rr(3.8, 8.6)
            yn = (yy - CROWN_C[1]) / CROWN_R[1]
            zn = rr(-0.15, 0.95)
            allow = math.sqrt(max(0.04, 1.0 - yn * yn - zn * zn))
            tx = rr(-1.0, 1.0) * CROWN_R[0] * allow * 0.94
            target = (tx, yy, zn * CROWN_R[2] * 0.94)
        else:
            q = rnd.next()
            factor = rr(0.35, 0.65) if q < 0.25 else (rr(0.65, 0.92) if q < 0.80 else rr(0.92, 1.05))
            zz = rr(0.10, 1.0) if i % 3 == 0 else rr(-1.0, 1.0)
            phi = rr(0.0, math.pi * 2.0)
            horiz = math.sqrt(max(0.0, 1.0 - zz * zz))
            target = (
                CROWN_C[0] + math.cos(phi) * horiz * CROWN_R[0] * factor,
                CROWN_C[1] + zz * CROWN_R[1] * factor,
                CROWN_C[2] + math.sin(phi) * horiz * CROWN_R[2] * factor,
            )
        if target[1] < 3.60:
            continue
        best, best_d2 = None, 1e9
        for cand in fill_pool:
            dx, dy, dz = target[0] - cand.p[0], target[1] - cand.p[1], target[2] - cand.p[2]
            d2 = dx * dx + dy * dy + dz * dz
            if d2 < best_d2:
                best_d2, best = d2, cand
        dist = math.sqrt(best_d2)
        if best is None or dist < 0.22 or dist > (2.30 if i >= wedge_from else 1.60):
            continue
        target_dir = nrm(sub(target, best.p))
        fill_grad = nrm((
            (best.p[0] - CROWN_C[0]) / (CROWN_R[0] * CROWN_R[0]),
            (best.p[1] - CROWN_C[1]) / (CROWN_R[1] * CROWN_R[1]),
            (best.p[2] - CROWN_C[2]) / (CROWN_R[2] * CROWN_R[2]),
        ))
        d = nrm(add(add(add(mul(target_dir, 0.62), mul(best.d, 0.26)),
                         mul(fill_grad, 0.16)), (0.0, 0.16, 0.0)))
        ln = dist * rr(0.68, 0.86)
        bend_phi = rr(0.0, math.pi * 2.0)
        bend_amt = rr(-0.12, 0.12)
        tangent = tilt(d, bend_phi, math.pi / 2.0)
        r0 = clamp(max(best.r, 0.012) * rr(0.50, 0.70), 0.008, 0.032)
        p0 = add(best.p, mul(d, -0.04))
        p1 = add(add(best.p, mul(d, ln * 0.38)), mul(tangent, bend_amt * ln))
        p2 = add(add(add(best.p, mul(d, ln * 0.72)), mul(tangent, -bend_amt * ln * 0.45)),
                 (0.0, ln * 0.035, 0.0))
        p3 = add(add(best.p, mul(d, ln)), (0.0, ln * 0.075, 0.0))
        fp, fr = smooth_path([p0, p1, p2, p3], [r0 * 1.15, r0 * 0.76, r0 * 0.52, r0 * 0.30], 1)
        rr(0.0, 30.0)  # tube seed
        end_dir = nrm(sub(p3, p2))
        limbs.append(Limb(fp, fr, 10))
        rec1 = Node(p1, end_dir, r0 * 0.76, 10, best.root)
        rec2 = Node(p2, end_dir, r0 * 0.52, 10, best.root)
        tip_rec = Node(p3, end_dir, r0 * 0.30, 10, best.root)
        nodes.append(rec1)
        nodes.append(rec2)
        tips.append(tip_rec)
        fill_pool.append(rec2)
        fill_pool.append(tip_rec)
        if ln > 0.62 and rnd.next() < 0.28:
            fork_phi = rr(0.0, math.pi * 2.0)
            fork_tilt = rr(0.28, 0.52)
            fd = nrm(add(add(tilt(end_dir, fork_phi, fork_tilt), mul(end_dir, 0.22)), (0.0, 0.08, 0.0)))
            fl = rr(0.18, 0.38)
            fe = add(add(p2, mul(fd, fl)), (0.0, fl * 0.08, 0.0))
            rr(0.0, 30.0)  # fork seed
            limbs.append(Limb([p2, fe], [r0 * 0.42, r0 * 0.16], 11))
            fork_rec = Node(fe, fd, r0 * 0.16, 11, best.root)
            tips.append(fork_rec)
            fill_pool.append(fork_rec)
    return limbs, tips, nodes, branch_count


# ---------------------------------------------------------------------------
# Organ placement — the design's site selection, shared by both looks
# ---------------------------------------------------------------------------

class Organ:
    """One seasonal organ, in the tree frame (metres, +y up, +z toward viewer)."""

    __slots__ = ("kind", "x", "y", "z", "length", "ax", "ay", "exposure", "front",
                 "seed", "tone", "detail")

    def __init__(self, kind, p, length, ax, ay, exp, fr, seed, tone, detail=0):
        self.kind = kind
        self.x, self.y, self.z = p
        self.length, self.ax, self.ay = length, ax, ay
        self.exposure, self.front, self.seed, self.tone = exp, fr, seed, tone
        self.detail = detail


LEAF, LEAF_BACK, CLUSTER, SAMARA, BUD = "leaf", "leaf_back", "cluster", "samara", "bud"


def radial_of(p):
    l = math.sqrt(p[0] * p[0] + p[2] * p[2])
    return (p[0] / l, 0.0, p[2] / l) if l > 0.15 else (0.0, 0.0, 1.0)


# The sun the sprite bakes its own shading against: high and slightly left and
# toward the viewer, so crowns have a lit upper-left shell and a shaded heart
# the scene's own light can then multiply.
SUN_DIR = (-0.42, 0.78, 0.46)


def shield(p):
    """Crown-normal light term: 0 deep in shade, 1 on the sun-facing shell."""
    n = nrm((p[0] - CROWN_C[0], (p[1] - CROWN_C[1]) * 0.85, p[2] - CROWN_C[2]))
    d = n[0] * SUN_DIR[0] + n[1] * SUN_DIR[1] + n[2] * SUN_DIR[2]
    return clamp(0.5 + 0.5 * d, 0.0, 1.0)


def leaf_hue(node, color_r):
    """The design's autumn hue coordinate: 0 = lingering green, 1 = scarlet."""
    height_f = clamp((node.p[1] - 3.5) / 15.0, 0.0, 1.0)
    root_tint = 0.07 * math.sin(node.root * 2.71 + 0.4)
    return clamp(0.10 + 0.58 * node.exp + 0.22 * height_f
                 + 0.20 * (color_r - 0.5) + root_tint, 0.0, 1.18)


def place_organs(limbs, tips, nodes, seed=SEED):
    rnd = Rnd(seed * 7919 + 17)
    rr = rnd.range
    out = []
    leaves = 0
    clusters = 0

    def axis_to(x, y, z):
        sx, sy = x, -y
        l = math.sqrt(sx * sx + sy * sy)
        if l < 1e-4 or l < 0.22:
            return 0.0, -1.0
        return sx / l, sy / l

    def add_leaf(node, sign, phi):
        nonlocal leaves
        e1, e2 = basis(node.d)
        leaf_axis = add(mul(e1, math.cos(phi)), mul(e2, math.sin(phi)))
        radial = radial_of(node.p)
        pet_dir = nrm(add(add(add(mul(leaf_axis, sign * 0.78), mul(radial, 0.30)),
                             mul(node.d, 0.25)), (0.0, 0.30, 0.0)))
        pet_len = rr(0.055, 0.105)
        rr(0.0, 20.0)                       # petiole seed
        base = add(add(node.p, mul(pet_dir, pet_len)), (0.0, pet_len * 0.08, 0.0))
        droop_r = rr(0.0, 1.0)
        droop_dir = 0.10 + 0.22 * droop_r
        blade_dir = nrm(add(add(add(add(mul(radial, 0.18), mul(pet_dir, 0.22)),
                                    mul(node.d, 0.34)), mul(leaf_axis, sign * 0.38)),
                            (0.0, -droop_dir, 0.0)))
        rr(0.0, math.pi * 2.0)              # blade phi
        rr(-0.34, 0.34)                     # blade roll
        rr(-0.75, 0.75)                     # up-hint roll
        len_r = rr(0.0, 1.0)
        length = (0.092 + 0.058 * len_r ** 1.15) * (0.90 + 0.22 * node.exp) * 2.2
        rr(0.92, 1.08)                      # width ratio
        rr(0.88, 1.10)                      # leaf shade
        color_r = rr(0.0, 1.0)
        org_seed = rr(0.0, 40.0)
        rr(0.90, 1.10)                      # asym
        rr(0.0, 1.0)                        # arch
        rr(0.0, 1.0)                        # fold
        rr(0.0, 1.0)                        # blade droop
        rr(0.0, 1.0)                        # ripple
        detail = 2 if (node.front > 0.96 and node.exp > 1.00) else (
            1 if (node.front > 0.86 and node.exp > 0.88) else 0)
        ax, ay = axis_to(blade_dir[0], blade_dir[1], blade_dir[2])
        leaves += 1
        if hash01(seed, leaves * 7 + 3) < 1.0 / LEAF_STRIDE:
            out.append(Organ(LEAF, base, length, ax, ay, node.exp, node.front,
                             int(org_seed * 1000), leaf_hue(node, color_r), detail))

    def add_cluster(node):
        nonlocal clusters
        color_r = rr(0.0, 1.0)
        radius_r = rr(0.0, 1.0)
        org_seed = rr(0.0, 40.0)
        radius = (0.32 + 0.34 * radius_r) * (0.74 + 0.36 * node.exp) * \
            (1.0 + 0.30 * clamp((node.p[1] - 13.5) / 4.5, 0.0, 1.0))
        radial = radial_of(node.p)
        centre = add(add(add(node.p, mul(radial, -radius * 0.08)), mul(node.d, radius * 0.16)),
                     (0.0, radius * 0.05, 0.0))
        ax, ay = axis_to(centre[0], centre[1], centre[2])
        # Nothing of the canopy mass may leave the crown shell: a cluster whose rim pokes out
        # reads as a detached island of leaves floating above the tree.
        if crown_d(centre) + radius / min(CROWN_R) > 0.98:
            return
        clusters += 1
        if hash01(seed, clusters * 11 + 5) < 1.0 / CLUSTER_STRIDE:
            out.append(Organ(CLUSTER, centre, radius * 2.0, ax, ay, node.exp, node.front,
                             int(org_seed * 1000), leaf_hue(node, color_r)))

    def add_samara(node):
        n = 2 + (1 if rnd.next() < 0.55 else 0) + (1 if rnd.next() < 0.22 else 0)
        for _ in range(n):
            e1, e2 = basis(node.d)
            phi = rr(0.0, math.pi * 2.0)
            axis = add(mul(e1, math.cos(phi)), mul(e2, math.sin(phi)))
            hang = nrm(add(add(add(mul(node.d, 0.22), mul(radial_of(node.p), 0.30)),
                               mul(axis, 0.38)), (0.0, -0.88, 0.0)))
            stalk = rr(0.035, 0.078)
            origin = add(node.p, mul(hang, stalk))
            org_seed = rr(0.0, 20.0)
            sam_len = rr(0.024, 0.034)
            rr(0.52, 0.78)                  # half angle
            rr(0.90, 1.05)                   # shade
            out.append(Organ(SAMARA, origin, sam_len * 1.9, hang[0], -hang[1],
                             node.exp, node.front, int(org_seed * 1000), 0.0))
            # the design emits two wings; the sprite draws both from one organ

    def add_bud(node):
        length = rr(0.010, 0.016)
        rr(0.92, 1.05)
        out.append(Organ(BUD, node.p, max(length, 0.012) * 2.2, node.d[0], -node.d[1],
                         node.exp, node.front, 0, 0.0))

    for node in nodes:
        if node.order >= 3:
            pair_chance = 0.16 + 0.16 * node.exp if node.order == 3 else 0.48 + 0.25 * node.exp
            if rnd.next() < pair_chance:
                phi = rr(0.0, math.pi * 2.0)
                add_leaf(node, -1, phi)
                if rnd.next() < 0.88:
                    add_leaf(node, 1, phi)
            elif rnd.next() < 0.24:
                phi = rr(0.0, math.pi * 2.0)
                add_leaf(node, -1 if rnd.next() < 0.5 else 1, phi)
    for tip in tips:
        if tip.order >= 4 and rnd.next() < 0.42 + 0.30 * tip.exp:
            phi = rr(0.0, math.pi * 2.0)
            sign = -1 if rnd.next() < 0.5 else 1
            add_leaf(tip, sign, phi)
            if rnd.next() < 0.42:
                add_leaf(tip, -sign, phi)
    for node in nodes:
        if node.order >= 3 and rnd.next() < (0.12 + 0.16 * node.exp if node.order == 3
                                             else 0.26 + 0.22 * node.exp):
            add_cluster(node)
    for tip in tips:
        if tip.order >= 4 and rnd.next() < 0.34 + 0.26 * tip.exp:
            add_cluster(tip)
    for node in nodes:
        if node.order >= 4 and node.p[1] > 15.0 and rnd.next() < 0.20:
            add_cluster(node)
    for tip in tips:
        if tip.order >= 4 and tip.p[1] > 14.0 and rnd.next() < 0.30:
            add_cluster(tip)
    for tip in tips:
        if tip.order >= 4 and tip.front > 0.55 and rnd.next() < 0.10:
            add_bud(tip)
    for tip in tips:
        if tip.order >= 4 and rnd.next() < 0.009 + 0.013 * tip.exp:
            add_samara(tip)
    for node in nodes:
        if node.order >= 7 and rnd.next() < 0.002 + 0.003 * node.exp:
            add_samara(node)
    return out


# ---------------------------------------------------------------------------
# Sprite: the one render, projected
# ---------------------------------------------------------------------------

class Sprite:
    def __init__(self, img, span_fraction, base_x_fraction, base_y_fraction):
        self.img = img
        self.span_fraction = span_fraction
        self.base_x_fraction = base_x_fraction
        self.base_y_fraction = base_y_fraction

    def at(self, hgt_px, flip=False, tint=(255, 255, 255), alpha=1.0):
        h = max(2, int(round(hgt_px * self.span_fraction)))
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


def hash01(seed, index):
    x = (seed * 1103515245 + index * 97531 + 12345) & 0xFFFFFFFF
    x ^= (x >> 13)
    x = (x * 0x5bd1e995) & 0xFFFFFFFF
    x ^= (x >> 15)
    return (x & 0x7FFFFFFF) / 2147483648.0


def cluster_polygon(r, squish, seed, index, lobes=9):
    n = max(5, min(9, lobes))
    phase = hash01(seed, index * 13 + 101) * 2 * math.pi
    lobe_freq = 2 + int(hash01(seed, index * 17 + 103) * 3)
    out = []
    for k in range(n):
        jitter = (hash01(seed + k * 29, index * 19 + k * 7) - 0.5) * 0.30
        ang = phase + (k / n) * 2 * math.pi + jitter * 0.45
        lobe = 0.5 + 0.5 * math.sin(ang * lobe_freq + phase * 2.1)
        factor = clamp(0.66 + 0.38 * lobe + (hash01(seed + k * 37, index * 23 + k * 11) - 0.5) * 0.36,
                       0.62, 1.20)
        out.append((math.cos(ang) * r * factor, math.sin(ang) * r * factor * squish))
    return out


def shift(color, factor):
    f = clamp(factor, 0.0, 1.60)
    return tuple(min(255, int(c * f)) for c in color[:3])


def blend(a, b, t):
    f = clamp(t, 0.0, 1.0)
    return tuple(int(a[i] + (b[i] - a[i]) * f) for i in range(3))


# The design's palette, translated to the landscape's daylight sRGB (the same
# translation the Paulownia sprite uses: finished art, not linear albedo).
BARK_GROOVE = (0x24, 0x1F, 0x1B)
BARK = (0x4A, 0x42, 0x3A)
BARK_RIDGE = (0x8A, 0x80, 0x72)
TWIG = (0x6B, 0x4A, 0x33)
LEAF_SHADE = (0x1B, 0x33, 0x12)
LEAF_SUN = (0x53, 0x84, 0x28)
LEAF_UNDER = (0x55, 0x6B, 0x3A)
VEIN = (0x6E, 0x80, 0x3C)
AUTUMNS = [
    (0x8A, 0x7A, 0x20),   # lingering yellow-green
    (0xB0, 0x8A, 0x18),   # yellow
    (0xB4, 0x55, 0x0F),   # gold-orange
    (0x9A, 0x2B, 0x0E),   # orange-red
    (0x8A, 0x14, 0x10),   # scarlet
    (0x6E, 0x0A, 0x08),   # deep red
]
AUTUMN_BROWN = (0x7A, 0x52, 0x30)
AUTUMN_UNDER = (0xA0, 0x64, 0x22)
SAMARA_GREEN = (0x4E, 0x6B, 0x24)
SAMARA_BROWN = (0x6E, 0x45, 0x20)
BUD_RED = (0x55, 0x30, 0x1E)


def autumn_colour(hue):
    if hue < 0.28:
        return AUTUMNS[0]
    if hue < 0.48:
        return AUTUMNS[1]
    if hue < 0.68:
        return AUTUMNS[2]
    if hue < 0.86:
        return AUTUMNS[3]
    return AUTUMNS[4]


LEAF_OUTLINE = [
    (0.0, -0.015), (0.150, 0.005), (0.305, 0.075), (0.205, 0.225),
    (0.520, 0.540), (0.285, 0.590), (0.205, 0.785), (0.0, 1.0),
    (-0.205, 0.785), (-0.285, 0.590), (-0.520, 0.540), (-0.205, 0.225),
    (-0.305, 0.075), (-0.150, 0.005),
]


def leaf_polygon(length):
    w = length * 1.02
    return [(x * w, -y * length) for (x, y) in LEAF_OUTLINE]


def render(look, span_px=280, sky=(0xC9, 0xD3, 0xDC), seed=SEED):
    """Rasterise the design's tree once, the way SugarMapleSprite.bake does."""
    limbs, tips, nodes, _ = grow(seed)
    organs = place_organs(limbs, tips, nodes, seed)

    xs, ys, zs = [], [], []
    for limb in limbs:
        for (x, y, z), r in zip(limb.pts, limb.rads):
            xs += [x - r, x + r]
            ys += [y - r, y + r]
            zs.append(z)
    for o in organs:
        ex, ey = o.ax * o.length, o.ay * o.length
        px, py = -o.ay * o.length, o.ax * o.length
        xs += [o.x, o.x + ex, o.x + px, o.x - px]
        ys += [o.y, o.y + ey, o.y + py, o.y - py]
    min_x, max_x = min(xs) - 0.10, max(xs) + 0.10
    min_y, max_y = min(ys) - 0.05, max(ys) + 0.10
    span_m = max(0.5, max_y - min_y)
    span_x = max(0.5, max_x - min_x)
    k = span_px / span_m
    width_px = max(1, int(math.ceil(span_x * k)))

    img = Image.new("RGBA" if sky is None else "RGB", (width_px, span_px),
                    (0, 0, 0, 0) if sky is None else sky)
    d = ImageDraw.Draw(img, "RGBA")

    def sx(v):
        return (v - min_x) * k

    def sy(v):
        return (max_y - v) * k

    wood, mass, orgs_out = [], [], []

    # ---- crown mass: one lobed cluster per selected cluster organ, in the look's
    # own interior shade (green under green, orange-brown under autumn).
    if look == GREEN:
        mass_col, mass_alpha = LEAF_SHADE, 226
    else:
        mass_col, mass_alpha = AUTUMN_BROWN, 210
    for o in organs:
        if o.kind != CLUSTER:
            continue
        rad = o.length * 0.5 * k
        cx, cy = sx(o.x), sy(o.y)
        lit = shield((o.x, o.y + o.length * 0.35, o.z))
        if look == GREEN:
            col = blend(LEAF_SHADE, blend(LEAF_SUN, LEAF_UNDER, 0.35), 0.62 * lit)
        else:
            col = shift(blend(AUTUMN_BROWN, autumn_colour(o.tone), 0.45),
                        0.45 + 0.55 * lit)
        pts = cluster_polygon(rad, 0.86, 7717 + int(o.seed), int(o.tone * 1000), 9)
        mass.append((o.z, "mass", [(cx + x, cy + y) for x, y in pts], col, mass_alpha))

    # ---- wood
    for limb in limbs:
        n = len(limb.rads)
        if n < 2:
            continue
        pts = [(sx(limb.pts[i][0]), sy(limb.pts[i][1])) for i in range(n)]
        rads = [limb.rads[i] * k for i in range(n)]
        mid = n // 2
        z = limb.pts[mid][2]
        exp = 0.62 + 0.40 * clamp(exposure(limb.pts[mid]), 0.0, 1.15)
        col = BARK if limb.order <= 1 else TWIG
        wood.append((z, "path", ribbon(pts, rads), shift(col, exp), None))
        if limb.order <= 1 and rads[0] > 3.0:
            for f in (-0.45, -0.05, 0.48):
                c2 = shift(BARK_GROOVE, exp * 0.9) if f < 0 else shift(BARK_RIDGE, exp)
                wood.append((z + 0.01, "stroke", pts, c2, (f, rads)))

    # ---- leaves, samaras, buds
    for o in organs:
        ox, oy = sx(o.x), sy(o.y)
        length = max(1.2, o.length * k)
        deg = math.degrees(math.atan2(o.ax, -o.ay))
        expo = clamp(o.exposure * 1.15, 0.0, 1.15)
        front_f = 0.80 + 0.30 * o.front
        z = o.z + o.length * 0.05
        if o.kind == LEAF:
            lit = shield((o.x, o.y, o.z))
            if look == GREEN:
                base = blend(LEAF_SHADE, LEAF_SUN, clamp(0.05 + 1.05 * lit, 0.0, 1.0))
                # Interior blades (low exposure) go dull, never sunlit.
                base = blend(base, LEAF_UNDER, 0.22 * (1.0 - clamp(o.exposure, 0.0, 1.0)))
                edge = LEAF_UNDER
            else:
                base = blend(autumn_colour(o.tone), AUTUMNS[0], 0.30 * (1.0 - lit))
                edge = AUTUMN_BROWN
            col = shift(base, front_f * (0.78 + 0.34 * lit))
            orgs_out.append((z, "leaf", (ox, oy, deg, length), col, shift(edge, front_f * 0.9)))
        elif o.kind == SAMARA:
            col = SAMARA_GREEN if look == GREEN else SAMARA_BROWN
            orgs_out.append((z, "samara", (ox, oy, deg, length), shift(col, front_f), None))
        elif o.kind == BUD:
            orgs_out.append((z, "bud", (ox, oy, length), BUD_RED, None))

    # Painter order is three layers, not one z-sort: the woody frame, then the
    # canopy mass over it (a crown's interior wood is hidden by its own leaves),
    # then the seasonal organs in front. Depth still sorts inside each layer.
    prims = sorted(wood, key=lambda p: p[0]) + sorted(mass, key=lambda p: p[0]) \
        + sorted(orgs_out, key=lambda p: p[0])
    for (_z, kind, geom, colour, extra) in prims:
        if kind == "mass":
            d.polygon(geom, fill=tuple(colour) + (extra,))
        elif kind == "path":
            d.polygon(geom, fill=tuple(colour) + (255,))
        elif kind == "stroke":
            pts, col, (f, rads) = geom, colour, extra
            n = len(pts)
            for i in range(n):
                a, b = max(0, i - 1), min(n - 1, i + 1)
                dx, dy = pts[b][0] - pts[a][0], pts[b][1] - pts[a][1]
                l = math.hypot(dx, dy) or 1.0
                x = pts[i][0] - (dy / l) * rads[i] * f
                y = pts[i][1] + (dx / l) * rads[i] * f
                r = max(0.6, rads[0] * 0.13)
                d.ellipse([x - r, y - r, x + r, y + r], fill=tuple(col) + (255,))
        elif kind == "leaf":
            ox, oy, deg, length = geom
            poly = [(ox + x, oy + y) for x, y in rotate(leaf_polygon(length), deg)]
            d.polygon(poly, fill=tuple(colour) + (255,), outline=tuple(extra) + (170,))
            # palmate veins
            vcol = shift(colour, 0.72)
            for vx, vy in ((0.0, -1.0), (0.62, -0.62), (-0.62, -0.62), (0.95, -0.08), (-0.95, -0.08)):
                vx2, vy2 = rotate([(vx * length * 0.5, vy * length)], deg)[0]
                d.line([(ox, oy), (ox + vx2, oy + vy2)], fill=tuple(vcol) + (200,), width=1)
        elif kind == "samara":
            ox, oy, deg, length = geom
            col = tuple(colour)
            for sign in (-1, 1):
                wing = [(ox, oy)] + [(ox + x, oy + y) for x, y in
                                     rotate(samara_wing(length, sign), deg)]
                d.polygon(wing, fill=col + (235,))
        elif kind == "bud":
            ox, oy, blen = geom
            r = max(0.6, blen * 0.35)
            d.ellipse([ox - r, oy - r, ox + r, oy + r], fill=tuple(colour) + (240,))

    return Sprite(
        img,
        span_fraction=span_m / HEIGHT_M,
        base_x_fraction=(0.0 - min_x) / span_x,
        base_y_fraction=(max_y - 0.0) / span_m,
    )


def rotate(pts, deg):
    a = math.radians(deg)
    ca, sa = math.cos(a), math.sin(a)
    return [(x * ca - y * sa, x * sa + y * ca) for x, y in pts]


def samara_wing(length, sign):
    """One winged half of a paired samara: a lens with a seed bulge at the base."""
    return [(sign * 0.10 * length, -0.16 * length),
            (sign * 0.34 * length, -0.20 * length),
            (sign * 0.52 * length, -0.72 * length),
            (sign * 0.30 * length, -1.00 * length),
            (sign * 0.16 * length, -0.62 * length),
            (sign * 0.06 * length, -0.30 * length)]


def ribbon(pts, rads):
    n = len(rads)
    nx, ny = [0.0] * n, [0.0] * n
    for i in range(n):
        a, b = max(0, i - 1), min(n - 1, i + 1)
        dx, dy = pts[b][0] - pts[a][0], pts[b][1] - pts[a][1]
        l = math.hypot(dx, dy)
        if l < 1e-3:
            dx, dy = 0.0, 1.0
        else:
            dx, dy = dx / l, dy / l
        nx[i], ny[i] = -dy, dx
    out = [(pts[0][0] + nx[0] * rads[0], pts[0][1] + ny[0] * rads[0])]
    for i in range(1, n):
        out.append((pts[i][0] + nx[i] * rads[i], pts[i][1] + ny[i] * rads[i]))
    for i in range(n - 1, -1, -1):
        out.append((pts[i][0] - nx[i] * rads[i], pts[i][1] - ny[i] * rads[i]))
    return out


# ---------------------------------------------------------------------------
# Sheet
# ---------------------------------------------------------------------------

def main():
    out = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "..",
                                        "docs", "sugar-maple.png"))
    panel = 420
    labels = [("spring", GREEN), ("summer", GREEN), ("autumn", AUTUMN), ("winter", AUTUMN)]
    sprites = [render(look, panel, sky=(0xC9, 0xD3, 0xDC)) for _n, look in labels]
    pad, gap, label_h = 18, 14, 26
    w = pad * 2 + sum(s.img.width for s in sprites) + gap * (len(sprites) - 1)
    h = pad * 2 + panel + label_h
    sheet = Image.new("RGB", (w, h), (18, 20, 26))
    dd = ImageDraw.Draw(sheet, "RGBA")
    x = pad
    for (name, _look), s in zip(labels, sprites):
        sheet.paste(s.img, (x, pad))
        dd.text((x + 4, pad + panel + 6), name, fill=(150, 156, 166))
        x += s.img.width + gap
    sheet.save(out)
    print(f"wrote {out} ({sheet.width}x{sheet.height})")


if __name__ == "__main__":
    main()
