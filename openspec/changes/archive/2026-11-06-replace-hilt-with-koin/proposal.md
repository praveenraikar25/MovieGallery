# Proposal

## Why

Phase 3 of the KMP migration. `:shared` now holds the domain layer, the networking, Room and the repositories, but none of it can describe its own wiring: Hilt runs only on Android, and `commonMain` can't use `javax.inject`, so `:app`'s `SharedModule` hand-builds every shared class with about 20 `@Provides` functions. An iOS app would have to duplicate all of that wiring by hand. Moving DI to Koin lets the shared graph be declared once, in `commonMain`, and assembled by each platform. The Android app must behave exactly as it does today.

## What Changes

- **Replace Hilt with Koin, using the plain Koin DSL only** (`module { }`, `single`, `factory`, `singleOf`, `factoryOf`, `viewModelOf`). No Koin annotations and no Koin compiler plugin, so Kotlin upgrades are never blocked on a DI release.
- **Declare the shared graph in `:shared` `commonMain`**: the Ktor clients and `TmdbApi` / `BffApi`, the four shared repositories, the watchlist DAO, the clock, the IO dispatcher and all 16 use cases. `SharedModule` is deleted.
- **Turn the three qualifiers into Koin qualifiers in `:shared`**. `@TmdbClient`, `@BffClient` and `@IoDispatcher` (`javax.inject` annotations in `:app`) become named Koin qualifier constants under the same names. The rule that the TMDB bearer token never reaches the BFF client or the base `OkHttpClient` stays.
- **Keep the Android-only bindings in `:app`**: the base `OkHttpClient` and the OkHttp Ktor engine, `BuildConfig` values (as one `NetworkConfig`), the Room database (built with the application `Context`), Apollo, Coil, Firebase Auth and Firebase AI, the auth, token and AI chat implementations, and the nine ViewModels.
- **Swap the Android entry points**: `startKoin { androidContext(...) }` in `MovieGalleryApplication` replaces `@HiltAndroidApp` and its injected `Provider<ImageLoader>`; `@AndroidEntryPoint` and `@HiltViewModel` / `@Inject` go away; `hiltViewModel()` becomes `koinViewModel()` at the same call sites. The session ViewModel stays scoped to the activity, and the movie detail ViewModel still reads `movieId` from its saved state.
- **Add graph tests**, because Koin resolves the graph at runtime:
  - two in `app/src/test`: Koin's `verify()` over the full graph for constructor parameters, and a Robolectric test that builds the real graph and resolves every use case, which catches the bindings that only a definition lambda asks for;
  - one in `:shared` `commonTest` that resolves the two qualified Ktor clients and checks which host and credentials each one sends.
- **Remove Hilt** from the build: the Hilt plugin, `hilt-android`, `hilt-android-compiler`, `hilt-navigation-compose`, and KSP from `:app` (Hilt was its only processor). Add Koin at 4.2.2, the latest stable release.
- **Update the docs and project rules** that describe Hilt: `CLAUDE.md`, the `android-architecture` skill and the design rule in `openspec/config.yaml` that names the `@TmdbClient` / `@BffClient` annotations.

Backends touched: none. TMDB, BFF, Countries GraphQL, Firebase AI and Firebase Auth are wired differently but their requests, headers, token handling and error mapping stay the same.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This swaps the DI library with no change in user-observable behaviour, so the change sets `skip_specs: true`. Every existing spec in `openspec/specs/` must still hold unchanged. The behaviours most exposed to a wiring mistake are the `app-navigation` start destination and sign-out, the `tv-shows` token requirements (which depend on the BFF client getting the Firebase token provider), and the `movie-detail` screen opening the movie that was tapped.

## Out of scope

- An iOS Koin entry point (`initKoin()`), iOS implementations of auth, AI or the database builder, or an iOS app. The shared modules only declare what a platform must supply.
- Moving the Room database builder or Apollo construction into `:shared`. Both stay in `:app`, unchanged.
- Moving ViewModels or any UI into `:shared`.
- Koin annotations, the Koin compiler plugin, or the 4.3.0 beta.
- Any change to an endpoint, header, timeout, scope (everything app-wide stays a singleton or stays unscoped as today), or user-visible text.

## Pre-production debt

None. No placeholder behaviour is introduced.

## Impact

- **Build:** `koin-core` (`:shared` `commonMain`), `koin-android` and `koin-compose-viewmodel` (`:app`) and `koin-test` (`:app` tests) are added to the catalog. The Hilt plugin, Hilt libraries and `hilt-navigation-compose` are removed, and `:app` no longer applies KSP. `:shared` keeps KSP for Room.
- **Code:**
  - New: `:shared` `di/` (shared modules and qualifiers) and `:app` `di/` Koin modules.
  - Deleted: every Hilt module in `:app` `di/` and the three qualifier annotations.
  - Edited: 9 ViewModels, 8 screens and `AppNavHost` (`koinViewModel()`), `MainActivity`, `MovieGalleryApplication`, and 5 `:app` data classes that lose `@Inject`.
- **Tests:** existing ViewModel and repository tests build their subjects directly and use no Hilt, so they don't change. Three new tests check the graph.
- **Users:** none. Stored data, requests and screens stay the same.
