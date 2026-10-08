package io.github.vinaooo.battlegrid.domain.rules

import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side

/** One kind of move: when it's allowed and what it does. [apply] is only called with a legal move. */
interface MoveRule<M : Move> {
    fun isLegal(state: GameState, move: M): Boolean

    fun apply(state: GameState, move: M): GameState
}

/** The side placing its fleet now, or null outside placement. */
private val GameState.placing: Side? get() = (phase as? Phase.Placement)?.side

object PlaceShipRule : MoveRule<Move.PlaceShip> {
    override fun isLegal(state: GameState, move: Move.PlaceShip): Boolean {
        val side = state.placing ?: return false
        return state.gridOf(side).fits(move.index, move.ship)
    }

    override fun apply(state: GameState, move: Move.PlaceShip): GameState {
        val side = state.placing!!
        return state.withGrid(side, state.gridOf(side).place(move.index, move.ship))
    }
}

/** Turns a ship about its bow; one that would then leave the grid is shifted back inside. */
object RotateShipRule : MoveRule<Move.RotateShip> {
    override fun isLegal(state: GameState, move: Move.RotateShip): Boolean {
        val side = state.placing ?: return false
        val grid = state.gridOf(side)
        val ship = grid.ships.getOrNull(move.index) ?: return false
        return grid.fits(move.index, turned(ship, grid.size.side))
    }

    override fun apply(state: GameState, move: Move.RotateShip): GameState {
        val grid = state.gridOf(state.placing!!)
        return PlaceShipRule.apply(state, Move.PlaceShip(move.index, turned(grid.ships[move.index]!!, grid.size.side)))
    }

    private fun turned(ship: Ship, side: Int): Ship {
        val last = side - ship.type.length
        val origin = Coord(ship.origin.row.coerceAtMost(last), ship.origin.col.coerceAtMost(last))
        val turned = ship.copy(orientation = ship.orientation.other)
        // Only the axis the ship now runs along is pulled in.
        return turned.copy(
            origin = if (turned.orientation == Orientation.VERTICAL) {
                ship.origin.copy(row = origin.row)
            } else {
                ship.origin.copy(col = origin.col)
            },
        )
    }
}

object SetFleetRule : MoveRule<Move.SetFleet> {
    override fun isLegal(state: GameState, move: Move.SetFleet): Boolean {
        val side = state.placing ?: return false
        val size = state.gridOf(side).size
        if (move.ships.size != size.fleet.size) return false
        // Placed one by one on an empty grid: each must fit beside the ones before it.
        var grid = Grid.empty(size)
        move.ships.forEachIndexed { index, ship ->
            if (!grid.fits(index, ship)) return false
            grid = grid.place(index, ship)
        }
        return true
    }

    override fun apply(state: GameState, move: Move.SetFleet): GameState {
        val side = state.placing!!
        return state.withGrid(side, state.gridOf(side).copy(ships = move.ships))
    }
}

/**
 * Ends a placement. In pass-and-play the second player places next; then (and against the AI, whose fleet is
 * placed with the new game) the battle starts with the first mover.
 */
object ConfirmFleetRule : MoveRule<Move.ConfirmFleet> {
    override fun isLegal(state: GameState, move: Move.ConfirmFleet): Boolean {
        val side = state.placing ?: return false
        return state.gridOf(side).isFleetComplete
    }

    override fun apply(state: GameState, move: Move.ConfirmFleet): GameState = if (!state.enemy.isFleetComplete) {
        state.copy(phase = Phase.Placement(Side.ENEMY), toMove = Side.ENEMY)
    } else {
        state.copy(phase = Phase.Battle, toMove = state.firstMover)
    }
}

/** Resolves [coords] in order at the target; the battle ends the moment its last ship sinks. */
internal fun GameState.resolveShots(coords: List<Coord>, firing: FiringRule): GameState {
    var grid = target
    val results = mutableListOf<ShotResult>()
    for (coord in coords) {
        val (after, result) = grid.shoot(coord)
        grid = after
        results += result
        if (grid.isFleetSunk) break
    }
    val shot = withGrid(toMove.other, grid).copy(salvoMarks = emptyList(), actions = actions + 1)
    return when {
        grid.isFleetSunk -> shot.copy(phase = Phase.Over(toMove))
        firing.keepsTurn(results) -> shot
        else -> shot.copy(toMove = toMove.other)
    }
}

private fun GameState.canShootAt(coord: Coord): Boolean = coord.isOn(target.size.side) && !target.isTried(coord)

/** A single shot, in Classic and Hit-again. */
object FireRule : MoveRule<Move.Fire> {
    override fun isLegal(state: GameState, move: Move.Fire): Boolean =
        state.isBattle && state.mode.firing != FiringMode.SALVO && state.canShootAt(move.coord)

    override fun apply(state: GameState, move: Move.Fire): GameState =
        state.resolveShots(listOf(move.coord), firingFor(state.mode.firing))
}

/** Marks an untried cell for the salvo while there are shots left, or unmarks a marked one. */
object MarkSalvoRule : MoveRule<Move.MarkSalvo> {
    override fun isLegal(state: GameState, move: Move.MarkSalvo): Boolean {
        if (!state.isBattle || state.mode.firing != FiringMode.SALVO || !state.canShootAt(move.coord)) return false
        return move.coord in state.salvoMarks || state.salvoMarks.size < SalvoFiring.shotsPerTurn(state)
    }

    override fun apply(state: GameState, move: Move.MarkSalvo): GameState = state.copy(
        salvoMarks = if (move.coord in state.salvoMarks) {
            state.salvoMarks - move.coord
        } else {
            state.salvoMarks + move.coord
        },
    )
}

/** Fires the salvo once every shot of the turn is marked. */
object FireSalvoRule : MoveRule<Move.FireSalvo> {
    override fun isLegal(state: GameState, move: Move.FireSalvo): Boolean = state.isBattle &&
        state.mode.firing == FiringMode.SALVO &&
        state.salvoMarks.size == SalvoFiring.shotsPerTurn(state)

    override fun apply(state: GameState, move: Move.FireSalvo): GameState =
        state.resolveShots(state.salvoMarks, SalvoFiring)
}

/** Giving up during the battle: the other side wins. */
object ResignRule : MoveRule<Move.Resign> {
    override fun isLegal(state: GameState, move: Move.Resign): Boolean = state.isBattle

    override fun apply(state: GameState, move: Move.Resign): GameState =
        state.copy(phase = Phase.Over(move.side.other, resigned = true), salvoMarks = emptyList())
}
