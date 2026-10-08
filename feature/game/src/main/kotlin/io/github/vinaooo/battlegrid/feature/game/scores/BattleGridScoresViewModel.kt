package io.github.vinaooo.battlegrid.feature.game.scores

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.battlegrid.domain.model.GameMode
import io.github.vinaooo.battlegrid.domain.rules.ShotsScoring
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.scores.ScoresViewModel
import javax.inject.Inject

/**
 * vinkit's Scores: a tab per board size played, a section per firing mode × opponent in it, ranked by fewest shots.
 * Pass-and-play isn't recorded.
 */
@HiltViewModel
class BattleGridScoresViewModel @Inject constructor(scores: ScoreRepository, stats: StatsRepository) :
    ScoresViewModel(
        scores,
        stats,
        modes = GameMode.ALL.filter { it.isVsAi }.map { it.key },
        groupOf = { key -> GameMode.fromKey(key)?.size?.name ?: key },
        rankingFor = { ShotsScoring.ranking },
    )
