package io.github.vinaooo.battlegrid.feature.game.board

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShotResult

/**
 * A grid of the sea drawn on one canvas: labels, cells, the ships in [view], shots, salvo marks and hint cells, the
 * last shot springing in. With [nodes], TalkBack gets one node per cell in reading order ([cellDescription]); a cell
 * [onTap] accepts says so with a click action.
 */
@Composable
internal fun SeaGrid(
    view: GridView,
    modifier: Modifier = Modifier,
    onTap: ((Coord) -> Unit)? = null,
    tappable: (Coord) -> Boolean = { false },
    nodes: Boolean = true,
    cellDescription: @Composable (Coord) -> String = { "" },
) {
    val colors = LocalBoardColors.current
    val measurer = rememberTextMeasurer()
    // Kept outside BoxWithConstraints, which may rebuild its content.
    val landing = remember(view.last) { Animatable(if (view.last == null) 1f else 0f) }
    val spring = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(view.last) { landing.animateTo(1f, spring) }
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val sizePx = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val geometry = GridGeometry(view.side, sizePx)
        Canvas(
            Modifier.fillMaxSize().pointerInput(view.side, sizePx, onTap) {
                if (onTap != null) detectTapGestures { geometry.cellAt(it.x, it.y)?.let(onTap) }
            },
        ) {
            drawSea(geometry, colors)
            drawLabels(geometry, colors, measurer)
            view.hint.forEach { drawHint(geometry, it, colors) }
            view.ships.forEach { drawShip(geometry, it, colors) }
            view.marks.forEach { drawMark(geometry, it, colors) }
            view.shots.forEach { (coord, result) ->
                val scale = if (coord == view.last) landing.value else 1f
                val color = when {
                    result == ShotResult.MISS -> colors.miss
                    view.ships.any { coord in it.cells } -> colors.hitOnShip
                    else -> colors.hit
                }
                drawShot(geometry, coord, result == ShotResult.MISS, color, scale)
            }
        }
        if (nodes) CellNodes(geometry, onTap, tappable, cellDescription)
    }
}

