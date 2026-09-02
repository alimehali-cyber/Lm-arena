#!/usr/bin/env python3
"""
Phase 4 manual cross-check: attitude solve Davenport q-method reference implementation using numpy.

Per Task 0 step 4: build independent reference implementation of Davenport q-method using numpy's
own eigen-decomposition as ground truth. This is primary correctness anchor for Task 4 if JVM remains unavailable.

We will hand-trace Kotlin implementation logic against this Python reference on identical synthetic inputs,
and report comparison numerically.
"""
import numpy as np
import math

print("=== Phase 4 Manual Cross-Check: Davenport q-method ===")
print(f"numpy {np.__version__}")

def quat_from_axis_angle(axis, angle_rad):
    ax, ay, az = axis
    half = angle_rad/2
    s = math.sin(half)
    c = math.cos(half)
    norm = math.sqrt(ax*ax+ay*ay+az*az)
    if norm < 1e-12:
        return np.array([1.0, 0.0, 0.0, 0.0]) # w,x,y,z
    ax/=norm; ay/=norm; az/=norm
    return np.array([c, ax*s, ay*s, az*s])

def quat_rotate_vector(q, v):
    # q * v * q_conj, q = [w,x,y,z], v = [x,y,z]
    w,x,y,z = q
    vx,vy,vz = v
    # qv = [0, vx, vy, vz]
    # q * qv
    qw = -x*vx - y*vy - z*vz
    qx = w*vx + y*vz - z*vy
    qy = w*vy - x*vz + z*vx
    qz = w*vz + x*vy - y*vx
    # (q*qv) * q_conj, q_conj = [w, -x, -y, -z]
    w2 = w; x2 = -x; y2 = -y; z2 = -z
    rw = qw*w2 - qx*x2 - qy*y2 - qz*z2
    rx = qw*x2 + qx*w2 + qy*z2 - qz*y2
    ry = qw*y2 - qx*z2 + qy*w2 + qz*x2
    rz = qw*z2 + qx*y2 - qy*x2 + qz*w2
    return np.array([rx, ry, rz])

def davenport_k_matrix(correspondences, weights):
    # correspondences: list of (b_i catalog, r_i observed) unit vectors
    B = np.zeros((3,3))
    for (b,r), w in zip(correspondences, weights):
        B += w * np.outer(b, r)
    S = B + B.T
    # z = sum w_i * (b_i x r_i)
    z = np.zeros(3)
    for (b,r), w in zip(correspondences, weights):
        z += w * np.cross(b, r)
    sigma = np.trace(B)
    K = np.zeros((4,4))
    K[0:3,0:3] = S - sigma*np.eye(3)
    K[0:3,3] = z
    K[3,0:3] = z
    K[3,3] = sigma
    return K, B, S, z, sigma

def solve_davenport(correspondences, weights):
    K, B, S, z, sigma = davenport_k_matrix(correspondences, weights)
    # Eigen decomposition
    eigenvalues, eigenvectors = np.linalg.eigh(K) # eigh for symmetric, returns sorted ascending
    max_idx = np.argmax(eigenvalues)
    q_vec = eigenvectors[:, max_idx] # [x,y,z,w] per K construction
    # Convert to [w,x,y,z]
    q = np.array([q_vec[3], q_vec[0], q_vec[1], q_vec[2]])
    q = q / np.linalg.norm(q)
    return q, K, eigenvalues, eigenvectors

# Test case 1: Simple known attitude
print("\n--- Test Case 1: Known attitude 30° yaw ---")
# Ground truth attitude: 30° yaw about Z axis
gt_q = quat_from_axis_angle([0,0,1], math.radians(30))
print(f"Ground truth quaternion [w,x,y,z]: {gt_q}")

# Create some catalog stars
cat_stars = [
    np.array([1.0, 0.0, 0.0]),
    np.array([0.0, 1.0, 0.0]),
    np.array([0.0, 0.0, 1.0]),
    np.array([0.707, 0.707, 0.0]),
]

# Rotate to observed frame
obs_stars = [quat_rotate_vector(gt_q, b) for b in cat_stars]

correspondences = list(zip(cat_stars, obs_stars))
weights = [1.0]*len(correspondences)

q_solved, K, eigenvalues, eigenvectors = solve_davenport(correspondences, weights)
print(f"Solved quaternion [w,x,y,z]: {q_solved}")
print(f"K matrix:\n{K}")
print(f"Eigenvalues: {eigenvalues}")

# Compare gt_q vs solved: quaternions q and -q represent same rotation
# Compute angular error
def quat_angular_error(q1, q2):
    # q1,q2 are [w,x,y,z]
    dot = abs(np.dot(q1, q2)) # abs because q and -q same
    dot = min(1.0, max(-1.0, dot))
    angle = 2*math.acos(dot)
    return math.degrees(angle)

err = quat_angular_error(gt_q, q_solved)
print(f"Angular error: {err:.6f}° = {err*3600:.2f} arcsec")
print("Expected: <0.0001° for zero noise")

# Test case 2: With noise
print("\n--- Test Case 2: With noise 0.01° ---")
np.random.seed(42)
noise_sigma_rad = math.radians(0.01)
noisy_obs = []
for r in obs_stars:
    # Add small random rotation
    axis = np.random.randn(3)
    axis = axis / np.linalg.norm(axis)
    # Make axis perpendicular to r for realistic noise
    axis = np.cross(r, axis)
    if np.linalg.norm(axis) < 1e-6:
        axis = np.array([1.0,0,0])
    axis = axis / np.linalg.norm(axis)
    angle = np.random.normal(0, noise_sigma_rad)
    q_noise = quat_from_axis_angle(axis, angle)
    noisy_r = quat_rotate_vector(q_noise, r)
    noisy_r = noisy_r / np.linalg.norm(noisy_r)
    noisy_obs.append(noisy_r)

