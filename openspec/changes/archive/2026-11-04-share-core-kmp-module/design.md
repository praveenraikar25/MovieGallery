# Design

## Context

Today everything is in `:app`: AGP 9.2.1 with AGP's built-in Kotlin support, Kotlin 2.3.21, Hilt 2.60.1, Room 3.0.3 and Apollo 5.2.0. An audit of the imports shows the domain layer depends on nothing platform-specific except two things:

- `javax.inject.Inject` on the 16 use cases.
- `"%.1f".format(rating)` in `Movie.kt` and `TvShow.kt`. This is JVM `String.format`: it uses the default locale and HALF_UP rounding.

The Room 3 runtime publishes `iosArm64` and `iosSimulatorArm64` variants (checked on Google Maven for 3.0.3), and so does `androidx.sqlite`'s bundled driver. Apollo's runtime is multiplatform. kotlinx.serialization already backs the DTOs and the Room converters.

The mappers, `TmdbGenres`, `TmdbImages` and `safeGraphQlCall` are currently `internal`, and Retrofit-backed repositories that stay in `:app` use them.

See proposal.md for motivation and scope.

## Goals / Non-Goals

**Goals:**
- `:shared` compiles for Android, `iosArm64` and `iosSimulatorArm64`, and its common tests pass on the Android host and the iOS simulator.
- `:app` produces an app that behaves the same as before: same rating label text, same database file and schema, same network calls.
- The layering in `CLAUDE.md` holds inside `:shared`, and `commonMain` has no `android.*`, `java.*` or `javax.*` imports.

**Non-Goals:**
- Any change to DI, networking, Firebase or UI technology (proposal: Out of scope).
- An iOS-facing API surface (framework export, Swift-friendly naming, SKIE). That comes when an iOS app consumes the module.

## Decisions

