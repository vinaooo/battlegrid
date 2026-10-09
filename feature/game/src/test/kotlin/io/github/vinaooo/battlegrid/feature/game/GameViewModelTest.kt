package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.fake.FakeAchievementRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeClock
import io.github.vinaooo.battlegrid.domain.fake.FakeDailyRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeScoreRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSeedSource
import io.github.vinaooo.battlegrid.domain.fake.FakeStatsRepository
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.repository.GameSettings
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.domain.usecase.FinishGame
import io.github.vinaooo.battlegrid.domain.usecase.RecordProgress
import io.github.vinaooo.battlegrid.domain.usecase.RestartGame
import io.github.vinaooo.battlegrid.domain.usecase.ResumeGame
import io.github.vinaooo.battlegrid.domain.usecase.SaveGame
import io.github.vinaooo.battlegrid.domain.usecase.StartDailyGame
import io.github.vinaooo.battlegrid.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val engine = GameEngine()
    private val savedGames = FakeSavedGameRepository()
    private val gameSettings = FakeGameSettingsRepository(GameSettings(nextFirstMover = Side.PLAYER))
    private val stats = FakeStatsRepository()
    private val appSettings = object : AppSettingsRepository {
        val current = MutableStateFlow(AppSettings())
        override val settings = current

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            current.value = transform(current.value)
        }
    }
    private val played = mutableListOf<String>()
    private val feedback = object : GameFeedback {
        override fun sound(event: FeedbackEvent) {
            played += "$event"
        }

        override fun haptic(event: FeedbackEvent) = Unit

        override fun sound(name: String) {
            played += name
        }
    }
    private val viewModels = mutableListOf<GameViewModel>()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() {
        // The clock is an endless loop: stop it, or runTest waits for it forever.
        viewModels.forEach { it.onIntent(GameIntent.Pause) }
        Dispatchers.resetMain()
    }

    private val clock = FakeClock(now = 20_000L * 86_400_000L)
    private val achievements = FakeAchievementRepository()
    private val progress = RecordProgress(FakeDailyRepository(), achievements, stats)

    private fun TestScope.viewModel(): GameViewModel = GameViewModel(
        StartNewGame(savedGames, gameSettings, progress, FakeSeedSource(), engine),
        StartDailyGame(savedGames, gameSettings, progress, clock, engine),
        RestartGame(savedGames, gameSettings, progress, engine),
        ResumeGame(savedGames),
        SaveGame(savedGames),
        FinishGame(savedGames, gameSettings, FakeScoreRepository(), clock, progress),
        appSettings,
        gameSettings,
        engine,
        feedback,
        dispatcher,
    ).also {
        viewModels += it
        runCurrent()
    }

    private fun GameViewModel.state() = uiState.value.session.shouldNotBeNull().state

    /** Places a random fleet and starts the battle. */
    private fun GameViewModel.toBattle() = apply {
        onIntent(GameIntent.RandomFleet)
        onIntent(GameIntent.ConfirmFleet)
    }

    /** A saved battle against the AI with every enemy ship cell hit but the destroyer's last. */
    private fun nearlyWon(mode: GameMode = GameMode.DEFAULT, toMove: Side = Side.PLAYER): GameSession {
        val fleet = mode.size.fleet.mapIndexed { i, type -> Ship(type, Coord(2 * i, 0), Orientation.HORIZONTAL) }
        val battle = GameSession(7, engine.newGame(mode, toMove, fleet))
            .play(Move.SetFleet(fleet), engine)!!
            .play(Move.ConfirmFleet, engine)!!.state
        val cells = fleet.flatMap { it.cells }
        val enemyShots = if (toMove == Side.PLAYER) cells.dropLast(1) else emptyList()
        val playerShots = if (toMove == Side.ENEMY) cells.dropLast(1) else emptyList()
        return GameSession(
            7,
            battle.copy(
                enemy = battle.enemy.copy(shots = enemyShots),
                player = battle.player.copy(shots = playerShots),
            ),
        )
    }

    @Test
    fun `the first launch starts a game in Settings' mode, the player placing`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.state().mode shouldBe GameMode.DEFAULT
        vm.state().phase shouldBe Phase.Placement(Side.PLAYER)
        vm.uiState.value.canPlace shouldBe true
        vm.uiState.value.canFire shouldBe false
    }

    @Test
    fun `ships are placed, turned and dealt at random, each change saved`() = runTest(dispatcher) {
        val vm = viewModel()
        val carrier = Ship(BoardSize.TEN.fleet[0], Coord(9, 5), Orientation.HORIZONTAL)
        vm.onIntent(GameIntent.PlaceShip(0, carrier))
        vm.state().player.ships[0] shouldBe carrier
        vm.onIntent(GameIntent.RotateShip(0))
        vm.state().player.ships[0] shouldBe carrier.copy(origin = Coord(5, 5), orientation = Orientation.VERTICAL)
        vm.onIntent(GameIntent.RandomFleet)
        vm.state().player.isFleetComplete shouldBe true
        runCurrent()
        savedGames.saved.shouldNotBeNull().state shouldBe vm.state()
    }

    @Test
    fun `the Random button deals a different fleet each press, the same ones for the same seed`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(GameIntent.RandomFleet)
            val first = vm.state().player.ships
            vm.onIntent(GameIntent.RandomFleet)
            (vm.state().player.ships == first) shouldBe false
            vm.onIntent(GameIntent.Restart)
            runCurrent()
            vm.onIntent(GameIntent.RandomFleet)
            vm.state().player.ships shouldBe first
        }

    @Test
    fun `a shot fires at the enemy, then the AI answers after a pause on the player's own grid`() =
        runTest(dispatcher) {
            val vm = viewModel().toBattle()
            vm.uiState.value.canFire shouldBe true
            val water = vm.state().enemy.untried.first { vm.state().enemy.shipAt(it) == null }
            vm.onIntent(GameIntent.Tap(water))
            vm.state().enemy.resultAt(water) shouldBe ShotResult.MISS
            vm.uiState.value.announcement shouldBe Announcement.Shot(Side.PLAYER, water, ShotResult.MISS, null)
            played shouldBe listOf("miss")
            vm.uiState.value.aiFiring shouldBe true
            vm.uiState.value.canFire shouldBe false

            advanceTimeBy(AI_PAUSE - 1)
            vm.state().player.shots shouldBe emptyList()
            advanceTimeBy(2)
            vm.state().player.shots.size shouldBe 1
            advanceUntilIdle()
            vm.state().toMove shouldBe Side.PLAYER
            vm.uiState.value.aiFiring shouldBe false
            savedGames.saved.shouldNotBeNull().state shouldBe vm.state()
        }

    @Test
    fun `a tried cell is refused with the rejected feedback`() = runTest(dispatcher) {
        val vm = viewModel().toBattle()
        val target = vm.state().enemy.untried.first()
        vm.onIntent(GameIntent.Tap(target))
        advanceUntilIdle()
        played.clear()
        vm.onIntent(GameIntent.Tap(target))
        played shouldBe listOf("REJECTED")
    }

    @Test
    fun `a salvo is marked, fired when full, and its results shown one by one`() = runTest(dispatcher) {
        gameSettings.update { it.copy(mode = GameMode(BoardSize.TEN, FiringMode.SALVO, Opponent.MEDIUM)) }
        val vm = viewModel().toBattle()
        val cells = vm.state().enemy.untried.take(5)
        cells.take(4).forEach { vm.onIntent(GameIntent.Tap(it)) }
        vm.onIntent(GameIntent.FireSalvo)
        vm.state().salvoMarks shouldBe cells.take(4)
        vm.onIntent(GameIntent.Tap(cells[4]))
        vm.onIntent(GameIntent.FireSalvo)
        vm.state().enemy.shots shouldBe cells
        vm.uiState.value.hiddenShots shouldBe 5
        vm.uiState.value.revealing shouldBe Side.ENEMY
        vm.uiState.value.canFire shouldBe false

        advanceTimeBy(AI_PAUSE + 1)
        vm.uiState.value.hiddenShots shouldBe 4
        advanceTimeBy(4 * AI_PAUSE)
        // Only once every result showed does the AI take its turn: its salvo lands one by one too.
        vm.uiState.value.aiFiring shouldBe true
        vm.uiState.value.revealing shouldBe Side.PLAYER
        vm.uiState.value.hiddenShots shouldBe 5
        advanceUntilIdle()
        vm.state().player.shots.size shouldBe 5
        vm.uiState.value.canFire shouldBe true
    }

    @Test
    fun `a hint shows five cells, only against the AI, at most three`() = runTest(dispatcher) {
        val vm = viewModel().toBattle()
        vm.onIntent(GameIntent.Hint)
        vm.state().hint.size shouldBe 5
        vm.state().hintsUsed shouldBe 1
        vm.uiState.value.announcement.shouldBeInstanceOf<Announcement.Hinted>()
        // A hint already shows: asking again changes nothing.
        vm.onIntent(GameIntent.Hint)
        vm.state().hintsUsed shouldBe 1
    }

    @Test
    fun `sinking the last ship wins, records it and celebrates`() = runTest(dispatcher) {
        savedGames.saved = nearlyWon()
        val vm = viewModel()
        val last = vm.state().enemy.ships.flatMap { it!!.cells }.last()
        vm.onIntent(GameIntent.Tap(last))
        runCurrent()
        vm.uiState.value.ended shouldBe true
        vm.uiState.value.announcement shouldBe Announcement.Ended(Side.PLAYER, vsAi = true, resigned = false)
        played shouldBe listOf("sunk", "WIN")
        vm.uiState.value.earned shouldBe achievements.current.value.unlocked
        vm.uiState.value.earned.contains(io.github.vinaooo.battlegrid.domain.model.Achievement.FIRST_WIN) shouldBe true
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(1, 1, 1, 1))
        savedGames.saved shouldBe null
    }

    @Test
    fun `resigning is a loss, without a celebration`() = runTest(dispatcher) {
        val vm = viewModel().toBattle()
        vm.onIntent(GameIntent.Resign)
        runCurrent()
        vm.state().winner shouldBe Side.ENEMY
        vm.uiState.value.announcement shouldBe Announcement.Ended(Side.ENEMY, vsAi = true, resigned = true)
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(played = 1))
        played shouldBe emptyList()
    }

    @Test
    fun `a new game during the AI's turn drops its shot`() = runTest(dispatcher) {
        val vm = viewModel().toBattle()
        vm.onIntent(GameIntent.Tap(vm.state().enemy.untried.first { vm.state().enemy.shipAt(it) == null }))
        vm.uiState.value.aiFiring shouldBe true
        vm.onIntent(GameIntent.NewGame)
        advanceUntilIdle()
        vm.state().phase shouldBe Phase.Placement(Side.PLAYER)
        vm.state().player.shots shouldBe emptyList()
        vm.uiState.value.aiFiring shouldBe false
        // The battle left counts as a loss.
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(played = 1))
    }

    @Test
    fun `restart keeps the board's seed`() = runTest(dispatcher) {
        val vm = viewModel()
        val seed = vm.uiState.value.session!!.seed
        vm.onIntent(GameIntent.Restart)
        runCurrent()
        vm.uiState.value.session!!.seed shouldBe seed
    }

    @Test
    fun `reopening during the AI's turn lets it fire`() = runTest(dispatcher) {
        savedGames.saved = nearlyWon(GameMode(BoardSize.EIGHT, FiringMode.CLASSIC, Opponent.EASY), Side.ENEMY)
        val vm = viewModel()
        vm.uiState.value.aiFiring shouldBe true
        advanceTimeBy(AI_PAUSE + 1)
        vm.state().player.shots.size shouldBe BoardSize.EIGHT.fleet.sumOf { it.length }
    }

    @Test
    fun `the clock runs in battle while the screen shows, and pausing saves`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.Resume)
        advanceTimeBy(3_001)
        vm.state().elapsedSeconds shouldBe 0
        vm.toBattle()
        advanceTimeBy(2_001)
        vm.state().elapsedSeconds shouldBe 2
        vm.onIntent(GameIntent.Pause)
        runCurrent()
        savedGames.saved.shouldNotBeNull().state.elapsedSeconds shouldBe 2
        advanceTimeBy(5_000)
        vm.state().elapsedSeconds shouldBe 2
    }

    @Test
    fun `a mode changed in Settings starts a game in it`() = runTest(dispatcher) {
        val vm = viewModel()
        val salvo = GameMode(BoardSize.EIGHT, FiringMode.SALVO, Opponent.HARD)
        gameSettings.update { it.copy(mode = salvo) }
        runCurrent()
        vm.state().mode shouldBe salvo
    }

    @Test
    fun `pass-and-play covers the screen at every handover`() = runTest(dispatcher) {
        gameSettings.update { it.copy(mode = GameMode(BoardSize.EIGHT, FiringMode.CLASSIC, Opponent.TWO_PLAYER)) }
        val vm = viewModel()
        vm.uiState.value.viewer shouldBe Side.PLAYER
        vm.toBattle()
        // Player 1 placed: the phone goes to player 2, who places next.
        vm.uiState.value.covered shouldBe true
        vm.uiState.value.announcement shouldBe Announcement.Handover(Side.ENEMY)
        vm.uiState.value.canPlace shouldBe false
        vm.onIntent(GameIntent.Uncover)
        vm.uiState.value.viewer shouldBe Side.ENEMY
        vm.uiState.value.canPlace shouldBe true
        vm.toBattle()
        // Both placed: the phone goes back to player 1, who fires first.
        vm.uiState.value.covered shouldBe true
        vm.onIntent(GameIntent.Uncover)
        vm.uiState.value.viewer shouldBe Side.PLAYER
        vm.uiState.value.canFire shouldBe true

        val water = vm.state().enemy.untried.first { vm.state().enemy.shipAt(it) == null }
        vm.onIntent(GameIntent.Tap(water))
        // The result shows for a moment, then the cover.
        vm.uiState.value.covered shouldBe false
        advanceTimeBy(RESULT_LOOK + 1)
        vm.uiState.value.covered shouldBe true
        vm.onIntent(GameIntent.Uncover)
        vm.uiState.value.viewer shouldBe Side.ENEMY
    }

    @Test
    fun `pass-and-play has no hints and records nothing`() = runTest(dispatcher) {
        gameSettings.update { it.copy(mode = GameMode(BoardSize.EIGHT, FiringMode.CLASSIC, Opponent.TWO_PLAYER)) }
        val vm = viewModel().toBattle()
        vm.onIntent(GameIntent.Uncover)
        vm.toBattle()
        vm.onIntent(GameIntent.Uncover)
        vm.onIntent(GameIntent.Hint)
        vm.state().hintsUsed shouldBe 0
        vm.onIntent(GameIntent.Resign)
        advanceUntilIdle()
        vm.state().winner shouldBe Side.ENEMY
        stats.stats.value shouldBe emptyMap()
    }

    @Test
    fun `the daily challenge starts from the menu, and a new game clears earned badges`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.Daily)
        runCurrent()
        vm.state().mode shouldBe GameMode.DAILY
        vm.uiState.value.session!!.recorded shouldBe true
        vm.uiState.value.earned shouldBe emptySet()
    }

    @Test
    fun `How to play is due on the first launch only`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.value.howToPlayDue shouldBe true
        vm.onIntent(GameIntent.HowToPlaySeen)
        runCurrent()
        vm.uiState.value.howToPlayDue shouldBe false
        gameSettings.current.value.howToPlaySeen shouldBe true
        viewModel().uiState.value.howToPlayDue shouldBe false
    }

    private companion object {
        const val AI_PAUSE = 600L
        const val RESULT_LOOK = 1_000L
    }
}
