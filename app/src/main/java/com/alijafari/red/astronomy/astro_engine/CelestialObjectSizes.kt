package com.alijafari.red.astronomy.astro_engine

import com.alijafari.red.astronomy.domain.CelestialObject
import com.alijafari.red.astronomy.domain.ObjectType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object CelestialObjectSizes {
    const val STAR_MAGNITUDE_0_SIZE_DP = 14f
    const val STAR_MAGNITUDE_1_SIZE_DP = 11f
    const val STAR_MAGNITUDE_2_SIZE_DP = 9f
    const val STAR_MAGNITUDE_3_SIZE_DP = 7f
    const val STAR_MAGNITUDE_4_SIZE_DP = 5f
    const val STAR_MAGNITUDE_5_SIZE_DP = 4f
    const val LABEL_SHOW_MAGNITUDE_THRESHOLD = 4.0f

    const val PLANET_INNER_SIZE_DP = 18f
    const val PLANET_GLOW_SIZE_DP = 48f
    const val SUN_SIZE_DP = 40f
    const val SUN_GLOW_SIZE_DP = 80f
    const val MOON_SIZE_DP = 36f

    const val DSO_GALAXY_SIZE_DP = 20f
    const val DSO_NEBULA_SIZE_DP = 22f
    const val DSO_CLUSTER_SIZE_DP = 18f

    const val SATELLITE_SIZE_DP = 14f
    const val ISS_SIZE_DP = 20f

    const val CENTER_PROXIMITY_MAX_SCALE = 2.5f
    const val CENTER_PROXIMITY_THRESHOLD_DEG = 3f
    const val CENTER_PROXIMITY_FULL_DEG = 0.5f

    /**
     * Radius in dp the AR canvas gives a star (and any other point-like glyph) at the
     * brightness end of its magnitude ramp; the existing `(4 - magnitude)` curve, exposed so the
     * point-to-shape resolver below can reuse it instead of duplicating it.
     */
    const val AR_POINT_GLYPH_MIN_RADIUS_DP = 1.2f
    const val AR_POINT_GLYPH_MAX_RADIUS_DP = 5.0f

    // -----------------------------------------------------------------------------------------
    // AR point-to-shape rendering
    //
    // A single rule shared by every object type that carries a magnitude (stars, planets, DSOs,
    // satellites, radiants). Faint objects are unresolved points of light and only reveal their
    // true shape and size once the user has zoomed in far enough for their effective on-screen
    // size to cross a size threshold; naked-eye-bright objects are shape tier at every zoom level.
    // This is the only sizing/decluttering mechanism — no per-object brightness slider and no
    // second, parallel size table.
    // -----------------------------------------------------------------------------------------

    /**
     * Naked-eye visibility limit for a typical user's sky (a suburban-to-rural horizon, not a
     * pristine dark-site limit). Anything at least this bright is always fully rendered; anything
     * fainter starts life as a dot.
     */
    const val AR_VISIBILITY_LIMIT_MAG = 4.8f

    /** Effective size (dp) at which a dot is promoted to its full glyph (and may be labelled). */
    const val AR_SHAPE_REVEAL_SIZE_DP = 6.5f

    /**
     * Effective size (dp) at which a resolved glyph falls back to a dot. Sitting below
     * [AR_SHAPE_REVEAL_SIZE_DP] it forms the hysteresis band, mirroring the arrow
     * show/hide band used by `ARCalibrationManager.computeGuidance`, so a slow zoom sweep across
     * the boundary cannot make an object flicker between tiers.
     */
    const val AR_SHAPE_RETRACT_SIZE_DP = 4.2f

    /** Diameter (dp) of an unresolved point source sitting exactly at the visibility limit. */
    const val AR_POINT_SOURCE_SIZE_DP = 3.4f

    /** Dot tier stays visually minimal: radius bounds in dp, and the alpha ramp across faintness. */
    const val AR_DOT_MIN_RADIUS_DP = 0.5f
    const val AR_DOT_MAX_RADIUS_DP = 1.5f
    const val AR_DOT_MIN_ALPHA = 0.45f
    const val AR_DOT_MAX_ALPHA = 0.95f

    enum class ArRenderTier { DOT, SHAPE }

    /**
     * Everything the AR renderer needs to know about one object for this frame: which tier it is
     * in, how far through the dot → shape transition it is, the radius its type glyph should be
     * drawn at, and the dot to cross-fade it with.
     */
    data class ArRenderPlan(
        val tier: ArRenderTier,
        /** 0 = pure dot … 1 = fully resolved type glyph. Continuous, so nothing pops. */
        val blend: Float,
        /** Radius the glyph is drawn at: [blend] eased from the dot size up to the full size. */
        val drawnRadiusPx: Float,
        val dotRadiusPx: Float,
        val dotAlpha: Float,
        /** Ambient labels are only allowed once the object is in shape tier. */
        val labelEligible: Boolean
    )

    /**
     * Gets the base diameter in Dp for a given Celestial Object.
     */
    fun getBaseSizeDp(obj: CelestialObject): Float {
        return when (obj.type) {
            ObjectType.SUN -> SUN_SIZE_DP
            ObjectType.MOON -> MOON_SIZE_DP
            ObjectType.PLANET, ObjectType.DWARF_PLANET -> PLANET_INNER_SIZE_DP
            ObjectType.SATELLITE -> if (obj.id == "sat_iss") ISS_SIZE_DP else SATELLITE_SIZE_DP
            ObjectType.DEEP_SKY -> {
                when {
                    obj.category.contains("Galaxy", ignoreCase = true) -> DSO_GALAXY_SIZE_DP
                    obj.category.contains("Nebula", ignoreCase = true) -> DSO_NEBULA_SIZE_DP
                    else -> DSO_CLUSTER_SIZE_DP
                }
            }
            ObjectType.STAR -> {
                when {
                    obj.magnitude <= 0.0 -> STAR_MAGNITUDE_0_SIZE_DP
                    obj.magnitude <= 1.0 -> STAR_MAGNITUDE_1_SIZE_DP
                    obj.magnitude <= 2.0 -> STAR_MAGNITUDE_2_SIZE_DP
                    obj.magnitude <= 3.0 -> STAR_MAGNITUDE_3_SIZE_DP
                    obj.magnitude <= 4.0 -> STAR_MAGNITUDE_4_SIZE_DP
                    else -> STAR_MAGNITUDE_5_SIZE_DP
                }
            }
            ObjectType.GALAXY, ObjectType.BLACK_HOLE -> DSO_GALAXY_SIZE_DP
            ObjectType.NEBULA -> DSO_NEBULA_SIZE_DP
            ObjectType.STAR_CLUSTER, ObjectType.GLOBULAR_CLUSTER -> DSO_CLUSTER_SIZE_DP
            ObjectType.METEOR_SHOWER -> 16f
            ObjectType.ASTERISM -> 12f
            ObjectType.CONSTELLATION -> 24f
            ObjectType.REFERENCE_POINT -> 14f
        }
    }

    /**
     * The types the AR canvas draws as a star-like point glyph (its `when (obj.type)` fall-through
     * branch) rather than as a disc, cloud, ellipse, bar or spike pattern.
     */
    fun isArPointLikeGlyph(type: ObjectType): Boolean = when (type) {
        ObjectType.STAR,
        ObjectType.ASTERISM,
        ObjectType.METEOR_SHOWER,
        ObjectType.CONSTELLATION,
        ObjectType.REFERENCE_POINT -> true
        else -> false
    }

    /**
     * The full (shape-tier) glyph radius of an object in dp — the size it is drawn at today:
     * the magnitude curve for point-like glyphs, half the type's base size otherwise.
     */
    fun getGlyphRadiusDp(obj: CelestialObject): Float =
        if (isArPointLikeGlyph(obj.type)) {
            (4.0f - obj.magnitude.toFloat()).coerceIn(
                AR_POINT_GLYPH_MIN_RADIUS_DP,
                AR_POINT_GLYPH_MAX_RADIUS_DP
            )
        } else {
            getBaseSizeDp(obj) / 2f
        }

    /**
     * Flux → apparent-diameter ratio relative to a reference magnitude. Every 5 magnitudes of
     * faintness costs a factor of 10 in flux, hence 10^(-0.2·Δm) in diameter: an object one
     * magnitude fainter than [limitMag] spans ~63% as much of the detector, three magnitudes
     * fainter ~25%.
     */
    fun arBrightnessDiameterRatio(magnitude: Double, limitMag: Float = AR_VISIBILITY_LIMIT_MAG): Float =
        10.0.pow(-0.2 * (magnitude - limitMag)).toFloat()

    /**
     * The object's effective on-screen diameter in dp, which is what decides whether a faint object
     * has been resolved. The larger of two physically motivated terms wins:
     *
     *  * the point-source term — [AR_POINT_SOURCE_SIZE_DP] scaled by brightness (fainter ⇒ smaller,
     *    see [arBrightnessDiameterRatio]) and linearly by the pinch-zoom factor, mirroring the way
     *    the projection itself spreads a point over the screen as the field of view narrows;
     *  * the true-extent term — an object with a catalogued angular size occupies
     *    `arcmin / 60 · dpPerDegree`, and `dpPerDegree` already carries zoom because it comes from
     *    the projected FOV, so extended objects resolve earlier than points of the same magnitude.
     *
     * [proximityScale] (the existing centre-proximity magnification) then scales the result, so the
     * tier decision and the drawn size are always driven by the same number and cannot disagree.
     */
    fun arEffectiveSizeDp(
        magnitude: Double,
        angularSizeArcmin: Double?,
        zoomFactor: Float,
        dpPerDegree: Float,
        proximityScale: Float = 1.0f
    ): Float {
        val zoom = max(zoomFactor, 0.01f)
        val pointSizeDp = AR_POINT_SOURCE_SIZE_DP * arBrightnessDiameterRatio(magnitude) * zoom
        val extentSizeDp = if (angularSizeArcmin != null && angularSizeArcmin > 0.0 && dpPerDegree > 0f) {
            (angularSizeArcmin / 60.0).toFloat() * dpPerDegree
        } else {
            0f
        }
        return max(pointSizeDp, extentSizeDp) * max(proximityScale, 1.0f)
    }

    /**
     * Dot-tier footprint: minimal, and only mildly responsive to zoom and brightness, so a faint
     * star reads as a point of light rather than as a shrunken glyph. Brighter members of the faint
     * range get a slightly larger, brighter dot than dimmer ones.
     */
    fun arDotRadiusDp(magnitude: Double, zoomFactor: Float): Float {
        val faintness = max(magnitude.toFloat() - AR_VISIBILITY_LIMIT_MAG, 0f)
        val brightnessBoost = 1f / (1f + 0.8f * faintness)
        val zoom = max(zoomFactor, 0.01f)
        val radius = (AR_DOT_MIN_RADIUS_DP + (AR_DOT_MAX_RADIUS_DP - AR_DOT_MIN_RADIUS_DP) * brightnessBoost) *
                (0.9f + 0.1f * zoom)
        return radius.coerceIn(AR_DOT_MIN_RADIUS_DP, AR_DOT_MAX_RADIUS_DP * 1.35f)
    }

    /** Dot-tier alpha: still visible against black, dimmer the fainter the object is. */
    fun arDotAlpha(magnitude: Double): Float {
        val faintness = max(magnitude.toFloat() - AR_VISIBILITY_LIMIT_MAG, 0f)
        val brightnessBoost = 1f / (1f + 0.8f * faintness)
        return (AR_DOT_MIN_ALPHA + (AR_DOT_MAX_ALPHA - AR_DOT_MIN_ALPHA) * brightnessBoost)
            .coerceIn(AR_DOT_MIN_ALPHA, AR_DOT_MAX_ALPHA)
    }

    /**
     * Shared dot/shape resolver used by every AR-rendered object type.
     *
     * @param previouslyShapeTier last tier this object was drawn in, for the hysteresis band. Pass
     *        the previous frame's value; the returned [ArRenderPlan.tier] is what to store back.
     *        Objects brighter than [AR_VISIBILITY_LIMIT_MAG] short-circuit to shape tier, so the
     *        Sun, the Moon and the classical planets need no special case at the call site.
     */
    fun resolveArRenderPlan(
        obj: CelestialObject,
        zoomFactor: Float,
        proximityScale: Float,
        density: Float,
        dpPerDegree: Float,
        previouslyShapeTier: Boolean = false
    ): ArRenderPlan {
        val sizeDp = arEffectiveSizeDp(
            magnitude = obj.magnitude,
            angularSizeArcmin = obj.angularSizeArcmin,
            zoomFactor = zoomFactor,
            dpPerDegree = dpPerDegree,
            proximityScale = proximityScale
        )
        val band = max(AR_SHAPE_REVEAL_SIZE_DP - AR_SHAPE_RETRACT_SIZE_DP, 0.01f)
        val alwaysShape = obj.magnitude <= AR_VISIBILITY_LIMIT_MAG
        // Continuous in size, so the cross-fade itself never pops; the discrete tier below only
        // decides label eligibility and uses the band for hysteresis. Anything at least as bright
        // as the visibility limit is fully blended in at every zoom level, which is why the Sun,
        // the Moon and the classical planets need no special case anywhere in the renderer.
        val blend = if (alwaysShape) 1f else ((sizeDp - AR_SHAPE_RETRACT_SIZE_DP) / band).coerceIn(0f, 1f)
        val tier = when {
            alwaysShape || blend >= 1f -> ArRenderTier.SHAPE
            blend <= 0f -> ArRenderTier.DOT
            else -> if (previouslyShapeTier) ArRenderTier.SHAPE else ArRenderTier.DOT
        }

        val fullGlyphRadiusPx = getGlyphRadiusDp(obj) * max(density, 0.01f) * max(proximityScale, 1.0f)
        // A dot is never drawn bigger than the shape it will grow into, which keeps the ramp below
        // monotonic for faint stars whose full glyph is itself only a pixel or two wide.
        val dotRadiusPx = min(
            arDotRadiusDp(obj.magnitude, zoomFactor) * max(density, 0.01f) * max(proximityScale, 1.0f),
            fullGlyphRadiusPx
        )
        // smoothstep on the blend keeps the growth from starting and ending abruptly.
        val eased = blend * blend * (3f - 2f * blend)
        val drawnRadiusPx = if (blend >= 1f) fullGlyphRadiusPx else max(dotRadiusPx, fullGlyphRadiusPx * eased)

        return ArRenderPlan(
            tier = tier,
            blend = blend,
            drawnRadiusPx = drawnRadiusPx,
            dotRadiusPx = dotRadiusPx,
            dotAlpha = if (blend >= 1f) 0f else arDotAlpha(obj.magnitude) * (1f - eased),
            labelEligible = tier == ArRenderTier.SHAPE
        )
    }

    /**
     * Calculates angular distance from center of screen (in degrees) and returns
     * proximity scale multiplier between 1.0f and CENTER_PROXIMITY_MAX_SCALE (2.5f).
     */
    fun calculateProximityScale(
        deltaAzDeg: Float,
        deltaAltDeg: Float
    ): Float {
        val distDeg = sqrt(deltaAzDeg * deltaAzDeg + deltaAltDeg * deltaAltDeg)
        if (distDeg >= CENTER_PROXIMITY_THRESHOLD_DEG) return 1.0f
        if (distDeg <= CENTER_PROXIMITY_FULL_DEG) return CENTER_PROXIMITY_MAX_SCALE

        val fraction = 1.0f - ((distDeg - CENTER_PROXIMITY_FULL_DEG) / (CENTER_PROXIMITY_THRESHOLD_DEG - CENTER_PROXIMITY_FULL_DEG))
        return 1.0f + fraction * (CENTER_PROXIMITY_MAX_SCALE - 1.0f)
    }
}
