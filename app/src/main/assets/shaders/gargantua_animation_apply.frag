#version 300 es
precision highp float;
precision highp sampler2D;

uniform sampler2D u_HdrTexture;
uniform sampler2D u_ModulationTexture;
uniform sampler2D u_SemanticTexture;
uniform vec3 u_SkyAxis;
uniform float u_SkyAngle;
uniform float u_Time;
uniform float u_SkyRotationSpeed;

in vec2 v_TexCoord;
layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

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

// Legacy aliases for compatibility
vec3 hash33(vec3 p) { return cosmosHash33(p); }
float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
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
vec3 rotateAxis(vec3 v, vec3 axis, float angle) {
    return v * cos(angle) + cross(axis, v) * sin(angle) + axis * dot(axis, v) * (1.0 - cos(angle));
}
vec3 rotateVector(vec3 v, vec3 axis, float angle) { return rotateAxis(v, axis, angle); }
vec3 rotateAroundAxis(vec3 v, vec3 axis, float angle) { return rotateAxis(v, axis, angle); }
vec3 proceduralCosmos(vec3 dir) { return renderProceduralCosmos(dir); }
vec3 sample_procedural_sky(vec3 dir) { return renderProceduralCosmos(dir); }

void main() {
    ivec2 coord = ivec2(gl_FragCoord.xy);
    vec4 hdrColor = texelFetch(u_HdrTexture, coord, 0);
    vec4 semantic = texelFetch(u_SemanticTexture, coord, 0);
    float state = semantic.a;

    if (state <= 1.5) {
        // Event Horizon Shadow or Unresolved: Preserve pure black & strict alpha
        // If HDR color has accumulated foreground disk radiance, preserve it; otherwise pure black
        if (hdrColor.a <= 0.5) {
            fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        } else {
            // Foreground gas in front of horizon: modulate radiance
            float modFactor = texelFetch(u_ModulationTexture, coord, 0).r;
            fragColor = vec4(hdrColor.rgb * modFactor, hdrColor.a);
        }
        return;
    }

    if (abs(state - 3.0) < 0.1) {
        // Accretion Disk Texel: multiply HDR radiance by the Keplerian flow factor
        float modFactor = texelFetch(u_ModulationTexture, coord, 0).r;
        fragColor = vec4(hdrColor.rgb * modFactor, hdrColor.a);
        return;
    }

    if (abs(state - 2.0) < 0.1) {
        // Escaped Sky Texel: dynamically rotate deflected ray around celestial axis
        vec3 deflectedRay = semantic.rgb;
        float len2 = dot(deflectedRay, deflectedRay);
        if (len2 < 0.5 || len2 > 1.5) {
            deflectedRay = vec3(0.0, 0.0, 1.0);
        }
        deflectedRay = normalize(deflectedRay);
        vec3 celestialAxis = u_SkyAxis;
        float axisLen = dot(celestialAxis, celestialAxis);
        if (axisLen < 0.1) {
            celestialAxis = normalize(vec3(0.28, 0.86, 0.42));
        } else {
            celestialAxis = normalize(celestialAxis);
        }
        float skyAngle = u_SkyAngle;
        if (abs(skyAngle) < 0.0001) {
            float speed = u_SkyRotationSpeed > 0.0001 ? u_SkyRotationSpeed : 0.02618;
            skyAngle = u_Time * speed;
        }
        if (abs(u_SkyAngle) < 0.0001 && abs(u_SkyRotationSpeed) < 0.0001) {
            skyAngle = u_Time * 0.02618; // Exactly 1.5 degrees per second
        }
        vec3 rotatedSky = rotateAxis(deflectedRay, celestialAxis, skyAngle);
        vec3 skyRadiance = renderProceduralCosmos(rotatedSky);

        // Blend with any translucent foreground disk radiance stored in hdrColor
        vec3 finalSky = hdrColor.rgb + skyRadiance;
        fragColor = vec4(finalSky, hdrColor.a);
        return;
    }

    // Default passthrough
    fragColor = hdrColor;
}
