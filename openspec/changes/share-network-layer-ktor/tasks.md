# Tasks

Three checkpoints (design decision 1). Each group ends with all tests passing and its own commit, made only once the user says to commit. Run `./gradlew ktlintFormat` before each commit; the pre-commit hook checks the result.

## 1. Checkpoint 0: tests that don't depend on the transport, still on Retrofit

No production code changes in this group. Every test below must pass against the current Retrofit code.

- [x] 1.1 Add `kotlin("test")` to `:app`'s `testImplementation` (`kotlinx-coroutines-test` is already there). In `app/src/test/.../testutil/`, add:
  - the `FakeHttpServer` interface;
  - a MockWebServer-backed implementation, with `failConnections` as a connection shutdown at response start;
  - a `TestBackends.kt` holding the `tmdbApi(server, token)` and `bffApi(server, tokenProvider, bffHost)` factories, built on Retrofit and OkHttp for now. `bffApi` installs the real `BffAuthInterceptor` and `BffTokenAuthenticator`.

  See design decision 2. Verify: `./gradlew :app:compileDebugUnitTestKotlin` passes.
- [x] 1.2 Rewrite `TmdbApiTest` (7 tests) and `BffApiTest` (2 tests) against the harness:
  - `kotlin.test` assertions;
  - no `retrofit2`, `okhttp3`, `mockwebserver3` or `java.*` imports;
  - every existing assertion kept: path, query, `Authorization`, an exact `Accept` value and no `api_key`.

  Verify: both pass under `:app:testDebugUnitTest`.
- [x] 1.3 Rewrite `MovieRepositoryImplTest` (10 tests) and `TvShowRepositoryImplTest` (6 tests) to build the real repositories over the harness. Each case that used to make a fake throw becomes the matching HTTP outcome:
  - `IOException` becomes `failConnections`;
  - `HttpException(n)` becomes a status `n` response.

  Rename `a total failure reports the underlying error, not a generic one` so the name has no comma. Delete `FakeTmdbApi.kt` and `FakeBffApi.kt`; keep `asResponse` and `asTvShowResponse` as JSON builders. Verify: both pass, and `grep -rn 'HttpException\|FakeTmdbApi\|FakeBffApi' app/src/test` returns nothing.
- [x] 1.4 Rewrite `BffAuthTest` (5 tests) to go through `bffApi(...)` and `TvShowRepositoryImpl` instead of a bare `OkHttpClient`. Keep each assertion: the bearer header, the request count and the token-lookup calls. Add three tests:
  - `Retry after a failure looks up the token again`: two repository calls lead to two `idToken(false)` lookups;
  - `a 401 from another host is not retried`, using a `bffHost` that doesn't match;
  - `a missing token sends no request and surfaces as AuthRequired`.

  Verify: all 8 pass.
- [x] 1.5 Split `SafeApiCallTest` (9 tests):
  - **Kept as unit tests:** a successful value, cancellation rethrown, `Unknown` keeping its cause, and `MissingAuthTokenException` mapping to `AuthRequired`.
  - **Replaced by HTTP-level tests through `tmdbApi(...)`:** 401 → `Unauthorized`, 404 → `NotFound`, 500 → `Http(500)`, 422 → `Http(422)`, a malformed body → `Serialization`, a 200 with an HTML body → `Serialization`, and a refused connection → `Network`.

  Verify: all pass, and the file has no `retrofit2` or `java.io` imports.
- [x] 1.6 Write a traceability list into the checkpoint 0 commit message. Map each of the 39 old test names to its replacement test, or mark it "kept unchanged", and give a reason for anything dropped. Verify: the list has 39 entries.
- [x] 1.7 Checkpoint 0 gate: `./gradlew ktlintCheck test assembleDebug`. Verify: everything passes, and `git diff --stat` shows changes only under `app/src/test` and in `app/build.gradle.kts`, for the test dependency.

## 2. Checkpoint 1: Ktor replaces Retrofit, still in `:app`

New production code is written without `android.*`, `java.*` or `javax.*` imports (except in `NetworkModule`), so group 3 can move it without edits.

