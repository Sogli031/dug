package com.example.debttracker.data.backup

import android.app.Application

object GoogleDriveBackupProvider {
    @Volatile
    private var instance: GoogleDriveBackup? = null

    fun get(app: Application): GoogleDriveBackup {
        return instance ?: synchronized(this) {
            instance ?: GoogleDriveBackup(app.applicationContext).also {
                instance = it
            }
        }
    }
}
