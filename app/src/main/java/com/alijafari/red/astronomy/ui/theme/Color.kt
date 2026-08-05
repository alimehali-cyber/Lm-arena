package com.alijafari.red.astronomy.ui.theme

import androidx.compose.ui.graphics.Color

// RED Space Astronomy Design System Color Tokens
val BackgroundPrimary = Color(0xFF0A0A12)      // Deep Space Black
val BackgroundCard = Color(0xFF13111A)         // Dark Purple-Black
val CardSurface = Color(0x0FFFFFFF)            // rgba(255, 255, 255, 0.06)
val CardBorder = Color(0x14FFFFFF)             // rgba(255, 255, 255, 0.08)

val AccentPrimary = Color(0xFFA855F7)          // Soft Purple/Violet
val AccentSecondary = Color(0xFFD946EF)        // Pink / Magenta
val AccentTertiary = Color(0xFF6366F1)         // Indigo for gradients

val TextPrimary = Color(0xFFF5F5F7)            // Near White
val TextSecondary = Color(0xFF9CA3AF)          // Muted Gray
val TextTertiary = Color(0xFF6B7280)           // Subtle Gray

val StatusExcellent = Color(0xFF34D399)        // Soft Green
val StatusGood = Color(0xFFFBBF24)             // Amber
val StatusWarning = Color(0xFFF87171)          // Soft Red

val GradientCardStart = Color(0xFF1E1533)
val GradientCardEnd = Color(0xFF0F0D1A)

val BottomNavBackground = Color(0xE60D0B14)     // #0D0B14 at ~90% opacity

// Legacy color mappings for theme compatibility
val NavyBackground = BackgroundPrimary
val NavySurface = BackgroundCard
val NavySurfaceVariant = Color(0xFF1A1726)
val NavyPrimary = AccentPrimary
val NavyOnPrimary = Color(0xFF000000)
val NavyPrimaryContainer = Color(0x26A855F7)
val NavyOnPrimaryContainer = TextPrimary
val NavyOutline = CardBorder
val NavyTextPrimary = TextPrimary
val NavyTextSecondary = TextSecondary

val AccentEmerald = StatusExcellent
val AccentTeal = Color(0xFF14B8A6)
val AccentAmber = StatusGood
val AccentCyan = AccentPrimary
val AccentViolet = AccentPrimary
val AccentRose = StatusWarning
val AccentBlue = AccentTertiary

