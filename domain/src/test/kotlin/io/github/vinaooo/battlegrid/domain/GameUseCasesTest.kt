package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.fake.FakeClock
import io.github.vinaooo.battlegrid.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeScoreRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSeedSource
import io.github.vinaooo.battlegrid.domain.fake.FakeStatsRepository
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.repository.GameSettings
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.domain.usecase.FinishGame
import io.github.vinaooo.battlegrid.domain.usecase.RestartGame
import io.github.vinaooo.battlegrid.domain.usecase.ResumeGame
import io.github.vinaooo.battlegrid.domain.usecase.SaveGame
import io.github.vinaooo.battlegrid.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GameUseCasesTest {
    private val engine = GameEngine()
    private val saved = FakeSavedGameRepository()
    private val settings = FakeGameSettingsRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()
    private val clock = FakeClock(now = 1_000)
    private val start = StartNewGame(saved, settings, stats, FakeSeedSource(), engine)
    private val restart = RestartGame(saved, settings, stats, engine)
    private val finish = FinishGame(saved, settings, stats, scores, clock)

    private fun GameSession.inBattle(): GameSession =
        play(Move.SetFleet(rowFleet(state.mode.size)), engine)!!.let { it.play(Move.ConfirmFleet, engine) ?: it }

    /** Plays [side]'s shots at every ship of the other side until the battle is over. */
    private fun GameSession.wonBy(side: Side): GameSession {
        var session = this
        while (!session.state.isOver) {
            val state = session.state
            val target = state.target.ships.flatMap { it!!.cells }.firstOrNull { !state.target.isTried(it) }
            val shot = if (state.toMove ==
                side
            ) {
                target!!
            } else {
                state.target.untried.first { state.target.shipAt(it) == null }
            }
            session = session.play(Move.Fire(shot), engine)!!
        }
        return session
    }

    @Test
    fun `a new game against the AI comes with the AI's fleet from the seed, waiting for the player's`() = runTest {
        val session = start()
        session.seed shouldBe 1
        session.state.mode shouldBe GameMode.DEFAULT
        session.state.phase shouldBe Phase.Placement(Side.PLAYER)
        session.state.enemy.ships shouldBe RandomFleetPlacer.place(BoardSize.TEN, Random(1))
        saved.saved shouldBe session
    }

    @Test
    fun `the very first game draws who fires first from the seed, later ones follow the last winner`() = runTest {
        start().state.firstMover shouldBe Side.entries[Random(1).nextInt(2)]
        settings.update { it.copy(nextFirstMover = Side.ENEMY) }
        start().state.firstMover shouldBe Side.ENEMY
        settings.update { it.copy(nextFirstMover = Side.PLAYER) }
        start().state.firstMover shouldBe Side.PLAYER
    }

    @Test
    fun `a pass-and-play game waits for both fleets, in the mode asked for`() = runTest {
        val mode = GameMode(BoardSize.EIGHT, FiringMode.SALVO, Opponent.TWO_PLAYER)
        val session = start(mode)
        session.state.mode shouldBe mode
        session.state.enemy.isFleetComplete shouldBe false
    }

    @Test
    fun `leaving a battle against the AI counts as a loss, and the AI fires first next`() = runTest {
        start().inBattle().also { saved.save(it) }
        start()
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(played = 1))
        settings.current.value.nextFirstMover shouldBe Side.ENEMY
    }

    @Test
    fun `leaving during placement, a finished game or a pass-and-play battle is not recorded`() = runTest {
        start()
        start()
        start(GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.TWO_PLAYER)).inBattle().inBattle()
            .also { saved.save(it) }
        start()
        stats.stats.value shouldBe emptyMap()
    }

    @Test
    fun `restarting keeps the seed, the AI's fleet and who fires first, and the player places again`() = runTest {
        val battle = start().inBattle().also { saved.save(it) }
        val again = restart(battle)
        again.seed shouldBe battle.seed
        again.state.enemy.ships shouldBe battle.state.enemy.ships
        again.state.firstMover shouldBe battle.state.firstMover
        again.state.phase shouldBe Phase.Placement(Side.PLAYER)
        again.state.player.ships.all { it == null } shouldBe true
        saved.saved shouldBe again
        // The battle left behind counts as a loss.
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(played = 1))
    }

    @Test
    fun `a win is recorded with its score, time and details, and the player fires first next`() = runTest {
        val won = start().inBattle().let { it.copy(state = it.state.copy(hintsUsed = 1, elapsedSeconds = 90)) }
            .wonBy(Side.PLAYER)
        finish(won)
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(1, 1, 1, 1))
        scores.records.value shouldBe listOf(
            ScoreRecord(
                GameMode.DEFAULT.key,
                points = won.state.playerShots + 5,
                elapsedSeconds = 90,
                playedAtMillis = 1_000,
                extras = mapOf("shots" to "${won.state.playerShots}", "hits" to "17", "hints" to "1"),
            ),
        )
        saved.saved shouldBe null
        settings.current.value shouldBe GameSettings(nextFirstMover = Side.PLAYER)
    }

    @Test
    fun `a loss is recorded without a score, and the AI fires first next`() = runTest {
        finish(start().inBattle().wonBy(Side.ENEMY))
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(played = 1))
        scores.records.value shouldBe emptyList()
        settings.current.value.nextFirstMover shouldBe Side.ENEMY
    }

    @Test
    fun `a resignation is a loss`() = runTest {
        finish(start().inBattle().play(Move.Resign(Side.PLAYER), engine)!!)
        stats.stats.value shouldBe mapOf(GameMode.DEFAULT.key to GameStats(played = 1))
    }

    @Test
    fun `a pass-and-play game only passes the first shot to its winner`() = runTest {
        val twoPlayer = start(GameMode(BoardSize.EIGHT, FiringMode.CLASSIC, Opponent.TWO_PLAYER)).inBattle().inBattle()
        finish(twoPlayer.wonBy(Side.ENEMY))
        stats.stats.value shouldBe emptyMap()
        scores.records.value shouldBe emptyList()
        settings.current.value.nextFirstMover shouldBe Side.ENEMY
        saved.saved shouldBe null
    }

    @Test
    fun `only a finished game is recorded`() = runTest {
        shouldThrow<IllegalStateException> { finish(start().inBattle()) }
    }

    @Test
    fun `resume and save go through the saved game`() = runTest {
        ResumeGame(saved)() shouldBe null
        val session = start()
        SaveGame(saved)(session.inBattle())
        ResumeGame(saved)() shouldBe session.inBattle()
        settings.settings.first().mode shouldBe GameMode.DEFAULT
    }
}
