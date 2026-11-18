package com.alijafari.red.astronomy.ui.satellites.core

import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.*

/** Also run by the Android module's JUnit suite. No Android stubs or copied implementation. */
object CoreContract {
    fun geometry() {
        for ((w, h) in listOf(360 to 640, 360 to 800, 412 to 915, 768 to 1024)) for (scale in
            listOf(1f, 1.15f, 1.3f)) for (rtl in listOf(false, true)) for (off in listOf(false, true)) {
            val a = HudLayout.measure(w.toFloat(), h.toFloat(), 24f, 96f, 150f * scale, recenter = off, rtl = rtl)
            val disc = a.frame
            val r = disc.diameter / 2
            check(disc.center.y - r >= a.top + 7.99f && disc.center.y + r <= a.bottom - 7.99f)
            if (!off) check(abs(disc.diameter - min(w * .94f, a.bottom - a.top - 16)) < .001f)
            a.recenter?.let { check(it.distanceTo(disc.center) > r) }
            for (distance in listOf(1.5f, 2.4f, 3.22f, 5f)) {
                val zoomed = a.frameAtDistance(3.22f, distance)
                val radius = zoomed.diameter / 2
                check(zoomed.center.y - radius >= a.top + .99f)
                check(zoomed.center.y + radius <= a.bottom - .99f)
                a.recenter?.let { check(it.distanceTo(zoomed.center) > radius) }
            }
            val halfTop = h - HudLayout.sheetHeight(h.toFloat(), false, false)
            val b = HudLayout.measure(w.toFloat(), h.toFloat(), 24f, 96f, 0f, halfTop)
            check(b.frame.diameter > 0 && b.frame.center.y + b.frame.diameter / 2 < halfTop)
        }
    }

    fun heading() {
        val path = Ephemeris { t -> Geo(18.0 + t * .000015, 21.0 + t * .00004, 420.0) }
        for (camera in
            listOf(Camera(0f, 0f, 3.22f), Camera(.85f, -.7f, 2f), Camera(-2.6f, .5f, 4f), Camera(2.7f, -1f, 3f))) {
            val p = GlobeProjection(GlobeFrame(Point(180f, 340f), 338f), camera)
            val a = p.project(path.at(0)).point
            val b = p.project(path.at(10000)).point
            val expected = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()))
            check(abs(MotionCues.heading(a, b, p.project(path.at(30000)).point, null, .01f)!! - expected) < 3)
        }
        val a = Point(0f, 0f)
        check(MotionCues.heading(a, a, a, 62f, .5f) == 62f)
        check(MotionCues.heading(a, a, Point(0f, 3f), 62f, .5f) == 90f)
        val p = GlobeProjection(GlobeFrame(a, 300f), Camera(0f, 0f, 3.22f))
        check(p.project(Geo(0.0, 180.0, 410.0)).occluded && !p.project(Geo(0.0, 0.0, 410.0)).occluded)
    }

    fun catalogue() {
        val iss =
            Craft(
                "iss",
                25544,
                "ISS",
                "ایستگاه فضایی بین‌المللی",
                "1998-067A",
                SatCategory.STATION,
                true,
                Orbit.LEO,
                listOf("زاریا"),
            )
        val hst =
            iss.copy(
                id = "hst",
                norad = 20580,
                name = "Hubble",
                persian = "تلسکوپ هابل",
                category = SatCategory.SCIENCE,
            )
        // A synthetic *name* suffix, not an asserted NORAD ID.
        val star =
            iss.copy(
                id = "sample",
                norad = 0,
                name = "STARLINK-341",
                persian = "استارلینک ۳۴۱",
                category = SatCategory.STARLINK,
                nakedEye = false,
            )
        val idx = SearchIndex(listOf(iss, hst.copy(aliases = emptyList()), star.copy(aliases = emptyList())))
        for ((q, id) in
            listOf(
                "ايستگاه" to "iss",
                "۲۵۵۴۴" to "iss",
                "25544" to "iss",
                "iss" to "iss",
                "زاریا" to "iss",
                "هابل" to "hst",
                "استارلینک ۳۴" to "sample",
                "بِیـن المللی" to "iss",
            )) check(idx.search(q).map { it.id } == listOf(id)) { q }
        check(Rules(setOf(SatCategory.STATION), "ايستگاه", Orbit.LEO, true).matches(iss))
        check(!Rules(nakedEyeOnly = true).matches(star))
        val custom = CollectionDef("mine", "رصد | شب", 3, rules = Rules(setOf(SatCategory.SCIENCE), "هابل"))
        val lib =
            Library(
                    custom = listOf(custom),
                    order = listOf(custom.id, Library.ALL),
                    active = custom.id,
                    sort = SatSort.NAME,
                    recent = listOf("ایستگاه"),
                )
                .toggleFavorite(iss.id)
        check(lib.accepts(custom.id, hst, false) && !lib.accepts(custom.id, iss, true))
        check(lib.delete(custom.id).active == Library.ALL)
        val bytes = ByteArrayOutputStream()
        LibraryCodec.write(lib, bytes)
        check(lib == LibraryCodec.read(bytes.toByteArray().inputStream()))
    }

    fun passesAndNumbers() {
        data class P(val t: Long, val cat: SatCategory, val score: Int = 62, val fav: Boolean = false)
        val star = (1..5).map { P(it * 60_000L, SatCategory.STARLINK, fav = it == 2) }
        val stations = star.map { it.copy(cat = SatCategory.STATION) }
        check(PassGrouping.rows(star + stations, P::cat, P::fav).map { it.size } == listOf(4, 1, 1, 1, 1, 1, 1))
        val next = star.first()
        val great = next.copy(t = next.t + 21_600_000L, score = 68)
        fun spotlight(p: P) = PassGrouping.spotlight(listOf(next, p), 0, P::t, { it.t + 360_000L }, P::score)
        check(spotlight(great) == great && spotlight(great.copy(t = great.t + 1)) == next)
        check(fmtSigned(-68.0, 0, true, suffix = "°") == "\u2066−۶۸°\u2069")
        check(fmtSigned(68.0, 0, false) == "\u206668\u2069")
        check(fmtSigned(2.5, 1, true, true) == "\u2066+۲٫۵\u2069")
        check(digits("4,56.0", true) == "۴٬۵۶٫۰")
        check(digits("عدد 2. پایان، عدد 3,400.", true) == "عدد ۲. پایان، عدد ۳٬۴۰۰.")
        check(localizedProse("قدر ۳.۸- و T-شکل", true) == "قدر \u2066−۳٫۸\u2069 و شبیه حرف «تی»")
        check(localizedProse("magnitude -3.8", false) == "magnitude -3.8")
        val tz = TimeZone.getTimeZone("UTC")
        val date =
            Calendar.getInstance(tz).apply {
                clear()
                set(2026, 10, 18)
            }
        check(gregorianFa(date.timeInMillis, tz) == "۱۸ نوامبر")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        geometry()
        heading()
        catalogue()
        passesAndNumbers()
        println("PASS: HUD geometry, projected heading, search, rules, storage, passes and formatting")
    }
}
