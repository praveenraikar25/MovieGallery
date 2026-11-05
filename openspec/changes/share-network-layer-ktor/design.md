# Design

## Context

See proposal.md for motivation and scope.

The TMDB and BFF stacks live in `:app` (`di/NetworkModule.kt`):

- **Base client.** One unauthenticated `OkHttpClient` with 15 s connect and read timeouts. Coil and Apollo share it.
- **`@TmdbClient`.** Derived from the base client. An interceptor adds `Authorization: Bearer <BuildConfig token>` and `Accept: application/json`. An `HttpLoggingInterceptor` logs bodies in debug builds and redacts `Authorization`. Retrofit uses this client for `TmdbApi`, whose base URL is `https://api.themoviedb.org/3/`.
- **`@BffClient`.** Derived from the base client. It adds `X-APP-VERSION`, and two classes do the auth:
  - `BffAuthInterceptor` looks up the Firebase ID token with `runBlocking`. With no token, it throws `MissingAuthTokenException` before sending.
  - `BffTokenAuthenticator` retries a 401 once with a force-refreshed token, and only for the BFF host.

  `MissingAuthTokenException` extends `IOException` only because OkHttp lets nothing else out of an interceptor.
- **`safeApiCall`.** Maps `HttpException` to `Unauthorized`, `NotFound` or `Http(code)`, `MissingAuthTokenException` to `AuthRequired`, `IOException` to `Network`, and `SerializationException` to `Serialization`. It rethrows `CancellationException`.
- **`TvShowRepositoryImpl`** turns `Unauthorized` into `AuthRequired`.

The `tv-shows` spec pins this behaviour: a token on every request, no request without a token, one refresh retry, and a fresh token lookup on Retry.

The safety-net problem: of the 39 network tests, nearly all construct transport types themselves:

- `SafeApiCallTest`, `MovieRepositoryImplTest` and `TvShowRepositoryImplTest` build Retrofit `HttpException`s.
- `TmdbApiTest` and `BffApiTest` build Retrofit and OkHttp clients.
- `BffAuthTest` drives an `OkHttpClient` directly.

`FakeTmdbApi` and `FakeBffApi` implement the Retrofit interfaces. The JSON fixtures load from the JVM classpath. Each is under 1 KB.

## Goals / Non-Goals

**Goals:**
- Every request the app sends to TMDB and the BFF stays the same: URL, path, query, headers and auth. So does every `DataResult` the repositories return for a given HTTP outcome.
- The same test assertions pass on Retrofit, then on Ktor in `:app`, then on Ktor in `:shared`. The last step runs on both the JVM and the iOS simulator.
- `commonMain` stays free of `android.*`, `java.*` and `javax.*` imports.

**Non-Goals:**
- Making the iOS networking usable from Swift. No framework export, no Swift-friendly API.
- Tuning: no HTTP caching, retries beyond the BFF token retry, or timeout changes.

## Decisions

### 1. Three checkpoints, each its own commit with all tests passing

```
 0  Retrofit, :app   tests rewritten over HTTP via a transport-neutral harness   GREEN
 1  Ktor,     :app   production swapped; only the harness factory may change     GREEN
 2  Ktor,  :shared   code + tests moved; harness backed by MockEngine            GREEN (JVM + iOS)
```

Checkpoint 1 is where the tests prove the swap kept behaviour, so the rule for it can be checked: `git diff --stat` for checkpoint 1 under `app/src/test` shows only the harness factory, plus the deletion of the two fake APIs. Each checkpoint ends with `./gradlew ktlintCheck test assembleDebug`. Checkpoint 2 also runs `:shared:iosSimulatorArm64Test`.

Alternative considered: write the tests directly against `MockEngine` during the swap. Rejected: the tests would be rewritten in the same step as the code, so nothing would check that behaviour stayed the same.

### 2. A transport-neutral test harness

Checkpoint 0 adds a small `FakeHttpServer` interface to the test utilities. Its methods:

