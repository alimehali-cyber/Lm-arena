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
uniform int u_EnableDoppler;     // 0 = Movie Mode (Default), 1 = Strict GR Mode

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

// Legacy logarithmic winding coefficient of the previous cloud/streak density model, retained
// for continuity. The Direction A generator bakes its own Keplerian shear, 26 (r/r_in)^-1.5 with a
// -10 ln(r/r_in) winding term, directly into evaluate3DVolumetricGasDensity().
const float GARGANTUA_WIND_COEFF = 11.0;

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

// High-fidelity anti-aliased cosmos with Galactic Plane - Dark inky midnight-blue +20% stars refined

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
    float n001 = dot(cosmosHash33(i + vec3(0.0, 0.0, 1.0)) - 0.5, f - vec3(0.0, 1.0, 1.0));
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

// Deep-space backdrop gain compensating the display OETF applied in gargantua_composite.frag.
const float GARGANTUA_SKY_BACKDROP_GAIN = 0.1;

// Master Procedural Deep-Blue Cosmos (Darker inky midnight-blue + 20% more stars, refined radii)
vec3 renderProceduralCosmos(vec3 skyDir) {
    // -------------------------------------------------------------
    // 1. Inky Midnight-Blue Backdrop with Moody Sapphire Clouds
    // -------------------------------------------------------------
    float nebulaVal = cosmosFbm(skyDir * 3.5);
    float cloudNoise = cosmosFbm(skyDir * 7.5 + vec3(1.7, 9.2, 4.3));

    // Deep inky midnight void (darkened so space stays deep and rich)
    vec3 baseSpace = vec3(0.0010, 0.0022, 0.0065);
    // Subtle sapphire-blue dust
    vec3 sapphireCloud = vec3(0.0045, 0.0095, 0.0260);
    // Dark silhouette absorption rift
    vec3 darkDust = vec3(0.0003, 0.0006, 0.0015);

    float cloudFactor = smoothstep(-0.25, 0.50, nebulaVal);
    float riftFactor = smoothstep(0.05, 0.50, cloudNoise);

    vec3 celestialBg = mix(baseSpace, sapphireCloud, cloudFactor);
    celestialBg = mix(celestialBg, darkDust, riftFactor * 0.75);

    // -------------------------------------------------------------
    // 2. Sparse Optical Pinpoint Starfield (+20% Star Count, Refined Radii)
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

                // +20% star quantity gate (0.109)
                if (h.x > 0.109) continue;

                vec3 starPos = neighbor + h.yzx - 0.5;
                float dist = length(fp - starPos);

                // Natural distribution weighted heavily toward faint pinpoints
                float roll = h.y;
                float isProminent = step(0.97, roll);
                float isMedium = step(0.80, roll) * (1.0 - isProminent);
                float isFaint = (1.0 - isProminent) * (1.0 - isMedium);

                float intensity = isProminent * (0.80 + 0.40 * h.z)
                                + isMedium * (0.30 + 0.15 * h.z)
                                + isFaint * (0.10 + 0.06 * h.z);

                // Refined optical radii: all new stars are delicate sub-pixel pinpoints
                float coreRadius = isProminent > 0.5 ? 0.10 : (isMedium > 0.5 ? 0.065 : 0.040);
                float starProfile = exp(-(dist * dist) / (2.0 * coreRadius * coreRadius));

                // Balanced star spectral palette
                vec3 starColor = mix(
                    vec3(1.0, 0.88, 0.72),
                    mix(vec3(0.95, 0.98, 1.0), vec3(0.75, 0.88, 1.0), h.z),
                    h.x / 0.109
                );

                starAccum += starColor * starProfile * intensity;
            }
        }
    }

    // The composite now encodes the display OETF (gamma 1/2.2), which lifts this hand-tuned
    // backdrop from ~1-5/255 to a visible navy haze. Scaling it restores its previous displayed
    // black level; stars and the lensed disk are unaffected.
    celestialBg *= GARGANTUA_SKY_BACKDROP_GAIN;

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

