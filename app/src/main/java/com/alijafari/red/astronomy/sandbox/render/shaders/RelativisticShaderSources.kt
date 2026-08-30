package com.alijafari.red.astronomy.sandbox.render.shaders

/**
 * GLSL ES 3.00 Shader Sources for Physically Grounded Relativistic Black Hole & Wormhole Rendering (Phase 5).
 *
 * Implements:
 * 1. General Relativistic Null Geodesic Ray Marching (Schwarzschild & Kerr Spacetimes).
 * 2. Event Horizon & Black Hole Shadow ($b_{crit} = \sqrt{27}/2 r_s \approx 2.598 r_s$).
 * 3. Relativistic Accretion Disk with:
 *    - Innermost Stable Circular Orbit (ISCO).
 *    - Novikov-Thorne temperature gradient.
 *    - Relativistic Doppler boosting & beaming ($\delta^4$ intensity scaling).
 *    - Gravitational redshift ($g = \sqrt{1 - r_s/r}$).
 *    - Primary and secondary (lensed top/bottom) Einstein disk images.
 * 4. Kerr Frame Dragging (Lense-Thirring effect) creating asymmetric shadow & photon regions.
 * 5. Gravitational Lensing of celestial starfield background.
 * 6. Theoretical Morris-Thorne / Ellis Traversible Wormhole Portal with inter-universal throat traversal.
 */
object RelativisticShaderSources {

    // =============================================================================================
    // COMMON RELATIVISTIC GLSL UTILITIES
    // =============================================================================================
    const val RELATIVISTIC_NOISE_UTILITY = """
// Fast 3D Simplex & FBM noise for accretion plasma & cosmic dust
vec4 mod289_r(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 mod289_r(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute_r(vec4 x) { return mod289_r(((x * 34.0) + 1.0) * x); }
vec4 taylorInvSqrt_r(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

float snoise_r(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i  = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    i = mod289_r(i);
    vec4 p = permute_r(permute_r(permute_r(
                i.z + vec4(0.0, i1.z, i2.z, 1.0))
            + i.y + vec4(0.0, i1.y, i2.y, 1.0))
            + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    vec4 norm = taylorInvSqrt_r(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float fbm_plasma(vec3 p) {
    float v = 0.0;
    float a = 0.55;
    vec3 shift = vec3(100.0);
    for (int i = 0; i < 3; ++i) {
        v += a * snoise_r(p);
        p = p * 2.05 + shift;
        a *= 0.5;
    }
    return v;
}

// Physically motivated blackbody temperature to RGB conversion
vec3 temperatureToRGB(float kelvin) {
    float T = clamp(kelvin / 100.0, 10.0, 400.0);
    float r, g, b;

    if (T <= 66.0) {
        r = 1.0;
        g = clamp(99.4708025861 * log(T) - 161.1195681661, 0.0, 255.0) / 255.0;
        if (T <= 19.0) {
            b = 0.0;
        } else {
            b = clamp(138.5177312231 * log(T - 10.0) - 305.0447927307, 0.0, 255.0) / 255.0;
        }
    } else {
        r = clamp(329.698727446 * pow(T - 60.0, -0.1332047592), 0.0, 255.0) / 255.0;
        g = clamp(288.1221695283 * pow(T - 60.0, -0.0755148492), 0.0, 255.0) / 255.0;
        b = 1.0;
    }
    return vec3(r, g, b);
}

// Procedural high-resolution celestial starry sky background
vec3 sampleCosmicBackground(vec3 dir) {
    vec3 d = normalize(dir);
    
    // Milky Way galactic plane band
    float galacticPlane = abs(d.y);
    float mwDust = exp(-galacticPlane * 5.0) * (0.5 + 0.5 * fbm_plasma(d * 4.0));
    vec3 mwColor = vec3(0.08, 0.06, 0.12) + vec3(0.25, 0.18, 0.10) * mwDust;

    // Distant star sparkles
    vec3 starCoord = d * 180.0;
    float starNoise = snoise_r(starCoord);
    float star = smoothstep(0.78, 0.98, starNoise);
    float starColorNoise = snoise_r(starCoord * 0.2 + vec3(50.0));
    vec3 starTint = mix(vec3(0.6, 0.8, 1.0), vec3(1.0, 0.7, 0.4), starColorNoise * 0.5 + 0.5);

    return mwColor + star * starTint * 2.2;
}

// Alternate universe background (for theoretical wormhole)
vec3 sampleAlternateUniverseBackground(vec3 dir) {
    vec3 d = normalize(dir);
    
    // Exotic cyan/magenta hyper-nebula
    float nebulaA = fbm_plasma(d * 2.5 + vec3(12.3, 45.6, 78.9));
    float nebulaB = fbm_plasma(d * 5.0 + vec3(98.7, 65.4, 32.1));
    
    vec3 colA = vec3(0.02, 0.25, 0.45) * (nebulaA * 0.5 + 0.5);
    vec3 colB = vec3(0.40, 0.05, 0.35) * (nebulaB * 0.5 + 0.5);
    
    // Dense stellar cluster sparkles
    vec3 sc = d * 220.0;
    float st = smoothstep(0.75, 0.96, snoise_r(sc));
    vec3 starTint = vec3(0.4, 0.9, 1.0);
    
    return colA + colB + st * starTint * 2.8;
}
"""

