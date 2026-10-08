I want you to plan and build an Android game with me, on **vinkit**, my shared game kit.

**Question policy.**
- **Game design: ask me everything.** Before any plan, interview me about every topic in the game-design checklist (section 1a), even the ones that look obvious. A value I filled in is your recommended answer, but still confirm it. Don't decide a rule, a level or a number on your own.
- **Project setup: don't ask.** Section 1b and sections 2–8 are already decided. Put those defaults in the plan as a short list I can veto. Ask only about rows marked `ASK`, conflicts between the game and the standard, and anything vinkit lacks that the game needs (a kit gap, section 2).
- **How to ask:** in rounds of at most four questions, in the checklist's order, each with 2–4 concrete options, a recommended one first and the trade-offs of each. Show a small board or example when it helps me pick. When my answer opens a new question (a new mode, a new piece, an edge case), ask it in the next round. Stop when every checklist topic has an answer, then write a summary of the game's rules and get my "yes" on it.

Present the plan and wait for my approval before starting the scaffold.

## 1. Game parameters

Section 1a is the interview about the game itself; section 1b is the project setup, already decided. The Slug is `battlegrid`.

### 1a. Game design (ask me about every row)

Fill in what you already know; leave the rest empty. Claude asks about every row either way.

| Topic | What to ask | Value |
|---|---|---|
| Game | which game, and the version of its rules (there are often several) | Battleship (classic: hidden fleets on a grid, players take turns firing at cells) |
| Name | app name, Slug (short lowercase id: package, repo, folder, emails, privacy URL and upload key derive from it), brand color (one of vinkit's `ThemeColor`s not taken: Solo green, Sudoku Trio blue, OX Play purple) | App name **BattleGrid**, Slug `battlegrid`, brand color **TEAL** |
| Setup | the board or table, its size, the pieces or cards, what's on it at the start, who or what moves first | square boards 8×8, 10×10, 12×12 (size is a variant); each side has its own hidden grid with a fleet of straight ships, horizontal or vertical; ships may touch (side or diagonal), never overlap. Fleets scale with the board (ship lengths): 8×8 → 5, 4, 3, 2 (14 cells); 10×10 → 5, 4, 3, 3, 2 (classic, 17 cells); 12×12 → 5, 4, 4, 3, 3, 2 (21 cells). Ship names, common in both languages: Carrier / Porta-aviões (5), Battleship / Encouraçado (4), Frigate / Fragata (4, 12×12 only), Cruiser / Cruzador (3), Submarine / Submarino (3, not on 8×8), Destroyer / Destróier (2). Placement phase before the battle: drag ships onto the grid, tap a ship to rotate (one that would leave the grid shifts back inside), a Random button fills a valid layout, Start when all are placed; the AI's fleet is placed from the seed. First shot: random in the very first game, then the winner of the previous game fires first |
| Core rules | legal moves, turn order, what happens after each move (captures, flips, chain reactions, forced moves), special rules, every edge case I may not have thought of | fire at an untried cell of the enemy grid; result hit / miss, "sunk" announced with the ship's name. Firing modes, all in the first release: **Classic** (one shot per turn), **Hit-again** (Classic, but a hit fires again), **Salvo** (shots per turn = own ships afloat, mark them all then Fire; each shot shows its own hit / miss) Hit-again and Salvo apply to both sides (the AI fires again on a hit; in Salvo its shot count is its ships afloat) |
| Win / loss / draw | each end condition, whether a game can end stuck, give-up / resign | the first side to sink every enemy ship wins; no draw and no stuck state. In Salvo, the salvo resolves shot by shot (each result shown 600 ms apart, for both sides) and the game ends the moment the last ship sinks. Resign in the new-game menu counts as a loss. Leaving during fleet placement (before the first shot) is not a loss: New game or a Settings change there just discards it, without a confirmation |
| Variants and modes | rule variants, board sizes, which can be combined, which are in the first release and which later || two independent choices, all combinations in the first release: board size (8×8, 10×10, 12×12) × firing mode (Classic, Hit-again, Salvo) = 9 variants |
| Difficulty levels | how many, their names, **what changes per level** (puzzle generation, AI strength, time limit, hints, fewer givens…), how a level is measured and tested so it really feels harder || AI only: **Easy** (random untried cells), **Medium** (hunt / target: random until a hit, then finishes that ship), **Hard** (probability map of where the remaining ships still fit + checkerboard parity while hunting). Measured with a seeded benchmark: average shots to sink a fleet must order Hard < Medium < Easy on every size |
| Opponent | none, AI, local 2-player; AI levels and how each plays; who starts, and whether it alternates; the AI's pause before answering. vinkit covers turn-based play only: real-time needs a kit change || AI (Easy / Medium / Hard) and local pass-and-play 2-player (a "pass the phone" cover screen before each turn and before each player's placement, so neither sees the other's fleet; not recorded in stats). First shot: random in the very first game, then the previous game's winner. The AI waits ~600 ms before each shot; in Salvo its shots land one by one at that pace. Pass-and-play: after each turn's result shows for ~1 s, the screen is fully covered with "Player 2, tap to start" (names fixed: Player 1 / Player 2) |
| Board generation | random, seeded, unique solution, fixed puzzle packs; a guaranteed-solvable deal or not; "restart the same board" || seeded random (`Random(seed)`): the AI's fleet, the Random placement button and the AI's shots all come from the seed, so a bug report replays the same game. "Restart the same board" keeps the AI's fleet and asks the player to place theirs again |
| Scoring and ranking | points, time, moves, mistakes, hints used; what a ranking sorts by; per mode or per level; or stats only (played, won, lost, drawn, streaks) || vs AI: score = shots fired to win (lower is better), plus a penalty per hint; ranking per size × firing mode × difficulty, sorted by fewest shots, ties by faster time; stats played / won / lost / streaks per mode; the win dialog shows shots, accuracy %, time and hints used. Pass-and-play games aren't recorded |
| Helps and limits | hints (what one shows, a cost), undo / redo (a penalty, how far back, undoing the AI's reply), mistake limit, auto-complete, stuck detection, notes or a flag mode || no undo / redo during the battle (undoing a shot would leak where ships are); during placement the player can move ships freely. Hint: highlights 5 untried enemy cells, exactly 1 of which is part of an unsunk ship. At most 3 hints per game; when fewer than 4 untried water cells remain, the hint shows what's left (still exactly 1 ship cell). Each hint adds **+5 shots** to the score; hints used are listed in the win dialog, and a hinted win still enters the ranking with that worse score. Hints are off in pass-and-play. No stuck detection (a game can't stall) |
| Timer | shown or not, counts up or down, time limits per mode || count-up, shown, no limit; runs during the battle only (not during placement), paused while the app is in background |
| Input | tap, drag, long-press, a number pad, select-then-place, confirm moves or not || placement: drag a ship onto the grid, tap a placed ship to rotate it, Random and Start buttons. Battle: Classic and Hit-again fire on tap of an untried enemy cell (no confirmation); Salvo: a tap marks or unmarks a cell, and a Fire button in the toolbar enables once N cells are marked |
| Look | how the board and pieces are drawn (style, symbols, shapes), animations of moves and of the end, what's highlighted (last move, legal moves, conflicts) || clean flat sea: theme-colored grid with A–L / 1–12 labels, ships as rounded capsules, hit = a burst mark, miss = a small ring, a sunk ship outlined and dimmed (revealed on the enemy grid). Battle layout: portrait shows the enemy grid at full width and the player's fleet as a mini grid above it (with the AI's hits); tapping the mini grid swaps the two (the player's fleet grows to full width and the enemy grid shrinks into the mini slot; tap it again to swap back), animated with the spatial spring, and firing works only while the enemy grid is big; during the AI's turn the player's grid swaps to big automatically so its shots are seen landing, then swaps back on the player's turn; in pass-and-play, after the cover lifts, the shooter's own grid shows big for ~1.5 s (where they were hit), then the target grid; landscape and tablets show both grids side by side at the same size, with no swap; cells take the room there is, no zoom (a 411dp-wide portrait phone gives about 42dp cells on 8×8, 34dp on 10×10, 29dp on 12×12). Motion: a hit's burst springs in, a miss ripples, a sunk ship's outline reveals, placed ships snap with the spatial spring; the last shot of each side is highlighted; vinkit's celebration on a win |
| Settings → Game | which of the above the player can change in Settings, and what a change does to a game in progress || Board size (`Choice`: 8×8 / 10×10 / 12×12), Firing mode (`Choice`: Classic / Hit-again / Salvo), Opponent (`IconChoice`: Easy / Medium / Hard / 2 players). Any change starts a new game and asks first while a game is in progress: a vs-AI game counts as a loss, a 2-player game is just lost |
| Anything else | tutorial or "how to play" screen, daily puzzle, sounds specific to the game, achievements: ask whether I want any, default none || all in the first release: a How-to-play screen (short illustrated rules per firing mode, from the menu, shown once on first launch); game sounds through vinkit's `GameFeedback` (splash on a miss, boom on a hit, sinking on a sunk ship, vibration on hit and sunk); a **daily challenge**: 10×10, Classic, vs Hard, seed from the UTC date so everyone faces the same AI fleet and the same AI random stream that day; the player places their own fleet; one ranked attempt per day under its own mode key `DAILY` (best and streak stats), replays allowed but unranked; **local achievements**, shown on a Scores tab: first win, a win on each size, a win in each firing mode, beat Hard, win without hints, sink a ship with no miss between its hits, 5-win streak, 7-day daily streak, win with ≥ 60% accuracy. Daily challenge and achievements get their own milestone after the core game, before release prep |

### 1b. Project setup (defaults; change only what differs)

Fill the Value column only to override a default.

| Key | Value | Default when left as is |
|---|---|---|
| Package / applicationId | | `io.github.vinaooo.battlegrid`, all lowercase: Kotlin packages are lowercase (ktlint rejects uppercase), and a capitalized applicationId (as Sudoku Trio's) differs in case from the package, so `adb shell am start` has to name both. Permanent once published |
| Repo | | `github.com/vinaooo/battlegrid`, public, MIT (as Solo, Sudoku Trio and OX Play); folder `~/Projects/battlegrid` |
| Persistence | | scores and stats in vinkit's scores database under each mode's stable `key`; auto-save of the game in progress with undo history |
| Languages | | pt-BR + en |
| Orientation / devices | | portrait + landscape, phones and tablets (phone view on tablets) |
| Monetization | | one AdMob banner on the game screen only, placeholder first (vinkit `PlaceholderAdBanner`), real ads + consent later |
| Online services | | none; no Play Games |
| Bug report | | `ReportTarget("vrpedrinho+battlegrid@gmail.com", "vinaooo/battlegrid")`: an email to that address, or a prefilled issue on the game's repo |
| Privacy policy | | `https://vinaooo.github.io/battlegrid/privacy.html` (`#en`, `#pt-br`), contact email `vrpedrinho+battlegrid@gmail.com` |
| Audience | | 13 and older: not in Google Play's Families program, and the listing doesn't market to children |
| Upload key | | `/home/vina/keys/battlegrid-upload.jks`, alias `battlegrid-upload`; I create it and type the passwords myself |
| Publishing | | release-ready but not on Play yet: `release.yml` stays off, no Play Console app or service account; the first upload is by hand, when I decide |

## 2. Working agreement

- **Decisions:** follow the question policy above. When something is open, ask in short rounds, each question with a recommended option and its trade-offs. When you're unsure whether an API or a version exists, check first; correct yourself openly if an earlier claim was wrong.
- **Fixed values stay fixed:** once I set an exact value (a size, a gap, a duration), a later tweak to a neighbor must not change it. Move something else, or ask which element gives way.
- **vinkit first:** `~/Projects/vinkit` (github.com/vinaooo/vinkit; its `README.md` and `CHANGELOG.md` list every module and API) holds the build logic, the version catalog and everything games share (section 3). Use it; don't rewrite in the game what the kit already has.
  - The game always builds against the **newest published vinkit tag** from JitPack (`vinkit.tag` in `gradle.properties`). Never a local `includeBuild("../vinkit")`, `mavenLocal` or a `vinkit.local` flag.
  - **Kit gaps:** when the game needs something every game could use (a shell slot, a settings row, a stats field), or finds a bug in the kit, fix it in vinkit, not in the game: change vinkit with its tests, add a `CHANGELOG.md` entry (marked **breaking** when games must change code), commit, tag, push, fetch `https://jitpack.io/com/github/vinaooo/vinkit/<tag>/build.log` to start the JitPack build, wait for it, then bump `vinkit.tag` here. Never bump the other games (`../paciencia`, `../sudoku`, `../xo`): I update them myself. List the gaps in the plan; the game-only parts stay in the game.
- **Reference game:** OX Play (`../xo`) is the newest game on vinkit and the shape to copy: `settings.gradle.kts` (JitPack, plugin resolution, the vinkit catalog), `gradle.properties`, `build.gradle.kts` (root coverage), `.editorconfig`, `.github/workflows`, `:app` (`MainActivity`, the NavHost, `di/AdsModule`, `di/GameModule`, `FakeAdsModule` in `src/sharedTest`), `:data`'s `DataModule`, the Scores and Settings routes, `BugReport.kt` and `DebugGameActivity`. Solo (`../paciencia`) and Sudoku Trio (`../sudoku`) are on vinkit too, with drag-and-drop, generators and timers. Every `CLAUDE.md` lists the pitfalls behind its choices.
- **Plan first:** write a `PLAN.md` (context, confirmed decisions, the vetoable defaults, vinkit gaps found, module graph, domain design, test strategy, milestones, Play Store checklist, verification) and get my approval before building.
- **Milestones in TDD order**, one PR each: scaffold (vinkit plugins and catalog, CI, wrapper, `CLAUDE.md`, an empty app on `VinkitTheme`) → domain core → domain extras (AI, generator, hints) → data → game screen → Settings, Scores, navigation, ads placeholder, bug report → adaptive layouts, localization, accessibility, screenshots, launcher icon → release prep → post-MVP.
- **Real device:** I have an Android device on adb. Install and exercise every interaction on it (tap, drag, undo, hints, win flow, rotation, themes, languages), and fix what the tests missed.
  - My phone is a Moto XT2125 on wireless adb; it often shows up twice, so check `adb devices -l` and pin `ANDROID_SERIAL`.
  - It has **no Google Play services**: no ads and no consent form there. Check ads and consent on the `Pixel_9a_Android_16` emulator (Play Store image). Its airplane mode may be left on from store screenshots: `cmd connectivity airplane-mode disable`.
  - Reuse the existing emulators: `Pixel_9a_Android_16` (ads, consent, store screenshots), `Pixel_Tablet_Android_16` (phone view, tablet layouts) and `Solo_API_26` (minSdk). Create one only if it's missing.
  - Once my phone has the release build signed with the upload key, a debug install fails on the signature, and uninstalling loses my saved game. Install release builds over it (`adb install -r`), and run instrumented tests on emulators. Ask before uninstalling anything on my device.
  - Never change my phone's system settings (dark mode, auto-rotate, font scale, …) to test: use the app's own settings, screenshots or an emulator, or ask me.
  - Don't tap "Email" in the bug report on my phone: backing out of the mail app's compose saves a draft in my account.
  - Taps from a script: `uiautomator dump` gives each TalkBack node's bounds.
  - **Animations:** check `adb shell settings get global animator_duration_scale` before judging timing. To check an animation, record it (`adb shell screenrecord`) and step through the frames (`ffmpeg … -fps_mode passthrough`).
  - **Performance:** judge smoothness and speed only on a release build compiled like a Play install (`adb shell cmd package compile -m speed-profile -f <package>`, then relaunch; measure with `dumpsys gfxinfo`, or a temporary `Log` line that is never committed). Debug and uncompiled builds are several times slower, so don't redesign an animation or an AI based on them.
- **Git:** commit, push and open PRs only when I ask. A single long-lived `master`: branch from it and open the PR against it; I merge with merge commits.
  - A milestone or feature gets its own PR. Wait for my merge, then branch the next one from the updated `master`; never stack a PR on an unmerged branch.
  - A run of small fixes or tweaks I request goes on one branch, one commit per fix (gate passing each time), and one PR when I say the batch is done.
- **CI:** don't watch PR runs unless I ask. When I do, watch in the background and play a sound when it ends (`paplay /usr/share/sounds/freedesktop/stereo/complete.oga` on success, `dialog-error.oga` on failure), then report the result.
- **Reporting:** don't report "done" until the full gate (section 7) passes. Report every decision you made on your own, and every place you changed behavior.
- **Project memory:** keep a `CLAUDE.md` up to date with commands, architecture and pitfalls as they're found, like OX Play's. Say there that the game is on vinkit and builds only against its newest tag.

## 3. Architecture (fixed)

**SOLID and Clean Code throughout:**
- small classes with one responsibility;
- extension through interfaces rather than edits to the engine;
- dependency inversion at the domain boundary;
- immutable state;
- named constants instead of magic numbers;
- no dead code (mutation testing finds it).

### Modules

```
:app ─► :feature:game, :data, vinkit settings / scores / shell / ads / designsystem
:feature:game ─► :domain, vinkit shell / designsystem / settings / scores / bugreport
:data ─► :domain, vinkit settings / scores
:domain   pure Kotlin/JVM: no Android, no DI annotations (vinkit core for AppSettings, GameStats, GameCodec)
```

- No `build-logic/`, no version catalog of the game's own, no `:core:*`, `:feature:scores` or `:feature:settings`: those are vinkit's.
  - Plugins: `vinkit.android.application`, `.android.library`, `.android.compose`, `.android.feature`, `.hilt`, `.jvm.library`, `.quality`, `.root.coverage`.
  - Versions: vinkit's catalog (`libs`). A newer or missing library goes into vinkit's catalog first (a kit release), not into the game.
  - Kit modules: `implementation("com.github.vinaooo.vinkit:<module>:${providers.gradleProperty("vinkit.tag").get()}")`.
- vinkit has no DI annotations. Hilt for DI in the game: `:app/di` builds the kit's objects (`AdsModule`, `GameModule` with `AndroidGameFeedback` as a `@Singleton`) and the use cases; `:data`'s `DataModule` provides the repositories, vinkit's `ScoresDatabase.create(context)` with `RoomScoreRepository` / `RoomStatsRepository`, and the brand color as `AppSettings`' default.
- Type-safe Navigation Compose routes (`@Serializable` objects): game, Scores, Settings.

### Domain design

This pattern generalizes to any turn-based or puzzle game:
- **State:** an immutable `GameState` (board, score, moves, elapsed time, status, **mode**), `@Serializable`.
- **Modes travel with the game:** the variant, difficulty and scoring mode are part of `GameState`, so a resumed or restarted game keeps its rules whatever Settings say now. Changing them in Settings starts a new game.
- **Mode keys:** each mode has a stable string `key` (e.g. `"FIVE_HARD"`) under which vinkit stores its scores and stats. Never rename one after release: the players' records are filed under it.
- **Moves:** a sealed `Move` hierarchy, one subtype per kind of move.
- **Rules:** behind a `RuleSet` interface, one small rule object per move type, composed by the game's rule set (a game with one move type skips the per-move objects). `GameEngine.apply(state, move)` is pure and returns a sealed `MoveOutcome` (`Applied(newState)` or `Rejected`). It also exposes `legalMoves(state)` and `isLegal`.
- **Scoring:** behind a `ScoringStrategy` interface fed by `ScoreEvent`s, picked per mode (`scoringFor(mode)`), so a new scoring mode is a new class. The strategy also says whether the score is clamped at zero. Each mode says how its ranking sorts (vinkit's `Ranking`: highest score, fastest win, …).
- **Time limits:** a timed mode has its limit in the mode. The engine stops the clock at the limit and rejects moves once time is up; a use case records the loss and deletes the save.
- **Generation:** behind interfaces, with a seeded implementation (`Random(seed)`), so boards are reproducible and "restart the same board" is just replaying the seed.
  - Retry budgets are counted (attempts), never timed, so a seed gives the same board on every device. Generation is cancellable.
  - Where generation searches (unique-solution puzzles, graded difficulty), add a `:domain:benchmarkGenerator` task reporting time, hit rate per difficulty and validity per mode, and tune the grading with it: a plausible metric can still miss its target level most of the time.
  - Time it on a release build on the device. If a mode takes more than about 0.5 s, prepare the next board in the background once a board shows, and let New game use it. While a board is generated, a loading state disables input and stops the clock.
- **AI** (when there is an opponent): behind one interface, one class per level, its random choices drawn from the game's seed (e.g. `Random(seed * 31 + moves)`), so a saved game or a bug report replays the same AI moves. Never change that formula after release. A search that is too slow on a phone gets a depth limit and an evaluation, and an obvious opening move skips the search.
- **History:** an `UndoHistory` with undo and redo stacks of snapshots with score deltas, `@Serializable` so it survives process death. It defines explicitly what undo restores (for example, the clock and move count keep running), any undo penalty, and what a redo earns back. A new move clears the redo stack. Against an AI, undo and redo stop only on the player's turn.
- **Session:** `GameSession(seed, state, history)` is the unit that is played, undone, ticked and saved. Its `codec` is vinkit's `GameCodec`, for bug reports.
- **Helpers:** an input resolver (tap/drop → `Move`, plus the legal destinations of a piece for TalkBack), a `HintEngine` that ranks legal moves and filters out useless ones, and an auto-solver or auto-complete where the game has one.
- **Stuck detection** where a game can dead-end: a cancellable `suspend` search over reachable positions with a position budget. Past the budget it answers "not stuck", because only a real dead end exhausts the search.
- **Repository interfaces:** `SavedGameRepository`, a settings repository for the game's own keys, `SeedSource` and `Clock`, plus vinkit's `ScoreRepository`, `StatsRepository` and `AppSettingsRepository`, so tests never depend on real randomness or real time. Fakes live in `:domain`'s `testFixtures`.
- **Use cases:** they own the cross-cutting rules, for example "starting a new game counts an unfinished one as a loss" and "finishing records the score and stats and clears the save".

### Data

- Scores and stats live in vinkit's `ScoresDatabase` (`vinkit_scores.db`); the kit owns its schema and migrations. The game adds no Room database unless it has data of its own; if it does, every schema change is an `AutoMigration` (or a manual one) with a test from the exported schema, and **never** a destructive fallback.
- The game in progress goes into a file: kotlinx.serialization JSON in a versioned envelope, written atomically (temp file + rename). A corrupt, invalid or unknown-version file is discarded, not crashed on.
- Settings: vinkit's `DataStoreAppSettingsRepository(dataStore, AppSettings(themeColor = <brand>))` and the game's own keys in the same Preferences DataStore, each repository touching only its own keys.
- Mappers live apart from the entities.

### Presentation

- **Game ViewModel (unidirectional data flow):** it exposes one `StateFlow<GameUiState>` and receives a sealed `GameIntent`; the UI never mutates state.
  - It saves after every move, undo and pause.
  - The clock is vinkit's `Ticker` (start/stop), started on resume and stopped on pause (`LifecycleResumeEffect`). It only ticks while the game is in progress, and freezes during auto-play.
  - Timed loops (auto-play, animations, the AI's reply) must **read the latest state on every step**. A local copy overwrites whatever happened between steps: clock ticks, taps, undo.
  - Background searches (AI, stuck detection, hints) run on an injected dispatcher; a new move or an undo cancels the previous search.
- **Feedback:** vinkit's `GameFeedback` (`AndroidGameFeedback`: SoundPool and predefined `VibrationEffect`s). The ViewModel decides *when* to play them (and whether the settings allow it).
- **Board:** geometry is a pure class, testable without Compose, that maps each piece to absolute positions and does hit-testing and drop targets. Pieces are keyed by identity and animate to their positions with the motion scheme's spatial spring.
- **Drag-and-drop:** `detectDragGestures` leaves the touch slop out of the first drag amount. Add it back along the drag direction, or the piece trails the finger and drops land short.
- **Ads:** vinkit `ads` (`AdMobBanner` in its `BannerSlot`, `DefaultAdConsent`: UMP consent before the SDK starts, sizes and the reserved slot height already handled), wired in `:app/di/AdsModule` with IDs from `BuildConfig` (the README's snippet). Host the banner only where section 1b says. For the game screen only, the game route is a `Column`: the game takes `weight(1f)` and consumes the bottom navigation-bar inset, and the banner below it pads for that inset. `MainActivity` gathers consent once per launch.
- **Debug entry point:** a debug-only `DebugGameActivity` that starts a chosen position from `adb shell am start -S -n <package>/.debug.DebugGameActivity --es game <preset>`: one move from winning, one move from stuck or losing, the largest possible board, a bug report's "State:" block (`--es state <code>`), or a report's attached JSON (`--es load game.json`). Add a preset whenever a feature needs a hard-to-reach position.

## 4. Standard UI (fixed)

Every game gets this look and these features, and vinkit already builds them: the game fills in its board, texts and options. Ask only where the game makes one of them meaningless (e.g. no stuck detection in tic-tac-toe). A feature vinkit lacks or gets wrong is a kit gap (section 2).

| Feature | vinkit | The game supplies |
|---|---|---|
| Theme | `VinkitTheme` (Material 3 Expressive, dynamic color or one of 8 `ThemeColor` palettes) | the brand color; its own colors (board, pieces) in a `CompositionLocal` built from `MaterialTheme.colorScheme` |
| Game screen | `GameSurface` (screenshot, bug report, announcer), `GameFrame` (portrait / landscape / phone view, hand, board position), `ModeAndTime` | the board, the info text, controls under the board (a pad, if any) |
| Toolbar and menu | `GameToolbar(actions, menuOptions)`, `ToolbarTip` | undo, redo, hint, input-mode toggles, contextual buttons, extra menu entries |
| End of game | `WinDialog(lines)` with a random celebration | the lines (score, time, result); no celebration for a loss or a draw |
| Settings | `SettingsScreen(gameSections = …)`, rows `Choice`, `IconChoice`, `ToggleRow`, `LinkRow`, `NewGameConfirmDialog(text)` | the Game section, the privacy policy URL, the privacy-options flag and lambdas from `:app` |
| Scores | `ScoresScreen(ranked, note)`, an open `ScoresViewModel` (`groupOf`: modes grouped into tabs) | a `@HiltViewModel` subclass with the game's modes, keys and rankings |
| Bug report | `BugReportDialog`, `ReportTarget`; the kit's manifest brings the `${applicationId}.reports` file provider (don't declare another) | `GameReport` text (a settings line, a game line, the `GameCodec` "State:" block) and `game.json` |

### Visual style

- **Material 3 Expressive** everywhere: rounded icons (`Icons.Rounded`), shapes and colors from the theme only, motion from the motion scheme (spatial specs for movement, effects specs for fades). No hard-coded colors.
- **Bounce:** a choice that changes squashes to 0.85 and springs back (damping 0.4, `StiffnessMediumLow`); new text slides in from below on the same spring. Only a change bounces, never the first showing.
- **Pauses in animations** use `animate()`, not `delay()`, so the device's animation scale speeds or slows them too.
- **Pieces** are drawn in Compose (no bitmaps). Piece colors come from scheme roles, with contrast ≥ 3:1 against the board tested in every palette and both themes. Blend in RGB, because Compose `lerp` works in Oklab and shifts the hue. Keep grey accents grey.
- Symbol glyphs (♠ ♥ ✓ …) need U+FE0E, or some fonts render them as grey emoji.

### Game screen

- **Portrait:** stats (score, moves, time, merged for TalkBack) and the Scores/Settings icon buttons on top, the board, and the floating toolbar at the bottom. **Landscape:** stats and navigation in a left column, the board centered at full height, the toolbar on the right (`GameFrame` does both).
- **Toolbar:** undo, redo, hint, and the new-game button. An input mode the game has (notes, flag mode) is a toggle in the toolbar (filled icon while on), not a key on the board's controls. A contextual button (auto-complete, …) grows into the toolbar when it applies.
- **New-game menu:** new game, restart this board, report a bug, and the game's own entries.
- **Tips:** a `ToolbarTip` pointing out a button the first time it matters (e.g. auto-complete).
- **Messages:** a stuck game says so in the middle of the screen, with new game and restart as actions. A time's-up dialog offers a new game or the same board again.
- **Bug report:** never embed a token in the app: it can be extracted.

### Settings screen

- **Game section:** the game's variants/difficulty/scoring modes. 2–3 options: `Choice` (segmented); 4+ options: `IconChoice` with a name and a note per option. A change that starts a new game asks first if a game is in progress (`NewGameConfirmDialog`, `pendingChange` in the ViewModel): "This will start a new game. The current game counts as a loss", or that it will be lost when the game isn't recorded.
- The rest (Appearance, Sounds and vibration, Privacy) comes from vinkit. The privacy policy URL is a string resource per language (`#en`, `#pt-br`); Google Play requires the link in the app.

### Theme and system

- Status and navigation bar icons follow the in-app theme choice: `enableEdgeToEdge(SystemBarStyle.auto(…) { darkTheme })`.
- Add a dark `values-night` window theme. Android 16's "make more apps dark" setting inverts any app with a light window theme at night and ignores `forceDarkAllowed`.
- Declare `android:appCategory="game"`, with a test on `applicationInfo.category`. On large screens, Android 16 ignores orientation and resizability restrictions for apps that aren't games.
- Edge-to-edge everywhere. Every user-facing string in `values/` plus each language folder of its module (vinkit's strings are prefixed `vinkit_`; declare the same name to override one); every piece has a spoken content description.
- **Zero counts** get their own string ("No mistakes" / "Nenhum erro"): Portuguese plural rules put 0 in `one`, so a plural prints "0 erro".
- **TalkBack:**
  - Announce moves through `GameSurface`'s announcer, never `View.announceForAccessibility` (deprecated at API 36).
  - **Grid games** drawn on one `Canvas`: lay one invisible node per cell over it, in reading order, saying position, content and state ("row 3, column 5, 7, given"), selectable, with a click action only where a move is possible.
  - Offer custom actions for each legal destination, hide covered or face-down pieces and count them on the visible one, read the board in the order the hand setting lays it out, merge stats into one node (`clearAndSetSemantics`), and mark section titles as headings.
- **Launcher icon:** an adaptive vector foreground on the brand color plus a **separate monochrome layer** with the marks cut out (boolean ops, e.g. skia-pathops). Reusing the foreground makes the themed icon a blank shape. Keep every stroke end inside the 66dp safe circle. Turn font glyphs into paths; Roboto is Apache 2.0.
  - Optional wallpaper-colored icon: `@android:color/system_accent1_600` in `values-v31`. Launcher3 and its forks cache the icon until the app updates, so wallpaper changes show late and the launch animation flashes the new color. Ask me before choosing it.
  - Keep a screenshot test of the adaptive and themed icons.

### Compose pitfalls already hit

- **Layering moving pieces:** lift only a piece that arrives in a new pile or cell; one shifting within its own keeps its layer, or it covers newer pieces. A piece counts as flying from the frame its target changes, not from when its animation starts. Never change a piece's layer as it settles: it makes faces blink. A piece being revealed snaps into place under the one leaving it and is never lifted.
- **State inside `BoxWithConstraints`** can be reset when Compose rebuilds its content as pieces move. Keep animation state outside it.
- **Ripples:** draw them from the piece's own `InteractionSource`, inside its clip, so they follow its rounded corners.
- **Trailing lambdas:** adding a new last lambda parameter to a composable silently rebinds existing callers' trailing lambdas to it. Pass callbacks by name.
- **Recomposition:** a map or list read by every piece recomposes the whole board on each frame. Watch for it when an animation stutters on a release build. Background texture (grain, paper) goes in a few batched `drawPoints` calls, cheap to redraw during animations.

## 5. Test-driven development (fixed)

- **TDD:** red → green → refactor, in small steps. Every new class or method gets tests; **every bug gets a regression test that fails before the fix** (confirm it fails, then fix); every behavior change updates its tests.
- Prefer hand-written fakes to mocks. Repository fakes live in `:domain`'s `testFixtures` and are shared with the feature modules.

| Layer | Tools | What to cover |
|---|---|---|
| `:domain` | JUnit 5, Kotest assertions + **property tests** (`checkAll(iterations, …)`) | every rule and scoring row per mode; invariants: no piece lost or duplicated, any sequence of legal moves keeps the state valid, undo restores the previous state, redo replays it, the same seed gives the same board, `legalMoves` agrees with `isLegal`; serialization and codec round trips; AI strength (never loses where it should be perfect, beats the easy level) |
| `:domain` | **Pitest** | ≥ 80% mutants killed, ≥ 90% line coverage; exclude generated `$$serializer` classes and slow whole-game tests (pin the same code with exact checks instead) |
| `:data` | Robolectric, Turbine | repositories, save/restore round trips, corrupt files, the game's keys beside vinkit's in one DataStore |
| ViewModels | JUnit 5, Turbine, coroutines-test (`StandardTestDispatcher`) | intent → state flows, persistence, feedback, win/loss/draw/time's-up flows, the AI's reply and its cancellation, clock behavior during long operations, settings changes that ask for confirmation |
| UI | Compose UI tests (Robolectric) | every button's intent, tap, **drag** (inject stepped `down`/`moveBy`/`up`, not one big jump), dialogs, messages, menus, loading, both orientations, the TalkBack nodes |
| App | Hilt + Robolectric (`HiltTestApplication`, `createAndroidComposeRule<MainActivity>()`) | the banner is exactly where section 1b puts it (present and absent per screen); consent is gathered once at launch. Swap the real ads module for fakes with `@TestInstallIn`, kept in a `src/sharedTest` dir added to both `test` and `androidTest` |
| On-device | Hilt instrumented tests on emulators (a custom `HiltTestRunner`), **Android Test Orchestrator with `clearPackageData`** | the main flow on the minSdk and the latest API: fresh start, a move and undo, activity recreation, navigation. Local only, not in CI |
| Visual | **Roborazzi** (native graphics) | light/dark, every mode and variant, portrait/landscape/tablet/phone view, every language, the launcher icon; dynamic color off (brand color) |
| Pure geometry | JUnit 5 | layout, hit-testing, drop targets, centering, the largest possible board fitting |

**Test pitfalls already hit:**
- **Never time things in tests.** CI runners are slower than a phone: a 300 ms AI limit failed every OX Play CI run for five milestones. Cap the work instead (positions searched, attempts), and measure speed on the device.
- **Endless loops hang tests.** `runTest` waits for an endless loop (such as the clock) forever. Wrap ViewModel tests in a helper that pauses every ViewModel in a **`finally`**, or a failing assertion hangs instead of failing.
- **Screen size.** Robolectric's default screen is 320dp wide, so size test layouts to fit it, or set `@Config(qualifiers = …)`. Qualifier order matters: `w891dp-h411dp-land-xhdpi`, not `…-xhdpi-land`.
- **Snackbars.** A snackbar's follow-up action only happens after it's dismissed, so advance `mainClock` past its duration.
- **Machine-dependent values.** Keep dates, time zones and other machine-dependent values out of screenshots. Use noon-UTC timestamps, and hold the clock so a delayed dialog isn't up yet.
- **Rendering platform.** Record and verify goldens on Linux; native graphics render differently on macOS and Windows.
- **One process, many tests.** Instrumented tests share a process, so a second Hilt graph opens a second DataStore on the same file and crashes. The orchestrator with `clearPackageData` gives each test its own process and clean data.
- **Merged semantics.** After `clearAndSetSemantics`, find nodes by their content description (e.g. "Score, 0"), not by their text.
- **SDKs in tests.** Never let the real ads or consent SDK run under Robolectric or in instrumented tests. Fake them through Hilt.
- **Pitest and coroutines.** Suspend functions leave coroutine bookkeeping mutants nothing can kill; `avoidCallsTo kotlin.ResultKt` drops the `throwOnFailure` ones. Prefer non-suspend logic in the domain where it reads as well.

## 6. Quality gates and tooling (fixed)

- **Static analysis:** `vinkit.quality` brings detekt (+ compose rules, **no baseline**, `maxIssues: 0`), ktlint and Android lint; `allWarningsAsErrors = true` everywhere. The game may add overrides in its own `config/detekt/detekt.yml`, never a baseline.
  - Experimental APIs get a local `@OptIn`.
  - When detekt's function-count limit is hit, split by responsibility (e.g. `GameScreen` / `GameControls`); don't suppress it.
- **Coverage:** Kover with vinkit's filters (composables, generated code, `*Activity`, `*Application`, Hilt, `di`); the root `build.gradle.kts` applies `vinkit.root.coverage` and lists every module with `kover(project(…))`.
  - Floors: ≥ 70% overall (UI excluded), ≥ 90% on `:domain`. Raise a floor when coverage grows; never lower one to get a change through.
- **Screenshots:** `roborazzi.test.verify=true` in `gradle.properties`, so every test run compares the screenshots; re-record them with `recordRoborazziDebug`, look at the new images, and commit them.
- **CI (GitHub Actions),** copied from OX Play (`ci.yml`, `mutation.yml`, `release.yml`):
  - runs on every PR and on pushes to `master`, with `concurrency: cancel-in-progress`;
  - JDK 21, the Android platform the catalog's compileSdk needs, `gradle/actions/setup-gradle`;
  - writes CI-only Gradle settings to `~/.gradle/gradle.properties` (parallel builds, more heap and Metaspace);
  - steps: ktlint + detekt + lint, then tests + coverage (no separate screenshot run); uploads reports on failure;
  - Pitest runs nightly in its own workflow;
  - every workflow checks out with `fetch-depth: 0`, because configuring `:app` reads the version from git (section 7);
  - lint workflows with `actionlint` (download its release binary). It catches things like the `runner` context in job-level `env`. OX Play's CI doesn't have it yet: add it.
- **Local lint flake:** after a branch switch, `:app` lint can crash inside lint (an LLFir exception) when it runs in the same build as the other gate tasks. Delete `app/build/intermediates/*lint*` and run `:app:lintDebug` alone first. CI doesn't hit it.
- **Gradle wrapper:** the same Gradle version as vinkit. Generate it with `./gradlew wrapper --gradle-version X --gradle-distribution-sha256-sum <published sha>`; never copy the jar from another project (CI's wrapper validation rejects unofficial jars).

## 7. Build setup and definition of done (fixed)

- **Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, Hilt, DataStore, kotlinx.serialization, Navigation Compose, and vinkit; versions and SDKs (compileSdk, targetSdk, minSdk 26) come from vinkit's catalog. AGP 9+ has Kotlin built in (no `kotlin-android` plugin).
- **Low RAM:** my machine runs other heavy jobs, so keep OX Play's `gradle.properties` (`-Xmx2g`, a 1 GB Kotlin daemon, `org.gradle.parallel=false`, configuration cache on) and don't raise it. Run the gate in pieces: all of it in one invocation runs out of Metaspace. CI raises these for itself.
- **Pinned test dependencies:** AGP pins test classpaths to the app's own dependency versions. When an androidTest library needs a newer transitive version, add a dependency **constraint** instead of dropping the library. For example, the ads SDK's Guava pins `error_prone_annotations` 2.11.0, below what Espresso needs.
- **Configuration cache is on:** read `local.properties` with `providers.fileContents`, env vars with `providers.environmentVariable`, and git with `providers.exec`.
- **What `vinkit.android.application` resolves** (tested in the kit): the version (`versionCode` = `git rev-list --count HEAD`, `versionName` = the latest `vX.Y.Z` tag; releasing is pushing a tag), the upload key and the AdMob IDs (Google's test IDs in debug builds and until real ones are set; partial or swapped IDs fail the build). They come from `local.properties` (`vinkit.signing.*`, `vinkit.ads.appId`, `vinkit.ads.bannerId`, `vinkit.ads.testDeviceIds`) or CI (`VINKIT_SIGNING_*`, `VINKIT_UPLOAD_KEYSTORE_BASE64`, `VINKIT_ADS_*`). The keystore and passwords are never committed.
- **Release:**
  - R8 + resource shrinking, an adaptive and themed icon, and `localeFilters` for the shipped languages. Hilt, Room and kotlinx.serialization bring their own keep rules, so `proguard-rules.pro` starts empty.
  - Check the R8 build on the device: sign it with the debug key (`zipalign -p 4`, then `apksigner` with `~/.android/debug.keystore`) so it installs over the debug build with its data, and check that saves, settings and scores survive both ways.
  - Real ad IDs in `local.properties` make every local release build serve real ads: install it only on devices in `testDeviceIds`, and never tap its ads.
- **Play upload workflow** (`release.yml`, off until the repository variable `PLAY_UPLOAD_ENABLED` is `true`): builds `bundleRelease` on a `vX.Y.Z` tag (internal track) or by hand (any track; `draft` while the app was never published), uploads it with the R8 mapping file, and fails before building if any signing, ads or `PLAY_SERVICE_ACCOUNT_JSON` secret is missing.
- **README:** what the game is, that it's built on vinkit (`vinkit.tag`), how to build it, the release setup table (`local.properties` keys, CI secrets) and the privacy policy link.

**Definition of done for every change:**
1. `./gradlew ktlintCheck detekt`, `./gradlew test verifyRoborazziDebug koverVerify` and `./gradlew lint` pass (and `:domain:pitest` after domain changes).
2. New or changed screenshots were looked at, not just re-recorded.
3. The change was exercised on the device when it touches UI or interaction.
4. `CLAUDE.md` describes any new behavior, command or pitfall; a kit change is released and `vinkit.tag` bumped.
5. Every decision made on my behalf is listed in the report.

## 8. Play Store readiness (fixed)

- **Privacy policy:**
  - Write it with me in every shipped language: the data on the device, AdMob and what Google collects, consent and privacy options, bug reports (what the report holds, its attachments, where it goes: `vrpedrinho+battlegrid@gmail.com` or a public GitHub issue), every permission in the merged release manifest (internet, network state, vibration, `AD_ID`, the ad services ones, …), legal bases (LGPD/GDPR), rights, the age group (section 1b), and the contact email (section 1b).
  - Host it on my public GitHub Pages repo, `vinaooo.github.io` (`/battlegrid/privacy.html`, a section per language with an anchor), not in the app repo. Its default branch is `main` but Pages serves `master`: push the same commit to both, check it with `curl`, and force a rebuild (`gh api -X POST repos/vinaooo/vinaooo.github.io/pages/builds`) if it doesn't update. Update it when the permissions change.
  - The app always links to it from Settings.
- **AdMob setup** (walk me through it click by click; the console UI is in Portuguese):
  - Add the app as not yet published and create a banner ad unit. Confirm which App ID owns the ad unit before using it: my account has several apps.
  - **Privacy & messaging:** publish a European message (turn **"Não consentir" / Do not consent** on) and a US-states message. The "entry point" page is satisfied by Settings → Privacy options.
  - **Real IDs** go only in `local.properties` and the repo secrets `VINKIT_ADS_APP_ID` / `VINKIT_ADS_BANNER_ID`. Register my phones as test devices (hash from logcat) before installing any release build with real IDs, and never tap real ads. A new ad unit returns "no fill" until AdMob reviews the app after launch.
  - To see the real European consent form, build a local release with the EEA simulated on a test device, then revert that change without committing it. Settings → Privacy options reopens an answered form.
  - `app-ads.txt` goes at the root of the developer website, and that exact URL is the Play listing's website.
- **Store kit** (in a scratchpad folder plus a zip):
  - A title per language, max 30 chars, leaving room for future modes. Short (≤ 80) and full descriptions listing only real features.
  - A 512×512 icon and a 1024×500 feature graphic without text to translate.
  - Screenshots in every language: from an offline emulator (no ads or consent form), SystemUI demo mode for a clean status bar, brand colors (dynamic color off), cropped to ≤ 2:1.
  - A checklist with the data safety table (AdMob: approximate location, app interactions, crash logs/diagnostics, device IDs; collected and shared; encrypted in transit; check Google's current SDK disclosures), the content rating answers, the target audience (section 1b), the ads and advertising-ID declarations, and the release steps.
- Target API as Play requires, 64-bit, AAB, Play App Signing; back up the upload keystore outside the repo.
- Personal developer accounts created after 13 Nov 2023 need a closed test (12+ testers, 14 days) before production; older accounts don't. Ask me which applies. The first upload is manual; the CI workflow only works after it.
