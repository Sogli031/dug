package com.example.debttracker

import android.app.Application
import com.example.debttracker.data.backup.BackupScheduler
import com.programi.crashlog.CrashLogStorage

class DebtTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogStorage.install(this, getString(R.string.app_name))

        // Dnevni backup ako korisnik dugo ne menja nista (KEEP: ne resetuje se na svakom startu)
        BackupScheduler.schedulePeriodic(this)
    }
}
