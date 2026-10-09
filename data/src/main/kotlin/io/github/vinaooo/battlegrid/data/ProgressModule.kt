package io.github.vinaooo.battlegrid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.battlegrid.domain.repository.AchievementRepository
import io.github.vinaooo.battlegrid.domain.repository.DailyRepository
import javax.inject.Singleton

/** The daily challenge's record and the badges, in the settings' DataStore. */
@Module
@InstallIn(SingletonComponent::class)
object ProgressModule {
    @Provides
    @Singleton
    fun dailyRecords(dataStore: DataStore<Preferences>): DailyRepository = DataStoreDailyRepository(dataStore)

    @Provides
    @Singleton
    fun achievements(dataStore: DataStore<Preferences>): AchievementRepository =
        DataStoreAchievementRepository(dataStore)
}
