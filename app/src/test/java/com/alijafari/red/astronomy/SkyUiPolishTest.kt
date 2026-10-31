package com.alijafari.red.astronomy

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source- and asset-level guards for the round of fixes after the two-card Home redesign:
 *
 *  - the eclipse plates carry their own alpha, so there is no disc of black sky around them;
 *  - the eclipse plate is drawn at the Moon's own size;
 *  - the eclipse card's text uses the space beside the plate;
 *  - the sky canvas' selection card points at its object;
 *  - the search field has no red rectangle;
 *  - the AR screen no longer chips objects at the screen edges;
 *  - "What's Up Tonight" survives any backdrop;
 *  - the Lab screen is titled, not introduced by a card.
 *
 * None of this can be asserted by a screenshot test (Home is a LazyColumn over a GL landscape, and the
 * plates are pictures), so the shape of the code — and the alpha flag in the shipped WebP files — is the
 * guard.
 */
class SkyUiPolishTest {

    private fun source(relative: String): String {
        val candidates = listOf("src/main/java/$relative", "app/src/main/java/$relative")
        return candidates.map(::File).firstOrNull { it.exists() }?.readText()
            ?: error("Source not found: $relative")
    }

    private fun asset(name: String): File {
        val candidates = listOf("src/main/res/drawable-nodpi/$name", "app/src/main/res/drawable-nodpi/$name")
        return candidates.map(::File).firstOrNull { it.exists() } ?: error("Asset not found: $name")
    }

    private val render by lazy { source("com/alijafari/red/astronomy/ui/components/EclipseRender.kt") }
    private val home by lazy { source("com/alijafari/red/astronomy/ui/screens/HomeScreen.kt") }
    private val hero by lazy { source("com/alijafari/red/astronomy/ui/components/HeroSkyCanvas.kt") }
    private val ar by lazy { source("com/alijafari/red/astronomy/ui/screens/CompassARScreen.kt") }
    private val lab by lazy { source("com/alijafari/red/astronomy/ui/screens/LabScreen.kt") }

    @Test
    fun everyEclipsePlateHasItsOwnAlphaSoThereIsNoBlackRing() {
        // A WebP with no alpha channel can only be composited as an opaque square: that opaque
        // surround *was* the black ring. All six plates ship alpha (the VP8X "alpha" flag).
        val plates = listOf(
            "eclipse_solar_total.webp",
            "eclipse_solar_annular.webp",
            "eclipse_solar_partial.webp",
            "eclipse_lunar_total.webp",
            "eclipse_lunar_partial.webp",
            "eclipse_lunar_penumbral.webp",
        )
        for (plate in plates) {
            val bytes = asset(plate).readBytes()
            assertTrue("$plate must be a RIFF/WEBP container", String(bytes.copyOfRange(0, 4)) == "RIFF")
            val vp8x = indexOf(bytes, "VP8X".toByteArray())
            assertTrue("$plate must use the extended (alpha-capable) format", vp8x >= 0)
            val flags = bytes[vp8x + 8].toInt() and 0xFF
            assertTrue("$plate must carry an alpha channel (flags 0x%02x)".format(flags), flags and 0x10 != 0)
        }
    }

    @Test
    fun thePlateIsNeverCroppedIntoADiscOfSky() {
        // Fit keeps the whole phenomenon (a corona must not be trimmed); the plate's own alpha is the
        // shape, and the old 2% overscan that pushed the photograph's sky against the clip is gone.
        assertTrue("the plate is fitted, not cropped", render.contains("contentScale = ContentScale.Fit"))
        assertFalse("no overscan is needed once the sky is transparent", render.contains(".scale(1.02f)"))
        assertFalse("the plate is not cropped", render.contains("contentScale = ContentScale.Crop"))
        assertTrue("the plate is still clipped to a hard circle", render.contains(".clip(CircleShape)"))
        // The lift is a shadow, not a ring: its inks are translucent.
        assertTrue("the shadow is soft ink", render.contains("shadowColor.copy(alpha = 0.55f)"))
        assertTrue("and a soft spot", render.contains("shadowColor.copy(alpha = 0.45f)"))
    }

    @Test
    fun theEclipsePlateIsDrawnAtTheMoonsOwnSize() {
        // Both cards are one height and one render square (see HomeMoonCardLayoutTest); the plate is
        // inscribed in that square exactly as the Moon disc is, and its art is scaled so the disc's alpha
        // ends on the same circle as MoonPhaseRender's limb (both 1.03 x the half-size).
        assertTrue("the plate keeps the Moon's square", home.contains(".fillMaxHeight()\n                        .aspectRatio(1f)"))
        assertTrue("the plate is laid out in the same square", home.contains("renderSquare = card.renderSquare,"))
        assertTrue(
            "the plate's art is scaled to the Moon's disc",
            render.contains("the lunar discs are drawn at the same diameter as [MoonPhaseRender]")
        )
    }

