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
