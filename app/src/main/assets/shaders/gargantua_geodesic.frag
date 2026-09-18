#version 300 es
precision highp float;

in vec2 v_TexCoord;
out vec4 fragColor;

// Uniforms
uniform vec2 u_Resolution;   // Screen or scaled FBO resolution (width, height)
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

// Relativistic synthetic test object uniforms
uniform int u_EnableObject;         // 1 to render relativistic test object, 0 otherwise
uniform float u_ObjectRadius;       // Physical radius of test object
uniform float u_ObjectOrbitRadius;  // Orbital coordinate radius in Kerr-Schild coordinates
uniform float u_ObjectOmega;        // Keplerian angular velocity Ω
uniform float u_ObjectPhi0;         // Initial phase angle φ_0
uniform float u_ObjectZ;            // Z coordinate of orbit (0 for equatorial)
uniform vec3 u_ObjectBaseColor;     // Procedural base color (e.g. cyan/electric azure)
uniform float u_ObjectRadiance;     // Base surface radiance

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
    float u = 0.5 * (S + sqrt(max(0.0, D)));
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

// Evaluates the 6D phase space RHS
// Uses factored exact analytical Kerr-Schild derivatives to eliminate matrix assembly and register spilling
void evaluate_rhs(
    float M, float a,
    vec3 pos, vec3 p_spatial,
    out vec3 dPos, out vec3 dP
) {
    float r = compute_r_KS(a, pos.x, pos.y, pos.z);
    float a2 = a * a;
    float z2 = pos.z * pos.z;
    float r2 = r * r;
    float r3 = r2 * r;
    float r4 = r2 * r2;
    float denom_sigma = r4 + a2 * z2;
    if (denom_sigma < 1.0e-20) denom_sigma = 1.0e-20;

    // 1. Exact radial derivatives ∂_i r
    float dr_dX = (r3 * pos.x) / denom_sigma;
    float dr_dY = (r3 * pos.y) / denom_sigma;
    float dr_dZ = (pos.z * r * (r2 + a2)) / denom_sigma;

    // 2. Exact scalar H and radial derivative
    float H = (M * r3) / denom_sigma;
    float denom_sigma2 = denom_sigma * denom_sigma;
    float dH_dr = M * r2 * (3.0 * a2 * z2 - r4) / denom_sigma2;

    float dH_dX = dH_dr * dr_dX;
    float dH_dY = dH_dr * dr_dY;
    float dH_dZ = dH_dr * dr_dZ - (2.0 * M * a2 * r3 * pos.z) / denom_sigma2;

    // 3. Exact null vector l^μ = (-1, lx, ly, lz)
    float denom_v = r2 + a2;
    float denom_v2 = denom_v * denom_v;
    float lx = (denom_v > 1.0e-12) ? (r * pos.x + a * pos.y) / denom_v : 0.0;
    float ly = (denom_v > 1.0e-12) ? (r * pos.y - a * pos.x) / denom_v : 0.0;
    float lz = (r > 1.0e-7) ? pos.z / r : 0.0;
    vec3 l_spatial = vec3(lx, ly, lz);

    // Contraction Lp = l^μ p_μ with l^0 = -1, p_0 = -1 (so l^0 * p_0 = 1.0)
    float Lp = 1.0 + dot(l_spatial, p_spatial);
    float twoH = 2.0 * H;

    // 4. dx^i / dλ = g^{iν} p_ν = p_i - 2.0 * H * Lp * l^i
    dPos = p_spatial - (twoH * Lp) * l_spatial;

    // 5. dp_i / dλ = -0.5 (∂_i g^αβ) p_α p_β = Lp * [ (∂_i H) Lp + 2 H (∂_i l^μ p_μ) ]
    // Spatial differentiation of l^μ
    float dv_X = 2.0 * r * dr_dX;
    float duX_X = dr_dX * pos.x + r;
    float dlX_X = (duX_X * denom_v - (r * pos.x + a * pos.y) * dv_X) / denom_v2;
    float duY_X = dr_dX * pos.y - a;
    float dlY_X = (duY_X * denom_v - (r * pos.y - a * pos.x) * dv_X) / denom_v2;
    float dlZ_X = (r > 1.0e-7) ? (-pos.z * dr_dX) / r2 : 0.0;
    float dl_p_X = dlX_X * p_spatial.x + dlY_X * p_spatial.y + dlZ_X * p_spatial.z;

    float dv_Y = 2.0 * r * dr_dY;
    float duX_Y = dr_dY * pos.x + a;
    float dlX_Y = (duX_Y * denom_v - (r * pos.x + a * pos.y) * dv_Y) / denom_v2;
    float duY_Y = dr_dY * pos.y + r;
    float dlY_Y = (duY_Y * denom_v - (r * pos.y - a * pos.x) * dv_Y) / denom_v2;
    float dlZ_Y = (r > 1.0e-7) ? (-pos.z * dr_dY) / r2 : 0.0;
    float dl_p_Y = dlX_Y * p_spatial.x + dlY_Y * p_spatial.y + dlZ_Y * p_spatial.z;

    float dv_Z = 2.0 * r * dr_dZ;
    float duX_Z = (dr_dZ * pos.x);
    float dlX_Z = (duX_Z * denom_v - (r * pos.x + a * pos.y) * dv_Z) / denom_v2;
    float duY_Z = (dr_dZ * pos.y);
    float dlY_Z = (duY_Z * denom_v - (r * pos.y - a * pos.x) * dv_Z) / denom_v2;
    float dlZ_Z = (r > 1.0e-7) ? (r - pos.z * dr_dZ) / r2 : 0.0;
    float dl_p_Z = dlX_Z * p_spatial.x + dlY_Z * p_spatial.y + dlZ_Z * p_spatial.z;

    dP.x = Lp * (dH_dX * Lp + twoH * dl_p_X);
    dP.y = Lp * (dH_dY * Lp + twoH * dl_p_Y);
    dP.z = Lp * (dH_dZ * Lp + twoH * dl_p_Z);
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

    // Deep cosmic space void (pitch black baseline)
    vec3 col = vec3(0.0004, 0.0006, 0.0010);

    // Subtle galactic plane band (very faint deep space nebula)
    float galacticDist = abs(d.z * 0.8 + d.y * 0.6);
    float milkyWay = exp(-galacticDist * galacticDist * 16.0) * 0.005;
    col += vec3(0.005, 0.006, 0.010) * milkyWay;

    // Procedural point stars using angular grid cells
    float starScale = 45.0;
    vec2 cell = floor(vec2(phi, theta) * starScale);
    float starRand = hash21(cell);
    if (starRand > 0.75) {
        vec2 starCenter = (cell + vec2(hash21(cell + vec2(0.0, 1.0)), hash21(cell + vec2(1.0, 0.0)))) / starScale;
        float dist = length(vec2(phi, theta) - starCenter) * starScale;
        float starBrightness = max(0.0, 1.0 - dist * 3.5);
        float starMag = (starRand - 0.75) * 4.0 * starBrightness;
        vec3 starCol = (hash21(cell * 2.0) > 0.5) ? vec3(0.85, 0.92, 1.0) : vec3(1.0, 0.85, 0.65);
        col += starCol * (starMag * 0.5);
    }

    return col;
}

