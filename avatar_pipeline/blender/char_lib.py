#!/usr/bin/env python3
"""
Primitives for authored characters: egg lofts, swept tubes, superellipsoids, tori,
arcs, and a radial sculpt pass.

Why a separate module
    `build_character.py` grew around one generic body built from spheres and lofts. A
    designed character needs shapes that primitive cannot express: a head with a jaw,
    eye sockets and a brow ridge is a *sculpted* egg, not a sphere with blobs stuck on
    it; a limb with a deltoid, an elbow and a wrist is a swept tube with a radius curve,
    not three cylinders. Mixing those into the generic builder would put them on the
    photo path too, where they are not wanted.

Everything here is deterministic: the only randomness takes a seed.
"""
from __future__ import annotations

import math

import bmesh
import bpy
from mathutils import Vector


# --------------------------------------------------------------------------- materials
def material(name, hex_colour, rough=0.55, metal=0.0, emission=None, emission_strength=0.0):
    m = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    r, g, b = (int(hex_colour[i:i + 2], 16) / 255.0 for i in (1, 3, 5))
    lin = tuple(c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in (r, g, b))
    bsdf.inputs["Base Color"].default_value = (*lin, 1.0)
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = metal
    if emission and emission_strength > 0:
        er, eg, eb = (int(emission[i:i + 2], 16) / 255.0 for i in (1, 3, 5))
        el = tuple(c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in (er, eg, eb))
        bsdf.inputs["Emission Color"].default_value = (*el, 1.0)
        bsdf.inputs["Emission Strength"].default_value = emission_strength
    m.diffuse_color = (r, g, b, 1.0)
    return m


def apply_material(obj, mat):
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    for p in obj.data.polygons:
        p.material_index = 0
    return obj


# --------------------------------------------------------------------------- helpers
def _new_object(name, bm):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(ob)
    return ob


def _fan_cap(bm, loop, centre, flip):
    c = bm.verts.new(centre)
    n = len(loop)
    for i in range(n):
        j = (i + 1) % n
        tri = (loop[i], loop[j], c) if flip else (loop[j], loop[i], c)
        try:
            bm.faces.new(tri)
        except ValueError:
            pass


def catmull(points, samples_per_segment=6):
    """Sample a Catmull-Rom spline through `points` (list of tuples)."""
    pts = [Vector(p) for p in points]
    if len(pts) < 3:
        return pts
    ext = [pts[0] * 2 - pts[1]] + pts + [pts[-1] * 2 - pts[-2]]
    out = []
    for i in range(len(pts) - 1):
        p0, p1, p2, p3 = ext[i], ext[i + 1], ext[i + 2], ext[i + 3]
        for s in range(samples_per_segment):
            t = s / samples_per_segment
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t
                              + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(pts[-1])
    return out


# --------------------------------------------------------------------------- shapes
def egg_loft(name, rings, ring=40, cap=True, close_top=None, samples=1):
    """
    Loft elliptical rings that may have DIFFERENT front and back radii.

    rings: [(z, half_width, half_depth_front, half_depth_back, y_offset), ...]
    bottom to top. -Y is the character's front, so half_depth_front applies to -Y.

    A plain ellipse cannot give a head a flat face and a round occiput at once, which is
    exactly the difference between a mannequin head and a head.
    """
    bm = bmesh.new()
    loops = []
    for (z, rx, ryf, ryb, yoff) in rings:
        loop = []
        for i in range(ring):
            a = 2.0 * math.pi * i / ring
            s = math.sin(a)
            ry = ryb if s > 0 else ryf
            loop.append(bm.verts.new((rx * math.cos(a), ry * s + yoff, z)))
        loops.append(loop)
    for ra, rb in zip(loops, loops[1:]):
        for i in range(ring):
            j = (i + 1) % ring
            try:
                bm.faces.new((ra[i], ra[j], rb[j], rb[i]))
            except ValueError:
                pass
    if cap:
        z0 = rings[0][0]
        z1 = rings[-1][0]
        _fan_cap(bm, loops[0], (0, rings[0][4], z0), True)
        _fan_cap(bm, loops[-1], (0, rings[-1][4], z1), False)
    bm.normal_update()
    return _new_object(name, bm)


