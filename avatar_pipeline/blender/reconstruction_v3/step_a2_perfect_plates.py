import os, json
import numpy as np
import cv2
from PIL import Image
from scipy import ndimage

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

# Re-run MediaPipe FaceMesh cleanly on upscaled 2x head crop for maximum sub-pixel accuracy!
import mediapipe as mp
hx0, hy0, hx1, hy1 = 240, 168, 450, 365
hcrop2x = cv2.resize(rgb[hy0:hy1, hx0:hx1], ((hx1 - hx0) * 2, (hy1 - hy0) * 2), interpolation=cv2.INTER_CUBIC)
with mp.solutions.face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True, min_detection_confidence=0.2) as fm:
    res = fm.process(hcrop2x)
    assert res.multi_face_landmarks, "FaceMesh failed on 2x head crop"
    lms = np.array([
        [lm.x * (hx1 - hx0) + hx0, lm.y * (hy1 - hy0) + hy0, lm.z * (hx1 - hx0)]
        for lm in res.multi_face_landmarks[0].landmark
    ], dtype=np.float32)
np.save(f"{out_dir}/front_facemesh_478.npy", lms)
print("2x-precision FaceMesh landmarks -> EyeL:", lms[468, :2].round(2), "EyeR:", lms[473, :2].round(2), "NoseTip:", lms[1, :2].round(2), "Chin:", lms[152, :2].round(2))

views = [("FRONT", 150, 535), ("SIDE", 615, 965), ("BACK", 1075, 1395)]
shoe_zooms = []

