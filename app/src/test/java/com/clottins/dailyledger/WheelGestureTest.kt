package com.clottins.dailyledger

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "zh-rCN-w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelGestureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun open() {
        compose.setContent { LedgerTheme { LedgerTimePicker("2026-11-17", "08:45", {}, {}) } }
        compose.waitForIdle()
    }

    private fun dragAndCheck(x: Float, y: Float) {
        val panel = compose.onNodeWithTag("time-wheel-panel")
        val start = panel.fetchSemanticsNode().boundsInRoot.top
        panel.performTouchInput {
            down(Offset(x, y))
            moveBy(Offset(0f, 40f), 100)
            moveBy(Offset(0f, 40f), 100)
        }
        compose.waitForIdle()
        val during = panel.fetchSemanticsNode().boundsInRoot.top
        assertEquals("Panel moved while finger was down", start, during, 1f)
        panel.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(
            "Panel moved after release",
            start,
            panel.fetchSemanticsNode().boundsInRoot.top,
            1f,
        )
    }

    @Test
    fun blankBesideColumnsDoesNotPullDownSheet() {
        open()
        dragAndCheck(5f, 110f)
        dragAndCheck(180f, 110f)
        compose.onNodeWithText("选择时间").assertIsDisplayed()
    }

    @Test
    fun rowGapsStillScrollBothWheelsWithoutMovingSheet() {
        open()
        val panel = compose.onNodeWithTag("time-wheel-panel").fetchSemanticsNode().boundsInRoot
        for (label in listOf("小时", "分钟")) {
            val wheel = compose.onNodeWithTag("time-$label")
            val before = wheel.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            val rect = wheel.fetchSemanticsNode().boundsInRoot
            // 34dp above the selected center: empty space between adjacent numeric glyphs.
            dragAndCheck(rect.center.x - panel.left, rect.center.y - panel.top - 34f)
            val after = wheel.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            assertNotEquals("Dragging a row gap must still adjust $label", before, after)
        }
    }
}
