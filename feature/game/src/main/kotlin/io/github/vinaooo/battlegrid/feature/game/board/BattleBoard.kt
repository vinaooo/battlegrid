package io.github.vinaooo.battlegrid.feature.game.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.feature.game.GameUiState
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.battlegrid.feature.game.ui.cellName
import io.github.vinaooo.battlegrid.feature.game.ui.cellResult
import io.github.vinaooo.battlegrid.feature.game.ui.shipName

/**
 * The battle: the target grid, which takes the shots, and the viewer's own. Landscape shows both side by side at the
 * same size; portrait shows the target big and the own grid small above it (one TalkBack item with a summary).
 */
@Composable
internal fun BattleBoard(ui: GameUiState, landscape: Boolean, onTap: (Coord) -> Unit, modifier: Modifier = Modifier) {
    val state = ui.session?.state ?: return
    val target = state.gridOf(ui.viewer.other)
    val own = state.gridOf(ui.viewer)
    val (targetView, ownView) = views(ui)
    BoxWithConstraints(modifier) {
        // The grids never trade places (user's choice): the target is always the big one.
        val slots = slots(landscape)
        Box(slots.big.place()) {
            SeaGrid(
                targetView,
                Modifier.size(slots.big.size),
                onTap = if (ui.canFire) onTap else null,
                tappable = { !target.isTried(it) },
                cellDescription = { targetCell(targetView, it) },
            )
        }
        val ownSummary = stringResource(R.string.your_fleet, own.shipsAfloat, own.ships.size)
        Box(
            slots.mini.place().let {
                if (landscape) it else it.clearAndSetSemantics { contentDescription = ownSummary }
            },
        ) {
            SeaGrid(ownView, Modifier.size(slots.mini.size), nodes = landscape, cellDescription = {
                ownCell(ownView, it)
            })
        }
    }
}

/** What the viewer sees of the target grid and of their own: shots held back show as marks, hints only vs AI. */
private fun views(ui: GameUiState): Pair<GridView, GridView> {
    val state = checkNotNull(ui.session).state
    val viewer = ui.viewer
    val hiddenOn = { side: Side -> if (ui.revealing == side) ui.hiddenShots else 0 }
    val marks = if (state.toMove == viewer) state.salvoMarks else emptyList()
    val hint = if (state.mode.isVsAi) state.hint else emptyList()
    val target = targetView(state.gridOf(viewer.other), hiddenOn(viewer.other), marks, hint, revealAll = state.isOver)
    return target to ownView(state.gridOf(viewer), hiddenOn(viewer))
}

/** Where a grid sits and how big it is. */
private data class Slot(val at: DpOffset, val size: Dp) {
    fun place(): Modifier = Modifier.offset(at.x, at.y).size(size)
}

private data class Slots(val big: Slot, val mini: Slot)

/** Landscape: side by side, centered. Portrait: the small grid on top, the big one under it. */
private fun BoxWithConstraintsScope.slots(landscape: Boolean): Slots {
    val gap = GAP.dp
    val layout = battleLayout(maxWidth.value, maxHeight.value, landscape, gap.value, MIN_MINI.toFloat())
    val big = layout.bigPx.dp
    val mini = layout.miniPx.dp
    return if (landscape) {
        val left = (maxWidth - big * 2 - gap) / 2
        val top = (maxHeight - big) / 2
        Slots(Slot(DpOffset(left, top), big), Slot(DpOffset(left + big + gap, top), mini))
    } else {
        val top = (maxHeight - big - mini - gap) / 2
        Slots(
            Slot(DpOffset((maxWidth - big) / 2, top + mini + gap), big),
            Slot(DpOffset((maxWidth - mini) / 2, top), mini),
        )
    }
}

/** A target cell for TalkBack: untried, marked, a hint, or what a shot found. */
@Composable
private fun targetCell(view: GridView, coord: Coord): String {
    val shot = view.shots.firstOrNull { it.first == coord }
    val state = when {
        shot != null -> cellResult(shot.second, view.ships.firstOrNull { coord in it.cells }?.type)
        coord in view.marks -> stringResource(R.string.cell_marked)
        coord in view.hint -> stringResource(R.string.cell_hint)
        else -> stringResource(R.string.cell_untried)
    }
    return stringResource(R.string.cell_state, cellName(coord), state)
}

/** A cell of the viewer's own grid: its ship or water, and what a shot there found. */
@Composable
private fun ownCell(view: GridView, coord: Coord): String {
    val ship = view.ships.firstOrNull { coord in it.cells }
    val what = ship?.let { shipName(it.type) } ?: stringResource(R.string.cell_water)
    val shot = view.shots.firstOrNull { it.first == coord }?.second
    val result = when (shot) {
        null -> what
        ShotResult.MISS -> stringResource(R.string.cell_miss)
        else -> stringResource(R.string.cell_state, what, cellResult(shot, ship?.type))
    }
    return stringResource(R.string.cell_state, cellName(coord), result)
}

private const val GAP = 12
private const val MIN_MINI = 96
