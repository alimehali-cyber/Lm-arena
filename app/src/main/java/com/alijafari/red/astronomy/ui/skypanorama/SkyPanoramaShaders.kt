package com.alijafari.red.astronomy.ui.skypanorama

/**
 * GLSL ES 3.00 sources for the panorama pass. Written from first principles for this renderer.
 *
 * The vertex shader emits one oversized triangle that covers the viewport, so no vertex buffer is
 * needed. The fragment shader converts each pixel to an equatorial view direction using the
 * uniform camera basis, converts that direction to NASA celestial-map texture coordinates, and
 * samples the panorama with an explicit, screen-uniform LOD.
 *
 * Mapping contract (see [SkyPanoramaMath]):
 *   u = fract(0.5 - RA / 2pi), v = 0.5 - Dec / pi
 * The horizontal wrap is handled by GL_REPEAT on S, so the seam at RA = +/-pi samples neighbouring
 * texels without a derivative discontinuity, because the LOD is a uniform.
 *
 * Night sky model
 * ---------------
 * The NASA plate is a calibrated *photograph of the galaxy*, not a photograph of the night sky: its
 * background is a flat grey field, there is no airglow, no light pollution, no zodiacal light, no
 * moonlight and no twilight — and noon and midnight look identical. Before this pass it was simply
 * multiplied down at night, which is why the night looked like a picture of the Milky Way rather
 * than a sky seen from the ground.
 *
 * This shader keeps the plate for exactly what it is good at — the galaxy's dust clouds, clusters
 * and star clouds — and synthesises every other contribution from the same physics the AR sky uses
 * (see `ar/core/SkyRenderModel` and `ar/core/SkyAtmosphere`), then tone-maps the sum once, so all
 * of it shares one exposure:
 *
 *  1. **Galaxy** — the plate, sRGB-decoded, sky-subtracted, masked to the galactic band (so the
 *     plate's flat background never shows as grey haze) and weighted by the galactic latitude. It
 *     reddens and dims through the atmosphere with the same per-channel extinction as everything
 *     else, so it rises red near the horizon and is drawn only above it.
 *  2. **Airglow** — the natural night sky (21.95 mag/arcsec² at the zenith, mean solar activity)
 *     brightening towards the horizon as X·T^(X−1) (Krisciunas & Schaefer 1991), with a slow
 *     large-scale patchiness, faintly green as the 557.7 nm OI line is.
 *  3. **Light pollution** — the Bortle excess over the natural sky, concentrated in domes near the
 *     horizon and tinted by high-pressure sodium/LED spectra.
 *  4. **Zodiacal light and Gegenschein** — along the ecliptic, brightest 90° from the Sun, with the
 *     anti-solar Gegenschein, both extinguished by airmass.
 *  5. **Moonlight** — Krisciunas & Schaefer's scattering function, scaled by the Moon's illuminated
 *     fraction, its altitude and the extinction on its own path; cool and strongest near the Moon.
 *  6. **Twilight** — single-scattered sunlight from the Sun's depression angle, Henyey-Greenstein
 *     forward scattering towards the Sun, the Earth's shadow and the Belt of Venus opposite it.
 *  7. **Ground** — below the horizon everything is strongly dimmed, so the plate never pretends to
 *     be sky where the ground is.
 *
 * The result is converted from luminance (cd/m²) to display sRGB with the same exposure, ACES
 * curve and Purkinje shift `SkyRenderModel` uses for the AR sky, then dithered so the very dark
 * gradients do not band on 8-bit panels.
 */
object SkyPanoramaShaders {

    const val VERTEX_SHADER = """#version 300 es
precision highp float;
precision highp int;

out vec2 vNdc;

void main() {
    // Vertices (-1,-1), (3,-1), (-1,3) form a triangle that covers the whole clip square.
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
    vNdc = p * 2.0 - 1.0;
    gl_Position = vec4(vNdc, 0.0, 1.0);
}
"""

