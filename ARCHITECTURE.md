# Bookline — Architecture

> **Last updated:** 2026-02-28

## Overview

Bookline is a personal Android app that reads Goodreads RSS feeds and displays
your books in a visually pleasing timeline. It is a small-scope, single-developer
project — the emphasis is on clean code, not enterprise process.

## Tech Stack

| Concern       | Choice                                                                 |
|---------------|------------------------------------------------------------------------|
| Language      | Kotlin (2.0+)                                                          |
| UI toolkit    | Jetpack Compose                                                        |
| Design system | Material 3 (Material You, dynamic color)                               |
| Min SDK       | 28 (Android 9) · Target SDK 36                                         |
| Build         | Gradle 9 with Kotlin DSL + version catalog (`libs.versions.toml`)      |
| Async         | Kotlin Coroutines + Flow                                               |
| Networking    | Ktor Client (or Retrofit — TBD)                                        |
| XML parsing   | Kotlin XML / XmlPullParser for RSS                                     |
| Image loading | Coil 3 (Compose)                                                       |
| Local storage | Room (SQLite)                                                          |
| Navigation    | Compose Navigation                                                     |
| DI            | Manual / simple constructor injection (no Hilt/Dagger — keep it light) |
| Secrets       | Android Keystore (AES-256-GCM) + plain SharedPreferences               |
| Architecture  | Single-activity, MVVM with UiState pattern                             |

## Module Structure

Single `:app` module — no multi-module split planned unless complexity grows.

## Package Layout

```
fi.pomeranssi.bookline
├── data                          # Data layer
│   ├── db                        # Room database, DAOs, entities
│   │   ├── BookEntity            # Room entity for books (maps to/from domain Book)
│   │   ├── BookDao               # DAO with Flow queries and transactional replaceAll
│   │   ├── BookSeriesEntity      # Room entity for book-series memberships (many-to-many)
│   │   ├── BookSeriesDao         # DAO for series queries + orphan cleanup
│   │   └── BooklineDatabase      # Room database singleton
│   ├── network                   # RSS feed fetching & parsing
│   │   ├── GoodreadsFeedService  # HTTP GET for RSS feed (HttpURLConnection)
│   │   ├── GoodreadsRssParser    # XmlPullParser-based RSS → Book parser
│   │   └── SeriesParser          # Extracts series name + position from book titles
│   └── repository                # Repository implementations
│       ├── BookRepository        # Offline-first: Room as source of truth, sync from RSS feed
│       └── SettingsRepository    # Keystore-encrypted SharedPreferences for feed URL + sync timestamp
├── domain                        # Domain layer (models, use cases if needed)
│   └── model
│       ├── Book                  # Book data class + ReadingStatus sealed interface
│       ├── Series                # Series data class (name, books, coverUrls, authors)
│       └── SeriesEntry           # Book's membership in a series (seriesName + position)
├── ui                            # Presentation layer
│   ├── theme                     # Material 3 theme (Color, Type, Theme)
│   ├── navigation                # NavHost, route definitions, BooklineApp scaffold
│   │   ├── BooklineApp           # Top-level scaffold with TopAppBar + BottomNavBar
│   │   └── TopLevelRoute         # Enum of bottom-nav destinations
│   ├── components                # Shared UI components (BookCard, SeriesCard, placeholders)
│   ├── timeline                  # Timeline screen (main screen)
│   │   ├── TimelineScreen        # LazyColumn of book cards with covers
│   │   └── TimelineViewModel     # Loads feed, exposes TimelineUiState
│   ├── shelves                   # Shelf browser / To Read screen
│   ├── series                    # Book Series screens (list + detail)
│   │   ├── SeriesListScreen      # LazyColumn of series cards, pull-to-refresh
│   │   ├── SeriesListViewModel   # Observes all series, exposes SeriesListUiState
│   │   ├── SeriesDetailScreen    # Books in a series ordered by position
│   │   └── SeriesDetailViewModel # Loads single series by name
│   ├── goodreads                 # Embedded Goodreads WebView screen
│   ├── bookdetail                # Book detail screen
│   └── settings                  # Settings screen (RSS URL config)
│       ├── SettingsScreen        # Compose UI for settings
│       └── SettingsViewModel     # ViewModel for settings state
└── MainActivity.kt               # Single Activity entry point
```

## Data Flow

```
Goodreads RSS feed (XML/HTTP, paginated)
        │
        ▼
   GoodreadsRssParser  ──  XmlPullParser streaming parse → List<Book>
        │                  SeriesParser extracts series info from titles
        ▼
   BookRepository      ──  sync(): fetches all pages, upserts books + book_series
        │
        ▼
   Room (BookDao +      ──  single source of truth, emits Flow<List<Book>>
    BookSeriesDao)          book_series table tracks many-to-many series memberships
        │
        ▼
   ViewModel           ──  observes Flow, exposes StateFlow<UiState> + isRefreshing
        │
        ▼
   Compose Screen      ──  observes state, pull-to-refresh triggers sync
```

## Key Screens

1. **Timeline** — chronological view of books read (main screen)
2. **Series** — browse book series with fan-style cover cards
3. **To Read** — books on the to-read shelf
4. **Goodreads** — embedded WebView showing goodreads.com (with in-WebView back navigation)
5. **Book detail** — cover, metadata, rating, review
6. **Series detail** — books in a series ordered by position
7. **Settings** — configure Goodreads RSS feed URL, theme prefs

## Design Decisions

- **No test infrastructure** — this is a quick personal prototype. Code quality
  is maintained via clean architecture and code review, not automated tests.
- **No CI/CD** — builds run locally in Android Studio.
- **No DI framework** — simple constructor injection keeps the dependency graph
  transparent. A `ServiceLocator` or manual factory is fine at this scale.
- **Offline-first** — Room acts as the single source of truth after the first
  fetch; network is only used to refresh.
- **Single module** — avoids build complexity; package-level separation provides
  enough boundary enforcement.

