package io.github.vinaooo.battlegrid.feature.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.vinkit.designsystem.R as DesignR

/** The rules in short: a heading with an icon and a few lines per topic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.how_to_play)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(DesignR.string.vinkit_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(SPACE.dp),
            verticalArrangement = Arrangement.spacedBy(SPACE.dp),
        ) {
            items(topics) { (icon, title, text) -> Topic(icon, stringResource(title), stringResource(text)) }
        }
    }
}

@Composable
private fun Topic(icon: ImageVector, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(SPACE.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private val topics = listOf(
    Triple(Icons.Rounded.Anchor, R.string.howto_goal, R.string.howto_goal_text),
    Triple(Icons.Rounded.DirectionsBoat, R.string.howto_placement, R.string.howto_placement_text),
    Triple(Icons.Rounded.TrackChanges, R.string.howto_classic, R.string.howto_classic_text),
    Triple(Icons.Rounded.Repeat, R.string.howto_hit_again, R.string.howto_hit_again_text),
    Triple(Icons.Rounded.Whatshot, R.string.howto_salvo, R.string.howto_salvo_text),
    Triple(Icons.Rounded.Lightbulb, R.string.howto_hints, R.string.howto_hints_text),
    Triple(Icons.Rounded.Today, R.string.howto_daily, R.string.howto_daily_text),
    Triple(Icons.Rounded.Groups, R.string.howto_two_players, R.string.howto_two_players_text),
)

private const val SPACE = 16
