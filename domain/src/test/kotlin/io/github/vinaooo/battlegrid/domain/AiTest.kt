package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.ai.EasyAi
import io.github.vinaooo.battlegrid.domain.ai.HardAi
import io.github.vinaooo.battlegrid.domain.ai.MediumAi
import io.github.vinaooo.battlegrid.domain.ai.TargetView
import io.github.vinaooo.battlegrid.domain.ai.aiFor
import io.github.vinaooo.battlegrid.domain.ai.aiRandom
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.random.Random
import org.junit.jupiter.api.Test

/** The AI fires at the player's [rowFleet]: on 10×10 the carrier is (0, 0)–(0, 4), row 1 is water. */
class AiTest {
    private val engine = GameEngine()

    /** The AI's turn, with [shots] already fired at the player's grid. */
    private fun aiTurn(vararg shots: Coord, firing: FiringMode = FiringMode.CLASSIC, size: BoardSize = BoardSize.TEN) =
        engine.battle(mode(size, firing), first = Side.ENEMY).let {
            it.copy(player = it.player.copy(shots = shots.toList()))
        }

    @Test
    fun `each level has its AI`() {
        aiFor(Opponent.EASY) shouldBeSameInstanceAs EasyAi
        aiFor(Opponent.MEDIUM) shouldBeSameInstanceAs MediumAi
        aiFor(Opponent.HARD) shouldBeSameInstanceAs HardAi
    }

    @Test
    fun `the AI's random choices come from the seed and the moves so far`() {
        val state = aiTurn()
        aiRandom(5, state).nextLong() shouldBe Random(5 * 31L).nextLong()
        aiRandom(5, state.copy(actions = 3)).nextLong() shouldBe Random(5 * 31L + 3).nextLong()
    }

    @Test
    fun `the target view shows only what the shooter knows`() {
        // A miss, an open hit on the carrier, and the destroyer sunk.
        val view = TargetView.of(aiTurn(c(1, 0), c(0, 2), c(8, 0), c(8, 1)).target)
        view.misses shouldBe setOf(c(1, 0))
        view.openHits shouldBe setOf(c(0, 2))
        view.sunk shouldBe setOf(c(8, 0), c(8, 1))
        view.remainingLengths shouldBe listOf(5, 4, 3, 3)
        view.untried.size shouldBe 96
    }

    @Test
    fun `easy fires at a random untried cell`() {
        val state = aiTurn(c(0, 0))
        repeat(50) { seed ->
            val shot = EasyAi.shots(state, Random(seed)).single()
            shot shouldBeIn state.target.untried
        }
    }

    @Test
    fun `medium hunts at random, then fires next to an open hit`() {
        MediumAi.shots(aiTurn(c(1, 1)), Random(1)).single() shouldBeIn aiTurn(c(1, 1)).target.untried
        repeat(20) { seed ->
            MediumAi.shots(aiTurn(c(0, 2)), Random(seed)).single() shouldBeIn listOf(c(0, 1), c(0, 3), c(1, 2))
        }
    }

    @Test
    fun `medium follows a line of hits along it`() {
        repeat(20) { seed ->
            MediumAi.shots(aiTurn(c(0, 2), c(0, 3)), Random(seed)).single() shouldBeIn listOf(c(0, 1), c(0, 4))
        }
    }

    @Test
    fun `medium leaves a sunk ship alone`() {
        val state = aiTurn(c(8, 0), c(8, 1))
        repeat(20) { seed -> MediumAi.shots(state, Random(seed)).single() shouldBeIn state.target.untried }
    }

    @Test
    fun `hard hunts on a checkerboard while no ship is hit`() {
        repeat(20) { seed ->
            val (row, col) = HardAi.shots(aiTurn(), Random(seed)).single()
            (row + col) % 2 shouldBe 0
        }
    }

    @Test
    fun `hard aims where the most ship positions still fit`() {
        // On 8×8, everything but row 0 is a miss: ships fit only along row 0, most over its middle.
        // Columns 3 and 4 fit 13 positions each; the checkerboard keeps column 4.
        val view = TargetView(
            side = 8,
            misses = BoardSize.EIGHT.cells.filter { it.row > 0 }.toSet(),
            openHits = emptySet(),
            sunk = emptySet(),
            remainingLengths = listOf(5, 4, 3, 2),
        )
        HardAi.pick(view, 1, Random(1)) shouldBe listOf(c(0, 4))
    }

    @Test
    fun `hard finishes a ship it has hit`() {
        repeat(20) { seed ->
            HardAi.shots(aiTurn(c(0, 2), c(0, 3)), Random(seed)).single() shouldBeIn listOf(c(0, 1), c(0, 4))
        }
    }

    @Test
    fun `a salvo gets one distinct untried cell per ship afloat, from every level`() {
        val state = aiTurn(c(0, 2), firing = FiringMode.SALVO)
        listOf(EasyAi, MediumAi, HardAi).forEach { ai ->
            val shots = ai.shots(state, Random(3))
            shots.size shouldBe 5
            shots.distinct() shouldBe shots
            state.target.untried shouldContainAll shots
            val marked = engine.play(state, *shots.map(Move::MarkSalvo).toTypedArray())
            engine.isLegal(marked, Move.FireSalvo) shouldBe true
        }
    }

    @Test
    fun `medium and hard spend a salvo around an open hit first`() {
        val state = aiTurn(c(0, 2), firing = FiringMode.SALVO)
        listOf(MediumAi, HardAi).forEach { ai ->
            ai.shots(state, Random(3)).take(2).forEach {
                it shouldBeIn
                    listOf(c(0, 1), c(0, 3), c(1, 2), c(0, 0), c(0, 4))
            }
        }
    }
}
