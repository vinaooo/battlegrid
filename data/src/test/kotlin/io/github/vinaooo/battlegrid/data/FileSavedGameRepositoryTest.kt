package io.github.vinaooo.battlegrid.data

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Coord
import io.github.vinaooo.battlegrid.domain.model.FiringMode
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Move
import io.github.vinaooo.battlegrid.domain.model.Opponent
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileSavedGameRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val dispatcher = StandardTestDispatcher()
    private val file get() = File(dir, "saved_game.json")
    private val repository get() = FileSavedGameRepository(file, dispatcher)
    private val engine = GameEngine()
    private val mode = GameMode(BoardSize.TEN, FiringMode.SALVO, Opponent.HARD)

    /** A salvo battle with shots fired both ways and marks pending. */
    private val session = GameSession(
        11,
        engine.newGame(mode, Side.PLAYER, RandomFleetPlacer.place(mode.size, Random(1))),
    )
        .play(Move.SetFleet(RandomFleetPlacer.place(mode.size, Random(2))), engine).shouldNotBeNull()
        .play(Move.ConfirmFleet, engine).shouldNotBeNull()
        .let { (0 until 5).fold(it) { s, col -> s.play(Move.MarkSalvo(Coord(0, col)), engine).shouldNotBeNull() } }
        .play(Move.FireSalvo, engine).shouldNotBeNull()
        .play(Move.MarkSalvo(Coord(9, 9)), engine).shouldNotBeNull()
        .tick()

    @Test
    fun `nothing saved loads as null`() = runTest(dispatcher) {
        repository.load().shouldBeNull()
    }

    @Test
    fun `a saved battle loads back whole`() = runTest(dispatcher) {
        repository.save(session)

        FileSavedGameRepository(file, dispatcher).load() shouldBe session
    }

    @Test
    fun `saving again replaces the previous game`() = runTest(dispatcher) {
        repository.save(session)
        val newer = session.tick()
        repository.save(newer)

        repository.load() shouldBe newer
    }

    @Test
    fun `clear removes the saved game`() = runTest(dispatcher) {
        repository.save(session)
        repository.clear()

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a corrupted file is discarded instead of crashing`() = runTest(dispatcher) {
        file.writeText("{ not json")

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a fleet that doesn't match its board is discarded`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replaceFirst("\"size\":\"TEN\"", "\"size\":\"EIGHT\""))

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a file from an unknown future version is ignored`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replace("\"version\":1", "\"version\":99"))

        repository.load().shouldBeNull()
    }

    @Test
    fun `no temporary files are left behind`() = runTest(dispatcher) {
        repository.save(session)

        dir.listFiles()!!.map { it.name } shouldBe listOf("saved_game.json")
    }
}
