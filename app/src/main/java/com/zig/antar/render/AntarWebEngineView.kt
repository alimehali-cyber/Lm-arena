package com.zig.antar.render

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zig.antar.config.AntarSimulationSettings
import com.zig.antar.config.AntarTelemetry

/**
 * Imperative handle to drive high-rate camera updates and parameter syncs into the hosted WebGL2
 * Kerr-Schild ray tracer without recomposing the `AndroidView`.
 */
class AntarEngineHandle {
    internal var webView: WebView? = null
    internal var pageLoaded: Boolean = false
    private var pendingSettingsJson: String? = null

    fun syncSettings(
        settings: AntarSimulationSettings,
        panXDp: Double,
        panYDp: Double,
        dockShiftDp: Double,
        yawRad: Double,
        pitchRad: Double
    ) {
        val json = AntarSimulationPayload.buildSettingsJson(
            settings = settings,
            panXDp = panXDp,
            panYDp = panYDp,
            dockShiftDp = dockShiftDp,
            yawRad = yawRad,
            pitchRad = pitchRad
        )
        val view = webView
        if (view != null && pageLoaded) {
            view.evaluateJavascript(
                "window.__antarBridge&&window.__antarBridge.applySettings($json);",
                null
            )
        } else {
            pendingSettingsJson = json
        }
    }

    fun syncCameraFast(
        settings: AntarSimulationSettings,
        panXDp: Double,
        panYDp: Double,
        dockShiftDp: Double,
        yawRad: Double,
        pitchRad: Double
    ) {
        val view = webView ?: return
        if (!pageLoaded) return
        val js = AntarSimulationPayload.buildFastCameraCall(
            azimuthDeg = settings.azimuthDeg,
            inclinationDeg = settings.inclinationDeg,
            distanceM = settings.distanceM,
            fovDeg = settings.fovDeg,
            rollDeg = settings.rollDeg,
            panXDp = panXDp,
            panYDp = panYDp,
            dockShiftDp = dockShiftDp,
            yawRad = yawRad,
            pitchRad = pitchRad,
            modeKey = settings.cameraMode.jsKey
        )
        view.evaluateJavascript(js, null)
    }

    fun restartBinary() {
        val view = webView ?: return
        if (!pageLoaded) return
        view.evaluateJavascript(
            "window.__antarBridge&&window.__antarBridge.restartBinary();",
            null
        )
    }

    internal fun onPageReady() {
        pageLoaded = true
        val pending = pendingSettingsJson
        pendingSettingsJson = null
        if (pending != null) {
            webView?.evaluateJavascript(
                "window.__antarBridge&&window.__antarBridge.applySettings($pending);",
                null
            )
        }
    }
}

/**
 * Pure-Kotlin parser for the flat JSON snapshot emitted by `window.__antarBridge.snapshot()`.
 * Avoids `org.json.JSONObject` so unit tests run directly on the JVM without Android stubs.
 */
internal fun parseAntarTelemetryJson(
    json: String,
    previous: AntarTelemetry = AntarTelemetry()
): AntarTelemetry {
    if (json.isBlank()) return previous

    fun strOrNull(key: String): String? {
        val m = Regex("\"$key\"\\s*:\\s*(null|\"((?:\\\\.|[^\"\\\\])*)\")").find(json) ?: return null
        val raw = m.groupValues[1]
        if (raw == "null") return null
        return m.groupValues[2]
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
    }

    fun bool(key: String, default: Boolean): Boolean {
        val m = Regex("\"$key\"\\s*:\\s*(true|false)").find(json) ?: return default
        return m.groupValues[1] == "true"
    }

    fun num(key: String, default: Double): Double {
        val m = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)").find(json) ?: return default
        return m.groupValues[1].toDoubleOrNull()?.takeIf { it.isFinite() } ?: default
    }

    val err = strOrNull("error")
    return previous.copy(
        engineReady = bool("ready", previous.engineReady),
        errorMessage = err,
        fps = num("fps", previous.fps.toDouble()).toInt().coerceIn(1, 240),
        renderScale = num("scale", previous.renderScale).coerceIn(0.25, 1.0),
        traceWidth = num("rw", previous.traceWidth.toDouble()).toInt().coerceAtLeast(0),
        traceHeight = num("rh", previous.traceHeight.toDouble()).toInt().coerceAtLeast(0),
        cameraDistanceM = num("dist", previous.cameraDistanceM),
        cameraInclinationDeg = num("incl", previous.cameraInclinationDeg),
        cameraAzimuthDeg = num("az", previous.cameraAzimuthDeg),
        plungeActive = bool("plunge", previous.plungeActive),
        plungeRadiusM = num("plungeR", previous.plungeRadiusM),
        plungeProperTimeM = num("plungeTau", previous.plungeProperTimeM),
        insideHorizon = bool("inside", previous.insideHorizon),
        tourActive = bool("tour", previous.tourActive),
        binarySeparationM = num("binSep", previous.binarySeparationM),
        binaryVelocityC = num("binV", previous.binaryVelocityC),
        binaryGwFreqHz = num("binFgw", previous.binaryGwFreqHz),
        binaryTimeToMergerSec = num("binTMerge", previous.binaryTimeToMergerSec),
        binaryMerged = bool("binMerged", previous.binaryMerged)
    )
}

