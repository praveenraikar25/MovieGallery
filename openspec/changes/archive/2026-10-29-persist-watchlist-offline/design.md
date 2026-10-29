## Context

See proposal.md (Why) for the motivation. The current state that shapes the approach:

- `WatchlistRepository` exposes `watchlist: StateFlow<List<Movie>>`, `isInWatchlist(id): Flow<Boolean>` and `suspend toggle(movie)`. It is backed by `InMemoryWatchlistDataSource`, a `MutableStateFlow<List<Movie>>` singleton.
- Consumers already treat the watchlist as a plain `Flow`. `ObserveWatchlistMoviesUseCase` returns `Flow<List<Movie>>`, `WatchlistViewModel` converts it with `stateIn(WhileSubscribed(5_000))`, and `MovieDetailViewModel` collects `ObserveIsInWatchlistUseCase`. Only tests read `.value`.
- `AuthRepository` exposes only `isSignedIn: Boolean`. `AuthRepositoryImpl` wraps an injected `FirebaseAuth` singleton (`AuthModule`), which restores the persisted user at cold start.
- Sign-in gates the main route (`AppSessionViewModel.startRoute`) and there is no guest mode, so a signed-in user exists whenever the Watchlist tab or Movie Detail is on screen.
- KSP is already applied (for Hilt), so the Room compiler needs no new build plugin beyond Room's own. JVM tests run with `unitTests.isReturnDefaultValues = true` and no Robolectric today.
- A Firestore sync is planned later (out of scope); this design keeps the local store compatible with it.

## Goals / Non-Goals

**Goals:**
- Make Room the single source of truth for the watchlist. The UI reads only from Room, so a later sync layer can write into Room without touching domain or UI code.
- Keep the domain layer free of Room and `android.*`. Room types stay in `data/local`.
- Test the real SQL (ordering, the atomic toggle, per-account separation, the genres converter) in `./gradlew test`.

**Non-Goals:**
- Sync bookkeeping (pending-upload flags, soft-deleted rows, server timestamps). These are added by the sync change in a schema migration.
- Clearing data on sign-out or when a different account signs in.
- Caching any other TMDB data in Room. The database is introduced for the watchlist only.

## Decisions

### 1. `watchlist` becomes `Flow<List<Movie>>`
Change the `WatchlistRepository.watchlist` type from `StateFlow` to `Flow`. The consumers already treat it as a `Flow`, so only the interface, the implementation and the test fakes change.

*Alternative:* keep `StateFlow` by calling `stateIn` on Room's flow with an app-wide `CoroutineScope`. Rejected. It needs a new scope binding, and its initial `emptyList()` would briefly show "Your watchlist is empty" on cold start before Room's first emission.

### 2. One table, keyed by account and movie
A `watchlist` table, one row per saved movie per account:

| column | type | notes |
|---|---|---|
| `owner_uid` | TEXT, not null | Firebase uid; primary key part 1 |
| `movie_id` | INTEGER, not null | TMDB id; primary key part 2 |
| `title`, `synopsis` | TEXT, not null | snapshot |
| `year` | INTEGER, nullable | null = "TBA" |
| `rating` | REAL, not null | snapshot |
| `genres` | TEXT, not null | JSON array via a type converter |
| `poster_url`, `backdrop_url` | TEXT, nullable | complete URLs, as in `Movie` |
| `added_at` | INTEGER, not null | epoch milliseconds |

The composite primary key `(owner_uid, movie_id)` makes a duplicate save impossible at the storage level. It also matches the likely Firestore layout `users/{uid}/watchlist/{movieId}`. The Firebase uid is stable across devices, reinstalls and sign-ins for the same account, so it is a durable key.

*Alternatives:* a table without the uid that is wiped on sign-out (rejected: with no server copy this loses the user's list); one database file per uid (rejected: more lifecycle code for no user-visible gain).

### 3. Order by `added_at`, then `rowid`
The list query is `WHERE owner_uid = :uid ORDER BY added_at ASC, rowid ASC`. `rowid` breaks ties between two saves in the same millisecond. Re-adding a removed movie inserts a new row, so it moves to the end, matching today's in-memory behavior. The time comes from an injected `java.time.Clock` (`Clock.systemUTC()` from `di/ClockModule`; a lambda type can't be bound cleanly by Hilt) so tests control ordering.

### 4. Toggle is one Room transaction in the DAO
The DAO has a `@Transaction suspend fun toggle(entity)` that checks whether `(owner_uid, movie_id)` exists, then deletes or inserts. Room serializes write transactions, so concurrent toggles from rapid taps each see the result of the previous one. The final state is set by how many taps happened, and the primary key ensures there is never a duplicate row.

*Alternative:* check and write from the repository as separate DAO calls. Rejected because two taps could interleave between the check and the write.

### 5. Genres stored as a JSON string
A Room `@TypeConverter` encodes `List<String>` with kotlinx.serialization, which is already in the build. Genres are only displayed, never queried, so a join table would add complexity for nothing.

