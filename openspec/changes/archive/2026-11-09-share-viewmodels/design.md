# Design

## Context

See proposal.md (Why) for the motivation. Here is the current state and the constraints that shape the approach, as read from the code on `feature/share-room-database-builder` (2968919).

- **Nine ViewModels in `:app`** (`ui/screens/<feature>/` and `ui/navigation/AppSessionViewModel.kt`). They extend `androidx.lifecycle.ViewModel`, use `viewModelScope`, take use cases as plain constructor parameters, and expose `StateFlow`. All use cases and repository interfaces are already in `:shared`. Only four things tie them to `:app`:
  - `MovieDetailViewModel` reads `movieId` from `SavedStateHandle`. The value comes from the `detail/{movieId}` navigation argument, an `IntType` in `AppNavHost`.
  - `AppSessionViewModel.startRoute` returns a `Routes` constant. `Routes` belongs to Compose Navigation and stays in `:app`.
  - `AiChatViewModel` imports `java.util.concurrent.atomic.AtomicLong`.
  - `viewModelModule` (nine `viewModelOf` entries) is in `:app`'s `AndroidModules.kt`.
- **UiState files** (`HomeUiState`, `SearchUiState`, `MovieDetailUiState`, `WatchlistUiState`, `ProfileUiState`, `LoginUiState`, `TopTvShowsUiState`, plus `chat/ChatModels.kt` for `ChatMessage` and `ChatUiState`) import only `domain.model` types. None of them reference Compose or `R`.
- `LoginViewModel` calls `isValidEmail` in `login/EmailValidator.kt`, a pure Kotlin `Regex` with 3 tests in `EmailValidatorTest`, so both move with it. `CountryDetailRowsTest` and `HeaderCollapseTest` test Compose-side helpers and stay in `:app`.
- **The nine ViewModel tests** (75 tests in all, in `app/src/test/.../ui/`) use JUnit 4 (`@Test`, `@Rule`, `org.junit.Assert`), `MainDispatcherRule` (`UnconfinedTestDispatcher` by default; `SearchViewModelTest` passes a `StandardTestDispatcher` for the debounce), Turbine and `kotlinx-coroutines-test`. None use Robolectric or MockK. Four test names contain characters Kotlin/Native rejects (`,` `.` `:` `;`). `MovieDetailViewModelTest` builds a `SavedStateHandle`.
- **Fakes** (`app/src/test/.../testutil/`): `FakeMovieRepository`, `FakeWatchlistRepository`, `FakeAuthRepository`, `FakeCountryRepository`, `FakeTvShowRepository` and `FakeAiChatRepository`. They implement `:shared` interfaces using coroutines only. `FakeAuthRepository` is also used by `WatchlistRepositoryImplTest` and `WatchlistPersistenceTest`, which are Robolectric tests on a real Room database and stay in `:app`.
- **Dependencies:**
  - The catalog has `lifecycle = 2.11.0` and `koin = 4.2.2`. `:shared` `commonMain` already exposes `koin-core` as `api`.
  - `viewModelOf` (`org.koin.core.module.dsl`) is in `koin-core-viewmodel`, which `:app` gets today only through `koin-compose-viewmodel`.
  - The multiplatform `androidx.lifecycle:lifecycle-viewmodel` artifact publishes `iosArm64` and `iosSimulatorArm64`. On every platform its `viewModelScope` runs on `Dispatchers.Main.immediate`, which on iOS is the main queue.
- **Project rule** (CLAUDE.md, DI): no compiler plugins in the DI path, "so a Kotlin upgrade never waits on a DI release." The same reasoning applies to anything else tied to the Kotlin compiler version.

## Goals / Non-Goals

**Goals:**
- Every ViewModel compiles for Android and both iOS targets from `commonMain`, with no `android.*`, `java.*` or `javax.*` imports.
- Every ViewModel test runs in `commonTest`, on the JVM (as part of `./gradlew test`) and on the iOS simulator.
- From Swift, a caller can create a ViewModel, read its state, be told of every change on the main thread, and end its lifetime, cancelling its coroutines. The bridge tests in `iosTest` prove this.
- Android behaves as today: each screen still gets its ViewModel from `koinViewModel()`, scoped to the same owner.

**Non-Goals:**
- A Swift-friendly API for every use of `Flow`. The bridge covers `StateFlow`, which is all the ViewModels expose.
- Testing the bridge from Swift. No Xcode project exists until phase 5. `iosTest` exercises it from Kotlin/Native.

## Decisions