internal class AntarJsHostBridge(
    private val mainHandler: Handler,
    private val onSnapshotJson: (String) -> Unit,
    private val reportRuntimeError: (String) -> Unit
) {
    @JavascriptInterface
    fun onTelemetry(json: String?) {
        if (json.isNullOrBlank()) return
        mainHandler.post { onSnapshotJson(json) }
    }

    @JavascriptInterface
    fun onRuntimeError(message: String?) {
        val safeMessage = message?.trim()?.take(500).orEmpty()
        if (safeMessage.isEmpty()) return
        mainHandler.post { reportRuntimeError(safeMessage) }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun AntarWebEngineView(
    initialSettings: AntarSimulationSettings,
    handle: AntarEngineHandle,
    onTelemetryUpdate: (AntarTelemetry) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnTelemetry by rememberUpdatedState(onTelemetryUpdate)
    var currentTelemetry by remember { mutableStateOf(AntarTelemetry()) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bootSettings = remember { initialSettings.normalize() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, handle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> handle.webView?.onResume()
                Lifecycle.Event.ON_STOP -> handle.webView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(bootSettings) {
        // Ensure initial settings (including tour/plunge if later selected) sync once page loads.
        handle.syncSettings(
            settings = bootSettings,
            panXDp = 0.0,
            panYDp = 0.0,
            dockShiftDp = 0.0,
            yawRad = 0.0,
            pitchRad = 0.0
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Keep WebView on the window's hardware-accelerated path. An extra hardware
                // layer over a transparent background can make WebGL canvases disappear on Android.
                setBackgroundColor(Color.BLACK)
                setLayerType(View.LAYER_TYPE_NONE, null)
                isHorizontalScrollBarEnabled = false
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                isClickable = false
                isFocusable = false
                isLongClickable = false
                setOnTouchListener { _, _ -> false }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    mediaPlaybackRequiresUserGesture = false
                }

                val hostBridge = AntarJsHostBridge(
                    mainHandler = mainHandler,
                    onSnapshotJson = { rawJson ->
                        val next = parseAntarTelemetryJson(rawJson, currentTelemetry)
                        currentTelemetry = next
                        latestOnTelemetry(next)
                    },
                    reportRuntimeError = { message ->
                        val next = currentTelemetry.copy(errorMessage = message)
                        currentTelemetry = next
                        latestOnTelemetry(next)
                    }
                )
                addJavascriptInterface(hostBridge, "AntarHostBridge")

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        if (message.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                            hostBridge.onRuntimeError(
                                "JavaScript: ${message.message().take(350)} (${message.sourceId().substringAfterLast('/')} : ${message.lineNumber()})"
                            )
                        }
                        return super.onConsoleMessage(message)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        handle.onPageReady()
                    }
                }

                handle.webView = this
                handle.pageLoaded = false
                val simulationUrl = AntarSimulationPayload.buildBaseUrl(bootSettings)
                loadDataWithBaseURL(
                    simulationUrl,
                    AntarSimulationPayload.buildHostedHtml(),
                    "text/html",
                    "UTF-8",
                    simulationUrl
                )
            }
        },
        onRelease = { view ->
            handle.webView = null
            handle.pageLoaded = false
            runCatching { view.removeJavascriptInterface("AntarHostBridge") }
            view.stopLoading()
            view.loadUrl("about:blank")
            view.destroy()
        }
    )
}