### 6. Current account from `AuthRepository.currentUserId: Flow<String?>`
`AuthRepository` gains `val currentUserId: Flow<String?>`. `AuthRepositoryImpl` builds it with `callbackFlow` over `FirebaseAuth.AuthStateListener` (which fires on registration with the current state), maps it to `currentUser?.uid`, and applies `distinctUntilChanged()`. Domain code depends only on the interface, so there are no Firebase types in `domain`.

`WatchlistRepositoryImpl` then uses:
- `watchlist = currentUserId.flatMapLatest { uid -> uid?.let(dao::observe)?.map { it.toDomain() } ?: flowOf(emptyList()) }`
- `isInWatchlist(id)`: the same switch over a `dao.observeContains(uid, id)` query, with `distinctUntilChanged()`.
- `toggle(movie)`: reads `currentUserId.first()`. If it is null, it returns without writing (no rows without an owner). Otherwise it calls `dao.toggle(movie.toEntity(uid, now()))`.

`flatMapLatest` means a change of account immediately switches every open screen to the new account's rows.

*Alternative:* pass the uid in from the UI or a use case. Rejected because every call site would need to know about auth, and the account would be scoped by convention instead of by the repository.

### 7. Database wiring (Room 3)
Use Room 3 (`androidx.room3`, latest stable), not Room 2. Room 3 uses the `androidx.room3` packages, has `Flow` and suspend support built into `room3-runtime` (there is no `-ktx` artifact), renames the converter annotations to `@ColumnTypeConverter` / `@ColumnTypeConverters`, and goes through an `androidx.sqlite` driver. On Android the builder defaults to `AndroidSQLiteDriver` when no driver is set, so no driver dependency is added.

*Alternative:* Room 2.8.x, which is still maintained. Rejected because CLAUDE.md requires the latest stable release.

- `data/local/`: `WatchlistEntity`, `WatchlistDao`, `Converters`, `MovieGalleryDatabase` (`version = 1`, `exportSchema = true`), and entity ↔ `Movie` mappers. `InMemoryWatchlistDataSource` is deleted.
- `di/DatabaseModule`: `@Provides @Singleton` for the database (`Room.databaseBuilder(context, …, "movie_gallery.db")`, no `fallbackToDestructiveMigration`) and for `WatchlistDao`.
- The Room 3 Gradle plugin (`androidx.room3`) sets `room3 { schemaDirectory("$projectDir/schemas") }`. The generated `1.json` is committed so future migrations can be checked against it.
- Room's suspend and `Flow` queries run on Room's own executor, so the repository is main-safe without the `@IoDispatcher`.

### 8. Tests: Robolectric with in-memory Room
- `WatchlistDaoTest` (`@RunWith(RobolectricTestRunner::class)`) uses `Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), …)` and covers ordering with ties, the toggle, per-uid isolation, and the genres round trip.
- `WatchlistRepositoryImplTest` also runs under Robolectric, with the real in-memory database plus a `FakeAuthRepository` whose `currentUserId` is a `MutableStateFlow`. It covers account switching, no-user behavior, and the spec's A → B → A scenario.
- The database is closed after each test. Room's query context is pointed at the test dispatcher, so `runTest` and Turbine stay deterministic.
- ViewModel tests keep using `FakeWatchlistRepository`, updated to the `Flow` interface.

## Risks / Trade-offs

- [Room or KSP version mismatch with Kotlin 2.3 / AGP 9] → Pick the latest stable Room release whose KSP support matches the catalog's `ksp` version, and confirm with `assembleDebug` before writing code that depends on it.
- [Robolectric slows `./gradlew test` and may lag the app's `compileSdk`] → Only the two data-layer test classes use it. If the latest Robolectric doesn't support `compileSdk` yet, pin `@Config(sdk = [...])` to the highest supported level.
- [`isReturnDefaultValues = true` hides missing Android behavior in non-Robolectric tests] → Unchanged by this design; Robolectric classes use real Android framework classes regardless.
- [Account rows stay on disk after sign-out] → Accepted and marked with a spec "> Note:"; the rows are only reachable in the app by the same uid. They will be cleared on sign-out once sync exists.
- [Saved details go stale] → Accepted and marked with a spec "> Note:"; Movie Detail still loads current data.
- [Linking setting creates different uids for email/password and Google sign-ins with the same address] → With Firebase's default "Link accounts that use the same email" setting, they share one uid. If the console is set to "Create multiple accounts for each identity provider", the same person has two watchlists. This is a console check, not code.
- [A session ended by Firebase without a sign-out (revoked token, deleted account)] → `currentUserId` emits null and the watchlist shows empty. No rows are deleted.

## Migration Plan

- The first launch after the update creates `movie_gallery.db` at version 1. Nothing in memory is carried over (proposal: out of scope).
- Later schema changes (for example soft-delete columns for sync) add a `Migration(1, 2)` and are checked against the committed schema JSON. Destructive fallback stays off.
- Rollback: shipping a build without Room leaves the database file unused and harmless. Re-shipping this version picks it up again unchanged.
