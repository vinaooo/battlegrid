package io.github.vinaooo.battlegrid.domain.usecase

import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.Achievements
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.FleetPlacer
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.repository.AchievementRepository
import io.github.vinaooo.battlegrid.domain.repository.Clock
import io.github.vinaooo.battlegrid.domain.repository.DailyRecord
import io.github.vinaooo.battlegrid.domain.repository.DailyRepository
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import io.github.vinaooo.battlegrid.domain.repository.SavedGameRepository
import io.github.vinaooo.battlegrid.domain.repository.SeedSource
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.ShotsScoring
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/** The daily challenge of UTC day [day] (days since the epoch): its seed, the same for everyone. Never change it. */
fun dailySeed(day: Long): Long = day * DAILY_SEED_FACTOR + DAILY_SEED_OFFSET

/** The UTC day of a daily challenge's [seed]. */
fun dailyDay(seed: Long): Long = (seed - DAILY_SEED_OFFSET) / DAILY_SEED_FACTOR

private const val DAILY_SEED_FACTOR = 1_000_003L
private const val DAILY_SEED_OFFSET = 7_919L
private const val DAY_MILLIS = 86_400_000L

/** What a game adds beyond its own stats: the daily challenge's streak and the badges. */
class RecordProgress(
    private val dailyRecords: DailyRepository,
    private val achievements: AchievementRepository,
    val stats: StatsRepository,
) {
    /** The day was played in [seed]'s ranked daily: a day after the last one adds to the streak. Returns the streak. */
    suspend fun daily(seed: Long): Int {
        val day = dailyDay(seed)
        dailyRecords.update {
            when (it.lastRankedDay) {
                day -> it
                day - 1 -> DailyRecord(day, it.streak + 1)
                else -> DailyRecord(day, 1)
            }
        }
        return dailyRecords.record.first().streak
    }

    suspend fun lastRankedDay(): Long? = dailyRecords.record.first().lastRankedDay

    /** Badges after [state], a finished game against the AI; returns the ones just earned. */
    suspend fun achievements(state: GameState, streak: Int, dailyStreak: Int?): Set<Achievement> {
        var earned = emptySet<Achievement>()
        achievements.update { before ->
            Achievements.after(before, state, streak, dailyStreak).also { earned = it.unlocked - before.unlocked }
        }
        return earned
    }
}

/**
 * The saved game is being replaced: a recorded battle against the AI still going counts as a loss (a ranked daily
 * uses up its day), and the AI fires first next. A placement or a pass-and-play game isn't recorded.
 */
private suspend fun abandonSaved(
    savedGames: SavedGameRepository,
    gameSettings: GameSettingsRepository,
    progress: RecordProgress,
) {
    val left = savedGames.load()?.takeIf { it.isInProgress && it.state.mode.isVsAi && it.recorded } ?: return
    progress.stats.update(left.state.mode.key, GameStats::afterLoss)
    if (left.state.mode.daily) progress.daily(left.seed)
    gameSettings.update { it.copy(nextFirstMover = Side.ENEMY) }
}

/** A new game from [seed]: the AI's fleet placed from it, the player placing theirs. */
private fun newSession(seed: Long, mode: GameMode, firstMover: Side, placer: FleetPlacer, engine: GameEngine) =
    GameSession(seed, engine.newGame(mode, firstMover, placer.place(mode.size, Random(seed)).takeIf { mode.isVsAi }))

/**
 * Starts a game in [GameMode] (Settings' mode by default). The last game's winner fires first; the very first game
 * draws it from the seed.
 */
class StartNewGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val progress: RecordProgress,
    private val seeds: SeedSource,
    private val engine: GameEngine,
    private val placer: FleetPlacer = RandomFleetPlacer,
) {
    suspend operator fun invoke(mode: GameMode? = null): GameSession {
        abandonSaved(savedGames, gameSettings, progress)
        val settings = gameSettings.settings.first()
        val seed = seeds.nextSeed()
        val first = settings.nextFirstMover ?: Side.entries[Random(seed).nextInt(Side.entries.size)]
        val session = newSession(seed, mode ?: settings.mode, first, placer, engine)
        savedGames.save(session)
        return session
    }
}

/**
 * The same board again: the same seed, so the same AI fleet and first mover; the player places again. A ranked daily
 * restarted mid-battle has used up its day: the board again is a replay.
 */
class RestartGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val progress: RecordProgress,
    private val engine: GameEngine,
    private val placer: FleetPlacer = RandomFleetPlacer,
) {
    suspend operator fun invoke(session: GameSession): GameSession {
        abandonSaved(savedGames, gameSettings, progress)
        val recorded = !session.state.mode.daily || (session.recorded && !session.isInProgress)
        val again = newSession(session.seed, session.state.mode, session.state.firstMover, placer, engine)
            .copy(recorded = recorded)
        savedGames.save(again)
        return again
    }
}

/**
 * The day's daily challenge (UTC): 10×10 Classic against Hard, the same AI fleet and AI random stream for everyone,
 * the player firing first. The first one played that day is ranked; later ones are replays.
 */
class StartDailyGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val progress: RecordProgress,
    private val clock: Clock,
    private val engine: GameEngine,
) {
    suspend operator fun invoke(): GameSession {
        abandonSaved(savedGames, gameSettings, progress)
        val day = clock.nowMillis() / DAY_MILLIS
        val ranked = progress.lastRankedDay() != day
        val session = newSession(dailySeed(day), GameMode.DAILY, Side.PLAYER, RandomFleetPlacer, engine)
            .copy(recorded = ranked)
        savedGames.save(session)
        return session
    }
}

/** The saved game, if one was left: in placement or in battle. */
class ResumeGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(): GameSession? = savedGames.load()
}

class SaveGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(session: GameSession) = savedGames.save(session)
}

/**
 * Records a finished game against the AI: a win with its score, a loss (a resignation too) in the stats, a ranked
 * daily's day, and the badges, returning the ones just earned. Its winner fires first next game, also in
 * pass-and-play, which isn't recorded. Clears the save.
 */
class FinishGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val scores: ScoreRepository,
    private val clock: Clock,
    private val progress: RecordProgress,
) {
    suspend operator fun invoke(session: GameSession): Set<Achievement> {
        val state = session.state
        val winner = checkNotNull(state.winner) { "Only a finished game can be recorded" }
        val earned = if (state.mode.isVsAi && session.recorded) record(session, winner) else emptySet()
        gameSettings.update { it.copy(nextFirstMover = winner) }
        savedGames.clear()
        return earned
    }

    private suspend fun record(session: GameSession, winner: Side): Set<Achievement> {
        val state = session.state
        val key = state.mode.key
        val stats = progress.stats
        if (winner == Side.PLAYER) {
            stats.update(key, GameStats::afterWin)
            scores.add(
                ScoreRecord(
                    mode = key,
                    points = ShotsScoring.points(state),
                    elapsedSeconds = state.elapsedSeconds,
                    playedAtMillis = clock.nowMillis(),
                    extras = mapOf(
                        SHOTS to "${state.playerShots}",
                        HITS to "${state.enemy.hits}",
                        HINTS to "${state.hintsUsed}",
                    ),
                ),
            )
        } else {
            stats.update(key, GameStats::afterLoss)
        }
        val streak = stats.observe(key).first().currentStreak
        val dailyStreak = if (state.mode.daily) progress.daily(session.seed) else null
        return progress.achievements(state, streak, dailyStreak)
    }

    companion object {
        /** A score row's extras: shots fired, hits among them, hints used. */
        const val SHOTS = "shots"
        const val HITS = "hits"
        const val HINTS = "hints"
    }
}
