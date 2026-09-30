package com.example.debttracker.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Kompletno stanje aplikacije koje se cuva lokalno i u cloud backup-u. */
data class DebtSnapshot(
    val debtors: List<Debtor>,
    val currentIndex: Int
)

/** Backup ne moze da se procita u celini. Poruka je namenjena korisniku. */
class BackupFormatException(message: String) : Exception(message)

/**
 * JSON format podataka (isti za lokalni fajl i cloud backup).
 *
 * strict = true (restore iz oblaka): sve sto ne moze da se procita je greska - nista se
 * tiho ne preskace niti zamenjuje podrazumevanom vrednoscu.
 * strict = false (lokalni fajl): stara popustljiva pravila, da se postojeci podaci uvek ucitaju.
 */
object DebtBackupCodec {
    private const val LEGACY_INITIAL_DEBT = 355000

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT)

    fun toJson(snapshot: DebtSnapshot): JSONObject {
        val json = JSONObject()
        json.put("current_index", snapshot.currentIndex)
        val debtorsArray = JSONArray()
        snapshot.debtors.forEach { debtor ->
            val debtorJson = JSONObject()
            debtorJson.put("name", debtor.name)
            debtorJson.put("initial_debt", debtor.initialDebt)
            debtorJson.put("total_debt", debtor.totalDebt)
            val paymentsArray = JSONArray()
            debtor.payments.forEach { payment ->
                val item = JSONObject()
                item.put("date", payment.date)
                item.put("amount", payment.amount)
                paymentsArray.put(item)
            }
            debtorJson.put("payments", paymentsArray)
            debtorsArray.put(debtorJson)
        }
        json.put("debtors", debtorsArray)
        return json
    }

    fun parse(jsonText: String, strict: Boolean): DebtSnapshot {
        val json = try {
            JSONObject(jsonText)
        } catch (e: JSONException) {
            throw BackupFormatException("Backup nije u ispravnom formatu.")
        }

        val debtors = mutableListOf<Debtor>()
        var currentIndex = 0

        val debtorsJson = json.optJSONArray("debtors")
        if (debtorsJson == null && strict && json.has("debtors")) {
            throw BackupFormatException("Spisak dužnika u backup-u nije ispravan.")
        }

        if (debtorsJson != null) {
            for (i in 0 until debtorsJson.length()) {
                val item = debtorsJson.optJSONObject(i)
                if (item == null) {
                    if (strict) throw BackupFormatException("Dužnik ${i + 1} u backup-u nije ispravan.")
                    continue
                }
                val rawName = item.optString("name", "Duznik ${i + 1}")
                val name = rawName.trim().ifBlank { "Duznik ${i + 1}" }
                val initial = parseAmount(item, "initial_debt", strict, required = false, owner = name)
                val total = parseAmount(item, "total_debt", strict, required = true, owner = name)
                val payments = parsePayments(item, strict, name)
                val inferredInitial = if (initial > 0) {
                    initial
                } else {
                    total + payments.sumOf { it.amount }
                }
                debtors.add(
                    Debtor(
                        name = name,
                        initialDebt = inferredInitial,
                        totalDebt = total,
                        payments = payments
                    )
                )
            }
            currentIndex = json.optInt("current_index", 0)
        } else if (json.has("total_debt") || !strict) {
            // Najstariji format: jedan duznik bez imena
            val total = parseAmount(json, "total_debt", strict, required = strict, owner = "Duznik 1")
            val payments = parsePayments(json, strict, "Duznik 1")
            debtors.add(
                Debtor(
                    name = "Duznik 1",
                    initialDebt = LEGACY_INITIAL_DEBT,
                    totalDebt = total,
                    payments = payments
                )
            )
        } else {
            throw BackupFormatException("Backup ne sadrži podatke o dugovima.")
        }

        currentIndex = if (debtors.isEmpty()) 0 else currentIndex.coerceIn(0, debtors.lastIndex)
        return DebtSnapshot(debtors = debtors, currentIndex = currentIndex)
    }

    private fun parsePayments(owner: JSONObject, strict: Boolean, ownerName: String): MutableList<Payment> {
        val list = mutableListOf<Payment>()
        if (owner.isNull("payments")) {
            return list
        }
        val rawPayments = owner.optJSONArray("payments")
        if (rawPayments == null) {
            if (strict) throw BackupFormatException("Uplate za \"$ownerName\" u backup-u nisu ispravne.")
            return list
        }
        for (i in 0 until rawPayments.length()) {
            val item = rawPayments.optJSONObject(i)
            if (item == null) {
                if (strict) throw BackupFormatException("Uplata ${i + 1} za \"$ownerName\" nije ispravna.")
                continue
            }
            val amount = parseAmount(item, "amount", strict, required = true, owner = ownerName)
            val rawDate = if (item.isNull("date")) null else item.optString("date")
            val date = normalizeDateOrNull(rawDate)
                ?: if (strict) {
                    throw BackupFormatException("Neispravan datum uplate za \"$ownerName\": ${rawDate ?: "-"}")
                } else {
                    todayString()
                }
            list.add(Payment(date = date, amount = amount))
        }
        return list
    }

    private fun parseAmount(
        obj: JSONObject,
        key: String,
        strict: Boolean,
        required: Boolean,
        owner: String
    ): Int {
        if (obj.isNull(key)) {
            if (strict && required) throw BackupFormatException("Nedostaje iznos ($key) za \"$owner\".")
            return 0
        }
        return amountOrNull(obj.opt(key))
            ?: if (strict) throw BackupFormatException("Neispravan iznos ($key) za \"$owner\".") else 0
    }

    /** Isto pravilo kao pri unosu: brojevi, ili tekst sa razmacima/tackama/zarezima kao separatorima. */
    fun amountOrNull(value: Any?): Int? {
        return when (value) {
            null -> null
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            else -> value.toString()
                .trim()
                .replace(" ", "")
                .replace(".", "")
                .replace(",", "")
                .toIntOrNull()
        }
    }

    /** Vraca datum u obliku dd.MM.yyyy, ili null ako ne moze da se procita. */
    fun normalizeDateOrNull(dateStr: String?): String? {
        if (dateStr == null) {
            return null
        }
        return try {
            val cleaned = dateStr.trim().replace(',', '.')
            val parts = cleaned.split(".")
            if (parts.size >= 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2].take(4)
                val normalized = "$day.$month.$year"
                LocalDate.parse(normalized, DATE_FORMATTER)
                normalized
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun todayString(): String {
        return LocalDate.now().format(DATE_FORMATTER)
    }
}
