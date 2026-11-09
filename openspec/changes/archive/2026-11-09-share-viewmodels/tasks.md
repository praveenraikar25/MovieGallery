# Tasks

This change moves the presentation layer between modules. The "Adding a feature screen" checklist in CLAUDE.md covers this layer in steps 6 (UiState and ViewModel, `viewModelModule`), 8 (`AppNavHost`) and 10 (ViewModel tests with fakes), and these tasks follow that order: dependencies, the files themselves, DI, `:app` wiring, tests, then the iOS bridge and docs.

The change sets `skip_specs: true`, so every existing spec must still hold. The moved ViewModel tests are the evidence: there are 78 tests now (75 ViewModel tests and 3 email-validator tests), and there must still be 78 afterwards, with unchanged assertions, passing on both the JVM and iOS.

Baseline before starting:
- `./gradlew test`: 163 `:app` tests and 103 `:shared` host tests.
- `:shared:iosSimulatorArm64Test`: 99 tests.

Work on a new branch `feature/share-viewmodels` (design, Migration Plan). Run `./gradlew ktlintFormat` before each commit, and commit only once the user says to.

## 1. Dependencies

- [x] 1.1 In `gradle/libs.versions.toml`, add two libraries:
  - `androidx-lifecycle-viewmodel` (`androidx.lifecycle:lifecycle-viewmodel`, `version.ref = "lifecycle"`);
  - `koin-core-viewmodel` (`io.insert-koin:koin-core-viewmodel`, `version.ref = "koin"`).

  First confirm 2.11.0 and 4.2.2 are still the latest stable releases, and bump the shared version refs if not. In `shared/build.gradle.kts`, add both as `api` in `commonMain`, and add `libs.turbine` to `commonTest` (design, decision 2). Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` and the Android compile of `:shared` pass.

## 2. Move the screen state and ViewModels to `:shared`

- [x] 2.1 `git mv` the seven UiState files, `chat/ChatModels.kt` and `login/EmailValidator.kt` from `app/src/main/java/com/raikar/moviegallery/ui/screens/<feature>/` to `shared/src/commonMain/kotlin/com/raikar/moviegallery/ui/screens/<feature>/`, keeping their packages (design, decision 1). Make any declaration `:app` still uses `public`; `EmailValidator`'s `internal` must become `public` only if `:app` calls it. Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [x] 2.2 `git mv` the eight screen ViewModels and `ui/navigation/AppSessionViewModel.kt` to the same packages under `shared/src/commonMain/kotlin/`. Verify: the files are gone from `app/src/main/`, and `grep -rn "^import \(android\|java\|javax\)\." shared/src/commonMain/kotlin/com/raikar/moviegallery/ui` prints only `AiChatViewModel`'s `AtomicLong` and `MovieDetailViewModel`'s `SavedStateHandle` (fixed in 2.3 to 2.5).
- [x] 2.3 `MovieDetailViewModel`: replace `savedStateHandle: SavedStateHandle` with `movieId: Int` as the first constructor parameter, and drop the `checkNotNull(savedStateHandle["movieId"])` line (design, decision 3). Verify: it compiles, with no `SavedStateHandle` import left.
- [x] 2.4 `AiChatViewModel`: replace `AtomicLong` with `private var nextId = GREETING_ID + 1L` and `nextId++` at each of the three call sites. Add a comment saying every write happens on the main thread (design, decision 7). Verify: it compiles with no `java.*` import.
- [x] 2.5 `AppSessionViewModel`: replace `startRoute: String` with `val isSignedIn: Boolean = isSignedIn()`, rename the constructor parameter so it doesn't clash, and drop the `Routes` dependency. Keep the KDoc about resolving it outside the `NavHost`, and point it at `AppNavHost` (design, decision 6). Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes, and the `grep` from 2.2 now prints nothing.

## 3. Shared Koin module and `:app` wiring

- [x] 3.1 Move `viewModelModule` from `app/src/main/.../di/AndroidModules.kt` to `shared/src/commonMain/.../di/SharedModules.kt`, with the same nine `viewModelOf` entries and its KDoc. Add it to `sharedModules`, and note in the `sharedModules` KDoc that ViewModels are included. Remove it from `androidModules`, along with the ViewModel imports and the `viewModelOf` import there. Verify: `./gradlew :shared:testAndroidHostTest` passes, including `SharedModulesTest`.
- [x] 3.2 `AppNavHost.kt`:
  - set `startDestination = if (sessionViewModel.isSignedIn) Routes.MAIN else Routes.LOGIN`;
  - in the `Routes.DETAIL` destination, read `movieId` from the back-stack entry's arguments (`IntType`), with a `checkNotNull` since the route guarantees it;
  - call `MovieDetailScreen(viewModel = koinViewModel { parametersOf(movieId) }, onBack = …)` (design, decision 3).

  Verify: `./gradlew assembleDebug` passes.
- [x] 3.3 `KoinGraphTest`: replace `SavedStateHandle::class` with `Int::class` in `extraTypes`, update the comment ("Supplied by the running app: androidContext() and the detail route's `movieId`"), and drop the `SavedStateHandle` import. Verify: `./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.di.*"` passes.

