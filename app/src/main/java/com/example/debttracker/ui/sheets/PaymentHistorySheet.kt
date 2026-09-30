package com.example.debttracker.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.Debtor
import com.example.debttracker.data.Payment
import com.example.debttracker.ui.components.ActionButton
import com.example.debttracker.ui.components.GlassCard
import com.example.debttracker.ui.components.RoundedButton
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistorySheet(
    debtor: Debtor,
    onDismiss: () -> Unit,
    onDeletePayment: (index: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DebtColors.bgPrimary,
        contentColor = DebtColors.textPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DebtColors.glassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UI.cardPadding.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Istorija uplata - ${debtor.name}",
                color = DebtColors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(UI.spacing.dp))

            SummaryCard(debtor = debtor)

            Spacer(modifier = Modifier.height(UI.spacing.dp))

            if (debtor.payments.isEmpty()) {
                Text(
                    text = "Nema zabelezenih uplata.",
                    color = DebtColors.textSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                PaymentsList(
                    debtor = debtor,
                    onDeletePayment = onDeletePayment,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

            ActionButton(
                text = "Zatvori",
                bgColor = DebtColors.accentPrimary,
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun SummaryCard(debtor: Debtor) {
    val paid = debtor.initialDebt - debtor.totalDebt

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        bgColor = DebtColors.bgCardElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UI.cardPadding.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SummaryRow("Pocetni dug:", formatAmount(debtor.initialDebt))
            Spacer(modifier = Modifier.height(4.dp))
            SummaryRow("Otplaceno:", formatAmount(paid), DebtColors.success)
            Spacer(modifier = Modifier.height(4.dp))
            SummaryRow("Preostalo:", formatAmount(debtor.totalDebt), DebtColors.accentSecondary)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = DebtColors.textPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = DebtColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "$value RSD",
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PaymentsList(
    debtor: Debtor,
    onDeletePayment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var remaining = debtor.initialDebt

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(debtor.payments) { index, payment ->
            remaining -= payment.amount
            PaymentItem(
                index = index,
                payment = payment,
                remainingAfter = remaining,
                onDelete = { onDeletePayment(index) }
            )
        }
    }
}

@Composable
private fun PaymentItem(
    index: Int,
    payment: Payment,
    remainingAfter: Int,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        bgColor = DebtColors.bgCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UI.spacing.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${payment.date} - ${formatAmount(payment.amount)} RSD",
                    color = DebtColors.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Preostalo: ${formatAmount(remainingAfter)} RSD",
                    color = DebtColors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            RoundedButton(
                text = "X",
                modifier = Modifier.size(40.dp),
                bgColor = DebtColors.error,
                radius = 8.dp,
                onClick = onDelete
            )
        }
    }
}

private fun formatAmount(amount: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(amount)
}
