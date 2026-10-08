package io.github.vinaooo.battlegrid.domain.usecase

import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.FleetPlacer
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.repository.Clock
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

/**
 * The saved game is being replaced: a battle against the AI still going counts as a loss, and the AI fires first
 * next. A placement or a pass-and-play game isn't recorded.
 */
private suspend fun abandonSaved(
    savedGames: SavedGameRepository,
    gameSettings: GameSettingsRepository,
    stats: StatsRepository,
) {
    val left = savedGames.load()?.takeIf { it.isInProgress && it.state.mode.isVsAi } ?: return
    stats.update(left.state.mode.key, GameStats::afterLoss)
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
    private val stats: StatsRepository,
    private val seeds: SeedSource,
    private val engine: GameEngine,
    private val placer: FleetPlacer = RandomFleetPlacer,
) {
    suspend operator fun invoke(mode: GameMode? = null): GameSession {
        abandonSaved(savedGames, gameSettings, stats)
        val settings = gameSettings.settings.first()
        val seed = seeds.nextSeed()
        val first = settings.nextFirstMover ?: Side.entries[Random(seed).nextInt(Side.entries.size)]
        val session = newSession(seed, mode ?: settings.mode, first, placer, engine)
        savedGames.save(session)
        return session
    }
}

/** The same board again: the same seed, so the same AI fleet and first mover; the player places again. */
class RestartGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val stats: StatsRepository,
    private val engine: GameEngine,
    private val placer: FleetPlacer = RandomFleetPlacer,
) {
    suspend operator fun invoke(session: GameSession): GameSession {
        abandonSaved(savedGames, gameSettings, stats)
        val again = newSession(session.seed, session.state.mode, session.state.firstMover, placer, engine)
        savedGames.save(again)
        return again
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
 * Records a finished game against the AI: a win with its score, a loss (a resignation too) in the stats. Its winner
 * fires first next game, also in pass-and-play, which isn't recorded. Clears the save.
 */
class FinishGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val stats: StatsRepository,
    private val scores: ScoreRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(session: GameSession) {
        val state = session.state
        val winner = checkNotNull(state.winner) { "Only a finished game can be recorded" }
        if (state.mode.isVsAi) {
            val key = state.mode.key
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
        }
        gameSettings.update { it.copy(nextFirstMover = winner) }
        savedGames.clear()
    }

    companion object {
        /** A score row's extras: shots fired, hits among them, hints used. */
        const val SHOTS = "shots"
        const val HITS = "hits"
        const val HINTS = "hints"
    }
}
