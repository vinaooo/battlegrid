package io.github.vinaooo.battlegrid

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AppCategoryTest {

    @Test
    fun `the app is filed as a game, so Android groups it with games in battery and data usage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.applicationInfo.category shouldBe ApplicationInfo.CATEGORY_GAME
    }
}