## 4. Move the tests to `commonTest`

- [x] 4.1 Move the six fakes from `app/src/test/java/com/raikar/moviegallery/testutil/` to `shared/src/commonTest/kotlin/com/raikar/moviegallery/testutil/`, replacing any `java.*` use with common equivalents. Leave a copy of `FakeAuthRepository` in `app/src/test/.../testutil/`, with a KDoc saying it exists only for the two Robolectric watchlist tests and that the shared copy is the one to extend (design, decision 8). Verify: `./gradlew :shared:compileTestKotlinIosSimulatorArm64` passes (or the iOS test compile task `./gradlew :shared:tasks --all` reports), and `WatchlistRepositoryImplTest` and `WatchlistPersistenceTest` still compile in `:app`.
- [x] 4.2 Move the nine ViewModel test classes and `EmailValidatorTest` to the same packages under `shared/src/commonTest/kotlin/`, and convert each one:
  - `org.junit.Test` becomes `kotlin.test.Test`;
  - `org.junit.Assert.*` becomes the `kotlin.test` equivalents, keeping the expected-first order;
  - `MainDispatcherRule` becomes `@BeforeTest` `Dispatchers.setMain(dispatcher)` / `@AfterTest` `Dispatchers.resetMain()`, with the same dispatcher (`StandardTestDispatcher` in `SearchViewModelTest`, `UnconfinedTestDispatcher` elsewhere);
  - `MovieDetailViewModelTest` passes `movieId` directly instead of building a `SavedStateHandle`;
  - `AppSessionViewModelTest` asserts `isSignedIn` instead of a route;
  - the four test names containing `,` `.` `:` `;` are reworded with the same meaning.

  Verify: `grep -rn "org.junit\|SavedStateHandle" shared/src/commonTest` prints nothing, and `./gradlew :shared:testAndroidHostTest` reports 103 + 78 = 181 tests, all passing.
- [x] 4.3 Delete `app/src/test/.../testutil/MainDispatcherRule.kt` and the five fakes no `:app` test uses any more. Verify: `./gradlew :app:testDebugUnitTest` passes with 163 − 78 = 85 tests. `CountryDetailRowsTest`, `HeaderCollapseTest`, the data tests and the DI tests remain.
- [x] 4.4 Run the moved tests on iOS. Verify: `./gradlew :shared:iosSimulatorArm64Test` passes with 99 + 78 = 177 tests.

## 5. iOS bridge

