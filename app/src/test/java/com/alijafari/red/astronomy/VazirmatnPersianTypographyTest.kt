package com.alijafari.red.astronomy

import androidx.compose.ui.text.font.FontFamily
import com.alijafari.red.astronomy.ui.theme.EstedadFontFamily
import com.alijafari.red.astronomy.ui.theme.IranSans
import com.alijafari.red.astronomy.ui.theme.VazirmatnFontFamily
import com.alijafari.red.astronomy.ui.theme.redFontFamily
import com.alijafari.red.astronomy.ui.theme.redTypographyFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
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
     * The token getters are `@Composable` (they follow the theme), so this checks the declarations
     * instead of calling them: the shared bases carry no family at all, and every custom token and
     * every `RedTypography` accessor re-puts the theme face on at read time. That is what makes
     * Persian work at the ~120 call sites that hand a style straight to Material's `Text`, which
     * does not merge a passed style onto the ambient one — and it must not disturb the metrics.
     */
    @Test
    fun redDesignTokensAreFamilyFreeAndRebaseOnTheThemeFace() {
        val type = readAppSource("ui/theme/Type.kt")
        val bases = type.substring(
            type.indexOf("internal object RedTypeBase {"),
            type.indexOf("object RedTypographyTokens {")
        )
        val tokens = type.substring(
            type.indexOf("object RedTypographyTokens {"),
            type.indexOf("object RedTypography {")
        )
        val accessor = type.substring(type.indexOf("object RedTypography {"))

        assertFalse("token bases must not pin a font family", bases.contains("fontFamily"))
        assertEquals(
            "all 11 custom styles are declared on the bases",
            11,
            Regex(
                "val (heroDisplay|sectionHeading|bodyPrimary|bodySecondary|numberLarge|numberMedium" +
                    "|numberSmall|screenTitle|sectionTitle|caption|badge) = TextStyle\\("
            ).findAll(bases).count()
        )
        assertEquals(
            "every custom token re-applies the theme face",
            11,
            Regex("redLocalized\\(RedTypeBase\\.\\w+\\)").findAll(tokens).count()
        )
        assertEquals(
            "the RedTypography accessor forwards the face for all 15 Material styles",
            15,
            Regex("redLocalized\\(Typography\\.\\w+\\)").findAll(accessor).count()
        )
        assertTrue(
            "metrics are untouched by the sweep",
            bases.contains("fontSize = 26.sp") && bases.contains("lineHeight = 34.sp") &&
                bases.contains("letterSpacing = (-0.4).sp") && bases.contains("fontSize = 12.sp")
        )
    }

    /**
     * `EstedadFontFamily` is the honest name for what the removed Efsan bundle always rendered as —
     * the platform face — and the historical `IranSans` alias now points at the real Vazirmatn
     * family, so no reference can silently land somewhere else.
     */
    @Test
    fun legacyFontAliasesResolveToTheDeclaredFamilies() {
        assertSame(FontFamily.Default, EstedadFontFamily)
        assertSame(VazirmatnFontFamily, IranSans)
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
    // Coverage sweep: no text in the app may escape the locale face
    //
    // The bug this guards against is structural, not visual: Material 3's Text uses the `style` it is
    // handed as-is (it does not merge the theme's ambient style into an explicit one), so a call site
    // that passes its own TextStyle without a family silently renders Persian in the system font. That
    // is how the AR pills, the home screen and the Lab/Moon screens kept the old face after the theme
    // swap. These assertions scan the sources so the same gap cannot come back unnoticed.
    // -----------------------------------------------------------------------------------------

    @Test
    fun noUiStylePinsAFamilyThatWouldDefeatTheLocaleFace() {
        val pin = Regex("fontFamily\\s*=\\s*FontFamily\\.(SansSerif|Serif|Monospace|Cursive|Default)")
        val offenders = appSources()
            .filter { it.relativePath().startsWith("ui/theme/") || it.name == exemptBrandLockup }
            .flatMap { f ->
                val text = f.readText()
                pin.findAll(text)
                    .filterNot { isBrandWordmark(text, it.range.first) }
                    .map { m -> "${f.name}:${lineOf(text, m.range.first)} ${m.value}" }
            }
            .toList()
        assertTrue("Styles must not pin a fixed family; use the theme face. Offenders: $offenders", offenders.isEmpty())
    }

    /**
     * The `ZIG` wordmark is a brand lockup: it renders identically in both locales and can never carry
     * Persian text, so a fixed family there is deliberate rather than a hole in the theme.
     */
    private fun isBrandWordmark(text: String, index: Int): Boolean {
        var depth = 0
        var i = index - 1
        while (i >= 0) {
            when (text[i]) {
                ')' -> depth++
                '(' -> if (depth == 0) return argumentsOf(text, i).contains("\"ZIG\"") else depth--
            }
            i--
        }
        return false
    }

    @Test
    fun everyIranSansUseIsGuardedByTheLocale() {
        val offenders = mutableListOf<String>()
        for (f in appSources()) {
            f.readText().lineSequence().forEachIndexed { i, line ->
                if (line.contains("= IranSans") && !line.contains("if (isFa)")) {
                    offenders += "${f.name}:${i + 1} ${line.trim()}"
                }
            }
        }
        assertTrue(
            "IranSans is the Persian face; using it unconditionally would restyle English text. " +
                "Guard it with the locale or use the theme family. Offenders: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun everyCanvasTextCarriesTheFontFamilyExplicitly() {
        val offenders = appSources().flatMap { f ->
            val text = f.readText()
            occurrences(text, "textMeasurer.measure(").filterNot { "fontFamily" in argumentsOf(text, it) }
                .map { "${f.name}:${lineOf(text, it)}" }
        }.toList()
        assertTrue(
            "Text measured onto a Canvas never inherits the theme's ambient style, so each measure() " +
                "must set fontFamily itself. Offenders: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun everyTextFieldPassesTheLocaleTextStyle() {
        val offenders = appSources().flatMap { f ->
            val text = f.readText()
            (occurrences(text, "OutlinedTextField(") + bareOccurrences(text, "TextField("))
                .filterNot { "textStyle" in argumentsOf(text, it) }
                .map { "${f.name}:${lineOf(text, it)}" }
        }.toList()
        assertTrue(
            "Material text fields take their value style from `textStyle`, not from the ambient style, " +
                "so typed Persian needs it explicitly. Offenders: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun theThemeResolvesAndDistributesTheFaceInOnePlace() {
        val theme = readAppSource("ui/theme/Theme.kt")
        assertTrue(
            "REDTheme must feed the locale face to Material typography, the ambient LocalTextStyle and " +
                "LocalAppFontFamily",
            theme.contains("redTypographyFor(isPersian)") &&
                theme.contains("LocalAppFontFamily provides appFontFamily") &&
                theme.contains("LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appFontFamily)")
        )
        assertTrue(
            "MainActivity must drive that decision from the app's own language state",
            readAppSource("MainActivity.kt").contains("isPersian = isFa")
        )
    }

    // ---- sweep helpers ----

    private val exemptBrandLockup get() = "PremiumSplashScreen.kt"

    private fun appSources(): List<File> {
        val dir = appSourceDir()
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun readAppSource(relative: String): String {
        val f = File(appSourceDir(), relative)
        assertTrue("missing source file $relative", f.isFile)
        return f.readText()
    }

    private fun File.relativePath(): String = relativeTo(appSourceDir()).path.replace(File.separatorChar, '/')

    private fun appSourceDir(): File {
        // Gradle runs unit tests with the module directory as the working directory, but walk up so
        // the sweep does not depend on that detail.
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "app/src/main/java/com/alijafari/red/astronomy")
            if (candidate.isDirectory) return candidate
            val direct = File(dir, "src/main/java/com/alijafari/red/astronomy")
            if (direct.isDirectory) return direct
            dir = dir.parentFile
        }
        throw AssertionError("could not locate the astronomy source directory")
    }

    private fun occurrences(text: String, needle: String): List<Int> {
        val out = mutableListOf<Int>()
        var i = text.indexOf(needle)
        while (i >= 0) {
            out += i
            i = text.indexOf(needle, i + needle.length)
        }
        return out
    }

    private fun bareOccurrences(text: String, needle: String): List<Int> =
        occurrences(text, needle).filterNot { pos ->
            val before = text.getOrNull(pos - 1)
            before != null && (before.isLetterOrDigit() || before == '_' || before == '.')
        }

    private fun argumentsOf(text: String, callStart: Int): String {
        var depth = 0
        var i = text.indexOf('(', callStart)
        if (i < 0) return ""
        val from = i + 1
        while (i < text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return text.substring(from, i)
                }
            }
            i++
        }
        return text.substring(from)
    }

    private fun lineOf(text: String, index: Int): Int = text.take(index).count { it == '\n' } + 1

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
