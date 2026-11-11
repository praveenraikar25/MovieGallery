# Tasks

No CLAUDE.md checklist covers porting screens to iOS: the ViewModels, use cases, repositories and graph entries already exist, which covers steps 1–6 of "Adding a feature screen". These tasks follow the design's Migration Plan:
1. the `:shared` moves first, while Android stays green;
2. the navigation shell and sign-out;
3. the screens;
4. tests;
5. docs and verification.

The change sets `skip_specs: true`. On Android, the existing JVM suites plus the moved tests are the evidence that nothing changed. On iOS, the acceptance criteria are the spec requirements the proposal lists. The manual simulator check (8.4) walks them, and they're backed by the shared ViewModel tests that already exist.

Work on `feature/ios-feature-screens` (already created from `kotlin_multiplatform` at 38adf86). Run `./gradlew ktlintFormat` before each commit, and commit only once the user says to. Take every Swift-facing name from the rebuilt `Shared.h`, not from the design.

## 1. Baseline

- [x] 1.1 Record the baseline test counts:
  - `./gradlew test` (`:app` and `:shared` host);
  - `./gradlew :shared:iosSimulatorArm64Test`;
  - the `iosAppTests` count from `xcodebuild … test`.

  Note them here.

  Result (2026-11-11, at 38adf86): `:app` 85, `:shared` host 193, `:shared` iOS 228, `iosAppTests` 14. All passing.

## 2. Shared copy and layout logic (Android stays green)

- [x] 2.1 Add `shared/src/commonMain/kotlin/com/raikar/moviegallery/ui/common/PosterGridLabels.kt`, moved verbatim from `:app`'s `ui/components/PosterGridCard.kt`, with their KDoc (design, decision 4):
  - `data class PosterGridItem`;
  - `Movie.toPosterGridItem()` and `TvShow.toPosterGridItem()`;
  - `internal fun gridSubtitle`.
- [x] 2.2 Add `CountryDetailRows.kt` (`countryDetailRows` and the "—" constant) and `HeaderCollapse.kt` (`collapseFraction`, `extraBottomPaddingPx`, now public) to the same package, moved verbatim. Add `val Movie.synopsisText: String` to `MovieLabels.kt`, which returns the synopsis, or "No synopsis available." when it's blank.
- [x] 2.3 Point `:app` at the shared versions and delete its own:
  - `PosterGridCard.kt` imports `PosterGridItem` and the mappers;
  - `CountryDetailsSheet.kt` imports `countryDetailRows`;
  - `MovieDetailScreen.kt` imports `collapseFraction` / `extraBottomPaddingPx` and uses `movie.synopsisText`;
  - `app/.../ui/screens/detail/HeaderCollapse.kt` is deleted.

  Verify: `./gradlew assembleDebug` succeeds, and `grep -rn "No synopsis available\|fun gridSubtitle\|fun countryDetailRows\|fun collapseFraction" app/src/main` finds nothing.
- [x] 2.4 Move `HeaderCollapseTest` and `CountryDetailRowsTest` from `app/src/test/.../ui/screens/detail/` to `shared/src/commonTest/.../ui/common/`, converted to `kotlin.test` with names that follow the Kotlin/Native naming rule. Add `PosterGridLabelsTest` (a movie and a TV show; year and genres, year only, genres only, neither → "TBA"; rated and unrated `ratingLabel`) and `synopsisText` cases (blank, whitespace-only, present) to `MovieLabelsTest`. Verify: `./gradlew :shared:testAndroidHostTest --tests "com.raikar.moviegallery.ui.common.*"` passes; `./gradlew :shared:compileKotlinIosSimulatorArm64` passes; `./gradlew :app:testDebugUnitTest` passes, with its count lower by exactly the moved tests.

  Result: `:app`'s existing `PosterGridItemTest` already covered every grid case listed here, so it moved to `commonTest` with the other two instead of a new `PosterGridLabelsTest` being written. 20 tests moved (9 header, 3 country rows, 8 grid), and 2 `synopsisText` tests were added. `:app` 85 → 65; `ui.common` 6 → 28.

  Also moved: `countryChipLabel` ("🇰🇷 South Korea", or the name alone), from `:app`'s `CountryChip.kt` to `ui/common/CountryChipLabel.kt`, with its `CountryChipLabelTest` (2 tests). It's derived copy that the iOS chip needs, and design decision 4's rule covers it; the table missed it. `:app` is now 63.

