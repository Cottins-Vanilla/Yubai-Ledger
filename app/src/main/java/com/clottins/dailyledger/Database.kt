package com.clottins.dailyledger

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM entries ORDER BY date DESC, time DESC, id DESC")
    fun observeEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM categories ORDER BY rowid") fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM preferences WHERE id = 0") fun observePreferences(): Flow<Preferences?>

    @Query("SELECT * FROM entries") suspend fun entries(): List<Entry>

    @Query("SELECT * FROM categories ORDER BY rowid") suspend fun categories(): List<Category>

    @Query("SELECT * FROM preferences WHERE id = 0") suspend fun preferences(): Preferences?

    @Upsert suspend fun putEntries(entries: List<Entry>)

    @Upsert suspend fun putCategories(categories: List<Category>)

    @Upsert suspend fun putPreferences(preferences: Preferences)

    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: String)

    @Query("DELETE FROM entries") suspend fun clearEntries()

    @Query("DELETE FROM categories") suspend fun clearCategories()
}

@Database(
    entities = [Entry::class, Category::class, Preferences::class],
    version = 1,
    exportSchema = true,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
}
