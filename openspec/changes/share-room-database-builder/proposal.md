# Proposal

## Why

Phase 4a of the KMP migration. `:shared` already holds the Room schema (entities, DAO, converters, the `@ConstructedBy` constructor, exported schemas) and runs Room's compiler for both iOS targets, but nothing outside `:app` can open the database: it is built only in `:app`'s `androidPlatformModule` from an Android `Context`, and the `sharedModules` KDoc lists `MovieGalleryDatabase` as something every platform must supply. An iOS app would have to rebuild the database itself, with its own copy of the migration list, which can drift from Android's. This change moves database construction into `:shared`, with only the platform-specific part (Android `Context`; iOS file path and SQLite driver) per platform, before phase 4b moves the ViewModels. The Android app must behave exactly as it does today, and existing installs must keep their saved watchlists.

## What Changes

- **Shared database setup in `commonMain`**: one function that applies the configuration both platforms must share. Today that is the (empty) migration list and the rule that there is no destructive fallback. Every future `Migration` is added there once.
- **Android builder in `androidMain`**: builds from the application `Context` with the same file name (`movie_gallery.db`), the same location and the same default SQLite driver as today, so existing installs open the same file.
- **iOS builder in `iosMain`**: builds from a file in the app's Application Support directory (created if missing), with the system SQLite driver and queries on `Dispatchers.IO`.
- **A shared Koin database module**: `:shared` provides `MovieGalleryDatabase` on both platforms, closing it when the Koin graph closes (as the Ktor clients already are). The Room binding is removed from `androidPlatformModule`, and `MovieGalleryDatabase` is removed from the "platform must supply" list in the `sharedModules` KDoc. Android still supplies only `androidContext(...)`, which it already does.
- **iOS linking**: Kotlin/Native iOS binaries link the system SQLite library (`-lsqlite3`); the SQLite interop library sets no linker flag for iOS.
- **Tests**:
  - Android: the Robolectric DAO, repository and persistence tests run against the production builder instead of a copy of it, plus a new upgrade test proving a database written by today's builder is read back, unchanged, by the new one.
  - iOS: a new `iosTest` suite for the watchlist DAO on both an in-memory and an on-disk database, including closing and reopening the file.
  - The Koin graph tests resolve the database from the shared module.
- **Docs**: `CLAUDE.md` (repo map and `di/` section) and the `android-architecture` skill stop saying `:app` builds the Room database.

Backends touched: none. Only on-device storage changes, and only in how it is opened.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This changes where the database is constructed, not what it stores or how the watchlist behaves, so the change sets `skip_specs: true`. Every existing spec must still hold unchanged. The requirements most exposed to a mistake here are in `watchlist`: "Watchlist survives app restarts" and the per-account requirement that saved movies stay on the device after sign-out. Both hold only if the Android build keeps opening the same database file.

## Out of scope

- Moving ViewModels or any UI into `:shared` (phase 4b).
- An iOS Koin entry point (`initKoin()`), an iOS app, or iOS auth and AI implementations (phase 5).
- Moving the full DAO/repository test suite into `commonTest` (it would need Robolectric in `androidHostTest`); the Robolectric tests stay in `:app`.
- Switching Android to the bundled SQLite driver, or changing Android's query threading.
- Any schema change, migration, new table or new column. The database stays at version 1.
- Moving Apollo or Coil construction into `:shared`.

## Pre-production debt

None. No placeholder behavior is introduced.

## Impact

- **Build:** no new dependencies. The iOS SQLite driver comes from `androidx.sqlite:sqlite-framework`, which Room's runtime already brings in for the iOS targets. `shared/build.gradle.kts` gains the `-lsqlite3` linker option for iOS binaries.
- **Code:**
  - New in `:shared`: the shared setup and Koin module declaration (`commonMain`), and an Android and an iOS builder with their module definitions (`androidMain`, `iosMain`).
  - Edited: `SharedModules.kt` (adds the database module to `sharedModules`, updates the KDoc), `AndroidModules.kt` (Room binding removed).
- **Tests:** `TestDatabases.kt` uses the production builder; one new Android upgrade test; a new iOS DAO test suite; `KoinGraphResolutionTest` updated if needed.
- **Users:** none. Existing installs keep the same database file and their saved watchlists.
