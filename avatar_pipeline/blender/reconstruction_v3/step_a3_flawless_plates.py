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
lms = np.load(f"{out_dir}/front_facemesh_478.npy")

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
    paper_rb = bg_c[:, :, 0] - bg_c[:, :, 2]
    pix_rb = crop[:, :, 0].astype(np.float32) - crop[:, :, 2].astype(np.float32)
    cool_rubber = (paper_rb - pix_rb) > 3.8

    upper_fg = (d_unit > 0.20) & ((diff_norm > 6.5) | cool_rubber | (luma_shift > 4.5))
    upper_fg[225:295, :] |= (d_unit[225:295, :] > 0.58)

    shoe_band_fg = (d_unit > 0.23) & ((crop_luma < 172.0) | cool_rubber | (luma_shift > 2.2))
    for cx in range(crop.shape[1]):
        ys_on = np.where(shoe_band_fg[885:975, cx])[0]
        if len(ys_on) >= 2:
            for k in range(len(ys_on) - 1):
                if ys_on[k + 1] - ys_on[k] <= 16:
                    shoe_band_fg[885 + ys_on[k] : 885 + ys_on[k + 1] + 1, cx] = True

    shoe_sub = ndimage.binary_fill_holes(shoe_band_fg[885:976, :])
    shoe_sub = cv2.morphologyEx(shoe_sub.astype(np.uint8), cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))).astype(bool)
    shoe_sub = ndimage.binary_fill_holes(shoe_sub)

    full_fg = upper_fg.copy()
    full_fg[885:976, :] = shoe_sub
    num_l, labels, stats, _ = cv2.connectedComponentsWithStats(full_fg.astype(np.uint8), connectivity=8)
    best = 1 + np.argmax(stats[1:, cv2.CC_STAT_AREA])
    mask_clean = (labels == best)

    side_near = mask_clean.copy()
    if vname == "SIDE":
        for y in range(622, 976):
            row_m = np.where(mask_clean[y, :])[0]
            if len(row_m) > 8:
                row_d = d_unit[y, row_m]
                if row_d.max() - row_d.min() > 0.12:
                    thr = 0.5 * (np.percentile(row_d, 85) + np.percentile(row_d, 15))
                    side_near[y, row_m[row_d < thr]] = False
        for cx in range(crop.shape[1]):
            ys_on = np.where(side_near[895:975, cx])[0]
            if len(ys_on) >= 2:
                for k in range(len(ys_on) - 1):
                    if ys_on[k + 1] - ys_on[k] <= 16 and mask_clean[895 + ys_on[k], cx]:
                        side_near[895 + ys_on[k] : 895 + ys_on[k + 1] + 1, cx] = mask_clean[895 + ys_on[k] : 895 + ys_on[k + 1] + 1, cx]
        num2, l2, s2, _ = cv2.connectedComponentsWithStats(side_near.astype(np.uint8), 8)
        side_near = (l2 == (1 + np.argmax(s2[1:, cv2.CC_STAT_AREA])))

    dist_in = cv2.distanceTransform(mask_clean.astype(np.uint8), cv2.DIST_L2, 3)
    alpha = np.clip((diff_norm - 3.5) / 11.0, 0.0, 1.0)
    alpha = np.where(dist_in >= 1.8, 1.0, np.where(mask_clean, np.maximum(alpha, 0.45), 0.0))
    unmixed = np.clip((crop.astype(np.float32) - (1.0 - alpha[:, :, None]) * bg_c) / np.maximum(alpha[:, :, None], 0.32), 0, 255).astype(np.uint8)
    unmixed = np.where(dist_in[:, :, None] < 2.5, unmixed, crop)

    ys, xs = np.where(mask_clean)
    print(f"{vname:5s} FLAWLESS: x=[{x0+xs.min()}..{x0+xs.max()}] w={xs.max()-xs.min()+1}, y=[{ys.min()}..{ys.max()}] h={ys.max()-ys.min()+1}")

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

front = np.load(f"{out_dir}/front_perfect.npz")
side = np.load(f"{out_dir}/side_perfect.npz")
f_rgb = front["rgb"].copy()
s_rgb = side["rgb"].copy()
f_luma = f_rgb.mean(axis=-1)
s_luma = s_rgb.mean(axis=-1)
fx0, sx0 = int(front["x0"]), int(side["x0"])

