# MovieGallery

An Android movie browsing app built with Jetpack Compose. Browse curated movie sections from
[TMDB](https://www.themoviedb.org/), search titles, keep a watchlist, view movie details, browse
top-rated TV shows, and chat with an AI assistant for movie recommendations.

The project is moving to Kotlin Multiplatform for a future iOS app. The domain layer, networking,
local database, repositories and dependency-injection wiring already live in a shared module that
compiles for Android and iOS. No iOS app uses it yet.

## Features

- **Home**: curated movie sections (trending, now playing, top rated, upcoming)
- **Search**: search the TMDB catalog by title
- **Movie details**: backdrop, overview, genres, watchlist toggle, and a details sheet for each
  production country
- **Watchlist**: saved per signed-in account and kept on the device, so it works offline
- **Top TV shows**: highest-rated series, served by a separate backend-for-frontend (BFF) that
  checks the signed-in user's Firebase ID token
- **AI chat**: ask natural-language questions about movies, or send a poster photo to identify the
  film, powered by Firebase AI Logic (Gemini)
- **Sign-in / Profile**: email/password or Google sign-in with Firebase Authentication; the
  profile shows the signed-in user

## Tech stack

- **Language**: Kotlin, with Kotlin Multiplatform for shared code (Android, `iosArm64`,
  `iosSimulatorArm64`)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture (UI / Domain / Data layers). See
  [.claude/skills/android-architecture/SKILL.md](.claude/skills/android-architecture/SKILL.md)
- **DI**: Koin (plain DSL)
- **Networking**: Ktor (OkHttp engine on Android, Darwin on iOS) + kotlinx.serialization for the
  [TMDB API](https://developer.themoviedb.org/docs) and the BFF; Apollo Kotlin for the public
  [Countries GraphQL API](https://countries.trevorblades.com/)
- **Persistence**: Room 3
- **Async**: Kotlin Coroutines & Flow
- **Images**: Coil
- **Auth**: Firebase Authentication, with Credential Manager for Google sign-in
- **AI**: Firebase AI Logic (Gemini Developer API), gated by Firebase App Check
- **Build**: Android Gradle Plugin, Gradle version catalog (`gradle/libs.versions.toml`)
- **Lint/format**: ktlint (`ktlint_official` style)

## Getting started

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- A [TMDB](https://www.themoviedb.org/settings/api) account for a v4 read access token
- A Firebase project (see below)
- Optional, for the shared module's iOS tests: Xcode with an iOS simulator runtime

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

### Firebase setup

The `com.google.gms.google-services` plugin is applied unconditionally, so the project won't
build without a `google-services.json` file (from a Firebase project) placed in `app/`. Without
it, the build fails with "File google-services.json is missing".

Some features need more setup in that Firebase project:

- **Sign-in**: enable the Email/Password and Google providers, register your debug SHA-1, then
  download `google-services.json` again so it includes an OAuth web client. Top TV shows and the
  watchlist depend on being signed in.
- **AI chat**: enable AI Logic, and register an App Check debug token for local development.

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

All JVM unit tests, including the shared module's:

```bash
./gradlew test
```

The shared module's tests on the iOS simulator (needs Xcode):

```bash
./gradlew :shared:iosSimulatorArm64Test
```

## Project structure

```text
shared/                      # Kotlin Multiplatform library (Android + iOS)
├── src/commonMain/          # Domain layer, DTOs and mappers, Ktor and GraphQL clients,
│                            # Room database, repositories, shared Koin modules
├── src/commonTest/          # Tests that run on the JVM and the iOS simulator
└── schemas/                 # Exported Room schemas (committed)

app/src/main/java/com/raikar/moviegallery/
├── data/                    # Android-only data code: Firebase Auth and AI
├── di/                      # Android Koin modules
└── ui/                      # Compose screens, components, navigation, theme
```

Both modules use the same `com.raikar.moviegallery.*` packages. For the full repo map, build
prerequisites and step-by-step checklists, see [CLAUDE.md](CLAUDE.md). For layering conventions,
see [.claude/skills/android-architecture/SKILL.md](.claude/skills/android-architecture/SKILL.md).
