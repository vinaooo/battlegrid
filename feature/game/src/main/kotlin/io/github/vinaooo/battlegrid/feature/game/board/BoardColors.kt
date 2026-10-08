package io.github.vinaooo.battlegrid.feature.game.board

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
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