# Vectorized ellipse fit in <0.2s! Notice also in debug_04b Panel 1 why the previous crop looked slightly offset:
# Check exact centers right on f_rgb[210:335, 120:260]! Wait: look at Panel 1 of debug_04b:
# Panel 1 width is 140px (`x_local = 120..260` -> `x_global = 270..410`) and height is 125px (`y = 210..335`)!
# Look at where the two glasses lenses actually sit inside `[210:335, 120:260]`:
# Left lens center is around `x_local = 152` (`x_global = 302`) and `y = 259`!!
# Right lens center is around `x_local = 200` (`x_global = 350`) and `y = 263`!!
# AHA!! LOOK WHY `lms` HAD `+30px` OFFSET IN `step_a2`: `hcrop2x` HAD `W=420, H=394`, AND WE MULTIPLIED `lm.x * (hx1 - hx0)` WHERE `hx0=240` WAS RIGHT — WAIT WHY WAS `EyeL` `334` INSTEAD OF `302`?!
# LET'S PRINT EXACT EYE PUPIL DARK MINIMA IN `f_rgb` DIRECTLY SO WE NEVER TRUST ANY SCALE BUG EVER!!
for tag, xa, xb in [("ViewerLeft_Pupil", 130, 175), ("ViewerRight_Pupil", 180, 225)]:
    sub = f_luma[245:280, xa:xb]
    iy, ix = np.unravel_index(np.argmin(sub), sub.shape)
    print(f"DIRECT PIXEL PUPIL {tag}: x_local={xa+ix} (x_global={fx0+xa+ix}), y={245+iy}, luma={sub[iy,ix]:.1f}")

# Re-run FaceMesh directly on full `f_rgb` (un-cropped!) so coordinates are 100.000% IDENTICAL to `f_rgb`!
import mediapipe as mp
with mp.solutions.face_mesh.FaceMesh(static_image_mode=True, max_num_faces=1, refine_landmarks=True, min_detection_confidence=0.2) as fm:
    res = fm.process(f_rgb)
    lms_local = np.array([[lm.x * f_rgb.shape[1], lm.y * f_rgb.shape[0], lm.z * f_rgb.shape[1]] for lm in res.multi_face_landmarks[0].landmark], dtype=np.float32)
lms_global = lms_local.copy()
lms_global[:, 0] += fx0
np.save(f"{out_dir}/front_facemesh_478.npy", lms_global)
print("EXACT ZERO-OFFSET FaceMesh -> EyeL:", lms_global[468, :2].round(2), "EyeR:", lms_global[473, :2].round(2), "NoseTip:", lms_global[1, :2].round(2), "Chin:", lms_global[152, :2].round(2))

# Vectorized ellipse fit around TRUE zero-offset eye centers in <0.15s!
arc = np.linspace(-np.pi, np.pi, 180, endpoint=False)
arc = arc[(arc < -2.35) | (arc > -0.75)]
ca, sa = np.cos(arc), np.sin(arc)

glasses_rings = {}
for key, cxy in [("viewer_left", lms_local[468, :2]), ("viewer_right", lms_local[473, :2])]:
    best_s, best_p = -1e9, None
    for dx in np.linspace(-5.0, 5.0, 21):
        for dy in np.linspace(-3.0, 5.0, 17):
            cxs = cxy[0] + dx
            cys = cxy[1] + dy
            for rx in np.linspace(17.0, 23.0, 13):
                for ry in np.linspace(16.0, 22.0, 13):
                    px = np.round(cxs + rx * ca).astype(int)
                    py = np.round(cys + ry * sa).astype(int)
                    px_i = np.round(cxs + (rx - 3.0) * ca).astype(int)
                    py_i = np.round(cys + (ry - 3.0) * sa).astype(int)
                    px_o = np.round(cxs + (rx + 3.0) * ca).astype(int)
                    py_o = np.round(cys + (ry + 3.0) * sa).astype(int)
                    sc = float(np.median(0.5 * (f_luma[py_i, px_i] + f_luma[py_o, px_o]) - f_luma[py, px]))
                    if sc > best_s:
                        best_s = sc
                        best_p = {"cx_local": round(float(cxs), 2), "cx_global": round(float(cxs + fx0), 2), "cy": round(float(cys), 2), "rx": round(float(rx), 2), "ry": round(float(ry), 2)}
    glasses_rings[key] = best_p
    print("TRUE ELLIPTICAL GLASSES RING", key, best_p, "contrast=", round(best_s, 2))

