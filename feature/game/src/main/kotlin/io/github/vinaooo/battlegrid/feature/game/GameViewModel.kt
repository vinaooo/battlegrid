package io.github.vinaooo.battlegrid.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.battlegrid.domain.ai.aiFor
import io.github.vinaooo.battlegrid.domain.ai.aiRandom
import io.github.vinaooo.battlegrid.domain.hint.HintEngine
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.domain.usecase.FinishGame
import io.github.vinaooo.battlegrid.domain.usecase.RestartGame
import io.github.vinaooo.battlegrid.domain.usecase.ResumeGame
import io.github.vinaooo.battlegrid.domain.usecase.SaveGame
import io.github.vinaooo.battlegrid.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.github.vinaooo.vinkit.shell.Ticker
import io.github.vinaooo.vinkit.shell.give
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "TooManyFunctions") // Each collaborator is one small, separately tested piece.
@HiltViewModel
class GameViewModel @Inject constructor(
    private val startNewGame: StartNewGame,
    private val restartGame: RestartGame,
    private val resumeGame: ResumeGame,
    private val saveGame: SaveGame,
    private val finishGame: FinishGame,
    appSettings: AppSettingsRepository,
    gameSettings: GameSettingsRepository,
    private val engine: GameEngine,
    private val feedback: GameFeedback,
    @AiDispatcher private val aiDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val state = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /** What the game is waiting on: the AI's turn, a salvo being shown, a handover pause. A new game cancels it. */
    private var pending: Job? = null

    /** Presses of the Random button in this game, so each press deals another fleet, the same ones on a restart. */
    private var randomPresses = 0

    /** Charges a second while the screen shows, in battle only. Reads the latest state on every tick. */
    private val clock = Ticker(viewModelScope, TICK_MILLIS) {
        state.update { ui ->
            val session = ui.session?.takeIf { it.state.isBattle && !ui.covered } ?: return@update ui
            ui.copy(session = session.tick())
        }
    }

    init {
        viewModelScope.launch {
            appSettings.settings.collect { settings -> state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch { show(resumeGame() ?: startNewGame()) }
        viewModelScope.launch {
            // A mode changed in Settings (confirmed there when a game was on) starts a game in it.
            gameSettings.settings.map { it.mode }.distinctUntilChanged().drop(1).collect { newGame(it) }
        }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.PlaceShip, is GameIntent.RotateShip, GameIntent.RandomFleet, GameIntent.ConfirmFleet ->
                placement(intent)
            is GameIntent.Tap -> tap(intent.coord)
            GameIntent.FireSalvo -> fireSalvo()
            GameIntent.Hint -> hint()
            GameIntent.Uncover -> uncover()
            GameIntent.Resign -> resign()
            GameIntent.NewGame -> viewModelScope.launch { newGame(null) }
            GameIntent.Restart -> viewModelScope.launch { restart() }
            GameIntent.Resume -> clock.start()
            GameIntent.Pause -> pause()
        }
    }

    private fun placement(intent: GameIntent) {
        when (intent) {
            is GameIntent.PlaceShip -> place(Move.PlaceShip(intent.index, intent.ship))
            is GameIntent.RotateShip -> place(Move.RotateShip(intent.index))
            GameIntent.RandomFleet -> randomFleet()
            else -> place(Move.ConfirmFleet)
        }
    }

    private suspend fun newGame(mode: GameMode?) {
        pending?.cancel()
        show(startNewGame(mode))
    }

    private suspend fun restart() {
        val session = state.value.session ?: return
        pending?.cancel()
        show(restartGame(session))
    }

    /** A game comes on screen, new or resumed. A resumed pass-and-play game is covered until someone takes it. */
    private fun show(session: GameSession) {
        randomPresses = 0
        val game = session.state
        val handedOver = !game.mode.isVsAi && (game.isBattle || game.phase == Phase.Placement(Side.ENEMY))
        state.update {
            it.copy(
                session = session,
                aiFiring = false,
                hiddenShots = 0,
                revealing = null,
                covered = handedOver,
            )
        }
        if (game.isAiTurn) playAi()
    }

    private fun place(move: Move) {
        val current = state.value
        val session = current.session?.takeIf { current.canPlace } ?: return
        val placed = session.play(move, engine)
        if (placed == null) {
            feedback.give(FeedbackEvent.REJECTED, current.settings)
            return
        }
        state.update { it.copy(session = placed) }
        viewModelScope.launch { saveGame(placed) }
        if (move != Move.ConfirmFleet) return
        val game = placed.state
        when {
            !game.mode.isVsAi -> handOver(game.toMove)
            game.isAiTurn -> playAi()
        }
    }

    private fun randomFleet() {
        val session = state.value.session ?: return
        val side = state.value.viewer
        val random = Random(session.seed * RANDOM_SEED_FACTOR + side.ordinal * RANDOM_SIDE_STRIDE + randomPresses++)
        place(Move.SetFleet(RandomFleetPlacer.place(session.state.mode.size, random)))
    }

    private fun tap(coord: Coord) {
        val current = state.value
        val session = current.session?.takeIf { current.canFire } ?: return
        val salvo = session.state.mode.firing == FiringMode.SALVO
        val next = session.play(if (salvo) Move.MarkSalvo(coord) else Move.Fire(coord), engine)
        when {
            next == null -> feedback.give(FeedbackEvent.REJECTED, current.settings)
            salvo -> state.update { it.copy(session = next) }
            else -> fired(session, next)
        }
    }

    private fun fireSalvo() {
        val current = state.value
        val session = current.session?.takeIf { current.canFire } ?: return
        val next = session.play(Move.FireSalvo, engine)
        if (next == null) feedback.give(FeedbackEvent.REJECTED, current.settings) else fired(session, next)
    }

    /** The viewer fired: one shot shows at once, a salvo one result at a time; then whatever comes next. */
    private fun fired(before: GameSession, after: GameSession) {
        val shots = newShots(before, after)
        if (shots.size == 1) {
            state.update { it.copy(session = after) }
            report(before.state.toMove, shots.single(), after)
            afterShots(before, after)
        } else {
            holdBack(before, after)
            pending = viewModelScope.launch {
                reveal(before, after)
                afterShots(before, after)
            }
        }
    }

    private fun newShots(before: GameSession, after: GameSession): List<Coord> {
        val target = before.state.toMove.other
        return after.state.gridOf(target).shots.drop(before.state.gridOf(target).shots.size)
    }

    /** A salvo was fired: its shots are in, none shown yet. */
    private fun holdBack(before: GameSession, after: GameSession) {
        val hidden = newShots(before, after).size
        state.update { it.copy(session = after, hiddenShots = hidden, revealing = before.state.toMove.other) }
    }

    /** Shows a held-back salvo's results one by one, a pause before each. */
    private suspend fun reveal(before: GameSession, after: GameSession) {
        val shots = newShots(before, after)
        shots.forEachIndexed { index, coord ->
            delay(SHOT_PAUSE_MILLIS)
            state.update { it.copy(hiddenShots = shots.size - index - 1) }
            report(before.state.toMove, coord, after)
        }
        state.update { it.copy(revealing = null) }
    }

    /** What [shooter]'s shot at [coord] found, said and felt, as it was when fired (a later shot may sink its ship). */
    private fun report(shooter: Side, coord: Coord, session: GameSession) {
        val grid = session.state.gridOf(shooter.other)
        val then = grid.copy(shots = grid.shots.take(grid.shots.indexOf(coord) + 1))
        val result = checkNotNull(then.resultAt(coord))
        val sunk = if (result == ShotResult.SUNK) grid.size.fleet[checkNotNull(grid.shipAt(coord))] else null
        announce(Announcement.Shot(shooter, coord, result, sunk))
        val settings = state.value.settings
        when (result) {
            ShotResult.MISS -> feedback.give(SOUND_MISS, FeedbackEvent.MOVE, settings)
            ShotResult.HIT -> feedback.give(SOUND_HIT, FeedbackEvent.MOVE, settings)
            ShotResult.SUNK -> feedback.give(SOUND_SUNK, FeedbackEvent.WIN, settings)
        }
    }

    /** After the viewer's shots: the end, the same shooter again (hit-again), or the other side's turn. */
    private fun afterShots(before: GameSession, after: GameSession) {
        val game = after.state
        if (game.isOver) {
            viewModelScope.launch { finish(after) }
            return
        }
        viewModelScope.launch { saveGame(after) }
        if (game.toMove == before.state.toMove) return
        if (game.mode.isVsAi) {
            playAi()
        } else {
            pending = viewModelScope.launch {
                delay(RESULT_LOOK_MILLIS)
                handOver(game.toMove)
            }
        }
    }

    /**
     * The AI's turn, at the player's own grid: a pause, then its shot (a salvo is shown one by one), until the turn
     * passes or the game ends. It reads the latest state before every shot.
     */
    private fun playAi() {
        state.update { it.copy(aiFiring = true) }
        pending = viewModelScope.launch {
            while (state.value.session?.state?.isAiTurn == true) {
                val session = checkNotNull(state.value.session)
                val game = session.state
                val shots =
                    withContext(aiDispatcher) { aiFor(game.mode.opponent).shots(game, aiRandom(session.seed, game)) }
                val moves = if (game.mode.firing == FiringMode.SALVO) {
                    shots.map(Move::MarkSalvo) + Move.FireSalvo
                } else {
                    delay(SHOT_PAUSE_MILLIS)
                    listOf(Move.Fire(shots.single()))
                }
                val after = moves.fold(session) { acc, move -> acc.play(move, engine) ?: return@launch }
                if (moves.size == 1) {
                    state.update { it.copy(session = after) }
                    report(Side.ENEMY, shots.single(), after)
                } else {
                    holdBack(session, after)
                    reveal(session, after)
                }
                if (after.state.isOver) finish(after) else saveGame(after)
            }
            state.update { it.copy(aiFiring = false) }
        }
    }

    private fun hint() {
        val current = state.value
        val session = current.session
            ?.takeIf { current.canFire && it.state.mode.isVsAi && it.state.hint.isEmpty() } ?: return
        val cells = HintEngine.cells(session.state, HintEngine.random(session.seed, session.state)) ?: return
        val hinted = session.play(Move.ShowHint(cells), engine) ?: return
        state.update { it.copy(session = hinted) }
        viewModelScope.launch { saveGame(hinted) }
        announce(Announcement.Hinted(cells))
    }

    private fun resign() {
        val current = state.value
        val session = current.session ?: return
        val resigned = session.play(Move.Resign(current.viewer), engine) ?: return
        pending?.cancel()
        state.update { it.copy(session = resigned, aiFiring = false, hiddenShots = 0, revealing = null) }
        viewModelScope.launch { finish(resigned) }
    }

    private suspend fun finish(session: GameSession) {
        val game = session.state
        val winner = checkNotNull(game.winner)
        if (!game.mode.isVsAi || winner == Side.PLAYER) feedback.give(FeedbackEvent.WIN, state.value.settings)
        announce(Announcement.Ended(winner, game.mode.isVsAi, (game.phase as Phase.Over).resigned))
        finishGame(session)
    }

    /** Pass-and-play: the screen covers until [next] takes the phone. */
    private fun handOver(next: Side) {
        state.update { it.copy(covered = true) }
        announce(Announcement.Handover(next))
    }

    /** The cover lifts. */
    private fun uncover() {
        state.update { it.copy(covered = false) }
    }

    private fun pause() {
        clock.stop()
        val session = state.value.session?.takeUnless { it.state.isOver } ?: return
        viewModelScope.launch { saveGame(session) }
    }

    private fun announce(announcement: Announcement) {
        state.update { it.copy(announcement = announcement, announcementSequence = it.announcementSequence + 1) }
    }

    companion object {
        /** The game's own sounds, by name (`AndroidGameFeedback(sounds = …)`). */
        const val SOUND_MISS = "miss"
        const val SOUND_HIT = "hit"
        const val SOUND_SUNK = "sunk"

        /** The pause before each AI shot, and between a salvo's results. */
        private const val SHOT_PAUSE_MILLIS = 600L

        /** Pass-and-play: how long the result shows before the cover. */
        private const val RESULT_LOOK_MILLIS = 1_000L

        private const val TICK_MILLIS = 1_000L
        private const val RANDOM_SEED_FACTOR = 41
        private const val RANDOM_SIDE_STRIDE = 1_000_003L
    }
}
