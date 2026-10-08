package io.github.vinaooo.battlegrid.feature.game.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.battlegrid.domain.hint.HintEngine
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.SalvoFiring
import io.github.vinaooo.battlegrid.domain.rules.ShotsScoring
import io.github.vinaooo.battlegrid.feature.game.GameIntent
import io.github.vinaooo.battlegrid.feature.game.GameUiState
import io.github.vinaooo.battlegrid.feature.game.GameViewModel
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.battlegrid.feature.game.board.BattleBoard
import io.github.vinaooo.battlegrid.feature.game.board.PlacementBoard
import io.github.vinaooo.vinkit.core.formatElapsed
import io.github.vinaooo.vinkit.shell.FrameInfo
import io.github.vinaooo.vinkit.shell.GameFrame
import io.github.vinaooo.vinkit.shell.GameSurface
import io.github.vinaooo.vinkit.shell.GameToolbar
import io.github.vinaooo.vinkit.shell.LocalFrameInfo
import io.github.vinaooo.vinkit.shell.MenuOption
import io.github.vinaooo.vinkit.shell.ModeAndTime
import io.github.vinaooo.vinkit.shell.R as ShellR
import io.github.vinaooo.vinkit.shell.ToolbarAction
import io.github.vinaooo.vinkit.shell.WinCelebration
import io.github.vinaooo.vinkit.shell.WinDialog
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** The game screen. The Scores and Settings buttons show only when their screens exist (non-null). */
@Composable
fun GameRoute(
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(GameIntent.Resume)
        onPauseOrDispose { viewModel.onIntent(GameIntent.Pause) }
    }
    GameScreen(uiState, viewModel::onIntent, modifier, onOpenScores, onOpenSettings)
}

@Composable
fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val state = uiState.session?.state
    val vsAi = state?.mode?.isVsAi ?: true
    // The cover replaces the game, so neither TalkBack nor a screenshot reaches the hidden grids.
    if (uiState.covered && state != null) {
        Cover(uiState.viewer.takeIf { state.phase is Phase.Placement } ?: state.toMove, onIntent, modifier)
        return
    }
    GameSurface(
        announcement = uiState.announcement?.let { announcementText(it, vsAi) },
        announcementSequence = uiState.announcementSequence,
        modifier = modifier,
    ) { reportBug ->
        GameFrame(
            settings = uiState.settings,
            info = { frame -> Info(uiState, frame) },
            board = {
                Board(uiState, onIntent)
            },
            toolbar = { frame -> Toolbar(uiState, onIntent, frame, reportBug) },
            onOpenScores = onOpenScores,
            onOpenSettings = onOpenSettings,
            boardAspectRatio = null,
        )
    }
    EndDialog(uiState, onNewGame = { onIntent(GameIntent.NewGame) })
}

@Composable
private fun Board(uiState: GameUiState, onIntent: (GameIntent) -> Unit) {
    val state = uiState.session?.state ?: return
    val frame = LocalFrameInfo.current
    if (state.phase is Phase.Placement) {
        PlacementBoard(
            grid = state.gridOf(uiState.viewer),
            onPlace = { index, ship -> onIntent(GameIntent.PlaceShip(index, ship)) },
            onRotate = { onIntent(GameIntent.RotateShip(it)) },
            modifier = Modifier.fillMaxSize().padding(BOARD_PADDING.dp),
        )
    } else {
        BattleBoard(
            ui = uiState,
            landscape = frame.landscape,
            onTap = { onIntent(GameIntent.Tap(it)) },
            modifier = Modifier.fillMaxSize().padding(BOARD_PADDING.dp),
        )
    }
}