- [ ] 2.1 Add Ktor to `gradle/libs.versions.toml` at its latest stable release, checked on Maven Central at apply time:
  - libraries: `ktor-client-core`, `ktor-client-okhttp`, `ktor-client-darwin`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging` and `ktor-client-mock`;
  - one `ktor` version ref for all of them.

  Add core, OkHttp, content negotiation, serialization and logging to `:app` `implementation`, and mock to `testImplementation`. If the latest stable is a new major version, note the API differences against design decisions 3 to 6 in a commit-message line. Verify: `./gradlew :app:dependencies --configuration debugRuntimeClasspath` resolves `io.ktor` at that version.
- [ ] 2.2 Write the shared-ready pieces in `:app`, keeping their existing packages:
  - `NetworkJson`;
  - the `BffAuth` client plugin (design decision 4);
  - `MissingAuthTokenException`, now a plain `Exception`;
  - `createTmdbHttpClient` and `createBffHttpClient` (design decision 5), with `expectSuccess`, `HttpTimeout` at 15 s, `ContentNegotiation`, `Logging` with `Authorization` redacted, and `defaultRequest`.

  Delete `BffAuthInterceptor` and `BffTokenAuthenticator`. Verify: `./gradlew :app:compileDebugKotlin` passes, and `grep -n runBlocking -r app/src/main` returns nothing.
- [ ] 2.3 Turn `TmdbApi` and `BffApi` into classes over `HttpClient` with the same method signatures and constants. Endpoint paths have no leading slash (design decision 7). Rewrite `safeApiCall` to follow the mapping table in design decision 3. Verify: `:app:compileDebugKotlin` passes, and `grep -rn retrofit2 app/src/main` returns nothing.
- [ ] 2.4 Rewrite `NetworkModule`:
  - provide `@TmdbClient` and `@BffClient` `HttpClient`s, each on its own `OkHttp.create { preconfigured = base }` engine;
  - pass the `BuildConfig` token, version code and log level, plus a logcat `Logger`;
  - provide `TmdbApi` and `BffApi`;
  - delete the OkHttp-level TMDB and BFF clients and the two Retrofit providers.

  Coil and `GraphQlModule` stay as they are. Verify: `./gradlew :app:assembleDebug` passes.
- [ ] 2.5 Change only `TestBackends.kt` so that `tmdbApi(...)` and `bffApi(...)` build the Ktor clients on the OkHttp engine, still pointed at MockWebServer. If any test fails, fix the production code, never the test. The known suspects are in the design's Risks section. Verify: all group 1 tests pass, and `git diff --stat -- app/src/test` for this group lists only `TestBackends.kt`.
- [ ] 2.6 Add two `MockEngine` tests in `app/src/test/.../data/remote/`:
  - at `BODY` log level, a captured `Logger` never sees the TMDB token or the BFF ID token value (design decision 6);
  - a handler that waits longer than a short `HttpTimeout` maps to `AppError.Network` (design decision 8).

  Verify: both pass.
- [ ] 2.7 Remove `retrofit` and `retrofit-converter-kotlinx-serialization` from `:app` and from the catalog. Verify: `grep -rn retrofit gradle/libs.versions.toml app/build.gradle.kts` returns nothing, and `./gradlew :app:assembleDebug` passes.
- [ ] 2.8 Checkpoint 1 gate: run `./gradlew ktlintCheck test assembleDebug`, then install the debug build and confirm that Home rails, search and TV shows load while signed in. Verify: the commands pass and all three screens show data.

## 3. Checkpoint 2: move to `:shared`, tests on `MockEngine`

- [ ] 3.1 In `shared/build.gradle.kts`, add the Ktor dependencies:
  - `commonMain`: core, content negotiation, serialization and logging, as `api` for core;
  - `androidMain`: OkHttp;
  - `iosMain`: Darwin;
  - `commonTest`: mock and `kotlinx-coroutines-test`;
  - `androidHostTest`: nothing extra.

  Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [ ] 3.2 `git mv` the following into `shared/src/commonMain/kotlin/com/raikar/moviegallery/`, with packages unchanged:
  - `NetworkJson`, the two client factories, `BffAuth`, `MissingAuthTokenException` and `SafeApiCall.kt` (as `internal`);
  - `TmdbApi`, `BffApi`, `MovieRepositoryImpl` and `TvShowRepositoryImpl`, with `@Inject`, `@Singleton` and `@IoDispatcher` removed so they take a plain `CoroutineDispatcher`.

  Then, in `:app`:
  - add `@Provides @Singleton` for `MovieRepository` and `TvShowRepository` to `SharedModule`, and remove their `@Binds` from `RepositoryModule`;
  - `NetworkModule` keeps creating the engines and calling the factories.

  Verify: `:app:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64` pass, and `grep -rE 'import (android|java|javax)\.' shared/src/commonMain` returns nothing.
- [ ] 3.3 Add the real-engine tests (design decision 8):
  - `shared/src/androidHostTest/.../RealEngineNetworkErrorTest.kt` on the OkHttp engine;
  - `shared/src/iosTest/.../RealEngineNetworkErrorTest.kt` on the Darwin engine.

  Each requests `http://127.0.0.1:1/` through `safeApiCall` and expects `AppError.Network`. If the iOS test returns `Unknown`, add the `isPlatformNetworkError` expect/actual fallback from design decision 3, and record which exception Darwin threw in a comment on the iOS actual. Verify: the first passes under `:shared:testAndroidHostTest` and the second under `:shared:iosSimulatorArm64Test`.
