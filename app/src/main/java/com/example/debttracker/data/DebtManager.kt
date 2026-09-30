package com.example.debttracker.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.max

import com.example.debttracker.data.backup.BackupScheduler

private const val DEFAULT_PAYMENT = 10000

class DebtManager(context: Context) {
    private val appContext = context.applicationContext
    private val localDataFile = File(appContext.filesDir, "debt_data.json")

    private val defaultPayment: Int = DEFAULT_PAYMENT
    val debtors: MutableList<Debtor> = mutableListOf()
    private var currentIndex: Int = 0

    private var onChangeCallback: (() -> Unit)? = null

    val currentDebtor: Debtor?
        get() {
            if (debtors.isEmpty()) {
                return null
            }
            currentIndex = currentIndex.coerceIn(0, debtors.lastIndex)
            return debtors[currentIndex]
        }

    val totalDebt: Int
        get() = currentDebtor?.totalDebt ?: 0
    val initialDebt: Int
        get() = currentDebtor?.initialDebt ?: 0
    val payments: MutableList<Payment>
        get() = currentDebtor?.payments ?: mutableListOf()
    val currentName: String
        get() = currentDebtor?.name ?: "Nema duznika"

    /** Nema nijednog duznika (npr. sveza instalacija). */
    val isEmpty: Boolean
        get() = debtors.isEmpty()

    init {
        // Učitaj lokalne podatke pri pokretanju
        loadLocalData()
    }

    private fun loadLocalData() {
        if (!localDataFile.exists()) {
            return
        }
        try {
            // Lokalni fajl se cita popustljivo, da se postojeci podaci uvek ucitaju
            applySnapshotInMemory(
                DebtBackupCodec.parse(localDataFile.readText(Charsets.UTF_8), strict = false)
            )
        } catch (_: Exception) {
            // Ignore errors
        }
    }

    private fun saveLocally(): Boolean {
        val saved = writeAtomically(DebtBackupCodec.toJson(snapshot()).toString(2))
        // Svaka snimljena izmena zakazuje i backup u oblak. applyBackup() namerno ne
        // prolazi ovuda - vraceno stanje ne treba odmah nazad u oblak.
        if (saved) BackupScheduler.scheduleAfterChange(appContext)
        return saved
    }

    /** Upis preko privremenog fajla + rename, da prekid usred upisa ne osteti postojece podatke. */
    private fun writeAtomically(text: String): Boolean {
        val tempFile = File(localDataFile.parentFile, localDataFile.name + ".tmp")
        return try {
            FileOutputStream(tempFile).use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            Files.move(
                tempFile.toPath(),
                localDataFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            true
        } catch (_: Exception) {
            tempFile.delete()
            false
        }
    }

    fun onChange(callback: () -> Unit) {
        onChangeCallback = callback
    }

    private fun notifyChange() {
        saveLocally()
        onChangeCallback?.invoke()
    }

    private fun snapshot(): DebtSnapshot {
        return DebtSnapshot(debtors = debtors.toList(), currentIndex = currentIndex)
    }

    private fun applySnapshotInMemory(snapshot: DebtSnapshot) {
        debtors.clear()
        debtors.addAll(snapshot.debtors)
        currentIndex = if (debtors.isEmpty()) 0 else snapshot.currentIndex.coerceIn(0, debtors.lastIndex)
    }

    fun exportBackupJson(): String {
        return DebtBackupCodec.toJson(snapshot()).toString(2)
    }

    /**
     * Proverava CEO backup pre bilo kakve izmene. Baca [BackupFormatException] ako bilo koji
     * deo nije ispravan - trenutni podaci tada ostaju netaknuti.
     */
    fun parseBackup(jsonText: String): DebtSnapshot {
        return DebtBackupCodec.parse(jsonText, strict = true)
    }

    /**
     * Zamenjuje sve podatke sadrzajem backup-a. Prvo se upisuje fajl (atomski), pa tek onda
     * menja stanje u memoriji. Vraca true samo ako su podaci zaista sacuvani na uredjaju.
     */
    fun applyBackup(snapshot: DebtSnapshot): Boolean {
        if (!writeAtomically(DebtBackupCodec.toJson(snapshot).toString(2))) {
            return false
        }
        applySnapshotInMemory(snapshot)
        onChangeCallback?.invoke()
        return true
    }

    fun addDebtor(name: String, amountRsd: Int) {
        val normalizedAmount = normalizeAmount(amountRsd)
        val safeName = name.trim().ifBlank { "Duznik ${debtors.size + 1}" }
        debtors.add(
            Debtor(
                name = safeName,
                initialDebt = normalizedAmount,
                totalDebt = normalizedAmount,
                payments = mutableListOf()
            )
        )
        currentIndex = debtors.lastIndex
        notifyChange()
    }

    fun selectNextDebtor() {
        if (debtors.isEmpty()) {
            return
        }
        currentIndex = (currentIndex + 1) % debtors.size
        // Izabrani duznik je deo stanja koje se cuva i vraca iz backup-a
        saveLocally()
    }

    fun selectPreviousDebtor() {
        if (debtors.isEmpty()) {
            return
        }
        currentIndex = if (currentIndex - 1 < 0) debtors.lastIndex else currentIndex - 1
        saveLocally()
    }

    private fun removeCurrentDebtor() {
        if (currentIndex !in debtors.indices) {
            return
        }
        debtors.removeAt(currentIndex)
        currentIndex = if (debtors.isEmpty()) {
            0
        } else {
            currentIndex.coerceIn(0, debtors.lastIndex)
        }
    }

    fun addPayment(amount: Int? = null, date: String? = null): PaymentResult? {
        val debtor = currentDebtor ?: return null
        val amt = if (amount == null) defaultPayment else normalizeAmount(amount)
        val dt = normalizeDate(date)
        debtor.payments.add(Payment(date = dt, amount = amt))
        val remaining = normalizeAmount(debtor.totalDebt) - amt
        val debtorName = debtor.name
        val removed = if (remaining <= 0) {
            removeCurrentDebtor()
            true
        } else {
            debtor.totalDebt = remaining
            false
        }
        notifyChange()
        return PaymentResult(
            amount = amt,
            date = dt,
            debtorName = debtorName,
            remainingDebt = max(remaining, 0),
            wasRemoved = removed
        )
    }

    fun deletePayment(index: Int): Payment? {
        val debtor = currentDebtor ?: return null
        if (index !in debtor.payments.indices) {
            return null
        }
        val payment = debtor.payments.removeAt(index)
        val amount = normalizeAmount(payment.amount)
        debtor.totalDebt = normalizeAmount(debtor.totalDebt) + amount
        notifyChange()
        return payment
    }

    private fun normalizeAmount(value: Any?): Int {
        return DebtBackupCodec.amountOrNull(value) ?: 0
    }

    private fun normalizeDate(dateStr: String?): String {
        return DebtBackupCodec.normalizeDateOrNull(dateStr) ?: DebtBackupCodec.todayString()
    }
}
