# Bookline — Architecture

> **Last updated:** 2026-03-13

## Overview

Bookline is a personal Android app that reads Goodreads RSS feeds and displays
your books in a visually pleasing timeline. It is a small-scope, single-developer
project — the emphasis is on clean code, not enterprise process.

## Tech Stack

| Concern       | Choice                                                                 |
|---------------|------------------------------------------------------------------------|
| Language      | Kotlin (2.3+)                                                          |
| UI toolkit    | Jetpack Compose                                                        |
| Design system | Material 3 (Material You, dynamic color)                               |
| Min SDK       | 28 (Android 9) · Target SDK 37                                         |
| Build         | Gradle 9 with Kotlin DSL + version catalog (`libs.versions.toml`)      |
| Async         | Kotlin Coroutines + Flow                                               |
| Networking    | HttpURLConnection (no library for a single GET)                        |
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
│   │   ├── BookDao               # DAO with Flow queries, upsert, and retention-based cleanup
│   │   ├── BookSeriesEntity      # Room entity for book-series memberships (many-to-many)
│   │   ├── BookSeriesDao         # DAO for series queries + orphan cleanup
│   │   ├── BookSortOverrideEntity # Room entity for to-read manual sort date overrides
│   │   ├── BookSortOverrideDao   # DAO for sort override CRUD + orphan cleanup
│   │   ├── SeriesInfoEntity     # Room entity for series display name mapping
│   │   ├── SeriesInfoDao        # DAO for series info (rename/merge lookups)
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
│       ├── SeriesEntry           # Book's membership in a series (seriesName + position)
│       └── ToReadBookItem        # Book + effectiveSortDateMs for to-read list ordering
├── ui                            # Presentation layer
│   ├── theme                     # Material 3 theme (Color, Type, Theme)
│   ├── navigation                # NavHost, route definitions, BooklineApp scaffold
│   │   ├── BooklineApp           # Top-level scaffold with TopAppBar + BottomNavBar
│   │   └── TopLevelRoute         # Enum of bottom-nav destinations
│   ├── components                # Shared UI components (BookCard, SeriesCard, BookCover, SearchField, SyncErrorBanner, placeholders)
│   ├── common                    # Cross-cutting utilities (SyncCoordinator, DateFormatters, PreviewData fixtures)
│   ├── timeline                  # Timeline screen (main screen)
│   │   ├── TimelineScreen        # LazyColumn of book cards with covers
│   │   └── TimelineViewModel     # Loads feed, exposes TimelineUiState
│   ├── shelves                   # Shelf browser / To Read screen
│   ├── library                   # Library screen (all books with search + shelf filter)
│   │   ├── LibraryScreen         # Search field, shelf chip row, LazyColumn of BookCards
│   │   └── LibraryViewModel      # Search/shelf filtering, exposes LibraryUiState
│   ├── series                    # Book Series screens (list + detail)
│   │   ├── SeriesListScreen      # LazyColumn of series cards, pull-to-refresh
│   │   ├── SeriesListViewModel   # Observes all series, exposes SeriesListUiState
│   │   ├── SeriesDetailScreen    # Books in a series ordered by position
│   │   └── SeriesDetailViewModel # Loads single series by name
│   ├── goodreads                 # Embedded Goodreads WebView (accessible from top bar icon)
│   ├── detail                    # Book detail screen (series names link to series detail)
│   ├── settings                  # Settings screen (RSS URL config)
│   │   ├── SettingsScreen        # Compose UI for settings
│   │   └── SettingsViewModel     # ViewModel for settings state
│   └── about                     # About screen (version, disclaimer, external links)
│       └── AboutScreen           # Static content, no ViewModel needed
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
   SyncCoordinator     ──  Mutex-based singleton, at most one sync at a time
        │                  checkSync() skips if running; requestSync() joins existing
        ▼
   BookRepository      ──  sync(): fetches all pages, upserts books + book_series
        │                  applies series_info display name mappings
        │                  30-day retention cleanup (not immediate deletion)
        │                  dormancy protection for long-unused app
        │                  renameSeries(): rename/merge via series_info table
        │
        ▼
   Room (BookDao +      ──  single source of truth, emits Flow<List<Book>>
    BookSeriesDao +         book_series table tracks many-to-many series memberships
    SeriesInfoDao)          series_info table maps parsed names → display names
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
3. **To Read** — books on the to-read shelf, with manual drag-to-reorder
4. **Library** — all books with search (title/author) and shelf filter chips
5. **Goodreads** — embedded WebView (accessible from top bar globe icon on all screens)
6. **Book detail** — cover, metadata, rating, review; series names link to series detail
7. **Series detail** — books in a series ordered by position, rename/merge via edit icon
8. **Settings** — configure Goodreads RSS feed URL, theme prefs

