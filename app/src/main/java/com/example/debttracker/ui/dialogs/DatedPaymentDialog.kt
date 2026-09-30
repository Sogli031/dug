package com.example.debttracker.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.Currency
import com.example.debttracker.data.convertToRsd
import com.example.debttracker.ui.components.CurrencyPicker
import com.example.debttracker.ui.components.ExchangeRateLabel
import com.example.debttracker.ui.components.RoundedButton
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DatedPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountRsd: Int, date: String) -> Unit
) {
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US))
    var dateText by remember { mutableStateOf(todayString) }
    var amountText by remember { mutableStateOf("10000") }
    var selectedCurrency by remember { mutableStateOf(Currency.RSD) }

    BaseDialog(title = "Uplata", onDismiss = onDismiss) {
        Text(
            text = "Datum (DD.MM.YYYY):",
            color = DebtColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(UI.smallSpacing.dp))

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(UI.spacing.dp))

        Text(
            text = "Iznos:",
            color = DebtColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(UI.smallSpacing.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = textFieldColors()
        )

        Spacer(modifier = Modifier.height(UI.spacing.dp))

        Text(
            text = "Valuta:",
            color = DebtColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(UI.smallSpacing.dp))

        CurrencyPicker(
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { selectedCurrency = it }
        )
        ExchangeRateLabel(modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UI.spacing.dp)
        ) {
            RoundedButton(
                text = "Otkaži",
                modifier = Modifier.weight(1f),
                bgColor = DebtColors.error,
                onClick = onDismiss
            )
            RoundedButton(
                text = "Potvrdi",
                modifier = Modifier.weight(1f),
                bgColor = DebtColors.success,
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    val amountRsd = convertToRsd(amount, selectedCurrency)
                    if (amountRsd > 0) {
                        onConfirm(amountRsd, dateText)
                    }
                }
            )
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DebtColors.textPrimary,
    unfocusedTextColor = DebtColors.textPrimary,
    focusedBorderColor = DebtColors.accentPrimary,
    unfocusedBorderColor = DebtColors.glassBorder,
    cursorColor = DebtColors.accentPrimary,
    focusedContainerColor = DebtColors.bgCardElevated,
    unfocusedContainerColor = DebtColors.bgCardElevated
)
