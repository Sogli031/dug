package com.example.debttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.debttracker.R

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_wght, weight = FontWeight.W400)
)

private val BaseTypography = Typography()
private val DebtTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = SpaceGrotesk),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = SpaceGrotesk),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = SpaceGrotesk),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = SpaceGrotesk),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = SpaceGrotesk),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = SpaceGrotesk),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = SpaceGrotesk),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = SpaceGrotesk),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = SpaceGrotesk),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = SpaceGrotesk),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = SpaceGrotesk),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = SpaceGrotesk),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = SpaceGrotesk),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = SpaceGrotesk),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = SpaceGrotesk)
)

@Composable
fun DebtTrackerTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = DebtColors.accentPrimary,
        secondary = DebtColors.accentSecondary,
        background = DebtColors.bgDark,
        surface = DebtColors.bgCard,
        error = DebtColors.error,
        onPrimary = DebtColors.textOnAccent,
        onSecondary = DebtColors.textOnAccent,
        onBackground = DebtColors.textPrimary,
        onSurface = DebtColors.textPrimary,
        onError = DebtColors.textPrimary
    )

    MaterialTheme(
        colorScheme = colors,
        typography = DebtTypography,
        content = content
    )
}