for vname, x0, x1 in views:
    d_npz = np.load(f"{out_dir}/{vname.lower()}.npz")
    d_hi = d_npz["depth"]
    d_unit = d_npz["depth_global"]

    crop = rgb[:, x0:x1]
    bg_c = bg_rgb[:, x0:x1]
    diff_norm = np.linalg.norm(crop.astype(np.float32) - bg_c, axis=-1)
    crop_luma = crop.mean(axis=-1)
    bg_luma = bg_c.mean(axis=-1)
    luma_shift = crop_luma - bg_luma
    chroma_shift = np.abs((crop[:, :, 0].astype(np.float32) - crop[:, :, 2].astype(np.float32)) - (bg_c[:, :, 0] - bg_c[:, :, 2]))

    # Above y=895: crystal-clean zero-shadow paper background
    upper_fg = (d_unit > 0.20) & ((diff_norm > 6.5) | (chroma_shift > 4.0) | (luma_shift > 4.5))
    upper_fg[225:295, :] |= (d_unit[225:295, :] > 0.58)

    # Below y=895 (rolled cuffs + Converse sneakers):
    # Notice exact physical photometry of Converse shoes vs warm cream paper + soft grey-beige shadow:
    #   - Black canvas upper, dark foxing stripe, dark bottom outsole line: `crop_luma < 150` -> 100% SHOE!
    #   - White rubber toe cap, laces & white midsole: `crop_luma >= bg_luma - 1.5` AND `(luma_shift > 0.8 or chroma_shift > 3.2)` AND within 14px of black shoe canvas/stripe!
    #   - Soft floor contact shadow: `155 < crop_luma < bg_luma - 2.0` -> STRICTLY EXCLUDED (0% shadow apron!!)!
    dark_canvas = crop_luma < 152.0
    near_dark = ndimage.binary_dilation(dark_canvas, iterations=14)
    white_sole = near_dark & (crop_luma >= bg_luma - 2.0) & ((luma_shift > 0.6) | (chroma_shift > 3.0))
    shoe_fg = dark_canvas | white_sole
    # Fill all enclosed holes inside each shoe (e.g. shaded interior of white midsole bounded between black canvas above & dark foxing/tread stripe below!)
    shoe_fg_sub = ndimage.binary_fill_holes(shoe_fg[890:980, :])
    # Smooth 3x3 morphological close ONLY on shoe rubber sole contour to remove 1px jaggedness
    shoe_fg_sub = cv2.morphologyEx(shoe_fg_sub.astype(np.uint8), cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))).astype(bool)
    shoe_fg_sub = ndimage.binary_fill_holes(shoe_fg_sub)

    full_fg = upper_fg.copy()
    full_fg[890:980, :] = shoe_fg_sub

    num_l, labels, stats, _ = cv2.connectedComponentsWithStats(full_fg.astype(np.uint8), connectivity=8)
    best = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
    mask_clean = (labels == best)

    # Clean single-leg SIDE mask (`side_near`): smooth transition across hip + full clean near shoe!
    side_near = mask_clean.copy()
    if vname == "SIDE":
        for y in range(622, 976):
            row_m = np.where(mask_clean[y, :])[0]
            if len(row_m) > 8:
                row_d = d_unit[y, row_m]
                if row_d.max() - row_d.min() > 0.12:
                    thr = 0.5 * (np.percentile(row_d, 85) + np.percentile(row_d, 15))
                    side_near[y, row_m[row_d < thr]] = False
        # Re-fill enclosed white rubber midsole of near shoe between its black upper & dark sole line!
        near_dark_shoe = side_near[900:976, :] & (crop_luma[900:976, :] < 152.0)
        near_dil = ndimage.binary_dilation(near_dark_shoe, iterations=13)
        near_white = near_dil & mask_clean[900:976, :] & (crop_luma[900:976, :] >= bg_luma[900:976, :] - 2.0)
        side_near[900:976, :] = ndimage.binary_fill_holes(side_near[900:976, :] | near_white)
        num2, l2, s2, _ = cv2.connectedComponentsWithStats(side_near.astype(np.uint8), 8)
        side_near = (l2 == (1 + np.argmax(s2[1:, cv2.CC_STAT_AREA])))

    dist_in = cv2.distanceTransform(mask_clean.astype(np.uint8), cv2.DIST_L2, 3)
    alpha = np.clip((diff_norm - 3.5) / 11.0, 0.0, 1.0)
    alpha = np.where(dist_in >= 1.8, 1.0, np.where(mask_clean, np.maximum(alpha, 0.45), 0.0))
    unmixed = np.clip((crop.astype(np.float32) - (1.0 - alpha[:, :, None]) * bg_c) / np.maximum(alpha[:, :, None], 0.32), 0, 255).astype(np.uint8)
    unmixed = np.where(dist_in[:, :, None] < 2.5, unmixed, crop)

    ys, xs = np.where(mask_clean)
    print(f"{vname:5s} PERFECT: x=[{x0+xs.min()}..{x0+xs.max()}] w={xs.max()-xs.min()+1}, y=[{ys.min()}..{ys.max()}] h={ys.max()-ys.min()+1}")

    z_panel = unmixed[855:975, :].copy()
    z_panel[~mask_clean[855:975, :]] = [25, 200, 85]
    shoe_zooms.append(cv2.resize(z_panel, (z_panel.shape[1]*2, z_panel.shape[0]*2), interpolation=cv2.INTER_NEAREST))
    if vname == "SIDE":
        zn = unmixed[855:975, :].copy()
        zn[~side_near[855:975, :]] = [35, 38, 52]
        shoe_zooms.append(cv2.resize(zn, (zn.shape[1]*2, zn.shape[0]*2), interpolation=cv2.INTER_NEAREST))

    np.savez_compressed(
        f"{out_dir}/{vname.lower()}_perfect.npz",
        rgb=unmixed, alpha=alpha, mask=mask_clean, side_near=side_near,
        depth=d_hi, depth_global=d_unit, x0=x0, x1=x1,
    )

# Detect exact circular/panto eyeglasses rings on 2x FaceMesh centers & cleanly inpaint ONLY the thin 2.5px rim from skin!
front = np.load(f"{out_dir}/front_perfect.npz")
side = np.load(f"{out_dir}/side_perfect.npz")
f_rgb = front["rgb"].copy()
s_rgb = side["rgb"].copy()
fx0, sx0 = int(front["x0"]), int(side["x0"])

