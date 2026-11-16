package com.clottins.dailyledger

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseTest {
    private lateinit var db: LedgerDatabase

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    LedgerDatabase::class.java,
                )
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun close() {
        db.close()
    }

    private val entry =
        Entry(
            id = "one",
            type = EXPENSE,
            cents = 19900,
            category = "购物消费",
            date = "2026-11-14",
            note = "耳机",
        )

    @Test
    fun upsertEditsOriginalWithoutDuplicating() = runBlocking {
        db.ledgerDao().putEntries(listOf(entry))
        db.ledgerDao().putEntries(listOf(entry.copy(cents = 15000)))
        assertEquals(1, db.ledgerDao().entries().size)
        assertEquals(15000L, db.ledgerDao().entries().single().cents)
    }

    @Test
    fun failedReplaceTransactionKeepsOriginal() = runBlocking {
        db.ledgerDao().putEntries(listOf(entry))
        db.ledgerDao().putPreferences(Preferences(openingCents = 1234))
        try {
            db.withTransaction {
                db.ledgerDao().clearEntries()
                db.ledgerDao().putPreferences(Preferences(openingCents = 0))
                error("simulated failure")
            }
            fail("Expected rollback")
        } catch (_: IllegalStateException) {}
        assertEquals(listOf(entry), db.ledgerDao().entries())
        assertEquals(1234L, db.ledgerDao().preferences()!!.openingCents)
    }

    @Test
    fun flowObservesCommittedRefund() = runBlocking {
        val refund =
            Entry(
                id = "return",
                type = REFUND,
                cents = 5000,
                category = "退款",
                date = "2026-11-16",
                linkedExpenseId = "one",
            )
        db.withTransaction {
            db.ledgerDao().putCategories(defaultCategories)
            db.ledgerDao().putEntries(listOf(entry, refund))
        }
        val all = db.ledgerDao().observeEntries().first()
        assertEquals(2, all.size)
        assertEquals(-14900L, Ledger.totals(all).balance)
        assertEquals("（部分退款）", Ledger.refundLabel(all, entry))
    }
}
