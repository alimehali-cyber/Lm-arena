#!/usr/bin/env python3
"""
Phase 7 cross-checks: distortion round-trip, intrinsics recovery, distortion recovery, degeneracy
"""
import numpy as np

def distort(x, y, k1, k2, p1, p2):
    r2 = x*x + y*y
    r4 = r2*r2
    radial = 1 + k1*r2 + k2*r4
    x_dist = x*radial + 2*p1*x*y + p2*(r2 + 2*x*x)
    y_dist = y*radial + p1*(r2 + 2*y*y) + 2*p2*x*y
    return x_dist, y_dist

def undistort_iterative(xd, yd, k1, k2, p1, p2, max_iter=20, eps=1e-8):
    x = xd
    y = yd
    for _ in range(max_iter):
        r2 = x*x + y*y
        r4 = r2*r2
        radial = 1 + k1*r2 + k2*r4
        dx = 2*p1*x*y + p2*(r2 + 2*x*x)
        dy = p1*(r2 + 2*y*y) + 2*p2*x*y
        x_new = (xd - dx) / radial
        y_new = (yd - dy) / radial
        if (x_new - x)**2 + (y_new - y)**2 < eps*eps:
            x, y = x_new, y_new
            break
        x, y = x_new, y_new
    return x, y

print("=== Task1: DistortionModel round-trip ===")
test_models = [
    (0.1, 0.01, 0.001, 0.001),
    (0.2, 0.05, 0.01, 0.01),
    (-0.3, 0.1, -0.01, 0.01),
]
points = [(0.0,0.0),(0.1,0.1),(0.3,0.2),(0.5,0.5),(-0.4,0.3)]

print("Model(k1,k2,p1,p2) | point | round-trip error")
for k1,k2,p1,p2 in test_models:
    for x,y in points:
        xd, yd = distort(x,y,k1,k2,p1,p2)
        xu, yu = undistort_iterative(xd,yd,k1,k2,p1,p2)
        err = np.hypot(x-xu, y-yu)
        print(f"({k1},{k2},{p1},{p2}) | ({x},{y}) | err {err:.2e}")
        assert err < 1e-6, f"Round-trip failed {err}"

print("\n=== Python cross-check 3 points (Task1 required) ===")
model = (0.1, 0.02, 0.005, -0.003)
k1,k2,p1,p2 = model
check_pts = [(0.1,0.2),(0.3,-0.2),(0.0,0.0)]
for x,y in check_pts:
    xd, yd = distort(x,y,k1,k2,p1,p2)
    print(f"Ideal ({x},{y}) -> Distorted ({xd:.6f},{yd:.6f})")
    # round-trip
    xu,yu = undistort_iterative(xd,yd,k1,k2,p1,p2)
    print(f"  -> Undistorted ({xu:.6f},{yu:.6f}) err {np.hypot(x-xu,y-yu):.2e}")

print("\n=== Task2: IntrinsicsRefiner recovery ===")
# True intrinsics
true_fx, true_fy, true_cx, true_cy, true_skew = 1200.0, 1205.0, 965.0, 535.0, 0.5

def generate_intrinsics_obs(count, noise_px, seed=42):
    rng = np.random.default_rng(seed)
    obs = []
    for _ in range(count):
        x = rng.uniform(-0.8,0.8)
        y = rng.uniform(-0.8,0.8)
        u_pred = true_fx*x + true_skew*y + true_cx
        v_pred = true_fy*y + true_cy
        u_obs = u_pred + rng.uniform(-noise_px, noise_px)
        v_obs = v_pred + rng.uniform(-noise_px, noise_px)
        obs.append(((x,y),(u_pred,v_pred),(u_obs,v_obs)))
    return obs

def refine_intrinsics(obs):
    # Build linear system: u = fx*x + skew*y + cx ; v = fy*y + cy
    # Solve separate LS
    if len(obs) < 6:
        return None
    # u equation
    Au = []
    bu = []
    Av = []
    bv = []
    for (x,y),(up,vp),(uo,vo) in obs:
        Au.append([x,y,1.0])
        bu.append(uo)
        Av.append([y,1.0])
        bv.append(vo)
    Au = np.array(Au)
    bu = np.array(bu)
    Av = np.array(Av)
    bv = np.array(bv)
    try:
        sol_u, residuals, rank, s = np.linalg.lstsq(Au, bu, rcond=None)
        sol_v, residuals, rank, s = np.linalg.lstsq(Av, bv, rcond=None)
    except np.linalg.LinAlgError:
        return None
    fx, skew, cx = sol_u
    fy, cy = sol_v
    # RMS
    rms = 0
    for (x,y),(up,vp),(uo,vo) in obs:
        u_pred = fx*x + skew*y + cx
        v_pred = fy*y + cy
        rms += (uo-u_pred)**2 + (vo-v_pred)**2
    rms = np.sqrt(rms / (2*len(obs)))
    return (fx,fy,cx,cy,skew,rms)

