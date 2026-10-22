#!/usr/bin/env python3
"""
Stage 1 - read a photograph and produce the numbers the character is built from.

What this actually does (no learned model, no network, no upload):
  * detects a face with OpenCV's Haar cascade (ships with the wheel)
  * samples ten regions of the image for representative colours, using a small
    k-means so a busy background does not drag the skin tone around
  * converts the face box into body proportions in head-units, which is the oldest
    reliable way to get a believable figure from a single photo
  * records explicit `limitations` so nothing downstream can pretend the result is a
    reconstruction of the person

What it deliberately does not do: claim identity. A single 2D photograph does not
contain enough information to rebuild a specific face, and this pipeline does not try.

Usage:
  python tools/analyze_photo.py --image photo.jpg --out working/x/01_analysis.json
"""
from __future__ import annotations
import argparse
import json
import pathlib
import sys

import cv2
import numpy as np


def kmeans_colour(pixels: np.ndarray, k: int = 3) -> tuple[str, list[str]]:
    """Dominant colour as #rrggbb, plus the cluster centres for inspection."""
    data = pixels.reshape(-1, 3).astype(np.float32)
    if len(data) > 4000:
        idx = np.linspace(0, len(data) - 1, 4000).astype(int)
        data = data[idx]
    if len(data) < k:
        k = max(1, len(data))
    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 20, 1.0)
    _compact, labels, centres = cv2.kmeans(data, k, None, criteria, 3, cv2.KMEANS_PP_CENTERS)
    counts = np.bincount(labels.flatten(), minlength=len(centres))
    order = np.argsort(-counts)
    hexes = []
    for i in order:
        b, g, r = centres[i]
        hexes.append("#%02X%02X%02X" % (int(r), int(g), int(b)))
    return hexes[0], hexes


def region(img, x0, y0, x1, y1, k=3, mask=None, min_pixels=40):
    """
    Dominant colour of a box, optionally restricted to the character's pixels.

    Without the mask, a box that overhangs the figure samples the background: on concept
    art with a flat field the crown box returned #F5F5F5 and the character got white hair.
    Returns (colour, clusters, fraction_of_box_inside_the_figure).
    """
    h, w = img.shape[:2]
    x0, x1 = max(0, int(x0)), min(w, int(x1))
    y0, y1 = max(0, int(y0)), min(h, int(y1))
    if x1 - x0 < 2 or y1 - y0 < 2:
        return None, [], 0.0
    patch = img[y0:y1, x0:x1]
    if mask is not None:
        m = mask[y0:y1, x0:x1]
        frac = float(m.mean()) if m.size else 0.0
        if int(m.sum()) >= min_pixels:
            hexcol, clusters = kmeans_colour(patch[m > 0], k=k)
            return hexcol, clusters, frac
        # too few character pixels to be meaningful; report nothing rather than the
        # background, and let the caller fall back
        return None, [], frac
    hexcol, clusters = kmeans_colour(patch, k=k)
    return hexcol, clusters, 1.0


def detect_face(gray):
    cascades = [
        "haarcascade_frontalface_default.xml",
        "haarcascade_frontalface_alt2.xml",
        "haarcascade_profileface.xml",
    ]
    best = None
    for name in cascades:
        path = cv2.data.haarcascades + name
        if not pathlib.Path(path).exists():
            continue
        c = cv2.CascadeClassifier(path)
        faces = c.detectMultiScale(gray, scaleFactor=1.08, minNeighbors=5,
                                   minSize=(int(min(gray.shape[:2]) * 0.12),) * 2)
        if len(faces):
            f = max(faces, key=lambda r: r[2] * r[3])
            if best is None or f[2] * f[3] > best[2] * best[3]:
                best = tuple(int(v) for v in f)
    return best


