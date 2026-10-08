package io.github.vinaooo.battlegrid.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.battlegrid.MainActivity
import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Orientation
import io.github.vinaooo.battlegrid.domain.model.Ship
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.repository.SavedGameRepository
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Debug builds only. Saves a test game and opens it, to try a position without playing up to it. Start it with `-S`,
 * so a running game screen can't overwrite the save:
 * `adb shell am start -S -n io.github.vinaooo.battlegrid/.debug.DebugGameActivity --es game near_win`
 *
 * Both fleets lie along every other row from the top-left (ship i on row 2i, from column A).
 * - `near_win` (the default): 10×10 Classic vs Medium, the player one shot (B9) from winning.
 * - `near_loss`: the same, the AI one shot from winning, on its turn.
 * - `salvo_big`: 12×12 Salvo vs Hard, the battle just started, six ships afloat each.
 * - `handover`: 8×8 pass-and-play, player 1's fleet placed, the phone going to player 2.
 *
 * Or replay a bug report:
 * - `--es state <code>`: the code in the report's "State:" block, the exact game.
 * - `--es load game.json`: the report's attached game, pushed first to the app's folder:
 *   `adb push game.json /sdcard/Android/data/io.github.vinaooo.battlegrid/files/`
 */
@AndroidEntryPoint
class DebugGameActivity : ComponentActivity() {

    @Inject lateinit var savedGames: SavedGameRepository

    @Inject lateinit var engine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking {
            val report = intent.getStringExtra("state")?.let { GameSession(SEED, GameSession.codec.decode(it)) }
                ?: intent.getStringExtra("load")?.let {
                    Json.decodeFromString(GameSession.serializer(), File(getExternalFilesDir(null), it).readText())
                }
            savedGames.save(report ?: preset(intent.getStringExtra("game")))
        }
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK,
            ),
        )
        finish()
    }

    private fun preset(name: String?): GameSession = when (name) {
        "near_loss" -> nearlyOver(GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.MEDIUM), Side.ENEMY)
        "salvo_big" -> battle(GameMode(BoardSize.TWELVE, FiringMode.SALVO, Opponent.HARD), Side.PLAYER)
        "handover" -> GameSession(
            SEED,
            engine.newGame(GameMode(BoardSize.EIGHT, FiringMode.CLASSIC, Opponent.TWO_PLAYER), Side.PLAYER),
        )
            .play(Move.SetFleet(rowFleet(BoardSize.EIGHT)), engine)!!
            .play(Move.ConfirmFleet, engine)!!
        else -> nearlyOver(GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.MEDIUM), Side.PLAYER)
    }

    private fun battle(mode: GameMode, first: Side): GameSession =
        GameSession(SEED, engine.newGame(mode, first, rowFleet(mode.size)))
            .play(Move.SetFleet(rowFleet(mode.size)), engine)!!
            .play(Move.ConfirmFleet, engine)!!

    /** Every ship cell of the side [shooter] fires at is hit but the last one, and it's [shooter]'s turn. */
    private fun nearlyOver(mode: GameMode, shooter: Side): GameSession {
        val session = battle(mode, shooter)
        val target = session.state.gridOf(shooter.other)
        val hit = target.copy(shots = target.ships.flatMap { it!!.cells }.dropLast(1))
        return session.copy(state = session.state.withGrid(shooter.other, hit))
    }

    private fun rowFleet(size: BoardSize): List<Ship> =
        size.fleet.mapIndexed { i, type -> Ship(type, Coord(2 * i, 0), Orientation.HORIZONTAL) }

    private companion object {
        const val SEED = 7L
    }
}
