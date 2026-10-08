package io.github.vinaooo.battlegrid.feature.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.vinaooo.battlegrid.feature.game.board.boardColors
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.paletteScheme
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import org.junit.jupiter.api.Test

/** Ships, hits, misses, marks and hints stay readable on the sea in every palette, light and dark. */
class BoardColorsTest {
    @Test
    fun `every mark keeps 3 to 1 contrast against the sea`() {
        ThemeColor.entries.forEach { color ->
            listOf(false, true).forEach { dark ->
                val board = boardColors(paletteScheme(color, dark))
                mapOf(
                    "ship" to board.ship,
                    "hit" to board.hit,
                    "miss" to board.miss,
                    "mark" to board.mark,
                    "hint" to board.hint,
                    "label" to board.label,
                ).forEach { (name, mark) ->
                    withClue("$name on $color, dark=$dark") {
                        contrast(mark, board.sea) shouldBeGreaterThanOrEqual MIN_CONTRAST
                    }
                }
                withClue("hit on a ship, $color, dark=$dark") {
                    contrast(board.hitOnShip, board.ship) shouldBeGreaterThanOrEqual MIN_CONTRAST
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + OFFSET) / (dark + OFFSET).toDouble()
    }

    private companion object {
        const val MIN_CONTRAST = 3.0
        const val OFFSET = 0.05f
    }
}
