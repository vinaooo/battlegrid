package io.github.vinaooo.battlegrid.domain.rules

import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.Side

sealed interface MoveOutcome {
    data class Applied(val state: GameState) : MoveOutcome

    data object Rejected : MoveOutcome
}

/** The rules of every move, each kind kept by its own [MoveRule]. Pure: the same state and move, the same result. */
class GameEngine {
    /**
     * A new game in placement. Against the AI its fleet comes in [enemyFleet] (placed from the seed) and the player
     * places theirs; in pass-and-play both place, the player first.
     */
    fun newGame(mode: GameMode, firstMover: Side, enemyFleet: List<Ship>? = null): GameState {
        val enemy = enemyFleet?.let { Grid(mode.size, it) } ?: Grid.empty(mode.size)
        return GameState(
            mode = mode,
            player = Grid.empty(mode.size),
            enemy = enemy,
            phase = Phase.Placement(Side.PLAYER),
            toMove = Side.PLAYER,
            firstMover = firstMover,
        )
    }

    fun isLegal(state: GameState, move: Move): Boolean = when (move) {
        is Move.PlaceShip -> PlaceShipRule.isLegal(state, move)
        is Move.RotateShip -> RotateShipRule.isLegal(state, move)
        is Move.SetFleet -> SetFleetRule.isLegal(state, move)
        Move.ConfirmFleet -> ConfirmFleetRule.isLegal(state, Move.ConfirmFleet)
        is Move.Fire -> FireRule.isLegal(state, move)
        is Move.MarkSalvo -> MarkSalvoRule.isLegal(state, move)
        Move.FireSalvo -> FireSalvoRule.isLegal(state, Move.FireSalvo)
        is Move.Resign -> ResignRule.isLegal(state, move)
    }

    fun apply(state: GameState, move: Move): MoveOutcome {
        if (!isLegal(state, move)) return MoveOutcome.Rejected
        val next = when (move) {
            is Move.PlaceShip -> PlaceShipRule.apply(state, move)
            is Move.RotateShip -> RotateShipRule.apply(state, move)
            is Move.SetFleet -> SetFleetRule.apply(state, move)
            Move.ConfirmFleet -> ConfirmFleetRule.apply(state, Move.ConfirmFleet)
            is Move.Fire -> FireRule.apply(state, move)
            is Move.MarkSalvo -> MarkSalvoRule.apply(state, move)
            Move.FireSalvo -> FireSalvoRule.apply(state, Move.FireSalvo)
            is Move.Resign -> ResignRule.apply(state, move)
        }
        return MoveOutcome.Applied(next)
    }

    /** Every legal move but [Move.SetFleet] (any whole valid fleet) and [Move.Resign]. */
    fun legalMoves(state: GameState): List<Move> = when (val phase = state.phase) {
        is Phase.Placement -> placementMoves(state.gridOf(phase.side))
        Phase.Battle -> battleMoves(state)
        is Phase.Over -> emptyList()
    }.filter { isLegal(state, it) }

    private fun placementMoves(grid: Grid): List<Move> {
        val places = grid.size.fleet.flatMapIndexed { index, type ->
            grid.size.cells.flatMap { origin ->
                Orientation.entries.map { Move.PlaceShip(index, Ship(type, origin, it)) }
            }
        }
        return places + grid.ships.indices.map(Move::RotateShip) + Move.ConfirmFleet
    }

    private fun battleMoves(state: GameState): List<Move> {
        val untried = state.target.untried
        return if (state.mode.firing == FiringMode.SALVO) {
            untried.map(Move::MarkSalvo) + Move.FireSalvo
        } else {
            untried.map(Move::Fire)
        }
    }
}
