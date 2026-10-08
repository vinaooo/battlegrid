package io.github.vinaooo.battlegrid.domain

import io.github.vinaooo.battlegrid.domain.ai.AiBenchmark
import io.github.vinaooo.battlegrid.domain.ai.EasyAi
import io.github.vinaooo.battlegrid.domain.ai.HardAi
import io.github.vinaooo.battlegrid.domain.ai.MediumAi
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.kotest.matchers.doubles.shouldBeLessThan
import org.junit.jupiter.api.Test

/** Each level sinks a fleet in clearly fewer shots than the one below it, on every board. Capped by games, not time. */
class AiStrengthTest {
    @Test
    fun `hard beats medium beats easy in average shots to sink a fleet`() {
        BoardSize.entries.forEach { size ->
            val easy = AiBenchmark.averageShots(EasyAi, size, GAMES)
            val medium = AiBenchmark.averageShots(MediumAi, size, GAMES)
            val hard = AiBenchmark.averageShots(HardAi, size, GAMES)
            medium shouldBeLessThan easy * MARGIN
            hard shouldBeLessThan medium * MARGIN
        }
    }

    private companion object {
        const val GAMES = 60

        /** "Clearly fewer": at least 10% fewer shots. */
        const val MARGIN = 0.9
    }
}
