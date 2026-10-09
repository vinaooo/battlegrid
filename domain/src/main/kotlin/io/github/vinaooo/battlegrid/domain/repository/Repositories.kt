package io.github.vinaooo.battlegrid.domain.repository

import io.github.vinaooo.battlegrid.domain.model.AchievementProgress
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.session.GameSession
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun load(): GameSession?

    suspend fun save(session: GameSession)

    suspend fun clear()
}

/** BattleGrid's own settings, beside vinkit's `AppSettings`. */
data class GameSettings(
    /** The mode of the next new game; the game in progress keeps its own. */
    val mode: GameMode = GameMode.DEFAULT,
    /** Who fires first next game: the last game's winner; null before the first game, which draws it at random. */
    val nextFirstMover: Side? = null,
    /** The How-to-play screen was seen (it opens by itself on the first launch only). */
    val howToPlaySeen: Boolean = false,
)

interface GameSettingsRepository {
    val settings: Flow<GameSettings>

    suspend fun update(transform: (GameSettings) -> GameSettings)
}

/** The daily challenge: the last UTC day (days since the epoch) whose ranked game was played, and the days in a row. */
data class DailyRecord(val lastRankedDay: Long? = null, val streak: Int = 0)

interface DailyRepository {
    val record: Flow<DailyRecord>

    suspend fun update(transform: (DailyRecord) -> DailyRecord)
}

interface AchievementRepository {
    val progress: Flow<AchievementProgress>

    suspend fun update(transform: (AchievementProgress) -> AchievementProgress)
}

fun interface SeedSource {
    fun nextSeed(): Long
}

fun interface Clock {
    fun nowMillis(): Long
}
