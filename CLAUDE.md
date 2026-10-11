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
  reference for wiring vinkit (the first game built on it); Solo and Sudoku Trio are the visual reference for the
  shared surfaces *(user, 2026-10-10)*.
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

## Game screen (`:feature:game`)

- `GameUiState.viewer` decides which grid is "own" and which is "target": the player vs the AI; in pass-and-play the
  side placing or firing. The target grid's view (`targetView`) never holds unsunk ships until the game is over, and
  the hint's 5 cells look alike, so the fleet can't leak through drawing or TalkBack.
- The ViewModel's AI loop reads the latest state before every shot; a new game, restart or resign cancels it
  (`pending`). Pauses: 600 ms before each AI shot and between a salvo's results (both sides); a salvo is in the
  session at once, the newest `hiddenShots` held back and drawn as marks until shown. Pass-and-play: the result shows
  1 s, then the cover.
- The grids never swap (user's rule): the target is always big, the own grid small above it in portrait (one
  TalkBack item with a summary); landscape shows both at the same size.
- The cover replaces the whole screen (not drawn over it), so TalkBack and the bug-report screenshot can't reach the
  hidden grids.
- Placement drag: the drop uses the drag-start point plus the raw travel; only the drawn offset adds the touch slop.
  The dock gives each ship a fixed slot, so ships don't jump when another is placed.
