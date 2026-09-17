#version 300 es
precision highp float;

in vec2 v_TexCoord;
out vec4 fragColor;

// Uniforms
uniform vec2 u_Resolution;   // Screen resolution (width, height)
uniform float u_Time;        // Elapsed time (seconds)
uniform float u_Mass;        // Black hole mass M (geometrized, G=c=1)
uniform float u_Spin;        // Kerr spin parameter a (|a| <= M)
uniform vec3 u_CamPos;       // Camera position in Kerr-Schild Cartesian coordinates
uniform vec3 u_CamForward;   // Camera forward vector
uniform vec3 u_CamRight;     // Camera right vector
uniform vec3 u_CamUp;        // Camera up vector
uniform float u_FovScale;    // tan(FOV/2)
uniform int u_MaxSteps;      // Maximum RK4 steps per pixel ray (e.g. 150)
uniform float u_DiskInnerRadius; // ISCO radius r_in
uniform float u_DiskOuterRadius; // Outer boundary r_out
uniform int u_EnableDisk;        // 1 to render relativistic accretion disk, 0 otherwise

// Maximum fixed compile-time loop bound for mobile GLSL ES 3.0 compliance
const int MAX_INTEGRATION_STEPS = 180;

// Computes Kerr-Schild radial coordinate r >= 0 from Cartesian coordinates (X, Y, Z)
float compute_r_KS(float a, float X, float Y, float Z) {
    float a2 = a * a;
    float z2 = Z * Z;
    float R2 = X * X + Y * Y + z2;
    if (abs(a) < 1.0e-7) {
        return sqrt(max(0.0, R2));
    }
    float S = R2 - a2;
    float D = S * S + 4.0 * a2 * z2;
    float sqrtD = sqrt(max(0.0, D));
    float u;
    if (S >= 0.0) {
        u = 0.5 * (S + sqrtD);
    } else {
        float denom = sqrtD - S;
        u = (denom > 0.0) ? (2.0 * a2 * z2) / denom : 0.0;
    }
    return sqrt(max(0.0, u));
}

// Computes Kerr-Schild contravariant metric g^μν
// Index ordering: 0=T, 1=X, 2=Y, 3=Z
mat4 compute_g_inv(float M, float a, float X, float Y, float Z, float r) {
    float a2 = a * a;
    float z2 = Z * Z;
    float r2 = r * r;
    float denom_sigma = r2 * r2 + a2 * z2;
    float H = (denom_sigma > 1.0e-20) ? (M * r2 * r) / denom_sigma : 0.0;
    float twoH = 2.0 * H;

    float denom_v = r2 + a2;
    float lx = (denom_v > 1.0e-12) ? (r * X + a * Y) / denom_v : 0.0;
    float ly = (denom_v > 1.0e-12) ? (r * Y - a * X) / denom_v : 0.0;
    float lz = (r > 1.0e-7) ? (Z / r) : 0.0;

    vec4 l = vec4(-1.0, lx, ly, lz);

    // eta^μν = diag(-1, 1, 1, 1)
    // g^μν = eta^μν - 2H l^μ l^ν
    mat4 gInv;
    gInv[0] = vec4(-1.0 - twoH * l[0] * l[0], -twoH * l[1] * l[0], -twoH * l[2] * l[0], -twoH * l[3] * l[0]);
    gInv[1] = vec4(-twoH * l[0] * l[1], 1.0 - twoH * l[1] * l[1], -twoH * l[2] * l[1], -twoH * l[3] * l[1]);
    gInv[2] = vec4(-twoH * l[0] * l[2], -twoH * l[1] * l[2], 1.0 - twoH * l[2] * l[2], -twoH * l[3] * l[2]);
    gInv[3] = vec4(-twoH * l[0] * l[3], -twoH * l[1] * l[3], -twoH * l[2] * l[3], 1.0 - twoH * l[3] * l[3]);
    return gInv;
}

