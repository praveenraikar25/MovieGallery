# Proposal

## Why

Phase 5b of the KMP migration. After 5a (PR #36), the full shared graph resolves on iOS and a static `Shared` framework links. But nothing runs on an iPhone yet: there is no Xcode project, no Swift code, no Firebase iOS setup, and the two Firebase bridges have only Kotlin fakes.

This change builds the iOS app shell and proves the whole path on the simulator with real backends. A user signs in through Firebase Auth from a SwiftUI Login screen, using the shared `LoginViewModel`, and lands on a SwiftUI Home that shows TMDB rails from the shared `HomeViewModel`.

That covers every layer phase 5c will build on: the framework build, Koin startup, config, both Firebase bridges, `ViewModelOwner` / `StateFlowWatcher`, and the shared error copy. 5c can then add the remaining screens without changing any of it. The first screen is **Login, then Home** (decided 2026-11-10).

## What Changes

- **An Xcode project, `iosApp/`, at the repo root.**
  - It's a checked-in `iosApp.xcodeproj` that uses Xcode 26's synchronized folders, so adding a Swift file doesn't touch the project file. No XcodeGen or Tuist is needed.
  - It has one app target, `iosApp` (bundle ID `com.raikar.moviegallery`, iOS 17+), and one unit-test target, `iosAppTests`.
- **The project links the `:shared` framework through Gradle.** A build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, so every Xcode build compiles the current Kotlin. The framework search path and `-lsqlite3` are set in a committed `xcconfig`.
- **Per-build config.**
  - The TMDB token comes from a git-ignored `iosApp/Configuration/Secrets.xcconfig` (a committed `Secrets.example.xcconfig` shows the key). It reaches Swift through `Info.plist`.
  - The app version comes from the project's build number.
  - `isDebug` is set from the Swift `DEBUG` flag.

  Together they make the `IosAppConfig` that `startMovieGallery` takes. Without a token, the app still builds and TMDB calls return 401, as on Android.
- **Firebase on iOS:**
  - the Firebase iOS SDK via Swift Package Manager: FirebaseAuth, Firebase AI Logic and FirebaseAppCheck, at the latest stable release;
  - a git-ignored `GoogleService-Info.plist`, which you download after registering the iOS app in the Firebase console;
  - `FirebaseApp.configure()` and App Check (the debug provider in debug builds, App Attest in release) at launch, before Koin starts.
- **Swift implementations of the two bridges:**
  - **`FirebaseAuthService`** implements `FirebaseAuthBridge`. It passes each `NSError`'s domain, code and description through untouched, and Kotlin maps them.
  - **`FirebaseAiService`** implements `FirebaseAiBridge`. It builds the model from `ChatModelConfig` (model name, timeout, `systemPrompt("iOS")`, poster prompt) and sorts SDK errors into `AiFailureKind`.

  The AI bridge is wired into the graph but no 5b screen calls it; the chat screen arrives in 5c.
- **The SwiftUI app:**
  - `MovieGalleryApp` starts Firebase, then Koin, and keeps the `MovieGalleryGraph`.
  - A root view shows Login or Home according to `AppSessionViewModel.isSignedIn`. A successful sign-in replaces Login with Home, with no way back.
  - A small generic helper pairs `ViewModelOwner` with `StateFlowWatcher`, so each SwiftUI view observes one ViewModel's state and clears the ViewModel when the view goes away.
- **The Login screen:**
  - email and password fields, "Log In" / "Create Account", and the mode toggle, using the same copy as Android;
  - the shared ViewModel's validation and error messages;
  - a progress indicator and disabled buttons while a request is in flight.
  
  There's no Google button yet (5c).
- **The Home screen:**
  - "MovieGallery" as the title;
  - one loading state, then the rails in the order the shared ViewModel returns them, each a horizontal row of cards with a poster (`AsyncImage`), a title and the same subtitle as Android;
  - a full-screen error with Retry when every rail is unavailable.

  Tapping a card and the avatar do nothing yet (5c).
- **Copy moved to `:shared` so Swift shows the same text:**
  - the error title and detail for each `AppError`, from `:app`'s `StateViews.kt`;
  - the movie card subtitle, from `:app`'s `MovieCard.kt`.

  Android reads both from `:shared` instead, so its text doesn't change. New `commonTest` tests pin each string.
- **Tests:**
  - `iosAppTests` (XCTest) covers the pure Swift logic: turning an `NSError` into a `BridgedAuthError`, sorting AI errors into `AiFailureKind`, and reading `IosAppConfig` from `Info.plist` values.
  - A manual simulator check covers the end-to-end path.
- **Docs:** `CLAUDE.md` gets the iOS build prerequisites, commands and repo map, and the `android-architecture` skill gets the iOS app layer.

Backends touched: TMDB (Home rails, from iOS) and Firebase Auth (sign-in and sign-up, from iOS). Firebase AI is configured on iOS but not called from any 5b screen. The BFF and the Countries API aren't reached from iOS in 5b. Android's calls to all five backends are unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. The specs describe behavior without naming a platform, and Android's behavior doesn't change, so the change sets `skip_specs: true`.

On iOS, this change must meet these existing requirements, which are its acceptance criteria:
- **`app-navigation`:** "App starts at login". The main screen on iOS in 5b is Home on its own, without the tab bar.
- **`authentication`:**
  - "Sign-in requires both fields";
  - "Editing a field clears the error";
  - "Successful sign-in removes login from history";
  - "Email and password sign-in";
  - "Email format is checked";
  - "Account creation";
  - "Auth requests show progress and block repeat taps" (without the Google button);
  - "Auth failures show specific messages";
  - "Sign-in is remembered" (the relaunch-after-sign-in scenario).
- **`movie-browse`:**
  - "Home shows four movie rails in a fixed order";
  - "Rails load together behind one loading state";
  - "A failed or empty rail is hidden";
  - "Home shows an error only when every rail is unavailable";
  - "Missing token is called out";
  - "Movie cards summarize a movie".

On iOS, 5c delivers the rest:
- the tab bar and its animations;
- Movie Detail and every other screen;
- Google sign-in;
- sign-out, and with it the relaunch-after-sign-out scenario;
- tapping a card;
- the Home avatar.

Until then the iOS app isn't shippable, which is why that gap is listed below rather than as a spec note.

## Out of scope

- Every screen except Login and Home, the tab bar, navigation to Movie Detail, and the Home avatar (5c).
- Google Sign-In on iOS and its credential question from the 5a design (5c).
- Sign-out on iOS (5c, with Profile).
- Any call to Firebase AI from iOS, and the chat UI (5c). The bridge is implemented and wired, but no screen calls it.
- Making the iOS UI look like Android's (theme, film-reel loader, card styling). 5b uses stock SwiftUI with the same copy, and 5c polishes it.
- CI for the iOS build, TestFlight, code signing for a real device, and App Store setup.
- Changes to the Firebase project beyond registering the iOS app and adding its App Check debug token. The user does both in the console.
- Swift interop plugins (SKIE and the like). The 5a decision stands.

## Pre-production debt

- **Google sign-in isn't available on iOS.** The Swift auth bridge's `signInWithGoogle` completes at once with an error, and no 5b screen can call it. 5c replaces it with the real Google Sign-In flow.
- **The iOS app is incomplete:** two screens, no tab bar, no sign-out. It's runnable on the simulator for development only. 5c completes it before the app is shippable.
- **The token message no longer names `local.properties`.** It was Android's developer copy, and once shared it pointed iOS at the wrong file. It now says the token is missing or invalid and to check the token this build was set up with, on both platforms (found in the simulator, task 7.4). The `movie-browse` note about developer-facing copy is now out of date. This change skips specs, so the note is left as is, and the next change that edits `movie-browse` should drop it.

## Impact

- **Build:**
  - New `iosApp/`: the Xcode project, Swift sources, `Configuration/*.xcconfig`, `Info.plist` and the unit-test target.
  - New Swift Package Manager dependency on `firebase-ios-sdk`, at its latest stable release, pinned by `Package.resolved`, which is committed. Swift packages can't be declared in `gradle/libs.versions.toml`, so the version catalog rule doesn't apply here.
  - `.gitignore` adds `iosApp/iosApp/GoogleService-Info.plist`, `iosApp/Configuration/Secrets.xcconfig`, `xcuserdata/` and Xcode build output.
- **Code:**
  - **`:shared` `commonMain`** gains `ui/common/ErrorCopy.kt` and `ui/common/MovieLabels.kt`.
  - **`:app`'s `StateViews.kt` and `MovieCard.kt`** read the copy from `:shared` and drop their own versions.
- **Tests:**
  - new `commonTest` tests for the moved copy;
  - new `iosAppTests` (XCTest);
  - the JVM and Kotlin/Native suites stay green.
- **Users:**
  - Android: none.
  - iOS: a development build on the simulator that signs in and shows Home.
- **Setup you do once:**
  - register an iOS app with bundle ID `com.raikar.moviegallery` in the Firebase console;
  - download `GoogleService-Info.plist` into `iosApp/iosApp/`;
  - copy `Secrets.example.xcconfig` to `Secrets.xcconfig` and add the TMDB token;
  - register the simulator's App Check debug token when you first need AI (5c).
