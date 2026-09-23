#version 300 es
precision highp float;

in vec2 v_TexCoord;
out vec4 fragColor;

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
// Debug-only MRT outputs. The normal production program is compiled without this define and
// therefore writes only the HDR scene color. These values are reduced to 1x1 offscreen textures
// only when explicit debug instrumentation is enabled.
layout(location = 1) out vec4 workloadTierStats;
layout(location = 2) out vec4 workloadCostStats;
#endif

#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
#if defined(GARGANTUA_WORKLOAD_TELEMETRY) && defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE)
layout(location = 3) out vec4 workloadSemanticCache;
#elif defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
layout(location = 1) out vec4 animationSemanticCache;
#endif
float gargantuaSemanticDiskHitAzimuth = 0.0;
vec3 gargantuaSemanticDeflectedDir = vec3(0.0, 0.0, 1.0);
#endif

// Uniforms
uniform vec2 u_Resolution;   // Screen or scaled FBO resolution (width, height)
uniform float u_Time;        // Elapsed time, or base-16 digit zero for animation variants
#if defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
uniform float u_TimeDigit1;
uniform vec2 u_TimeDigits23;
uniform vec2 u_TimeDigits45;
uniform vec2 u_TimeDigits67;
#endif
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

// Relativistic synthetic test marker uniforms (M9 Option B: coordinate sphere in Kerr-Schild Cartesian chart)
uniform int u_EnableObject;         // 1 to render relativistic test marker, 0 otherwise
uniform float u_ObjectRadius;       // Coordinate radius R_coord of test marker in Kerr-Schild units M
uniform float u_ObjectOrbitRadius;  // Orbital coordinate radius in Kerr-Schild coordinates
uniform float u_ObjectOmega;        // Keplerian angular velocity Ω
uniform float u_ObjectPhi0;         // Initial phase angle φ_0
uniform float u_ObjectZ;            // Z coordinate of orbit (0 for equatorial)
uniform vec3 u_ObjectBaseColor;     // Procedural base color (e.g. cyan/electric azure)
uniform float u_ObjectRadiance;     // Base surface radiance

#if defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
const float GARGANTUA_TIME_BASE = 16.0;
const float GARGANTUA_TAU = 6.28318530718;

float gargantuaAnimationPhaseAdvance(float factor) {
    float coefficient = fract(factor);
    float advance = u_Time * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigit1 * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigits23.x * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigits23.y * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigits45.x * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigits45.y * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigits67.x * coefficient;
    coefficient = fract(coefficient * GARGANTUA_TIME_BASE);
    advance += u_TimeDigits67.y * coefficient;
    return fract(advance);
}

#define GARGANTUA_OBJECT_PHASE(t) (GARGANTUA_TAU * gargantuaAnimationPhaseAdvance(u_ObjectOmega / GARGANTUA_TAU) + u_ObjectOmega * (t) + u_ObjectPhi0)
#else
#define GARGANTUA_OBJECT_PHASE(t) (u_ObjectOmega * (t) + u_ObjectPhi0)
#endif

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

// High-fidelity anti-aliased cosmos with Galactic Plane

// Deterministic high-precision 3D hash
vec3 cosmosHash33(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yxz + 19.19);
    return fract((p.xxy + p.yxx) * p.zyx);
}

// Lightweight 3D value noise for soft nebula clouds
float cosmosNoise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float n000 = dot(cosmosHash33(i + vec3(0.0, 0.0, 0.0)) - 0.5, f - vec3(0.0, 0.0, 0.0));
    float n100 = dot(cosmosHash33(i + vec3(1.0, 0.0, 0.0)) - 0.5, f - vec3(1.0, 0.0, 0.0));
    float n010 = dot(cosmosHash33(i + vec3(0.0, 1.0, 0.0)) - 0.5, f - vec3(0.0, 1.0, 0.0));
    float n110 = dot(cosmosHash33(i + vec3(1.0, 1.0, 0.0)) - 0.5, f - vec3(1.0, 1.0, 0.0));
    float n001 = dot(cosmosHash33(i + vec3(0.0, 0.0, 1.0)) - 0.5, f - vec3(0.0, 0.0, 1.0));
    float n101 = dot(cosmosHash33(i + vec3(1.0, 0.0, 1.0)) - 0.5, f - vec3(1.0, 0.0, 1.0));
    float n011 = dot(cosmosHash33(i + vec3(0.0, 1.0, 1.0)) - 0.5, f - vec3(0.0, 1.0, 1.0));
    float n111 = dot(cosmosHash33(i + vec3(1.0, 1.0, 1.0)) - 0.5, f - vec3(1.0, 1.0, 1.0));

    return mix(
        mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
        mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y),
        u.z
    );
}

