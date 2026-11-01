package com.alijafari.red.astronomy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alijafari.red.astronomy.R
import com.alijafari.red.astronomy.astro_engine.EclipseEngine

/**
 * The eclipse as a picture: a photographic plate, chosen by the *kind* of eclipse being shown.
 *
 * There are six plates and each one is the phenomenon itself, not a stand-in:
 *
 * | Eclipse | Plate |
 * |---|---|
 * | total / hybrid solar | totality — corona, chromosphere, diamond ring |
 * | annular solar | the ring of fire |
 * | partial solar | a bitten solar disc with its glare |
 * | total lunar | the copper "blood moon" disc |
 * | partial lunar | the Moon with the umbra biting into one limb |
 * | penumbral lunar | an almost-full Moon with one limb faintly dimmed — which is all a penumbral eclipse is |
 *
 * They are 1024 px renders generated for this app, shipped as WebP in `drawable-nodpi` so no density
 * scaling is applied to an image that is already the right resolution.
 *
 * **The sky is not part of the picture.** Every plate carries its own alpha channel, keyed from the
 * photograph: the lunar discs are cut to their limb, and on the solar plates the occluding disc and the
 * photosphere stay solid (that black bite *is* the eclipse) while the corona and the glare survive by their
 * own luminance and fade out well before the edge. So the plate is a Moon or an eclipse floating on the
 * card — there is no disc of black sky behind it, and therefore no black ring around it in either theme.
 * (That dark surround was the "ring": it was the photograph's background, cropped to a circle.)
 *
 * **Nothing is softened.** The plates used to be cut with a `DstIn` radial mask, which faded their edges
 * into the card and washed out the corona's outermost streamers and the sharp limb; the penumbral case
 * leaned on the same mask instead of having a plate of its own. The picture is clipped to a hard circle at
 * full opacity — a crisp limb against the card — and lifted off it by [elevationDp] of a soft shadow, so it
 * reads as a disc resting in front of the surface rather than a smudge printed on it.
 *
 * **One size.** Each plate is scaled so the phenomenon it shows fills its square exactly like the Moon card's
 * Moon does: the lunar discs are drawn at the same diameter as [MoonPhaseRender]'s disc (their alpha ends on
 * the same circle the Moon's limb does, so the pair read as one size in the two cards), and the solar plates
 * are scaled to their corona's own outer falloff rather than to a disc of leftover sky.
 *
 * Nothing is animated and nothing is random: one image, one clip, so a recomposition cannot make an eclipse
 * flicker.
 */
@Composable
fun EclipseRender(
    type: EclipseEngine.EclipseType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    /** Height of the plate's shadow, in dp. 0 leaves it flat on the surface. */
    elevationDp: Dp = 10.dp,
    /** Shadow ink. Default black reads correctly on both the dark and the light theme's card. */
    shadowColor: Color = Color.Black
) {
    // Held in a local so the semantics lambda cannot capture the receiver's own property name.
    val described = contentDescription
    val semanticsModifier = if (described != null) {
        modifier.semantics { this.contentDescription = described }
    } else {
        modifier
    }

    val plate = plateFor(type)

    Box(
        modifier = semanticsModifier
            // The lift is a shadow *under* the disc, not a ring drawn around it: a soft, mostly-ambient
            // black at a fraction of full opacity, so a bright corona never gains a dark halo.
            .shadow(
                elevation = elevationDp,
                shape = CircleShape,
                clip = false,
                ambientColor = shadowColor.copy(alpha = 0.55f),
                spotColor = shadowColor.copy(alpha = 0.45f)
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = plate),
            contentDescription = null,
            // The plate's own alpha is the shape now, so the image is never cropped into a disc: Fit keeps
            // the whole phenomenon inside the square (the corona must not be trimmed) and the transparent
            // sky around it simply does not draw.
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * The plate for one eclipse type.
 *
 * Every type gets its own picture — including the penumbral lunar eclipse, which used to borrow the partial
 * plate and therefore showed an umbra bite that a penumbral eclipse never has. `NONE_*` is the "no eclipse"
 * state of each family and keeps the family's most characteristic plate.
 */
internal fun plateFor(type: EclipseEngine.EclipseType): Int = when (type) {
    EclipseEngine.EclipseType.TOTAL_SOLAR,
    EclipseEngine.EclipseType.HYBRID_SOLAR -> R.drawable.eclipse_solar_total

    EclipseEngine.EclipseType.ANNULAR_SOLAR -> R.drawable.eclipse_solar_annular

    EclipseEngine.EclipseType.PARTIAL_SOLAR,
    EclipseEngine.EclipseType.NONE_SOLAR -> R.drawable.eclipse_solar_partial

    EclipseEngine.EclipseType.TOTAL_LUNAR -> R.drawable.eclipse_lunar_total

    EclipseEngine.EclipseType.PARTIAL_LUNAR,
    EclipseEngine.EclipseType.NONE_LUNAR -> R.drawable.eclipse_lunar_partial

    EclipseEngine.EclipseType.PENUMBRAL_LUNAR -> R.drawable.eclipse_lunar_penumbral
}
