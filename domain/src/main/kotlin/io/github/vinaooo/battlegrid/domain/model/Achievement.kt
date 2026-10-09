package io.github.vinaooo.battlegrid.domain.model

import io.github.vinaooo.vinkit.core.AchievementProgress

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

/** The badges earned that this version knows (vinkit keeps them by name). */
val AchievementProgress.badges: Set<Achievement>
    get() = known(unlocked)

/** The board sizes won at least once, toward [Achievement.WIN_EVERY_SIZE]. */
val AchievementProgress.sizesWon: Set<BoardSize>
    get() = known(collected[Achievements.SIZES_WON])

/** The firing modes won at least once, toward [Achievement.WIN_EVERY_FIRING]. */
val AchievementProgress.firingsWon: Set<FiringMode>
    get() = known(collected[Achievements.FIRINGS_WON])

/** The entries of [T] named in [names]; names a newer version wrote are skipped. */
private inline fun <reified T : Enum<T>> known(names: Set<String>?): Set<T> =
    enumValues<T>().filter { it.name in names.orEmpty() }.toSet()

/** The progress with [badges], [sizes] and [firings] added; what this version doesn't know is kept. */
fun AchievementProgress.plus(badges: Set<Achievement>, sizes: Set<BoardSize>, firings: Set<FiringMode>) =
    AchievementProgress(
        unlocked + badges.map { it.name },
        collected + mapOf(
            Achievements.SIZES_WON to collected[Achievements.SIZES_WON].orEmpty() + sizes.map { it.name },
            Achievements.FIRINGS_WON to collected[Achievements.FIRINGS_WON].orEmpty() + firings.map { it.name },
        ),
    )

/** When each badge is earned, after a finished game against the AI. */
object Achievements {
    const val WIN_STREAK = 5
    const val DAILY_STREAK = 7
    const val ACCURACY = 0.6

    /** Keys of the collected sets in storage: never rename. */
    const val SIZES_WON = "sizes_won"
    const val FIRINGS_WON = "firings_won"

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
        return progress.plus(earned, sizes, firings)
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
