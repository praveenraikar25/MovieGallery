---
name: android-architecture
description: Expert guidance on setting up and maintaining a modern Android application architecture using Clean Architecture and Koin. Use this when asked about project structure, module setup, or dependency injection.
---

# Android Modern Architecture & Modularization

## Instructions

When designing or refactoring an Android application, adhere to the **Guide to App Architecture** and **Clean Architecture** principles.

### 1. High-Level Layers
Structure the application into three primary layers. Dependencies must strictly flow **inwards** (or downwards) to the core logic.

*   **UI Layer (Presentation)**:
    *   **Responsibility**: Displaying data and handling user interactions.
    *   **Components**: Activities, Fragments, Composables, ViewModels.
    *   **Dependencies**: Depends on the Domain Layer (or Data Layer if simple). **Never** depends on the Data Layer implementation details directly.
*   **Domain Layer (Business Logic) [Optional but Recommended]**:
    *   **Responsibility**: Encapsulating complex business rules and reuse.
    *   **Components**: Use Cases (e.g., `GetLatestNewsUseCase`), Domain Models (pure Kotlin data classes).
    *   **Pure Kotlin**: Must NOT contain any Android framework dependencies (no `android.*` imports).
    *   **Dependencies**: Depends on Repository Interfaces.
*   **Data Layer**:
    *   **Responsibility**: Managing application data (fetching, caching, saving).
    *   **Components**: Repositories (implementations), Data Sources (Ktor HTTP APIs, the Apollo GraphQL client, Room DAOs). Generated Apollo models stay in the data layer and are mapped to domain models there.
    *   **Dependencies**: Depends only on external sources and libraries.
    *   **Local persistence**: Room 3 (`androidx.room3`). Persisted data uses Room as the single source of truth — DAOs expose `Flow` for reads and suspend functions for writes, and a check-then-write runs in one `@Transaction`. Export schemas (`shared/schemas/`), commit them, and add a `Migration` per version bump instead of a destructive fallback, registered once in the shared `MIGRATIONS` list so both platforms run the same migrations. The database is built in `:shared`: a shared `applySharedConfig()` on top of a per-platform builder (Android: the application `Context`, same file name and default driver; iOS: a file in Application Support on `NativeSQLiteDriver`, with iOS binaries linking `-lsqlite3`), provided by the shared Koin `databaseModule`. Per-account data is keyed by the signed-in user's id.

### 2. Dependency Injection with Koin
Use **Koin** for all dependency injection, with the **plain DSL only**: no Koin annotations and no Koin compiler plugin, so Kotlin upgrades never wait on a DI release. Classes carry no DI annotations at all; they declare dependencies as ordinary constructor parameters.

