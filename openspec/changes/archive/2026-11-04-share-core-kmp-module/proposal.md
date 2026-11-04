# Proposal

## Why

MovieGallery is meant to get an iOS app, and the cheapest route there is Kotlin Multiplatform: share the domain and data layers, keep each platform's UI. Every later step (Ktor, KMP-friendly DI, shared ViewModels, the iOS app itself) needs a shared module to move code into, so this change creates one and fills it with the code that already has no Android dependency. The Android app must behave exactly as it does today.

## What Changes

- Add a `:shared` Kotlin Multiplatform module with Android, `iosArm64` and `iosSimulatorArm64` targets. `:app` stays the Android application module and depends on `:shared`.
- Move the whole domain layer (`domain/model`, `domain/repository`, `domain/usecase`) into `:shared` common code, without the `javax.inject` annotations.
- Move the pure data code into `:shared` common code:
  - TMDB and BFF response DTOs, and their mappers to domain models
  - `TmdbGenres` and `TmdbImages`
  - The Room watchlist entity, DAO, database and column converters, plus the schema exports
  - The Countries GraphQL operation and schema, `safeGraphQlCall` and the country repository
  - The watchlist repository, whose timestamps switch from the JVM-only clock to the Kotlin standard-library clock
- The rating label (`Movie.ratingLabel`, `TvShow.ratingLabel`) uses JVM-only string formatting. It becomes a per-platform function, and the Android version keeps the exact current output, including locale-specific decimal separators.
- Hilt stays in `:app`. Shared classes are wired through `@Provides` functions there, because Hilt can't put `@Inject` on classes it doesn't compile.
- Mappers that stay in use by `:app` code (Retrofit-backed repositories) become public until those repositories move in a later phase.
- Tests that only need plain Kotlin move to `:shared`, and run on the Android host and the iOS simulator. Tests that need Robolectric, MockWebServer, MockK or JVM classpath fixtures stay in `:app` and keep exercising the moved code.
- Update `CLAUDE.md`, the `android-architecture` skill and the OpenSpec project context: they currently describe a single `:app` module.

Backends touched: TMDB, BFF and Countries GraphQL, by moving their DTOs, mappers and GraphQL client code. No request, response or error mapping changes. Firebase AI and Firebase Auth: none.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a structural refactor with no change in user-observable behavior, so the change sets `skip_specs: true`. Every existing spec in `openspec/specs/` must still hold unchanged after it lands.

## Out of scope

- Replacing Retrofit/OkHttp with Ktor. `TmdbApi`, `BffApi`, `SafeApiCall`, `BffAuth`, `NetworkModule`, and the movie and TV-show repositories stay in `:app`.
- Replacing Hilt with a KMP-friendly DI library (Koin, kotlin-inject or Metro).
- Firebase Auth and Firebase AI abstractions or iOS implementations; `AuthRepositoryImpl`, the AI chat stack and App Check stay in `:app`.
- Moving ViewModels or any Compose UI into `:shared`.
- An iOS database builder, an iOS framework export, or an Xcode project. This change only proves the shared code compiles and its tests pass for iOS targets.
- Renaming `:app` to `:androidApp`. That costs a churn of paths (the git-ignored `google-services.json`, hooks, docs, OpenSpec rules) for no functional gain.

## Pre-production debt

None. No placeholder behavior is introduced. The public mappers are a temporary widening of visibility, tracked as a task in the Ktor phase, not user-facing debt.

## Impact

- **Build:**
  - New `:shared` module and `settings.gradle.kts` entry.
  - New catalog entries: the Kotlin Multiplatform plugin, the AGP KMP library plugin and `kotlinx-coroutines-core`.
  - The Room and Apollo Gradle configuration moves from `:app` to `:shared`.
  - Room schemas move from `app/schemas/` to `shared/schemas/`.
- **Code:** about 50 main files and 5 test files move. Package names stay `com.raikar.moviegallery.*`, so imports in `:app` don't change.
- **DI:** `RepositoryModule` loses two `@Binds`. A new `:app` Hilt module provides the shared use cases and repositories, and `ClockModule` provides a `kotlin.time.Clock`.
- **Tests:** `TickingClock` and the watchlist repository tests switch to the new clock type. The commands for running shared tests are documented in `CLAUDE.md`.
- **Users:** none. The database file, schema version and stored data stay the same.
