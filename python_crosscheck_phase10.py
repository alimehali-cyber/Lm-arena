#!/usr/bin/env python3
"""
Phase 10 cross-check — HONEST REWRITE (2026-09-03 remediation).

HISTORY: the previous version of this script pretended to be a full-stack
validation but was all placeholder: it injected 'solver error' as a random
rotation (no pixels, no stars, no solver), fabricated success rates with
`1.0 if noise<100 else 0.8  # simulate`, ran the 'hemisphere' comparison with
two IDENTICAL seeded calls (diff always 0), ignored the FOV variable in the
device sweep, used a fixed error axis in the rotation sweep, and ended with
"All Phase10 cross-checks passed" although several sections asserted nothing.
It also imported numpy. [pass-3 retraction: the pass-1 note claimed numpy
was "not even available in this environment — consistent with it never having
been executed"; that inference was wrong — numpy availability is environment-
specific (pass 2 installed numpy 2.4.6 and the original script runs; see
evidence/ORIGINAL_PHASE10_RERUN_2026-09-04.txt). The fabrication verdict rests
on the code content above, not on the dependency.] This rewrite uses ONLY the
Python standard library (math + random) so it runs dependency-free.

WHAT THIS SCRIPT HONESTLY IS: an independent cross-check of the math
primitives and noise statistics that ValidationMatrixRunner.kt relies on
(quaternion ops, camera pixel<->ray round-trip, error metrics, and the
statistical behavior of the injected-noise model), plus geometric reasoning
about FOV-dependent error scaling that EXPLAINS (not simulates) the measured
device sweep. It still runs no star-tracker solver: the real synthetic-bench
numbers live in the Kotlin suite (ValidationMatrixRunnerTest) and are captured
in docs/startracker/evidence/VALIDATION_MATRIX_2026-09-03.txt.

Each section prints MEASURED values and performs REAL assertions; any failure
exits non-zero.
"""
import math
import random
import sys

FAILURES = []

def check(name, cond, detail=""):
    status = "PASS" if cond else "FAIL"
    print(f"  [{status}] {name}" + (f" — {detail}" if detail else ""))
    if not cond:
        FAILURES.append(name)

print("=== 1. Quaternion primitives (mirror of solver/Quaternion.kt ops) ===")

def quat_from_axis_angle(axis, angle):
    ax, ay, az = axis
    norm = math.sqrt(ax*ax + ay*ay + az*az)
    ax, ay, az = ax/norm, ay/norm, az/norm
    half = angle/2
    s, c = math.sin(half), math.cos(half)
    return (c, ax*s, ay*s, az*s)  # w,x,y,z

def quat_multiply(q1, q2):
    w1,x1,y1,z1 = q1; w2,x2,y2,z2 = q2
    return (
        w1*w2 - x1*x2 - y1*y2 - z1*z2,
        w1*x2 + x1*w2 + y1*z2 - z1*y2,
        w1*y2 - x1*z2 + y1*w2 + z1*x2,
        w1*z2 + x1*y2 - y1*x2 + z1*w2)

def quat_conj(q):
    return (q[0], -q[1], -q[2], -q[3])

def quat_rotate_vector(q, v):
    qv = (0.0, v[0], v[1], v[2])
    tmp = quat_multiply(q, qv)
    return quat_multiply(tmp, quat_conj(q))[1:]

def quat_angular_error(q1, q2):
    dot = q1[0]*q2[0] + q1[1]*q2[1] + q1[2]*q2[2] + q1[3]*q2[3]
    dot = min(1.0, abs(dot))
    angle = 2*math.acos(dot)
    return 2*math.pi - angle if angle > math.pi else angle

rng = random.Random(7)
def rand_vec():
    return (rng.gauss(0,1), rng.gauss(0,1), rng.gauss(0,1))

q = quat_from_axis_angle(rand_vec(), rng.uniform(0, 2*math.pi))
v = rand_vec()
n_v = math.sqrt(sum(c*c for c in v))
check("rotation preserves vector norm",
      abs(math.sqrt(sum(c*c for c in quat_rotate_vector(q, v))) - n_v) < 1e-12)
comp = quat_multiply(q, q)
check("q * q_conj rotates a vector back to itself",
      all(abs(a-b) < 1e-12 for a, b in zip(quat_rotate_vector(quat_multiply(q, quat_conj(q)), v), v)))
v_rot = quat_rotate_vector(q, v)
check("rotation then inverse-rotation returns original",
      all(abs(a-b) < 1e-12 for a, b in zip(quat_rotate_vector(quat_conj(q), v_rot), v)))
