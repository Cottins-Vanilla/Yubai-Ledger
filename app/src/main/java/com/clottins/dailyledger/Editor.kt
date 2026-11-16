package com.clottins.dailyledger

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    entry: Entry?,
    state: LedgerState,
    busy: Boolean,
    onBack: () -> Unit,
    onSave: (Entry) -> Unit,
) {
    val context = LocalContext.current
    var income by
        rememberSaveable(entry?.id) { mutableStateOf(entry?.type != EXPENSE && entry != null) }
    var raw by
        rememberSaveable(entry?.id) {
            mutableStateOf(entry?.let { Ledger.rawMoney(it.cents) } ?: "0")
        }
    var expenseCategory by
        rememberSaveable(entry?.id) {
            mutableStateOf(if (entry?.type == EXPENSE) entry.category else "餐饮美食")
        }
    var incomeCategory by
        rememberSaveable(entry?.id) {
            mutableStateOf(if (entry?.type != EXPENSE && entry != null) entry.category else "工资收入")
        }
    var incomeCategoryType by rememberSaveable(entry?.id) {
        mutableStateOf(if(entry!=null && entry.type!=EXPENSE) entry.type else INCOME)
    }
    var date by
        rememberSaveable(entry?.id) { mutableStateOf(entry?.date ?: LocalDate.now().toString()) }
    var note by rememberSaveable(entry?.id) { mutableStateOf(entry?.note ?: "") }
    var linked by rememberSaveable(entry?.id) { mutableStateOf(entry?.linkedExpenseId) }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var leave by remember { mutableStateOf(false) }
    val categoryName = if (income) incomeCategory else expenseCategory
    val type = if (income) incomeCategoryType else EXPENSE
    val categoryList =
        state.categories.filter {
            if (income) it.type in setOf(INCOME, REFUND) else it.type == EXPENSE
        }
    val amount = runCatching { Ledger.parseMoney(raw.removeSuffix(".")) }.getOrDefault(0)
    val linkedEntry = state.entries.find { it.id == linked }
    val dirty =
        raw != (entry?.let { Ledger.rawMoney(it.cents) } ?: "0") ||
            note != (entry?.note ?: "") ||
            date != (entry?.date ?: LocalDate.now().toString()) ||
            type != (entry?.type ?: EXPENSE) ||
            categoryName != (entry?.category ?: "餐饮美食") ||
            linked != entry?.linkedExpenseId
    fun back() {
        if (dirty) leave = true else onBack()
    }
    BackHandler { back() }
    fun key(value: String) {
        error = null
        raw =
            when (value) {
                "back" -> raw.dropLast(1).ifEmpty { "0" }
                "." -> if ('.' in raw) raw else "$raw."
                else -> {
                    val parts = raw.split('.')
                    if (parts.size > 1 && parts[1].length >= 2) raw
                    else if (parts.size == 1 && parts[0].length >= 9) raw
                    else if (raw == "0") value else raw + value
                }
            }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(24.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedIconButton(onClick = { back() }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
            }
            Text(
                if (entry == null) "记一笔" else "编辑账目",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(48.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            Segment(
                listOf("支出", "收入"),
                if (income) 1 else 0,
                {
                    income = it == 1
                    error = null
                },
                Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SmallLabel(
                    when (type) {
                        EXPENSE -> "支出金额"
                        REFUND -> "退款金额"
                        else -> "收入金额"
                    }
                )
                SmallLabel("人民币")
            }
            Row(
                Modifier.padding(top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("¥", fontSize = 24.sp, color = Color(0xFF789467))
                Spacer(Modifier.width(9.dp))
                Text(
                    raw.split('.').let { parts ->
                        val int = parts[0].reversed().chunked(3).joinToString(",").reversed()
                        int + if (parts.size == 1) ".00" else ".${parts[1]}"
                    },
                    fontSize = 43.sp,
                    color = Green,
                    maxLines = 1,
                )
            }
            if (type == REFUND) {
                SmallLabel("仅影响余额，不计入收入和支出")
                Spacer(Modifier.height(6.dp))
            }
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SmallLabel("选择分类")
                SmallLabel(categoryName)
            }
            categoryList.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { cat ->
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (income) {
                                        incomeCategory = cat.name
                                        incomeCategoryType = cat.type
                                    } else expenseCategory = cat.name
                                    error = null
                                }
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            IconTile(cat.icon, Modifier.size(40.dp), selected = categoryName == cat.name && cat.type == type)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                cat.name
                                    .removeSuffix("美食")
                                    .removeSuffix("出行")
                                    .removeSuffix("消费")
                                    .removeSuffix("日用")
                                    .removeSuffix("住房")
                                    .removeSuffix("娱乐")
                                    .removeSuffix("健康")
                                    .removeSuffix("收入")
                                    .ifEmpty { cat.name }
                                    .let { if (it.length > 4) it.take(4) else it },
                                fontSize = 10.sp,
                                  color = if (cat.name == categoryName && cat.type == type) Green else Muted,
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth()
                    .clickable {
                        val parsed = LocalDate.parse(date)
                        DatePickerDialog(
                                context,
                                { _, y, m, d -> date = LocalDate.of(y, m + 1, d).toString() },
                                parsed.year,
                                parsed.monthValue - 1,
                                parsed.dayOfMonth,
                            )
                            .show()
                    }
                    .padding(vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    null,
                    tint = Muted,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(10.dp))
                SmallLabel("日期")
                Spacer(Modifier.weight(1f))
                Text(date.replace('-', '/'), fontSize = 11.sp, color = Green)
            }
            HorizontalDivider(color = Line)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EditNote, null, tint = Muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                SmallLabel("备注")
                TextField(
                    note,
                    { if (it.length <= 1000) note = it },
                    Modifier.weight(1f),
                    placeholder = { Text("写点什么，方便日后查看", fontSize = 10.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Green),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                )
            }
            HorizontalDivider(color = Line)
            if (type == REFUND) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Link, null, tint = Muted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(10.dp))
                    SmallLabel("关联账单")
                    TextButton(
                        onClick = {
                            query = ""
                            picker = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            linkedEntry?.let {
                                "${it.note.ifBlank{it.category}} · ¥${Ledger.money(it.cents)}"
                            } ?: "选择原支出账单",
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    if (linked != null)
                        IconButton(onClick = { linked = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Close, "取消关联", modifier = Modifier.size(16.dp))
                        }
                }
                HorizontalDivider(color = Line)
            }
            error?.let {
                Text(
                    it,
                    color = Color(0xFFA57961),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
        Column(Modifier.padding(24.dp, 12.dp, 24.dp, 18.dp)) {
            listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "back"),
                )
                .forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { v ->
                            Box(
                                Modifier.weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color(0xFFEFF2E9))
                                    .clickable { key(v) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (v == "back")
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Backspace,
                                        "删除一位",
                                        tint = Green,
                                        modifier = Modifier.size(21.dp),
                                    )
                                else Text(v, fontSize = 23.sp, color = Green)
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
            Spacer(Modifier.height(7.dp))
            PrimaryButton(
                if (entry == null) "✓  保存账目" else "✓  保存修改",
                onClick = {
                    val candidate =
                        Entry(
                            id = entry?.id ?: java.util.UUID.randomUUID().toString(),
                            type = type,
                            cents = amount,
                            category = categoryName,
                            date = date,
                            time = entry?.time ?: java.time.LocalTime.now().toString().take(5),
                            note = note,
                            linkedExpenseId = if (type == REFUND) linked else null,
                        )
                    onSave(candidate)
                },
                enabled = amount > 0 && !busy,
            )
        }
    }
    if (leave)
        AlertDialog(
            onDismissRequest = { leave = false },
            title = { Text("放弃未保存的修改？") },
            text = { Text("返回后，本次填写的内容不会保存。") },
            confirmButton = { TextButton(onClick = onBack) { Text("放弃修改") } },
            dismissButton = { TextButton(onClick = { leave = false }) { Text("继续编辑") } },
        )
    if (picker)
        ModalBottomSheet(
            onDismissRequest = { picker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Bg,
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp, 0.dp, 24.dp, 24.dp)) {
                Text("关联原账单", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    query,
                    { query = it },
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    placeholder = { Text("搜索分类或备注", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                )
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    val eligible =
                        state.entries.filter {
                            it.type == EXPENSE &&
                                (it.note + it.category + it.date).contains(query, true)
                        }
                    if (eligible.isEmpty()) EmptyState("没有匹配的支出账单")
                    eligible.forEach { original ->
                        val remaining =
                            original.cents - Ledger.refunded(state.entries, original.id, entry?.id)
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = remaining > 0) {
                                    linked = original.id
                                    picker = false
                                    error = null
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(original.note.ifBlank { original.category }, fontSize = 12.sp)
                                Spacer(Modifier.height(6.dp))
                                SmallLabel("${original.date} · ${original.category}")
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("− ${Ledger.money(original.cents)}", fontSize = 13.sp)
                                SmallLabel("可退 ¥${Ledger.money(remaining.coerceAtLeast(0))}")
                            }
                        }
                        HorizontalDivider(color = Line)
                    }
                }
                Spacer(Modifier.height(14.dp))
                SmallLabel("可关联一笔支出；支持部分退款")
            }
        }
}
