# AGENTS.md

This file provides guidance to AI coding assistants (Claude Code, Codex, etc.) when working with code in this repository. `CLAUDE.md` is a symlink to this file.

## Project

Bookline — single-module Android app (Kotlin + Jetpack Compose) that fetches a Goodreads RSS feed and renders the library as a timeline. Personal prototype, single professional developer.

- Package: `fi.pomeranssi.bookline`
- Min SDK 28, Target SDK 37, Java 17, Gradle 9 (Kotlin DSL) with version catalog (`gradle/libs.versions.toml`).

## Authoritative docs

- `ARCHITECTURE.md` — package layout, data flow, sync safety design, to-read sort overrides. Always consult for current layout. Update when packages or significant components change.
- `README.md` — user-facing feature list. Update when user-visible features change.

## Common commands

```bash
./gradlew assembleDebug      # build debug APK
./gradlew installDebug       # install on connected device/emulator
./gradlew assembleRelease    # signed release build (needs local.properties keystore vars)
./gradlew lint               # Android lint (warningsAsErrors — must be clean)
./gradlew spotlessApply      # format code (Spotless + ktlint) — run after making edits
./gradlew spotlessCheck      # verify formatting (CI enforces this)
./gradlew testDebugUnitTest  # run local unit tests (CI enforces this)
./gradlew clean
```

**Formatting is enforced**: CI (`.github/workflows/ci.yml`) runs `spotlessCheck`, `lint`, `testDebugUnitTest`, and `assembleDebug` on every push/PR. Always run `./gradlew spotlessApply` after editing Kotlin or Gradle files. Lint runs with `warningsAsErrors = true`; new-version-available checks are downgraded to informational. ktlint config lives in `.editorconfig`.

Unit tests exist only for pure logic with mangling risk (e.g. `SeriesParser`) — do not add test files elsewhere unless explicitly asked. `example-feed.rss` at repo root is sample Goodreads data for manual parser testing. If missing, ask user to re-export from Goodreads.

## Tech stack

- **Kotlin 2.0+** — idiomatic modern Kotlin (coroutines, flows, extension functions, data classes, sealed types).
- **Jetpack Compose + Material 3** — all UI is Compose; no XML layouts. Use `MaterialTheme` tokens (colors, typography, shapes), never hardcoded values. Support dynamic color / Material You.
- **Room** for local persistence.
- **Coil 3** for image loading.
- **HttpURLConnection** for HTTP + `XmlPullParser` for RSS — no Retrofit/OkHttp client.
- **Compose Navigation** — single-activity MVVM, one `MainActivity`, ViewModels exposing `StateFlow<UiState>`.
- **Manual constructor injection** — no Hilt/Dagger/Koin. Lightweight `ServiceLocator` / factory acceptable.
- **Gradle Kotlin DSL** with version catalog. Add new dependencies to `gradle/libs.versions.toml` first, then reference in `app/build.gradle.kts`.
- **License attributions are static**: the About screen lists shipped open-source libraries in a hand-maintained `OPEN_SOURCE_LIBRARIES` list (`ui/about/AboutScreen.kt`). When adding or removing an `implementation` dependency, update that list (and verify the new library is Apache-2.0 — if not, the attribution text needs adjusting).

## Architecture essentials (full detail in ARCHITECTURE.md)

- **Single Activity + Compose Navigation**, MVVM with `StateFlow<UiState>` per screen. ViewModels expose state; composables observe.
- **Offline-first**: Room is single source of truth. `BookRepository.sync()` fetches all RSS pages, upserts into `books` + `book_series` + `series_info` tables, then emits via `Flow`.
- **SyncCoordinator** (`ui/common/`) — Mutex-backed singleton. All ViewModels delegate to it; at most one sync runs at a time. `checkSync()` for auto-sync on resume, `requestSync()` for pull-to-refresh. Shared `isRefreshing` + `lastSyncResult` StateFlows drive all screens.
- **30-day retention** — books missing from feed are NOT deleted immediately; kept via `lastSyncedMs` timestamp. Protects against partial syncs, transient Goodreads outages, and long app dormancy (>30d inactive → all timestamps refreshed before sync to avoid mass deletion).
- **Stale book flag** — books with `lastSyncedMs < lastSuccessfulSyncMs` render at 50% opacity with warning badge.
- **To-read manual reorder** — `book_sort_overrides` table holds an effective sort-date (epoch ms) per book, independent of `books` table so it survives sync. Default sort date = `userDateAdded` × 86 400 000. Drag-to-reorder computes midpoint / ±1 day vs neighbors.
- **Series rename/merge** — `series_info` table maps parsed series name → display name. Applied in repository read path.
- **Settings storage** — Keystore-encrypted SharedPreferences (`SettingsRepository`) for feed URL + sync timestamp.

## Package layout

```
fi.pomeranssi.bookline
├── data/          # Data layer (db, network, repository)
├── domain/        # Domain models & use cases
├── ui/            # Compose screens, theme, navigation, common utilities
└── MainActivity.kt
```

Place new files in the appropriate package. If a new package is needed, update `ARCHITECTURE.md`.

## Code conventions

- Favour clarity over cleverness.
- Naming: `PascalCase` for classes/composables, `camelCase` for functions/properties, `SCREAMING_SNAKE_CASE` for constants.
- Composable functions that emit UI start with uppercase letter.
- Keep composables small and focused; extract reusable pieces early.
- Use `sealed interface` / `sealed class` for UiState hierarchies.
- Prefer `data class` and `value class` for models.
- Use named arguments for Compose modifiers and non-obvious parameters.
- Keep business logic out of composables — push into ViewModels or repositories.
- Group imports; no wildcard imports.

## File generation patterns

- **New screen** → create `<Name>Screen.kt` + `<Name>ViewModel.kt` with sealed `UiState` type.
- **New data source** → DAO/entity in `data/db/`, domain model in `domain/model/`, repository interface + impl in `data/repository/`.
- Update `ARCHITECTURE.md` when adding significant new components.

## Things to avoid

- No XML layout files — Compose only.
- No DI framework (Hilt/Dagger/Koin) — manual DI.
- No test files unless explicitly asked.
- No multi-module splits unless explicitly discussed.
- No deprecated Compose APIs (old `accompanist` libs folded into platform).
- No hardcoded colors or text sizes — always via `MaterialTheme`.
- CI (GitHub Actions) only checks format/lint/build — no release/deploy pipelines; release builds run locally in Android Studio.
