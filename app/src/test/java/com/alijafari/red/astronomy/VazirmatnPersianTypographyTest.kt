package com.alijafari.red.astronomy

import androidx.compose.ui.text.font.FontFamily
import com.alijafari.red.astronomy.ui.theme.IranSans
import com.alijafari.red.astronomy.ui.theme.RedTypographyTokens
import com.alijafari.red.astronomy.ui.theme.VazirmatnFontFamily
import com.alijafari.red.astronomy.ui.theme.redFontFamily
import com.alijafari.red.astronomy.ui.theme.redTypographyFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Vazirmatn is the app's Persian type face, and only its Persian one.
 *
 * Two halves: the locale rule that picks the family (the thing that must never regress — English has
 * to keep rendering with the face it always used), and the integrity of the bundled font resources.
 * The second half matters because every font file this module used to carry was byte-corrupted and
 * therefore unloadable at runtime, which is why the aliases had been stubbed to [FontFamily.Default].
 */
class VazirmatnPersianTypographyTest {

    // -----------------------------------------------------------------------------------------
    // The locale rule
    // -----------------------------------------------------------------------------------------

    @Test
    fun persianPicksVazirmatnAndEnglishPicksThePlatformFace() {
        assertSame(VazirmatnFontFamily, redFontFamily(isPersian = true))
        assertSame(FontFamily.Default, redFontFamily(isPersian = false))
        assertNotSame("Vazirmatn must be the real bundled family, not the platform default", FontFamily.Default, VazirmatnFontFamily)
    }

    @Test
    fun persianScaleCarriesVazirmatnOnEveryMaterialStyle() {
        val styles = redTypographyFor(isPersian = true).allMaterialStyles()
        assertEquals("Material 3 defines 15 text styles", 15, styles.size)
        for ((name, style) in styles) {
            assertSame("$name must render in Vazirmatn", VazirmatnFontFamily, style.fontFamily)
        }
    }

    @Test
    fun englishScaleKeepsThePlatformFaceOnEveryMaterialStyle() {
        val styles = redTypographyFor(isPersian = false).allMaterialStyles()
        assertEquals(15, styles.size)
        for ((name, style) in styles) {
            assertSame("$name must stay on the system font", FontFamily.Default, style.fontFamily)
        }
    }

    /**
     * Only the face may change: sizes, weights, line heights and tracking are what the design system
     * was tuned on, so a locale switch must not disturb the layout of any screen.
     */
    @Test
    fun switchingTheLocaleChangesNothingButTheFontFamily() {
        val latin = redTypographyFor(isPersian = false).allMaterialStyles()
        val persian = redTypographyFor(isPersian = true).allMaterialStyles()
        assertEquals(latin.keys, persian.keys)
        for (name in latin.keys) {
            val a = latin.getValue(name)
            val b = persian.getValue(name)
            assertEquals("$name fontSize", a.fontSize, b.fontSize)
            assertEquals("$name lineHeight", a.lineHeight, b.lineHeight)
            assertEquals("$name letterSpacing", a.letterSpacing, b.letterSpacing)
            assertEquals("$name fontWeight", a.fontWeight, b.fontWeight)
            assertEquals("$name fontStyle", a.fontStyle, b.fontStyle)
        }
    }

    /**
     * The custom RED tokens leave `fontFamily` unset on purpose, so they inherit whatever the theme
     * provides through LocalTextStyle. Pinning a family there would silently opt Persian text out of
     * Vazirmatn at the 120+ call sites that use them.
     */
    @Test
    fun redDesignTokensLeaveTheFamilyUnsetSoTheyFollowTheLocale() {
        val tokens = mapOf(
            "heroDisplay" to RedTypographyTokens.heroDisplay,
            "sectionHeading" to RedTypographyTokens.sectionHeading,
            "bodyPrimary" to RedTypographyTokens.bodyPrimary,
            "bodySecondary" to RedTypographyTokens.bodySecondary,
            "numberLarge" to RedTypographyTokens.numberLarge,
            "numberMedium" to RedTypographyTokens.numberMedium,
            "numberSmall" to RedTypographyTokens.numberSmall,
            "screenTitle" to RedTypographyTokens.screenTitle,
            "sectionTitle" to RedTypographyTokens.sectionTitle,
            "caption" to RedTypographyTokens.caption,
            "badge" to RedTypographyTokens.badge
        )
        assertEquals("all token styles are covered", 11, tokens.size)
        for ((name, style) in tokens) {
            assertNull("$name must leave the font family unset so the theme's face wins", style.fontFamily)
        }
    }

    /** The still-unused aliases stay honest: platform face, never a broken bundle. */
    @Test
    fun unusedFontAliasesStillResolveToThePlatformFace() {
        assertSame(FontFamily.Default, IranSans)
    }

    // -----------------------------------------------------------------------------------------
    // Bundled font integrity
    // -----------------------------------------------------------------------------------------

    @Test
    fun everyBundledVazirmatnWeightIsACompleteLoadableFont() {
        val expected = mapOf(
            "vazirmatn_regular.ttf" to 400,
            "vazirmatn_medium.ttf" to 500,
            "vazirmatn_semibold.ttf" to 600,
            "vazirmatn_bold.ttf" to 700
        )
        val requiredTables = setOf("name", "head", "hhea", "maxp", "OS/2", "cmap", "glyf", "loca")
        for ((fileName, weightClass) in expected) {
            val file = resFont(fileName)
            assertTrue("$fileName must be bundled in res/font", file.isFile)
            val bytes = file.readBytes()

            assertTrue("$fileName must be a TrueType sfnt", SFNT_VERSION == readUInt(bytes, 0))
            val tables = sfntTables(bytes)
            assertTrue(
                "$fileName is missing tables ${requiredTables - tables.keys}",
                tables.keys.containsAll(requiredTables)
            )
            assertFalse(
                "$fileName contains UTF-8 replacement sequences — it was corrupted by a text re-encode " +
                    "and Android cannot load it",
                containsReplacementBytes(bytes)
            )
            assertEquals("$fileName usWeightClass", weightClass, os2WeightClass(bytes, tables.getValue("OS/2").first))
            val family = primaryFamilyName(bytes, tables.getValue("name"))
            assertTrue("$fileName must be the real Vazirmatn family, was '$family'", family.contains("Vazirmatn"))
        }
    }

