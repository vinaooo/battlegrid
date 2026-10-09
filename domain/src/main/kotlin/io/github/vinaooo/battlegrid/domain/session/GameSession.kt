package io.github.vinaooo.battlegrid.domain.session

import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.MoveOutcome
import io.github.vinaooo.vinkit.core.GameCodec
import kotlinx.serialization.Serializable

/**
 * A game being played: its seed (the AI's fleet and random choices) and the current state. This is what gets saved.
 * There is no undo: taking back a shot would show where the ships aren't. A game not [recorded] (a replay of the day's
 * daily challenge) leaves no stats, scores or badges.
 */
@Serializable
data class GameSession(val seed: Long, val state: GameState, val recorded: Boolean = true) {
    /** The battle started and isn't over: abandoning it counts as a loss against the AI. Placement doesn't count. */
    val isInProgress: Boolean get() = state.isBattle

    fun play(move: Move, engine: GameEngine): GameSession? = when (val outcome = engine.apply(state, move)) {
        is MoveOutcome.Applied -> copy(state = outcome.state)
        MoveOutcome.Rejected -> null
    }

    /** One second more on the clock, which runs during the battle only. */
    fun tick(): GameSession =
        if (state.isBattle) copy(state = state.copy(elapsedSeconds = state.elapsedSeconds + 1)) else this

    companion object {
        /** A game's state as short text for bug reports ("State:"), and back for the debug build. */
        val codec = GameCodec(GameState.serializer())
    }
}
