package com.alijafari.red.astronomy.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RED Design System - Surface & Material Primitives
 * Built on modern Apple-inspired principles:
 * - Visual restraint (no heavy glows, excessive gradients, or noisy borders)
 * - Concentric geometry (consistent corner radius hierarchy)
 * - Content-first layout with high-contrast text and adaptive surfaces
 * - Subtle depth (hairline borders and very soft elevations)
 */

/**
 * Base RED Content Card
 */
@Composable
fun RedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RedCornerRadius.lg),
    backgroundColor: Color = RedTheme.colors.surface,
    borderColor: Color = RedTheme.colors.border,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(RedSpacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = RedTheme.colors.accentRed),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Elevated RED Card for primary highlights / floating panels
 */
@Composable
fun RedElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RedCornerRadius.xl),
    backgroundColor: Color = RedTheme.colors.surfaceElevated,
    borderColor: Color = RedTheme.colors.border,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = RedElevation.card,
    contentPadding: PaddingValues = PaddingValues(RedSpacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = RedTheme.colors.accentRed),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = backgroundColor,
        shadowElevation = shadowElevation,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Clean Section Header with strong visual hierarchy
 */
@Composable
fun RedSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    actionText: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RedSpacing.xs, vertical = RedSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RedSpacing.sm)
            ) {
                Text(
                    text = title,
                    style = RedTypographyTokens.sectionTitle,
                    color = RedTheme.colors.textPrimary
                )

                val badge = badgeText ?: actionText
                if (badge != null) {
                    RedBadge(text = badge)
                }
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = RedTypographyTokens.caption,
                    color = RedTheme.colors.textSecondary
                )
            }
        }

        if (action != null) {
            action()
        }
    }
}

/**
 * Refined Pill / Capsule Badge
 */
@Composable
fun RedBadge(
    text: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    backgroundColor: Color = if (isAccent) RedTheme.colors.accentRed.copy(alpha = 0.15f) else RedTheme.colors.surfaceGrouped,
    textColor: Color = if (isAccent) RedTheme.colors.accentRed else RedTheme.colors.textSecondary,
    borderColor: Color = if (isAccent) RedTheme.colors.accentRed.copy(alpha = 0.4f) else RedTheme.colors.border
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = textColor
        )
    }
}

/**
 * Semantic Status Pill Badge
 */
@Composable
fun RedStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    statusColor: Color = RedTheme.colors.statusSuccess,
    containerColor: Color = RedTheme.colors.statusSuccessContainer
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, statusColor.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = statusColor
        )
    }
}

/**
 * Hairline Divider / Separator
 */
@Composable
fun RedHairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = RedTheme.colors.separator,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

/**
 * Standard Concentric Action Control / Button
 */
@Composable
fun RedControl(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    enabled: Boolean = true
) {
    val bg = if (isPrimary) RedTheme.colors.accentRed else RedTheme.colors.surfaceElevated
    val fg = if (isPrimary) Color.White else RedTheme.colors.textPrimary
    val borderCol = if (isPrimary) Color.Transparent else RedTheme.colors.border

    Surface(
        modifier = modifier
            .height(RedControlHeight.regular)
            .clip(RoundedCornerShape(RedCornerRadius.md))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isPrimary) Color.White.copy(alpha = 0.2f) else RedTheme.colors.accentRed),
                onClick = onClick
            ),
        shape = RoundedCornerShape(RedCornerRadius.md),
        color = bg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RedSpacing.lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(RedIconSize.sm)
                )
                Spacer(modifier = Modifier.width(RedSpacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = fg
            )
        }
    }
}

/**
 * Shared RED Slider
 * Modern Apple-inspired slider:
 * - Slim refined track with clean rounded ends
 * - High-contrast active track using RedTheme.colors.accentRed
 * - Restrained subtle inactive track adapting to surface
 * - Refined circular thumb with subtle elevation & clean accent border
 * - Generous touch target meeting accessibility standards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = RedTheme.colors.accentRed,
            activeTrackColor = RedTheme.colors.accentRed,
            inactiveTrackColor = RedTheme.colors.surfaceGrouped,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
            disabledThumbColor = RedTheme.colors.textTertiary,
            disabledActiveTrackColor = RedTheme.colors.separator,
            disabledInactiveTrackColor = RedTheme.colors.border
        )
    )
}

