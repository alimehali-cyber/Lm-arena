# Z-S4 — D4 centroid table + D5 distortion table (2026-09-04)

Probe `S4CentroidDistortionProbe.kt` (real chain: GrayscaleImage → BackgroundEstimator →
StarBlobDetector(5σ) → Centroider moment + Gaussian fit; 50 deterministic sub-pixel
placements per cell, Box-Muller noise σ=1 ADU, flat background 10, SNR = peak/noise-σ).
All numbers SYNTHETIC-SKY (simulated images), raw output in
`evidence/S4_CENTROID_DISTORTION_RAW_2026-09-04.txt`.

## D4 — sub-pixel centroid error (mean |err| px; 1 px = 57″ = 0.95′)

| σ \ SNR | 5 | 10 | 30 |
|---|---|---|---|
| 0.8 px | NO DETECTION¹ | mom 0.126 / fit 0.415 (30/50 detected²) | mom 0.071 / fit 0.386 |
| 1.2 px | NO DETECTION¹ | mom 0.131 / fit 0.337 | mom 0.054 / fit 0.329 |
| 2.0 px | mom 0.446 / fit 0.422 (8/50 detected²) | mom 0.163 / fit 0.220 | mom 0.057 / fit 0.079 |

¹ SNR 5 with σ ≤ 1.2 px never crosses the detector's 5σ threshold with ≥3 pixels —
  the star is undetectable by design (honest finding, not a defect: threshold × PSF
  width sets the detection floor at SNR·(pixels above 5σ)).
² Detection is probabilistic at the threshold edge (sub-pixel position dependent);
  detected-fraction reported, errors averaged over detections.
Systematic biases ≤ 0.08 px (fit) / ≤ 0.04 px (moment) except the marginal σ2.0/SNR10
row. Note: moment centroid BEATS the Gaussian fit for tight PSFs (σ0.8: 0.07–0.13 px
vs 0.33–0.42 px); the fit wins only for broad PSFs (σ2.0) at high SNR.

## D5 — Brown-Conrady k1 = −0.05 (k2 = p1 = p2 = 0), 63.5° tier, via the REAL DistortionModel

Tier geometry: short-side half-width 540 px, f = (1080/2)/tan(31.75°) = 872.63 px.

| field position | radius (px) | r_norm | displacement (px) | undistort round-trip error |
|---|---|---|---|---|
| 50% half-width | 270 | 0.3094 | −1.292 | 4×10⁻⁶ px |
| 75% half-width | 405 | 0.4641 | −4.362 | 5×10⁻⁶ px |
| 100% half-width (field edge) | 540 | 0.6188 | −10.339 | 6×10⁻⁶ px |

Interpretation: an UNMODELLED k1 = −0.05 barrel distortion shifts star positions by up
to **10.3 px (9.8′) at the field edge** — 5× the full-field-verification tolerance
(300″ = 5.3 px) at the edge, confirming k1 must either be calibrated (DistortionRefiner
exists for this) or the verification tolerance must account for it before device trials.
The iterative undistort inversion is effectively exact (round-trip ≤ 6×10⁻⁶ px).

KIND: measurement on synthetic images (SYNTHETIC-SKY); no production code changed.
Loosened: none.