## To-Read Sort Overrides

The to-read list supports manual reordering via drag-and-drop (toggle with the
⇅ button in the top app bar). Position is determined by an **effective sort
date** (epoch milliseconds), sorted descending:

- **Default**: `userDateAdded` × 86 400 000 (epoch days → ms)
- **Override**: stored in the `book_sort_overrides` table, independent of the
  main `books` table so it survives feed sync

When a book is dragged to a new position, its override is set to:
- **Top**: `max(now, topNeighbor + 1 day)` — new Goodreads books naturally
  appear above since they get a fresh date
- **Between two books**: midpoint of neighbors' effective dates
- **Bottom**: bottom neighbor − 1 day

## Sync Safety

The sync system is designed to prevent data loss from network failures,
partial syncs, and concurrent sync requests.

### Concurrency control

A shared `SyncCoordinator` singleton (in `ui/common/`) guarantees at most one
sync runs at a time using a Kotlin `Mutex`. All ViewModels delegate to this
coordinator:

- **Auto-sync** (`checkSync`): called on screen resume; skips if a sync is
  already running or data is fresh (< 24 hours old).
- **Manual refresh** (`requestSync`): called by pull-to-refresh; if a sync is
  already running, the existing sync is observed (no duplicate started).
- **Shared state**: `isRefreshing` and `lastSyncResult` are `StateFlow`s
  observed by all screens simultaneously.

### 30-day data retention

Books removed from the Goodreads RSS feed are **not** immediately deleted.
Instead, each book's `lastSyncedMs` timestamp tracks when it was last seen in
a successful sync. After a successful sync, only books whose `lastSyncedMs`
is more than 30 days older than the current sync timestamp are deleted:

```sql
DELETE FROM books WHERE lastSyncedMs > 0 AND lastSyncedMs < :syncTimestamp - 30_DAYS
```

This protects against:
- **Partial sync failures**: if only some pages load, books from unfetched
  pages keep their old `lastSyncedMs` and survive for 30 more days.
- **Temporary Goodreads API issues**: books briefly missing from the feed
  are retained and reappear on the next successful sync.
- **Manual sort overrides**: `book_sort_overrides` are only orphan-cleaned
  after book deletion, so to-read ordering survives for the retention period.

### Dormancy protection

If the app hasn't been used for more than 30 days (last successful sync is
> 30 days ago), all existing books' `lastSyncedMs` is refreshed to `now`
before the sync begins. This prevents mass deletion of books whose
timestamps became stale due to app inactivity rather than feed absence.

### Stale book UI

Books not present in the latest successful sync (`lastSyncedMs <
lastSuccessfulSyncMs`) are marked as `isStale = true` in the domain model.
They appear at 50% opacity with a small warning badge on their cover image.
This is computed in `BookRepository`'s observe methods and applies uniformly
across all screens (Timeline, Library, Series, To Read, Book Detail).

### Sync error reporting

When a sync fails, `SyncCoordinator.lastSyncResult` emits a `SyncResult.Error`.
All list screens show a `SyncErrorBanner` at the top of their content with the
error message and a retry button. The banner auto-clears when a subsequent sync
succeeds.

## Localization

- **Languages**: English (default, `res/values/strings.xml`) and Finnish
  (`res/values-fi/strings.xml`, where the app is named *Lukujana*). All
  user-facing text goes through string resources; only `@Preview` composables
  may use hardcoded strings.
- **Per-app language**: `res/xml/locales_config.xml` + `android:localeConfig`
  in the manifest enable the Android 13+ system per-app language picker.
  `androidResources.localeFilters` in `app/build.gradle.kts` limits packaged
  locales to `en`/`fi` — add new languages in both places, plus an option in
  `LanguageDropdown` in `SettingsScreen`.
- **In-app language selector**: the Settings screen opens with a Language
  dropdown (System default / English / Suomi) that reads and writes the same per-app
  locale store via `LocaleManager.applicationLocales`, so it stays in sync with
  the system dialog. API 33+ only; the section is hidden on older versions
  (`currentLanguageTag = null`), where the app follows the system locale.
- **Counts use `<plurals>`** (`book_count`, `series_count`, `page_count`, …)
  via `pluralStringResource`.
- **ViewModels never resolve display text**: `TimelineViewModel` emits
  `SectionTitle` (sealed: `Text` / `MonthName` / `CurrentlyReading` /
  `DateUnknown`) and the composable resolves it against the current locale,
  because ViewModels outlive locale changes. Dates are formatted with
  `DateFormatters.displayDate` (localized `FormatStyle.MEDIUM`, resolved per
  call).
- **Sync error messages** (exception text shown in `SyncErrorBanner`) stay in
  English — they are diagnostic strings originating from exceptions in the data
  layer; only the "Sync failed:" wrapper is localized.

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

