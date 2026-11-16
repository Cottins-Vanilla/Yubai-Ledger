package com.clottins.dailyledger

import java.time.Instant
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object Backup {
    const val MAX_BYTES = 10 * 1024 * 1024

    private fun JSONObject.strictLong(key: String): Long =
        get(key).let { value ->
            require(value is Number) { "$key 必须是整数" }
            value.toString().toLongOrNull() ?: error("$key 必须是整数")
        }

    fun json(snapshot: LedgerSnapshot): String =
        JSONObject()
            .apply {
                put("schemaVersion", 1)
                put("currency", "CNY")
                put("exportedAt", Instant.now().toString())
                put(
                    "preferences",
                    JSONObject()
                        .put("openingCents", snapshot.preferences.openingCents)
                        .put("hideBalance", snapshot.preferences.hideBalance),
                )
                put(
                    "categories",
                    JSONArray().apply {
                        snapshot.categories.forEach { c ->
                            put(
                                JSONObject()
                                    .put("type", c.type)
                                    .put("name", c.name)
                                    .put("icon", c.icon)
                            )
                        }
                    },
                )
                put(
                    "entries",
                    JSONArray().apply {
                        snapshot.entries.forEach { e ->
                            put(
                                JSONObject().apply {
                                    put("id", e.id)
                                    put("type", e.type)
                                    put("amountCents", e.cents)
                                    put("category", e.category)
                                    put("date", e.date)
                                    put("time", e.time)
                                    put("note", e.note)
                                    put("linkedExpenseId", e.linkedExpenseId ?: JSONObject.NULL)
                                }
                            )
                        }
                    },
                )
            }
            .toString(2)

    fun parseJson(text: String): LedgerSnapshot {
        val root = JSONObject(text.removePrefix("\uFEFF"))
        require(root.strictLong("schemaVersion") == 1L && root.getString("currency") == "CNY") {
            "不支持的备份版本或币种"
        }
        val settings = root.getJSONObject("preferences")
        val opening = settings.strictLong("openingCents")
        require(opening in -MAX_CENTS..MAX_CENTS) { "期初余额超出范围" }
        val cats =
            root.getJSONArray("categories").let { array ->
                require(array.length() <= 500) { "分类数量过多" }
                (0 until array.length()).map { i ->
                    val c = array.getJSONObject(i)
                    category(c.getString("type"), c.getString("name"), c.optString("icon", "other"))
                }
            }
        require(
            cats.all {
                it.type in setOf(INCOME, EXPENSE, REFUND) &&
                    it.name.isNotBlank() &&
                    it.name.length <= 40
            }
        ) {
            "分类无效"
        }
        val entries =
            root.getJSONArray("entries").let { array ->
                require(array.length() <= 20_000) { "账目数量超限" }
                (0 until array.length()).map { i ->
                    val e = array.getJSONObject(i)
                    Entry(
                        e.getString("id"),
                        e.getString("type"),
                        e.strictLong("amountCents"),
                        e.getString("category"),
                        e.getString("date"),
                        e.optString("time", "00:00"),
                        e.optString("note", ""),
                        if (e.isNull("linkedExpenseId")) null
                        else e.getString("linkedExpenseId").ifBlank { null },
                    )
                }
            }
        return LedgerSnapshot(
            entries,
            (defaultCategories + cats).distinctBy { it.key },
            Preferences(
                openingCents = opening,
                hideBalance = settings.optBoolean("hideBalance", false),
            ),
        )
    }

    private fun escapeText(s: String): String =
        if (s.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r', '\n', '\'')) "'$s" else s

    private fun unescapeText(s: String): String =
        if (
            s.length > 1 &&
                s[0] == '\'' &&
                s[1] in listOf('=', '+', '-', '@', '\t', '\r', '\n', '\'')
        )
            s.substring(1)
        else s

    private fun quote(s: String) = "\"${s.replace("\"","\"\"")}\""

    fun csv(entries: List<Entry>): String {
        val rows =
            mutableListOf(
                listOf(
                    "id",
                    "date",
                    "type",
                    "category",
                    "amount",
                    "note",
                    "linkedExpenseId",
                    "time",
                    "textEncoding",
                )
            )
        entries.forEach { e ->
            rows +=
                listOf(
                    e.id,
                    e.date,
                    e.type,
                    escapeText(e.category),
                    Ledger.rawMoney(e.cents),
                    escapeText(e.note),
                    e.linkedExpenseId.orEmpty(),
                    e.time,
                    "apostrophe-v1",
                )
        }
        return "\uFEFF" +
            rows.joinToString("\r\n") { row -> row.joinToString(",") { quote(it) } } +
            "\r\n"
    }

    fun parseCsv(text: String): LedgerSnapshot {
        val rows = parseRows(text.removePrefix("\uFEFF"))
        require(rows.isNotEmpty()) { "CSV 文件为空" }
        val header = rows.first()
        require(header.distinct().size == header.size) { "CSV 列名重复" }
        require(listOf("date", "type", "category", "amount", "note").all { it in header }) {
            "缺少 CSV 必要列"
        }
        val entries =
            rows
                .drop(1)
                .filterNot { it.all(String::isEmpty) }
                .mapIndexed { index, row ->
                    require(row.size == header.size) { "CSV 第 ${index+2} 行列数不符" }
                    fun get(key: String) = header.indexOf(key).let { if (it < 0) "" else row[it] }
                    fun decoded(key: String) =
                        if (get("textEncoding") == "apostrophe-v1") unescapeText(get(key))
                        else get(key)
                    Entry(
                        get("id").ifBlank { UUID.randomUUID().toString() },
                        get("type"),
                        Ledger.parseMoney(get("amount")),
                        decoded("category"),
                        get("date"),
                        get("time").ifBlank { "00:00" },
                        decoded("note"),
                        get("linkedExpenseId").ifBlank { null },
                    )
                }
        require(entries.size <= 20_000) { "账目数量超限" }
        val cats =
            (defaultCategories + entries.map { category(it.type, it.category, "other") })
                .distinctBy { it.key }
        return LedgerSnapshot(entries, cats, Preferences())
    }

    fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var closed = false
        var i = 0
        fun finishCell() {
            row += cell.toString()
            cell.setLength(0)
            closed = false
        }
        fun finishRow() {
            finishCell()
            rows += row
            row = mutableListOf()
        }
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        quoted = false
                        closed = true
                    }
                } else cell.append(c)
            } else
                when (c) {
                    '"' -> {
                        require(cell.isEmpty() && !closed) { "CSV 引号格式错误" }
                        quoted = true
                    }
                    ',' -> finishCell()
                    '\r',
                    '\n' -> {
                        finishRow()
                        if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    }
                    else -> {
                        require(!closed) { "CSV 引号后有多余字符" }
                        cell.append(c)
                    }
                }
            i++
        }
        require(!quoted) { "CSV 引号未闭合" }
        if (cell.isNotEmpty() || row.isNotEmpty() || closed) finishRow()
        return rows
    }
}
