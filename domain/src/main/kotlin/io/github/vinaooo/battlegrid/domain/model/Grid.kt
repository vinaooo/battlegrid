package io.github.vinaooo.battlegrid.domain.model

import kotlinx.serialization.Serializable

/**
 * One side's waters: its fleet ([ships], one slot per class of the board's fleet, null until placed) and the
 * shots fired at it, in order. Hits, misses and sunk ships are worked out from those two, never stored.
 */
@Serializable
data class Grid(val size: BoardSize, val ships: List<Ship?>, val shots: List<Coord> = emptyList()) {
    init {
        // A save edited or written by another version must not load as a broken grid.
        require(ships.size == size.fleet.size) { "${ships.size} ship slots on a ${size.name} grid" }
        require(
            ships.withIndex().all { (i, ship) ->
                ship == null || ship.type == size.fleet[i]
            },
        ) { "Wrong ship class" }
        require(shots.all { it.isOn(size.side) }) { "A shot off the grid" }
    }

    val isFleetComplete: Boolean get() = ships.all { it != null }

    val shipsAfloat: Int get() = ships.indices.count { !isSunk(it) }

    val isFleetSunk: Boolean get() = shipsAfloat == 0

    val hits: Int get() = shots.count { shipAt(it) != null }

    val untried: List<Coord> get() = size.cells.filter { it !in shots }

    /** The index of the ship covering [coord], if any. */
    fun shipAt(coord: Coord): Int? = ships.indexOfFirst { it != null && coord in it.cells }.takeIf { it >= 0 }

    fun isSunk(index: Int): Boolean = ships[index]?.cells?.all { it in shots } ?: false

    fun isTried(coord: Coord): Boolean = coord in shots

    /** What a shot at [coord] found, or null while it's untried. A sunk ship's cells all read [ShotResult.SUNK]. */
    fun resultAt(coord: Coord): ShotResult? {
        if (!isTried(coord)) return null
        val ship = shipAt(coord) ?: return ShotResult.MISS
        return if (isSunk(ship)) ShotResult.SUNK else ShotResult.HIT
    }

    /** Whether [ship] can go in slot [index]: its class, inside the grid, not over another ship (touching is fine). */
    fun fits(index: Int, ship: Ship): Boolean = index in ships.indices &&
        ship.type == size.fleet[index] &&
        ship.cells.all { it.isOn(size.side) } &&
        ships.withIndex().none { (i, other) -> i != index && other != null && other.cells.any { it in ship.cells } }

    fun place(index: Int, ship: Ship): Grid = copy(ships = ships.toMutableList().also { it[index] = ship })

    /** Fires at [coord]: what it found, with the grid after it. */
    fun shoot(coord: Coord): Pair<Grid, ShotResult> {
        val after = copy(shots = shots + coord)
        val ship = shipAt(coord) ?: return after to ShotResult.MISS
        return after to if (after.isSunk(ship)) ShotResult.SUNK else ShotResult.HIT
    }

    companion object {
        fun empty(size: BoardSize): Grid = Grid(size, List(size.fleet.size) { null })
    }
}
