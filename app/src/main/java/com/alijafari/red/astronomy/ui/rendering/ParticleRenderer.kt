package com.alijafari.red.astronomy.ui.rendering

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.alijafari.red.astronomy.ui.components.StardustParticle

object ParticleRenderer {

    fun drawStardust(
        drawScope: DrawScope,
        particles: List<StardustParticle>
    ) {
        particles.forEach { particle ->
            if (particle.alpha > 0f) {
                drawScope.drawCircle(
                    color = particle.color.copy(alpha = particle.alpha.coerceIn(0f, 1f)),
                    radius = particle.size,
                    center = androidx.compose.ui.geometry.Offset(particle.x, particle.y)
                )
            }
        }
    }
}
