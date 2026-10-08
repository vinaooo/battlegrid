package io.github.vinaooo.battlegrid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.battlegrid.data.BRAND_COLOR
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.AdConsent
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.github.vinaooo.vinkit.designsystem.isDarkTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appSettings: AppSettingsRepository

    @Inject lateinit var adBanner: AdBannerProvider

    @Inject lateinit var adConsent: AdConsent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Once per launch, not again when the activity is recreated (rotation, theme change).
        if (savedInstanceState == null) adConsent.gather(this)
        setContent {
            val settings by appSettings.settings.collectAsStateWithLifecycle(AppSettings(themeColor = BRAND_COLOR))
            val darkTheme = isDarkTheme(settings.themeMode, isSystemInDarkTheme())
            LaunchedEffect(darkTheme) {
                // System bar icons follow the in-app theme choice, not only the system's.
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            VinkitTheme(settings.themeColor, settings.themeMode, settings.dynamicColor) {
                val consent by adConsent.state.collectAsStateWithLifecycle()
                BattleGridApp(
                    adBanner = adBanner,
                    privacyOptionsRequired = consent.privacyOptionsRequired,
                    onOpenPrivacyOptions = { adConsent.showPrivacyOptions(this) },
                )
            }
        }
    }
}
