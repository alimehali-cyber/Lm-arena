package com.alijafari.red.astronomy.sandbox.render.shaders

/**
 * GLSL ES 3.00 Shader Sources for Research-Driven Celestial Rendering (Phase 3).
 *
 * Implements physics-motivated procedural planet generation, atmospheric Rayleigh/Mie
 * scattering shells, multi-layered cloud decks, Saturn ring systems with mutual shadows,
 * solar granulation/corona shaders, and ACES filmic tone mapping.
 */
object CelestialShaderSources {

    // =============================================================================================
    // COMMON GLSL NOISE & UTILITIES (Included in Celestial Shaders)
    // =============================================================================================
    const val GLSL_NOISE_UTILITY = """
// 3D Permutation polynomial hash for high-performance GLES
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

// 3D Simplex noise
float snoise(vec3 v) {
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

    i = mod289(i);
    vec4 p = permute(permute(permute(
                i.z + vec4(0.0, i1.z, i2.z, 1.0))
            + i.y + vec4(0.0, i1.y, i2.y, 1.0))
            + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    float n_ = 0.142857142857; // 1.0/7.0
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

    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

// 4-Octave Fractional Brownian Motion
float fbm(vec3 p) {
    float total = 0.0;
    float amp = 0.5;
    float freq = 1.0;
    for (int i = 0; i < 4; i++) {
        total += snoise(p * freq) * amp;
        freq *= 2.02;
        amp *= 0.5;
    }
    return total;
}
"""

