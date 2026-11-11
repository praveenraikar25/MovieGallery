# Tasks

No CLAUDE.md checklist covers porting a screen to iOS. The ViewModels, use cases, repositories, bridges and graph entries already exist, which covers steps 1–6 of "Adding a feature screen". These tasks go:
1. the `:shared` copy move first, while Android stays green;
2. the chat screen and poster capture;
3. Google sign-in;
4. tests, docs and verification.

Spec coverage:
- The `ai-chat` scenarios are already covered by `AiChatViewModelTest` in `commonTest` (send, history, one in flight, retry, dismiss, not persisted) and `IosAiChatRepositoryTest` in `iosTest` (bridge failure kinds, empty reply, unreadable image).
- The delta's new iOS scenarios ("Cancelling the gallery", "Device without a camera", "Camera access denied on iOS") are Swift UI behavior, so XCTest covers their pure parts (4.2), and the simulator check (5.4) covers the rest.
- The `authentication` "Continue with Google" scenarios are covered by `LoginViewModelTest`. The Swift outcome mapping and the setup check get XCTests (4.2).

Work on `feature/ios-chat-google-signin` (created from `kotlin_multiplatform` at fe03b4f). Run `./gradlew ktlintFormat` before committing, and commit only once the user says to. Take every Swift-facing name from the rebuilt `Shared.h`, not from the design. Never read or print `GoogleService-Info.plist` or `Secrets.xcconfig`.

## 1. Baseline

- [x] 1.1 Record the baseline test counts here:
  - `./gradlew test` (`:app` and `:shared` host);
  - `./gradlew :shared:iosSimulatorArm64Test`;
  - the `iosAppTests` count from `xcodebuild … test`.

  Result (2026-11-11, at fe03b4f): `:app` 63, `:shared` host 217, `:shared` iOS 252, `iosAppTests` 21. All passing.

## 2. Shared chat copy (Android stays green)

- [x] 2.1 Add `ui/common/ChatCopy.kt` in `:shared` `commonMain` (design decision 5):
  - `AppError.chatErrorMessage`, moved from `AiChatScreen.kt`'s private `chatMessage` with its comment;
  - `chatTypingLabel(isIdentifyingPoster:)`;
  - `NO_CAMERA_APP_MESSAGE` and `NO_PICKER_APP_MESSAGE`.
- [x] 2.2 Add `ChatCopyTest` in `commonTest`: one test per `AppError` case (including the HTTP code in the text), and both typing-label cases. Use Kotlin/Native-safe test names.
- [x] 2.3 Point `AiChatScreen.kt` at the shared copy, and delete the private copies. Run `./gradlew :app:testDebugUnitTest :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64`.

## 3. AI chat on iOS

- [x] 3.1 Add `Route.chat`, and map it in `MainTabView.routeDestinations` to `AiChatView` (decision 1).
- [x] 3.2 Add Profile's "DISCOVER" section, above the account rows: "Find Movies with AI" / "Chat with an assistant for personalized picks", as a `NavigationLink(value: Route.chat)`. Remove the "arrives in 5c-2" comment.
- [x] 3.3 Add `Chat/AiChatView.swift` on `graph.aiChatViewModel()` (decision 2):
  - the header title and subtitle;
  - the bubbles, typing bubble and shared typing label;
  - scroll-to-latest;
  - the input bar in a bottom safe-area inset, with local `@State` text;
  - Send and attach disabled while sending;
  - the tab bar hidden.
- [x] 3.4 Add the error row: the shared `chatErrorMessage`, with Retry and Dismiss. A local capture error takes precedence and has Dismiss only. Attach and Send clear the capture error, as on Android.
- [x] 3.5 Add `Chat/PosterFileStore.swift` (decision 4):
  - downscale to a 1,600 px longest edge and write a JPEG at quality 0.85 to `Caches/poster-captures/`;
  - write the original bytes when the data can't be decoded;
  - `prune(olderThan:)`;
  - an injected directory and clock.

  Prune off the main thread when the chat appears.
- [x] 3.6 Add the poster source sheet ("Add a movie poster", "Take Photo" only when the camera exists, "Choose from Gallery", "Cancel"), with `PhotosPicker` for the gallery (decision 3). A pick writes through the store and calls `sendPoster(imageUri:)`. A cancel writes nothing.
- [x] 3.7 Add `Chat/CameraPicker.swift`:
  - a `UIImagePickerController` representable in a full-screen cover;
  - the authorization check (`.notDetermined` requests, and `.denied` / `.restricted` show "Camera access is off. Turn it on in Settings to take a photo.");
  - `NSCameraUsageDescription` in `Info.plist`.
- [x] 3.8 Add the poster bubble: an image read from the file URL, 120 pt wide at 2:3, with a placeholder when it can't load.
- [x] 3.9 Build, then pause for you to register the simulator's App Check debug token (printed at launch) in the Firebase console. Then send one text message and one gallery poster on the simulator.
  - Result: after the token was registered, a text message got a reply, and a gallery poster showed "Identifying poster…" and was identified. Before that, Firebase AI returned 401 (App Check) and the row showed "Couldn't get a reply. Tap retry to try again.", as on Android.

## 4. Google sign-in on iOS

