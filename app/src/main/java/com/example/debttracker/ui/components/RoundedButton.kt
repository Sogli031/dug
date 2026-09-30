package com.example.debttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI

@Composable
fun RoundedButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color = DebtColors.textPrimary,
    borderColor: Color = DebtColors.glassBorder,
    radius: Dp = UI.buttonRadius.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .height(UI.buttonHeight.dp)
            .clip(shape)
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.5f))
            .border(1.dp, borderColor.copy(alpha = 0.3f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            style = textStyle.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RoundedButton(
        text = text,
        modifier = modifier.fillMaxWidth(),
        bgColor = bgColor,
        onClick = onClick
    )
}