    const val FRAGMENT_SHADER = """#version 300 es
precision highp float;
precision highp int;

precision highp sampler2D;

in vec2 vNdc;
out vec4 fragColor;

uniform sampler2D uPanorama;
uniform vec3 uForward;
uniform vec3 uRight;
uniform vec3 uUp;
uniform vec2 uTanHalf;
uniform float uLod;
uniform float uExposure;
uniform float uSaturation;
uniform float uContrast;
uniform float uVisibility;
// Daytime palette (sRGB 0..1) and the day/night blend. uNightSkyWeight 0 = daytime gradient only,
// 1 = the physical night sky only.
uniform vec3 uSkyZenith;
uniform vec3 uSkyMid;
uniform vec3 uSkyHorizon;
// Blend from the daytime palette to the physical night sky. It reaches 1 while the sky is still in
// twilight, so the twilight the model synthesises is never held back by the day/night mix.
uniform float uNightSkyWeight;

// --- Observer and geometry (equatorial ICRF/J2000 unit vectors and horizontal positions) --------
uniform vec3 uZenithDir;      // observer's zenith in equatorial coordinates
uniform vec3 uNorthDir;       // the north point of the horizon
uniform vec3 uEastDir;        // the east point of the horizon
uniform float uSunAzDeg;
uniform float uSunAltDeg;
uniform float uMoonAzDeg;
uniform float uMoonAltDeg;

// --- Photometry, all in cd/m^2 unless stated otherwise ------------------------------------------
uniform vec3 uTau;            // zenith optical depth per channel (extinction of starlight/galaxy)
uniform float uAirglow;       // natural airglow at the zenith, cd/m^2
uniform float uLightPollution;// artificial (Bortle excess) sky at the zenith, cd/m^2
uniform float uZenithTz;      // photopic zenith transmittance, for the airglow path term
uniform float uMoonSky;       // sky luminance 90 deg from a full Moon at the zenith, cd/m^2
uniform float uGalaxyPeak;    // cd/m^2 for a unit galaxy texel after the sky is subtracted
uniform float uGalaxyGamma;   // compresses the plate's dynamic range onto visual surface brightness
uniform float uZodiacalPeak;  // cd/m^2 at the ecliptic, 90 deg from the Sun
uniform float uGegenschein;   // cd/m^2 of the anti-solar Gegenschein
uniform float uTwilightGain;  // scales the synthesised twilight (weather/transparency, 1 = clear)
uniform float uDitherSeed;

const float PI = 3.14159265358979;
const float MID_STOP = 0.55;
const float HORIZON_STOP = 0.86;

// North galactic pole in ICRS: RA 192.85948 deg, Dec +27.12825 deg — the same pole
// CoordinateEngineLegacy.galacticToEquatorial uses.
const vec3 GAL_POLE = vec3(-0.05487556, -0.87343709, -0.48383502);
// Ecliptic north pole (ICRS): Dec +66.56071 deg at RA 18h.
const vec3 ECL_POLE = vec3(0.0, -0.3977772, 0.9174821);

vec3 srgbToLinear(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c));
}

vec3 linearToSrgb(vec3 c) {
    c = clamp(c, 0.0, 1.0);
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c));
}

// Kasten & Young (1989) relative airmass, valid down to the horizon.
float airmass(float altDeg) {
    float h = max(altDeg, -0.9);
    return 1.0 / (max(sin(radians(h)), -0.999) + 0.50572 * pow(h + 6.07995, -1.6364));
}

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

// Smooth, periodic, large-scale field used for the airglow and light-pollution patchiness.
float patch(float azDeg, float altDeg, float seed) {
    float a = radians(azDeg);
    float b = radians(altDeg);
    float v = sin(3.0 * a + 1.7 * b + seed) * cos(2.0 * a - 2.3 * b + 0.7 * seed)
            + 0.6 * sin(5.0 * a + 3.1 * b - 0.4 + seed * 1.3) * cos(4.0 * a + 1.9 * b);
    return 0.5 + 0.5 * (v / 1.6);
}

float acesCurve(float x) {
    float v = max(x, 0.0);
    return clamp((v * (2.51 * v + 0.03)) / (v * (2.43 * v + 0.59) + 0.14), 0.0, 1.0);
}

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

void main() {
    // Equatorial direction of this pixel. +right is west, +up is north (see SkyPanoramaMath).
    vec3 dir = normalize(uForward + vNdc.x * uTanHalf.x * uRight + vNdc.y * uTanHalf.y * uUp);

    float sinAlt = clamp(dot(dir, uZenithDir), -1.0, 1.0);
    float altDeg = degrees(asin(sinAlt));
    float azDeg = degrees(atan(dot(dir, uEastDir), dot(dir, uNorthDir)));
    if (azDeg < 0.0) azDeg += 360.0;

    // The same equatorial point in the NASA celestial plate (RA increases to the left).
    float ra = atan(dir.y, dir.x);
    float dec = asin(clamp(dir.z, -1.0, 1.0));
    vec2 uv = vec2(fract(0.5 - ra / (2.0 * PI)), 0.5 - dec / PI);

    // ---------------------------------------------------------------- galaxy
    // The plate is display-referred: decode it, remove its flat sky pedestal (a constant ~0.105 in
    // sRGB space), and keep only what the galactic band actually contains. Without the mask the
    // pedestal would paint the whole sky grey, which is what made the night look like wallpaper.
    vec3 plate = textureLod(uPanorama, uv, uLod).rgb;
    vec3 galLin = srgbToLinear(clamp((plate - vec3(0.105)) / 0.895, 0.0, 1.0));
    float galLum = clamp(luma(galLin), 0.0, 1.0);

    float galLat = degrees(asin(clamp(dot(dir, GAL_POLE), -1.0, 1.0)));
    // Confine the plate to the band the galaxy occupies (the plate's own field is wider than the
    // band because of its sky pedestal), and let the band's own brightness shape the rest.
    float band = 0.50 + 0.50 * exp(-0.5 * (galLat / 12.0) * (galLat / 12.0));
    float halo = exp(-0.5 * (galLat / 34.0) * (galLat / 34.0));

    // Colour: the bulge and inner disk are old, reddened populations; the outer arms are blue-white.
    float warm = clamp(galLum * 1.9, 0.0, 1.0) * exp(-0.5 * (galLat / 16.0) * (galLat / 16.0));
    float cool = halo * (1.0 - clamp(galLum * 3.2, 0.0, 1.0));
    vec3 galTint = mix(vec3(0.97, 0.99, 1.04), vec3(1.10, 1.00, 0.86), warm);
    galTint = mix(galTint, vec3(0.86, 0.95, 1.12), 0.45 * cool);

    float altForExtinction = max(altDeg, 0.0);
    vec3 extinction = exp(-uTau * airmass(altForExtinction));
    float aboveHorizon = smoothstep(-0.8, 2.6, altDeg);
    vec3 galaxy = uGalaxyPeak * pow(max(galLum, 0.0), uGalaxyGamma) * band * galTint;
    galaxy *= extinction * aboveHorizon;

    // ---------------------------------------------------------------- airglow
    float x = airmass(altDeg);
    float glow = uAirglow * x * pow(clamp(uZenithTz, 0.02, 1.0), x - 1.0);
    glow *= smoothstep(-1.2, 3.0, altDeg);
    glow *= 0.84 + 0.32 * patch(azDeg, altDeg, 0.0);

    // ---------------------------------------------------------------- light pollution
    // Domes concentrate within about 8 degrees of the horizon; the rest is a broad floor.
    float domes = 1.0;
    float dAz;
    dAz = abs(mod(azDeg - 28.0 + 180.0, 360.0) - 180.0); domes += 1.00 * exp(-0.5 * pow(dAz / 38.0, 2.0));
    dAz = abs(mod(azDeg - 146.0 + 180.0, 360.0) - 180.0); domes += 0.50 * exp(-0.5 * pow(dAz / 24.0, 2.0));
    dAz = abs(mod(azDeg - 262.0 + 180.0, 360.0) - 180.0); domes += 0.28 * exp(-0.5 * pow(dAz / 62.0, 2.0));
    float alt3 = max(altDeg, 0.4);
    float lp = uLightPollution * (0.40 + 2.5 * exp(-alt3 / 7.5) * (0.28 + domes));
    lp *= 0.85 + 0.30 * patch(azDeg, altDeg, 3.1);
    lp *= smoothstep(-2.2, 2.2, altDeg);

    // ---------------------------------------------------------------- zodiacal light
    vec3 sunDir = normalize(cos(radians(uSunAltDeg)) * cos(radians(uSunAzDeg)) * uNorthDir
                          + cos(radians(uSunAltDeg)) * sin(radians(uSunAzDeg)) * uEastDir
                          + sin(radians(uSunAltDeg)) * uZenithDir);
    vec3 moonDir = normalize(cos(radians(uMoonAltDeg)) * cos(radians(uMoonAzDeg)) * uNorthDir
                           + cos(radians(uMoonAltDeg)) * sin(radians(uMoonAzDeg)) * uEastDir
                           + sin(radians(uMoonAltDeg)) * uZenithDir);
    float sunAngle = degrees(acos(clamp(dot(dir, sunDir), -1.0, 1.0)));
    float eclLat = degrees(asin(clamp(dot(dir, ECL_POLE), -1.0, 1.0)));
    float zl = uZodiacalPeak * exp(-0.5 * pow(eclLat / 9.0, 2.0))
             * (0.25 + 0.75 * smoothstep(25.0, 100.0, sunAngle));
    zl += uGegenschein * exp(-0.5 * pow(eclLat / 5.5, 2.0))
        * exp(-0.5 * pow((sunAngle - 180.0) / 17.0, 2.0));
    vec3 zodiacal = zl * extinction * smoothstep(-1.2, 2.5, altDeg) * vec3(1.0, 0.95, 0.86);

    // ---------------------------------------------------------------- moonlight
    // Krisciunas & Schaefer (1991) eq. 15: f(rho) = 10^5.36 (1.06 + cos^2 rho) + 10^6.15 e^(-rho/40),
    // normalised so f(0) = 1, then scaled by uMoonSky (which already carries the illuminated
    // fraction and the extinction on the Moon's own path).
    float rho = degrees(acos(clamp(dot(dir, moonDir), -1.0, 1.0)));
    float cr = cos(radians(rho));
    float fRho = 1.0e5 * (1.06 + cr * cr) + 1.0e6 * exp(-rho / 40.0);
    float fZero = 1.0e5 * 2.06 + 1.0e6;
    vec3 moon = uMoonSky * (fRho / fZero) * extinction * (1.0 - exp(-x * 1.2));
    moon *= smoothstep(-1.2, 3.0, altDeg) * vec3(0.82, 0.95, 1.28);

    // ---------------------------------------------------------------- twilight
    // Single-scattered sunlight above the Earth's shadow. The zenith term falls a factor of 10 per
    // 2.5 degrees of solar depression (the classic twilight law), the horizon term is the long air
    // path to the Sun's side (Henyey-Greenstein, g = 0.72, plus Rayleigh).
    float depression = max(-uSunAltDeg, 0.0);
    float twZenith = pow(10.0, -0.40 * (depression - 6.0));
    twZenith *= smoothstep(-18.0, -5.0, uSunAltDeg);
    const float G = 0.72;
    float hg = (1.0 - G * G) / (4.0 * PI * pow(1.0 + G * G - 2.0 * G * cos(radians(sunAngle)), 1.5));
    float hg0 = (1.0 - G * G) / (4.0 * PI * pow(1.0 + G * G - 2.0 * G, 1.5));
    float rayleigh = 0.75 * (1.0 + cos(radians(sunAngle)) * cos(radians(sunAngle))) / 1.5;
    float twDome = exp(max(altDeg, -9.0) / 12.0);
    float tw = twZenith * twDome * (0.22 * rayleigh + 1.25 * hg / hg0) * uTwilightGain;
    // Warm scattering close to the Sun and low in the sky, cool Rayleigh light elsewhere.
    float warmTw = exp(-0.5 * pow(sunAngle / 38.0, 2.0)) * exp(-0.5 * pow(max(altDeg, 0.0) / 22.0, 2.0));
    vec3 twilight = tw * mix(vec3(0.50, 0.68, 1.0), vec3(1.35, 0.62, 0.30), warmTw);
    // The Earth's shadow: the dark band opposite the Sun, just above the horizon.
    float antiSolar = exp(-0.5 * pow((180.0 - sunAngle) / 45.0, 2.0))
                    * exp(-0.5 * pow((altDeg - 5.0) / 9.0, 2.0));
    twilight *= 1.0 - 0.6 * antiSolar * (1.0 - smoothstep(-10.0, -2.0, uSunAltDeg));

    // ---------------------------------------------------------------- sum
    vec3 night = galaxy
               + glow * vec3(0.92, 1.05, 0.88)
               + lp * vec3(1.14, 0.96, 0.78)
               + zodiacal
               + moon
               + twilight;

    // Exposure, ACES curve and the Purkinje shift, exactly as SkyRenderModel.toneMap does them, so
    // the panorama and the AR sky respond to the night the same way.
    vec3 c = max(night, vec3(0.0)) * uExposure;
    float y = luma(c);
    vec3 o = vec3(0.0);
    if (y > 1e-12) {
        float yd = acesCurve(y);
        c *= yd / y;
        float m = max(c.r, max(c.g, c.b));
        if (m > 1.0) {
            float sat = clamp((1.0 - yd) / max(m - yd, 1e-9), 0.0, 1.0);
            c = vec3(yd) + (c - vec3(yd)) * sat;
        }
        o = clamp(c, 0.0, 1.0);
    }
    // Display-referred gradations the config exposes, applied in the same space as before.
    float l = luma(o);
    o = mix(vec3(l), o, uSaturation);
    o = (o - 0.5) * uContrast + 0.5;
    o *= uVisibility;
    vec3 nightSrgb = linearToSrgb(clamp(o, 0.0, 1.0));

    // ---------------------------------------------------------------- ground
    // Below the horizon the plate has no business being sky: dim it hard, so the bottom of the
    // frame reads as dark ground wherever no landscape is drawn over it.
    float ground = smoothstep(-1.5, 0.5, altDeg);
    nightSrgb *= mix(0.06, 1.0, ground);

    // ---------------------------------------------------------------- daytime palette
    // The same vertical stops as the Compose Real Sky gradient (top = zenith).
    float t = 0.5 - 0.5 * vNdc.y;
    vec3 day = t < MID_STOP
        ? mix(uSkyZenith, uSkyMid, t / MID_STOP)
        : (t < HORIZON_STOP
            ? mix(uSkyMid, uSkyHorizon, (t - MID_STOP) / (HORIZON_STOP - MID_STOP))
            : uSkyHorizon);
    vec3 sky = mix(day, nightSrgb, uNightSkyWeight);

    // Triangular dither against banding in the very dark gradients.
    float n1 = hash11(dot(gl_FragCoord.xy + uDitherSeed, vec2(12.9898, 78.233)));
    float n2 = hash11(dot(gl_FragCoord.yx + uDitherSeed * 1.7, vec2(39.3468, 11.135)));
    sky += (n1 + n2 - 1.0) / 255.0;

    fragColor = vec4(clamp(sky, 0.0, 1.0), 1.0);
}
"""
}
