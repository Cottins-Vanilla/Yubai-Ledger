package com.clottins.dailyledger

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ScreenSummary(
    val totals: Totals = Totals(0, 0, 0, 0),
    val monthly: Totals = Totals(0, 0, 0, 0),
    val entries: List<Entry> = emptyList(),
    val expenseDays: Map<Int, Long> = emptyMap(),
    val incomeDays: Map<Int, Long> = emptyMap(),
    val expenseCats: List<Pair<String, Long>> = emptyList(),
    val incomeCats: List<Pair<String, Long>> = emptyList(),
)

@Composable
fun rememberSummary(state: LedgerState, month: YearMonth): ScreenSummary {
    val summary by
        produceState(ScreenSummary(), state.entries, state.preferences.openingCents, month) {
            value =
                withContext(Dispatchers.Default) {
                    val entries = state.entries.filter { it.date.startsWith(month.toString()) }
                    fun daily(type: String) =
                        entries
                            .filter { it.type == type }
                            .groupBy { LocalDate.parse(it.date).dayOfMonth }
                            .mapValues { (_, v) -> v.sumOf { it.cents } }
                    fun categories(type: String) =
                        entries
                            .filter { it.type == type }
                            .groupBy { it.category }
                            .mapValues { (_, v) -> v.sumOf { it.cents } }
                            .toList()
                            .sortedByDescending { it.second }
                    ScreenSummary(
                        Ledger.totals(state.entries, state.preferences.openingCents),
                        Ledger.totals(entries),
                        entries,
                        daily(EXPENSE),
                        daily(INCOME),
                        categories(EXPENSE),
                        categories(INCOME),
                    )
                }
        }
    return summary
}

@Composable
fun HomeScreen(
    state: LedgerState,
    summary: ScreenSummary,
    date: LocalDate,
    onDate: () -> Unit,
    onSettings: () -> Unit,
    onHide: () -> Unit,
    onEntry: (Entry) -> Unit,
) {
    var filter by remember { mutableIntStateOf(0) }
    var all by remember { mutableStateOf(false) }
    val visible =
        remember(summary.entries, filter, all) {
            summary.entries
                .filter { filter == 0 || it.type == if (filter == 1) EXPENSE else INCOME }
                .let { if (all) it else it.take(30) }
        }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 23.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.clickable(onClick = onDate)) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    SmallLabel(date.format(DateTimeFormatter.ofPattern("EEEE  ⌄", Locale.CHINESE)))
                }
                OutlinedIconButton(onClick = onSettings, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Tune, "设置", modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            BalanceCard(
                summary.totals,
                summary.monthly,
                state.preferences.hideBalance,
                false,
                onHide,
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 27.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (all) "全部账单" else "最近账单", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { all = !all }, contentPadding = PaddingValues(0.dp)) {
                    SmallLabel(if (all) "最近账单" else "全部账单 ›")
                }
            }
            Segment(listOf("全部", "支出", "收入"), filter, { filter = it }, Modifier.width(206.dp))
            Spacer(Modifier.height(18.dp))
        }
        if (visible.isEmpty()) item { EmptyState("还没有账目，点下方「＋」记一笔") }
        visible
            .groupBy { it.date }
            .forEach { (day, entries) ->
                item(key = "day-$day") {
                    val parsed = LocalDate.parse(day)
                    val daily = Ledger.totals(entries)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SmallLabel(
                            when (parsed) {
                                LocalDate.now() -> "今天 · ${parsed.monthValue}月${parsed.dayOfMonth}日"
                                LocalDate.now().minusDays(1) ->
                                    "昨天 · ${parsed.monthValue}月${parsed.dayOfMonth}日"
                                else -> "${parsed.monthValue}月${parsed.dayOfMonth}日"
                            }
                        )
                        SmallLabel(
                            if (daily.expense > 0) "支出 ¥ ${Ledger.money(daily.expense)}"
                            else if (daily.income > 0) "收入 ¥ ${Ledger.money(daily.income)}"
                            else "资金返还"
                        )
                    }
                }
                items(entries, key = { it.id }) { e ->
                    EntryRow(e, state, onEntry)
                    HorizontalDivider(color = Line)
                }
            }
    }
}

