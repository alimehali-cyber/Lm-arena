package com.alijafari.red.astronomy

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alijafari.red.astronomy.domain.AppLanguage
import com.alijafari.red.astronomy.ui.MainViewModel
import com.alijafari.red.astronomy.ui.demo.Showcase
import com.alijafari.red.astronomy.ui.demo.ShowcasePlan
import com.alijafari.red.astronomy.ui.demo.ShowcaseScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real-activity regression test: the complete tour must run and restore the user's state. */
@RunWith(AndroidJUnit4::class)
class ShowcaseSmokeTest {
    private fun onMain(block: () -> Unit) {
        val latch = CountDownLatch(1)
        val error = AtomicReference<Throwable?>()
        Handler(Looper.getMainLooper()).post {
            try { block() } catch (t: Throwable) { error.set(t) } finally { latch.countDown() }
        }
        assertTrue("Main thread did not respond", latch.await(20, TimeUnit.SECONDS))
        error.get()?.let { throw it }
    }

    @Test fun fullTourRestoresUserState() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val vmRef = AtomicReference<MainViewModel>()
        scenario.onActivity { vmRef.set(ViewModelProvider(it)[MainViewModel::class.java]) }
        // Let launch/splash finish before the button is pressed.
        SystemClock.sleep(12_000)
        val vm = vmRef.get()
        onMain {
            vm.setLanguage(AppLanguage.ENGLISH)
            vm.selectTab(4)
            vm.returnToLiveTime()
            Showcase.start()
        }
        val seen = linkedSetOf<ShowcaseScene>()
        val deadline = SystemClock.elapsedRealtime() + 90_000
        var finished = false
        while (SystemClock.elapsedRealtime() < deadline && !finished) {
            onMain {
                val scene = Showcase.scene
                if (scene != ShowcaseScene.IDLE) seen.add(scene)
                else if (seen.isNotEmpty()) finished = true
            }
            SystemClock.sleep(200)
        }
        assertTrue("Tour did not complete; saw $seen", finished)
        assertEquals(ShowcasePlan.steps.map { it.scene }, seen.toList())
        onMain {
            assertEquals(AppLanguage.ENGLISH, vm.uiState.value.language)
            assertEquals(4, vm.uiState.value.selectedTab)
            assertEquals(com.alijafari.red.astronomy.domain.TimeMachineMode.LIVE, vm.uiState.value.timeMachineState.mode)
        }
        // The animated renderers may never leave the main looper idle, so do not use
        // ActivityScenario.close(), which waits for idle indefinitely on some emulators.
        onMain { androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
            .firstOrNull { it is MainActivity }?.finish() }
    }
}
