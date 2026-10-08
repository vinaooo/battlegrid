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

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per
milestone (`PLAN.md`). Commit, push and open PRs only when the user asks.
