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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.Debtor
import com.example.debttracker.ui.components.ActionButton
import com.example.debttracker.ui.components.GlassCard
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class MonthlyData(
    val monthName: String,
    val status: String,
    val amount: Int,
    val count: Int,
    val isPaid: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAnalysisSheet(
    debtor: Debtor,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val monthlyData = remember(debtor.payments) { calculateMonthlyData(debtor) }

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
                text = "Mesecna analiza - ${debtor.name}",
                color = DebtColors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

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
                HeaderRow()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(monthlyData) { data ->
                        MonthRow(data = data)
                    }
                }
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
private fun HeaderRow() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        bgColor = DebtColors.bgCardElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UI.spacing.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Mesec",
                color = DebtColors.accentSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = "Status",
                color = DebtColors.accentSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.3f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Iznos",
                color = DebtColors.accentSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.3f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun MonthRow(data: MonthlyData) {
    val statusColor = if (data.isPaid) DebtColors.success else DebtColors.error
    val amountText = if (data.isPaid) "${formatAmount(data.amount)} RSD" else "0 RSD"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UI.spacing.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = data.monthName,
            color = DebtColors.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = data.status,
            color = statusColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.3f),
            textAlign = TextAlign.Center
        )
        Text(
            text = amountText,
            color = statusColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.3f),
            textAlign = TextAlign.End
        )
    }
}

private fun calculateMonthlyData(debtor: Debtor): List<MonthlyData> {
    if (debtor.payments.isEmpty()) return emptyList()

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US)
    val monthly = mutableMapOf<String, Pair<Int, Int>>()

    debtor.payments.forEach { payment ->
        try {
            val date = LocalDate.parse(payment.date, dateFormatter)
            val key = "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
            val current = monthly[key]
            if (current == null) {
                monthly[key] = payment.amount to 1
            } else {
                monthly[key] = (current.first + payment.amount) to (current.second + 1)
            }
        } catch (_: Exception) {
            // Skip invalid dates
        }
    }

    val result = mutableListOf<MonthlyData>()
    val startDate = try {
        LocalDate.parse(debtor.payments.first().date, dateFormatter)
    } catch (_: Exception) {
        LocalDate.now()
    }

    var currentDate = LocalDate.of(startDate.year, startDate.month, 1)
    val endDate = LocalDate.now()

    while (!currentDate.isAfter(endDate)) {
        val key = "${currentDate.year}-${currentDate.monthValue.toString().padStart(2, '0')}"
        val monthName = "${currentDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${currentDate.year}"
        val info = monthly[key]

        if (info != null) {
            result.add(
                MonthlyData(
                    monthName = monthName,
                    status = "Vratio",
                    amount = info.first,
                    count = info.second,
                    isPaid = true
                )
            )
        } else {
            result.add(
                MonthlyData(
                    monthName = monthName,
                    status = "Nije vratio",
                    amount = 0,
                    count = 0,
                    isPaid = false
                )
            )
        }

        currentDate = currentDate.plusMonths(1)
    }

    return result
}

private fun formatAmount(amount: Int): String {
    return NumberFormat.getIntegerInstance(Locale.US).format(amount)
}
