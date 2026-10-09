package io.github.vinaooo.battlegrid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
