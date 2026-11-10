# Tasks

This change adds the iOS platform layer under `sharedModules`. No CLAUDE.md checklist matches it exactly. The closest is "Adding a feature screen" steps 3–5 (repository implementation, then DI registration, then graph tests), and these tasks follow that order: shared settings first, then bridges, adapters, DI, the framework, tests and docs.

The change sets `skip_specs: true`, so every existing spec must still hold. The evidence:
- the JVM suites pass unchanged, plus the new `ChatModelConfigTest`, which pins Android's prompt;
- the iOS adapter tests show iOS maps each failure the way Android does.

Before starting, record the baseline counts for `./gradlew test` (`:app` and `:shared` host) and `./gradlew :shared:iosSimulatorArm64Test`. The verification tasks compare against them.

Work on a new branch `feature/ios-platform-layer` from `kotlin_multiplatform` (design, Migration Plan). Run `./gradlew ktlintFormat` before each commit, and commit only once the user says to.

## 1. Shared chat settings, reply rule and Countries URL (Android stays green)

- [x] 1.1 Add `shared/src/commonMain/kotlin/com/raikar/moviegallery/data/remote/ai/ChatModelConfig.kt`: a public `object ChatModelConfig` with `MODEL_NAME`, `REQUEST_TIMEOUT_MILLIS`, `IDENTIFY_POSTER_PROMPT` and `fun systemPrompt(platformName: String): String`. Move the KDoc from `AiModule.kt` (why not `gemini-3.7-flash`, why 60 seconds, why the image rules live in the system prompt) with the values. Copy the prompt text verbatim, except "an Android app" becomes "an $platformName app" (design, decision 4). Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [x] 1.2 Add `ChatModelConfigTest` in `shared/src/commonTest/.../data/remote/ai/`, asserting that `systemPrompt("Android")` equals the prompt string currently in `:app`'s `AiModule.kt`. Copy that string into the test as a literal before deleting it from `:app`. Verify: `./gradlew :shared:testAndroidHostTest --tests "*ChatModelConfigTest"` passes.
- [x] 1.3 Move `requireText()` from `AiChatRepositoryImpl` to a public `fun DataResult<String?>.requireReplyText(): DataResult<String>` in `shared/src/commonMain/.../data/remote/ai/ReplyText.kt`, with its KDoc, and add a `commonTest` test covering a trimmed reply, a null reply, a blank reply and a passed-through failure. Verify: the test passes on the JVM.
- [x] 1.4 Move `COUNTRIES_URL` from `:app` `AndroidModules.kt` to `shared/src/commonMain/.../data/remote/CountriesEndpoint.kt`, with its KDoc. Verify: `grep -rn "COUNTRIES_URL" app/src/main shared/src/commonMain` shows one definition, in `:shared`.
- [x] 1.5 Switch `:app` over:
  - `AiModule.kt` uses `ChatModelConfig.MODEL_NAME`, `REQUEST_TIMEOUT_MILLIS` and `systemPrompt("Android")`, and drops its private copies;
  - `GeminiChatDataSource` uses `ChatModelConfig.IDENTIFY_POSTER_PROMPT`;
  - `AiChatRepositoryImpl` calls `requireReplyText()`;
  - `AndroidModules.kt` imports `COUNTRIES_URL`.

  Verify: `./gradlew :app:testDebugUnitTest` passes with the baseline count, and `./gradlew assembleDebug` succeeds.

## 2. Bridge interfaces and error mapping (`iosMain`)

- [x] 2.1 Add `shared/src/iosMain/kotlin/com/raikar/moviegallery/platform/`:
  - `FirebaseAuthBridge.kt`: the interface from design decision 2, plus `BridgedUser`, `BridgedAuthError` and `BridgeRegistration`;
  - `FirebaseAiBridge.kt`: the interface, plus `BridgedAiFailure` and `AiFailureKind` (`Unauthorized`, `Network`, `Timeout`, `Serialization`, `ImageUnreadable`, `Other`).

  The KDoc on each method says the completion must be called exactly once, on any queue. The KDoc on `addAuthStateListener` says the registration is removed when collection stops. The KDoc on each `AiFailureKind` entry names the Firebase iOS errors Swift should map to it. Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [x] 2.2 Confirm the Firebase Auth error codes in design decision 3 against the Firebase iOS SDK's `FIRAuthErrors.h` (`FIRAuthErrorCode`), on the latest stable `firebase-ios-sdk` tag. Fix the design table if any code differs. Verify: the design table lists each code with its source line, or a note saying it was confirmed.
