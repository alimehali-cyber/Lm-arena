#version 300 es
precision highp float;
precision highp sampler2D;

uniform sampler2D u_HdrTexture;
uniform sampler2D u_ModulationTexture;
uniform sampler2D u_SemanticTexture;
uniform float u_Time;
uniform float u_SkyRotationSpeed;

in vec2 v_TexCoord;
out vec4 fragColor;

const float TAU = 6.28318530718;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yzz) * p.zyx);
}

float valueNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);
    return mix(nxy0, nxy1, f.z);
}

vec3 rotateAroundAxis(vec3 v, vec3 axis, float angle) {
    // Rodrigues rotation
    float cosA = cos(angle);
    float sinA = sin(angle);
    return v * cosA + cross(axis, v) * sinA + axis * dot(axis, v) * (1.0 - cosA);
}

vec3 proceduralCosmos(vec3 dir) {
    vec3 d = normalize(dir);
    vec3 col = vec3(0.0005, 0.0007, 0.0012);

    // Nebula - 2 octaves low amplitude
    float nebula1 = valueNoise3(d * 2.5);
    float nebula2 = valueNoise3(d * 5.0 + vec3(12.3, 7.1, 3.7));
    float nebula = nebula1 * 0.6 + nebula2 * 0.4;
    nebula = pow(nebula, 1.6) * 0.85;
    vec3 nebulaColor = vec3(0.04, 0.03, 0.08) * nebula * 0.06;
    nebulaColor += vec3(0.02, 0.015, 0.05) * pow(nebula2, 2.2) * 0.03;
    col += nebulaColor;

    float galacticDist = abs(d.z * 0.8 + d.y * 0.6);
    float milkyWay = exp(-galacticDist * galacticDist * 18.0) * 0.004;
    col += vec3(0.005, 0.006, 0.012) * milkyWay;

    // Pinpoint stars - search neighboring cells for accurate Voronoi (more expensive but only in apply pass at 30 FPS, not in geodesic)
    vec3 p = d * 280.0;
    vec3 i = floor(p);
    vec3 f = fract(p);

    float bestDist = 10.0;
    vec3 bestOffset = vec3(0.0);
    vec3 bestCell = vec3(0.0);

    // Check 8 neighboring cells for nearest star (cheap Voronoi approximation)
    // This prevents chunky streaks and ensures crisp points that stretch into arcs when lensed
    for (int x = -1; x <= 0; x++) {
        for (int y = -1; y <= 0; y++) {
            for (int z = -1; z <= 0; z++) {
                vec3 cell = i + vec3(float(x), float(y), float(z));
                vec3 offset = hash33(cell);
                vec3 diff = (vec3(float(x), float(y), float(z)) + offset - f);
                float dist = dot(diff, diff);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestOffset = offset;
                    bestCell = cell;
                }
            }
        }
    }

    float dist = sqrt(bestDist);
    float starIntensity = pow(max(0.0, 1.0 - dist * 2.5), 32.0);

    if (starIntensity > 0.0001) {
        float rand = hash13(bestCell);
        vec3 starCol;
        float colorRand = hash13(bestCell + vec3(19.7, 27.3, 11.5));
        if (colorRand < 0.33) {
            starCol = vec3(1.0, 0.75, 0.5);
        } else if (colorRand < 0.66) {
            starCol = vec3(0.95, 0.95, 1.0);
        } else {
            starCol = vec3(0.7, 0.85, 1.0);
        }

        float mag;
        if (rand < 0.90) {
            mag = 0.05 + rand * 0.11;
            starIntensity *= mag;
        } else if (rand < 0.99) {
            mag = 0.2 + (rand - 0.90) * 3.33;
            starIntensity *= mag * 1.5;
        } else {
            mag = 0.8 + (rand - 0.99) * 70.0;
            starIntensity *= mag * 2.0;
            // Diffraction spikes
            vec3 diff = bestOffset - f;
            // Approximate 2D cross in tangent plane
            float spikeX = pow(max(0.0, 1.0 - abs(diff.y) * 32.0), 24.0) * pow(max(0.0, 1.0 - abs(diff.x) * 3.0), 4.0);
            float spikeY = pow(max(0.0, 1.0 - abs(diff.x) * 32.0), 24.0) * pow(max(0.0, 1.0 - abs(diff.y) * 3.0), 4.0);
            float spikes = (spikeX + spikeY) * 0.12 * mag;
            col += starCol * spikes;
        }
        col += starCol * starIntensity;
    }

    return clamp(col, vec3(0.0), vec3(1.5));
}

void main() {
    vec4 hdr = texture(u_HdrTexture, v_TexCoord);
    float modulation = texture(u_ModulationTexture, v_TexCoord).r;
    vec4 semantic = texture(u_SemanticTexture, v_TexCoord);
    int state = int(floor(semantic.a + 0.5));

    if (state == 2) {
        // Dynamic lensed starfield: unpack exact deflected vector, apply slow cosmic rotation
        vec3 d = vec3(semantic.r, semantic.g, semantic.b);
        float len2 = dot(d, d);
        if (len2 < 0.5 || len2 > 1.5) {
            // Fallback if semantic not yet populated or corrupted
            d = vec3(0.0, 0.0, 1.0);
        }
        d = normalize(d);

        // Slow celestial drift axis - arbitrary but stable
        vec3 axis = normalize(vec3(0.31, 0.62, 0.27));
        float angle = u_Time * u_SkyRotationSpeed;
        vec3 vSky = rotateAroundAxis(d, axis, angle);

        vec3 sky = proceduralCosmos(vSky);
        fragColor = vec4(sky, 1.0);
    } else if (state == 3) {
        // Disk: apply filament modulation
        fragColor = vec4(hdr.rgb * modulation, hdr.a);
    } else {
        // Shadow (0.0), unresolved (0.5), object (4.0) - preserve original HDR (pure black for shadow)
        // Ensure capture boundary outputs pure black unconditionally
        if (state == 1) {
            fragColor = vec4(0.0, 0.0, 0.0, 0.0);
        } else {
            fragColor = hdr;
        }
    }
}
