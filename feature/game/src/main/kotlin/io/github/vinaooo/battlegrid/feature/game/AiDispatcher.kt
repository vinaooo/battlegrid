package io.github.vinaooo.battlegrid.feature.game

import javax.inject.Qualifier

/** Where the AI and the hint search run, off the main thread: `Dispatchers.Default`, a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiDispatcher
