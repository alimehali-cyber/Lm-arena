package com.alijafari.red.astronomy.ui.monument

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alijafari.red.astronomy.ui.theme.LocalAppFontFamily
import com.alijafari.red.astronomy.ui.theme.RedCornerRadius
import com.alijafari.red.astronomy.ui.theme.RedTheme
import com.alijafari.red.astronomy.ui.theme.squircleShape

/**
 * The page of one monument in the Home landscape: a photograph first, then the record.
 *
 * ### Why this is not the object detail modal
 *
 * A star's page is a data sheet — magnitude, altitude, rise and set. A monument has none of those
 * numbers; it has a place, a date, a material, a story and photographs. Forcing it through the
 * object modal would have meant either empty rows or a second kind of content inside a screen built
 * around the first. So it is its own page, and it is allowed to be quiet: one photograph at the top,
 * generous type, four figures, numbered sections separated by white space rather than by cards.
 *
 * ### The layout rules it follows
 *
 *  - **One continuous surface.** The page has no cards, no filled panels and no second accent. The
 *    only chrome is a hairline, the number of each section, and the one accent used at low opacity
 *    on those numbers and on the quote's rule. Everything else is type on the theme's background.
 *  - **Photographs go edge to edge.** A photograph indented by the text margin reads as decoration; a
 *    full-bleed one reads as a place. Captions sit under it in the margin, in the same ink as the
 *    credits.
 *  - **Measure over density.** The body column is `24 dp` from each edge with a `31 sp` line height —
 *    long paragraphs stay readable on a phone in Persian, whose ascenders and descenders need the
 *    room more than Latin text does.
 *  - **It ends by going back to the sky.** The last block is not a footnote: it says what this
 *    monument does in *this* app's night model, and the page's only button returns there.
 *
 * Both languages are drawn through [LocalAppFontFamily], so the Persian page is Vazirmatn and the
 * English one the platform face, exactly as everywhere else in the app.
 */
@Composable
fun MonumentScreen(
    monument: MonumentId,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val page = remember(monument, isFa) { MonumentPages.of(monument, isFa) }

    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RedTheme.colors.background)
                .testTag("monument_screen")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("monument_screen_scroll"),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item(key = "hero") {
                    MonumentHero(page = page, isFa = isFa, onDismiss = onDismiss)
                }
                item(key = "lede") {
                    Text(
                        text = page.lede,
                        style = bodyStyle(size = 18.5.sp, lineHeight = 33.sp),
                        color = RedTheme.colors.textPrimary,
                        modifier = Modifier.padding(start = TextMargin, end = TextMargin, top = 28.dp)
                    )
                }
                item(key = "facts") {
                    MonumentFactStrip(page = page)
                }
                page.sections.forEachIndexed { index, section ->
                    item(key = "s-${section.number}") {
                        MonumentSectionText(section = section, first = index == 0)
                    }
                    val photo = section.photo
                    if (photo != null) {
                        item(key = "p-${section.number}") {
                            MonumentPhotoBreak(photo = photo)
                        }
                    }
                }
                item(key = "quote") {
                    MonumentQuote(quote = page.quote)
                }
                item(key = "sky") {
                    MonumentSkyBlock(page = page, onDismiss = onDismiss)
                }
                item(key = "credits") {
                    MonumentCredits(page = page)
                }
            }
        }
    }
}

private val TextMargin = 24.dp
private val SectionGap = 40.dp

/** The page's one body style: the locale face, with the size and leading the call site needs. */
@Composable
private fun bodyStyle(size: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit) =
    TextStyle(
        fontFamily = LocalAppFontFamily.current,
        fontWeight = FontWeight.Normal,
        fontSize = size,
        lineHeight = lineHeight
    )

// ------------------------------------------------------------------------------------------------
// Hero
// ------------------------------------------------------------------------------------------------

/**
 * The photograph, its scrim, and the name.
 *
 * The scrim is not decoration: a photograph of white limestone under a bright sky would swallow a
 * white title. It ramps from nothing at the top of the lower third to near-black at the bottom edge,
 * so the title always has ink beneath it and the picture above it is untouched.
 */
@Composable
private fun MonumentHero(page: MonumentPage, isFa: Boolean, onDismiss: () -> Unit) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val heroHeight = (screenHeightDp * 0.62f).coerceIn(360.dp, 580.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .testTag("monument_hero")
    ) {
        Image(
            painter = painterResource(id = page.hero.imageRes),
            contentDescription = page.hero.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom scrim: the title's ink.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.42f to Color.Black.copy(alpha = 0.06f),
                        0.62f to Color.Black.copy(alpha = 0.42f),
                        0.80f to Color.Black.copy(alpha = 0.78f),
                        1.00f to Color.Black.copy(alpha = 0.92f)
                    )
                )
        )

        // Top scrim: the back control's ink.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.42f),
                        1.0f to Color.Transparent
                    )
                )
        )

        Surface(
            onClick = onDismiss,
            shape = squircleShape(RedCornerRadius.pill),
            color = Color.Black.copy(alpha = 0.34f),
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.34f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 10.dp)
                .size(42.dp)
                .testTag("monument_screen_back")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isFa) "بازگشت" else "Back",
                    tint = Color(0xFFF7F4EF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = TextMargin, end = TextMargin, bottom = 22.dp)
        ) {
            Text(
                text = page.overline,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 1.4.sp
                ),
                color = Color.White.copy(alpha = 0.74f)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = page.title,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isFa) 38.sp else 40.sp,
                    lineHeight = if (isFa) 52.sp else 46.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = Color(0xFFFBF8F2)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = page.subtitle,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 24.sp
                ),
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Fact strip
// ------------------------------------------------------------------------------------------------