/** One invisible node per cell over the canvas, row by row, for TalkBack: the label band, then the cells. */
@Composable
private fun CellNodes(
    geometry: GridGeometry,
    onTap: ((Coord) -> Unit)?,
    tappable: (Coord) -> Boolean,
    cellDescription: @Composable (Coord) -> String,
) {
    val density = LocalDensity.current
    val cellDp = with(density) { geometry.cellPx.toDp() }
    val labelDp = with(density) { geometry.labelPx.toDp() }
    Column(Modifier.padding(start = labelDp, top = labelDp)) {
        for (row in 0 until geometry.side) {
            Row {
                for (col in 0 until geometry.side) {
                    val coord = Coord(row, col)
                    val description = cellDescription(coord)
                    Box(
                        Modifier.size(cellDp).semantics {
                            contentDescription = description
                            if (onTap != null && tappable(coord)) {
                                onClick {
                                    onTap(coord)
                                    true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSea(geometry: GridGeometry, colors: BoardColors) {
    val start = Offset(geometry.labelPx, geometry.labelPx)
    val side = geometry.sizePx - geometry.labelPx
    drawRoundRect(colors.sea, start, Size(side, side), CornerRadius(geometry.cellPx * CORNER))
    for (i in 1 until geometry.side) {
        val at = geometry.labelPx + i * geometry.cellPx
        drawLine(colors.line, Offset(at, geometry.labelPx), Offset(at, geometry.sizePx), LINE.dp.toPx())
        drawLine(colors.line, Offset(geometry.labelPx, at), Offset(geometry.sizePx, at), LINE.dp.toPx())
    }
}

private fun DrawScope.drawLabels(
    geometry: GridGeometry,
    colors: BoardColors,
    measurer: androidx.compose.ui.text.TextMeasurer,
) {
    val style = TextStyle(color = colors.label, fontSize = (geometry.labelPx * LABEL_SIZE).toSp())
    for (i in 0 until geometry.side) {
        val center = geometry.labelPx + (i + HALF) * geometry.cellPx
        val letter = measurer.measure("${'A' + i}", style)
        drawText(
            letter,
            topLeft = Offset(center - letter.size.width / 2f, (geometry.labelPx - letter.size.height) / 2f),
        )
        val number = measurer.measure("${i + 1}", style)
        drawText(
            number,
            topLeft = Offset((geometry.labelPx - number.size.width) / 2f, center - number.size.height / 2f),
        )
    }
}

private fun DrawScope.cellCenter(geometry: GridGeometry, coord: Coord): Offset {
    val (x, y) = geometry.cellOrigin(coord)
    return Offset(x + geometry.cellPx / 2, y + geometry.cellPx / 2)
}

/** A ship: a capsule over its cells. */
private fun DrawScope.drawShip(geometry: GridGeometry, ship: Ship, colors: BoardColors) {
    val inset = geometry.cellPx * SHIP_INSET
    val (x, y) = geometry.cellOrigin(ship.origin)
    val (last, lastY) = geometry.cellOrigin(ship.cells.last())
    val topLeft = Offset(x + inset, y + inset)
    val size = Size(last + geometry.cellPx - inset - topLeft.x, lastY + geometry.cellPx - inset - topLeft.y)
    drawRoundRect(colors.ship, topLeft, size, CornerRadius(minOf(size.width, size.height) / 2))
}

/** A salvo mark: a crosshair ring. */
private fun DrawScope.drawMark(geometry: GridGeometry, coord: Coord, colors: BoardColors) {
    val center = cellCenter(geometry, coord)
    val radius = geometry.cellPx * MARK_RADIUS
    drawCircle(colors.mark, radius, center, style = Stroke(width = geometry.cellPx * STROKE))
    drawCircle(colors.mark, radius * MARK_DOT, center)
}

/** A hint cell: a rounded frame, the same on all five. */
private fun DrawScope.drawHint(geometry: GridGeometry, coord: Coord, colors: BoardColors) {
    val (x, y) = geometry.cellOrigin(coord)
    val inset = geometry.cellPx * HINT_INSET
    drawRoundRect(
        colors.hint,
        Offset(x + inset, y + inset),
        Size(geometry.cellPx - 2 * inset, geometry.cellPx - 2 * inset),
        CornerRadius(geometry.cellPx * CORNER),
        style = Stroke(width = geometry.cellPx * STROKE),
    )
}

/** A miss: a small ring. A hit: a burst (in the ship's contrast color where a ship shows). */
private fun DrawScope.drawShot(geometry: GridGeometry, coord: Coord, miss: Boolean, color: Color, scale: Float) {
    val center = cellCenter(geometry, coord)
    if (miss) {
        drawCircle(color, geometry.cellPx * MISS_RADIUS * scale, center, style = Stroke(geometry.cellPx * STROKE))
        return
    }
    val radius = geometry.cellPx * HIT_RADIUS * scale
    drawCircle(color, radius * CORE, center)
    repeat(SPOKES) { i ->
        val angle = Math.toRadians(i * FULL_TURN / SPOKES.toDouble())
        val direction = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
        drawLine(color, center + direction * radius * CORE, center + direction * radius, geometry.cellPx * STROKE)
    }
}

private const val HALF = 0.5f
private const val LINE = 1
private const val CORNER = 0.15f
private const val LABEL_SIZE = 0.6f

/** How far a ship stays inside its cells, as a fraction of a cell (the grid's and the dock's). */
internal const val SHIP_INSET = 0.12f

private const val HINT_INSET = 0.08f
private const val MARK_RADIUS = 0.28f
private const val MISS_RADIUS = 0.16f
private const val HIT_RADIUS = 0.36f
private const val CORE = 0.5f
private const val MARK_DOT = 0.33f
private const val STROKE = 0.08f
private const val SPOKES = 8
private const val FULL_TURN = 360.0
