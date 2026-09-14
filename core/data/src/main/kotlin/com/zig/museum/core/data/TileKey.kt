package com.zig.museum.core.data

/**
 * TileKey per §7.1
 * objectId: pack id (e.g. "moon")
 * level: 0 = finest/native, increasing = coarser (matches assetkit L0 native)
 * x,y: tile coordinates at that level
 *
 * For M2 pyramid: tiles/L<level>/<x>_<y>.ktx2 where level 0 is native.
 * For M3 we keep same convention.
 */
data class TileKey(
    val objectId: String,
    val level: Int,
    val x: Int,
    val y: Int
) {
    init {
        require(level >= 0) { "level must be >=0" }
        require(x >= 0 && y >= 0) { "x,y must be >=0" }
    }

    fun toPath(): String = "tiles/L$level/${x}_${y}.ktx2"

    companion object {
        fun fromPath(objectId: String, path: String): TileKey? {
            // path like tiles/L0/0_1.ktx2
            val regex = Regex("""tiles/L(\d+)/(\d+)_(\d+)\.ktx2""")
            val m = regex.find(path) ?: return null
            return TileKey(
                objectId = objectId,
                level = m.groupValues[1].toInt(),
                x = m.groupValues[2].toInt(),
                y = m.groupValues[3].toInt()
            )
        }
    }
}