/**
 * Four figures on the theme's own background, divided by hairlines.
 *
 * Not a card: no fill, no radius, no border around the row. A thin vertical rule between the columns
 * is all the structure it needs, and it reads as a caption to the lede rather than as a widget.
 */
@Composable
private fun MonumentFactStrip(page: MonumentPage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TextMargin, end = TextMargin, top = 30.dp, bottom = 6.dp)
            .testTag("monument_facts"),
        verticalAlignment = Alignment.Top
    ) {
        page.facts.forEachIndexed { index, fact ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(0.5.dp)
                        .height(38.dp)
                        .background(RedTheme.colors.separator)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = fact.value,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = RedTheme.colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = fact.label,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    ),
                    color = RedTheme.colors.textTertiary,
                    maxLines = 2
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Sections
// ------------------------------------------------------------------------------------------------

/**
 * One numbered section: the number, the heading, then the paragraphs.
 *
 * The hairline above the number — never above the first section, whose heading follows the fact strip
 * — is the only divider on the page. It is deliberately short of full width: a rule that stops before
 * the edge reads as a pause, a full-width one reads as a table.
 */
@Composable
private fun MonumentSectionText(section: MonumentSection, first: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TextMargin, end = TextMargin)
            .padding(top = SectionGap)
    ) {
        if (!first) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(0.5.dp)
                    .background(RedTheme.colors.separator)
            )
            Spacer(Modifier.height(26.dp))
        }
        Text(
            text = section.number,
            style = TextStyle(
                fontFamily = LocalAppFontFamily.current,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 2.4.sp
            ),
            color = RedTheme.colors.accentGold.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = section.title,
            style = TextStyle(
                fontFamily = LocalAppFontFamily.current,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.2).sp
            ),
            color = RedTheme.colors.textPrimary
        )
        section.paragraphs.forEach { paragraph ->
            Spacer(Modifier.height(14.dp))
            Text(
                text = paragraph,
                style = bodyStyle(size = 15.sp, lineHeight = 31.sp),
                color = RedTheme.colors.textSecondary,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * A photograph across the full width, with its caption and credit in the text margin underneath.
 *
 * The caption is set in the page's smallest ink on purpose: it is there to be found when looked for,
 * not to compete with the picture.
 */
@Composable
private fun MonumentPhotoBreak(photo: MonumentPhoto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 34.dp)
    ) {
        Image(
            painter = painterResource(id = photo.imageRes),
            contentDescription = photo.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.32f)
        )
        Column(
            modifier = Modifier.padding(start = TextMargin, end = TextMargin, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = photo.caption,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                ),
                color = RedTheme.colors.textSecondary
            )
            Text(
                text = photo.credit,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp
                ),
                color = RedTheme.colors.textTertiary
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Quote
// ------------------------------------------------------------------------------------------------

/** One line, set large, with a short rule beside it. No quotation glyphs, no panel. */
@Composable
private fun MonumentQuote(quote: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TextMargin, end = TextMargin, top = SectionGap + 8.dp)
            .testTag("monument_quote"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(58.dp)
                .background(RedTheme.colors.accentGold.copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = quote,
            style = TextStyle(
                fontFamily = LocalAppFontFamily.current,
                fontWeight = FontWeight.Medium,
                fontSize = 19.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.1).sp
            ),
            color = RedTheme.colors.textPrimary
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Sky block and credits
// ------------------------------------------------------------------------------------------------

/**
 * The closing block: what this monument does in the app's own sky, and the way back to it.
 *
 * It sits on the theme's background like everything else, separated only by a hairline — the page has
 * earned its calm by this point and should not end in a panel.
 */
@Composable
private fun MonumentSkyBlock(page: MonumentPage, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TextMargin, end = TextMargin, top = SectionGap + 8.dp)
            .testTag("monument_sky_block")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(RedTheme.colors.separator)
        )
        Spacer(Modifier.height(26.dp))
        Text(
            text = page.skyTitle,
            style = TextStyle(
                fontFamily = LocalAppFontFamily.current,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                lineHeight = 28.sp
            ),
            color = RedTheme.colors.textPrimary
        )
        page.skyParagraphs.forEach { paragraph ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = paragraph,
                style = bodyStyle(size = 14.5.sp, lineHeight = 29.sp),
                color = RedTheme.colors.textSecondary
            )
        }
        Spacer(Modifier.height(24.dp))
        Surface(
            onClick = onDismiss,
            shape = squircleShape(RedCornerRadius.pill),
            color = Color.Transparent,
            border = BorderStroke(0.8.dp, RedTheme.colors.textPrimary.copy(alpha = 0.28f)),
            modifier = Modifier.testTag("monument_sky_action")
        ) {
            Text(
                text = page.skyAction,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp
                ),
                color = RedTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

/** Photographers, licences and the sources the prose is built on, at the very bottom. */
@Composable
private fun MonumentCredits(page: MonumentPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TextMargin, end = TextMargin, top = SectionGap)
            .navigationBarsPadding()
            .testTag("monument_credits")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(RedTheme.colors.separator)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = page.creditTitle,
            style = TextStyle(
                fontFamily = LocalAppFontFamily.current,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            ),
            color = RedTheme.colors.textTertiary
        )
        page.credits.forEach { line ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = line,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp,
                    lineHeight = 17.sp
                ),
                color = RedTheme.colors.textTertiary
            )
        }
    }
}
