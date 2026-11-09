# Tasks

This is a data-layer refactor, so neither the "Adding a feature screen" nor the "Adding a TMDB endpoint" checklist in CLAUDE.md fits. Tasks follow the same order those checklists use for the data layer: data code first, then `di/`, then tests and docs. The change sets `skip_specs: true`, so every existing spec must still hold. The tests at risk are the `watchlist` restart and per-account persistence tests (`WatchlistPersistenceTest`, `WatchlistRepositoryImplTest`), and they must pass unchanged in what they assert. Run `./gradlew ktlintFormat` before each commit, and commit only once the user says to.

## 1. Shared setup and platform builders in `:shared`

- [x] 1.1 Add `shared/src/commonMain/.../data/local/MovieGalleryDatabaseFactory.kt` with `internal val MIGRATIONS: Array<Migration>` (empty) and `RoomDatabase.Builder<MovieGalleryDatabase>.applySharedConfig()`, which adds `MIGRATIONS` (design decision 1). Its KDoc says every new `Migration` goes into `MIGRATIONS` and that there is deliberately no destructive fallback. `MIGRATIONS` stays `internal`; `applySharedConfig()` is `public`, because `:app`'s tests in 3.1 and 3.2 call it. Its KDoc says it is for `databaseModule` and tests. Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes and `commonMain` has no `android.*`, `java.*` or `javax.*` imports.
- [x] 1.2 Add `shared/src/androidMain/.../data/local/MovieGalleryDatabase.android.kt` with `movieGalleryDatabaseBuilder(context: Context, name: String = MovieGalleryDatabase.NAME)`, which returns `Room.databaseBuilder(context, MovieGalleryDatabase::class.java, name)`: the exact call in `AndroidModules.kt` today, with no `setDriver` and no `setQueryCoroutineContext` (design decision 2). Its KDoc warns that the Context-less overload would open a different file. Verify: the Android compile passes (`./gradlew :shared:compileAndroidMain`, or the task name `./gradlew :shared:tasks --all` reports).
- [x] 1.3 Add `shared/src/iosMain/.../data/local/MovieGalleryDatabase.ios.kt` with `movieGalleryDatabaseBuilder(path: String = defaultDatabasePath())`, which returns `Room.databaseBuilder<MovieGalleryDatabase>(name = path).setDriver(NativeSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)`, importing `kotlinx.coroutines.IO`. `defaultDatabasePath()` resolves `<Application Support>/movie_gallery.db` with `NSFileManager.URLForDirectory(..., create = true, ...)` and throws if the directory can't be resolved, with no in-memory fallback (design decision 3). Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [x] 1.4 In `shared/build.gradle.kts`, add `linkerOpts("-lsqlite3")` to every binary of both iOS targets, with a comment saying the `sqlite-framework` interop declares no iOS linker flag (design decision 3). Verify: `./gradlew :shared:linkDebugTestIosSimulatorArm64` links with no undefined `sqlite3_*` symbols (the iOS suite in 3.3 also proves this).

## 2. Shared Koin database module

