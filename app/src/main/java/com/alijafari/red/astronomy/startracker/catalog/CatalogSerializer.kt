package com.alijafari.red.astronomy.startracker.catalog

import java.io.*

/**
 * Simple binary format for built catalog + index, suitable for shipping as Android asset.
 * Explicitly NOT Kotlin source-code list literals (which limits StarCatalog.kt 43 stars, won't scale to thousands).
 *
 * Format (binary, big-endian via DataOutputStream):
 * - Magic: 4 bytes 0x5354524B = "STRK" ASCII
 * - Version: int (1)
 * - Star count: int
 * - For each star:
 *   - id length: short (unsigned, max 65535)
 *   - id bytes: UTF-8
 *   - raRad: double
 *   - decRad: double
 *   - magnitude: float (or double, we use double for precision)
 *   - sourceCatalog length: short
 *   - sourceCatalog bytes: UTF-8
 * - Pair index count: int
 * - For each pair:
 *   - separationRad: double
 *   - starIndex1: int
 *   - starIndex2: int
 * - Quad index count: int
 * - For each quad:
 *   - starIndex0,1,2,3: int each
 *   - maxSeparationRad: double
 *   - 5 ratios: double each
 *   - quantizedKey length: short
 *   - quantizedKey bytes: UTF-8
 *
 * This is simple, no heavy dependencies, suitable for Android asset.
 */
object CatalogSerializer {

    private const val MAGIC: Int = 0x5354524B // "STRK"
    private const val VERSION: Int = 1

    data class SerializedCatalog(
        val stars: List<CatalogStar>,
        val pairs: List<SeparationPair>,
        val quads: List<CatalogQuad>
    )

    fun serialize(
        stars: List<CatalogStar>,
        pairs: List<SeparationPair>,
        quads: List<CatalogQuad>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeInt(MAGIC)
        dos.writeInt(VERSION)

        // Stars
        dos.writeInt(stars.size)
        for (star in stars) {
            writeString(dos, star.id)
            dos.writeDouble(star.raRad)
            dos.writeDouble(star.decRad)
            dos.writeDouble(star.magnitude)
            writeString(dos, star.sourceCatalog)
        }

        // Pairs
        dos.writeInt(pairs.size)
        for (pair in pairs) {
            dos.writeDouble(pair.separationRad)
            dos.writeInt(pair.starIndex1)
            dos.writeInt(pair.starIndex2)
        }

        // Quads
        dos.writeInt(quads.size)
        for (quad in quads) {
            // 4 indices
            for (idx in quad.starIndices) {
                dos.writeInt(idx)
            }
            dos.writeDouble(quad.descriptor.maxSeparationRad)
            for (ratio in quad.descriptor.ratios) {
                dos.writeDouble(ratio)
            }
            writeString(dos, quad.quantizedKey)
        }

        dos.flush()
        return baos.toByteArray()
    }

    fun deserialize(bytes: ByteArray): SerializedCatalog {
        val bais = ByteArrayInputStream(bytes)
        val dis = DataInputStream(bais)

        val magic = dis.readInt()
        if (magic != MAGIC) {
            throw IOException("Invalid magic: expected ${MAGIC.toString(16)}, got ${magic.toString(16)}")
        }

        val version = dis.readInt()
        if (version != VERSION) {
            throw IOException("Unsupported version: $version, expected $VERSION")
        }

        // Stars
        val starCount = dis.readInt()
        val stars = mutableListOf<CatalogStar>()
        for (i in 0 until starCount) {
            val id = readString(dis)
            val raRad = dis.readDouble()
            val decRad = dis.readDouble()
            val magnitude = dis.readDouble()
            val sourceCatalog = readString(dis)
            stars.add(CatalogStar(id, raRad, decRad, magnitude, sourceCatalog))
        }

        // Pairs
        val pairCount = dis.readInt()
        val pairs = mutableListOf<SeparationPair>()
        for (i in 0 until pairCount) {
            val sep = dis.readDouble()
            val idx1 = dis.readInt()
            val idx2 = dis.readInt()
            val id1 = if (idx1 in stars.indices) stars[idx1].id else "UNKNOWN"
            val id2 = if (idx2 in stars.indices) stars[idx2].id else "UNKNOWN"
            pairs.add(SeparationPair(sep, id1, id2, idx1, idx2))
        }

        // Quads
        val quadCount = dis.readInt()
        val quads = mutableListOf<CatalogQuad>()
        for (i in 0 until quadCount) {
            val indices = mutableListOf<Int>()
            for (j in 0 until 4) {
                indices.add(dis.readInt())
            }
            val maxSep = dis.readDouble()
            val ratios = mutableListOf<Double>()
            for (j in 0 until 5) {
                ratios.add(dis.readDouble())
            }
            val key = readString(dis)

            val ids = indices.map { idx -> if (idx in stars.indices) stars[idx].id else "UNKNOWN" }
            val descriptor = QuadDescriptor(
                ratios = ratios,
                maxSeparationRad = maxSep,
                starIndices = indices,
                starIds = ids
            )

            quads.add(
                CatalogQuad(
                    starIndices = indices,
                    starIds = ids,
                    descriptor = descriptor,
                    quantizedKey = key
                )
            )
        }

        return SerializedCatalog(stars, pairs, quads)
    }