- [x] 2.3 Add `shared/src/iosMain/.../data/remote/auth/BridgedAuthErrors.kt`:
  - `internal fun mapBridgedAuthError(error: BridgedAuthError): AuthError`, with one named constant per code (its Swift case name in a comment) and the `NSURLErrorDomain` → `Network` rule;
  - `BridgedAuthException(domain, code, message)` as the `Unknown` cause.

  The KDoc points at `:app`'s `safeAuthCall` as the behavior it mirrors, including why an unknown account reads as a wrong password. Verify: it compiles, as in 2.1.
- [x] 2.4 Add `shared/src/iosMain/.../data/remote/ai/BridgedAiErrors.kt`: `internal fun mapBridgedAiFailure(failure: BridgedAiFailure): AppError` and `BridgedAiException(kind, message)`, mapping as design decision 3 lists, with the "timeout is not a network failure" comment carried over from `safeAiCall`. Verify: it compiles.

## 3. Adapters (`iosMain`)

- [x] 3.1 Add `shared/src/iosMain/.../data/repository/IosAuthRepository.kt` implementing `AuthRepository` over `FirebaseAuthBridge`, as in design decision 7:
  - `currentUserId` and `currentUser` are `callbackFlow`s that send the current user first, register the listener, remove it in `awaitClose`, and are `distinctUntilChanged`;
  - every call waits on a `CompletableDeferred` (design, decision 2).

  Verify: it compiles.
- [x] 3.2 Add `shared/src/iosMain/.../data/remote/auth/IosAuthTokenProvider.kt` implementing `AuthTokenProvider` over the same bridge. Verify: it compiles.
- [x] 3.3 Add `shared/src/iosMain/.../data/repository/IosAiChatRepository.kt` implementing `AiChatRepository` over `FirebaseAiBridge` and an injected dispatcher. A completion with a failure maps through `mapBridgedAiFailure`; otherwise the text goes through `requireReplyText()`. Verify: it compiles.

## 4. iOS platform module and entry point

- [x] 4.1 Add `IosAppConfig(tmdbAccessToken, appVersion, isDebug)` with a redacting `toString()`, and `iosPlatformModule(config, authBridge, aiBridge)` in `shared/src/iosMain/.../di/IosPlatformModule.kt`, as design decision 6 lists:
  - a Darwin engine `factory`;
  - `NetworkConfig` with `LogLevel.ALL` + `Logger.SIMPLE` when `isDebug`, else `NONE`;
  - `ApolloClient` on `COUNTRIES_URL`, with `LoggingInterceptor` when `isDebug`;
  - both bridges as `single`s;
  - the three adapters bound to their interfaces, with `get(IoDispatcher)` for the AI repository.

  Confirm `LoggingInterceptor` is in Apollo 5's common runtime. If it isn't, leave it out and note that in the design. Verify: it compiles.
- [x] 4.2 Add `shared/src/iosMain/.../di/IosKoin.kt` (design, decision 5):
  - `internal fun iosModules(...) = sharedModules + iosPlatformModule(...)`;
  - `fun startMovieGallery(config, authBridge, aiBridge): MovieGalleryGraph`, which calls `startKoin`;
  - `class MovieGalleryGraph internal constructor(koin: Koin)` with one function per ViewModel in `viewModelModule`, and `movieDetailViewModel(movieId: Int)` passing `parametersOf(movieId)`.

  The KDoc shows the Swift call (`IosKoinKt.startMovieGallery(...)`) and its use with `ViewModelOwner`. Update the KDoc on `ViewModelOwner` to point at `MovieGalleryGraph` instead of the "phase 5" note, and the `sharedModules` KDoc to name both platforms' modules. Verify: it compiles.

## 5. Framework binary