// Computes covariant metric g_μν
mat4 compute_g_lower(float M, float a, float X, float Y, float Z, float r) {
    float a2 = a * a;
    float z2 = Z * Z;
    float r2 = r * r;
    float denom_sigma = r2 * r2 + a2 * z2;
    float H = (denom_sigma > 1.0e-20) ? (M * r2 * r) / denom_sigma : 0.0;
    float twoH = 2.0 * H;

    float denom_v = r2 + a2;
    float lx = (denom_v > 1.0e-12) ? (r * X + a * Y) / denom_v : 0.0;
    float ly = (denom_v > 1.0e-12) ? (r * Y - a * X) / denom_v : 0.0;
    float lz = (r > 1.0e-7) ? (Z / r) : 0.0;

    vec4 l = vec4(1.0, lx, ly, lz);

    mat4 g;
    g[0] = vec4(-1.0 + twoH * l[0] * l[0], twoH * l[1] * l[0], twoH * l[2] * l[0], twoH * l[3] * l[0]);
    g[1] = vec4(twoH * l[0] * l[1], 1.0 + twoH * l[1] * l[1], twoH * l[2] * l[1], twoH * l[3] * l[1]);
    g[2] = vec4(twoH * l[0] * l[2], twoH * l[1] * l[2], 1.0 + twoH * l[2] * l[2], twoH * l[3] * l[2]);
    g[3] = vec4(twoH * l[0] * l[3], twoH * l[1] * l[3], twoH * l[2] * l[3], 1.0 + twoH * l[3] * l[3]);
    return g;
}

// Computes exact analytical spatial gradients of g^μν
// Returns d_dX, d_dY, d_dZ
void compute_spatial_derivatives(
    float M, float a, float X, float Y, float Z, float r,
    out mat4 dg_dX, out mat4 dg_dY, out mat4 dg_dZ
) {
    float a2 = a * a;
    float z2 = Z * Z;
    float r2 = r * r;
    float r3 = r2 * r;
    float r4 = r2 * r2;
    float denom_sigma = r4 + a2 * z2;
    if (denom_sigma < 1.0e-20) denom_sigma = 1.0e-20;

    // 1. Exact radial derivatives ∂_i r
    float dr_dX = (r3 * X) / denom_sigma;
    float dr_dY = (r3 * Y) / denom_sigma;
    float dr_dZ = (Z * r * (r2 + a2)) / denom_sigma;

    // 2. Exact scalar H derivatives ∂_i H
    float H = (M * r3) / denom_sigma;
    float denom_sigma2 = denom_sigma * denom_sigma;
    float dH_dr = M * r2 * (3.0 * a2 * z2 - r4) / denom_sigma2;

    float dH_dX = dH_dr * dr_dX;
    float dH_dY = dH_dr * dr_dY;
    float dH_dZ = dH_dr * dr_dZ - (2.0 * M * a2 * r3 * Z) / denom_sigma2;

    // 3. Exact null vector derivatives ∂_i l^μ
    float denom_v = r2 + a2;
    float denom_v2 = denom_v * denom_v;
    float lx = (r * X + a * Y) / denom_v;
    float ly = (r * Y - a * X) / denom_v;
    float lz = (r > 1.0e-7) ? Z / r : 0.0;
    vec4 l = vec4(-1.0, lx, ly, lz);

    // Differentiation of l^μ
    float dv_X = 2.0 * r * dr_dX;
    float duX_X = dr_dX * X + r;
    float dlX_X = (duX_X * denom_v - (r * X + a * Y) * dv_X) / denom_v2;
    float duY_X = dr_dX * Y - a;
    float dlY_X = (duY_X * denom_v - (r * Y - a * X) * dv_X) / denom_v2;
    float dlZ_X = (r > 1.0e-7) ? (-Z * dr_dX) / r2 : 0.0;
    vec4 dl_dX = vec4(0.0, dlX_X, dlY_X, dlZ_X);

    float dv_Y = 2.0 * r * dr_dY;
    float duX_Y = dr_dY * X + a;
    float dlX_Y = (duX_Y * denom_v - (r * X + a * Y) * dv_Y) / denom_v2;
    float duY_Y = dr_dY * Y + r;
    float dlY_Y = (duY_Y * denom_v - (r * Y - a * X) * dv_Y) / denom_v2;
    float dlZ_Y = (r > 1.0e-7) ? (-Z * dr_dY) / r2 : 0.0;
    vec4 dl_dY = vec4(0.0, dlX_Y, dlY_Y, dlZ_Y);

    float dv_Z = 2.0 * r * dr_dZ;
    float duX_Z = (dr_dZ * X);
    float dlX_Z = (duX_Z * denom_v - (r * X + a * Y) * dv_Z) / denom_v2;
    float duY_Z = (dr_dZ * Y);
    float dlY_Z = (duY_Z * denom_v - (r * Y - a * X) * dv_Z) / denom_v2;
    float dlZ_Z = (r > 1.0e-7) ? (r - Z * dr_dZ) / r2 : 0.0;
    vec4 dl_dZ = vec4(0.0, dlX_Z, dlY_Z, dlZ_Z);

    // 4. Assemble ∂_i g^μν = -2 (∂_i H) l^μ l^ν - 2 H (∂_i l^μ) l^ν - 2 H l^μ (∂_i l^ν)
    #define ASSEMBLE_DG(dg, dH_di, dl_di) \
        for (int mu = 0; mu < 4; mu++) { \
            for (int nu = 0; nu < 4; nu++) { \
                dg[mu][nu] = -2.0 * dH_di * l[mu] * l[nu] - 2.0 * H * (dl_di[mu] * l[nu] + l[mu] * dl_di[nu]); \
            } \
        }

    ASSEMBLE_DG(dg_dX, dH_dX, dl_dX)
    ASSEMBLE_DG(dg_dY, dH_dY, dl_dY)
    ASSEMBLE_DG(dg_dZ, dH_dZ, dl_dZ)
}

