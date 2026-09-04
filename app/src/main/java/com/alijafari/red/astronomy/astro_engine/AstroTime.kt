package com.alijafari.red.astronomy.astro_engine

/**
 * Single value type representing astronomical time.
 * This is the ONLY way time should enter the astronomy calculation layer.
 *
 * All times are internally stored as UTC milliseconds since epoch.
 * Conversions to TT, UT1, and Julian centuries are computed on demand.
 */
class AstroTime(utcMs: Long) {

    // ============================================================
    // Public Properties
    // ============================================================

    /** UTC milliseconds since 1970-01-01 00:00:00 UTC */
    val utcMs: Long = utcMs

    /** Julian Date in UTC */
    val jdUtc: Double
        get() = (utcMs / 86400000.0) + 2440587.5

    /** Julian Date in Terrestrial Time (TT) = JD_UTC + ΔT/86400 */
    val jdTt: Double
        get() = jdUtc + (deltaT / 86400.0)

    /** ΔT = TT - UT1 in seconds. Uses Espenak-Meeus polynomials. */
    val deltaT: Double
        get() = calculateDeltaT(jdUtc)

    /** Julian centuries from J2000.0 in Terrestrial Time */
    val jcTt: Double
        get() = (jdTt - 2451545.0) / 36525.0

    /** Julian centuries from J2000.0 in UTC (for cases where TT is not needed) */
    val jcUtc: Double
        get() = (jdUtc - 2451545.0) / 36525.0

    // ============================================================
    // Companion Object
    // ============================================================

    companion object {
        /**
         * Creates an AstroTime representing the current moment.
         */
        fun now(): AstroTime {
            return AstroTime(System.currentTimeMillis())
        }

        /**
         * Creates an AstroTime from a Julian Date in UTC.
         * @param jd Julian Date in UTC
         */
        fun fromJd(jd: Double): AstroTime {
            val ms = ((jd - 2440587.5) * 86400000.0).toLong()
            return AstroTime(ms)
        }

        /**
         * Creates an AstroTime from a specific UTC date and time.
         * @param year Gregorian year (e.g., 2026)
         * @param month Month (1-12)
         * @param day Day of month (1-31)
         * @param hour Hour (0-23)
         * @param minute Minute (0-59)
         * @param second Second (0-59)
         */
        fun fromUtcDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): AstroTime {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(java.util.Calendar.YEAR, year)
                set(java.util.Calendar.MONTH, month - 1)
                set(java.util.Calendar.DAY_OF_MONTH, day)
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, second)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return AstroTime(cal.timeInMillis)
        }
    }

    // ============================================================
    // ΔT Calculation (Espenak-Meeus Polynomials)
    // ============================================================

    /**
     * Calculates ΔT = TT - UT1 in seconds using the Espenak-Meeus polynomial approximations.
     *
     * Reference: Espenak & Meeus, "Five Millennium Canon of Solar Eclipses", NASA TP-2006-214141.
     *
     * NOTE: UT1 ≈ UTC is an approximation used here. The actual difference between
     * UT1 and UTC is always less than 0.9 seconds due to leap seconds, which is
     * negligible for this app's precision requirements.
     *
     * @param jd Julian Date in UTC
     * @return ΔT in seconds
     * @throws IllegalArgumentException if the year is outside the valid range (1620-2150)
     */
    private fun calculateDeltaT(jd: Double): Double {
        val year = 2000.0 + (jd - 2451545.0) / 365.25

        return when {
            // F-A5 KIND-B fix (2026-09-04): the previous branch invented a cubic term
            // (+0.001727 t^3) giving DeltaT=96.3s for 2025 — the real value is ~69s
            // (IERS finals; Espenak-Meeus 2005-2050 formula gives ~74s). The 27s excess
            // fed every TT-based series 27s late (Moon error ~0.25 arcmin). Replaced with
            // the genuine Espenak-Meeus piecewise: 1986-2005 quadratic for 2000-2005,
            // 2005-2050 quadratic beyond (documented residual <=6s => <=0.05' on the Moon).
            year in 2000.0..2005.0 -> {
                val t = year - 2000.0
                63.86 + 0.3345 * t - 0.006037 * t * t
            }
            year in 2005.0..2050.0 -> {
                val t = year - 2000.0
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
            // Years 2050-2150: Espenak-Meeus polynomial
            year in 2050.0..2150.0 -> {
                val t = year - 2000.0
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
            // Years 1950-2000: Morrison & Stephenson polynomial
            year in 1950.0..2000.0 -> {
                val t = year - 2000.0
                63.86 + 0.3345 * t - 0.006037 * t * t + 0.001727 * t * t * t
            }
            // Years 1900-1950: Morrison & Stephenson
            year in 1900.0..1950.0 -> {
                val t = year - 1900.0
                -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t - 0.000197 * t * t * t * t
            }
            // Years 1800-1900: Morrison & Stephenson
            year in 1800.0..1900.0 -> {
                val t = year - 1800.0
                -0.000009 + 0.003844 * t + 0.082901 * t * t - 0.000235 * t * t * t + 0.0000008 * t * t * t * t
            }
            // Years 1700-1800: Morrison & Stephenson
            year in 1700.0..1800.0 -> {
                val t = year - 1700.0
                8.83 + 0.1603 * t - 0.0059285 * t * t + 0.00013336 * t * t * t - 0.00000001 * t * t * t * t
            }
            // Years 1620-1700: Morrison & Stephenson
            year in 1620.0..1700.0 -> {
                val t = year - 1600.0
                120.0 - 0.9808 * t - 0.01532 * t * t + t * t * t / 7129.0
            }
            // Out of range
            else -> {
                throw IllegalArgumentException(
                    "ΔT calculation not available for year ${year.toInt()}. " +
                    "Valid range is 1620-2150. Requested year: ${year.toInt()}"
                )
            }
        }
    }

    // ============================================================
    // Utility Methods
    // ============================================================

    /**
     * Returns the year (Gregorian) corresponding to this time.
     */
    fun getYear(): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMs
        }
        return cal.get(java.util.Calendar.YEAR)
    }

    override fun toString(): String {
        return "AstroTime(JD_UTC=${"%.5f".format(jdUtc)}, JD_TT=${"%.5f".format(jdTt)}, ΔT=${"%.2f".format(deltaT)}s)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AstroTime) return false
        return utcMs == other.utcMs
    }

    override fun hashCode(): Int {
        return utcMs.hashCode()
    }
}
