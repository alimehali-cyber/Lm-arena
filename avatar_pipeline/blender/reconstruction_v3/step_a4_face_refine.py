#!/usr/bin/env python3
"""Stage A4 — Face refinement against the high-detail 6-view head reference (`step_a4_face_refine.py`).

Iteration-4b: `avatar_pipeline/input/professor_z_face_ref.png` (1536x1024, six head views at ~2.4x the
face scale of the full-body turnaround) is the authoritative likeness. This step composites the new head
onto the per-view PLATES written by step_a2 ({front,side,back}_perfect.npz) WITHOUT changing any frame:

  * the new FRONT head is rigidly mapped into the plate — rotate by the eye-line-roll difference, uniform
    scale to the old pupil separation, translate eye-midpoint onto eye-midpoint — so every downstream step
    keeps the same coordinate frame, head size and pose; only face pixels, head silhouette and hair mass
    change (and they change to be IDENTICAL to the new reference, not the old render);
  * the nose-left PROFILE panel feeds the SIDE plate (nose-tip point anchor), the BACK panel feeds the BACK
    plate (crown row + head-width centroid column); same rotation+scale, per-view translation;
  * masks/alpha: the warped (feathered) new head is UNIONED into the masks; old-head pixels that fall
    INSIDE the front/side head window but OUTSIDE the new silhouette (mismatched old hair tufts) are
    carved to background so the multi-view hull follows the new hair;
  * Depth-Anything-V2 is re-run on the composited plates via step_a0's `view_depth` (imported; weights
    stay local) so relief high-passes stay consistent with the new surface;
  * step_a3 must run next: FaceMesh, iris guard, glasses rings, inpaint masks and front_facemesh_478.npy
    are all recomputed on the new face — no stale constants.

Deterministic, 100% local; the reference file is never modified or uploaded.
Run order:  a0 -> a_surgical_extract -> a2 -> **a4** -> a3 -> b_v4 -> bc_v6 -> v7 -> v8 -> c/v7 -> d.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

import cv2
import numpy as np
import onnxruntime as ort
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[3]
WORK = ROOT / "avatar_pipeline/working/recon_v3"
FACE_REF = ROOT / "avatar_pipeline/input/professor_z_face_ref.png"
ONNX_PATH = ROOT / "avatar_pipeline/models/depth_anything_v2_small/model_fp16.onnx"
# White-gap segmentation of the 1536x1024 sheet: top row = front / nose-left profile / back-of-head.
PANELS = {"front": (204, 33, 543, 498), "side": (1017, 33, 1367, 498), "back": (1017, 532, 1367, 967)}
BG_LUMA = 231.6667
CHIN_MARGIN = 46  # plate px below the old chin where the paste (and any carving) stops, all views

_spec = importlib.util.spec_from_file_location("a0mod", Path(__file__).with_name("step_a0_neural_depth.py"))
_a0 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_a0)  # safe: a0 only runs main() under __main__


def head_mask(rgb: np.ndarray) -> np.ndarray:
    d = np.abs(rgb.astype(np.float32) - BG_LUMA).mean(-1)
    m = d > 10.5
    m = ndimage.binary_opening(m, np.ones((2, 2), bool))
    m = ndimage.binary_closing(m, np.ones((4, 4), bool))
    lab, n = ndimage.label(m)
    if n > 0:
        sizes = ndimage.sum(m, lab, range(1, n + 1))
        m = lab == (1 + int(np.argmax(sizes)))
    return m


def main() -> None:
    assert FACE_REF.is_file(), f"missing {FACE_REF}"
    sheet = np.asarray(Image.open(FACE_REF).convert("RGB"))
    assert sheet.shape[:2] == (1024, 1536), f"unexpected face-ref sheet size {sheet.shape}"
    meta = {"sheet": [1024, 1536], "panels": {k: list(v) for k, v in PANELS.items()}}

    panels = {}
    for key, (x0, y0, x1, y1) in PANELS.items():
        rgb = sheet[y0:y1, x0:x1]
        msk = head_mask(rgb)
        rws = np.where(msk.any(1))[0]
        cws = np.where(msk.any(0))[0]
        panels[key] = {"rgb": rgb, "off": np.array([x0, y0], np.float64), "msk": msk,
                       "top": int(rws.min()), "bot": int(rws.max()),
                       "lft": int(cws.min()), "rgt": int(cws.max())}

    # ---- new FRONT anchors (FaceMesh on the exact panel) ----
    import mediapipe as mp
    front = panels["front"]
    with mp.solutions.face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1,
                                         refine_landmarks=True, min_detection_confidence=0.2) as fm:
        res = fm.process(np.ascontiguousarray(front["rgb"]))
    assert res.multi_face_landmarks, "FaceMesh failed on the NEW front panel"
    lmsF = np.array([[lm.x * front["rgb"].shape[1], lm.y * front["rgb"].shape[0]]
                     for lm in res.multi_face_landmarks[0].landmark], dtype=np.float64)
    nL, nR = lmsF[468], lmsF[473]
    new_sep = float(np.linalg.norm(nR - nL))
    new_roll = float(np.degrees(np.arctan2(nR[1] - nL[1], nR[0] - nL[0])))
    assert 60.0 < new_sep < 130.0, f"new-sheet pupil separation {new_sep:.1f}px implausible"
    new_mid = 0.5 * (nL + nR)
    eye_row_panel = float(0.5 * (nL[1] + nR[1]))
    prof = panels["side"]
    def mass_centroid(mask, r0, r1, cl):
        sub = mask[r0:r1, :cl]
        rs_, cs_ = np.where(sub)
        assert len(cs_) > 300, "profile mass band empty"
        return np.array([float(cs_.mean()), float(rs_.mean() + r0)], np.float64)
    # profile anchor = head-mass centroid in the face band (forelock spikes beat a naive nose-tip scan)
    nose_new = mass_centroid(prof["msk"], int(eye_row_panel - 30), int(eye_row_panel + 130),
                             int(0.60 * prof["rgb"].shape[1]))
    bk = panels["back"]
    back_top_new = float(bk["top"])
    back_cent_new = 0.5 * (bk["lft"] + bk["rgt"])

    # ---- old anchors (plate coords; the three plates share the reference row space) ----
    fp = np.load(WORK / "front_perfect.npz")
    sp = np.load(WORK / "side_perfect.npz")
    bp = np.load(WORK / "back_perfect.npz")
    lms_old = np.load(WORK / "front_facemesh_478.npy").astype(np.float64)
    oL, oR = lms_old[468, :2], lms_old[473, :2]
    old_sep = float(np.linalg.norm(oR - oL))
    old_mid = 0.5 * (oL + oR)
    old_roll = float(np.degrees(np.arctan2(oR[1] - oL[1], oR[0] - oL[0])))
    chin_row_old = float(lms_old[152][1])
    S = old_sep / new_sep
    assert 0.25 < S < 0.75, f"plate/sheet head scale {S:.3f} implausible"
    ROT = old_roll - new_roll
    fx0, sx0, bx0 = int(fp["x0"]), int(sp["x0"]), int(bp["x0"])
    nose_old = mass_centroid(sp["mask"].astype(bool), int(old_mid[1] - 30), int(old_mid[1] + 130),
                             int(0.60 * sp["rgb"].shape[1]))
    brow = np.where(bp["mask"].astype(bool).any(1))[0]
    bcol = np.where(bp["mask"].astype(bool)[int(brow.min()):int(brow.min()) + 260].any(0))[0]
    back_top_old = float(brow.min())
    back_cent_old = float(0.5 * (bcol.min() + bcol.max()))
    meta.update(old_eye_sep=round(old_sep, 2), new_eye_sep=round(new_sep, 2), plate_scale=round(S, 4),
                old_eye_roll=round(old_roll, 2), new_eye_roll=round(new_roll, 2), rot_deg=round(ROT, 2),
                nose_old_plate=[round(v, 1) for v in nose_old],
                nose_new_panel=[round(v, 1) for v in nose_new],
                back_top=[back_top_old, back_top_new], back_cent=[back_cent_old, float(back_cent_new)],
                chin_row_old=round(chin_row_old, 1))
    ow = sp["mask"].astype(bool)[int(old_mid[1])-20:int(old_mid[1])+20].any(0)
    oc = np.where(ow)[0]
    hw_old = np.where(fp["mask"].astype(bool)[int(old_mid[1])-40:int(old_mid[1])+40].any(0))[0]
    hw_new = np.where(panels["front"]["msk"][int(eye_row_panel) - 40:int(eye_row_panel) + 120].any(0))[0]
    print(f"A4 widths: old head(plate) cols {hw_old.min()}..{hw_old.max()} ({hw_old.max()-hw_old.min()}px) | "
          f"new*scale {int((hw_new.max()-hw_new.min())*S)}px | old side cols {oc.min()}..{oc.max()}")
    print(f"A4: sep {old_sep:.2f}/{new_sep:.2f} -> scale {S:.4f}; rot {ROT:+.2f} deg; "
          f"nose old={[round(v, 1) for v in nose_old]} new={[round(v, 1) for v in nose_new]}")

    # per view: (src anchor in PANEL-local coords, dst anchor in SHEET coords)
    def anchors(view):
        if view == "front":
            return new_mid, old_mid
        if view == "side":
            return nose_new, nose_old + np.array([sx0, 0.0])
        return np.array([back_cent_new, back_top_new]), np.array([back_cent_old + bx0, back_top_old])

    def make_M(src, dst_sheet, x0v):
        # exact rigid mapping: f(in) = S*R*(in - src) + d   (pivot-free -> no double translation)
        a = np.radians(ROT)
        ca, sa = np.cos(a), np.sin(a)
        d = np.asarray(dst_sheet, np.float64) - np.array([x0v, 0.0])
        R = np.array([[ca, -sa], [sa, ca]])
        t = d - S * (R @ np.asarray(src, np.float64))
        return np.array([[S * ca, -S * sa, t[0]], [S * sa, S * ca, t[1]]], np.float32)

    out_npz, dbg = {}, {}
    for view, dst_npz in (("front", fp), ("side", sp), ("back", bp)):
        H, W = int(dst_npz["rgb"].shape[0]), int(dst_npz["rgb"].shape[1])
        x0v = int(dst_npz["x0"])
        p = panels[view]
        src, dstg = anchors(view)
        M = make_M(src, dstg, x0v)
        rgba = np.dstack([p["rgb"].astype(np.float32), p["msk"].astype(np.float32) * 255.0])
        rgba[..., :3] *= rgba[..., 3:4] / 255.0
        warped = cv2.warpAffine(rgba, M, (W, H), flags=cv2.INTER_CUBIC, borderValue=0.0)
        wgt = np.clip(warped[..., 3] / 255.0, 0.0, 1.0)
        _nz = np.where(warped[..., 3] > 128)
        print(f"A4-DBG {view}: warped>0.5 rows {_nz[0].min()}..{_nz[0].max()} cols {_nz[1].min()}..{_nz[1].max()} "
              f"M={M.ravel().round(2)} src={np.round(src,1)} dst={np.round(dstg,1)}")
        nrgb = warped[..., :3] / np.maximum(wgt[..., None], 1e-4)
        wgt = cv2.GaussianBlur(wgt, (0, 0), 2.0)
        binw = wgt > 0.30
        lab, n = ndimage.label(binw)
        if n > 1:
            sizes = ndimage.sum(binw, lab, range(1, n + 1))
            wgt *= (lab == (1 + int(np.argmax(sizes)))).astype(np.float32)
        assert wgt.max() > 0.98, f"{view}: warped head coverage collapsed"
        nmask = (wgt > 0.5)
        chin_cut = min(int(chin_row_old) + CHIN_MARGIN, H)
        keep = np.zeros((H, W), bool)
        keep[:chin_cut, :] = True
        nmask &= keep
        wgt = np.where(keep, wgt, 0.0)
        nr = np.where(nmask.any(1))[0]
        nc = np.where(nmask.any(0))[0]
        # hard window: inside it, old head pixels missing from the new head are carved to background
        win = np.zeros((H, W), bool)
        win[max(int(nr.min()) - 8, 0):min(int(nr.max()) + 6, chin_cut),
            max(int(nc.min()) - 14, 0):min(int(nc.max()) + 15, W)] = True
        if view == "back":
            win[:] = False  # back keeps old hair (locks over the jacket) — union only, no carving
        rgb = dst_npz["rgb"].astype(np.float32).copy()
        mask = dst_npz["mask"].astype(bool).copy()
        alpha = dst_npz["alpha"].astype(np.float32).copy()
        carve = win & ~nmask & mask
        frac = int(carve.sum()) / max(int(mask.sum()), 1)
        assert frac < 0.45, f"{view}: carve removed {frac:.0%} of the old head — alignment is wrong"
        print(f"A4 {view}: head rows {int(nr.min())}..{int(nr.max())}, cols {int(nc.min())}..{int(nc.max())}, "
              f"carve {frac:.1%}")
        # skin-tone transfer: match the new head's brightness to the old face region (both from the same
        # character family, but separate renders drift ~8%); clipped to stay conservative.
        both = nmask & mask
        lumn, lumo = nrgb.mean(-1), rgb.mean(-1)
        skin = both & (nrgb.max(-1) - nrgb.min(-1) < 60) & (lumn > 90)
        if skin.sum() > 400:
            gain = np.clip(rgb[skin].mean(0) / np.maximum(nrgb[skin].mean(0), 1e-3), 0.85, 1.22)
            nrgb = np.clip(nrgb * gain[None, None, :], 0, 255)
            print(f"A4 {view}: skin gain {gain.round(3)}")
        b = wgt[..., None]
        rgb = np.where(carve[..., None], 255.0, rgb * (1.0 - b) + nrgb * b)
        mask2 = (mask | nmask) & ~carve
        # island cleanup: anything in the union mask that is not connected to the main body blob (floating
        # old-hair tufts left by the new silhouette, detached panel curls) is carved to background.
        labI, nI = ndimage.label(mask2)
        if nI > 1:
            szI = ndimage.sum(mask2, labI, range(1, nI + 1))
            main = labI == (1 + int(np.argmax(szI)))
            floats_ = mask2 & ~main
            if int(floats_.sum()):
                rgb[floats_] = 255.0
                alpha2_0 = np.where(floats_, 0.0, np.maximum(alpha, wgt))
                mask2 = mask2 & main
                alpha = alpha2_0
                print(f"A4 {view}: removed {int(floats_.sum())}px of disconnected mask islands")
        alpha2 = np.where(carve, 0.0, np.maximum(alpha, wgt))
        out_npz[view] = (rgb.astype(np.float32), mask2, alpha2, int(dst_npz['mask'].sum()))
        dbg[view] = (np.clip(rgb, 0, 255).astype(np.uint8), mask2)

    sess = ort.InferenceSession(str(ONNX_PATH), providers=["CPUExecutionProvider"])
    for view in ("front", "side", "back"):
        rgb2, mask2, alpha2, old_area = out_npz[view]
        src_npz = np.load(WORK / f"{view}_perfect.npz")  # pre-save snapshot for passthrough keys
        x0v, x1v = int(src_npz["x0"]), int(src_npz["x1"])
        canvas_u8 = np.full((rgb2.shape[0], x1v - x0v, 3), 255, np.uint8)
        canvas_u8[:, : rgb2.shape[1]] = np.clip(rgb2, 0, 255).astype(np.uint8)
        d_hi, d_unit = _a0.view_depth(sess, canvas_u8, 0, x1v - x0v)
        np.savez_compressed(WORK / f"{view}_perfect.npz", rgb=rgb2, alpha=alpha2, mask=mask2,
                            side_near=src_npz["side_near"].astype(bool), depth=d_hi, depth_global=d_unit,
                            x0=x0v, x1=x1v)
        print(f"A4 wrote {view}_perfect.npz ({int(mask2.sum())} head px, was {old_area})")

    tiles = []
    for view in ("front", "side", "back"):
        vis, m2 = dbg[view]
        both = vis.copy()
        both[~m2] = 255
        t = cv2.resize(both, (both.shape[1] // 2, both.shape[0] // 2), interpolation=cv2.INTER_AREA)
        tiles.append(t)
    Wm = max(t.shape[1] for t in tiles)
    canvas_dbg = np.full((sum(t.shape[0] for t in tiles) + 20, Wm, 3), 250, np.uint8)
    y = 10
    for t in tiles:
        canvas_dbg[y:y + t.shape[0], : t.shape[1]] = t
        y += t.shape[0] + 5
    cv2.imwrite(str(WORK / "debug_a4_face_composite.png"), canvas_dbg[:, :, ::-1])
    meta["status"] = "ok"
    (WORK / "face_ref_meta.json").write_text(json.dumps(meta, indent=2))
    print("A4 COMPLETE — re-run step_a3 immediately (FaceMesh/rings/inpaint recomputed on the new face).")


if __name__ == "__main__":
    main()