// Evaluates the 6D phase space RHS
void evaluate_rhs(
    float M, float a,
    vec3 pos, vec3 p_spatial,
    out vec3 dPos, out vec3 dP
) {
    float r = compute_r_KS(a, pos.x, pos.y, pos.z);
    mat4 gInv = compute_g_inv(M, a, pos.x, pos.y, pos.z, r);

    mat4 dg_dX, dg_dY, dg_dZ;
    compute_spatial_derivatives(M, a, pos.x, pos.y, pos.z, r, dg_dX, dg_dY, dg_dZ);

    vec4 p = vec4(-1.0, p_spatial.x, p_spatial.y, p_spatial.z);

    // dx^i / dλ = g^{iν} p_ν
    // Matrix columns are indexed as gInv[col][row], so gInv[nu][i]
    dPos.x = gInv[0][1] * p.x + gInv[1][1] * p.y + gInv[2][1] * p.z + gInv[3][1] * p.w;
    dPos.y = gInv[0][2] * p.x + gInv[1][2] * p.y + gInv[2][2] * p.z + gInv[3][2] * p.w;
    dPos.z = gInv[0][3] * p.x + gInv[1][3] * p.y + gInv[2][3] * p.z + gInv[3][3] * p.w;

    // dp_i / dλ = -1/2 (∂_i g^αβ) p_α p_β
    #define CONTRA_DP(dg) (-0.5 * dot(p, dg * p))
    dP.x = CONTRA_DP(dg_dX);
    dP.y = CONTRA_DP(dg_dY);
    dP.z = CONTRA_DP(dg_dZ);
}

// Single RK4 step
void rk4_step(
    float M, float a,
    inout vec3 pos, inout vec3 p_spatial,
    float dlambda
) {
    vec3 k1_pos, k1_p;
    evaluate_rhs(M, a, pos, p_spatial, k1_pos, k1_p);

    vec3 s2_pos = pos + 0.5 * dlambda * k1_pos;
    vec3 s2_p = p_spatial + 0.5 * dlambda * k1_p;
    vec3 k2_pos, k2_p;
    evaluate_rhs(M, a, s2_pos, s2_p, k2_pos, k2_p);

    vec3 s3_pos = pos + 0.5 * dlambda * k2_pos;
    vec3 s3_p = p_spatial + 0.5 * dlambda * k2_p;
    vec3 k3_pos, k3_p;
    evaluate_rhs(M, a, s3_pos, s3_p, k3_pos, k3_p);

    vec3 s4_pos = pos + dlambda * k3_pos;
    vec3 s4_p = p_spatial + dlambda * k3_p;
    vec3 k4_pos, k4_p;
    evaluate_rhs(M, a, s4_pos, s4_p, k4_pos, k4_p);

    pos += (dlambda / 6.0) * (k1_pos + 2.0 * k2_pos + 2.0 * k3_pos + k4_pos);
    p_spatial += (dlambda / 6.0) * (k1_p + 2.0 * k2_p + 2.0 * k3_p + k4_p);
}

