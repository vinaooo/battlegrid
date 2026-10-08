package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.MoveOutcome
import kotlin.random.Random

/** The fleet of [size] laid along every other row from the top-left: ship i on row 2i. */
fun rowFleet(size: BoardSize): List<Ship> =
    size.fleet.mapIndexed { i, type -> Ship(type, Coord(2 * i, 0), Orientation.HORIZONTAL) }

fun mode(
    size: BoardSize = BoardSize.TEN,
    firing: FiringMode = FiringMode.CLASSIC,
    opponent: Opponent = Opponent.MEDIUM,
) = GameMode(size, firing, opponent)

fun GameEngine.play(state: GameState, vararg moves: Move): GameState = moves.fold(state) { current, move ->
    (apply(current, move) as? MoveOutcome.Applied)?.state ?: error("$move rejected")
}

/** A battle against the AI, both fleets on [rowFleet], [first] firing first. */
fun GameEngine.battle(mode: GameMode = mode(), first: Side = Side.PLAYER): GameState =
    play(newGame(mode, first, rowFleet(mode.size)), Move.SetFleet(rowFleet(mode.size)), Move.ConfirmFleet)

/** A random valid fleet, ships dropped at random spots until each fits. */
fun randomFleet(size: BoardSize, random: Random): List<Ship> {
    val placed = mutableListOf<Ship>()
    for (type in size.fleet) {
        while (true) {
            val ship = Ship(
                type,
                Coord(random.nextInt(size.side), random.nextInt(size.side)),
                Orientation.entries.random(random),
            )
            val clear = ship.cells.all { it.isOn(size.side) } &&
                placed.none { other -> other.cells.any { it in ship.cells } }
            if (clear) {
                placed += ship
                break
            }
        }
    }
    return placed
}

fun c(row: Int, col: Int) = Coord(row, col)
