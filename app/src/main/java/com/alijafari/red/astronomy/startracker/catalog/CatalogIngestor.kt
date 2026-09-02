package com.alijafari.red.astronomy.startracker.catalog

/**
 * Catalog ingestion pipeline — builds machinery, not data.
 *
 * Standard input format:
 * - CSV with header: id,ra_deg,dec_deg,magnitude
 * - Columns:
 *   - id: string, unique identifier, non-empty, no commas
 *   - ra_deg: double, right ascension in decimal degrees, J2000 equatorial, range [0, 360)
 *   - dec_deg: double, declination in decimal degrees, J2000 equatorial, range [-90, +90]
 *   - magnitude: double, apparent magnitude, reasonable range [-2, 15] (Sirius -1.46 to faint limit)
 * - Units: degrees for RA/Dec, magnitude unitless
 * - Header required: first line must be exactly "id,ra_deg,dec_deg,magnitude" (case-insensitive trimmed, but we enforce exact for simplicity)
 * - Lines starting with # are comments and ignored
 * - Empty lines ignored
 * - RA/Dec interpreted as J2000 equatorial, decimal degrees
 * - Converts degrees to radians internally
 * - Validates ranges, rejects malformed rows with clear error rather than silently skipping
 */
object CatalogIngestor {

    data class ParseError(
        val lineNumber: Int,
        val lineContent: String,
        val reason: String
    ) : Exception("Line $lineNumber: $reason — content: '$lineContent'")

    /**
     * Parse CSV text into List<CatalogStar>.
     * @param csvText full CSV content as string
     * @param sourceCatalogName source catalog name to assign to all parsed stars
     * @return list of CatalogStar
     * @throws ParseError if any row malformed (fail-fast, not silent skip)
     */
    fun parse(csvText: String, sourceCatalogName: String = "CSV_IMPORT"): List<CatalogStar> {
        val lines = csvText.lines()
        if (lines.isEmpty()) {
            throw ParseError(0, "", "Empty CSV")
        }

        var headerFound = false
        var lineNumber = 0
        val stars = mutableListOf<CatalogStar>()

        for (rawLine in lines) {
            lineNumber++
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#")) continue

            if (!headerFound) {
                // Expect header
                val normalized = line.lowercase().replace(" ", "")
                if (normalized != "id,ra_deg,dec_deg,magnitude") {
                    throw ParseError(lineNumber, rawLine, "Expected header 'id,ra_deg,dec_deg,magnitude', got '$line'")
                }
                headerFound = true
                continue
            }

            // Parse data row
            val parts = line.split(",").map { it.trim() }
            if (parts.size != 4) {
                throw ParseError(lineNumber, rawLine, "Expected 4 columns, got ${parts.size}")
            }

            val id = parts[0]
            if (id.isEmpty()) {
                throw ParseError(lineNumber, rawLine, "ID must be non-empty")
            }
            if (id.contains(",")) {
                throw ParseError(lineNumber, rawLine, "ID must not contain comma")
            }

            val raDeg = parts[1].toDoubleOrNull()
                ?: throw ParseError(lineNumber, rawLine, "ra_deg must be a valid double, got '${parts[1]}'")
            val decDeg = parts[2].toDoubleOrNull()
                ?: throw ParseError(lineNumber, rawLine, "dec_deg must be a valid double, got '${parts[2]}'")
            val magnitude = parts[3].toDoubleOrNull()
                ?: throw ParseError(lineNumber, rawLine, "magnitude must be a valid double, got '${parts[3]}'")

            // Validate ranges
            if (raDeg < 0.0 || raDeg >= 360.0) {
                // Allow 360.0 exactly as 0? We reject, require [0,360)
                if (raDeg == 360.0) {
                    // Normalize 360 to 0
                } else {
                    throw ParseError(lineNumber, rawLine, "ra_deg must be in [0,360), got $raDeg")
                }
            }
            if (decDeg < -90.0 || decDeg > 90.0) {
                throw ParseError(lineNumber, rawLine, "dec_deg must be in [-90,90], got $decDeg")
            }
            if (magnitude < -5.0 || magnitude > 15.0) {
                throw ParseError(lineNumber, rawLine, "magnitude must be in [-5,15] (reasonable range), got $magnitude")
            }

            // Normalize RA 360 -> 0
            val raNorm = if (raDeg == 360.0) 0.0 else raDeg

            stars.add(
                CatalogStar.fromDegrees(
                    id = id,
                    raDeg = raNorm,
                    decDeg = decDeg,
                    magnitude = magnitude,
                    sourceCatalog = sourceCatalogName
                )
            )
        }

        if (!headerFound) {
            throw ParseError(0, "", "No header found")
        }

        return stars
    }
}
