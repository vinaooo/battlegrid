package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.ShipClass
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.MoveOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class PlacementTest {
    private val engine = GameEngine()
    private val vsAi = engine.newGame(mode(BoardSize.EIGHT), Side.ENEMY, rowFleet(BoardSize.EIGHT))
    private val carrier = Ship(ShipClass.CARRIER, c(0, 0), Orientation.HORIZONTAL)

    @Test
    fun `a new game against the AI starts with the player placing, the AI's fleet already there`() {
        vsAi.phase shouldBe Phase.Placement(Side.PLAYER)
        vsAi.player.ships shouldBe listOf(null, null, null, null)
        vsAi.enemy.ships shouldBe rowFleet(BoardSize.EIGHT)
        vsAi.firstMover shouldBe Side.ENEMY
    }

    @Test
    fun `ships are placed, moved and turned`() {
        val placed = engine.play(vsAi, Move.PlaceShip(0, carrier))
        placed.player.ships[0] shouldBe carrier
        val moved = engine.play(placed, Move.PlaceShip(0, carrier.copy(origin = c(3, 1))))
        moved.player.ships[0] shouldBe carrier.copy(origin = c(3, 1))
        val turned = engine.play(moved, Move.RotateShip(0))
        turned.player.ships[0] shouldBe carrier.copy(origin = c(3, 1), orientation = Orientation.VERTICAL)
    }

    @Test
    fun `a turn that would leave the grid shifts the ship back inside`() {
        val atBottom = engine.play(vsAi, Move.PlaceShip(0, carrier.copy(origin = c(5, 0))))
        engine.play(atBottom, Move.RotateShip(0)).player.ships[0] shouldBe
            carrier.copy(origin = c(3, 0), orientation = Orientation.VERTICAL)
        val atRight = engine.play(
            vsAi,
            Move.PlaceShip(0, carrier.copy(origin = c(3, 7), orientation = Orientation.VERTICAL)),
        )
        engine.play(atRight, Move.RotateShip(0)).player.ships[0] shouldBe carrier.copy(origin = c(3, 3))
    }

    @Test
    fun `a turn that crosses a ship is refused, even after shifting, as is turning an unplaced ship`() {
        val shiftedOntoShip = engine.play(
            vsAi,
            Move.PlaceShip(0, carrier.copy(origin = c(5, 0))),
            Move.PlaceShip(3, Ship(ShipClass.DESTROYER, c(3, 0), Orientation.HORIZONTAL)),
        )
        engine.apply(shiftedOntoShip, Move.RotateShip(0)) shouldBe MoveOutcome.Rejected
        val crossed = engine.play(
            vsAi,
            Move.PlaceShip(0, carrier),
            Move.PlaceShip(3, Ship(ShipClass.DESTROYER, c(1, 0), Orientation.HORIZONTAL)),
        )
        engine.apply(crossed, Move.RotateShip(0)) shouldBe MoveOutcome.Rejected
        engine.apply(vsAi, Move.RotateShip(1)) shouldBe MoveOutcome.Rejected
        engine.apply(vsAi, Move.RotateShip(9)) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `a whole fleet is set at once only when it is valid`() {
        engine.play(vsAi, Move.SetFleet(rowFleet(BoardSize.EIGHT))).player.ships shouldBe rowFleet(BoardSize.EIGHT)
        engine.apply(vsAi, Move.SetFleet(rowFleet(BoardSize.EIGHT).drop(1))) shouldBe MoveOutcome.Rejected
        engine.apply(vsAi, Move.SetFleet(rowFleet(BoardSize.TEN).take(4))) shouldBe MoveOutcome.Rejected
        val overlapping = rowFleet(BoardSize.EIGHT).toMutableList().also { it[1] = it[1].copy(origin = c(0, 0)) }
        engine.apply(vsAi, Move.SetFleet(overlapping)) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `the fleet is confirmed only when complete, and the battle opens with the first mover`() {
        engine.apply(vsAi, Move.ConfirmFleet) shouldBe MoveOutcome.Rejected
        val battle = engine.play(vsAi, Move.SetFleet(rowFleet(BoardSize.EIGHT)), Move.ConfirmFleet)
        battle.phase shouldBe Phase.Battle
        battle.toMove shouldBe Side.ENEMY
        battle.isAiTurn shouldBe true
    }

    @Test
    fun `in pass-and-play player 2 places after player 1, then the battle starts`() {
        val twoPlayer = engine.newGame(mode(BoardSize.EIGHT, opponent = Opponent.TWO_PLAYER), Side.PLAYER)
        twoPlayer.enemy.ships shouldBe listOf(null, null, null, null)
        val second = engine.play(twoPlayer, Move.SetFleet(rowFleet(BoardSize.EIGHT)), Move.ConfirmFleet)
        second.phase shouldBe Phase.Placement(Side.ENEMY)
        second.toMove shouldBe Side.ENEMY
        val placed = engine.play(second, Move.PlaceShip(0, carrier.copy(origin = c(7, 3))))
        placed.enemy.ships[0] shouldBe carrier.copy(origin = c(7, 3))
        placed.player.ships shouldBe rowFleet(BoardSize.EIGHT)
        val battle = engine.play(placed, Move.SetFleet(rowFleet(BoardSize.EIGHT)), Move.ConfirmFleet)
        battle.phase shouldBe Phase.Battle
        battle.toMove shouldBe Side.PLAYER
        battle.isAiTurn shouldBe false
    }

    @Test
    fun `placement moves are refused once the battle starts`() {
        val battle = engine.battle()
        val placements =
            listOf(
                Move.PlaceShip(0, carrier),
                Move.RotateShip(0),
                Move.SetFleet(rowFleet(BoardSize.TEN)),
                Move.ConfirmFleet,
            )
        placements.forEach { engine.apply(battle, it) shouldBe MoveOutcome.Rejected }
    }

    @Test
    fun `nothing fires during placement`() {
        engine.apply(vsAi, Move.Fire(c(0, 0))).shouldBeInstanceOf<MoveOutcome.Rejected>()
        engine.apply(vsAi, Move.Resign(Side.PLAYER)) shouldBe MoveOutcome.Rejected
    }
}
