package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.domain.usecase.ResumeGame
import io.github.vinaooo.battlegrid.feature.game.settings.PendingMode
import io.github.vinaooo.battlegrid.feature.game.settings.SettingsViewModel
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.ThemeMode
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val engine = GameEngine()
    private val savedGames = FakeSavedGameRepository()
    private val gameSettings = FakeGameSettingsRepository()
    private val appSettings = object : AppSettingsRepository {
        val current = MutableStateFlow(AppSettings())
        override val settings = current

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            current.value = transform(current.value)
        }
    }
    private val salvo = GameMode(BoardSize.TWELVE, FiringMode.SALVO, Opponent.HARD)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(appSettings, gameSettings, ResumeGame(savedGames))

    private fun placing(opponent: Opponent): GameSession {
        val mode = GameMode(BoardSize.EIGHT, FiringMode.CLASSIC, opponent)
        val fleet = RandomFleetPlacer.place(BoardSize.EIGHT, Random(1))
        return GameSession(1, engine.newGame(mode, Side.PLAYER, fleet.takeIf { mode.isVsAi }))
    }

    private fun inBattle(opponent: Opponent): GameSession {
        var session = placing(opponent)
        while (!session.state.isBattle) {
            session = session.play(Move.SetFleet(RandomFleetPlacer.place(BoardSize.EIGHT, Random(2))), engine)!!
                .play(Move.ConfirmFleet, engine)!!
        }
        return session
    }

    @Test
    fun `with no game on, a new mode applies at once`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onModeChange(salvo)
        advanceUntilIdle()
        gameSettings.current.value.mode shouldBe salvo
        vm.pendingMode.value.shouldBeNull()
    }

    @Test
    fun `during placement no shot was fired, so a new mode applies at once`() = runTest(dispatcher) {
        savedGames.saved = placing(Opponent.EASY)
        val vm = viewModel()
        vm.onModeChange(salvo)
        advanceUntilIdle()
        gameSettings.current.value.mode shouldBe salvo
    }

    @Test
    fun `with a battle against the AI on, it asks first, saying the game counts as a loss`() = runTest(dispatcher) {
        savedGames.saved = inBattle(Opponent.EASY)
        val vm = viewModel()
        vm.onModeChange(salvo)
        advanceUntilIdle()
        vm.pendingMode.value shouldBe PendingMode(salvo, countsAsLoss = true)
        gameSettings.current.value.mode shouldBe GameMode.DEFAULT
        vm.confirmMode()
        advanceUntilIdle()
        gameSettings.current.value.mode shouldBe salvo
        vm.pendingMode.value.shouldBeNull()
    }

    @Test
    fun `a pass-and-play battle also asks, without the loss, and dismissing keeps the mode`() = runTest(dispatcher) {
        savedGames.saved = inBattle(Opponent.TWO_PLAYER)
        val vm = viewModel()
        vm.onModeChange(salvo)
        advanceUntilIdle()
        vm.pendingMode.value shouldBe PendingMode(salvo, countsAsLoss = false)
        vm.dismissMode()
        advanceUntilIdle()
        vm.pendingMode.value.shouldBeNull()
        gameSettings.current.value.mode shouldBe GameMode.DEFAULT
    }

    @Test
    fun `appearance changes go to vinkit's settings`() = runTest(dispatcher) {
        viewModel().onAppChange { it.copy(themeMode = ThemeMode.DARK) }
        advanceUntilIdle()
        appSettings.current.value.themeMode shouldBe ThemeMode.DARK
    }
}
