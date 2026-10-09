package io.github.vinaooo.battlegrid.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.battlegrid.R
import io.github.vinaooo.battlegrid.feature.game.AiDispatcher
import io.github.vinaooo.battlegrid.feature.game.GameViewModel
import io.github.vinaooo.vinkit.shell.AndroidGameFeedback
import io.github.vinaooo.vinkit.shell.GameFeedback
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object GameModule {
    /** One per app: it loads the sounds once. */
    @Provides
    @Singleton
    fun feedback(@ApplicationContext context: Context): GameFeedback = AndroidGameFeedback(
        context,
        sounds = mapOf(
            GameViewModel.SOUND_MISS to R.raw.sfx_miss,
            GameViewModel.SOUND_HIT to R.raw.sfx_hit,
            GameViewModel.SOUND_SUNK to R.raw.sfx_sunk,
        ),
    )

    @Provides
    @AiDispatcher
    fun aiDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
