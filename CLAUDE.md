# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

BattleGrid is an open-source (MIT) Battleship for Android, to be published on Google Play: 8×8, 10×10 and 12×12
boards with scaled fleets, Classic / Hit-again / Salvo firing, against an AI (easy, medium, hard) or local
pass-and-play, plus a daily challenge and local achievements. Kotlin, Jetpack Compose, Material 3 Expressive, Hilt.
The UI is in pt-BR and English: every user-facing string lives in `values/strings.xml` and
`values-pt-rBR/strings.xml` of its module, never hard-coded. The user makes the product and design decisions, so ask
before choosing one. The confirmed rules are section 1a of `game-prompt-template.md`; the plan, with the decisions
and their defaults, is `PLAN.md`.

- applicationId and package `io.github.vinaooo.battlegrid`; app name "BattleGrid" (`app_name`); brand color teal.
- Built on **vinkit** (`../vinkit`, github.com/vinaooo/vinkit): build-logic plugins (`vinkit.*`), the version
  catalog, theme, ads, bug report, settings, scores and the game screen shell come from it. OX Play (`../xo`) is the
  reference game.
  - Always the newest published vinkit tag (`vinkit.tag` in `gradle.properties`), from JitPack; never a local
    `includeBuild` of vinkit (user's rule).
  - A kit gap found here is fixed in vinkit, released as a new tag, and then used here. Never bump the other games.

## Commands

```bash
./gradlew assembleDebug                          # build
./gradlew installDebug                           # build + install on the connected device
./gradlew ktlintCheck detekt                     # static analysis (ktlintFormat fixes formatting)
./gradlew test koverVerify                       # unit tests + coverage gates
./gradlew lint
```

- **Low RAM:** `gradle.properties` is sized for a low-RAM dev machine (2 GB Gradle heap, no parallel builds). Don't
  raise these values. Run the gate in pieces (`ktlintCheck detekt`, `test koverVerify`, `lint`): all of it in one
  invocation runs out of Metaspace. CI writes its own larger settings.
- Coverage: vinkit's filters leave out composables, generated code, `*Activity` and `*Application`.

## Domain (`:domain`, pure Kotlin)

- `Grid` stores a side's fleet (`ships`, one slot per class of `BoardSize.fleet`, null until placed) and the shots
  fired at it, in order. Hit / miss / sunk are always derived from those two (`resultAt`, `isSunk`), never stored.
- `GameState.toMove` fires at `target` (the other side's grid). `actions` counts applied battle moves and will seed
  the AI's random choices (`Random(seed * 31 + actions)`): never change that after release.
- One `MoveRule` per `Move` type (`rules/Rules.kt`), dispatched by `GameEngine`; firing modes are `FiringRule`s
  (shots per turn, keeps the turn). A salvo resolves shot by shot and stops at the last sinking.
- The Random placement button is a `Move.SetFleet` with a fleet built outside the engine, so the engine stays pure.
- No undo in battle (it would leak where ships aren't). Leaving during placement isn't a loss
  (`GameSession.isInProgress` is true only in battle).
- Mode keys (`GameMode.key`, e.g. `TEN_SALVO_HARD`, `DAILY`) file scores and stats in vinkit: never rename them.
- AI (`ai/Ai.kt`): each level sees only a `TargetView` (misses, open hits, sunk cells, lengths afloat), never the
  fleet. A salvo is picked one cell at a time, each chosen cell treated as a miss for the next. Random choices come
  from `aiRandom(seed, state)` = `Random(seed * 31 + actions)`: frozen after release.
- Strength: `./gradlew :domain:benchmarkAi -Pgames=300` prints average shots to sink a random fleet per level and
  size (10×10: Easy ≈ 95, Medium ≈ 59, Hard ≈ 46). `AiStrengthTest` requires each level ≥ 10% fewer shots than the
  one below, over 60 seeded games (capped by games, never timed).
- Fleets: `RandomFleetPlacer` picks each ship among every spot still free, so it never retries and a seed gives the
  same fleet everywhere. Hints: `HintEngine` (5 untried cells, exactly 1 on a ship afloat; fewer when water runs out),
  `Random(seed * 37 + hintsUsed)`; `Move.ShowHint` is checked by `HintEngine.isValid` and cleared by the next shot.
- Pitest skips `GameInvariantsPropertyTest` and `AiStrengthTest` (whole games under every mutant); exact tests pin the same code.

## Data and use cases

- Use cases (`domain/usecase`): `StartNewGame` / `RestartGame` count a left battle against the AI as a loss (and
  the AI fires first next); a placement or a pass-and-play game isn't recorded. `FinishGame` records a win with its
  score (`points` = shots + 5 per hint, extras `shots`, `hits`, `hints`) or a loss, and hands the next first shot to
  the winner. The very first game draws the first mover from its seed (`GameSettings.nextFirstMover == null`).
- Restart keeps the seed: the same AI fleet and first mover; the player places again.
- `:data`: `FileSavedGameRepository` (versioned JSON, temp file + rename; a corrupt, invalid or newer file is
  discarded) and `DataStoreGameSettingsRepository` (keys `board_size`, `firing_mode`, `opponent`,
  `next_first_mover`, beside vinkit's in one DataStore). `Grid` and `GameState` `require` a consistent fleet and
  board, so a tampered save is discarded instead of loading broken.
- `:app/di/UseCaseModule` builds the use cases; `DataModule` provides vinkit's settings, scores and stats.

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per
milestone (`PLAN.md`). Commit, push and open PRs only when the user asks.
