package com.clottins.dailyledger

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "zh-rCN-w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeUiTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val date = LocalDate.of(2026, 11, 16)
    private val rows =
        listOf(
            Entry("shopping", EXPENSE, 19900, "购物消费", "2026-11-14", "15:20", "降噪耳机"),
            Entry("refund", REFUND, 19900, "退款", "2026-11-16", "15:10", "耳机退货", "shopping"),
            Entry("salary", INCOME, 1500000, "工资收入", "2026-11-15", "10:00", "十一月工资"),
        )
    private val state =
        LedgerState(
            rows.sortedByDescending { it.date },
            defaultCategories,
            Preferences(),
            true,
            mapOf("shopping" to 19900),
        )

    private fun capture(name: String) {
        System.getenv("LEDGER_SCREENSHOTS")?.let { path ->
            File(path).mkdirs()
            compose.runOnIdle {
                val view = compose.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                File(path, "$name.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }

    @Test
    fun editRefundKeepsTypeAndAssociation() {
        var saved: Entry? = null
        compose.setContent {
            LedgerTheme {
                Surface(color = Bg) { EditorScreen(rows[1], state, false, {}, { saved = it }) }
            }
        }
        compose.onNodeWithText("退款金额").assertIsDisplayed()
        compose.onNodeWithText("生活费").assertExists()
        compose.onNodeWithText("关联账单").assertIsDisplayed()
        capture("native-refund")
        compose.onNodeWithText("✓  保存修改").performClick()
        compose.runOnIdle {
            assertEquals(REFUND, saved?.type)
            assertEquals("shopping", saved?.linkedExpenseId)
            assertEquals(19900L, saved?.cents)
        }
    }

    @Test
    fun newEntryKeypadAndSave() {
        var saved: Entry? = null
        compose.setContent {
            LedgerTheme {
                Surface(color = Bg) { EditorScreen(null, state, false, {}, { saved = it }) }
            }
        }
        compose.onNodeWithText("✓  保存账目").assertIsNotEnabled()
        compose.onNodeWithText("3").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("✓  保存账目").performClick()
        compose.runOnIdle {
            assertEquals(3200L, saved?.cents)
            assertEquals(EXPENSE, saved?.type)
        }
        capture("native-entry")
    }

    @Test
    fun homeRefundBadge() {
        val summary = ScreenSummary(Ledger.totals(rows), Ledger.totals(rows), state.entries)
        compose.setContent {
            LedgerTheme { Surface(color = Bg) { HomeScreen(state, summary, date, {}, {}, {}, {}) } }
        }
        compose.onNodeWithText("（已退款）").assertExists()
        compose.onNodeWithText("退款 · 仅影响余额").assertExists()
        capture("native-home")
    }

    @Test
    fun statisticsIncomeAndExpenseSwitch() {
        val summary =
            ScreenSummary(
                Ledger.totals(rows),
                Ledger.totals(rows),
                state.entries,
                mapOf(14 to 19900),
                mapOf(15 to 1500000),
                listOf("购物消费" to 19900),
                listOf("工资收入" to 1500000),
            )
        compose.setContent {
            LedgerTheme {
                Surface(color = Bg) { StatisticsScreen(state, summary, YearMonth.from(date), {}) }
            }
        }
        capture("native-statistics")
        compose.onAllNodesWithText("收入").onLast().performClick()
        compose.onNodeWithText("工资收入").assertExists()
        compose.onNodeWithText("总收入").assertExists()
    }

    @Test
    fun importedRefundCategoryKeepsRefundType() {
        var saved: Entry? = null
        val custom = rows[1].copy(category = "售后返还")
        val importedState =
            state.copy(categories = defaultCategories + category(REFUND, "售后返还", "refund"))
        compose.setContent {
            LedgerTheme {
                Surface(color = Bg) {
                    EditorScreen(custom, importedState, false, {}, { saved = it })
                }
            }
        }
        compose.onNodeWithText("退款金额").assertIsDisplayed()
        compose.onNodeWithText("✓  保存修改").performClick()
        compose.runOnIdle {
            assertEquals(REFUND, saved?.type)
            assertEquals("售后返还", saved?.category)
        }
    }
}
