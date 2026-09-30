package com.example.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI

data class ActionButtonData(
    val text: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun ActionButtonGrid(
    buttons: List<ActionButtonData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UI.spacing.dp)
    ) {
        buttons.forEach { button ->
            ActionButton(
                text = button.text,
                bgColor = button.color,
                modifier = Modifier.fillMaxWidth(),
                onClick = button.onClick
            )
        }
    }
}

object ButtonColors {
    val defaultPayment = DebtColors.accentPrimary
    val customPayment = DebtColors.accentPrimaryDark
    val datedPayment = Color(0xFF5C6B8E)
    val history = DebtColors.accentSecondary
    val monthly = Color(0xFF7B6A9E)
    val addDebtor = Color(0xFF4A5A8E)
    val backup = DebtColors.warning
}
