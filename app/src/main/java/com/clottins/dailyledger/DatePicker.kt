package com.clottins.dailyledger

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

internal fun calendarDays(month: YearMonth): List<LocalDate?> {
    val leading = month.atDay(1).dayOfWeek.value % 7
    val cells = (leading + month.lengthOfMonth() + 6) / 7 * 7
    return List(cells) { index ->
        val day = index - leading + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
}

private fun calendarDateLabel(date: LocalDate) =
    "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerDatePicker(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selectedText by rememberSaveable(initialDate) { mutableStateOf(initialDate) }
    var monthText by
        rememberSaveable(initialDate) {
            mutableStateOf(YearMonth.from(LocalDate.parse(initialDate)).toString())
        }
    val selected = LocalDate.parse(selectedText)
    val month = YearMonth.parse(monthText)
    val today = remember { LocalDate.now() }
    val days = remember(month) { calendarDays(month) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 24.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9DFD1))
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                .testTag("ledger-date-sheet")
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("选择日期", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                SheetCloseButton("取消日期选择", onDismiss)
            }
            Text(
                calendarDateLabel(selected),
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(top = 8.dp).testTag("selected-calendar-date"),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { monthText = month.minusMonths(1).toString() },
                    enabled = month > YearMonth.of(1, 1),
                    modifier =
                        Modifier.size(40.dp)
                            .background(Color(0xFFEDF1E5), RoundedCornerShape(12.dp)),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "上个月", tint = Muted)
                }
                Text(
                    "${month.year}年${month.monthValue}月",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.testTag("calendar-month"),
                )
                IconButton(
                    onClick = { monthText = month.plusMonths(1).toString() },
                    enabled = month < YearMonth.of(9999, 12),
                    modifier =
                        Modifier.size(40.dp)
                            .background(Color(0xFFEDF1E5), RoundedCornerShape(12.dp)),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "下个月", tint = Muted)
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 11.sp, color = Muted)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().animateContentSize().testTag("calendar-days")) {
                days.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f).height(48.dp))
                            else {
                                val picked = date == selected
                                val color by
                                    animateColorAsState(
                                        if (picked) Green else Color.Transparent,
                                        label = "日期选中",
                                    )
                                Box(
                                    Modifier.weight(1f)
                                        .height(48.dp)
                                        .testTag("date-day-$date")
                                        .semantics { contentDescription = calendarDateLabel(date) }
                                        .selectable(
                                            picked,
                                            role = Role.Button,
                                            onClick = { selectedText = date.toString() },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val outline =
                                        if (date == today && !picked)
                                            Modifier.border(1.dp, Line, CircleShape)
                                        else Modifier
                                    Box(
                                        Modifier.size(40.dp)
                                            .then(outline)
                                            .background(color, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            date.dayOfMonth.toString(),
                                            fontSize = 14.sp,
                                            color = if (picked) Color.White else Green,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                "确定",
                onClick = { onConfirm(selectedText) },
                modifier = Modifier.testTag("confirm-date"),
            )
        }
    }
}
