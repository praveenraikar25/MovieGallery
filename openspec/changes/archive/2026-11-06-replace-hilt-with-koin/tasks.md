# Tasks

Two checkpoints (design, Migration Plan). Each group ends with all tests passing and its own commit, made only once the user says to commit. Run `./gradlew ktlintFormat` before each commit; the pre-commit hook checks the result.

## 1. Checkpoint 1: the shared Koin modules, not yet used by the app

Hilt still wires `:app` throughout this group, so app behaviour can't change.

- [x] 1.1 Add Koin to `gradle/libs.versions.toml`: a `koin` version of `4.2.2` (the latest stable; check Maven Central for a newer stable release first) and library entries for `koin-core`, `koin-android`, `koin-compose-viewmodel` and `koin-test`, each on `version.ref = "koin"` (no BOM: one version ref does the same job and avoids platform dependencies in a KMP source set). Add `koin-core` to `:shared` `commonMain` as `api`, since `:app` consumes the shared modules. Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileAndroidMain` passes. Use the actual compile task name reported by `./gradlew :shared:tasks --all`.
- [x] 1.2 Add `NetworkConfig` in `:shared` `data/remote/` (design decision 4), with `tmdbAccessToken`, `appVersion`, `logLevel` and `logger`, and a `toString()` that redacts the token. Add a `commonTest` test that the token never appears in `toString()`. Verify: `./gradlew :shared:testAndroidHostTest` passes.
- [x] 1.3 Add `:shared` `commonMain` `com/raikar/moviegallery/di/Qualifiers.kt` with the `TmdbClient`, `BffClient` and `IoDispatcher` qualifier constants (design decision 3). Carry over the KDoc from `:app`'s qualifier annotations, including the rule that the TMDB token never reaches the BFF client or the base `OkHttpClient`. Verify: it compiles for iOS, as in 1.1.
- [x] 1.4 Add `:shared` `commonMain` `di/SharedModules.kt` (design decision 2):
  - `networkModule`: the two qualified `HttpClient` singles built with `createTmdbHttpClient` / `createBffHttpClient` from `NetworkConfig`, an injected `HttpClientEngine` and `AuthTokenProvider`, plus `TmdbApi` / `BffApi` on the right qualifier.
  - `dataModule`: the four repositories as singles bound to their interfaces, using explicit lambdas wherever `get(IoDispatcher)` is needed; `WatchlistDao` as a factory from `MovieGalleryDatabase`; `Clock.System` and the qualified `Dispatchers.IO` as factories.
  - `domainModule`: all 16 use cases as `factoryOf`.
  - `sharedModules`, with KDoc listing every type a platform must supply.

  Verify: it compiles for both Android and iOS, as in 1.1, with no `android.*`, `java.*` or `javax.*` imports.
- [x] 1.5 Add `shared/src/commonTest/.../di/SharedModulesTest.kt` (design decision 7), using an isolated `koinApplication { }` with `networkModule`, a `MockEngine` factory, a fake `AuthTokenProvider` and a `NetworkConfig` with a known token. Assert:
  - `TmdbApi` hits the TMDB host with `Bearer <tmdb token>`;
  - `BffApi` hits the BFF host with `Bearer <fake id token>` and the TMDB token appears in none of its headers.

  Test names may not contain `,` `.` `:` `;`. Verify: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test` passes.
- [x] 1.6 Checkpoint 1 gate: `./gradlew ktlintCheck test assembleDebug`. Verify: everything passes, and `git diff --stat` shows changes only in `:shared` and `gradle/libs.versions.toml`.

## 2. Checkpoint 2: cut `:app` over to Koin and remove Hilt

- [x] 2.1 Add `:app` `di/AndroidModules.kt` (design decisions 2, 4 and 6):
  - `androidPlatformModule`: the base `OkHttpClient`; the `HttpClientEngine` as a **factory** wrapping it; `NetworkConfig` from `BuildConfig`; `MovieGalleryDatabase` via `Room.databaseBuilder(androidContext(), ...)`; `ApolloClient` with `COUNTRIES_URL`; Coil's `ImageLoader`; `FirebaseAuth`; the `GenerativeModel` with its model name, system prompt and timeout; `AuthRepositoryImpl`, `FirebaseAuthTokenProvider` and `AiChatRepositoryImpl` bound to their interfaces; `GeminiChatDataSource` and `PosterImageDecoder` as singles (both are `@Singleton` classes today). The AI bindings stay in their own `aiModule` (in `AiModule.kt`, with the model name, system prompt and timeout), loaded as part of `androidModules`.
  - `viewModelModule`: the 9 ViewModels via `viewModelOf`.
  - `androidModules`.

  Move each comment from the old Hilt modules to its definition. That includes the no-token-on-the-base-client note, the model choice and timeout notes, and the no-destructive-fallback note. Verify: `./gradlew :app:compileDebugKotlin` passes.
