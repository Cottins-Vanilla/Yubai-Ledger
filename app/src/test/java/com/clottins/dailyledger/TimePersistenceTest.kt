package com.clottins.dailyledger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TimePersistenceTest {
    @Test
    fun reopeningExistingLedgerAndChangingTimeKeepsOtherDataAndBackups() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filename = "time-update-compatibility.db"
        val original = Entry("kept", EXPENSE, 3200, "餐饮美食", "2026-11-17", "08:45", "晚餐")
        val refund = Entry("linked", REFUND, 500, "退款", "2026-11-17", "09:00", "退差价", "kept")
        val preferences = Preferences(openingCents = 123456, hideBalance = true)
        fun open() = Room.databaseBuilder(context, LedgerDatabase::class.java, filename).build()
        open().let { db ->
            try {
                db.ledgerDao().putCategories(defaultCategories)
                db.ledgerDao().putPreferences(preferences)
                db.ledgerDao().putEntries(listOf(original, refund))
            } finally {
                db.close()
            }
        }
        open().let { db ->
            try {
                val dao = db.ledgerDao()
                assertEquals(setOf(original, refund), dao.entries().toSet())
                val edited = original.copy(time = "23:59")
                dao.putEntries(listOf(edited))
                assertEquals(setOf(edited, refund), dao.entries().toSet())
                assertEquals(preferences, dao.preferences())
                assertEquals(defaultCategories.size, dao.categories().size)
                val snapshot = LedgerSnapshot(dao.entries(), dao.categories(), preferences)
                assertEquals(snapshot.entries, Backup.parseJson(Backup.json(snapshot)).entries)
                assertEquals(
                    snapshot.entries,
                    Backup.parseCsv(Backup.csv(snapshot.entries)).entries,
                )
            } finally {
                db.close()
            }
        }
    }
}
