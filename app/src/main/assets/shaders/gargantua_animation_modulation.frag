#version 300 es
precision highp float;
precision highp sampler2D;

uniform sampler2D u_SemanticTexture;
uniform sampler2D u_NoiseTexture; // 512x256 periodic R8 texture

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
const float TWO_PI = 6.283185307179586;

// High-speed flowing fluid gas sample with differential Keplerian shear and inward accretion drift
float sampleFluidFlow(vec2 normCoord, float tPhase, float rPhysical) {
    float rNorm = normCoord.x;
    float phiNorm = normCoord.y;

    float sqrtMass = sqrt(max(u_Mass, 1.0e-6));
    float denomOmega = pow(max(rPhysical, 1.0e-6), 1.5) + u_Spin * sqrtMass;
    float omega = sqrtMass / max(1.0e-6, denomOmega);

    float rRatio = u_DiskInnerRadius / max(1.0e-5, rPhysical);
    float vInflow = 0.060 * sqrt(rRatio);
    float rDrift = rNorm - vInflow * (tPhase / 24.0);

    float spiralCoil = 11.0 * log(max(1.0, rPhysical / u_DiskInnerRadius));
    float phiSheared = phiNorm + (omega * u_TimeScale * tPhase) / TWO_PI - spiralCoil / TWO_PI;

    // Rebalanced to near-isotropic phi:r frequency ratio (previously ~1.5-1.7x mismatch
    // caused thin aligned "guitar string" artifacts). Per-octave offsets decorrelate
    // repeated sampling of the same 512x256 texture at different scales.
    vec2 uvMacro = vec2(fract(phiSheared * 3.0), fract(rDrift * 3.0 + 0.11));
    vec2 uvMeso  = vec2(fract(phiSheared * 7.0 + 0.37), fract(rDrift * 7.0 + 0.59));
    vec2 uvMicro = vec2(fract(phiSheared * 13.0 + 0.71), fract(rDrift * 13.0 + 0.19));

    float nMacro = texture(u_NoiseTexture, uvMacro).r;
    float nMeso  = texture(u_NoiseTexture, uvMeso).r;
    float nMicro = texture(u_NoiseTexture, uvMicro).r;

    // Plasma streaks toned down so they accent rather than dominate the blobby cloud shape
    float streakPattern = pow(texture(u_NoiseTexture, vec2(fract(phiSheared * 10.0 + 0.83), fract(rDrift * 4.0))).r, 2.2);

    float dustRift = smoothstep(0.25, 0.75, texture(u_NoiseTexture, vec2(fract(phiSheared * 2.0), fract(rDrift * 2.0 + 0.47))).r);

    float fluidNoise = (nMacro * 0.35 + nMeso * 0.30 + nMicro * 0.20 + streakPattern * 0.40) * (0.25 + 0.75 * dustRift);
    return fluidNoise;
}

// Multi-scale Keplerian sheared turbulence combining 3D fluid streams
float keplerianNoise(vec2 azimuthRadiusNorm, float shiftNorm, float radiusNorm, float tPhase, float rPhysical) {
    float az = azimuthRadiusNorm.x;
    float rn = azimuthRadiusNorm.y;

    float rRatio = u_DiskInnerRadius / max(1.0e-5, rPhysical);
    float vInflow = 0.060 * sqrt(rRatio);
    float rDrift = rn - vInflow * (tPhase / 24.0);
    float spiralCoil = 11.0 * log(max(1.0, rPhysical / u_DiskInnerRadius));
    float phiSheared = az + shiftNorm - spiralCoil / TAU;

    // Reduced max frequency from 128.0 (previously 32.0, 64.0, 128.0 octaves) to 54.0 to avoid
    // sub-Nyquist aliasing against the 512px-wide noise texture (512/54 ≈ 9.5 texels/cycle, safe under GL_LINEAR).
    // Also reduced phi:r frequency ratio for isotropic blob shapes instead of streaks.
    vec2 uv1 = vec2(fract(phiSheared * 18.0), fract(rDrift * 8.0));
    float n1 = texture(u_NoiseTexture, uv1).r;

    vec2 uv2 = vec2(fract(phiSheared * 36.0 + 0.29), fract(rDrift * 16.0 + 0.61));
    float n2 = texture(u_NoiseTexture, uv2).r;

    vec2 uv3 = vec2(fract(phiSheared * 54.0 + 0.53), fract(rDrift * 24.0 + 0.17));
    float n3 = texture(u_NoiseTexture, uv3).r;

    float combined = n1 * 0.55 + n2 * 0.30 + n3 * 0.15;
    float fluidSample = sampleFluidFlow(vec2(rn, az), tPhase, rPhysical);
    return clamp(combined * 0.40 + fluidSample * 0.60, 0.0, 1.0);
}

void main() {
    if (u_Amplitude == 0.0) {
        fragColor = 1.0;
        return;
    }

    ivec2 texCoord = ivec2(gl_FragCoord.xy);
    vec4 semantic = texelFetch(u_SemanticTexture, texCoord, 0);
    int state = int(floor(semantic.a + 0.5));

    // Passthrough if not accretion disk
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

    float fluidSignal = wA * noiseA + wB * noiseB;
    fluidSignal = clamp(fluidSignal, 0.0, 1.0);

    float dust = smoothstep(0.20, 0.80, fluidSignal);

    float amp = u_Amplitude;
    float brightFactor = 1.0 + amp * 0.85 * (1.0 - dust);
    float absorbFactor = exp(-amp * 1.10 * dust);

    // High dynamic range contrast curve: deep dark dust lanes (0.15) to blazing highlights (2.6)
    float centered = fluidSignal - u_NoiseMean;
    float dynamicAmplitude = amp * 1.8;
    float modFactor = clamp(1.0 + dynamicAmplitude * centered * 2.5, 0.15, 2.60);
    modFactor = modFactor * (brightFactor * absorbFactor);

    // Radial boundary fade
    float boundaryFade = smoothstep(0.0, 0.04, radiusNorm) * smoothstep(1.0, 0.88, radiusNorm);
    float finalModulation = mix(1.0, clamp(modFactor, 0.15, 2.60), boundaryFade);

    fragColor = finalModulation;
}
