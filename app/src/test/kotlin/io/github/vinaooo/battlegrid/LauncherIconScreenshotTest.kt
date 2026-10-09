package io.github.vinaooo.battlegrid

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val icon: AdaptiveIconDrawable
        get() = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable

    @Test
    fun `the icon's background is the brand teal`() {
        context.getColor(R.color.ic_launcher_background) shouldBe BRAND_TEAL
    }

    @Test
    fun `the themed icon has its own monochrome layer, not the colored foreground`() {
        val monochrome = icon.monochrome.shouldNotBeNull().toBitmap(SIZE_PX, SIZE_PX)
        val foreground = context.getDrawable(R.drawable.ic_launcher_foreground).shouldNotBeNull()
            .toBitmap(SIZE_PX, SIZE_PX)

        monochrome.sameAs(foreground) shouldNotBe true
    }

    @Test
    fun `launcher icon and its themed versions`() {
        val colored = icon.toBitmap(SIZE_PX, SIZE_PX).asImageBitmap()
        val monochrome = icon.monochrome.shouldNotBeNull().toBitmap(SIZE_PX, SIZE_PX).asImageBitmap()
        compose.setContent {
            Row(
                modifier = Modifier.background(Color.White).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(colored, contentDescription = null, modifier = Modifier.size(ICON_DP.dp).clip(CircleShape))
                // Themed icons as a light and a dark launcher tint them from the wallpaper.
                listOf(LIGHT_TINT to DARK_TINT, DARK_TINT to LIGHT_TINT).forEach { (back, ink) ->
                    Box(Modifier.size(ICON_DP.dp).clip(CircleShape).background(back)) {
                        Image(
                            monochrome,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ink),
                            modifier = Modifier.size(ICON_DP.dp),
                        )
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/launcher_icon.png")
    }

    private companion object {
        const val SIZE_PX = 432
        const val ICON_DP = 80
        val LIGHT_TINT = Color(0xFFB1F0E3)
        val DARK_TINT = Color(0xFF00504A)
        const val BRAND_TEAL = 0xFF006B5F.toInt()
    }
}