def foreground_mask(img_raw, img):
    """
    Best-effort separation of the character from its background.

    Concept art is usually a figure on a flat (often white) field, or a PNG with alpha.
    A photograph has neither, so this returns (None, "none", None) and the caller falls
    back to the cascade. Nothing here is allowed to guess silently: the source of the
    mask is reported in the analysis output.
    """
    h, w = img.shape[:2]
    if img_raw is not None and img_raw.ndim == 3 and img_raw.shape[2] == 4:
        alpha = img_raw[:, :, 3]
        if alpha.min() < 250:                      # real transparency, not a solid channel
            return (alpha > 127).astype(np.uint8), "alpha", None

    border = np.concatenate([
        img[0:3].reshape(-1, 3), img[max(0, h - 3):h].reshape(-1, 3),
        img[:, 0:3].reshape(-1, 3), img[:, max(0, w - 3):w].reshape(-1, 3),
    ])
    # A photograph has a lit, uneven background: the border varies a lot, the median is
    # not the background, and segmenting on it produces a blob that covers the whole frame
    # (this happened, and the blob then "rejected" a perfectly good face box). Only trust
    # the background model when the border is actually uniform.
    if float(np.median(border.std(axis=0))) > 12.0:
        return None, "uneven_background", None
    bg = np.median(border, axis=0)
    dist = np.linalg.norm(img.astype(np.float32) - bg.astype(np.float32), axis=2)
    d8 = np.clip(dist, 0, 255).astype(np.uint8)
    if float(d8.max()) < 12.0:
        return None, "none", bg                # flat image: nothing to segment
    level, _ = cv2.threshold(d8, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    # Otsu is dominated by the biggest area, so on a dark-clothed figure it cuts off the
    # pale face. With a uniform background anything above sensor noise is foreground, so
    # bias the threshold well below Otsu.
    mask = (d8 > max(18.0, 0.40 * float(level))).astype(np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((7, 7), np.uint8))
    # keep the largest connected blob: stray text, logos and props are not the character
    n, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    if n <= 1:
        return None, "none", bg
    biggest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    keep = (labels == biggest).astype(np.uint8)
    if keep.sum() < 0.02 * h * w:
        return None, "none", bg
    return keep, "background", bg


def figure_metrics(mask):
    """
    Silhouette measurements that hold for both photographs and drawn characters.

    Only shape is used, never colour: skin tone is meaningless on a stylised drawing, but
    a 5-head-tall cartoon silhouette still measures as 5 heads tall.
    """
    h, w = mask.shape[:2]
    ys, xs = np.where(mask > 0)
    if len(ys) == 0:
        return None
    x0, x1, y0, y1 = int(xs.min()), int(xs.max()), int(ys.min()), int(ys.max())
    fw, fh = max(1, x1 - x0 + 1), max(1, y1 - y0 + 1)
    coverage = float(mask.sum()) / float(h * w)
    rows = mask.sum(axis=1)

    def width_at(frac):
        y = int(y0 + fh * frac)
        y = min(max(y, 0), h - 1)
        band = rows[max(0, y - 2):min(h, y + 3)]
        return float(band.max()) if len(band) else float(rows[y])

    # A standing figure is much taller than it is wide. Requiring it to fill the frame as
    # well rejected ordinary concept art, which always has margins.
    figure_type = "full_body" if (fh / fw) >= 1.45 and fh >= 0.5 * h else "bust"
    # Measure the skull where it actually is - just below the crown - instead of at a fixed
    # fraction of the height. A fixed 22% lands on the head for an 8-head figure and on the
    # shoulders for a 4-head chibi, which is exactly the stylisation concept art uses.
    # Widest row in the top quarter is the skull (or the hair around it). Looking at a fixed
    # fraction of the height instead lands on the shoulders for a chibi and on the crown for
    # an 8-head figure, which is how a 3-head drawing once measured as 1.76 heads.
    top_band = mask[y0:min(h, y0 + max(4, int(fh * 0.25)))]
    head_w = float(top_band.sum(axis=1).max()) if top_band.size else float(width_at(0.10))
    if figure_type == "full_body":
        head_px = head_w / 1.15      # a drawn head is rounder than a photographic one
        confidence_note = "crown width"
    else:
        head_px = fh * 0.36          # bust crop: head is roughly a third of the blob
        confidence_note = "bust fraction"
    shoulder = width_at(0.22) if figure_type == "full_body" else width_at(0.62)
    head_px = min(max(head_px, fh / 12.0), fh / (1.5 if figure_type == "full_body" else 1.05))
    heads_tall = min(max(fh / head_px, 2.5), 9.0)
    return {
        "bbox": [x0, y0, fw, fh],
        "coverage": round(coverage, 4),
        "figure_type": figure_type,
        "shoulder_px": round(shoulder, 1),
        "head_px_estimate": round(head_px, 1),
        "head_width_estimate": round(head_w, 1),
        "heads_tall_estimate": round(heads_tall, 2),
        "head_estimate_basis": confidence_note,
        "bottom_reaches_frame": bool(y1 >= h - 2),
    }


def face_plausible(face, mask, metrics):
    """
    A Haar face on a photograph is a strong signal; on a drawing it is frequently a false
    positive (a head-and-shoulders shape reads as a face). Trusting it silently produces a
    wrong head size at confidence 0.72, which is the worst possible outcome.

    Returns (ok, reason).
    """
    if face is None:
        return False, "no face"
    fx, fy, fw, fh = face
    if metrics:
        if metrics["figure_type"] == "full_body":
            if fh > 0.40 * metrics["bbox"][3]:
                return False, f"face box is {fh / metrics['bbox'][3]:.0%} of the figure height"
            top = metrics["bbox"][1]
            if fy > top + 0.35 * metrics["bbox"][3]:
                return False, "face box sits in the lower body"
    if mask is not None:
        inside = mask[fy:fy + fh, fx:fx + fw]
        if inside.size == 0 or inside.mean() < 0.50:
            return False, "face box is mostly outside the character"
        # On a full figure the silhouette measures the skull directly, while the cascade
        # assumes realistic human proportions. Stylised art breaks that assumption, so when
        # the two disagree by more than ~30% the direct measurement wins.
        if metrics and metrics["figure_type"] == "full_body":
            head_from_face = fh * 1.35
            head_from_mask = metrics["head_px_estimate"]
            ratio = head_from_face / max(1.0, head_from_mask)
            if not (0.75 <= ratio <= 1.30):
                return False, (f"cascade head {head_from_face:.0f}px vs silhouette "
                               f"{head_from_mask:.0f}px suggests non-human proportions")
    return True, "ok"


def head_box_from_mask(mask, metrics):
    """A head box for a drawn character, approximating the cascade's output."""
    if not metrics:
        return None
    x0, y0, fw, fh = metrics["bbox"]
    head_px = metrics["head_px_estimate"]
    head_w = head_px * 0.78
    cx = x0 + fw * 0.5
    # full body: head sits at the top; bust: centre the head in the upper part of the blob
    cy = y0 + head_px * 0.55 if metrics["figure_type"] == "full_body" else y0 + fh * 0.30
    bx0 = int(max(0, cx - head_w / 2))
    bx1 = int(min(mask.shape[1], cx + head_w / 2))
    by0 = int(max(0, cy - head_px / 2))
    by1 = int(min(mask.shape[0], cy + head_px / 2))
    if bx1 - bx0 < 8 or by1 - by0 < 8:
        return None
    # the cascade reports the FACE inside the skull; this box is the skull, so hand back
    # the face-sized box the rest of the stage expects
    return (bx0, by0, bx1 - bx0, by1 - by0)


def parse_face_box(text, w, h):
    parts = [float(v) for v in str(text).replace(";", ",").split(",") if v.strip()]
    if len(parts) != 4:
        raise SystemExit("--face-box needs x,y,w,h in pixels")
    x, y, bw, bh = parts
    if max(bw, bh) <= 1.0:                       # given as fractions of the image
        x, y, bw, bh = x * w, y * h, bw * w, bh * h
    return (int(x), int(y), int(bw), int(bh))



def estimate_proportions(face, shape, source="haar", metrics=None,
                         heads_tall_override=None, height_m=1.75):
    """
    Body proportions in head-units.

    With a detected face we can measure the head and assume seven heads, which is the
    classic adult figure and reads correctly at a glance. Without one we fall back to
    population averages; both paths are marked in the output.
    """
    h, w = shape[:2]
    if source == "silhouette" and metrics:
        # Drawn characters are not photographed people: the cascade usually finds nothing,
        # and the silhouette is the only real measurement available. Head size comes from
        # the width just below the crown, which survives stylisation better than the face.
        head_px = metrics["head_px_estimate"]
        heads_tall = metrics["heads_tall_estimate"]
        notes = ("no photographic face; head height derived from the silhouette crown "
                 f"({metrics['figure_type']}, {heads_tall} heads tall, a guess)")
        confidence = 0.50
    elif face:
        fx, fy, fw, fh = face
        # the cascade box is the face, not the skull: a head is ~1.35x the face box
        head_px = fh * 1.35
        heads_tall = 7.0 if source == "haar" else max(4.0, min(8.0, metrics["heads_tall_estimate"]
                                                            if metrics else 7.0))
        notes = ("face detected by cascade; head measured from the box (x1.35 for the skull)"
                 if source == "haar" else
                 f"head box supplied manually or from silhouette ({source})")
        confidence = 0.72 if source == "haar" else 0.60
    else:
        head_px = h * 0.16
        heads_tall = 7.2
        notes = "no face and no usable silhouette; proportions are population averages"
        confidence = 0.30
    # A drawn character does not obey human head counts. If the caller states the count,
    # it wins and the report says so, rather than pretending a guess is a measurement.
    if heads_tall_override:
        heads_tall = float(heads_tall_override)
        figure_px = metrics["bbox"][3] if metrics else h
        head_px = figure_px / max(1.0, heads_tall)
        notes = (f"head count supplied by the caller: {heads_tall:g} heads tall "
                 f"({metrics['figure_type'] if metrics else 'figure unknown'})")
        confidence = 0.80
    height_px = h
    est_height_m = float(height_m)                     # never measured: no scale in an image
    head_unit = est_height_m / heads_tall
    return {
        "estimated_person_metres": round(est_height_m, 3),
        "heads_tall": heads_tall,
        "head_unit_m": round(head_unit, 4),
        "head_count_source": ("caller" if heads_tall_override
                              else "silhouette" if source == "silhouette"
                              else "cascade+7" if source == "haar" else "default"),
        "shoulder_width_in_heads": 1.90,
        "arm_span_in_heads": 3.60,
        "leg_length_in_heads": 3.40,
        "waist_height_in_heads": 4.20,
        "head_px": round(head_px, 1),
        "image_px": [int(w), int(h)],
        "head_takes_fraction_of_frame": round(head_px / height_px, 4),
    }, notes, confidence


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--target-tris", type=int, default=30000)
    ap.add_argument("--heads-tall", type=float, default=None,
                    help="how many heads tall the character is (e.g. 5 for a stylised one); "
                         "overrides the silhouette estimate, which is a guess on drawings")
    ap.add_argument("--height-m", type=float, default=1.75,
                    help="assumed real height in metres; a single image has no scale reference")
    ap.add_argument("--face-box", default=None,
                    help="x,y,w,h in pixels (or 0..1 fractions) to override detection; "
                         "use this when concept art defeats the cascade")
    a = ap.parse_args()

    path = pathlib.Path(a.image)
    img_raw = cv2.imread(str(path), cv2.IMREAD_UNCHANGED)
    if img_raw is None:
        raise SystemExit(f"cannot read image: {path}")
    # Composite onto white so sampled colours are not contaminated by black where the art
    # is transparent. Flat colour sampling on a transparent PNG otherwise reads #000000.
    if img_raw.ndim == 3 and img_raw.shape[2] == 4:
        alpha = img_raw[:, :, 3:4].astype(np.float32) / 255.0
        img = (img_raw[:, :, :3].astype(np.float32) * alpha
               + 255.0 * (1.0 - alpha)).astype(np.uint8)
    else:
        img = img_raw

    # ---- validate the input rather than trusting it -------------------------
    h, w = img.shape[:2]
    checks = {
        "readable": True,
        "min_resolution_ok": w >= 160 and h >= 160,
        "not_a_blank_frame": float(img.std()) > 6.0,
        "aspect_sane": 0.3 <= w / max(1, h) <= 3.0,
    }
    if not all(checks.values()):
        # Write the report first: "it failed" is useless without the reason, and the
        # pipeline reads limitations from this file.
        pathlib.Path(a.out).parent.mkdir(parents=True, exist_ok=True)
        pathlib.Path(a.out).write_text(json.dumps(
            {"image": str(a.image), "validation": checks, "usable": False,
             "limitations": ["image failed validation: " + ", ".join(
                 k for k, v in checks.items() if not v)]}, indent=2))
        raise SystemExit(f"image failed validation: {checks}")

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    mask, mask_source, bg_colour = foreground_mask(img_raw, img)
    metrics = figure_metrics(mask) if mask is not None else None

    rejected = []
    if a.face_box:
        face, face_source = parse_face_box(a.face_box, w, h), "manual"
    else:
        face, face_source = detect_face(gray), "haar"
        if face is not None:
            ok, why = face_plausible(face, mask, metrics)
            if not ok:
                rejected.append({"box": list(face), "reason": why})
                face, face_source = None, "none"
        if face is None and metrics:
            face, face_source = head_box_from_mask(mask, metrics), "silhouette"
        if face is None:
            face_source = "none"

    prop, prop_note, confidence = estimate_proportions(
        face, (h, w), face_source, metrics,
        heads_tall_override=a.heads_tall, height_m=a.height_m)

    # ---- colours ------------------------------------------------------------
    colours, regions = {}, {}
    if face:
        fx, fy, fw, fh = face
        regions = {
            "skin_forehead": (fx + fw * 0.30, fy + fh * 0.10, fx + fw * 0.70, fy + fh * 0.26),
            "skin_cheek": (fx + fw * 0.12, fy + fh * 0.52, fx + fw * 0.34, fy + fh * 0.72),
            "skin_cheek_r": (fx + fw * 0.66, fy + fh * 0.52, fx + fw * 0.88, fy + fh * 0.72),
            "hair_top": (fx + fw * 0.25, max(0, fy - fh * 0.22), fx + fw * 0.75, fy + fh * 0.08),
            "lips": (fx + fw * 0.38, fy + fh * 0.74, fx + fw * 0.62, fy + fh * 0.82),
            "eye_left": (fx + fw * 0.20, fy + fh * 0.38, fx + fw * 0.34, fy + fh * 0.46),
            "eye_right": (fx + fw * 0.66, fy + fh * 0.38, fx + fw * 0.80, fy + fh * 0.46),
            "clothing_chest": (fx, fy + fh * 1.55, fx + fw * 2.0, fy + fh * 2.6),
            "background": (0, 0, w * 0.12, h * 0.12),
            "clothing_shoulder": (fx - fw * 0.3, fy + fh * 1.25, fx + fw * 1.3, fy + fh * 1.6),
        }
    else:
        regions = {
            "skin_forehead": (w * 0.40, h * 0.10, w * 0.60, h * 0.18),
            "skin_cheek": (w * 0.35, h * 0.22, w * 0.48, h * 0.30),
            "skin_cheek_r": (w * 0.52, h * 0.22, w * 0.65, h * 0.30),
            "hair_top": (w * 0.35, h * 0.02, w * 0.65, h * 0.08),
            "lips": (w * 0.44, h * 0.31, w * 0.56, h * 0.34),
            "eye_left": (w * 0.38, h * 0.17, w * 0.45, h * 0.20),
            "eye_right": (w * 0.55, h * 0.17, w * 0.62, h * 0.20),
            "clothing_chest": (w * 0.30, h * 0.62, w * 0.70, h * 0.80),
            "background": (0, 0, w * 0.12, h * 0.12),
            "clothing_shoulder": (w * 0.20, h * 0.52, w * 0.40, h * 0.62),
        }
    for name, box in regions.items():
        hexcol, clusters, frac = region(img, *box, mask=mask)
        if hexcol:
            colours[name] = hexcol
            regions[name] = {"box": [round(v, 1) for v in box], "clusters": clusters,
                             "inside_figure": round(frac, 3)}

    # An eye box averages mostly eyelid skin, so its mean cannot colour the iris.
    # Take the darkest cluster of each eye box instead: that is the iris and pupil.
    def _lum(h):
        return 0.2126 * int(h[1:3], 16) + 0.7152 * int(h[3:5], 16) + 0.0722 * int(h[5:7], 16)

    def _scale(h, f):
        r, g, b = (int(h[i:i + 2], 16) for i in (1, 3, 5))
        return "#%02X%02X%02X" % (int(r * f), int(g * f), int(b * f))

    for side in ("eye_left", "eye_right"):
        cl = regions.get(side, {}).get("clusters") or []
        if cl:
            colours["iris_" + side.split("_")[1]] = _scale(min(cl, key=_lum), 0.62)
    lips_cl = regions.get("lips", {}).get("clusters") or []
    if lips_cl:
        # like the eye boxes, the mean of the lip box is mostly skin; the darkest
        # cluster is the lip pigment itself
        colours["lips"] = _scale(min(lips_cl, key=_lum), 0.92)
    hair = regions.get("hair_top", {}).get("clusters") or []
    if hair:
        colours["brow"] = min(hair, key=_lum)
    colours.setdefault("iris", colours.get("iris_right") or colours.get("iris_left") or "#463127")

    # If a masked region came back empty (the box was mostly background), the honest
    # fallback is a region we *know* is on the character: the top of the silhouette.
    if mask is not None and metrics and ("hair_top" not in colours):
        x0, y0, fw, fh = metrics["bbox"]
        band = (x0 + fw * 0.30, y0 + fh * 0.02, x0 + fw * 0.70, y0 + fh * 0.14)
        hexcol, clusters, frac = region(img, *band, k=3, mask=mask)
        if hexcol:
            colours["hair_top"] = hexcol
            regions["hair_top"] = {"box": [round(v, 1) for v in band], "clusters": clusters,
                                   "inside_figure": round(frac, 3), "fallback": "silhouette crown"}

    # a single skin tone: average the samples so one shadowed patch cannot dominate
    skin_samples = [colours.get(k) for k in ("skin_forehead", "skin_cheek", "skin_cheek_r")
                    if colours.get(k)]
    if skin_samples:
        rgb = np.mean([[int(s[i:i + 2], 16) for i in (1, 3, 5)] for s in skin_samples], axis=0)
        colours["skin"] = "#%02X%02X%02X" % tuple(int(v) for v in rgb)

    result = {
        "image": str(path),
        "image_sha256_note": "hash recorded by the manifest stage, not here",
        "validation": checks,
        "face_source": face_source,
        "rejected_faces": rejected,
        "silhouette": (dict(metrics, source=mask_source) if metrics else {"source": mask_source}),
        "face": ({"x": face[0], "y": face[1], "w": face[2], "h": face[3],
                  "detector": "opencv_haar"} if face else None),
        "colours": colours,
        "regions": regions,
        "proportions": prop,
        "proportion_method": prop_note,
        "proportion_confidence": confidence,
        "target_tris": a.target_tris,
        "learned_reconstruction": "unavailable",
        "limitations": [
            "No learned image-to-3D model is installed in this sandbox (no GPU, no "
            "model weights). The mesh is parametric, fitted to the measured face box "
            "and sampled colours.",
            "The output is NOT a likeness of this person. Identity, expression and "
            "fine facial structure are not recovered from one photograph.",
            "Scale is assumed: a single photo carries no metric reference, so height "
            "defaults to 1.75 m.",
            "Back-of-head, clothing folds and occluded regions are invented from "
            "sample colours, not measured.",
        ],
    }
    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2))
    print(json.dumps({k: result[k] for k in
                      ("validation", "face", "proportion_method", "proportion_confidence")}, indent=2))
    print("colours:", json.dumps(colours, indent=1))


if __name__ == "__main__":
    main()
