package io.github.vinaooo.battlegrid.feature.game.board

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.battlegrid.feature.game.ui.cellName
import io.github.vinaooo.battlegrid.feature.game.ui.shipName
import kotlin.math.roundToInt

/**
 * The fleet being placed: the grid, and each ship either on it or in its dock slot below. Drag a ship to place or
 * move it (it lands with the grabbed cell under the finger); tap a placed ship to turn it. A refused drop leaves the
 * ship where it was. TalkBack moves ships with custom actions.
 */
@Composable
internal fun PlacementBoard(
    grid: Grid,
    onPlace: (Int, Ship) -> Unit,
    onRotate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The ship being dragged and how far: kept outside BoxWithConstraints, which may rebuild its content.
    var dragged by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val layout = PlacementLayout(grid.size, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val gridDp = with(density) { layout.grid.sizePx.toDp() }
        Box(Modifier.size(gridDp, with(density) { layout.heightPx.toDp() })) {
            SeaGrid(GridView(grid.size.side, emptyList(), emptyList()), Modifier.size(gridDp), nodes = false)
            grid.size.fleet.indices.forEach { index ->
                FleetShip(
                    grid = grid,
                    index = index,
                    layout = layout,
                    drag = dragged?.takeIf { it.first == index }?.second,
                    onDrag = { dragged = it?.let { offset -> index to offset } },
                    onPlace = onPlace,
                    onRotate = onRotate,
                )
            }
        }
    }
}

/**
 * Ship [index] of the fleet, on the grid or in its dock slot, moved by [drag] while dragged. A drop sends where its
 * bow lands; a tap on a placed ship turns it.
 */
@Composable
private fun FleetShip(
    grid: Grid,
    index: Int,
    layout: PlacementLayout,
    drag: Offset?,
    onDrag: (Offset?) -> Unit,
    onPlace: (Int, Ship) -> Unit,
    onRotate: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val type = grid.size.fleet[index]
    val placed = grid.ships[index]
    val orientation = placed?.orientation ?: Orientation.HORIZONTAL
    val cellPx = layout.grid.cellPx
    val (x, y) = placed?.let { layout.grid.cellOrigin(it.origin) } ?: layout.dockSlot(index)
    val long = with(density) { (type.length * cellPx).toDp() }
    val short = with(density) { cellPx.toDp() }
    val description = shipDescription(type, placed)
    val actions = shipActions(grid, index, placed, onPlace, onRotate)
    val horizontal = orientation == Orientation.HORIZONTAL
    Box(
        Modifier
            .offset {
                val moved = drag ?: Offset.Zero
                IntOffset((x + moved.x).roundToInt(), (y + moved.y).roundToInt())
            }
            .zIndex(if (drag != null) 1f else 0f)
            .size(if (horizontal) long else short, if (horizontal) short else long)
            .scale(if (drag != null) LIFTED else 1f)
            .clearAndSetSemantics {
                contentDescription = description
                customActions = actions
            }
            .pointerInput(index, placed) { detectTapGestures { if (placed != null) onRotate(index) } }
            .shipDrag(
                key = placed,
                cellAt = { grabbedCell(it, horizontal, cellPx, type.length) },
                onDrag = onDrag,
            ) { start, grabbed ->
                val finger = Offset(x, y) + start
                layout.dropOrigin(finger.x, finger.y, grabbed, orientation)
                    ?.let { onPlace(index, Ship(type, it, orientation)) }
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(with(density) { (cellPx * SHIP_INSET).toDp() })
                .background(LocalBoardColors.current.ship, CircleShape),
        )
    }
}

/**
 * Drags a ship: [onDrag] with how far it moved (null once let go), then [onDrop] with where the finger was let go
 * (relative to the ship's resting corner) and which of its cells ([cellAt] a point on it) was grabbed.
 */
private fun Modifier.shipDrag(
    key: Any?,
    cellAt: (Offset) -> Int,
    onDrag: (Offset?) -> Unit,
    onDrop: (finger: Offset, grabbed: Int) -> Unit,
): Modifier = pointerInput(key) {
    // Where the drag began on the ship, which of its cells that is, and how far the finger went since.
    var start = Offset.Zero
    var grabbed = 0
    var travelled = Offset.Zero
    var slop = Offset.Zero
    detectDragGestures(
        onDragStart = {
            start = it
            travelled = Offset.Zero
            slop = Offset.Zero
            grabbed = cellAt(it)
            onDrag(Offset.Zero)
        },
        onDrag = { change, amount ->
            change.consume()
            if (travelled == Offset.Zero) slop = amount.touchSlop(viewConfiguration.touchSlop)
            travelled += amount
            // The ship follows the finger from where it went down, the slop included.
            onDrag(travelled + slop)
        },
        onDragEnd = {
            onDrag(null)
            onDrop(start + travelled, grabbed)
        },
        onDragCancel = { onDrag(null) },
    )
}

/** Which cell of a ship [length] cells long the point [at] on it falls in. */
private fun grabbedCell(at: Offset, horizontal: Boolean, cellPx: Float, length: Int): Int =
    ((if (horizontal) at.x else at.y) / cellPx).toInt().coerceIn(0, length - 1)

/** "Cruiser, 3 cells, at B7, vertical", or "not placed". */
@Composable
private fun shipDescription(type: ShipClass, placed: Ship?): String {
    if (placed == null) return stringResource(R.string.ship_in_dock, shipName(type), type.length)
    val orientation = if (placed.orientation == Orientation.HORIZONTAL) R.string.horizontal else R.string.vertical
    return stringResource(
        R.string.ship_placed,
        shipName(type),
        type.length,
        cellName(placed.origin),
        stringResource(orientation),
    )
}

/** TalkBack's ways to place a ship: on the grid from the dock, then moved a cell at a time or turned. */
@Composable
private fun shipActions(
    grid: Grid,
    index: Int,
    placed: Ship?,
    onPlace: (Int, Ship) -> Unit,
    onRotate: (Int) -> Unit,
): List<CustomAccessibilityAction> {
    if (placed == null) {
        val label = stringResource(R.string.place_on_grid)
        val type = grid.size.fleet[index]
        return listOf(
            CustomAccessibilityAction(label) {
                val spot = grid.size.cells.asSequence()
                    .flatMap { origin -> Orientation.entries.map { Ship(type, origin, it) } }
                    .firstOrNull { grid.fits(index, it) }
                spot?.let { onPlace(index, it) }
                spot != null
            },
        )
    }
    fun move(label: String, rows: Int, cols: Int) = CustomAccessibilityAction(label) {
        val moved = placed.copy(origin = Coord(placed.origin.row + rows, placed.origin.col + cols))
        grid.fits(index, moved).also { if (it) onPlace(index, moved) }
    }
    return listOf(
        CustomAccessibilityAction(stringResource(R.string.rotate)) {
            onRotate(index)
            true
        },
        move(stringResource(R.string.move_up), -1, 0),
        move(stringResource(R.string.move_down), 1, 0),
        move(stringResource(R.string.move_left), 0, -1),
        move(stringResource(R.string.move_right), 0, 1),
    )
}

/**
 * The touch slop the finger travelled before the drag began, which the first drag amount leaves out: along that
 * amount's direction. Adding it back keeps the ship under the finger.
 */
private fun Offset.touchSlop(touchSlop: Float): Offset {
    val distance = getDistance()
    return if (distance == 0f) Offset.Zero else this / distance * touchSlop
}

private const val LIFTED = 1.06f
