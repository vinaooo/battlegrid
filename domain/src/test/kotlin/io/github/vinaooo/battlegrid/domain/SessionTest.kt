package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.rules.ShotsScoring
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.vinkit.core.Ranking
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SessionTest {
    private val engine = GameEngine()

    @Test
    fun `every mode has its own stable key, and the daily one is apart`() {
        GameMode(BoardSize.TEN, FiringMode.SALVO, Opponent.HARD).key shouldBe "TEN_SALVO_HARD"
        GameMode(BoardSize.EIGHT, FiringMode.HIT_AGAIN, Opponent.TWO_PLAYER).key shouldBe "EIGHT_HIT_AGAIN_TWO_PLAYER"
        GameMode.DAILY.key shouldBe "DAILY"
        GameMode.DAILY shouldBe GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.HARD, daily = true)
        GameMode.ALL.size shouldBe 36
        GameMode.ALL.map { it.key }.distinct().size shouldBe 36
        GameMode.ALL.forEach { GameMode.fromKey(it.key) shouldBe it }
        GameMode.fromKey("DAILY") shouldBe GameMode.DAILY
        GameMode.fromKey("NOPE") shouldBe null
        GameMode.ALL.count { it.isVsAi } shouldBe 27
        GameMode.DEFAULT shouldBe GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.MEDIUM)
    }

    @Test
    fun `the score is the player's shots plus five per hint, fewest first`() {
        val battle = engine.play(engine.battle(), Move.Fire(c(1, 1)), Move.Fire(c(1, 1)), Move.Fire(c(0, 0)))
        ShotsScoring.points(battle) shouldBe 2
        ShotsScoring.points(battle.copy(hintsUsed = 2)) shouldBe 12
        ShotsScoring.ranking shouldBe Ranking.LOWEST_POINTS
    }

    @Test
    fun `the clock runs during the battle only`() {
        val placing = GameSession(1, engine.newGame(mode(), Side.PLAYER, rowFleet(BoardSize.TEN)))
        placing.tick() shouldBe placing
        val battle = GameSession(1, engine.battle())
        battle.tick().tick().state.elapsedSeconds shouldBe 2
        val over = battle.play(Move.Resign(Side.PLAYER), engine)!!
        over.tick() shouldBe over
    }

    @Test
    fun `a battle in progress counts, a placement or a finished game does not`() {
        GameSession(1, engine.newGame(mode(), Side.PLAYER, rowFleet(BoardSize.TEN))).isInProgress shouldBe false
        val battle = GameSession(1, engine.battle())
        battle.isInProgress shouldBe true
        battle.play(Move.Resign(Side.PLAYER), engine)!!.isInProgress shouldBe false
    }

    @Test
    fun `a rejected move leaves no new session`() {
        val battle = GameSession(1, engine.battle())
        battle.play(Move.ConfirmFleet, engine) shouldBe null
        battle.play(Move.Fire(c(0, 0)), engine)!!.state.enemy.shots shouldBe listOf(c(0, 0))
    }

    @Test
    fun `a game round-trips through the bug report codec`() {
        val state = engine.play(engine.battle(mode(firing = FiringMode.SALVO)), Move.MarkSalvo(c(3, 3)))
        GameSession.codec.decode(GameSession.codec.encode(state)) shouldBe state
    }
}