    @Test
    fun theOflLicenceAndAuthorListShipWithTheFont() {
        val dir = thirdPartyLicencesDir()
        val licence = File(dir, "Vazirmatn-OFL.txt")
        assertTrue("Vazirmatn-OFL.txt must be preserved with the font", licence.isFile)
        val text = licence.readText()
        assertTrue("the licence must be the SIL OFL text", text.contains("SIL OPEN FONT LICENSE"))
        assertTrue("the licence must name the Vazirmatn copyright holders", text.contains("Vazirmatn"))
        val authors = File(dir, "Vazirmatn-AUTHORS.txt")
        assertTrue("the upstream author list must be preserved", authors.isFile && authors.readText().isNotBlank())
        assertTrue(
            "the provenance of the bundled files must be recorded",
            File(dir, "README.md").readText().contains("v33.003")
        )
    }

    /**
     * The sibling bundles this module still carries (`iran_sans_*`, `estedad_*`) are corrupt and
     * referenced by nothing. If someone deletes them this test stays silent; if they are still broken
     * they must still not be wired into the type scale.
     */
    @Test
    fun corruptedSiblingBundlesAreNeverWiredIntoTheScale() {
        for (name in listOf("iran_sans_regular.ttf", "estedad_regular.ttf")) {
            val file = resFont(name)
            if (!file.isFile) continue
            val bytes = file.readBytes()
            val unusable = containsReplacementBytes(bytes) || sfntTables(bytes).isEmpty()
            assertTrue("$name is expected to stay unusable until it is replaced", unusable)
        }
    }

    // -----------------------------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------------------------

    private fun androidx.compose.material3.Typography.allMaterialStyles() = mapOf(
        "displayLarge" to displayLarge, "displayMedium" to displayMedium, "displaySmall" to displaySmall,
        "headlineLarge" to headlineLarge, "headlineMedium" to headlineMedium, "headlineSmall" to headlineSmall,
        "titleLarge" to titleLarge, "titleMedium" to titleMedium, "titleSmall" to titleSmall,
        "bodyLarge" to bodyLarge, "bodyMedium" to bodyMedium, "bodySmall" to bodySmall,
        "labelLarge" to labelLarge, "labelMedium" to labelMedium, "labelSmall" to labelSmall
    )

    private fun resFont(name: String): File {
        val candidates = listOf(File("src/main/res/font/$name"), File("app/src/main/res/font/$name"))
        return candidates.firstOrNull { it.isFile } ?: candidates.first()
    }

    private fun thirdPartyLicencesDir(): File {
        val candidates = listOf(File("THIRD_PARTY_LICENSES"), File("../THIRD_PARTY_LICENSES"))
        return candidates.firstOrNull { it.isDirectory } ?: candidates.first()
    }

    private fun readUInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)

    private fun readUShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun sfntTables(bytes: ByteArray): Map<String, Pair<Int, Int>> {
        if (bytes.size < 12) return emptyMap()
        val numTables = readUShort(bytes, 4)
        if (numTables <= 0 || 12 + numTables * 16 > bytes.size) return emptyMap()
        val out = LinkedHashMap<String, Pair<Int, Int>>()
        for (i in 0 until numTables) {
            val record = 12 + i * 16
            val tag = String(bytes, record, 4, Charsets.ISO_8859_1)
            val offset = readUInt(bytes, record + 8)
            val length = readUInt(bytes, record + 12)
            if (offset in 0 until bytes.size && offset + length <= bytes.size) out[tag] = offset to length
        }
        return out
    }

    private fun containsReplacementBytes(bytes: ByteArray): Boolean {
        val marker = byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte())
        for (i in 0..bytes.size - 3) {
            var hit = true
            for (j in marker.indices) {
                if (bytes[i + j] != marker[j]) {
                    hit = false
                    break
                }
            }
            if (hit) return true
        }
        return false
    }

    private fun os2WeightClass(bytes: ByteArray, os2Offset: Int): Int = readUShort(bytes, os2Offset + 4)

    private fun primaryFamilyName(bytes: ByteArray, nameTable: Pair<Int, Int>): String {
        val (offset, _) = nameTable
        val count = readUShort(bytes, offset + 2)
        val stringOffset = readUShort(bytes, offset + 4)
        for (i in 0 until count) {
            val record = offset + 6 + i * 12
            if (record + 12 > bytes.size) break
            val platform = readUShort(bytes, record)
            val nameId = readUShort(bytes, record + 6)
            val length = readUShort(bytes, record + 8)
            val start = offset + stringOffset + readUShort(bytes, record + 10)
            if (nameId != 1 || start < 0 || start + length > bytes.size) continue
            return if (platform == 3) {
                String(bytes, start, length, Charsets.UTF_16BE)
            } else {
                String(bytes, start, length, Charsets.ISO_8859_1)
            }
        }
        return ""
    }

    private companion object {
        const val SFNT_VERSION = 0x00010000
    }
}