    // =============================================================================================
    // 1. CELESTIAL PLANET SHADER (Surface, Oceans, Craters, Bands, Storms)
    // =============================================================================================
    const val PLANET_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoord;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;
uniform mat3 u_NormalMatrix;
uniform mat4 u_VisualOrientationMatrix;

out vec3 v_WorldPosition;
out vec3 v_Normal;
out vec2 v_TexCoord;
out vec3 v_LocalPosition;

void main() {
    v_LocalPosition = a_Position;
    vec4 rotatedPos = u_VisualOrientationMatrix * vec4(a_Position, 1.0);
    vec4 worldPos = u_ModelMatrix * rotatedPos;
    v_WorldPosition = worldPos.xyz;
    
    vec3 rotatedNormal = (u_VisualOrientationMatrix * vec4(a_Normal, 0.0)).xyz;
    v_Normal = normalize(u_NormalMatrix * rotatedNormal);
    v_TexCoord = a_TexCoord;
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val PLANET_FRAGMENT_SHADER = """#version 300 es
precision highp float;

$GLSL_NOISE_UTILITY

in vec3 v_WorldPosition;
in vec3 v_Normal;
in vec2 v_TexCoord;
in vec3 v_LocalPosition;

uniform int u_BodyType;          // 0=Sun, 1=Mercury, 2=Venus, 3=Earth, 4=Moon, 5=Mars, 6=Jupiter, 7=Saturn, 8=Uranus, 9=Neptune, 10=Other
uniform vec4 u_BaseColor;
uniform vec3 u_LightPosition;
uniform vec3 u_CameraPosition;
uniform float u_SpecularIntensity;
uniform float u_Shininess;
uniform float u_Roughness;
uniform float u_AmbientIntensity;
uniform float u_SimTime;
uniform float u_QualityTier;     // 0=Low, 1=Med, 2=High
uniform float u_HasRingShadow;   // 1.0 for Saturn
uniform float u_RingInnerRadius;
uniform float u_RingOuterRadius;

out vec4 fragColor;

void main() {
    vec3 N = normalize(v_Normal);
    vec3 L = normalize(u_LightPosition - v_WorldPosition);
    vec3 V = normalize(u_CameraPosition - v_WorldPosition);
    vec3 H = normalize(L + V);
    vec3 P = v_LocalPosition;

    float NdotL = dot(N, L);
    float NdotV = max(dot(N, V), 0.0);
    float NdotH = max(dot(N, H), 0.0);

    // =========================================================================
    // TYPE 0: SUN / STELLAR BODY (Convective Granulation & Limb Darkening)
    // =========================================================================
    if (u_BodyType == 0) {
        float timeScale = u_SimTime * 0.05;
        float gran1 = snoise(P * 14.0 + vec3(timeScale * 0.3, 0.0, timeScale * 0.2));
        float gran2 = snoise(P * 28.0 - vec3(0.0, timeScale * 0.5, 0.0)) * 0.5;
        float granulation = (gran1 + gran2) * 0.18;

        // Solar limb darkening: I(mu) = 0.35 + 0.65 * mu^0.6
        float mu = pow(NdotV, 0.6);
        float limbDarkening = 0.38 + 0.62 * mu;

        vec3 coreColor = vec3(1.0, 0.95, 0.75);
        vec3 surfaceColor = vec3(1.0, 0.68, 0.15) * (1.0 + granulation);
        vec3 edgeColor = vec3(0.95, 0.28, 0.05);

        vec3 finalSun = mix(edgeColor, mix(surfaceColor, coreColor, mu), limbDarkening);
        fragColor = vec4(finalSun * 1.85, 1.0); // HDR luminous emission
        return;
    }

    // Planetary Surface Albedo & Specular Computation
    vec3 surfaceAlbedo = u_BaseColor.rgb;
    float specMask = u_SpecularIntensity;
    float roughMask = u_Roughness;
    vec3 rimColor = u_BaseColor.rgb;

    // =========================================================================
    // TYPE 3: EARTH (Continents, Oceans, Mountain Ranges, Ocean Glint)
    // =========================================================================
    if (u_BodyType == 3) {
        float elev = fbm(P * 3.2);
        float detail = snoise(P * 12.0) * 0.15;
        float terrain = elev + detail;

        if (terrain < 0.02) {
            // Ocean: Deep navy to shallow continental shelf
            float depth = clamp(-terrain * 4.0, 0.0, 1.0);
            surfaceAlbedo = mix(vec3(0.05, 0.35, 0.65), vec3(0.01, 0.12, 0.32), depth);
            specMask = 0.95; // High specular glint on water
            roughMask = 0.15;
        } else {
            // Landmass: Coastline to verdant land to mountains to snow peaks
            float height = terrain;
            if (height < 0.18) {
                surfaceAlbedo = mix(vec3(0.18, 0.45, 0.15), vec3(0.35, 0.52, 0.20), height / 0.18);
            } else if (height < 0.42) {
                surfaceAlbedo = mix(vec3(0.48, 0.42, 0.25), vec3(0.40, 0.35, 0.30), (height - 0.18) / 0.24);
            } else {
                surfaceAlbedo = vec3(0.85, 0.88, 0.92); // Mountain snow caps
            }
            specMask = 0.02; // Land is matte
            roughMask = 0.85;
        }
        rimColor = vec3(0.35, 0.65, 1.00); // Earth blue atmospheric rim
    }
    // =========================================================================
    // TYPE 4: MOON (Craters, Basaltic Maria, Rough Highland Terrain)
    // =========================================================================
    else if (u_BodyType == 4) {
        float mariaNoise = fbm(P * 2.5);
        float craterNoise = fbm(P * 9.0);
        float microCrater = snoise(P * 22.0) * 0.1;

        float isMare = smoothstep(-0.15, 0.15, mariaNoise);
        vec3 highlandColor = vec3(0.78, 0.77, 0.75) * (0.85 + craterNoise * 0.25 + microCrater);
        vec3 mareColor = vec3(0.38, 0.37, 0.36) * (0.90 + craterNoise * 0.15);

        surfaceAlbedo = mix(mareColor, highlandColor, isMare);
        specMask = 0.03;
        roughMask = 0.95; // Rough lunar regolith
        rimColor = vec3(0.0);
    }
    // =========================================================================
    // TYPE 1: MERCURY (Heavily Cratered Basalt, High Contrast, No Atmosphere)
    // =========================================================================
    else if (u_BodyType == 1) {
        float craterDetail = fbm(P * 7.5) + snoise(P * 18.0) * 0.15;
        float albedoVar = snoise(P * 2.0) * 0.2;
        surfaceAlbedo = vec3(0.62, 0.60, 0.58) * (0.85 + craterDetail * 0.3 + albedoVar);
        specMask = 0.04;
        roughMask = 0.95;
        rimColor = vec3(0.0);
    }
    // =========================================================================
    // TYPE 2: VENUS (Dense Sulfuric Cloud Deck, Super-Rotation Streaks)
    // =========================================================================
    else if (u_BodyType == 2) {
        float lat = P.y;
        float streak = snoise(vec3(P.x * 2.0, P.y * 6.0, P.z * 2.0) + vec3(u_SimTime * 0.08, 0.0, 0.0));
        float haze = fbm(P * 4.0);
        surfaceAlbedo = vec3(0.92, 0.84, 0.58) * (0.92 + streak * 0.08 + haze * 0.05);
        specMask = 0.18;
        roughMask = 0.35;
        rimColor = vec3(0.95, 0.85, 0.60);
    }
    // =========================================================================
    // TYPE 5: MARS (Iron Oxide Dust, Dark Volcanic Basalt, Polar Ice Caps)
    // =========================================================================
    else if (u_BodyType == 5) {
        float darkBasalt = fbm(P * 3.5);
        float dustDetail = snoise(P * 11.0) * 0.1;
        vec3 rustRed = vec3(0.85, 0.38, 0.18);
        vec3 darkVolcanic = vec3(0.38, 0.24, 0.18);

        surfaceAlbedo = mix(darkVolcanic, rustRed, smoothstep(-0.2, 0.3, darkBasalt)) + dustDetail;

        // Polar ice caps (North & South)
        float polarDistance = abs(P.y);
        if (polarDistance > 0.82) {
            float iceBorder = smoothstep(0.82, 0.88, polarDistance + snoise(P * 14.0) * 0.04);
            surfaceAlbedo = mix(surfaceAlbedo, vec3(0.92, 0.94, 0.98), iceBorder);
            specMask = mix(specMask, 0.45, iceBorder);
        }
        rimColor = vec3(0.85, 0.55, 0.40);
    }
    // =========================================================================
    // TYPE 6: JUPITER (Turbulent Belts/Zones, Domain Warping, Great Red Spot)
    // =========================================================================
    else if (u_BodyType == 6) {
        // Latitudinal band distortion via domain warping
        float warp = snoise(P * 3.5 + vec3(u_SimTime * 0.02, 0.0, 0.0)) * 0.12;
        float yCoord = P.y + warp;
        float bands = sin(yCoord * 22.0) * 0.5 + 0.5;
        float turbulentDetail = fbm(P * 8.0) * 0.2;

        vec3 beltColor = vec3(0.68, 0.42, 0.25); // Dark brown-red belt
        vec3 zoneColor = vec3(0.92, 0.85, 0.72); // Creamy white-tan zone
        surfaceAlbedo = mix(beltColor, zoneColor, bands) + turbulentDetail;

        // Great Red Spot (Southern Tropical Zone around y ~ -0.32)
        vec2 stormCenter = vec2(0.0, -0.32); // In rotating local space
        vec2 stormDelta = vec2((P.x - stormCenter.x) * 1.5, P.y - stormCenter.y);
        float stormDist = length(stormDelta);
        if (stormDist < 0.22) {
            float stormEdge = smoothstep(0.22, 0.08, stormDist);
            float stormSwirl = snoise(vec3(stormDelta * 12.0, u_SimTime * 0.1));
            vec3 grsColor = vec3(0.82, 0.25, 0.12) * (0.85 + stormSwirl * 0.25);
            surfaceAlbedo = mix(surfaceAlbedo, grsColor, stormEdge);
        }
        specMask = 0.10;
        roughMask = 0.45;
        rimColor = vec3(0.88, 0.75, 0.58);
    }
    // =========================================================================
    // TYPE 7: SATURN (Soft Golden Banding & Ring-To-Planet Shadow)
    // =========================================================================
    else if (u_BodyType == 7) {
        float bandWarp = snoise(P * 2.5) * 0.06;
        float satBands = sin((P.y + bandWarp) * 18.0) * 0.5 + 0.5;
        vec3 satGold1 = vec3(0.92, 0.84, 0.60);
        vec3 satGold2 = vec3(0.82, 0.72, 0.48);
        surfaceAlbedo = mix(satGold2, satGold1, satBands);
        specMask = 0.12;
        roughMask = 0.45;
        rimColor = vec3(0.92, 0.85, 0.65);
    }
    // =========================================================================
    // TYPE 8: URANUS (Pale Cyan, Subtle Methane Absorption)
    // =========================================================================
    else if (u_BodyType == 8) {
        float subtleBands = sin(P.y * 10.0) * 0.04;
        surfaceAlbedo = vec3(0.52, 0.85, 0.88) + subtleBands;
        specMask = 0.25;
        roughMask = 0.30;
        rimColor = vec3(0.48, 0.88, 0.92);
    }
    // =========================================================================
    // TYPE 9: NEPTUNE (Deep Azure, Methane Absorption, White Cirrus Streaks)
    // =========================================================================
    else if (u_BodyType == 9) {
        float band = sin(P.y * 12.0) * 0.06;
        vec3 deepBlue = vec3(0.15, 0.45, 0.92) + band;

        // Bright white high-altitude methane cirrus clouds
        float cirrus = snoise(vec3(P.x * 4.0, P.y * 14.0, P.z * 4.0) + vec3(u_SimTime * 0.04, 0.0, 0.0));
        if (cirrus > 0.45) {
            float cirrusAlpha = smoothstep(0.45, 0.70, cirrus);
            deepBlue = mix(deepBlue, vec3(0.85, 0.92, 1.0), cirrusAlpha * 0.75);
        }
        surfaceAlbedo = deepBlue;
        specMask = 0.30;
        roughMask = 0.30;
        rimColor = vec3(0.20, 0.55, 0.98);
    }

    // =========================================================================
    // SATURN RING-TO-PLANET SHADOW CALCULATION
    // =========================================================================
    float ringShadow = 1.0;
    if (u_HasRingShadow > 0.5) {
        // Trace ray from surface point towards light L to intersect equatorial plane Y=0
        if (abs(L.y) > 1e-4) {
            float t = -P.y / L.y;
            if (t > 0.0) { // Light is in front of the equatorial plane relative to P
                vec3 hitPlane = P + L * t;
                float rDist = length(hitPlane.xz);
                if (rDist >= u_RingInnerRadius && rDist <= u_RingOuterRadius) {
                    // Inside the ring shadow path
                    float cassiniGap = smoothstep(1.92, 1.96, rDist) * (1.0 - smoothstep(2.00, 2.04, rDist));
                    float density = 0.85 - cassiniGap * 0.75;
                    ringShadow = 1.0 - density;
                }
            }
        }
    }

    // =========================================================================
    // ILLUMINATION COMPOSITION (Lambert Diffuse + Blinn-Phong + Limb Fresnel)
    // =========================================================================
    float diffuseFactor = max(NdotL, 0.0) * ringShadow;
    vec3 diffuse = surfaceAlbedo * diffuseFactor;

    // Specular highlight (water glint, ice, atmosphere reflection)
    float specularFactor = pow(NdotH, max(u_Shininess, 1.0)) * specMask * (NdotL > 0.0 ? 1.0 : 0.0) * ringShadow;
    vec3 specular = vec3(1.0, 0.95, 0.9) * specularFactor;

    // Ambient baseline
    vec3 ambient = surfaceAlbedo * u_AmbientIntensity;

    // Atmospheric limb scattering highlight
    float fresnel = pow(1.0 - NdotV, 3.2) * (1.0 - roughMask * 0.5);
    vec3 rim = rimColor * fresnel * max(NdotL + 0.25, 0.0) * 0.35;

    vec3 finalColor = ambient + diffuse + specular + rim;
    fragColor = vec4(finalColor, u_BaseColor.a);
}
"""

    // =============================================================================================
    // 2. EARTH CLOUD SHELL SHADER (Concentric Shell at R * 1.012, Independent Rotation)
    // =============================================================================================
    const val CLOUD_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoord;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;
uniform mat3 u_NormalMatrix;
uniform mat4 u_VisualOrientationMatrix;
uniform float u_CloudScale;

out vec3 v_WorldPosition;
out vec3 v_Normal;
out vec2 v_TexCoord;
out vec3 v_LocalPosition;

void main() {
    v_LocalPosition = a_Position;
    vec3 scaledPos = a_Position * u_CloudScale;
    vec4 rotatedPos = u_VisualOrientationMatrix * vec4(scaledPos, 1.0);
    vec4 worldPos = u_ModelMatrix * rotatedPos;
    v_WorldPosition = worldPos.xyz;

    vec3 rotatedNormal = (u_VisualOrientationMatrix * vec4(a_Normal, 0.0)).xyz;
    v_Normal = normalize(u_NormalMatrix * rotatedNormal);
    v_TexCoord = a_TexCoord;
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val CLOUD_FRAGMENT_SHADER = """#version 300 es
precision highp float;

$GLSL_NOISE_UTILITY

in vec3 v_WorldPosition;
in vec3 v_Normal;
in vec2 v_TexCoord;
in vec3 v_LocalPosition;

uniform vec3 u_LightPosition;
uniform vec3 u_CameraPosition;
uniform float u_SimTime;

out vec4 fragColor;

void main() {
    vec3 N = normalize(v_Normal);
    vec3 L = normalize(u_LightPosition - v_WorldPosition);
    vec3 V = normalize(u_CameraPosition - v_WorldPosition);

    // Multi-octave procedural cloud formations with swirling weather loops
    vec3 cloudCoord = v_LocalPosition * 4.5;
    float cloudNoise = fbm(cloudCoord);
    float detail = snoise(cloudCoord * 3.0) * 0.2;
    float cloudDensity = cloudNoise + detail;

    // Cloud thresholding: Coverage ~50%
    float coverage = smoothstep(0.05, 0.38, cloudDensity);
    if (coverage <= 0.01) {
        discard;
    }

    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);

    // Sunlit cloud tops vs terminator shading
    vec3 cloudColorDay = vec3(0.98, 0.98, 1.00);
    vec3 cloudColorDark = vec3(0.35, 0.40, 0.50);
    vec3 litCloud = mix(cloudColorDark, cloudColorDay, NdotL);

    // Forward light scattering on clouds
    float forwardScatter = pow(max(dot(L, -V), 0.0), 4.0) * 0.35;
    litCloud += vec3(forwardScatter);

    float alpha = coverage * 0.88 * smoothstep(0.0, 0.2, NdotV);
    fragColor = vec4(litCloud, alpha);
}
"""

    // =============================================================================================
    // 3. ATMOSPHERIC LIMB SCATTERING SHADER (Rayleigh & Mie Analytical Scattering Shell)
    // =============================================================================================
    const val ATMOSPHERE_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;
uniform mat3 u_NormalMatrix;
uniform float u_AtmosphereScale;

out vec3 v_WorldPosition;
out vec3 v_Normal;
out vec3 v_LocalPosition;

void main() {
    v_LocalPosition = a_Position;
    vec3 scaledPos = a_Position * u_AtmosphereScale;
    vec4 worldPos = u_ModelMatrix * vec4(scaledPos, 1.0);
    v_WorldPosition = worldPos.xyz;
    v_Normal = normalize(u_NormalMatrix * a_Normal);
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val ATMOSPHERE_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec3 v_WorldPosition;
in vec3 v_Normal;
in vec3 v_LocalPosition;

uniform vec3 u_LightPosition;
uniform vec3 u_CameraPosition;
uniform vec4 u_AtmosphereColor;
uniform float u_AtmosphereDensity;

out vec4 fragColor;

void main() {
    vec3 N = normalize(v_Normal);
    vec3 L = normalize(u_LightPosition - v_WorldPosition);
    vec3 V = normalize(u_CameraPosition - v_WorldPosition);

    float NdotV = max(dot(N, V), 0.0);
    float NdotL = dot(N, L);
    float VdotL = dot(V, L);

    // View-dependent optical depth path through shell (Fresnel rim peak)
    float opticalPath = pow(1.0 - NdotV, 2.8) * u_AtmosphereDensity;

    // Rayleigh Phase function: P_R(theta) = 3/4 * (1 + cos^2(theta))
    float rayleighPhase = 0.75 * (1.0 + VdotL * VdotL);

    // Forward Mie scattering peak: Henyey-Greenstein g ~ 0.76
    float g = 0.76;
    float miePhase = (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * max(VdotL, 0.0), 1.5) * 0.15;

    // Day/Night terminator extinction with smooth twilight transition
    float sunExposure = smoothstep(-0.25, 0.35, NdotL);

    vec3 skyColor = u_AtmosphereColor.rgb * (rayleighPhase + miePhase);
    float alpha = clamp(opticalPath * sunExposure, 0.0, 0.85);

    if (alpha <= 0.005) {
        discard;
    }

    fragColor = vec4(skyColor, alpha);
}
"""

    // =============================================================================================
    // 4. SATURN RING SHADER (Double-Sided, Cassini Division, Planet Shadowing)
    // =============================================================================================
    const val RING_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoord;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;
uniform mat3 u_NormalMatrix;
uniform mat4 u_VisualOrientationMatrix;

out vec3 v_WorldPosition;
out vec3 v_Normal;
out vec2 v_TexCoord;
out vec3 v_LocalPosition;

void main() {
    v_LocalPosition = a_Position;
    vec4 rotatedPos = u_VisualOrientationMatrix * vec4(a_Position, 1.0);
    vec4 worldPos = u_ModelMatrix * rotatedPos;
    v_WorldPosition = worldPos.xyz;
    
    vec3 rotatedNormal = (u_VisualOrientationMatrix * vec4(a_Normal, 0.0)).xyz;
    v_Normal = normalize(u_NormalMatrix * rotatedNormal);
    v_TexCoord = a_TexCoord;
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val RING_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec3 v_WorldPosition;
in vec3 v_Normal;
in vec2 v_TexCoord;
in vec3 v_LocalPosition;

uniform vec3 u_LightPosition;
uniform vec3 u_CameraPosition;
uniform vec3 u_PlanetCenter;
uniform float u_PlanetRadius;

out vec4 fragColor;

void main() {
    vec3 N = normalize(v_Normal);
    vec3 L = normalize(u_LightPosition - v_WorldPosition);
    vec3 V = normalize(u_CameraPosition - v_WorldPosition);

    // Radial distance in ring model space: length(v_LocalPosition.xz)
    float r = length(v_LocalPosition.xz);

    // Ring system density profile:
    // C Ring: r in [1.22, 1.52] -> faint, translucent
    // B Ring: r in [1.52, 1.95] -> bright, dense
    // Cassini Division: r in [1.95, 2.02] -> dark gap
    // A Ring: r in [2.02, 2.27] -> medium density
    // Encke Gap: r in [2.21, 2.22]
    // F Ring: r in [2.32, 2.36]

    float density = 0.0;
    vec3 ringColor = vec3(0.92, 0.84, 0.62);

    if (r >= 1.22 && r < 1.52) {
        // C Ring
        density = 0.25 + 0.15 * sin(r * 120.0);
        ringColor = vec3(0.65, 0.58, 0.45);
    } else if (r >= 1.52 && r < 1.95) {
        // B Ring (Main dense ring)
        density = 0.88 + 0.08 * sin(r * 180.0) + 0.04 * cos(r * 320.0);
        ringColor = vec3(0.96, 0.88, 0.68);
    } else if (r >= 1.95 && r < 2.02) {
        // Cassini Division
        density = 0.05;
        ringColor = vec3(0.20, 0.18, 0.15);
    } else if (r >= 2.02 && r < 2.27) {
        // A Ring
        if (r >= 2.205 && r <= 2.225) {
            density = 0.08; // Encke Gap
        } else {
            density = 0.65 + 0.10 * sin(r * 160.0);
        }
        ringColor = vec3(0.90, 0.82, 0.62);
    } else if (r >= 2.30 && r <= 2.38) {
        // F Ring
        density = 0.35 * smoothstep(2.30, 2.34, r) * (1.0 - smoothstep(2.34, 2.38, r));
        ringColor = vec3(0.85, 0.78, 0.60);
    } else {
        discard;
    }

    // =========================================================================
    // PLANET-TO-RING SHADOW PROJECTION
    // =========================================================================
    // Test if the ring point is in Saturn's shadow cylinder/cone facing away from L
    vec3 toRing = v_WorldPosition - u_PlanetCenter;
    float projL = dot(toRing, -L); // Distance along shadow cylinder axis (away from Sun)
    float shadowFactor = 1.0;

    if (projL > 0.0) { // Point is on the night side of Saturn
        vec3 perp = toRing - (-L) * projL;
        float perpDist = length(perp);
        if (perpDist < u_PlanetRadius) {
            // Smooth shadow penumbra edge
            shadowFactor = smoothstep(u_PlanetRadius * 0.95, u_PlanetRadius * 1.05, perpDist) * 0.05;
        }
    }

    // Double-sided illumination (ice particle scattering)
    float NdotL = abs(dot(N, L));
    float phase = 0.5 + 0.5 * NdotL;

    vec3 litColor = ringColor * phase * shadowFactor;
    float alpha = density * (0.4 + 0.6 * shadowFactor);

    fragColor = vec4(litColor, alpha);
}
"""

    // =============================================================================================
    // 5. SUN CORONA & PROMINENCE SHADER (Emissive Outer Halo)
    // =============================================================================================
    const val SUN_CORONA_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;
uniform float u_CoronaScale;

out vec3 v_LocalPosition;

void main() {
    v_LocalPosition = a_Position;
    vec4 worldPos = u_ModelMatrix * vec4(a_Position * u_CoronaScale, 1.0);
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val SUN_CORONA_FRAGMENT_SHADER = """#version 300 es
precision highp float;

$GLSL_NOISE_UTILITY

in vec3 v_LocalPosition;
uniform vec3 u_CameraPosition;
uniform float u_SimTime;

out vec4 fragColor;

void main() {
    float r = length(v_LocalPosition);
    if (r < 0.95 || r > 1.45) {
        discard;
    }

    // Radial exponential falloff
    float radialFalloff = exp(-(r - 0.95) * 8.0);

    // Procedural solar flares and corona streamers
    float angle = atan(v_LocalPosition.z, v_LocalPosition.x);
    float streamer = snoise(vec3(v_LocalPosition * 4.0 + vec3(u_SimTime * 0.08, 0.0, 0.0))) * 0.25;
    float flare = pow(max(snoise(vec3(v_LocalPosition * 8.0 - vec3(0.0, u_SimTime * 0.15, 0.0))), 0.0), 2.0) * 0.5;

    vec3 coronaColor = mix(vec3(1.0, 0.45, 0.10), vec3(1.0, 0.85, 0.40), radialFalloff);
    float alpha = (radialFalloff + streamer + flare) * 0.65;

    fragColor = vec4(coronaColor * 1.5, clamp(alpha, 0.0, 0.8));
}
"""

    // =============================================================================================
    // 6. ACES FILMIC POST-PROCESSING & TONE MAPPING SHADER
    // =============================================================================================
    const val TONE_MAPPING_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec2 v_TexCoord;
uniform sampler2D u_SceneTexture;
uniform float u_Exposure;

out vec4 fragColor;

// Narkowicz 2015 ACES Filmic Tone Mapping Curve
vec3 acesFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 sceneColor = texture(u_SceneTexture, v_TexCoord);
    vec3 exposed = sceneColor.rgb * u_Exposure;
    vec3 mapped = acesFilmic(exposed);
    fragColor = vec4(mapped, sceneColor.a);
}
"""
}
