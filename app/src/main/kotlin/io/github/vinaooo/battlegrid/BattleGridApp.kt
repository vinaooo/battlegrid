package io.github.vinaooo.battlegrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.vinaooo.battlegrid.feature.game.scores.ScoresRoute
import io.github.vinaooo.battlegrid.feature.game.settings.SettingsRoute
import io.github.vinaooo.battlegrid.feature.game.ui.GameRoute
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import kotlinx.serialization.Serializable

@Serializable
data object GameDestination

@Serializable
data object ScoresDestination

@Serializable
data object SettingsDestination

/** The navigation host. Only the game screen carries the ad banner, at its bottom; Scores and Settings have none. */
@Composable
fun BattleGridApp(
    adBanner: AdBannerProvider,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = GameDestination,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        composable<GameDestination> {
            Column {
                // The banner pads for the navigation bar, so the game above it must not pad for it again.
                val navigationBar = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                Box(Modifier.weight(1f).consumeWindowInsets(navigationBar)) {
                    GameRoute(
                        onOpenScores = { navController.navigate(ScoresDestination) },
                        onOpenSettings = { navController.navigate(SettingsDestination) },
                    )
                }
                adBanner.Banner(Modifier.navigationBarsPadding())
            }
        }
        composable<ScoresDestination> { ScoresRoute(onBack = navController::popBackStack) }
        composable<SettingsDestination> {
            SettingsRoute(
                onBack = navController::popBackStack,
                privacyOptionsRequired = privacyOptionsRequired,
                onOpenPrivacyOptions = onOpenPrivacyOptions,
            )
        }
    }
}