- [x] 4.1 Add the GoogleSignIn-iOS package (product `GoogleSignIn`) to the app target at its latest stable release that resolves with Firebase 13.1.1 (decision 6). Commit the updated `Package.resolved`. If 10.1.0 doesn't resolve, record the version used and why here.

  Result: 10.1.0 resolved, adding AppAuth 3.0.0 and GTMAppAuth 6.0.0. Firebase 13.1.1, GTMSessionFetcher 5.3.1 and GoogleUtilities 8.1.5 are unchanged.
- [x] 4.2 Add `Login/GoogleSignInFlow.swift`:
  - `GoogleSignInSetup.problem(clientID:urlSchemes:)`;
  - the outcome enum and its mapping from `GIDSignInError`;
  - `signIn()`, presenting from the key window's top view controller.

  Add XCTests in `iosAppTests` for the setup check (no client ID, missing scheme, mismatched scheme, OK), the outcome mapping, and the camera authorization mapping from 3.7.

  Result: `ChatAndGoogleSignInTests`, 16 tests, also covering `PosterFileStore` (downscale, small image kept, undecodable bytes kept, prune by age). `iosAppTests` went from 21 to 37, all passing.
- [x] 4.3 Add `GOOGLE_REVERSED_CLIENT_ID` (decision 8):
  - empty in `Config.xcconfig`;
  - documented in `Secrets.example.xcconfig`;
  - a `CFBundleURLTypes` entry in `Info.plist`.

  Pause for you to add the value to your `Secrets.xcconfig`. I don't open either file.
- [x] 4.4 In `MovieGalleryApp`, configure `GIDSignIn` from `FirebaseApp.app()?.options.clientID` after `FirebaseApp.configure()`, skipped as a test host. Add `.onOpenURL` in `RootView`.
- [x] 4.5 Implement `FirebaseAuthService.signInWithGoogle` with the current Google user's access token, or a bridged error when there isn't one (decision 7). Make `signOut` also sign out of `GIDSignIn`.
- [x] 4.6 Add the "OR" divider and "Continue with Google" button (with the Google "G" from Android's `ic_google_g` as an asset) to `LoginView`. Use a local `isGoogleChooserOpen` so every control is disabled while the chooser is open or a request is loading.
- [x] 4.7 Simulator check, which you do with your own Google account; I don't enter credentials:
  - a successful Google sign-in lands on Home;
  - dismissing the chooser stays on Login with no error;
  - sign out, then "Continue with Google" shows the chooser again.
  - Result: all three pass. You signed in and landed on Home with the Google account on Profile; cancelling the system prompt stayed on Login with no error; after Sign Out, "Continue with Google" showed the prompt again.

## 5. Docs and verification

- [x] 5.1 Update CLAUDE.md: iOS has every screen; `Chat/`, the GoogleSignIn package, `GOOGLE_REVERSED_CLIENT_ID`, `ChatCopy.kt`. Update `.claude/skills/android-architecture/SKILL.md` where it lists the iOS gaps.
- [x] 5.2 Run `./gradlew ktlintCheck`, `./gradlew test`, `./gradlew assembleDebug` and `./gradlew :shared:iosSimulatorArm64Test`.
  - Result: all pass. `:app` 63 (unchanged), `:shared` host 217 → 226, `:shared` iOS 252 → 261 (the 9 `ChatCopyTest` tests).
- [x] 5.3 Run `xcodebuild … build` and `xcodebuild … test`, and record the counts against 1.1.
  - Result: build and test succeed; `iosAppTests` 21 → 37, all pass.
- [x] 5.4 Simulator walk-through of the specs:
  - `ai-chat`: greeting; text send and reply; follow-up uses history; controls disabled in flight; gallery poster with "Identifying poster…"; cancelling the gallery sends nothing; no "Take Photo" on the simulator; an error row with Retry, and only one copy after retry; leaving and reopening shows only the greeting.
  - `authentication`: the Google scenarios from 4.7.
  - Landscape on the chat.
  - Result: all pass on iPhone 17 (iOS 26.5).
    - A follow-up ("Which of those three is the oldest?") was answered from the earlier reply, so history is sent.
    - Attach and Send are disabled while a reply is pending.
    - Closing the gallery picker sends nothing.
    - Leaving and reopening the chat shows only the greeting.
    - In landscape the input bar spans the width, bubbles keep their 280 pt cap, and the conversation survives the rotation.
    - The camera-denied path shows the iOS message with Dismiss only.
    - The iOS 26 simulator reports a camera, so "Take Photo" shows there. The rule (hidden when `isSourceTypeAvailable(.camera)` is false) still holds; this simulator just has one.
    - Found and fixed: the "Add a movie poster" dialog was anchored mid-screen. It now sits on the attach button.
- [x] 5.5 Check that Android's chat and Login look the same as before the copy move.
  - Result: on the Pixel 9 emulator the chat looks as before: greeting, user bubble, typing bubble, and the error row with Retry and Dismiss.
    - The error row shows the shared `chatErrorMessage` text, "Couldn't get a reply. Tap retry to try again."
    - The failure itself was this emulator's unregistered App Check debug token.
    - Retry left one user message and one error row.
    - Login wasn't re-checked: this change doesn't touch any Android file but `AiChatScreen.kt`.

## Workflow follow-up

- Archive with `/opsx:archive`, commit and open a PR into `kotlin_multiplatform` once you say so.
