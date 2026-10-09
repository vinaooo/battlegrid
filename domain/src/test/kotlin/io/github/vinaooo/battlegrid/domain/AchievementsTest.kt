package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.AchievementProgress
import io.github.vinaooo.battlegrid.domain.model.Achievements
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AchievementsTest {
    private val engine = GameEngine()

    /** A win: the player sinks the enemy's [rowFleet] with [misses] first, the AI missing in between. */
    private fun won(
        size: BoardSize = BoardSize.TEN,
        firing: FiringMode = FiringMode.HIT_AGAIN,
        opponent: Opponent = Opponent.MEDIUM,
        misses: List<Coord> = emptyList(),
        hints: Int = 0,
    ): GameState {
        val battle = engine.battle(mode(size, firing, opponent))
        val shots = misses + rowFleet(size).flatMap { it.cells }
        val enemy = battle.enemy.copy(shots = shots)
        return battle.copy(
            enemy = enemy,
            hintsUsed = hints,
            phase = io.github.vinaooo.battlegrid.domain.model.Phase.Over(Side.PLAYER),
        )
    }

    private fun lost(state: GameState) =
        state.copy(phase = io.github.vinaooo.battlegrid.domain.model.Phase.Over(Side.ENEMY))

    @Test
    fun `a first clean win without hints earns its badges`() {
        val after = Achievements.after(AchievementProgress(), won(), streak = 1, dailyStreak = null)
        after.unlocked shouldContainExactlyInAnyOrder listOf(
            Achievement.FIRST_WIN,
            Achievement.NO_HINT_WIN,
            Achievement.CLEAN_SINK,
            Achievement.ACCURACY_60,
        )
        after.sizesWon shouldBe setOf(BoardSize.TEN)
        after.firingsWon shouldBe setOf(FiringMode.HIT_AGAIN)
    }

    @Test
    fun `hints, misses and a weak opponent keep some badges locked`() {
        // A miss between every two hits: no clean sink, and 17 hits in 34 shots is 50%.
        val water = (0 until 10).map { Coord(1, it) } + (0 until 7).map { Coord(3, it + 3) }
        val misses = won(misses = water, hints = 1)
        val interleaved = misses.copy(
            enemy = misses.enemy.copy(
                shots = rowFleet(BoardSize.TEN).flatMap {
                    it.cells
                }.zip(water) { a, b -> listOf(a, b) }.flatten(),
            ),
        )
        Achievements.after(AchievementProgress(), interleaved, streak = 1, dailyStreak = null).unlocked shouldBe
            setOf(Achievement.FIRST_WIN)
    }

    @Test
    fun `hard falls, and every size and firing mode won adds up over games`() {
        var progress = AchievementProgress()
        BoardSize.entries.forEachIndexed { i, size ->
            progress = Achievements.after(progress, won(size, FiringMode.entries[i], Opponent.HARD), 1, null)
        }
        progress.unlocked.containsAll(
            listOf(Achievement.BEAT_HARD, Achievement.WIN_EVERY_SIZE, Achievement.WIN_EVERY_FIRING),
        ) shouldBe true
        Achievements.after(AchievementProgress(), won(opponent = Opponent.MEDIUM), 1, null)
            .unlocked.contains(Achievement.BEAT_HARD) shouldBe false
    }

    @Test
    fun `streaks of five wins, and of seven days of the daily`() {
        Achievements.after(AchievementProgress(), won(), streak = 5, dailyStreak = null)
            .unlocked.contains(Achievement.WIN_STREAK_5) shouldBe true
        Achievements.after(AchievementProgress(), won(), streak = 4, dailyStreak = 6)
            .unlocked.contains(Achievement.WIN_STREAK_5) shouldBe false
        // The daily streak counts days played, won or lost.
        Achievements.after(AchievementProgress(), lost(won()), streak = 0, dailyStreak = 7).unlocked shouldBe
            setOf(Achievement.DAILY_STREAK_7, Achievement.CLEAN_SINK)
    }

    @Test
    fun `a loss earns no win badges and adds no size or firing mode`() {
        val after = Achievements.after(AchievementProgress(), lost(won(misses = listOf(Coord(1, 0)))), 0, null)
        after.unlocked shouldBe setOf(Achievement.CLEAN_SINK)
        after.sizesWon shouldBe emptySet()
    }

    @Test
    fun `a clean sink is a ship's hits with no miss among the player's shots between them`() {
        // The destroyer hit, a miss, then sunk: not clean; the carrier hit five times in a row: clean.
        val battle = engine.battle()
        val dirty = battle.copy(enemy = battle.enemy.copy(shots = listOf(Coord(8, 0), Coord(1, 1), Coord(8, 1))))
        Achievements.cleanSink(dirty) shouldBe false
        val other = battle.copy(enemy = battle.enemy.copy(shots = listOf(Coord(8, 0), Coord(0, 0), Coord(8, 1))))
        // Another ship's hit in between is no miss.
        Achievements.cleanSink(other) shouldBe true
        Achievements.cleanSink(battle) shouldBe false
    }

    @Test
    fun `earlier badges stay, and the daily streak alone counts without a win`() {
        val start = AchievementProgress(unlocked = setOf(Achievement.FIRST_WIN))
        Achievements.after(start, lost(engine.play(engine.battle(), Move.Resign(Side.PLAYER))), 0, 1).unlocked shouldBe
            setOf(Achievement.FIRST_WIN)
    }
}
