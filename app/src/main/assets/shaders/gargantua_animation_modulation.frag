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

void main() {
    if (u_Amplitude == 0.0) {
        fragColor = 1.0;
        return;
    }

    ivec2 recordCoord = ivec2(gl_FragCoord.xy);
    vec4 semantic = texelFetch(u_SemanticTexture, recordCoord, 0);
    int state = int(floor(semantic.a + 0.5));

    // The semantic cache is ray-grid sized and nearest/texelFetch is intentional. Non-disk
    // records remain exactly one, so no animation texture is applied outside state 3.
    if (state != 3) {
        fragColor = 1.0;
        return;
    }

    float radiusNorm = clamp(semantic.r, 0.0, 1.0);
    float radius = mix(u_DiskInnerRadius, u_DiskOuterRadius, radiusNorm);
    float sqrtMass = sqrt(max(u_Mass, 1.0e-6));
    float denominator = pow(max(radius, 1.0e-6), 1.5) + u_Spin * sqrtMass;
    float omega = sqrtMass / max(1.0e-6, denominator);
    float phaseA = fract(semantic.g - omega * u_TimeScale * u_TimeA / TAU);
    float phaseB = fract(semantic.g - omega * u_TimeScale * u_TimeB / TAU);
    float wA = clamp(u_BlendA, 0.0, 1.0);
    float wB = 1.0 - wA;

    float nA = texture(u_NoiseTexture, vec2(phaseA, radiusNorm)).r;
    float nB = texture(u_NoiseTexture, vec2(phaseB + 0.37, radiusNorm + 0.29)).r;
    float n = (wA * (nA - u_NoiseMean) + wB * (nB - u_NoiseMean)) /
        max(sqrt(wA * wA + wB * wB), 1.0e-3);
    float centered = clamp(n / max(u_NoiseMean, 1.0 - u_NoiseMean), -1.0, 1.0);
    fragColor = 1.0 + u_Amplitude * centered;
}
