package io.github.vinaooo.battlegrid.feature.game.board

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Orientation

/** A label band ([LABEL_CELLS] of a cell) above the grid (letters) and left of it (numbers). */
private const val LABEL_CELLS = 0.6f

/** A [side]×[side] grid drawn [sizePx] wide, labels included: which cell a point falls in, and where cells are. */
internal class GridGeometry(val side: Int, val sizePx: Float) {
    val cellPx: Float = sizePx / (side + LABEL_CELLS)
    val labelPx: Float = cellPx * LABEL_CELLS

    /** The cell under ([x], [y]), or null on the labels or outside. */
    fun cellAt(x: Float, y: Float): Coord? {
        val col = ((x - labelPx) / cellPx).toInt()
        val row = ((y - labelPx) / cellPx).toInt()
        val inside = x >= labelPx && y >= labelPx && x < sizePx && y < sizePx
        return if (inside) Coord(row, col) else null
    }

    /** The top-left corner of [coord], as (x, y). */
    fun cellOrigin(coord: Coord): Pair<Float, Float> = labelPx + coord.col * cellPx to labelPx + coord.row * cellPx
}

/**
 * The placement screen: the grid on top, and below it a dock holding each ship of the fleet in its own slot, in rows
 * as wide as the grid, a cell between ships and half a cell between rows. Grid and dock share one cell size, the
 * largest that fits [widthPx] × [heightPx].
 */
internal class PlacementLayout(size: BoardSize, widthPx: Float, heightPx: Float) {
    private val slots: List<Pair<Int, Int>> // (row, first cell) of each ship's dock slot
    val dockRows: Int

    init {
        var row = 0
        var cell = 0
        slots = size.fleet.map { type ->
            if (cell > 0 && cell + type.length > size.side) {
                row++
                cell = 0
            }
            (row to cell).also { cell += type.length + 1 }
        }
        dockRows = row + 1
    }

    private val cells = size.side + LABEL_CELLS + DOCK_GAP + dockRows + DOCK_GAP * (dockRows - 1)
    val grid = GridGeometry(size.side, minOf(widthPx, heightPx * (size.side + LABEL_CELLS) / cells))

    /** The height grid and dock take. */
    val heightPx: Float get() = grid.cellPx * cells

    /** The top-left corner of ship [index]'s dock slot, laid horizontally. */
    fun dockSlot(index: Int): Pair<Float, Float> {
        val (row, cell) = slots[index]
        val top = grid.sizePx + grid.cellPx * (DOCK_GAP + row * (1 + DOCK_GAP))
        return grid.labelPx + cell * grid.cellPx to top
    }

    /** Where a ship's bow goes when dropped with its [grabbed] cell under the finger at ([x], [y]); null off grid. */
    fun dropOrigin(x: Float, y: Float, grabbed: Int, orientation: Orientation): Coord? {
        val cell = grid.cellAt(x, y) ?: return null
        return if (orientation == Orientation.HORIZONTAL) {
            Coord(cell.row, cell.col - grabbed)
        } else {
            Coord(cell.row - grabbed, cell.col)
        }
    }

    private companion object {
        const val DOCK_GAP = 0.5f
    }
}

internal data class BattleLayout(val bigPx: Float, val miniPx: Float)

/**
 * The battle's two grids. Landscape: side by side, the same size. Portrait: the big grid takes the full width, the
 * mini grid what height is left (at most [MINI_SHARE] of the width); the big grid shrinks only when the mini grid
 * would fall under [minMiniPx].
 */
internal fun battleLayout(
    widthPx: Float,
    heightPx: Float,
    landscape: Boolean,
    gapPx: Float,
    minMiniPx: Float,
): BattleLayout {
    if (landscape) {
        val each = minOf(heightPx, (widthPx - gapPx) / 2)
        return BattleLayout(each, each)
    }
    val big = minOf(widthPx, heightPx - gapPx - minMiniPx)
    return BattleLayout(big, minOf(widthPx * MINI_SHARE, heightPx - big - gapPx))
}

private const val MINI_SHARE = 0.45f