- [x] 2.2 Remove `@Inject` / `javax.inject` from `AuthRepositoryImpl`, `AiChatRepositoryImpl`, `FirebaseAuthTokenProvider`, `GeminiChatDataSource` and `PosterImageDecoder`, along with the class-level `@Singleton` and the `@ApplicationContext` and `@IoDispatcher` parameter annotations, and collapse the leftover `class X⏎constructor(` to `class X(`. Verify: `grep -rn "javax.inject\|dagger" app/src/main/java/com/raikar/moviegallery/data` returns nothing.
- [x] 2.3 Remove `@HiltViewModel` and `@Inject` from the 9 ViewModels, keeping their constructors unchanged (`MovieDetailViewModel` keeps `SavedStateHandle`). Verify: the existing ViewModel tests pass unchanged under `./gradlew :app:testDebugUnitTest`.
- [x] 2.4 Replace `hiltViewModel()` with `koinViewModel()` from `org.koin.compose.viewmodel` in the 8 screens and in `AppNavHost`, keeping the `AppNavHost` default parameter outside the `NavHost`. Add the KDoc rule to `AppSessionViewModel`: resolve it outside the `NavHost` so the activity owns it (design decision 6). Verify: `grep -rn hiltViewModel app/src` returns nothing.
- [x] 2.5 In `MovieGalleryApplication`, replace `@HiltAndroidApp` and the injected `Provider<ImageLoader>` with `startKoin { androidContext(...); modules(sharedModules + androidModules) }` in `onCreate`, before `installAppCheck()`, and resolve the `ImageLoader` from Koin in `newImageLoader`. Remove `@AndroidEntryPoint` from `MainActivity`. Verify: `./gradlew :app:compileDebugKotlin` passes.
- [x] 2.6 Delete every Hilt module in `app/.../di/` (`NetworkModule`, `GraphQlModule`, `DatabaseModule`, `AuthModule`, `AiModule`, `ClockModule`, `DispatchersModule`, `RepositoryModule`, `SharedModule`) and `di/qualifier/`. In the build:
  - remove the Hilt plugin, `hilt-android`, `hilt-android-compiler` and `hilt-navigation-compose` from `app/build.gradle.kts`, the root build file and the catalog;
  - remove the KSP plugin from `:app` only (`:shared` keeps it for Room).
  - add `koin-android` and `koin-compose-viewmodel` to `:app`, and `koin-test` to `:app`'s tests.

  Verify: `grep -rni "hilt\|dagger" app/src gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts` returns nothing, and `./gradlew assembleDebug` passes.
- [x] 2.7 Add the two `:app` graph tests (design decision 7):
  - `app/src/test/.../di/KoinGraphTest.kt`: `verify()` over a module including `sharedModules + androidModules`. Its `extraTypes` are `Context`, `SavedStateHandle` and `FirebaseAuth`'s own constructor parameters, each with a comment. The Koin experimental opt-in goes on the test only.
  - `app/src/test/.../di/KoinGraphResolutionTest.kt` (Robolectric): the real modules with a Robolectric context and only `FirebaseAuth` / `GenerativeModel` faked. Resolve all 16 use cases and the `ImageLoader`.

  Verify: both pass; temporarily deleting the `AuthTokenProvider` binding makes the resolution test fail (`verify()` alone doesn't catch it); and temporarily wiring `TmdbApi` to `BffClient` makes `SharedModulesTest` fail.
- [x] 2.8 Update the docs:
  - `CLAUDE.md`: the tech-stack DI line, the `:app` repo-map line, the `ui/screens` and `di/` map entries, "Adding a feature screen" steps 4–6 and "Adding a GraphQL query" step 3, so new classes are registered in the shared or Android Koin modules with no `@Provides` / `@Binds` / `@HiltViewModel`.
  - `.claude/skills/android-architecture/SKILL.md`: its description, section 2 (rewritten for Koin: the plain-DSL rule, the shared/platform split, qualifiers, `koinViewModel()` and the graph tests) and the `:app` line in section 3.
  - `openspec/config.yaml`: the design rule naming `@TmdbClient` / `@BffClient` now names the Koin qualifiers.
  - `README.md`: the DI line and the `di/` entry in the project tree.

  Verify: `grep -rni "hilt\|@Provides\|@Binds\|SharedModule" CLAUDE.md .claude/skills/android-architecture openspec/config.yaml` returns nothing.
- [x] 2.9 Manual check on an emulator with a debug build:
  - cold start lands on Login when signed out and on Home when signed in;
  - sign in;
  - Home, Search and Watchlist load, with posters;
  - open a movie from Home and from Search, and the right movie shows; rotate on Movie Detail;
  - toggle the watchlist;
  - open a production-country sheet;
  - Top TV Shows load (BFF with the ID token);
  - AI chat sends a text message;
  - Profile shows the signed-in user;
  - sign out returns to Login, and back does not re-enter the app.

  Verify: each item behaves as before, and Logcat shows no Koin `NoDefinitionFoundException`.
- [x] 2.10 Checkpoint 2 gate: `./gradlew ktlintCheck test assembleDebug` and `./gradlew :shared:iosSimulatorArm64Test`. Verify: everything passes.

## Workflow follow-up

- Archive the change once it is reviewed and merged, as with phases 1 and 2.
