package com.example.debttracker.ui.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.debttracker.ui.components.ActionButton
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI

/** Dijalog sa glavnom (preporucenom) akcijom, opcionom sporednom i otkazivanjem. */
@Composable
fun CloudChoiceDialog(
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    cancelText: String,
    onCancel: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    BaseDialog(title = title, onDismiss = onCancel) {
        Text(
            text = message,
            color = DebtColors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

        ActionButton(
            text = primaryText,
            bgColor = DebtColors.success,
            onClick = onPrimary
        )

        if (secondaryText != null && onSecondary != null) {
            Spacer(modifier = Modifier.height(UI.spacing.dp))
            ActionButton(
                text = secondaryText,
                bgColor = DebtColors.error,
                onClick = onSecondary
            )
        }

        Spacer(modifier = Modifier.height(UI.spacing.dp))
        ActionButton(
            text = cancelText,
            bgColor = DebtColors.bgCardElevated,
            onClick = onCancel
        )
    }
}
