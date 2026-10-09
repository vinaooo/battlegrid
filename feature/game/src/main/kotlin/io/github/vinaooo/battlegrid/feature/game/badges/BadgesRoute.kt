package io.github.vinaooo.battlegrid.feature.game.badges

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.feature.game.ui.badgeName
import io.github.vinaooo.battlegrid.feature.game.ui.badgeNote
import io.github.vinaooo.vinkit.achievements.Badge
import io.github.vinaooo.vinkit.achievements.BadgesScreen

@Composable
fun BadgesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BadgesViewModel = hiltViewModel()) {
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    BadgesScreen(battleGridBadges(), unlocked, onBack, modifier)
}

/** Every badge, in vinkit's terms: stored by its name. */
@Composable
internal fun battleGridBadges(): List<Badge> = Achievement.entries.map { Badge(it.name, badgeName(it), badgeNote(it)) }
