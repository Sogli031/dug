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

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    BaseDialog(title = title, onDismiss = onDismiss) {
        Text(
            text = message,
            color = DebtColors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(UI.largeSpacing.dp))
        ActionButton(
            text = "OK",
            bgColor = DebtColors.accentPrimary,
            onClick = onDismiss
        )
    }
}
