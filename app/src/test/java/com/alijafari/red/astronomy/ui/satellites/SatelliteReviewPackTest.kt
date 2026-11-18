package com.alijafari.red.astronomy.ui.satellites

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.alijafari.red.astronomy.domain.ThemeMode
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Optional host-side captures. These are not screenshots of the native GLES surface. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "en-rUS-ldltr-w1024dp-h1600dp-mdpi")
class SatelliteReviewPackTest {
    @get:Rule val compose = createComposeRule()

    private fun allowed(env: String, value: String): Boolean {
        val filter = System.getenv(env)?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }
        return filter.isNullOrEmpty() || value.lowercase() in filter
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun captureFixtureMatrix() {
        assumeTrue("Set SAT_REVIEW_CAPTURE=1 to generate the optional review pack", System.getenv("SAT_REVIEW_CAPTURE") == "1")
        val all = mutableListOf<ReviewScenario>()
        for (skin in SatSkin.entries) for (theme in ThemeMode.entries) for (language in listOf("en", "fa"))
            for (scale in listOf(1f, 1.3f)) for (page in ReviewPage.entries) {
                all += ReviewScenario(scale = scale, language = language, skin = skin, theme = theme, page = page)
            }
        // Optional geometry set complements the core contract and bounds test at 1.0/1.15/1.3.
        if (System.getenv("SAT_REVIEW_SIZE_SWEEP") == "1") {
            for ((w, h) in listOf(360 to 640, 412 to 915, 768 to 1024))
                for (skin in SatSkin.entries) for (language in listOf("en", "fa"))
                    for (scale in listOf(1f, 1.15f, 1.3f)) for (page in listOf(ReviewPage.Rest, ReviewPage.Recenter)) {
                        all += ReviewScenario(w, h, scale, language, skin, ThemeMode.DARK, page)
                    }
        }
        val scenarios = all.distinct().filter {
            allowed("SAT_REVIEW_SKINS", it.skin.name) && allowed("SAT_REVIEW_THEMES", it.theme.name) &&
                allowed("SAT_REVIEW_LANGUAGES", it.language) && allowed("SAT_REVIEW_SCALES", (it.scale * 100).toInt().toString()) &&
                allowed("SAT_REVIEW_PAGES", it.page.name)
        }
        assertTrue("Review filters selected no scenarios", scenarios.isNotEmpty())
        val root = listOf(File("."), File("..")).first { File(it, "settings.gradle.kts").isFile }
        val output = File(root, "review/generated")
        val current = mutableStateOf(scenarios.first())
        compose.setContent { ScenarioContent(current.value) }
        for (scenario in scenarios) {
            compose.runOnIdle { current.value = scenario }
            compose.waitForIdle()
            val folder = File(output, "${scenario.skin.name.lowercase()}/${scenario.theme.name.lowercase()}/${scenario.language}/scale-${(scenario.scale * 100).toInt()}")
            check(folder.mkdirs() || folder.isDirectory)
            val file = File(folder, "${scenario.width}x${scenario.height}-${scenario.page.name.lowercase()}.png")
            compose.onNodeWithTag("review_case", useUnmergedTree = true).captureRoboImage(
                file = file, roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
            )
        }
        println("Captured ${scenarios.size} deterministic stub fixtures into ${output.absolutePath}")
    }
}
