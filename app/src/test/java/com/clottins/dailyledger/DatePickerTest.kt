package com.clottins.dailyledger

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
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
class DatePickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calendarUsesSundayFirstAndHandlesLeapYears() {
        val november = calendarDays(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 11, 1), november.first())
        assertEquals(35, november.size)
        val leap = calendarDays(YearMonth.of(2024, 2))
        assertEquals(29, leap.filterNotNull().size)
        assertEquals(LocalDate.of(2024, 2, 29), leap.filterNotNull().last())
        assertEquals(42, calendarDays(YearMonth.of(2026, 8)).size)
    }

    @Test
    fun bottomSheetMatchesPreviewAndNavigatesAcrossYears() {
        var result = ""
        compose.setContent { LedgerTheme { LedgerDatePicker("2026-11-17", {}, { result = it }) } }
        compose.onNodeWithTag("date-day-2026-11-17").assertIsSelected()
        val sheet = compose.onNodeWithTag("ledger-date-sheet").fetchSemanticsNode().boundsInRoot
        val rootHeight = compose.activity.window.decorView.height
        assertTrue(
            "Date picker must be anchored to the bottom, not a centered platform dialog",
            sheet.top > rootHeight / 4f,
        )
        capture()
        compose.onNodeWithContentDescription("下个月").performClick()
        compose.onNodeWithContentDescription("下个月").performClick()
        compose.onNodeWithText("2027年1月").assertIsDisplayed()
        compose.onNodeWithTag("date-day-2027-01-03").performClick()
        compose.onNodeWithTag("confirm-date").performClick()
        compose.runOnIdle { assertEquals("2027-01-03", result) }
    }

    @Test
    fun cancellingCalendarKeepsOriginalEntry() {
        val entry = Entry("original", EXPENSE, 3200, "餐饮美食", "2026-11-17", "08:45", "晚餐")
        var saved: Entry? = null
        compose.setContent {
            LedgerTheme {
                Surface(color = Bg) {
                    EditorScreen(
                        entry,
                        LedgerState(entries = listOf(entry), ready = true),
                        false,
                        {},
                        { saved = it },
                    )
                }
            }
        }
        compose.onNodeWithTag("entry-date-time").performClick()
        compose.onNodeWithTag("date-day-2026-11-18").performClick()
        compose.onNodeWithContentDescription("取消日期选择").performClick()
        compose.onNodeWithText("2026/11/17  08:45").assertIsDisplayed()
        compose.onNodeWithText("✓  保存修改").performClick()
        compose.runOnIdle { assertEquals(entry, saved) }
    }

    private fun capture() {
        System.getenv("LEDGER_SCREENSHOTS")?.let { path ->
            compose.runOnIdle {
                val view = ShadowDialog.getLatestDialog().window!!.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                File(path, "native-date-sheet.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }
}
