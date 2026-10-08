package io.github.vinaooo.battlegrid.feature.game.board

import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShotResult

/**
 * What a grid shows: the ships the viewer may see, the shots shown so far with what each found, the salvo's marks,
 * the hint's cells, and the last shot shown.
 */
internal data class GridView(
    val side: Int,
    val ships: List<Ship>,
    val shots: List<Pair<Coord, ShotResult>>,
    val marks: List<Coord> = emptyList(),
    val hint: List<Coord> = emptyList(),
) {
    val last: Coord? get() = shots.lastOrNull()?.first
}

/** [grid] as its shots shown so far left it: the newest [hidden] are still held back, showing as marks. */
private fun Grid.shown(hidden: Int): Grid = copy(shots = shots.dropLast(hidden))

private fun Grid.results(): List<Pair<Coord, ShotResult>> = shots.map { it to checkNotNull(resultAt(it)) }

/** The viewer's own waters: their whole fleet and every shot shown. */
internal fun ownView(grid: Grid, hidden: Int): GridView {
    val shown = grid.shown(hidden)
    return GridView(grid.size.side, grid.ships.filterNotNull(), shown.results(), grid.shots.takeLast(hidden))
}

/**
 * The waters fired at: only the ships sunk by the shots shown, so the fleet never shows before its time, unless
 * [revealAll] (the game is over). The hint's cells all look alike.
 */
internal fun targetView(grid: Grid, hidden: Int, marks: List<Coord>, hint: List<Coord>, revealAll: Boolean): GridView {
    val shown = grid.shown(hidden)
    val ships = if (revealAll) {
        grid.ships.filterNotNull()
    } else {
        grid.ships.indices.filter(shown::isSunk).map {
            grid.ships[it]!!
        }
    }
    return GridView(grid.size.side, ships, shown.results(), marks + grid.shots.takeLast(hidden), hint)
}
