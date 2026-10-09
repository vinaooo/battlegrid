package io.github.vinaooo.battlegrid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.AchievementProgress
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.repository.AchievementRepository
import io.github.vinaooo.battlegrid.domain.repository.DailyRecord
import io.github.vinaooo.battlegrid.domain.repository.DailyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The daily challenge's last ranked day and streak, in the shared Preferences DataStore (its own keys only). */
class DataStoreDailyRepository(private val dataStore: DataStore<Preferences>) : DailyRepository {
    override val record: Flow<DailyRecord> = dataStore.data.map { it.toRecord() }

    override suspend fun update(transform: (DailyRecord) -> DailyRecord) {
        dataStore.edit { prefs ->
            val record = transform(prefs.toRecord())
            val day = record.lastRankedDay
            if (day == null) prefs.remove(LAST_DAY) else prefs[LAST_DAY] = day
            prefs[STREAK] = record.streak
        }
    }

    private fun Preferences.toRecord() = DailyRecord(this[LAST_DAY], this[STREAK] ?: 0)

    private companion object {
        val LAST_DAY = longPreferencesKey("daily_last_ranked_day")
        val STREAK = intPreferencesKey("daily_streak")
    }
}

/**
 * The badges and what adds up toward them, in the shared Preferences DataStore. Names a newer version wrote that this
 * one doesn't know are skipped.
 */
class DataStoreAchievementRepository(private val dataStore: DataStore<Preferences>) : AchievementRepository {
    override val progress: Flow<AchievementProgress> = dataStore.data.map { it.toProgress() }

    override suspend fun update(transform: (AchievementProgress) -> AchievementProgress) {
        dataStore.edit { prefs ->
            val progress = transform(prefs.toProgress())
            prefs[UNLOCKED] = progress.unlocked.map { it.name }.toSet()
            prefs[SIZES] = progress.sizesWon.map { it.name }.toSet()
            prefs[FIRINGS] = progress.firingsWon.map { it.name }.toSet()
        }
    }

    private fun Preferences.toProgress() = AchievementProgress(
        unlocked = names<Achievement>(this[UNLOCKED]),
        sizesWon = names<BoardSize>(this[SIZES]),
        firingsWon = names<FiringMode>(this[FIRINGS]),
    )

    private inline fun <reified T : Enum<T>> names(stored: Set<String>?): Set<T> =
        enumValues<T>().filter { it.name in stored.orEmpty() }.toSet()

    private companion object {
        val UNLOCKED = stringSetPreferencesKey("achievements_unlocked")
        val SIZES = stringSetPreferencesKey("achievements_sizes_won")
        val FIRINGS = stringSetPreferencesKey("achievements_firings_won")
    }
}
