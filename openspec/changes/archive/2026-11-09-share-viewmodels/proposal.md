# Proposal

## Why

Phase 4b of the KMP migration. The domain layer, the data layer and the database are in `:shared` now, but the nine ViewModels that turn use cases into screen state are still in `:app`. So is the logic inside them: login validation and error copy, search debouncing, TV-show paging, chat history and retry, and the country sheet. A SwiftUI app would have to rewrite all of it, and the two copies would drift. The iOS UI will be **SwiftUI** (decided 2026-11-09). This change moves the ViewModels into `:shared` in a form Swift can use: no navigation types in their constructors, no JVM-only APIs, and a small iOS-side bridge so Swift can observe their state and end their lifetime. It comes before phase 5 so the iOS app starts with the screen logic already shared and tested. The Android app must behave exactly as it does today.

## What Changes

- **ViewModels move to `:shared` `commonMain`**, with their UiState files, `ChatModels.kt` and `login/EmailValidator.kt` (used by `LoginViewModel`), under the same `ui.screens.<feature>` and `ui.navigation` packages. They still extend the AndroidX `ViewModel`, now the multiplatform artifact, and still expose `StateFlow`. Moved: `HomeViewModel`, `SearchViewModel`, `MovieDetailViewModel`, `WatchlistViewModel`, `ProfileViewModel`, `LoginViewModel`, `AiChatViewModel`, `TopTvShowsViewModel`, `AppSessionViewModel`. The Compose screens stay in `:app`.
- **Changes needed to make them shareable** (none of these change behavior):
  - `MovieDetailViewModel` takes `movieId: Int` as a constructor parameter instead of reading it from `SavedStateHandle`. Android passes it from the navigation entry's argument with Koin `parametersOf`, and SwiftUI passes it directly.
  - `AppSessionViewModel` exposes `isSignedIn: Boolean` instead of an Android route string. `AppNavHost` maps it to `Routes.MAIN` / `Routes.LOGIN`, because routes are Compose Navigation's and stay in `:app`.
  - `AiChatViewModel` replaces `java.util.concurrent.AtomicLong` with a plain `Long` counter. It is only touched on the main thread.
- **`viewModelModule` moves to `:shared`** (`di/SharedModules.kt`, in `sharedModules`) and still uses `viewModelOf`. `:app` keeps `koinViewModel()` and drops the module from `androidModules`.
- **iOS bridge in `iosMain`**, written by hand with no compiler plugin:
  - a watcher that delivers a `StateFlow`'s current value and every change to a Swift closure, and can be cancelled;
  - a ViewModel owner that holds one ViewModel and clears it (cancelling its coroutines) when Swift is done with it.

  This is the minimum a SwiftUI view needs. Resolving ViewModels from Koin on iOS waits for the iOS Koin entry point in phase 5.
- **Tests move to `:shared` `commonTest`** and run on both the JVM and the iOS simulator:
  - all nine ViewModel tests and `EmailValidatorTest` (78 tests in all), rewritten from JUnit 4 + `MainDispatcherRule` to `kotlin.test` with `Dispatchers.setMain` in `@BeforeTest` / `@AfterTest`, and with names Kotlin/Native accepts;
  - the six repository fakes.
  - A new `iosTest` suite covers the bridge.
  - The Koin graph tests are updated for the new `movieId` parameter.
- **Docs**: `CLAUDE.md` (repo map, "Adding a feature screen" steps 6 and 10, `di/` section) and the `android-architecture` skill now say ViewModels live in `:shared`.

Backends touched: none. The ViewModels call the same use cases, which reach TMDB, the BFF, Firebase AI and the Countries API exactly as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This moves screen logic between modules and changes how two values reach it (`movieId` and the start route). It doesn't change what any screen shows or does, so the change sets `skip_specs: true`. Every existing spec must still hold unchanged. These are the requirements most at risk here, because their logic sits in the code being moved:
- `authentication`: the sign-in and create-account validation messages and the error copy;
- `movie-search`: debouncing and retry;
- `tv-shows`: paging and de-duplication;
- `ai-chat`: history, the greeting and retry;
- `movie-detail`: the production-country sheet;
- `app-navigation`: the start destination from the saved session.

## Out of scope

- Any SwiftUI code, an iOS app or Xcode project, or the iOS Koin entry point (`initKoin()`) and the iOS helpers that resolve ViewModels from Koin (phase 5).
- iOS implementations of `AuthRepository`, `AiChatRepository` or `AuthTokenProvider`, iOS Google Sign-In, and iOS image loading (phase 5).
- SKIE, KMP-NativeCoroutines or any other Swift interop compiler plugin (see design, decision 4).
- Moving Compose screens, navigation or `ui/components` into `:shared` (that would be Compose Multiplatform, which wasn't chosen).
- Moving error-to-text mapping that lives in Compose screens. Only the strings already inside ViewModels (login validation and auth errors) move.
- Moving the Robolectric watchlist repository and persistence tests out of `:app`.
- Process-death restoration beyond what exists today. The detail screen's `movieId` still comes back from the navigation back stack on Android.

## Pre-production debt

None. No placeholder behavior is introduced.

## Impact

- **Build:**
  - Two catalog entries: the multiplatform `androidx.lifecycle:lifecycle-viewmodel` (using the existing `lifecycle` version) and `io.insert-koin:koin-core-viewmodel` (using the existing `koin` version). Both become `:shared` `commonMain` API dependencies.
  - `:shared` `commonTest` adds Turbine (existing version).
  - No compiler plugins.
- **Code:**
  - `:app` loses nine ViewModels, seven UiState files, `ChatModels.kt` and `EmailValidator.kt`.
  - It gains a `parametersOf(movieId)` at the detail destination and a route mapping in `AppNavHost`.
  - `AndroidModules.kt` loses `viewModelModule`.
  - `:shared` gains those files, `viewModelModule`, and the iOS bridge (`iosMain`).
- **Tests:**
  - Nine ViewModel tests, `EmailValidatorTest` and six fakes move to `shared/src/commonTest/`. `MainDispatcherRule` is deleted.
  - `FakeAuthRepository` stays duplicated in `app/src/test/` for the two Robolectric watchlist tests that still use it.
  - New `iosTest` bridge tests.
  - `KoinGraphTest` swaps `SavedStateHandle` for `Int` in `extraTypes`.
- **Users:** none. Every screen behaves as today.
