#version 300 es
precision highp float;
precision highp sampler2D;

uniform sampler2D u_SemanticTexture;
uniform sampler2D u_NoiseTexture;
uniform float u_TimeA;
uniform float u_TimeB;
uniform float u_BlendA;
uniform float u_TimeScale;
uniform float u_Amplitude;
uniform float u_NoiseMean;
uniform float u_Mass;
uniform float u_Spin;
uniform float u_DiskInnerRadius;
uniform float u_DiskOuterRadius;

out float fragColor;

const float TAU = 6.28318530718;

float hash12(vec2 p) {
    float h = dot(p, vec2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float valueNoisePeriodicX(vec2 uv, float periodX) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    f = f * f * (3.0 - 2.0 * f);
    float px = periodX;
    float x0 = mod(i.x, px);
    float x1 = mod(i.x + 1.0, px);
    float y0 = i.y;
    float y1 = i.y + 1.0;
    float a = hash12(vec2(x0, y0));
    float b = hash12(vec2(x1, y0));
    float c = hash12(vec2(x0, y1));
    float d = hash12(vec2(x1, y1));
    float top = mix(a, b, f.x);
    float bottom = mix(c, d, f.x);
    return mix(top, bottom, f.y);
}

// Multi-scale Keplerian sheared fBm - all prograde, no counter-shear
float keplerianNoise(vec2 azimuthRadiusNorm, float shiftNorm, float radiusNorm) {
    float az = azimuthRadiusNorm.x;
    float rn = azimuthRadiusNorm.y;

    // Octave 1: Macro stream (32, 4) weight 0.55 - prograde only
    vec2 uv1 = vec2(az * 32.0 + shiftNorm * 32.0, rn * 4.0);
    float n1 = valueNoisePeriodicX(uv1, 32.0);

    // Octave 2: Turbulent eddies (64, 12) weight 0.30 - prograde, no negative shear
    vec2 uv2 = vec2(az * 64.0 + shiftNorm * 64.0, rn * 12.0);
    float n2 = valueNoisePeriodicX(uv2, 64.0);

    // Octave 3: Micro filaments (128, 24) weight 0.15 - prograde
    vec2 uv3 = vec2(az * 128.0 + shiftNorm * 128.0, rn * 24.0);
    float n3 = valueNoisePeriodicX(uv3, 128.0);

    float combined = n1 * 0.55 + n2 * 0.30 + n3 * 0.15;

    float filament = valueNoisePeriodicX(vec2(az * 96.0 + shiftNorm * 96.0, rn * 18.0), 96.0);
    filament = smoothstep(0.25, 0.85, filament);
    combined = mix(combined, filament, 0.16);

    return clamp(combined, 0.0, 1.0);
}

void main() {
    if (u_Amplitude == 0.0) {
        fragColor = 1.0;
        return;
    }

    ivec2 recordCoord = ivec2(gl_FragCoord.xy);
    vec4 semantic = texelFetch(u_SemanticTexture, recordCoord, 0);
    int state = int(floor(semantic.a + 0.5));

    if (state != 3) {
        fragColor = 1.0;
        return;
    }

    float radiusNorm = clamp(semantic.r, 0.0, 1.0);
    float azimuthNorm = fract(semantic.g);

    float radius = mix(u_DiskInnerRadius, u_DiskOuterRadius, radiusNorm);
    float sqrtMass = sqrt(max(u_Mass, 1.0e-6));
    float denominator = pow(max(radius, 1.0e-6), 1.5) + u_Spin * sqrtMass;
    float omega = sqrtMass / max(1.0e-6, denominator);

    float shiftA = omega * u_TimeScale * u_TimeA / TAU;
    float shiftB = omega * u_TimeScale * u_TimeB / TAU;

    float wA = clamp(u_BlendA, 0.0, 1.0);
    float wB = 1.0 - wA;

    float noiseA = keplerianNoise(vec2(azimuthNorm, radiusNorm), shiftA, radiusNorm);
    float noiseB = keplerianNoise(vec2(azimuthNorm, radiusNorm), shiftB + 0.37, radiusNorm + 0.29);

    float noiseCombined = wA * noiseA + wB * noiseB;
    noiseCombined = clamp(noiseCombined, 0.0, 1.0);

    // Stable modulation formula - no tearing at ±80%
    float n = noiseCombined;
    float dust = smoothstep(0.20, 0.80, n);

    float amp = u_Amplitude; // 0.15, 0.40, 0.80
    float brightFactor = 1.0 + amp * 0.60 * (1.0 - dust);
    float absorbFactor = exp(-amp * 1.10 * dust);

    float finalMod = clamp(brightFactor * absorbFactor, 0.35, 1.85);

    fragColor = finalMod;
}
