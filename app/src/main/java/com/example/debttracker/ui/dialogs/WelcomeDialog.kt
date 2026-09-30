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
fun WelcomeDialog(
    onConnectGoogle: () -> Unit,
    onSkip: () -> Unit
) {
    BaseDialog(title = "Dobrodosli", onDismiss = onSkip) {
        Text(
            text = "Ova aplikacija cuva podatke u Google Drive cloud-u.\n\n" +
                "Povezi svoj Google nalog da bi se podaci automatski cuvali i sinhronizovali.\n\n" +
                "Bez prijave, podaci ce se izgubiti kada zatvoris aplikaciju.",
            color = DebtColors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

        ActionButton(
            text = "Povezi Google nalog",
            bgColor = DebtColors.success,
            onClick = onConnectGoogle
        )

        Spacer(modifier = Modifier.height(UI.spacing.dp))

        ActionButton(
            text = "Preskoci",
            bgColor = DebtColors.bgCardElevated,
            onClick = onSkip
        )
    }
}
