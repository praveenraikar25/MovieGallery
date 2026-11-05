# MovieGallery

Android app (package `com.raikar.moviegallery`), moving to Kotlin Multiplatform: `:app` is the Android application, `:shared` is a KMP library (Android, `iosArm64`, `iosSimulatorArm64`) holding the code both platforms will use. No iOS app consumes it yet.

## Tech Stack

Always use the latest stable Android tech stack. Concretely:

- **Kotlin & AGP**: target the latest stable Kotlin and Android Gradle Plugin versions. `gradle/libs.versions.toml` is the single source of truth for all dependency/plugin versions — add new versions there, don't hardcode them in module `build.gradle.kts` files.
- **UI**: Jetpack Compose with Material 3 for all UI. Do not add new XML layouts or View-based Activities/Fragments.
- **DI**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Module`/`@InstallIn`).
- **Networking/async**: Ktor for REST calls (OkHttp engine on Android, Darwin on iOS), Apollo Kotlin for GraphQL, Kotlin Coroutines/Flow for async data. Repositories expose main-safe suspend functions / `Flow`; ViewModels expose `StateFlow`.
- **Persistence**: Room 3 (`androidx.room3`, not Room 2's `androidx.room`) for on-device data. DAOs return `Flow` / suspend; schemas are exported to `shared/schemas/` and committed, and there is no destructive fallback — a schema change needs a real `Migration`.
- **Dependencies**: when adding any new library, use its latest stable release and add it via the version catalog.

## Code style

Kotlin formatting is enforced by ktlint (`org.jlleitschuh.gradle.ktlint`), configured via the root `.editorconfig` (`ktlint_official` style, 120-column limit).

- `./gradlew ktlintCheck` — verify; `./gradlew ktlintFormat` — auto-fix.
- **Once per clone**: run `./gradlew installGitHooks` to point git at `.githooks/`. The `pre-commit` hook then formats staged Kotlin, re-stages the fixes, and blocks the commit on anything ktlint can't auto-fix. Bypass with `SKIP_KTLINT=1 git commit` or `--no-verify`.

For layering and module structure (Clean Architecture: UI / Domain / Data, multi-module strategy), see `.claude/skills/android-architecture/SKILL.md` — keep that skill and this file in sync rather than duplicating details here.

## Commands

- `./gradlew assembleDebug` — build; `./gradlew test` — all JVM unit tests (no instrumented tests of note). Room DAO/repository tests run under Robolectric against a real database (`testutil/TestDatabases.kt`), with `@Config(application = Application::class)` so the app's Firebase setup isn't started.
- Single test: `./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.ui.screens.home.HomeViewModelTest"` (append `.methodName` for one method). Shared-module tests: `./gradlew :shared:testAndroidHostTest --tests "…"`.
- `./gradlew test` includes `:shared`'s JVM tests (its `test` task wraps `testAndroidHostTest`). iOS runs separately: `./gradlew :shared:iosSimulatorArm64Test` (needs Xcode and an iOS simulator runtime).
- `:shared` common code must compile for iOS: no `android.*`, `java.*` or `javax.*` imports in `commonMain`/`commonTest`, and test names can't contain `,` `.` `:` `;` (Kotlin/Native rejects them). Check with `./gradlew :shared:compileKotlinIosSimulatorArm64`.

## Build prerequisites

- `app/google-services.json` must exist — the google-services plugin is applied unconditionally, so the build fails without it.
- TMDB v4 token comes from the `TMDB_READ_ACCESS_TOKEN` env var, else `tmdb.readAccessToken` in `local.properties` (git-ignored; do not read or edit it — denied in `.claude/settings.json`). It is injected as a `BuildConfig` field via the variant API (not `buildConfigField`, removed in AGP 10). Without it the build still works but TMDB calls return 401.
- AI chat additionally needs Firebase AI Logic + an App Check debug token; other features don't.
- Sign-in needs the Email/Password and Google providers enabled in the Firebase console, your debug SHA-1 registered, and `app/google-services.json` re-downloaded so it carries an OAuth web client (the google-services plugin turns it into `default_web_client_id`).

## Backends

