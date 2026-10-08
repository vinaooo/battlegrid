package io.github.vinaooo.battlegrid.feature.game

import io.github.vinaooo.battlegrid.domain.model.BoardSize
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.model.Side
import io.github.vinaooo.battlegrid.domain.placement.RandomFleetPlacer
import io.github.vinaooo.battlegrid.domain.rules.GameEngine
import io.github.vinaooo.battlegrid.domain.session.GameSession
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class BugReportTest {
    @Test
    fun `a report carries the settings, the game, its replayable state and its session file`() {
        val fleet = RandomFleetPlacer.place(BoardSize.TEN, Random(1))
        val session = GameSession(77, GameEngine().newGame(GameMode.DEFAULT, Side.ENEMY, fleet))
        val report = gameReport(GameUiState(session = session))

        report.details shouldHaveSize 2
        report.details[0] shouldContain "Settings: theme SYSTEM"
        report.details[1] shouldContain "Game: seed 77, TEN_CLASSIC_MEDIUM, first ENEMY, Placement(side=PLAYER)"
        GameSession.codec.decode(report.state!!) shouldBe session.state
        Json.decodeFromString(GameSession.serializer(), report.files.getValue("game.json")) shouldBe session
    }

    @Test
    fun `before a game loads, the report has the settings alone`() {
        val report = gameReport(GameUiState())
        report.details shouldHaveSize 1
        report.state shouldBe null
    }

    @Test
    fun `reports go to BattleGrid's own address and repository`() {
        REPORT_TARGET.email shouldBe "vrpedrinho+battlegrid@gmail.com"
        REPORT_TARGET.issuesUrl shouldBe "https://github.com/vinaooo/battlegrid/issues/new"
    }
}
