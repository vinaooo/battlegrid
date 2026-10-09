package io.github.vinaooo.battlegrid.feature.game.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.battlegrid.feature.game.ui.firingName
import io.github.vinaooo.battlegrid.feature.game.ui.opponentName
import io.github.vinaooo.vinkit.settings.Choice
import io.github.vinaooo.vinkit.settings.IconChoice
import io.github.vinaooo.vinkit.settings.IconOption
import io.github.vinaooo.vinkit.settings.NewGameConfirmDialog
import io.github.vinaooo.vinkit.settings.SettingsScreen
import io.github.vinaooo.vinkit.settings.SettingsSection

/** vinkit's Settings screen with BattleGrid's Game section: board size, firing mode and opponent. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val game by viewModel.game.collectAsStateWithLifecycle()
    val pending by viewModel.pendingMode.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    SettingsScreen(
        settings = app,
        onChange = viewModel::onAppChange,
        onBack = onBack,
        onOpenPrivacyPolicy = { uriHandler.openUri(privacyPolicyUrl) },
        modifier = modifier,
        gameSections = listOf(
            SettingsSection(stringResource(R.string.section_game)) { GameSection(game.mode, viewModel::onModeChange) },
        ),
        privacyOptionsRequired = privacyOptionsRequired,
        onOpenPrivacyOptions = onOpenPrivacyOptions,
    )
    pending?.let {
        if (it.countsAsLoss) {
            NewGameConfirmDialog(viewModel::confirmMode, viewModel::dismissMode)
        } else {
            NewGameConfirmDialog(
                viewModel::confirmMode,
                viewModel::dismissMode,
                stringResource(R.string.new_game_not_counted),
            )
        }
    }
}

@Composable
internal fun GameSection(mode: GameMode, onModeChange: (GameMode) -> Unit) {
    Choice(
        title = stringResource(R.string.board),
        options = BoardSize.entries.map { it to stringResource(R.string.board_size, it.side) },
        selected = mode.size,
        onSelect = { onModeChange(mode.copy(size = it)) },
    )
    Choice(
        title = stringResource(R.string.firing),
        options = FiringMode.entries.map { it to firingName(it) },
        selected = mode.firing,
        onSelect = { onModeChange(mode.copy(firing = it)) },
    )
    IconChoice(
        title = stringResource(R.string.opponent),
        options = Opponent.entries.map { IconOption(it, icons.getValue(it), opponentName(it), opponentNote(it)) },
        selected = mode.opponent,
        onSelect = { onModeChange(mode.copy(opponent = it)) },
    )
}

@Composable
private fun opponentNote(opponent: Opponent): String = stringResource(
    when (opponent) {
        Opponent.EASY -> R.string.opponent_easy_note
        Opponent.MEDIUM -> R.string.opponent_medium_note
        Opponent.HARD -> R.string.opponent_hard_note
        Opponent.TWO_PLAYER -> R.string.opponent_two_player_note
    },
)

private val icons = mapOf(
    Opponent.EASY to Icons.Rounded.SentimentVerySatisfied,
    Opponent.MEDIUM to Icons.Rounded.SentimentSatisfied,
    Opponent.HARD to Icons.Rounded.Psychology,
    Opponent.TWO_PLAYER to Icons.Rounded.Groups,
)
