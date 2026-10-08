package io.github.vinaooo.battlegrid.domain.ai

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.Grid
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import kotlin.random.Random

/** Shots each AI level needs to sink a random fleet, over seeded games: `./gradlew :domain:benchmarkAi -Pgames=500`. */
object AiBenchmark {
    fun shotsToSink(ai: Ai, size: BoardSize, seed: Long): Int {
        var grid = Grid(size, RandomFleetPlacer.place(size, Random(seed)))
        val random = Random(seed + 1)
        while (!grid.isFleetSunk) grid = grid.shoot(ai.next(TargetView.of(grid), random)).first
        return grid.shots.size
    }

    fun averageShots(ai: Ai, size: BoardSize, games: Int): Double =
        (0 until games).map { shotsToSink(ai, size, it.toLong()) }.average()
}

fun main(args: Array<String>) {
    val games = args.firstOrNull()?.toInt() ?: 200
    BoardSize.entries.forEach { size ->
        val line = listOf("Easy" to EasyAi, "Medium" to MediumAi, "Hard" to HardAi).joinToString("  ") { (name, ai) ->
            "$name %.1f".format(AiBenchmark.averageShots(ai, size, games))
        }
        println("${size.side}×${size.side} (${size.side * size.side} cells, $games games): $line")
    }
}
