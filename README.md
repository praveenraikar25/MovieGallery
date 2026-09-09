# MovieGallery

An Android movie browsing app built with Jetpack Compose. Browse curated movie sections from
[TMDB](https://www.themoviedb.org/), search titles, keep a watchlist, view movie details, and chat
with an AI assistant for movie recommendations.

## Features

- **Home** — curated movie sections (now playing, popular, top rated, upcoming, etc.)
- **Search** — search the TMDB catalog by title
- **Movie details** — poster, overview, genres, and watchlist toggle
- **Watchlist** — save and remove movies locally
- **AI chat** — ask natural-language questions about movies, powered by Firebase AI Logic (Gemini)
- **Login / Profile** — basic account screens

## Tech stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture (UI / Domain / Data layers) — see
  [.claude/skills/android-architecture/SKILL.md](.claude/skills/android-architecture/SKILL.md)
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp + kotlinx.serialization, backed by the
  [TMDB API](https://developer.themoviedb.org/docs)
- **Async**: Kotlin Coroutines & Flow
- **Images**: Coil
- **AI**: Firebase AI Logic (Gemini Developer API), gated by Firebase App Check
- **Build**: Android Gradle Plugin, Gradle version catalog (`gradle/libs.versions.toml`)
- **Lint/format**: ktlint (`ktlint_official` style)

## Getting started

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- A [TMDB](https://www.themoviedb.org/settings/api) account for a v4 read access token

### Configure the TMDB token

The app reads the TMDB v4 read access token from either an environment variable or a local
properties file (never commit it):

```bash
# Option A: environment variable
export TMDB_READ_ACCESS_TOKEN="your-tmdb-v4-read-access-token"
```

```properties
# Option B: local.properties (repo root, git-ignored)
tmdb.readAccessToken=your-tmdb-v4-read-access-token
```

Without a token, the app builds but TMDB requests fail with `401`.

### Firebase AI chat (optional)

The AI chat feature requires a `google-services.json` file (from a Firebase project with AI Logic
enabled) placed in `app/`, plus an App Check debug token for local development. The rest of the
app works without it.

### Build & run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and run the `app` configuration.

### Lint & format

```bash
./gradlew ktlintCheck   # verify
./gradlew ktlintFormat  # auto-fix
```

Run once per clone to enable the pre-commit ktlint hook:

```bash
./gradlew installGitHooks
```

### Tests

```bash
./gradlew test
```

## Project structure

```
app/src/main/java/com/raikar/moviegallery/
├── data/        # Repositories, remote (Retrofit) and local data sources, DTOs/mappers
├── di/          # Hilt modules
├── domain/      # Models, repository interfaces, use cases
└── ui/          # Compose screens, components, navigation, theme
```

For details on layering conventions and module structure, see
[.claude/skills/android-architecture/SKILL.md](.claude/skills/android-architecture/SKILL.md).
