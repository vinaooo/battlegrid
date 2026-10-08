package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.ai.aiFor
import io.github.vinaooo.battlegrid.domain.ai.aiRandom
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.GameState
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** Random fleets and random legal moves, in every mode, keep the game valid. */
class GameInvariantsPropertyTest {
    private val engine = GameEngine()

    /** A whole game: random fleets, then random legal battle moves until someone wins. */
    private fun randomGame(mode: GameMode, first: Side, seed: Long): List<GameState> {
        val random = Random(seed)
        val states = mutableListOf(engine.newGame(mode, first, randomFleet(mode.size, random).takeIf { mode.isVsAi }))
        while (!states.last().isOver) {
            val state = states.last()
            val move = when (val phase = state.phase) {
                is Phase.Placement -> if (state.gridOf(phase.side).isFleetComplete) {
                    Move.ConfirmFleet
                } else {
                    Move.SetFleet(randomFleet(mode.size, random))
                }
                // Fire a salvo as soon as it's full, so the game moves on.
                else -> engine.legalMoves(state).let { moves ->
                    moves.firstOrNull { it == Move.FireSalvo } ?: moves.filter { it != Move.FireSalvo }.random(random)
                }
            }
            states += engine.play(state, move)
        }
        return states
    }

    @Test
    fun `fleets stay put, shots are never repeated, and a ship is sunk only when all its cells are hit`() = runTest {
        checkAll(ITERATIONS, Arb.element(GameMode.ALL), Arb.enum<Side>(), Arb.long()) { mode, first, seed ->
            val states = randomGame(mode, first, seed)
            val battle = states.first { it.isBattle }
            states.dropWhile { !it.isBattle }.forEach { state ->
                Side.entries.forEach { side ->
                    val grid = state.gridOf(side)
                    grid.ships shouldBe battle.gridOf(side).ships
                    grid.shots.distinct() shouldBe grid.shots
                    grid.ships.indices.forEach { i -> grid.isSunk(i) shouldBe grid.ships[i]!!.cells.all(grid::isTried) }
                }
            }
            val last = states.last()
            last.gridOf(last.winner!!.other).isFleetSunk shouldBe true
            last.gridOf(last.winner!!).isFleetSunk shouldBe false
        }
    }

    @Test
    fun `legal moves agree with isLegal`() = runTest {
        checkAll(SMALL_ITERATIONS, Arb.element(GameMode.ALL), Arb.enum<Side>(), Arb.long()) { mode, first, seed ->
            val states = randomGame(mode, first, seed)
            listOf(states.first(), states[states.size / 2], states.last()).forEach { state ->
                val legal = engine.legalMoves(state).toSet()
                legal.forEach { engine.isLegal(state, it) shouldBe true }
                battleCandidates(state).forEach { engine.isLegal(state, it) shouldBe (it in legal) }
            }
        }
    }

    @Test
    fun `the same seed gives the same game`() = runTest {
        checkAll(SMALL_ITERATIONS, Arb.element(GameMode.ALL), Arb.long()) { mode, seed ->
            randomGame(mode, Side.PLAYER, seed) shouldBe randomGame(mode, Side.PLAYER, seed)
        }
    }

    @Test
    fun `every state of a game round-trips through the save format`() = runTest {
        checkAll(SMALL_ITERATIONS, Arb.element(GameMode.ALL), Arb.long()) { mode, seed ->
            randomGame(mode, Side.ENEMY, seed).forEach {
                GameSession.codec.decode(GameSession.codec.encode(it)) shouldBe it
            }
        }
    }

    @Test
    fun `AIs of every level only ever make legal moves, through whole games in every mode`() = runTest {
        val vsAi = GameMode.ALL.filter { it.isVsAi }
        checkAll(SMALL_ITERATIONS, Arb.element(vsAi), Arb.enum<Opponent>(), Arb.long()) { mode, rival, seed ->
            val players = mapOf(
                Side.ENEMY to aiFor(mode.opponent),
                Side.PLAYER to aiFor(rival.takeIf { it != Opponent.TWO_PLAYER } ?: Opponent.EASY),
            )
            var state = engine.play(
                engine.newGame(mode, Side.PLAYER, RandomFleetPlacer.place(mode.size, Random(seed))),
                Move.SetFleet(RandomFleetPlacer.place(mode.size, Random(seed + 1))),
                Move.ConfirmFleet,
            )
            while (!state.isOver) {
                val shots = players.getValue(state.toMove).shots(state, aiRandom(seed, state))
                val moves = if (mode.firing ==
                    FiringMode.SALVO
                ) {
                    shots.map(Move::MarkSalvo) + Move.FireSalvo
                } else {
                    shots.map(Move::Fire)
                }
                state = engine.play(state, *moves.toTypedArray())
            }
            state.gridOf(state.winner!!.other).isFleetSunk shouldBe true
        }
    }

    /** Every cell fired or marked, off the grid by one too, plus a salvo and both resignations. */
    private fun battleCandidates(state: GameState): List<Move> {
        val side = state.mode.size.side
        val cells = (-1..side).flatMap { row -> (-1..side).map { c(row, it) } }
        return cells.map(Move::Fire) + cells.map(Move::MarkSalvo) + Move.FireSalvo + Move.ConfirmFleet
    }

    private companion object {
        const val ITERATIONS = 200
        const val SMALL_ITERATIONS = 40
    }
}