# Fit exact glasses ring centers & radii in FRONT around 2x iris centers lms[468] & lms[473]
glasses_rings = {}
for key, cxy in [("viewer_left", lms[468, :2]), ("viewer_right", lms[473, :2])]:
    cx, cy = cxy[0] - fx0, cxy[1]
    best_s, best_p = -1e9, None
    thetas = np.linspace(-np.pi, np.pi, 240, endpoint=False)
    # Use lower & side 230 degrees (avoid thick dark eyebrow at top -2.35..-0.80 rad)
    arc = thetas[(thetas < -2.35) | (thetas > -0.75)]
    for dx in np.linspace(-5.0, 5.0, 21):
        for dy in np.linspace(-3.0, 5.0, 17):
            for r in np.linspace(16.0, 25.0, 37):
                px = np.round(cx + dx + r * np.cos(arc)).astype(int)
                py = np.round(cy + dy + r * np.sin(arc)).astype(int)
                px_i = np.round(cx + dx + (r - 3.0) * np.cos(arc)).astype(int)
                py_i = np.round(cy + dy + (r - 3.0) * np.sin(arc)).astype(int)
                px_o = np.round(cx + dx + (r + 3.0) * np.cos(arc)).astype(int)
                py_o = np.round(cy + dy + (r + 3.0) * np.sin(arc)).astype(int)
                rim = f_rgb[py, px].mean(axis=-1)
                sur = 0.5 * (f_rgb[py_i, px_i].mean(axis=-1) + f_rgb[py_o, px_o].mean(axis=-1))
                sc = float(np.percentile(sur - rim, 55))
                if sc > best_s:
                    best_s = sc
                    best_p = {"cx_global": round(float(cx + dx + fx0), 2), "cy": round(float(cy + dy), 2), "r_px": round(float(r), 2)}
    glasses_rings[key] = best_p
    print("PERFECT GLASSES RING", key, best_p, "contrast=", round(best_s, 2))

# Build surgical 2.5px mask ONLY along the two thin rims + nose bridge + side hinges, PROTECTING 100% of iris/eyelids/eyebrows!
rim_mask_f = np.zeros(f_rgb.shape[:2], dtype=np.uint8)
for key, p in glasses_rings.items():
    cv2.circle(rim_mask_f, (int(round(p["cx_global"] - fx0)), int(round(p["cy"]))), int(round(p["r_px"])), 255, thickness=3)
# Bridge line between inner edges of both rings + tiny temple stubs out to hair
pL, pR = glasses_rings["viewer_left"], glasses_rings["viewer_right"]
cv2.line(
    rim_mask_f,
    (int(round(pL["cx_global"] - fx0 + pL["r_px"] - 1)), int(round(pL["cy"] - 2))),
    (int(round(pR["cx_global"] - fx0 - pR["r_px"] + 1)), int(round(pR["cy"] - 2))),
    255, thickness=3,
)
cv2.line(rim_mask_f, (int(round(pL["cx_global"] - fx0 - pL["r_px"])), int(round(pL["cy"] - 2))), (int(round(pL["cx_global"] - fx0 - pL["r_px"] - 9)), int(round(pL["cy"] - 4))), 255, thickness=3)
cv2.line(rim_mask_f, (int(round(pR["cx_global"] - fx0 + pR["r_px"])), int(round(pR["cy"] - 1))), (int(round(pR["cx_global"] - fx0 + pR["r_px"] + 8)), int(round(pR["cy"] - 2))), 255, thickness=3)

# Protect dark eyebrows & eye apertures so not a single eyelash or eyebrow pixel is blurred!
protect_eyes_brows = np.zeros(f_rgb.shape[:2], dtype=bool)
for idx_group in [
    [33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246],  # viewer-left eye contour
    [362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398],  # viewer-right eye contour
    [70, 63, 105, 66, 107, 55, 65, 52, 53, 46],  # viewer-left brow
    [300, 293, 334, 296, 336, 285, 295, 282, 283, 276],  # viewer-right brow
]:
    poly = np.round(lms[idx_group, :2] - np.array([fx0, 0])).astype(np.int32)
    cv2.fillPoly(protect_eyes_brows.view(np.uint8), [poly], 1)
protect_eyes_brows = ndimage.binary_dilation(protect_eyes_brows, iterations=2)
rim_mask_f[protect_eyes_brows] = 0
rim_mask_f[~front["mask"]] = 0