`NetworkModule` provides two Ktor `HttpClient`s selected by qualifier, both on the OkHttp engine over the shared base `OkHttpClient`: `@TmdbClient` (`TmdbApi`, movies, bearer-token auth) and `@BffClient` (`BffApi`, TV shows, a separate demo host). They are built by `createTmdbHttpClient` / `createBffHttpClient` (`:shared` `data/remote/HttpClients.kt`), which take the token, app version and log level as parameters instead of reading `BuildConfig`. The BFF client installs the `BffAuth` plugin (`data/remote/auth/`): a fresh Firebase ID token on every request, no request without one, and one force-refreshed retry on a 401 from the BFF host. New endpoints must pick the right client; base URLs need a trailing slash and request paths have no leading slash. Wrap calls in `safeApiCall`, which maps Ktor failures to `AppError`s. Authentication is a third, non-HTTP-client backend: Firebase Auth, reached through `AuthRepository` and mapped to `AuthError` by `safeAuthCall` (`data/remote/auth/`). The Google ID token comes from Credential Manager in the UI layer (`ui/screens/login/GoogleSignIn.kt`) because it needs an Activity; only the token string crosses into the repository.

The fourth backend is GraphQL: the public, unauthenticated Countries API, reached through the single `ApolloClient` in `GraphQlModule` (derived from the shared base `OkHttpClient`; no qualifier while it is the only Apollo client). Its schema and `.graphql` operations live in `shared/src/commonMain/graphql/countries/`, and Apollo generates typed models into `data/remote/graphql/countries` at build time — they stay in the data layer. Apollo returns failures on the response instead of throwing, so wrap calls in `safeGraphQlCall`, which maps them to the same `AppError`s as `safeApiCall`. `CountryRepository` backs the production-country details sheet on Movie Detail.

## Repo map

Source lives in two modules, with the same `com.raikar.moviegallery.*` packages in both:

- `:shared` — `shared/src/commonMain/kotlin/com/raikar/moviegallery/` (per-platform `actual`s in `androidMain/` and `iosMain/`): all of `domain/`; from `data/`, the DTOs and mappers, `TmdbGenres`/`TmdbImages`, `local/` (Room), the Countries GraphQL code, the Ktor networking (`TmdbApi`, `BffApi`, the client factories, `BffAuth`, `safeApiCall`), and the movie, TV-show, watchlist and country repositories. Tests: `shared/src/commonTest/` (plain Kotlin, `kotlin.test`, runs on JVM and iOS; network tests use the `MockEngine`-backed `FakeHttpServer` and `testutil/TestBackends.kt`, with JSON fixtures as strings in `data/remote/TestFixtures.kt`) and the JVM-only `androidHostTest/` and iOS-only `iosTest/` (each with a real-engine `RealEngineNetworkErrorTest`).
- `:app` — `app/src/main/java/com/raikar/moviegallery/`: all UI, `di/`, and the data code still tied to the JVM or Android — `auth/` (Firebase Auth), `ai/`, and the auth and AI repositories. Tests needing Robolectric, MockWebServer, MockK or classpath JSON fixtures stay in `app/src/test/`.

The per-package map below applies across both modules:

