package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ui.monuments.MonumentBlock
import com.alijafari.red.astronomy.ui.monuments.MonumentContent
import com.alijafari.red.astronomy.ui.monuments.MonumentCopy
import com.alijafari.red.astronomy.ui.monuments.MonumentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the two monument pages.
 *
 * Structure is asserted so a page can never lose its photographs, its chapters, its cited quote or
 * its era list, and the Persian copy is checked mechanically for the things a native reader notices
 * first: Arabic codepoints where Persian ones belong, Latin digits inside Persian prose, em dashes,
 * and the administrative vocabulary («می‌باشد», «لازم به ذکر است») that marks a text as machine-made.
 * This is the same rule set the persian-writing skill's `fa_lint.py` checks; the test keeps it
 * enforced by CI rather than by reviewer memory.
 */
class MonumentContentTest {

    private fun persianPages(): List<Pair<MonumentId, MonumentContent>> =
        MonumentId.entries.map { it to MonumentCopy.of(it, isPersian = true) }

    private fun englishPages(): List<Pair<MonumentId, MonumentContent>> =
        MonumentId.entries.map { it to MonumentCopy.of(it, isPersian = false) }

    /** Every string a page shows, in reading order. */
    private fun strings(content: MonumentContent): List<String> = buildList {
        add(content.title)
        add(content.kicker)
        add(content.subtitle)
        add(content.lead)
        addAll(content.meta)
        content.stats.forEach { stat ->
            add(stat.value)
            add(stat.label)
        }
        content.blocks.forEach { block ->
            when (block) {
                is MonumentBlock.Chapter -> {
                    add(block.chapter.index)
                    add(block.chapter.title)
                    addAll(block.chapter.paragraphs)
                }
                is MonumentBlock.Figure -> add(block.figure.caption)
                is MonumentBlock.Quote -> {
                    add(block.quote.text)
                    block.quote.attribution?.let { add(it) }
                }
                is MonumentBlock.Timeline -> block.entries.forEach { entry ->
                    add(entry.year)
                    add(entry.event)
                }
            }
        }
        add(content.credits)
        add(content.sources)
    }

    @Test
    fun eachPageCarriesPhotographsChaptersACitedQuoteAndAnEraList() {
        for ((id, page) in persianPages()) {
            assertTrue("$id must have a hero photograph", page.heroRes != 0)

            val figures = page.blocks.filterIsInstance<MonumentBlock.Figure>().map { it.figure }
            assertTrue("$id needs at least three photographs", figures.size >= 3)
            assertEquals(
                "$id must not repeat a photograph on its own page",
                figures.size,
                figures.map { it.imageRes }.distinct().size
            )

            assertTrue(
                "$id needs at least four chapters",
                page.blocks.count { it is MonumentBlock.Chapter } >= 4
            )
            assertEquals(
                "$id needs exactly one pull quote",
                1,
                page.blocks.count { it is MonumentBlock.Quote }
            )
            assertEquals(
                "$id needs exactly one era list",
                1,
                page.blocks.count { it is MonumentBlock.Timeline }
            )
            assertTrue(
                "$id needs an era list with real entries",
                (page.blocks.first { it is MonumentBlock.Timeline } as MonumentBlock.Timeline)
                    .entries.size >= 4
            )
            assertEquals("$id needs four cited measurements", 4, page.stats.size)
        }
    }

    @Test
    fun bothLanguagesTellTheSameStoryInTheSameOrder() {
        for (id in MonumentId.entries) {
            val fa = MonumentCopy.of(id, isPersian = true)
            val en = MonumentCopy.of(id, isPersian = false)

            assertEquals("$id stats", fa.stats.size, en.stats.size)
            assertEquals("$id meta", fa.meta.size, en.meta.size)
            assertEquals("$id blocks", fa.blocks.size, en.blocks.size)
            assertEquals(
                "$id photographs",
                fa.blocks.filterIsInstance<MonumentBlock.Figure>().map { it.figure.imageRes },
                en.blocks.filterIsInstance<MonumentBlock.Figure>().map { it.figure.imageRes }
            )
            assertEquals(
                "$id era entries",
                (fa.blocks.first { it is MonumentBlock.Timeline } as MonumentBlock.Timeline).entries.size,
                (en.blocks.first { it is MonumentBlock.Timeline } as MonumentBlock.Timeline).entries.size
            )
            assertTrue("$id Persian title must not be empty", fa.title.isNotBlank())
            assertTrue("$id English title must not be empty", en.title.isNotBlank())
            assertTrue(
                "$id must not show the same title in both languages",
                fa.title != en.title
            )
        }
    }

    @Test
    fun persianCopyUsesPersianCodepointsOnly() {
        val forbidden = mapOf(
            'ي' to "Arabic yeh (use ی)",
            'ك' to "Arabic kaf (use ک)",
            '٤' to "Arabic-Indic digit",
            '٥' to "Arabic-Indic digit",
            '٦' to "Arabic-Indic digit",
            '—' to "em dash",
            '–' to "en dash"
        )
        for ((id, page) in persianPages()) {
            for (text in strings(page)) {
                for ((character, reason) in forbidden) {
                    assertTrue(
                        "$id Persian copy contains a $reason: «$text»",
                        !text.contains(character)
                    )
                }
            }
        }
    }

    @Test
    fun persianCopyUsesPersianDigits() {
        val latinDigit = Regex("[0-9]")
        for ((id, page) in persianPages()) {
            for (text in strings(page)) {
                assertTrue(
                    "$id Persian copy must not carry Latin digits: «$text»",
                    !latinDigit.containsMatchIn(text)
                )
            }
        }
    }

    @Test
    fun persianCopyCarriesNoAdministrativeOrMachineTells() {
        val tells = listOf(
            "می باشد",
            "می‌باشد",
            "لازم به ذکر",
            "شایان ذکر",
            "در راستای",
            "در همین راستا",
            "نقش بسزایی",
            "از اهمیت ویژه‌ای برخوردار",
            "در دنیای امروز",
            "در نهایت می‌توان گفت",
            "بی‌نظیر"
        )
        for ((id, page) in persianPages()) {
            for (text in strings(page)) {
                for (tell in tells) {
                    assertTrue(
                        "$id Persian copy contains the tell «$tell»: «$text»",
                        !text.contains(tell)
                    )
                }
            }
        }
    }

    @Test
    fun persianCopyUsesHalfSpaces() {
        // ZWNJ is the one mechanical habit that most separates professional Persian from typed
        // Persian; the pages use it in می‌/‌ها/‌تر compounds.
        val halfSpace = '\u200C'
        for ((id, page) in persianPages()) {
            val text = strings(page).joinToString(" ")
            assertTrue("$id must use ZWNJ in its Persian copy", text.contains(halfSpace))
            assertTrue(
                "$id must not write می with a full space",
                !text.contains("می ") || text.contains("می‌")
            )
        }
    }

    @Test
    fun theCanvasCalloutStaysShortEnoughForItsCard() {
        for (id in MonumentId.entries) {
            for (isPersian in listOf(true, false)) {
                val title = MonumentCopy.title(id, isPersian)
                val detail = MonumentCopy.calloutDetail(id, isPersian)
                assertTrue("$id callout title is empty", title.isNotBlank())
                assertTrue("$id callout detail is empty", detail.isNotBlank())
                assertTrue(
                    "$id callout title must fit one line: «$title»",
                    title.length <= 18
                )
                assertTrue(
                    "$id callout detail must fit one line: «$detail»",
                    detail.length <= 48
                )
            }
        }
    }
}
