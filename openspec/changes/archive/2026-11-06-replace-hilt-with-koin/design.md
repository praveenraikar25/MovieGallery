# Design

## Context

See proposal.md for why. The current wiring, all in `:app` `di/`:

- **Hilt modules:** `NetworkModule`, `GraphQlModule`, `DatabaseModule`, `AuthModule`, `AiModule`, `ClockModule`, `DispatchersModule`, `RepositoryModule` (3 `@Binds`) and `SharedModule` (about 20 `@Provides` for `:shared` classes, which can't carry `@Inject`).
- **`@Inject` constructors:** the 9 ViewModels, plus `AuthRepositoryImpl`, `AiChatRepositoryImpl`, `FirebaseAuthTokenProvider`, `GeminiChatDataSource` and `PosterImageDecoder`.
- **Scopes:** only `@Singleton` and unscoped. No activity, ViewModel or assisted bindings.
- **Qualifiers:** `@TmdbClient`, `@BffClient`, `@IoDispatcher`, as `javax.inject` annotations.
- **`Context`:** used by the Room builder, Coil and `PosterImageDecoder`.
- **Entry points:** `MovieGalleryApplication` field-injects a `Provider<ImageLoader>` for Coil's `SingletonImageLoader.Factory`. `MainActivity` is `@AndroidEntryPoint`. Screens call `hiltViewModel()`. `AppSessionViewModel` is resolved as a default parameter of `AppNavHost`, outside the `NavHost`, so its owner is the activity. `MovieDetailViewModel` reads `movieId` from `SavedStateHandle`.
- **Tests:** no test uses Hilt. ViewModel and repository tests construct their subjects directly with fakes from `app/src/test/.../testutil/`.

Constraints:
- `commonMain` can't import `android.*`, `java.*` or `javax.*`.
- CLAUDE.md requires the latest stable Kotlin, so a DI tool that pins the Kotlin version is unwelcome.

## Goals / Non-Goals

**Goals:**
- One Koin graph, split so that everything buildable from multiplatform code is declared in `:shared` `commonMain`, and `:app` supplies only what is Android-, Firebase- or `BuildConfig`-specific.
- Keep Hilt's guarantee that a missing binding never reaches a user, moved from compile time to `./gradlew test`.
- Keep every lifetime the same: Hilt `@Singleton` becomes `single`, unscoped becomes `factory`.

**Non-Goals:**
- Building the iOS half of the graph. The shared modules document what a platform must supply; nothing on iOS supplies it yet.
- Changing how any object is constructed: same OkHttp base client, same engine wrapping, same Room builder, same Apollo setup, same Firebase model options.

## Decisions

### 1. Plain Koin DSL, no annotations or compiler plugin

Definitions are written by hand with `module { }`. Use `singleOf(::X) { bind<I>() }` / `factoryOf(::X)` / `viewModelOf(::X)` where a constructor needs no qualified parameter. Use an explicit lambda (`single<I> { X(get(), get(IoDispatcher)) }`) where it does: `MovieRepositoryImpl`, `TvShowRepositoryImpl`, `CountryRepositoryImpl` and `AiChatRepositoryImpl` all take the IO dispatcher, and `TmdbApi` / `BffApi` take a qualified client.

*Alternatives:*
- **Koin compiler plugin (1.2.3):** compile-time checks, but it is a Kotlin compiler plugin, so every Kotlin upgrade waits for a compatible release.
- **Metro:** compile-time like Dagger, but has the same Kotlin coupling.

The graph is about 40 bindings, half of them third-party objects that need hand-written lambdas either way, so the plugin would save little.

### 2. Module layout and the platform contract

```
shared/commonMain  com.raikar.moviegallery.di
  Qualifiers.kt   TmdbClient, BffClient, IoDispatcher
  SharedModules.kt
    networkModule   single(TmdbClient) HttpClient, single(BffClient) HttpClient, TmdbApi, BffApi
    dataModule      Movie/TvShow/Watchlist/Country repositories (single, bound to interface)
                    WatchlistDao (factory, from MovieGalleryDatabase)
                    Clock.System (factory), Dispatchers.IO (factory, IoDispatcher)
    domainModule    16 x factoryOf(::XUseCase)
    val sharedModules = listOf(networkModule, dataModule, domainModule)

app  com.raikar.moviegallery.di
  AndroidModules.kt
    androidPlatformModule
      OkHttpClient (single, base, 15s timeouts)
      HttpClientEngine (factory: OkHttp.create { preconfigured = get() })
      NetworkConfig (single, from BuildConfig)
      MovieGalleryDatabase (single, Room.databaseBuilder(androidContext(), ...))
      ApolloClient (single), ImageLoader (single)
      FirebaseAuth, GenerativeModel (single)
      AuthRepository, AuthTokenProvider, AiChatRepository (single, bound to interface)
      GeminiChatDataSource, PosterImageDecoder (single: both are @Singleton today)
    viewModelModule   9 x viewModelOf(::XViewModel)
    val androidModules = listOf(androidPlatformModule, viewModelModule)
```

The KDoc on `sharedModules` lists what a platform must provide: `HttpClientEngine`, `NetworkConfig`, `MovieGalleryDatabase`, `ApolloClient`, `AuthRepository`, `AuthTokenProvider` and `AiChatRepository`. That list is the contract a future iOS `initKoin()` has to meet.

The file names and module boundaries above are a starting point. What matters is the split between `:shared` and `:app`.

- **Engine as `factory`.** Today `engineOn(base)` makes a new engine per Ktor client, all wrapping one `OkHttpClient`. A `single` engine would be shared by both clients, and closing one would break the other.
- **Room and Apollo stay in `:app`.** Building Room in `commonMain` would require a platform-supplied `RoomDatabase.Builder` and a driver decision. Building Apollo there would require replacing the OkHttp transport and logging interceptor. Both are iOS-phase work. `:shared` only turns the database into a DAO.
- **`Dispatchers.IO` in `commonMain`.** It is available on native through `kotlinx.coroutines.IO`, so the dispatcher binding can be shared.
- **The `COUNTRIES_URL` constant.** It moves with Apollo into the `:app` module file.
- **The comments move with the definitions.** The comments in today's Hilt modules carry real constraints: no token on the base client, the model choice and timeout, no destructive Room fallback. Each one moves next to its definition.

### 3. Qualifiers are named constants in `:shared`

```kotlin
val TmdbClient: Qualifier = named("TmdbClient")
val BffClient: Qualifier = named("BffClient")
val IoDispatcher: Qualifier = named("IoDispatcher")
```

The names and security KDoc stay. Call sites use the constants, never the raw strings, so a typo doesn't compile.

*Alternative:* `named<MarkerType>()` with empty marker objects. It adds three types for no extra safety.

### 4. `NetworkConfig` carries the `BuildConfig` values

A small data class in `:shared` `data/remote/` holds `tmdbAccessToken`, `appVersion`, `logLevel` and `logger`. `:app` builds it once from `BuildConfig`: `DEBUG` → `LogLevel.ALL`, else `NONE`, plus `Logger.ANDROID`. The shared `networkModule` passes its fields to `createTmdbHttpClient` / `createBffHttpClient`, whose signatures don't change.

Its `toString()` is overridden to redact the token, so a logged or crash-reported config never prints it.

*Alternative:* four separate bindings (a qualified `String` for the token, and so on). That makes the graph harder to read, and a bare `String` binding is easy to resolve by mistake.

### 5. Android entry points

- **`MovieGalleryApplication.onCreate`** calls `startKoin { androidContext(this@MovieGalleryApplication); modules(sharedModules + androidModules) }` before `installAppCheck()`.
- **Coil's loader.** `newImageLoader` resolves the `ImageLoader` from Koin (`get()`). It runs on the first image load, after `onCreate`, so the loader still lives in a DI module rather than in the Application. The `Provider` and its KDoc go.
- **`MainActivity`** loses `@AndroidEntryPoint`. Koin needs nothing on the activity.
- **ViewModels** lose `@HiltViewModel` and `@Inject`. Screens call `koinViewModel()` from `koin-compose-viewmodel` (`org.koin.compose.viewmodel`). That is the multiplatform artifact, so the screens won't change again if ViewModels move to `:shared` later.

*Alternative:* `koin-androidx-compose`'s Android-only `koinViewModel()`. It behaves the same today but would be swapped later.

### 6. ViewModel ownership and `SavedStateHandle`

`koinViewModel()` defaults its owner to `LocalViewModelStoreOwner.current`, the same default as `hiltViewModel()`:
- **`AppSessionViewModel`** stays a default parameter of `AppNavHost`, outside the `NavHost`, so it is still owned by the activity. That is what keeps `startRoute` computed once, and what keeps the ViewModel alive through sign-out's `popUpTo(0)`. Its KDoc gains a line saying it must be resolved outside the `NavHost`.
- **`MovieDetailViewModel`** keeps `SavedStateHandle` as a constructor parameter. Koin builds it from the back-stack entry's `CreationExtras`, which carry the `movieId` route argument, the same source Hilt used.

### 7. Three graph tests, because `verify()` alone misses most gaps

**Constructor check.** `app/src/test/.../di/KoinGraphTest.kt` runs Koin's `verify()` over a module that includes `sharedModules + androidModules`.

- `verify()` inspects constructor types by reflection and creates nothing, so it needs no Robolectric and doesn't start Firebase. It is JVM-only, which is why it lives in `app/src/test`.
- `extraTypes` holds `Context` and `SavedStateHandle`, which the running app supplies (`androidContext()` and the navigation entry). It also holds `FirebaseAuth`'s own constructor parameters (`FirebaseApp`, `Provider`, `Executor`, `ScheduledExecutorService`): `verify()` walks into the constructor of every bound type, but `FirebaseAuth.getInstance()` builds it, never Koin.
- Any other type it reports must be either a real missing binding, which gets fixed, or a type genuinely supplied from outside, which gets added to `extraTypes` with a comment. Never add one just to make the test pass.

**Its limit, found during implementation:** `verify()` only checks the parameters of classes registered by constructor reference (`singleOf`, `factoryOf`, `viewModelOf`). It can't see a `get()` inside a definition lambda. Deleting the `AuthTokenProvider` binding, which only the BFF client's lambda asks for, still passed it. Lambdas build both Ktor clients, the four repositories that take the IO dispatcher, Room, Apollo and the Firebase objects, so on its own it misses most of the gaps this migration could introduce.

**Resolution check.** `app/src/test/.../di/KoinGraphResolutionTest.kt` (Robolectric, `@Config(application = Application::class)` like the Room tests) builds an isolated `koinApplication { }` with the real modules and a Robolectric application context. It overrides only `FirebaseAuth` and `GenerativeModel` with MockK fakes, because both need a configured `FirebaseApp`. Then it resolves all 16 use cases and the `ImageLoader`. That runs every definition lambda, so it builds every repository, API, client, the database, Apollo and the Firebase-backed implementations. Creating them opens no connection and no database file.

ViewModels aren't resolved, because several start loading in `init`. `verify()` covers their constructors. With the `AuthTokenProvider` binding deleted, this test fails on `GetTopTvShowsUseCase`.

**Qualified-client check.** Neither test above checks *which* qualified client a lambda asks for, so swapping `TmdbClient` and `BffClient` would get past both. `shared/src/commonTest/.../di/SharedModulesTest.kt` builds an isolated `koinApplication { }` with `networkModule` plus a test module supplying a `MockEngine` factory, a fake `AuthTokenProvider` and a `NetworkConfig` with a known token. Then it checks:
- a call through `TmdbApi` goes to the TMDB host with the TMDB bearer token;
- a call through `BffApi` goes to the BFF host with the fake ID token and **not** the TMDB token.

It runs on the JVM and the iOS simulator. With `TmdbApi` wired to `BffClient`, it fails.

All three use `koinApplication { }` or a bare `verify()`, never `startKoin`, so no global Koin state leaks between tests.

### 8. Existing tests and test utilities stay as they are

ViewModel tests construct ViewModels directly, so removing the annotations changes nothing for them. `koin-test` is added only for `verify()`. Nothing else in the suite starts Koin.

## Risks / Trade-offs

- **A missing binding fails at runtime, not compile time** → the constructor check and the resolution check both run in `./gradlew test`, part of every change's verification tasks. Between them they cover constructor parameters and every `get()` inside a definition lambda. The manual check opens every screen once.
- **A qualifier mix-up leaks the TMDB token to the BFF host, or sends BFF calls without the ID token** → the `commonTest` check above asserts host and `Authorization` for both clients.
- **`AppSessionViewModel` is later moved inside the `NavHost`**, silently scoping it to an entry, so sign-out recreates it with a stale `startRoute` → a KDoc rule on the class and `AppNavHost`. The `app-navigation` sign-out scenario is part of the manual check.
- **`movieId` is missing from `SavedStateHandle` under Koin** → `checkNotNull` crashes Movie Detail immediately, so the manual check (open a movie from Home and from Search) catches it before merge.
- **`verify()` is marked experimental in Koin 4.x** → the opt-in annotation goes on the test only. If the API changes in a later Koin, only this test changes.
- **A singleton that Hilt created on first use is created at a different time** → Koin `single` is also lazy by default, and no definition uses `createdAtStart`, so creation timing stays the same.
- **The token appears in a logged `NetworkConfig`** → `toString()` redacts it (decision 4).

## Migration Plan

Two checkpoints, each a commit with `./gradlew test` passing:

1. **Shared modules, unused.** Add Koin to the catalog, add `:shared` `di/` (qualifiers, modules, `NetworkConfig`) and the `commonTest` client check. Hilt still wires the app, so the app's behaviour is untouched. Also run `:shared:iosSimulatorArm64Test`.
2. **Cut over.** Add the `:app` Koin modules and the `verify()` test, switch the entry points, ViewModels and screens, delete every Hilt module and qualifier annotation, remove Hilt and KSP from `:app`, then update the docs.

There is no period with both DI systems wiring `:app`: they would hold separate singleton instances of the same Firebase and HTTP objects.

**Rollback:** revert the commits. Stored data is untouched, so nothing needs migrating back.
