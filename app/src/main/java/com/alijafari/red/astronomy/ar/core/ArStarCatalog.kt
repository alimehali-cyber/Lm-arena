package com.alijafari.red.astronomy.ar.core

import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Yale Bright Star Catalogue, 5th revised edition (Hoffleit & Warren 1991), all stars to V = 6.5,
 * as packed by `tools/ar-sky-assets/build_ar_assets.py` (asset `ar/stars_bsc5.bin`).
 * Positions are ICRS/J2000.0 with FK5 proper motions (μα* = μα·cos δ, arcsec/yr).
 */
class ArStarCatalog(
    val hr: IntArray,
    val hd: IntArray,
    val raDeg: DoubleArray,
    val decDeg: DoubleArray,
    val pmRaCosDec: DoubleArray,
    val pmDec: DoubleArray,
    val vmag: FloatArray,
    /** B−V colour index, NaN when unknown (then [spectral] gives a fallback). */
    val bv: FloatArray,
    val spectral: CharArray,
    /** IAU / common proper names keyed by HR number. */
    val names: Map<Int, String>
) {
    /**
     * Number of stars that can be read from **every** parallel array, i.e. the length of the
     * shortest column.
     *
     * These arrays are always indexed together — a star's position, proper motion, magnitude,
     * colour and spectral class — so the usable catalogue is only as long as its shortest column.
     * Taking the length from `hr` alone was a crash: a pack with no colour column (or one that was
     * truncated) was read past the end of `bv`/`spectral`, which is exactly
     * `ArrayIndexOutOfBoundsException: length=0; index=0` when the star pass asked for star 0's
     * B−V. Every consumer that iterates stars uses this count, so no reader can ask for a star
     * that any of the arrays cannot answer for.
     */
    val size: Int = minOf(
        hr.size, hd.size, raDeg.size, decDeg.size, pmRaCosDec.size, pmDec.size,
        vmag.size, bv.size, spectral.size
    )

    private val hrIndex: Map<Int, Int> by lazy {
        hr.withIndex().take(size).associate { it.value to it.index }
    }
    private val hdIndex: Map<Int, Int> by lazy {
        hd.withIndex().take(size).filter { it.value > 0 }.associate { it.value to it.index }
    }

    fun indexOfHr(hrNumber: Int): Int = hrIndex[hrNumber] ?: -1
    fun indexOfHd(hdNumber: Int): Int = hdIndex[hdNumber] ?: -1

    /** Colour index to use for display (catalogue B−V or a spectral-class estimate). */
    fun colorIndex(i: Int): Double {
        if (i !in 0 until size) return SOLAR_BV
        val v = bv[i]
        return if (v.isNaN()) SkyRenderModel.spectralClassToBv(spectral[i]) else v.toDouble()
    }

    fun name(i: Int): String? = if (i in 0 until size) names[hr[i]] else null

    companion object {
        /** B−V of a solar-type star: what a star with no colour data is drawn as. */
        const val SOLAR_BV = 0.6

        fun parse(stars: InputStream, names: InputStream? = null): ArStarCatalog {
            val din = DataInputStream(stars.buffered())
            val magic = ByteArray(4)
            din.readFully(magic)
            require(String(magic, Charsets.US_ASCII) == "BSC5") { "Not a BSC5 star pack" }
            val version = din.readUnsignedShort()
            require(version == 1) { "Unsupported star pack version $version" }
            val n = din.readInt()
            require(n in 1..20000) { "Implausible star count $n" }
            val hr = IntArray(n); val hd = IntArray(n)
            val ra = DoubleArray(n); val dec = DoubleArray(n)
            val pmra = DoubleArray(n); val pmde = DoubleArray(n)
            val v = FloatArray(n); val bv = FloatArray(n); val sp = CharArray(n)
            for (i in 0 until n) {
                hr[i] = din.readShort().toInt()
                hd[i] = din.readInt()
                ra[i] = din.readFloat().toDouble()
                dec[i] = din.readFloat().toDouble()
                pmra[i] = din.readFloat().toDouble()
                pmde[i] = din.readFloat().toDouble()
                v[i] = din.readShort() / 100f
                val b = din.readShort().toInt()
                bv[i] = if (b == 32767) Float.NaN else b / 1000f
                sp[i] = din.readUnsignedByte().toChar()
            }
            val nameMap = HashMap<Int, String>()
            if (names != null) {
                BufferedReader(InputStreamReader(names, Charsets.UTF_8)).useLines { lines ->
                    lines.forEach { line ->
                        val bar = line.indexOf('|')
                        if (bar > 0) line.substring(0, bar).trim().toIntOrNull()?.let { nameMap[it] = line.substring(bar + 1).trim() }
                    }
                }
            }
            return ArStarCatalog(hr, hd, ra, dec, pmra, pmde, v, bv, sp, nameMap)
        }
    }
}

/** One constellation figure: line segments between star indices of [ArStarCatalog]. */
class ArConstellation(
    val code: String,
    val nameEn: String,
    val nameFa: String,
    val labelRaDeg: Double,
    val labelDecDeg: Double,
    /** 1 = most prominent … 3 = faint (d3-celestial ranking). */
    val rank: Int,
    /** Flattened pairs (a0, b0, a1, b1, …) of star indices. */
    val segments: IntArray
)

object ArConstellationCatalog {
    /** Parses `ar/constellations.txt` (format documented in the asset builder). */
    fun parse(input: InputStream, starCount: Int): List<ArConstellation> {
        val heads = LinkedHashMap<String, Array<String>>()
        val lines = HashMap<String, IntArray>()
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { seq ->
            seq.forEach { raw ->
                val parts = raw.split('|')
                when {
                    parts.size >= 7 && parts[0] == "C" -> heads[parts[1]] = parts.toTypedArray()
                    parts.size >= 3 && parts[0] == "L" -> {
                        val segs = parts[2].split(';').filter { it.isNotBlank() }.flatMap { pair ->
                            val ab = pair.split(',')
                            listOf(ab[0].trim().toInt(), ab[1].trim().toInt())
                        }
                        require(segs.all { it in 0 until starCount }) { "Constellation ${parts[1]} references a missing star" }
                        // Serpens is split into Caput and Cauda: both figures share the code "Ser".
                        lines[parts[1]] = (lines[parts[1]] ?: IntArray(0)) + segs.toIntArray()
                    }
                }
            }
        }
        return heads.map { (code, p) ->
            ArConstellation(
                code = code,
                nameEn = p[2],
                nameFa = p[3],
                labelRaDeg = p[4].toDouble(),
                labelDecDeg = p[5].toDouble(),
                rank = p[6].trim().toIntOrNull() ?: 3,
                segments = lines[code] ?: IntArray(0)
            )
        }
    }
}
