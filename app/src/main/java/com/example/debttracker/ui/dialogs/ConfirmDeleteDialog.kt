package com.example.debttracker.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.Payment
import com.example.debttracker.ui.components.RoundedButton
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ConfirmDeleteDialog(
    payment: Payment,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val message = "Obrisi uplatu?\n\n" +
        "Datum: ${payment.date}\n" +
        "Iznos: ${formatAmount(payment.amount)} RSD\n\n" +
        "Iznos ce biti vracen na dug."

    BaseDialog(title = "Potvrda brisanja", onDismiss = onDismiss) {
        Text(
            text = message,
            color = DebtColors.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UI.spacing.dp)
        ) {
            RoundedButton(
                text = "Otkazi",
                modifier = Modifier.weight(1f),
                bgColor = DebtColors.accentPrimaryDark,
                onClick = onDismiss
            )
            RoundedButton(
                text = "Obrisi",
                modifier = Modifier.weight(1f),
                bgColor = DebtColors.error,
                onClick = onConfirm
            )
        }
    }
}

private fun formatAmount(amount: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(amount)
}
