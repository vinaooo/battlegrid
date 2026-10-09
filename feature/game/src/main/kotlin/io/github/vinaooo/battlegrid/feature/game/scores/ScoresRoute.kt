package io.github.vinaooo.battlegrid.feature.game.scores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.usecase.FinishGame
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.battlegrid.feature.game.ui.firingName
import io.github.vinaooo.battlegrid.feature.game.ui.opponentName
import io.github.vinaooo.vinkit.scores.ScorePoints
import io.github.vinaooo.vinkit.scores.ScoresScreen

@Composable
fun ScoresRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BattleGridScoresViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(
        uiState = uiState,
        onBack = onBack,
        modeName = { key ->
            GameMode.fromKey(key)?.let {
                if (it.daily) {
                    stringResource(R.string.daily)
                } else {
                    stringResource(R.string.firing_and_opponent, firingName(it.firing), opponentName(it.opponent))
                }
            } ?: key
        },
        modifier = modifier,
        groupName = { group -> groupName(group) },
        onSelectGroup = viewModel::selectGroup,
        details = { record ->
            val hints = record.extras[FinishGame.HINTS]?.toIntOrNull() ?: 0
            if (hints == 0) {
                stringResource(R.string.no_hints)
            } else {
                pluralStringResource(R.plurals.hints_used, hints, hints)
            }
        },
        points = { ScorePoints("${it.points}", pluralStringResource(R.plurals.shots_count, it.points, it.points)) },
    )
}

/** A tab's name: a board size or the daily challenge. */
@Composable
private fun groupName(group: String): String = when (group) {
    GameMode.DAILY_KEY -> stringResource(R.string.tab_daily)
    else -> BoardSize.entries.firstOrNull { it.name == group }?.let { stringResource(R.string.board_size, it.side) }
        ?: group
}
