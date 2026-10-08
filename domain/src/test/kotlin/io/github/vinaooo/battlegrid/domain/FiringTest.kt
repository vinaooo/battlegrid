package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Phase
import io.github.vinaooo.battlegrid.domain.model.ShotResult
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.MoveOutcome
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Both fleets are [rowFleet]: on 10×10, row 1 is water and the destroyer is (8, 0)–(8, 1). */
class FiringTest {
    private val engine = GameEngine()

    @Test
    fun `classic turns alternate after every shot, hit or miss`() {
        val battle = engine.battle()
        val hit = engine.play(battle, Move.Fire(c(0, 0)))
        hit.enemy.resultAt(c(0, 0)) shouldBe ShotResult.HIT
        hit.toMove shouldBe Side.ENEMY
        val reply = engine.play(hit, Move.Fire(c(1, 1)))
        reply.player.resultAt(c(1, 1)) shouldBe ShotResult.MISS
        reply.toMove shouldBe Side.PLAYER
        reply.actions shouldBe 2
        reply.playerShots shouldBe 1
    }

    @Test
    fun `a cell is fired at once, and only inside the grid`() {
        val battle = engine.play(engine.battle(), Move.Fire(c(1, 1)), Move.Fire(c(5, 5)))
        engine.apply(battle, Move.Fire(c(1, 1))) shouldBe MoveOutcome.Rejected
        engine.apply(battle, Move.Fire(c(10, 0))) shouldBe MoveOutcome.Rejected
        engine.apply(battle, Move.Fire(c(0, -1))) shouldBe MoveOutcome.Rejected
        // The enemy's shot at (5, 5) was on the player's grid: the player may still fire there.
        engine.isLegal(battle, Move.Fire(c(5, 5))) shouldBe true
    }

    @Test
    fun `hit-again keeps the turn after a hit or a sinking and passes it on a miss`() {
        val battle = engine.battle(mode(firing = FiringMode.HIT_AGAIN))
        val hit = engine.play(battle, Move.Fire(c(8, 0)))
        hit.toMove shouldBe Side.PLAYER
        val sunk = engine.play(hit, Move.Fire(c(8, 1)))
        sunk.enemy.isSunk(4) shouldBe true
        sunk.toMove shouldBe Side.PLAYER
        engine.play(sunk, Move.Fire(c(1, 0))).toMove shouldBe Side.ENEMY
    }

    @Test
    fun `the AI fires again on a hit too`() {
        val battle = engine.battle(mode(firing = FiringMode.HIT_AGAIN), first = Side.ENEMY)
        engine.play(battle, Move.Fire(c(0, 0))).toMove shouldBe Side.ENEMY
    }