// 2-Octave FBM for celestial depth
float cosmosFbm(vec3 p) {
    return cosmosNoise3D(p) * 0.65 + cosmosNoise3D(p * 2.05) * 0.35;
}

// Master Procedural Deep-Blue Cosmos
vec3 renderProceduralCosmos(vec3 skyDir) {
    // -------------------------------------------------------------
    // 1. Deep Celestial Midnight & Sapphire Nebula Backdrop
    // -------------------------------------------------------------
    float nebulaVal = cosmosFbm(skyDir * 3.5);
    float cloudNoise = cosmosFbm(skyDir * 7.5 + vec3(1.7, 9.2, 4.3));

    // Base dark navy space
    vec3 baseSpace = vec3(0.007, 0.014, 0.038);
    // Luminous midnight-sapphire nebula
    vec3 sapphireCloud = vec3(0.022, 0.048, 0.115);
    // Dark silhouette dust rift
    vec3 darkDust = vec3(0.002, 0.004, 0.009);

    float cloudFactor = smoothstep(-0.25, 0.45, nebulaVal);
    float riftFactor = smoothstep(0.05, 0.50, cloudNoise);

    vec3 celestialBg = mix(baseSpace, sapphireCloud, cloudFactor);
    celestialBg = mix(celestialBg, darkDust, riftFactor * 0.70);

    // -------------------------------------------------------------
    // 2. Sparse Pinpoint Starfield (93% Empty Void)
    // -------------------------------------------------------------
    vec3 p = skyDir * 80.0;
    vec3 ip = floor(p);
    vec3 fp = fract(p);
    vec3 starAccum = vec3(0.0);

    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 neighbor = vec3(float(x), float(y), float(z));
                vec3 cellId = ip + neighbor;
                vec3 h = cosmosHash33(cellId);

                // CRITICAL SPARSITY GATE: 93% of cells are pure empty void
                if (h.x > 0.070) continue;

                // Jittered position within neighbor cell
                vec3 starPos = neighbor + h.yzx - 0.5;
                float dist = length(fp - starPos);

                // Natural brightness distribution
                float roll = h.y;
                float isProminent = step(0.96, roll);
                float isMedium = step(0.80, roll) * (1.0 - isProminent);
                float isFaint = (1.0 - isProminent) * (1.0 - isMedium);

                float intensity = isProminent * (0.85 + 0.45 * h.z)
                                + isMedium * (0.32 + 0.18 * h.z)
                                + isFaint * (0.12 + 0.08 * h.z);

                // Core radius calibrated to 1.0 - 2.5 mobile screen pixels
                float coreRadius = isProminent > 0.5 ? 0.12 : (isMedium > 0.5 ? 0.08 : 0.055);
                float starProfile = exp(-(dist * dist) / (2.0 * coreRadius * coreRadius));

                // Star spectrum: crisp diamond white, icy blue, and pale warm gold
                vec3 starColor = mix(
                    vec3(1.0, 0.88, 0.72), // Subtle warm star
                    mix(vec3(0.95, 0.98, 1.0), vec3(0.72, 0.88, 1.0), h.z), // Crisp icy-blue/white
                    h.x / 0.070
                );

                starAccum += starColor * starProfile * intensity;
            }
        }
    }

    return celestialBg + starAccum;
}