*   **Where definitions live**: anything buildable from multiplatform code is declared in `:shared` `commonMain` `di/SharedModules.kt` (`networkModule`, `dataModule`, `domainModule`, `viewModelModule`, collected in `sharedModules`). `:app` `di/` supplies only what is Android-, Firebase- or `BuildConfig`-specific (`androidPlatformModule`, `aiModule`, collected in `androidModules`). The KDoc on `sharedModules` lists what every platform must supply; keep it current. iOS supplies the same through `iosPlatformModule` (`:shared` `iosMain` `di/IosPlatformModule.kt`): a Darwin engine factory, `NetworkConfig` from the `IosAppConfig` Swift passes in, Apollo, and the Firebase-backed repositories.
*   **Platform SDKs with no Kotlin Multiplatform version (Firebase on iOS)**: reach `:shared` through a bridge. `iosMain` `platform/` declares a small callback interface (`FirebaseAuthBridge`, `FirebaseAiBridge`) that Swift implements with the native SDK. A Kotlin adapter (`IosAuthRepository`, `IosAuthTokenProvider`, `IosAiChatRepository`) implements the domain interface on top of it and owns the error mapping (`mapBridgedAuthError`, `mapBridgedAiFailure`, mirroring Android's `safeAuthCall` / `safeAiCall`), so Swift only forwards results. Settings both apps must share, such as the Gemini model and prompts (`ChatModelConfig`), live in `commonMain`.
*   **Lifetimes**: app-wide objects (repositories, clients, database, Firebase) are `single`; cheap wrappers (use cases, the DAO, the clock, the dispatcher) are `factory`. The `HttpClientEngine` is a `factory` so each Ktor client gets its own engine.
*   **Definitions**: `singleOf(::Impl) bind Interface::class` / `factoryOf(::X)` / `viewModelOf(::X)` when no parameter needs a qualifier; an explicit lambda (`single<I> { Impl(get(), get(IoDispatcher)) }`) when one does.
*   **Qualifiers**: the `TmdbClient`, `BffClient` and `IoDispatcher` constants in `:shared` `di/Qualifiers.kt`. Always use the constants, never raw `named("...")` strings.
*   **ViewModels**: defined in `:shared` (multiplatform AndroidX `ViewModel`, `viewModelOf` in `viewModelModule`). On Android, resolve with `koinViewModel()` (`org.koin.compose.viewmodel`), which scopes to the current `LocalViewModelStoreOwner`. A navigation argument is a plain constructor parameter passed with `koinViewModel { parametersOf(arg) }`, never `SavedStateHandle`, so Swift can pass it too. `AppSessionViewModel` must be resolved outside the `NavHost` so the activity owns it. On iOS, `ViewModelOwner` (`iosMain` `ui/bridge/`) holds a ViewModel and clears it from Swift's `deinit`, and `StateFlowWatcher` delivers its state to Swift on the main thread; no Swift-interop compiler plugin (SKIE, KMP-NativeCoroutines), for the same reason as no Koin compiler plugin.
*   **Startup**: `MovieGalleryApplication.onCreate` calls `startKoin { androidContext(...); modules(sharedModules + androidModules) }`. On iOS, Swift calls `startMovieGallery(config, authBridge, aiBridge)` (`iosMain` `di/IosKoin.kt`) once at launch. It starts Koin with `sharedModules + iosPlatformModule` and returns a `MovieGalleryGraph`, which has one function per ViewModel because Swift can't call Koin's reified `get<T>()`. A new ViewModel needs a function there too; `IosKoinGraphTest` enforces it.
*   **Graph tests**: Koin resolves at runtime, so three tests stand in for a compile-time graph check. Keep them passing and extend them with new bindings:
    *   `KoinGraphTest` runs `verify()` over constructor parameters.
    *   `KoinGraphResolutionTest` (Robolectric) resolves every use case against the real graph, which catches `get()` calls inside lambdas that `verify()` can't see. Add each new use case to its list.
    *   `SharedModulesTest` checks which qualified client each API gets.

### 3. Modularization Strategy
For production apps, use a multi-module strategy to improve build times and separation of concerns.

**This project today** is moving to Kotlin Multiplatform for an iOS app:

*   **:shared** (KMP library: Android, `iosArm64`, `iosSimulatorArm64`): the domain layer, plus the data code that needs only multiplatform libraries (DTOs, mappers, Room, Apollo, the Ktor clients and APIs, and the repositories built on them; `:app` passes in the engine and anything from `BuildConfig`), plus every ViewModel and UiState. `iosMain` adds the iOS platform layer: the Swift bridge (`ui/bridge/`), the Firebase bridges (`platform/`) and their adapters, `iosPlatformModule` and the `startMovieGallery` entry point. Both iOS targets build a static `Shared` framework for the iOS app. `commonMain` has no `android.*`, `java.*` or `javax.*` imports; platform differences go through `expect`/`actual` in `androidMain`/`iosMain`.
*   **iosApp/** (SwiftUI app, Xcode project, not a Gradle module): consumes `:shared` as the static `Shared` framework, which its build phase has Gradle build. Swift translates and does not decide: `Platform/` implements the Firebase bridges by forwarding SDK results (errors untouched) and builds `IosAppConfig`; screens observe a shared ViewModel through `ObservedViewModel` (created once per screen by `ViewModelScope`, cleared when the view goes away) and show the shared copy and labels from `ui/common/` (`ErrorCopyKt`, `ChatCopyKt`, `Movie.cardSubtitle`, `toPosterGridItem()`, …). Navigation is a `TabView` with one `NavigationStack` per tab (`Main/`). A new screen gets a folder (`iosApp/iosApp/<Feature>/`) and a `MovieGalleryGraph` function; validation, error mapping and copy stay in `:shared`. An SDK only the UI needs because it presents its own screen (Credential Manager on Android, GoogleSignIn-iOS, the camera and photo pickers) stays in the screen layer, and only its result (a token string, a `file://` URL) crosses into the ViewModel.
*   **:app** (Android application): the Compose UI (screens, components, theme, navigation), the Android Koin modules, and data code still tied to Android or the JVM (Firebase Auth and AI, and the OkHttp base client the Ktor engine, Coil and Apollo share). It moves into `:shared` as multiplatform replacements land.

The target layout below is the direction for splitting further, not the current state:

*   **:app**: The main entry point, connects features.
*   **:core:model**: Shared domain models (Pure Kotlin).
*   **:core:data**: Repositories, Data Sources, Database, Network.
*   **:core:domain**: Use Cases and Repository Interfaces.
*   **:core:ui**: Shared Composables, Theme, Resources.
*   **:feature:[name]**: Standalone feature modules containing their own UI and ViewModels. Depends on `:core:domain` and `:core:ui`.

### 4. Checklist for implementation
- [ ] Ensure `Domain` layer has no Android dependencies.
- [ ] Repositories should default to main-safe suspend functions (use `Dispatchers.IO` internally if needed).
- [ ] ViewModels should interact with the UI layer via `StateFlow` (see `android-viewmodel` skill).
