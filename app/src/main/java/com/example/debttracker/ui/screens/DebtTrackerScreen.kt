package com.example.debttracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.debttracker.data.DebtManager
import com.example.debttracker.data.PaymentResult
import com.example.debttracker.data.backup.GoogleDriveBackup
import com.example.debttracker.data.backup.SignInState
import com.example.debttracker.data.backup.SyncState
import com.example.debttracker.ui.components.ActionButtonData
import com.example.debttracker.ui.components.ActionButtonGrid
import com.example.debttracker.ui.components.ButtonColors
import com.example.debttracker.ui.components.DebtCard
import com.example.debttracker.ui.components.DebtProgressIndicator
import com.example.debttracker.ui.dialogs.AddDebtorDialog
import com.example.debttracker.ui.dialogs.CloudBackupDialog
import com.example.debttracker.ui.dialogs.ConfirmDeleteDialog
import com.example.debttracker.ui.dialogs.DatedPaymentDialog
import com.example.debttracker.ui.dialogs.InfoDialog
import com.example.debttracker.ui.dialogs.SuccessDialog
import com.example.debttracker.ui.dialogs.WelcomeDialog
import com.example.debttracker.ui.sheets.MonthlyAnalysisSheet
import com.example.debttracker.ui.sheets.PaymentHistorySheet
import com.example.debttracker.ui.theme.DebtColors
import com.example.debttracker.ui.theme.UI

data class InfoDialogState(val title: String, val message: String)

