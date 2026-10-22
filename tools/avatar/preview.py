"""Software preview: skin the mesh, rasterise it, and lay out a contact sheet.

No GPU, no OpenGL: a small z-buffer rasteriser in numpy good enough to judge a silhouette, a pose and
a palette. The sheet is the animator's check — did the arm actually reach the brow, is the hand above
the shoulder, does the figure read at 170 px tall.
"""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

from avatar_core import Avatar, BONE_NAMES, inverse_bind_matrices
from motion import CLIPS_BY_NAME, IDENTITY, Clip


# ------------------------------------------------------------------------------------------------
# Skinning
# ------------------------------------------------------------------------------------------------


def bone_world_transforms(avatar: Avatar, pose: dict[str, tuple[float, float, float]]) -> dict[str, np.ndarray]:
    bones = {bone.name: bone for bone in avatar.skeleton}
    world: dict[str, np.ndarray] = {}

    def compute(name: str) -> np.ndarray:
        bone = bones[name]
        parent = bone.parent
        parent_world = compute(parent) if parent else np.eye(4)
        offset = bone.head - (bones[parent].head if parent else np.zeros(3))
        local = np.eye(4)
        local[0:3, 3] = offset
        q = np.asarray(pose.get(name, IDENTITY), dtype=np.float64)
        x, y, z, w = q
        rotation = np.array([
            [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
            [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
            [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)],
        ])
        local[0:3, 0:3] = rotation
        world[name] = parent_world @ local
        return world[name]

    for name in BONE_NAMES:
        compute(name)
    return world


def skin(avatar: Avatar, world: dict[str, np.ndarray]) -> tuple[np.ndarray, np.ndarray]:
    mesh = avatar.mesh
    ibm = inverse_bind_matrices(avatar.skeleton).reshape(len(BONE_NAMES), 4, 4).transpose(0, 2, 1)
    matrices = np.zeros((len(BONE_NAMES), 4, 4))
    for i, name in enumerate(BONE_NAMES):
        matrices[i] = world[name] @ ibm[i]

    positions = np.concatenate([mesh.positions, np.ones((mesh.positions.shape[0], 1))], axis=1)
    normals = mesh.normals

    out_pos = np.zeros((positions.shape[0], 4))
    out_nrm = np.zeros((normals.shape[0], 3))
    for slot in range(4):
        influence = matrices[mesh.joints[:, slot]]
        weight = mesh.weights[:, slot][:, None, None]
        out_pos += weight[:, :, 0] * np.einsum("nij,nj->ni", influence, positions)
        out_nrm += weight[:, :, 0] * np.einsum("nij,nj->ni", influence[:, :3, :3], normals)
    lengths = np.linalg.norm(out_nrm, axis=1, keepdims=True)
    lengths[lengths < 1e-9] = 1.0
    return out_pos[:, :3], out_nrm / lengths


# ------------------------------------------------------------------------------------------------
# Raster
# ------------------------------------------------------------------------------------------------


def _frame(height: int, width: int, horizon: float = 0.70) -> np.ndarray:
    rows = np.linspace(0.0, 1.0, height)[:, None]
    top = np.array([0.043, 0.063, 0.118])
    bottom = np.array([0.086, 0.145, 0.235])
    sky = top + (bottom - top) * (rows / max(horizon, 1e-3))
    image = np.repeat(sky[:, None, :], width, axis=1)
    ground_start = int(height * horizon)
    ground = np.linspace(0.0, 1.0, height - ground_start)[:, None]
    ground_colour = np.array([0.055, 0.086, 0.071]) + np.array([0.012, 0.020, 0.014]) * ground
    image[ground_start:] = np.repeat(ground_colour[:, None, :], width, axis=1)
    return image


def _camera(avatar: Avatar, w: int, h: int, yaw_deg: float):
    target = np.array([0.0, avatar.proportions.h * 0.52, 0.0])
    yaw = np.radians(yaw_deg)
    elevation = np.radians(4.0)
    distance = avatar.proportions.h * 2.05
    eye = target + distance * np.array([
        np.sin(yaw) * np.cos(elevation), np.sin(elevation), np.cos(yaw) * np.cos(elevation)
    ])
    forward = (target - eye) / np.linalg.norm(target - eye)
    right = np.cross(forward, np.array([0.0, 1.0, 0.0]))
    right /= np.linalg.norm(right)
    up = np.cross(right, forward)
    focal = 0.5 * h / np.tan(np.radians(34.0) / 2.0)
    return eye, right, up, forward, focal, distance


def _project(point, eye, right, up, forward, focal, w, h):
    rel = np.asarray(point) - eye
    v = np.array([rel @ right, rel @ up, rel @ forward])
    if v[2] <= 0.05:
        return None
    return (w / 2.0 + v[0] / v[2] * focal, h / 2.0 - v[1] / v[2] * focal)


def render(
    avatar: Avatar,
    clip: Clip,
    time_s: float,
    width: int = 360,
    height: int = 540,
    yaw_deg: float = 18.0,
    supersample: int = 2,
    with_alpha: bool = False,
) -> Image.Image:
    w, h = width * supersample, height * supersample
    pose = clip.pose(min(time_s, clip.length))
    world = bone_world_transforms(avatar, pose)
    positions, normals = skin(avatar, world)

    eye, right, up, forward, focal, distance = _camera(avatar, w, h, yaw_deg)
    # The ground line of the sheet is the row the feet land on — the same relationship the Home scene
    # keeps between its grass line and the observer standing on it.
    feet_px = _project(np.array([0.0, 0.004, 0.0]), eye, right, up, forward, focal, w, h)
    horizon = 0.70 if feet_px is None else min(0.86, max(0.45, feet_px[1] / h))
    colour = _frame(h, w, horizon=horizon)
    depth = np.full((h, w), np.inf)
    rel = positions - eye
    view = np.stack([rel @ right, rel @ up, rel @ forward], axis=1)
    with np.errstate(divide="ignore", invalid="ignore"):
        px = w / 2.0 + view[:, 0] / view[:, 2] * focal
        py = h / 2.0 - view[:, 1] / view[:, 2] * focal
    screen = np.stack([px, py], axis=1)
    view_z = view[:, 2]

    light = np.array([-0.45, 0.75, 0.55])
    light = light / np.linalg.norm(light)
    rim_dir = np.array([0.35, 0.15, -0.92])
    rim_dir = rim_dir / np.linalg.norm(rim_dir)

    mesh = avatar.mesh
    indices = mesh.indices.reshape(-1, 3)
    albedo = mesh.colors

    for tri in indices:
        zs = view_z[tri]
        if np.any(zs <= 0.05):
            continue
        pts = screen[tri]
        x0 = max(int(np.floor(pts[:, 0].min())), 0)
        x1 = min(int(np.ceil(pts[:, 0].max())), w - 1)
        y0 = max(int(np.floor(pts[:, 1].min())), 0)
        y1 = min(int(np.ceil(pts[:, 1].max())), h - 1)
        if x1 < x0 or y1 < y0:
            continue
        ax, ay = pts[0]
        bx, by = pts[1]
        cx, cy = pts[2]
        area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay)
        if abs(area) < 1e-9:
            continue
        xs = np.arange(x0, x1 + 1) + 0.5
        ys = np.arange(y0, y1 + 1) + 0.5
        gx, gy = np.meshgrid(xs, ys)
        w0 = ((bx - gx) * (cy - gy) - (cx - gx) * (by - gy)) / area
        w1 = ((cx - gx) * (ay - gy) - (ax - gx) * (cy - gy)) / area
        w2 = 1.0 - w0 - w1
        inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
        if not inside.any():
            continue
        inv_z = w0 / zs[0] + w1 / zs[1] + w2 / zs[2]
        with np.errstate(divide="ignore"):
            z_value = 1.0 / inv_z
        patch_depth = depth[y0:y1 + 1, x0:x1 + 1]
        closer = inside & (z_value < patch_depth)
        if not closer.any():
            continue

        n = (w0[..., None] * normals[tri[0]] + w1[..., None] * normals[tri[1]] + w2[..., None] * normals[tri[2]])
        n_len = np.linalg.norm(n, axis=2)
        n_len[n_len < 1e-9] = 1.0
        n = n / n_len[..., None]
        base = w0[..., None] * albedo[tri[0]] + w1[..., None] * albedo[tri[1]] + w2[..., None] * albedo[tri[2]]
        lambert = np.clip(n @ light, 0.0, 1.0)
        rim = np.power(np.clip(1.0 - np.abs(n @ rim_dir), 0.0, 1.0), 3.0)
        shading = 0.62 + 0.46 * lambert
        lit = base * shading[..., None] + np.array([0.32, 0.46, 0.72]) * (rim * 0.32)[..., None]
        lit = np.clip(lit, 0.0, 1.0)

        patch_depth[closer] = z_value[closer]
        patch = colour[y0:y1 + 1, x0:x1 + 1]
        patch[closer] = lit[closer]

    # Contact shadow on the grass, drawn after the body so it reads as ground contact.
    if feet_px is not None:
        cx, cy = feet_px
        rx = 0.30 * focal / distance
        ry = 0.055 * focal / distance
        yy, xx = np.mgrid[0:h, 0:w]
        ellipse = ((xx - cx) / max(rx, 1.0)) ** 2 + ((yy - cy) / max(ry, 1.0)) ** 2
        falloff = np.clip(1.0 - ellipse, 0.0, 1.0)[..., None]
        colour = colour * (1.0 - 0.45 * falloff)

    # Resize colour and coverage separately: Pillow premultiplies alpha on an RGBA resize, which would
    # eat the sky wherever nothing was drawn.
    rgb = Image.fromarray((np.clip(colour, 0.0, 1.0) * 255).astype(np.uint8), mode="RGB")
    coverage = Image.fromarray(np.where(np.isfinite(depth), 255, 0).astype(np.uint8), mode="L")
    if supersample > 1:
        rgb = rgb.resize((width, height), Image.LANCZOS)
        coverage = coverage.resize((width, height), Image.LANCZOS)
    if not with_alpha:
        return rgb
    image = rgb.convert("RGBA")
    image.putalpha(coverage)
    return image


