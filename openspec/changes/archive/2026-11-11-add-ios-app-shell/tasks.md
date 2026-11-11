# Tasks

No CLAUDE.md checklist covers a new platform app. These tasks follow the design's Migration Plan:
1. shared copy first, while Android stays green;
2. the Xcode project and framework build;
3. config;
4. Firebase and the bridges;
5. the screens;
6. Swift tests;
7. docs and verification.

The change sets `skip_specs: true`. On Android, the existing JVM suites plus the new copy tests are the evidence that nothing changed. On iOS, the acceptance criteria are the spec requirements the proposal lists. The manual simulator check (7.4) walks them, and they're backed by the shared ViewModel tests that already exist.

Baseline before starting:
- `./gradlew test`: 85 `:app` and 187 `:shared` host tests.
- `:shared:iosSimulatorArm64Test`: 222 tests.

Work on `feature/ios-app-shell` (already created from `kotlin_multiplatform` at 9c2559d). Run `./gradlew ktlintFormat` before each commit, and commit only once the user says to.

## 1. Shared copy (Android stays green)

- [x] 1.1 Add `shared/src/commonMain/kotlin/com/raikar/moviegallery/ui/common/ErrorCopy.kt` with public `val AppError.errorTitle: String` and `val AppError.errorDetail: String`, moved verbatim from `:app`'s `ui/components/StateViews.kt`. Add `MovieLabels.kt` beside it with public `val Movie.cardSubtitle: String`, moved verbatim from `MovieCard.kt`'s `subtitleWithRating`, with its KDoc (design, decision 8). Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [x] 1.2 Add `ErrorCopyTest` and `MovieLabelsTest` in `shared/src/commonTest/.../ui/common/`:
  - every `AppError` case's title and detail, with the expected strings copied as literals from `StateViews.kt` before 1.3 removes them;
  - the subtitle when rated with genres, unrated with genres, rated with no genres, and with neither (a year, and no year).

  Verify: `./gradlew :shared:testAndroidHostTest --tests "com.raikar.moviegallery.ui.common.*"` passes.
- [x] 1.3 Point `:app`'s `StateViews.kt` at `errorTitle` / `errorDetail` and `MovieCard.kt` at `cardSubtitle`, deleting their private versions. Verify: `./gradlew :app:testDebugUnitTest` passes with 85 tests, `./gradlew assembleDebug` succeeds, and `grep -n "\"No connection\"\|\"★ " app/src/main` finds nothing.

## 2. Xcode project and framework build

- [x] 2.1 Add `.gitignore` entries: `iosApp/iosApp/GoogleService-Info.plist`, `iosApp/Configuration/Secrets.xcconfig`, `xcuserdata/`, `*.xcuserstate`, `iosApp/build/` and `DerivedData/`. Verify: `git check-ignore -v iosApp/Configuration/Secrets.xcconfig iosApp/iosApp/GoogleService-Info.plist` reports both.
- [x] 2.2 Create `iosApp/Configuration/Config.xcconfig` and `Secrets.example.xcconfig`, as listed in design decisions 2 and 3:
  - framework search path, `-framework Shared -lsqlite3`, `ENABLE_USER_SCRIPT_SANDBOXING = NO`;
  - `#include? "Secrets.xcconfig"`, an empty `TMDB_READ_ACCESS_TOKEN` default;
  - `CURRENT_PROJECT_VERSION = 2` and `MARKETING_VERSION = 1.0`.

  Verify: the files exist, and `Secrets.example.xcconfig` contains only the key with an empty value.
