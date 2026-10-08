# BattleGrid: plan

## Context
BattleGrid is Battleship for Android, built on vinkit (`vinkit.tag`, the newest release from JitPack, never a local
copy). It follows `game-prompt-template.md` (this folder's copy, section 1a filled in with the confirmed design)
sections 2–8, with OX Play (`../xo`) as the reference shape. What vinkit already provides is not rewritten; gaps are
fixed in vinkit and released before the game uses them.

## Confirmed decisions
| Key | Value |
|---|---|
| App name | BattleGrid (a string resource) |
| Slug / package | `battlegrid`; `io.github.vinaooo.battlegrid`; repo `vinaooo/battlegrid`, public, MIT |
| Brand color | teal (`ThemeColor.TEAL`) |
| Boards and fleets | 8×8: 5, 4, 3, 2 · 10×10: 5, 4, 3, 3, 2 · 12×12: 5, 4, 4, 3, 3, 2; straight ships, may touch, never overlap |
| Ship names | Carrier / Porta-aviões (5), Battleship / Encouraçado (4), Frigate / Fragata (4, 12×12 only), Cruiser / Cruzador (3), Submarine / Submarino (3, not on 8×8), Destroyer / Destróier (2) |
| Placement | drag ships onto the grid, tap a ship to rotate (a ship that would leave the grid shifts back inside), Random, Start; the AI's fleet from the seed |
| Firing modes | Classic (1 shot), Hit-again (a hit fires again), Salvo (shots = own ships afloat, mark then Fire, per-shot results); same rules for both sides |
| First shot | random in the very first game, then the previous game's winner |
| End | sink every enemy ship = win; no draw; resign = loss; leaving during placement isn't a loss |
| Variants | size × firing mode, all 9 combinations, two independent Settings rows |
| Opponent | AI Easy (random), Medium (hunt / target), Hard (probability map + parity); local pass-and-play (cover screen each turn, not recorded); AI pause ~600 ms per shot |
| Generation | seeded (`Random(seed)`); "restart the same board" keeps the AI's fleet, the player places again |
| Score | shots to win + 5 per hint, lower is better; ranking per size × firing mode × difficulty, ties by time; stats played / won / lost / streaks |
| Helps | no undo / redo in battle; hint = 5 untried cells, exactly 1 is a ship cell; max 3 per game; off in 2-player |
| Timer | count-up, shown, battle only, no limit |
| Input | battle: tap fires at once (Classic, Hit-again); Salvo: tap marks / unmarks, toolbar Fire button |
| Look | flat sea, capsule ships, hit burst, miss ring, sunk ship outlined and dimmed; portrait = enemy grid big + own mini (tap the mini to swap; auto-swap on the opponent's turn); landscape / tablet = side by side |
| Settings → Game | Board size (`Choice`), Firing mode (`Choice`), Opponent (`IconChoice`: Easy / Medium / Hard / 2 players) |
| Extras | How-to-play screen, sounds (miss / hit / sunk), daily challenge (10×10 Classic vs Hard, UTC date seed, 1 ranked attempt), 9 local achievements on a Scores tab |

## Project defaults (veto any)
- Languages pt-BR + en; portrait + landscape, phones and tablets (phone view on tablets).
- One placeholder banner (`PlaceholderAdBanner`) on the game screen only; real AdMob + consent later.
- No online services, no Play Games.
- Bug report `ReportTarget("vrpedrinho+battlegrid@gmail.com", "vinaooo/battlegrid")`.
- Privacy policy `https://vinaooo.github.io/battlegrid/privacy.html` (`#en`, `#pt-br`).
- Audience 13+, not in the Families program.
- Persistence: scores and stats in vinkit's `ScoresDatabase` under the mode keys; the game in progress in a versioned
  JSON file written atomically.
- Publishing: release-ready, `release.yml` off, first upload by hand.

## Implementation defaults (veto any)
1. **Mode keys:** `<SIZE>_<FIRE>_<LEVEL>`, e.g. `TEN_SALVO_HARD` (27 AI modes) + `DAILY`. Never renamed after release.
2. **Scores tabs:** grouped by board size (`groupOf`), a section per firing mode × difficulty; `DAILY` and
   Achievements get their own tabs.
3. **AI randomness:** `Random(seed * 31 + actions)` (`actions` = applied battle moves; a whole salvo is one), frozen after release.
4. **Hint randomness:** `Random(seed * 37 + hintsUsed)`, so a hint is the same after process death.
5. **Hit-again with Salvo** is not a combination (they are values of one setting).
6. **Info area:** mode ("10×10 · Salvo · Hard"), whose turn ("Your turn", "Enemy firing", "Player 2's turn"), in
   Salvo "3 shots"; stats: shots, hits, time.
7. **Toolbar:** hint, Fire (Salvo only, enabled at N marks), new game. Undo / redo hidden (`visible = false`);
   during placement: Random and Start.
8. **New-game menu:** new game, restart this board, resign (battle only), how to play, daily challenge, report a bug.
9. **End:** win → `WinDialog` (shots, accuracy %, time, hints) with celebration; loss → same dialog "You lost", no
    celebration. Pass-and-play: "Player 1 wins".
10. **Daily storage:** day streak and the last daily date are the game's own DataStore keys.
11. **Achievements:** a set of unlocked ids in the game's DataStore keys, checked by a pure `AchievementRules` after
    each finished game; an unlock shows a snackbar.
12. **TalkBack:** one node per cell ("B 7, miss" / "C 3, hit, Cruiser sunk" / "D 4, untried"), click = fire;
    placement: custom actions per ship (move left / right / up / down, rotate); shots announced, the AI's included.
13. **Pieces' colors:** ships `secondary`, hit `error`, miss `outline`, sea `surfaceContainerHigh`, with contrast
    ≥ 3:1 tested in every palette and both themes.

## Game rules confirmed after the plan
- **Cell size on phones:** cells take the room there is, no zoom: about 42dp on 8×8, 34dp on 10×10, 29dp on 12×12
  on a 411dp portrait phone.
- **AI in Salvo:** Hard picks its N cells one at a time from the probability map, treating the cells already chosen
  as tried; Medium fires at target cells first, then random ones.
- **Loss reveal:** on a loss, the AI's remaining ships are shown on its grid before the dialog.
- **Daily attempt:** the ranked attempt is the first battle started that UTC day; replays are unranked, against the
  same AI fleet. Hints are allowed in the daily (+5 shots each).
- **Restart in pass-and-play:** both players place again; only the Random button's layouts repeat.
- **Hint highlight:** stays until the next shot.

## vinkit gaps (one kit release inside milestone 2, as OX Play did)
- `core`: `Ranking.LOWEST_POINTS` (fewest points first, then fastest) and its SQL query in `scores`; `ScoresScreen`
  rows show it like points.
- `shell`: `FeedbackEvent` has only `MOVE`, `REJECTED`, `WIN`. Games need their own sounds (miss, hit, sunk, loss):
  let `AndroidGameFeedback` take extra sound resources keyed by game events (non-breaking: keep the enum's three).
- `scores`: `ScoresScreen` can't show a game's own tab (achievements): add an `extraGroups` slot (a name + a
  composable) after the mode groups.
- Whatever else comes up while building goes to this list.

## Modules
```
:app ─► :feature:game, :data, vinkit settings / scores / shell / ads / designsystem
:feature:game ─► :domain, vinkit shell / designsystem / settings / scores / bugreport
:data ─► :domain, vinkit settings / scores
:domain   pure Kotlin/JVM: fleet, rules, AI, hints, session, use cases (vinkit core)
```
No `build-logic`, no own catalog, no `:feature:scores` / `:feature:settings`: vinkit's.

## Domain design
- `Coord(row, col)`, `Orientation`, `ShipClass(nameKey, length)`, `Ship(class, origin, orientation)`,
  `Fleet(ships)` with `cells`, `isValid(size)`; `FleetSpec.forSize(size)`.
- `Grid(size, fleet, shots: Map<Coord, ShotResult>)` per side; `ShotResult` = `Miss` / `Hit` / `Sunk(shipIndex)`.
- `GameMode(size, fire, opponent)` + `key`; `GameState(mode, phase, grids, toMove, salvoMarks, actions,
  hintsUsed, hint, elapsed, status)`; `phase` = `Placement(side)` / `Battle` / `Over(winner)`. All `@Serializable`.
- `Move` sealed: `PlaceShip`, `RotateShip`, `RandomFleet`, `ConfirmFleet`, `Fire(coord)`, `MarkSalvo(coord)`,
  `FireSalvo`, `Resign`.
- `RuleSet` with one rule object per move type; `FiringRule` per mode (`ClassicFiring`, `HitAgainFiring`,
  `SalvoFiring`) decides shots per turn and who moves next. `GameEngine.apply` → `Applied` / `Rejected`;
  `legalMoves`, `isLegal`.
- `ScoringStrategy`: `ShotsScoring` (points = shots + 5 × hints, ranking `LOWEST_POINTS`).
- `FleetPlacer` (seeded random valid fleet, attempt-counted retries).
- `Opponent` interface: `RandomAi`, `HuntTargetAi`, `ProbabilityAi`; `:domain:benchmarkAi` reports average shots to
  win per level and size over N seeds.
- `HintEngine`: 1 cell of an unsunk ship + 4 untried water cells (fewer when water runs out), seeded.
- No `UndoHistory` in battle (decided); placement edits are moves on the state, not history.
- `GameSession(seed, state)`; codec = vinkit `GameCodec`.
- Use cases: start (an unfinished vs-AI battle counts as a loss; placement doesn't), fire, finish (records score,
  stats, daily, achievements; clears the save), resign, daily.
- Repositories: `SavedGameRepository`, `GameSettingsRepository`, `DailyRepository`, `AchievementRepository`,
  `SeedSource`, `Clock` (+ vinkit's). Fakes in `testFixtures`.
- Tests: every rule per firing mode and size; property tests (random fleets are always valid, any legal sequence
  keeps the state valid, no cell shot twice, sunk only when every cell is hit, same seed same game, `legalMoves`
  agrees with `isLegal`); codec round trip; hint always has exactly one ship cell; AI benchmark ordering
  Hard < Medium < Easy in average shots on every size (capped by game count, never timed).

## Test strategy
- `:domain`: JUnit 5 + Kotest property tests (above); Pitest ≥ 80% mutants killed, ≥ 90% line coverage.
- `:data`: Robolectric + Turbine: save / restore round trip, corrupt and unknown-version files, the game's keys
  beside vinkit's in one DataStore.
- ViewModels: Turbine + `StandardTestDispatcher`: placement, firing in each mode, AI reply and its cancellation,
  auto-swap, covers, hint, win / loss / resign, daily and achievements, settings changes that confirm; every test
  pauses its ViewModels in a `finally`.
- UI (Compose on Robolectric): placement drag (stepped `moveBy`), rotate, Random, firing taps, Salvo marks and Fire,
  grid swap, dialogs, menus, both orientations, TalkBack nodes.
- App: Hilt + Robolectric, banner on the game screen only, consent once at launch, ads faked via `@TestInstallIn`.
- On-device: Hilt instrumented tests with the orchestrator (`clearPackageData`) on `Solo_API_26` and
  `Pixel_9a_Android_16`, local only.
- Roborazzi: light / dark, every size and firing mode, placement and battle, portrait / landscape / tablet / phone
  view, both languages, the launcher icon.
- Kover: ≥ 70% overall (UI excluded), ≥ 90% on `:domain`.

## Milestones (one PR each, after your approval)
1. Scaffold: vinkit plugins and catalog, CI (+ actionlint), wrapper, `CLAUDE.md`, empty app on `VinkitTheme` (teal).
2. vinkit release (gaps above), then domain core: fleet, grids, placement, engine with the three firing modes,
   scoring, session, codec.
3. Domain extras: fleet placer, AI Easy / Medium / Hard + benchmark, hint engine.
4. Data: saved game file, settings keys, daily and achievement keys, vinkit settings + scores wiring.
5. Game screen: placement (drag, rotate, Random, Start), battle (two grids, swap, Salvo marks), pass-and-play
   covers, end dialog, feedback, TalkBack.
6. Settings, Scores (with the Achievements tab), navigation, ads placeholder, bug report.
7. Daily challenge, achievements, How-to-play screen, game sounds.
8. Adaptive layouts, pt-BR, accessibility pass, screenshots, launcher icon.
9. Release prep: signing, Play workflow (off), privacy policy, store kit and checklist.

## Play Store checklist
Privacy policy (en, pt-BR) on `vinaooo.github.io/battlegrid/privacy.html`; AdMob app + banner + EU / US messages;
`app-ads.txt`; titles ≤ 30 chars, descriptions; 512×512 icon, 1024×500 feature graphic; screenshots per language;
data safety, content rating, audience 13+, ads / advertising-ID declarations; upload key
`/home/vina/keys/battlegrid-upload.jks` (you create it); ask whether your developer account needs the closed test.

## Verification
- Gate (in pieces, low RAM): `./gradlew ktlintCheck detekt`, `./gradlew test verifyRoborazziDebug koverVerify`,
  `./gradlew lint`, `:domain:pitest` after domain changes.
- On your Moto and the emulators: placement drag / rotate / Random, every firing mode and size vs each AI level,
  pass-and-play covers, grid swap and auto-swap, hints, resign, daily, achievements, rotation, tablet phone view,
  themes, both languages, Scores, bug report (GitHub path only), ads and consent on `Pixel_9a_Android_16`.
