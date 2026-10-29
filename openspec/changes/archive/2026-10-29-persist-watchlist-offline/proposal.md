## Why

The watchlist is held in memory only, so everything a user saves disappears when the app process is killed. It is also shared by whoever signs in during the same process. The app is meant to ship to production, and a watchlist that does not survive a restart is not usable. Storing it on the device per signed-in account fixes both problems and sets up the local store that a later Firestore sync can build on.

## What Changes

- Save the watchlist on the device so saved movies survive the app being killed and relaunched, and keep the order in which they were added (oldest first).
- Keep a separate watchlist for each signed-in account: a user only ever sees the movies they saved themselves on this device.
- Keep each account's saved movies on the device after sign-out, so the same user gets their list back when they sign in again, including after another account has used the device.
- Make adding and removing a movie a single all-or-nothing step, so quick repeated taps can never save a movie twice.
- Saved movies keep the details captured when they were saved (title, year, rating, genres, synopsis, poster); they are not refreshed from TMDB.
- **BREAKING** (behavior): "Watchlist lasts only for the app process" is reversed; the watchlist is no longer empty after a restart and no longer shared between accounts.

**Backends touched:** none. The watchlist stays on the device; it only reads the signed-in account's identity from Firebase Authentication's local session. TMDB, BFF and Firebase AI Logic are untouched.

**Resolves "> Note:" gaps:**
- `watchlist` > "Watchlist lasts only for the app process" (in-memory only, not saved to disk, shared by whoever signs in during the same process).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `watchlist`: "Watchlist lasts only for the app process" is replaced by a requirement that the watchlist survives app restarts and is kept separately for each account, staying on the device across sign-out. "Movies are added and removed by toggling" gains the guarantee that repeated toggles never save a movie twice. The other requirements are unchanged.

## Out of scope

- Syncing the watchlist with Firestore or any server, and so seeing the same watchlist on another device, after a reinstall, or after app data is cleared. Planned as a future change.
- Clearing the on-device watchlist on sign-out or when a different account signs in. Deferred until Firestore sync exists; without a server copy, clearing would destroy the user's list.
- Refreshing saved movies' details (rating, release year, poster) from TMDB.
- Migrating anything saved in memory before this change ships (there is nothing on disk to migrate).
- Removing an account's on-device rows when the Firebase account is deleted.
- Reordering, sorting or filtering the watchlist.

## Pre-production debt

- Saved movie details can go stale. A movie saved as "TBA" or with an early rating keeps those values on the Watchlist tab while Movie Detail shows current TMDB data. Accepted for now; marked with a "> Note:" in the `watchlist` spec.
- An account's saved movies stay in the app's private storage on the device after that account signs out. Other accounts cannot see them in the app. They will be cleared on sign-out once Firestore sync provides a server copy.

## Impact

- **Dependencies** (declared in `gradle/libs.versions.toml` at the latest stable releases): Room 3 (`androidx.room3`: `room3-runtime`, `room3-compiler` via the existing KSP plugin) and the Room 3 Gradle plugin for schema export; Robolectric and `androidx.test:core` as test-only dependencies.
- **Code**:
  - `domain/repository/WatchlistRepository`: `watchlist` changes from `StateFlow` to `Flow`.
  - `domain/repository/AuthRepository`: gains a `Flow` of the current user id, implemented in `AuthRepositoryImpl` from Firebase Auth's auth-state listener.
  - `data/local`: a Room database, DAO, entity and genres type converter replace `InMemoryWatchlistDataSource`.
  - `data/repository/WatchlistRepositoryImpl`: rewritten over the DAO and the current user id.
  - A new Hilt module provides the database and DAO.
  - Watchlist and Movie Detail ViewModels need little or no change, since they already convert the flows with `stateIn`.
- **Build**: Room schema JSON is exported to `app/schemas/` and checked in.
- **Tests**: Robolectric tests for the DAO and repository against an in-memory Room database; `FakeWatchlistRepository` and `FakeAuthRepository` updated for the new interfaces; existing ViewModel tests adjusted.
- **Docs**: the `CLAUDE.md` repo map (`data/local/`, `di/`) and the `android-architecture` skill note the Room database.
