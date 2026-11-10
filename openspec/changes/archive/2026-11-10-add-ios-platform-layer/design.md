# Design

## Context

See proposal.md (Why) for the motivation. Here is the current state, as read from `kotlin_multiplatform` at 5a844c1 (after PR #34).

- **What `sharedModules` asks a platform for** (its KDoc, `di/SharedModules.kt`):
  - an `HttpClientEngine` **factory**;
  - `NetworkConfig`;
  - an `ApolloClient`;
  - `AuthRepository`, `AuthTokenProvider` and `AiChatRepository`.

  `databaseModule` is already `expect`/`actual`, and its iOS `actual` builds the Room database in Application Support on `NativeSQLiteDriver`.
- **What Android supplies** (`:app` `di/AndroidModules.kt`, `di/AiModule.kt`):
  - an OkHttp engine factory over one base `OkHttpClient`;
  - `NetworkConfig` from `BuildConfig`, with `LogLevel.ALL` in debug and `Logger.ANDROID`;
  - Apollo on OkHttp, pointed at `COUNTRIES_URL` (a `const` in `AndroidModules.kt`);
  - Firebase Auth through `AuthRepositoryImpl` and `FirebaseAuthTokenProvider`, with errors mapped by `safeAuthCall`;
  - Firebase AI through `GeminiChatDataSource` and `AiChatRepositoryImpl`, with errors mapped by `safeAiCall` and empty replies rejected by a private `requireText()`. The model name, timeout and system prompt are `private` in `AiModule.kt`, and the poster prompt is `private` in `GeminiChatDataSource`.
- **Android's error mapping, which iOS must match:**
  - Auth: weak password → `WeakPassword`; email in use → `EmailInUse`; invalid credentials *and* invalid user (unknown or disabled account) → `InvalidCredentials`, deliberately not distinguished; too many requests → `TooManyRequests`; network or `IOException` → `Network`; anything else → `Unknown(cause)`. Cancellation is rethrown.
  - AI: invalid API key, API not configured or service disabled → `Unauthorized`; request timeout → `Unknown` (*not* `Network`: the model stalled, the device isn't offline); serialization → `Serialization`; `IOException` → `Network`; anything else → `Unknown(cause)`, keeping the cause so the chat can show the model's own explanation. An unreadable image → `Unknown`, never `Network`. A reply with no text → `Unknown`.
- **iOS today:** `iosMain` has `databaseModule`, `movieGalleryDatabaseBuilder`, `RatingFormat`, and the bridge (`StateFlowWatcher`, `ViewModelOwner`, whose KDoc expects "a Koin lookup" as its `create` lambda). `iosTest` has `WatchlistDaoIosTest`, `RealEngineNetworkErrorTest` (Darwin) and `ViewModelBridgeTest`. No framework binary is declared. Every iOS binary links `-lsqlite3`.
- **Koin on iOS:** `koin-core` is multiplatform. `verify()` lives in `koin-test` and relies on JVM reflection, so on iOS the graph can only be checked by resolving it, as `KoinGraphResolutionTest` does on Android.
- **Objective-C export rules that shape the API:**
  - reified generic functions such as `get<T>()` are not exported;
  - top-level functions appear as `<File>Kt.<name>`;
  - names starting with `init` get a `do` prefix;
  - Kotlin `suspend` members become completion handlers;
  - a Kotlin interface becomes a protocol Swift can adopt.

## Goals / Non-Goals

**Goals:**
- The full graph (`sharedModules` plus the iOS module) resolves on the iOS simulator: every use case, every ViewModel, both HTTP clients and Apollo. Tests prove it, using fake bridges and no network.
- The iOS adapters map failures to the same `AuthError` / `AppError` values Android produces for the equivalent failure. Tests show it case by case.
- The Swift-facing API (entry point, config, bridge protocols, ViewModel functions) links into a framework, and the generated header shows the expected names.
- Android is unchanged: same Gemini instructions, same errors, same graph.

**Non-Goals:**
- Exercising the real Firebase iOS SDK. That needs the Xcode project and `GoogleService-Info.plist` (5b).
- A Swift-side test suite. No Xcode project exists yet. Kotlin/Native tests stand in for Swift callers.
- Sharing `:app`'s Firebase code. Each platform talks to its own Firebase SDK, behind the same shared interfaces.

## Decisions

### 1. Firebase through Swift-implemented bridges

`iosMain` declares `FirebaseAuthBridge` and `FirebaseAiBridge` (package `com.raikar.moviegallery.platform`). The Swift app adopts them with the Firebase iOS SDK and passes them to the entry point (decision 5). Kotlin adapters in `iosMain` implement the domain interfaces on top.

- Firebase AI Logic has no Kotlin Multiplatform SDK, so AI has to cross into Swift whatever we choose. Using the same route for Auth gives one pattern.
- No Firebase framework is linked into `:shared`, so the iOS test binaries keep building and running with only Xcode, as they do today.
- The error mapping and the `Flow` and `suspend` shapes stay in Kotlin, where they are tested. Swift only forwards SDK results.
- *Alternative:* GitLive's `firebase-kotlin-sdk` for Auth. It covers Auth but not AI, needs the Firebase iOS frameworks linked into every iOS test binary, and adds a third-party wrapper whose releases follow both Kotlin and Firebase. Declined (user decision, 2026-11-10).
- *Alternative:* cinterop against the Firebase Objective-C headers. Firebase's newer products, AI Logic included, are Swift-only, so this doesn't work for AI at all.

### 2. Callback bridges, adapted with `CompletableDeferred`

Each bridge method takes a completion closure instead of being `suspend`:

```kotlin
interface FirebaseAuthBridge {
    fun currentUser(): BridgedUser?
    fun addAuthStateListener(onChange: (BridgedUser?) -> Unit): BridgeRegistration
    fun signIn(email: String, password: String, completion: (BridgedAuthError?) -> Unit)
    fun signUp(email: String, password: String, completion: (BridgedAuthError?) -> Unit)
    fun signInWithGoogle(idToken: String, completion: (BridgedAuthError?) -> Unit)
    fun signOut()
    fun idToken(forceRefresh: Boolean, completion: (String?) -> Unit)
}

interface FirebaseAiBridge {
    fun sendMessage(history: List<ChatTurn>, message: String, completion: (String?, BridgedAiFailure?) -> Unit)
    fun identifyPoster(history: List<ChatTurn>, imageUri: String, completion: (String?, BridgedAiFailure?) -> Unit)
}
```

- The supporting types are plain classes, easy to construct from Swift:
  - `BridgedUser(uid, email, displayName)`;
  - `BridgedAuthError(domain, code, message)`;
  - `BridgedAiFailure(kind, message)`;
  - `AiFailureKind`, an enum;
  - `BridgeRegistration`, an interface with `remove()`.
- **The adapters wait on a `CompletableDeferred`.** The completion calls `complete(...)` and the adapter `await()`s it.
  - A second call from Swift is ignored, because `complete` returns `false`. That matters because a bridge that fires twice would otherwise crash with a double resume.
  - The deferred is thread-safe, so Swift may call back on any queue.
  - Cancelling the caller (a ViewModel cleared mid-request) cancels the `await`, and the late result is dropped, as on Android.
- **Swift threading:** Firebase iOS calls completions on the main queue. The adapters don't depend on that.
- *Alternative:* `suspend` members that Swift implements as `async` functions. It works in Kotlin 2.3, but failures then reach Kotlin as Swift `Error`s wrapped in `NSError`. They would lose the typed classification, and cancellation from Kotlin doesn't reach the Swift task. Explicit callbacks keep the contract obvious in the header and simple to fake.

### 3. Error mapping stays in Kotlin

- **Auth:** Swift passes the `NSError`'s domain, code and description untouched. `mapBridgedAuthError` (`iosMain`, `data/remote/auth/`) mirrors `safeAuthCall`:

  | Firebase iOS (`FIRAuthErrorDomain`) | `AuthError` |
  |---|---|
  | `weakPassword` (17026) | `WeakPassword` |
  | `emailAlreadyInUse` (17007) | `EmailInUse` |
  | `wrongPassword` (17009), `invalidCredential` (17004), `invalidEmail` (17008), `userNotFound` (17011), `userDisabled` (17005) | `InvalidCredentials` |
  | `tooManyRequests` (17010) | `TooManyRequests` |
  | `networkError` (17020); any code in `NSURLErrorDomain` | `Network` |
  | anything else | `Unknown(BridgedAuthException(domain, code, message))` |

  All nine codes and the `FIRAuthErrorDomain` string were confirmed on 2026-11-10 against firebase-ios-sdk 13.1.1, the latest stable tag (task 2.2). The enum now lives in Swift, in `FirebaseAuth/Sources/Swift/Utilities/AuthErrors.swift` (`AuthErrorCode`, lines 59–121), not in `FIRAuthErrors.h`. In the code they are named constants, each with its Swift case name in a comment.
- **AI:** the Firebase iOS AI SDK reports Swift error enums, not stable numeric codes, so Swift classifies each failure into `AiFailureKind`, and Kotlin maps the kind (`mapBridgedAiFailure`, `iosMain`, `data/remote/ai/`):
  - `Unauthorized` → `AppError.Unauthorized`;
  - `Network` → `Network`;
  - `Timeout` → `Unknown`;
  - `Serialization` → `Serialization`;
  - `ImageUnreadable` and `Other` → `Unknown(BridgedAiException(kind, message))`, with the SDK's message kept for the chat bubble.

  The KDoc on each `AiFailureKind` entry names the Firebase iOS errors Swift should map to it, so 5b has a checklist.
- *Alternative:* mapping in Swift, with Swift passing `AuthError`s directly. That splits the logic across languages and leaves the most bug-prone part untested until 5b.

### 4. One shared definition of the chat settings and the reply rule

- **`ChatModelConfig`** (`commonMain`, `data/remote/ai/`) is a public object holding:
  - `MODEL_NAME`;
  - `REQUEST_TIMEOUT_MILLIS`;
  - `IDENTIFY_POSTER_PROMPT`;
  - `systemPrompt(platformName: String)`.
  
  The prompt text is moved verbatim, with "an Android app" replaced by "an $platformName app". Android passes `"Android"`, so the string it sends is byte-for-byte the same; a JVM test pins it. Swift (5b) passes `"iOS"` and reads the other constants when it builds its `GenerativeModel`.
- **The reply rule** (trim the text; reject a null or blank reply as `Unknown`) moves from `AiChatRepositoryImpl`'s private `requireText()` to a public `DataResult<String?>.requireReplyText()` in the same `commonMain` package. Both repositories call it.
- **`COUNTRIES_URL`** moves to `commonMain` next to the Countries code (`data/remote/`), and `AndroidModules.kt` imports it.
- *Alternative:* a separate iOS copy of the prompt. It would drift the first time someone tunes it.

### 5. The entry point: `startMovieGallery(...)` returning a `MovieGalleryGraph`

In `iosMain` `di/IosKoin.kt`:

```kotlin
fun startMovieGallery(config: IosAppConfig, authBridge: FirebaseAuthBridge, aiBridge: FirebaseAiBridge): MovieGalleryGraph

class MovieGalleryGraph internal constructor(private val koin: Koin) {
    fun appSessionViewModel(): AppSessionViewModel
    fun homeViewModel(): HomeViewModel
    fun movieDetailViewModel(movieId: Int): MovieDetailViewModel
    // ... one function per ViewModel in viewModelModule
}
```

- **`startMovieGallery`** calls `startKoin { modules(iosModules(config, authBridge, aiBridge)) }`, so a second call fails loudly with Koin's "already started". It returns the graph, and Swift keeps it for the app's lifetime. In Swift this is `IosKoinKt.startMovieGallery(config:authBridge:aiBridge:)`. It is deliberately not called `initKoin`, which Objective-C export would rename to `doInitKoin`.
- **`internal fun iosModules(...)`** returns `sharedModules + iosPlatformModule(...)`. Tests build a `koinApplication { }` from it, with no global state, and wrap that in `MovieGalleryGraph`.
- **ViewModel functions:**
  - One per ViewModel, because Swift can't call the reified `get<T>()`.
  - Each returns a new instance. Lifetime belongs to `ViewModelOwner`, for example `ViewModelOwner { graph.homeViewModel() }`.
  - A test compares the function list with `viewModelModule`'s definitions, so a ViewModel added to one but not the other fails it.
- *Alternative:* a generic `get(KClass)` exposed to Swift. Swift gets an `ObjCClass` and an untyped result, losing the type at every call site.

### 6. The iOS platform module

`iosPlatformModule(config, authBridge, aiBridge)` in `iosMain` `di/IosPlatformModule.kt`:

- **Engine:** `factory<HttpClientEngine> { Darwin.create() }`. A factory for the same reason as Android: closing one client mustn't close the other's engine. The Darwin engine's default `NSURLSession` timeouts apply. Android's 15-second OkHttp timeouts aren't copied here; that is left to 5b, which can tune them against real traffic.
- **`NetworkConfig`:** built from **`IosAppConfig(tmdbAccessToken, appVersion, isDebug)`**, which Swift fills in 5b. It uses `LogLevel.ALL` and `Logger.SIMPLE` (stdout, which shows in the Xcode console) when `isDebug`, else `LogLevel.NONE`. Ktor redacts `Authorization` at every level, as on Android. `IosAppConfig.toString()` redacts the token, as `NetworkConfig` does.
- **Apollo:** `ApolloClient.Builder().serverUrl(COUNTRIES_URL).build()`, on Apollo's default iOS network transport (`NSURLSession`). When `isDebug`, it adds Apollo's multiplatform `LoggingInterceptor` (`com.apollographql.apollo.network.http`), matching Android's body-level logging in debug. Task 4.1 confirms the class is in Apollo 5's common runtime. If it isn't, iOS ships without Apollo logging, and the task records that.
- **Bridges:** each bridge is a `single`, plus `single<AuthRepository> { IosAuthRepository(get()) }`, `single<AuthTokenProvider> { IosAuthTokenProvider(get()) }` and `single<AiChatRepository> { IosAiChatRepository(get(), get(IoDispatcher)) }`.

### 7. Auth adapter behavior

- **`isSignedIn`** is `bridge.currentUser() != null`. It is synchronous, because Firebase iOS restores the persisted user from the keychain before `configure()` returns.
- **`currentUserId` and `currentUser`** are built with `callbackFlow`:
  - first send `bridge.currentUser()`;
  - register the listener;
  - `awaitClose { registration.remove() }`;
  - `distinctUntilChanged()`.

  Emitting the current value first means the flow doesn't rely on Firebase calling a new listener immediately (it does, but it's an SDK detail), and `distinctUntilChanged` drops the duplicate.
- **Sign-in, sign-up and Google sign-in** complete with `null` on success, and map a `BridgedAuthError` through decision 3.
- **`signOut()`** forwards. Swift swallows the SDK's thrown error, matching Android, where sign-out can't fail.
- **`IosAuthTokenProvider.idToken(forceRefresh)`** forwards. `null` means signed out or no token, which the BFF plugin already handles.

### 8. Framework binary

Both iOS targets declare `binaries.framework { baseName = "Shared"; isStatic = true }`.

- **Static**, so the app links one binary and Xcode needs no embed step. `-lsqlite3` already applies to every binary (`configureEach`).
- **Why now:** linking it (`linkDebugFrameworkIosSimulatorArm64`) proves the Swift-facing API exports. A task checks `Shared.h` for `startMovieGallery`, `MovieGalleryGraph`, the two bridge protocols and `IosAppConfig`.
- How Xcode builds and consumes it (an `embedAndSignAppleFrameworkForXcode` build phase, or an XCFramework) is 5b's decision.

### 9. Tests

All tests are in `iosTest`, use `kotlin.test`, and have names without `,` `.` `:` `;`.

- **`IosKoinGraphTest`:**
  - **Graph setup:** a `koinApplication` from `iosModules(...)`, with fake bridges and two test overrides (`allowOverride(true)`):
    - a `MockEngine` factory, so no request leaves the process;
    - an in-memory `MovieGalleryDatabase` on `NativeSQLiteDriver`.

    Everything else is real.
  - **Dispatcher:** `Dispatchers.setMain(StandardTestDispatcher())`, never advanced, so ViewModels' `init` loads don't run.
  - **What it checks:**
    - every use case resolves (the same list as `KoinGraphResolutionTest`);
    - every `MovieGalleryGraph` ViewModel function returns an instance, and each is then cleared through a `ViewModelOwner`;
    - the graph has as many ViewModel functions as `viewModelModule` has definitions;
    - `TmdbClient` and `BffClient` get different engines;
    - the `ApolloClient` resolves;
    - closing the graph closes both clients.
  - **Unmodified graph:** a separate test resolves the real `HttpClientEngine` factory twice and checks it gives two distinct Darwin engines. It creates no client and makes no request.
- **`IosAuthRepositoryTest`** (with a `FakeFirebaseAuthBridge` in `iosTest`):
  - `isSignedIn` follows `currentUser()`;
  - `currentUserId` emits the current value, then each change, without repeats;
  - cancelling collection removes the listener;
  - `currentUser` maps the email and display name;
  - each row of the decision 3 table maps as listed, including the `NSURLErrorDomain` case;
  - an unknown code is `Unknown`, with the domain, code and message in its cause;
  - a completion fired twice is harmless;
  - a cancelled caller doesn't crash on a late completion;
  - `signOut` forwards;
  - `IosAuthTokenProvider` passes `forceRefresh` through and returns the token or `null`.
- **`IosAiChatRepositoryTest`** (with a `FakeFirebaseAiBridge`):
  - a reply is trimmed;
  - a null or blank reply is `Unknown`;
  - each `AiFailureKind` maps as decision 3 lists;
  - `ImageUnreadable` is `Unknown`, not `Network`;
  - the history and image URI reach the bridge unchanged;
  - a completion fired twice is harmless.
- **JVM (`commonTest`):**
  - `ChatModelConfigTest` pins `systemPrompt("Android")` to the current prompt text;
  - `requireReplyText` is tested once.

## Risks / Trade-offs

- **[Risk] The Firebase iOS error codes in decision 3 are wrong or change.** → They are checked against the SDK header before coding, and each is a named constant with its case name. 5b's manual check signs in with a wrong password, an unknown account and a taken email on a real device.
- **[Risk] A Swift bridge never calls its completion**, so the caller hangs. → On Android the same thing is bounded by the SDK: Gemini's 60-second timeout, which Swift sets from `ChatModelConfig`, and Firebase Auth's own timeouts. The KDoc on each bridge method states that the completion must be called exactly once. A Kotlin-side timeout isn't added: Android has none either, and one would change behavior between platforms.
- **[Risk] The graph test overrides two bindings, so it doesn't prove the real database or real engine work in the graph.** → Those are covered on their own: `WatchlistDaoIosTest` (the real builder on disk), `RealEngineNetworkErrorTest` (Darwin) and the unmodified-engine test above. The override only keeps the graph test offline and free of files.
- **[Risk] Google sign-in on iOS may need the access token as well as the ID token.** Firebase iOS's `GoogleAuthProvider.credential(withIDToken:accessToken:)` takes both, while the domain's `signInWithGoogle(idToken)` passes one. → See Open Questions. The bridge method can gain a parameter in 5c without touching this change's other code.
- **[Trade-off] The prompt becomes a function of the platform name.** This adds a little indirection to a string, and in exchange the two apps can't drift. The JVM test makes any accidental change to Android's text fail.
- **[Trade-off] The Darwin engine uses `NSURLSession`'s default timeouts**, which are longer than OkHttp's 15 seconds. → Recorded for 5b, which sees real iOS traffic.

## Migration Plan

- **Branch:** `feature/ios-platform-layer`, from `kotlin_multiplatform` at 5a844c1. The PR goes into `kotlin_multiplatform`, like #30–#34.
- **Order:**
  1. Shared settings and the reply rule, with `:app` switched over (Android stays green).
  2. Bridge interfaces and error mapping.
  3. Adapters.
  4. Platform module and entry point.
  5. Framework binary.
  6. Tests.
  7. Docs.
- **Rollback:** revert the PR. No schema or data changes are involved, and no iOS app depends on this yet.
- **Android check before commit:** send a chat message and identify a poster. This confirms the moved prompt and the reply rule, which the JVM tests don't reach through Gemini. Then sign in with a wrong password and confirm the usual error.

## Open Questions

- **Does Firebase iOS accept a Google credential built from the ID token alone?** If not, 5c adds an access-token parameter to `signInWithGoogle` on the bridge, and decides whether the domain interface carries it. This change's tasks stay the same either way, because the bridge method and its test are one line each.
