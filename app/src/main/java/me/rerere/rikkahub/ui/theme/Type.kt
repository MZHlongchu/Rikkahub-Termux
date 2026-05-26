package me.rerere.rikkahub.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.rerere.rikkahub.R

private val BaseTypography = Typography()

// Set of Material typography styles to start with
val Typography = BaseTypography.copy(
    bodyLarge = BaseTypography.bodyLarge.copy(lineHeight = 26.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(lineHeight = 24.sp),
    bodySmall = BaseTypography.bodySmall.copy(lineHeight = 20.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    displayLargeEmphasized = BaseTypography.displayLargeEmphasized.copy(
        fontFamily = GoogleSansFlex.Display.Emphasized.Large
    ),
    displayMediumEmphasized = BaseTypography.displayMediumEmphasized.copy(
        fontFamily = GoogleSansFlex.Display.Emphasized.Medium
    ),
    displaySmallEmphasized = BaseTypography.displaySmallEmphasized.copy(
        fontFamily = GoogleSansFlex.Display.Emphasized.Large
    ),
    headlineLargeEmphasized = BaseTypography.headlineLargeEmphasized.copy(
        fontFamily = GoogleSansFlex.Headline.Emphasized.Large
    ),
    headlineMediumEmphasized = BaseTypography.headlineMediumEmphasized.copy(
        fontFamily = GoogleSansFlex.Headline.Emphasized.Medium
    ),
    headlineSmallEmphasized = BaseTypography.headlineSmallEmphasized.copy(
        fontFamily = GoogleSansFlex.Headline.Emphasized.Large
    ),
    titleLargeEmphasized = BaseTypography.titleLargeEmphasized.copy(
        fontFamily = GoogleSansFlex.Title.Emphasized.Large
    ),
    titleMediumEmphasized = BaseTypography.titleMediumEmphasized.copy(
        fontFamily = GoogleSansFlex.Title.Emphasized.Medium
    ),
    titleSmallEmphasized = BaseTypography.titleSmallEmphasized.copy(
        fontFamily = GoogleSansFlex.Title.Emphasized.Small
    ),
    bodyLargeEmphasized = BaseTypography.bodyLargeEmphasized.copy(
        fontFamily = GoogleSansFlex.Body.Emphasized.Large
    ),
    bodyMediumEmphasized = BaseTypography.bodyMediumEmphasized.copy(
        fontFamily = GoogleSansFlex.Body.Emphasized.Medium
    ),
    bodySmallEmphasized = BaseTypography.bodySmallEmphasized.copy(
        fontFamily = GoogleSansFlex.Body.Emphasized.Small
    ),
    labelLargeEmphasized = BaseTypography.labelLargeEmphasized.copy(
        fontFamily = GoogleSansFlex.Label.Emphasized.Large
    ),
    labelMediumEmphasized = BaseTypography.labelMediumEmphasized.copy(
        fontFamily = GoogleSansFlex.Label.Emphasized.Medium
    ),
    labelSmallEmphasized = BaseTypography.labelSmallEmphasized.copy(
        fontFamily = GoogleSansFlex.Label.Emphasized.Small
    ),
)

@OptIn(ExperimentalTextApi::class)
val JetbrainsMono = FontFamily(
    Font(
        resId = R.font.jetbrains_mono,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Normal.weight),
        )
    )
)
