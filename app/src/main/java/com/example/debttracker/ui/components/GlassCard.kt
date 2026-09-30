package com.example.debttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    bgColor: Color = DebtColors.bgCard,
    borderColor: Color = DebtColors.glassBorder,
    radius: Dp = UI.cardRadius.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val background = bgColor.copy(alpha = bgColor.alpha * 0.95f)
    Box(
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = DebtColors.shadow.copy(alpha = 0.33f))
            .background(background, shape)
            .border(1.2.dp, borderColor.copy(alpha = 0.25f), shape)
    ) {
        content()
    }
}
