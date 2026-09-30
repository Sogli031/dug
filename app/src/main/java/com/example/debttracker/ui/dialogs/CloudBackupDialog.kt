package com.example.debttracker.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.backup.SignInState
import com.example.debttracker.data.backup.SyncState
import com.example.debttracker.ui.components.ActionButton
import com.example.debttracker.ui.components.RoundedButton
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudBackupDialog(
    signInState: SignInState,
    syncState: SyncState,
    lastSyncTime: Long?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSignOut: () -> Unit
) {
    val isSyncing = syncState is SyncState.Syncing
    val isSignedIn = signInState is SignInState.SignedIn
    val isEmptyData = syncState is SyncState.EmptyData
    val isError = syncState is SyncState.Error

    val statusText = when {
        isSyncing -> "Sinhronizacija..."
        isError -> "Greška pri sinhronizaciji - pokušaj ponovo"
        isEmptyData -> "Nema podataka za backup"
        isSignedIn -> (signInState as SignInState.SignedIn).email
        else -> "Nije povezano"
    }

    val lastSyncText = if (lastSyncTime != null) {
        "Poslednji backup: ${formatSyncTime(lastSyncTime)}"
    } else {
        "Nema prethodnog backup-a"
    }

    BaseDialog(title = "Cloud Backup", onDismiss = onDismiss) {
        // Status
        if (isSyncing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = DebtColors.accentSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(UI.spacing.dp))
                Text(
                    text = statusText,
                    color = DebtColors.accentSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Text(
                text = statusText,
                color = when {
                    isError -> DebtColors.error
                    isSignedIn -> DebtColors.success
                    else -> DebtColors.textSecondary
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(UI.smallSpacing.dp))

        // Last sync time
        if (isSignedIn) {
            Text(
                text = lastSyncText,
                color = DebtColors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

        if (isSignedIn) {
            // Backup/Restore buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UI.spacing.dp)
            ) {
                RoundedButton(
                    text = "Backup",
                    modifier = Modifier.weight(1f),
                    bgColor = DebtColors.success,
                    enabled = !isSyncing,
                    onClick = onBackup
                )
                RoundedButton(
                    text = "Restore",
                    modifier = Modifier.weight(1f),
                    bgColor = DebtColors.accentPrimaryDark,
                    enabled = !isSyncing,
                    onClick = onRestore
                )
            }

            Spacer(modifier = Modifier.height(UI.spacing.dp))

            // Sign out button
            ActionButton(
                text = "Odjavi se",
                bgColor = DebtColors.bgCardElevated,
                onClick = {
                    onSignOut()
                    onDismiss()
                }
            )
        } else {
            // Sign in button
            ActionButton(
                text = "Povezi Google nalog",
                bgColor = DebtColors.success,
                onClick = {
                    onDismiss()
                    onSignIn()
                }
            )
        }

        Spacer(modifier = Modifier.height(UI.spacing.dp))

        // Close button
        ActionButton(
            text = "Zatvori",
            bgColor = DebtColors.accentPrimary,
            onClick = onDismiss
        )
    }
}

private fun formatSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Upravo sada"
        minutes < 60 -> "Pre $minutes min"
        hours < 24 -> "Pre $hours h"
        days == 1L -> "Juce"
        days < 7 -> "Pre $days dana"
        else -> {
            val date = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())
            date.format(Date(timestamp))
        }
    }
}
