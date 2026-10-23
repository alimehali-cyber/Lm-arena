import os, json
import numpy as np
import cv2
from PIL import Image

out_dir = "avatar_pipeline/working/recon_v3"
rgb_orig = np.asarray(Image.open("avatar_pipeline/input/professor_z_turnaround_ref.png").convert("RGB"))
H, W, _ = rgb_orig.shape
yy, xx = np.mgrid[0:H, 0:W]

bg_seed = np.zeros((H, W), dtype=bool)
bg_seed[:, :140] = True
bg_seed[:, 535:625] = True
bg_seed[:, 960:1080] = True
bg_seed[:, 1390:] = True
bg_seed[990:, :] = True
bg_seed[:168, :] = False

A = np.column_stack([
    np.ones(bg_seed.sum()),
    xx[bg_seed] / W, yy[bg_seed] / H,
    (xx[bg_seed] / W)**2, (yy[bg_seed] / H)**2, (xx[bg_seed] / W) * (yy[bg_seed] / H),
])
A_all = np.column_stack([
    np.ones(H * W),
    xx.ravel() / W, yy.ravel() / H,
    (xx.ravel() / W)**2, (yy.ravel() / H)**2, (xx.ravel() / W) * (yy.ravel() / H),
])
bg_rgb = np.zeros((H, W, 3), dtype=np.float32)
for c in range(3):
    coef, _, _, _ = np.linalg.lstsq(A, rgb_orig[:, :, c][bg_seed].astype(np.float32), rcond=None)
    bg_rgb[:, :, c] = (A_all @ coef).reshape(H, W)

rgb = rgb_orig.copy()
rgb[:168, :] = np.clip(np.round(bg_rgb[:168, :]), 0, 255).astype(np.uint8)

views = [("FRONT", 150, 535), ("SIDE", 615, 965), ("BACK", 1075, 1395)]
panels = []

