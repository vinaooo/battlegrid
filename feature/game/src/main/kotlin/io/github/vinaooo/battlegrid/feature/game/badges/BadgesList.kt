package io.github.vinaooo.battlegrid.feature.game.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.battlegrid.feature.game.ui.badgeName
import io.github.vinaooo.battlegrid.feature.game.ui.badgeNote

/** Every badge: earned ones in the theme's primary color, locked ones dimmed with a lock; each read as one item. */
@Composable
internal fun BadgesList(
    unlocked: Set<Achievement>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(Achievement.entries) { badge ->
            val earned = badge in unlocked
            val name = badgeName(badge)
            val note = badgeNote(badge)
            val spoken = stringResource(if (earned) R.string.badge_unlocked else R.string.badge_locked, name, note)
            val color = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                Modifier.clearAndSetSemantics { contentDescription = spoken },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (earned) Icons.Rounded.MilitaryTech else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = color,
                )
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = color)
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
