package io.github.vinaooo.battlegrid.feature.game.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.domain.repository.AchievementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The badges earned so far. */
@HiltViewModel
class BadgesViewModel @Inject constructor(achievements: AchievementRepository) : ViewModel() {
    val unlocked: StateFlow<Set<Achievement>> = achievements.progress.map { it.unlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