    @Test
    fun theEclipseCardTextUsesTheFreeSpaceBesideItsPlate() {
        // The card is far wider than plate + text needs: the circumstances start right beside the plate
        // and end on the card's own inner margin, instead of sitting behind a gutter on both sides.
        assertTrue("the details sit close to the plate", home.contains(".padding(start = RedSpacing.xxs, end = RedSpacing.sm)"))
        assertFalse("the details no longer start behind a 4 dp gutter", home.contains(".padding(start = RedSpacing.xs, end = RedSpacing.md)"))
    }

    @Test
    fun theSelectionCardPointsAtItsObjectInsteadOfSomewhereElse() {
        // The old pill was a fixed 74 x 56 dp offset from the object with an arrow *glyph* inside it, so it
        // pointed at nothing. The callout is centred on the object and its wedge is aimed at the object.
        assertTrue("the callout is its own composable", hero.contains("private fun SelectionCallout("))
        assertTrue("it is handed the object's projected position", hero.contains("pinX = livePos.x,"))
        assertTrue("the wedge is drawn, not an arrow glyph in the text", hero.contains("val wedgeH = with(density) { 7.dp.toPx() }"))
        assertTrue("the card is centred on the object", hero.contains("val x = (pinX - w / 2f).coerceIn("))
        assertTrue("it flips below the object near the top edge", hero.contains("val placeAbove = pinY - lift - wedgeH - h >= edge"))
        assertTrue("the pointer stays inside the card", hero.contains("val tipX = (pinX - x).coerceIn("))
        assertFalse("no cheap glyph arrow inside the card", hero.contains("text = if (isFa) \"‹\" else \"›\""))
        assertFalse("no accent ring around it", hero.contains("border = BorderStroke(1.dp, accent.copy(alpha = 0.72f))"))
        // The identify → detail affordance itself is untouched.
        assertTrue("the callout opens the object", hero.contains("viewModel.openObjectDetailById(idToOpen, simulatedTimeMs)"))
    }

    @Test
    fun theSearchFieldHasNoRedRectangle() {
        assertTrue("the field's outline is transparent in every state", home.contains("focusedBorderColor = Color.Transparent,"))
        assertTrue("including unfocused", home.contains("unfocusedBorderColor = Color.Transparent,"))
        assertFalse("the accent border is gone", home.contains("focusedBorderColor = RedTheme.colors.accentRed"))
        assertTrue("the field sits transparent on the search card", home.contains("focusedContainerColor = Color.Transparent,"))
    }

    @Test
    fun theArScreenNoLongerChipsObjectsAtTheScreenEdges() {
        assertFalse("the edge chip is gone", ar.contains("EdgeIndicatorChip"))
        assertFalse("and its data class with it", ar.contains("class ArEdgeIndicator"))
        assertFalse("nothing is computed for the screen edges", ar.contains("val edgeIndicators"))
        // What the AR screen does keep: the finder arrow for the object the user picked.
        assertTrue("the arrow-guided finder stays", ar.contains("Arrow-Guided Finder Overlay"))
    }

    @Test
    fun whatsUpTonightIsReadableOnEveryBackdrop() {
        // Home scrolls over the theme background, the live sky or a photo backdrop, so a section title
        // cannot be bare ink on an unknown picture: it carries the surface the theme measured its ink
        // against.
        assertTrue("the title has its own surface", home.contains("private fun HomeSectionHeader("))
        assertTrue("taken from the theme", home.contains(".background(RedTheme.colors.surfaceElevated.copy(alpha = 0.92f))"))
        assertTrue("the title uses the readable ink", home.contains("color = RedTheme.colors.textPrimary,"))
        assertTrue(
            "and the section uses it",
            home.contains("HomeSectionHeader(\n                        title = if (isFa) \"امشب در آسمان\" else \"What's Up Tonight\",")
        )
        assertFalse(
            "the bare header that vanished into bright backdrops is gone",
            home.contains("RedSectionHeader(\n                        title = if (isFa) \"امشب در آسمان\"")
        )
    }

    @Test
    fun theLabScreenIsTitledInsteadOfIntroducedByACard() {
        assertFalse("the intro card is gone", lab.contains("lab_header_card"))
        assertTrue("the title is plain text", lab.contains(".testTag(\"lab_title\")"))
        assertTrue("it names the lab", lab.contains("\"Astrophysics Lab\""))
        assertTrue("and its Persian equivalent", lab.contains("\"آزمایشگاه اخترفیزیک\""))
        assertTrue("it is drawn in the theme's ink", lab.contains("color = RedTheme.colors.textPrimary,"))
        assertTrue("the tool list is untouched", lab.contains("lab_feature_card_"))
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}
