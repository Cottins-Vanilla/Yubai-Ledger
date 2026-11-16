package com.clottins.dailyledger

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    private val model: LedgerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (BuildConfig.DEBUG && intent.getBooleanExtra("demo", false)) model.seedDemo()
        setContent { LedgerTheme { LedgerApp(model) } }
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        val rates =
            windowManager.defaultDisplay.supportedModes
                .map { it.refreshRate }
                .filter { it <= 144.1f }
        rates.maxOrNull()?.let { rate ->
            val attrs = window.attributes
            attrs.preferredRefreshRate = rate
            window.attributes = attrs
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp(vm: LedgerViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val plan by vm.importPlan.collectAsStateWithLifecycle()
    val recovery by vm.recoveryAvailable.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("home") }
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(dateText)
    val month = YearMonth.from(date)
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(false) }
    var openingDialog by remember { mutableStateOf(false) }
    var csvDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var csvStart by remember { mutableStateOf<LocalDate?>(null) }
    var csvEnd by remember { mutableStateOf<LocalDate?>(null) }
    var exportRecovery by remember { mutableStateOf(false) }
    val summary = rememberSummary(state, month)
    val createJson =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) vm.export(uri, true, recovery = exportRecovery)
        }
    val createCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            if (uri != null) vm.export(uri, false, csvStart, csvEnd)
        }
    val openImport =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.inspectImport(uri)
        }
    LaunchedEffect(vm) {
        vm.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    fun chooseDate() {
        DatePickerDialog(
                context,
                { _, y, m, d -> dateText = LocalDate.of(y, m + 1, d).toString() },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            )
            .show()
    }
    if (page != "edit") BackHandler(enabled = page != "home") { page = "home" }
    Surface(Modifier.fillMaxSize(), color = Bg) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                if (!state.ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.weight(1f),
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                        label = "页面切换",
                    ) { current ->
                        when (current) {
                            "edit" ->
                                EditorScreen(
                                    state.entries.find { it.id == editId },
                                    state,
                                    busy,
                                    onBack = {
                                        page = "home"
                                        if (editId != null) detailId = editId
                                    },
                                    onSave = { entry ->
                                        vm.save(entry) {
                                            dateText = entry.date
                                            page = "home"
                                            detailId = null
                                            editId = null
                                        }
                                    },
                                )
                            "stats" -> StatisticsScreen(state, summary, month, { chooseDate() })
                            else ->
                                HomeScreen(
                                    state,
                                    summary,
                                    date,
                                    { chooseDate() },
                                    { settings = true },
                                    { vm.toggleBalance() },
                                    { detailId = it.id },
                                )
                        }
                    }
                    if (page != "edit") CompactBottomBar(page) { page = it }
                }
            }
            if (state.ready && page != "edit") {
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .offset(y = (-12).dp)
                        .size(64.dp)
                        .shadow(4.dp, CircleShape, clip = false)
                        .background(Bg, CircleShape)
                        .padding(5.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            editId = null
                            page = "edit"
                        },
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = Green,
                                contentColor = Color.White,
                            ),
                    ) {
                        Icon(Icons.Outlined.Add, "记一笔", modifier = Modifier.size(27.dp))
                    }
                }
            }
        }
    }
    val detail = state.entries.find { it.id == detailId }
    if (detail != null && page != "edit")
        ModalBottomSheet(
            onDismissRequest = { detailId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Bg,
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp, 0.dp, 24.dp, 28.dp)
            ) {
                Text("账单详情", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        SmallLabel(
                            when (detail.type) {
                                EXPENSE -> "支出金额"
                                REFUND -> "退款金额"
                                else -> "收入金额"
                            }
                        )
                        Text(
                            "${if(detail.type==EXPENSE)"−"else"+"} ¥${Ledger.money(detail.cents)}",
                            fontSize = 35.sp,
                            color = Green,
                            modifier = Modifier.padding(vertical = 7.dp),
                        )
                        val refunded = Ledger.refundLabel(state.entries, detail)
                        if (refunded.isNotEmpty()) SmallLabel(refunded)
                        SmallLabel("${detail.date} ${detail.time}")
                    }
                    IconTile(
                        state.categories
                            .find { it.type == detail.type && it.name == detail.category }
                            ?.icon ?: "other"
                    )
                }
                HorizontalDivider(color = Line)
                InfoRow("分类", detail.category)
                InfoRow("备注", detail.note.ifBlank { "未填写" })
                detail.linkedExpenseId?.let { id ->
                    InfoRow(
                        "关联账单",
                        state.entries
                            .find { it.id == id }
                            ?.let { "${it.note.ifBlank{it.category}} · ¥${Ledger.money(it.cents)}" }
                            ?: "原账单不存在",
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = { deleteDialog = true },
                        modifier = Modifier.width(86.dp).height(49.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFFF0EAE2),
                                contentColor = Color(0xFFA38A73),
                            ),
                    ) {
                        Text("删除", fontSize = 12.sp)
                    }
                    PrimaryButton(
                        "编辑账目",
                        onClick = {
                            editId = detail.id
                            detailId = null
                            page = "edit"
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                    )
                }
            }
        }
    if (deleteDialog && detail != null)
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("删除这笔账目？") },
            text = {
                Text(
                    if (state.entries.any { it.linkedExpenseId == detail.id })
                        "这笔支出有关联退款。请先解除关联或删除对应退款，防止账目失去来源。"
                    else if (detail.type == REFUND) "删除退款后，余额及原账单退款状态会同步更新。" else "删除后，余额与统计会同步更新。"
                )
            },
            confirmButton = {
                if (state.entries.none { it.linkedExpenseId == detail.id })
                    TextButton(
                        onClick = {
                            deleteDialog = false
                            vm.delete(detail) { detailId = null }
                        },
                        enabled = !busy,
                    ) {
                        Text("删除账目")
                    }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("取消") } },
        )
    if (settings)
        ModalBottomSheet(
            onDismissRequest = { settings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Bg,
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp, 0.dp, 24.dp, 24.dp)) {
                Text("设置", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(22.dp))
                SmallLabel("备份与导出")
                SettingsRow("完整备份", "JSON", Icons.Outlined.SaveAlt, !busy) {
                    settings = false
                    exportRecovery = false
                    createJson.launch("Yubai-Ledger-${LocalDate.now()}.json")
                }
                SettingsRow("导出账目", "CSV", Icons.Outlined.TableChart, !busy) {
                    settings = false
                    csvDialog = true
                }
                SettingsRow("导入账目", "JSON / CSV", Icons.Outlined.FileOpen, !busy) {
                    settings = false
                    openImport.launch(arrayOf("*/*"))
                }
                if (recovery)
                    SettingsRow("恢复前备份", "JSON", Icons.Outlined.History, !busy) {
                        settings = false
                        exportRecovery = true
                        createJson.launch("Yubai-Ledger-恢复前.json")
                    }
                Spacer(Modifier.height(14.dp))
                SmallLabel("账本")
                SettingsRow(
                    "期初余额",
                    "${if(state.preferences.openingCents<0)"−"else""}¥${Ledger.money(state.preferences.openingCents)}",
                    Icons.Outlined.AccountBalanceWallet,
                    !busy,
                ) {
                    settings = false
                    openingDialog = true
                }
                Spacer(Modifier.height(14.dp))
                SmallLabel("账目保存在本机。卸载前请导出备份。")
            }
        }
    if (openingDialog)
        OpeningDialog(
            state.preferences.openingCents,
            onDismiss = { openingDialog = false },
            onSave = {
                vm.setOpening(it)
                openingDialog = false
            },
        )
    if (csvDialog)
        CsvDialog(
            month,
            onDismiss = { csvDialog = false },
            onExport = { start, end ->
                csvStart = start
                csvEnd = end
                csvDialog = false
                createCsv.launch("Yubai-Ledger-${LocalDate.now()}.csv")
            },
        )
    plan?.let { preview ->
        ImportDialog(
            preview,
            busy,
            onDismiss = { if (!busy) vm.importPlan.value = null },
            onApply = { vm.applyImport(it) },
        )
    }
}