@Composable
fun EntryRow(entry: Entry, state: LedgerState, onClick: (Entry) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(entry) }
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            state.categories.find { it.type == entry.type && it.name == entry.category }?.icon
                ?: "other"
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.note.ifBlank { entry.category }, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(5.dp))
            SmallLabel(
                if (entry.type == REFUND) "退款 · 仅影响余额" else "${entry.category} · ${entry.time}"
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val returned = state.refundedByExpense[entry.id] ?: 0L
            val refunded =
                if (entry.type != EXPENSE || returned <= 0) ""
                else if (returned < entry.cents) "（部分退款）" else "（已退款）"
            if (refunded.isNotEmpty()) {
                Text(refunded, fontSize = 9.sp, color = Color(0xFF8DA079))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                "${if(entry.type==EXPENSE)"−"else"+"} ${Ledger.money(entry.cents)}",
                fontSize = 16.sp,
                color = if (entry.type == EXPENSE) Ink else Color(0xFF6A8B51),
            )
        }
    }
}

@Composable
fun StatisticsScreen(
    state: LedgerState,
    summary: ScreenSummary,
    month: YearMonth,
    onMonth: () -> Unit,
) {
    var trendIncome by remember { mutableStateOf(false) }
    var pieIncome by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 18.dp, 24.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("收支统计", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = onMonth,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("${month.year}年${month.monthValue}月 ⌄", fontSize = 11.sp)
                }
            }
        }
        item {
            BalanceCard(summary.totals, summary.monthly, state.preferences.hideBalance, true, {})
        }
        item {
            StatCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("每日趋势", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Segment(
                        listOf("支出", "收入"),
                        if (trendIncome) 1 else 0,
                        { trendIncome = it == 1 },
                        Modifier.width(110.dp),
                    )
                }
                Spacer(Modifier.height(15.dp))
                SmallLabel("金额 / 元")
                Spacer(Modifier.height(9.dp))
                TrendChart(
                    month,
                    if (trendIncome) summary.incomeDays else summary.expenseDays,
                    trendIncome,
                )
            }
        }
        item {
            StatCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("分类占比", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Segment(
                        listOf("支出", "收入"),
                        if (pieIncome) 1 else 0,
                        { pieIncome = it == 1 },
                        Modifier.width(110.dp),
                    )
                }
                Spacer(Modifier.height(7.dp))
                SmallLabel("${month.year}年${month.monthValue}月 · 全月汇总")
                val categories = if (pieIncome) summary.incomeCats else summary.expenseCats
                if (categories.isEmpty()) EmptyState("暂无${if(pieIncome)"收入"else"支出"}数据")
                else {
                    Spacer(Modifier.height(22.dp))
                    DonutChart(categories, pieIncome)
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Line)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SmallLabel("${categories.first().first}占比最高")
                        SmallLabel(
                            String.format(
                                Locale.US,
                                "%.1f%%",
                                categories.first().second * 100.0 / categories.sumOf { it.second },
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFDFEFA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(17.dp), content = content)
    }
}

