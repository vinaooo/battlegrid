package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.MoveOutcome
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LegalMovesTest {
    private val engine = GameEngine()

    @Test
    fun `on an empty grid every ship may go wherever it fits, and nothing else`() {
        val placing = engine.newGame(mode(BoardSize.EIGHT), Side.PLAYER, rowFleet(BoardSize.EIGHT))
        placing.isOver shouldBe false
        val moves = engine.legalMoves(placing)
        // A ship of length n fits 8 × (9 - n) ways each way.
        moves.size shouldBe listOf(5, 4, 3, 2).sumOf { 2 * 8 * (9 - it) }
        moves.all { it is Move.PlaceShip } shouldBe true
    }

    @Test
    fun `a full fleet can be turned where there is room, and confirmed`() {
        val placed = engine.play(
            engine.newGame(mode(BoardSize.EIGHT), Side.PLAYER, rowFleet(BoardSize.EIGHT)),
            Move.SetFleet(rowFleet(BoardSize.EIGHT)),
        )
        val moves = engine.legalMoves(placed)
        // Turned down, every ship but the destroyer on the last fleet row would cross the ship below.
        moves.filterIsInstance<Move.RotateShip>() shouldBe listOf(Move.RotateShip(3))
        moves.contains(Move.ConfirmFleet) shouldBe true
    }

    @Test
    fun `a ship can't be placed off the grid or in another class's slot`() {
        val placing = engine.newGame(mode(BoardSize.EIGHT), Side.PLAYER, rowFleet(BoardSize.EIGHT))
        val offGrid = Ship(ShipClass.CARRIER, c(0, 4), Orientation.HORIZONTAL)
        engine.apply(placing, Move.PlaceShip(0, offGrid)) shouldBe MoveOutcome.Rejected
        engine.apply(placing, Move.PlaceShip(1, offGrid.copy(origin = c(0, 0)))) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `classic battles offer a shot at every untried cell`() {
        val battle = engine.play(engine.battle(mode(BoardSize.EIGHT)), Move.Fire(c(0, 0)))
        // The enemy's turn: it may fire anywhere on the player's untouched grid.
        engine.legalMoves(battle) shouldContainExactlyInAnyOrder BoardSize.EIGHT.cells.map(Move::Fire)
    }

    @Test
    fun `salvo battles offer marks, then the salvo once it's full`() {
        val battle = engine.battle(mode(BoardSize.EIGHT, FiringMode.SALVO))
        engine.legalMoves(battle) shouldContainExactlyInAnyOrder BoardSize.EIGHT.cells.map(Move::MarkSalvo)
        val full = engine.play(battle, *(0 until 4).map { Move.MarkSalvo(c(1, it)) }.toTypedArray())
        engine.legalMoves(full) shouldContainExactlyInAnyOrder
            (0 until 4).map { Move.MarkSalvo(c(1, it)) } + Move.FireSalvo
    }
}
