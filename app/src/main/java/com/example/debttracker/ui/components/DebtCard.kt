package com.example.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DebtCard(
    debtorName: String?,
    currentDebt: Int,
    hasMultipleDebtors: Boolean,
    onPreviousDebtor: () -> Unit,
    onNextDebtor: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UI.cardPadding.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavButton(
                text = "<",
                enabled = hasMultipleDebtors,
                onClick = onPreviousDebtor
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (debtorName != null) {
                    Text(
                        text = "$debtorName:",
                        color = DebtColors.textPrimary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${formatAmount(currentDebt)} RSD",
                        color = DebtColors.textSecondary,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Nema duznika",
                        color = DebtColors.textSecondary,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            NavButton(
                text = ">",
                enabled = hasMultipleDebtors,
                onClick = onNextDebtor
            )
        }
    }
}

@Composable
private fun NavButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    RoundedButton(
        text = text,
        modifier = Modifier.size(UI.navButtonSize.dp),
        bgColor = DebtColors.accentPrimaryDark,
        textColor = DebtColors.textPrimary,
        enabled = enabled,
        textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
        onClick = onClick
    )
}

private fun formatAmount(amount: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(amount)
}
