#!/usr/bin/env python3
"""
Builds the layered splash-screen assets from the single flattened master photograph.

    assets/brand/zig_splash_master.png   (821 x 1915, the frosted-glass Z on crimson velvet)

The splash animates the velvet and the glass Z independently (parallax, focus pull, light sweep),
so the master is separated into layers:

    splash_velvet.webp     the velvet with the Z removed and the hole re-synthesised (1x, lossy)
    splash_z.webp          the glass Z, RGBA cut-out, upscaled 2x with Lanczos + light unsharp mask
    splash_z_rim.webp      alpha-only ring around the Z's bevel (2x) - lets the light sweep "catch"
                           the polished edge separately from the frosted face
    splash_z_shadow.webp   soft blurred silhouette for the contact shadow (1x)

All of them land in app/src/main/res/drawable-nodpi so Android never density-scales them; the
splash decodes them itself at full resolution.

The Z layers share one crop frame, recorded in the generated Kotlin constants file
(SplashArtGeometry.kt) so the code can place the cut-out exactly where it was in the photograph.

Usage:  python3 tools/splash/build_splash_assets.py         (needs pillow, numpy, scipy)
"""
from __future__ import annotations

import pathlib

import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as ndi

ROOT = pathlib.Path(__file__).resolve().parents[2]
MASTER = ROOT / "assets/brand/zig_splash_master.png"
OUT = ROOT / "app/src/main/res/drawable-nodpi"
KOTLIN = ROOT / "app/src/main/java/com/alijafari/red/astronomy/ui/splash/SplashArtGeometry.kt"

PAD = 40          # crop padding around the Z (master pixels); room for the rim glow + shadow tail
UPSCALE = 2       # the Z is the focal point, so it is shipped at 2x


def smoothstep(e0: float, e1: float, x: np.ndarray) -> np.ndarray:
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def matte(img: np.ndarray) -> np.ndarray:
    """Soft alpha of the glass Z. Velvet is strongly saturated red; the frosted glass is neutral."""
    mx = img.max(-1)
    mn = img.min(-1)
    sat = (mx - mn) / (mx + 1e-6)
    a = smoothstep(0.62, 0.30, sat) * smoothstep(25.0, 55.0, mx)
    hard = a > 0.5
    lab, n = ndi.label(hard)
    sizes = ndi.sum(hard, lab, range(1, n + 1))
    keep = ndi.binary_fill_holes(lab == (int(np.argmax(sizes)) + 1))
    core = ndi.binary_erosion(keep, iterations=3)
    band = ndi.binary_dilation(keep, iterations=4)
    alpha = np.where(core, 1.0, np.where(band, a, 0.0)).astype(np.float32)
    alpha = ndi.gaussian_filter(alpha, 0.6)
    alpha[core] = 1.0
    return alpha


def velvet_plate(img: np.ndarray, alpha: np.ndarray) -> np.ndarray:
    """Remove the Z: diffuse the surrounding low frequencies in, borrow pile texture from above."""
    hole = ndi.binary_dilation(alpha > 0.02, iterations=12)
    known = (~hole).astype(np.float32)

    def norm_blur(sigma: float):
        num = np.stack([ndi.gaussian_filter(img[..., c] * known, sigma) for c in range(3)], -1)
        den = ndi.gaussian_filter(known, sigma)[..., None]
        return num / np.maximum(den, 1e-4), den

    low, weight = norm_blur(25)
    for sigma in (45, 80):
        coarse, den = norm_blur(sigma)
        w = np.clip(weight * 4.0, 0.0, 1.0)
        low = low * w + coarse * (1.0 - w)
        weight = np.maximum(weight, den)

    donor = np.roll(img, 520, axis=0)  # donor[y] = img[y - 520]: velvet from above the Z
    d_fine = np.stack([ndi.gaussian_filter(donor[..., c], 6) for c in range(3)], -1)
    d_mid = np.stack([ndi.gaussian_filter(donor[..., c], 25) for c in range(3)], -1)
    gain = np.clip((low.mean(-1, keepdims=True) + 4.0) / (d_fine.mean(-1, keepdims=True) + 4.0), 0.5, 2.0)
    synth = np.clip(low + (donor - d_fine) * gain + (d_fine - d_mid) * gain, 0.0, 255.0)

    feather = np.clip(ndi.gaussian_filter(hole.astype(np.float32), 6)[..., None] * 1.3, 0.0, 1.0)
    return img * (1.0 - feather) + synth * feather


