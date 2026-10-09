package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.fake.FakeAchievementRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeClock
import io.github.vinaooo.battlegrid.domain.fake.FakeDailyRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeScoreRepository
import io.github.vinaooo.battlegrid.domain.fake.FakeSeedSource
import io.github.vinaooo.battlegrid.domain.fake.FakeStatsRepository
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.model.badges
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.repository.DailyRecord
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.battlegrid.domain.usecase.FinishGame
import io.github.vinaooo.battlegrid.domain.usecase.RecordProgress
import io.github.vinaooo.battlegrid.domain.usecase.StartDailyGame
import io.github.vinaooo.battlegrid.domain.usecase.StartNewGame
import io.github.vinaooo.battlegrid.domain.usecase.dailyDay
import io.github.vinaooo.battlegrid.domain.usecase.dailySeed
import io.github.vinaooo.vinkit.core.GameStats
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DailyTest {
    private val engine = GameEngine()
    private val saved = FakeSavedGameRepository()
    private val settings = FakeGameSettingsRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()
    private val daily = FakeDailyRepository()
    private val achievements = FakeAchievementRepository()
    private val clock = FakeClock(now = 20_000L * DAY + 5_000)
    private val progress = RecordProgress(daily, achievements, stats)
    private val startDaily = StartDailyGame(saved, settings, progress, clock, engine)
    private val start = StartNewGame(saved, settings, progress, FakeSeedSource(), engine)
    private val finish = FinishGame(saved, settings, scores, clock, progress)

    private fun GameSession.inBattle(): GameSession =
        play(Move.SetFleet(rowFleet(state.mode.size)), engine)!!.play(Move.ConfirmFleet, engine)!!

    @Test
    fun `the daily is 10x10 Classic against Hard, the same board for everyone that UTC day`() = runTest {
        val today = startDaily()
        today.state.mode shouldBe GameMode.DAILY
        today.seed shouldBe dailySeed(20_000)
        dailyDay(today.seed) shouldBe 20_000
        today.state.enemy.ships shouldBe RandomFleetPlacer.place(GameMode.DAILY.size, Random(dailySeed(20_000)))
        today.state.firstMover shouldBe Side.PLAYER
        today.recorded shouldBe true
        clock.now = 20_001L * DAY
        (startDaily().seed == today.seed) shouldBe false
    }

    @Test
    fun `the first daily battle of the day is ranked, a replay isn't`() = runTest {
        finish(startDaily().inBattle().play(Move.Resign(Side.PLAYER), engine)!!)
        stats.stats.value shouldBe mapOf("DAILY" to GameStats(played = 1))
        daily.current.value shouldBe DailyRecord(lastRankedDay = 20_000, streak = 1)

        val replay = startDaily()
        replay.recorded shouldBe false
        finish(replay.inBattle().play(Move.Resign(Side.PLAYER), engine)!!)
        stats.stats.value shouldBe mapOf("DAILY" to GameStats(played = 1))
    }

    @Test
    fun `leaving a ranked daily battle uses up the day's attempt, leaving its placement doesn't`() = runTest {
        startDaily()
        start()
        daily.current.value shouldBe DailyRecord()
        startDaily().inBattle().also { saved.save(it) }
        start()
        stats.stats.value shouldBe mapOf("DAILY" to GameStats(played = 1))
        daily.current.value.lastRankedDay shouldBe 20_000
        startDaily().recorded shouldBe false
    }

    @Test
    fun `days played in a row make the streak, a day missed restarts it`() = runTest {
        daily.current.value = DailyRecord(lastRankedDay = 19_999, streak = 6)
        val result = finish(startDaily().inBattle().play(Move.Resign(Side.PLAYER), engine)!!)
        daily.current.value shouldBe DailyRecord(20_000, 7)
        result shouldContain Achievement.DAILY_STREAK_7
        achievements.current.value.badges shouldContain Achievement.DAILY_STREAK_7

        daily.current.value = DailyRecord(lastRankedDay = 19_990, streak = 6)
        clock.now = 20_000L * DAY
        saved.saved = null
        progress.daily(dailySeed(20_000))
        daily.current.value shouldBe DailyRecord(20_000, 1)
    }

    @Test
    fun `a normal win unlocks badges and reports the new ones`() = runTest {
        val battle = start().inBattle()
        val won = battle.copy(
            state = battle.state.copy(
                enemy = battle.state.enemy.copy(shots = battle.state.enemy.ships.flatMap { it!!.cells }),
                phase = io.github.vinaooo.battlegrid.domain.model.Phase.Over(Side.PLAYER),
            ),
        )
        val first = finish(won)
        first shouldContain Achievement.FIRST_WIN
        finish(won).contains(Achievement.FIRST_WIN) shouldBe false
    }

    @Test
    fun `pass-and-play earns no badges`() = runTest {
        val mode = GameMode.DEFAULT.copy(opponent = io.github.vinaooo.battlegrid.domain.model.Opponent.TWO_PLAYER)
        val twoPlayer = start(mode).inBattle().inBattle()
        finish(twoPlayer.play(Move.Resign(Side.ENEMY), engine)!!) shouldBe emptySet()
        achievements.current.value.badges shouldBe emptySet()
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