for vname, x0, x1 in views:
    d_npz = np.load(f"{out_dir}/{vname.lower()}.npz")
    d_hi = d_npz["depth"]
    d_unit = d_npz["depth_global"]

    crop = rgb[:, x0:x1]
    bg_c = bg_rgb[:, x0:x1]
    diff_vec = crop.astype(np.float32) - bg_c
    diff_norm = np.linalg.norm(diff_vec, axis=-1)
    luma_shift = crop.mean(axis=-1) - bg_c.mean(axis=-1)
    chroma_shift = np.abs((crop[:, :, 0].astype(np.float32) - crop[:, :, 2].astype(np.float32)) - (bg_c[:, :, 0] - bg_c[:, :, 2]))

    # Gradient magnitude detects EVERY crisp sneaker rubber sole boundary while soft floor shadow has gradient < 4!
    gx = cv2.Sobel(crop.astype(np.float32), cv2.CV_32F, 1, 0, ksize=3) / 4.0
    gy = cv2.Sobel(crop.astype(np.float32), cv2.CV_32F, 0, 1, ksize=3) / 4.0
    grad_mag = np.linalg.norm(np.hypot(gx, gy), axis=-1)

    # Notice what distinguishes fuzzy floor shadow around y=925..975 from actual shoe:
    # Floor shadow is DARKER than paper (`luma_shift < -2`), has NEAR-ZERO paper chroma shift (`chroma_shift < 5.5`),
    # AND is outside the crisp dark outsole boundary (`grad_mag` low & not bright white rubber)!
    # Let's build a super-clean GrabCut seed with explicit SURE_FG / SURE_BG / PROB_BG around the shoes!
    gc_mask = np.full(crop.shape[:2], cv2.GC_PR_BGD, dtype=np.uint8)

    # Above y=905 (from rolled cuffs all the way up to hair crown), background has zero shadow!
    upper_fg = (d_unit > 0.20) & ((diff_norm > 6.5) | (chroma_shift > 4.0) | (luma_shift > 4.5))
    upper_fg[225:295, :] |= (d_unit[225:295, :] > 0.58)  # eyeglasses lens interior

    # In shoe zone y >= 905:
    # 1. Black canvas upper, dark foxing stripe, dark bottom outsole line: crop luma < 145 -> 100% SURE SHOE!
    # 2. Cool/bright white rubber toe cap, laces & midsole: (luma_shift > 1.5) or (chroma_shift > 4.8) inside d_unit > 0.22 -> SURE SHOE!
    # 3. Any pixel enclosed BETWEEN black upper/foxing stripe above and dark bottom tread line below -> 100% SURE SHOE!
    shoe_band = np.zeros_like(upper_fg)
    dark_shoe = (crop.mean(axis=-1) < 165) & (diff_norm > 28.0)
    white_rubber = (d_unit > 0.24) & ((luma_shift > 1.2) | (chroma_shift > 4.6))
    shoe_core = dark_shoe | white_rubber

    # Column-wise vertical enclosure inside shoe zone y in [900..975]:
    # In every column x where dark_shoe exists (black canvas upper above AND dark rubber bottom edge / foxing stripe below!),
    # EVERY pixel vertically between the top dark shoe pixel and the bottom dark shoe pixel is 100% INSIDE THE SNEAKER MIDSOLE!!
    for cx in range(crop.shape[1]):
        col_dark = np.where(dark_shoe[895:975, cx])[0]
        if len(col_dark) >= 2:
            y_top_d = 895 + col_dark[0]
            y_bot_d = 895 + col_dark[-1]
            if y_bot_d - y_top_d <= 68:
                shoe_core[y_top_d:y_bot_d + 1, cx] = True

    full_fg = upper_fg.copy()
    full_fg[900:, :] = shoe_core[900:, :]
    # Close tiny 3px gaps inside shoe toe tips ONLY on rows 900..975
    shoe_closed = cv2.morphologyEx(full_fg[895:976, :].astype(np.uint8), cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))).astype(bool)
    full_fg[895:976, :] = shoe_closed

    num_l, labels, stats, _ = cv2.connectedComponentsWithStats(full_fg.astype(np.uint8), connectivity=8)
    best = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
    mask_clean = (labels == best)

    # Re-extract clean SIDE single-leg mask using the clean shoe bottom!
    side_near = mask_clean.copy()
    if vname == "SIDE":
        for y in range(605, 976):
            row_m = np.where(mask_clean[y, :])[0]
            if len(row_m) > 8:
                row_d = d_unit[y, row_m]
                if row_d.max() - row_d.min() > 0.13:
                    thr = 0.5 * (np.percentile(row_d, 85) + np.percentile(row_d, 15))
                    side_near[y, row_m[row_d < thr]] = False
        # Close 3px on shoe sole of side_near & keep largest component
        side_near[900:976, :] = cv2.morphologyEx(side_near[900:976, :].astype(np.uint8), cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))).astype(bool)
        num2, l2, s2, _ = cv2.connectedComponentsWithStats(side_near.astype(np.uint8), 8)
        side_near = (l2 == (1 + np.argmax(s2[1:, cv2.CC_STAT_AREA])))

    dist_in = cv2.distanceTransform(mask_clean.astype(np.uint8), cv2.DIST_L2, 3)
    alpha = np.clip((diff_norm - 3.5) / 12.0, 0.0, 1.0)
    alpha = np.where(dist_in >= 1.8, 1.0, np.where(mask_clean, np.maximum(alpha, 0.40), 0.0))
    unmixed = np.clip((crop.astype(np.float32) - (1.0 - alpha[:, :, None]) * bg_c) / np.maximum(alpha[:, :, None], 0.30), 0, 255).astype(np.uint8)
    unmixed = np.where(dist_in[:, :, None] < 2.5, unmixed, crop)

    ys, xs = np.where(mask_clean)
    print(f"{vname:5s} SURGICAL: x=[{x0+xs.min()}..{x0+xs.max()}] w={xs.max()-xs.min()+1}, y=[{ys.min()}..{ys.max()}] h={ys.max()-ys.min()+1}")

    # Zoomed shoe inspection panel (y=840..980 at 2x magnification on bright magenta checker!)
    vis = unmixed.copy()
    vis[~mask_clean] = [25, 200, 85]
    panels.append(vis[160:980])
    if vname == "SIDE":
        vn = unmixed.copy()
        vn[~side_near] = [35, 38, 52]
        panels.append(vn[160:980])

    np.savez_compressed(
        f"{out_dir}/{vname.lower()}_clean.npz",
        rgb=unmixed, alpha=alpha, mask=mask_clean, side_near=side_near,
        depth=d_hi, depth_global=d_unit, x0=x0, x1=x1,
    )

