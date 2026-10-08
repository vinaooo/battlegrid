package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.feature.game.board.GridGeometry
import io.github.vinaooo.battlegrid.feature.game.board.PlacementLayout
import io.github.vinaooo.battlegrid.feature.game.board.battleLayout
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt
import org.junit.jupiter.api.Test

class GeometryTest {
    @Test
    fun `a grid keeps a label band above and left, the cells share the rest`() {
        val grid = GridGeometry(side = 10, sizePx = 530f)
        grid.cellPx shouldBe (50f plusOrMinus 0.01f)
        grid.labelPx shouldBe (30f plusOrMinus 0.01f)
        grid.cellOrigin(Coord(2, 3)).px() shouldBe (180 to 130)
    }

    @Test
    fun `a point finds its cell, and nothing on the labels or outside`() {
        val grid = GridGeometry(side = 10, sizePx = 530f)
        grid.cellAt(31f, 31f) shouldBe Coord(0, 0)
        grid.cellAt(529f, 529f) shouldBe Coord(9, 9)
        grid.cellAt(185f, 130f) shouldBe Coord(2, 3)
        grid.cellAt(29f, 100f) shouldBe null
        grid.cellAt(100f, 29f) shouldBe null
        grid.cellAt(530f, 100f) shouldBe null
        grid.cellAt(100f, -1f) shouldBe null
    }

    @Test
    fun `the dock lays the fleet in rows under the grid, a cell apart`() {
        val ten = PlacementLayout(BoardSize.TEN, widthPx = 530f, heightPx = 2_000f)
        ten.dockRows shouldBe 2
        ten.grid.cellPx shouldBe (50f plusOrMinus 0.01f)
        // Row one: carrier and battleship; row two: cruiser, submarine, destroyer.
        ten.dockSlot(0).px() shouldBe (30 to 555)
        ten.dockSlot(1).px() shouldBe (330 to 555)
        ten.dockSlot(2).px() shouldBe (30 to 630)
        ten.dockSlot(4).px() shouldBe (430 to 630)
        PlacementLayout(BoardSize.EIGHT, 530f, 2_000f).dockRows shouldBe 3
        PlacementLayout(BoardSize.TWELVE, 530f, 2_000f).dockRows shouldBe 3
    }

    @Test
    fun `grid and dock shrink together to fit a short screen`() {
        val short = PlacementLayout(BoardSize.TEN, widthPx = 530f, heightPx = 530f)
        // 10.6 cells of grid, half a cell, two dock rows and half a cell between them: 13.6 cells.
        short.grid.cellPx shouldBe (530f / 13.6f plusOrMinus 0.01f)
        short.heightPx shouldBe (530f plusOrMinus 0.01f)
    }

    @Test
    fun `a dropped ship lands with the grabbed cell under the finger`() {
        val layout = PlacementLayout(BoardSize.TEN, 530f, 2_000f)
        // Grabbed by its third cell, dropped with the finger over (4, 6).
        layout.dropOrigin(330f + 5f, 230f + 5f, grabbed = 2, Orientation.HORIZONTAL) shouldBe Coord(4, 4)
        layout.dropOrigin(330f + 5f, 230f + 5f, grabbed = 2, Orientation.VERTICAL) shouldBe Coord(2, 6)
        // Hanging over the edge still gives an origin: the rules refuse it, not the geometry.
        layout.dropOrigin(31f, 31f, grabbed = 3, Orientation.HORIZONTAL) shouldBe Coord(0, -3)
        layout.dropOrigin(10f, 600f, grabbed = 0, Orientation.HORIZONTAL) shouldBe null
    }

    @Test
    fun `portrait gives the big grid the full width and the mini grid what's left`() {
        val roomy = battleLayout(widthPx = 400f, heightPx = 700f, landscape = false, gapPx = 10f, minMiniPx = 100f)
        roomy.bigPx shouldBe 400f
        roomy.miniPx shouldBe 180f // 45% of the width at most
        val tight = battleLayout(widthPx = 400f, heightPx = 560f, landscape = false, gapPx = 10f, minMiniPx = 100f)
        tight.bigPx shouldBe 400f
        tight.miniPx shouldBe 150f
        // No room left for the mini grid's minimum: the big grid gives way only then.
        val cramped = battleLayout(widthPx = 400f, heightPx = 450f, landscape = false, gapPx = 10f, minMiniPx = 100f)
        cramped.miniPx shouldBe 100f
        cramped.bigPx shouldBe 340f
    }

    @Test
    fun `landscape puts both grids side by side, the same size`() {
        val wide = battleLayout(widthPx = 900f, heightPx = 400f, landscape = true, gapPx = 20f, minMiniPx = 100f)
        wide.bigPx shouldBe 400f
        wide.miniPx shouldBe 400f
        val narrow = battleLayout(widthPx = 620f, heightPx = 400f, landscape = true, gapPx = 20f, minMiniPx = 100f)
        narrow.bigPx shouldBe 300f
    }

    private fun Pair<Float, Float>.px() = first.roundToInt() to second.roundToInt()
}