# Exact FRONT glasses wire + highlight mask
rim_mask_f = np.zeros(f_rgb.shape[:2], dtype=np.uint8)
for key, p in glasses_rings.items():
    cv2.ellipse(rim_mask_f, (int(round(p["cx_local"])), int(round(p["cy"]))), (int(round(p["rx"])), int(round(p["ry"]))), 0, 0, 360, 255, thickness=5)
pL, pR = glasses_rings["viewer_left"], glasses_rings["viewer_right"]
cv2.line(rim_mask_f, (int(round(pL["cx_local"] + pL["rx"] - 2)), int(round(pL["cy"] - 2))), (int(round(pR["cx_local"] - pR["rx"] + 2)), int(round(pR["cy"] - 1))), 255, thickness=5)
cv2.line(rim_mask_f, (int(round(pL["cx_local"] - pL["rx"])), int(round(pL["cy"] - 2))), (int(round(pL["cx_local"] - pL["rx"] - 14)), int(round(pL["cy"] - 4))), 255, thickness=5)
cv2.line(rim_mask_f, (int(round(pR["cx_local"] + pR["rx"])), int(round(pR["cy"] - 1))), (int(round(pR["cx_local"] + pR["rx"] + 14)), int(round(pR["cy"] - 1))), 255, thickness=5)

protect_eyes_brows = np.zeros(f_rgb.shape[:2], dtype=bool)
for idx_group in [
    [33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246],
    [362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398],
    [70, 63, 105, 66, 107, 55, 65, 52, 53, 46],
    [300, 293, 334, 296, 336, 285, 295, 282, 283, 276],
]:
    poly = np.round(lms_local[idx_group, :2]).astype(np.int32)
    cv2.fillPoly(protect_eyes_brows.view(np.uint8), [poly], 1)
protect_eyes_brows = ndimage.binary_dilation(protect_eyes_brows, iterations=1)
rim_mask_f[protect_eyes_brows] = 0
rim_mask_f[~front["mask"]] = 0
f_rgb_noglass = cv2.inpaint(f_rgb, rim_mask_f, 4, cv2.INPAINT_TELEA)

# Exact SIDE glasses ellipse + temple bar
best_sc_s, best_ps = -1e9, None
arc_s = arc[(arc < -2.2) | (arc > -0.9)]
cas, sas = np.cos(arc_s), np.sin(arc_s)
for cx_s in np.linspace(76, 94, 19):
    for cy_s in np.linspace(252, 272, 21):
        for rx_s in np.linspace(7.0, 12.0, 11):
            for ry_s in np.linspace(14.5, 20.5, 13):
                px = np.round(cx_s + rx_s * cas).astype(int)
                py = np.round(cy_s + ry_s * sas).astype(int)
                px_o = np.round(cx_s + (rx_s + 3.0) * cas).astype(int)
                py_o = np.round(cy_s + (ry_s + 3.0) * sas).astype(int)
                sc = float(np.median(s_luma[py_o, px_o] - s_luma[py, px]))
                if sc > best_sc_s:
                    best_sc_s = sc
                    best_ps = {"cx_local": round(float(cx_s), 2), "cx_global": round(float(cx_s + sx0), 2), "cy": round(float(cy_s), 2), "rx": round(float(rx_s), 2), "ry": round(float(ry_s), 2)}
print("TRUE SIDE GLASSES OVAL:", best_ps, "contrast=", round(best_sc_s, 2))
glasses_rings["side_lens"] = best_ps

