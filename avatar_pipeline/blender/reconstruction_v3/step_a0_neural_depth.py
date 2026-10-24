#!/usr/bin/env python3
"""Stage A0 — Neural depth & seed landmarks (`step_a0_neural_depth.py`).

Deterministic, 100% local preparation of the per-view inputs consumed by `step_a_*`:
  1. Runs `Depth-Anything-V2-Small` (`model_fp16.onnx`, CPU `onnxruntime`, float32 input tensors)
     over each of the three turnaround views and stores, per view:
       - `depth`         : full-resolution relative disparity (higher = closer), float32
       - `depth_global`  : min/max-normalized version of `depth` in `[0, 1]`
  2. Runs MediaPipe `FaceMesh(refine_landmarks=True)` on the 2x-upscaled FRONT head crop and stores
     `front_facemesh_478.npy` (global pixel `x, y` + scaled `z`) which `step_a2` re-derives exactly
     and `step_a3` finally re-runs with zero offset on the clean plate.

Inputs :  `avatar_pipeline/input/professor_z_turnaround_ref.png` (untouched reference)
Outputs:  `avatar_pipeline/working/recon_v3/{front,side,back}.npz` + `front_facemesh_478.npy`

The reference photograph is NEVER uploaded anywhere — all inference runs inside this sandbox on
Apache-2.0 upstream weights (see `avatar_pipeline/models/MODEL_MANIFEST.json`).
"""
from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np
import onnxruntime as ort
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
WORK = ROOT / "avatar_pipeline/working/recon_v3"
REF_PATH = ROOT / "avatar_pipeline/input/professor_z_turnaround_ref.png"
ONNX_PATH = ROOT / "avatar_pipeline/models/depth_anything_v2_small/model_fp16.onnx"

VIEWS = [("FRONT", 150, 535), ("SIDE", 615, 965), ("BACK", 1075, 1395)]

# Identical crop geometry used by `step_a2_perfect_plates.py` for its 2x FaceMesh refinement pass.
HEAD_CROP = (240, 168, 450, 365)


def view_depth(session: ort.InferenceSession, rgb: np.ndarray, x0: int, x1: int) -> tuple[np.ndarray, np.ndarray]:
    crop = np.ascontiguousarray(rgb[:, x0:x1])
    mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
    std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
    small = cv2.resize(crop, (518, 518), interpolation=cv2.INTER_CUBIC).astype(np.float32) / 255.0
    inp = np.transpose((small - mean) / std, (2, 0, 1))[None].astype(np.float32)
    pred = session.run(None, {session.get_inputs()[0].name: inp})[0].squeeze().astype(np.float32)
    d_hi = cv2.resize(pred, (crop.shape[1], crop.shape[0]), interpolation=cv2.INTER_CUBIC)
    lo, hi = float(np.min(d_hi)), float(np.max(d_hi))
    d_unit = (d_hi - lo) / max(hi - lo, 1e-6)
    return d_hi.astype(np.float32), d_unit.astype(np.float32)


def seed_facemesh(rgb: np.ndarray) -> np.ndarray:
    import mediapipe as mp

    hx0, hy0, hx1, hy1 = HEAD_CROP
    hcrop2x = cv2.resize(rgb[hy0:hy1, hx0:hx1], ((hx1 - hx0) * 2, (hy1 - hy0) * 2), interpolation=cv2.INTER_CUBIC)
    with mp.solutions.face_mesh.FaceMesh(
        static_image_mode=True, max_num_faces=1, refine_landmarks=True, min_detection_confidence=0.2
    ) as fm:
        res = fm.process(hcrop2x)
        assert res.multi_face_landmarks, "FaceMesh failed on 2x head crop"
        return np.array(
            [
                [lm.x * (hx1 - hx0) + hx0, lm.y * (hy1 - hy0) + hy0, lm.z * (hx1 - hx0)]
                for lm in res.multi_face_landmarks[0].landmark
            ],
            dtype=np.float32,
        )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true", help="Recompute even when cached plates exist.")
    args = ap.parse_args()
    WORK.mkdir(parents=True, exist_ok=True)

    plates = [WORK / f"{v.lower()}.npz" for v, _, _ in VIEWS]
    npy = WORK / "front_facemesh_478.npy"
    if not args.force and all(p.is_file() for p in plates) and npy.is_file():
        print("A0 cache hit — neural depth plates & seed landmarks already present (use --force to recompute).")
        return

    rgb = np.asarray(Image.open(REF_PATH).convert("RGB")).copy()
    # Match the paper background inpainting of the rows above the heads used by `step_a*` (bg-seeded fit).
    H, W, _ = rgb.shape
    yy, xx = np.mgrid[0:H, 0:W]
    bg_seed = np.zeros((H, W), dtype=bool)
    bg_seed[:, :140] = True
    bg_seed[:, 535:625] = True
    bg_seed[:, 960:1080] = True
    bg_seed[:, 1390:] = True
    bg_seed[990:, :] = True
    bg_seed[:168, :] = False
    A = np.column_stack(
        [
            np.ones(bg_seed.sum()),
            xx[bg_seed] / W,
            yy[bg_seed] / H,
            (xx[bg_seed] / W) ** 2,
            (yy[bg_seed] / H) ** 2,
            (xx[bg_seed] / W) * (yy[bg_seed] / H),
        ]
    )
    coef_all = [
        np.linalg.lstsq(A, rgb[:, :, c][bg_seed].astype(np.float32), rcond=None)[0] for c in range(3)
    ]
    for c in range(3):
        a_all = np.column_stack(
            [np.ones(H * W), xx.ravel() / W, yy.ravel() / H, (xx.ravel() / W) ** 2, (yy.ravel() / H) ** 2, (xx.ravel() / W) * (yy.ravel() / H)]
        )
        bg_c = (a_all @ coef_all[c]).reshape(H, W)
        rgb[:168, :, c] = np.clip(np.round(bg_c[:168, :]), 0, 255).astype(np.uint8)

    session = ort.InferenceSession(str(ONNX_PATH), providers=["CPUExecutionProvider"])
    for vname, x0, x1 in VIEWS:
        d_hi, d_unit = view_depth(session, rgb, x0, x1)
        np.savez_compressed(WORK / f"{vname.lower()}.npz", depth=d_hi, depth_global=d_unit, x0=x0, x1=x1)
        print(f"{vname}: depth {d_hi.shape} range [{d_hi.min():.3f}, {d_hi.max():.3f}]")

    lms = seed_facemesh(rgb)
    assert lms.shape == (478, 3), f"unexpected landmark count {lms.shape}"
    np.save(npy, lms)
    print(f"FaceMesh seed -> {npy.name}: EyeL {lms[468, :2].round(2)} EyeR {lms[473, :2].round(2)}")
    print("STAGE A0 NEURAL DEPTH + SEED LANDMARKS DONE")


if __name__ == "__main__":
    main()
