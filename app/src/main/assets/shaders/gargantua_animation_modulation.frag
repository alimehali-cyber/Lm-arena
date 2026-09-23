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

// Concentric harmonic striation generator for modulation
float computeHarmonicStriations(float r) {
    float rings = 0.0;
    rings += sin(r * 48.0) * 0.25;
    rings += sin(r * 115.0 + rings * 1.5) * 0.20;
    rings += sin(r * 290.0) * 0.15;
    return rings;
}

// Multi-scale Keplerian sheared fBm with inward transonic spiral advection
float keplerianNoise(vec2 azimuthRadiusNorm, float shiftNorm, float radiusNorm, float tPhase, float rPhysical) {
    float az = azimuthRadiusNorm.x;
    float rn = azimuthRadiusNorm.y;

    // Inward transonic accretion drift
    float rRatio = u_DiskInnerRadius / max(1.0e-5, rPhysical);
    float vInflow = 0.045 * sqrt(rRatio);
    float rDrift = rn - vInflow * (tPhase / 24.0);
    float spiralCoil = 1.8 * log(max(1.0, rPhysical / u_DiskInnerRadius));
    float phiSheared = az + shiftNorm - spiralCoil / TAU;

    // Octave 1: Macro stream (32, 4) weight 0.55
    vec2 uv1 = vec2(fract(phiSheared * 32.0), fract(rDrift * 4.0));
    float n1 = texture(u_NoiseTexture, uv1).r;

    // Octave 2: Turbulent eddies (64, 12) weight 0.30
    vec2 uv2 = vec2(fract(phiSheared * 64.0), fract(rDrift * 12.0));
    float n2 = texture(u_NoiseTexture, uv2).r;

    // Octave 3: Micro filaments (128, 24) weight 0.15
    vec2 uv3 = vec2(fract(phiSheared * 128.0), fract(rDrift * 24.0));
    float n3 = texture(u_NoiseTexture, uv3).r;

    float combined = n1 * 0.55 + n2 * 0.30 + n3 * 0.15;
    float rings = computeHarmonicStriations(rPhysical);
    return clamp(combined + rings * 0.35, 0.0, 1.0);
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

    float noiseA = keplerianNoise(vec2(azimuthNorm, radiusNorm), shiftA, radiusNorm, u_TimeA, radius);
    float noiseB = keplerianNoise(vec2(azimuthNorm, radiusNorm), shiftB + 0.37, radiusNorm + 0.29, u_TimeB, radius);

    float noiseCombined = wA * noiseA + wB * noiseB;
    noiseCombined = clamp(noiseCombined, 0.0, 1.0);

    float n = noiseCombined;
    float dust = smoothstep(0.20, 0.80, n);

    float amp = u_Amplitude;
    float brightFactor = 1.0 + amp * 0.60 * (1.0 - dust);
    float absorbFactor = exp(-amp * 1.10 * dust);

    float boundaryFade = smoothstep(0.0, 0.05, radiusNorm) * smoothstep(1.0, 0.85, radiusNorm);
    float finalMod = mix(1.0, clamp(brightFactor * absorbFactor, 0.35, 1.85), boundaryFade);

    fragColor = finalMod;
}