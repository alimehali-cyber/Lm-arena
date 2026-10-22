package com.alijafari.red.astronomy.avatar

/**
 * Pure OpenGL ES 3.0 (`#version 300 es`) Vertex Skinning + 4-Active-Morph Blending + Metallic-Roughness
 * Hemisphere Sky Shader designed specifically for ZIG (`com.alijafari.red.astronomy`).
 *
 * Zero Vulkan dependencies, zero NDK/C++, guaranteed compatible with mobile uniform limits (`uJoints[64]`).
 */
object AvatarShaders {

    const val VERTEX_SHADER_GLES30 = """#version 300 es
precision highp float;

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in ivec4 aJoints;
layout(location = 4) in vec4 aWeights;
layout(location = 5) in vec3 aMorph0;
layout(location = 6) in vec3 aMorph1;
layout(location = 7) in vec3 aMorph2;
layout(location = 8) in vec3 aMorph3;

uniform mat4 uViewProj;
uniform mat4 uModel;
uniform mat4 uJoints[64];
uniform vec4 uActiveMorphWeights;

out vec3 vWorldPos;
out vec3 vWorldNormal;
out vec2 vTexCoord;

void main() {
    vec3 morphedPos = aPosition
        + aMorph0 * uActiveMorphWeights.x
        + aMorph1 * uActiveMorphWeights.y
        + aMorph2 * uActiveMorphWeights.z
        + aMorph3 * uActiveMorphWeights.w;

    mat4 skinMat =
          aWeights.x * uJoints[aJoints.x]
        + aWeights.y * uJoints[aJoints.y]
        + aWeights.z * uJoints[aJoints.z]
        + aWeights.w * uJoints[aJoints.w];

    vec4 localSkinned = skinMat * vec4(morphedPos, 1.0);
    vec4 worldPos = uModel * localSkinned;
    vWorldPos = worldPos.xyz;
    vWorldNormal = normalize(mat3(uModel) * mat3(skinMat) * aNormal);
    vTexCoord = aTexCoord;
    gl_Position = uViewProj * worldPos;
}
"""

    const val FRAGMENT_SHADER_GLES30 = """#version 300 es
precision mediump float;

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
    vec3 albedo = pow(baseSample.rgb, vec3(2.2));
    vec3 orm = texture(uOrmTex, vTexCoord).rgb;
    float ao = orm.r;
    float roughness = clamp(orm.g, 0.04, 1.0);
    float metallic = clamp(orm.b, 0.0, 1.0);

    vec3 N = normalize(vWorldNormal);
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 L = normalize(uKeyLightDir);
    vec3 H = normalize(V + L);

    float NdotL = max(dot(N, L), 0.0);
    float NdotH = max(dot(N, H), 0.0);
    float fresnelRim = pow(1.0 - max(dot(N, V), 0.0), 3.2);

    vec3 f0 = mix(vec3(0.04), albedo, metallic);
    float shininess = mix(128.0, 6.0, roughness * roughness);
    vec3 spec = f0 * pow(NdotH, shininess) * (1.25 - roughness * 0.7);
    vec3 diffuse = albedo * (1.0 - metallic);

    vec3 hemi = mix(uGroundBounceColor, uSkyZenithColor, 0.5 + 0.5 * N.y) * ao;
    vec3 lit = diffuse * (hemi + uKeyLightColor * NdotL) + spec * uKeyLightColor + uRimColor * fresnelRim * 0.28;

    vec3 mapped = lit / (lit + vec3(1.0));
    fragColor = vec4(pow(mapped, vec3(1.0 / 2.2)), baseSample.a);
}
"""
}
