# Design

## Context

See proposal.md for why. The constraints that shape the approach:

- **The shared code is done.** `AiChatViewModel` (`sendMessage`, `sendPoster(imageUri:)`, `retry`, `dismissError`) and `LoginViewModel` (`onGoogleToken`, `onGoogleCancelled`, `onGoogleFailed`) are already in `MovieGalleryGraph`. `IosAiChatRepository` maps bridge failures with `mapBridgedAiFailure`, the iOS twin of Android's `safeAiCall`, and rejects empty replies with `requireReplyText`. No Kotlin logic changes.
- **The AI bridge is done.** `FirebaseAiService.identifyPoster` loads the image with `UIImage(contentsOfFile:)` from a `file://` URL and fails with `.imageunreadable` if it can't. So the Swift UI only has to put a readable image file on disk and pass its URL.
- **The auth bridge is a stub.** `FirebaseAuthService.signInWithGoogle` completes with a fixed error. On iOS, Firebase's `GoogleAuthProvider.credential(withIDToken:accessToken:)` needs both the ID token and the access token, but the shared bridge passes only the ID token, as Android's Credential Manager flow does.
- **Configuration.** `GoogleService-Info.plist` is git-ignored, and I don't read it. Google Sign-In for iOS needs the OAuth client ID (`FirebaseApp.app()?.options.clientID` gives it at runtime) and the reversed client ID registered as a URL scheme in `Info.plist` at build time. `Info.plist` already takes the TMDB token from a build setting, filled by the git-ignored `Secrets.xcconfig`.
- **Project rules.** "Compose with Material 3" and "versions in `libs.versions.toml`" apply to Android. iOS UI is SwiftUI (iOS 17+), and Swift packages are pinned by Xcode in the committed `Package.resolved`, the exception CLAUDE.md names. No Ktor client is involved, since Firebase AI and Auth go through the Swift bridges.

## Goals / Non-Goals

**Goals:**
- The chat and "Continue with Google" behave as Android's do, against the existing `ai-chat` and `authentication` specs, with the one iOS camera difference in the delta.
- Nothing in Swift decides what an error means; copy that both apps show lives in `:shared`.
- A checkout without Google configuration still builds, launches and signs in with email.

**Non-Goals:**
- Changing the shared bridge interfaces or any ViewModel.
- Matching Android's chat visuals pixel for pixel. It uses system controls with the same layout, copy and states.

## Decisions

1. **The chat is a `Route.chat` pushed from Profile.** Profile gets a "DISCOVER" section whose row is a `NavigationLink(value: Route.chat)`, and `routeDestinations` maps it to `AiChatView`, with the tab bar hidden as for Detail.
   - `ViewModelScope` creates the ViewModel when the view appears and clears it when the view leaves, so each visit starts with only the greeting (`ai-chat` "Conversation is not persisted").
   - *Alternative:* a sheet. Rejected, because the spec says it's a full-screen destination with back.

2. **The conversation is a `ScrollView` + `LazyVStack` with `.defaultScrollAnchor(.bottom)` and a `ScrollViewReader`.**
   - Each change to the last item's id (a message, or the typing bubble) scrolls to it.
   - The input bar sits in `.safeAreaInset(edge: .bottom)`, so the keyboard pushes it and the list up together.
   - The input text is local `@State`, as on Login. Send passes it to `sendMessage` and clears it. The ViewModel ignores a blank or in-flight send, so Swift doesn't re-check that.

3. **The gallery uses `PhotosPicker`, and the camera uses `UIImagePickerController` with `.camera`.**
   - `PhotosPicker` runs out of process and needs no permission. It returns a `PhotosPickerItem`, loaded as `Data`.
   - SwiftUI has no camera view, so a small `UIViewControllerRepresentable` wraps the picker. It's shown with `.fullScreenCover`.
   - "Take Photo" appears only when `UIImagePickerController.isSourceTypeAvailable(.camera)` is true. That's false on the simulator, which covers the "Device without a camera" scenario.
   - Before opening the camera, `AVCaptureDevice.authorizationStatus(for: .video)` is checked:
     - `.notDetermined` asks for access;
     - `.denied` or `.restricted` shows the camera-off message without opening anything.
     
     `Info.plist` gains `NSCameraUsageDescription`. Without it, iOS terminates the app on first camera use.
   - The "No app on this device…" messages can't happen on iOS, because the system picker and camera are always present. Their shared copy still moves, for Android.
   - *Alternative:* `AVCaptureSession` with a custom camera view. Rejected: much more code for the same result.

4. **The poster file store is `PosterFileStore`, a plain Swift type with injected directory and clock, so XCTest can cover it.**
   - It decodes the image and scales it down so the longest edge is at most 1,600 px. It then writes it as JPEG (quality 0.85) to `Caches/poster-captures/poster-<millis>.jpg` and returns the `file://` URL string.
   - Images from the photo library or camera can be 12 MP or more, often HEIC. Downscaling keeps the request small and gives Firebase AI a format it accepts.
   - A file is written only after the user picks or captures, so cancelling either source writes nothing (spec "Cancelling the camera", "Cancelling the gallery").
   - `prune(olderThan: 24h)` runs off the main thread when the chat opens, mirroring Android's `prunePosterCaptures`.
   - If the data can't be decoded, the store writes the original bytes unchanged and still returns the URL. `FirebaseAiService` then fails to load it and reports `.imageunreadable`, so the shared error row and Retry appear, with no Swift-only copy. Android behaves the same way with an unreadable pick.
   - The poster bubble reads the file with `UIImage(contentsOfFile:)`, at Android's 120 pt width and 2:3 aspect, with a neutral placeholder when it can't.

