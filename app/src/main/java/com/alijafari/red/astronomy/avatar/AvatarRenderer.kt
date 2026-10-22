package com.alijafari.red.astronomy.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alijafari.red.astronomy.ui.rendering.LightingState
import com.alijafari.red.astronomy.ui.rendering.SceneComposition

/**
 * Draws the baked avatar into the scene's motion layer, in the slot the vector observer used to hold.
 *
 * The contract with the scene is the observer's, unchanged: feet on the grass line at
 * [SceneComposition.figureFeetY], centred on [SceneComposition.figureCenterX], as tall as
 * [SceneComposition.figureHeightPx], and dimmed or graded by the light of the moment — so the character
 * belongs to the sky behind it instead of being pasted onto it.
 *
 * Cost per frame is one scaled bitmap with a colour matrix, plus two small ovals for the contact
 * shadow: no GL, no allocation of note, and nothing that can fail on a device that has already drawn
 * the stars.
 */
object AvatarRenderer {

    /**
     * @param frameElapsedMs how long the current clip has been showing, for the frame index.
     * @param colorFilter the theme/lighting grade from [AvatarGrade], or null for ungraded.
     */
    fun draw(
        drawScope: DrawScope,
        composition: SceneComposition,
        avatar: LoadedAvatar,
        clip: AvatarClip,
        frameElapsedMs: Long,
        lighting: LightingState,
        colorFilter: ColorFilter?
    ) {
        val image: ImageBitmap = avatar.sheetFor(clip.name) ?: return
        if (composition.figureHeightPx < 6f) return

        val placement = AvatarFraming.placement(composition, avatar.meta)
        drawContactShadow(drawScope, composition, placement, lighting)

        val frame = AvatarFraming.frameIndex(clip, frameElapsedMs)
        val (srcOffset, srcSize) = AvatarFraming.sourceRect(avatar.meta, clip, frame)

        drawScope.drawImage(
            image = image,
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstOffset = placement.dstOffset(),
            dstSize = placement.dstSize(),
            alpha = AvatarGrade.alpha(lighting),
            colorFilter = colorFilter
        )
    }

    /**
     * The two nested ovals the observer used: a soft one for the ambient ground darkening, a tighter
     * one for self-contact. Sized off the character's own stance, so a separate stance reads heavier.
     */
    private fun drawContactShadow(
        drawScope: DrawScope,
        composition: SceneComposition,
        placement: AvatarPlacement,
        lighting: LightingState
    ) {
        val centreX = placement.left + placement.width * 0.5f
        val feetY = composition.figureFeetY
        val strength = 0.10f + 0.22f * lighting.ambientBrightness.coerceIn(0f, 1f)
        val half = placement.width * 0.30f
        drawScope.drawOval(
            color = Color.Black.copy(alpha = strength * 0.55f),
            topLeft = Offset(centreX - half, feetY - placement.height * 0.010f),
            size = Size(half * 2f, placement.height * 0.030f)
        )
        drawScope.drawOval(
            color = Color.Black.copy(alpha = strength),
            topLeft = Offset(centreX - half * 0.55f, feetY - placement.height * 0.007f),
            size = Size(half * 1.1f, placement.height * 0.018f)
        )
    }
}
