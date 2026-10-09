package io.github.vinaooo.battlegrid.domain.model

/** A local badge. Its name is its key in storage: never rename one after release. */
enum class Achievement {
    FIRST_WIN,
    WIN_EVERY_SIZE,
    WIN_EVERY_FIRING,
    BEAT_HARD,
    NO_HINT_WIN,
    CLEAN_SINK,
    WIN_STREAK_5,
    DAILY_STREAK_7,
    ACCURACY_60,
}

/** The badges earned, and what the ones that add up over games have gathered so far. */
data class AchievementProgress(
    val unlocked: Set<Achievement> = emptySet(),
    val sizesWon: Set<BoardSize> = emptySet(),
    val firingsWon: Set<FiringMode> = emptySet(),
)

/** When each badge is earned, after a finished game against the AI. */
object Achievements {
    const val WIN_STREAK = 5
    const val DAILY_STREAK = 7
    const val ACCURACY = 0.6

    /**
     * [progress] after [state], a finished game against the AI, with the player's win [streak] in its mode and, for
     * a ranked daily, the [dailyStreak] of days played in a row.
     */
    fun after(progress: AchievementProgress, state: GameState, streak: Int, dailyStreak: Int?): AchievementProgress {
        val won = state.winner == Side.PLAYER
        val sizes = if (won) progress.sizesWon + state.mode.size else progress.sizesWon
        val firings = if (won) progress.firingsWon + state.mode.firing else progress.firingsWon
        val earned = buildSet {
            if (cleanSink(state)) add(Achievement.CLEAN_SINK)
            if ((dailyStreak ?: 0) >= DAILY_STREAK) add(Achievement.DAILY_STREAK_7)
            if (won) addAll(winBadges(state, streak, sizes, firings))
        }
        return AchievementProgress(progress.unlocked + earned, sizes, firings)
    }

    private fun winBadges(state: GameState, streak: Int, sizes: Set<BoardSize>, firings: Set<FiringMode>) = buildSet {
        add(Achievement.FIRST_WIN)
        if (state.hintsUsed == 0) add(Achievement.NO_HINT_WIN)
        if (state.mode.opponent == Opponent.HARD) add(Achievement.BEAT_HARD)
        if (streak >= WIN_STREAK) add(Achievement.WIN_STREAK_5)
        if (state.enemy.hits >= ACCURACY * state.enemy.shots.size) add(Achievement.ACCURACY_60)
        if (sizes.containsAll(BoardSize.entries)) add(Achievement.WIN_EVERY_SIZE)
        if (firings.containsAll(FiringMode.entries)) add(Achievement.WIN_EVERY_FIRING)
    }

    /** The player sank a ship with no miss among their shots from its first hit to its last. */
    fun cleanSink(state: GameState): Boolean {
        val grid = state.enemy
        return grid.ships.indices.filter(grid::isSunk).any { ship ->
            val hits = grid.shots.indices.filter { grid.shipAt(grid.shots[it]) == ship }
            (hits.first()..hits.last()).none { grid.shipAt(grid.shots[it]) == null }
        }
    }
}
