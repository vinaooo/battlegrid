package io.github.vinaooo.battlegrid.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.badges
import io.github.vinaooo.battlegrid.domain.model.firingsWon
import io.github.vinaooo.battlegrid.domain.model.sizesWon
import io.github.vinaooo.battlegrid.domain.repository.DailyRecord
import io.github.vinaooo.vinkit.achievements.DataStoreAchievementRepository
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreProgressRepositoriesTest {
    @TempDir
    lateinit var dir: File

    private val scope = TestScope(StandardTestDispatcher())

    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(dir, "settings.preferences_pb") }
    }

    @Test
    fun `the daily record starts empty and keeps its day and streak`() = scope.runTest {
        DataStoreDailyRepository(store).record.first() shouldBe DailyRecord()
        DataStoreDailyRepository(store).update { DailyRecord(20_000, 3) }
        DataStoreDailyRepository(store).record.first() shouldBe DailyRecord(20_000, 3)
    }

    @Test
    fun `badges BattleGrid saved before vinkit kept them still load, unknown ones skipped`() = scope.runTest {
        store.edit {
            it[stringSetPreferencesKey("achievements_unlocked")] = setOf("FIRST_WIN", "MOON_LANDING")
            it[stringSetPreferencesKey("achievements_sizes_won")] = setOf("EIGHT")
            it[stringSetPreferencesKey("achievements_firings_won")] = setOf("SALVO")
        }
        val progress = DataStoreAchievementRepository(store).progress.first()
        progress.badges shouldBe setOf(Achievement.FIRST_WIN)
        progress.sizesWon shouldBe setOf(BoardSize.EIGHT)
        progress.firingsWon shouldBe setOf(FiringMode.SALVO)
    }

    @Test
    fun `progress shares the file with the game settings without touching them`() = scope.runTest {
        DataStoreGameSettingsRepository(store).update { it.copy(howToPlaySeen = true) }
        DataStoreDailyRepository(store).update { DailyRecord(1, 1) }
        DataStoreGameSettingsRepository(store).settings.first().howToPlaySeen shouldBe true
    }
}