// Procedural starfield on celestial sphere
float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 sample_procedural_sky(vec3 dir) {
    vec3 d = normalize(dir);
    float theta = acos(clamp(d.z, -1.0, 1.0));
    float phi = atan(d.y, d.x);

    // Deep space background gradient
    vec3 bgSlate = vec3(0.02, 0.03, 0.06);
    vec3 bgDeepBlue = vec3(0.04, 0.06, 0.12);
    float grad = 0.5 + 0.5 * d.z;
    vec3 col = mix(bgSlate, bgDeepBlue, grad);

    // Galactic equator plane
    float galacticDist = abs(d.z * 0.8 + d.y * 0.6);
    float milkyWay = exp(-galacticDist * galacticDist * 16.0) * 0.25;
    col += vec3(0.25, 0.22, 0.35) * milkyWay;

    // Structured celestial coordinate grid lines (every 30 degrees = π/6 rad)
    float gridSpacing = 3.14159265 / 6.0;
    float latRem = abs(mod(theta, gridSpacing) - gridSpacing * 0.5);
    float lonRem = abs(mod(phi + 3.14159265, gridSpacing) - gridSpacing * 0.5);
    float gridLineWidth = 0.015;
    float latLine = 1.0 - smoothstep(0.0, gridLineWidth, abs(latRem - gridSpacing * 0.5));
    float lonLine = 1.0 - smoothstep(0.0, gridLineWidth, abs(lonRem - gridSpacing * 0.5));
    float grid = max(latLine, lonLine) * 0.20;
    col += vec3(0.18, 0.35, 0.55) * grid;

    // Procedural point stars using angular grid cells
    float starScale = 40.0;
    vec2 cell = floor(vec2(phi, theta) * starScale);
    float starRand = hash21(cell);
    if (starRand > 0.75) {
        vec2 starCenter = (cell + vec2(hash21(cell + vec2(0.0, 1.0)), hash21(cell + vec2(1.0, 0.0)))) / starScale;
        float dist = length(vec2(phi, theta) - starCenter) * starScale;
        float starBrightness = max(0.0, 1.0 - dist * 3.5);
        float starMag = (starRand - 0.75) * 4.0 * starBrightness;
        vec3 starCol = (hash21(cell * 2.0) > 0.5) ? vec3(0.9, 0.95, 1.0) : vec3(1.0, 0.85, 0.65);
        col += starCol * starMag;
    }

    return clamp(col, 0.0, 1.0);
}