/** The mode and the battle's time, and what happens now, read as one item. */
@Composable
private fun Info(uiState: GameUiState, frame: FrameInfo) {
    val state = uiState.session?.state ?: return
    val turn = turnText(uiState)
    // ModeAndTime reads "mode, time, …"; the turn joins it as one item.
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        ModeAndTime(modeName(state.mode), state.elapsedSeconds, large = frame.landscape)
        Text(
            turn,
            style = if (frame.landscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Toolbar(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    frame: FrameInfo,
    onReportBug: (() -> Unit)?,
) {
    val state = uiState.session?.state ?: return
    val placing = state.phase is Phase.Placement
    val actions = if (placing) {
        listOf(
            ToolbarAction.Button(Icons.Rounded.Shuffle, stringResource(R.string.random_fleet), uiState.canPlace) {
                onIntent(GameIntent.RandomFleet)
            },
            ToolbarAction.Button(
                Icons.Rounded.Check,
                stringResource(R.string.start_battle),
                enabled = uiState.canPlace && state.gridOf(uiState.viewer).isFleetComplete,
            ) { onIntent(GameIntent.ConfirmFleet) },
        )
    } else {
        val salvo = state.mode.firing == FiringMode.SALVO
        listOf(
            ToolbarAction.Button(
                Icons.Rounded.Lightbulb,
                stringResource(ShellR.string.vinkit_hint),
                enabled = uiState.canFire && state.hint.isEmpty() && state.hintsUsed < HintEngine.MAX_HINTS,
                visible = state.mode.isVsAi,
            ) { onIntent(GameIntent.Hint) },
            ToolbarAction.Button(
                Icons.Rounded.TrackChanges,
                stringResource(R.string.fire),
                enabled = uiState.canFire && state.salvoMarks.size == SalvoFiring.shotsPerTurn(state),
                visible = salvo,
            ) { onIntent(GameIntent.FireSalvo) },
        )
    }
    val battle = state.isBattle
    GameToolbar(
        actions = actions,
        menuOptions = listOfNotNull(
            MenuOption(Icons.Rounded.Replay, stringResource(R.string.new_game)) { onIntent(GameIntent.NewGame) },
            MenuOption(Icons.Rounded.RestartAlt, stringResource(R.string.restart)) { onIntent(GameIntent.Restart) },
            if (battle) {
                MenuOption(Icons.AutoMirrored.Rounded.ExitToApp, stringResource(R.string.resign)) {
                    onIntent(GameIntent.Resign)
                }
            } else {
                null
            },
        ),
        onReportBug = onReportBug,
        vertical = frame.landscape,
        mirrored = frame.mirrored,
    )
}

/** Pass-and-play: the whole screen hidden until [next] has the phone and taps. */
@Composable
private fun Cover(next: Side, onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxSize().clickable { onIntent(GameIntent.Uncover) },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(Modifier.safeDrawingPadding().padding(COVER_PADDING.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.pass_phone, playerName(next)),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(stringResource(R.string.tap_to_start), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Once the game ends and its last shots show, after a moment to see the board (on a loss, the enemy's fleet is
 * revealed on it): the result, shots, accuracy, time and hints, and a new game. Only a win is celebrated: the
 * player's against the AI, either side's in pass-and-play.
 */
@Composable
private fun EndDialog(uiState: GameUiState, onNewGame: () -> Unit) {
    val state = uiState.session?.state?.takeIf { uiState.ended }
    var shown by remember(state?.phase) { mutableStateOf(false) }
    LaunchedEffect(state?.phase) {
        if (state != null) {
            delay(END_DIALOG_DELAY_MILLIS)
            shown = true
        }
    }
    val celebration = remember(state?.phase) { WinCelebration.entries.random() }
    if (state == null || !shown) return
    val winner = checkNotNull(state.winner)
    val vsAi = state.mode.isVsAi
    val shooter = if (vsAi) Side.PLAYER else winner
    val target = state.gridOf(shooter.other)
    val shots = target.shots.size
    val accuracy = if (shots == 0) 0 else (target.hits * PERCENT / shots.toFloat()).roundToInt()
    val lines = buildList {
        add(modeName(state.mode))
        add(pluralStringResource(R.plurals.shots_count, shots, shots))
        add(stringResource(R.string.accuracy, accuracy))
        add(stringResource(R.string.time, formatElapsed(state.elapsedSeconds)))
        if (vsAi) {
            val hints = state.hintsUsed
            add(
                if (hints == 0) {
                    stringResource(R.string.no_hints)
                } else {
                    pluralStringResource(R.plurals.hints_count, hints, hints, hints * ShotsScoring.HINT_PENALTY)
                },
            )
        }
    }
    WinDialog(
        lines = lines,
        onNewGame = onNewGame,
        title = resultText(winner, vsAi),
        kind = celebration.takeIf { !vsAi || winner == Side.PLAYER },
    )
}

/** Long enough to see the last shot, and on a loss the enemy's fleet, before the dialog covers the board. */
private const val END_DIALOG_DELAY_MILLIS = 1_200L
private const val PERCENT = 100
private const val BOARD_PADDING = 8
private const val COVER_PADDING = 24