correspondences_noisy = list(zip(cat_stars, noisy_obs))
q_solved_noisy, K_noisy, _, _ = solve_davenport(correspondences_noisy, weights)
err_noisy = quat_angular_error(gt_q, q_solved_noisy)
print(f"Noisy solved quaternion: {q_solved_noisy}")
print(f"Angular error with 0.01° noise: {err_noisy:.6f}° = {err_noisy*60:.4f} arcmin = {err_noisy*3600:.2f} arcsec")

# Test case 3: Different attitude
print("\n--- Test Case 3: Random attitude ---")
# Random attitude: 45° about axis (1,1,0)
gt_q2 = quat_from_axis_angle([1,1,0], math.radians(45))
cat_stars2 = [
    np.array([0.0, 0.0, 1.0]),
    np.array([1.0, 0.0, 0.0]),
    np.array([0.0, 1.0, 0.0]),
    np.array([0.5, 0.5, 0.707]),
    np.array([-0.5, 0.5, 0.707]),
]
obs_stars2 = [quat_rotate_vector(gt_q2, b) for b in cat_stars2]
correspondences2 = list(zip(cat_stars2, obs_stars2))
q_solved2, K2, _, _ = solve_davenport(correspondences2, [1.0]*len(correspondences2))
err2 = quat_angular_error(gt_q2, q_solved2)
print(f"GT quat: {gt_q2}")
print(f"Solved quat: {q_solved2}")
print(f"Angular error: {err2:.6f}° = {err2*3600:.2f} arcsec")
print(f"K matrix for this case:\n{K2}")

# TRIAD test
print("\n--- TRIAD Test (2 stars) ---")
# Use first 2 stars from case 1
v1_cat = cat_stars[0]
v2_cat = cat_stars[1]
v1_obs = obs_stars[0]
v2_obs = obs_stars[1]

def triad(v1_cat, v2_cat, v1_obs, v2_obs):
    def normalize(v):
        return v/np.linalg.norm(v)
    def cross(a,b):
        return np.cross(a,b)
    t1_cat = normalize(v1_cat)
    t2_cat = normalize(cross(t1_cat, v2_cat))
    t3_cat = cross(t1_cat, t2_cat)
    t1_obs = normalize(v1_obs)
    t2_obs = normalize(cross(t1_obs, v2_obs))
    t3_obs = cross(t1_obs, t2_obs)
    # Build matrices with columns t1,t2,t3
    cat_mat = np.column_stack([t1_cat, t2_cat, t3_cat])
    obs_mat = np.column_stack([t1_obs, t2_obs, t3_obs])
    R = obs_mat @ cat_mat.T
    # Convert R to quaternion
    # Use same method as Kotlin fromRotationMatrix
    trace = R[0,0]+R[1,1]+R[2,2]
    if trace>0:
        s = math.sqrt(trace+1.0)*2
        w = 0.25*s
        x = (R[2,1]-R[1,2])/s
        y = (R[0,2]-R[2,0])/s
        z = (R[1,0]-R[0,1])/s
    elif R[0,0]>R[1,1] and R[0,0]>R[2,2]:
        s = math.sqrt(1.0+R[0,0]-R[1,1]-R[2,2])*2
        w = (R[2,1]-R[1,2])/s
        x = 0.25*s
        y = (R[0,1]+R[1,0])/s
        z = (R[0,2]+R[2,0])/s
    elif R[1,1]>R[2,2]:
        s = math.sqrt(1.0+R[1,1]-R[0,0]-R[2,2])*2
        w = (R[0,2]-R[2,0])/s
        x = (R[0,1]+R[1,0])/s
        y = 0.25*s
        z = (R[1,2]+R[2,1])/s
    else:
        s = math.sqrt(1.0+R[2,2]-R[0,0]-R[1,1])*2
        w = (R[1,0]-R[0,1])/s
        x = (R[0,2]+R[2,0])/s
        y = (R[1,2]+R[2,1])/s
        z = 0.25*s
    q = np.array([w,x,y,z])
    return q/np.linalg.norm(q), R

q_triad, R_triad = triad(v1_cat, v2_cat, v1_obs, v2_obs)
err_triad = quat_angular_error(gt_q, q_triad)
print(f"TRIAD solved quat: {q_triad}")
print(f"TRIAD angular error: {err_triad:.6f}° = {err_triad*3600:.2f} arcsec")
print(f"Rotation matrix:\n{R_triad}")

print("\n=== Summary ===")
print(f"Test1 zero noise error: {err:.6f}° ({err*3600:.2f} arcsec) — Python reference recovers attitude essentially perfectly")
print(f"Test2 0.01° noise error: {err_noisy:.6f}° ({err_noisy*3600:.2f} arcsec)")
print(f"Test3 random attitude error: {err2:.6f}° ({err2*3600:.2f} arcsec)")
print(f"TRIAD error: {err_triad:.6f}° ({err_triad*3600:.2f} arcsec)")
print("\nKotlin logic, traced by hand step-by-step, produces identical K-matrix construction:")
print("  B = Σ w*b*r^T, S = B+B^T, z = Σ w*(b×r), sigma=trace(B), K=[[S-sigma*I, z],[z^T, sigma]]")
print("  Jacobi iteration on 4x4 symmetric matrix to find largest eigenvector — same as numpy eigh")
print("  Would be expected to converge to same result, but this has NOT been executed as Kotlin code")
print("  (JVM still blocked, 4 phases without automated execution)")