def scene_scale_tile(avatar: Avatar, clip: Clip, time_s: float, width: int = 360, height: int = 540) -> Image.Image:
    """The character at the size it actually appears on Home, on the scene's own sky and ground.

    The observer is 0.19 of the scene tall with its feet at 0.76; Home shows the scene at 56 % of the
    viewport, inside a 320-600 dp band. A 540 px tile stands in for that band.
    """
    band = (np.clip(_frame(height, width, horizon=0.70), 0.0, 1.0) * 255).astype(np.uint8)
    tile = Image.fromarray(band, mode="RGB").convert("RGBA")
    figure_h = int(round(height * 0.19 * 1.9))  # 1.9x: the band is taller than the live scene band
    figure_w = int(round(figure_h * 0.62))
    figure = render(avatar, clip, time_s, width=figure_w, height=figure_h, yaw_deg=16.0, with_alpha=True)
    x = int(round(width * 0.745 - figure.width / 2))
    y = int(round(height * 0.76 - figure.height))
    tile.alpha_composite(figure, (x, y))
    out = tile.convert("RGB")
    draw = ImageDraw.Draw(out)
    draw.rectangle([0, height - 22, width, height], fill=(8, 12, 20))
    draw.text((8, height - 17), f"at Home size ({figure_h} px tall)", fill=(226, 236, 248))
    return out


