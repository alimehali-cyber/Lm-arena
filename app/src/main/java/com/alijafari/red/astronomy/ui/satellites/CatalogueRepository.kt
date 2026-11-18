package com.alijafari.red.astronomy.ui.satellites

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.*
import com.alijafari.red.astronomy.ui.satellites.core.*
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

private object LibrarySerializer : Serializer<Library> {
    override val defaultValue = Library()

    override suspend fun readFrom(input: InputStream) =
        try {
            LibraryCodec.read(input)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Invalid satellite library", e)
        }

    override suspend fun writeTo(t: Library, output: OutputStream) = LibraryCodec.write(t, output)
}

internal val Context.satelliteLibrary by dataStore("satellite-library.v1", LibrarySerializer)
internal val SatCategory.label: Int
    get() =
        when (this) {
            SatCategory.STATION -> R.string.sat_station
            SatCategory.SCIENCE -> R.string.sat_science
            SatCategory.STARLINK -> R.string.sat_starlink
            SatCategory.OTHER_CONSTELLATION -> R.string.sat_constellations
            SatCategory.NAVIGATION -> R.string.sat_navigation
            SatCategory.WEATHER_EO -> R.string.sat_weather
            SatCategory.COMMS -> R.string.sat_comms
            SatCategory.AMATEUR_CUBESAT -> R.string.sat_amateur
            SatCategory.DEBRIS -> R.string.sat_debris
            SatCategory.OTHER -> R.string.sat_other
        }

internal fun SatelliteItem.craft(): Craft {
    val revs = defaultTle.line2.getOrNull(62)?.let { defaultTle.line2.substring(52, 63).trim().toDoubleOrNull() }
    val orbit =
        revs
            ?.takeIf { it > 0 }
            ?.let { n ->
                val meanHeight = (398600.4418 * (86400 / (2 * Math.PI * n)).pow(2)).pow(1.0 / 3) - 6371
                when {
                    meanHeight < 2000 -> Orbit.LEO
                    n in .99..1.01 -> Orbit.GEO
                    meanHeight < 35786 -> Orbit.MEO
                    else -> null
                }
            }
    return Craft(
        id,
        noradId,
        nameEn,
        nameFa,
        designation,
        taxonomy,
        isNakedEyeCandidate,
        orbit,
        listOf(defaultTle.name) + if (noradId == 25544) listOf("Zarya", "زاریا") else emptyList(),
    )
}

internal data class SatRow(
    val item: SatelliteItem,
    val craft: Craft,
    val elevation: Double? = null,
    val brightness: Double? = null,
    val next: Long? = null,
)

internal class CatalogueRepository(items: List<SatelliteItem>) {
    val rows = items.distinctBy { it.id }.map { SatRow(it, it.craft()) }
    private val index = SearchIndex(rows.map { it.craft })

    fun rows(
        query: String,
        library: Library,
        angles: Map<String, Double>,
        passes: Map<String, Long>,
        fa: Boolean,
        magnitudes: Map<String, Double> = emptyMap(),
    ): Flow<List<SatRow>> = flow {
        val matches = if (query.isBlank()) null else index.search(query).mapTo(HashSet()) { it.id }
        val filtered = rows.mapNotNull { row ->
            val id = row.craft.id
            val angle = angles[id]
            if (matches?.contains(id) ?: library.accepts(library.active, row.craft, (angle ?: -90.0) > 0))
                row.copy(elevation = angle, brightness = magnitudes[id], next = passes[id])
            else null
        }
        val name = compareBy<SatRow> { if (fa) it.craft.persian else it.craft.name }
        val sort =
            when (library.sort) {
                SatSort.NAME -> name
                SatSort.HORIZON -> compareByDescending<SatRow> { (it.elevation ?: -90.0) > 0 }.then(name)
                SatSort.ANGLE -> compareByDescending<SatRow> { it.elevation ?: -91.0 }.then(name)
                SatSort.NEXT_PASS -> compareBy<SatRow> { it.next ?: Long.MAX_VALUE }.then(name)
                SatSort.BRIGHTNESS -> compareBy<SatRow> { it.brightness ?: Double.MAX_VALUE }.then(name)
            }
        emit(filtered.sortedWith(compareBy<SatRow> { it.craft.category }.then(sort)))
    }
        .flowOn(Dispatchers.Default)
}
