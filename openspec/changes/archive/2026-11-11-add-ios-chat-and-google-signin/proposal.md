# Proposal

## Why

This is phase 5c-2 of the KMP migration, and its last iOS screen work. After 5c-1 (PR #38), the iOS app has every screen except two: the AI chat, and "Continue with Google" on Login. The shared code for both is already there. `AiChatViewModel` and `LoginViewModel.onGoogleToken` run on iOS, and the Firebase AI bridge was built in 5a. What's missing is the Swift UI, the poster capture, and a Google account chooser on iOS.

These were split off from 5c-1 because each needs setup outside the code:
- The AI chat makes the first real Firebase AI call from iOS, so the simulator's App Check debug token must be registered in the Firebase console.
- Google sign-in needs a new Swift package (Google Sign-In for iOS) and a URL scheme from the Firebase iOS app's configuration.

## What Changes

- **Profile gains a "DISCOVER" section** above the account rows, as on Android. It has one entry, "Find Movies with AI", with the subtitle "Chat with an assistant for personalized picks". Tapping it pushes the chat onto the Profile tab's stack with the tab bar hidden.
- **The AI chat screen**, on the shared `AiChatViewModel`:
  - the "AI Movie Search" title with "Ask for a recommendation" under it, and a back button;
  - the local greeting, then user and assistant bubbles, with a poster bubble for a sent image;
  - a typing bubble while a reply is pending, reading "Identifying poster…" for a poster;
  - an input bar ("Message AI Movie Search…") with Send and an attach button, all disabled while a request is in flight;
  - the error row beside the input with Retry and Dismiss, using the same per-error copy as Android;
  - the list scrolls to the newest message, and stays there when the keyboard opens.
  
  Leaving and reopening the chat starts a new conversation, because each push creates a new ViewModel.
- **Poster capture on iOS.** The attach button opens a sheet with "Take Photo" (only on a device with a camera, so never on the simulator) and "Choose from Gallery".
  - The gallery uses the system photo picker, which needs no permission.
  - The camera uses the system camera. On iOS that always asks for camera access the first time, which Android doesn't. If access is denied, the error row says "Camera access is off. Turn it on in Settings to take a photo." with Dismiss and no Retry.
  - The chosen image is written as a downscaled JPEG to the app's caches, and its `file://` URL goes to `sendPoster`. Cancelling the camera or the picker writes nothing. Files older than a day are removed when the chat opens, as on Android.
- **"Continue with Google" on Login**, below an "OR" divider, as on Android.
  - It opens Google's account chooser. Picking an account signs in through `onGoogleToken`. Dismissing it does nothing. Any other failure shows the shared "Google sign-in failed. Please try again.".
  - All three controls are disabled while the chooser is open or a request is in flight.
  - Sign-out also clears the Google session, so the next "Continue with Google" asks for an account again.
  - If the app isn't set up for Google sign-in (no client ID or URL scheme), the button fails with the shared message rather than crashing, and the console says what's missing.
- **The chat's error copy moves to `:shared`** (`ui/common/ChatCopy.kt`): the per-`AppError` message, the typing label and the two "No app on this device…" messages. Android reads it from there, so its behavior doesn't change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `ai-chat`: "User can identify a movie from a poster image" says no runtime permission is requested. That stays true on Android, but iOS can't open the camera without asking. The requirement gains the iOS camera prompt and the message shown when access is denied.

## Impact

- **Backends:** Firebase AI, the first real calls from iOS, through the existing `FirebaseAiBridge`. Firebase Auth gains a working `signInWithGoogle` on iOS. TMDB and the BFF are untouched.
- **iOS code:**
  - new: `iosApp/iosApp/Chat/` (chat screen, bubbles, input bar, poster sheet, poster file store), the Google sign-in helper in `Login/`;
  - changed: `Route` (a `.chat` case) and `MainTabView`, `ProfileView`, `LoginView`, `FirebaseAuthService.signInWithGoogle` and `signOut`, `MovieGalleryApp` (Google configuration and URL handling), `Info.plist` (URL scheme, camera usage description).
- **Dependencies:** the GoogleSignIn-iOS Swift package, at its latest stable release that resolves alongside Firebase iOS SDK 13.1.1. `Package.resolved` changes.
- **Configuration:** a new `GOOGLE_REVERSED_CLIENT_ID` build setting, empty in `Config.xcconfig` and filled in each developer's git-ignored `Secrets.xcconfig`. `Secrets.example.xcconfig` documents it.
- **`:shared`:** `ui/common/ChatCopy.kt` and its test. No ViewModel, repository or bridge interface changes.
- **`:app`:** `AiChatScreen.kt` imports the moved copy. No behavior change.
- **Console setup (yours, not code):**
  - register the simulator's App Check debug token, which the debug console prints at launch;
  - make sure the Firebase iOS app's `GoogleService-Info.plist` was downloaded after the Google provider was enabled, so it has a client ID, then copy its reversed client ID into `Secrets.xcconfig`.
- **Docs:** CLAUDE.md (iOS now has every screen; the new build setting), the `android-architecture` skill.

### Out of scope

- Android changes beyond the copy move.
- Saving the chat, or a movie card in the chat reply (declined in phase 3d).
- Sign in with Apple, or any other new sign-in provider.
- Custom tab-bar animations (5c-1 debt, still open).
- Running the camera path on a physical iPhone. The simulator has no camera, so "Take Photo" is checked only for being hidden.

### Pre-production debt

- The reversed client ID is copied into `Secrets.xcconfig` by hand. A build phase could read it from `GoogleService-Info.plist` instead.
- Camera access denied has no "Open Settings" shortcut; the message says where to go.
