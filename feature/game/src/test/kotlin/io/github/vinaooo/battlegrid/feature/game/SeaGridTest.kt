package io.github.vinaooo.battlegrid.feature.game

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.feature.game.board.GridView
import io.github.vinaooo.battlegrid.feature.game.board.SeaGrid
import io.github.vinaooo.battlegrid.feature.game.ui.cellName
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeaGridTest {
    @get:Rule
    val compose = createComposeRule()

    private val tapped = mutableListOf<Coord>()

    private fun show() = compose.setContent {
        VinkitTheme(ThemeColor.TEAL) {
            SeaGrid(
                GridView(10, emptyList(), emptyList()),
                Modifier.size(318.dp),
                onTap = { tapped += it },
                tappable = { true },
                cellDescription = { cellName(it) },
            )
        }
    }

    @Test
    fun `each cell's node sits on its drawn cell, and a touch at its center hits that cell`() {
        show()
        // 318dp = 10.6 cells of 30dp: an 18dp label band, then E5 at 18 + 4 × 30 across, 18 + 4 × 30 down.
        val bounds = compose.onNodeWithContentDescription("E5").getBoundsInRoot()
        bounds.left.value shouldBe 138f
        bounds.top.value shouldBe 138f
        compose.onRoot().performTouchInput { click(Offset(153.dp.toPx(), 153.dp.toPx())) }
        tapped shouldBe listOf(Coord(4, 4))
    }

    @Test
    fun `TalkBack fires at a cell through its node`() {
        show()
        compose.onNodeWithContentDescription("J10").performClick()
        tapped shouldBe listOf(Coord(9, 9))
    }

    @Test
    fun `in the battle, a touch on a target cell's node fires at that cell`() {
        val engine = io.github.vinaooo.battlegrid.domain.rules.GameEngine()
        val fleet = io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
            .place(io.github.vinaooo.battlegrid.domain.model.BoardSize.TEN, kotlin.random.Random(1))
        val session = io.github.vinaooo.battlegrid.domain.session.GameSession(
            1,
            engine.newGame(
                io.github.vinaooo.battlegrid.domain.model.GameMode.DEFAULT,
                io.github.vinaooo.battlegrid.domain.model.Side.PLAYER,
                fleet,
            ),
        ).play(io.github.vinaooo.battlegrid.domain.model.Move.SetFleet(fleet), engine)!!
            .play(io.github.vinaooo.battlegrid.domain.model.Move.ConfirmFleet, engine)!!
        compose.setContent {
            VinkitTheme(ThemeColor.TEAL) {
                io.github.vinaooo.battlegrid.feature.game.board.BattleBoard(
                    GameUiState(session),
                    landscape = true,
                    onTap = { tapped += it },
                    onSwap = {},
                    modifier = Modifier.size(900.dp, 400.dp),
                )
            }
        }
        val node = compose.onNodeWithContentDescription("E5, untried").getBoundsInRoot()
        compose.onRoot().performTouchInput {
            click(Offset(((node.left + node.right) / 2).toPx(), ((node.top + node.bottom) / 2).toPx()))
        }
        tapped shouldBe listOf(Coord(4, 4))
    }
}
