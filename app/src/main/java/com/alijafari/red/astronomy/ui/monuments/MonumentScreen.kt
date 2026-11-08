package com.alijafari.red.astronomy.ui.monuments

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import kotlinx.coroutines.delay

// ------------------------------------------------------------------------------------------------
// The monument pages: one restrained editorial surface, one accent, no card grid.
//
// Persepolis and Mil-e Azhdaha are places, not catalogue entries, so their pages are built like a
// printed plate rather than like a dashboard: a full-bleed photograph under a single headline, a
// generous lead, a band of four cited measurements divided by hairlines (not four cards), numbered
// chapters with photographs at full width, one pull quote, one era list, and a quiet colophon. The
// only colour on the page is the paper (#F3EBDD), the ink (#0B0A08) and one warm limestone gold
// (#C9A063); everything else is a value of those three. No second accent, no chip, no redundant
// container: hierarchy comes from scale, spacing and the hairline.
//
// Motion is deliberately slow, so it reads as weight rather than as animation:
//  * the page itself is placed by MainActivity's shared transition (a scale/fade lift off the canvas);
//  * the hero photograph has a 26-second Ken Burns drift and parallaxes under the finger;
//  * every block rises 32 dp and fades in once, on a soft custom easing, when it is first composed;
//  * the hero caption lands a beat after the photograph;
//  * the back button compresses under the finger with a spring;
//  * a slender title bar fades in as the hero leaves, over a gradient rather than a hard edge.
// ------------------------------------------------------------------------------------------------

private val MonumentInk = Color(0xFF0B0A08)
private val MonumentPaper = Color(0xFFF3EBDD)
private val MonumentBody = Color(0xFFD9CEBC)
private val MonumentMuted = Color(0xFFA2957F)
private val MonumentGold = Color(0xFFC9A063)
private val MonumentHairline = Color(0x2EF3EBDD)

/** Lalezar, the bundled Persian display face (SIL OFL). Headlines only, never body copy. */
private val MonumentDisplayFamily = FontFamily(Font(R.font.lalezar_regular, FontWeight.Normal))

/** The one easing every reveal shares: quick to leave, slow to settle. */
private val MonumentEase = CubicBezierEasing(0.16f, 0.84f, 0.24f, 1f)

/**
 * The dedicated page for one monument that stands in the Home landscape.
 *
 * Self-contained and stateful only in its scroll position: it is rendered above everything else by
 * [com.alijafari.red.astronomy.MainActivity], so the floating navigation bar and every other overlay
 * are behind it. [onClose] is called by the back button and by the system back gesture alike.
 */