    // =============================================================================================
    // 1. RELATIVISTIC BLACK HOLE RAYMARCHER (Schwarzschild & Kerr Metric)
    // =============================================================================================
    const val BLACK_HOLE_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position; // Unit cube / proxy bounding mesh vertices

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;

out vec3 v_WorldPos;

void main() {
    vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
    v_WorldPos = worldPos.xyz;
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val BLACK_HOLE_FRAGMENT_SHADER = """#version 300 es
precision highp float;

$RELATIVISTIC_NOISE_UTILITY

in vec3 v_WorldPos;

uniform vec3 u_CameraPosition;
uniform vec3 u_BlackHoleCenter;
uniform float u_SchwarzschildRadius; // r_s = 2GM / c^2 (in render units)
uniform float u_SpinParameter;       // a/M in [-0.998, 0.998] (Kerr metric rotation)
uniform vec3 u_SpinAxis;             // Normalized spin axis vector (default (0, 1, 0))
uniform float u_DiskInnerRadius;     // r_ISCO factor * r_s
uniform float u_DiskOuterRadius;     // outer disk radius
uniform float u_DiskBaseTemperature; // Kelvin (e.g. 18000 K)
uniform float u_DiskDensityScale;    // Opacity density multiplier
uniform float u_SimTime;             // Simulation time in seconds
uniform float u_QualityTier;         // 0.0 = Low (24 steps), 1.0 = Medium (48 steps), 2.0 = High (72 steps)

out vec4 fragColor;

void main() {
    // 1. Setup Camera Ray in World Space
    vec3 rayOrigin = u_CameraPosition;
    vec3 rayDir = normalize(v_WorldPos - u_CameraPosition);

    // Coordinate system centered on black hole
    vec3 bhPos = u_BlackHoleCenter;
    vec3 relPos = rayOrigin - bhPos;
    float initialDist = length(relPos);

    float rs = max(u_SchwarzschildRadius, 0.05);
    float a = clamp(u_SpinParameter, -0.998, 0.998);
    vec3 spinAxis = normalize(u_SpinAxis);

    // Horizon radius for Kerr: r_+ = M + sqrt(M^2 - a^2) where M = 0.5 * rs
    float M = 0.5 * rs;
    float rH = M * (1.0 + sqrt(max(1.0 - a * a, 0.001)));
    float rPhoton = 1.5 * rs; // Photon sphere nominal
    float rISCO = max(u_DiskInnerRadius, 1.5 * rs);
    float rOuter = max(u_DiskOuterRadius, 6.0 * rs);

    // Number of RK/Euler integration steps based on quality tier
    int maxSteps = 48;
    if (u_QualityTier < 0.5) {
        maxSteps = 24;
    } else if (u_QualityTier > 1.5) {
        maxSteps = 72;
    }

    vec3 currentPos = relPos;
    vec3 currentVel = rayDir; // Normalized speed of light = 1

    vec3 accumDiskRadiance = vec3(0.0);
    float rayTransmittance = 1.0;
    bool hitHorizon = false;

    // Step size based on distance and Schwarzschild radius
    float maxDistance = max(initialDist * 2.5, 30.0 * rs);
    float dtBase = (maxDistance / float(maxSteps)) * 0.75;

    vec3 prevPos = currentPos;

    for (int step = 0; step < 72; ++step) {
        if (step >= maxSteps) break;

        float r = length(currentPos);

        // 1. Check Event Horizon Capture
        if (r <= rH * 1.02) {
            hitHorizon = true;
            break;
        }

        // 2. Check if ray escaped to asymptotic infinity
        if (r > maxDistance && step > 5) {
            break;
        }

        // Adaptive step size: smaller near photon sphere & horizon, larger far away
        float rRatio = r / rs;
        float dt = dtBase * clamp(rRatio * 0.35, 0.08, 2.5);

        // 3. Relativistic Geodesic Acceleration (General Relativity Deflection)
        // a_geo = -1.5 * rs / r^5 * |r x v|^2 * r + Kerr Frame Dragging torque
        vec3 L = cross(currentPos, currentVel);
        float L2 = dot(L, L);
        
        vec3 accel = -(1.5 * rs * L2 / (r * r * r * r * r)) * currentPos;

        // Kerr Frame Dragging (Lense-Thirring effect)
        if (abs(a) > 0.01) {
            vec3 aVec = spinAxis * (a * M);
            vec3 frameDrag = (2.0 / (r * r * r * r)) * cross(cross(currentPos, aVec), currentVel);
            accel += frameDrag;
        }

        // Integrate Geodesic (Verlet / Euler step)
        prevPos = currentPos;
        currentPos += currentVel * dt + 0.5 * accel * dt * dt;
        currentVel = normalize(currentVel + accel * dt);

        // 4. Accretion Disk Equatorial Plane Crossing Check
        // Disk lies in plane perpendicular to spinAxis: dot(pos, spinAxis) = 0
        float dPrev = dot(prevPos, spinAxis);
        float dCurr = dot(currentPos, spinAxis);

        if ((dPrev * dCurr <= 0.0) && abs(dPrev - dCurr) > 1e-6) {
            // Compute exact crossing fraction
            float tCross = clamp(-dPrev / (dCurr - dPrev), 0.0, 1.0);
            vec3 crossPos = mix(prevPos, currentPos, tCross);
            float rCross = length(crossPos);

            if (rCross >= rISCO && rCross <= rOuter) {
                // Accretion Disk Physics:
                // a. Keplerian Orbital Velocity in local disk plane
                vec3 diskTangential = normalize(cross(spinAxis, crossPos));
                // Keplerian beta = v/c
                float beta = clamp(sqrt(0.5 * rs / rCross), 0.0, 0.65);
                vec3 vDisk = diskTangential * beta;

                // b. Relativistic Doppler Beaming Factor: delta = sqrt(1-beta^2) / (1 - beta * (rayDir . diskTangential))
                vec3 localRayDir = normalize(currentVel);
                float cosTheta = dot(localRayDir, diskTangential);
                float gamma = 1.0 / sqrt(max(1.0 - beta * beta, 0.01));
                float delta = 1.0 / (gamma * max(1.0 - beta * cosTheta, 0.05));

                // c. Gravitational Redshift Factor: g_grav = sqrt(1 - rs / rCross)
                float gGrav = sqrt(max(1.0 - rs / rCross, 0.02));
                float totalShift = clamp(delta * gGrav, 0.15, 4.5);

                // d. Novikov-Thorne Temperature Profile
                // T(r) ~ T_base * (r_ISCO / r)^(3/4) * (1 - sqrt(r_ISCO / r))^(1/4)
                float rNorm = rCross / rISCO;
                float tempProfile = pow(1.0 / rNorm, 0.75) * pow(max(1.0 - sqrt(1.0 / rNorm), 0.01), 0.25);
                float localTemp = u_DiskBaseTemperature * tempProfile * totalShift;

                vec3 blackbodyCol = temperatureToRGB(localTemp);

                // e. Relativistic Doppler Beaming Radiance Scaling: I = I_0 * delta^4
                float beaming = pow(totalShift, 3.8);

                // f. Procedural turbulent spiral plasma structure
                float angle = atan(crossPos.z, crossPos.x);
                float spiralCoord = angle * 2.0 - log(rCross / rs) * 3.5 + u_SimTime * (0.8 * beta);
                float plasmaNoise = fbm_plasma(vec3(rCross * 2.0, spiralCoord, u_SimTime * 0.2));
                float density = smoothstep(0.2, 0.8, plasmaNoise * 0.5 + 0.5);

                // Edge feathering
                float innerFade = smoothstep(rISCO, rISCO * 1.15, rCross);
                float outerFade = smoothstep(rOuter, rOuter * 0.85, rCross);
                float diskAlpha = density * innerFade * outerFade * 0.85 * u_DiskDensityScale;

                vec3 diskRadiance = blackbodyCol * beaming * (1.2 + density * 1.8);

                // Volumetric alpha blending
                accumDiskRadiance += rayTransmittance * diskRadiance * diskAlpha;
                rayTransmittance *= (1.0 - diskAlpha);

                if (rayTransmittance < 0.02) break;
            }
        }
    }

    // 5. Final Composition: Horizon Shadow, Accretion Disk & Lensed Starfield
    if (hitHorizon) {
        // Deep Black Event Horizon Shadow with subtle blue/violet Hawking-like photon ring edge
        float edgeFactor = smoothstep(rH, rH * 1.06, length(currentPos));
        vec3 photonRingGlow = vec3(0.35, 0.45, 1.0) * pow(1.0 - edgeFactor, 3.0) * 0.6;
        
        fragColor = vec4(accumDiskRadiance + photonRingGlow, 1.0);
    } else {
        // Lensed Background Starfield in deflected ray direction
        vec3 lensedBackground = sampleCosmicBackground(currentVel);
        
        // Gravitational lens flare / Einstein ring boost
        float minRadiusReached = length(currentPos);
        float einsteinGlow = exp(-max(minRadiusReached - rPhoton, 0.0) / (0.8 * rs)) * 0.4;
        vec3 ringGlow = vec3(0.9, 0.8, 0.6) * einsteinGlow;

        vec3 finalColor = accumDiskRadiance + rayTransmittance * (lensedBackground + ringGlow);
        fragColor = vec4(finalColor, 1.0);
    }
}
"""

    // =============================================================================================
    // 2. THEORETICAL TRAVERSIBLE WORMHOLE RAYMARCHER (Morris-Thorne / Ellis Spacetime)
    // =============================================================================================
    const val WORMHOLE_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;

out vec3 v_WorldPos;

void main() {
    vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
    v_WorldPos = worldPos.xyz;
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val WORMHOLE_FRAGMENT_SHADER = """#version 300 es
precision highp float;

$RELATIVISTIC_NOISE_UTILITY

in vec3 v_WorldPos;

uniform vec3 u_CameraPosition;
uniform vec3 u_WormholeCenter;
uniform float u_ThroatRadius;      // Throat radius b_0 (in render units)
uniform float u_ThroatLength;      // L (separation parameter)
uniform float u_SimTime;           // Simulation time
uniform float u_QualityTier;       // 0.0=Low, 1.0=Med, 2.0=High

out vec4 fragColor;

void main() {
    vec3 rayOrigin = u_CameraPosition;
    vec3 rayDir = normalize(v_WorldPos - u_CameraPosition);
    vec3 whCenter = u_WormholeCenter;

    vec3 relPos = rayOrigin - whCenter;
    float initialDist = length(relPos);

    float b0 = max(u_ThroatRadius, 0.2);
    float b0Sq = b0 * b0;

    int maxSteps = 44;
    if (u_QualityTier < 0.5) maxSteps = 22;
    else if (u_QualityTier > 1.5) maxSteps = 64;

    // Morris-Thorne / Ellis Drainhole coordinate l: r^2 = l^2 + b_0^2
    // l > 0 is Universe A (Our Universe), l < 0 is Universe B (Exotic Parallel Universe)
    float l = sqrt(max(initialDist * initialDist - b0Sq, 0.01));
    vec3 rayPos = relPos;
    vec3 rayVel = rayDir;

    float dt = (initialDist * 2.2 / float(maxSteps)) * 0.85;
    bool traversedToUniverseB = false;

    for (int step = 0; step < 64; ++step) {
        if (step >= maxSteps) break;

        float r = length(rayPos);
        if (r < 0.001) break;

        // Ellis wormhole effective deflection toward throat:
        // Acceleration a_wh = - (b0^2 / r^4) * rayPos
        vec3 accel = -(b0Sq / (r * r * r * r)) * rayPos;

        rayPos += rayVel * dt + 0.5 * accel * dt * dt;
        rayVel = normalize(rayVel + accel * dt);

        // Check if ray passes through throat sphere r <= b_0
        if (length(rayPos) <= b0 * 1.01) {
            traversedToUniverseB = true;
            // Ray traverses through throat into Universe B, exiting with inverted radial component
            rayPos = normalize(rayPos) * (b0 * 1.05);
            break;
        }

        if (r > initialDist * 2.0 && step > 8) {
            break;
        }
    }

    // Throat Rim Lensing / Chromatic Einstein Ring
    float impactParam = length(cross(relPos, rayDir));
    float throatDistanceRatio = abs(impactParam - b0) / b0;
    float rimGlow = exp(-throatDistanceRatio * 8.0);
    vec3 chromaticRim = vec3(0.0, 0.9, 1.0) * rimGlow * 1.8;

    if (traversedToUniverseB) {
        // Ray passed through throat -> Render Universe B (Alternate Parallel Galaxy)
        vec3 universeBView = sampleAlternateUniverseBackground(rayVel);
        fragColor = vec4(universeBView + chromaticRim, 1.0);
    } else {
        // Ray deflected back into Universe A -> Render gravitationally lensed Milky Way
        vec3 universeAView = sampleCosmicBackground(rayVel);
        fragColor = vec4(universeAView + chromaticRim, 1.0);
    }
}
"""
}
