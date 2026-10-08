package io.github.vinaooo.battlegrid

import android.app.Activity
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.vinaooo.battlegrid.di.AdsModule
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.AdConsent
import io.github.vinaooo.vinkit.ads.AdConsentState
import io.github.vinaooo.vinkit.ads.PlaceholderAdBanner
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps the real ads and consent SDKs out of app tests. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AdsModule::class])
object FakeAdsModule {
    @Provides
    fun banner(): AdBannerProvider = PlaceholderAdBanner()

    @Provides
    @Singleton
    fun consent(): FakeAdConsent = FakeAdConsent()

    @Provides
    fun adConsent(fake: FakeAdConsent): AdConsent = fake
}

class FakeAdConsent : AdConsent {
    override val state = MutableStateFlow(AdConsentState())
    var gathered = 0

    override fun gather(activity: Activity) {
        gathered++
    }

    override fun showPrivacyOptions(activity: Activity) = Unit
}
