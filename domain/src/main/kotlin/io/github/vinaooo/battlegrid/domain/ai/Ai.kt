package io.github.vinaooo.battlegrid.domain.ai

import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.rules.firingFor
import kotlin.random.Random

/**
 * What a shooter knows of the grid it fires at: misses, hits on ships still afloat ([openHits]), the cells of sunk
 * ships (shown once sunk), and the lengths of the ships still afloat (their names are announced when sunk).
 */
data class TargetView(
    val side: Int,
    val misses: Set<Coord>,
    val openHits: Set<Coord>,
    val sunk: Set<Coord>,
    val remainingLengths: List<Int>,
) {
    val untried: List<Coord>
        get() = (0 until side).flatMap { row -> (0 until side).map { Coord(row, it) } }
            .filter { it !in misses && it !in openHits && it !in sunk }

    /** As if [coord] were a miss: the next shot of a salvo is picked around the ones already chosen. */
    fun withPending(coord: Coord): TargetView = copy(misses = misses + coord)

    companion object {
        fun of(grid: Grid): TargetView {
            val results = grid.shots.groupBy { grid.resultAt(it) }
            return TargetView(
                side = grid.size.side,
                misses = results[ShotResult.MISS].orEmpty().toSet(),
                openHits = results[ShotResult.HIT].orEmpty().toSet(),
                sunk = results[ShotResult.SUNK].orEmpty().toSet(),
                remainingLengths = grid.ships.indices.filter { !grid.isSunk(it) }.map { grid.size.fleet[it].length },
            )
        }
    }
}

/** An AI level. It sees only a [TargetView] of the player's grid, never where the ships are. */
interface Ai {
    /** The cells to fire at this turn: one, or a whole salvo. */
    fun shots(state: GameState, random: Random): List<Coord> =
        pick(TargetView.of(state.target), firingFor(state.mode.firing).shotsPerTurn(state), random)

    /** [count] distinct cells, each picked as if the ones before it had missed. */
    fun pick(view: TargetView, count: Int, random: Random): List<Coord> {
        var current = view
        return List(count) {
            next(current, random).also { current = current.withPending(it) }
        }
    }

    fun next(view: TargetView, random: Random): Coord
}

/** Random untried cells. */
object EasyAi : Ai {
    override fun next(view: TargetView, random: Random): Coord = view.untried.random(random)
}

/** Hunt at random; after a hit, fire next to it, along the line when two hits are in a row. */
object MediumAi : Ai {
    override fun next(view: TargetView, random: Random): Coord {
        val untried = view.untried.toSet()
        val around = view.openHits.flatMap(Coord::neighbours).filter { it in untried }.distinct()
        val alongLines = around.filter { cell ->
            view.openHits.any { hit -> hit in cell.neighbours() && hit.beyond(cell) in view.openHits }
        }
        return alongLines.ifEmpty { around }.ifEmpty { view.untried }.random(random)
    }
}

/**
 * Counts, for every untried cell, the ways the ships still afloat could cover it given what's known. With open hits,
 * only positions through them count, more for each hit they cover; without, it hunts on a checkerboard spaced by the
 * shortest ship left. The best cell wins; ties are random.
 */
object HardAi : Ai {
    /** How much more a position through an open hit counts, per hit. */
    private const val HIT_WEIGHT = 10

    override fun next(view: TargetView, random: Random): Coord {
        val scores = scores(view)
        val hunting = view.openHits.isEmpty()
        val spacing = view.remainingLengths.minOrNull() ?: 1
        val parity = view.untried.filter { (it.row + it.col) % spacing == 0 && scores.getOrDefault(it, 0) > 0 }
        val candidates = if (hunting && parity.isNotEmpty()) parity else view.untried
        val best = candidates.maxOf { scores.getOrDefault(it, 0) }
        return candidates.filter { scores.getOrDefault(it, 0) == best }.random(random)
    }

    private fun scores(view: TargetView): Map<Coord, Int> {
        val scores = mutableMapOf<Coord, Int>()
        for (cells in positions(view)) {
            val hits = cells.count { it in view.openHits }
            if (view.openHits.isNotEmpty() && hits == 0) continue
            val weight = 1 + HIT_WEIGHT * hits
            cells.filter { it !in view.openHits }.forEach { scores[it] = scores.getOrDefault(it, 0) + weight }
        }
        return scores
    }

    /** Every position each ship still afloat could take: on the grid, clear of misses and sunk ships. */
    private fun positions(view: TargetView): List<List<Coord>> {
        val blocked = view.misses + view.sunk
        val origins = (0 until view.side).flatMap { row -> (0 until view.side).map { Coord(row, it) } }
        return view.remainingLengths.flatMap { length ->
            origins.flatMap { origin ->
                listOf(Coord(0, 1), Coord(1, 0)).map { step ->
                    List(length) { Coord(origin.row + step.row * it, origin.col + step.col * it) }
                }
            }
        }.filter { cells -> cells.all { it.isOn(view.side) && it !in blocked } }
    }
}

private fun Coord.neighbours(): List<Coord> =
    listOf(Coord(row - 1, col), Coord(row + 1, col), Coord(row, col - 1), Coord(row, col + 1))

/** The cell on the far side of this one from [from]: (0, 2).beyond((0, 1)) is (0, 3). */
private fun Coord.beyond(from: Coord): Coord = Coord(2 * row - from.row, 2 * col - from.col)

fun aiFor(opponent: Opponent): Ai = when (opponent) {
    Opponent.EASY -> EasyAi
    Opponent.MEDIUM -> MediumAi
    Opponent.HARD -> HardAi
    Opponent.TWO_PLAYER -> error("No AI in pass-and-play")
}

/** The AI's random choices for this turn: from the game's seed and the battle moves so far. Never change it. */
fun aiRandom(seed: Long, state: GameState): Random = Random(seed * AI_SEED_FACTOR + state.actions)

private const val AI_SEED_FACTOR = 31