    private fun writeString(dos: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        if (bytes.size > 65535) throw IOException("String too long: ${s.length}")
        dos.writeShort(bytes.size)
        dos.write(bytes)
    }

    private fun readString(dis: DataInputStream): String {
        val len = dis.readUnsignedShort()
        val bytes = ByteArray(len)
        dis.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Estimate file size for real catalog extrapolation.
     * Given fixture file size and fixture counts, extrapolate to 9k-15k stars.
     * Shows arithmetic, not just assertion.
     */
    fun extrapolateFileSize(
        fixtureStars: Int,
        fixturePairs: Int,
        fixtureQuads: Int,
        fixtureBytes: Int,
        targetStars: Int
    ): ExtrapolationResult {
        // Pair count scales ~ O(N^2) but limited by maxSeparation cutoff.
        // For uniform sky distribution, number of pairs within 40° is roughly proportional to N * (density * area)
        // Density = N / (4π steradians), area within 40° = 2π*(1-cos(40°)) ≈ 1.46 sr
        // So pairs ≈ N * (density * area) /2 = N * (N/(4π) * 1.46)/2 = N^2 *1.46/(8π) ≈ N^2 *0.058
        // For N=15, pairs predicted ~15^2*0.058=13, actual may differ due to test fixture clustering
        // For extrapolation, we use simple scaling: pairs ∝ N^2, quads ∝ N^4 but limited by nearby search

        // More conservative: assume pairs ∝ N^2, quads ∝ N^2 * avg_nearby^2 (since we only combine nearby)
        // For simplicity in this phase, we extrapolate using observed fixture ratio and N^2 scaling for pairs,
        // and for quads we assume scaling ~ N * (avg nearby choose 3)

        // We'll compute two estimates: optimistic (linear) and realistic (quadratic)

        val bytesPerStar = fixtureBytes.toDouble() / fixtureStars // rough
        val pairsPerStarSquared = fixturePairs.toDouble() / (fixtureStars * fixtureStars)
        val quadsPerStar = fixtureQuads.toDouble() / fixtureStars

        // For target N, estimate pairs = N^2 * pairsPerStarSquared
        val estPairs = (targetStars * targetStars * pairsPerStarSquared).toInt()
        // Quads: for real catalog, we would limit to nearby stars, so quads not full C(N,4)
        // Assume avg nearby stars per star = 50 (from config), then quads per star ≈ C(50,3)=19600, total ≈ N*19600/4
        val estQuadsNearbyLimited = (targetStars * 19600 / 4)

        // File size estimation:
        // Star entry: id ~10 bytes + 2*8 + 8 + source ~10 = ~36 bytes + overhead
        // Pair entry: 8 + 4+4 = 16 bytes
        // Quad entry: 4*4=16 + 8 + 5*8=40 + key ~20 = ~84 bytes
        val starBytes = 50 // approx per star
        val pairBytes = 16
        val quadBytes = 84

        val estTotalBytes = targetStars * starBytes + estPairs * pairBytes + estQuadsNearbyLimited * quadBytes

        return ExtrapolationResult(
            fixtureStars = fixtureStars,
            fixturePairs = fixturePairs,
            fixtureQuads = fixtureQuads,
            fixtureBytes = fixtureBytes,
            targetStars = targetStars,
            estimatedPairs = estPairs,
            estimatedQuadsNearbyLimited = estQuadsNearbyLimited,
            estimatedTotalBytes = estTotalBytes,
            estimatedTotalKB = estTotalBytes / 1024,
            estimatedTotalMB = estTotalBytes / (1024 * 1024)
        )
    }

    data class ExtrapolationResult(
        val fixtureStars: Int,
        val fixturePairs: Int,
        val fixtureQuads: Int,
        val fixtureBytes: Int,
        val targetStars: Int,
        val estimatedPairs: Int,
        val estimatedQuadsNearbyLimited: Int,
        val estimatedTotalBytes: Int,
        val estimatedTotalKB: Int,
        val estimatedTotalMB: Int
    )
}