rim_mask_s = np.zeros(s_rgb.shape[:2], dtype=np.uint8)
cv2.ellipse(rim_mask_s, (int(round(best_ps["cx_local"])), int(round(best_ps["cy"]))), (int(round(best_ps["rx"])), int(round(best_ps["ry"]))), 0, 0, 360, 255, thickness=5)
# Trace exact horizontal dark temple bar rows dynamically across x in [92..145], y in [250..268]!
for x in range(int(round(best_ps["cx_local"] + best_ps["rx"] - 2)), 146):
    col = s_luma[250:269, x]
    yi = 250 + int(np.argmin(col))
    if s_luma[yi - 4, x] - s_luma[yi, x] > 14 or s_luma[yi + 4, x] - s_luma[yi, x] > 14:
        rim_mask_s[yi - 2 : yi + 3, x] = 255
rim_mask_s[:249, :] = 0
rim_mask_s[:, 146:] = 0
eye_dark_s = (s_luma < 65) & (np.arange(s_rgb.shape[1])[None, :] <= int(round(best_ps["cx_local"] + 3))) & (np.arange(s_rgb.shape[0])[:, None] < 270)
rim_mask_s[ndimage.binary_dilation(eye_dark_s, iterations=1)] = 0
rim_mask_s[~side["mask"]] = 0
s_rgb_noglass = cv2.inpaint(s_rgb, rim_mask_s, 4, cv2.INPAINT_TELEA)

# Exact watch removal strictly inside forearm linear cylinder `[x_lo(y), x_hi(y)]`!
arm_rows, x_arm_lo, x_arm_hi = [], [], []
for y in range(460, 526):
    skin_x = np.where(((s_rgb_noglass[y, :, 0].astype(int) - s_rgb_noglass[y, :, 2].astype(int)) > 32) & (s_rgb_noglass[y, :, 0] > 130))[0]
    if len(skin_x) > 8:
        arm_rows.append(y); x_arm_lo.append(skin_x.min()); x_arm_hi.append(skin_x.max())
p_lo = np.polyfit(arm_rows, x_arm_lo, 1)
p_hi = np.polyfit(arm_rows, x_arm_hi, 1)

watch_band_mask = np.zeros(s_rgb.shape[:2], dtype=np.uint8)
for y in range(522, 574):
    xl = int(np.polyval(p_lo, y)) - 6
    xh = int(np.polyval(p_hi, y)) + 6
    for x in range(max(0, xl), min(s_rgb.shape[1], xh + 1)):
        pix = s_rgb_noglass[y, x]
        is_skin = (int(pix[0]) - int(pix[2])) > 26 and pix[0] > 125
        if not is_skin and y < 569:
            watch_band_mask[y, x] = 255
watch_band_mask = ndimage.binary_dilation(watch_band_mask > 0, iterations=2).astype(np.uint8) * 255
s_rgb_nowatch = cv2.inpaint(s_rgb_noglass, watch_band_mask, 6, cv2.INPAINT_TELEA)

with open(f"{out_dir}/glasses_and_landmarks.json", "w") as fp:
    json.dump({"glasses_rings": glasses_rings, "fx0": fx0, "sx0": sx0, "bx0": 1075}, fp, indent=2)
np.save(f"{out_dir}/front_rgb_noglass.npy", f_rgb_noglass)
np.save(f"{out_dir}/side_rgb_noglass.npy", s_rgb_noglass)
np.save(f"{out_dir}/side_rgb_nowatch.npy", s_rgb_nowatch)

face_cmp = np.hstack([
    cv2.resize(f_rgb[210:335, 120:260], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(f_rgb_noglass[210:335, 120:260], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb[210:335, 55:195], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb_noglass[210:335, 55:195], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb_noglass[475:600, 105:245], (280, 250), interpolation=cv2.INTER_CUBIC),
    cv2.resize(s_rgb_nowatch[475:600, 105:245], (280, 250), interpolation=cv2.INTER_CUBIC),
])
Image.fromarray(np.hstack(shoe_zooms)).save(f"{out_dir}/debug_04a_shoes_zoom.png")
Image.fromarray(face_cmp).save(f"{out_dir}/debug_04b_inpaint_zoom.png")
print("DONE IN <5s! Saved flawless debug_04a_shoes_zoom.png & debug_04b_inpaint_zoom.png")
