package probe

import com.alijafari.red.astronomy.startracker.catalog.AngularSeparationIndex
import com.alijafari.red.astronomy.startracker.catalog.CatalogBuildConfig
import com.alijafari.red.astronomy.startracker.catalog.CatalogSerializer
import com.alijafari.red.astronomy.startracker.catalog.CatalogStar
import com.alijafari.red.astronomy.startracker.catalog.QuadPatternIndex
import java.io.DataOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.asin
import kotlin.random.Random

/**
 * B1 catalog-size probe (remediation pass 2). Measures, with the REAL index builders:
 *  - pair count + wall time via AngularSeparationIndex (CatalogBuildConfig defaults)
 *  - quad count + wall time via QuadPatternIndex (CatalogBuildConfig defaults)
 *  - serialized byte count via a counting OutputStream that writes EXACTLY the same
 *    field sequence as CatalogSerializer.serialize (validated byte-equal against
 *    serialize() at small N, because serialize() materializes the whole ByteArray
 *    and this sandbox has 3 GB RAM).
 *
 * Modes:
 *   pairs N   -> build pair index only (quad section = 0) ... feasible at 9k/15k
 *   quads N   -> build pair + quad indexes, full accounting ... feasible N <= ~800
 *   check N   -> validate the counting mirror against CatalogSerializer.serialize()
 *
 * Stars: deterministic seed 42, UNIFORM on the sphere (z uniform, phi uniform),
 * CONSTANT magnitude 3.0. Neither AngularSeparationIndex nor QuadPatternIndex applies
 * any magnitude filter (their build loops never read `magnitude`), so a constant is
 * faithful; this is stated in the evidence file.
 */
class CountingOutputStream : OutputStream() {
    var count = 0L
    override fun write(b: Int) { count += 1 }
    override fun write(b: ByteArray, off: Int, len: Int) { count += len.toLong() }
}

fun genStars(n: Int, seed: Long = 42L): List<CatalogStar> {
    val rng = Random(seed)
    return (0 until n).map { i ->
        val z = rng.nextDouble(-1.0, 1.0)
        val phi = rng.nextDouble(0.0, 2.0 * PI)
        CatalogStar.fromDegrees(
            id = "CAT$i",
            raDeg = Math.toDegrees(phi),
            decDeg = Math.toDegrees(asin(z)),
            magnitude = 3.0,
            sourceCatalog = "SYNTH_SIZE_PROBE"
        )
    }
}

fun accountBytes(stars: List<CatalogStar>, pairs: List<com.alijafari.red.astronomy.startracker.catalog.SeparationPair>, quads: List<com.alijafari.red.astronomy.startracker.catalog.CatalogQuad>): Long {
    val cos = CountingOutputStream()
    val dos = DataOutputStream(cos)
    fun wstr(s: String) {
        val b = s.toByteArray(Charsets.UTF_8)
        dos.writeShort(b.size)
        dos.write(b)
    }
    dos.writeInt(0x53544152) // MAGIC (same value as CatalogSerializer.MAGIC)
    dos.writeInt(1)          // VERSION
    dos.writeInt(stars.size)
    for (s in stars) { wstr(s.id); dos.writeDouble(s.raRad); dos.writeDouble(s.decRad); dos.writeDouble(s.magnitude); wstr(s.sourceCatalog) }
    dos.writeInt(pairs.size)
    for (p in pairs) { dos.writeDouble(p.separationRad); dos.writeInt(p.starIndex1); dos.writeInt(p.starIndex2) }
    dos.writeInt(quads.size)
    for (q in quads) {
        for (idx in q.starIndices) dos.writeInt(idx)
        dos.writeDouble(q.descriptor.maxSeparationRad)
        for (r in q.descriptor.ratios) dos.writeDouble(r)
        wstr(q.quantizedKey)
    }
    dos.flush()
    return cos.count
}