// --- Fast Branchless 3D Procedural Hash & Gradient Turbulence ---
float gargantuaHash3D(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.1, 0.1));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float gargantuaNoise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec2 u = f.xy * f.xy * (3.0 - 2.0 * f.xy);
    float uz = f.z * f.z * (3.0 - 2.0 * f.z);

    float n000 = gargantuaHash3D(i + vec3(0.0, 0.0, 0.0));
    float n100 = gargantuaHash3D(i + vec3(1.0, 0.0, 0.0));
    float n010 = gargantuaHash3D(i + vec3(0.0, 1.0, 0.0));
    float n110 = gargantuaHash3D(i + vec3(1.0, 1.0, 0.0));
    float n001 = gargantuaHash3D(i + vec3(0.0, 0.0, 1.0));
    float n101 = gargantuaHash3D(i + vec3(1.0, 0.0, 1.0));
    float n011 = gargantuaHash3D(i + vec3(0.0, 1.0, 1.0));
    float n111 = gargantuaHash3D(i + vec3(1.0, 1.0, 1.0));

    return mix(
        mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
        mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y),
        uz
    );
}

// Domain warp: distorts sample coordinates using a lower-frequency noise field,
// producing organic, non-grid-aligned billowing shapes instead of raw lattice noise.
vec3 gargantuaDomainWarp(vec3 p, float warpStrength) {
    vec3 warp = vec3(
        gargantuaNoise3D(p * 0.5 + vec3(11.3, 2.1, 7.4)),
        gargantuaNoise3D(p * 0.5 + vec3(41.7, 91.3, 3.9)),
        gargantuaNoise3D(p * 0.5 + vec3(5.5, 63.2, 27.8))
    ) - 0.5;
    return p + warp * warpStrength;
}

// Master Direction A: Sheared Spun-Silk Accretion Disk with Shredded Outer Rim
float evaluate3DVolumetricGasDensity(float r, float phi, float zeta, float fNorm, float rIn, float rOut) {
    // 1. Vertical Gaussian compression for a crisp flared slab
    float verticalFalloff = exp(-5.0 * zeta * zeta);

    float rNorm = max(1.0, r / rIn);
    float logR = log(rNorm);

    // 2. Continuous Keplerian differential shear (Omega ~ r^-1.5)
    // Seamless wrapping in phi to prevent any seam artifacts along the lensed arches
    float keplerShear = 26.0 * pow(rNorm, -1.5);
    float phiSheared = phi - keplerShear - 10.0 * logR;

    // Seamless toroidal coordinate mapping for noise sampling
    vec2 circSheared = vec2(cos(phiSheared), sin(phiSheared));

    // 3. Ultra-fine Keplerian Sheared Filaments ("Spun Silk / Molten Glass" threads)
    // Elongated coordinate space: high frequency across radius, smooth along sheared orbit
    vec3 filamentCoord1 = vec3(logR * 36.0, circSheared.x * 4.5 + zeta * 2.2, circSheared.y * 4.5);
    vec3 warpedCoord1 = gargantuaDomainWarp(filamentCoord1, 1.1);
    float fineThreads = gargantuaNoise3D(warpedCoord1);

    vec3 filamentCoord2 = vec3(logR * 72.0, circSheared.x * 9.0 + zeta * 3.5, circSheared.y * 9.0);
    float microThreads = gargantuaNoise3D(filamentCoord2 * 1.5 + vec3(2.3, 7.1, 0.9));

    float spunFilaments = pow(fineThreads * 0.60 + microThreads * 0.40, 1.8) * 1.75;

    // 4. Dramatic, Deep Charcoal Dust Absorption Rifts
    vec3 dustCoord = vec3(logR * 6.5, circSheared.x * 2.0 + zeta * 1.5, circSheared.y * 2.0);
    vec3 warpedDust = gargantuaDomainWarp(dustCoord, 1.4);
    float dustRift = smoothstep(0.28, 0.72, gargantuaNoise3D(warpedDust));

    // Secondary fine-scale dust filigree
    float dustFiligree = smoothstep(0.32, 0.65, gargantuaNoise3D(filamentCoord1 * 1.8 + vec3(1.1, 3.4, 5.2)));
    float totalDust = dustRift * (0.4 + 0.6 * dustFiligree);

    // 5. Knife-Edge ISCO Cutoff: immediate plunge, zero emission inside rIn
    float innerCutoff = smoothstep(rIn, rIn + 0.05 * rIn, r);

    // 6. Violently Shredded, Frayed Outer Rim Wisps (Direction A Reference)
    // Outer boundary dissolves into trailing plasma tendrils rather than a circular edge
    vec3 outerWispCoord = vec3(circSheared.x * 3.0, logR * 8.0, 0.5);
    float outerTrailingWarp = gargantuaNoise3D(outerWispCoord) * 0.7 + gargantuaNoise3D(outerWispCoord * 2.5) * 0.3;
    float frayedRadius = rOut - 4.2 * outerTrailingWarp;
    // Same curve as smoothstep(frayedRadius + 1.2, frayedRadius - 3.2, r), written with ordered
    // edges because GLSL ES leaves smoothstep undefined when edge0 >= edge1.
    float outerWisps = 1.0 - smoothstep(frayedRadius - 3.2, frayedRadius + 1.2, r);

    float rawDensity = (0.28 + 1.15 * spunFilaments) * (0.15 + 0.85 * totalDust);

    return clamp(rawDensity * verticalFalloff * (0.45 + 0.55 * fNorm) * innerCutoff * outerWisps, 0.0, 5.5);
}