f_rgb_noglass = cv2.inpaint(f_rgb, rim_mask_f, 3, cv2.INPAINT_TELEA)

# Also in SIDE view: cleanly inpaint the thin horizontal temple arm across the temple (`y=246..268, x_local=90..158`) AND create right-wrist watch-free `s_rgb_nowatch`!
rim_mask_s = np.zeros(s_rgb.shape[:2], dtype=np.uint8)
# Temple bar across side cheek/temple has low luma relative to bright skin right above & below (`s_rgb[y-4] - s_rgb[y] > 28`)!
for y in range(245, 272):
    for x in range(88, 158):
        if side["mask"][y, x]:
            above = s_rgb[y - 4, x].mean()
            below = s_rgb[y + 4, x].mean()
            cur = s_rgb[y, x].mean()
            if min(above, below) - cur > 18.0:
                rim_mask_s[y - 1:y + 2, x] = 255
s_rgb_noglass = cv2.inpaint(s_rgb, rim_mask_s, 3, cv2.INPAINT_TELEA)

# Watch on SIDE near wrist (`y=525..572`): dark watch pixels (`luma < 95`) right between bright skin arm & pocket!
watch_mask_s = np.zeros(s_rgb.shape[:2], dtype=np.uint8)
# Locate center of watch dial in SIDE (`y=525..575, x_local=125..195`)
sub_w = (s_rgb[525:575, 125:195].mean(axis=-1) < 95) & side["mask"][525:575, 125:195]
# Dilate slightly & inpaint strictly from adjacent bright warm skin pixels (`R - B > 30`) so right wrist is seamless tanned skin!
watch_mask_s[525:575, 125:195] = ndimage.binary_dilation(sub_w, iterations=3).astype(np.uint8) * 255
# Note: below pocket rim dark denim has R-B < 8; don't overwrite denim pocket! Sample median skin color directly above watch (`y=515..530`) for smooth wrist skin gradient!
s_rgb_nowatch = s_rgb_noglass.copy()
skin_patch = s_rgb_noglass[505:532, 165:198]
skin_valid = (skin_patch[:, :, 0].astype(int) - skin_patch[:, :, 2].astype(int)) > 30
skin_med = np.median(skin_patch[skin_valid], axis=0)
# Replace dark watch band inside arm contour with natural shaded wrist skin + gentle cylindrical shading!
arm_skin_zone = watch_mask_s > 0
s_rgb_nowatch[arm_skin_zone] = skin_med.astype(np.uint8)
s_rgb_nowatch = cv2.inpaint(s_rgb_nowatch, (ndimage.binary_dilation(arm_skin_zone, iterations=1) ^ arm_skin_zone).astype(np.uint8) * 255, 3, cv2.INPAINT_TELEA)

# Save clean plates & detected metadata
with open(f"{out_dir}/glasses_and_landmarks.json", "w") as fp:
    json.dump({"glasses_rings": glasses_rings, "fx0": fx0, "sx0": sx0, "bx0": 1075}, fp, indent=2)
np.save(f"{out_dir}/front_rgb_noglass.npy", f_rgb_noglass)
np.save(f"{out_dir}/side_rgb_noglass.npy", s_rgb_noglass)
np.save(f"{out_dir}/side_rgb_nowatch.npy", s_rgb_nowatch)

# Save 2x zoom verification panel of: (1) shoe soles x4, (2) front face before/after glasses wire removal, (3) side temple & wrist watch removal!
face_cmp = np.hstack([
    cv2.resize(f_rgb[210:335, 120:260], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(f_rgb_noglass[210:335, 120:260], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb[210:335, 65:205], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb_noglass[210:335, 65:205], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb_noglass[475:600, 105:245], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb_nowatch[475:600, 105:245], (280, 250), interpolation=cv2.INTER_CUBIC),
])
Image.fromarray(np.hstack(shoe_zooms)).save(f"{out_dir}/debug_04a_shoes_zoom.png")
Image.fromarray(face_cmp).save(f"{out_dir}/debug_04b_inpaint_zoom.png")
print("Saved debug_04a_shoes_zoom.png & debug_04b_inpaint_zoom.png")
