package io.github.vinaooo.battlegrid.feature.game.board

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
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
 * The battle: the target grid and the viewer's own. Landscape shows both side by side; portrait shows one big and
 * the other small above it, and a tap on the small one swaps them with the motion scheme's spring. Only the big
 * target grid takes shots.
 */
@Composable
internal fun BattleBoard(
    ui: GameUiState,
    landscape: Boolean,
    onTap: (Coord) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ui.session?.state ?: return
    val target = state.gridOf(ui.viewer.other)
    val own = state.gridOf(ui.viewer)
    val (targetView, ownView) = views(ui)
    val ownBig = ui.ownBig && !landscape
    val swapLabel = stringResource(R.string.swap_grids)
    BoxWithConstraints(modifier) {
        val slots = slots(landscape)
        val targetSlot = animatedSlot(if (ownBig) slots.mini else slots.big, landscape, "target")
        val ownSlot = animatedSlot(if (ownBig) slots.big else slots.mini, landscape, "own")
        val targetSmall = ownBig
        val ownSmall = !ownBig && !landscape
        val targetSummary = stringResource(R.string.enemy_waters, target.shipsAfloat, target.ships.size)
        Box(targetSlot.place().let { if (targetSmall) it.small(targetSummary, swapLabel, onSwap) else it }) {
            SeaGrid(
                targetView,
                Modifier.size(targetSlot.size),
                onTap = if (ui.canFire && !targetSmall) onTap else null,
                tappable = { !target.isTried(it) },
                nodes = !targetSmall,
                cellDescription = { targetCell(targetView, it) },
            )
        }
        val ownSummary = stringResource(R.string.your_fleet, own.shipsAfloat, own.ships.size)
        Box(ownSlot.place().let { if (ownSmall) it.small(ownSummary, swapLabel, onSwap) else it }) {
            SeaGrid(ownView, Modifier.size(ownSlot.size), nodes = !ownSmall, cellDescription = { ownCell(ownView, it) })
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

/** [slot], reached with the motion scheme's spring in portrait (the swap); landscape has nothing to swap. */
@Composable
private fun animatedSlot(slot: Slot, landscape: Boolean, label: String): Slot {
    val size by animateDpAsState(
        slot.size,
        if (landscape) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "$label size",
    )
    val at by animateValueAsState(
        slot.at,
        DpOffset.VectorConverter,
        if (landscape) snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "$label place",
    )
    return Slot(at, size)
}

/** The small grid: one TalkBack node with a summary, and a tap (or double tap) shows it big. */
private fun Modifier.small(summary: String, swapLabel: String, onSwap: () -> Unit): Modifier =
    clickable(onClick = onSwap).clearAndSetSemantics {
        contentDescription = summary
        onClick(swapLabel) {
            onSwap()
            true
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
