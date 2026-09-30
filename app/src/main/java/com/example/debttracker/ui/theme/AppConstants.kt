package com.example.debttracker.ui.theme

import androidx.compose.ui.graphics.Color

object DebtColors {
    // Background layers (warm-neutral dark)
    val bgDark = Color(0xFF0F1114)
    val bgPrimary = Color(0xFF161A1E)
    val bgCard = Color(0xFF1C2127)
    val bgCardElevated = Color(0xFF232930)

    // Glass/Border effects
    val glassBorder = Color(0xFF2E3640)
    val divider = Color(0xFF3A424D)
    val shadow = Color(0xFF000000)

    // Primary accent - Muted slate blue
    val accentPrimary = Color(0xFF6B8AAE)
    val accentPrimaryDark = Color(0xFF4A6A8E)

    // Secondary accent - Soft teal
    val accentSecondary = Color(0xFF5CA3A3)

    // Semantic colors (muted versions)
    val success = Color(0xFF6AAF7A)
    val error = Color(0xFFC97171)
    val warning = Color(0xFFCBA258)

    // Text colors
    val textPrimary = Color(0xFFF0F2F4)
    val textSecondary = Color(0xFFADB5BD)
    val textMuted = Color(0xFF6C757D)
    val textOnAccent = Color(0xFF0F1114)

    // Progress bar
    val progressFill = Color(0xFF6AAF7A)
    val progressTrack = Color(0xFF2E3640)
}

object UI {
    const val buttonHeight = 52
    const val navButtonSize = 44
    const val cardRadius = 16
    const val buttonRadius = 12
    const val cardPadding = 16
    const val spacing = 12
    const val smallSpacing = 8
    const val largeSpacing = 16
}
