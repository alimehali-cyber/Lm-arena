package com.alijafari.red.astronomy.ui.satellites

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.disk.DiskCache
import coil.request.ImageRequest
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.SatelliteItem
import com.alijafari.red.astronomy.ui.satellites.core.isolate
import kotlin.math.roundToInt
import org.json.JSONObject

internal enum class Kind(val label: Int) {
    PHOTO(R.string.sat_photo),
    RENDER(R.string.sat_render),
    ARTIST_CONCEPT(R.string.sat_concept),
}

internal data class SatImage(
    val uri: String,
    val kind: Kind,
    val credit: String,
    val creditFa: String,
    val author: String,
    val license: String,
    val sourceUrl: String,
    val focal: Offset,
    val placeholderColor: Color,
    val altEn: String,
    val altFa: String,
)

internal data class SatMedia(val images: List<SatImage>)

internal object MediaLibrary {
    private var store: Map<String, SatMedia>? = null
    private var loader: ImageLoader? = null

    @Synchronized
    fun loader(context: Context) =
        loader
            ?: ImageLoader.Builder(context.applicationContext)
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("satellite-photos"))
                        .maxSizeBytes(50L * 1024 * 1024)
                        .build()
                }
                .build()
                .also { loader = it }

    @Synchronized
    fun read(context: Context): Map<String, SatMedia> =
        store
            ?: run {
                val entries =
                    context.assets.open("satellites/sat_media_manifest.json").bufferedReader().use {
                        JSONObject(it.readText()).getJSONObject("spacecraft")
                    }
                entries.keys().asSequence().associateWith { key ->
                    val craft = entries.getJSONObject(key)
                    val a = craft.getJSONArray("images")
                    SatMedia(
                        buildList {
                            if (craft.getString("status") == "VERIFIED")
                                for (i in 0 until minOf(6, a.length())) {
                                    val o = a.getJSONObject(i)
                                    if (!o.optBoolean("verified") || o.optString("license").isBlank()) continue
                                    val focal = o.getJSONArray("focal")
                                    add(SatImage(
                                        o.getString("uri"),
                                        Kind.valueOf(o.getString("kind")),
                                        o.getString("credit"),
                                        o.getString("creditFa"),
                                        o.getString("author"),
                                        o.getString("license"),
                                        o.getString("sourceUrl"),
                                        Offset(focal.getDouble(0).toFloat(), focal.getDouble(1).toFloat()),
                                        Color(android.graphics.Color.parseColor(o.getString("placeholderColor"))),
                                        o.getString("altEn"),
                                        o.getString("altFa"),
                                    ))
                                }
                        },
                    )
                }
            }
                .also { store = it }
}

@Composable
private fun Plate(id: String, t: ZigSatTokens) {
    BoxWithConstraints(Modifier.fillMaxSize().background(t.card), contentAlignment = Alignment.Center) {
        Icon(
            SpacecraftArt.vector(id, Lod.XL),
            null,
            Modifier.size(maxWidth * .56f),
            tint = if (id == "starlink") t.starlink else t.inkPrimary,
        )
    }
}

@Composable
private fun Caption(image: SatImage, fa: Boolean, t: ZigSatTokens) {
    val uri = LocalUriHandler.current
    val credit = if (fa) image.creditFa else image.credit
    SatText(
        stringResource(image.kind.label) +
            " · " +
            credit +
            (if (image.author.isNotBlank()) " · " + isolate(image.author) else ""),
        t.inkSecondary,
        Modifier.heightIn(min = 48.dp).clickable { uri.openUri(image.sourceUrl) }.padding(vertical = 8.dp),
        size = 11,
    )
}

