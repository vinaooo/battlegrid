package io.github.vinaooo.battlegrid.domain.fake

import io.github.vinaooo.battlegrid.domain.repository.Clock
import io.github.vinaooo.battlegrid.domain.repository.GameSettings
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import io.github.vinaooo.battlegrid.domain.repository.SavedGameRepository
import io.github.vinaooo.battlegrid.domain.repository.SeedSource
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSavedGameRepository(var saved: GameSession? = null) : SavedGameRepository {
    override suspend fun load(): GameSession? = saved

    override suspend fun save(session: GameSession) {
        saved = session
    }

    override suspend fun clear() {
        saved = null
    }
}

class FakeGameSettingsRepository(initial: GameSettings = GameSettings()) : GameSettingsRepository {
    val current = MutableStateFlow(initial)

    override val settings: Flow<GameSettings> = current

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        current.value = transform(current.value)
    }
}

class FakeStatsRepository(initial: Map<String, GameStats> = emptyMap()) : StatsRepository {
    val stats = MutableStateFlow(initial)

    override fun observe(mode: String): Flow<GameStats> = stats.map { it[mode] ?: GameStats() }

    override fun observePlayedModes(): Flow<Set<String>> = stats.map { all -> all.filterValues { it.played > 0 }.keys }

    override suspend fun update(mode: String, transform: (GameStats) -> GameStats) {
        stats.value = stats.value + (mode to transform(stats.value[mode] ?: GameStats()))
    }
}

class FakeScoreRepository : ScoreRepository {
    val records = MutableStateFlow(emptyList<ScoreRecord>())

    override fun observeTopScores(mode: String, ranking: Ranking, limit: Int): Flow<List<ScoreRecord>> =
        records.map { all -> all.filter { it.mode == mode }.sortedWith(ranking.comparator).take(limit) }

    override suspend fun add(record: ScoreRecord) {
        records.value = records.value + record
    }
}

/** Seeds 1, 2, 3, … */
class FakeSeedSource : SeedSource {
    private var next = 0L

    override fun nextSeed(): Long = ++next
}

class FakeClock(var now: Long = 0) : Clock {
    override fun nowMillis(): Long = now
}
