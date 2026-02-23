# Bookline — Copilot Instructions

## Project Identity

**Bookline** is a personal Android app that reads Goodreads RSS feeds and
displays your reading history as a visually rich timeline. It is built by a
single professional developer as a quick prototype to validate the idea.

- Package: `fi.pomeranssi.bookline`
- Architecture doc: see `ARCHITECTURE.md` in the project root — always consult
  it for the current package layout, tech choices, and data flow.

## Tech & Tooling

- **Kotlin** (2.0+) — use idiomatic, modern Kotlin throughout (coroutines,
  flows, extension functions, data classes, sealed types).
- **Jetpack Compose** with **Material 3** — all UI is Compose; no XML layouts.
  Use `MaterialTheme` tokens (colors, typography, shapes) rather than hardcoded
  values. Support dynamic color / Material You.
- **Gradle Kotlin DSL** with a version catalog at `gradle/libs.versions.toml`.
  When adding dependencies, add them to the catalog first.
- **Single-activity** MVVM — one `MainActivity`, Compose Navigation for routing,
  ViewModels exposing `StateFlow<UiState>`.
- **Room** for local persistence, **Coil** for image loading, **Ktor** (or
  Retrofit) for HTTP + RSS parsing.
- **No DI framework** — use simple constructor injection. A lightweight manual
  `ServiceLocator` / factory is acceptable.
- **No test infrastructure** — we are not writing tests for this project. Do not
  generate test files or test scaffolding unless explicitly asked.
- **No CI/CD** — no GitHub Actions, no pipelines.

## Code Style & Conventions

- Write **clean, readable Kotlin** — favour clarity over cleverness.
- Follow standard Android/Kotlin naming: `PascalCase` for classes/composables,
  `camelCase` for functions/properties, `SCREAMING_SNAKE_CASE` for constants.
- Composable functions that emit UI start with an uppercase letter.
- Keep composables small and focused; extract reusable pieces early.
- Use `sealed interface` or `sealed class` for UiState hierarchies.
- Prefer `data class` and `value class` for models.
- Use named arguments for Compose modifiers and non-obvious parameters.
- Keep business logic out of composables — push it into ViewModels or
  repositories.
- Group imports; no wildcard imports.

## Package Structure

Follow the layout described in `ARCHITECTURE.md`:

```
fi.pomeranssi.bookline
├── data/          # Data layer (db, network, repository)
├── domain/        # Domain models & use cases
├── ui/            # Compose screens, theme, navigation
└── MainActivity.kt
```

When creating new files, place them in the appropriate package. If a new package
is needed, update `ARCHITECTURE.md` to reflect the change.

## File Generation Preferences

- When generating a new screen, create:
    1. A screen composable (e.g. `TimelineScreen.kt`)
    2. A ViewModel (e.g. `TimelineViewModel.kt`) with a `UiState` sealed type
- When generating a new data source, create:
    1. The data source / DAO file in `data/`
    2. The domain model in `domain/model/`
    3. The repository interface + implementation in `data/repository/`
- Always keep `ARCHITECTURE.md` up to date when adding significant new
  components.

## Things to Avoid

- Do **not** generate XML layout files — Compose only.
- Do **not** add Hilt, Dagger, or Koin — we use manual DI.
- Do **not** create test files unless explicitly asked.
- Do **not** introduce multi-module splits unless explicitly discussed.
- Do **not** use deprecated Compose APIs (e.g. old `accompanist` libs that have
  been folded into the platform).
- Do **not** hardcode colors or text sizes — always go through `MaterialTheme`.

