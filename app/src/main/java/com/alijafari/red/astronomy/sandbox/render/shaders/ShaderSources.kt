package com.alijafari.red.astronomy.sandbox.render.shaders

/**
 * Embedded GLSL ES 3.00 shader sources for the Gravity Sandbox rendering pipeline.
 */
object ShaderSources {

    // =============================================================================================
    // 1. CELESTIAL BODY SHADER (Phased Lighting & Emissive Sun Support)
    // =============================================================================================
    const val BODY_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec3 a_Normal;
layout(location = 2) in vec2 a_TexCoord;

uniform mat4 u_ModelMatrix;
uniform mat4 u_ViewProjectionMatrix;
uniform mat3 u_NormalMatrix;

out vec3 v_WorldPosition;
out vec3 v_Normal;
out vec2 v_TexCoord;

void main() {
    vec4 worldPos = u_ModelMatrix * vec4(a_Position, 1.0);
    v_WorldPosition = worldPos.xyz;
    v_Normal = normalize(u_NormalMatrix * a_Normal);
    v_TexCoord = a_TexCoord;
    gl_Position = u_ViewProjectionMatrix * worldPos;
}
"""

    const val BODY_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec3 v_WorldPosition;
in vec3 v_Normal;
in vec2 v_TexCoord;

uniform vec4 u_BaseColor;
uniform vec3 u_LightPosition;
uniform vec3 u_CameraPosition;
uniform float u_IsEmissive;      // 1.0 = Sun / Star (self-illuminating), 0.0 = Planet/Moon
uniform float u_Shininess;
uniform float u_AmbientIntensity;

out vec4 fragColor;

void main() {
    if (u_IsEmissive > 0.5) {
        // Emissive Sun / Star core with limb darkening effect
        vec3 N = normalize(v_Normal);
        vec3 V = normalize(u_CameraPosition - v_WorldPosition);
        float NdotV = max(dot(N, V), 0.0);
        
        // Solar limb darkening approximation
        float limbFactor = 0.45 + 0.55 * pow(NdotV, 0.6);
        vec3 sunColor = u_BaseColor.rgb * limbFactor * 1.35;
        
        fragColor = vec4(sunColor, u_BaseColor.a);
    } else {
        // Diffuse + Specular Blinn-Phong lighting for planets/asteroids
        vec3 N = normalize(v_Normal);
        vec3 L = normalize(u_LightPosition - v_WorldPosition);
        vec3 V = normalize(u_CameraPosition - v_WorldPosition);
        vec3 H = normalize(L + V);

        // Diffuse Lambertian
        float NdotL = max(dot(N, L), 0.0);
        vec3 diffuse = u_BaseColor.rgb * NdotL;

        // Specular highlight
        float NdotH = max(dot(N, H), 0.0);
        float specularFactor = pow(NdotH, max(u_Shininess, 1.0));
        vec3 specular = vec3(0.35) * specularFactor * (NdotL > 0.0 ? 1.0 : 0.0);

        // Ambient floor
        vec3 ambient = u_BaseColor.rgb * u_AmbientIntensity;

        // Fresnel atmosphere rim highlight
        float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0) * 0.15;
        vec3 rim = u_BaseColor.rgb * fresnel;

        vec3 finalRgb = ambient + diffuse + specular + rim;
        fragColor = vec4(finalRgb, u_BaseColor.a);
    }
}
"""

    // =============================================================================================
    // 2. PROCEDURAL GPU STARFIELD SHADER
    // =============================================================================================
    const val STARFIELD_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;    // Normalized direction on celestial sphere
layout(location = 1) in float a_Brightness; // Apparent stellar magnitude / brightness
layout(location = 2) in vec3 a_StarColor;   // Spectral color tint (B-V approximation)

uniform mat4 u_ViewRotationMatrix; // View matrix with (0,0,0) translation
uniform mat4 u_ProjectionMatrix;
uniform float u_PointSizeScale;

out float v_Brightness;
out vec3 v_Color;

void main() {
    v_Brightness = a_Brightness;
    v_Color = a_StarColor;
    
    vec4 viewPos = u_ViewRotationMatrix * vec4(a_Position * 400.0, 1.0);
    gl_Position = u_ProjectionMatrix * viewPos;
    
    // Size based on brightness
    gl_PointSize = clamp((a_Brightness * 3.5 + 1.2) * u_PointSizeScale, 1.0, 16.0);
}
"""

    const val STARFIELD_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in float v_Brightness;
in vec3 v_Color;

uniform float u_BackgroundAlpha;

out vec4 fragColor;

void main() {
    // Soft circular Gaussian point sprite
    vec2 coord = gl_PointCoord - vec2(0.5);
    float distSq = dot(coord, coord);
    if (distSq > 0.25) {
        discard;
    }
    
    float alpha = exp(-distSq * 10.0) * v_Brightness;
    fragColor = vec4(v_Color, alpha * u_BackgroundAlpha);
}
"""

    // =============================================================================================
    // 3. ORBITAL TRAIL SHADER
    // =============================================================================================
    const val TRAIL_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in float a_Alpha;

uniform mat4 u_ViewProjectionMatrix;

out float v_Alpha;

void main() {
    v_Alpha = a_Alpha;
    gl_Position = u_ViewProjectionMatrix * vec4(a_Position, 1.0);
}
"""

    const val TRAIL_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in float v_Alpha;
uniform vec4 u_TrailColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(u_TrailColor.rgb, u_TrailColor.a * v_Alpha);
}
"""

    // =============================================================================================
    // 4. REFERENCE GRID SHADER (Orbital plane / Reference rings)
    // =============================================================================================
    const val GRID_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in vec4 a_Color;

uniform mat4 u_ViewProjectionMatrix;
uniform vec3 u_CameraPosition;

out vec4 v_Color;
out float v_Dist;

void main() {
    v_Color = a_Color;
    v_Dist = length(a_Position - u_CameraPosition);
    gl_Position = u_ViewProjectionMatrix * vec4(a_Position, 1.0);
}
"""

    const val GRID_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in vec4 v_Color;
in float v_Dist;

out vec4 fragColor;

void main() {
    // Fade out grid in distance for clean aesthetic
    float fade = clamp(1.0 - (v_Dist / 120.0), 0.0, 1.0);
    fragColor = vec4(v_Color.rgb, v_Color.a * fade);
}
"""

    // =============================================================================================
    // 5. FULLSCREEN QUAD (Post-processing & Phase 5 Relativistic Black Hole Pipeline Hook)
    // =============================================================================================
    const val FULLSCREEN_QUAD_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 a_Position;
out vec2 v_TexCoord;

void main() {
    v_TexCoord = (a_Position + 1.0) * 0.5;
    gl_Position = vec4(a_Position, 0.0, 1.0);
}
"""

    const val FULLSCREEN_QUAD_FRAGMENT_SHADER = """#version 300 es
precision highp float;

in vec2 v_TexCoord;
uniform sampler2D u_SceneTexture;

out vec4 fragColor;

void main() {
    // Pass-through post-process shader
    fragColor = texture(u_SceneTexture, v_TexCoord);
}
"""
}