def tube(name, points, radii, ring=18, cap=True, samples=5, squash=None):
    """
    Sweep a circular cross-section along a polyline.

    points: control points, radii: radius at each, squash: optional (sx, sy) per point
    scaling the cross-section in the frame's local axes. Rings are built in a frame that
    is transported along the curve, so a bent arm does not pinch at the elbow.
    """
    pts = catmull(points, samples)
    # radius along the curve: linear interpolate the control radii over arc length
    n_ctrl = len(points)
    ctrl = [Vector(p) for p in points]
    seglen = [(ctrl[i + 1] - ctrl[i]).length for i in range(n_ctrl - 1)]
    total = sum(seglen) or 1.0
    cum = [0.0]
    for s in seglen:
        cum.append(cum[-1] + s)
    rr, sq = [], []
    for p in pts:
        # nearest control arc position
        best, best_d = 0, 1e18
        for i in range(n_ctrl):
            d = (p - ctrl[i]).length_squared
            if d < best_d:
                best, best_d = i, d
        rr.append(radii[best])
        if squash:
            sq.append(squash[best])
        else:
            sq.append((1.0, 1.0))

    bm = bmesh.new()
    loops = []
    prev_u = None
    for i, p in enumerate(pts):
        if i == 0:
            t = (pts[1] - pts[0]).normalized()
        elif i == len(pts) - 1:
            t = (pts[-1] - pts[-2]).normalized()
        else:
            t = (pts[i + 1] - pts[i - 1]).normalized()
        if prev_u is None:
            ref = Vector((0, 0, 1)) if abs(t.dot(Vector((0, 0, 1)))) < 0.9 else Vector((0, 1, 0))
            u = (ref - t * ref.dot(t)).normalized()
        else:
            u = (prev_u - t * prev_u.dot(t))
            u = u.normalized() if u.length > 1e-6 else Vector((1, 0, 0))
        v = t.cross(u).normalized()
        prev_u = u
        loop = []
        sx, sy = sq[i]
        for k in range(ring):
            a = 2.0 * math.pi * k / ring
            off = u * (rr[i] * sx * math.cos(a)) + v * (rr[i] * sy * math.sin(a))
            loop.append(bm.verts.new(p + off))
        loops.append(loop)
    for ra, rb in zip(loops, loops[1:]):
        for k in range(ring):
            j = (k + 1) % ring
            try:
                bm.faces.new((ra[k], ra[j], rb[j], rb[k]))
            except ValueError:
                pass
    if cap:
        _fan_cap(bm, loops[0], pts[0], True)
        _fan_cap(bm, loops[-1], pts[-1], False)
    bm.normal_update()
    return _new_object(name, bm)


def superellipsoid(name, loc, scale, n=3.2, seg=24, rings=16, rot=None):
    """Rounded box: |x|^n + |y|^n + |z|^n = 1, scaled. Great for soles and fabric panels."""
    bm = bmesh.new()
    rows = []
    for i in range(rings + 1):
        phi = math.pi * i / rings - math.pi / 2
        cz = math.sin(phi)
        cr = math.cos(phi)
        e = 2.0 / n
        z = math.copysign(abs(cz) ** e, cz)
        r = abs(cr) ** e
        row = []
        for k in range(seg):
            a = 2.0 * math.pi * k / seg
            cx, cy = math.cos(a), math.sin(a)
            x = math.copysign(abs(cx) ** e, cx) * r
            y = math.copysign(abs(cy) ** e, cy) * r
            row.append(bm.verts.new((x * scale[0], y * scale[1], z * scale[2])))
        rows.append(row)
    for i in range(rings):
        for k in range(seg):
            j = (k + 1) % seg
            v = [rows[i][k], rows[i][j], rows[i + 1][j], rows[i + 1][k]]
            try:
                bm.faces.new(v)
            except ValueError:
                pass
    # poles are degenerate rows; weld them
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-5)
    bm.normal_update()
    ob = _new_object(name, bm)
    ob.location = loc
    if rot:
        ob.rotation_euler = rot
    return ob


def torus(name, loc, R, r, rot=(0, 0, 0), major=24, minor=10, arc=2 * math.pi):
    bm = bmesh.new()
    closed = abs(arc - 2 * math.pi) < 1e-6
    n_major = major if closed else major + 1
    rows = []
    for i in range(n_major):
        a = arc * i / major
        ca, sa = math.cos(a), math.sin(a)
        row = []
        for k in range(minor):
            b = 2 * math.pi * k / minor
            rr = R + r * math.cos(b)
            row.append(bm.verts.new((rr * ca, rr * sa, r * math.sin(b))))
        rows.append(row)
    for i in range(n_major - 1 if not closed else n_major):
        ra = rows[i]
        rb = rows[(i + 1) % n_major]
        for k in range(minor):
            j = (k + 1) % minor
            try:
                bm.faces.new((ra[k], ra[j], rb[j], rb[k]))
            except ValueError:
                pass
    if not closed:
        for row, flip in ((rows[0], True), (rows[-1], False)):
            c = sum((v.co for v in row), Vector()) / len(row)
            _fan_cap(bm, row, c, flip)
    bm.normal_update()
    ob = _new_object(name, bm)
    ob.location = loc
    ob.rotation_euler = rot
    return ob