    @Test
    fun `classic and hit-again refuse salvo moves`() {
        val battle = engine.battle()
        engine.apply(battle, Move.MarkSalvo(c(0, 0))) shouldBe MoveOutcome.Rejected
        engine.apply(battle, Move.FireSalvo) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `salvo fires one shot per own ship afloat, marked first, then all at once`() {
        val battle = engine.battle(mode(firing = FiringMode.SALVO))
        engine.apply(battle, Move.Fire(c(0, 0))) shouldBe MoveOutcome.Rejected
        engine.apply(battle, Move.FireSalvo) shouldBe MoveOutcome.Rejected
        val marks = listOf(c(0, 0), c(1, 0), c(1, 1), c(1, 2), c(8, 0))
        val marked = engine.play(battle, *marks.map(Move::MarkSalvo).toTypedArray())
        marked.salvoMarks shouldBe marks
        engine.apply(marked, Move.MarkSalvo(c(1, 3))) shouldBe MoveOutcome.Rejected
        engine.play(marked, Move.MarkSalvo(c(1, 2))).salvoMarks shouldBe marks - c(1, 2)

        val fired = engine.play(marked, Move.FireSalvo)
        marks.map { fired.enemy.resultAt(it) } shouldBe
            listOf(ShotResult.HIT, ShotResult.MISS, ShotResult.MISS, ShotResult.MISS, ShotResult.HIT)
        fired.salvoMarks shouldBe emptyList()
        fired.toMove shouldBe Side.ENEMY
        fired.actions shouldBe 1
        // The enemy's turn now: it marks its own salvo, at the player's grid.
        engine.apply(fired, Move.FireSalvo) shouldBe MoveOutcome.Rejected
        engine.play(fired, Move.MarkSalvo(c(0, 0))).salvoMarks shouldBe listOf(c(0, 0))
    }

    @Test
    fun `a side that lost ships fires fewer shots`() {
        val battle = engine.battle(mode(firing = FiringMode.SALVO), first = Side.ENEMY)
        // The enemy sinks the player's destroyer and hits the submarine with its five shots.
        val shots = listOf(c(8, 0), c(8, 1), c(6, 0), c(1, 0), c(1, 1)).map(Move::MarkSalvo)
        val afterEnemy = engine.play(battle, *shots.toTypedArray(), Move.FireSalvo)
        afterEnemy.player.shipsAfloat shouldBe 4
        afterEnemy.toMove shouldBe Side.PLAYER
        val four = listOf(c(1, 0), c(1, 1), c(1, 2), c(1, 3)).map(Move::MarkSalvo)
        val marked = engine.play(afterEnemy, *four.toTypedArray())
        engine.apply(marked, Move.MarkSalvo(c(1, 4))) shouldBe MoveOutcome.Rejected
        engine.isLegal(marked, Move.FireSalvo) shouldBe true
    }

    @Test
    fun `sinking the last ship wins at once, also in the middle of a salvo`() {
        val battle = engine.battle(mode(BoardSize.EIGHT, FiringMode.SALVO))
        val fleetCells = rowFleet(BoardSize.EIGHT).flatMap { it.cells }
        // Every enemy ship hit but the destroyer's last cell.
        val nearlyWon = battle.copy(enemy = battle.enemy.copy(shots = fleetCells.dropLast(1)))
        nearlyWon.enemy.shipsAfloat shouldBe 1
        // Four shots: the destroyer's last cell is the second; the last two are never fired.
        val marks = listOf(c(3, 7), fleetCells.last(), c(3, 6), c(3, 5)).map(Move::MarkSalvo)
        val won = engine.play(nearlyWon, *marks.toTypedArray(), Move.FireSalvo)
        won.phase shouldBe Phase.Over(Side.PLAYER)
        won.winner shouldBe Side.PLAYER
        won.enemy.isTried(c(3, 6)) shouldBe false
        won.isOver shouldBe true
        engine.legalMoves(won) shouldBe emptyList()
        engine.apply(won, Move.MarkSalvo(c(5, 5))) shouldBe MoveOutcome.Rejected
        engine.apply(won, Move.Fire(c(5, 5))) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `a salvo never asks for more shots than cells are left`() {
        val battle = engine.battle(mode(BoardSize.EIGHT, FiringMode.SALVO))
        val allButTwo = battle.enemy.untried.filter { it != c(7, 6) && it != c(7, 7) }
        val cornered = battle.copy(enemy = battle.enemy.copy(shots = allButTwo.filter { it != c(6, 0) }))
        // Four ships afloat, three cells untried: three shots.
        cornered.target.untried.size shouldBe 3
        val marked = engine.play(cornered, Move.MarkSalvo(c(7, 6)), Move.MarkSalvo(c(7, 7)), Move.MarkSalvo(c(6, 0)))
        engine.isLegal(marked, Move.FireSalvo) shouldBe true
    }

    @Test
    fun `a classic battle is won by the side that sinks every ship`() {
        val battle = engine.battle(mode(BoardSize.EIGHT, FiringMode.HIT_AGAIN))
        val won = engine.play(battle, *rowFleet(BoardSize.EIGHT).flatMap { it.cells }.map(Move::Fire).toTypedArray())
        won.phase shouldBe Phase.Over(Side.PLAYER)
        won.playerShots shouldBe 14
    }

    @Test
    fun `the AI can win too`() {
        val battle = engine.battle(mode(BoardSize.EIGHT, FiringMode.HIT_AGAIN), first = Side.ENEMY)
        val won = engine.play(battle, *rowFleet(BoardSize.EIGHT).flatMap { it.cells }.map(Move::Fire).toTypedArray())
        won.winner shouldBe Side.ENEMY
    }

    @Test
    fun `resigning hands the battle to the other side`() {
        val resigned = engine.play(engine.battle(mode(firing = FiringMode.SALVO)), Move.MarkSalvo(c(1, 1)))
            .let { engine.play(it, Move.Resign(Side.PLAYER)) }
        resigned.phase shouldBe Phase.Over(Side.ENEMY, resigned = true)
        resigned.salvoMarks shouldBe emptyList()
        engine.apply(resigned, Move.Resign(Side.ENEMY)) shouldBe MoveOutcome.Rejected
        engine.play(engine.battle(), Move.Resign(Side.ENEMY)).winner shouldBe Side.PLAYER
    }
}
