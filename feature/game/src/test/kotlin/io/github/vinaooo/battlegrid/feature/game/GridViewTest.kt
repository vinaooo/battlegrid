package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.feature.game.board.ownView
import io.github.vinaooo.battlegrid.feature.game.board.targetView
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GridViewTest {
    private val fleet = BoardSize.TEN.fleet.mapIndexed { i, type ->
        Ship(type, Coord(2 * i, 0), Orientation.HORIZONTAL)
    }

    /** A miss, a hit on the carrier, the destroyer sunk. */
    private val grid = Grid(BoardSize.TEN, fleet, listOf(Coord(1, 0), Coord(0, 0), Coord(8, 0), Coord(8, 1)))

    @Test
    fun `the player's own grid shows every ship and every shot`() {
        val view = ownView(grid, hidden = 0)
        view.ships shouldBe fleet
        view.shots shouldBe listOf(
            Coord(1, 0) to ShotResult.MISS,
            Coord(0, 0) to ShotResult.HIT,
            Coord(8, 0) to ShotResult.SUNK,
            Coord(8, 1) to ShotResult.SUNK,
        )
        view.last shouldBe Coord(8, 1)
    }

    @Test
    fun `the target grid shows only sunk ships, never where the others are`() {
        val view = targetView(grid, hidden = 0, marks = emptyList(), hint = emptyList(), revealAll = false)
        view.ships shouldBe listOf(fleet[4])
    }

    @Test
    fun `held-back salvo shots are not shown, and a ship they sink is not shown sunk yet`() {
        val view = targetView(grid, hidden = 1, marks = emptyList(), hint = emptyList(), revealAll = false)
        view.ships shouldBe emptyList()
        view.shots.last() shouldBe (Coord(8, 0) to ShotResult.HIT)
        view.last shouldBe Coord(8, 0)
        // Until it shows, the held-back shot stays marked.
        view.marks shouldBe listOf(Coord(8, 1))
        ownView(grid, hidden = 2).marks shouldBe listOf(Coord(8, 0), Coord(8, 1))
    }

    @Test
    fun `a finished game shows the whole enemy fleet`() {
        targetView(grid, 0, emptyList(), emptyList(), revealAll = true).ships shouldBe fleet
    }

    @Test
    fun `marks and the hint's cells show as they are, all alike`() {
        val hint = listOf(Coord(5, 5), Coord(0, 1), Coord(9, 9))
        val view = targetView(grid, 0, listOf(Coord(3, 3)), hint, revealAll = false)
        view.marks shouldBe listOf(Coord(3, 3))
        view.hint shouldBe hint
        // A hint cell on a ship looks like the others: no ship shows there.
        view.ships.none { Coord(0, 1) in it.cells } shouldBe true
    }

    @Test
    fun `no shot yet, nothing highlighted`() {
        ownView(grid.copy(shots = emptyList()), 0).last shouldBe null
    }
}
