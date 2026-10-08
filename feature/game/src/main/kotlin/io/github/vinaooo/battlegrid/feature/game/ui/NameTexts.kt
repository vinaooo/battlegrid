package io.github.vinaooo.battlegrid.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.feature.game.R

@Composable
internal fun shipName(type: ShipClass): String = stringResource(
    when (type) {
        ShipClass.CARRIER -> R.string.ship_carrier
        ShipClass.BATTLESHIP -> R.string.ship_battleship
        ShipClass.FRIGATE -> R.string.ship_frigate
        ShipClass.CRUISER -> R.string.ship_cruiser
        ShipClass.SUBMARINE -> R.string.ship_submarine
        ShipClass.DESTROYER -> R.string.ship_destroyer
    },
)

@Composable
internal fun firingName(firing: FiringMode): String = stringResource(
    when (firing) {
        FiringMode.CLASSIC -> R.string.firing_classic
        FiringMode.HIT_AGAIN -> R.string.firing_hit_again
        FiringMode.SALVO -> R.string.firing_salvo
    },
)

@Composable
internal fun opponentName(opponent: Opponent): String = stringResource(
    when (opponent) {
        Opponent.EASY -> R.string.opponent_easy
        Opponent.MEDIUM -> R.string.opponent_medium
        Opponent.HARD -> R.string.opponent_hard
        Opponent.TWO_PLAYER -> R.string.opponent_two_player
    },
)

/** "10×10 · Salvo · Hard", or the daily challenge's name. */
@Composable
internal fun modeName(mode: GameMode): String = if (mode.daily) {
    stringResource(R.string.daily)
} else {
    stringResource(
        R.string.mode_name,
        stringResource(R.string.board_size, mode.size.side),
        firingName(mode.firing),
        opponentName(mode.opponent),
    )
}

@Composable
internal fun playerName(side: Side): String =
    stringResource(if (side == Side.PLAYER) R.string.player_1 else R.string.player_2)
