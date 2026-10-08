package io.github.vinaooo.battlegrid.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.repository.GameSettings
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.settings.DataStoreAppSettingsRepository
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreGameSettingsRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val scope = TestScope(StandardTestDispatcher())

    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(dir, "settings.preferences_pb") }
    }

    @Test
    fun `first launch reads the defaults, with no first mover yet`() = scope.runTest {
        DataStoreGameSettingsRepository(store).settings.first() shouldBe GameSettings()
    }

    @Test
    fun `the mode and the next first mover are persisted, and the first mover can be cleared`() = scope.runTest {
        val changed = GameSettings(GameMode(BoardSize.TWELVE, FiringMode.SALVO, Opponent.TWO_PLAYER), Side.ENEMY)
        DataStoreGameSettingsRepository(store).update { changed }
        DataStoreGameSettingsRepository(store).settings.first() shouldBe changed

        DataStoreGameSettingsRepository(store).update { it.copy(nextFirstMover = null) }
        DataStoreGameSettingsRepository(store).settings.first().nextFirstMover shouldBe null
    }

    @Test
    fun `game and vinkit settings share the file without touching each other`() = scope.runTest {
        val app = DataStoreAppSettingsRepository(store)
        app.update { it.copy(themeMode = ThemeMode.DARK) }
        DataStoreGameSettingsRepository(store).update { it.copy(nextFirstMover = Side.PLAYER) }

        app.settings.first() shouldBe AppSettings(themeMode = ThemeMode.DARK)
        DataStoreGameSettingsRepository(store).settings.first().nextFirstMover shouldBe Side.PLAYER
    }

    @Test
    fun `a value from a newer version reads as the default`() = scope.runTest {
        store.edit {
            it[stringPreferencesKey("board_size")] = "SIXTEEN"
            it[stringPreferencesKey("firing_mode")] = "NUKE"
            it[stringPreferencesKey("next_first_mover")] = "PLAYER_3"
        }

        DataStoreGameSettingsRepository(store).settings.first() shouldBe GameSettings()
    }
}