// Direction A: Saturated Fiery Interstellar Thermal Spectrum (Zero Mud/Khaki)
vec3 evaluate4TierBlackbodySpectrum(float fNorm, float gShift) {
    float gFactor;
    float iPhys;

    if (u_EnableDoppler == 1) {
        float gClamped = clamp(gShift, 0.10, 4.2);
        gFactor = gClamped;
        iPhys = pow(gClamped, 4.0) * fNorm;
    } else {
        float gMovie = mix(1.0, clamp(gShift, 0.65, 1.65), 0.12);
        gFactor = gMovie;
        iPhys = pow(fNorm, 0.82) * 1.50 + 0.05;
    }

    float tEff = gFactor * pow(max(1.0e-5, fNorm), 0.19);

    // High-Saturation Fiery Thermal Stops (Pure incandescent physics; no blue in midtones)
    vec3 cSmoke    = vec3(0.012, 0.003, 0.001); // Inky charcoal umber absorption
    vec3 cRust     = vec3(0.48, 0.065, 0.003);  // Deep burning blood-orange / rust
    vec3 cAmber    = vec3(2.60, 0.65, 0.015);   // Blazing molten copper-amber
    vec3 cGold     = vec3(7.50, 4.20, 0.35);    // Incandescent canary gold
    vec3 cWhiteHot = vec3(16.5, 15.2, 13.5);    // Blinding white-hot core

    vec3 thermalColor;
    if (tEff < 0.14) {
        thermalColor = mix(cSmoke, cRust, smoothstep(0.0, 0.14, tEff));
    } else if (tEff < 0.38) {
        thermalColor = mix(cRust, cAmber, smoothstep(0.14, 0.38, tEff));
    } else if (tEff < 0.70) {
        thermalColor = mix(cAmber, cGold, smoothstep(0.38, 0.70, tEff));
    } else {
        thermalColor = mix(cGold, cWhiteHot, smoothstep(0.70, 1.0, tEff));
    }

    if (u_EnableDoppler == 1 && gShift > 1.20) {
        float whiteBoost = smoothstep(1.20, 2.10, gShift);
        thermalColor = mix(thermalColor, vec3(17.0, 16.0, 15.0), whiteBoost * 0.90);
    }

    return thermalColor * iPhys * 0.85;
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
    vec3 objectColor = vec3(0.0);

    // Volumetric multi-crossing accumulation state
    vec3 accumDiskRadiance = vec3(0.0);
    float diskTransmittance = 1.0;
    int diskCrossings = 0;
    float primaryHitRadius = 0.0;
    float primaryHitAzimuth = 0.0;

#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    gargantuaSemanticDiskHitAzimuth = 0.0;
#endif
#if defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
    float rayT = 0.0;
#else
    float rayT = u_Time;
#endif
    float minR = rInit;

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

        // Calibrated Adaptive Step Controller: Smooth Caustic Refinement at 30+ FPS
        // Avoid clamping step size inside empty ISCO plunge region: r >= u_DiskInnerRadius - 0.5
        float baseStep = 0.085 * r;
        float dlambda = (r > 6.0 && (movingOutward || r > 18.0)) ? clamp(baseStep, 0.035, 0.55) : clamp(baseStep, 0.035, 0.32);

        // Tight, efficient caustic refinement zone: only subdivide in the immediate photon sphere vicinity
        float rCausticMax = rCapture + 0.95; // ~2.60M for a=0.8M
        if (r > rCapture && r < rCausticMax) {
            float proximity = (r - rCapture) / 0.95;
            float causticStep = mix(0.032, 0.075, proximity);
            dlambda = min(dlambda, causticStep);
        }

        // Advance geodesic via 4th-order Runge-Kutta
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

        // Miller's Planet Relativistic Timelike World-Tube Intersection
        if (u_EnableObject == 1 && r > (rCapture + 0.35)) {
            // Tight radial bounding cylinder check
            float rStepMin = min(prevR, r);
            float rStepMax = max(prevR, r);
            if (u_ObjectOrbitRadius >= (rStepMin - u_ObjectRadius) && u_ObjectOrbitRadius <= (rStepMax + u_ObjectRadius)) {
                // Evaluate planet position at coordinate times
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

                // Strict sphere collision test
                if (hitDist <= u_ObjectRadius) {
                    vec3 sphereNormal = normalize(hitRel);

                    // Directional disk lighting: disk is in equatorial plane inside the orbit
                    vec3 toBlackHole = normalize(vec3(-objPosHit.xy, 0.0));
                    float diskIllumination = max(0.0, dot(sphereNormal, toBlackHole));
                    float rim = pow(1.0 - max(0.0, abs(dot(sphereNormal, normalize(prevPos - pos)))), 3.0);

                    // Surface coloration: Dark oceanic basalt world with incandescent disk bounce
                    vec3 oceanicBase = vec3(0.08, 0.11, 0.14);
                    vec3 diskBounceColor = vec3(2.1, 1.4, 0.6) * diskIllumination * 3.5;
                    vec3 atmosphericRim = vec3(0.4, 0.6, 0.9) * rim * 1.2;
                    vec3 planetColor = oceanicBase + diskBounceColor + atmosphericRim;

                    // Physical Solid Occlusion: planet absorbs all light behind it
                    accumDiskRadiance += diskTransmittance * planetColor;
                    diskTransmittance = 0.0;
                    rayState = 4; // OBJECT HIT
                    break;
                }
            }
        }

        // Volumetric Multi-Crossing Flared 3D Disk Slab Traversal (Opaque Direction A)
        if (u_EnableDisk == 1 && prevPos.z * pos.z <= 0.0 && prevPos.z != pos.z && diskCrossings < 4) {
            float tau = clamp(-prevPos.z / (pos.z - prevPos.z), 0.0, 1.0);
            vec3 hitPos = mix(prevPos, pos, tau);
            float rho2 = hitPos.x * hitPos.x + hitPos.y * hitPos.y;
            float a2_kerr = u_Spin * u_Spin;
            float rHit = sqrt(max(0.0, rho2 - a2_kerr));

            if (rHit >= u_DiskInnerRadius && rHit <= u_DiskOuterRadius) {
                diskCrossings++;
                if (diskCrossings == 1) {
                    primaryHitRadius = rHit;
                    primaryHitAzimuth = atan(hitPos.y, hitPos.x);
#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
                    gargantuaSemanticDiskHitAzimuth = primaryHitAzimuth;
#endif
                }

                // 1. Flared 3D Scale Height H(r)
                float h0 = 0.075 * u_Mass;
                float rNormScale = rHit / max(1.0e-5, u_DiskInnerRadius);
                float H_r = h0 * pow(rNormScale, 1.20);

                // 2. Physical path length traversal through the flared slab
                vec3 rayStepDir = normalize(pos - prevPos);
                float cosIncidence = max(abs(rayStepDir.z), 0.065);
                float fullPathLength = (2.0 * H_r) / cosIncidence;

                // 3. Relativistic kinematics & Doppler factor
                vec3 hitP = mix(prevP, p_spatial, tau);
                float omega = sqrt(u_Mass) / (pow(rHit, 1.5) + u_Spin * sqrt(u_Mass));
                float a2 = u_Spin * u_Spin;
                float denom_v = rHit * rHit + a2;
                float lx = (rHit * hitPos.x + u_Spin * hitPos.y) / denom_v;
                float ly = (rHit * hitPos.y - u_Spin * hitPos.x) / denom_v;
                float H_metric = u_Mass / max(1.0e-6, rHit);

                float l_dot_u = 1.0 + omega * (ly * hitPos.x - lx * hitPos.y);
                float eta_u_u = -1.0 + (omega * omega) * (hitPos.x * hitPos.x + hitPos.y * hitPos.y);
                float denomContract = eta_u_u + 2.0 * H_metric * (l_dot_u * l_dot_u);
                float u0 = (denomContract < 0.0) ? 1.0 / sqrt(-denomContract) : 1.0;

                float lz = hitPos.x * hitP.y - hitPos.y * hitP.x;
                float denomG = u0 * (1.0 + omega * lz);
                float uObs0 = 1.0 / sqrt(max(1.0e-6, -g[0][0]));
                float gShift = (abs(denomG) > 1.0e-6) ? clamp(uObs0 / denomG, 0.05, 5.0) : 1.0;

                // 4. Emissivity profile & Unit Test Assertion Anchors
                float rRatio = u_DiskInnerRadius / rHit;
                float F = (u_Mass / (rHit * rHit * rHit)) * max(0.0, 1.0 - sqrt(rRatio));
                float rPeak = 1.361111 * u_DiskInnerRadius;
                float fPeak = u_Mass / (7.0 * rPeak * rPeak * rPeak);
                float fNorm = (fPeak > 1.0e-7) ? clamp(F / fPeak, 0.0, 1.0) : 0.0;

                float g2 = gShift * gShift;
                float g4 = g2 * g2;
                float iPhys = g4 * fNorm;
                float radiance = iPhys;

                // 5. Direction A Thermal Shading
                vec3 crossingColor = evaluate4TierBlackbodySpectrum(fNorm, gShift);
                float phiHit = atan(hitPos.y, hitPos.x);

                // 6. Multi-Stratum Volumetric Integration:
                // High optical density multiplier (2.2) guarantees full midplane opacity.
                // The equatorial ribbon solidly blocks the rear disk and shadow.
                float stratumZetas[3] = float[3](-0.55, 0.0, 0.55);
                float stratumWeights[3] = float[3](0.28, 0.44, 0.28);
                float slabStepTau = fullPathLength * 2.20;

                for (int s = 0; s < 3; s++) {
                    float optDensity = evaluate3DVolumetricGasDensity(rHit, phiHit, stratumZetas[s], fNorm, u_DiskInnerRadius, u_DiskOuterRadius);
                    float segAlpha = 1.0 - exp(-optDensity * slabStepTau * stratumWeights[s]);
                    accumDiskRadiance += diskTransmittance * crossingColor * segAlpha;
                    diskTransmittance *= (1.0 - segAlpha);
                    if (diskTransmittance < 0.008) {
                        diskTransmittance = 0.0;
                        break;
                    }
                }

                if (diskTransmittance == 0.0) {
                    rayState = 3; // Fully opaque disk hit
                    break;
                }
            }
        }
    }

    // Asymptotic Relativistic Critical Curve Caustic Flux (Continuous Photon Ring)
    if (rayState != 1 && minR > rCapture && minR < (rCapture + 0.45)) {
        float proximity = 1.0 - smoothstep(rCapture, rCapture + 0.45, minR);
        // Ultra-fine primary ring (power 22) + secondary caustic arc (power 5)
        float primaryRing = pow(proximity, 22.0) * 9.5;
        float secondaryArc = pow(proximity, 5.0) * 2.4;
        vec3 causticFluxColor = vec3(16.0, 14.8, 12.5) * (primaryRing + secondaryArc);
        accumDiskRadiance += diskTransmittance * causticFluxColor;
    }

    // Robust Horizon Resolution: eliminates jagged bitten teeth
    if (rayState == 0) {
        if (minR <= (rCapture + 0.25) || (prevR <= 3.2 && length(p_spatial) < 1.5)) {
            rayState = 1; // Pure Horizon Shadow
        } else {
            rayState = 2; // Escaped Sky
        }
    }

    outState = rayState;
    outMinR = minR;
    outCrossings = diskCrossings;
    outHitR = primaryHitRadius;
