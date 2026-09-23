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
out vec4 fragColor;
out vec4 out_Color;

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
    vec4 sem = texelFetch(u_SemanticTexture, coord, 0);
      
    int state = int(sem.w + 0.5);
  
    if (state <= 1) {
        // Event Horizon / Shadow: Pure Invariant Black (Never blooms)
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        out_Color = vec4(0.0, 0.0, 0.0, 1.0);
    } else if (state == 2) {
        // Escaped Sky Ray: sem.xyz holds deflected unit ray direction
        vec3 deflectedRay = normalize(sem.xyz);
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
            float speed = u_SkyRotationSpeed > 0.0001 ? u_SkyRotationSpeed : 0.045;
            skyAngle = u_Time * speed;
        }
        vec3 rotatedSky = rotateAxis(deflectedRay, celestialAxis, skyAngle);
          
        vec3 cosmosColor = renderProceduralCosmos(rotatedSky);
        fragColor = vec4(cosmosColor, 1.0);
        out_Color = vec4(cosmosColor, 1.0);
    } else if (state == 3) {
        // Accretion Disk Texel: modulate by Keplerian flow buffer
        float modFactor = texelFetch(u_ModulationTexture, coord, 0).r;
        vec3 diskRadiance = hdrColor.rgb * modFactor;
        fragColor = vec4(diskRadiance, 1.0);
        out_Color = vec4(diskRadiance, 1.0);
    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        out_Color = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