- [x] 2.5 Rebuild the framework (`./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`) and record the Swift names in `Shared.h`:
  - `toPosterGridItem`;
  - `PosterGridItem`'s properties;
  - `countryDetailRows`;
  - `collapseFraction` / `extraBottomPaddingPx`;
  - `synopsisText`;
  - `CountrySheetState`;
  - `MovieDetail`'s `summary` / `cast` / `productionCountries`;
  - `TopTvShowsUiState.isEmpty`.

  Update design decision 4's table if any of them differs.

  Result: the names match decision 4's table. `countryDetailRows(details:)` returns `[KotlinPair<NSString, NSString>]`, typed rather than untyped, so the risk note about casts no longer applies. The action selectors are `onQueryChange(newQuery:)`, `onToggleWatchlist(movie:)`, `onCountryClick(country:)` and `movieDetailViewModel(movieId:)`. `MovieDetail.metadataLabel`, `ProductionCountry.flag` and `CastMember.initial` are properties.

## 3. Navigation shell and sign-out

- [x] 3.1 Add `ObservedFlow<Value>` to `iosApp/iosApp/Shared/ObservedViewModel.swift` (design, decision 3).
- [x] 3.2 Add `iosApp/iosApp/Main/`:
  - `Route.swift`, with `enum Tab` and `enum Route: Hashable { case movie(Int32), topTvShows }`;
  - `MainTabView.swift`, with the `TabView` holding four tabs, each in its own `NavigationStack` with its own path, plus the `navigationDestination(for: Route.self)` registration (design, decision 1). Until the tab screens exist (sections 4–7), they can be placeholder views.

  Result: no placeholders were needed. Every tab view was written before the first build. `Tab` is named `MainTab`, because SwiftUI 18 has its own `Tab` type.

- [x] 3.3 Change `RootView` to take the app-wide `AppSessionViewModel` and show `MainTabView` when signed in. Pass `onSignOut`, which calls `signOut()` and sets `signedIn = false` (design, decision 2). Update `MovieGalleryApp` to pass `AppEnvironment.shared.session.viewModel`.
- [x] 3.4 Change `HomeView`:
  - drop its own `NavigationStack`;
  - take `onMovieClick` and `onProfileClick`;
  - make `MovieRail` cards tappable;
  - add a trailing toolbar avatar showing `avatarInitial` through `ObservedFlow`, which calls `onProfileClick`.

  Verify: the app builds with `xcodebuild … build`.

  Result: `ErrorStateView` moved from `Home/` to `Shared/`, since four screens use it now. The rail cards' placeholder is the generated gradient, as on Android's `PosterBox`.

## 4. Poster grid, Search and Watchlist

- [x] 4.1 Add `iosApp/iosApp/Shared/PosterGrid.swift`: `PosterGridTile` and `PosterGrid` over `PosterGridItem`, with an optional tap action and badge (design, decision 5).

  Result: Watchlist's badge is a `showsBookmark` flag rather than a generic badge view, since it's the only badge. `PosterGrid` also takes `onItemAppear(index)` for Top TV Shows' paging.

- [x] 4.2 Add `iosApp/iosApp/Search/SearchView.swift`:
  - the title "Search", and `.searchable` with the prompt "Search movies…" over a local `@State` query forwarded to `onQueryChange`;
  - `headerLabel`;
  - the grid, or `ErrorStateView` with Retry calling `retry()`;
  - taps call `onMovieClick` (design, decision 6).
- [x] 4.3 Add `iosApp/iosApp/Watchlist/WatchlistView.swift`:
  - the empty state ("Your watchlist is empty" / "Movies you save will appear here.");
  - otherwise the title "Watchlist", "N saved", and the badged grid;
  - taps call `onMovieClick` (design, decision 9).

## 5. Movie Detail and the country sheet

- [x] 5.1 Add `iosApp/iosApp/Detail/MovieDetailView.swift`:
  - the ViewModel from `graph.movieDetailViewModel(movieId:)` through `ViewModelScope`;
  - the loading, error-with-Retry and content states;
  - the tab bar hidden;
  - the transparent navigation bar with a white back chevron (design, decision 7).
