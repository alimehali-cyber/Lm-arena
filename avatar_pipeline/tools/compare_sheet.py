#!/usr/bin/env python3
"""
Overlay an orthographic render on the reference sheet and report the differences.

Both images are reduced to silhouettes, resampled to the sheet's own pixel grid, and
compared row by row. That turns "it looks off" into numbers: at this row the model is
12 px too wide on the left, at that row 30 px too low.

  .venv/bin/python tools/compare_sheet.py --render renders/profz/ortho/ortho_front.png \
      --view front --out /tmp/overlay.png
"""
import argparse, json, pathlib
import cv2
import numpy as np

SHEET = pathlib.Path(__file__).resolve().parents[1] / "catalogue/profz_sheet_v2.png"
VIEWS = {"front": (183, 449), "side": (657, 852), "back": (1102, 1338)}
TOP, BOTTOM = 168, 964


def sheet_mask(view):
    """The reference silhouette for one view of the turnaround sheet.

    `catalogue/` is gitignored (the art is the user's), so a fresh checkout has no sheet.
    Fail with a sentence instead of a traceback, and fall back to deriving the mask from
    the paper-white background when `_mask.png` is absent - the sheet is drawn on a flat
    near-white card, so a luminance threshold recovers it.
    """
    im = cv2.imread(str(SHEET))
    if im is None:
        raise SystemExit(
            f"reference sheet not found: {SHEET}\n"
            "catalogue/ is not committed - drop the turnaround sheet (and optionally "
            "_mask.png) back into avatar_pipeline/catalogue/ to run this comparison.")
    mask_path = SHEET.parent / "_mask.png"
    m = cv2.imread(str(mask_path), cv2.IMREAD_GRAYSCALE)
    if m is None:
        gray = cv2.cvtColor(im, cv2.COLOR_BGR2GRAY)
        m = ((gray < 238) * 255).astype(np.uint8)
        print(f"[compare] {mask_path.name} missing - derived the mask from the background")
    x0, x1 = VIEWS[view]
    sub = (m[TOP:BOTTOM, x0:x1] > 0).astype(np.uint8)
    return im[TOP:BOTTOM, x0:x1], sub


def render_mask(path):
    im = cv2.imread(path, cv2.IMREAD_UNCHANGED)
    if im.shape[2] == 4:
        return im[:, :, :3], (im[:, :, 3] > 12).astype(np.uint8)
    return im, (cv2.cvtColor(im, cv2.COLOR_BGR2GRAY) < 235).astype(np.uint8)


def profile(mask, step=8):
    rows = {}
    for y in range(mask.shape[0]):
        xs = np.where(mask[y])[0]
        if len(xs):
            rows[y] = (int(xs.min()), int(xs.max()))
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--render", required=True)
    ap.add_argument("--view", default="front")
    ap.add_argument("--out", required=True)
    ap.add_argument("--report")
    a = ap.parse_args()

    ref_rgb, ref = sheet_mask(a.view)
    ren_rgb, ren = render_mask(a.render)

    # scale the render so its figure height matches the sheet's
    ys, xs = np.where(ren > 0)
    rh = ys.max() - ys.min() + 1
    rw = xs.max() - xs.min() + 1
    sy = ref.shape[0] / rh
    sx = ref.shape[1] / rw
    ren2 = cv2.resize(ren, (int(round(ren.shape[1] * sy)), int(round(ren.shape[0] * sy))),
                      interpolation=cv2.INTER_NEAREST)
    ys2, xs2 = np.where(ren2 > 0)
    # Best-fit alignment. Centring on bounding boxes is fooled by one stray pixel or by
    # a limb the sheet draws a few pixels further out, and then every row reports a
    # constant bias. Search a small window for the shift that maximises overlap.
    ys_ref, _ = np.where(ref > 0)
    best = None
    for oy in range(-8, 9):
        for ox in range(-30, 31):
            cand = np.zeros_like(ref)
            y0 = int(ys_ref.min()) + oy
            x0 = (int(xs2.min()) + int(xs2.max())) // 2 - ren2.shape[1] // 2 + ox
            for yy in range(0, ren2.shape[0]):
                ty2 = y0 + yy
                if not (0 <= ty2 < cand.shape[0]):
                    continue
                a0 = max(0, -x0)
                b0 = min(ren2.shape[1], cand.shape[1] - x0)
                if b0 <= a0:
                    continue
                cand[ty2, x0 + a0:x0 + b0] = np.maximum(cand[ty2, x0 + a0:x0 + b0],
                                                       ren2[yy, a0:b0])
            score = int(((cand > 0) & (ref > 0)).sum())
            if best is None or score > best[0]:
                best = (score, ox, oy, cand)
    _, best_ox, best_oy, canvas = best

    inter = int(((canvas > 0) & (ref > 0)).sum())
    union = int(((canvas > 0) | (ref > 0)).sum())
    iou = inter / max(1, union)
    over = (ref > 0) & (canvas == 0)
    under = (canvas > 0) & (ref == 0)

    # a readable overlay: reference in green, render in magenta, agreement white
    ov = np.zeros((*ref.shape, 3), np.uint8)
    ov[(ref > 0) & (canvas == 0)] = (0, 190, 0)
    ov[(canvas > 0) & (ref == 0)] = (210, 0, 210)
    ov[(ref > 0) & (canvas > 0)] = (235, 235, 235)
    cv2.imwrite(a.out, ov)

    rp, cp = profile(ref), profile(canvas)
    rows = []
    for y in range(0, ref.shape[0], 8):
        if y in rp and y in cp:
            rows.append({"y": y, "sheet_frac": round(y / ref.shape[0], 3),
                         "ref": list(rp[y]), "model": list(cp[y]),
                         "dw_left": cp[y][0] - rp[y][0], "dw_right": cp[y][1] - rp[y][1]})
    worst = sorted(rows, key=lambda r: -(abs(r["dw_left"]) + abs(r["dw_right"])))[:10]
    rep = {"iou": round(iou, 4), "shift_px": [best_ox, best_oy], "missing_px": int(over.sum()), "extra_px": int(under.sum()),
           "worst_rows": worst}
    print(json.dumps(rep, indent=2))
    if a.report:
        pathlib.Path(a.report).write_text(json.dumps(rep, indent=2))


if __name__ == "__main__":
    main()