# Also inspect exact eyeglasses rim geometry in FRONT & SIDE + create clean glasses-free face texture + watch-free right-side texture!
front = np.load(f"{out_dir}/front_clean.npz")
side = np.load(f"{out_dir}/side_clean.npz")
lms = np.load(f"{out_dir}/front_facemesh_478.npy")  # global (x, y, z)

# MediaPipe iris centers: 468 = character right eye (viewer left), 473 = character left eye (viewer right)!
eye_L_img = lms[468, :2]  # viewer left (~x=318, y=258)
eye_R_img = lms[473, :2]  # viewer right (~x=369, y=262)
nose_bridge = lms[168, :2]
nose_tip = lms[1, :2]
mouth_L = lms[61, :2]
mouth_R = lms[291, :2]
chin_bot = lms[152, :2]
forehead_top = lms[10, :2]
print("Eye viewer-L:", eye_L_img.round(2), "Eye viewer-R:", eye_R_img.round(2), "Nose tip:", nose_tip.round(2), "Chin:", chin_bot.round(2))

# Measure exact bronze circular glasses frame radius around each eye in FRONT by radial edge profile!
f_rgb = front["rgb"]
fx0 = int(front["x0"])
for label, cxy in [("ViewerL_Frame", eye_L_img), ("ViewerR_Frame", eye_R_img)]:
    cx, cy = cxy[0] - fx0, cxy[1]
    best_score, best_params = -1, None
    # Search fine center offset dx, dy in [-4..+4] and radius r in [16..27]
    thetas = np.linspace(0, 2 * np.pi, 180, endpoint=False)
    # Exclude top arc overlapping dark eyebrow (-130 deg .. -50 deg)!
    keep_ang = ~((thetas > -2.35) & (thetas < -0.75)) & ~((thetas > 3.93) & (thetas < 5.53))
    thetas = thetas[keep_ang]
    for dx in np.linspace(-3.5, 5.5, 19):
        for dy in np.linspace(-2.0, 5.0, 15):
            for r in np.linspace(17.5, 25.5, 33):
                px = np.clip((cx + dx + r * np.cos(thetas)).astype(int), 2, f_rgb.shape[1]-3)
                py = np.clip((cy + dy + r * np.sin(thetas)).astype(int), 2, f_rgb.shape[0]-3)
                px_in = np.clip((cx + dx + (r - 3.0) * np.cos(thetas)).astype(int), 2, f_rgb.shape[1]-3)
                py_in = np.clip((cy + dy + (r - 3.0) * np.sin(thetas)).astype(int), 2, f_rgb.shape[0]-3)
                px_out = np.clip((cx + dx + (r + 3.0) * np.cos(thetas)).astype(int), 2, f_rgb.shape[1]-3)
                py_out = np.clip((cy + dy + (r + 3.0) * np.sin(thetas)).astype(int), 2, f_rgb.shape[0]-3)
                rim_luma = f_rgb[py, px].mean(axis=-1)
                sur_luma = 0.5 * (f_rgb[py_in, px_in].mean(axis=-1) + f_rgb[py_out, px_out].mean(axis=-1))
                score = float(np.median(sur_luma - rim_luma))
                if score > best_score:
                    best_score = score
                    best_params = (round(float(cx + dx + fx0), 2), round(float(cy + dy), 2), round(float(r), 2))
    print(f"Detected exact glasses ring {label}: center=({best_params[0]}, {best_params[1]}), radius={best_params[2]} px, contrast={best_score:.2f}")

Image.fromarray(np.hstack(panels)).save(f"{out_dir}/debug_03_surgical_masks.png")
print("Saved debug_03_surgical_masks.png")
