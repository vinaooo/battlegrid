package io.github.vinaooo.battlegrid.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.vinkit.achievements.R as AchievementsR
import io.github.vinaooo.vinkit.designsystem.R as DesignR

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
        Opponent.EASY -> DesignR.string.vinkit_difficulty_easy
        Opponent.MEDIUM -> DesignR.string.vinkit_difficulty_medium
        Opponent.HARD -> DesignR.string.vinkit_difficulty_hard
        Opponent.TWO_PLAYER -> R.string.opponent_two_player
    },
)

/** "10×10 · Salvo · Hard", or the daily challenge's name (a practice when not [recorded]). */
@Composable
internal fun modeName(mode: GameMode, recorded: Boolean = true): String = if (mode.daily) {
    stringResource(if (recorded) R.string.daily else R.string.daily_practice)
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

@Composable
internal fun badgeName(badge: Achievement): String = stringResource(badgeTexts.getValue(badge).first)

@Composable
internal fun badgeNote(badge: Achievement): String = stringResource(badgeTexts.getValue(badge).second)

private val badgeTexts = mapOf(
    Achievement.FIRST_WIN to (R.string.badge_first_win to R.string.badge_first_win_note),
    Achievement.WIN_EVERY_SIZE to (R.string.badge_every_size to R.string.badge_every_size_note),
    Achievement.WIN_EVERY_FIRING to (R.string.badge_every_firing to R.string.badge_every_firing_note),
    Achievement.BEAT_HARD to (R.string.badge_beat_hard to R.string.badge_beat_hard_note),
    Achievement.NO_HINT_WIN to (AchievementsR.string.vinkit_badge_no_hints to R.string.badge_no_hints_note),
    Achievement.CLEAN_SINK to (R.string.badge_clean_sink to R.string.badge_clean_sink_note),
    Achievement.WIN_STREAK_5 to (R.string.badge_streak to R.string.badge_streak_note),
    Achievement.DAILY_STREAK_7 to (R.string.badge_daily_streak to R.string.badge_daily_streak_note),
    Achievement.ACCURACY_60 to (R.string.badge_accuracy to R.string.badge_accuracy_note),
)
