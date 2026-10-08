package io.github.vinaooo.battlegrid.domain.hint

import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Grid
import kotlin.random.Random

/** The player's hint: [CELLS] untried enemy cells, exactly one of them on a ship still afloat. */
object HintEngine {
    const val CELLS = 5
    const val MAX_HINTS = 3

    /** The hint's cells, shuffled; fewer when water runs short. Null when no ship cell is left untried. */
    fun cells(state: GameState, random: Random): List<Coord>? {
        val untried = state.enemy.untried
        val ship = untried.filter { state.enemy.shipAt(it) != null }.randomOrNull(random) ?: return null
        val water = untried.filter { state.enemy.shipAt(it) == null }.shuffled(random).take(CELLS - 1)
        return (water + ship).shuffled(random)
    }

    /** Whether [cells] are a hint [cells] could have given on [grid]. */
    fun isValid(grid: Grid, cells: List<Coord>): Boolean {
        val untried = grid.untried.toSet()
        val water = untried.count { grid.shipAt(it) == null }
        return cells.size == minOf(CELLS, water + 1) &&
            cells.distinct().size == cells.size &&
            cells.all { it in untried } &&
            cells.count { grid.shipAt(it) != null } == 1
    }

    /** The hint's random choices: from the game's seed and the hints used, so a hint survives process death. */
    fun random(seed: Long, state: GameState): Random = Random(seed * HINT_SEED_FACTOR + state.hintsUsed)

    private const val HINT_SEED_FACTOR = 37
}