vec3 sample_procedural_sky(vec3 dir) {
    return renderProceduralCosmos(normalize(dir));
}

// Legacy helpers for compatibility - DO NOT REMOVE, disk code uses some
float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    // Alias to cosmosHash33 for backward compat
    return cosmosHash33(p);
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);
    return mix(nxy0, nxy1, f.z);
}

float fbm3D(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * valueNoise3(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}


vec4 traceRaySample(
    vec2 stCoord,
    out int outState,
    out float outMinR,
    out int outCrossings,
    out float outHitR
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    , out int outStepsTaken
#endif
) {
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
#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    gargantuaSemanticDiskHitAzimuth = 0.0;
#endif
#if defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    float rayT = 0.0;
#else
    float rayT = u_Time;
#endif
    float minR = rInit;
    int crossings = 0;
    float hitRadius = 0.0;

    float prevR = rInit;
    bool movingOutward = false;
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    int stepsTaken = 0;
#endif

    // Primary null Hamiltonian geodesic integration loop
    for (int step = 0; step < MAX_INTEGRATION_STEPS; step++) {
        if (step >= maxSteps) {
            break;
        }
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
        stepsTaken = step + 1;
#endif

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

        // M9 experimental marker disabled for production to eliminate cyan horizon fringe
#if 0
        // Check for intersection with relativistic test marker (bounded linear-interpolation closest-approach approximation)
        float minStepR = min(prevR, r);
        float maxStepR = max(prevR, r);
        float objMargin = u_ObjectRadius + 1.2;
        if (u_EnableObject == 1 && u_ObjectOrbitRadius >= minStepR - objMargin && u_ObjectOrbitRadius <= maxStepR + objMargin) {
            float phiPrev = GARGANTUA_OBJECT_PHASE(prevT);
            vec3 objPosPrev = vec3(u_ObjectOrbitRadius * cos(phiPrev), u_ObjectOrbitRadius * sin(phiPrev), u_ObjectZ);

            float phiCurr = GARGANTUA_OBJECT_PHASE(rayT);
            vec3 objPosCurr = vec3(u_ObjectOrbitRadius * cos(phiCurr), u_ObjectOrbitRadius * sin(phiCurr), u_ObjectZ);

            vec3 dp0 = prevPos - objPosPrev;
            vec3 dp1 = pos - objPosCurr;
            vec3 vRel = dp1 - dp0;
            float vRel2 = dot(vRel, vRel);
            float sStar = (vRel2 > 1.0e-10) ? clamp(-dot(dp0, vRel) / vRel2, 0.0, 1.0) : 0.0;

            vec3 hitPos = mix(prevPos, pos, sStar);
            float hitT = mix(prevT, rayT, sStar);
            float phiHit = GARGANTUA_OBJECT_PHASE(hitT);
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

                // Invariant frequency shift: g = (-p_μ u_obs^μ) / (-p_μ u_emit^μ)
                // In backward ray tracing, hitP is directed from observer into scene.
                // Consistent with thin-disk invariant convention: denomG = u0 * (1.0 + omega * lz) = u0 * (1.0 + p · v)
                float pDotV = hitP.x * vx + hitP.y * vy + hitP.z * vz;
                float denomG = u0 * (1.0 + pDotV);
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
#endif

        // Check for intersection with thin equatorial accretion disk at Z = 0
        if (u_EnableDisk == 1 && prevPos.z * pos.z <= 0.0 && prevPos.z != pos.z) {
            float tau = clamp(-prevPos.z / (pos.z - prevPos.z), 0.0, 1.0);
            vec3 hitPos = mix(prevPos, pos, tau);
            float rHit = length(hitPos.xy);
            crossings++;
            if (rHit >= u_DiskInnerRadius && rHit <= u_DiskOuterRadius) {
                hitRadius = rHit;
#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
                gargantuaSemanticDiskHitAzimuth = atan(hitPos.y, hitPos.x);
#endif
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

                // Interstellar-grade thermal ramp: blueshifted side = incandescent white-hot
                // with pale gold edges, redshifted side = deep amber-brown.
                // Calibrated to survive ACES shoulder while preserving filament contrast.
                float tNorm = clamp(tObs * 4.0, 0.0, 2.5);
                // Explicit ISCO rejection for inner silhouette cleanup (razor-sharp torque-free boundary)
                // Disk hit already gated by rHit >= r_in, but enforce strict zero below to avoid fringe.
                if (rHit < u_DiskInnerRadius) {
                    // Should never reach here due to outer gate, but guard against numerical fringe
                    continue;
                }
                // Amber-brown to white-hot grading
                vec3 thermalRamp = vec3(
                    clamp(1.0 + 0.25 * tNorm + 0.05 * tNorm * tNorm, 0.0, 1.95),
                    clamp(0.35 + 0.25 * tNorm + 0.10 * tNorm * tNorm, 0.0, 1.65),
                    clamp(0.15 + 0.15 * tNorm + 0.08 * tNorm * tNorm, 0.0, 1.25)
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
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    outStepsTaken = stepsTaken;
#endif

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
        // Re-enabled lensed background starfield with deflected vector packing for dynamic lensing
        vec3 skyDir = normalize(p_spatial);
        if (dot(skyDir, skyDir) < 0.5) {
            skyDir = normalize(vec3(0.0, 0.0, 1.0));
        }
#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
        gargantuaSemanticDeflectedDir = skyDir;
#endif
        vec3 sky = sample_procedural_sky(skyDir);
        float skyScale = 0.85;
        vec3 scaledSky = sky * skyScale;
        scaledSky = clamp(scaledSky, vec3(0.0), vec3(0.45));
        return vec4(scaledSky, 1.0);
    } else {
        // UNRESOLVED: budget exhausted without proving capture, escape, or disk intersection.
        // Strictly pure black visually, alpha 0.5 distinguishes unresolved in telemetry.
        return vec4(0.0, 0.0, 0.0, 0.5);
    }
}

// M7 outcome classes: disk/object share the material class, while captured and escaped remain
// distinct topological outcomes. This prevents uniform disk/object samples from escalating while
// still refining captured/escaped boundaries.
int adaptiveOutcomeClass(int state) {
    if (state == 3 || state == 4) return 0; // emitting material
    if (state == 1) return 1; // captured
    if (state == 2) return 2; // escaped
    if (state == 0) return 3; // unresolved
    return 4; // invalid/unknown state; never collapse it into unresolved
}

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
bool gargantuaRayIsDifficult(int state, float minR, int crossings, float hitR) {
    return crossings >= 2 || minR < 2.5 || (state == 3 && hitR < 6.0) || state == 4;
}
#endif

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
#define GARGANTUA_TRACE_RAY_SAMPLE(coord, state, minR, crossings, hitR, steps) \
    traceRaySample(coord, state, minR, crossings, hitR, steps)
#else
#define GARGANTUA_TRACE_RAY_SAMPLE(coord, state, minR, crossings, hitR, steps) \
    traceRaySample(coord, state, minR, crossings, hitR)
#endif

void main() {
    // Aspect-ratio-corrected normalized device coordinates in [-1, 1]
    vec2 st = (gl_FragCoord.xy * 2.0 - u_Resolution.xy) / min(u_Resolution.x, u_Resolution.y);

    int baseState;
    float baseMinR;
    int baseCrossings;
    float baseHitR;
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    int baseSteps;
#endif
    vec4 baseSample = GARGANTUA_TRACE_RAY_SAMPLE(
        st, baseState, baseMinR, baseCrossings, baseHitR, baseSteps
    );
#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    float baseDiskHitAzimuth = gargantuaSemanticDiskHitAzimuth;
#endif

    int rayState = baseState;
    if (rayState == 1) {
        // Strict shadow: pure black, alpha 0.0 for shadow protection (never bloomed)
        fragColor = vec4(0.0, 0.0, 0.0, 0.0);
    } else if (rayState == 2) {
        // Escaped: lensed starfield background, alpha 1.0 preserved
        fragColor = baseSample;
    } else if (rayState == 3) {
        fragColor = baseSample;
    } else if (rayState == 4) {
        fragColor = baseSample;
    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 0.5);
    }

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    // Bounded M7 adaptive statistics. Values describe the actual rays executed by this pixel.
    int tierForStats = 0;
    int raysForStats = 1;
    int totalStepsForStats = baseSteps;
    int maxStepsForStats = baseSteps;
    int diskHitsForStats = (baseState == 3) ? 1 : 0;
    int difficultRayCountForStats = 0;
#endif

    // Selective Subpixel Supersampling (Physical Subpixel Sampling Gate):
    // Only pixels near strong-lensing/shadow boundary or unresolved subpixel filaments
    // take 4 additional physical rays in a symmetric 2D pattern around pixel center.
    bool needsRefinement = (baseCrossings >= 2) || (baseMinR < 2.5) || (baseState == 3 && baseHitR < 6.0) || (baseState == 4);
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    difficultRayCountForStats = gargantuaRayIsDifficult(baseState, baseMinR, baseCrossings, baseHitR) ? 1 : 0;
#endif

    if (needsRefinement) {
        float pxScale = 2.0 / min(u_Resolution.x, u_Resolution.y);
        vec2 off = vec2(0.30 * pxScale, 0.30 * pxScale);

        int s1State, s2State, s3State, s4State;
        float s1MinR, s2MinR, s3MinR, s4MinR;
        float s1HitR, s2HitR, s3HitR, s4HitR;
        int s1Crossings, s2Crossings, s3Crossings, s4Crossings;
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
        int s1Steps, s2Steps, s3Steps, s4Steps;
#endif

        vec4 sample1 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2(-off.x, -off.y), s1State, s1MinR, s1Crossings, s1HitR, s1Steps);
        vec4 sample2 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2( off.x, -off.y), s2State, s2MinR, s2Crossings, s2HitR, s2Steps);
        vec4 sample3 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2(-off.x,  off.y), s3State, s3MinR, s3Crossings, s3HitR, s3Steps);
        vec4 sample4 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2( off.x,  off.y), s4State, s4MinR, s4Crossings, s4HitR, s4Steps);

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
        tierForStats = 1;
        raysForStats = 5;
        totalStepsForStats = baseSteps + s1Steps + s2Steps + s3Steps + s4Steps;
        maxStepsForStats = max(max(baseSteps, s1Steps), max(max(s2Steps, s3Steps), s4Steps));
        difficultRayCountForStats +=
            (gargantuaRayIsDifficult(s1State, s1MinR, s1Crossings, s1HitR) ? 1 : 0) +
            (gargantuaRayIsDifficult(s2State, s2MinR, s2Crossings, s2HitR) ? 1 : 0) +
            (gargantuaRayIsDifficult(s3State, s3MinR, s3Crossings, s3HitR) ? 1 : 0) +
            (gargantuaRayIsDifficult(s4State, s4MinR, s4Crossings, s4HitR) ? 1 : 0);
        diskHitsForStats = (baseState == 3 ? 1 : 0) + (s1State == 3 ? 1 : 0) +
            (s2State == 3 ? 1 : 0) + (s3State == 3 ? 1 : 0) + (s4State == 3 ? 1 : 0);
#endif

        fragColor = (baseSample + sample1 + sample2 + sample3 + sample4) / 5.0;

        // Tier 2: refine only a genuine outcome boundary or deep winding. Disk/object share one
        // material class; captured and escaped remain distinct classes.
        int baseOutcomeClass = adaptiveOutcomeClass(baseState);
        bool hasMixedOutcomes =
            adaptiveOutcomeClass(s1State) != baseOutcomeClass ||
            adaptiveOutcomeClass(s2State) != baseOutcomeClass ||
            adaptiveOutcomeClass(s3State) != baseOutcomeClass ||
            adaptiveOutcomeClass(s4State) != baseOutcomeClass;
        bool needsTier2 = hasMixedOutcomes || (baseMinR < 2.20 && baseCrossings >= 2);
        if (needsTier2) {
            int s5State, s6State, s7State, s8State;
            float s5MinR, s6MinR, s7MinR, s8MinR;
            float s5HitR, s6HitR, s7HitR, s8HitR;
            int s5Crossings, s6Crossings, s7Crossings, s8Crossings;
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
            int s5Steps, s6Steps, s7Steps, s8Steps;
#endif

            vec4 sample5 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2(-off.x, 0.0), s5State, s5MinR, s5Crossings, s5HitR, s5Steps);
            vec4 sample6 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2( off.x, 0.0), s6State, s6MinR, s6Crossings, s6HitR, s6Steps);
            vec4 sample7 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2(0.0, -off.y), s7State, s7MinR, s7Crossings, s7HitR, s7Steps);
            vec4 sample8 = GARGANTUA_TRACE_RAY_SAMPLE(st + vec2(0.0,  off.y), s8State, s8MinR, s8Crossings, s8HitR, s8Steps);

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
            tierForStats = 2;
            raysForStats = 9;
            totalStepsForStats += s5Steps + s6Steps + s7Steps + s8Steps;
            maxStepsForStats = max(maxStepsForStats, max(max(s5Steps, s6Steps), max(s7Steps, s8Steps)));
            difficultRayCountForStats +=
                (gargantuaRayIsDifficult(s5State, s5MinR, s5Crossings, s5HitR) ? 1 : 0) +
                (gargantuaRayIsDifficult(s6State, s6MinR, s6Crossings, s6HitR) ? 1 : 0) +
                (gargantuaRayIsDifficult(s7State, s7MinR, s7Crossings, s7HitR) ? 1 : 0) +
                (gargantuaRayIsDifficult(s8State, s8MinR, s8Crossings, s8HitR) ? 1 : 0);
            diskHitsForStats += (s5State == 3 ? 1 : 0) + (s6State == 3 ? 1 : 0) +
                (s7State == 3 ? 1 : 0) + (s8State == 3 ? 1 : 0);
