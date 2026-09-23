#version 300 es
precision highp float;

uniform sampler2D u_SemanticTexture; // Attachment 1 from ray-trace pass
uniform sampler2D u_NoiseTexture;    // Deterministic 512x256 periodic R8 texture

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

const float PI = 3.141592653589793;
const float TWO_PI = 6.283185307179586;

// Concentric harmonic striation generator for modulation
float computeHarmonicStriations(float r) {
    float rings = 0.0;
    rings += sin(r * 48.0) * 0.25;
    rings += sin(r * 115.0 + rings * 1.5) * 0.20;
    rings += sin(r * 290.0) * 0.15;
    return rings;
}

// Single-phase advected noise sample with Keplerian shear and inward spiral drift
float sampleAdvectedNoise(vec2 normCoord, float tPhase, float rPhysical) {
    float rNorm = normCoord.x;
    float phiNorm = normCoord.y;

    // 1. Keplerian differential angular velocity
    float sqrtMass = sqrt(max(u_Mass, 1.0e-6));
    float denomOmega = pow(max(rPhysical, 1.0e-6), 1.5) + u_Spin * sqrtMass;
    float omega = sqrtMass / max(1.0e-6, denomOmega);

    // 2. Inward viscous accretion drift: matter accelerates as it approaches ISCO
    float rRatio = u_DiskInnerRadius / max(1.0e-5, rPhysical);
    float vInflow = 0.045 * sqrt(rRatio); // Transonic drift parameter
    float rDrift = rNorm - vInflow * (tPhase / 24.0);

    // 3. Logarithmic spiral coordinate: coils inward toward the black hole
    float spiralCoil = 1.8 * log(max(1.0, rPhysical / u_DiskInnerRadius));
    float phiSheared = phiNorm - (omega * u_TimeScale * tPhase) / TWO_PI - spiralCoil / TWO_PI;

    // 4. Multi-scale periodic hardware texture sampling
    // Texture wraps periodically along X (phi) and Y (r)
    vec2 uvMacro = vec2(fract(phiSheared * 4.0), fract(rDrift * 2.0));
    vec2 uvMeso  = vec2(fract(phiSheared * 12.0), fract(rDrift * 6.0));
    vec2 uvMicro = vec2(fract(phiSheared * 32.0), fract(rDrift * 16.0));

    float nMacro = texture(u_NoiseTexture, uvMacro).r;
    float nMeso  = texture(u_NoiseTexture, uvMeso).r;
    float nMicro = texture(u_NoiseTexture, uvMicro).r;

    float combinedNoise = nMacro * 0.50 + nMeso * 0.35 + nMicro * 0.15;

    // Inject concentric rings
    float rings = computeHarmonicStriations(rPhysical);
    return combinedNoise + rings * 0.35;
}

void main() {
    ivec2 texCoord = ivec2(gl_FragCoord.xy);
    vec4 semantic = texelFetch(u_SemanticTexture, texCoord, 0);
    float rayState = semantic.a;

    // If not accretion disk (state != 3), output identity modulation
    if (abs(rayState - 3.0) > 0.1) {
        fragColor = 1.0;
        return;
    }

    float rNorm = semantic.r;
    float phiNorm = semantic.g;
    float rPhysical = mix(u_DiskInnerRadius, u_DiskOuterRadius, rNorm);

    // Dual-phase flow-mapping ping-pong evaluation
    float sampleA = sampleAdvectedNoise(vec2(rNorm, phiNorm), u_TimeA, rPhysical);
    float sampleB = sampleAdvectedNoise(vec2(rNorm, phiNorm), u_TimeB, rPhysical);

    // Seamless cross-fade between Phase A and Phase B
    float rawNoise = mix(sampleB, sampleA, u_BlendA);

    // Scale by user-configured modulation amplitude centered around noise mean
    float centered = rawNoise - u_NoiseMean;
    float modFactor = 1.0 + u_Amplitude * centered;

    // Radial boundary fade: modulation gracefully vanishes at disk edges
    float boundaryFade = smoothstep(0.0, 0.05, rNorm) * smoothstep(1.0, 0.85, rNorm);
    float finalModulation = mix(1.0, modFactor, boundaryFade);

    // Output single-channel float scalar (R16F / R8)
    fragColor = max(0.15, finalModulation);
}
