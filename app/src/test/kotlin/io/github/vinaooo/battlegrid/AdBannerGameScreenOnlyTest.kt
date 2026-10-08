package io.github.vinaooo.battlegrid

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.kotest.matchers.shouldBe
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The whole app, wired by Hilt: the banner belongs to the game screen only, and every screen is reachable. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-port")
class AdBannerGameScreenOnlyTest {
    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hilt).around(compose)

    @Test
    fun `the banner is on the game screen and not on scores or settings`() {
        compose.onNodeWithContentDescription("Random").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertExists()

        compose.onNodeWithContentDescription("Scores").performClick()
        compose.onNodeWithText("Scores").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertDoesNotExist()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Opponent").assertExists()
        compose.onNodeWithTag(AdBannerProvider.TEST_TAG).assertDoesNotExist()
    }

    @Inject lateinit var consent: FakeAdConsent

    @Test
    fun `consent is gathered once when the app starts`() {
        hilt.inject()

        consent.gathered shouldBe 1
    }
}
