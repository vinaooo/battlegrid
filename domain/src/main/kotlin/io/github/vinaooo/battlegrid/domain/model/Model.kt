package io.github.vinaooo.battlegrid.domain.model

import kotlinx.serialization.Serializable

/** A ship's class: its length, and its name on screen (a string resource keyed by the class). */
enum class ShipClass(val length: Int) {
    CARRIER(5),
    BATTLESHIP(4),
    FRIGATE(4),
    CRUISER(3),
    SUBMARINE(3),
    DESTROYER(2),
}

/** A board's side and the fleet each player places on it: bigger boards get more ships. */
enum class BoardSize(val side: Int, val fleet: List<ShipClass>) {
    EIGHT(8, listOf(ShipClass.CARRIER, ShipClass.BATTLESHIP, ShipClass.CRUISER, ShipClass.DESTROYER)),
    TEN(
        10,
        listOf(ShipClass.CARRIER, ShipClass.BATTLESHIP, ShipClass.CRUISER, ShipClass.SUBMARINE, ShipClass.DESTROYER),
    ),
    TWELVE(
        12,
        listOf(
            ShipClass.CARRIER,
            ShipClass.BATTLESHIP,
            ShipClass.FRIGATE,
            ShipClass.CRUISER,
            ShipClass.SUBMARINE,
            ShipClass.DESTROYER,
        ),
    ),
    ;

    /** Every cell, row by row. */
    val cells: List<Coord> get() = (0 until side).flatMap { row -> (0 until side).map { Coord(row, it) } }
}

/** How many shots a turn has: one, one more after each hit, or one per ship still afloat. */
enum class FiringMode {
    CLASSIC,
    HIT_AGAIN,
    SALVO,
}

enum class Opponent {
    EASY,
    MEDIUM,
    HARD,
    TWO_PLAYER,
}

/** The two fleets: the player's (Player 1 in pass-and-play) and the enemy's (the AI, or Player 2). */
enum class Side {
    PLAYER,
    ENEMY,
    ;

    val other: Side get() = if (this == PLAYER) ENEMY else PLAYER
}

/**
 * A board size, a firing mode and an opponent. It travels with the game: a resumed game keeps it whatever Settings
 * say now. [daily] games are the daily challenge, recorded apart.
 */
@Serializable
data class GameMode(val size: BoardSize, val firing: FiringMode, val opponent: Opponent, val daily: Boolean = false) {
    val isVsAi: Boolean get() = opponent != Opponent.TWO_PLAYER

    /** The key vinkit stores this mode's scores and stats under. Never renamed after release. */
    val key: String get() = if (daily) DAILY_KEY else "${size.name}_${firing.name}_${opponent.name}"

    companion object {
        const val DAILY_KEY = "DAILY"

        val DEFAULT = GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.MEDIUM)

        /** The daily challenge's fixed rules. */
        val DAILY = GameMode(BoardSize.TEN, FiringMode.CLASSIC, Opponent.HARD, daily = true)

        val ALL: List<GameMode> = BoardSize.entries.flatMap { size ->
            FiringMode.entries.flatMap { firing -> Opponent.entries.map { GameMode(size, firing, it) } }
        }

        fun fromKey(key: String): GameMode? = if (key == DAILY_KEY) DAILY else ALL.firstOrNull { it.key == key }
    }
}

@Serializable
data class Coord(val row: Int, val col: Int) {
    fun isOn(side: Int): Boolean = row in 0 until side && col in 0 until side
}

enum class Orientation {
    HORIZONTAL,
    VERTICAL,
    ;

    val other: Orientation get() = if (this == HORIZONTAL) VERTICAL else HORIZONTAL
}

/** A ship on a grid: its bow at [origin], running right ([Orientation.HORIZONTAL]) or down. */
@Serializable
data class Ship(val type: ShipClass, val origin: Coord, val orientation: Orientation) {
    val cells: List<Coord>
        get() = List(type.length) { i ->
            if (orientation == Orientation.HORIZONTAL) {
                Coord(origin.row, origin.col + i)
            } else {
                Coord(origin.row + i, origin.col)
            }
        }
}

/** What a shot found. */
enum class ShotResult {
    MISS,
    HIT,

    /** The hit that sank its ship. */
    SUNK,
}
