package io.github.vinaooo.battlegrid.feature.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.feature.game.ui.GameScreen
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()
    private val intents = mutableListOf<GameIntent>()

    private fun battle(): GameSession {
        val fleet = RandomFleetPlacer.place(BoardSize.TEN, Random(1))
        return GameSession(1, engine.newGame(GameMode.DEFAULT, Side.PLAYER, fleet))
            .play(Move.SetFleet(fleet), engine)!!
            .play(Move.ConfirmFleet, engine)!!
    }

    private fun show(ui: GameUiState) = compose.setContent {
        VinkitTheme(ThemeColor.TEAL) { GameScreen(ui, { intents += it }) }
    }

    private fun tapNode(description: String) {
        val node = compose.onNodeWithContentDescription(description).getBoundsInRoot()
        compose.onRoot().performTouchInput {
            click(Offset(((node.left + node.right) / 2).toPx(), ((node.top + node.bottom) / 2).toPx()))
        }
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun `in landscape, a touch on a target cell's node fires at that cell`() {
        show(GameUiState(battle()))
        tapNode("E5, untried")
        intents shouldBe listOf(GameIntent.Tap(Coord(4, 4)))
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `in portrait, a touch on a target cell's node fires at that cell`() {
        show(GameUiState(battle()))
        tapNode("E5, untried")
        intents shouldBe listOf(GameIntent.Tap(Coord(4, 4)))
    }

    private fun placing(mode: GameMode = GameMode.DEFAULT) =
        GameSession(1, engine.newGame(mode, Side.PLAYER, RandomFleetPlacer.place(mode.size, Random(1))))

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a docked ship dragged onto the grid lands with the grabbed cell under the finger`() {
        show(GameUiState(placing()))
        val ship = compose.onNodeWithContentDescription("Carrier, 5 cells, not placed").getBoundsInRoot()
        val target = compose.onRoot().getBoundsInRoot()
        // Grab the carrier by its first cell, then move in steps, as a finger does.
        compose.onRoot().performTouchInput {
            val start = Offset((ship.left + (ship.bottom - ship.top) / 2).toPx(), ((ship.top + ship.bottom) / 2).toPx())
            down(start)
            val steps = 20
            val travel = Offset(0f, -(ship.top - target.top).toPx() / 2)
            repeat(steps) { moveBy(travel / steps.toFloat()) }
            up()
        }
        val placed = intents.filterIsInstance<GameIntent.PlaceShip>().single()
        placed.index shouldBe 0
        placed.ship.origin.col shouldBe 0
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a tap on a placed ship turns it, and the toolbar deals and starts`() {
        val fleet = RandomFleetPlacer.place(BoardSize.TEN, Random(2))
        show(GameUiState(placing().play(Move.SetFleet(fleet), engine)))
        compose.onNodeWithContentDescription("Destroyer", substring = true).performClick()
        compose.onNodeWithContentDescription("Random").performClick()
        compose.onNodeWithContentDescription("Start").performClick()
        intents shouldBe listOf(GameIntent.RotateShip(4), GameIntent.RandomFleet, GameIntent.ConfirmFleet)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `Start waits for the whole fleet`() {
        show(GameUiState(placing()))
        compose.onNodeWithContentDescription("Start").assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `in portrait the small grid shows big on a tap, and a hint is asked from the toolbar`() {
        show(GameUiState(battle()))
        compose.onNodeWithContentDescription("Your fleet, 5 of 5 ships afloat").performClick()
        compose.onNodeWithContentDescription("Hint").performClick()
        intents shouldBe listOf(GameIntent.SwapGrids, GameIntent.Hint)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a salvo fires from the toolbar once every shot is marked`() {
        val mode =
            GameMode(
                BoardSize.TEN,
                io.github.vinaooo.battlegrid.domain.model.FiringMode.SALVO,
                io.github.vinaooo.battlegrid.domain.model.Opponent.EASY,
            )
        val fleet = RandomFleetPlacer.place(BoardSize.TEN, Random(1))
        val session = GameSession(1, engine.newGame(mode, Side.PLAYER, fleet))
            .play(Move.SetFleet(fleet), engine)!!.play(Move.ConfirmFleet, engine)!!
        compose.setContent { VinkitTheme(ThemeColor.TEAL) { GameScreen(GameUiState(session), { intents += it }) } }
        compose.onNodeWithContentDescription("Fire").assertIsNotEnabled()
        compose.onNodeWithText("Mark 5 shots").assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `the cover hides the game until the next player taps`() {
        val mode =
            GameMode(
                BoardSize.EIGHT,
                io.github.vinaooo.battlegrid.domain.model.FiringMode.CLASSIC,
                io.github.vinaooo.battlegrid.domain.model.Opponent.TWO_PLAYER,
            )
        val session = GameSession(1, engine.newGame(mode, Side.PLAYER))
            .play(Move.SetFleet(RandomFleetPlacer.place(BoardSize.EIGHT, Random(1))), engine)!!
            .play(Move.ConfirmFleet, engine)!!
        show(GameUiState(session, covered = true))
        compose.onNodeWithText("Pass the phone to Player 2").assertExists()
        compose.onAllNodesWithContentDescription("A1", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Tap to start").performClick()
        intents shouldBe listOf(GameIntent.Uncover)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a won game shows its result after a moment to see the board`() {
        val won = battle().play(Move.Resign(Side.ENEMY), engine)!!
        compose.mainClock.autoAdvance = false
        show(GameUiState(won))
        // The info line says it at once; the dialog, with the game's lines, a moment later.
        compose.onNodeWithText("No hints").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1_300)
        compose.onNodeWithText("No hints").assertExists()
    }
}
