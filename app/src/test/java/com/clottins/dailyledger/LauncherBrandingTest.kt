package com.clottins.dailyledger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherBrandingTest {
    @Test
    fun installedLabelAndAdaptiveIconLoadFromManifest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        assertEquals("余白", pm.getApplicationLabel(context.applicationInfo).toString())
        assertEquals("com.clottins.dailyledger", context.packageName)
        val icon = context.applicationInfo.loadIcon(pm)
        assertTrue("Launcher icon must use the adaptive resource", icon is AdaptiveIconDrawable)
        val adaptive = icon as AdaptiveIconDrawable
        assertNotNull(adaptive.background)
        assertNotNull(adaptive.foreground)
        val bitmap = Bitmap.createBitmap(288, 288, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 288, 288)
        icon.draw(Canvas(bitmap))
        assertEquals("Pale background must render", 0xFFE9EFCF.toInt(), bitmap.getPixel(144, 30))
        assertEquals("Wallet body must render", 0xFF235447.toInt(), bitmap.getPixel(120, 170))
        System.getenv("LEDGER_SCREENSHOTS")?.let { path ->
            File(path, "native-launcher-icon.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        bitmap.recycle()
    }
}
