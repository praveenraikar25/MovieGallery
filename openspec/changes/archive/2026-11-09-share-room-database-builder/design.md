# Design

## Context

See proposal.md (Why) for the motivation. The current state and constraints that shape the approach, checked against the Room 3.0.3 and `androidx.sqlite` 2.7.1 sources in the Gradle cache:

- `MovieGalleryDatabase` (`:shared` `commonMain` `data/local/`) is version 1 with `exportSchema = true`, has no migrations and no destructive fallback, and declares `expect object MovieGalleryDatabaseConstructor`. Room's compiler generates the `actual` for Android and both iOS targets.
- It is built only at `AndroidModules.kt:89`: `Room.databaseBuilder(androidContext(), MovieGalleryDatabase::class.java, MovieGalleryDatabase.NAME).build()`, with no driver and no query context set.
- **Android:** when no driver is set, Room's `build()` falls back to `AndroidSQLiteDriver()` (`RoomDatabase.android.kt`). With a `Context`, the name is resolved through `context.getDatabasePath(name)`.
- **iOS:** the native `build()` fails with `requireNotNull(driver)` ("Cannot create a RoomDatabase without providing a SQLiteDriver via setDriver()"). The native `Room.databaseBuilder<T>(name)` treats `name` as a file path. `NativeSQLiteDriver` lives in `androidx.sqlite:sqlite-framework`, which `room3-runtime`'s iOS variants already depend on.
- The iOS SQLite interop klib in `sqlite-framework` declares `linkerOpts` for Linux and macOS but not for iOS, so iOS binaries don't link `libsqlite3` on their own.
- `dataModule` already provides `factory<WatchlistDao> { get<MovieGalleryDatabase>().watchlistDao() }`. Nothing in `:shared` needs to change for consumers of the database.
- Android tests that open the database: `WatchlistDaoTest` (7 tests), `WatchlistRepositoryImplTest` (15), `WatchlistPersistenceTest` (on-disk, deletes the file in `@Before`), all through `app/src/test/.../testutil/TestDatabases.kt`, plus `KoinGraphResolutionTest`, which builds the real graph and closes the database afterwards. `:shared` has no database tests apart from `WatchlistMappersTest`.

## Goals / Non-Goals

**Goals:**
- Database configuration that both platforms must share is defined once, in `commonMain`.
- The Android database is opened by exactly the same call as today, so existing installs open the same file with the same driver and threading.
- The iOS database can be built, written, closed and reopened, and that is proven by tests running on the iOS simulator.
- The database comes out of `sharedModules` on both platforms, so phase 5's iOS Koin start has nothing to supply for it.

**Non-Goals:**
- A shared test suite covering both platforms. The Android DAO tests stay Robolectric tests in `:app`; iOS gets its own suite in `iosTest`.
- Any change to what the database stores or how the watchlist behaves.

## Decisions

### 1. Three layers: shared setup, platform builder, shared Koin module

- **`commonMain` `data/local/MovieGalleryDatabaseFactory.kt`**: `internal val MIGRATIONS: Array<Migration>` (empty) and `internal fun RoomDatabase.Builder<MovieGalleryDatabase>.applySharedConfig()`, which adds `MIGRATIONS`. A KDoc line says the destructive fallback is deliberately absent and that every new `Migration` goes into `MIGRATIONS`. Room 3 migrations take a `SQLiteConnection`, so they can be written in common code.
- **`androidMain` / `iosMain` `data/local/MovieGalleryDatabase.<platform>.kt`**: an `internal fun movieGalleryDatabaseBuilder(...)` returning an unbuilt `RoomDatabase.Builder<MovieGalleryDatabase>`. It sets only what differs by platform: where the file lives, the driver and, on iOS, the query context.
- **`commonMain` `di/`**: `internal expect val databaseModule: Module`, with `actual`s in `androidMain` and `iosMain`, each declaring `single { movieGalleryDatabaseBuilder(...).applySharedConfig().build() } onClose { it?.close() }`. `sharedModules` becomes `listOf(networkModule, dataModule, domainModule, databaseModule)`.

Builders return an unbuilt `Builder` rather than a database so tests can apply the shared configuration and then build. That way the tests exercise the same setup as production.

*Alternatives considered:*
- **A single `expect fun createDatabase(): MovieGalleryDatabase`.** This hides the builder, so tests can't reuse the production setup, and an `expect fun` on Android would need the `Context` passed in anyway.
- **The platform keeps supplying the database (status quo), and iOS's Koin module builds its own.** This is what the proposal sets out to remove: two copies of the migration list.
- **An `expect class DatabaseFactory`.** It adds a Beta `expect class` and an object to inject, for no benefit over a function plus a module.

### 2. Android: the identical call, no driver, no query context

The Android builder keeps today's exact call: `Room.databaseBuilder(context, MovieGalleryDatabase::class.java, MovieGalleryDatabase.NAME)`. It does not call `setDriver` or `setQueryCoroutineContext`.

- Room's default `AndroidSQLiteDriver` and its own executors stay in place, so threading and SQLite behaviour are unchanged.
- The `Context` comes from `get<Context>()`, which `androidContext(...)` in `MovieGalleryApplication` already registers, so `:shared` doesn't need `koin-android`.
- The Context-less `databaseBuilder<T>(name)` overload is **not** used on Android. It treats the name as a path relative to the working directory and needs an explicit driver, so it would open a different, empty file.

