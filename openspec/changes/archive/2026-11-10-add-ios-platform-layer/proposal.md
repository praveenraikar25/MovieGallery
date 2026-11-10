# Proposal

## Why

Phase 5a of the KMP migration. Since phase 4b, the domain layer, the data layer, the database and all nine ViewModels are in `:shared`. But the shared Koin graph only resolves on Android, because only `:app` supplies what `sharedModules` asks a platform for: an HTTP engine factory, `NetworkConfig`, an `ApolloClient`, `AuthRepository`, `AuthTokenProvider` and `AiChatRepository`. iOS has a database module and the Swift bridge (`StateFlowWatcher`, `ViewModelOwner`) but nothing else, so a Swift app could not start Koin or get a single ViewModel today.

This change adds everything iOS must supply and proves it with tests, before any Xcode project exists. Phase 5b (the iOS app shell) can then start from a graph known to resolve, and phase 5c (the SwiftUI screens) from ViewModels Swift can already reach.

Phase 5 is three changes, in this order:
- **5a, iOS platform layer** (this change);
- **5b, iOS app shell:** an Xcode project consuming the `:shared` framework, Koin startup, config, Firebase setup, and one screen working end to end;
- **5c, SwiftUI screens:** the rest of the screens, on the bridge.

## What Changes

- **Firebase reaches `:shared` through Swift-implemented bridges** (decided 2026-11-10; design, decision 1). Firebase AI Logic has no Kotlin Multiplatform SDK, and linking the Firebase iOS frameworks into `:shared` would make every iOS test binary depend on them. Instead:
  - `iosMain` declares two small callback interfaces, a Firebase Auth bridge and a Firebase AI bridge. The Swift app implements them with the Firebase iOS SDK in 5b.
  - Kotlin adapters turn them into `AuthRepository`, `AuthTokenProvider` and `AiChatRepository`.
  - The adapters carry the same error mapping as Android's `safeAuthCall` / `safeAiCall`. Auth maps Firebase iOS error codes to the same `AuthError`s, including "unknown account reads as wrong password". AI maps failure kinds to the same `AppError`s, including "a timeout is not a network failure".
- **The iOS platform module** (`iosMain` `di/`) supplies:
  - a Darwin `HttpClientEngine` factory (a new engine per client, as on Android);
  - `NetworkConfig`, built from a config object Swift passes in (TMDB token, app version, debug flag);
  - an `ApolloClient` for the Countries API;
  - the three bridge-backed bindings above.
- **An iOS Koin entry point** that Swift calls once at launch with the config and the two bridges. It starts Koin with `sharedModules` plus the iOS module, and exposes one function per ViewModel (with `movieId` for Movie Detail) so Swift can pass them to `ViewModelOwner`. Swift can't call Koin's reified `get<T>()`, so these functions are how it reaches a ViewModel.
- **Shared AI chat settings**: the model name, request timeout, system prompt and poster prompt move from `:app` to `:shared` `commonMain`, so both apps send Gemini the same instructions. The prompt takes the platform name as a parameter, and Android passes "Android", so the text it sends doesn't change. The "model returned no text" rule moves too, so both repositories apply it.
- **The Countries URL** moves to `:shared`, read by both platforms' Apollo clients.
- **An iOS framework binary** (`Shared`, static) is declared on both iOS targets, so the Swift-facing API can be linked and its Objective-C header checked now. 5b consumes it.
- **Tests in `iosTest`**, all with fake bridges:
  - a graph test that starts the real iOS graph and resolves every use case, every ViewModel through the entry point, both HTTP clients (each with its own engine) and the Apollo client;
  - adapter tests for auth (session state, the auth-state stream, each error code, sign-out, ID tokens) and for AI (replies, empty replies, each failure kind, an unreadable image, a callback that fires twice).
- **Docs:** `CLAUDE.md` (repo map, `di/` section, Commands) and the `android-architecture` skill describe the iOS platform module, the bridges and the entry point.

Backends touched: none. Android reaches TMDB, the BFF, Firebase Auth, Firebase AI and the Countries API exactly as before. iOS code paths to the same backends are added but not yet called by any app.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. Nothing a user sees changes on Android, and there is no iOS app yet, so the change sets `skip_specs: true`. Every existing spec must still hold. These are the requirements most at risk, because their logic is being moved or reproduced:
- `ai-chat`: the assistant's instructions (the system prompt moves) and the error shown for an empty or failed reply;
- `authentication`: the sign-in and sign-up error messages, which the iOS adapter must map to the same `AuthError`s.

## Out of scope

- The Xcode project, Swift code, Firebase iOS setup (`GoogleService-Info.plist`, App Check), and how the iOS app gets its TMDB token (5b).
- The Swift implementations of the two bridges (5b). This change defines the interfaces and tests them against Kotlin fakes.
- SwiftUI screens, iOS navigation, iOS image loading and iOS Google Sign-In (5c).
- SKIE, KMP-NativeCoroutines, GitLive's Firebase SDK, or any other interop compiler plugin or wrapper (design, decisions 1 and 4).
- Moving `:app`'s Firebase implementations into `:shared`. Android keeps its own Firebase Auth and Firebase AI code.
- Rewording the system prompt beyond naming the platform.

## Pre-production debt

None. No placeholder behavior is introduced. The iOS bindings are real adapters; only their Swift halves arrive in 5b.

## Impact

- **Build:**
  - `shared/build.gradle.kts` declares a static `Shared` framework on `iosArm64` and `iosSimulatorArm64`.
  - No new libraries or plugins. Ktor Darwin, Apollo's iOS runtime and Koin core are already `:shared` dependencies.
- **Code:**
  - `:shared` `iosMain` gains:
    - the two bridge interfaces and their error types;
    - `IosAuthRepository`, `IosAuthTokenProvider` and `IosAiChatRepository`;
    - the iOS platform module;
    - the iOS Koin entry point.
  - `:shared` `commonMain` gains the AI chat settings, the reply-text rule and the Countries URL.
  - `:app`'s `AiModule`, `AiChatRepositoryImpl`, `GeminiChatDataSource` and `AndroidModules` read those from `:shared` and drop their own copies.
- **Tests:**
  - New `iosTest` suites for the graph and the adapters.
  - The JVM suites are unchanged. `KoinGraphTest` and `KoinGraphResolutionTest` must still pass.
- **Users:** none. Android behaves as today; no iOS app ships yet.
