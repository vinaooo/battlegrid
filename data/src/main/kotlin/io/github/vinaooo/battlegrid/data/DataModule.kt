package io.github.vinaooo.battlegrid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.battlegrid.domain.repository.Clock
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import io.github.vinaooo.battlegrid.domain.repository.SavedGameRepository
import io.github.vinaooo.battlegrid.domain.repository.SeedSource
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.scores.data.RoomScoreRepository
import io.github.vinaooo.vinkit.scores.data.RoomStatsRepository
import io.github.vinaooo.vinkit.scores.data.ScoresDatabase
import io.github.vinaooo.vinkit.settings.DataStoreAppSettingsRepository
import java.io.File
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers

/** Teal is BattleGrid's brand: the colors when dynamic color is off, until the player picks another. */
val BRAND_COLOR = ThemeColor.TEAL

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    fun appSettings(dataStore: DataStore<Preferences>): AppSettingsRepository =
        DataStoreAppSettingsRepository(dataStore, AppSettings(themeColor = BRAND_COLOR))

    @Provides
    @Singleton
    fun gameSettings(dataStore: DataStore<Preferences>): GameSettingsRepository =
        DataStoreGameSettingsRepository(dataStore)

    @Provides
    @Singleton
    fun scoresDatabase(@ApplicationContext context: Context): ScoresDatabase = ScoresDatabase.create(context)

    @Provides
    @Singleton
    fun stats(db: ScoresDatabase): StatsRepository = RoomStatsRepository(db)

    @Provides
    @Singleton
    fun scores(db: ScoresDatabase): ScoreRepository = RoomScoreRepository(db)

    @Provides
    @Singleton
    fun savedGames(@ApplicationContext context: Context): SavedGameRepository =
        FileSavedGameRepository(File(context.filesDir, "saved_game.json"), Dispatchers.IO)

    @Provides
    fun seeds(): SeedSource = SeedSource { Random.nextLong() }

    @Provides
    fun clock(): Clock = Clock { System.currentTimeMillis() }
}