- [x] 2.3 Create `iosApp/iosApp.xcodeproj` (design, decision 1):
  - **app target `iosApp`:**
    - iOS 17, bundle ID `com.raikar.moviegallery`, display name "MovieGallery";
    - synchronized folder `iosApp/iosApp/`;
    - `Config.xcconfig` as the base configuration for Debug and Release;
    - a Run Script phase "Build Shared framework" before Compile Sources (design, decision 2);
  - **test target `iosAppTests`:** hosted in the app, synchronized folder `iosApp/iosAppTests/`;
  - **a shared scheme `iosApp`** that builds the app and runs the tests.

  Add a placeholder `MovieGalleryApp.swift` that `import Shared`s and shows a `Text("MovieGallery")`, plus `Info.plist` and `Assets.xcassets` with the app icon reused from Android's adaptive icon foreground. Verify: `xcodebuild -project iosApp/iosApp.xcodeproj -list` shows both targets and the scheme, and `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' build` succeeds. The build log shows the Gradle step producing `shared/build/xcode-frameworks/Debug/iphonesimulator*/Shared.framework`.
- [x] 2.4 Open the project in Xcode once and let it save. Then commit-ready the `project.pbxproj` Xcode wrote, so later diffs show only real changes (design, Risks). Verify: a second `xcodebuild ... build` succeeds and `git diff iosApp/iosApp.xcodeproj` is empty after a rebuild.

## 3. Config

- [x] 3.1 Add `TMDBReadAccessToken = $(TMDB_READ_ACCESS_TOKEN)` to `Info.plist`. Add `Platform/AppConfiguration.swift` with `enum AppConfiguration { static func make(info: [String: Any], isDebug: Bool) -> IosAppConfig }`, defaulting to an empty token and `"0"` (design, decision 3). Verify: it compiles, as in the 2.3 build.

## 4. Firebase and the bridges

- [x] 4.1 **User step:**
  1. Register the iOS app `com.raikar.moviegallery` in the Firebase console.
  2. Put `GoogleService-Info.plist` in `iosApp/iosApp/`.
  3. Create `iosApp/Configuration/Secrets.xcconfig` with the TMDB token.

  Pause here and ask. Never read or print the token. Verify: both files exist, and `git status` doesn't list either.
- [x] 4.2 Add the Swift Package Manager dependency `https://github.com/firebase/firebase-ios-sdk`, "Up to Next Major" from 13.1.1, with the products `FirebaseAuth`, `FirebaseAILogic` and `FirebaseAppCheck` on the app target. Commit `Package.resolved` (design, decision 4). Verify: `xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj` resolves 13.x, and the build from 2.3 still succeeds.
- [x] 4.3 Add `Platform/FirebaseAuthService.swift` implementing `FirebaseAuthBridge`, with an internal `func bridgedAuthError(_ error: Error) -> BridgedAuthError` and Google sign-in completing with its "isn't available on iOS yet" error (design, decision 5). Then read 13.1.1's `FirebaseAI` `GenerateContentError` source and add `Platform/FirebaseAiService.swift` implementing `FirebaseAiBridge`:
  - the model built from `ChatModelConfig` with `systemPrompt("iOS")`;
  - a fresh chat per call;
  - `identifyPoster` loading the file URL and failing with `.imageUnreadable` when it can't;
  - one completion per call, on the main actor;
  - an internal `func aiFailureKind(_ error: Error) -> AiFailureKind` covering the cases the source shows.

  Verify: the app builds.
- [x] 4.4 In `MovieGalleryApp.init`:
  1. install the App Check provider factory (debug or App Attest);
  2. call `FirebaseApp.configure()`;
  3. call `IosKoinKt.startMovieGallery` with `AppConfiguration.make(info: Bundle.main.infoDictionary ?? [:], isDebug: …)` and the two services;
  4. keep the returned graph.

  Verify: the app launches in the simulator without a crash, and the debug console shows Firebase configured and the App Check debug token line.

## 5. ViewModel helper and screens

- [x] 5.1 Read the built `Shared.h` (`shared/build/xcode-frameworks/Debug/iphonesimulator*/Shared.framework/Headers/Shared.h`) and note the Swift names the design assumes:
  - the AndroidX `ViewModel` base class;
  - `ChatModelConfig`'s accessor;
  - the `ErrorCopyKt` and `MovieLabelsKt` functions;
  - `LoginUiState` and `HomeUiState`;
  - `MovieSection`, `Movie` and `AppError`.

  Correct the design if any differs. Verify: the design names match the header.