def sphere(name, loc, scale, seg=24, ring=16, rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=ring, radius=1.0, location=loc)
    o = bpy.context.object
    o.name = name
    o.scale = scale
    o.rotation_euler = rot
    bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
    return o


def cylinder(name, p0, p1, r0, r1=None, verts=16):
    p0, p1 = Vector(p0), Vector(p1)
    d = p1 - p0
    bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r0,
                                    radius2=(r0 if r1 is None else r1),
                                    depth=max(1e-4, d.length), location=(p0 + p1) / 2)
    o = bpy.context.object
    o.name = name
    o.rotation_mode = "QUATERNION"
    o.rotation_quaternion = d.to_track_quat("Z", "Y")
    return o


# --------------------------------------------------------------------------- sculpt
def sculpt(obj, fields, centre=None):
    """
    Radial displacement on a closed surface, applied before the object is joined.

    fields: [(x, y, z, sigma, amount[, axis]), ...]. Each field pushes vertices near its
    centre outward (positive) or inward (negative) along the direction from `centre` (or
    along `axis`, e.g. (0,-1,0) for a brow ridge on a face). Gaussian falloff, so the
    features blend into one surface instead of intersecting as separate blobs.
    """
    if centre is None:
        centre = Vector((0, 0, 0))
    for v in obj.data.vertices:
        p = v.co
        total = Vector((0, 0, 0))
        for f in fields:
            c = Vector(f[:3])
            sigma, amount = f[3], f[4]
            d = (p - c).length
            if d > sigma * 3.0:
                continue
            w = math.exp(-(d / sigma) ** 2)
            if len(f) > 5 and f[5] is not None:
                direction = Vector(f[5]).normalized()
            else:
                direction = (p - centre)
                direction = direction.normalized() if direction.length > 1e-6 else Vector((0, 0, 1))
            total += direction * (amount * w)
        v.co = p + total
    obj.data.update()
    return obj


def coil(name, center, normal, radius=0.009, thick=0.0044, turns=1.15, phase=0.0,
         ring=5, rise=0.0016):
    """
    A short spiral of tube - one curl of hair.

    A torus reads as a ring with a hole whenever its plane faces the camera, and hair
    made of 900 of them looked like a pile of doughnuts. A coil of ~1.2 turns, lying
    tangent to the scalp, reads as a curl from every angle and ends in a visible tip.
    """
    n = Vector(normal)
    n.normalize()
    ref = Vector((0, 0, 1)) if abs(n.z) < 0.9 else Vector((1, 0, 0))
    u = (ref - n * ref.dot(n)).normalized()
    v = n.cross(u).normalized()
    pts, rr = [], []
    steps = 6
    for i in range(steps + 1):
        t = i / steps
        a = phase + turns * 2.0 * math.pi * t
        rad = radius * (1.0 - 0.20 * t)
        pts.append(Vector(center) + (u * math.cos(a) + v * math.sin(a)) * rad
                   + n * (rise * t))
        rr.append(thick * (0.55 + 0.45 * math.sin(math.pi * min(1.0, t * 0.85 + 0.15))))
    return tube(name, pts, rr, ring=ring, samples=2)


def conformal_patch(name, curve_fn, height_fn, out_fn, u_steps=26, v_steps=8):
    """
    A lens-shaped patch laid on a curved surface.

    curve_fn(u) -> the centre point of the patch on that surface, u in [-1, 1]
    height_fn(u) -> half height of the patch there (0 at the ends makes it a lens)
    out_fn(u, v) -> how far to lift the point off the surface

    Used for the mouth and the eyelids: a tube following a curve on a face breaks into
    pieces at the corners, because the surface turns away faster than the tube does. A
    patch follows the surface exactly, edge to edge, and tapers to nothing.
    """
    bm = bmesh.new()
    rows = []
    for j in range(v_steps + 1):
        v = -1.0 + 2.0 * j / v_steps
        row = []
        for i in range(u_steps + 1):
            u = -1.0 + 2.0 * i / u_steps
            c = curve_fn(u)
            h = height_fn(u)
            z = c.z + v * h
            lift = out_fn(u, v) * (1.0 - v * v) ** 0.6
            row.append(bm.verts.new((c.x, c.y - lift, z)))
        rows.append(row)
    for j in range(v_steps):
        for i in range(u_steps):
            try:
                bm.faces.new((rows[j][i], rows[j][i + 1], rows[j + 1][i + 1], rows[j + 1][i]))
            except ValueError:
                pass
    bm.normal_update()
    return _new_object(name, bm)


def shade_smooth(obj):
    for p in obj.data.polygons:
        p.use_smooth = True
    return obj