@Composable
fun MonumentScreen(
    monument: MonumentId,
    isPersian: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true, onBack = onClose)

    val content = remember(monument, isPersian) { MonumentCopy.of(monument, isPersian) }
    val listState = rememberLazyListState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MonumentInk)
            .testTag("monument_screen")
    ) {
        val heroHeight = (maxHeight * 0.74f).coerceIn(340.dp, 620.dp)
        val heroHeightPx = with(LocalDensity.current) { heroHeight.toPx() }.coerceAtLeast(1f)

        // How far the hero has scrolled away, 0..1. It drives both the parallax and the title bar, so
        // the two can never disagree about where the photograph has gone.
        val scrollOffset by remember(heroHeightPx) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    heroHeightPx
                }
            }
        }
        val heroProgress = (scrollOffset / heroHeightPx).coerceIn(0f, 1f)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("monument_scroll")
        ) {
            item(key = "monument_hero") {
                MonumentHero(
                    content = content,
                    progress = heroProgress,
                    height = heroHeight,
                    isPersian = isPersian
                )
            }
            item(key = "monument_lead") {
                MonumentLead(content = content, isPersian = isPersian)
            }
            content.blocks.forEachIndexed { index, block ->
                item(key = "monument_block_$index") {
                    when (block) {
                        is MonumentBlock.Chapter -> MonumentChapterBlock(block.chapter, isPersian)
                        is MonumentBlock.Figure -> MonumentFigureBlock(block.figure)
                        is MonumentBlock.Quote -> MonumentQuoteBlock(block.quote, isPersian)
                        is MonumentBlock.Timeline -> MonumentTimelineBlock(block.entries, isPersian)
                    }
                }
            }
            item(key = "monument_colophon") {
                MonumentColophon(content = content)
            }
        }

        MonumentTopBar(
            title = content.title,
            isPersian = isPersian,
            showTitle = heroProgress > 0.72f,
            onClose = onClose
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Hero
// ------------------------------------------------------------------------------------------------

@Composable
private fun MonumentHero(
    content: MonumentContent,
    progress: Float,
    height: Dp,
    isPersian: Boolean
) {
    // A slow drift on the photograph itself, independent of the finger, so the plate is never
    // completely still: 26 s out and back is below the threshold of "an animation is running".
    val drift = rememberInfiniteTransition(label = "monument_hero_drift")
    val zoom by drift.animateFloat(
        initialValue = 1.03f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "monument_hero_zoom"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        Image(
            painter = painterResource(content.heroRes),
            contentDescription = content.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Ken Burns plus parallax: the stone falls slower than the page it sits on.
                    val scale = zoom * (1f + progress * 0.16f)
                    scaleX = scale
                    scaleY = scale
                    translationY = progress * size.height * 0.24f
                }
        )

        // Two gradients, one job each: the top one keeps the status bar and back affordance legible
        // on a white sky, the bottom one dissolves the photograph into the page's ink so the
        // headline never sits on a hard edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.52f),
                        0.20f to Color.Black.copy(alpha = 0.14f),
                        0.52f to Color.Transparent,
                        0.76f to MonumentInk.copy(alpha = 0.58f),
                        0.92f to MonumentInk.copy(alpha = 0.94f),
                        1.0f to MonumentInk
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 30.dp)
        ) {
            Reveal(delayMillis = 90) {
                Column {
                    Text(
                        text = content.kicker,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            letterSpacing = if (isPersian) 0.sp else 2.4.sp,
                            color = MonumentGold
                        ),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = content.title,
                        style = TextStyle(
                            fontFamily = if (isPersian) MonumentDisplayFamily else FontFamily.Default,
                            fontWeight = if (isPersian) FontWeight.Normal else FontWeight.Bold,
                            fontSize = if (isPersian) 52.sp else 40.sp,
                            lineHeight = if (isPersian) 62.sp else 46.sp,
                            letterSpacing = if (isPersian) 0.sp else (-1).sp,
                            color = MonumentPaper
                        ),
                        maxLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = content.subtitle,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = MonumentPaper.copy(alpha = 0.82f)
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    MonumentMetaRow(content.meta)
                }
            }
        }
    }
}

@Composable
private fun MonumentMetaRow(items: List<String>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(MonumentGold.copy(alpha = 0.85f))
                )
            }
            Text(
                text = item,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.sp,
                    color = MonumentPaper.copy(alpha = 0.62f)
                ),
                maxLines = 1
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Lead and measurements
// ------------------------------------------------------------------------------------------------

@Composable
private fun MonumentLead(content: MonumentContent, isPersian: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 30.dp)
    ) {
        Reveal {
            Column {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(2.dp)
                        .background(MonumentGold)
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = content.lead,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Normal,
                        fontSize = if (isPersian) 17.sp else 16.5.sp,
                        lineHeight = if (isPersian) 36.sp else 29.sp,
                        color = MonumentBody
                    )
                )
                Spacer(Modifier.height(38.dp))
                MonumentStats(content.stats, isPersian)
            }
        }
    }
}