@Composable
internal fun MediaHero(item: SatelliteItem, t: ZigSatTokens, fa: Boolean, scrollPx: Int, shared: Modifier = Modifier) {
    val context = LocalContext.current
    val rule = remember(item) { shapeOf(item) }
    val media =
        remember(item) {
            val library = MediaLibrary.read(context)
            library[item.noradId.toString()] ?: library[rule.id]?.takeIf { rule.tier != 3 }
        }
    val images = media?.images.orEmpty()
    val pages = rememberPagerState { images.size.coerceAtLeast(1) }
    var failed by remember(images) { mutableStateOf(emptySet<String>()) }
    var viewer by remember { mutableStateOf<SatImage?>(null) }
    val current = images.getOrNull(pages.currentPage)?.takeUnless { it.uri in failed }
    val fallback = stringResource(if (rule.tier == 3) R.string.sat_plate_generic else R.string.sat_plate_none)
    Column(Modifier.fillMaxWidth().testTag("detail_hero")) {
        Box(shared.fillMaxWidth().aspectRatio(1.6f).clip(SatShapes.hero).background(t.card)) {
            if (images.isEmpty()) Plate(rule.id, t)
            else
                HorizontalPager(pages) { page ->
                    val photo = images[page]
                    if (photo.uri in failed) Plate(rule.id, t)
                    else
                        MediaPhoto(
                            photo,
                            fa,
                            Modifier.fillMaxSize()
                                .clickable { viewer = photo }
                                .graphicsLayer {
                                    translationY = scrollPx * .5f
                                },
                            error = { failed = failed + photo.uri },
                        )
                }
            if (images.size > 1)
                SatText(
                    SatFmt.int(pages.currentPage + 1, fa) + " / " + SatFmt.int(images.size, fa),
                    t.inkPrimary,
                    Modifier.align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(t.card, SatShapes.capsule)
                        .padding(horizontal = 8.dp),
                    size = 11,
                )
        }
        if (current == null) SatText(fallback, t.inkSecondary, Modifier.padding(top = 8.dp), size = 11)
        else Caption(current, fa, t)
        SatText(shortNameOf(item, fa), t.inkPrimary, Modifier.padding(top = 8.dp), size = 24, strong = true)
        if (item.isTrain) SatText(stringResource(R.string.sat_train_long, SatFmt.int(item.trainCount, fa)), t.inkSecondary, size = 11)
        SatText(
            listOf(
                    stringResource(item.taxonomy.label),
                    stringResource(R.string.sat_norad) + " " + isolate(item.noradId.toString()),
                    item.designation.takeIf { it.isNotBlank() }?.let(::isolate),
                )
                .filterNotNull()
                .joinToString(" · "),
            t.inkSecondary,
        )
    }
    viewer?.let { image ->
        Dialog(
            onDismissRequest = { viewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            var zoom by remember { mutableFloatStateOf(1f) }
            var pan by remember { mutableStateOf(Offset.Zero) }
            var down by remember { mutableFloatStateOf(0f) }
            Box(
                Modifier.fillMaxSize().background(t.canvas).systemBarsPadding().pointerInput(zoom > 1f) {
                    if (zoom <= 1f)
                        detectVerticalDragGestures(
                            onDragStart = { down = 0f },
                            onDragEnd = { if (down > 80.dp.toPx()) viewer = null },
                        ) { c, d ->
                            down += d
                            c.consume()
                        }
                },
            ) {
                MediaPhoto(
                    image,
                    fa,
                    Modifier.align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .transformable(
                            rememberTransformableState { scale, move, _ ->
                                zoom = (zoom * scale).coerceIn(1f, 5f)
                                pan = if (zoom > 1f) pan + move else Offset.Zero
                            },
                        )
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                    fit = true,
                    error = {
                        viewer = null
                        failed = failed + image.uri
                    },
                )
                ZigSatIconButton(
                    Icons.Rounded.Close,
                    stringResource(R.string.sat_close),
                    t,
                    { viewer = null },
                    Modifier.align(Alignment.TopStart).padding(16.dp),
                )
                Box(Modifier.align(Alignment.BottomStart).padding(16.dp)) { Caption(image, fa, t) }
            }
        }
    }
}

@Composable
private fun MediaPhoto(image: SatImage, fa: Boolean, modifier: Modifier, fit: Boolean = false, error: () -> Unit) {
    val context = LocalContext.current
    val model = remember(image.uri) { ImageRequest.Builder(context).data(image.uri).crossfade(200).build() }
    val focal =
        remember(image.focal) {
            object : Alignment {
                override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection) =
                    IntOffset(
                        ((space.width - size.width) * image.focal.x).roundToInt(),
                        ((space.height - size.height) * image.focal.y).roundToInt(),
                    )
            }
        }
    SubcomposeAsyncImage(
        model,
        if (fa) image.altFa else image.altEn,
        imageLoader = MediaLibrary.loader(context),
        modifier = modifier.background(image.placeholderColor),
        alignment = focal,
        contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
        onError = { error() },
    )
}