- `enqueue(status, body)` queues a response.
- `failConnections(path)` makes every request to a path (or to any unplanned path) fail at the connection. It is sticky rather than one-shot because OkHttp retries a GET once when the connection drops, which would otherwise consume the next planned response.
- `requests` lists the recorded requests, each with its path, query parameters and headers.
- `requestCount` counts them.

Next to it sits one factory per backend: `tmdbApi(server, token)` and `bffApi(server, tokenProvider)`, each returning the production API class pointed at the fake server.

- **Checkpoints 0 and 1:** the interface is backed by MockWebServer. `failConnections` shuts the connection down as the response starts. The factory builds Retrofit in checkpoint 0 and Ktor with the OkHttp engine in checkpoint 1.
- **Checkpoint 2:** the interface is backed by `MockEngine`. `failConnections` throws a `kotlinx.io.IOException` from the handler, and `requests` reads `requestHistory`.

From checkpoint 0 on, the tests are written so that checkpoint 2 can move them without editing them:

- `kotlin.test` assertions and `kotlinx.coroutines.test.runTest`;
- names without `,` `.` `:` `;` (one existing name has a comma and gets reworded);
- no imports from Retrofit, OkHttp or `java.*`.

Repository tests stop using `FakeTmdbApi` and `FakeBffApi`. They build the real repository over the harness. The partial-success and total-failure cases for Home rails become "this path returns 500, that one returns 200" and so on.

A few `safeApiCall` tests never touch the transport, and they stay plain unit tests: a successful value, `CancellationException` rethrown, any other `Throwable` mapped to `Unknown` with its cause, and `MissingAuthTokenException` mapped to `AuthRequired`.

Alternative considered: keep the fakes and make them throw Ktor exceptions. Rejected: that still encodes the transport's exception types in the tests, which is the coupling this change removes.

### 3. Ktor error model: `expectSuccess = true`, and an explicit mapping table

Without `expectSuccess`, Ktor hands a 404 body to the JSON decoder. With it, Ktor throws `ClientRequestException` for 4xx and `ServerResponseException` for 5xx, both subclasses of `ResponseException`. In `commonMain`, `safeApiCall` catches the following, in this order:

| Thrown | `AppError` | Note |
|---|---|---|
| `CancellationException` | rethrown | Ktor's timeout exceptions are not cancellations. Catching timeouts first avoids any ambiguity |
| `MissingAuthTokenException` | `AuthRequired` | Now a plain `Exception`, no longer an `IOException` |
| `ResponseException` | `Unauthorized` (401), `NotFound` (404), otherwise `Http(code)` | Read from `response.status.value` |
| `HttpRequestTimeoutException`, `ConnectTimeoutException`, `SocketTimeoutException` | `Network` | Listed explicitly, so the result doesn't depend on whether they extend `IOException` |
| `kotlinx.io.IOException` | `Network` | On JVM and Android this is a typealias of `java.io.IOException`, so it covers OkHttp's exceptions |
| `ContentConvertException` (incl. `JsonConvertException`), `NoTransformationFoundException`, `SerializationException` | `Serialization` | Ktor wraps decoder failures. A non-JSON 200, such as a captive portal page, also maps here, as Retrofit's converter did |
| `Throwable` | `Unknown(e)` | |

The iOS Darwin engine reports failures as `DarwinHttpRequestException`. Task 3.2 checks whether it extends `kotlinx.io.IOException`. If it doesn't, the fallback is an `internal expect fun Throwable.isPlatformNetworkError(): Boolean`: false on Android, an `is DarwinHttpRequestException` check on iOS. `safeApiCall` checks it before `Unknown`.

### 4. BFF token handling is a custom Ktor plugin, not Ktor's `Auth` plugin

Ktor's `Auth` bearer provider would break the `tv-shows` spec in three ways:

- It caches the tokens it loads and only calls `loadTokens` again after a refresh. That breaks "Retry looks up a fresh token", and after sign-out and a new sign-in it can send the old account's token.
- Whether it refreshes depends on the `WWW-Authenticate` header, which the BFF isn't required to send.
- With no token, it sends the request unauthenticated instead of not sending it. That breaks "SHALL NOT contact the BFF service".

