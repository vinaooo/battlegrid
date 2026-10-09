package io.github.vinaooo.battlegrid.feature.game.board

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The grids' colors, all from the theme's scheme, so every palette and both themes stay readable. */
@Immutable
internal data class BoardColors(
    val sea: Color,
    val line: Color,
    val label: Color,
    val ship: Color,
    val hit: Color,
    /** A hit drawn over a ship (the player's own, or a sunk one): the ship's own contrast color. */
    val hitOnShip: Color,
    val miss: Color,
    val mark: Color,
    val hint: Color,
)

internal fun boardColors(scheme: ColorScheme) = BoardColors(
    sea = scheme.surfaceContainerHigh,
    line = scheme.outlineVariant,
    label = scheme.onSurfaceVariant,
    ship = scheme.secondary,
    hit = scheme.error,
    hitOnShip = scheme.onSecondary,
    miss = scheme.outline,
    mark = scheme.primary,
    hint = scheme.tertiary,
)

/** The board's colors, provided by [ProvideBoardColors]. */
internal val LocalBoardColors = staticCompositionLocalOf<BoardColors> {
    error("No BoardColors: wrap the board in ProvideBoardColors")
}

/** Gives [content] the board's colors, built from the current theme's scheme. */
@Composable
internal fun ProvideBoardColors(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalBoardColors provides remember(scheme) { boardColors(scheme) }, content = content)
}