@Composable
fun DebtTrackerScreen(
    debtManager: DebtManager,
    googleDriveBackup: GoogleDriveBackup,
    signInState: SignInState,
    syncState: SyncState,
    lastSyncTime: Long?,
    showWelcomeDialog: Boolean,
    refreshTrigger: Int,
    onSignIn: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSignOut: () -> Unit,
    onSkipWelcome: () -> Unit,
    onRefresh: () -> Unit
) {
    var localRefreshTrigger by remember { mutableIntStateOf(0) }
    fun refresh() {
        localRefreshTrigger++
        onRefresh()
    }

    // Dialog states
    var showDatedPaymentDialog by remember { mutableStateOf(false) }
    var showAddDebtorDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showMonthlySheet by remember { mutableStateOf(false) }
    var infoDialogState by remember { mutableStateOf<InfoDialogState?>(null) }
    var successResult by remember { mutableStateOf<PaymentResult?>(null) }
    var deletePaymentIndex by remember { mutableStateOf<Int?>(null) }

    // Derived state
    val currentDebtor = remember(refreshTrigger, localRefreshTrigger) { debtManager.currentDebtor }
    val hasMultipleDebtors = remember(refreshTrigger, localRefreshTrigger) { debtManager.debtors.size > 1 }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background with subtle gradient
        BackgroundLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(UI.cardPadding.dp)
        ) {
            // Header
            Text(
                text = "DEBT TRACKER",
                color = DebtColors.accentSecondary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

            // Debt display card
            DebtCard(
                debtorName = currentDebtor?.name,
                currentDebt = currentDebtor?.totalDebt ?: 0,
                hasMultipleDebtors = hasMultipleDebtors,
                onPreviousDebtor = {
                    debtManager.selectPreviousDebtor()
                    refresh()
                },
                onNextDebtor = {
                    debtManager.selectNextDebtor()
                    refresh()
                }
            )

            Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

            // Progress indicator
            if (currentDebtor != null) {
                DebtProgressIndicator(
                    initialDebt = currentDebtor.initialDebt,
                    currentDebt = currentDebtor.totalDebt
                )
            }

            Spacer(modifier = Modifier.height(UI.largeSpacing.dp))

            // Action buttons
            ActionButtonGrid(
                buttons = listOf(
                    ActionButtonData("Uplata", ButtonColors.defaultPayment) {
                        if (currentDebtor == null) {
                            infoDialogState = InfoDialogState("Info", "Nema duznika.")
                        } else {
                            showDatedPaymentDialog = true
                        }
                    },
                    ActionButtonData("Istorija uplata", ButtonColors.history) {
                        if (currentDebtor == null) {
                            infoDialogState = InfoDialogState("Info", "Nema duznika.")
                        } else {
                            showHistorySheet = true
                        }
                    },
                    ActionButtonData("Mesecna analiza", ButtonColors.monthly) {
                        if (currentDebtor == null) {
                            infoDialogState = InfoDialogState("Info", "Nema duznika.")
                        } else {
                            showMonthlySheet = true
                        }
                    },
                    ActionButtonData("Dodaj duznika", ButtonColors.addDebtor) {
                        showAddDebtorDialog = true
                    },
                    ActionButtonData("Cloud Backup", ButtonColors.backup) {
                        showCloudBackupDialog = true
                    }
                )
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    // Welcome dialog for first launch
    if (showWelcomeDialog) {
        WelcomeDialog(
            onConnectGoogle = {
                onSkipWelcome()
                onSignIn()
            },
            onSkip = onSkipWelcome
        )
    }

    // Dialogs
    if (showDatedPaymentDialog) {
        DatedPaymentDialog(
            onDismiss = { showDatedPaymentDialog = false },
            onConfirm = { amountRsd, date ->
                showDatedPaymentDialog = false
                val result = debtManager.addPayment(amountRsd, date)
                if (result != null) {
                    successResult = result
                }
                refresh()
            }
        )
    }

    if (showAddDebtorDialog) {
        AddDebtorDialog(
            onDismiss = { showAddDebtorDialog = false },
            onConfirm = { name, amountRsd ->
                showAddDebtorDialog = false
                debtManager.addDebtor(name, amountRsd)
                refresh()
            }
        )
    }

    if (showCloudBackupDialog) {
        CloudBackupDialog(
            signInState = signInState,
            syncState = syncState,
            lastSyncTime = lastSyncTime,
            onDismiss = { showCloudBackupDialog = false },
            onSignIn = {
                showCloudBackupDialog = false
                onSignIn()
            },
            onBackup = onBackup,
            onRestore = {
                onRestore()
                refresh()
            },
            onSignOut = onSignOut
        )
    }

    infoDialogState?.let { state ->
        InfoDialog(
            title = state.title,
            message = state.message,
            onDismiss = { infoDialogState = null }
        )
    }

    successResult?.let { result ->
        SuccessDialog(
            result = result,
            onDismiss = {
                successResult = null
                refresh()
            }
        )
    }

    // Bottom sheets
    if (showHistorySheet && currentDebtor != null) {
        key(localRefreshTrigger) {
            PaymentHistorySheet(
                debtor = currentDebtor,
                onDismiss = { showHistorySheet = false },
                onDeletePayment = { index ->
                    deletePaymentIndex = index
                }
            )
        }
    }

    if (showMonthlySheet && currentDebtor != null) {
        MonthlyAnalysisSheet(
            debtor = currentDebtor,
            onDismiss = { showMonthlySheet = false }
        )
    }

    // Delete confirmation
    deletePaymentIndex?.let { index ->
        val payment = currentDebtor?.payments?.getOrNull(index)
        if (payment != null) {
            ConfirmDeleteDialog(
                payment = payment,
                onDismiss = { deletePaymentIndex = null },
                onConfirm = {
                    debtManager.deletePayment(index)
                    deletePaymentIndex = null
                    refresh()
                }
            )
        }
    }
}

@Composable
private fun BackgroundLayer() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = DebtColors.bgDark)
        drawOval(
            color = DebtColors.accentPrimary.copy(alpha = 0.08f),
            topLeft = Offset(-size.width * 0.25f, size.height * 0.4f),
            size = Size(size.width * 1.1f, size.height * 0.6f)
        )
        drawOval(
            color = DebtColors.accentSecondary.copy(alpha = 0.06f),
            topLeft = Offset(size.width * 0.2f, -size.height * 0.15f),
            size = Size(size.width * 0.9f, size.height * 0.5f)
        )
    }
}