#endif

            fragColor = (baseSample + sample1 + sample2 + sample3 + sample4 + sample5 + sample6 + sample7 + sample8) / 9.0;
        }
    }

#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    workloadTierStats = vec4(
        tierForStats == 0 ? 1.0 : 0.0,
        tierForStats == 1 ? 1.0 : 0.0,
        tierForStats == 2 ? 1.0 : 0.0,
        float(difficultRayCountForStats)
    );
    workloadCostStats = vec4(
        float(raysForStats),
        float(totalStepsForStats),
        float(maxStepsForStats),
        float(diskHitsForStats)
    );
#endif

#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    vec4 semanticRecord;
    if (baseState == 3) {
        float diskRadiusNormalized = clamp(
            (baseHitR - u_DiskInnerRadius) /
                max(1.0e-6, u_DiskOuterRadius - u_DiskInnerRadius),
            0.0,
            1.0
        );
        float diskAzimuthNormalized = fract(baseDiskHitAzimuth / 6.28318530718 + 0.5);
        semanticRecord = vec4(
            diskRadiusNormalized,
            diskAzimuthNormalized,
            float(baseCrossings),
            float(baseState)
        );
    } else if (baseState == 2) {
        // Pack exact curved-spacetime deflected unit vector for zero-cost dynamic lensing
        // Format: vec4(d.x, d.y, d.z, 2.0) as per flagship spec
        vec3 d = normalize(gargantuaSemanticDeflectedDir);
        if (dot(d, d) < 0.5) d = vec3(0.0, 0.0, 1.0);
        semanticRecord = vec4(d, 2.0);
    } else {
        semanticRecord = vec4(
            0.0,
            0.0,
            float(baseCrossings),
            float(baseState)
        );
    }
#if defined(GARGANTUA_WORKLOAD_TELEMETRY) && defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE)
    workloadSemanticCache = semanticRecord;
#elif defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    animationSemanticCache = semanticRecord;
#endif
#endif
}
