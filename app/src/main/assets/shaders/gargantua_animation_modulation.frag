#version 300 es
precision highp float;
precision highp sampler2D;

uniform sampler2D u_SemanticTexture;
uniform sampler2D u_NoiseTexture;
uniform float u_Time; // base-16 digit zero, always in [0, 16)
uniform float u_TimeDigit1;
uniform vec2 u_TimeDigits23;
uniform vec2 u_TimeDigits45;
uniform vec2 u_TimeDigits67;
uniform float u_TimeScale;
uniform float u_Amplitude;
uniform float u_NoiseMean;
uniform float u_Mass;
uniform float u_Spin;
uniform float u_DiskInnerRadius;
uniform float u_DiskOuterRadius;

out float fragColor;

const float TAU = 6.28318530718;
const float TIME_BASE = 16.0;

float phaseAdvance(
    float factor,
    float digit0,
    float digit1,
    vec2 digits23,
    vec2 digits45,
    vec2 digits67
) {
    float coefficient = fract(factor);
    float advance = digit0 * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digit1 * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digits23.x * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digits23.y * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digits45.x * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digits45.y * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digits67.x * coefficient;
    coefficient = fract(coefficient * TIME_BASE);
    advance += digits67.y * coefficient;
    return fract(advance);
}

void main() {
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

    // Existing geodesic shader convention: vx = -Omega*y, vy = Omega*x. For a > 0,
    // Omega > 0 and atan(y,x) increases, so this is prograde motion.
    // Time is decomposed into base-16 digits. Each coefficient is reduced before the next
    // multiplication, keeping every fp32 operation small even after hours of uptime.
    float phaseFactor1 = omega * u_TimeScale / TAU;
    float phaseFactor2 = 2.0 * omega * u_TimeScale * 0.7 / TAU;
    float phase1 = fract(semantic.g + phaseAdvance(
        phaseFactor1,
        u_Time,
        u_TimeDigit1,
        u_TimeDigits23,
        u_TimeDigits45,
        u_TimeDigits67
    ));
    // Layer two has a 2x spatial azimuth/radial scale and 0.7x temporal speed.
    float phase2 = fract(2.0 * semantic.g + phaseAdvance(
        phaseFactor2,
        u_Time,
        u_TimeDigit1,
        u_TimeDigits23,
        u_TimeDigits45,
        u_TimeDigits67
    ));

    float noise0 = texture(u_NoiseTexture, vec2(phase1, radiusNorm)).r;
    float noise1 = texture(u_NoiseTexture, vec2(phase2, radiusNorm * 2.0)).r;
    float weightedCenteredNoise =
        0.7 * (noise0 - u_NoiseMean) +
        0.3 * (noise1 - u_NoiseMean);
    // Normalize by the larger possible centered excursion. This keeps the peak absolute
    // deviation exactly equal to u_Amplitude even when the measured tile mean is not 0.5.
    float centeredNoise = weightedCenteredNoise / max(u_NoiseMean, 1.0 - u_NoiseMean);

    // The measured CPU tile mean is subtracted here. Both frequency mappings are integer-periodic
    // over the repeated tile, so the ideal disk-domain mean remains one without artificial pairing.
    fragColor = 1.0 + u_Amplitude * centeredNoise;
}
