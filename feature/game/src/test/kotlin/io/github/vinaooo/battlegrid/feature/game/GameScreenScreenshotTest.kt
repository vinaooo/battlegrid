package io.github.vinaooo.battlegrid.feature.game

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.feature.game.ui.GameScreen
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The game screen as players see it, in the layouts and themes it supports. Dynamic color off: the brand teal. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
class GameScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()

    private fun fleet(size: BoardSize) =
        size.fleet.mapIndexed { i, type -> Ship(type, Coord(2 * i, i % 3), Orientation.HORIZONTAL) }

    private fun placing(mode: GameMode) = GameSession(1, engine.newGame(mode, Side.PLAYER, fleet(mode.size)))

    /** A battle a few shots in: a miss, a hit and a sunk destroyer on each side. */
    private fun battle(mode: GameMode): GameSession {
        val started = placing(mode).play(Move.SetFleet(fleet(mode.size)), engine)!!.play(Move.ConfirmFleet, engine)!!
        val destroyer = fleet(mode.size).last().cells
        val shots = listOf(Coord(1, 5), Coord(0, 0)) + destroyer
        val state = started.state
        return started.copy(
            state = state.copy(enemy = state.enemy.copy(shots = shots), player = state.player.copy(shots = shots)),
        )
    }

    private fun shoot(name: String, state: GameUiState, settings: AppSettings = AppSettings()) {
        val app = settings.copy(dynamicColor = false, themeColor = ThemeColor.TEAL)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            VinkitTheme(app.themeColor, app.themeMode, app.dynamicColor) {
                GameScreen(state.copy(settings = app), {}, onOpenScores = {}, onOpenSettings = {}, onOpenBadges = {})
            }
        }
        compose.mainClock.advanceTimeBy(SETTLE_MILLIS)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun placementLight() = shoot("game_placement_light", GameUiState(placing(GameMode.DEFAULT)))

    @Test
    fun battleDark() = shoot(
        "game_battle_dark",
        GameUiState(battle(GameMode.DEFAULT)),
        AppSettings(themeMode = ThemeMode.DARK),
    )

    @Test
    fun salvo12() {
        val session = battle(GameMode(BoardSize.TWELVE, FiringMode.SALVO, Opponent.HARD))
        val marked = session.play(Move.MarkSalvo(Coord(5, 5)), engine)!!.play(Move.MarkSalvo(Coord(7, 9)), engine)!!
        shoot("game_salvo_12", GameUiState(marked))
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun landscape8() = shoot(
        "game_landscape_8",
        GameUiState(battle(GameMode(BoardSize.EIGHT, FiringMode.HIT_AGAIN, Opponent.EASY))),
    )

    @Test
    @Config(qualifiers = "sw800dp-w800dp-h1280dp-port-mdpi")
    fun tabletPhoneView() = shoot(
        "game_tablet_phone_view",
        GameUiState(battle(GameMode.DEFAULT)),
        AppSettings(phoneView = true, phoneViewSide = PhoneViewSide.RIGHT),
    )

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-port-xxhdpi")
    fun portuguese() = shoot("game_pt_br", GameUiState(battle(GameMode.DEFAULT)))

    private companion object {
        const val SETTLE_MILLIS = 600L
    }
}
