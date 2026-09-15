# MovieGallery

Android app (package `com.raikar.moviegallery`).

## Tech Stack

Always use the latest stable Android tech stack. Concretely:

- **Kotlin & AGP**: target the latest stable Kotlin and Android Gradle Plugin versions. `gradle/libs.versions.toml` is the single source of truth for all dependency/plugin versions — add new versions there, don't hardcode them in module `build.gradle.kts` files.
- **UI**: Jetpack Compose with Material 3 for all UI. Do not add new XML layouts or View-based Activities/Fragments.
- **DI**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Module`/`@InstallIn`).
- **Networking/async**: Retrofit for network calls, Kotlin Coroutines/Flow for async data. Repositories expose main-safe suspend functions / `Flow`; ViewModels expose `StateFlow`.
- **Dependencies**: when adding any new library, use its latest stable release and add it via the version catalog.

## Code style

Kotlin formatting is enforced by ktlint (`org.jlleitschuh.gradle.ktlint`), configured via the root `.editorconfig` (`ktlint_official` style, 120-column limit).

- `./gradlew ktlintCheck` — verify; `./gradlew ktlintFormat` — auto-fix.
- **Once per clone**: run `./gradlew installGitHooks` to point git at `.githooks/`. The `pre-commit` hook then formats staged Kotlin, re-stages the fixes, and blocks the commit on anything ktlint can't auto-fix. Bypass with `SKIP_KTLINT=1 git commit` or `--no-verify`.

For layering and module structure (Clean Architecture: UI / Domain / Data, multi-module strategy), see `.claude/skills/android-architecture/SKILL.md` — keep that skill and this file in sync rather than duplicating details here.

## Repo map

All source lives under `app/src/main/java/com/raikar/moviegallery/`:

- `ui/screens/<feature>/` — one package per screen: `<Feature>Screen.kt` (Compose UI), `<Feature>ViewModel.kt` (`@HiltViewModel`, exposes `StateFlow`), `<Feature>UiState.kt`. Existing features: `home`, `search`, `detail`, `watchlist`, `profile`, `login`, `chat`, `tvshows`.
- `ui/navigation/` — `Destinations.kt` / `Routes` (route constants), `AppNavHost.kt` (`NavHost` graph), `MainScaffold.kt` (bottom nav / tab scaffold for top-level tabs).
- `ui/components/` — shared Compose widgets (cards, chips, buttons, state views) reused across screens.
- `ui/theme/` — Material 3 theme, color, type, shape, dimens.
- `domain/model/` — plain data classes shared across layers.
- `domain/repository/` — repository interfaces (`<Thing>Repository`), implemented in `data/repository/`.
- `domain/usecase/` — one class per use case, invoked from a ViewModel; wraps a repository call.
- `data/remote/` — `TmdbApi.kt` (Retrofit service), `dto/` (network DTOs), `MovieMappers.kt` (DTO → domain mapping), `SafeApiCall.kt` (network error wrapping), `ai/` (Firebase AI client).
- `data/repository/` — repository implementations (`<Thing>RepositoryImpl.kt`), or a static/in-memory impl where there's no backing API yet (e.g. `StaticTvShowRepository.kt`).
- `data/local/` — in-memory/local data sources (e.g. watchlist).
- `di/` — Hilt modules: `NetworkModule` (Retrofit/OkHttp), `RepositoryModule` (`@Binds` interface → impl), `DispatchersModule`, `AiModule`. `di/qualifier/` holds `@Qualifier` annotations.

## Adding a feature screen

Touch these in order:

1. `domain/model/` — add/extend the domain model if needed.
2. `domain/repository/<Thing>Repository.kt` — interface with suspend/`Flow` methods.
3. `data/repository/<Thing>RepositoryImpl.kt` — implementation (or a static impl if there's no API yet).
4. `di/RepositoryModule.kt` — `@Binds` the new repository interface to its impl.
5. `domain/usecase/` — one use case class wrapping the repository call the screen needs.
6. `ui/screens/<feature>/` — `<Feature>UiState.kt`, `<Feature>ViewModel.kt` (`@HiltViewModel`, injects the use case, exposes `StateFlow<UiState>`), `<Feature>Screen.kt` (Compose UI, `hiltViewModel()`).
7. `ui/navigation/Destinations.kt` — add a route constant.
8. `ui/navigation/AppNavHost.kt` — register the composable route.
9. `ui/navigation/MainScaffold.kt` — add a tab/nav entry if it's a top-level destination.
10. `app/src/test/java/.../ui/screens/<feature>/` — ViewModel test using a fake repository from `app/src/test/java/.../testutil/`.

## Adding a TMDB endpoint

1. `data/remote/dto/` — response DTO(s) matching the TMDB JSON shape.
2. `data/remote/TmdbApi.kt` — new `@GET`/`@POST` method.
3. `data/remote/MovieMappers.kt` — DTO → domain model mapping function.
4. `domain/repository/` + `data/repository/...Impl.kt` — expose the call through the repository layer (wrap with `SafeApiCall`).
5. `app/src/test/java/.../data/remote/` — add a fixture + test (see `Fixtures.kt`, `FakeTmdbApi.kt` for patterns).
