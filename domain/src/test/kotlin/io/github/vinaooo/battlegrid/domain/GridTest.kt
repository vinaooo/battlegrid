package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GridTest {
    private val full = Grid(BoardSize.TEN, rowFleet(BoardSize.TEN))

    @Test
    fun `fleets grow with the board`() {
        BoardSize.EIGHT.fleet.map { it.length } shouldBe listOf(5, 4, 3, 2)
        BoardSize.TEN.fleet.map { it.length } shouldBe listOf(5, 4, 3, 3, 2)
        BoardSize.TWELVE.fleet.map { it.length } shouldBe listOf(5, 4, 4, 3, 3, 2)
        BoardSize.entries.map { it.side } shouldBe listOf(8, 10, 12)
    }

    @Test
    fun `a ship runs right or down from its bow`() {
        Ship(ShipClass.CRUISER, c(1, 2), Orientation.HORIZONTAL).cells shouldBe listOf(c(1, 2), c(1, 3), c(1, 4))
        Ship(ShipClass.DESTROYER, c(1, 2), Orientation.VERTICAL).cells shouldBe listOf(c(1, 2), c(2, 2))
    }

    @Test
    fun `a ship fits inside the grid, in its own slot's class, beside but not over another`() {
        val grid = Grid.empty(BoardSize.EIGHT).place(3, Ship(ShipClass.DESTROYER, c(0, 0), Orientation.HORIZONTAL))
        val carrier = { row: Int, col: Int, o: Orientation -> Ship(ShipClass.CARRIER, c(row, col), o) }
        grid.fits(0, carrier(1, 0, Orientation.HORIZONTAL)) shouldBe true // touching the destroyer
        grid.fits(0, carrier(0, 1, Orientation.HORIZONTAL)) shouldBe false // over it
        grid.fits(0, carrier(0, 3, Orientation.HORIZONTAL)) shouldBe true // the last column
        grid.fits(0, carrier(0, 4, Orientation.HORIZONTAL)) shouldBe false // past it
        grid.fits(0, carrier(3, 7, Orientation.VERTICAL)) shouldBe true
        grid.fits(0, carrier(4, 7, Orientation.VERTICAL)) shouldBe false
        grid.fits(0, carrier(-1, 0, Orientation.HORIZONTAL)) shouldBe false
        grid.fits(1, carrier(5, 0, Orientation.HORIZONTAL)) shouldBe false // the battleship's slot
        grid.fits(4, carrier(5, 0, Orientation.HORIZONTAL)) shouldBe false // no such slot
        grid.fits(-1, carrier(5, 0, Orientation.HORIZONTAL)) shouldBe false
        // A ship may stay where it is when moved onto itself.
        grid.fits(3, Ship(ShipClass.DESTROYER, c(0, 1), Orientation.HORIZONTAL)) shouldBe true
    }

    @Test
    fun `shots find water, hits, and sink a ship once all its cells are hit`() {
        val (missed, miss) = full.shoot(c(1, 0))
        miss shouldBe ShotResult.MISS
        missed.resultAt(c(1, 0)) shouldBe ShotResult.MISS
        missed.resultAt(c(0, 0)) shouldBe null

        val (hit, first) = missed.shoot(c(8, 0))
        first shouldBe ShotResult.HIT
        hit.resultAt(c(8, 0)) shouldBe ShotResult.HIT
        hit.shipAt(c(8, 0)) shouldBe 4
        hit.shipAt(c(9, 0)) shouldBe null

        val (sunk, last) = hit.shoot(c(8, 1))
        last shouldBe ShotResult.SUNK
        sunk.resultAt(c(8, 0)) shouldBe ShotResult.SUNK
        sunk.isSunk(4) shouldBe true
        sunk.isSunk(3) shouldBe false
        sunk.shipsAfloat shouldBe 4
        sunk.hits shouldBe 2
        sunk.untried.size shouldBe 97
        sunk.isTried(c(1, 0)) shouldBe true
    }

    @Test
    fun `an unplaced ship is neither sunk nor complete`() {
        val grid = Grid.empty(BoardSize.EIGHT)
        grid.isFleetComplete shouldBe false
        grid.isSunk(0) shouldBe false
        grid.shipsAfloat shouldBe 4
        grid.isFleetSunk shouldBe false
        full.isFleetComplete shouldBe true
    }

    @Test
    fun `the fleet is sunk when every ship is`() {
        val sunk = full.ships.flatMap { it!!.cells }.fold(full) { grid, cell -> grid.shoot(cell).first }
        sunk.isFleetSunk shouldBe true
        sunk.shipsAfloat shouldBe 0
    }
}
