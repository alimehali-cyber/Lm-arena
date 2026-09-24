#version 300 es
precision mediump float;

uniform sampler2D u_HdrTexture;
uniform sampler2D u_BloomTexture;
uniform float u_Exposure;
uniform float u_BloomIntensity;
uniform int u_EnableBloom;
uniform vec2 u_TexelSize; // 1.0 / hdrTextureWidth, 1.0 / hdrTextureHeight

in vec2 v_TexCoord;
out vec4 fragColor;

// ACES filmic tone mapping operator (Krzysztof Narkowicz fit)
vec3 aces_filmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.42;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 hdr = texture(u_HdrTexture, v_TexCoord);

    // Strictly guaranteed pure black shadow: event-horizon (alpha 0.0) and unresolved
    // (alpha 0.5) rays never reach the display transform.
    if (hdr.a <= 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 color = hdr.rgb;

    // Boundary-guarded unsharp mask:
    // Only sharpen if all neighbors are outside the black hole shadow, so the shadow edge and the
    // photon ring never receive a bright overshoot rim.
    vec4 n1 = texture(u_HdrTexture, v_TexCoord + vec2(u_TexelSize.x, 0.0));
    vec4 n2 = texture(u_HdrTexture, v_TexCoord - vec2(u_TexelSize.x, 0.0));
    vec4 n3 = texture(u_HdrTexture, v_TexCoord + vec2(0.0, u_TexelSize.y));
    vec4 n4 = texture(u_HdrTexture, v_TexCoord - vec2(0.0, u_TexelSize.y));

    if (n1.a > 0.5 && n2.a > 0.5 && n3.a > 0.5 && n4.a > 0.5) {
        vec3 neighborAvg = (n1.rgb + n2.rgb + n3.rgb + n4.rgb) * 0.25;
        color += (color - neighborAvg) * 0.40;
        color = max(color, vec3(0.0));
    }

    // Bloom addition
    if (u_EnableBloom == 1) {
        vec3 bloom = texture(u_BloomTexture, v_TexCoord).rgb;
        color += bloom * u_BloomIntensity;
    }

    // Exposure scaling
    vec3 exposed = color * u_Exposure;

    // ACES Filmic tonemapping
    vec3 ldr = aces_filmic(exposed);

    // CRITICAL FIX: Display OETF (Gamma 2.2 / sRGB conversion)
    // Narkowicz ACES produces linear display output.
    // Without this gamma encoding, midtones display severely crushed and muddy on the
    // RGBA8888 (non-sRGB) window surface.
    ldr = pow(ldr, vec3(1.0 / 2.2));

    fragColor = vec4(ldr, 1.0);
}