*Alternatives considered:*
- **`BundledSQLiteDriver` on both platforms**, for one SQLite version everywhere. It adds a native library to the APK and changes the SQLite build under an existing file, for no current need.
- **`setQueryCoroutineContext(Dispatchers.IO)` in the shared setup.** It would move Android queries off Room's executor. The change is small, but it breaks the rule that Android behaves exactly as before.

### 3. iOS: system SQLite, Application Support, `Dispatchers.IO`

- **Driver: `NativeSQLiteDriver`.** It needs no new dependency and adds no binary size. `BundledSQLiteDriver` can replace it later if a SQLite feature newer than the system one is ever needed.
- **File location:** `<Application Support>/movie_gallery.db`, found with `NSFileManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, appropriateForURL = null, create = true, error = …)`. Passing `create = true` makes the directory on first launch. A failure is an exception: the app can't run without its database, and a silent fallback to an in-memory database would be the in-memory-persistence placeholder the project rules forbid. Documents was rejected because it holds user-facing files and appears in the Files app once file sharing is enabled.
- **Queries:** `setQueryCoroutineContext(Dispatchers.IO)`, the dispatcher Room's KMP docs recommend. On Native it needs an explicit `import kotlinx.coroutines.IO`.
- **Linking:** in `shared/build.gradle.kts`, add `linkerOpts("-lsqlite3")` to every binary of both iOS targets (`targets.withType<KotlinNativeTarget>().configureEach { binaries.configureEach { linkerOpts("-lsqlite3") } }`). This covers the test executables now and the framework phase 5 adds.

### 4. Tests

- **Android, production setup:** `TestDatabases.onDisk(name)` becomes `movieGalleryDatabaseBuilder(context, name).applySharedConfig().build()`. To allow that, the Android builder takes an optional `name` defaulting to `NAME`, and is reachable from `:app`'s tests (`internal` doesn't cross modules, so the Android builder and `applySharedConfig()` are `public`, with a KDoc saying they are for `databaseModule` and tests). `inMemory()` stays on `Room.inMemoryDatabaseBuilder(context, …)` but also applies `applySharedConfig()`.
- **Android, upgrade safety:** a new Robolectric test, `DatabaseUpgradeTest`, writes watchlist rows through a database opened with a verbatim copy of today's call, `Room.databaseBuilder(context, MovieGalleryDatabase::class.java, "movie_gallery.db")`, closes it, then opens the database the way the app now does, by resolving it from `sharedModules` with `androidContext` (or from the Android builder with its default name), and asserts the same rows come back in the same order. The old call is copied into the test on purpose: it pins down the file that today's installs have on disk.
- **iOS:** a new `shared/src/iosTest/.../data/local/WatchlistDaoIosTest.kt` covering insert, `observe` order, per-account isolation and toggle on an in-memory database (`Room.inMemoryDatabaseBuilder<MovieGalleryDatabase>().setDriver(NativeSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).applySharedConfig()`), plus one on-disk test under `NSTemporaryDirectory()` that writes, closes, reopens and reads back. Test names avoid `, . : ;` (a Kotlin/Native restriction).
- **Koin:** `KoinGraphResolutionTest` already resolves and closes `MovieGalleryDatabase`, so it now checks the shared binding with no edit expected. `KoinGraphTest`'s `verify()` only inspects constructor-reference definitions, so the lambda-based `single` needs no extra types. Both are run to confirm.

## Risks / Trade-offs

- [**Android opens a different file**, so every user's watchlist appears empty after updating, with no error] → Decision 2 keeps the exact call, the Context-less overload is ruled out, and `DatabaseUpgradeTest` writes with today's call and reads with the new one.
- [**iOS binaries fail to link** with undefined `sqlite3_*` symbols] → the `-lsqlite3` linker option applies to all iOS binaries; `iosSimulatorArm64Test` proves it for test executables.
- [**`Dispatchers.IO` resolves on Android but not iOS** because the Native import is missing] → it is used only in `iosMain` and `iosTest`, which `./gradlew :shared:compileKotlinIosSimulatorArm64` and the iOS tests compile.
- [**The Android builder becomes public API** of `:shared`, so `:app` code could build a second database] → a KDoc on the function, and the production path goes through `databaseModule`. A future `:shared` test fixtures setup could make it `internal` again.
- [**The iOS DAO suite duplicates part of the Android one**] → accepted for now: it is intentionally smaller and exists to prove the iOS driver path, not to retest DAO logic the Android suite already covers.
- [**System SQLite on iOS is a different version from Android's**] → the schema uses only basic SQL (one table, plain columns), so behaviour is the same on both. Switching iOS to `BundledSQLiteDriver` is a one-line change if that ever stops being true.

## Migration Plan

No data migration: the schema stays at version 1 and Android opens the same file. Rollback means reverting the commit. Because the file and schema are unchanged, the previous build reads the same data. Before merging, do a manual upgrade check on a device: install the `main` build, save two movies, install this build over it, and confirm both are still listed in the order they were added.
