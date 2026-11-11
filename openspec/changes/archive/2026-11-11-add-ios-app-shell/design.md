# Design

## Context

See proposal.md (Why). Here is the current state, as read from `kotlin_multiplatform` at 9c2559d (after PR #36).

- **What Swift gets from `Shared.framework`:**
  - **entry point:** `IosKoinKt.startMovieGallery(config:authBridge:aiBridge:)` returns a `MovieGalleryGraph`, with one function per ViewModel;
  - **config:** `IosAppConfig(tmdbAccessToken:appVersion:isDebug:)`;
  - **protocols to adopt:** `FirebaseAuthBridge` and `FirebaseAiBridge`, whose methods take completion blocks;
  - **supporting types:** `BridgedUser`, `BridgedAuthError(domain:code:message:)`, `BridgedAiFailure(kind:message:)` and `AiFailureKind`;
  - **the bridge:** `ViewModelOwner<VM>(create:)` and `StateFlowWatcher<T>(flow:)` with `watch(onEach:) -> WatchHandle`;
  - **settings:** `ChatModelConfig`.
- **The framework:** static, `baseName = "Shared"`, on `iosArm64` and `iosSimulatorArm64`. Every iOS binary links `-lsqlite3`. The Kotlin Gradle plugin provides `embedAndSignAppleFrameworkForXcode`, which reads Xcode's environment (`CONFIGURATION`, `SDK_NAME`, `ARCHS`) and writes the framework to `shared/build/xcode-frameworks/$CONFIGURATION/$SDK_NAME/`.
- **The shared ViewModels 5b uses:**
  - **`AppSessionViewModel`:** `isSignedIn: Boolean`, read once.
  - **`LoginViewModel`:**
    - its `uiState` is `StateFlow<LoginUiState>`, with email, password, mode, `isLoading`, `errorMessage` and `isAuthenticated`;
    - its actions are `onEmailChange`, `onPasswordChange`, `onToggleMode` and `onSubmit`;
    - it holds all the validation and error copy.
  - **`HomeViewModel`:**
    - its `uiState` is `StateFlow<HomeUiState>`, with `isLoading`, `sections` and `error: AppError?`;
    - it also has `avatarInitial` and `retry()`;
    - it loads in `init`.
- **Copy that still lives only in `:app`:**
  - `StateViews.kt` has private `AppError.title` / `AppError.detail`, the full-screen error text;
  - `MovieCard.kt` has private `Movie.subtitleWithRating`: "★ rating" when rated, up to two genres, joined with " · ", falling back to `yearLabel`.

  Both are pure Kotlin over domain types.
- **Firebase iOS SDK 13.1.1** is the latest stable release (checked 2026-11-10).
  - Its Swift Package Manager products are `FirebaseAuth`, `FirebaseAILogic` and `FirebaseAppCheck`.
  - It needs iOS 15 or later.
  - Auth errors are `NSError`s in `AuthErrorDomain` (`"FIRAuthErrorDomain"`).
  - Firebase AI Logic reports `GenerateContentError` cases.
- **Tooling on this machine:** Xcode 26.6, iPhone 16 simulators, and no XcodeGen or Tuist. The Claude iOS Simulator tool can build and launch an `.app` and drive it.
- **Android's equivalents, for parity:**
  - `app/google-services.json` is git-ignored, and the build fails without it;
  - the TMDB token comes from an environment variable or `local.properties`, and without it the build still works but calls return 401;
  - the app is `com.raikar.moviegallery`, `versionCode` 2.

## Goals / Non-Goals

**Goals:**
- `xcodebuild` (and Xcode's Run) builds the app from a clean checkout once the two ignored files are in place. The Kotlin framework is rebuilt as part of the Xcode build, with no manual Gradle step.
- On the simulator, a signed-out launch shows Login. A real Firebase account signs in and lands on Home with TMDB rails, and a relaunch goes straight to Home. A wrong password shows the same message Android shows.
- Swift holds no copy of business logic or user-facing copy that the shared code already has.
- Swift adds no new behavior. Its only logic is translating Firebase and `Info.plist` values into the bridge types, and that is unit-tested.

**Non-Goals:**
- Making the iOS UI match Android's look. 5b uses stock SwiftUI components.
- Swift-side tests of the screens, either UI tests or snapshots. The ViewModels they render are already tested in `commonTest`.

## Decisions

### 1. A checked-in Xcode project with synchronized folders

`iosApp/iosApp.xcodeproj` uses `PBXFileSystemSynchronizedRootGroup` (Xcode 16+, `objectVersion = 77`) for `iosApp/iosApp/` and `iosApp/iosAppTests/`.

- Files added to those folders join their target without any change to `project.pbxproj`, so the file changes only for real build-setting or dependency changes. That keeps merge conflicts rare, which was the main argument for a generator.
- *Alternative:* XcodeGen or Tuist. Both regenerate the project from YAML or Swift, but every contributor and every CI job would need the tool installed, and neither is installed here. Synchronized folders remove most of what they solve.

Layout:

```
iosApp/
  iosApp.xcodeproj/                  (project, shared scheme "iosApp", Package.resolved)
  Configuration/
    Config.xcconfig                  (committed: framework paths, linker flags, version, #include? Secrets)
    Secrets.example.xcconfig         (committed: TMDB_READ_ACCESS_TOKEN = )
    Secrets.xcconfig                 (git-ignored)
  iosApp/                            (app target, synchronized)
    MovieGalleryApp.swift, RootView.swift
    Platform/  AppConfiguration.swift, FirebaseAuthService.swift, FirebaseAiService.swift
    Shared/    ObservedViewModel.swift
    Login/     LoginView.swift
    Home/      HomeView.swift, MovieRail.swift, ErrorStateView.swift
    Info.plist, Assets.xcassets, GoogleService-Info.plist (git-ignored)
  iosAppTests/                       (unit-test target, synchronized)
```

- **Target:** iOS 17, so SwiftUI's Observation (`@Observable`) is available; Firebase only needs iOS 15.
- **Identity:** bundle ID `com.raikar.moviegallery`, the same as Android, so the Firebase console shows the same app on both platforms. Display name "MovieGallery".
- **Signing:** automatic. Simulator builds need no development team.

### 2. Gradle builds the framework inside the Xcode build

A Run Script phase, ordered before "Compile Sources" and named "Build Shared framework", runs:

```sh
cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

`Config.xcconfig` sets:
- `FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`;
- `OTHER_LDFLAGS = $(inherited) -framework Shared -lsqlite3`;
- `ENABLE_USER_SCRIPT_SANDBOXING = NO`, because the script writes outside the sandbox.

The phase declares no outputs, so it runs on every build, and Gradle's own up-to-date checks keep repeat builds fast.

- This is JetBrains' standard integration for direct framework linking, and it builds the right architecture for the simulator or a device automatically.
- *Alternative:* an XCFramework assembled by Gradle and checked in, or published through Swift Package Manager. It decouples the builds, but every Kotlin change would need a manual reassembly, which is wrong while both sides change together.
- *Alternative:* CocoaPods (`kotlin("native.cocoapods")`). It adds a Podfile, a workspace and a second dependency manager next to Swift Package Manager, which Firebase recommends.

### 3. Config through xcconfig, Info.plist and `IosAppConfig`

- **`Config.xcconfig`:**
  - `#include? "Secrets.xcconfig"`, which is optional, so the build succeeds without it;
  - `TMDB_READ_ACCESS_TOKEN` defaults to empty;
  - `CURRENT_PROJECT_VERSION = 2` and `MARKETING_VERSION = 1.0`, matching Android's `versionCode` / `versionName`.
- **`Info.plist`** maps `TMDBReadAccessToken` to `$(TMDB_READ_ACCESS_TOKEN)`. The bundle version already carries `CURRENT_PROJECT_VERSION`.
- **`AppConfiguration.make(info:isDebug:)`** (Swift) reads both values from a dictionary and returns an `IosAppConfig`. It defaults to an empty token and `"0"`, and never crashes. The app passes `Bundle.main.infoDictionary` and `#if DEBUG`; the tests pass dictionaries.
- **Exposure:** the token ends up in the app bundle's `Info.plist`, just as Android's ends up in `BuildConfig`. That's the same exposure already accepted for the demo build, and nothing about it changes here.
- *Alternative:* an environment variable read by the Run Script. Xcode launched from the Dock doesn't see shell variables, so an xcconfig is the reliable channel. A CI job (out of scope) can still write `Secrets.xcconfig` from a secret.

### 4. Firebase through Swift Package Manager, configured before Koin

- **Package:** `https://github.com/firebase/firebase-ios-sdk`, "Up to Next Major" from 13.1.1. The products are `FirebaseAuth`, `FirebaseAILogic` and `FirebaseAppCheck`. `Package.resolved` is committed so every build resolves the same versions.
- **Startup, in `MovieGalleryApp.init`:**
  1. Install the App Check provider factory: `AppCheckDebugProviderFactory` under `DEBUG`, else a factory returning `AppAttestProvider`.
  2. Call `FirebaseApp.configure()`.
  3. Build both services and `AppConfiguration`, then call `startMovieGallery`.

  App Check must be installed before `configure()`. Koin comes after it, because the auth bridge's `currentUser()` is read when `AppSessionViewModel` is created.
- **`GoogleService-Info.plist`** lives in `iosApp/iosApp/` and is git-ignored, like `app/google-services.json`. Without it, `configure()` stops the app at launch with Firebase's own message. CLAUDE.md lists it as a prerequisite, as it does for Android.

### 5. The Swift bridges translate and do nothing else

**`FirebaseAuthService: NSObject, FirebaseAuthBridge`:**

| Bridge method | Firebase iOS SDK call |
|---|---|
| `currentUser()` | `Auth.auth().currentUser` → `BridgedUser` |
| `addAuthStateListener` | `addStateDidChangeListener` |
| `signIn` | `signIn(withEmail:password:)` |
| `signUp` | `createUser(withEmail:password:)` |
| `idToken` | `currentUser?.getIDTokenForcingRefresh(_:)` |
| `signOut` | `try? Auth.auth().signOut()` |

- **The listener's registration** calls `removeStateDidChangeListener` when Kotlin removes it.
- **Every error** becomes `BridgedAuthError(domain:code:message:)` through one function, `bridgedAuthError(_ error: Error)`, which keeps the domain, code and `localizedDescription`. That function is unit-tested.
- **`signInWithGoogle`** completes at once with `BridgedAuthError(domain: "MovieGallery", code: 0, message: "Google sign-in isn't available on iOS yet")`. Kotlin maps that to `Unknown`. No 5b screen can call it (proposal, Pre-production debt).

**`FirebaseAiService: NSObject, FirebaseAiBridge`:**

- **Model:** built once from `FirebaseAI.firebaseAI(backend: .googleAI())` with:
  - `ChatModelConfig.shared.MODEL_NAME`;
  - `systemInstruction` set to `ChatModelConfig.shared.systemPrompt(platformName: "iOS")`;
  - `RequestOptions(timeout:)` from `REQUEST_TIMEOUT_MILLIS / 1000`.
- **Each call** starts a fresh `Chat` seeded with the history mapped to `ModelContent(role: "user" | "model")`, as Android's `GeminiChatDataSource` does. It sends the message, or for a poster sends `IDENTIFY_POSTER_PROMPT` plus the image.
- **The image:** `identifyPoster` loads the `file://` URL into a `UIImage`. If that fails, it completes with `.imageUnreadable` without calling the model.
- **Completions:** the SDK is `async`, so each call runs in a `Task`. The completion is called exactly once, on the main actor, with the reply text or a failure.
- **Classifying errors:** `aiFailureKind(_ error: Error) -> AiFailureKind` is a pure function, unit-tested. It was confirmed against 13.1.1's sources (task 4.3). `GenerateContentError` has only four cases: `internalError(underlying:)`, `promptImageContentError`, `promptBlocked` and `responseStoppedEarly`. There's no `invalidAPIKey` case, so the first draft of this rule was wrong. Every failure that isn't a `GenerateContentError` is wrapped in `internalError(underlying:)`, including URL errors, decoding errors and the internal `BackendError`. `BackendError` bridges to an `NSError` in domain `com.google.firebase.firebaseai.BackendError`, with the HTTP status as its code. So the function unwraps `internalError` and then sorts:
  - an `NSURLErrorDomain` error with code `NSURLErrorTimedOut` → `.timeout`, any other `NSURLErrorDomain` error → `.network`;
  - a backend 403 (API not enabled, service disabled), or a backend 400 whose message names the API key → `.unauthorized`, as Android's `ServiceDisabledException` / `APINotConfiguredException` / `InvalidAPIKeyException` are;
  - a backend 401 (an App Check rejection) → `.other`, because Android's App Check rejection also lands in `Unknown`;
  - a `DecodingError` → `.serialization`;
  - everything else, including `promptBlocked`, `responseStoppedEarly`, `promptImageContentError` and quota errors (429) → `.other`.

  Each failure carries the error's `localizedDescription`.

### 6. One Swift helper pairs `ViewModelOwner` with `StateFlowWatcher`

```swift
@Observable @MainActor
final class ObservedViewModel<VM: Lifecycle_viewmodelViewModel, State: AnyObject> {
    let viewModel: VM
    private(set) var state: State
    // init(_ create: @escaping () -> VM, state: (VM) -> StateFlow) — owns a ViewModelOwner and a WatchHandle
    // deinit: handle.cancel(); owner.clear()
}
```

- **Usage:** a view holds one with `@State`, for example `ObservedViewModel({ graph.loginViewModel() }, state: { $0.uiState })`. SwiftUI keeps it alive as long as the view's identity lasts and releases it when the view leaves the hierarchy. Its `deinit` cancels the watch and clears the ViewModel, which cancels `viewModelScope`.
- **Names, confirmed from `Shared.h` (task 5.1):**
  - the ViewModel base class is `Lifecycle_viewmodelViewModel`;
  - `uiState` is an untyped `Kotlinx_coroutines_coreStateFlow`, which `StateFlowWatcher<State>` types;
  - the accessor is `ChatModelConfig.shared`;
  - Kotlin enum cases export lowercased: `AiFailureKind.imageunreadable`, `LoginMode.signin` / `.createaccount`.
- **Created on first appearance:** a small `ViewModelScope` view creates a screen's `ObservedViewModel` the first time it appears and keeps it in `@State`. Creating it in a view's initializer would build, and start loading, a throwaway ViewModel each time SwiftUI re-creates the view struct.
- **Why one helper:** a hand-written `ObservableObject` per screen would repeat this lifecycle code ten times in 5c.

### 7. Root, Login and Home views

- **`RootView`:**
  - It holds the app-wide `AppSessionViewModel`, owned for the app's lifetime by `MovieGalleryApp`, as the activity owns it on Android.
  - It keeps one `@State var signedIn`, seeded from `isSignedIn`, and shows `HomeView` or `LoginView`.
  - `LoginView` reports success (`state.isAuthenticated` becoming true) through a closure, and `RootView` swaps to Home. There's no navigation stack, so there's no back gesture to Login.

  This meets `app-navigation` "App starts at login" and `authentication` "Successful sign-in removes login from history".
- **`LoginView`:**
  - **Content:** the "MovieGallery" title and "Discover movies you'll love." tagline; Email and Password fields (`SecureField`, email keyboard, no autocapitalization); a primary button reading "Log In" or "Create Account"; and the toggle reading "Don't have an account? Create account" or "Already have an account? Log in". All copy is the same as Android's `LoginScreen`.
  - **Error:** `errorMessage` appears in red below the fields.
  - **Loading:** while `isLoading`, a `ProgressView` replaces the button label, and the button and toggle are disabled.
  - **Actions:** each field keeps its text in local `@State` and forwards every edit to `onEmailChange` / `onPasswordChange`. A `Binding` read straight from `state` drops keystrokes: the new state reaches Swift a main-queue hop later, and SwiftUI re-reads the stale value in between. This holds because the ViewModel only echoes these two values and never rewrites them (found in the simulator, task 7.4).
  - Swift adds no validation of its own.
- **`HomeView`:**
  - **Title:** "MovieGallery".
  - **Loading:** a centered `ProgressView` while `isLoading`.
  - **Error:** when `error` is set, `ErrorStateView` shows the shared title and detail for that `AppError`, with a "Retry" button calling `retry()`.
  - **Content:** otherwise a vertical list of `MovieRail`s, one per section. Each has the section title and a horizontal `ScrollView` of cards. A card shows the `AsyncImage` of `posterUrl`, or a placeholder when it's absent or fails, plus the title and the shared subtitle.
  - **Not yet:** card taps and the avatar (5c).

### 8. The error copy and card subtitle move to `:shared`

- **`ui/common/ErrorCopy.kt`** (`commonMain`): public `val AppError.errorTitle: String` and `val AppError.errorDetail: String`, moved verbatim from `StateViews.kt`, including the `Http` code interpolation.
- **`ui/common/MovieLabels.kt`:** public `val Movie.cardSubtitle: String`, moved verbatim from `MovieCard.kt`'s `subtitleWithRating`.
- **Android:** `StateViews.kt` and `MovieCard.kt` call these and drop their private copies.
- **Tests:** a `commonTest` test pins every string, covering each `AppError` case and the subtitle's rated, unrated, genre-less and year-less cases.
- **From Swift (confirmed in `Shared.h`):** the `AppError` copy is on an interface, so Swift sees static functions `ErrorCopyKt.errorTitle(_:)` / `errorDetail(_:)`. `cardSubtitle` extends a class in the same module, so Kotlin exports it as a property on `Movie` (`movie.cardSubtitle`), and there's no `MovieLabelsKt`.
- *Alternative:* copying the strings into Swift. That would be two places to edit every time the copy changes, and Android's tests would never notice drift.

### 9. Tests

- **`iosAppTests` (XCTest, hosted in the app)** tests three things:
  - `bridgedAuthError` with a Firebase-shaped `NSError` and with a non-`NSError` Swift error;
  - `aiFailureKind` for each case in decision 5, built from `NSError`s and `DecodingError`s (`GenerateContentError` cases are covered where the SDK lets tests construct them);
  - `AppConfiguration.make` with a full dictionary, a missing token, and a missing version.

  They run with `xcodebuild test` on the iPhone 17 simulator (iOS 26.5). On this machine the iPhone 16 exists only on the iOS 18.6 runtime, and `xcodebuild` defaults to the latest OS. When the app runs as the test host (`XCTestConfigurationFilePath` is set), it starts neither Firebase nor Koin: the tests cover pure functions, and Firebase would need `GoogleService-Info.plist`.
- **Kotlin:** the new `commonTest` copy tests run on the JVM and iOS. No existing test changes.
- **Manual (simulator), task 7.4:**
  - a signed-out launch shows Login;
  - `admin` / `admin` shows "Enter a valid email address";
  - a wrong password shows "Invalid email or password";
  - a valid account lands on Home with four rails;
  - a relaunch opens on Home;
  - with the Mac offline, Home shows "No connection" and Retry reloads once it's back online;
  - with an empty token, Home shows "TMDB rejected the request".

## Risks / Trade-offs

- **[Risk] A hand-written `project.pbxproj` is malformed or Xcode rewrites it on first open.** → It's kept minimal: two synchronized groups, one package reference, one script phase. `xcodebuild -list` and a full build verify it, and the file is committed after Xcode has opened and saved it, so later diffs show only real changes.
- **[Risk] The Gradle step makes every Xcode build slower.** → Gradle's configuration cache and up-to-date checks make a no-change build a few seconds. A Kotlin change costs a Kotlin/Native link, which is unavoidable whatever the integration.
- **[Risk] `ObservedViewModel` is released early or late**, for example when SwiftUI recreates `@State` storage. → `@State` keeps the object for the view's identity. RootView swapping Login for Home is the case to check: Login's ViewModel must be cleared, which the manual check confirms with a `deinit` log in debug builds.
- **[Risk] Objective-C export names in Swift differ from what this design assumes** (`Lifecycle_viewmodelViewModel`, `ChatModelConfig.shared`, `ErrorCopyKt`). → Every Swift-facing name is taken from the built `Shared.h` (task 5.1) rather than from this document.
- **[Risk] Firebase AI's Swift error cases change between releases**, so `aiFailureKind` sorts a new case into `.other`. → `.other` maps to `Unknown`, which shows the SDK's message with a retry. That's the same fallback Android has.
- **[Trade-off] The AI bridge ships untested end to end** until 5c's chat screen. → Its classification is unit-tested, and the call path mirrors Android's data source. 5c's manual check covers a real send.
- **[Trade-off] The iOS UI is plain SwiftUI.** → That's deliberate: 5b proves the plumbing, and 5c does the visual work across every screen at once.

## Migration Plan

- **Branch:** `feature/ios-app-shell`, from `kotlin_multiplatform` at 9c2559d (already created). The PR goes into `kotlin_multiplatform`.
- **Order:**
  1. Shared copy (Android stays green).
  2. Xcode project and framework build.
  3. Config.
  4. Firebase and the bridges.
  5. ViewModel helper and screens.
  6. Swift tests.
  7. Docs.
- **One-time setup, done by you:**
  1. Register the iOS app (`com.raikar.moviegallery`) in the Firebase console, with Email/Password already enabled for Android.
  2. Download `GoogleService-Info.plist` to `iosApp/iosApp/`.
  3. Create `iosApp/Configuration/Secrets.xcconfig` with the TMDB token.

  Implementation pauses at task 4.1 until the plist exists.
- **Rollback:** revert the PR. Android's only code change is reading the moved copy from `:shared`, which the copy tests and the Android check cover.
