package com.example.debttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.Currency
import com.example.debttracker.ui.theme.DebtColors

@Composable
fun CurrencyPicker(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Currency.entries.forEach { currency ->
            val isSelected = currency == selectedCurrency
            val bgColor = if (isSelected) DebtColors.accentSecondary else DebtColors.bgCardElevated
            val textColor = if (isSelected) DebtColors.textOnAccent else DebtColors.textPrimary

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) DebtColors.accentSecondary else DebtColors.glassBorder.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onCurrencySelected(currency) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currency.label,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExchangeRateLabel(modifier: Modifier = Modifier) {
    Text(
        text = "1 EUR = 117 RSD",
        color = DebtColors.textMuted,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(top = 4.dp)
    )
}
