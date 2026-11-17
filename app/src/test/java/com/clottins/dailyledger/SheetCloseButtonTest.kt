package com.clottins.dailyledger

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "zh-rCN-w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SheetCloseButtonTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun closeDiscsHaveCompleteRoundCapsInBothSheetsAtHighDensity() {
        var time by mutableStateOf(false)
        compose.setContent {
            LedgerTheme {
                if (time) LedgerTimePicker("2026-11-17", "08:45", {}, {})
                else LedgerDatePicker("2026-11-17", {}, { time = true })
            }
        }
        checkCircle("date")
        compose.onNodeWithTag("confirm-date").performClick()
        compose.onNodeWithText("选择时间").assertIsDisplayed()
        checkCircle("time")
    }

    private fun checkCircle(name: String) {
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("sheet-dismiss").fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("Close button must reserve at least 48dp", bounds.height / density >= 47.9f)
        compose.runOnIdle {
            val view = ShadowDialog.getLatestDialog().window!!.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val x = bounds.left.roundToInt()
            val y = bounds.top.roundToInt()
            val width = bounds.width.roundToInt()
            val height = bounds.height.roundToInt()
            val widths =
                (0 until height).map { row ->
                    (0 until width).count { col ->
                        bitmap.getPixel(x + col, y + row) == 0xFFEAF0E3.toInt()
                    }
                }
            val painted = widths.indices.filter { widths[it] > 0 }
            assertTrue("Close disc must render", painted.isNotEmpty())
            assertTrue("Disc top must stay inside header", painted.first() >= (3 * density).toInt())
            assertTrue(
                "Disc bottom must stay inside header",
                height - painted.last() >= (3 * density).toInt(),
            )
            val widest = widths.max()
            assertTrue(
                "Top cap must be round rather than clipped flat",
                widths[painted.first()] < widest / 2,
            )
            assertTrue("Bottom cap must be round", widths[painted.last()] < widest / 2)
            System.getenv("LEDGER_SCREENSHOTS")?.let { path ->
                File(path, "native-$name-close-fixed.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                val crop = Bitmap.createBitmap(bitmap, x, y, width, height)
                File(path, "close-button-$name.png").outputStream().use {
                    crop.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                crop.recycle()
            }
            bitmap.recycle()
        }
    }
}