- `ui/screens/<feature>/` — one package per screen: `<Feature>Screen.kt` (Compose UI), `<Feature>ViewModel.kt` (`@HiltViewModel`, exposes `StateFlow`), `<Feature>UiState.kt`. Existing features: `home`, `search`, `detail`, `watchlist`, `profile`, `login`, `chat`, `tvshows`.
- `ui/navigation/` — `Destinations.kt` / `Routes` (route constants), `AppNavHost.kt` (`NavHost` graph), `AppSessionViewModel.kt` (start route from the persisted session, sign-out), `MainScaffold.kt` (bottom nav / tab scaffold for top-level tabs).
- `ui/components/` — shared Compose widgets (cards, chips, buttons, state views) reused across screens.
- `ui/theme/` — Material 3 theme, color, type, shape, dimens.
- `domain/model/` — plain data classes shared across layers.
- `domain/repository/` — repository interfaces (`<Thing>Repository`), implemented in `data/repository/`.
- `domain/usecase/` — one class per use case, invoked from a ViewModel; wraps a repository call.
- `data/remote/` — `auth/` (`safeAuthCall`, Firebase Auth error mapping), `TmdbApi.kt` / `BffApi.kt` (Ktor calls), `HttpClients.kt` (client factories, `networkJson`), `dto/` (network DTOs), `MovieMappers.kt` (DTO → domain mapping), `SafeApiCall.kt` (network error wrapping), `ai/` (Firebase AI client), `SafeGraphQlCall.kt` + `CountryMappers.kt` (GraphQL error wrapping and Apollo model → domain mapping; generated Apollo code lands in `graphql/countries`).
- `data/repository/` — repository implementations (`<Thing>RepositoryImpl.kt`), or a static/in-memory impl where there's no backing API yet. Shared ones (`WatchlistRepositoryImpl`, `CountryRepositoryImpl`) take plain constructor params, with no annotations.
- `data/local/` — the Room database (`MovieGalleryDatabase`), its DAOs, entities, column converters and entity ↔ domain mappers. The watchlist lives here, keyed by the signed-in account's Firebase uid.
- `di/` — Hilt modules: `NetworkModule` (base `OkHttpClient`, the two Ktor `HttpClient`s and APIs, Coil `ImageLoader`), `RepositoryModule` (`@Binds` interface → impl), `DispatchersModule`, `AiModule`, `GraphQlModule` (`ApolloClient`), `DatabaseModule` (Room database + DAOs), `ClockModule` (`kotlin.time.Clock`, injected so tests control timestamps), `SharedModule` (`@Provides` for every `:shared` class Hilt hands out: use cases and shared repositories — `commonMain` can't use `@Inject`). `di/qualifier/` holds `@Qualifier` annotations.

## Adding a feature screen

Touch these in order:

1. `domain/model/` (`:shared` `commonMain`) — add/extend the domain model if needed.
2. `domain/repository/<Thing>Repository.kt` (`:shared`) — interface with suspend/`Flow` methods.
3. `data/repository/<Thing>RepositoryImpl.kt` — implementation (or a static impl if there's no API yet). In `:shared` if it only needs multiplatform libraries; in `:app` if it needs Firebase or Android APIs.
4. `:app` `di/` — `@Binds` it in `RepositoryModule.kt` for an `:app` impl (`@Inject` constructor), or add a `@Provides @Singleton` to `SharedModule.kt` for a `:shared` impl.
5. `domain/usecase/` (`:shared`) — one use case class wrapping the repository call the screen needs, with no annotations, plus a `@Provides` for it in `SharedModule.kt`.
6. `ui/screens/<feature>/` — `<Feature>UiState.kt`, `<Feature>ViewModel.kt` (`@HiltViewModel`, injects the use case, exposes `StateFlow<UiState>`), `<Feature>Screen.kt` (Compose UI, `hiltViewModel()`).
7. `ui/navigation/Destinations.kt` — add a route constant.
8. `ui/navigation/AppNavHost.kt` — register the composable route.
9. `ui/navigation/MainScaffold.kt` — add a tab/nav entry if it's a top-level destination.
10. `app/src/test/java/.../ui/screens/<feature>/` — ViewModel test using a fake repository from `app/src/test/java/.../testutil/`.

## Adding a TMDB endpoint

1. `data/remote/dto/` (`:shared`) — response DTO(s) matching the TMDB JSON shape.
2. `data/remote/TmdbApi.kt` (`:shared`) — new method: `client.get("path") { parameter(...) }.decode()`.
3. `data/remote/MovieMappers.kt` (`:shared`) — `internal` DTO → domain model mapping function.
4. `domain/repository/` + `data/repository/...Impl.kt` (both `:shared`) — expose the call through the repository layer (wrap with `safeApiCall`).
5. `shared/src/commonTest/.../data/remote/` — add a fixture string to `TestFixtures.kt` and a test against `FakeHttpServer` (see `TmdbApiTest.kt`).

## Adding a GraphQL query

1. `shared/src/commonMain/graphql/countries/` — add a `.graphql` operation; the build validates it against the committed `schema.graphqls` (refresh with `./gradlew :shared:downloadCountriesApolloSchemaFromIntrospection`; regenerate models with `./gradlew :shared:generateCountriesApolloSources`).
2. `data/remote/` (`:shared`) — map the generated `…Query.Data` to a domain model (see `CountryMappers.kt`).
3. `domain/repository/` + `data/repository/...Impl.kt` (both `:shared`) — call `apollo.query(...).execute()` inside `safeGraphQlCall` on an injected dispatcher, and provide it from `SharedModule.kt` with the `@IoDispatcher` one.
4. Tests — a mapper test with generated models in `shared/src/commonTest/`, and in `app/src/test/java/.../data/` a repository test running a real `ApolloClient` against `MockWebServerRule` with a JSON fixture (see `CountryRepositoryImplTest.kt`).
