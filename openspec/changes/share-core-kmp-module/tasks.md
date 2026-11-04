# Tasks

## 1. Shared module scaffolding

- [x] 1.1 Add catalog entries in `gradle/libs.versions.toml`: plugins `kotlin-multiplatform` (ref `kotlin`) and `android-kotlin-multiplatform-library` (ref `agp`), plus library `kotlinx-coroutines-core` (ref `coroutines`). Declare them `apply false` in the root `build.gradle.kts`. Verify with `./gradlew help`.
- [x] 1.2 Create `shared/build.gradle.kts` per design decision 2:
  - Android target with namespace `com.raikar.moviegallery.shared`, compileSdk 37, minSdk 29 and host tests enabled.
  - `iosArm64()` and `iosSimulatorArm64()`.
  - The ktlint plugin.
  - The `commonMain` and `commonTest` dependencies.

  Add `include(":shared")` to `settings.gradle.kts` and `implementation(project(":shared"))` to `:app`. Verify with `./gradlew :shared:compileKotlinIosSimulatorArm64 :app:assembleDebug`, which passes with the module still empty.

## 2. Domain layer

- [x] 2.1 Move `domain/model/` to `shared/src/commonMain/kotlin/com/raikar/moviegallery/domain/model/` with `git mv`. Replace both `"%.1f".format(rating)` calls with `expect fun formatRating(rating: Double): String`:
  - `androidMain` actual: keeps `"%.1f".format(rating)`.
  - `iosMain` actual: `NSNumberFormatter` with one fraction digit and `NSNumberFormatterRoundHalfUp`.

  Verify: `:shared:compileKotlinIosSimulatorArm64` and `:app:assembleDebug` pass.
- [x] 2.2 Move `domain/repository/` and `domain/usecase/` into `:shared` `commonMain`, removing every `javax.inject` import and `@Inject` constructor. Then add `app/.../di/SharedModule.kt` with one unscoped `@Provides` per use case (16). Verify: `:app:assembleDebug` passes, and `grep -rE 'import (android|java|javax)\.' shared/src/commonMain` returns nothing.
- [x] 2.3 Move the tests:
  - Move `CurrentUserTest` and `ProductionCountryTest` to `shared/src/commonTest`, converting the `org.junit` asserts and `@Test` to `kotlin.test`.
  - Add a `formatRating` `commonTest` that only checks locale-independent properties (design decision 10).

  Verify: they pass under `:shared:testAndroidHostTest`, or whatever task name the plugin generates, and under `:shared:iosSimulatorArm64Test`.

## 3. Pure data code (TMDB, BFF)

- [x] 3.1 Move `data/remote/dto/`, `MovieMappers.kt`, `TvShowMappers.kt`, `TmdbGenres.kt` and `TmdbImages.kt` into `:shared` `commonMain`. Make the declarations that `:app` still uses `public`, each with a one-line comment marking it data-layer-only until the Retrofit repositories move (design decision 9). Verify: `:app:assembleDebug` passes, and `MovieMappersTest`, `TvShowMappersTest`, `TmdbApiTest` and `BffApiTest` still pass under `:app:testDebugUnitTest`.

## 4. Countries GraphQL

- [x] 4.1 Move the Apollo plugin and `apollo { service("countries") }` block from `app/build.gradle.kts` to `shared/build.gradle.kts`, and `git mv` `app/src/main/graphql/countries/` to `shared/src/commonMain/graphql/countries/`. Verify: `./gradlew :shared:generateCountriesApolloSources` generates `CountryDetailsQuery` in the unchanged package.
- [x] 4.2 Move `CountryMappers.kt`, `SafeGraphQlCall.kt` and `CountryRepositoryImpl.kt` into `:shared` `commonMain`, dropping `javax.inject` and `@IoDispatcher` from the constructor. In `SharedModule`, add a `@Provides @Singleton` for `CountryRepository` that passes the `@IoDispatcher` dispatcher, and delete its `@Binds` from `RepositoryModule`. Verify: `CountryRepositoryImplTest` still passes in `:app`.
- [x] 4.3 Move the tests:
  - `CountryMappersTest` to `commonTest`, using `kotlin.test`.
  - `SafeGraphQlCallTest` to `:shared`'s Android host test source set.

  Verify: both pass under the `:shared` Android host test task, and `CountryMappersTest` also passes under `:shared:iosSimulatorArm64Test`.