### 1. Keep `:app` and add `:shared`, rather than renaming to `:androidApp` + `:shared`
AGP 9 doesn't allow the KMP plugin on a `com.android.application` module, so the shared code has to live in a library module either way. Keeping the name `:app` leaves several references valid that a rename would break:
- the git-ignored `app/google-services.json` (a rename wouldn't move it, and the build would fail);
- the `.githooks/pre-commit` report paths;
- the `app/src/test/.../testutil` paths that the OpenSpec task rules require;
- every `:app:` command in `CLAUDE.md`.

Alternative: rename now to match the JetBrains wizard layout. Rejected, because it adds churn without functional gain. A rename can happen in its own change if it's ever wanted.

### 2. `:shared` uses the Kotlin Multiplatform plugin plus AGP's KMP library plugin
- **Plugins:** `org.jetbrains.kotlin.multiplatform`, version from the existing `kotlin` ref, plus `com.android.kotlin.multiplatform.library`, version from the existing `agp` ref. AGP 9 deprecates the classic `com.android.library` + KMP combination.
- **Android target:** configured inside the `kotlin {}` block with `namespace = "com.raikar.moviegallery.shared"`. `compileSdk` 37 and `minSdk` 29 match `:app`. Host tests are enabled so `commonTest` runs on the JVM during `./gradlew test`.
- **iOS targets:** `iosArm64()` and `iosSimulatorArm64()`. `iosX64` is skipped, since Intel simulators aren't needed on this machine and JetBrains has deprecated the target.
- **Plugins moved from `:app` to `:shared`:** ktlint, kotlinx.serialization, KSP, `androidx.room3` and Apollo. `:app` keeps Hilt, KSP (for Hilt), Compose, google-services and serialization (still used by Retrofit DTO parsing and `NetworkModule`).
- **Dependencies:** `commonMain` declares `kotlinx-coroutines-core` (new catalog entry, existing `coroutines` version), `kotlinx-serialization-json`, `room3-runtime` and `apollo-runtime`, all as `api`. `:app` sees them through its `:shared` dependency, but keeps its own direct declarations where it still uses them, so its build file still reads honestly.

### 3. Packages don't change
Files move with their existing `com.raikar.moviegallery.*` packages. The source path changes; imports in `:app` don't. The generated Apollo package `com.raikar.moviegallery.data.remote.graphql.countries` is also unchanged.

Alternative: a `com.raikar.moviegallery.shared.*` package. Rejected: it would touch every importing file in `:app` for nothing.

### 4. Hilt reaches shared classes through `@Provides`, not `@Inject`
`javax.inject` is a JVM artifact, so `commonMain` can't use it. Dagger can't add `@Inject` to classes it doesn't compile, so `:app` gets a new `di/SharedModule.kt`:
- one `@Provides` per use case (16 of them), unscoped as today;
- `@Provides @Singleton` for `WatchlistRepository` and `CountryRepository`, replacing their two `@Binds` in `RepositoryModule`. `CountryRepository` keeps receiving the `@IoDispatcher` dispatcher.

The `@IoDispatcher` qualifier stays in `:app`. Shared constructors take a plain `CoroutineDispatcher`.

Alternatives considered:
- An `expect annotation class Inject` with `actual typealias` to `javax.inject.Inject` on Android. Rejected: Dagger's KSP processor doesn't reliably resolve typealiased annotations, and the trick leaves an iOS no-op annotation behind.
- Moving to a KMP-friendly DI library now. Out of scope; that's the phase that deletes `SharedModule`.

### 5. The rating label becomes a per-platform function
`expect fun formatRating(rating: Double): String` lives in `domain/model`:
- **Android:** `"%.1f".format(rating)`, byte-for-byte the current behavior, locale included.
- **iOS:** an `NSNumberFormatter` with one fraction digit and `NSNumberFormatterRoundHalfUp`, using the current locale.

Alternative: hand-rolled rounding in common code. Rejected: it would change Android output for locales that use a comma decimal separator, and could differ on HALF_UP edge cases.

### 6. `kotlin.time.Clock` replaces `java.time.Clock`
- `WatchlistRepositoryImpl` takes a `kotlin.time.Clock` and stores `clock.now().toEpochMilliseconds()` in the same `addedAt` column, with the same unit.
- `ClockModule` provides `Clock.System`.
- `TickingClock` in `:app` tests is rewritten as a `kotlin.time.Clock`, keeping its "advance on every read" behavior.

If `kotlin.time.Clock` still needs `@OptIn(ExperimentalTime::class)` under Kotlin 2.3.21, opt in at the use sites. Don't add the `kotlinx-datetime` dependency for this.

### 7. Room moves whole; the database builder stays in `:app`
- **What moves:** the entity, DAO, database and converters go to `commonMain`.
- **KMP requirement:** Room KMP needs the database to declare a generated constructor: `@ConstructedBy` on the database, plus an `expect object` implementing the constructor interface, whose `actual` KSP generates per target. Check the exact Room 3 names against the 3.0.3 API; Room 2.7 calls these `ConstructedBy` and `RoomDatabaseConstructor`.
- **Compiler wiring:** the Room compiler is added per target (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`).
- **Schemas:** `room3 { schemaDirectory(...) }` moves to `:shared`, and `app/schemas/` moves to `shared/schemas/` with `git mv`. The exported `1.json` must come out byte-identical after a build.
- **What stays:** `DatabaseModule` remains in `:app` with the same `Room.databaseBuilder(context, …)` call and database name. No driver change and no migration. An iOS builder is a later phase.

### 8. Apollo service configuration moves to `:shared`
- `apollo { service("countries") }` moves with the same package and introspection endpoint. `src/main/graphql/countries/` becomes `shared/src/commonMain/graphql/countries/`.
- The `downloadCountriesApolloSchemaFromIntrospection` task becomes `:shared:`-prefixed.
- `GraphQlModule` stays in `:app`, because it builds the client from the shared `OkHttpClient`.

### 9. Mapper visibility widens to `public` for now
These become public with a one-line comment marking them as data-layer-only until the repositories move: the `toDomain` mappers, `toReleaseYear`, `TmdbGenres`, `TmdbImages` and `toWatchlistEntity`. `safeGraphQlCall` and `toAppError` stay `internal`, because only shared code calls them, so `SafeGraphQlCallTest` moves into `:shared`'s Android host tests.

Alternative: a `@RequiresOptIn` marker annotation. Rejected as ceremony for something the next phase reverts.

### 10. Tests split by what they need
Which test goes where:

| Where | Tests | Why |
|---|---|---|
| `:shared` `commonTest` (`kotlin.test`; runs on the Android host and the iOS simulator) | `CurrentUserTest`, `ProductionCountryTest`, `WatchlistMappersTest`, `CountryMappersTest` | Plain Kotlin only |
| `:shared` Android host tests | `SafeGraphQlCallTest` | Uses `java.util.UUID`, and needs access to `internal` code |
| Stay in `:app` | `MovieMappersTest`, `TvShowMappersTest` | Read JSON fixtures from the JVM classpath |
| Stay in `:app` | DAO, persistence and watchlist repository tests | Robolectric |
| Stay in `:app` | `CountryRepositoryImplTest` | MockWebServer |
| Stay in `:app` | `FirebaseAuthTokenProviderTest` | MockK |

Tests that stay in `:app` exercise the moved classes through the dependency, so coverage doesn't drop.

Add one `commonTest` for `formatRating` that only checks inputs whose output doesn't depend on the locale's decimal separator (such as the digit count after rounding). This protects the iOS version without making the test locale-flaky.

## Risks / Trade-offs

- **[Risk]** Room 3's KMP constructor API differs from Room 2.7's. → Mitigation: check it in the first Room task, against the 3.0.3 sources or docs, before moving the DAO.
- **[Risk]** The exported Room schema changes when KSP runs from `:shared`. A different identity hash would break the next migration check. → Mitigation: a task diffs `shared/schemas/.../1.json` against the committed file and fails the change on any difference.
- **[Risk]** `./gradlew test` doesn't pick up `:shared`'s Android host tests or iOS simulator tests. → Mitigation: the verification tasks run the explicit task names, and `CLAUDE.md` documents them.
- **[Risk]** iOS simulator tests need Xcode with an installed simulator runtime. → Mitigation: the iOS test task is a local verification step; it doesn't gate `./gradlew test`.
- **[Trade-off]** `SharedModule` adds about 18 boilerplate `@Provides` functions that the DI phase deletes. This is accepted, because it keeps this change free of a DI migration.
- **[Trade-off]** Public mappers can be misused from the UI layer in `:app` until the Ktor phase re-internalizes them. This is accepted for a short window, and the comment on each one flags it.

## Migration Plan

This is a pure build and source move, with no data migration: the database name, version and schema are unchanged. Land it as one PR. Rolling back means reverting that PR. Before merging, the device sanity check in the tasks confirms an existing install keeps its watchlist.

## Open Questions

- The exact AGP 9 DSL name for host-test enablement in the KMP library plugin (`withHostTest {}` vs a newer spelling) is resolved when writing the build file. It doesn't affect the approach.
