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
import com.example.debttracker.data.PaymentResult
import com.example.debttracker.ui.components.ActionButton
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SuccessDialog(
    result: PaymentResult,
    onDismiss: () -> Unit
) {
    val removalNote = if (result.wasRemoved) "\n\nDuznik je isplacen i obrisan." else ""
    val message = "Uplata zabelezena!\n\n" +
        "Iznos: ${formatAmount(result.amount)} RSD\n" +
        "Datum: ${result.date}\n\n" +
        "Preostali dug (${result.debtorName}): ${formatAmount(result.remainingDebt)} RSD" +
        removalNote

    BaseDialog(title = "Uspešno!", onDismiss = onDismiss) {
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
            bgColor = DebtColors.success,
            onClick = onDismiss
        )
    }
}

private fun formatAmount(amount: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(amount)
}
