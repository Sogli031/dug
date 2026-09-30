package com.example.debttracker.data

import com.example.debttracker.data.backup.BackupFilePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DebtBackupCodecTest {
    private val populated = DebtSnapshot(
        debtors = listOf(
            Debtor(
                name = "Pera Perić",
                initialDebt = 355000,
                totalDebt = 325000,
                payments = mutableListOf(
                    Payment("01.02.2026", 10000),
                    Payment("01.02.2026", 10000),
                    Payment("15.03.2026", 10000)
                )
            ),
            Debtor(name = "Žika \"Kum\"", initialDebt = 1170, totalDebt = 1170),
            Debtor(
                name = "Duznik 3",
                initialDebt = 50000,
                totalDebt = 1,
                payments = mutableListOf(Payment("29.02.2024", 49999))
            )
        ),
        currentIndex = 2
    )

    @Test
    fun `export - import - export gives identical state`() {
        val firstExport = DebtBackupCodec.toJson(populated).toString(2)
        val imported = DebtBackupCodec.parse(firstExport, strict = true)
        val secondExport = DebtBackupCodec.toJson(imported).toString(2)

        assertEquals(populated, imported)
        assertEquals(firstExport, secondExport)
    }

    @Test
    fun `empty state round-trips`() {
        val empty = DebtSnapshot(debtors = emptyList(), currentIndex = 0)

        val imported = DebtBackupCodec.parse(DebtBackupCodec.toJson(empty).toString(2), strict = true)

        assertEquals(empty, imported)
    }

    @Test
    fun `oldest single-debtor backup format still restores`() {
        val legacy = """{"total_debt": 345000, "payments": [{"date": "1.2.2025", "amount": 10000}]}"""

        val imported = DebtBackupCodec.parse(legacy, strict = true)

        assertEquals(
            DebtSnapshot(
                debtors = listOf(
                    Debtor("Duznik 1", 355000, 345000, mutableListOf(Payment("01.02.2025", 10000)))
                ),
                currentIndex = 0
            ),
            imported
        )
    }

    @Test
    fun `backup without initial debt and current index still restores`() {
        val old = """{"debtors": [{"name": " Mika ", "total_debt": 9000, "payments": [{"date": "05.01.2026", "amount": 1000}]}]}"""

        val imported = DebtBackupCodec.parse(old, strict = true)

        assertEquals(0, imported.currentIndex)
        assertEquals(
            listOf(Debtor("Mika", 10000, 9000, mutableListOf(Payment("05.01.2026", 1000)))),
            imported.debtors
        )
    }

    @Test
    fun `invalid payloads fail as a whole in strict mode`() {
        val invalid = listOf(
            "ovo nije json",
            "[]",
            "{}",
            """{"error": {"code": 401}}""",
            """{"debtors": "nije niz"}""",
            """{"debtors": ["nije objekat"]}""",
            """{"debtors": [{"name": "A"}]}""",
            """{"debtors": [{"name": "A", "total_debt": "abc"}]}""",
            """{"debtors": [{"name": "A", "total_debt": 100, "payments": "nije niz"}]}""",
            """{"debtors": [{"name": "A", "total_debt": 100, "payments": [7]}]}""",
            """{"debtors": [{"name": "A", "total_debt": 100, "payments": [{"date": "01.01.2026"}]}]}""",
            """{"debtors": [{"name": "A", "total_debt": 100, "payments": [{"amount": 5}]}]}""",
            """{"debtors": [{"name": "A", "total_debt": 100, "payments": [{"date": "31.02.2026", "amount": 5}]}]}""",
            """{"debtors": [{"name": "OK", "total_debt": 100}, {"name": "Los", "total_debt": 100, "payments": [{"date": "x", "amount": 5}]}]}"""
        )

        invalid.forEach { raw ->
            try {
                DebtBackupCodec.parse(raw, strict = true)
                fail("Ocekivan neuspeh za: $raw")
            } catch (expected: BackupFormatException) {
                assertFalse(expected.message.isNullOrBlank())
            }
        }
    }

    @Test
    fun `local file is still read leniently`() {
        val sloppy = """{"debtors": ["smece", {"name": "A", "total_debt": "10.000", "payments": [{"date": "x", "amount": 5}]}]}"""

        val loaded = DebtBackupCodec.parse(sloppy, strict = false)

        assertEquals(1, loaded.debtors.size)
        assertEquals(10000, loaded.debtors[0].totalDebt)
        assertEquals(DebtBackupCodec.todayString(), loaded.debtors[0].payments[0].date)
    }

    @Test
    fun `prev copy names round-trip and only oldest copies are pruned`() {
        val main = "debt_backup.json"
        val name = BackupFilePolicy.prevCopyName(main, 1700000000000L)

        assertEquals("debt_backup_prev_1700000000000.json", name)
        assertEquals(1700000000000L, BackupFilePolicy.prevCopyTimestamp(main, name))
        assertNull(BackupFilePolicy.prevCopyTimestamp(main, main))
        assertNull(BackupFilePolicy.prevCopyTimestamp(main, "debt_backup_prev_x.json"))

        val files = (1..8).map { "id$it" to BackupFilePolicy.prevCopyName(main, it * 1000L) } + ("main" to main)
        assertEquals(setOf("id1", "id2", "id3"), BackupFilePolicy.prevCopiesToDelete(main, files.shuffled()).toSet())
        assertTrue(BackupFilePolicy.prevCopiesToDelete(main, files.take(5)).isEmpty())
    }
}
