package com.clottins.dailyledger

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "zh-rCN-w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimePickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val entry = Entry("existing", EXPENSE, 3200, "餐饮美食", "2026-11-17", "08:45", "晚餐")
    private val state = LedgerState(entries = listOf(entry), ready = true)

    private fun openEditor(day: Int = 18, onSave: (Entry) -> Unit) {
        compose.setContent {
            LedgerTheme { Surface(color = Bg) { EditorScreen(entry, state, false, {}, onSave) } }
        }
        compose.onNodeWithTag("entry-date-time").performClick()
        compose.onNodeWithTag("date-day-2026-11-${day.toString().padStart(2, '0')}").performClick()
        compose.onNodeWithTag("confirm-date").performClick()
        compose.onNodeWithText("选择时间").assertIsDisplayed()
    }

    private fun setWheel(label: String, value: Int) {
        compose.onNodeWithTag("time-$label").performSemanticsAction(SemanticsActions.SetProgress) {
            it(value.toFloat())
        }
        compose.waitForIdle()
    }

    private fun step(label: String, index: Int) {
        val actions =
            compose
                .onNodeWithTag("time-$label")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(actions[index].action()) }
        compose.waitForIdle()
    }

    @Test
    fun dateThenTimeConfirmsAndSavesExistingId() {
        var saved: Entry? = null
        openEditor { saved = it }
        compose
            .onNodeWithTag("time-小时")
            .assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                    "08",
                )
            )
        compose
            .onNodeWithTag("time-分钟")
            .assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                    "45",
                )
            )
        capture()
        setWheel("小时", 23)
        setWheel("分钟", 59)
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithText("2026/11/18  23:59").assertIsDisplayed()
        compose.onNodeWithText("✓  保存修改").performClick()
        compose.runOnIdle { assertEquals(entry.copy(date = "2026-11-18", time = "23:59"), saved) }
    }

    @Test
    fun bothWheelsWrapInBothDirectionsAndTouchScrolls() {
        var result = ""
        compose.setContent {
            LedgerTheme { LedgerTimePicker("2026-11-17", "23:59", {}, { result = it }) }
        }
        step("小时", 0)
        step("分钟", 0)
        compose.onNodeWithTag("confirm-time").performClick()
        compose.runOnIdle { assertEquals("00:00", result) }
        step("小时", 1)
        step("分钟", 1)
        compose.onNodeWithTag("confirm-time").performClick()
        compose.runOnIdle { assertEquals("23:59", result) }
        compose.onNodeWithTag("time-小时").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithTag("confirm-time").performClick()
        compose.runOnIdle {
            val hour = result.substringBefore(':').toInt()
            assertTrue(hour in 0..23)
            assertNotEquals(23, hour)
        }
    }

    @Test
    fun cancelKeepsBothOriginalDateAndTime() {
        var saved: Entry? = null
        openEditor { saved = it }
        setWheel("小时", 0)
        compose.onNodeWithContentDescription("取消时间选择").performClick()
        compose.onNodeWithText("2026/11/17  08:45").assertIsDisplayed()
        compose.onNodeWithText("✓  保存修改").performClick()
        compose.runOnIdle { assertEquals(entry, saved) }
    }

    @Test
    fun timeOnlyChangeTriggersUnsavedConfirmation() {
        openEditor(day = 17) {}
        setWheel("小时", 12)
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("放弃未保存的修改？").assertIsDisplayed()
    }

    private fun capture() {
        System.getenv("LEDGER_SCREENSHOTS")?.let { path ->
            compose.runOnIdle {
                val view = ShadowDialog.getLatestDialog().window!!.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                File(path, "native-time-picker.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }
}
