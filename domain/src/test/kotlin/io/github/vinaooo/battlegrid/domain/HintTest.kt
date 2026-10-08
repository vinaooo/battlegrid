package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.hint.HintEngine
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.MoveOutcome
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class HintTest {
    private val engine = GameEngine()
    private val battle = engine.battle()

    @Test
    fun `a hint is five untried enemy cells, exactly one of them on an afloat ship`() = runTest {
        checkAll(ITERATIONS, Arb.long()) { seed ->
            val cells = HintEngine.cells(battle, Random(seed))!!
            cells.size shouldBe 5
            cells.distinct() shouldBe cells
            cells.all { !battle.enemy.isTried(it) } shouldBe true
            cells.count { battle.enemy.shipAt(it) != null } shouldBe 1
        }
    }

    @Test
    fun `the hint's random comes from the seed and the hints used`() {
        HintEngine.random(9, battle).nextLong() shouldBe Random(9 * 37L).nextLong()
        HintEngine.random(9, battle.copy(hintsUsed = 2)).nextLong() shouldBe Random(9 * 37L + 2).nextLong()
    }

    @Test
    fun `the ship cell is never on a sunk ship or an already hit cell`() {
        // Every ship cell hit but the carrier's (0, 4); the destroyer sunk.
        val cells = rowFleet(BoardSize.TEN).flatMap { it.cells } - c(0, 4)
        val nearlyDone = battle.copy(enemy = battle.enemy.copy(shots = cells))
        repeat(20) { seed ->
            HintEngine.cells(nearlyDone, Random(seed))!!.single { nearlyDone.enemy.shipAt(it) != null } shouldBe c(0, 4)
        }
    }

    @Test
    fun `when water runs short the hint shows what is left`() {
        val water = BoardSize.TEN.cells.filter { battle.enemy.shipAt(it) == null }
        val shortOfWater = battle.copy(enemy = battle.enemy.copy(shots = water.drop(2)))
        val cells = HintEngine.cells(shortOfWater, Random(1))!!
        cells.size shouldBe 3
        cells.count { shortOfWater.enemy.shipAt(it) != null } shouldBe 1
    }

    @Test
    fun `a hint is shown, counted, and cleared by the next shot`() {
        val cells = HintEngine.cells(battle, Random(1))!!
        val hinted = engine.play(battle, Move.ShowHint(cells))
        hinted.hint shouldBe cells
        hinted.hintsUsed shouldBe 1
        val shot = engine.play(hinted, Move.Fire(cells.first()))
        shot.hint shouldBe emptyList()
        shot.hintsUsed shouldBe 1
    }

    @Test
    fun `at most three hints a game, on the player's turn, against the AI, in battle`() {
        val three = (1..3).fold(battle) { state, seed ->
            engine.play(state, Move.ShowHint(HintEngine.cells(state, Random(seed))!!))
        }
        three.hintsUsed shouldBe 3
        engine.apply(three, Move.ShowHint(HintEngine.cells(three, Random(4))!!)) shouldBe MoveOutcome.Rejected

        val aiTurn = engine.battle(first = Side.ENEMY)
        engine.apply(aiTurn, Move.ShowHint(hintFor(aiTurn))) shouldBe MoveOutcome.Rejected
        val twoPlayer = engine.battle(mode(opponent = Opponent.TWO_PLAYER))
        engine.apply(twoPlayer, Move.ShowHint(hintFor(twoPlayer))) shouldBe MoveOutcome.Rejected
        val placing = engine.newGame(mode(), Side.PLAYER, rowFleet(BoardSize.TEN))
        engine.apply(placing, Move.ShowHint(hintFor(battle))) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `a made-up hint is refused`() {
        val good = HintEngine.cells(battle, Random(1))!!
        val ship = good.single { battle.enemy.shipAt(it) != null }
        val water = good - ship
        val twoShips = water.drop(1) + ship + rowFleet(BoardSize.TEN)[4].cells.first { it != ship }
        listOf(
            water + water.first(), // no ship cell, a repeat
            twoShips,
            good.drop(1), // too few
            water.take(3) + ship + c(10, 10), // off the grid
        ).forEach { engine.apply(battle, Move.ShowHint(it)) shouldBe MoveOutcome.Rejected }
        val tried = engine.play(engine.play(battle, Move.Fire(water.first())), Move.Fire(c(9, 9)))
        engine.apply(tried, Move.ShowHint(good)) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `hints work in a salvo too, and the salvo clears them`() {
        val salvo = engine.battle(mode(firing = FiringMode.SALVO))
        val hinted = engine.play(salvo, Move.ShowHint(HintEngine.cells(salvo, Random(1))!!))
        val marks = (0 until 5).map { Move.MarkSalvo(Coord(1, it)) }.toTypedArray()
        engine.play(hinted, *marks, Move.FireSalvo).hint shouldBe emptyList()
    }

    private fun hintFor(state: io.github.vinaooo.battlegrid.domain.model.GameState): List<Coord> {
        val asPlayer = state.copy(toMove = Side.PLAYER)
        return HintEngine.cells(asPlayer, Random(1))!!
    }

    private companion object {
        const val ITERATIONS = 200
    }
}
