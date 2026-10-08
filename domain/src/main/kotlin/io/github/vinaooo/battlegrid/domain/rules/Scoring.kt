package io.github.vinaooo.battlegrid.domain.rules

import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.vinkit.core.Ranking

/** A won game's points and how its mode's ranking sorts them. */
interface ScoringStrategy {
    val ranking: Ranking

    fun points(state: GameState): Int
}

/** The player's shots to win, plus a penalty per hint: fewer is better. */
object ShotsScoring : ScoringStrategy {
    const val HINT_PENALTY = 5

    override val ranking: Ranking = Ranking.LOWEST_POINTS

    override fun points(state: GameState): Int = state.playerShots + HINT_PENALTY * state.hintsUsed
}