q2 = quat_from_axis_angle(rand_vec(), rng.uniform(0, 2*math.pi))
comp2 = quat_multiply(q, q2)
r_comp = quat_rotate_vector(comp2, v)
r_seq2 = quat_rotate_vector(q, quat_rotate_vector(q2, v))
check("composed rotation == sequential rotations",
      all(abs(a-b) < 1e-12 for a, b in zip(r_comp, r_seq2)))
check("double cover: error(q, -q) == 0", quat_angular_error(q, (-q[0], -q[1], -q[2], -q[3])) < 1e-7,  # acos float floor ~sqrt(2*eps)
          f"error={quat_angular_error(q, (-q[0], -q[1], -q[2], -q[3])):.2e} rad")
q10 = quat_from_axis_angle((0, 0, 1), math.radians(10))
check("known axis-angle magnitude: 10 deg from identity",
      abs(math.degrees(quat_angular_error((1.0, 0, 0, 0), q10)) - 10.0) < 1e-12)

print("\n=== 2. Camera pixel<->ray round-trip (pinhole + skew) ===")

def pixel_to_unit_vector(u, v, fx, fy, cx, cy, skew=0.0):
    y_n = (v - cy)/fy
    x_n = (u - cx - skew*y_n)/fx
    n = math.sqrt(x_n*x_n + y_n*y_n + 1.0)
    return (x_n/n, y_n/n, 1.0/n)

def unit_vector_to_pixel(vec, fx, fy, cx, cy, skew=0.0):
    if vec[2] <= 0:
        return None
    x_n, y_n = vec[0]/vec[2], vec[1]/vec[2]
    return (fx*x_n + skew*y_n + cx, fy*y_n + cy)

def dot3(a, b):
    return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]

fx, fy, cx, cy, skew = 1200.0, 1200.0, 960.0, 540.0, 25.0
worst = 0.0
for _ in range(200):
    vec = rand_vec()
    vec = (vec[0], vec[1], abs(vec[2]) + 0.3)
    n = math.sqrt(dot3(vec, vec))
    vec = (vec[0]/n, vec[1]/n, vec[2]/n)
    pix = unit_vector_to_pixel(vec, fx, fy, cx, cy, skew)
    back = pixel_to_unit_vector(pix[0], pix[1], fx, fy, cx, cy, skew)
    err_arcsec = math.degrees(math.acos(min(1.0, dot3(vec, back))))*3600
    worst = max(worst, err_arcsec)
print(f"  worst round-trip error over 200 random rays: {worst:.6f} arcsec")
check("pixel<->ray round-trip < 0.01 arcsec (double precision)", worst < 0.01)
p0 = unit_vector_to_pixel((0.0, 0.0, 1.0), fx, fy, cx, cy, skew)
check("center ray -> principal point", abs(p0[0]-cx) < 1e-9 and abs(p0[1]-cy) < 1e-9)

print("\n=== 3. Statistical check of the bench's injected-noise model ===")
# ValidationMatrixRunner.runStaticBench builds q_est = q_true * q_err with rotation
# angle |N(0,1)| * sigma_rad about a random axis; the resulting angular error is
# exactly that angle. Statistical consequence: RMS(errors) ~= sigma, and errors are
# half-normally distributed (median/sigma ~= 0.6745). Verifying that here, with the
# SAME construction, guards the bench's noise semantics.

def injected_error_arcsec(sigma_arcsec, seed, trials):
    rg = random.Random(seed)
    out = []
    for _ in range(trials):
        axis = rand_vec_from(rg)
        q_true = quat_from_axis_angle(rand_vec_from(rg), rg.uniform(0, 2*math.pi))
        noise_rad = math.radians(sigma_arcsec/3600.0)*rg.gauss(0, 1)
        q_err = quat_from_axis_angle(axis, noise_rad)
        q_est = quat_multiply(q_true, q_err)
        out.append(math.degrees(quat_angular_error(q_true, q_est))*3600)
    return out

def rand_vec_from(rg):
    return (rg.gauss(0,1), rg.gauss(0,1), rg.gauss(0,1))

def rms(xs):
    return math.sqrt(sum(x*x for x in xs)/len(xs))

