package com.example.debttracker.data

import android.content.Context

/**
 * Jedna instanca [DebtManager] za ceo proces.
 *
 * Ranije ga je pravio MainActivity, pa mu pozadinski posao (auto-backup) nije mogao
 * da priđe. Dve instance bi držale dva odvojena keša istog fajla.
 */
object DebtManagerProvider {
    @Volatile
    private var instance: DebtManager? = null

    fun get(context: Context): DebtManager {
        return instance ?: synchronized(this) {
            instance ?: DebtManager(context.applicationContext).also { instance = it }
        }
    }
}
