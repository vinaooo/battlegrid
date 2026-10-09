package io.github.vinaooo.battlegrid.feature.game.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.repository.GameSettings
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import io.github.vinaooo.battlegrid.domain.usecase.ResumeGame
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A mode change waiting for the player to confirm abandoning the game on the board. */
data class PendingMode(val mode: GameMode, val countsAsLoss: Boolean)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettingsRepository,
    private val gameSettings: GameSettingsRepository,
    private val resumeGame: ResumeGame,
) : ViewModel() {

    val app: StateFlow<AppSettings> = appSettings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppSettings())

    val game: StateFlow<GameSettings> = gameSettings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), GameSettings())

    private val pending = MutableStateFlow<PendingMode?>(null)

    /** A board size or opponent change that would end the game in progress, until confirmed or dismissed. */
    val pendingMode: StateFlow<PendingMode?> = pending.asStateFlow()

    fun onAppChange(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { appSettings.update(transform) }
    }

    fun onModeChange(mode: GameMode) {
        viewModelScope.launch {
            val inProgress = resumeGame()?.takeIf { it.isInProgress }
            if (inProgress != null) {
                pending.value = PendingMode(mode, countsAsLoss = inProgress.state.mode.isVsAi)
            } else {
                gameSettings.update { it.copy(mode = mode) }
            }
        }
    }

    fun confirmMode() {
        val change = pending.value ?: return
        pending.value = null
        viewModelScope.launch { gameSettings.update { it.copy(mode = change.mode) } }
    }

    fun dismissMode() {
        pending.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
