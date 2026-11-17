package com.clottins.dailyledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm")

@Stable
private class ClockWheel(
    val list: LazyListState,
    val count: Int,
    private val initialIndex: Int,
    private val centerOffset: Int,
) {
    val selectedIndex: Int
        get() {
            val layout = list.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            return layout.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2f - center) }
                ?.index ?: initialIndex
        }

    val value: Int
        get() = selectedIndex % count

    suspend fun center(index: Int) {
        // 2 * 68 + 34 - 18 = 152dp, the center of the 304dp viewport.
        list.animateScrollToItem((index - 2).coerceAtLeast(0), centerOffset)
    }
}

@Composable
private fun rememberClockWheel(count: Int, initial: Int): ClockWheel {
    val index = remember(count, initial) { 1_000_000 / count * count + initial }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offset = with(density) { 18.dp.roundToPx() }
    val list = rememberLazyListState(index - 2, offset)
    return remember(list, count, index, offset) { ClockWheel(list, count, index, offset) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LedgerTimePicker(
    date: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initial = remember(initialTime) { LocalTime.parse(initialTime) }
    val hours = rememberClockWheel(24, initial.hour)
    val minutes = rememberClockWheel(60, initial.minute)
    // The infinite wheels consume their own scroll. Stop any remaining vertical
    // scroll/fling here so it cannot turn into a drag of the surrounding sheet.
    val scrollBoundary = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ) = Offset(0f, available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity) =
                Velocity(0f, available.y)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .testTag("time-picker-content")
                .nestedScroll(scrollBoundary)
                .pointerInput(Unit) {
                    // Child LazyColumns get first chance to scroll. A drag starting
                    // on the gaps/units/padding is consumed instead of moving the sheet.
                    detectVerticalDragGestures { change, _ -> change.consume() }
                }
                .padding(24.dp, 0.dp, 24.dp, 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("选择时间", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                SheetCloseButton("取消时间选择", onDismiss)
            }
            Text(
                date.replaceFirst("-", "年").replaceFirst("-", "月") + "日",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Box(
                Modifier.fillMaxWidth()
                    .testTag("time-wheel-panel")
                    .padding(top = 20.dp, bottom = 16.dp)
                    .height(304.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE9EFDF))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClockWheelColumn(hours, "小时")
                    Text(
                        "时",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    ClockWheelColumn(minutes, "分钟")
                    Text("分", color = Muted, fontSize = 12.sp)
                }
            }
            PrimaryButton(
                "确定",
                onClick = {
                    onConfirm(LocalTime.of(hours.value, minutes.value).format(ClockFormat))
                },
                modifier = Modifier.testTag("confirm-time"),
            )
        }
    }
}

@Composable
private fun ClockWheelColumn(wheel: ClockWheel, label: String) {
    val scope = rememberCoroutineScope()
    val selected by remember { derivedStateOf { wheel.value } }
    LazyColumn(
        state = wheel.list,
        flingBehavior = rememberSnapFlingBehavior(wheel.list),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.width(112.dp).height(304.dp).testTag("time-$label").semantics {
                contentDescription = label
                stateDescription = "%02d".format(java.util.Locale.ROOT, selected)
                progressBarRangeInfo =
                    ProgressBarRangeInfo(selected.toFloat(), 0f..(wheel.count - 1).toFloat())
                setProgress { value ->
                    val target = value.roundToInt().coerceIn(0, wheel.count - 1)
                    scope.launch { wheel.center(wheel.selectedIndex + target - wheel.value) }
                    true
                }
                customActions =
                    listOf(
                        CustomAccessibilityAction("增加${label}") {
                            scope.launch { wheel.center(wheel.selectedIndex + 1) }
                            true
                        },
                        CustomAccessibilityAction("减少${label}") {
                            scope.launch { wheel.center(wheel.selectedIndex - 1) }
                            true
                        },
                    )
            },
    ) {
        items(2_000_000) { index ->
            Box(
                Modifier.fillMaxWidth().height(68.dp).clickable {
                    scope.launch { wheel.center(index) }
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%02d".format(java.util.Locale.ROOT, index % wheel.count),
                    fontSize = 38.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Green,
                    modifier =
                        Modifier.graphicsLayer {
                            val info = wheel.list.layoutInfo
                            val item = info.visibleItemsInfo.find { it.index == index }
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val distance =
                                item?.let { abs(it.offset + it.size / 2f - center) / it.size } ?: 3f
                            alpha = (1f - distance * .44f).coerceIn(0f, 1f)
                            val scale = 1f - distance.coerceAtMost(3f) * .18f
                            scaleX = scale
                            scaleY = scale
                            rotationX =
                                ((item?.let { (it.offset + it.size / 2f - center) / it.size }
                                    ?: 0f) * -17f)
                            cameraDistance = 500f
                        },
                )
            }
        }
    }
}
