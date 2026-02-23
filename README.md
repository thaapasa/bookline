# 📚 Bookline

**A timeline for your books.**

Bookline is an Android app that reads the RSS feed exported from
[Goodreads](https://www.goodreads.com/) and presents your read (and to-read)
books in a beautiful, visually rich timeline — giving you a fresh perspective on
your reading journey.

## ✨ Features

- **Goodreads RSS import** — fetch your shelves directly from your Goodreads RSS
  feed
- **Reading timeline** — see your books laid out chronologically, showing when
  you started and finished each one
- **Shelf browsing** — browse books by shelf (_read_, _currently-reading_,
  _to-read_, and any custom shelves)
- **Beautiful book cards** — cover art, title, author, rating, and review at a
  glance
- **Material You theming** — dynamic color support with Material 3 so the app
  feels right at home on your device
- **Dark mode** — full light & dark theme support out of the box
- **Offline-ready** — books are cached locally so you can browse your collection
  without a network connection

## 📸 Screenshots

_Coming soon._

## 🏗️ Tech Stack

| Layer            | Technology                               |
|------------------|------------------------------------------|
| **Language**     | Kotlin                                   |
| **UI**           | Jetpack Compose with Material 3          |
| **Minimum SDK**  | 28 (Android 9 Pie)                       |
| **Target SDK**   | 36                                       |
| **Build system** | Gradle (Kotlin DSL) with version catalog |
| **Architecture** | Single-module (for now)                  |

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Meerkat (2024.3+) or newer
- **JDK 11** or newer
- An Android device or emulator running **API 28+**

### Build & Run

```bash
# Clone the repository
git clone https://github.com/thaapasa/bookline.git
cd bookline

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug
```

### Goodreads RSS Feed

To use Bookline you need your Goodreads RSS feed URL. You can find it on your
Goodreads profile:

1. Go to **My Books** on Goodreads.
2. At the bottom of the page, find the **RSS** link for your shelf.
3. Copy the URL and paste it into Bookline's settings.

## 🗂️ Project Structure

```
bookline/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/fi/pomeranssi/bookline/   # Application code
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── ui/theme/                   # Material 3 theming
│   │   │   ├── res/                            # Resources & drawables
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                               # Unit tests
│   │   └── androidTest/                        # Instrumented tests
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                      # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to open an
issue or submit a pull request.

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

_Built with ❤️ and Jetpack Compose._

