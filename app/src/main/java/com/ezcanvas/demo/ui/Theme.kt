package com.ezcanvas.demo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design tokens taken straight from the EZCanvas Hi-Fi mock. Indigo is the primary; the surfaces
 * are warm off-whites with a near-black ink. The dark device frame in the mock is just the phone,
 * so the app itself is this light surface system.
 */
object EzColors {
    val Ink = Color(0xFF0C0C0E)
    val Surface = Color(0xFFFFFFFF)
    val AppBg = Color(0xFFF4F2EC)
    val ChipBg = Color(0xFFF4F3EF)
    val Divider = Color(0xFFECECEA)

    val Primary = Color(0xFF4F46E5)
    val Coral = Color(0xFFFB6F61)
    val Teal = Color(0xFF14B8A6)
    val Amber = Color(0xFFF59E0B)

    val Muted = Color(0xFF86837B)
    val Subtle = Color(0xFF9B988F)
    val SectionLabel = Color(0xFFB4B0A6)
}

/** Brush swatches — the mock's accents first, then a wider set so the row scrolls. */
val BrushSwatches: List<Color> = listOf(
    EzColors.Primary, EzColors.Coral, EzColors.Teal, EzColors.Amber, EzColors.Ink,
    Color(0xFFF43F5E), Color(0xFF06B6D4), Color(0xFF22C55E),
    Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF3B82F6), Color(0xFFFFFFFF),
)

/** Background swatches for the configurable canvas. */
val BackgroundSwatches: List<Color> = listOf(
    EzColors.Surface, EzColors.AppBg, Color(0xFFFFFDE7), Color(0xFFE0F2FE),
    Color(0xFFFCE7F3), EzColors.Ink, Color(0xFF0B2A4A), Color(0xFF1E293B),
)

private val EzScheme = lightColorScheme(
    primary = EzColors.Primary,
    onPrimary = EzColors.Surface,
    secondary = EzColors.Ink,
    onSecondary = EzColors.Surface,
    background = EzColors.AppBg,
    onBackground = EzColors.Ink,
    surface = EzColors.Surface,
    onSurface = EzColors.Ink,
    surfaceVariant = EzColors.ChipBg,
    onSurfaceVariant = EzColors.Muted,
    outline = EzColors.Muted,
    outlineVariant = EzColors.Divider,
)

private val EzTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Medium),
    )
}

/**
 * Material 3 theme for the demo. The mock is a single light design, so we apply the brand scheme
 * directly rather than following the system day/night setting.
 */
@Composable
fun EzCanvasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EzScheme,
        typography = EzTypography,
        content = content,
    )
}
