# Tasks

## 1. Dependencies and build setup

- [x] 1.1 Add Room 3 (`androidx.room3:room3-runtime`, `room3-compiler`) and the `androidx.room3` Gradle plugin to `gradle/libs.versions.toml` at the latest stable release; apply the plugin in `app/build.gradle.kts` with `room3 { schemaDirectory("$projectDir/schemas") }` and add `ksp(libs.room3.compiler)`; verify `./gradlew assembleDebug` succeeds
- [x] 1.2 Add Robolectric and `androidx.test:core` to the catalog at their latest stable releases as `testImplementation` only; verify `./gradlew test` still passes with no test changes

## 2. Domain contracts

- [x] 2.1 Change `domain/repository/WatchlistRepository.watchlist` from `StateFlow<List<Movie>>` to `Flow<List<Movie>>`, and update `FakeWatchlistRepository` and the tests that read `.value` (use Turbine or `first()`); verify `./gradlew test` passes and `WatchlistViewModel` / `MovieDetailViewModel` compile unchanged
- [x] 2.2 Add `val currentUserId: Flow<String?>` to `domain/repository/AuthRepository`; implement it in `AuthRepositoryImpl` with `callbackFlow` over `FirebaseAuth.AuthStateListener` → `currentUser?.uid` → `distinctUntilChanged()`, removing the listener in `awaitClose`; give `FakeAuthRepository` a settable `MutableStateFlow<String?>` for it; verify `domain/` has no Firebase or `android.*` imports and `./gradlew test` passes

## 3. Room storage

- [x] 3.1 Add `data/local/WatchlistEntity` (table `watchlist`, columns and composite primary key `(owner_uid, movie_id)` per design §2), `Converters` (genres `List<String>` ↔ JSON via kotlinx.serialization) and entity ↔ `Movie` mappers; verify a plain JVM test round-trips a `Movie` with null year, null poster and multiple genres through the mappers and converter
- [x] 3.2 Add `data/local/WatchlistDao` with `observe(uid): Flow<List<WatchlistEntity>>` (`ORDER BY added_at ASC, rowid ASC`), `observeContains(uid, movieId): Flow<Boolean>` and a `@Transaction suspend fun toggle(entity)` that deletes if `(owner_uid, movie_id)` exists, else inserts; add `MovieGalleryDatabase` (`version = 1`, `exportSchema = true`); verify `./gradlew assembleDebug` generates `app/schemas/.../1.json` and the file is committed
- [x] 3.3 Add a Robolectric `WatchlistDaoTest` against an in-memory database: insertion order including two rows with the same `added_at`, toggle on then off, re-adding moves a movie to the end, rows for one uid never appear in another uid's queries, and the genres column round-trips; verify `./gradlew :app:testDebugUnitTest --tests "*WatchlistDaoTest"` passes
- [x] 3.4 Add `di/DatabaseModule` providing the `@Singleton` database (`movie_gallery.db`, no destructive fallback) and `WatchlistDao`; verify `./gradlew assembleDebug` succeeds (Hilt graph resolves)

## 4. Repository

- [x] 4.1 Rewrite `data/repository/WatchlistRepositoryImpl` over `WatchlistDao`, `AuthRepository.currentUserId` and an injected `java.time.Clock` per design §6 (`flatMapLatest` switching, empty list and no-op toggle when there is no user); delete `InMemoryWatchlistDataSource`; verify `./gradlew assembleDebug` succeeds and no references to the in-memory source remain
- [x] 4.2 Rewrite `WatchlistRepositoryImplTest` as a Robolectric test using the real in-memory database, `FakeAuthRepository` and a fake clock, keeping the existing cases (full movie retained, insertion order, removing from the middle, `isInWatchlist` emissions and isolation) and adding scenarios from `specs/watchlist/spec.md`: "Toggle twice", "Rapid repeated taps" (many concurrent toggles leave at most one row, with the state set by how many taps there were), "Another account on the same device", "Changes stay with the account that made them", "Sign out and back in", "Back after another account" (A → B → A with A's order), plus no-user toggle writes nothing; verify `./gradlew :app:testDebugUnitTest --tests "*WatchlistRepositoryImplTest"` passes
- [x] 4.3 Add restart tests that use a file-backed Room database in Robolectric's temp directory, closing it and opening a new database and repository instance to simulate a process restart: "Process restart" (A before B, 2 saved), "Removal survives restart" (`isInWatchlist` false) and "Offline after restart" (stored details returned with no network dependency); verify they pass in `./gradlew test`

## 5. Docs

- [x] 5.1 Update `CLAUDE.md` (repo map: `data/local/` holds the Room database, DAO and entities; `di/` lists `DatabaseModule`; note that Room schemas live in `app/schemas/`) and the matching section of `.claude/skills/android-architecture/SKILL.md`; verify both describe the same layout and neither mentions `InMemoryWatchlistDataSource`

## 6. Verification

- [x] 6.1 Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck`; verify it reports no violations
- [x] 6.2 Run `./gradlew test`; verify all JVM tests pass, including the Robolectric classes
- [x] 6.3 Run `./gradlew assembleDebug`; verify the build succeeds and `app/schemas/` contains only the committed version 1 schema
- [x] 6.4 On a device or emulator: save two movies, kill the app from Recents, relaunch, and check the Watchlist tab shows them in order; sign out, sign in as a second account and check the tab is empty; sign back in as the first account and check its list returns

## Workflow follow-up

- Check in the Firebase console that Authentication → Settings → User account linking is "Link accounts that use the same email", so email/password and Google sign-ins with the same address share one watchlist.
- Archive the change after review with `/opsx:archive`.
