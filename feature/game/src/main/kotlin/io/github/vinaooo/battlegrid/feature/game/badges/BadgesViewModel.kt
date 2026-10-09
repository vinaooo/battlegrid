package io.github.vinaooo.battlegrid.feature.game.badges

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.vinkit.achievements.BadgesViewModel as VinkitBadgesViewModel
import io.github.vinaooo.vinkit.core.AchievementRepository
import javax.inject.Inject

/** vinkit's badges, injected. */
@HiltViewModel
class BadgesViewModel @Inject constructor(achievements: AchievementRepository) : VinkitBadgesViewModel(achievements)
