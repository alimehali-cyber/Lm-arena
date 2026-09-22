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

const float TAU = 6.28318530718;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yzz) * p.zyx);
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

vec3 rotateVector(vec3 v, vec3 axis, float angle) {
    float cosA = cos(angle);
    float sinA = sin(angle);
    return v * cosA + cross(axis, v) * sinA + axis * dot(axis, v) * (1.0 - cosA);
}

vec3 rotateAroundAxis(vec3 v, vec3 axis, float angle) {
    return rotateVector(v, axis, angle);
}

vec3 renderProceduralCosmos(vec3 skyDir) {
    vec3 d = normalize(skyDir);
    vec3 col = vec3(0.0006, 0.0008, 0.0014);

    // Ethereal Galactic Plane - continuous band that warps into Einstein arcs
    float b = dot(d, vec3(0.577, 0.577, -0.577));
    float galacticDisk = exp(-abs(b) * 4.5);
    float galacticHalo = exp(-abs(b) * 1.5) * 0.35;
    float dustNoise = fbm3D(d * 12.0);
    float dustAbsorption = smoothstep(0.35, 0.75, dustNoise) * 0.7;
    vec3 galacticColor = vec3(0.85, 0.75, 0.65) * (galacticDisk * (1.0 - dustAbsorption))
                       + vec3(0.30, 0.35, 0.60) * galacticHalo;
    galacticColor *= 0.18;
    col += galacticColor;

    // Subtle deep nebula for depth
    float nebula1 = valueNoise3(d * 2.5);
    float nebula2 = valueNoise3(d * 5.0 + vec3(12.3, 7.1, 3.7));
    float nebula = nebula1 * 0.6 + nebula2 * 0.4;
    nebula = pow(nebula, 1.6) * 0.85;
    vec3 nebulaColor = vec3(0.04, 0.03, 0.08) * nebula * 0.05;
    col += nebulaColor;

    // Anti-aliased, non-blinking stars - lower frequency 85.0 for distinct spacing
    vec3 p = d * 85.0;
    vec3 ip = floor(p);
    vec3 fp = fract(p);
    vec3 starRadiance = vec3(0.0);

    for (int z = -1; z <= 1; z++) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec3 neighbor = vec3(float(x), float(y), float(z));
                vec3 cellHash = hash33(ip + neighbor);
                vec3 starPos = neighbor + cellHash - 0.5;
                float dist = length(fp - starPos);

                float brightnessHash = cellHash.x;
                float isProminent = step(0.96, brightnessHash);
                float isMedium = step(0.82, brightnessHash) * (1.0 - isProminent);
                float isFaint = (1.0 - isProminent) * (1.0 - isMedium);

                float baseIntensity = isProminent * (0.8 + 0.6 * cellHash.y)
                                    + isMedium * (0.35 + 0.25 * cellHash.y)
                                    + isFaint * (0.08 + 0.08 * cellHash.y);

                float coreRadius = isProminent > 0.5 ? 0.38 : (isMedium > 0.5 ? 0.28 : 0.20);
                float core = exp(-(dist * dist) / (2.0 * coreRadius * coreRadius));
                float corona = isProminent > 0.5 ? exp(-dist * 2.2) * 0.20 : 0.0;

                vec3 warm = vec3(1.0, 0.78, 0.55);
                vec3 whiteBlue = mix(vec3(0.95, 0.95, 1.0), vec3(0.65, 0.82, 1.0), cellHash.y);
                vec3 starColor = mix(warm, whiteBlue, cellHash.z);

                starRadiance += starColor * (core + corona) * baseIntensity;
            }
        }
    }

    col += starRadiance;

    return clamp(col, vec3(0.0), vec3(1.5));
}

// Legacy alias for previous implementation
vec3 proceduralCosmos(vec3 dir) {
    return renderProceduralCosmos(dir);
}

void main() {
    // Fix frozen sky: use integer rounding on half-float RGBA16F
    vec4 sem = texture(u_SemanticTexture, v_TexCoord);
    int state = int(sem.w + 0.5);

    if (state == 2) {
        // ESCAPED RAY -> Evaluate dynamic lensed sky
        vec3 d = normalize(sem.xyz);
        float len2 = dot(d, d);
        if (len2 < 0.5 || len2 > 1.5) {
            d = vec3(0.0, 0.0, 1.0);
        }
        d = normalize(d);
        // Use new axis/angle uniforms with fallback to old speed uniform
        vec3 axis = normalize(u_SkyAxis);
        float axisLen = dot(u_SkyAxis, u_SkyAxis);
        if (axisLen < 0.1) {
            axis = normalize(vec3(0.28, 0.86, 0.42));
        }
        float angle = u_SkyAngle;
        // Fallback if angle not set but old speed is
        if (abs(angle) < 0.0001 && u_SkyRotationSpeed > 0.0001) {
            angle = u_Time * u_SkyRotationSpeed;
        }
        vec3 skyDir = rotateVector(d, axis, angle);
        vec3 skyColor = renderProceduralCosmos(skyDir);
        fragColor = vec4(skyColor, 1.0);
    } else if (state == 3) {
        // DISK -> Apply turbulence modulation
        vec4 hdr = texture(u_HdrTexture, v_TexCoord);
        float modFactor = texture(u_ModulationTexture, v_TexCoord).r;
        fragColor = vec4(hdr.rgb * modFactor, hdr.a);
    } else {
        // EVENT HORIZON SHADOW (state 0 or 1) -> Pure black
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
