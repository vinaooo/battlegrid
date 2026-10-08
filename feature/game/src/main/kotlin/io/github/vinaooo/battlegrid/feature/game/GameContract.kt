package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.vinkit.core.AppSettings

data class GameUiState(
    val session: GameSession? = null,
    val settings: AppSettings = AppSettings(),
    /** The AI is taking its turn: the grids take no taps meanwhile. */
    val aiFiring: Boolean = false,
    /** The newest shots at [revealing]'s grid not shown yet: a salvo's results appear one by one. */
    val hiddenShots: Int = 0,
    val revealing: Side? = null,
    /** Portrait: the viewer's own grid is the big one (tapped, or while the opponent fires). */
    val ownBig: Boolean = false,
    /** Pass-and-play: the screen is covered until the next player taps. */
    val covered: Boolean = false,
    val announcement: Announcement? = null,
    /** Counts announcements, so the same one twice in a row is spoken twice. */
    val announcementSequence: Int = 0,
) {
    /**
     * Whose eyes are on the screen: the player against the AI; in pass-and-play, the side placing or firing. Their
     * own grid shows their ships; the other is the target.
     */
    val viewer: Side
        get() {
            val state = session?.state ?: return Side.PLAYER
            if (state.mode.isVsAi) return Side.PLAYER
            return (state.phase as? Phase.Placement)?.side ?: state.toMove
        }

    /** The viewer may fire (or mark a salvo) now. */
    val canFire: Boolean
        get() {
            val state = session?.state ?: return false
            return state.isBattle && !aiFiring && hiddenShots == 0 && !covered && state.toMove == viewer
        }

    /** The viewer is placing their fleet now. */
    val canPlace: Boolean
        get() = session?.state?.phase == Phase.Placement(viewer) && !covered

    /** The game is over and every shot is shown: time for the end dialog. */
    val ended: Boolean
        get() = session?.state?.isOver == true && hiddenShots == 0
}

sealed interface GameIntent {
    data class PlaceShip(val index: Int, val ship: Ship) : GameIntent

    data class RotateShip(val index: Int) : GameIntent

    data object RandomFleet : GameIntent

    data object ConfirmFleet : GameIntent

    /** A tap on the target grid: fires, or marks a salvo cell. */
    data class Tap(val coord: Coord) : GameIntent

    data object FireSalvo : GameIntent

    data object Hint : GameIntent

    /** Portrait: swaps which grid is big. */
    data object SwapGrids : GameIntent

    /** Pass-and-play: the next player lifts the cover. */
    data object Uncover : GameIntent

    data object Resign : GameIntent

    data object NewGame : GameIntent

    data object Restart : GameIntent

    /** The screen shows: the clock runs. */
    data object Resume : GameIntent

    /** The screen hides: the clock stops and the game is saved. */
    data object Pause : GameIntent
}

/** What TalkBack says after something happens. */
sealed interface Announcement {
    /** [shooter] fired at [coord]: [result], with the sunk ship's class. */
    data class Shot(val shooter: Side, val coord: Coord, val result: ShotResult, val sunk: ShipClass?) : Announcement

    data class Hinted(val cells: List<Coord>) : Announcement

    data class Ended(val winner: Side, val vsAi: Boolean, val resigned: Boolean) : Announcement

    /** Pass-and-play: [next] takes the phone. */
    data class Handover(val next: Side) : Announcement
}