- [x] 5.2 Add `Detail/CollapsingHeader.swift` and `Detail/MovieGradient.swift`:
  - the offset is read with `onGeometryChange`;
  - `fraction` and the bottom padding come from the shared `collapseFraction` / `extraBottomPaddingPx`;
  - the hero height is 340, or 220 in compact vertical size class;
  - the collapsed height is the safe-area top plus 60;
  - the title scales to 0.64 and moves from x 20 to x 60;
  - the scrim is 0.35 × fraction;
  - the gradient palette is copied from `MovieGradients.kt`.

  Result, departing from the design in two places:
  - `MovieGradient.swift` lives in `Shared/`, because the grid tiles and Home cards also use it as their placeholder.
  - The collapsed height is the safe-area top (status bar plus navigation bar), not that plus 60. On iOS the system back button sits in the navigation bar, so the strip is the bar itself, and the title centres in its 44 points. Android's 60 is its own back button plus insets.

  The back control is the system back button, which iOS 26 draws on glass, rather than a forced white chevron. 8.4 checks that it reads well over the backdrop.

- [x] 5.3 Add the detail body:
  - the metadata line and " · ★ rating";
  - the "Add to Watchlist" / "In Watchlist" button calling `onToggleWatchlist(movie:)`;
  - genre chips;
  - "Made in" with the scrolling country chips calling `onCountryClick`;
  - "Synopsis" with `synopsisText`;
  - `Detail/CastRow.swift`, with the "Cast" heading and 88-wide tiles (a 68-point initial avatar, and the name and role held to two lines).
- [x] 5.4 Add `Detail/CountryDetailsSheet.swift`, presented from local `@State` that follows `state.countrySheet != nil`. Its `onDismiss` calls `onCountrySheetDismiss()` (design, decision 8). It shows:
  - the heading (flag, name, native name);
  - loading;
  - the shared rows;
  - the error, "Couldn't load country details.", with Retry calling `retryCountry()`;
  - not found, "No details available for this country.".

## 6. Profile

- [x] 6.1 Add `iosApp/iosApp/Profile/ProfileView.swift` (design, decision 9):
  - the title "Profile";
  - the initial avatar, the name (only when non-null) and the email;
  - "Top TV Shows", which pushes `.topTvShows`;
  - inert "Preferences", "Notifications" and "About";
  - "Sign Out" in red, calling `onSignOut`.

  No "DISCOVER" section.

## 7. Top TV Shows

- [x] 7.1 Add `iosApp/iosApp/TvShows/TvShowsPaging.swift`, with `shouldLoadMore(index:count:hasError:)`: true when `count - index <= 6` and there's no error.
- [x] 7.2 Add `TvShows/TopTvShowsView.swift` (design, decision 9):
  - the title "Top TV Shows" and the subtitle "The highest rated series";
  - the loading, first-page error with Retry (`loadNextPage()`), and empty ("No shows to display") states;
  - the grid, with tiles that can't be tapped, where each tile's `onAppear` calls `loadNextPage()` when `shouldLoadMore` is true;
  - a footer with a spinner while `isLoadingMore`, or "Couldn't load more shows." with Retry when `error != nil`.
- [x] 7.3 Replace the placeholders from 3.2 with the real views. Verify: `xcodebuild … build` succeeds with no warnings in the new Swift files.

## 8. Tests, docs and verification

- [x] 8.1 Add `iosApp/iosAppTests/TvShowsPagingTests.swift`, covering inside the threshold, exactly at it, outside it, the last item, and an error present. Verify: `xcodebuild … test` passes, with the count from 1.1 plus the new tests.

  Result: 21 tests (14 + 7), all passing.

- [x] 8.2 Update CLAUDE.md:
  - the intro and repo map list the new iOS folders (`Main/`, `Search/`, `Detail/`, `Watchlist/`, `Profile/`, `TvShows/`, `Shared/PosterGrid.swift`) and say that chat and Google sign-in are phase 5c-2;
  - `ui/common/` lists the moved helpers.

  Check that the `android-architecture` skill's iOS section still holds, and update it if it doesn't.

  Result: CLAUDE.md now has the new folders, `Shared/`'s views, the local-text-field rule and the moved helpers. The skill's iOS paragraph now names `ui/common/` and the tab-per-stack navigation.
- [x] 8.3 Run `./gradlew ktlintFormat`, then:
  - `./gradlew ktlintCheck`;
  - `./gradlew test`;
  - `./gradlew :shared:iosSimulatorArm64Test`;
  - `./gradlew assembleDebug`.

  All must pass. Record the counts against 1.1: `:app` lower and `:shared` higher by the moved tests, plus the new ones.

  Result: all pass, with no failures.
  - `:app`: 85 → 63 (22 moved).
  - `:shared` host: 193 → 217.
  - `:shared` iOS: 228 → 252.

  Both `:shared` counts are up by the 22 moved tests plus 2 new `synopsisText` tests.
