package io.github.vinaooo.battlegrid.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.repository.GameSettings
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * BattleGrid's settings, in the same Preferences DataStore as vinkit's (its own keys only). A value this version
 * doesn't know reads as the default.
 */
class DataStoreGameSettingsRepository(private val dataStore: DataStore<Preferences>) : GameSettingsRepository {
    override val settings: Flow<GameSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        dataStore.edit { prefs ->
            val settings = transform(prefs.toSettings())
            prefs[BOARD_SIZE] = settings.mode.size.name
            prefs[FIRING_MODE] = settings.mode.firing.name
            prefs[OPPONENT] = settings.mode.opponent.name
            val first = settings.nextFirstMover
            if (first == null) prefs.remove(NEXT_FIRST_MOVER) else prefs[NEXT_FIRST_MOVER] = first.name
        }
    }

    private fun Preferences.toSettings(): GameSettings {
        val defaults = GameSettings().mode
        return GameSettings(
            mode = GameMode(
                size = enumOrDefault(this[BOARD_SIZE], defaults.size),
                firing = enumOrDefault(this[FIRING_MODE], defaults.firing),
                opponent = enumOrDefault(this[OPPONENT], defaults.opponent),
            ),
            nextFirstMover = Side.entries.firstOrNull { it.name == this[NEXT_FIRST_MOVER] },
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val BOARD_SIZE = stringPreferencesKey("board_size")
        val FIRING_MODE = stringPreferencesKey("firing_mode")
        val OPPONENT = stringPreferencesKey("opponent")
        val NEXT_FIRST_MOVER = stringPreferencesKey("next_first_mover")
    }
}