- [ ] 3.4 Make the four `toDomain` mappers in `MovieMappers.kt` and `TvShowMappers.kt` `internal`, and delete their `TEMP(share-core-kmp-module)` comments (design decision 10). Verify: `grep -rn 'TEMP(share-core-kmp-module)' shared app CLAUDE.md` returns nothing, and both modules compile.
- [ ] 3.5 Move the test harness to `shared/src/commonTest/.../testutil/`:
  - `FakeHttpServer` gets a `MockEngine` implementation (design decision 2);
  - `TestBackends.kt` builds the factories on that engine;
  - `TestFixtures.kt` holds the TMDB and BFF JSON fixtures as string constants behind `fixture(name)`;
  - `testJson` becomes an alias of `NetworkJson`.

  Delete the moved fixture files from `app/src/test/resources`, keeping the `country_*` and `movie_detail_*countr*` files, which `CountryRepositoryImplTest` uses. Before deleting, confirm with `grep -rn` which files those are. Verify: `:shared:compileTestKotlinIosSimulatorArm64` passes.
- [ ] 3.6 `git mv` these tests to `shared/src/commonTest`, editing only imports and harness wiring, never assertions:
  - `TmdbApiTest`, `BffApiTest`, `MovieRepositoryImplTest`, `TvShowRepositoryImplTest`, `BffAuthTest` and `SafeApiCallTest`;
  - the two tests from 2.6;
  - `MovieMappersTest` and `TvShowMappersTest`.

  Remove `ktor-client-mock` from `:app` if nothing there uses it any more. Keep MockWebServer for `CountryRepositoryImplTest`. Verify: all pass under `:shared:testAndroidHostTest` and `:shared:iosSimulatorArm64Test`, `./gradlew :app:testDebugUnitTest` passes, and `git diff -M --stat` shows the moved tests as renames.
- [ ] 3.7 Update the documentation and project rules:
  - `CLAUDE.md`:
    - tech stack: Ktor for REST;
    - Backends: two `HttpClient`s by qualifier, and the `BffAuth` plugin;
    - repo map: the networking code now in `:shared`, and the `di/NetworkModule` description;
    - "Adding a TMDB endpoint": `TmdbApi` is in `:shared`, wrap the call with `safeApiCall`, and tests go in `commonTest` using `FakeHttpServer` and `TestFixtures.kt`;
    - remove the public-mapper `TEMP` note.
  - `.claude/skills/android-architecture/SKILL.md`: the same module split.
  - `openspec/config.yaml`: in the `design` rules, "Retrofit stack" becomes "HTTP client (`@TmdbClient` or `@BffClient`)", and "(no Retrofit stack)" becomes "(no Ktor client)". In the `tasks` rule, the "fake APIs" reference becomes the `FakeHttpServer` harness.

  Verify: `grep -rni 'retrofit' CLAUDE.md .claude/skills/android-architecture/SKILL.md openspec/config.yaml` returns nothing, and every path `CLAUDE.md` names exists.

## 4. Integration verification

- [ ] 4.1 Run `./gradlew ktlintFormat`, then `./gradlew ktlintCheck`. Verify: it passes for both modules.
- [ ] 4.2 Run `./gradlew test` and `./gradlew :shared:iosSimulatorArm64Test`. Verify: all pass. The JVM test total across `:app` and `:shared` must be at least the 249 from before the change (220 + 29), plus the tests added in 1.4, 2.6 and 3.3. The iOS count must be above the earlier 21.
- [ ] 4.3 Run `./gradlew assembleDebug`. Verify: it builds.
- [ ] 4.4 Upgrade-install on the Galaxy S10 (`adb -s RZ8M22J5NBB install -r`) and check, while signed in:
  - Home shows all four rails;
  - search returns results;
  - Movie Detail shows cast and the production-country sheet;
  - TV shows loads page 1 and page 2 on scroll;
  - with airplane mode on, Home shows its no-connection error, and Retry recovers once the connection is back;
  - logcat shows `Authorization` redacted.

  Verify by observing each of these.

## Workflow follow-up

- Open one PR into `kotlin_multiplatform` with the three checkpoint commits. Archive the change after review.
- Next phase candidates: a multiplatform DI library to replace `SharedModule`, Firebase Auth and AI behind shared interfaces, or the SwiftUI app with a framework export.