def main() -> None:
    src = Image.open(MASTER).convert("RGB")
    img = np.asarray(src).astype(np.float32)
    h, w = img.shape[:2]
    alpha = matte(img)

    ys, xs = np.where(alpha > 0.01)
    x0, x1 = int(xs.min()) - PAD, int(xs.max()) + PAD + 1
    y0, y1 = int(ys.min()) - PAD, int(ys.max()) + PAD + 1
    cw, ch = x1 - x0, y1 - y0

    # --- velvet plate ---------------------------------------------------------------------
    plate = velvet_plate(img, alpha).astype(np.uint8)
    Image.fromarray(plate).save(OUT / "splash_velvet.webp", quality=93, method=6)
    edge_mean = np.concatenate([plate[:40].reshape(-1, 3), plate[-40:].reshape(-1, 3)]).mean(0)
    overall = plate.reshape(-1, 3).mean(0)

    # --- glass Z (2x) ----------------------------------------------------------------------
    rgb = Image.fromarray(img[y0:y1, x0:x1].astype(np.uint8))
    a8 = Image.fromarray((alpha[y0:y1, x0:x1] * 255).astype(np.uint8))
    big = (cw * UPSCALE, ch * UPSCALE)
    rgb2 = rgb.resize(big, Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=1.6, percent=55, threshold=1))
    a2 = a8.resize(big, Image.LANCZOS)
    z = rgb2.copy()
    z.putalpha(a2)
    z.save(OUT / "splash_z.webp", lossless=True, quality=100, method=6)

    # --- bevel rim (2x, alpha only) --------------------------------------------------------
    hard = alpha[y0:y1, x0:x1] > 0.5
    inner = ndi.binary_erosion(hard, iterations=7)
    ring = alpha[y0:y1, x0:x1] * (1.0 - ndi.gaussian_filter(inner.astype(np.float32), 1.5))
    ring = np.clip(ring * 1.15, 0.0, 1.0)
    rim = Image.fromarray((ring * 255).astype(np.uint8)).resize(big, Image.LANCZOS)
    rim_rgba = Image.new("RGBA", big, (255, 255, 255, 0))
    rim_rgba.putalpha(rim)
    rim_rgba.save(OUT / "splash_z_rim.webp", lossless=True, quality=100, method=6)

    # --- soft shadow (1x) ------------------------------------------------------------------
    sh = ndi.gaussian_filter(alpha[y0:y1, x0:x1], 13.0)
    sh = np.clip(sh / max(sh.max(), 1e-6), 0.0, 1.0)
    shadow = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    shadow.putalpha(Image.fromarray((sh * 255).astype(np.uint8)))
    shadow.save(OUT / "splash_z_shadow.webp", lossless=True, quality=100, method=6)

    # bevel top-right corner for the specular sparkle: the right-most opaque pixel of the top row
    top = int(ys.min())
    row = np.where(alpha[top + 18] > 0.5)[0]
    spark_x, spark_y = (row.max() - 14 - x0) / cw, (top + 14 - y0) / ch

    to_hex = lambda c: "0xFF%02X%02X%02X" % tuple(int(round(v)) for v in c)
    KOTLIN.write_text(
        f"""package com.alijafari.red.astronomy.ui.splash

// GENERATED by tools/splash/build_splash_assets.py - do not edit by hand.

/** Where the glass-Z cut-out sat inside the master photograph, so the layers re-assemble exactly. */
internal object SplashArtGeometry {{
    /** Master photograph size, px. */
    const val MASTER_WIDTH = {w}f
    const val MASTER_HEIGHT = {h}f

    /** The Z crop frame inside the master, px (shared by the Z, rim and shadow layers). */
    const val Z_FRAME_LEFT = {x0}f
    const val Z_FRAME_TOP = {y0}f
    const val Z_FRAME_WIDTH = {cw}f
    const val Z_FRAME_HEIGHT = {ch}f

    /** The visible glass inside that frame (frame minus padding), px. */
    const val Z_GLASS_WIDTH = {cw - 2 * PAD}f
    const val Z_GLASS_HEIGHT = {ch - 2 * PAD}f
    const val Z_FRAME_PAD = {PAD}f

    /** Top-right bevel corner, as a fraction of the frame - where the closing sparkle lands. */
    const val SPARKLE_X = {spark_x:.4f}f
    const val SPARKLE_Y = {spark_y:.4f}f

    /** Mean velvet colours - the window/system-splash colour hands off to the animation from these. */
    const val VELVET_EDGE_ARGB = {to_hex(edge_mean)}
    const val VELVET_MEAN_ARGB = {to_hex(overall)}
}}
"""
    )
    print("frame", (x0, y0, cw, ch), "edge", to_hex(edge_mean), "mean", to_hex(overall),
          "sparkle", (round(spark_x, 4), round(spark_y, 4)))
    for f in ("splash_velvet.webp", "splash_z.webp", "splash_z_rim.webp", "splash_z_shadow.webp"):
        print(f, (OUT / f).stat().st_size // 1024, "KB")


if __name__ == "__main__":
    main()
