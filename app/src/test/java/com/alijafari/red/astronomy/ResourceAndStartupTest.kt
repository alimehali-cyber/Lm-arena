package com.alijafari.red.astronomy

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.alijafari.red.astronomy.data.catalog.AstronomicalCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResourceAndStartupTest {

    @Test
    def testAllSplashDrawablesExistAndDecode8Bit() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val splashResIds = listOf(
            R.drawable.img_splash_1,
            R.drawable.img_splash_2,
            R.drawable.img_splash_3,
            R.drawable.red_app_logo,
            R.drawable.red_app_logo_display,
            R.drawable.img_full_moon_photo,
            R.drawable.ic_launcher_fg
        )

        for (resId in splashResIds) {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            assertNotNull("Resource ID $resId failed to decode into Bitmap!", bitmap)
            assertTrue("Resource ID $resId decoded bitmap width is 0!", bitmap.width > 0)
            assertTrue("Resource ID $resId decoded bitmap height is 0!", bitmap.height > 0)
        }
    }

    @Test
    def testAstronomicalCatalogLoadsCompletely() {
        val catalog = AstronomicalCatalog.getAllObjects()
        assertTrue("Catalog must contain all visible objects!", catalog.size >= 100)
        
        // Verify Safashahr location exists in built-in cities
        val cities = com.alijafari.red.astronomy.data.location.BuiltInCities.getCities()
        val safashahr = cities.find { it.nameEn.equals("Safashahr", ignoreCase = true) }
        assertNotNull("Safashahr must exist in built-in city database!", safashahr)
        assertEquals("Safashahr latitude must match official coordinates", 30.612, safashahr!!.latitude, 0.05)
    }
}
