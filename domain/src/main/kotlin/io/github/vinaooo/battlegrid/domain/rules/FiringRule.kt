package io.github.vinaooo.battlegrid.domain.rules

import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.ShotResult

/** How many shots a turn has, and whether the shooter fires again after them. */
interface FiringRule {
    fun shotsPerTurn(state: GameState): Int

    fun keepsTurn(results: List<ShotResult>): Boolean
}

object ClassicFiring : FiringRule {
    override fun shotsPerTurn(state: GameState): Int = 1

    override fun keepsTurn(results: List<ShotResult>): Boolean = false
}

/** Classic, but a hit (or a sinking) fires again. */
object HitAgainFiring : FiringRule {
    override fun shotsPerTurn(state: GameState): Int = 1

    override fun keepsTurn(results: List<ShotResult>): Boolean = results.last() != ShotResult.MISS
}

/** One shot per own ship afloat, never more than the cells still untried. */
object SalvoFiring : FiringRule {
    override fun shotsPerTurn(state: GameState): Int =
        minOf(state.gridOf(state.toMove).shipsAfloat, state.target.untried.size)

    override fun keepsTurn(results: List<ShotResult>): Boolean = false
}

fun firingFor(mode: FiringMode): FiringRule = when (mode) {
    FiringMode.CLASSIC -> ClassicFiring
    FiringMode.HIT_AGAIN -> HitAgainFiring
    FiringMode.SALVO -> SalvoFiring
}