vec4 traceRaySample(vec2 stCoord, out int outState, out float outMinR, out int outCrossings, out float outHitR) {
    // Initial ray direction in camera frame
    vec3 rayDir = normalize(u_CamForward + u_CamRight * (stCoord.x * u_FovScale) + u_CamUp * (stCoord.y * u_FovScale));

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
    float rEscape = max(50.0, rInit + 15.0);

    int maxSteps = clamp(u_MaxSteps, 40, MAX_INTEGRATION_STEPS);
    
    // Explicit Ray Termination Classification:
    // 0 = UNRESOLVED (budget exhausted without proving capture, escape, or disk intersection)
    // 1 = CAPTURED   (physically crossed event horizon capture threshold r <= rCapture)
    // 2 = ESCAPED    (physically reached asymptotic background r >= rEscape moving outward)
    // 3 = DISK       (physically intersected equatorial accretion disk)
    // 4 = OBJECT     (physically intersected relativistic synthetic test object)
    int rayState = 0;
    vec3 diskColor = vec3(0.0);
    vec3 objectColor = vec3(0.0);
    float rayT = u_Time;
    float minR = rInit;
    int crossings = 0;
    float hitRadius = 0.0;

    float prevR = rInit;
    bool movingOutward = false;

    // Primary null Hamiltonian geodesic integration loop
    for (int step = 0; step < MAX_INTEGRATION_STEPS; step++) {
        if (step >= maxSteps) {
            break;
        }

        float r = compute_r_KS(u_Spin, pos.x, pos.y, pos.z);
        if (r < minR) {
            minR = r;
        }
        if (r > prevR) {
            movingOutward = true;
        }
        prevR = r;

        // 1. Capture detection (physically inside black hole shadow)
        if (r <= rCapture) {
            rayState = 1;
            break;
        }

        // 2. Escape detection (physically reached asymptotic background or cleared outer disk boundary moving outward)
        if (movingOutward && (r >= rEscape || r >= u_DiskOuterRadius)) {
            rayState = 2;
            break;
        }

        vec3 prevPos = pos;
        vec3 prevP = p_spatial;
        float prevT = rayT;

        // Adaptive step size: robust bounded steps preventing ray-crawling near disk plane
        float baseStep = 0.08 * r;
        float dlambda = (r > 5.0 && (movingOutward || r > 20.0)) ? clamp(baseStep, 0.02, 0.50) : clamp(baseStep, 0.02, 0.35);
        if (u_EnableDisk == 1 && abs(pos.z) < 0.60 && r >= u_DiskInnerRadius - 0.5 && r <= u_DiskOuterRadius + 1.0) {
            float vz = abs(p_spatial.z);
            float stepToDisk = abs(pos.z) / max(0.15, vz);
            dlambda = min(dlambda, max(0.04, stepToDisk * 0.80 + 0.02));
        }
        rk4_step(u_Mass, u_Spin, pos, p_spatial, dlambda);

        // Coordinate time evolution along backward null ray:
        // dT = -(1.0 + 2.0 * H * Lp) * dlambda
        float rMid = compute_r_KS(u_Spin, pos.x, pos.y, pos.z);
        float a2_m = u_Spin * u_Spin;
        float denomSigma_m = rMid * rMid * rMid * rMid + a2_m * pos.z * pos.z;
        float H_m = (denomSigma_m > 1.0e-20) ? (u_Mass * rMid * rMid * rMid) / denomSigma_m : 0.0;
        float denomV_m = rMid * rMid + a2_m;
        float lx_m = (denomV_m > 1.0e-12) ? (rMid * pos.x + u_Spin * pos.y) / denomV_m : 0.0;
        float ly_m = (denomV_m > 1.0e-12) ? (rMid * pos.y - u_Spin * pos.x) / denomV_m : 0.0;
        float lz_m = (rMid > 1.0e-7) ? pos.z / rMid : 0.0;
        float Lp_m = 1.0 + (lx_m * p_spatial.x + ly_m * p_spatial.y + lz_m * p_spatial.z);
        float dt_dlambda = 1.0 + 2.0 * H_m * Lp_m;
        rayT -= dt_dlambda * dlambda;

        // Check for intersection with relativistic test object
        float minStepR = min(prevR, r);
        float maxStepR = max(prevR, r);
        float objMargin = u_ObjectRadius + 1.2;
        if (u_EnableObject == 1 && u_ObjectOrbitRadius >= minStepR - objMargin && u_ObjectOrbitRadius <= maxStepR + objMargin) {
            float phiPrev = u_ObjectOmega * prevT + u_ObjectPhi0;
            vec3 objPosPrev = vec3(u_ObjectOrbitRadius * cos(phiPrev), u_ObjectOrbitRadius * sin(phiPrev), u_ObjectZ);

            float phiCurr = u_ObjectOmega * rayT + u_ObjectPhi0;
            vec3 objPosCurr = vec3(u_ObjectOrbitRadius * cos(phiCurr), u_ObjectOrbitRadius * sin(phiCurr), u_ObjectZ);

            vec3 dp0 = prevPos - objPosPrev;
            vec3 dp1 = pos - objPosCurr;
            vec3 vRel = dp1 - dp0;
            float vRel2 = dot(vRel, vRel);
            float sStar = (vRel2 > 1.0e-10) ? clamp(-dot(dp0, vRel) / vRel2, 0.0, 1.0) : 0.0;

            vec3 hitPos = mix(prevPos, pos, sStar);
            float hitT = mix(prevT, rayT, sStar);
            float phiHit = u_ObjectOmega * hitT + u_ObjectPhi0;
            vec3 objPosHit = vec3(u_ObjectOrbitRadius * cos(phiHit), u_ObjectOrbitRadius * sin(phiHit), u_ObjectZ);

            vec3 hitRel = hitPos - objPosHit;
            float hitDist = length(hitRel);

            if (hitDist <= u_ObjectRadius) {
                vec3 hitP = mix(prevP, p_spatial, sStar);

                // Object 4-velocity u^mu = u0 * (1, -Omega * Y, Omega * X, 0)
                float rObjHit = length(objPosHit.xy);
                float a2Obj = u_Spin * u_Spin;
                float denomVObj = rObjHit * rObjHit + a2Obj;
                float lxObj = (rObjHit * objPosHit.x + u_Spin * objPosHit.y) / denomVObj;
                float lyObj = (rObjHit * objPosHit.y - u_Spin * objPosHit.x) / denomVObj;
                float HObj = u_Mass / max(1.0e-6, rObjHit);

                float l_dot_u = 1.0 + u_ObjectOmega * (lyObj * objPosHit.x - lxObj * objPosHit.y);
                float eta_u_u = -1.0 + (u_ObjectOmega * u_ObjectOmega) * (objPosHit.x * objPosHit.x + objPosHit.y * objPosHit.y);
                float denomContract = eta_u_u + 2.0 * HObj * (l_dot_u * l_dot_u);
                float u0 = (denomContract < 0.0) ? 1.0 / sqrt(-denomContract) : 1.0;

                // Contravariant coordinate 3-velocity
                float vx = -u_ObjectOmega * objPosHit.y;
                float vy = u_ObjectOmega * objPosHit.x;
                float vz = 0.0;

                // Invariant frequency shift: g = u_obs^0 / [ u^0 (1 - p · v) ]
                float pDotV = hitP.x * vx + hitP.y * vy + hitP.z * vz;
                float denomG = u0 * (1.0 - pDotV);
                float uObs0 = 1.0 / sqrt(max(1.0e-6, -g[0][0]));
                float gShift = (abs(denomG) > 1.0e-6) ? clamp(uObs0 / denomG, 0.05, 5.0) : 1.0;

                float g2 = gShift * gShift;
                float g4 = g2 * g2;

                // Distinctive procedural surface appearance
                float distNorm = hitDist / u_ObjectRadius;
                vec3 localNormal = (hitDist > 1.0e-6) ? hitRel / hitDist : vec3(0.0, 0.0, 1.0);
                float lat = acos(clamp(localNormal.z, -1.0, 1.0));
                float lon = atan(localNormal.y, localNormal.x);
                float bands = 0.85 + 0.15 * cos(lat * 12.0) * cos(lon * 8.0);
                float coreGlow = exp(-distNorm * distNorm * 2.0);
                float limb = 0.7 + 0.3 * (1.0 - distNorm);

                vec3 dopplerTint = (gShift > 1.0) ? mix(vec3(1.0), vec3(0.8, 0.95, 1.2), min(1.0, (gShift - 1.0) * 0.5))
                                                   : mix(vec3(1.0), vec3(1.2, 0.75, 0.5), min(1.0, (1.0 - gShift) * 0.5));

                vec3 emitColor = u_ObjectBaseColor * (bands * limb + 0.5 * coreGlow) * dopplerTint;
                objectColor = g4 * u_ObjectRadiance * emitColor;

                rayState = 4; // OBJECT
                break;
            }
        }

        // Check for intersection with thin equatorial accretion disk at Z = 0
        if (u_EnableDisk == 1 && prevPos.z * pos.z <= 0.0 && prevPos.z != pos.z) {
            float tau = clamp(-prevPos.z / (pos.z - prevPos.z), 0.0, 1.0);
            vec3 hitPos = mix(prevPos, pos, tau);
            float rHit = length(hitPos.xy);
            crossings++;
            if (rHit >= u_DiskInnerRadius && rHit <= u_DiskOuterRadius) {
                hitRadius = rHit;
                vec3 hitP = mix(prevP, p_spatial, tau);

                // Relativistic Keplerian angular velocity Omega = sqrt(M) / (r^(3/2) + a * sqrt(M))
                float omega = sqrt(u_Mass) / (pow(rHit, 1.5) + u_Spin * sqrt(u_Mass));

                // Disk 4-velocity u^mu = u0 * (1, -Omega * Y, Omega * X, 0)
                // Factored Kerr-Schild metric contraction at Z = 0:
                float a2 = u_Spin * u_Spin;
                float denom_v = rHit * rHit + a2;
                float lx = (rHit * hitPos.x + u_Spin * hitPos.y) / denom_v;
                float ly = (rHit * hitPos.y - u_Spin * hitPos.x) / denom_v;
                float H = u_Mass / rHit;

                float l_dot_u = 1.0 + omega * (ly * hitPos.x - lx * hitPos.y);
                float eta_u_u = -1.0 + (omega * omega) * (hitPos.x * hitPos.x + hitPos.y * hitPos.y);
                float denomContract = eta_u_u + 2.0 * H * (l_dot_u * l_dot_u);
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
                float tEmit = pow(max(1.0e-12, F), 0.25);
                float tObs = gShift * tEmit;

                // Relativistic frequency shift beaming g^4
                float g2 = gShift * gShift;
                float g4 = g2 * g2;

                // Principled dimensionless reference normalization:
                // Peak emissivity of Novikov-Thorne profile analytically occurs at r_peak = (49/36) * r_in:
                float rPeak = 1.361111 * u_DiskInnerRadius;
                float fPeak = u_Mass / (7.0 * rPeak * rPeak * rPeak);
                float fNorm = (fPeak > 1.0e-7) ? clamp(F / fPeak, 0.0, 1.0) : 0.0;

                // Physical transferred emission: I_phys = g^4 * fNorm
                // Follows relativistic invariant intensity transfer I_obs ∝ g^4 * I_emit
                // No artificial outer taper or edge gradient; emissivity is governed purely by the physical model
                float iPhys = g4 * fNorm;
                float radiance = iPhys;

                // Thermal blackbody spectral color approximation
                float tNorm = clamp(tObs * 4.0, 0.0, 2.5);
                vec3 thermalRamp = vec3(
                    clamp(1.0 + 0.3 * tNorm, 0.0, 1.5),
                    clamp(tNorm * tNorm * 0.45 + tNorm * 0.25, 0.0, 1.2),
                    clamp(tNorm * tNorm * tNorm * 0.35, 0.0, 1.2)
                );

                diskColor = radiance * thermalRamp;
                rayState = 3; // DISK
                break;
            }
        }
    }

    // Finalize classification of unresolved rays using physical trajectory state
    if (rayState == 0) {
        if (movingOutward && prevR > 5.0) {
            rayState = 2; // Moving outward into asymptotic Minkowski space
        } else if (prevR <= rCapture + 0.3) {
            rayState = 1; // Trapped in horizon vicinity
        }
    }

    outState = rayState;
    outMinR = minR;
    outCrossings = crossings;
    outHitR = hitRadius;

    if (rayState == 4) {
        // Relativistic test object (unbounded HDR radiance)
        return vec4(objectColor, 1.0);
    } else if (rayState == 3) {
        // Relativistic equatorial accretion disk (unbounded HDR radiance)
        return vec4(diskColor, 1.0);
    } else if (rayState == 1) {
        // True black hole shadow (strictly 0.0 radiance, alpha 0.0 for shadow protection)
        return vec4(0.0, 0.0, 0.0, 0.0);
    } else if (rayState == 2) {
        // Physically escaped ray: clean, deep black background for M6 (no procedural stars)
        return vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        // UNRESOLVED: budget exhausted without proving capture, escape, or disk intersection.
        // Strictly pure black visually, alpha 0.5 distinguishes unresolved in telemetry.
        return vec4(0.0, 0.0, 0.0, 0.5);
    }
}

