package io.github.vinaooo.battlegrid.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface Phase {
    /** [side] places its fleet. Leaving now isn't a loss: no shot was fired. */
    @Serializable
    data class Placement(val side: Side) : Phase

    @Serializable
    data object Battle : Phase

    @Serializable
    data class Over(val winner: Side, val resigned: Boolean = false) : Phase
}

/**
 * A game: its mode, both grids, the phase, whose turn it is ([toMove] fires at the other side's grid), who fired
 * first, the cells marked for the next salvo, hints used, battle time, and [actions] (every applied battle move,
 * which seeds the AI's random choices).
 */
@Serializable
data class GameState(
    val mode: GameMode,
    val player: Grid,
    val enemy: Grid,
    val phase: Phase,
    val toMove: Side,
    val firstMover: Side,
    val salvoMarks: List<Coord> = emptyList(),
    val hintsUsed: Int = 0,
    val elapsedSeconds: Long = 0,
    val actions: Int = 0,
) {
    val isOver: Boolean get() = phase is Phase.Over

    val isBattle: Boolean get() = phase == Phase.Battle

    val winner: Side? get() = (phase as? Phase.Over)?.winner

    /** Against the AI, during the battle: it's the AI's turn. */
    val isAiTurn: Boolean get() = mode.isVsAi && isBattle && toMove == Side.ENEMY

    /** The shots the player has fired: their score before hints. */
    val playerShots: Int get() = enemy.shots.size

    fun gridOf(side: Side): Grid = if (side == Side.PLAYER) player else enemy

    /** The grid [toMove] fires at. */
    val target: Grid get() = gridOf(toMove.other)

    fun withGrid(side: Side, grid: Grid): GameState =
        if (side == Side.PLAYER) copy(player = grid) else copy(enemy = grid)
}

sealed interface Move {
    /** Puts (or moves) the fleet's ship [index] where [ship] says. */
    data class PlaceShip(val index: Int, val ship: Ship) : Move

    /** Turns placed ship [index] about its bow. */
    data class RotateShip(val index: Int) : Move

    /** Replaces the whole fleet being placed (the Random button). */
    data class SetFleet(val ships: List<Ship>) : Move

    /** The fleet being placed is ready. */
    data object ConfirmFleet : Move

    /** One shot, in Classic and Hit-again. */
    data class Fire(val coord: Coord) : Move

    /** Marks or unmarks a cell for the next salvo. */
    data class MarkSalvo(val coord: Coord) : Move

    /** Fires every marked cell. */
    data object FireSalvo : Move

    /** [side] gives up the battle. */
    data class Resign(val side: Side) : Move
}