@Composable
private fun CompactBottomBar(page: String, onPage: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().height(60.dp).background(Bg).testTag("ledger-bottom-bar")) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            val gap = 40.dp.toPx()
            drawLine(
                Line,
                androidx.compose.ui.geometry.Offset.Zero,
                androidx.compose.ui.geometry.Offset(size.width / 2 - gap, 0f),
                1.dp.toPx(),
            )
            drawLine(
                Line,
                androidx.compose.ui.geometry.Offset(size.width / 2 + gap, 0f),
                androidx.compose.ui.geometry.Offset(size.width, 0f),
                1.dp.toPx(),
            )
        }
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationItem("明细", Icons.Outlined.Home, page == "home") { onPage("home") }
            Spacer(Modifier.width(64.dp))
            NavigationItem("统计", Icons.AutoMirrored.Outlined.ShowChart, page == "stats") {
                onPage("stats")
            }
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(96.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            label,
            tint = if (selected) Green else Color(0xFF99998F),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            color = if (selected) Green else Color(0xFF99998F),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SmallLabel(label)
        Text(
            value,
            fontSize = 11.sp,
            color = Green,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    format: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color(0xFF73866A), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(11.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        SmallLabel(format)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(15.dp))
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun OpeningDialog(value: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var text by remember { mutableStateOf(Ledger.rawMoney(value)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("期初余额") },
        text = {
            Column {
                Text("只调整余额，不计入收入和支出。可填写负数。", fontSize = 12.sp, color = Muted)
                OutlinedTextField(
                    text,
                    {
                        text = it
                        error = null
                    },
                    label = { Text("金额 / 元") },
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runCatching { Ledger.parseMoney(text, true) }
                        .onSuccess(onSave)
                        .onFailure { error = it.message }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun CsvDialog(
    month: YearMonth,
    onDismiss: () -> Unit,
    onExport: (LocalDate?, LocalDate?) -> Unit,
) {
    val context = LocalContext.current
    var all by remember { mutableStateOf(true) }
    var start by remember { mutableStateOf(month.atDay(1)) }
    var end by remember { mutableStateOf(month.atEndOfMonth()) }
    var error by remember { mutableStateOf(false) }
    fun pick(current: LocalDate, set: (LocalDate) -> Unit) {
        DatePickerDialog(
                context,
                { _, y, m, d -> set(LocalDate.of(y, m + 1, d)) },
                current.year,
                current.monthValue - 1,
                current.dayOfMonth,
            )
            .show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出 CSV") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(all, { all = it })
                    Text("全部账目", fontSize = 13.sp)
                }
                if (!all) {
                    TextButton(
                        onClick = {
                            pick(start) {
                                start = it
                                error = false
                            }
                        }
                    ) {
                        Text("开始：$start")
                    }
                    TextButton(
                        onClick = {
                            pick(end) {
                                end = it
                                error = false
                            }
                        }
                    ) {
                        Text("结束：$end")
                    }
                }
                SmallLabel("包含退款明细；CSV 不包含期初余额及完整设置。关联的原账单若不在所选日期内，需要一起保留用于导入。")
                if (error)
                    Text("结束日期不能早于开始日期", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!all && end < start) error = true
                    else onExport(if (all) null else start, if (all) null else end)
                }
            ) {
                Text("选择保存位置")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ImportDialog(
    plan: ImportPlan,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApply: (Boolean) -> Unit,
) {
    var replace by remember(plan) { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    val error = if (replace) plan.replaceError else plan.mergeError
    val totals = remember(plan) { Ledger.totals(plan.snapshot.entries) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("导入预览") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${plan.snapshot.entries.size} 笔账目",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                SmallLabel(
                    "收入 ¥${Ledger.money(totals.income)}\n支出 ¥${Ledger.money(totals.expense)}\n退款 ¥${Ledger.money(totals.refund)}"
                )
                Spacer(Modifier.height(12.dp))
                SmallLabel(
                    "相同 ID 且内容一致：${plan.duplicateCount} 笔，将跳过\nID 内容冲突：${plan.conflictCount} 笔"
                )
                if (plan.possibleDuplicates > 0)
                    Text(
                        "${plan.possibleDuplicates} 笔内容相似但 ID 不同，将作为独立记录导入，请确认是否重复。",
                        fontSize = 11.sp,
                        color = Color(0xFFA57961),
                    )
                if (plan.json)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(replace, { replace = it }, enabled = !busy)
                        Text("完整恢复，替换本地账本", fontSize = 12.sp)
                    }
                else SmallLabel("CSV 合并账目，保留当前期初余额和设置。")
                if (error != null)
                    Text(error, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                if (replace) SmallLabel("将先保留一份恢复前备份，可从设置导出。")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (replace) confirmReplace = true else onApply(false) },
                enabled = error == null && !busy,
            ) {
                Text(if (busy) "处理中…" else "确认导入")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
    if (confirmReplace)
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("替换当前账本？") },
            text = { Text("现有账目、期初余额和设置会被这份 JSON 备份替换。恢复前备份将保存在应用内，卸载后也会删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplace = false
                        onApply(true)
                    }
                ) {
                    Text("替换并恢复")
                }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("返回预览") } },
        )
}
