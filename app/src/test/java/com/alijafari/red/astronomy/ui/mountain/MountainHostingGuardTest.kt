package com.alijafari.red.astronomy.ui.mountain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Integration source guards. Actual EGL rendering/compositing still needs an Android device. */
class MountainHostingGuardTest {
    @Test
    fun optimizedSceneKeepsTheInWindowGles3Host() {
        val host = source("MountainTextureView.kt")
        val scene = source("Mountain.kt")
        assertTrue(host.contains(": TextureView("))
        assertTrue(host.contains("isOpaque = false"))
        assertTrue(host.contains("EGL_CONTEXT_CLIENT_VERSION, 3"))
        assertTrue(scene.contains("import android.opengl.GLES30"))
        for (text in listOf(host, scene, source("HomeMountain.kt"))) {
            assertFalse("standalone upload wrapper must not cover Home overlays", text.contains("GLSurfaceView"))
            assertFalse(text.contains("setZOrderOnTop"))
        }
    }

    @Test
    fun scaledBufferAndViewportAreUsedOnAttachAndResize() {
        val host = source("MountainTextureView.kt")
        assertTrue(source("HomeMountain.kt").contains("setRenderScale(HOME_MOUNTAIN_RENDER_SCALE)"))
        assertTrue(host.contains("owner.attach(surface, width, height, renderScale, paused)"))
        assertTrue(host.contains("renderer?.setViewport(width, height, renderScale)"))
        assertTrue(host.contains("surface.setDefaultBufferSize(size.width, size.height)"))
        assertTrue(host.contains("viewportWidth = size.width"))
        assertTrue(host.contains("viewportHeight = size.height"))
        assertTrue(host.contains("scene.draw(viewportWidth, viewportHeight, clear = true)"))
    }

    @Test
    fun resizingRecreatesOnlyTheWindowSurfaceNotTheStaticMeshOrContext() {
        val resize = source("MountainTextureView.kt")
            .substringAfter("private fun resizeBuffer(")
            .substringBefore("fun setPaused(")
        val destroy = resize.indexOf("eglDestroySurface(")
        val size = resize.indexOf("setDefaultBufferSize(")
        val create = resize.indexOf("createWindowSurface(surface)")
        assertTrue("old EGL drawable is disconnected before changing its size", destroy >= 0 && destroy < size)
        assertTrue("new EGL drawable picks up the scaled buffer size", size < create)
        assertFalse(resize.contains("eglCreateContext"))
        assertFalse(resize.contains("scene.init()"))
        assertFalse(resize.contains("scene.release()"))
    }

    @Test
    fun renderingRemainsCoalescedOnDemandAndLifecycleAware() {
        val host = source("MountainTextureView.kt")
        val home = source("HomeMountain.kt")
        assertTrue(host.contains("drawPending.compareAndSet(false, true)"))
        assertTrue(host.contains("if (failed || paused || eglSurface == EGL14.EGL_NO_SURFACE) return"))
        assertTrue(home.contains("Lifecycle.Event.ON_PAUSE -> current?.onPause()"))
        assertTrue(home.contains("Lifecycle.Event.ON_RESUME -> current?.onResume()"))
        assertTrue(home.contains("onRelease = { it.release();"))
        assertFalse(host.contains("Choreographer"))
        assertFalse(host.contains("postDelayed"))
    }

    private fun source(name: String): String {
        val relative = "src/main/java/com/alijafari/red/astronomy/ui/mountain/$name"
        return listOf(File(relative), File("app/$relative"))
            .firstOrNull { it.isFile }?.readText() ?: error("Cannot locate $relative")
    }
}
