#version 300 es
precision mediump float;

uniform sampler2D u_HdrTexture;
uniform sampler2D u_BloomTexture;
uniform float u_Exposure;
uniform float u_BloomIntensity;
uniform int u_EnableBloom;

in vec2 v_TexCoord;
out vec4 fragColor;

// ACES filmic tone mapping operator (Krzysztof Narkowicz 2015 fit)
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

    // Black hole shadow protection and unresolved ray handling:
    // If ray hit event horizon (alpha <= 0.0) or remained unresolved (alpha <= 0.5),
    // strictly guarantee pure black.
    if (hdr.a <= 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec3 color = hdr.rgb;
    if (u_EnableBloom == 1) {
        vec3 bloom = texture(u_BloomTexture, v_TexCoord).rgb;
        color += bloom * u_BloomIntensity;
    }

    // Explicit cinematic exposure adjustment
    vec3 exposed = color * u_Exposure;

    // Scientifically controlled ACES filmic display transform
    vec3 ldr = aces_filmic(exposed);
    fragColor = vec4(ldr, 1.0);
}