- [x] 2.1 Add `internal expect val databaseModule: Module` in `shared/src/commonMain/.../di/`, with `actual`s in `androidMain` (`single { movieGalleryDatabaseBuilder(get<Context>()).applySharedConfig().build() } onClose { it?.close() }`) and `iosMain` (the same without the `Context`). Verify: both targets compile, as in 1.2 and 1.3.
- [x] 2.2 In `SharedModules.kt`, add `databaseModule` to `sharedModules` and remove `MovieGalleryDatabase` from the KDoc's "platform must supply" list. Add that Android must call `androidContext(...)` (it already does). Verify: `./gradlew :shared:testAndroidHostTest` passes, including `SharedModulesTest`.
- [x] 2.3 In `app/src/main/.../di/AndroidModules.kt`, remove the Room `single`, its "No fallbackToDestructiveMigration" comment (that rule now lives in task 1.1's KDoc) and the unused `Room` / `MovieGalleryDatabase` imports. Verify: `./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.di.*"` passes, meaning `KoinGraphTest` and `KoinGraphResolutionTest` resolve the database from the shared module and close it.

## 3. Tests on both platforms

- [x] 3.1 Point `app/src/test/.../testutil/TestDatabases.kt` at the production setup: `onDisk(name)` uses `movieGalleryDatabaseBuilder(context, name).applySharedConfig().build()`, and `inMemory()` keeps `Room.inMemoryDatabaseBuilder(context, …)` and also applies `applySharedConfig()` (design decision 4). Verify: `WatchlistDaoTest`, `WatchlistRepositoryImplTest` and `WatchlistPersistenceTest` pass with no change to their assertions (`./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.data.*"`).
- [x] 3.2 Add `app/src/test/.../data/local/DatabaseUpgradeTest.kt` (Robolectric, `@Config(application = Application::class)`). It deletes `movie_gallery.db` in `@Before`, saves two movies for one uid through a database opened with a verbatim copy of today's call, `Room.databaseBuilder(context, MovieGalleryDatabase::class.java, "movie_gallery.db")`, and closes it. It then opens the database the way the app now does (`movieGalleryDatabaseBuilder(context)` with its default name, plus `applySharedConfig()`) and asserts both movies are listed in the order they were added. A comment says the old call is copied on purpose to pin down the file today's installs have. Verify: `./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.data.local.DatabaseUpgradeTest"` passes. Also check it fails when the builder's default name is temporarily changed, then revert that.
- [x] 3.3 Add `shared/src/iosTest/.../data/local/WatchlistDaoIosTest.kt` (`kotlin.test`, `runTest`) on an in-memory database built with `Room.inMemoryDatabaseBuilder<MovieGalleryDatabase>().setDriver(NativeSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).applySharedConfig()`. Cover:
  - saved movies are listed oldest-added first;
  - toggling adds and then removes;
  - one account's rows never appear for another.

  Add an on-disk test using `movieGalleryDatabaseBuilder(path)` under `NSTemporaryDirectory()` (deleting the file first and afterwards) that saves, closes, reopens and reads back. Test names contain none of `,` `.` `:` `;`. Verify: `./gradlew :shared:iosSimulatorArm64Test` passes.

## 4. Docs

- [x] 4.1 Update `CLAUDE.md`:
  - Repo map, `:shared` entry: the database builders live under `data/local/` (shared setup in `commonMain`, a builder per platform).
  - `di/` section: `sharedModules` includes `databaseModule`; `androidPlatformModule` no longer lists the Room database.
  - `data/local/` entry: new migrations go into the shared `MIGRATIONS` list.

  Verify: `grep -n "Room database" CLAUDE.md` no longer attributes the database to `androidPlatformModule`.
- [x] 4.2 Update `.claude/skills/android-architecture/SKILL.md`, local persistence bullet: the database is built in `:shared`, with a per-platform builder (Android `Context`; iOS Application Support path, `NativeSQLiteDriver`, `-lsqlite3`), and migrations are registered once in shared code. Verify: the skill and CLAUDE.md describe the same layout, with no detail contradicting the other.

## 5. Verification

- [x] 5.1 Run `./gradlew ktlintCheck`. Verify: it passes.
- [x] 5.2 Run `./gradlew test`. Verify: all `:app` and `:shared` JVM tests pass.
- [x] 5.3 Run `./gradlew :shared:iosSimulatorArm64Test`. Verify: it passes, including the new iOS DAO suite.
- [x] 5.4 Run `./gradlew assembleDebug`. Verify: the build succeeds.
- [x] 5.5 Manual upgrade check on a device or emulator (design, Migration Plan): install the `main` build, sign in, save two movies, install this build over it without uninstalling, and confirm both movies are still on the Watchlist tab in the order they were added. Verify: the user confirms the result. This is not a code task, so report it rather than claiming it.

## Workflow follow-up

- Archive the change once the PR is merged and `./gradlew test` passes on `main`.
