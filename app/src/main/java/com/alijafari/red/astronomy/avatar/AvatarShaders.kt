package com.alijafari.red.astronomy.avatar

/**
 * Pure OpenGL ES 3.0 (`#version 300 es`) Vertex Skinning + PBR Metallic-Roughness + Hemisphere Sky +
 * Soft Ground Contact Shadow Shaders for ZIG (`com.alijafari.red.astronomy`).
 *
 * Uses `vec4 aJoints` float vertex attributes (`int(aJoints.x + 0.5)`) for 100% compatibility across
 * all Adreno, Mali, and PowerVR mobile GLES 3.0 drivers (`minSdk 24`). Pure Kotlin only.
 */
object AvatarShaders {

    const val VERTEX_SHADER_GLES30 = """#version 300 es
precision highp float;
precision highp int;

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aJoints;
layout(location = 4) in vec4 aWeights;

uniform mat4 uViewProj;
uniform mat4 uModel;
uniform mat4 uJoints[64];

out vec3 vWorldPos;
out vec3 vWorldNormal;
out vec2 vTexCoord;

void main() {
    int j0 = clamp(int(aJoints.x + 0.5), 0, 63);
    int j1 = clamp(int(aJoints.y + 0.5), 0, 63);
    int j2 = clamp(int(aJoints.z + 0.5), 0, 63);
    int j3 = clamp(int(aJoints.w + 0.5), 0, 63);

    mat4 skinMat =
          aWeights.x * uJoints[j0]
        + aWeights.y * uJoints[j1]
        + aWeights.z * uJoints[j2]
        + aWeights.w * uJoints[j3];

    vec4 localSkinned = skinMat * vec4(aPosition, 1.0);
    vec4 worldPos = uModel * localSkinned;
    vWorldPos = worldPos.xyz;
    vWorldNormal = normalize(mat3(uModel) * mat3(skinMat) * aNormal);
    vTexCoord = aTexCoord;
    gl_Position = uViewProj * worldPos;
}
"""

    const val FRAGMENT_SHADER_GLES30 = """#version 300 es
precision highp float;
precision highp int;

in vec3 vWorldPos;
in vec3 vWorldNormal;
in vec2 vTexCoord;

uniform sampler2D uAlbedoTex;
uniform sampler2D uOrmTex;
uniform vec3 uCameraPos;
uniform vec3 uKeyLightDir;
uniform vec3 uKeyLightColor;
uniform vec3 uSkyZenithColor;
uniform vec3 uGroundBounceColor;
uniform vec3 uRimColor;

out vec4 fragColor;

void main() {
    vec4 baseSample = texture(uAlbedoTex, vTexCoord);
    vec3 albedo = pow(max(baseSample.rgb, vec3(0.0)), vec3(2.2));
    vec3 orm = texture(uOrmTex, vTexCoord).rgb;
    float ao = mix(1.0, orm.r, 0.72);
    float roughness = clamp(orm.g, 0.08, 1.0);
    float metallic = clamp(orm.b, 0.0, 1.0);

    vec3 N = normalize(vWorldNormal);
    if (!gl_FrontFacing) {
        N = -N;
    }
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 L = normalize(uKeyLightDir);
    vec3 H = normalize(V + L);

    float wrapNdotL = clamp((dot(N, L) + 0.22) / 1.22, 0.0, 1.0);
    float NdotH = max(dot(N, H), 0.0);
    float fresnelRim = pow(1.0 - max(dot(N, V), 0.0), 3.0);

    vec3 f0 = mix(vec3(0.04), albedo, metallic);
    float shininess = mix(96.0, 8.0, roughness * roughness);
    vec3 spec = f0 * pow(NdotH, shininess) * (1.15 - roughness * 0.65);
    vec3 diffuse = albedo * (1.0 - metallic);

    vec3 hemi = mix(uGroundBounceColor, uSkyZenithColor, 0.5 + 0.5 * N.y) * ao;
    vec3 lit = diffuse * (hemi + uKeyLightColor * wrapNdotL)
        + spec * uKeyLightColor
        + uRimColor * fresnelRim * 0.26;

    // ACES filmic tone curve + sRGB encode + premultiplied alpha for Android TextureView compositing
    vec3 mapped = (lit * (2.51 * lit + 0.03)) / (lit * (2.43 * lit + 0.59) + 0.14);
    vec3 srgb = pow(clamp(mapped, 0.0, 1.0), vec3(1.0 / 2.2));
    float alpha = clamp(baseSample.a, 0.0, 1.0);
    fragColor = vec4(srgb * alpha, alpha);
}
"""

    const val SHADOW_VERTEX_SHADER_GLES30 = """#version 300 es
precision highp float;

layout(location = 0) in vec2 aQuadCorner;

uniform mat4 uViewProj;
uniform vec2 uShadowCenterXZ;
uniform vec2 uShadowRadiusXZ;

out vec2 vLocalUV;

void main() {
    vLocalUV = aQuadCorner;
    vec3 worldPos = vec3(
        uShadowCenterXZ.x + aQuadCorner.x * uShadowRadiusXZ.x,
        0.004,
        uShadowCenterXZ.y + aQuadCorner.y * uShadowRadiusXZ.y
    );
    gl_Position = uViewProj * vec4(worldPos, 1.0);
}
"""

    const val SHADOW_FRAGMENT_SHADER_GLES30 = """#version 300 es
precision mediump float;

in vec2 vLocalUV;
uniform float uShadowAlpha;

out vec4 fragColor;

void main() {
    float r = length(vLocalUV);
    float falloff = pow(clamp(1.0 - r, 0.0, 1.0), 1.85);
    float a = uShadowAlpha * falloff;
    fragColor = vec4(0.0, 0.0, 0.0, a);
}
"""
}