- [x] 4.4 Update the GraphQL paths in `CLAUDE.md` (the Backends section and "Adding a GraphQL query") to `shared/src/commonMain/graphql/countries/` and `./gradlew :shared:downloadCountriesApolloSchemaFromIntrospection`. Verify: running the documented generate task as written succeeds.

## 5. Room watchlist

- [x] 5.1 Confirm Room 3.0.3's KMP database-constructor API: the names corresponding to Room 2.7's `@ConstructedBy` and `RoomDatabaseConstructor`. Check against the published 3.0.3 sources or docs, and record the names in a comment on the database class when it moves in 5.2. Verify: the names resolve when 5.2 compiles.
- [x] 5.2 Move the Room setup and code to `:shared`:
  - Move the `androidx.room3` plugin and `schemaDirectory` to `:shared`.
  - Add the Room compiler on `kspAndroid`, `kspIosArm64` and `kspIosSimulatorArm64`.
  - `git mv` `app/schemas/` to `shared/schemas/`.
  - Move `WatchlistEntity`, `WatchlistDao`, `MovieGalleryDatabase` (with the constructor hook from 5.1) and `Converters` into `commonMain`.

  `DatabaseModule` stays in `:app`, unchanged. Verify: after `./gradlew :app:assembleDebug :shared:compileKotlinIosSimulatorArm64`, `git diff --exit-code shared/schemas/` shows the exported schema is byte-identical.
- [x] 5.3 Move `WatchlistMappers.kt` to `commonMain`, making `toDomain` and `toWatchlistEntity` public as in 3.1. Move `WatchlistMappersTest` to `commonTest`. Verify: it passes on the Android host and the iOS simulator, and `WatchlistDaoTest` still passes in `:app`. (Applied: the mappers stayed `internal` — once 5.4 moved the repository, nothing in `:app` calls them.)
- [x] 5.4 Switch the watchlist repository to the Kotlin clock and move it:
  - Change `WatchlistRepositoryImpl` to take `kotlin.time.Clock` and write `clock.now().toEpochMilliseconds()`, opting into `ExperimentalTime` only if Kotlin 2.3.21 still requires it. Move it to `commonMain`.
  - `ClockModule` provides `Clock.System`.
  - Add a `@Provides @Singleton` for `WatchlistRepository` to `SharedModule`, and remove its `@Binds`.
  - Rewrite `TickingClock` as a `kotlin.time.Clock` that keeps advancing on each read.

  Verify: `WatchlistRepositoryImplTest` and `WatchlistPersistenceTest` pass in `:app`, including their insertion-order assertions.

## 6. Documentation

- [x] 6.1 Update `CLAUDE.md`:
  - Repo map: `:shared` (`commonMain`/`androidMain`/`iosMain`) vs `:app`, and which layers live where.
  - The Room schema path, now `shared/schemas/`.
  - The test commands for `:shared` (Android host and `iosSimulatorArm64Test`).
  - A note that shared classes are wired by `SharedModule` `@Provides`, not `@Inject`.
  - In the "Adding a feature screen" and "Adding a TMDB endpoint" checklists, the module each step's file now lives in.

  Verify: every path named in the file exists (`grep -oE '(app|shared)/[A-Za-z0-9_./-]+'` over it, then check each with `ls`).
- [x] 6.2 Update the `android-architecture` skill's module structure and Room schema path to match `CLAUDE.md`. Also update the `context` text in `openspec/config.yaml`, which still says "single-module Android app". Verify the two documents agree by reading them side by side; there's no automated check for this step.

## 7. Verification

- [x] 7.1 Run `./gradlew ktlintFormat`, then `./gradlew ktlintCheck`. Verify: ktlintCheck passes for both modules.
- [x] 7.2 Run `./gradlew test`, plus the `:shared` Android host test task if `test` doesn't include it, and `./gradlew :shared:iosSimulatorArm64Test`. Verify: all pass, and the test count across both modules is at least the count before the change.
- [x] 7.3 Run `./gradlew assembleDebug`. Verify: it builds.
- [x] 7.4 Upgrade-install on a device or emulator:
  1. Install the pre-change debug build, sign in and add a watchlist item.
  2. Install the new build over it (`adb install -r`).
  3. Confirm the watchlist item is still there, ratings render as before, and a production-country sheet opens on Movie Detail.

  Verify by observing those three things in the app.

## Workflow follow-up

- Archive the change after review, once `./gradlew test` passes on the merged result.
- Next phase: Retrofit/OkHttp → Ktor, which moves the movie and TV-show repositories and makes the mappers `internal` again.