### 1. Same packages, in `:shared` `commonMain`

The ViewModels and UiState files move to `shared/src/commonMain/kotlin/com/raikar/moviegallery/ui/screens/<feature>/` and `.../ui/navigation/`. They keep their packages, so the Compose screens in `:app` change no imports, and CLAUDE.md's rule that both modules use the same packages still holds.

- *Alternative:* a new `presentation.<feature>` package in `:shared`. It is more accurate, since `:shared` has no UI, but it renames every import in nine screens and their tests for no behavior gain. It can be done later as a rename-only change.

### 2. Keep AndroidX `ViewModel`, now the multiplatform artifact

`:shared` `commonMain` adds `api(libs.androidx.lifecycle.viewmodel)` and `api(libs.koin.core.viewmodel)`, and `viewModelModule` moves to `SharedModules.kt` unchanged apart from the `movieId` parameter (decision 3). `api`, because `:app`'s `koinViewModel()` and the screens see the `ViewModel` supertype.

- Android keeps `koinViewModel()` scoping to the navigation entry, and `viewModelScope` cancellation on `onCleared`.
- On iOS, the same class is cleared through a `ViewModelStore` (decision 5).
- *Alternative:* plain classes that own a `CoroutineScope` and a `close()`. That avoids the lifecycle dependency, but Android would need its own wrapper ViewModel per screen to keep today's scoping. That is nine adapters to keep in sync, which is exactly the duplication this change removes.

### 3. `movieId` is a constructor parameter, passed with `parametersOf`

The constructor is `MovieDetailViewModel(movieId: Int, getMovie, observeIsInWatchlist, toggleWatchlist, getCountryDetails)`, still registered with `viewModelOf(::MovieDetailViewModel)`. Koin fills `Int` from the definition parameters.

- **Android:** `AppNavHost`'s detail destination reads the argument from its back-stack entry and passes `koinViewModel { parametersOf(movieId) }` to `MovieDetailScreen`, which already takes `viewModel` as a parameter. The ViewModel is still keyed to the navigation entry, so recomposition and configuration changes reuse it. After process death, the entry's arguments are restored and supply the same id again, as `SavedStateHandle` did.
- **Swift:** passes the id directly.
- *Alternative:* keep `SavedStateHandle`, which is now multiplatform. SwiftUI has nothing that fills it, so every Swift caller would have to build one by hand with a magic `"movieId"` key. The detail ViewModel never writes state back, so it gains nothing from it.

### 4. A hand-written Swift bridge, no SKIE or KMP-NativeCoroutines

Without help, Swift sees a `StateFlow` as an opaque `Kotlinx_coroutines_coreStateFlow` that it can't collect. SKIE and KMP-NativeCoroutines both fix this, and both are compiler or KSP plugins pinned to a Kotlin version. That is the coupling CLAUDE.md rules out for DI. The ViewModels expose only `StateFlow` and plain functions, so a small bridge covers everything SwiftUI needs (decision 5).

- This updates my earlier note, which suggested SKIE or KMP-NativeCoroutines. Applying the project's own plugin rule tips it.
- *Revisit if* phase 5's SwiftUI code finds the bridge too thin, for example if it needs `suspend` calls from Swift or `Flow` outside ViewModels. Adding SKIE later changes no ViewModel, so this choice is reversible.

### 5. Bridge shape (`iosMain`, `ui/bridge/` package)

- **`StateFlowWatcher<T : Any>(flow: StateFlow<T>)`**
  - `val value: T` returns the current value.
  - `fun watch(onEach: (T) -> Unit): WatchHandle` calls `onEach` with the current value and then with every change. It collects on a scope using `Dispatchers.Main`, so the closures run on the main thread, which SwiftUI requires.
  - `WatchHandle.cancel()` stops it.
  - A class with a generic parameter, not an extension function, because Objective-C export keeps class generics and drops them on extension functions.
- **`ViewModelOwner<VM : ViewModel>(create: () -> VM)`**
  - Holds a private `ViewModelStore` and gets the ViewModel through `ViewModelProvider.create(store, viewModelFactory { initializer { create() } })`, the public multiplatform API.
  - `val viewModel: VM`.
  - `fun clear()` calls `store.clear()`, which runs `onCleared` and cancels `viewModelScope`.
  - Swift calls `clear()` from the owning object's `deinit`.
  - The factory takes a lambda, so phase 5 can pass a Koin lookup without this class depending on Koin.
- *Alternative:* expose `ViewModelStore` directly to Swift. It works, but it pushes AndroidX lifecycle concepts into every SwiftUI view.

