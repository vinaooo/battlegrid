package io.github.vinaooo.battlegrid.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.SalvoFiring
import io.github.vinaooo.battlegrid.feature.game.Announcement
import io.github.vinaooo.battlegrid.feature.game.GameUiState
import io.github.vinaooo.battlegrid.feature.game.R

/** A cell's name: its column letter and row number, "B7". */
internal fun cellName(coord: Coord): String = "${'A' + coord.col}${coord.row + 1}"

/** What happens now: who places, whose turn, how many salvo shots to mark, or how the game ended. */
@Composable
internal fun turnText(ui: GameUiState): String {
    val state = ui.session?.state ?: return ""
    val vsAi = state.mode.isVsAi
    val phase = state.phase
    return when {
        phase is Phase.Placement && vsAi -> stringResource(R.string.place_fleet)
        phase is Phase.Placement -> stringResource(R.string.player_place_fleet, playerName(phase.side))
        phase is Phase.Over -> resultText(phase.winner, vsAi)
        vsAi && state.toMove == Side.ENEMY -> stringResource(R.string.enemy_turn)
        state.mode.firing == FiringMode.SALVO && ui.canFire -> {
            val shots = SalvoFiring.shotsPerTurn(state) - state.salvoMarks.size
            pluralStringResource(R.plurals.salvo_shots, shots, shots)
        }
        vsAi -> stringResource(R.string.your_turn)
        else -> stringResource(R.string.player_turn, playerName(state.toMove))
    }
}

@Composable
internal fun resultText(winner: Side, vsAi: Boolean): String = when {
    !vsAi -> stringResource(R.string.player_won, playerName(winner))
    winner == Side.PLAYER -> stringResource(R.string.you_won)
    else -> stringResource(R.string.you_lost)
}

@Composable
internal fun shotText(result: ShotResult, sunk: ShipClass?, coord: Coord): String = when (result) {
    ShotResult.MISS -> stringResource(R.string.shot_miss, cellName(coord))
    ShotResult.HIT -> stringResource(R.string.shot_hit, cellName(coord))
    ShotResult.SUNK -> stringResource(R.string.shot_sunk, shipName(checkNotNull(sunk)), cellName(coord))
}

/** What TalkBack says after [announcement]. */
@Composable
internal fun announcementText(announcement: Announcement, vsAi: Boolean): String = when (announcement) {
    is Announcement.Shot -> if (vsAi && announcement.shooter == Side.ENEMY) {
        stringResource(
            R.string.enemy_shot,
            cellName(announcement.coord),
            cellResult(announcement.result, announcement.sunk),
        )
    } else {
        shotText(announcement.result, announcement.sunk, announcement.coord)
    }
    is Announcement.Hinted -> stringResource(
        R.string.hinted,
        announcement.cells.joinToString(", ", transform = ::cellName),
    )
    is Announcement.Ended -> resultText(announcement.winner, announcement.vsAi)
    is Announcement.Handover -> stringResource(R.string.pass_phone, playerName(announcement.next))
}

/** A shot's result as a cell's state: "miss", "hit", "Cruiser sunk". */
@Composable
internal fun cellResult(result: ShotResult, sunk: ShipClass?): String = when (result) {
    ShotResult.MISS -> stringResource(R.string.cell_miss)
    ShotResult.HIT -> stringResource(R.string.cell_hit)
    ShotResult.SUNK -> stringResource(R.string.cell_sunk, shipName(checkNotNull(sunk)))
}
