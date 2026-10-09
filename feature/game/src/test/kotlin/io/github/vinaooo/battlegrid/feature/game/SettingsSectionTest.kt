package io.github.vinaooo.battlegrid.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.feature.game.settings.GameSection
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the game section picks the board, the firing mode and the opponent, and explains the opponent`() {
        val changes = mutableListOf<GameMode>()
        compose.setContent {
            // vinkit's section card lays its rows out in a column; so does this test.
            VinkitTheme(ThemeColor.TEAL) {
                Column { GameSection(GameMode.DEFAULT) { changes += it } }
            }
        }
        compose.onNodeWithText("Fires at random until it hits, then sinks that ship").assertExists()
        compose.onNodeWithText("12×12").performClick()
        compose.onNodeWithText("Salvo").performClick()
        compose.onNodeWithContentDescription("2 players").performClick()
        changes shouldContainExactly listOf(
            GameMode.DEFAULT.copy(size = BoardSize.TWELVE),
            GameMode.DEFAULT.copy(firing = FiringMode.SALVO),
            GameMode.DEFAULT.copy(opponent = Opponent.TWO_PLAYER),
        )
    }
}
