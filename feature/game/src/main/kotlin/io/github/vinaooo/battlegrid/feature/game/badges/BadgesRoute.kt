package io.github.vinaooo.battlegrid.feature.game.badges

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.battlegrid.domain.model.Achievement
import io.github.vinaooo.battlegrid.feature.game.R
import io.github.vinaooo.vinkit.designsystem.R as DesignR

@Composable
fun BadgesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BadgesViewModel = hiltViewModel()) {
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    BadgesScreen(unlocked, onBack, modifier)
}

/** Every badge, earned or locked, on a screen of its own. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(unlocked: Set<Achievement>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.badges)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(DesignR.string.vinkit_back))
                    }
                },
            )
        },
    ) { padding ->
        BadgesList(unlocked, Modifier.fillMaxSize().padding(padding), PaddingValues(BADGES_PADDING))
    }
}

private val BADGES_PADDING = 16.dp
