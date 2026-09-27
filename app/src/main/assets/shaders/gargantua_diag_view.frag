#version 300 es
// TEMPORARY GPU pipeline diagnostics: fullscreen display of one intermediate texture.
// No tone mapping, bloom, palette or animation is applied here.
precision highp float;
precision highp int;
precision highp sampler2D;
precision highp usampler2D;

uniform int u_Mode;          // 0 linear RGB clamp(v*scale)  1 ray-record false colour  2 higher-order/total grayscale
uniform sampler2D u_A;
uniform usampler2D u_R;
uniform float u_Scale;
uniform ivec2 u_Size;
uniform int u_MarkerCount;
uniform vec2 u_Markers[32];  // marker positions in [0,1] texture coordinates
uniform vec2 u_Screen;       // destination size in pixels

in vec2 v_TexCoord;
out vec4 fragColor;

void main() {
    vec3 c;
    if (u_Mode == 1) {
        ivec2 p = clamp(ivec2(v_TexCoord * vec2(u_Size)), ivec2(0), u_Size - 1);
        uvec4 r = texelFetch(u_R, p, 0);
        bool nonzero = any(notEqual(r, uvec4(0u)));
        bool hasHigh = ((r.y | r.w) & 0xFFFF0000u) != 0u;
        // R = hit radius (unorm16), G = hit azimuth (unorm16), B = valid crossing (high halves set).
        c = vec3(float(r.x & 0xFFFFu) / 65535.0, float(r.x >> 16) / 65535.0, hasHigh ? 1.0 : 0.0);
        if (nonzero && !hasHigh) c = vec3(1.0, 0.0, 1.0); // truncated record signature
    } else {
        vec4 v = texture(u_A, v_TexCoord);
        if (u_Mode == 2) {
            float l = dot(v.rgb, vec3(0.2126, 0.7152, 0.0722));
            c = vec3((v.a >= 1.0 && l > 1.0e-4) ? clamp((v.a - 1.0) / l, 0.0, 1.0) : 0.0);
        } else {
            c = clamp(v.rgb * u_Scale, 0.0, 1.0);
        }
    }
    vec2 px = v_TexCoord * u_Screen;
    for (int i = 0; i < 32; i++) {
        if (i >= u_MarkerCount) break;
        vec2 d = abs(px - u_Markers[i] * u_Screen);
        if ((d.x < 1.0 && d.y < 9.0) || (d.y < 1.0 && d.x < 9.0)) {
            c = (i < 10) ? vec3(0.0, 1.0, 1.0) : ((i < 12) ? vec3(1.0, 0.2, 0.2) : vec3(0.3, 1.0, 0.3));
        }
    }
    fragColor = vec4(c, 1.0);
}
