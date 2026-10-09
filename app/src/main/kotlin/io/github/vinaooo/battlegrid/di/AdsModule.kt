package io.github.vinaooo.battlegrid.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.battlegrid.BuildConfig
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.AdConsent
import io.github.vinaooo.vinkit.ads.AdMobBanner
import io.github.vinaooo.vinkit.ads.AdsConfig
import io.github.vinaooo.vinkit.ads.DefaultAdConsent
import io.github.vinaooo.vinkit.ads.MobileAdsSdk
import io.github.vinaooo.vinkit.ads.UmpConsentClient
import javax.inject.Singleton

/**
 * AdMob through vinkit: consent first (UMP), then the SDK and the banner. The IDs come from the build (`AdIds` in
 * vinkit's build logic): Google's test IDs in debug builds and until real ones are in local.properties or CI.
 * App tests replace this module (`FakeAdsModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AdsModule {
    @Provides
    fun config() = AdsConfig(
        bannerUnitId = BuildConfig.AD_BANNER_ID,
        testDeviceIds = BuildConfig.AD_TEST_DEVICE_IDS.split(',').filter(String::isNotEmpty),
        // Debug builds act as in the EEA, so the consent form shows (only on test devices: emulators, listed phones).
        simulateEea = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun consent(@ApplicationContext context: Context, config: AdsConfig): AdConsent =
        DefaultAdConsent(UmpConsentClient(context), MobileAdsSdk(), config)

    @Provides
    fun banner(config: AdsConfig, consent: AdConsent): AdBannerProvider = AdMobBanner(config, consent)
}
