package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FleetPlacerTest {
    private val engine = GameEngine()

    @Test
    fun `a random fleet is always a valid fleet for its board`() = runTest {
        checkAll(ITERATIONS, Arb.enum<BoardSize>(), Arb.long()) { size, seed ->
            val fleet = RandomFleetPlacer.place(size, Random(seed))
            fleet.map { it.type } shouldBe size.fleet
            val placing = engine.newGame(mode(size), Side.PLAYER, fleet)
            engine.isLegal(placing, Move.SetFleet(fleet)) shouldBe true
        }
    }

    @Test
    fun `the same seed places the same fleet, another seed another one`() {
        RandomFleetPlacer.place(BoardSize.TEN, Random(7)) shouldBe RandomFleetPlacer.place(BoardSize.TEN, Random(7))
        RandomFleetPlacer.place(BoardSize.TEN, Random(7)) shouldNotBe RandomFleetPlacer.place(BoardSize.TEN, Random(8))
    }

    private companion object {
        const val ITERATIONS = 300
    }
}
