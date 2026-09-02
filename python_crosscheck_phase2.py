#!/usr/bin/env python3
"""
Manual cross-check of Phase 2 core numeric logic vs Kotlin implementation.
Per Phase 3 Task 0 step 5: port CORE NUMERIC LOGIC ONLY (weighted centroid + sigma-clipped background median)
into throwaway Python script, run against hand-constructed tiny example, compare to Kotlin logic by hand.

This is labeled as "manual cross-check, not equivalent to running actual test suite."
"""
import numpy as np
import math

print("=== Phase 2 Manual Cross-Check ===")
print("Python + numpy version:", np.__version__)

# --- Test 1: Weighted centroid ---
# Hand-constructed tiny example: 3x3 patch with known intensities
# Simulate star at (1.3, 1.7) sub-pixel, sigma 1.0, amplitude 100, bg 20
# Create synthetic data similar to Kotlin SyntheticStarFieldGenerator

def gaussian_2d(x, y, cx, cy, amp, sigma):
    dx = x - cx
    dy = y - cy
    r2 = dx*dx + dy*dy
    return amp * math.exp(-r2 / (2*sigma*sigma))

width, height = 5, 5
bg = 20.0
cx_true, cy_true = 2.3, 2.7
amp = 100.0
sigma = 1.0

# Build image
image = np.full((height, width), bg, dtype=float)
for y in range(height):
    for x in range(width):
        image[y, x] += gaussian_2d(x, y, cx_true, cy_true, amp, sigma)

print("\n--- Weighted Centroid Test ---")
print(f"True center: ({cx_true}, {cy_true})")
print("Image patch (5x5):")
print(image)

# Background estimation: assume perfect bg = 20
bg_map = np.full((height, width), bg, dtype=float)
residual = image - bg_map

# Weighted centroid logic from Kotlin Centroider.kt:
# - Include margin 1 around blob bounding box
# - Only positive residuals
# - Weight = residual
# - Exclude saturated (none here)
# For this test, we manually define blob as all pixels with residual > threshold (5*sigma noise, noise=2 => thresh 10)
threshold = 10.0
mask = residual > threshold
print(f"\nResidual > {threshold} mask:")
print(mask.astype(int))

# Compute weighted centroid over masked region + margin 1
# Find bounding box of mask
ys, xs = np.where(mask)
if len(xs) > 0:
    min_x, max_x = xs.min(), xs.max()
    min_y, max_y = ys.min(), ys.max()
    x0 = max(0, min_x - 1)
    x1 = min(width-1, max_x + 1)
    y0 = max(0, min_y - 1)
    y1 = min(height-1, max_y + 1)

    sum_w = 0.0
    sum_x = 0.0
    sum_y = 0.0
    for y in range(y0, y1+1):
        for x in range(x0, x1+1):
            res = residual[y, x]
            if res <= 0:
                continue
            w = res
            sum_w += w
            sum_x += w * x
            sum_y += w * y

    cx_est = sum_x / sum_w if sum_w>0 else 0
    cy_est = sum_y / sum_w if sum_w>0 else 0

    err = math.hypot(cx_est - cx_true, cy_est - cy_true)
    print(f"\nWeighted centroid result: ({cx_est:.4f}, {cy_est:.4f})")
    print(f"Error vs true: {err:.4f} px")
    print(f"Expected: <0.3 px for high SNR (amp 100, noise 0)")
    if err < 0.3:
        print("PASS: centroid error <0.3 px")
    else:
        print(f"FAIL: error {err:.4f} >=0.3 px")

    # Manual trace vs Kotlin logic:
    # Kotlin: sumW += w, sumX += w*x, sumY += w*y, where w = residual (raw-bg)
    # Same as Python above. So logic matches.
    print("\nManual trace: Kotlin logic identical to Python implementation above.")
    print("Kotlin Centroider.kt lines: for y in y0..y1, for x in x0..x1, residual = raw-bg, if residual<=0 continue, w=residual, sumW+=w, sumX+=w*x, etc.")
    print("Python implements same loop → expected identical result if run as Kotlin.")

else:
    print("No pixels above threshold!")

# --- Test 2: Sigma-clipped background median ---
print("\n--- Sigma-Clipped Background Median Test ---")
# Create block with 20 background pixels ~20 +/-1 and 2 star pixels 150
np.random.seed(42)
bg_pixels = np.random.normal(20, 1, 20)
star_pixels = np.array([150.0, 145.0])
all_pixels = np.concatenate([bg_pixels, star_pixels])
print(f"Block pixels: {len(all_pixels)} total (20 bg ~20, 2 stars ~150)")
print(f"Mean without clipping: {all_pixels.mean():.2f} (should be high due to stars)")
print(f"Median without clipping: {np.median(all_pixels):.2f} (should be ~20, robust)")

# Kotlin BackgroundEstimator sigma-clipped median logic:
# 1. Initial estimate = median
# 2. For iter in 0..sigmaClipIterations-1:
#    mean = mean(currentValues), variance, sigma = sqrt(var)
#    clip to mean ± k*sigma, keep values within
#    if clipped size < half or empty, break
#    currentValues = clipped, estimate = mean
# 3. Final = median of clipped set

def sigma_clipped_median_kotlin_logic(values, k=3.0, iters=2):
    current = list(values)
    # initial estimate median
    estimate = np.median(current)
    for it in range(iters):
        mean = np.mean(current)
        var = np.mean((np.array(current)-mean)**2)
        sigma = math.sqrt(max(0, var))
        if sigma < 1e-6:
            estimate = mean
            break
        lower = mean - k*sigma
        upper = mean + k*sigma
        clipped = [v for v in current if lower <= v <= upper]
        print(f"  Iter {it}: mean={mean:.2f}, sigma={sigma:.2f}, clip [{lower:.2f},{upper:.2f}], kept {len(clipped)}/{len(current)}")
        if len(clipped) < len(current)/2 or len(clipped)==0:
            estimate = mean
            break
        current = clipped
        estimate = mean
    final_median = np.median(current)
    print(f"  Final clipped median: {final_median:.2f} from {len(current)} values")
    return final_median

result = sigma_clipped_median_kotlin_logic(all_pixels, k=3.0, iters=2)
print(f"\nSigma-clipped result: {result:.2f}")
print(f"Expected: close to true bg 20, not contaminated by stars")
if abs(result - 20) < 2:
    print("PASS: robust to stars")
else:
    print(f"FAIL: result {result:.2f} far from 20")

print("\n=== Manual Cross-Check Summary ===")
print("Weighted centroid logic: Kotlin and Python identical → expected same numeric result if Kotlin executed.")
print("Sigma-clipped median: Kotlin logic robust to bright stars, median ~20 despite 2 stars at 150.")
print("This is manual cross-check, NOT equivalent to running actual test suite via Gradle/JUnit.")
print("Full automated execution still BLOCKED after 3 phases (Phase 1,2,3) without JVM.")