- [x] 5.1 In `shared/build.gradle.kts`, add `binaries.framework { baseName = "Shared"; isStatic = true }` to both iOS targets in the existing `forEach`, with a comment saying the iOS app (5b) consumes it and that `-lsqlite3` already applies through `configureEach` (design, decision 8). Verify: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` succeeds, and `grep -E "startMovieGallery|MovieGalleryGraph|FirebaseAuthBridge|FirebaseAiBridge|IosAppConfig" shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Headers/Shared.h` finds all five names.

## 6. Tests (`iosTest`)

- [x] 6.1 Add `FakeFirebaseAuthBridge` and `FakeFirebaseAiBridge` in `shared/src/iosTest/kotlin/com/raikar/moviegallery/testutil/`. Each records its calls, completes with a scripted result, can complete twice on request, and counts active listener registrations. Verify: `./gradlew :shared:compileTestKotlinIosSimulatorArm64` passes.
- [x] 6.2 Add `IosAuthRepositoryTest` in `shared/src/iosTest/.../data/repository/`, covering every auth case in design decision 9. That includes one test per row of the decision 3 table and the `NSURLErrorDomain` case, plus `IosAuthTokenProvider`. Verify: `./gradlew :shared:iosSimulatorArm64Test --tests "*IosAuthRepositoryTest"` passes.
- [x] 6.3 Add `IosAiChatRepositoryTest` in the same package, covering every AI case in design decision 9. Verify: `./gradlew :shared:iosSimulatorArm64Test --tests "*IosAiChatRepositoryTest"` passes.
- [x] 6.4 Add `IosKoinGraphTest` in `shared/src/iosTest/.../di/`, set up and checking what design decision 9 lists:
  - a `koinApplication` from `iosModules(...)` with fake bridges, a `MockEngine` factory and an in-memory database as overrides;
  - `Dispatchers.setMain(StandardTestDispatcher())`;
  - every use case resolves;
  - every `MovieGalleryGraph` ViewModel function returns an instance, cleared through `ViewModelOwner`;
  - the function count equals `viewModelModule`'s definition count;
  - the two clients get different engines, and closing the graph closes both;
  - `ApolloClient` resolves;
  - a separate test on the unmodified graph shows the engine factory gives two distinct Darwin engines.

  Close the database and the application in `@AfterTest`. Verify: `./gradlew :shared:iosSimulatorArm64Test --tests "*IosKoinGraphTest"` passes, and the run makes no network request (the `MockEngine` handler fails the test on any request).

## 7. Docs

- [x] 7.1 Update `CLAUDE.md`:
  - **Repo map (`:shared`):** `iosMain` holds the iOS platform module, the entry point (`di/IosKoin.kt`), the Firebase bridges (`platform/`) and their adapters. `iosTest` holds the graph and adapter tests.
  - **`di/` section:** what iOS supplies (`iosPlatformModule`), and that Swift starts the graph with `startMovieGallery` and gets ViewModels from `MovieGalleryGraph`.
  - **Backends:** on iOS, Firebase Auth and Firebase AI are reached through the Swift bridges, and the error mapping lives in `iosMain`.
  - **Commands:** `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` builds the framework.
  - **"Adding a feature screen":** step 6 adds a `MovieGalleryGraph` function for each new ViewModel, which `IosKoinGraphTest`'s count check enforces.

  Verify: the doc names each new file at its real path.
- [x] 7.2 Update `.claude/skills/android-architecture/SKILL.md`:
  - section 2: the iOS platform module, and how a platform-specific binding (Firebase) reaches `:shared` through a bridge;
  - section 3: the `:shared` `iosMain` contents.

  Verify: the skill and CLAUDE.md describe the same layout, with no detail contradicting the other.

## 8. Verification

- [x] 8.1 Run `./gradlew ktlintCheck`. Verify: it passes.
- [x] 8.2 Run `./gradlew test`. Verify: everything passes, with the baseline `:app` count and the baseline `:shared` host count plus the new `ChatModelConfigTest` and `requireReplyText` tests.
- [x] 8.3 Run `./gradlew :shared:iosSimulatorArm64Test`. Verify: it passes, with the baseline count plus the new JVM-and-iOS common tests and the three new `iosTest` suites.
- [x] 8.4 Run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` and `./gradlew assembleDebug`. Verify: both succeed.
- [x] 8.5 Android check on the phone, installing over the current build (design, Migration Plan):
  - send a chat message and get a reply;
  - identify a poster from a photo;
  - sign in with a wrong password and see the usual error;
  - sign in correctly and see Home load;
  - open a movie's production-country sheet (Apollo, using the moved URL).

  Verify: report each step's result to the user. This is a manual check, so report it rather than claim it.

## Workflow follow-up

- Open the PR from `feature/ios-platform-layer` into `kotlin_multiplatform`, then archive the change once it is merged and `./gradlew test` passes.
- Next change: 5b, the iOS app shell. It implements both bridges in Swift, fills `IosAppConfig`, sets up Firebase, and brings up one screen end to end. It also answers the design's open question on Google credentials.
