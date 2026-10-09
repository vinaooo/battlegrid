package io.github.vinaooo.battlegrid.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.model.AchievementProgress
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.repository.DailyRecord
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
    fun `badges and their progress are kept`() = scope.runTest {
        val progress = AchievementProgress(
            setOf(Achievement.FIRST_WIN, Achievement.BEAT_HARD),
            setOf(BoardSize.EIGHT),
            setOf(FiringMode.SALVO),
        )
        DataStoreAchievementRepository(store).progress.first() shouldBe AchievementProgress()
        DataStoreAchievementRepository(store).update { progress }
        DataStoreAchievementRepository(store).progress.first() shouldBe progress
    }

    @Test
    fun `a badge from a newer version is skipped, the rest kept`() = scope.runTest {
        store.edit { it[stringSetPreferencesKey("achievements_unlocked")] = setOf("FIRST_WIN", "MOON_LANDING") }
        DataStoreAchievementRepository(store).progress.first().unlocked shouldBe setOf(Achievement.FIRST_WIN)
    }

    @Test
    fun `progress shares the file with the game settings without touching them`() = scope.runTest {
        DataStoreGameSettingsRepository(store).update { it.copy(howToPlaySeen = true) }
        DataStoreDailyRepository(store).update { DailyRecord(1, 1) }
        DataStoreGameSettingsRepository(store).settings.first().howToPlaySeen shouldBe true
    }
}