- [x] 5.2 Add `Shared/ObservedViewModel.swift` (design, decision 6): it owns a `ViewModelOwner` and a `WatchHandle`, publishes `state` with `@Observable`, and on `deinit` cancels the handle and clears the owner, with a debug-only log line. Verify: it compiles.
- [x] 5.3 Add `RootView.swift`, which shows Login or Home from the app-lifetime `AppSessionViewModel` and swaps to Home when Login reports success, and `Login/LoginView.swift` with the copy, states and actions in design decision 7. Verify: the app builds. In the simulator while signed out, Login shows, and tapping "Don't have an account? Create account" switches the button to "Create Account".
- [x] 5.4 Add `Home/HomeView.swift`, `Home/MovieRail.swift` and `Home/ErrorStateView.swift` (design, decision 7): the loading state, rails of `AsyncImage` cards with `cardSubtitle`, and the error state with the shared copy and "Retry". Verify: the app builds, and after a sign-in in the simulator Home shows the rails.

## 6. Swift tests

- [x] 6.1 Add `iosAppTests/BridgeTranslationTests.swift` covering `bridgedAuthError` (a Firebase-shaped `NSError` and a plain Swift error) and `aiFailureKind` (timeout, other `NSURLError`, decoding, invalid API key where constructible, other). Add `iosAppTests/AppConfigurationTests.swift` covering a full dictionary, a missing token and a missing version (design, decision 9). Verify: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' test` passes.

## 7. Docs and verification

- [x] 7.1 Update `CLAUDE.md`:
  - **Intro:** `iosApp/` is the SwiftUI app consuming `:shared`.
  - **Build prerequisites:** `iosApp/iosApp/GoogleService-Info.plist`, `iosApp/Configuration/Secrets.xcconfig` (copied from the example), and the App Check debug token for AI.
  - **Commands:** the `xcodebuild` build and test commands.
  - **Repo map:** an `iosApp/` entry with its folders, and `ui/common/` in `:shared`.
  - **Backends:** the Swift bridge implementations.
  - **Tech Stack:** SwiftUI and iOS 17 for the iOS UI; Firebase via Swift Package Manager, pinned in `Package.resolved`, as the one dependency outside the version catalog.

  Update `.claude/skills/android-architecture/SKILL.md` with the iOS app layer (where Swift code goes, `ObservedViewModel`, and the rule that Swift translates and doesn't decide). Verify: both describe the same layout, and every path they name exists.
- [x] 7.2 Run `./gradlew ktlintCheck test` and `./gradlew :shared:iosSimulatorArm64Test`. Verify: everything passes, with 85 `:app` tests, the 187 `:shared` host tests plus the new copy tests, and the 222 iOS tests plus the same.
- [x] 7.3 Run `./gradlew assembleDebug` and the `xcodebuild` build and test from 2.3 and 6.1. Verify: all succeed.
- [x] 7.4 Manual simulator check, with the live panel open (design, decision 9):
  - a signed-out launch shows Login;
  - `admin` / `admin` → "Enter a valid email address";
  - a wrong password → "Invalid email or password";
  - "Create account" with a 5-character password → "Password must be at least 6 characters";
  - a valid account → a progress indicator, then Home with four rails in order, posters and subtitles;
  - the debug log shows Login's ViewModel cleared;
  - a relaunch → Home directly;
  - Mac offline → "No connection", and Retry after reconnecting → the rails return;
  - an empty token → "TMDB rejected the request".

  Result (2026-11-11, iPhone 17, iOS 26.5): every step passed except the offline one, which the user chose to skip. The Mac stayed online through a VPN, and the shared `RealEngineNetworkErrorTest` (iosTest) covers the connection-lost mapping. Found along the way: the TMDB-rejected detail copy named Android's `local.properties`, which iOS showed too; it is now platform-neutral (`ErrorCopy.kt`).

  Signing in needs the user's account, so ask the user to type the credentials, and never enter them yourself. Verify: report each step's result to the user.
- [x] 7.5 Android check on the phone: Home rails show the same subtitles, and an error state (airplane mode) shows the same copy as before. Verify: report the result to the user.

## Workflow follow-up

- Open the PR from `feature/ios-app-shell` into `kotlin_multiplatform`, then archive the change once it is merged and `./gradlew test` passes.
- Next change: 5c, the remaining SwiftUI screens on the same helper: the tab bar, Search, Detail, Watchlist, Profile (sign-out), TV shows, chat (the first real AI call from iOS) and Google sign-in.
