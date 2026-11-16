package com.clottins.dailyledger

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "zh-rCN-w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesSavesAndRestoresEntry() {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("最近账单").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("记一笔").performClick()
        compose.onNodeWithText("3").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("✓  保存账目").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("− 32.00").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("− 32.00").onLast().assertIsDisplayed()
        System.getenv("LEDGER_SCREENSHOTS")?.let { path ->
            compose.runOnIdle {
                val v = compose.activity.window.decorView
                val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
                v.draw(android.graphics.Canvas(b))
                File(path, "native-app-home.png").outputStream().use {
                    b.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                b.recycle()
            }
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("− 32.00").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("− 32.00").onLast().assertIsDisplayed()
    }
}