- [x] 8.4 Manual check on the iPhone 17 simulator against real backends (design, decision 10). Record the result here.
  - **Tabs:** four tabs in order, opening on Home. A query on Search survives a switch to Home and back, and so does a Detail pushed on Watchlist.
  - **Home:** the avatar shows the account's initial and switches to Profile; a card opens Detail.
  - **Detail** (from Home, Search and Watchlist, with the tab bar hidden):
    - the header collapses to a pinned strip and expands again, and also in landscape, and for a movie with no backdrop (gradient);
    - a short page still collapses fully;
    - the swipe back returns with state intact;
    - "Add to Watchlist" turns into "In Watchlist" and the movie appears on the Watchlist tab;
    - tapping it again removes the movie.
  - **The country sheet:** it opens with details; a country with no record shows "No details available for this country."; swiping down closes it.
  - **Watchlist:** the empty state on a fresh account. A saved movie survives a relaunch.
  - **Profile:** the name and email are right. "Preferences" does nothing. "Top TV Shows" pages as you scroll and shows no duplicate tiles.
  - **Sign out:** Login with empty fields, in sign-in mode. A relaunch opens on Login. Signing in with a second account shows its own (empty) Watchlist and its own Profile.
  Progress (2026-11-11, iPhone 17, iOS 26.5, real backends):
  - **Passed:**
    - four tabs, opening on Home;
    - the Home avatar shows "Q" and switches to Profile;
    - a Home card and a Search result each open Detail, with the tab bar hidden;
    - the header collapses to the pinned strip, the title sits beside the back button, and a short page still collapses fully;
    - the edge swipe and the back button both return with state intact;
    - "Add to Watchlist" turns into "In Watchlist", and the movie appears on the Watchlist tab with its badge and "1 saved" (Room on iOS);
    - the Germany sheet loads its details (Apollo on iOS), swipes closed and opens again;
    - Search shows "Popular now", then "12 results" for a query, and keeps both across a switch to Home and back, and across a Detail push;
    - Profile shows the initial and email with no name row; "Preferences" does nothing;
    - Top TV Shows pages well past the first page;
    - Sign Out shows an empty Login in sign-in mode, and a relaunch opens on Login.
  - **Found and fixed:**
    - The status bar was dark over the backdrop. A visible but clear bar background with the dark scheme now makes it light.
    - The collapsed title touched the back button and sat low. It now starts at 72 and centres 32 above the strip's bottom.
  - **Seen, not a bug:** the first Top TV Shows load showed "No connection". The BFF on Render's free tier was cold and took about a minute to wake, longer than the request timeout. Retry loaded it. This is the existing BFF hosting behaviour, and it applies to Android too.
  - **After signing back in** (the same account, qwerty@…):
    - both saved movies were still there after the sign-out and relaunch ("2 saved");
    - Battleship Potemkin's "Soviet Union" chip opens "No details available for this country.";
    - "Del Rey and the Sun Kings: Battleship Potemkin" (no backdrop, no year or genres) shows the gradient header and a two-line title, and its short page collapses fully over the gradient.

    The SU chip draws its flag as the letters "S U". The shared `flagEmoji` already documents that retired codes do this; it isn't new.
  - **Not checked:**
    - **A second account's Watchlist and Profile.** The sign-in was the same account again. The separation is covered by the shared ViewModel and Room tests and by Android's check; on iOS it rests on the fresh `MainTabView` after a sign-in.
  - **Landscape**, enabled at your request:
    - Both build configurations now allow portrait and both landscape orientations (`INFOPLIST_KEY_UISupportedInterfaceOrientations`). 5b had allowed portrait only.
    - Detail in landscape starts from the 220 hero and collapses to a pinned strip under the bar, with the title beside the back button.
    - One fix: the header now ignores the horizontal safe area so the backdrop runs edge to edge. The title is offset by the leading inset (and its width by the trailing one) so it stays clear of the Dynamic Island.
    - Home, Search (four grid columns), Profile and the push and back to Detail all work in landscape.
    - **Not checked in landscape:** Login. It has no scroll view, so with the keyboard up in landscape its fields may be clipped. Checking it needs a sign-out.
- [x] 8.5 Android regression: open the app on an emulator, then check that a Search grid tile's subtitle, a Detail synopsis, the header collapse and the country sheet look as they did before.

  Result (2026-11-11): you checked on Android and it looks the same as before.

## Workflow follow-up

- Archive with `/opsx:archive` once 8.4 passes and the user agrees, then commit and open a PR into `kotlin_multiplatform` when the user says to.
- Then propose 5c-2 (AI chat with poster capture, Google sign-in).