fun main(args: Array<String>) {
    val mode = args[0]
    val n = if (mode == "ingest" || mode == "cappedcsv") 0 else args[1].toInt()
    val t0 = System.currentTimeMillis()

    // mode == ingest <csvPath>: E2 real-catalog validation — parse the REAL catalog
    // through CatalogIngestor, sanity-check it, build the pair index with the REAL
    // builder, and report counts + serialized size (star+pair sections; quads need
    // the capped index from item C).
    if (mode == "ingest") {
        val path = args[1]
        val csv = java.io.File(path).readText()
        val tP = System.currentTimeMillis()
        val stars = com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor.parse(csv, "HYG_V36_LE6P5")
        val tParse = System.currentTimeMillis() - tP
        check(stars.isNotEmpty())
        val ids = stars.map { it.id }.toSet()
        check(ids.size == stars.size) { "duplicate ids" }
        val mags = stars.map { it.magnitude }
        val decs = stars.map { Math.toDegrees(it.decRad) }
        val north = decs.count { it > 0 }
        val t1 = System.currentTimeMillis()
        val pairIndex = AngularSeparationIndex(stars)
        val tPairs = System.currentTimeMillis() - t1
        val bytes = accountBytes(stars, pairIndex.pairs, emptyList())
        println(
            "ingest file=$path stars=${stars.size} parseMs=$tParse magRange=[${mags.min()},${mags.max()}] " +
            "north=$north south=${stars.size - north} pairCount=${pairIndex.pairs.size} pairBuildMs=$tPairs " +
            "starSectionBytes=${bytes - 8L - 4L - 4L - 16L * pairIndex.pairs.size} pairBytes=${16L * pairIndex.pairs.size} totalBytesNoQuads=$bytes"
        )
        return
    }

    // mode == cappedcsv <csv> <mag> <K>: C4 sweep — real catalog, capped quad build,
    // reports eligible count, quads, build time, serialized size (stars+quads, PAIRS=0
    // since the runtime solver consumes quads only; pair index is build-time scaffolding).
    if (mode == "cappedcsv") {
        val path = args[1]
        val mag = args[2].toDouble()
        val k = args[3].toInt()
        val csv = java.io.File(path).readText()
        val stars = com.alijafari.red.astronomy.startracker.catalog.CatalogIngestor.parse(csv, "HYG_V36_LE6P5")
        val t1 = System.currentTimeMillis()
        val qi = QuadPatternIndex.capped(
            stars, maxMagnitudeForQuads = mag, neighborsPerStar = k, maxQuads = Int.MAX_VALUE
        )
        val tBuild = System.currentTimeMillis() - t1
        val bytes = accountBytes(stars, emptyList(), qi.quads)
        println(
            "cappedcsv file=${java.io.File(path).name} mag<=$mag K=$k eligible=${stars.count { it.magnitude <= mag }} " +
            "quads=${qi.quads.size} hashKeys=${qi.hashTable.size} buildMs=$tBuild totalBytesStarsQuads=$bytes " +
            "bytesPerQuad=${if (qi.quads.isNotEmpty()) (bytes - 435204 - 16) / qi.quads.size else 0}"
        )
        return
    }

    val stars = genStars(n)
    val tGen = System.currentTimeMillis() - t0

    if (mode == "check") {
        val pairIndex = AngularSeparationIndex(stars)
        val quadIndex = QuadPatternIndex(stars)
        val mirror = accountBytes(stars, pairIndex.pairs, quadIndex.quads)
        val real = CatalogSerializer.serialize(stars, pairIndex.pairs, quadIndex.quads).size.toLong()
        println("N=$n mirror=$mirror real=${real} match=${mirror == real}")
        check(mirror == real) { "counting mirror does NOT match CatalogSerializer output" }
        return
    }

    val t1 = System.currentTimeMillis()
    val pairIndex = AngularSeparationIndex(stars)
    val tPairs = System.currentTimeMillis() - t1
    val pairCount = pairIndex.pairs.size

    if (mode == "pairs") {
        val bytes = accountBytes(stars, pairIndex.pairs, emptyList())
        val starBytes = bytes - 8L - 4L - 4L - 16L * pairCount
        println("N=$n genMs=$tGen pairCount=$pairCount pairBuildMs=$tPairs quads=0 totalBytes=$bytes starSectionBytes=$starBytes pairBytes=${16L * pairCount} bytesPerStarApprox=${starBytes.toDouble() / n} heapMaxMB=${Runtime.getRuntime().maxMemory() / 1048576}")
        return
    }

    // mode == quads
    val t2 = System.currentTimeMillis()
    val quadIndex = QuadPatternIndex(stars)
    val tQuads = System.currentTimeMillis() - t2
    val quadCount = quadIndex.quads.size
    val pairBytesOnly = accountBytes(stars, pairIndex.pairs, emptyList())
    val totalBytes = accountBytes(stars, pairIndex.pairs, quadIndex.quads)
    val quadBytes = totalBytes - pairBytesOnly - 4L
    println("N=$n genMs=$tGen pairCount=$pairCount pairBuildMs=$tPairs quadCount=$quadCount quadBuildMs=$tQuads totalBytes=$totalBytes quadSectionBytes=$quadBytes bytesPerQuad=${if (quadCount > 0) quadBytes.toDouble() / quadCount else 0.0} bytesPerPair=16 quadsPerStar=${quadCount.toDouble() / n} heapMaxMB=${Runtime.getRuntime().maxMemory() / 1048576}")
}
