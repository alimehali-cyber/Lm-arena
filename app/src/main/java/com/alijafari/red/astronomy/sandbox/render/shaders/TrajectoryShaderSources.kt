package com.alijafari.red.astronomy.sandbox.render.shaders

/**
 * Embedded GLSL ES 3.00 shader sources for Phase 4 Visualization Layer:
 * - Orbital Trails (Historical)
 * - Trajectory Prediction (Forward-projected dashed line)
 * - Velocity & Acceleration Vectors
 * - Barycenter Marker Reticle
 * - Collision & Merger Shockwaves
 * - Selection Targeting Reticle
 */
object TrajectoryShaderSources {

    // =============================================================================================
    // 1. HISTORICAL ORBITAL TRAIL SHADER
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
uniform float u_IsSelected; // 1.0 = selected body emphasis, 0.0 = normal

out vec4 fragColor;

void main() {
    float alphaBoost = (u_IsSelected > 0.5) ? 1.3 : 1.0;
    float finalAlpha = clamp(v_Alpha * u_TrailColor.a * alphaBoost, 0.0, 1.0);
    vec3 color = (u_IsSelected > 0.5) ? mix(u_TrailColor.rgb, vec3(1.0), 0.25) : u_TrailColor.rgb;
    fragColor = vec4(color, finalAlpha);
}
"""

    // =============================================================================================
    // 2. PREDICTED TRAJECTORY SHADER (Dashed / Stippled pattern)
    // =============================================================================================
    const val PREDICTION_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in float a_StepProgress; // 0.0 at current position -> 1.0 at prediction horizon

uniform mat4 u_ViewProjectionMatrix;

out float v_StepProgress;

void main() {
    v_StepProgress = a_StepProgress;
    gl_Position = u_ViewProjectionMatrix * vec4(a_Position, 1.0);
}
"""

    const val PREDICTION_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in float v_StepProgress;
uniform vec4 u_PredictionColor;
uniform float u_StippleFrequency; // Controls dash spacing

out vec4 fragColor;

void main() {
    // Dynamic dash pattern along normalized step progress
    float pattern = sin(v_StepProgress * u_StippleFrequency * 6.283185);
    if (pattern < -0.15) {
        discard; // Stippled dash gap
    }
    
    // Smooth fade towards the end of prediction horizon
    float alphaFade = 1.0 - smoothstep(0.7, 1.0, v_StepProgress);
    float finalAlpha = u_PredictionColor.a * alphaFade * 0.85;
    
    fragColor = vec4(u_PredictionColor.rgb, finalAlpha);
}
"""

    // =============================================================================================
    // 3. VECTOR OVERLAY SHADER (Velocity & Gravitational Acceleration 3D Vectors)
    // =============================================================================================
    const val VECTOR_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;
layout(location = 1) in float a_SegmentAlpha;

uniform mat4 u_ViewProjectionMatrix;

out float v_SegmentAlpha;

void main() {
    v_SegmentAlpha = a_SegmentAlpha;
    gl_Position = u_ViewProjectionMatrix * vec4(a_Position, 1.0);
}
"""

    const val VECTOR_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

in float v_SegmentAlpha;
uniform vec4 u_VectorColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(u_VectorColor.rgb, u_VectorColor.a * v_SegmentAlpha);
}
"""

    // =============================================================================================
    // 4. BARYCENTER RETICLE SHADER (Scientific Center-of-Mass Marker)
    // =============================================================================================
    const val BARYCENTER_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position;

uniform mat4 u_ViewProjectionMatrix;
uniform vec3 u_BarycenterWorldPos;
uniform float u_MarkerScale;

void main() {
    vec3 worldPos = u_BarycenterWorldPos + a_Position * u_MarkerScale;
    gl_Position = u_ViewProjectionMatrix * vec4(worldPos, 1.0);
}
"""

    const val BARYCENTER_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

uniform vec4 u_BarycenterColor;

out vec4 fragColor;

void main() {
    fragColor = u_BarycenterColor;
}
"""

    // =============================================================================================
    // 5. COLLISION & MERGER SHOCKWAVE SHADER
    // =============================================================================================
    const val SHOCKWAVE_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position; // Unit circle vertex on XZ plane

uniform mat4 u_ViewProjectionMatrix;
uniform vec3 u_ShockwaveOrigin;
uniform float u_CurrentRadius;

void main() {
    vec3 worldPos = u_ShockwaveOrigin + a_Position * u_CurrentRadius;
    gl_Position = u_ViewProjectionMatrix * vec4(worldPos, 1.0);
}
"""

    const val SHOCKWAVE_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

uniform vec4 u_ShockwaveColor;
uniform float u_AlphaDecay;

out vec4 fragColor;

void main() {
    fragColor = vec4(u_ShockwaveColor.rgb, u_ShockwaveColor.a * u_AlphaDecay);
}
"""

    // =============================================================================================
    // 6. SELECTION RETICLE SHADER (Targeted Body Halo Ring)
    // =============================================================================================
    const val SELECTION_RING_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 a_Position; // Unit circle around origin

uniform mat4 u_ViewProjectionMatrix;
uniform mat4 u_ModelMatrix;

void main() {
    gl_Position = u_ViewProjectionMatrix * (u_ModelMatrix * vec4(a_Position, 1.0));
}
"""

    const val SELECTION_RING_FRAGMENT_SHADER = """#version 300 es
precision mediump float;

uniform vec4 u_RingColor;

out vec4 fragColor;

void main() {
    fragColor = u_RingColor;
}
"""
}