- Debug presets: `adb shell am start -S -n io.github.vinaooo.battlegrid/.debug.DebugGameActivity --es game <preset>`
  with `near_win`, `near_loss`, `salvo_big`, `handover` (see the class's doc).
- Known, to check with TalkBack on: on the Moto, `uiautomator` reports the target grid's cell nodes about a cell off
  (touches and Robolectric bounds are right; the own grid's nodes are right).

## App shell (`:app`)

- `BattleGridApp`: a NavHost with type-safe routes (game, Scores, Settings). Only the game route carries the banner
  (a `Column`: the game takes `weight(1f)` and consumes the bottom navigation-bar inset, the banner pads for it).
- Ads: vinkit's `AdMobBanner` and `DefaultAdConsent` (`di/AdsModule`), Google's test IDs until real ones are set;
  consent is gathered once per launch in `MainActivity`. App tests swap them for fakes (`src/sharedTest`,
  `FakeAdsModule`).
- Scores: a tab per board size, a section per firing mode × opponent, ranked `LOWEST_POINTS` (shots + 5 per hint);
  pass-and-play isn't recorded. Settings: Board and Firing (`Choice`), Opponent (`IconChoice`); a change during a
  battle asks first (`NewGameConfirmDialog`), during placement it applies at once.
- Bug report: `ReportTarget("vrpedrinho+battlegrid@gmail.com", "vinaooo/battlegrid")`, with the settings, a game
  line, the `GameCodec` state and `game.json`.

## Daily challenge, badges, sounds

- Daily: `dailySeed(day)` (UTC days since the epoch; frozen after release), 10×10 Classic vs Hard, the player fires
  first, same AI fleet and AI random stream for everyone. The first daily finished or left mid-battle that day is
  ranked (`GameSession.recorded`); later ones are practice (no stats, scores or badges). `DailyRecord` keeps the last
  ranked day and the streak of days played in a row.
- Badges: vinkit's: `AchievementProgress` and `DataStoreAchievementRepository` (settings DataStore,
  `achievements_*` keys), `BadgesScreen`. The rules stay here: `Achievements.after` (pure), with typed views
  `badges` / `sizesWon` / `firingsWon` (collected `sizes_won`, `firings_won`); `Achievement` names and those keys:
  never rename. `FinishGame` returns the ones just earned; the end dialog lists them. Their screen (`badges/`) is
  opened by a button beside Scores (vinkit 0.7.0 `GameFrame(navigation = …)`).
- How to play opens by itself once (`GameSettings.howToPlaySeen`), and from the new-game menu.
- Sounds: see Game identity → Motion and feedback.

## Icon and screenshots

- Launcher icon: a warship on waves, white on the brand teal (`#FF006B5F`, vinkit's teal primary), with a separate
  `ic_launcher_monochrome` layer for themed icons; every mark within the 66dp safe circle. No wallpaper color.
- Roborazzi goldens: `app/src/test/screenshots/launcher_icon.png` and `feature/game/src/test/screenshots/game_*.png`
  (placement, battle dark, 12×12 salvo, landscape, tablet phone view, pt-BR). Re-record with
  `./gradlew recordRoborazziDebug` and look at the images before committing.

## Game identity

*(code)* = read from the code; *(user)* = the user's decision, 2026-10-09.

- **Visual metaphor:** to be defined: a prototype comes before the decision *(user)*. Today: a flat sea grid seen
  from above, A–L / 1–12 labels *(code)*. Mood: tense naval combat, keeping Material 3 Expressive's elements *(user)*.
  The metaphor is decided before the first release *(user)*.
- **Game tokens:** `BoardColors` (`board/BoardColors.kt`) in `LocalBoardColors`, built from
  `MaterialTheme.colorScheme` by `ProvideBoardColors` (around the board in `GameScreen`; tests wrap it too): sea
  `surfaceContainerHigh`, line `outlineVariant`, label `onSurfaceVariant`, ship `secondary`, hit `error`, hitOnShip
  `onSecondary`, miss `outline`, mark `primary`, hint `tertiary`. Shapes are fractions of a cell (`SeaGrid.kt`:
  `CORNER`, `SHIP_INSET` (also the dock's), `*_RADIUS`, `STROKE`, `SPOKES`); labels scale to the label band
  (`LABEL_SIZE`), no own typeface. Cover: `primaryContainer` *(code)*.
- **Custom components:** `board/`: `SeaGrid` (one Canvas + TalkBack cell nodes), `BattleBoard` (big target, mini
  own; `battleLayout` in `Geometry.kt`), `PlacementBoard` (dock, drag, rotate, TalkBack actions). `ui/GameScreen.kt`:
  `Cover`, `EndDialog` (wraps `WinDialog`). `ui/HowToPlayScreen.kt` *(code)*.
- **Motion and feedback:** the last shot springs in (`motionScheme.fastSpatialSpec`); a dragged ship lifts ×1.06;
  pauses as in Game screen; end dialog after 1.2 s, random `WinCelebration`. Game sounds `miss` / `hit` / `sunk`
  (`GameViewModel.SOUND_*`, `app/di/GameModule`) with the kit's haptics MOVE / MOVE / WIN; kit sounds: `REJECTED`
  (refused tap or drop), `WIN` (a celebrated win). Game sounds are `app/src/main/res/raw/sfx_*.wav`, made by
  `python3 tools/sfx.py app/src/main/res/raw` (synthesized, no third-party audio), played by name through
  `AndroidGameFeedback(sounds = …)` *(code)*.
- **vinkit extension points used:** `SettingsScreen(gameSections)`: Game (Board, Firing `Choice`, Opponent
  `IconChoice`). `ScoresScreen`: `groupName` (sizes + Daily), `modeName`, `details` (hints), `points` (shots).
  `WinDialog(lines, title, kind)`: mode, shots, accuracy, time, hints, new badges.
  `GameFrame(navigation)`: Badges. `GameToolbar` + `MenuOption`, `AndroidGameFeedback(sounds)`,
  `BadgesScreen(battleGridBadges())`, `GameSurface(gameReport)` *(code)*.
- **Game achievements:** 9 `Achievement`s, texts `badge_*` / `badge_*_note`: First win, All waters, Every way to
  fire, Admiral, On your own, Clean sink, On a roll, Every day, Sharpshooter. No own icons; the kit's `Badge` has
  none *(code)*.
- **Do not:**
  - use colors outside `colorScheme` / `BoardColors`; never hardcode one *(user)*;
  - change the shot glyphs: miss = ring, hit = burst, salvo mark = crosshair, hint = the same frame on all five
    *(user)*;
  - build own Settings, Scores, Badges, toolbar, new-game menu or end dialog: extend vinkit's *(code)*;
  - draw anything that tells hint cells apart or shows unsunk enemy ships, or swap the grids (Game screen) *(code)*.
- **Open to experimentation:** shot animations, ship and sea drawing, sound timbre (made as in Motion and
  feedback), the Cover and How to play look *(user)*.

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per
milestone (`PLAN.md`). Commit, push and open PRs only when the user asks.