Instead, `BffAuth` is a client plugin of about 40 lines, built with `createClientPlugin`. It takes an `AuthTokenProvider` and the BFF host:

- **Before each request:** it looks up `idToken(forceRefresh = false)`, sets `Authorization: Bearer <token>`, and throws `MissingAuthTokenException` when the lookup returns null.
- **On a 401** (hooked on `Send`): when the response is a 401, the request's host is the BFF host and this request hasn't been retried yet, it looks up `idToken(forceRefresh = true)`. If that returns a token, it resends once with the new token. Otherwise the original 401 goes through.
- **Token lookup:** `withTimeoutOrNull(10 s)`, as today. A provider exception counts as no token, and `CancellationException` is rethrown.

Because it's all suspend code, `runBlocking` is gone.

The ordering with `expectSuccess` matters: the 401 must reach the `Send` hook before response validation throws. The tests "a 401 is retried once with a refreshed token" and "a persistent 401 is surfaced after a single retry" check this. If the hook sees the exception instead, the plugin moves to `HttpSend.intercept`, which runs before validation.

Alternative considered: Ktor's `Auth` with `loadTokens` calling the provider and `clearToken()` before every request. Rejected: it works against the plugin's caching, still sends a request when there is no token, and is harder to read than the custom plugin.

### 5. Shared client factories; `:app` supplies the engine and the `BuildConfig` values

`commonMain` gets two factories in `data/remote/`, plus a shared `NetworkJson` with `ignoreUnknownKeys` and `coerceInputValues`. It moves out of `NetworkModule.provideJson`, and the tests' `testJson` becomes an alias of it.

```
createTmdbHttpClient(engine, accessToken, logLevel, logger, baseUrl = TmdbApi.BASE_URL)
createBffHttpClient(engine, tokenProvider, appVersion, logLevel, logger, baseUrl = BffApi.BASE_URL)
```

Both factories install the same set:

- `ContentNegotiation` with `NetworkJson`;
- `HttpTimeout` with 15 s connect and 15 s socket timeouts, matching OkHttp's connect and read, so iOS gets the same limits;
- `Logging`;
- `expectSuccess = true`;
- `defaultRequest`, which sets the base URL and headers. TMDB gets the bearer token and `Accept: application/json`. The BFF gets `X-APP-VERSION`, and installs the `BffAuth` plugin.

Base URLs keep their trailing slash, and endpoint paths have no leading slash. With a leading slash, Ktor would resolve the path from the host root and drop `/3`. The path assertions in the tests (`/3/movie/popular`) catch that mistake.

`:app`'s `NetworkModule` provides:

- `@TmdbClient HttpClient` and `@BffClient HttpClient`, each built on its own `OkHttp.create { preconfigured = base }` engine. Each engine wraps the shared base client, so connections stay pooled with Coil and Apollo;
- the token, the version code and the log level, from `BuildConfig`;
- `TmdbApi` and `BffApi`.

The OkHttp-level `@TmdbClient` and `@BffClient` clients are deleted: their interceptors move into the factories. The qualifiers now mark `HttpClient`s.

The Darwin engine is a dependency of `iosMain`. For now only the iOS connection test (decision 8) uses it, and later the iOS app will.

Alternative considered: `expect fun platformEngine()` in `:shared`. Rejected for now: Android needs the engine wrapped around `:app`'s base client, which `:shared` can't see.

### 6. Logging moves to Ktor's `Logging` plugin, with the token always redacted

`Logging` gets `sanitizeHeader { it == HttpHeaders.Authorization }`. Its level is `BODY` when `BuildConfig.DEBUG` is set and `NONE` otherwise. `:app` passes a `Logger` that writes to logcat: Ktor's Android logger if the chosen version has one, otherwise a three-line `android.util.Log` adapter in `:app`.

