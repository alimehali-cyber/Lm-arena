#version 300 es
precision highp float;
precision highp sampler2D;

uniform sampler2D u_HdrTexture;
uniform sampler2D u_ModulationTexture;

in vec2 v_TexCoord;
out vec4 fragColor;

void main() {
    // The modulation texture is ray-grid sized and deliberately sampled with GL_LINEAR. This
    // follows the exact same normalized-coordinate alignment as the existing coarse color upscale.
    vec4 hdr = texture(u_HdrTexture, v_TexCoord);
    float modulation = texture(u_ModulationTexture, v_TexCoord).r;
    fragColor = vec4(hdr.rgb * modulation, hdr.a);
}