5. **Chat copy moves to `:shared` `ui/common/ChatCopy.kt`.** It holds:
   - `AppError.chatErrorMessage` (Android's private `chatMessage`, renamed so the Swift name is clear);
   - `chatTypingLabel(isIdentifyingPoster:)`, which returns "Identifying poster…" or null;
   - `NO_CAMERA_APP_MESSAGE` and `NO_PICKER_APP_MESSAGE`.
   
   It gets a `commonTest`, and Android's `AiChatScreen` imports it.
   
   The camera-off message is iOS-only, so it stays a Swift literal; Android never shows it. The static labels ("AI Movie Search", "Take Photo", the placeholder) stay in each UI, as 5c-1 kept "Search movies…".

6. **Google sign-in uses the GoogleSignIn-iOS Swift package (`GoogleSignIn` product only).** `GoogleSignInSwift`'s button doesn't match Android's "Continue with Google" style. The version is the latest stable that resolves with Firebase 13.1.1's `gtm-session-fetcher` and `GoogleUtilities` pins; 10.1.0 at the time of writing, checked at apply.
   - **Configuration, at launch in `AppEnvironment`, after `FirebaseApp.configure()`:** if `FirebaseApp.app()?.options.clientID` is non-nil, set `GIDSignIn.sharedInstance.configuration`.
   - **URL handling:** `RootView` handles callbacks with `.onOpenURL { GIDSignIn.sharedInstance.handle($0) }`.
   - **The flow:** `GoogleSignInFlow.signIn()` is an `async` function in `Login/`. It finds the key window's top view controller, calls `GIDSignIn.sharedInstance.signIn(withPresenting:)`, and returns an outcome enum mirroring Android's `GoogleSignInOutcome`:
     - `.token(idToken)`;
     - `.cancelled` for `GIDSignInError.canceled`;
     - `.failed` for anything else, logged with its `NSError`.
     
     `LoginView` maps them to `onGoogleToken` / `onGoogleCancelled` / `onGoogleFailed`. A local `isGoogleChooserOpen` keeps every control disabled while the chooser is up, as Android's does.
   - *Alternative:* Firebase's `OAuthProvider` web flow. Rejected: the native flow is what Google documents for Firebase on iOS, and it signs in with the same Google account Android does.

7. **The access token Firebase needs comes from `GIDSignIn.sharedInstance.currentUser`.**
   - `FirebaseAuthService.signInWithGoogle(idToken:)` builds the credential from the ID token Kotlin passes back, plus the current Google user's access token. It's the user who just signed in, since the flow above calls `onGoogleToken` straight after.
   - If there's no current Google user, it completes with a bridged error, which Kotlin maps to "Google sign-in failed. Please try again.".
   - `signOut()` also calls `GIDSignIn.sharedInstance.signOut()`, so the next sign-in shows the chooser rather than reusing the account silently.
   - *Alternative:* add an `accessToken` parameter to the bridge and `onGoogleToken`. Rejected: it changes the shared API and Android only to carry an iOS-specific value through Kotlin, which never reads it.

8. **The URL scheme is a build setting, and a missing one is detected before the SDK is called.**
   - `Info.plist` gets a `CFBundleURLTypes` entry whose scheme is `$(GOOGLE_REVERSED_CLIENT_ID)`. `Config.xcconfig` defaults it to empty, and each developer sets it in `Secrets.xcconfig`; the example file documents where the value comes from. I never see the value.
   - `GIDSignIn.signIn` raises an Objective-C exception, which Swift can't catch, when the scheme is missing. So `GoogleSignInSetup.problem(clientID:urlSchemes:)` first checks that a client ID exists and that a registered scheme equals the reversed client ID. It's a pure function, unit-tested.
   - Any problem returns `.failed`, logged with what's missing, so a checkout without Google setup shows the shared message instead of crashing.
   - *Alternative:* a build phase that copies `REVERSED_CLIENT_ID` from `GoogleService-Info.plist` into the built `Info.plist`. Deferred (pre-production debt): it edits a processed plist after Xcode writes it, which is fragile, and the manual step matches how the TMDB token already works.

## Risks / Trade-offs

- **GoogleSignIn-iOS may not resolve with Firebase 13.1.1's dependency pins.** → At apply, add the package and let Xcode resolve. If 10.1.0 conflicts, take the newest release that resolves, and record the version and the reason in tasks.md. Don't move Firebase in this change.
- **The App Check debug token isn't registered yet.** → Firebase AI returns 401 and the chat shows the shared "Firebase rejected the request…" copy. Apply pauses for you to register the token printed at launch, as the AI-chat memory notes. A stalled free-tier model shows as a timeout; the model name is shared config, unchanged here.
- **The access-token lookup relies on the call order** (decision 7). → The flow and the bridge both run on the main actor, and `onGoogleToken` is called straight after `signIn` returns, so `currentUser` is that user. A missing user fails cleanly rather than signing in the wrong account.
- **The camera path can't be checked on the simulator.** → The no-camera state is checked there. The permission branch is a small pure mapping (status to action) with an XCTest. Camera capture on a device is left unticked in the PR test plan.
- **Undecodable images reach the bridge as raw bytes** (decision 4), which is indirect. → It reuses the one tested error path instead of adding Swift-only copy. A comment in `PosterFileStore` explains it, and an XCTest covers it.

## Migration Plan

None. The app isn't released. Developers pull, add `GOOGLE_REVERSED_CLIENT_ID` to `Secrets.xcconfig` to use Google sign-in, and register their App Check debug token to use the chat. Without either, the app still builds and runs, and only that feature fails with its normal error.
