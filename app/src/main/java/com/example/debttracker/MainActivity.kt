package com.example.debttracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.debttracker.data.BackupFormatException
import com.example.debttracker.data.DebtManager
import com.example.debttracker.data.DebtManagerProvider
import com.example.debttracker.data.DebtSnapshot
import com.example.debttracker.data.backup.CloudBackupStatus
import com.example.debttracker.data.backup.GoogleDriveBackup
import com.example.debttracker.data.backup.GoogleDriveBackupProvider
import com.example.debttracker.data.backup.SyncState
import com.example.debttracker.ui.dialogs.CloudChoiceDialog
import com.example.debttracker.ui.dialogs.InfoDialog
import com.example.debttracker.ui.screens.CrashLogScreen
import com.example.debttracker.ui.screens.DebtTrackerScreen
import com.example.debttracker.ui.screens.InfoDialogState
import com.example.debttracker.ui.theme.DebtTrackerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    // Ista instanca koju koristi i auto-backup u pozadini
    private val debtManager by lazy { DebtManagerProvider.get(this) }
    private lateinit var googleDriveBackup: GoogleDriveBackup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Proveri crash log od prethodnog pokretanja
        val crashFile = File(filesDir, "crash_log.txt")
        if (crashFile.exists()) {
            val crashLog = crashFile.readText()
            crashFile.delete()
            setContent {
                DebtTrackerTheme {
                    CrashLogScreen(crashLog = crashLog)
                }
            }
            return
        }

        // Inicijalizacija servisa - wrappovana u try-catch
        try {
            initializeApp()
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val crashLog = "Greška pri pokretanju:\n\n$sw"
            setContent {
                DebtTrackerTheme {
                    CrashLogScreen(crashLog = crashLog)
                }
            }
            return
        }

        setContent {
            DebtTrackerTheme {
                val scope = rememberCoroutineScope()
                val signInState by googleDriveBackup.signInState.collectAsState()
                val syncState by googleDriveBackup.syncState.collectAsState()
                val lastSyncTime by googleDriveBackup.lastSyncTime.collectAsState()

                var showWelcomeDialog by remember { mutableStateOf(googleDriveBackup.isFirstLaunch()) }
                var refreshTrigger by remember { mutableStateOf(0) }
                var cloudMessage by remember { mutableStateOf<InfoDialogState?>(null) }
                var showRestoreOffer by remember { mutableStateOf(false) }
                var overwriteConfirm by remember {
                    mutableStateOf<SyncState.OverwriteNeedsConfirmation?>(null)
                }
                var pendingRestore by remember { mutableStateOf<DebtSnapshot?>(null) }

                val signInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = try {
                        task.result
                    } catch (e: Exception) {
                        Log.e(TAG, "Sign in failed", e)
                        null
                    }
                    googleDriveBackup.handleSignInResult(account)

                    if (account != null && showWelcomeDialog) {
                        showWelcomeDialog = false
                        googleDriveBackup.markFirstLaunchDone()
                    }

                    if (account != null) {
                        scope.launch {
                            // Sveza instalacija + postojeci backup u oblaku -> ponudi restore
                            if (!googleDriveBackup.isCloudAdopted() &&
                                googleDriveBackup.cloudBackupStatus() == CloudBackupStatus.EXISTS
                            ) {
                                showRestoreOffer = true
                            }
                        }
                    }
                }

                // Primena vec proverenog backup-a; uspeh se javlja tek kad je sve sacuvano
                fun applyRestore(snapshot: DebtSnapshot) {
                    if (debtManager.applyBackup(snapshot)) {
                        googleDriveBackup.markCloudAdopted()
                        refreshTrigger++
                        cloudMessage = InfoDialogState("Uspeh", "Podaci su vraćeni iz oblaka.")
                    } else {
                        cloudMessage = InfoDialogState(
                            "Greška",
                            "Čuvanje na uređaju nije uspelo. Podaci nisu promenjeni."
                        )
                    }
                }

                fun startRestore() {
                    scope.launch {
                        val json = googleDriveBackup.restore()
                        if (json == null) {
                            cloudMessage = if (googleDriveBackup.syncState.value is SyncState.Error) {
                                InfoDialogState(
                                    "Greška",
                                    "Preuzimanje backup-a nije uspelo. Proveri internet i pokušaj ponovo."
                                )
                            } else {
                                InfoDialogState("Info", "U oblaku nema backup-a.")
                            }
                            return@launch
                        }

                        // Ceo backup se proverava pre prvog upisa - neispravan backup ne menja nista
                        val snapshot = try {
                            debtManager.parseBackup(json)
                        } catch (e: Exception) {
                            val reason = (e as? BackupFormatException)?.message
                                ?: "Backup nije u ispravnom formatu."
                            cloudMessage = InfoDialogState("Greška", "$reason Ništa nije promenjeno.")
                            return@launch
                        }

                        if (debtManager.isEmpty) {
                            applyRestore(snapshot)
                        } else {
                            pendingRestore = snapshot
                        }
                    }
                }

                fun startBackup(confirmedOverwrite: Boolean) {
                    scope.launch {
                        val success = googleDriveBackup.backup(
                            jsonData = debtManager.exportBackupJson(),
                            localIsEmpty = debtManager.isEmpty,
                            confirmedOverwrite = confirmedOverwrite
                        )
                        val state = googleDriveBackup.syncState.value
                        when {
                            success -> cloudMessage = InfoDialogState("Uspeh", "Backup je sačuvan u oblaku.")
                            // U oblaku je backup koji bi bio pregazen - ne prepisuj bez pitanja
                            state is SyncState.OverwriteNeedsConfirmation -> overwriteConfirm = state
                            state is SyncState.EmptyData ->
                                cloudMessage = InfoDialogState("Info", "Nema podataka za backup.")
                            else -> cloudMessage = InfoDialogState(
                                "Greška",
                                "Backup nije uspeo. Proveri internet i pokušaj ponovo."
                            )
                        }
                    }
                }

                DebtTrackerScreen(
                    debtManager = debtManager,
                    googleDriveBackup = googleDriveBackup,
                    signInState = signInState,
                    syncState = syncState,
                    lastSyncTime = lastSyncTime,
                    showWelcomeDialog = showWelcomeDialog,
                    refreshTrigger = refreshTrigger,
                    onSignIn = {
                        signInLauncher.launch(googleDriveBackup.getSignInIntent())
                    },
                    onBackup = { startBackup(confirmedOverwrite = false) },
                    onRestore = { startRestore() },
                    onSignOut = {
                        scope.launch {
                            googleDriveBackup.signOut()
                        }
                    },
                    onSkipWelcome = {
                        showWelcomeDialog = false
                        googleDriveBackup.markFirstLaunchDone()
                    },
                    onRefresh = { refreshTrigger++ }
                )

                overwriteConfirm?.let { confirm ->
                    CloudChoiceDialog(
                        title = "U oblaku već postoji backup",
                        message = if (confirm.localIsEmpty) {
                            "Na ovom uređaju nema dužnika, a u oblaku postoji backup. " +
                                "Ako sada napraviš backup, on će biti zamenjen praznim podacima."
                        } else {
                            "U oblaku postoji backup koji na ovom uređaju još nije vraćen. " +
                                "Ako sada napraviš backup, on će biti zamenjen podacima sa ovog uređaja."
                        },
                        primaryText = "Vrati iz oblaka",
                        onPrimary = {
                            overwriteConfirm = null
                            startRestore()
                        },
                        secondaryText = "Ipak napravi backup",
                        onSecondary = {
                            overwriteConfirm = null
                            startBackup(confirmedOverwrite = true)
                        },
                        cancelText = "Otkaži",
                        onCancel = { overwriteConfirm = null }
                    )
                }

                if (showRestoreOffer) {
                    CloudChoiceDialog(
                        title = "Pronađen backup u oblaku",
                        message = "Na Google Drive-u postoji backup tvojih podataka. " +
                            "Želiš li da ga vratiš na ovaj uređaj?",
                        primaryText = "Vrati iz oblaka",
                        onPrimary = {
                            showRestoreOffer = false
                            startRestore()
                        },
                        cancelText = "Ne sada",
                        onCancel = { showRestoreOffer = false }
                    )
                }

                pendingRestore?.let { snapshot ->
                    CloudChoiceDialog(
                        title = "Vraćanje podataka",
                        message = "Broj dužnika u backup-u iz oblaka: ${snapshot.debtors.size}. " +
                            "Svi trenutni podaci na ovom uređaju biće zamenjeni.",
                        primaryText = "Zameni",
                        onPrimary = {
                            pendingRestore = null
                            applyRestore(snapshot)
                        },
                        cancelText = "Otkaži",
                        onCancel = { pendingRestore = null }
                    )
                }

                cloudMessage?.let { state ->
                    InfoDialog(
                        title = state.title,
                        message = state.message,
                        onDismiss = { cloudMessage = null }
                    )
                }
            }
        }
    }

    private fun initializeApp() {
        googleDriveBackup = GoogleDriveBackupProvider.get(application)
    }
}
