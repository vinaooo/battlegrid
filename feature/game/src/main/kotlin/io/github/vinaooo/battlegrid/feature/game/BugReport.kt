package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.github.vinaooo.vinkit.bugreport.GameReport
import io.github.vinaooo.vinkit.bugreport.ReportTarget
import kotlinx.serialization.json.Json

/** Where BattleGrid's bug reports go: the contact in its privacy policy, chosen by the user, or a GitHub issue. */
val REPORT_TARGET = ReportTarget(email = "vrpedrinho+battlegrid@gmail.com", githubRepo = "vinaooo/battlegrid")

/**
 * What BattleGrid adds to a bug report: the settings and the game in a line each, the exact state as `GameCodec` text
 * (for a GitHub issue), and the whole session as `game.json` (for an email).
 */
internal fun gameReport(uiState: GameUiState): GameReport {
    val settings = with(uiState.settings) {
        "Settings: theme $themeMode, dynamic color $dynamicColor, color $themeColor, $handedness hand, " +
            "board $boardAlignment, phone view $phoneView"
    }
    val session = uiState.session ?: return GameReport(details = listOf(settings))
    val state = session.state
    val game = "Game: seed ${session.seed}, ${state.mode.key}, first ${state.firstMover}, ${state.phase}, " +
        "to move ${state.toMove}, ${state.actions} battle moves, ${state.hintsUsed} hints, ${state.elapsedSeconds} s"
    return GameReport(
        details = listOf(settings, game),
        state = GameSession.codec.encode(state),
        files = mapOf(GAME_FILE to Json.encodeToString(GameSession.serializer(), session)),
    )
}

private const val GAME_FILE = "game.json"
