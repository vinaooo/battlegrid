package io.github.vinaooo.battlegrid.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.battlegrid.domain.repository.Clock
import io.github.vinaooo.battlegrid.domain.repository.DailyRepository
import io.github.vinaooo.battlegrid.domain.repository.GameSettingsRepository
import io.github.vinaooo.battlegrid.domain.repository.SavedGameRepository
import io.github.vinaooo.battlegrid.domain.repository.SeedSource
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.usecase.FinishGame
import io.github.vinaooo.battlegrid.domain.usecase.RecordProgress
import io.github.vinaooo.battlegrid.domain.usecase.RestartGame
import io.github.vinaooo.battlegrid.domain.usecase.ResumeGame
import io.github.vinaooo.battlegrid.domain.usecase.SaveGame
import io.github.vinaooo.battlegrid.domain.usecase.StartDailyGame
import io.github.vinaooo.battlegrid.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository

/** The domain's classes, which carry no DI annotations, assembled for Hilt. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun engine() = GameEngine()

    @Provides
    fun progress(daily: DailyRepository, achievements: AchievementRepository, stats: StatsRepository) =
        RecordProgress(daily, achievements, stats)

    @Provides
    fun startNewGame(
        savedGames: SavedGameRepository,
        gameSettings: GameSettingsRepository,
        progress: RecordProgress,
        seeds: SeedSource,
        engine: GameEngine,
    ) = StartNewGame(savedGames, gameSettings, progress, seeds, engine)

    @Provides
    fun startDailyGame(
        savedGames: SavedGameRepository,
        gameSettings: GameSettingsRepository,
        progress: RecordProgress,
        clock: Clock,
        engine: GameEngine,
    ) = StartDailyGame(savedGames, gameSettings, progress, clock, engine)

    @Provides
    fun restartGame(
        savedGames: SavedGameRepository,
        gameSettings: GameSettingsRepository,
        progress: RecordProgress,
        engine: GameEngine,
    ) = RestartGame(savedGames, gameSettings, progress, engine)

    @Provides
    fun resumeGame(savedGames: SavedGameRepository) = ResumeGame(savedGames)

    @Provides
    fun saveGame(savedGames: SavedGameRepository) = SaveGame(savedGames)

    @Provides
    fun finishGame(
        savedGames: SavedGameRepository,
        gameSettings: GameSettingsRepository,
        scores: ScoreRepository,
        clock: Clock,
        progress: RecordProgress,
    ) = FinishGame(savedGames, gameSettings, scores, clock, progress)
}
