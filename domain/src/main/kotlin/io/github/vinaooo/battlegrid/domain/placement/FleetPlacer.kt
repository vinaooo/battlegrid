package io.github.vinaooo.battlegrid.domain.placement

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import kotlin.random.Random

/** Places a whole fleet: the AI's, and the player's Random button. */
interface FleetPlacer {
    fun place(size: BoardSize, random: Random): List<Ship>
}

/**
 * Each ship, largest first, at a random spot among every spot still free for it. Never retries: the boards' fleets
 * always leave room, so the same seed gives the same fleet on every device.
 */
object RandomFleetPlacer : FleetPlacer {
    override fun place(size: BoardSize, random: Random): List<Ship> {
        var grid = Grid.empty(size)
        size.fleet.forEachIndexed { index, type ->
            val spots = size.cells.flatMap { origin -> Orientation.entries.map { Ship(type, origin, it) } }
            grid = grid.place(index, spots.filter { grid.fits(index, it) }.random(random))
        }
        return grid.ships.map { it!! }
    }
}
