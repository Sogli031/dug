package com.example.debttracker.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val TAG = "GoogleDriveBackup"
private const val BACKUP_FILE_NAME = "debt_backup.json"
private const val APP_DATA_FOLDER = "appDataFolder"
private const val PREFS_NAME = "backup_prefs"
private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
private const val KEY_LAST_SYNC_TIME = "last_sync_time"
private const val KEY_CLOUD_ADOPTED = "cloud_adopted"

class GoogleDriveBackup(private val context: Context) {

    private val _signInState = MutableStateFlow<SignInState>(SignInState.SignedOut)
    val signInState: StateFlow<SignInState> = _signInState

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime

    private var driveService: Drive? = null
    private val syncMutex = Mutex()

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        val savedTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        if (savedTime > 0) {
            _lastSyncTime.value = savedTime
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            _signInState.value = SignInState.SignedIn(account.email ?: "Unknown")
            initDriveService(account)
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            _signInState.value = SignInState.SignedIn(account.email ?: "Unknown")
            initDriveService(account)
        } else {
            _signInState.value = SignInState.SignedOut
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut()
                _signInState.value = SignInState.SignedOut
                driveService = null
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }

    private fun initDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("DebtTracker")
            .build()
    }

    /**
     * Da li je ova instalacija "preuzela" cloud backup: uspesan restore, uspesan backup kad u
     * oblaku nije bilo fajla, ili izricita potvrda korisnika da prepise postojeci backup.
     * Dok nije preuzeto, postojeci cloud backup se ne prepisuje bez pitanja.
     */
    fun isCloudAdopted(): Boolean {
        if (!prefs.contains(KEY_CLOUD_ADOPTED)) {
            // Postojece instalacije: ako je sa ovog uredjaja vec radjen backup, cloud je "nas"
            val adopted = prefs.getLong(KEY_LAST_SYNC_TIME, 0L) > 0
            prefs.edit().putBoolean(KEY_CLOUD_ADOPTED, adopted).apply()
        }
        return prefs.getBoolean(KEY_CLOUD_ADOPTED, false)
    }

    /** Poziva se kada su podaci iz oblaka uspesno vraceni i sacuvani na uredjaju. */
    fun markCloudAdopted() {
        prefs.edit().putBoolean(KEY_CLOUD_ADOPTED, true).apply()
    }

    /** Neuspelo listanje (mreza/nalog) je UNKNOWN - nikad se ne tumaci kao "oblak je prazan". */
    suspend fun cloudBackupStatus(): CloudBackupStatus = syncMutex.withLock {
        val drive = driveService ?: return@withLock CloudBackupStatus.UNKNOWN
        withContext(Dispatchers.IO) {
            try {
                if (findBackupFile(drive) != null) CloudBackupStatus.EXISTS else CloudBackupStatus.MISSING
            } catch (e: Exception) {
                Log.e(TAG, "Cloud check failed", e)
                CloudBackupStatus.UNKNOWN
            }
        }
    }

    /**
     * @param localIsEmpty lokalno nema nijednog duznika - prazno ne sme da pregazi popunjeno
     * @param confirmedOverwrite korisnik je izricito potvrdio prepisivanje postojeceg backup-a
     */
    suspend fun backup(
        jsonData: String,
        localIsEmpty: Boolean,
        confirmedOverwrite: Boolean = false
    ): Boolean = syncMutex.withLock {
        val drive = driveService ?: return@withLock false

        withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                val existingFileId = findBackupFile(drive)

                if (existingFileId == null && localIsEmpty) {
                    Log.w(TAG, "Backup skipped - no data to backup")
                    _syncState.value = SyncState.EmptyData
                    return@withContext false
                }

                if (existingFileId != null && !confirmedOverwrite && (localIsEmpty || !isCloudAdopted())) {
                    _syncState.value = SyncState.OverwriteNeedsConfirmation(localIsEmpty)
                    return@withContext false
                }

                val contentStream = ByteArrayContent.fromString("application/json", jsonData)

                if (existingFileId != null) {
                    if (confirmedOverwrite) {
                        markCloudAdopted()
                    }
                    // Ako kopija ne uspe, baca izuzetak i backup se prekida pre prepisivanja
                    createPrevCopy(drive, existingFileId)
                    drive.files().update(existingFileId, null, contentStream).execute()
                } else {
                    val fileMetadata = com.google.api.services.drive.model.File().apply {
                        name = BACKUP_FILE_NAME
                        parents = listOf(APP_DATA_FOLDER)
                    }
                    drive.files().create(fileMetadata, contentStream).execute()
                }

                markCloudAdopted()
                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()
                prunePrevCopies(drive)
                _syncState.value = SyncState.Success
                true
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Backup failed")
                false
            }
        }
    }

    suspend fun restore(): String? = syncMutex.withLock {
        val drive = driveService ?: return@withLock null

        withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                val fileId = findBackupFile(drive)
                if (fileId == null) {
                    _syncState.value = SyncState.Idle
                    return@withContext null
                }

                val data = downloadFile(drive, fileId)
                if (data.isBlank()) {
                    throw java.io.IOException("Backup fajl je prazan")
                }
                _syncState.value = SyncState.Success
                data
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Restore failed")
                null
            }
        }
    }

    /**
     * Trazi glavni backup fajl po tacnom imenu. Nista ne brise - ako postoji vise fajlova
     * sa istim imenom, koristi se najnoviji, a ostali se ne diraju. Greska pri listanju
     * se prosledjuje pozivaocu (ne tumaci se kao "nema fajla").
     */
    private fun findBackupFile(drive: Drive): String? {
        val result = drive.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setOrderBy("modifiedTime desc")
            .setFields("files(id, name, modifiedTime)")
            .execute()

        return result.files.orEmpty()
            .firstOrNull { it.name == BACKUP_FILE_NAME }
            ?.id
    }

    private fun downloadFile(drive: Drive, fileId: String): String {
        val outputStream = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        return outputStream.toString("UTF-8")
    }

    /** Pre prepisivanja cuva postojeci fajl kao "<ime>_prev_<vreme>.json". Baca izuzetak ako ne uspe. */
    private fun createPrevCopy(drive: Drive, fileId: String) {
        val copyMetadata = com.google.api.services.drive.model.File().apply {
            name = BackupFilePolicy.prevCopyName(BACKUP_FILE_NAME, System.currentTimeMillis())
            parents = listOf(APP_DATA_FOLDER)
        }
        try {
            drive.files().copy(fileId, copyMetadata).execute()
        } catch (e: Exception) {
            Log.w(TAG, "files.copy failed, trying download + create", e)
            val content = ByteArrayContent.fromString("application/json", downloadFile(drive, fileId))
            drive.files().create(copyMetadata, content).execute()
        }
    }

    /** Cuva se najnovijih 5 prethodnih kopija. Neuspeh ciscenja ne obara backup. */
    private fun prunePrevCopies(drive: Drive) {
        try {
            val files = mutableListOf<Pair<String, String>>()
            var pageToken: String? = null
            do {
                val result = drive.files().list()
                    .setSpaces(APP_DATA_FOLDER)
                    .setQ("trashed = false")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageSize(200)
                    .setPageToken(pageToken)
                    .execute()
                result.files.orEmpty().forEach { file ->
                    val id = file.id
                    val name = file.name
                    if (id != null && name != null) files.add(id to name)
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)

            BackupFilePolicy.prevCopiesToDelete(BACKUP_FILE_NAME, files).forEach { id ->
                try {
                    drive.files().delete(id).execute()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete old prev copy: $id", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pruning prev copies failed", e)
        }
    }

    fun isSignedIn(): Boolean = _signInState.value is SignInState.SignedIn

    fun isFirstLaunch(): Boolean {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    }

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }
}

sealed class SignInState {
    data object SignedOut : SignInState()
    data class SignedIn(val email: String) : SignInState()
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data object EmptyData : SyncState()
    /**
     * U oblaku postoji backup koji bi bio pregazen: ova instalacija ga nije preuzela ili je
     * lokalno stanje prazno. Backup se ne radi dok korisnik izricito ne potvrdi.
     */
    data class OverwriteNeedsConfirmation(val localIsEmpty: Boolean) : SyncState()
    data class Error(val message: String) : SyncState()
}

enum class CloudBackupStatus { EXISTS, MISSING, UNKNOWN }