- [x] 5.1 Add `shared/src/iosMain/kotlin/com/raikar/moviegallery/ui/bridge/StateFlowWatcher.kt`: `StateFlowWatcher<T : Any>(flow: StateFlow<T>)` with `val value: T` and `fun watch(onEach: (T) -> Unit): WatchHandle`, collecting on a `Dispatchers.Main` scope. `WatchHandle.cancel()` stops the collection (design, decision 5). The KDoc shows the intended Swift use: `ObservableObject` publishes `onEach`, and `deinit` calls `cancel()`. Verify: `./gradlew :shared:compileKotlinIosSimulatorArm64` passes.
- [x] 5.2 Add `shared/src/iosMain/.../ui/bridge/ViewModelOwner.kt`: `ViewModelOwner<VM : ViewModel>(create: () -> VM)` holding a private `ViewModelStore`, getting the ViewModel through `ViewModelProvider.create(store, viewModelFactory { initializer { create() } })`, with `val viewModel: VM` and `fun clear()` calling `store.clear()`. The KDoc says Swift calls `clear()` from `deinit`, and that phase 5 passes a Koin lookup as `create`. Verify: it compiles, as in 5.1.
- [x] 5.3 Add `shared/src/iosTest/.../ui/bridge/ViewModelBridgeTest.kt` (`kotlin.test`, `Dispatchers.setMain(UnconfinedTestDispatcher())`). Use `WatchlistViewModel` over `FakeWatchlistRepository` (its `stateIn` state exercises every case below), and cover:
  - a watcher delivers the current state first, then each change;
  - after `cancel()` no further values arrive;
  - `ViewModelOwner` returns the same instance on repeated reads;
  - after `clear()`, the ViewModel's `viewModelScope` is cancelled (for example, a watcher on its state receives nothing more when the fake emits).

  Names contain none of `,` `.` `:` `;`. Verify: `./gradlew :shared:iosSimulatorArm64Test` passes with 177 + the new bridge tests.

## 6. Docs

- [x] 6.1 Update `CLAUDE.md`:
  - **Repo map:** `:shared` holds the ViewModels, UiState files and the iOS bridge (`ui/bridge/`, `iosMain`); `:app` holds the Compose screens, `ui/components`, `ui/theme` and navigation.
  - **Per-package map:** `ui/screens/<feature>/` is split across the modules (screen in `:app`, ViewModel and UiState in `:shared`).
  - **"Adding a feature screen":** step 6 says the UiState and ViewModel go in `:shared` `commonMain`, with `viewModelOf` in `:shared`'s `viewModelModule`; navigation arguments are passed as constructor parameters through `parametersOf`, never `SavedStateHandle`. Step 10 says ViewModel tests go in `shared/src/commonTest/` with fakes from `shared/src/commonTest/.../testutil/`.
  - **`di/` section:** `viewModelModule` is in `sharedModules`, not `:app`.
  - **Commands:** the single-test example uses a `:shared` ViewModel test.

  Verify: `grep -n "viewModelModule\|MainDispatcherRule\|app/src/test/java/.../testutil" CLAUDE.md` shows no stale `:app` placement.
- [x] 6.2 Update `.claude/skills/android-architecture/SKILL.md`:
  - section 2, "Where definitions live": `viewModelModule` is in `:shared`;
  - section 2, "ViewModels": `koinViewModel()` on Android, `parametersOf` for navigation arguments, and `ViewModelOwner` + `StateFlowWatcher` on iOS;
  - section 3, `:shared` and `:app` bullets: where ViewModels live.

  Verify: the skill and CLAUDE.md describe the same layout, with no detail contradicting the other.
- [x] 6.3 Update `openspec/config.yaml` `rules.tasks`: ViewModel tests use fakes from `shared/src/commonTest/.../testutil`. Verify: no rule still points ViewModel tests at `app/src/test/.../testutil`.

## 7. Verification

- [x] 7.1 Run `./gradlew ktlintCheck`. Verify: it passes.
- [x] 7.2 Run `./gradlew test`. Verify: everything passes, with 85 `:app` tests and 181 `:shared` host tests. The total of 266 equals the baseline.
- [x] 7.3 Run `./gradlew :shared:iosSimulatorArm64Test`. Verify: it passes, with 177 tests plus the bridge tests.
- [x] 7.4 Run `./gradlew assembleDebug`. Verify: the build succeeds.
- [x] 7.5 Device check on the phone, installing over the current build (design, Migration Plan):
  - sign in;
  - Home loads and the avatar initial shows;
  - search debounces and shows results;
  - open movie A, go back, open movie B: each shows its own detail; go back to Home;
  - toggle the watchlist on a detail screen and see it on the Watchlist tab;
  - open a production-country sheet;
  - page through TV shows;
  - send a chat message;
  - sign out, which lands on Login;
  - relaunch, which starts on Login while signed out and on Main while signed in.

  Verify: report each step's result to the user. This is a manual check, so report it rather than claim it.

## Workflow follow-up

- Open the PR from `feature/share-viewmodels` into `kotlin_multiplatform`, then archive the change once it is merged and `./gradlew test` passes.
