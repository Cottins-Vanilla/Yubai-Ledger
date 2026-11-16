package com.clottins.dailyledger

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LedgerState(
    val entries: List<Entry> = emptyList(),
    val categories: List<Category> = defaultCategories,
    val preferences: Preferences = Preferences(),
    val ready: Boolean = false,
    val refundedByExpense: Map<String, Long> = emptyMap(),
)

data class ImportPlan(
    val snapshot: LedgerSnapshot,
    val json: Boolean,
    val duplicateCount: Int,
    val conflictCount: Int,
    val possibleDuplicates: Int,
    val mergeError: String?,
    val replaceError: String?,
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val db =
        Room.databaseBuilder(application, LedgerDatabase::class.java, "ledger.db").build()
    private val dao = db.ledgerDao()
    private val mutex = Mutex()
    val busy = MutableStateFlow(false)
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()
    val importPlan = MutableStateFlow<ImportPlan?>(null)
    val state =
        combine(dao.observeEntries(), dao.observeCategories(), dao.observePreferences()) { e, c, p
                ->
                LedgerState(
                    e,
                    c.ifEmpty { defaultCategories },
                    p ?: Preferences(),
                    p != null,
                    e.filter { it.type == REFUND && it.linkedExpenseId != null }
                        .groupBy { it.linkedExpenseId!! }
                        .mapValues { (_, rows) -> rows.sumOf { it.cents } },
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerState())
    val recoveryAvailable =
        MutableStateFlow(application.getFileStreamPath("recovery-before-import.json").exists())

    init {
        action {
            db.withTransaction {
                if (dao.categories().isEmpty()) dao.putCategories(defaultCategories)
                if (dao.preferences() == null) dao.putPreferences(Preferences())
            }
        }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                busy.value = true
                try {
                    withContext(Dispatchers.IO) { block() }
                } catch (e: Exception) {
                    messageChannel.send(e.message ?: "操作失败，请重试")
                } finally {
                    busy.value = false
                }
            }
        }
    }

    private suspend fun snapshot() =
        LedgerSnapshot(dao.entries(), dao.categories(), dao.preferences() ?: Preferences())

    fun save(entry: Entry, done: () -> Unit) = action {
        db.withTransaction {
            val current = dao.entries()
            val replacement = current.filterNot { it.id == entry.id } + entry
            Ledger.validate(replacement, dao.categories())
            dao.putEntries(listOf(entry))
        }
        withContext(Dispatchers.Main) { done() }
    }

    fun delete(entry: Entry, done: () -> Unit) = action {
        db.withTransaction {
            val all = dao.entries()
            require(all.none { it.type == REFUND && it.linkedExpenseId == entry.id }) {
                "这笔支出有关联退款，请先解除关联或删除退款"
            }
            dao.delete(entry.id)
        }
        withContext(Dispatchers.Main) { done() }
    }

    fun setOpening(cents: Long) = action {
        require(cents in -MAX_CENTS..MAX_CENTS)
        db.withTransaction {
            dao.putPreferences((dao.preferences() ?: Preferences()).copy(openingCents = cents))
        }
    }

    fun toggleBalance() = action {
        db.withTransaction {
            val p = dao.preferences() ?: Preferences()
            dao.putPreferences(p.copy(hideBalance = !p.hideBalance))
        }
    }

    fun export(
        uri: Uri,
        json: Boolean,
        start: LocalDate? = null,
        end: LocalDate? = null,
        recovery: Boolean = false,
    ) = action {
        val text =
            if (recovery)
                getApplication<Application>()
                    .getFileStreamPath("recovery-before-import.json")
                    .readText()
            else {
                val data = db.withTransaction { snapshot() }
                if (json) Backup.json(data)
                else
                    Backup.csv(
                        data.entries.filter {
                            (start == null || it.date >= start.toString()) &&
                                (end == null || it.date <= end.toString())
                        }
                    )
            }
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("无法写入所选位置")
        messageChannel.send("导出完成")
    }

    fun inspectImport(uri: Uri) = action {
        val bytes =
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    require(output.size() + n <= Backup.MAX_BYTES) { "文件超过 10 MB 限制" }
                    output.write(buffer, 0, n)
                }
                output.toByteArray()
            } ?: error("无法读取文件")
        val text =
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
                .removePrefix("\uFEFF")
        val json = text.trimStart().startsWith('{')
        val imported = if (json) Backup.parseJson(text) else Backup.parseCsv(text)
        val current = db.withTransaction { snapshot() }
        val byId = current.entries.associateBy { it.id }
        val duplicates = imported.entries.count { byId[it.id] == it }
        val conflicts = imported.entries.count { byId[it.id] != null && byId[it.id] != it }
        val fingerprints = current.entries.map { it.copy(id = "") }.toSet()
        val possible = imported.entries.count { it.id !in byId && it.copy(id = "") in fingerprints }
        importPlan.value =
            ImportPlan(
                imported,
                json,
                duplicates,
                conflicts,
                possible,
                runCatching { Ledger.merge(current, imported, false) }.exceptionOrNull()?.message,
                if (json)
                    runCatching { Ledger.validate(imported.entries, imported.categories) }
                        .exceptionOrNull()
                        ?.message
                else "CSV 不支持完整替换恢复",
            )
    }

    fun applyImport(replace: Boolean) = action {
        val plan = importPlan.value ?: return@action
        require(!replace || plan.json) { "只有 JSON 完整备份能替换恢复" }
        db.withTransaction {
            val current = snapshot()
            val merged = Ledger.merge(current, plan.snapshot, replace)
            if (replace) {
                getApplication<Application>()
                    .getFileStreamPath("recovery-before-import.json")
                    .writeText(Backup.json(current))
                dao.clearEntries()
                dao.clearCategories()
            }
            dao.putCategories(merged.categories)
            dao.putEntries(merged.entries)
            if (replace) dao.putPreferences(merged.preferences.copy(id = 0))
        }
        recoveryAvailable.value =
            getApplication<Application>().getFileStreamPath("recovery-before-import.json").exists()
        importPlan.value = null
        messageChannel.send("导入完成")
    }

    fun seedDemo() = action {
        db.withTransaction {
            if (dao.entries().isNotEmpty()) return@withTransaction
            dao.putCategories(defaultCategories)
            dao.putPreferences(Preferences())
            val today = LocalDate.now()
            val month = today.withDayOfMonth(1)
            val dayCount = today.dayOfMonth
            val amounts =
                listOf(
                    7800L,
                    12500,
                    6200,
                    18800,
                    9500,
                    24000,
                    11000,
                    15800,
                    10000,
                    10000,
                    18000,
                    8500,
                    14000,
                    21000,
                    20000,
                    6850,
                )
            val rows =
                (0 until dayCount)
                    .map { i ->
                        Entry(
                            id = "demo-day-$i",
                            type = EXPENSE,
                            cents = amounts[i % amounts.size],
                            category = defaultCategories[i % 8].name,
                            date = month.plusDays(i.toLong()).toString(),
                            time = "12:30",
                            note = if (i == dayCount - 1) "午间的一碗好饭" else "日常消费",
                        )
                    }
                    .toMutableList()
            rows +=
                Entry(
                    id = "demo-salary",
                    type = INCOME,
                    cents = 1500000,
                    category = "工资收入",
                    date = today.minusDays(1).toString(),
                    note = "本月工资",
                )
            rows +=
                Entry(
                    id = "demo-shopping",
                    type = EXPENSE,
                    cents = 19900,
                    category = "购物消费",
                    date = today.minusDays(2).toString(),
                    note = "降噪耳机",
                )
            rows +=
                Entry(
                    id = "demo-refund",
                    type = REFUND,
                    cents = 19900,
                    category = "退款",
                    date = today.toString(),
                    note = "耳机退货",
                    linkedExpenseId = "demo-shopping",
                )
            Ledger.validate(rows, defaultCategories)
            dao.putEntries(rows)
        }
    }
}
