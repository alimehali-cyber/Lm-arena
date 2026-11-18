package com.alijafari.red.astronomy

import com.alijafari.red.astronomy.ui.satellites.SatFmt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AndroidX BidiFormatter uses a platform SpannableStringBuilder. Test the real implementation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SatSignedTextTest {
    @Test
    fun signedNumbersUseARealMinusAndAnExplicitPlus() {
        val negative = SatFmt.signed(-3.4, 1, "", isFa = false)
        val positive = SatFmt.signed(1.2, 1, "", isFa = false)

        assertTrue("negative carries U+2212", negative.contains('\u2212'))
        assertFalse("negative never uses a hyphen", negative.contains('-'))
        assertTrue("positive carries an explicit plus", positive.contains('+'))
        // The isolate pair is what stops a sign jumping to the other side in RTL.
        assertTrue("the number is isolated", negative.contains('\u2068') && negative.contains('\u2069'))
        assertFalse("no ASCII plus-before-minus anywhere", negative.contains("+-"))
    }

    @Test
    fun persianNumbersUsePersianDigitsAndTheArabicDecimalSeparator() {
        val fa = SatFmt.signed(-3.4, 1, "", isFa = true)
        assertTrue("persian digits", fa.any { it in '\u06F0'..'\u06F9' })
        assertTrue("arabic decimal separator", fa.contains('\u066B'))
        assertFalse("no latin digits", fa.any { it in '0'..'9' })
    }
}
