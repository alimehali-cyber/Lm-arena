package com.alijafari.red.astronomy.ui.satellites.core

import java.io.*

/** Versioned, length-prefixed DataStore payload. Names may contain punctuation, emoji or ZWNJ. */
internal object LibraryCodec {
    fun write(lib: Library, stream: OutputStream) =
        with(DataOutputStream(stream)) {
            fun strings(v: Collection<String>) {
                writeInt(v.size)
                v.forEach(::writeUTF)
            }
            writeInt(1)
            strings(lib.order)
            strings(lib.hidden)
            strings(lib.favorites)
            writeUTF(lib.active)
            writeUTF(lib.sort.name)
            strings(lib.recent)
            writeInt(lib.custom.size)
            for (c in lib.custom) {
                writeUTF(c.id)
                writeUTF(c.name)
                writeInt(c.glyph)
                strings(c.members)
                writeBoolean(c.rules != null)
                c.rules?.let {
                    strings(it.categories.map(SatCategory::name))
                    writeUTF(it.nameContains)
                    writeUTF(it.orbit?.name.orEmpty())
                    writeBoolean(it.nakedEyeOnly)
                }
            }
            flush()
        }

    fun read(stream: InputStream): Library =
        with(DataInputStream(stream)) {
            fun count() = readInt().also { require(it in 0..1_000_000) }
            fun strings() = List(count()) { readUTF() }
            require(readInt() == 1)
            val order = strings()
            val hidden = strings().toSet()
            val favorites = strings().toSet()
            val active = readUTF()
            val sort = SatSort.valueOf(readUTF())
            val recent = strings()
            val custom =
                List(count()) {
                    val id = readUTF()
                    val name = readUTF()
                    val glyph = readInt()
                    val members = strings().toSet()
                    val rules =
                        if (readBoolean())
                            Rules(
                                strings().map(SatCategory::valueOf).toSet(),
                                readUTF(),
                                readUTF().takeIf(String::isNotEmpty)?.let(Orbit::valueOf),
                                readBoolean(),
                            )
                        else null
                    CollectionDef(id, name, glyph, members, rules)
                }
            Library(custom, order, hidden, favorites, active, sort, recent)
        }
}