void main() {
    // Aspect-ratio-corrected normalized device coordinates in [-1, 1]
    vec2 st = (gl_FragCoord.xy * 2.0 - u_Resolution.xy) / min(u_Resolution.x, u_Resolution.y);

    int baseState;
    float baseMinR;
    int baseCrossings;
    float baseHitR;
    vec4 baseSample = traceRaySample(st, baseState, baseMinR, baseCrossings, baseHitR);

    int rayState = baseState;
    if (rayState == 1) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else if (rayState == 2) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else if (rayState == 3) {
        fragColor = baseSample;
    } else if (rayState == 4) {
        fragColor = baseSample;
    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 0.5);
    }

    // Selective Subpixel Supersampling (Physical Subpixel Sampling Gate):
    // Only pixels near strong-lensing/shadow boundary or unresolved subpixel filaments
    // take 4 additional physical rays in a symmetric 2D pattern around pixel center.
    // 97%+ of screen executes only the single base ray.
    bool needsRefinement = (baseCrossings >= 2) || (baseMinR < 2.5) || (baseState == 3 && baseHitR < 6.0) || (baseState == 4);

    if (needsRefinement) {
        float pxScale = 2.0 / min(u_Resolution.x, u_Resolution.y);
        vec2 off = vec2(0.30 * pxScale, 0.30 * pxScale);

        int s1State, s2State, s3State, s4State;
        float s1MinR, s2MinR, s3MinR, s4MinR;
        float s1HitR, s2HitR, s3HitR, s4HitR;
        int s1Crossings, s2Crossings, s3Crossings, s4Crossings;

        vec4 sample1 = traceRaySample(st + vec2(-off.x, -off.y), s1State, s1MinR, s1Crossings, s1HitR);
        vec4 sample2 = traceRaySample(st + vec2( off.x, -off.y), s2State, s2MinR, s2Crossings, s2HitR);
        vec4 sample3 = traceRaySample(st + vec2(-off.x,  off.y), s3State, s3MinR, s3Crossings, s3HitR);
        vec4 sample4 = traceRaySample(st + vec2( off.x,  off.y), s4State, s4MinR, s4Crossings, s4HitR);

        fragColor = (baseSample + sample1 + sample2 + sample3 + sample4) / 5.0;

        // Tier 2: Adaptive High-Frequency Boundary & Caustic Refinement (M7 Bounded Refinement)
        // Evaluates 4 additional axial quarter-offsets (total 9 samples) ONLY when Tier 1 detects
        // mixed topological outcomes (e.g. subpixel boundary between disk and shadow/sky)
        // or extreme strong-field caustic winding (rMin < 2.20M with crossings >= 2).
        bool hasMixedOutcomes = ((baseState == 3 || s1State == 3 || s2State == 3 || s3State == 3 || s4State == 3 ||
                                  baseState == 4 || s1State == 4 || s2State == 4 || s3State == 4 || s4State == 4) &&
                                 (baseState != 3 || s1State != 3 || s2State != 3 || s3State != 3 || s4State != 3 ||
                                  baseState != 4 || s1State != 4 || s2State != 4 || s3State != 4 || s4State != 4));
        bool needsTier2 = hasMixedOutcomes || (baseMinR < 2.20 && baseCrossings >= 2);
        if (needsTier2) {
            int s5State, s6State, s7State, s8State;
            float s5MinR, s6MinR, s7MinR, s8MinR;
            float s5HitR, s6HitR, s7HitR, s8HitR;
            int s5Crossings, s6Crossings, s7Crossings, s8Crossings;

            vec4 sample5 = traceRaySample(st + vec2(-off.x, 0.0), s5State, s5MinR, s5Crossings, s5HitR);
            vec4 sample6 = traceRaySample(st + vec2( off.x, 0.0), s6State, s6MinR, s6Crossings, s6HitR);
            vec4 sample7 = traceRaySample(st + vec2(0.0, -off.y), s7State, s7MinR, s7Crossings, s7HitR);
            vec4 sample8 = traceRaySample(st + vec2(0.0,  off.y), s8State, s8MinR, s8Crossings, s8HitR);

            fragColor = (baseSample + sample1 + sample2 + sample3 + sample4 + sample5 + sample6 + sample7 + sample8) / 9.0;
        }
    }
}
