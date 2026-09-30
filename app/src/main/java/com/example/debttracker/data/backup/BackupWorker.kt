package com.example.debttracker.data.backup

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.debttracker.data.DebtManagerProvider

/**
 * Pozadinski posao koji salje backup na Google Drive. Pokrece ga [BackupScheduler]
 * (posle izmene + dnevno), uz uslov da ima mreze.
 *
 * Pravila "prazno ne gazi popunjeno" i "sveza instalacija ne prepisuje zateceni
 * backup" su u [GoogleDriveBackup.backup]; ovde se `confirmedOverwrite` nikad ne
 * salje, pa se takav backup tiho preskoci umesto da pita korisnika.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? Application ?: return Result.success()
        val backup = GoogleDriveBackupProvider.get(app)

        if (!backup.isSignedIn()) {
            Log.d(TAG, "Preskacem auto-backup: korisnik nije prijavljen")
            return Result.success()
        }

        val debtManager = DebtManagerProvider.get(app)
        val success = try {
            backup.backup(
                jsonData = debtManager.exportBackupJson(),
                localIsEmpty = debtManager.isEmpty,
                confirmedOverwrite = false
            )
        } catch (e: Exception) {
            Log.w(TAG, "Auto-backup pukao: ${e.message}")
            false
        }

        if (success) {
            Log.d(TAG, "Auto-backup uspesan")
            return Result.success()
        }

        return when (val state = backup.syncState.value) {
            // Obe su odluke koje mora da donese korisnik na ekranu - ponavljanje ne pomaze.
            is SyncState.OverwriteNeedsConfirmation -> {
                Log.d(TAG, "Preskacem auto-backup: prepisivanje trazi potvrdu korisnika")
                Result.success()
            }
            is SyncState.EmptyData -> {
                Log.d(TAG, "Preskacem auto-backup: nema podataka")
                Result.success()
            }
            else -> {
                Log.w(TAG, "Auto-backup neuspesan: ${(state as? SyncState.Error)?.message}")
                if (runAttemptCount >= MAX_RETRY_ATTEMPTS) Result.failure() else Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val MAX_RETRY_ATTEMPTS = 7
    }
}
