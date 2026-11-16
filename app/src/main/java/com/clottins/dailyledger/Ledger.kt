package com.clottins.dailyledger

import androidx.room.*
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

const val INCOME = "income"
const val EXPENSE = "expense"
const val REFUND = "refund"
const val MAX_CENTS = 99_999_999_999L

@Entity(tableName = "entries", indices = [Index("date"), Index("linkedExpenseId")])
data class Entry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,
    val cents: Long,
    val category: String,
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().toString().take(5),
    val note: String = "",
    val linkedExpenseId: String? = null,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val key: String,
    val type: String,
    val name: String,
    val icon: String = "other",
)

fun category(type: String, name: String, icon: String) = Category("$type:$name", type, name, icon)

val defaultCategories =
    listOf(
        category(EXPENSE, "餐饮美食", "food"),
        category(EXPENSE, "交通出行", "train"),
        category(EXPENSE, "购物消费", "bag"),
        category(EXPENSE, "生活日用", "daily"),
        category(EXPENSE, "居家住房", "home"),
        category(EXPENSE, "休闲娱乐", "game"),
        category(EXPENSE, "医疗健康", "medical"),
        category(EXPENSE, "其他支出", "other"),
        category(INCOME, "工资收入", "wallet"),
        category(INCOME, "奖金收入", "gift"),
        category(INCOME, "生活费", "home"),
        category(INCOME, "兼职收入", "work"),
        category(INCOME, "红包收入", "redpacket"),
        category(REFUND, "退款", "refund"),
        category(INCOME, "礼金收入", "gift"),
        category(INCOME, "其他收入", "other"),
    )

@Entity(tableName = "preferences")
data class Preferences(
    @PrimaryKey val id: Int = 0,
    val openingCents: Long = 0,
    val hideBalance: Boolean = false,
)

data class Totals(val income: Long, val expense: Long, val refund: Long, val balance: Long)

data class LedgerSnapshot(
    val entries: List<Entry>,
    val categories: List<Category>,
    val preferences: Preferences,
)

object Ledger {
    fun parseMoney(raw: String, allowNegative: Boolean = false): Long {
        val pattern = if (allowNegative) "-?\\d{1,9}(\\.\\d{1,2})?" else "\\d{1,9}(\\.\\d{1,2})?"
        require(raw.matches(Regex(pattern))) { "金额最多 9 位整数、2 位小数" }
        val negative = raw.startsWith('-')
        val parts = raw.removePrefix("-").split('.')
        val cents = parts[0].toLong() * 100 + parts.getOrElse(1) { "" }.padEnd(2, '0').toLong()
        return if (negative) -cents else cents
    }

    fun money(cents: Long): String =
        String.format(
            Locale.US,
            "%,d.%02d",
            kotlin.math.abs(cents) / 100,
            kotlin.math.abs(cents) % 100,
        )

    fun rawMoney(cents: Long): String =
        "${if(cents<0)"-"else""}${kotlin.math.abs(cents)/100}.${(kotlin.math.abs(cents)%100).toString().padStart(2,'0')}"

    fun totals(entries: List<Entry>, opening: Long = 0): Totals {
        fun sum(type: String) =
            entries.filter { it.type == type }.fold(0L) { a, e -> Math.addExact(a, e.cents) }
        val income = sum(INCOME)
        val expense = sum(EXPENSE)
        val refund = sum(REFUND)
        return Totals(
            income,
            expense,
            refund,
            Math.addExact(Math.subtractExact(Math.addExact(opening, income), expense), refund),
        )
    }

    fun refunded(entries: List<Entry>, id: String, excludeId: String? = null): Long =
        entries
            .filter { it.type == REFUND && it.linkedExpenseId == id && it.id != excludeId }
            .fold(0L) { a, e -> Math.addExact(a, e.cents) }

    fun refundLabel(entries: List<Entry>, entry: Entry): String {
        if (entry.type != EXPENSE) return ""
        val amount = refunded(entries, entry.id)
        return when {
            amount <= 0 -> ""
            amount < entry.cents -> "（部分退款）"
            else -> "（已退款）"
        }
    }

    fun validate(entries: List<Entry>, categories: List<Category>) {
        require(entries.size <= 20_000) { "最多支持一次处理 20,000 笔账目" }
        require(entries.map { it.id }.distinct().size == entries.size) { "存在重复账目 ID" }
        val byId = entries.associateBy { it.id }
        entries.forEach { e ->
            require(e.id.matches(Regex("[A-Za-z0-9_-]{1,100}"))) { "账目 ID 无效" }
            require(e.type in setOf(INCOME, EXPENSE, REFUND)) { "未知账目类型" }
            require(e.cents in 1..MAX_CENTS) { "金额必须大于 0 且在范围内" }
            require(e.category.isNotBlank() && e.category.length <= 40) { "分类名称无效" }
            require(categories.any { it.type == e.type && it.name == e.category }) {
                "缺少分类：${e.category}"
            }
            require(e.note.length <= 1000) { "备注不能超过 1000 字符" }
            LocalDate.parse(e.date)
            LocalTime.parse(e.time)
            if (e.type != REFUND) require(e.linkedExpenseId == null) { "只有退款能关联原账单" }
            e.linkedExpenseId?.let { linked ->
                val original = byId[linked] ?: error("退款缺少原账单：$linked，请先补充原记录")
                require(original.type == EXPENSE && original.id != e.id) { "退款只能关联原支出" }
                require(e.date >= original.date) { "退款日期不能早于原支出日期" }
            }
        }
        val refunds =
            entries
                .filter { it.type == REFUND && it.linkedExpenseId != null }
                .groupBy { it.linkedExpenseId }
                .mapValues { (_, rows) ->
                    rows.fold(0L) { sum, row -> Math.addExact(sum, row.cents) }
                }
        entries
            .filter { it.type == EXPENSE }
            .forEach {
                require((refunds[it.id] ?: 0L) <= it.cents) {
                    "${it.note.ifBlank { it.category }} 的累计退款超过原金额"
                }
            }
        totals(entries)
    }

    fun merge(current: LedgerSnapshot, imported: LedgerSnapshot, replace: Boolean): LedgerSnapshot {
        if (replace) {
            validate(imported.entries, imported.categories)
            return imported
        }
        val entries = current.entries.associateBy { it.id }.toMutableMap()
        imported.entries.forEach { e ->
            val existing = entries[e.id]
            require(existing == null || existing == e) { "同一 ID 内容冲突：${e.id}，未导入任何账目" }
            entries[e.id] = e
        }
        val categories = (current.categories + imported.categories).distinctBy { it.key }
        val result = LedgerSnapshot(entries.values.toList(), categories, current.preferences)
        validate(result.entries, result.categories)
        return result
    }
}