#ifdef GARGANTUA_WORKLOAD_TELEMETRY
    outStepsTaken = stepsTaken;
#endif

    // Final Radiance Composite
    if (rayState == 1) { // Captured by Horizon
        if (accumDiskRadiance.r + accumDiskRadiance.g + accumDiskRadiance.b > 0.005) {
            // Emissive gas in front of the black hole
            return vec4(accumDiskRadiance, 1.0);
        } else {
            // Pure un-occluded black hole shadow
            return vec4(0.0, 0.0, 0.0, 0.0);
        }
    } else if (rayState == 2) { // Escaped Sky
        vec3 skyDir = normalize(p_spatial);
        if (dot(skyDir, skyDir) < 0.5) {
            skyDir = normalize(vec3(0.0, 0.0, 1.0));
        }
#if defined(GARGANTUA_WORKLOAD_SEMANTIC_CACHE) || defined(GARGANTUA_ANIMATION_SEMANTIC_CACHE)
        gargantuaSemanticDeflectedDir = skyDir;
#endif
        vec3 sky = sample_procedural_sky(skyDir);
        float skyScale = 0.85;
        vec3 scaledSky = clamp(sky * skyScale, vec3(0.0), vec3(0.45));
        vec3 compositeSky = accumDiskRadiance + diskTransmittance * scaledSky;
        return vec4(compositeSky, 1.0);
    } else if (rayState == 3) { // Opaque Disk Hit
        return vec4(accumDiskRadiance, 1.0);
    } else if (rayState == 4) { // Relativistic Object
        return vec4(objectColor, 1.0);
    } else { // Unresolved / Timeout
        if (accumDiskRadiance.r + accumDiskRadiance.g + accumDiskRadiance.b > 0.005) {
            return vec4(accumDiskRadiance, 1.0);
        } else {
            return vec4(0.0, 0.0, 0.0, 0.5);
        }
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
        // BUGFIX: traceRaySample() already correctly encodes foreground disk gas
        // radiance (alpha=1.0) vs. genuine unobstructed shadow (alpha=0.0) in
        // baseSample. Previously this branch unconditionally discarded baseSample,
        // clipping legitimate foreground emission and creating a false flat notch
        // in the shadow's silhouette wherever the near disk edge crosses in front
        // of the horizon. Respect the alpha baseSample already computed.
        if (baseSample.a > 0.5) {
            fragColor = baseSample;
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        }
    } else if (rayState == 2) {
        // Escaped: lensed starfield background, alpha 1.0 preserved
        fragColor = baseSample;
    } else if (rayState == 3) {
        fragColor = baseSample;
    } else if (rayState == 4) {
        fragColor = baseSample;
    } else {
        // Unresolved branch: baseSample contains vec4(0.0, 0.0, 0.0, 0.5);
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
    if (baseState == 3 || baseCrossings > 0) {
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
            3.0
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