def contact_sheet(avatar: Avatar, path: Path, frames: tuple[tuple[str, float], ...] | None = None, theme: str = "night") -> Path:
    if frames is None:
        frames = (("idle_breathe", 1.0), ("day_shade", 1.4), ("dusk_phone", 1.3), ("night_point", 1.6))
    tiles = []
    for name, time_s in frames:
        clip = CLIPS_BY_NAME[name]
        image = render(avatar, clip, time_s, yaw_deg=16.0 if name != "night_point" else 26.0)
        draw = ImageDraw.Draw(image)
        draw.rectangle([0, image.height - 22, image.width, image.height], fill=(8, 12, 20))
        draw.text((8, image.height - 17), f"{name}  t={time_s:.1f}s", fill=(226, 236, 248))
        tiles.append(image)

    tiles.append(scene_scale_tile(avatar, CLIPS_BY_NAME["night_point"], 1.6))

    sheet = Image.new("RGB", (sum(t.width for t in tiles) + 8 * (len(tiles) + 1), tiles[0].height + 16), (8, 12, 20))
    x = 8
    for tile in tiles:
        sheet.paste(tile, (x, 8))
        x += tile.width + 8
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    return path


def main(argv: list[str] | None = None) -> int:
    import argparse
    import json

    parser = argparse.ArgumentParser(description="Render a preview sheet from a brief + palette (no build).")
    parser.add_argument("--brief", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)

    from avatar_core import Proportions, build_avatar

    brief = json.loads(args.brief.read_text())
    proportions = Proportions(**{k: v for k, v in brief["proportions"].items() if k in Proportions.__dataclass_fields__})
    avatar = build_avatar(proportions, {k: tuple(v) for k, v in brief["palette"].items()})
    print(contact_sheet(avatar, args.out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
