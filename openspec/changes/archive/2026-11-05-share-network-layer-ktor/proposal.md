# Proposal

## Why

The iOS app needs the movie and TV-show data that only `:app` can reach today, because those calls go through Retrofit, and Retrofit runs only on the JVM. Ktor runs on Android and iOS. Swapping the TMDB and BFF stacks to Ktor lets the repositories, `safeApiCall`, the BFF token handling and the mappers move into `:shared`.

This is the riskiest step of the migration so far. It replaces the transport under every movie and TV-show screen, and the error mapping that decides which error state each screen shows. The Android app must behave exactly as it does today, and the existing tests have to prove that.

## What Changes

- **Make the tests independent of the transport first, while Retrofit is still in place.** Today most network tests build Retrofit or OkHttp objects themselves (`HttpException`, `OkHttpClient`, Retrofit builders), so they would be rewritten along with the code. They are rewritten first to work over real HTTP against a recorded fake server, and to assert `DataResult` outcomes at the API and repository boundary. They must pass on Retrofit before any production code changes.
- **Replace Retrofit with Ktor**, still in `:app`, on the OkHttp engine:
  - The OkHttp engine wraps the existing base client, so connections stay pooled with Coil and Apollo.
  - The rewritten tests must keep passing with no assertion changes. Only the helper that builds the API under test may change.
  - `safeApiCall` maps Ktor's failure types to the same `AppError`s.
  - The BFF token handling becomes a Ktor plugin with the same behaviour: the token is looked up fresh on every request, no request is sent without one, a 401 is retried once with a force-refreshed token, and only requests to the BFF host are retried.
- **Move the networking code into `:shared` `commonMain`**:
  - `TmdbApi`, `BffApi`, `safeApiCall`, the BFF auth plugin, the HTTP client factories, `MovieRepositoryImpl` and `TvShowRepositoryImpl`.
  - Android uses the OkHttp engine and iOS the Darwin engine.
  - Values that come from `BuildConfig` today (the TMDB token, the app version code, the debug flag) become factory parameters that `:app` passes in.
- **Move the tests into `:shared` `commonTest`**:
  - The fake server becomes Ktor's `MockEngine`, so the same assertions also run on the iOS simulator.
  - A small platform test on each of Android and iOS checks that a refused connection maps to `AppError.Network` with the real engine, which `MockEngine` can't check.
- **Return the four mappers to `internal`** and drop their `TEMP(share-core-kmp-module)` comments, since no `:app` code calls them any more.
- **Remove Retrofit and its converter.** Add the Ktor libraries at their latest stable release through the version catalog. OkHttp stays, for the Ktor Android engine, Coil and Apollo.
- **Update the docs and project rules** that describe the Retrofit stacks: `CLAUDE.md`, the `android-architecture` skill and the design rules in `openspec/config.yaml`.

Backends touched: TMDB and BFF. Their request URLs, headers, query parameters, token handling and error mapping stay the same. Countries GraphQL, Firebase AI and Firebase Auth: none.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a transport swap and a module move with no change in user-observable behaviour, so the change sets `skip_specs: true`. Every existing spec must still hold unchanged. In particular, these `tv-shows` requirements are protected by tests that survive all three steps: "every request carries the bearer token", "no request is sent without a token", "a rejected token is refreshed once" and "Retry looks up a fresh token". So are the `movie-browse`, `movie-search` and `movie-detail` error states.

## Out of scope

- Moving Apollo or Coil off OkHttp. Both keep using the shared base `OkHttpClient`.
- Replacing Hilt. Shared classes stay annotation-free, and `:app` creates them with `@Provides`.
- Firebase Auth and Firebase AI, including `AuthRepositoryImpl`, `FirebaseAuthTokenProvider`, `safeAuthCall` and `safeAiCall`. The Ktor swap doesn't unblock moving them.
- Moving ViewModels or any UI into `:shared`.
- An iOS app, Xcode project or framework export. iOS code is compiled and tested on the simulator only.
- Changing any endpoint, query parameter, header, timeout or user-visible error message.

## Pre-production debt

None. No placeholder behaviour is introduced, and this change removes the temporary public visibility of the mappers.

## Impact

- **Build:**
  - New catalog entries for Ktor: client core, the OkHttp and Darwin engines, logging and the mock engine. Also `kotlinx-coroutines-test` for `commonTest`.
  - `retrofit` and `retrofit-converter-kotlinx-serialization` are removed.
  - `:app` keeps OkHttp and `mockwebserver3`. `CountryRepositoryImplTest` still needs MockWebServer.
- **Code:**
  - `TmdbApi`, `BffApi`, `SafeApiCall`, `BffAuth`, and the movie and TV-show repositories move from `:app` to `:shared`, with their packages unchanged.
  - `NetworkModule` builds Ktor clients instead of Retrofit instances.
  - `RepositoryModule` loses two `@Binds`, and `SharedModule` gains two `@Provides @Singleton`.
  - `FakeTmdbApi` and `FakeBffApi` are deleted. Repository tests go through HTTP instead.
- **Tests:**
  - About 39 network tests are rewritten in the first step and then moved to `commonTest`.
  - The JSON fixtures that TMDB and BFF tests use become Kotlin string constants, because `commonTest` can't read classpath resources. The country fixtures stay in `:app`.
  - The mapper tests move to `commonTest`.
- **Users:** none. Requests, responses, stored data and error states stay the same.