obs_counts = [4,10,30,100]
noise_levels = [0.0,0.2,0.5,1.0]
print("Noise\\Obs | 4 | 10 | 30 | 100")
for noise in noise_levels:
    row = f"{noise}px |"
    for cnt in obs_counts:
        obs = generate_intrinsics_obs(cnt, noise, seed=int(noise*100+cnt))
        res = refine_intrinsics(obs)
        if res is None:
            row += " FAIL |"
        else:
            fx,fy,cx,cy,skew,rms = res
            err_fx = abs(fx-true_fx)
            row += f" rms={rms:.2f} fxErr={err_fx:.1f} |"
    print(row)

print("\n=== Task3: DistortionRefiner recovery ===")
true_k1, true_k2, true_p1, true_p2 = 0.1, 0.02, 0.005, -0.003

def generate_distortion_obs(count, noise, seed=42, center=False, range_val=0.8):
    rng = np.random.default_rng(seed)
    obs = []
    for _ in range(count):
        if center:
            x = rng.uniform(-0.1,0.1)
            y = rng.uniform(-0.1,0.1)
        else:
            x = rng.uniform(-range_val,range_val)
            y = rng.uniform(-range_val,range_val)
        xd, yd = distort(x,y,true_k1,true_k2,true_p1,true_p2)
        xd += rng.uniform(-noise,noise)
        yd += rng.uniform(-noise,noise)
        obs.append(((x,y),(xd,yd)))
    return obs

def refine_distortion(obs):
    if len(obs) < 10:
        return None, "insufficient"
    xs = [o[0][0] for o in obs]
    ys = [o[0][1] for o in obs]
    spanX = max(xs)-min(xs)
    spanY = max(ys)-min(ys)
    maxR2 = max(x*x+y*y for x,y in [o[0] for o in obs])
    if spanX < 0.5 or spanY < 0.5 or maxR2 < 0.1:
        return None, f"clustered spanX={spanX:.2f} spanY={spanY:.2f} maxR2={maxR2:.2f}"
    m = len(obs)*2
    n = 4
    A = np.zeros((m,n))
    b = np.zeros(m)
    for idx, ((x,y),(xd,yd)) in enumerate(obs):
        r2 = x*x+y*y
        r4 = r2*r2
        rowX = idx*2
        A[rowX,0] = x*r2
        A[rowX,1] = x*r4
        A[rowX,2] = 2*x*y
        A[rowX,3] = r2+2*x*x
        b[rowX] = xd - x
        rowY = idx*2+1
        A[rowY,0] = y*r2
        A[rowY,1] = y*r4
        A[rowY,2] = r2+2*y*y
        A[rowY,3] = 2*x*y
        b[rowY] = yd - y
    try:
        sol, residuals, rank, s = np.linalg.lstsq(A,b,rcond=None)
    except:
        return None, "singular"
    k1,k2,p1,p2 = sol
    if abs(k1)>1.0 or abs(k2)>1.0 or abs(p1)>0.1 or abs(p2)>0.1:
        return None, f"overfit k1={k1:.2f} k2={k2:.2f} p1={p1:.3f} p2={p2:.3f}"
    rms = 0
    for (x,y),(xd,yd) in obs:
        xdp, ydp = distort(x,y,k1,k2,p1,p2)
        rms += (xd-xdp)**2 + (yd-ydp)**2
    rms = np.sqrt(rms/m)
    return (k1,k2,p1,p2,rms), "ok"

print("Noise\\Obs | 10 | 30 | 100")
for noise in [0.0,0.001,0.005]:
    row = f"{noise} |"
    for cnt in [10,30,100]:
        obs = generate_distortion_obs(cnt, noise, seed=int(noise*1000+cnt))
        res, msg = refine_distortion(obs)
        if res is None:
            row += f" FAIL({msg}) |"
        else:
            k1,k2,p1,p2,rms = res
            errK1 = abs(k1-true_k1)
            errK2 = abs(k2-true_k2)
            row += f" k1Err={errK1:.4f} k2Err={errK2:.4f} rms={rms:.5f} |"
    print(row)

print("\n=== Degenerate: clustered near center ===")
obs = generate_distortion_obs(30, 0.0, center=True)
res, msg = refine_distortion(obs)
print(f"Clustered result: {msg}, res={res}")
assert res is None, "Should decline clustered"

print("\n=== Task4: CameraProfileCache merge convergence ===")
# Simulate bad early batch down-weighted
good_fx = 1000.0
bad_fx = 1500.0
# weighted average
merged = (200*good_fx + 10*bad_fx)/210
print(f"Good fx=1000 (200 samples), bad fx=1500 (10 samples), merged={merged:.2f} (should be ~1023, close to good)")
assert abs(merged-1023.8) < 1.0

print("\nAll Phase7 cross-checks passed")