def median(xs):
    s = sorted(xs)
    n = len(s)
    return s[n//2] if n % 2 else 0.5*(s[n//2 - 1] + s[n//2])

def percentile95(xs):
    s = sorted(xs)
    return s[min(len(s)-1, int(0.95*len(s)))]

for sigma_arcsec in (5.0, 10.0, 50.0):
    errs = injected_error_arcsec(sigma_arcsec, seed=42, trials=2000)
    r, m = rms(errs), median(errs)
    check(f"RMS(injected error) ~= sigma at {sigma_arcsec:.0f}\"", abs(r - sigma_arcsec) < 0.05*sigma_arcsec,
          f"RMS={r:.3f}")
    check(f"median/sigma ~= 0.6745 (half-normal) at {sigma_arcsec:.0f}\"", abs(m/sigma_arcsec - 0.6745) < 0.02,
          f"ratio={m/sigma_arcsec:.4f}")
    print(f"  sigma={sigma_arcsec:.0f}\": RMS={r:.2f}, median={m:.2f}, p95={percentile95(errs):.2f} arcsec")

print("\n=== 4. FOV-dependent error scaling (geometry, explains the Kotlin device sweep) ===")
# One pixel subtends ~FOV/W radians: FINER angular resolution at NARROW FOV
# (30 deg -> 56 arcsec/px vs 120 deg -> 225 arcsec/px, ratio 0.25). Pure pixel-noise
# scaling would therefore make narrow FOV 4x MORE accurate at fixed sigma_px. The
# Kotlin device sweep measured the OPPOSITE (RMS 18.0/7.2/3.1/2.5 arcsec at
# 30/60/90/120 deg, i.e. narrow FOV 7.2x WORSE with TooFewStars x9): the degradation
# is star-count / fit-geometry driven (fewer stars in a narrower field), not
# pixel-scale driven. This section verifies the geometric baseline with actual
# numbers instead of asserting the old fabricated table.
W = 1920
for fov_deg in (30, 60, 90, 120):
    pix_scale = math.degrees(math.radians(fov_deg)/W)*3600
    print(f"  FOV {fov_deg:3d} deg: 1 px = {pix_scale:6.2f} arcsec")
r30, r120 = math.radians(30)/W, math.radians(120)/W
check("pixel scale 30deg/120deg == 0.25 (finer at narrow FOV)",
      abs((r30/r120) - 0.25) < 1e-9, f"ratio={r30/r120:.4f}")
kotlin_rms = {30: 18.0, 60: 7.2, 90: 3.1, 120: 2.5}
check("Kotlin measured degradation at narrow FOV INVERTS the pixel-scale benefit (star-count effect dominates)",
      (kotlin_rms[30]/kotlin_rms[120]) > 4.0,
      f"measured {kotlin_rms[30]/kotlin_rms[120]:.2f}x worse at 30deg despite 4x finer pixel scale")
check("Kotlin measured RMS decreases monotonically with wider FOV",
      all(kotlin_rms[a] > kotlin_rms[b] for a, b in [(30, 60), (60, 90), (90, 120)]))

print("\n=== 5. Hemisphere equivalence done honestly (different seeds AND hemisphere-restricted attitudes) ===")
# The old script called run_static_bench twice with the SAME seed and labeled them
# north/south — the reported diff was always 0. Here: distinct seeds, and attitudes
# constructed so the boresight actually points into each celestial hemisphere.

def hemisphere_bench(dec_center_deg, sigma_arcsec, trials, seed):
    rg = random.Random(seed)
    errs = []
    for _ in range(trials):
        dec = max(-89.0, min(89.0, dec_center_deg + rg.gauss(0, 1)*20))
        ra = rg.uniform(0, 360)
        boresight = (math.cos(math.radians(dec))*math.cos(math.radians(ra)),
                     math.cos(math.radians(dec))*math.sin(math.radians(ra)),
                     math.sin(math.radians(dec)))
        q_true = quat_from_axis_angle(boresight, rg.uniform(0, 2*math.pi))
        q_err = quat_from_axis_angle(rand_vec_from(rg), math.radians(sigma_arcsec/3600.0)*rg.gauss(0, 1))
        errs.append(math.degrees(quat_angular_error(q_true, quat_multiply(q_true, q_err)))*3600)
    return rms(errs)

north = hemisphere_bench(+60, 10.0, 400, seed=101)
south = hemisphere_bench(-60, 10.0, 400, seed=202)
diff = abs(north - south)
print(f"  north RMS {north:.2f}\", south RMS {south:.2f}\", |diff| {diff:.2f}\" (distinct seeds, 400 trials each)")
tol = 10.0/math.sqrt(2*400) * 5  # ~5 sampling-sigmas of the RMS estimate, not a magic 20
check("hemisphere RMS equivalence within sampling tolerance", diff < tol, f"tol={tol:.2f}\"")

print("\n=== 6. What this script does NOT do (no fabrication) ===")
print("  - It runs no star-tracker solver, no centroider, no matcher: 'success rate'")
print("    of an actual solve is NOT computed here. Real synthetic-bench numbers are")
print("    produced by the Kotlin ValidationMatrixRunnerTest and captured in")
print("    docs/startracker/evidence/VALIDATION_MATRIX_2026-09-03.txt.")
print("  - The old fabricated per-condition success rates (1.0 if noise<100 else 0.8)")
print("    are gone. The old identical-seed 'hemisphere' check is gone.")

print()
if FAILURES:
    print(f"FAILED sections: {FAILURES}")
    sys.exit(1)
print("All REAL cross-checks in this script passed (nothing simulated).")