A new test captures logger output with `MockEngine` at `BODY` level and checks that no token value appears. Today nothing tests this.

Alternative considered: keep `HttpLoggingInterceptor` on the preconfigured OkHttp client. Rejected: iOS would have no redaction rule, and the logging setup would be split across two modules.

### 7. `TmdbApi` and `BffApi` become concrete classes over `HttpClient`

They keep their method names, parameters, default values and `BASE_URL` and `DEFAULT_LANGUAGE` constants, so the repositories' call sites don't change. Each method is a `client.get("movie/popular") { parameter(...) }.body()` call. There's no interface: with the fakes gone, only one implementation exists.

### 8. Testing what `MockEngine` can't

`MockEngine` skips the real engine, so the exceptions it throws are only what we expect the engine to throw. One real-engine test per platform covers that:

- **Android:** an `androidHostTest` using the OkHttp engine.
- **iOS:** an `iosTest` using the Darwin engine.

Each sends a request through `safeApiCall` to `http://127.0.0.1:1/`, a port nothing listens on, so the connection is refused offline and right away, and checks for `AppError.Network`. Timeout mapping is tested in `commonTest`, using `MockEngine` with a handler that waits longer than a short `HttpTimeout`.

### 9. Test placement and fixtures

The repository, API, `safeApiCall`, `BffAuth`, logging and mapper tests all move to `shared/src/commonTest`. Their JSON fixtures become string constants in a `commonTest` `TestFixtures.kt`, behind the same `fixture("name.json")` lookup, so call sites don't change. The country fixtures and `CountryRepositoryImplTest` stay in `:app`, which keeps MockWebServer there.

The mapper tests have to move with the mappers. They become `internal` (decision 10), and `:app` tests can't see `internal` declarations of `:shared`.

### 10. The mappers go back to `internal`

Once the repositories are in `:shared`, nothing in `:app` calls the four `toDomain` functions. They go back to `internal`, and their `TEMP(share-core-kmp-module)` comments are removed. A `grep` for `TEMP(share-core-kmp-module)` must return nothing.

### 11. DI changes

- **`SharedModule`:** adds `@Provides @Singleton` for `MovieRepository` and `TvShowRepository`, passing the API and the `@IoDispatcher` dispatcher.
- **`RepositoryModule`:** drops their `@Binds`.
- **`MissingAuthTokenException`, `safeApiCall` and `BffAuth`:** move to `:shared` under unchanged packages, as `data.remote` and `data.remote.auth`.
- **`AuthTokenProvider`:** already in `:shared`. `FirebaseAuthTokenProvider` stays in `:app`.

## Risks / Trade-offs

- **The rewritten tests could be wrong in the same way the old ones were.** → Checkpoint 0 is written and has to pass against the unchanged Retrofit code. `tasks.md` maps each of the 39 old tests to its replacement, so none is dropped without a decision.
- **`ContentNegotiation` may add a second `Accept` value.** → The TMDB test asserts the header is exactly `application/json`. If it fails, set the header only once, either in `defaultRequest` or through content negotiation.
- **`expectSuccess` validation may run before the `Send` hook sees the 401.** → The retry tests catch it. The fallback is named in decision 4.
- **`MockEngine` doesn't reproduce real engine failures.** → The real-engine tests in decision 8, plus the explicit timeout rows in decision 3.
- **The token could leak into debug logs.** → The redaction test in decision 6.
- **Behaviour on a real device** (pooling, TLS, the real Firebase token) isn't covered by JVM tests. → A manual check on the Galaxy S10 at the end, signed in and signed out (`tasks.md` section 4).
- **A breaking Ktor major release.** → Use the latest stable at apply time. If it's a new major, the API names in decisions 3 to 6 may differ, but the approach doesn't.

## Migration Plan

Three commits on a feature branch off `kotlin_multiplatform`, in checkpoint order, then one PR into `kotlin_multiplatform`. No data migration: no stored data or schema changes. To roll back, revert the commits. Checkpoint 0 can stay on its own as a pure test improvement.
