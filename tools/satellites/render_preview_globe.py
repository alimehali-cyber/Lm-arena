#!/usr/bin/env python3
"""Layoutlib has no native Earth EGL surface. Bake two stills from the app's actual maps.

    python -m pip install Pillow numpy
    python tools/satellites/render_preview_globe.py

Both resources are debug-only; the installed release renders Earth3D, not these images.
"""
from pathlib import Path
import math
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app/src/main/assets'
OUT = ROOT / 'app/src/debug/res/drawable-nodpi'
SIZE = 600


def texture(name, lon, lat):
    image = np.asarray(Image.open(ASSETS / name).convert('RGB'), dtype=np.float32) / 255.0
    h, w = image.shape[:2]
    px = (lon / (2 * math.pi) + .5) * w
    py = (.5 - lat / math.pi) * (h - 1)
    xx, yy = (px.astype(int) % w, py.astype(int).clip(0, h - 1))
    ix, iy = (px % 1)[..., None], (py % 1)[..., None]
    nx, ny = (xx + 1) % w, (yy + 1).clip(0, h - 1)
    return (image[yy, xx] * (1 - ix) + image[yy, nx] * ix) * (1 - iy) + (
        image[ny, xx] * (1 - ix) + image[ny, nx] * ix) * iy


def main():
    grid = (np.arange(SIZE) + .5 - SIZE / 2) / (SIZE / 2 - 2)
    x, y = np.meshgrid(grid, -grid)
    r = np.sqrt(x ** 2 + y ** 2)
    z = np.sqrt(np.maximum(0, 1 - r ** 2))
    theta, phi = math.radians(24), math.radians(13)
    lon = np.arctan2(x, z * math.cos(theta) - y * math.sin(theta)) + phi
    lat = np.arcsin((y * math.cos(theta) + z * math.sin(theta)).clip(-1, 1))
    sun = -x * .70 + y * .49 + z * .52
    lit = ((sun + .09) / 1.09).clip(0, 1)
    day = texture('earth_day_4k.jpg', lon, lat)
    clouds = texture('earth_clouds.jpg', lon, lat).mean(axis=2) ** 1.6
    cities = texture('earth_night_2k.jpg', lon, lat)
    haze = (1 - z).clip(0, 1) ** 4
    grid_lat = 1 - (np.abs(np.sin(lat * 12)) * 18).clip(0, 1)
    grid_lon = 1 - (np.abs(np.sin(lon * 12)) * 15).clip(0, 1)
    graticule = np.maximum(grid_lat, grid_lon) * z * .08
    equator = (1 - np.abs(lat) * 295).clip(0, 1) * z * .18
    for name, ambient in [('night', .095), ('day', .31)]:
        exposure = (ambient + (1.0 - ambient) * np.sqrt(lit))[..., None]
        surface = day * exposure * 1.10
        surface += (clouds * (ambient * .75 + .44 * lit))[..., None] * np.array([.83, .93, 1.])
        surface += (cities * ((1 - lit) ** 3)[..., None]) * .75
        surface += haze[..., None] * np.array([.09, .21, .36]) * (.35 + lit[..., None] * .75)
        surface += graticule[..., None] + equator[..., None] * np.array([.30, .68, .87])
        alpha = ((1 - r) * (SIZE / 2) + .5).clip(0, 1)
        rgba = (np.dstack((surface.clip(0, 1), alpha)) * 255).astype('uint8')
        Image.fromarray(rgba).save(OUT / f'satellite_preview_{name}.webp', quality=88, method=6)


if __name__ == '__main__':
    main()