### 6. `AppSessionViewModel` exposes `isSignedIn`, not a route

`val isSignedIn: Boolean`, read once at creation as today. `AppNavHost` computes `startDestination = if (sessionViewModel.isSignedIn) Routes.MAIN else Routes.LOGIN`. The KDoc about resolving it outside the `NavHost` stays on the class and on `AppNavHost`.

- *Alternative:* move `Routes` to `:shared`. They are Compose Navigation strings with Android argument syntax (`detail/{movieId}`), which SwiftUI navigation wouldn't use.

### 7. `AiChatViewModel` uses a plain `Long` counter

Every write to `nextId` happens on the main thread: from the public functions the UI calls, and from `handle`, which runs in `viewModelScope` (`Dispatchers.Main.immediate`). So a plain `private var nextId = GREETING_ID + 1L` with post-increment is equivalent. A comment records the main-thread assumption.

- *Alternative:* `kotlin.concurrent.atomics.AtomicLong`. It is still experimental in Kotlin 2.3, so it needs an opt-in for no real gain.

### 8. Tests in `commonTest`, one dispatcher setup per class

- Each moved test class sets `Dispatchers.setMain(...)` in `@BeforeTest` and `Dispatchers.resetMain()` in `@AfterTest`, using the dispatcher its `MainDispatcherRule` used (`UnconfinedTestDispatcher` unless the test passed another).
- Assertions move from `org.junit.Assert` to `kotlin.test`.
  - `assertEquals` keeps the expected-first order.
  - `assertNull` / `assertTrue` / `assertFalse` keep their meaning.
- The four test names with `,` `.` `:` `;` are reworded without those characters, keeping their meaning.
- `MainDispatcherRule` is deleted, since no JUnit test uses it any more.
- The six fakes move to `shared/src/commonTest/.../testutil/`. `:app` keeps its own `FakeAuthRepository` for the two Robolectric watchlist tests, with a KDoc pointing at the shared copy. `commonTest` isn't visible to `:app`'s tests, and KMP test fixtures aren't supported by the Android KMP library plugin.
- *Alternative:* a shared test-fixtures module just for `FakeAuthRepository`. That is a new module and Gradle wiring for one class. Revisit when the watchlist tests leave `:app`.

## Risks / Trade-offs

- **[Risk] `koinViewModel { parametersOf(movieId) }` picks up a different ViewModel than today**, for example one shared across detail entries. → It is scoped to the same `LocalViewModelStoreOwner` (the navigation entry) as today's call. The device check opens two different movies one after another, then goes back and confirms the first one's state.
- **[Risk] Objective-C export of the generic bridge classes is awkward in Swift**, for example bounds being erased to `id`. → The class generics export as Objective-C lightweight generics. If Swift ergonomics turn out poor in phase 5, add thin non-generic per-screen wrappers there. The bridge's Kotlin API doesn't block that.
- **[Risk] A test that passes only because of JUnit ordering or rule timing behaves differently under `kotlin.test` on iOS.** → Both the JVM and the iOS test runs are required to pass, and the test counts are compared before and after (the same nine classes, the same number of tests).
- **[Risk] The `ui.screens` packages in a module with no UI may mislead readers.** → CLAUDE.md's repo map says ViewModels and UiState live in `:shared`, and screens and components in `:app`. A package rename stays possible later (decision 1).
- **[Trade-off] `FakeAuthRepository` exists twice** until the watchlist tests move. → Small and documented. Both copies implement the same interface, so the compiler flags any drift when the interface changes.
- **[Trade-off] Login copy now lives in `:shared`**, so SwiftUI shows the same English strings. There's no localization yet on either platform, so nothing regresses.

## Migration Plan

- **Branch:** a new branch `feature/share-viewmodels`, based on `kotlin_multiplatform` after PR #33 merges (or stacked on `feature/share-room-database-builder` if it hasn't). The PR goes into `kotlin_multiplatform`, like #30–#33.
- **Order:**
  1. Dependencies.
  2. Move the UiState and ViewModel files.
  3. Make the shareability edits.
  4. Move `viewModelModule`.
  5. Wire `:app`.
  6. Move the tests.
  7. Add the bridge and its tests.
  8. Update the docs.
- **Rollback:** revert the PR. No data or schema changes are involved.
- **Device check before commit:** sign in, browse Home, search, open two movie details in a row and go back, toggle the watchlist, open a production-country sheet, page through TV shows, send a chat message, then sign out and back in.
