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

    /**
     * Cold CI runners spend seconds unpacking the Robolectric runtime, seeding Room and rendering
     * the first frame, so the first assertion must not race a fixed ten second budget. Every wait
     * keeps a generous ceiling and fails only when the UI is genuinely missing.
     */
    private val uiTimeoutMs = 60_000L

    @Test
    fun launchesSavesAndRestoresEntry() {
        compose.waitUntil(uiTimeoutMs) {
            compose.onAllNodesWithText("最近账单").fetchSemanticsNodes().isNotEmpty()
        }
        val scale = compose.activity.resources.displayMetrics.density
        val bar = compose.onNodeWithTag("ledger-bottom-bar").fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithContentDescription("记一笔").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(
            "Navigation body should be 60dp",
            kotlin.math.abs(bar.height / scale - 60f) < 1f,
        )
        org.junit.Assert.assertTrue("Add button must float above bar", add.top < bar.top)
        org.junit.Assert.assertTrue(
            "Add touch target must remain at least 48dp",
            add.width / scale >= 48f && add.height / scale >= 48f,
        )
        compose.onNodeWithContentDescription("统计").performClick()
        compose.onNodeWithText("收支统计").assertIsDisplayed()
        compose.onNodeWithContentDescription("明细").performClick()
        compose.onNodeWithContentDescription("记一笔").performClick()
        compose.onNodeWithText("3").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("✓  保存账目").performClick()
        compose.waitUntil(uiTimeoutMs) {
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
        compose.waitUntil(uiTimeoutMs) {
            compose.onAllNodesWithText("− 32.00").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("− 32.00").onLast().assertIsDisplayed()
    }
}
