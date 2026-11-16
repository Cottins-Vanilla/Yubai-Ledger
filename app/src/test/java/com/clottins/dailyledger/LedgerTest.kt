package com.clottins.dailyledger

import org.junit.Assert.*
import org.junit.Test

class LedgerTest {
    private val expense = Entry("purchase", EXPENSE, 20000, "购物消费", "2026-11-14", "15:20", "耳机")
    private val income = Entry("salary", INCOME, 100000, "工资收入", "2026-11-15", "10:00", "工资")
    private val refund =
        Entry("refund1", REFUND, 5000, "退款", "2026-11-16", "09:00", "退货", "purchase")

    private fun snapshot(entries: List<Entry>) =
        LedgerSnapshot(entries, defaultCategories, Preferences(openingCents = 1234))

    @Test
    fun refundsOnlyAffectBalance() {
        val t = Ledger.totals(listOf(expense, income, refund))
        assertEquals(100000L, t.income)
        assertEquals(20000L, t.expense)
        assertEquals(5000L, t.refund)
        assertEquals(85000L, t.balance)
        assertEquals(86234L, Ledger.totals(listOf(expense, income, refund), 1234).balance)
    }

    @Test
    fun exactMoneyAndNegativeOpening() {
        assertEquals(1234L, Ledger.parseMoney("12.34"))
        assertEquals(-50L, Ledger.parseMoney("-0.50", true))
        assertEquals("-0.50", Ledger.rawMoney(-50))
        assertThrows(IllegalArgumentException::class.java) { Ledger.parseMoney("1.001") }
        assertThrows(IllegalArgumentException::class.java) { Ledger.parseMoney("1e3") }
        assertThrows(IllegalArgumentException::class.java) { Ledger.parseMoney("-1") }
    }

    @Test
    fun partialAndFullRefunds() {
        assertEquals("（部分退款）", Ledger.refundLabel(listOf(expense, refund), expense))
        assertEquals(
            "（已退款）",
            Ledger.refundLabel(listOf(expense, refund.copy(cents = 20000)), expense),
        )
        assertEquals("", Ledger.refundLabel(listOf(expense), expense))
    }

    @Test
    fun aggregateRefundLimit() {
        Ledger.validate(
            listOf(expense, refund, refund.copy(id = "refund2", cents = 15000)),
            defaultCategories,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.validate(
                listOf(expense, refund, refund.copy(id = "refund2", cents = 15001)),
                defaultCategories,
            )
        }
    }

    @Test
    fun refundEditDoesNotDoubleCount() {
        val current = snapshot(listOf(expense, refund))
        val replacement =
            current.entries.filterNot { it.id == refund.id } + refund.copy(cents = 20000)
        Ledger.validate(replacement, current.categories)
        assertEquals(20000L, Ledger.refunded(replacement, expense.id))
    }

    @Test
    fun rejectInvalidLinksAndDates() {
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.validate(
                listOf(income, refund.copy(linkedExpenseId = income.id)),
                defaultCategories,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.validate(listOf(expense, refund.copy(date = "2026-11-13")), defaultCategories)
        }
        assertThrows(IllegalStateException::class.java) {
            Ledger.validate(listOf(refund), defaultCategories)
        }
        Ledger.validate(listOf(refund.copy(linkedExpenseId = null)), defaultCategories)
    }

    @Test
    fun mergeSkipsIdenticalIdsButRejectsConflicts() {
        val current = snapshot(listOf(expense))
        assertEquals(1, Ledger.merge(current, snapshot(listOf(expense)), false).entries.size)
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.merge(current, snapshot(listOf(expense.copy(cents = 30000))), false)
        }
        assertEquals(20000L, current.entries.single().cents)
    }

    @Test
    fun importResolvesLinksRegardlessOfRowOrder() {
        val current = snapshot(emptyList())
        val merged = Ledger.merge(current, snapshot(listOf(refund, expense)), false)
        assertEquals(2, merged.entries.size)
        assertEquals(1234L, merged.preferences.openingCents)
    }

    @Test
    fun originalAmountCannotFallBelowRefunds() {
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.validate(listOf(expense.copy(cents = 4999), refund), defaultCategories)
        }
    }

    @Test
    fun replacementKeepsBackupPreferences() {
        val imported =
            snapshot(listOf(expense))
                .copy(preferences = Preferences(openingCents = -50, hideBalance = true))
        assertEquals(imported, Ledger.merge(snapshot(listOf(income)), imported, true))
    }

    @Test
    fun jsonRoundTripPreservesRefundAndSettings() {
        val s =
            snapshot(listOf(expense, income, refund))
                .copy(preferences = Preferences(openingCents = -50, hideBalance = true))
        val decoded = Backup.parseJson(Backup.json(s))
        assertEquals(s.entries, decoded.entries)
        assertEquals(s.preferences, decoded.preferences)
        Ledger.validate(decoded.entries, decoded.categories)
    }

    @Test
    fun jsonRejectsFractionalCentsAndUnknownVersions() {
        val s = snapshot(listOf(expense))
        assertThrows(IllegalArgumentException::class.java) {
            Backup.parseJson(
                Backup.json(s).replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
            )
        }
        assertThrows(IllegalStateException::class.java) {
            Backup.parseJson(
                Backup.json(s).replace("\"amountCents\": 20000", "\"amountCents\": 20000.5")
            )
        }
    }

    @Test
    fun csvRoundTripPreservesChineseQuotesNewlinesAndFormulaText() {
        listOf("中文,备注\n换行\r\n与\"引号\"", "=SUM(A1:A2)", "'+原始", "'普通备注", "\t开头", "@函数", "-备注", "+文本")
            .forEach { note ->
                val rows = listOf(expense.copy(note = note), refund)
                val decoded = Backup.parseCsv(Backup.csv(rows))
                assertEquals(rows, decoded.entries)
            }
    }

    @Test
    fun externalCsvApostrophesAreNotRemoved() {
        val csv = "id,date,type,category,amount,note\nx,2026-11-14,expense,购物消费,1.00,'=原文\n"
        assertEquals("'=原文", Backup.parseCsv(csv).entries.single().note)
    }

    @Test
    fun malformedCsvIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Backup.parseRows("a,\"b") }
        assertThrows(IllegalArgumentException::class.java) { Backup.parseRows("\"a\"oops,b") }
        assertThrows(IllegalArgumentException::class.java) {
            Backup.parseCsv("date,type,category,amount,note\n2026-11-14,expense,购物消费,1.00\n")
        }
    }

    @Test
    fun zeroAndDuplicateIdsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.validate(listOf(expense.copy(cents = 0)), defaultCategories)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ledger.validate(listOf(expense, expense), defaultCategories)
        }
    }
}