@Composable
private fun MonumentStats(stats: List<MonumentStat>, isPersian: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        stats.chunked(2).forEach { rowStats ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                rowStats.forEach { stat ->
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.75.dp)
                                .background(MonumentHairline)
                        )
                        Spacer(Modifier.height(13.dp))
                        Text(
                            text = stat.value,
                            style = TextStyle(
                                fontFamily = if (isPersian) MonumentDisplayFamily else FontFamily.Default,
                                fontWeight = if (isPersian) FontWeight.Normal else FontWeight.SemiBold,
                                fontSize = if (isPersian) 27.sp else 22.sp,
                                lineHeight = if (isPersian) 34.sp else 28.sp,
                                letterSpacing = 0.sp,
                                color = MonumentPaper
                            ),
                            maxLines = 1
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stat.label,
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 18.sp,
                                color = MonumentMuted
                            )
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }
                if (rowStats.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Blocks
// ------------------------------------------------------------------------------------------------

@Composable
private fun MonumentChapterBlock(chapter: MonumentChapter, isPersian: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 54.dp)
    ) {
        Reveal {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chapter.index,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            letterSpacing = if (isPersian) 0.sp else 1.6.sp,
                            color = MonumentGold
                        ),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                            .height(0.75.dp)
                            .background(MonumentHairline)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = chapter.title,
                    style = TextStyle(
                        fontFamily = if (isPersian) MonumentDisplayFamily else FontFamily.Default,
                        fontWeight = if (isPersian) FontWeight.Normal else FontWeight.SemiBold,
                        fontSize = if (isPersian) 27.sp else 23.sp,
                        lineHeight = if (isPersian) 40.sp else 32.sp,
                        letterSpacing = 0.sp,
                        color = MonumentPaper
                    )
                )
                Spacer(Modifier.height(16.dp))
                chapter.paragraphs.forEachIndexed { index, paragraph ->
                    Text(
                        text = paragraph,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = if (isPersian) 15.5.sp else 15.sp,
                            lineHeight = if (isPersian) 34.sp else 27.sp,
                            color = MonumentBody
                        )
                    )
                    if (index < chapter.paragraphs.lastIndex) Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MonumentFigureBlock(figure: MonumentFigure) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp)
    ) {
        Reveal {
            Column {
                Image(
                    painter = painterResource(figure.imageRes),
                    contentDescription = figure.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(figure.aspectRatio)
                        .clipToBounds()
                )
                Spacer(Modifier.height(13.dp))
                Row(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .width(14.dp)
                            .height(1.dp)
                            .background(MonumentGold.copy(alpha = 0.75f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = figure.caption,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.5.sp,
                            lineHeight = 20.sp,
                            color = MonumentMuted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MonumentQuoteBlock(quote: MonumentQuote, isPersian: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 50.dp)
    ) {
        Reveal {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MonumentGold.copy(alpha = 0.85f))
                )
                Column(modifier = Modifier.padding(start = 18.dp)) {
                    Text(
                        text = quote.text,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Medium,
                            fontSize = if (isPersian) 20.sp else 19.sp,
                            lineHeight = if (isPersian) 38.sp else 31.sp,
                            color = MonumentPaper
                        )
                    )
                    if (quote.attribution != null) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = quote.attribution,
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.5.sp,
                                letterSpacing = if (isPersian) 0.sp else 1.2.sp,
                                color = MonumentMuted
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonumentTimelineBlock(entries: List<MonumentTimelineEntry>, isPersian: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 54.dp)
    ) {
        Reveal {
            Column {
                Text(
                    text = if (isPersian) "گاه‌شمار" else "TIMELINE",
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.5.sp,
                        letterSpacing = if (isPersian) 0.sp else 2.2.sp,
                        color = MonumentGold
                    )
                )
                Spacer(Modifier.height(12.dp))
                entries.forEachIndexed { index, entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.75.dp)
                            .background(MonumentHairline)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = entry.year,
                            modifier = Modifier.width(if (isPersian) 96.dp else 104.dp),
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = MonumentGold.copy(alpha = 0.92f)
                            )
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = entry.event,
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                fontFamily = LocalAppFontFamily.current,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 24.sp,
                                color = MonumentBody
                            )
                        )
                    }
                    if (index == entries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.75.dp)
                                .background(MonumentHairline)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonumentColophon(content: MonumentContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 56.dp)
    ) {
        Reveal {
            Column {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                        .size(7.dp)
                        .rotate(45f)
                        .background(MonumentGold.copy(alpha = 0.9f))
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = content.credits,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        lineHeight = 18.sp,
                        color = MonumentMuted.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = content.sources,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        lineHeight = 18.sp,
                        color = MonumentMuted.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Chrome and motion primitives
// ------------------------------------------------------------------------------------------------

@Composable
private fun BoxScope.MonumentTopBar(
    title: String,
    isPersian: Boolean,
    showTitle: Boolean,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = showTitle,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to MonumentInk.copy(alpha = 0.94f),
                            0.62f to MonumentInk.copy(alpha = 0.62f),
                            1f to Color.Transparent
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonumentBackButton(isPersian = isPersian, onClose = onClose)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { -it / 4 },
                exit = fadeOut(tween(160))
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MonumentPaper
                    ),
                    maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            // Balances the back button so the title stays optically centred.
            Spacer(Modifier.size(42.dp))
        }
    }
}

@Composable
private fun MonumentBackButton(isPersian: Boolean, onClose: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "monument_back_scale"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(buttonScale)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(0.75.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClose
            )
            .testTag("monument_back"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (isPersian) "بازگشت" else "Back",
            tint = MonumentPaper,
            modifier = Modifier.size(19.dp)
        )
    }
}

/**
 * The single entrance animation of every block on the page.
 *
 * The first composition of a block is the moment it is scrolled into view (a LazyColumn composes
 * only what is visible), so this is both an entrance and a scroll reveal, and it costs one
 * animatable per block rather than a scroll listener driving the whole list.
 */
@Composable
private fun Reveal(delayMillis: Int = 0, content: @Composable () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        started = true
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 760, easing = MonumentEase),
        label = "monument_reveal"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 34.dp.toPx()
        }
    ) {
        content()
    }
}