void main() {
    // Aspect-ratio-corrected normalized device coordinates in [-1, 1]
    vec2 st = (gl_FragCoord.xy * 2.0 - u_Resolution.xy) / min(u_Resolution.x, u_Resolution.y);

    // Initial ray direction in camera frame
    vec3 rayDir = normalize(u_CamForward + u_CamRight * (st.x * u_FovScale) + u_CamUp * (st.y * u_FovScale));

    // Construct exact null momentum p_μ at camera position
    vec3 pos = u_CamPos;
    float rInit = compute_r_KS(u_Spin, pos.x, pos.y, pos.z);
    mat4 g = compute_g_lower(u_Mass, u_Spin, pos.x, pos.y, pos.z, rInit);

    float Aw = g[0][0];
    float Bw = g[1][0] * rayDir.x + g[2][0] * rayDir.y + g[3][0] * rayDir.z;
    float Cw = 0.0;
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            Cw += g[i + 1][j + 1] * rayDir[i] * rayDir[j];
        }
    }

    float Dw = max(0.0, Bw * Bw - Aw * Cw);
    float w = (-Bw - sqrt(Dw)) / Aw;
    vec4 v = vec4(w, rayDir);

    vec4 vLower = g * v;
    float scale = -vLower[0];
    vec3 p_spatial = vec3(vLower.y, vLower.z, vLower.w) / scale;

    // Numerical integration constants
    float rPlus = u_Mass + sqrt(max(0.0, u_Mass * u_Mass - u_Spin * u_Spin));
    float rCapture = rPlus + 0.05;
    float rEscape = 50.0;

    int maxSteps = clamp(u_MaxSteps, 40, MAX_INTEGRATION_STEPS);
    bool isCaptured = false;
    bool hitDisk = false;
    vec3 diskColor = vec3(0.0);
    vec3 finalDir = rayDir;

    float prevR = rInit;
    bool movingOutward = false;

    // Primary null Hamiltonian geodesic integration loop
    for (int step = 0; step < MAX_INTEGRATION_STEPS; step++) {
        if (step >= maxSteps) {
            isCaptured = true;
            break;
        }

        float r = compute_r_KS(u_Spin, pos.x, pos.y, pos.z);
        if (r > prevR) {
            movingOutward = true;
        }
        prevR = r;

        // 1. Capture detection (inside black hole shadow)
        if (r <= rCapture) {
            isCaptured = true;
            break;
        }

        // 2. Escape detection (asymptotic background reached)
        if (r >= rEscape && (movingOutward || step > 20)) {
            // Compute spatial 3-velocity direction at escape
            mat4 gInv = compute_g_inv(u_Mass, u_Spin, pos.x, pos.y, pos.z, r);
            vec4 pFinal = vec4(-1.0, p_spatial);
            vec3 vSpatial = vec3(
                gInv[0][1] * pFinal.x + gInv[1][1] * pFinal.y + gInv[2][1] * pFinal.z + gInv[3][1] * pFinal.w,
                gInv[0][2] * pFinal.x + gInv[1][2] * pFinal.y + gInv[2][2] * pFinal.z + gInv[3][2] * pFinal.w,
                gInv[0][3] * pFinal.x + gInv[1][3] * pFinal.y + gInv[2][3] * pFinal.z + gInv[3][3] * pFinal.w
            );
            finalDir = normalize(vSpatial);
            break;
        }

        vec3 prevPos = pos;
        vec3 prevP = p_spatial;

        // Adaptive step size: smaller near photon sphere and horizon
        float dlambda = clamp(0.08 * r, 0.015, 0.45);
        rk4_step(u_Mass, u_Spin, pos, p_spatial, dlambda);

        // Check for intersection with thin equatorial accretion disk at Z = 0
        if (u_EnableDisk == 1 && prevPos.z * pos.z <= 0.0 && prevPos.z != pos.z) {
            float tau = -prevPos.z / (pos.z - prevPos.z);
            if (tau >= 0.0 && tau <= 1.0) {
                vec3 hitPos = prevPos + tau * (pos - prevPos);
                float rHit = length(hitPos.xy);
                if (rHit >= u_DiskInnerRadius && rHit <= u_DiskOuterRadius) {
                    vec3 hitP = prevP + tau * (p_spatial - prevP);

                    // Relativistic Keplerian angular velocity Omega = sqrt(M) / (r^(3/2) + a * sqrt(M))
                    float omega = sqrt(u_Mass) / (pow(rHit, 1.5) + u_Spin * sqrt(u_Mass));

                    // Disk 4-velocity u^mu = u0 * (1, -Omega * Y, Omega * X, 0)
                    mat4 gHit = compute_g_lower(u_Mass, u_Spin, hitPos.x, hitPos.y, 0.0, rHit);
                    vec4 vEmit = vec4(1.0, -omega * hitPos.y, omega * hitPos.x, 0.0);

                    float denomContract = 0.0;
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 4; j++) {
                            denomContract += gHit[i][j] * vEmit[i] * vEmit[j];
                        }
                    }
                    float u0 = (denomContract < 0.0) ? 1.0 / sqrt(-denomContract) : 1.0;

                    // Invariant frequency shift g = (-p_mu u_obs^mu) / (-p_mu u_emit^mu)
                    float lz = hitPos.x * hitP.y - hitPos.y * hitP.x;
                    float denomG = u0 * (1.0 + omega * lz);

                    // Static observer at camera position
                    float uObs0 = 1.0 / sqrt(max(1.0e-6, -g[0][0]));
                    float gShift = (abs(denomG) > 1.0e-6) ? clamp(uObs0 / denomG, 0.05, 5.0) : 1.0;

                    // Novikov-Thorne-inspired thin-disk flux profile F(r) = (M/r^3) * [1 - sqrt(r_in/r)]
                    float rRatio = u_DiskInnerRadius / rHit;
                    float F = (u_Mass / (rHit * rHit * rHit)) * max(0.0, 1.0 - sqrt(rRatio));
                    float tEmit = pow(max(0.0, F), 0.25);
                    float tObs = gShift * tEmit;

                    // Radiance with relativistic beaming g^4
                    float g2 = gShift * gShift;
                    float g4 = g2 * g2;
                    float radiance = clamp(g4 * F * 60.0, 0.0, 2.5);

                    // Thermal blackbody spectral color approximation
                    float tNorm = clamp(tObs * 4.0, 0.0, 2.5);
                    vec3 thermalRamp = vec3(
                        clamp(1.0 + 0.3 * tNorm, 0.0, 1.5),
                        clamp(tNorm * tNorm * 0.45 + tNorm * 0.25, 0.0, 1.2),
                        clamp(tNorm * tNorm * tNorm * 0.35, 0.0, 1.2)
                    );

                    diskColor = clamp(radiance * thermalRamp, 0.0, 1.0);
                    hitDisk = true;
                    break;
                }
            }
        }
    }

    if (hitDisk) {
        // Relativistic equatorial accretion disk
        fragColor = vec4(diskColor, 1.0);
    } else if (isCaptured) {
        // True black hole shadow
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        // Gravitationally lensed procedural celestial background
        vec3 color = sample_procedural_sky(finalDir);
        fragColor = vec4(color, 1.0);
    }
}