@Composable
fun TrendChart(month: YearMonth, values: Map<Int, Long>, income: Boolean) {
    val scroll = rememberScrollState()
    var selected by remember(month, income) { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val count = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
    val data = remember(values, count) { (1..count).map { values[it] ?: 0L } }
    val maxValue = (data.maxOrNull() ?: 0L).coerceAtLeast(100L)
    val ceiling =
        ceil(maxValue / 100.0 / if (maxValue > 100000) 5000 else 50).toFloat() *
            (if (maxValue > 100000) 5000 else 50)
    val density = LocalDensity.current
    LaunchedEffect(month, count) {
        snapshotFlow { scroll.maxValue }.first { it > 0 }
        scroll.scrollTo(scroll.maxValue)
    }
    LaunchedEffect(scroll.value) { selected = 0 }
    if (selected > 0)
        SmallLabel(
            "${month.monthValue}月${selected}日  ${if(income)"+"else"−"} ¥${Ledger.money(data[selected-1])}",
            Modifier.padding(bottom = 6.dp),
        )
    Row(Modifier.fillMaxWidth()) {
        Canvas(Modifier.width(31.dp).height(156.dp)) {
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(154, 167, 139)
                    textSize = with(density) { 9.sp.toPx() }
                    textAlign = Paint.Align.RIGHT
                }
            for (i in 0..2) {
                val value = ceiling * (1 - i / 2f)
                drawContext.canvas.nativeCanvas.drawText(
                    if (value >= 1000) "${String.format(Locale.US,"%.1f",value/1000)}k"
                    else value.toInt().toString(),
                    size.width - 5.dp.toPx(),
                    15.dp.toPx() + i * 52.5.dp.toPx() + 3.dp.toPx(),
                    paint,
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val chartWidth = maxOf(maxWidth, (count - 1) * 44.dp + 30.dp)
            Box(Modifier.horizontalScroll(scroll)) {
                Canvas(
                    Modifier.width(chartWidth).height(156.dp).pointerInput(month, income, count) {
                        detectTapGestures { p ->
                            selected =
                                (round((p.x - 15.dp.toPx()) / 44.dp.toPx()).toInt() + 1).coerceIn(
                                    1,
                                    count,
                                )
                        }
                    }
                ) {
                    val top = 15.dp.toPx()
                    val bottom = 120.dp.toPx()
                    val dx = 44.dp.toPx()
                    val pad = 15.dp.toPx()
                    fun y(v: Long) = bottom - (v / 100f) / ceiling * (bottom - top)
                    repeat(3) { i ->
                        drawLine(
                            Line,
                            androidx.compose.ui.geometry.Offset(0f, top + i * (bottom - top) / 2),
                            androidx.compose.ui.geometry.Offset(
                                size.width,
                                top + i * (bottom - top) / 2,
                            ),
                            1.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                        )
                    }
                    val path = Path()
                    data.forEachIndexed { i, v ->
                        if (i == 0) path.moveTo(pad, y(v)) else path.lineTo(pad + i * dx, y(v))
                    }
                    val fill =
                        Path().apply {
                            addPath(path)
                            lineTo(pad + (count - 1) * dx, bottom)
                            lineTo(pad, bottom)
                            close()
                        }
                    drawPath(
                        fill,
                        Brush.verticalGradient(listOf(Color(0x3382A16C), Color.Transparent)),
                    )
                    drawPath(
                        path,
                        Color(0xFF527A4A),
                        style =
                            Stroke(2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    val paint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(151, 164, 138)
                            textSize = with(density) { 9.sp.toPx() }
                            textAlign = Paint.Align.CENTER
                        }
                    data.forEachIndexed { i, v ->
                        val p = androidx.compose.ui.geometry.Offset(pad + i * dx, y(v))
                        drawCircle(
                            Color(0xFFFDFEFA),
                            if (selected == i + 1) 4.dp.toPx() else 2.5.dp.toPx(),
                            p,
                        )
                        drawCircle(
                            Color(0xFF527A4A),
                            if (selected == i + 1) 4.dp.toPx() else 2.5.dp.toPx(),
                            p,
                            style = Stroke(1.5.dp.toPx()),
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${month.monthValue}/${(i+1).toString().padStart(2,'0')}",
                            p.x,
                            143.dp.toPx(),
                            paint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(categories: List<Pair<String, Long>>, income: Boolean) {
    val total = categories.sumOf { it.second }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                var angle = -90f
                categories.forEachIndexed { i, pair ->
                    val sweep = pair.second.toFloat() / total * 360
                    drawArc(
                        ChartColors[i % ChartColors.size],
                        angle,
                        sweep,
                        false,
                        style = Stroke(16.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(9.dp.toPx(), 9.dp.toPx()),
                        size =
                            androidx.compose.ui.geometry.Size(
                                size.width - 18.dp.toPx(),
                                size.height - 18.dp.toPx(),
                            ),
                    )
                    angle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SmallLabel(if (income) "总收入" else "总支出")
                Text(Ledger.money(total), fontSize = 17.sp, color = Green, maxLines = 1)
                Text("人民币 / 元", fontSize = 8.sp, color = Muted)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            categories.forEachIndexed { i, pair ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ChartColors[i % ChartColors.size])
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        pair.first,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = Color(0xFF758569),
                        maxLines = 1,
                    )
                    Text(Ledger.money(pair.second), fontSize = 10.sp, color = Green)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${(pair.second*100.0/total).roundToInt()}%",
                        fontSize = 9.sp,
                        color = Muted,
                    )
                }
            }
        }
    }
}
