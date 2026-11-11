# Design

## Context

See proposal.md (Why). Here is the current state, as read from `kotlin_multiplatform` at 38adf86 (after PR #37).

- **iOS app today:**
  - `RootView` shows `LoginView` or `HomeView` from a `@State signedIn`, seeded from `AppSessionViewModel.isSignedIn`. `AppEnvironment.shared.session` owns that ViewModel for the app's lifetime.
  - `HomeView` wraps its content in its own `NavigationStack`. Card taps and the avatar do nothing.
  - Each screen gets its ViewModel from `ViewModelScope` + `ObservedViewModel`, which watches one `StateFlow`.
- **`MovieGalleryGraph`** already returns every ViewModel this change needs:
  - `searchViewModel()`;
  - `movieDetailViewModel(movieId:)`;
  - `watchlistViewModel()`;
  - `profileViewModel()`;
  - `topTvShowsViewModel()`;
  - `appSessionViewModel()`.

  No Kotlin ViewModel or graph change is needed.
- **The shared ViewModels these screens use:**
  - **`SearchViewModel`:**
    - `uiState` is `query`, `isLoading`, `results`, `error` and `headerLabel`;
    - its actions are `onQueryChange` and `retry`;
    - it debounces by 500 ms and echoes the raw query without rewriting it.
  - **`MovieDetailViewModel(movieId)`:**
    - `uiState` is `isLoading`, `movie: MovieDetail?`, `isInWatchlist`, `error` and `countrySheet: CountrySheetState?`;
    - its actions are `retry`, `onToggleWatchlist(movie)`, `onCountryClick`, `retryCountry` and `onCountrySheetDismiss`.
  - **`WatchlistViewModel`:** `uiState` is `movies` and `isEmpty`.
  - **`ProfileViewModel`:** `uiState` is `name?`, `email` and `initial`.
  - **`TopTvShowsViewModel`:**
    - `uiState` is `shows`, `page`, `totalPages`, `isLoading`, `isLoadingMore`, `error`, `isEmpty` and `canLoadMore`;
    - its one action is `loadNextPage()`, which does nothing while a fetch is in flight or after the last page.
  - **`HomeViewModel`:** `avatarInitial` is a second `StateFlow<String>`, next to `uiState` (`Shared.h`, line 3473).
- **Logic that still lives only in `:app`:**
  - **`PosterGridCard.kt`:** `PosterGridItem`, `Movie.toPosterGridItem()` / `TvShow.toPosterGridItem()`, and `gridSubtitle` ("2011 · Drama", or "TBA").
  - **`CountryDetailsSheet.kt`:** `countryDetailRows` (Capital, Continent, Languages, Currency, with "—" for a missing value).
  - **`HeaderCollapse.kt`:** `collapseFraction` and `extraBottomPaddingPx`.
  - **`MovieDetailScreen.kt`:** the synopsis fallback "No synopsis available.".

  `HeaderCollapseTest` and `CountryDetailRowsTest` in `app/src/test` cover the header math and the country rows. All of it is pure Kotlin over domain types.
- **Android layout values the iOS screens match:**
  - hero height 340 (220 in landscape);
  - grid tile minimum width 150;
  - large avatar 68;
  - collapsed strip 60 below the status bar;
  - collapsed title scale 0.64.
- **Tooling:** Xcode 26.6; the iPhone 17 simulator on iOS 26.5; deployment target iOS 17. `onGeometryChange` back-deploys to iOS 16, but `onScrollGeometryChange` needs iOS 18, so it isn't used.

## Goals / Non-Goals

**Goals:**
- Every `app-navigation`, `movie-search`, `movie-detail`, `watchlist`, `profile` and `tv-shows` requirement the proposal lists holds on the simulator with real backends.
- Swift adds no business logic and no derived copy. Anything with a branch over data comes from `:shared`. Fixed literals ("Watchlist", "Sign Out") are written in Swift exactly as Android's Compose has them, as 5b did.
- No Kotlin ViewModel, use case, repository or graph changes. If a screen seems to need one, that's a design problem to raise, not something to patch in Swift.

**Non-Goals:**
- Visual parity with Android: theme colours, type, the film-reel loader, tab animations. The screens use stock SwiftUI with Android's layout structure and copy.
- UI tests or snapshot tests of the SwiftUI screens. The ViewModels are tested in `commonTest`, and the screens are checked by hand.

## Decisions

### 1. One `TabView`, one `NavigationStack` per tab

```
RootView
 ├─ LoginView                         (signed out)
 └─ MainTabView(selection: Tab)       (signed in)
     ├─ NavigationStack(path: $homePath)       HomeView
     ├─ NavigationStack(path: $searchPath)     SearchView
     ├─ NavigationStack(path: $watchlistPath)  WatchlistView
     └─ NavigationStack(path: $profilePath)    ProfileView
        .navigationDestination(for: Route.self)
            .movie(id)    → MovieDetailView
            .topTvShows   → TopTvShowsView
```

- **Tabs:** `enum Tab { home, search, watchlist, profile }` with the labels "Home", "Search", "Watchlist" and "Profile". The SF Symbols are `house`, `magnifyingglass`, `bookmark` and `person.crop.circle`. Opening `MainTabView` selects `.home`, which meets "Landing after sign-in".
- **Kept state:** `TabView` keeps every tab's view tree alive while you're on another tab. So each tab's `ObservedViewModel`, the Search field's text and each stack's path survive a tab switch ("Tabs retain their state"). Re-selecting the current tab adds nothing to its stack.
- **Routes:** `enum Route: Hashable { case movie(Int32), topTvShows }`. Each stack registers both destinations once, in `MainTabView`, so every tab can push either one.
- **Full-screen destinations:** they push onto the current tab's stack and hide the tab bar with `.toolbar(.hidden, for: .tabBar)`. Back (the system back button or the edge swipe) pops to the tab with its state intact.
- **Movie taps:** each screen takes an `onMovieClick: (Int32) -> Void` that appends `.movie(id)` to its own path. This meets "Movie Detail is reachable from every movie list" and movie-browse "Tapping a card opens its details". Profile's "Top TV Shows" appends `.topTvShows`.
- **Home avatar:** `HomeView` takes `onProfileClick`, which sets `selection = .profile`.
- *Alternative:* one root `NavigationStack` with the `TabView` inside it, which mirrors Android's two-level `NavHost`. On iOS that puts every tab under one navigation bar and fights `TabView`'s own layout. A stack per tab is the platform's standard pattern, and it hides the tab bar the same way.
- *Alternative:* a `fullScreenCover` for Detail. It loses the edge-swipe back and slides up instead of across, unlike every other push in iOS.

### 2. Sign-out goes through the app-wide session

- `RootView` takes the app-wide `AppSessionViewModel` (`AppEnvironment.shared.session.viewModel`) alongside the graph. It passes `onSignOut` to `MainTabView`, and from there to `ProfileView`.
- `onSignOut` calls `session.signOut()` and then sets `signedIn = false`.
- `MainTabView` leaves the hierarchy, so every tab's `ObservedViewModel` deinits and clears its ViewModel, and every path goes with it. A new `LoginView` is created with a fresh `LoginViewModel`, so its fields are empty and it's in sign-in mode. Nothing leads back. That meets authentication "Sign-out returns to login and clears history".
- A later sign-in creates a new `MainTabView`, so Profile and Watchlist start from the new account ("Switching accounts", "Watchlist is kept separately per account").
- *Alternative:* observing auth state to drop back to Login. The shared session API is "read once, then call `signOut()`", which is exactly what Android's `AppNavHost` does. Following it keeps the two apps the same.

### 3. A second-flow helper for `avatarInitial`

```swift
@Observable @MainActor
final class ObservedFlow<Value: AnyObject> {
    private(set) var value: Value
    init(_ flow: any Kotlinx_coroutines_coreStateFlow)   // StateFlowWatcher, watch; deinit cancels
}
```

- It goes in `Shared/ObservedViewModel.swift`.
- `HomeContent` creates one from `model.viewModel.avatarInitial` (as `NSString`) the first time it appears, and keeps it in `@State`. Its lifetime ends with the view.
- It doesn't own the ViewModel. `ObservedViewModel` does.
- *Alternative:* teaching `ObservedViewModel` to watch several flows. That makes its generic signature harder to read for the one screen that needs it.

### 4. Copy and layout logic move to `:shared` `ui/common/`

| New file (`commonMain`) | Moved from (`:app`) | Swift sees (confirm in `Shared.h`) |
|---|---|---|
| `PosterGridLabels.kt`: `data class PosterGridItem`, `Movie.toPosterGridItem()`, `TvShow.toPosterGridItem()`, `internal gridSubtitle` | `PosterGridCard.kt` | `movie.toPosterGridItem()`, `show.toPosterGridItem()` |
| `CountryDetailRows.kt`: `countryDetailRows(details): List<Pair<String, String>>` | `CountryDetailsSheet.kt` | `CountryDetailRowsKt.countryDetailRows(details:)` returning `[KotlinPair<NSString, NSString>]` |
| `HeaderCollapse.kt`: `collapseFraction`, `extraBottomPaddingPx` (made public) | `screens/detail/HeaderCollapse.kt` | `HeaderCollapseKt.collapseFraction(...)` |
| `MovieLabels.kt` gains `val Movie.synopsisText` (the synopsis, or "No synopsis available.") | `MovieDetailScreen.kt` | `movie.synopsisText` |

- **Text unchanged:** the bodies move verbatim, so Android's text and behavior don't change. Android's composables import the shared versions and drop their own.
- **Tests:** `HeaderCollapseTest` and `CountryDetailRowsTest` move from `app/src/test` to `shared/src/commonTest/.../ui/common/` and become `kotlin.test` tests, so they also run on iOS. New `commonTest` cases pin the grid subtitle (year and genres, year only, genres only, neither → "TBA"), the unrated grid item (`ratingLabel == null`) and the synopsis fallback (blank and non-blank).
- **Names:** test names follow the Kotlin/Native naming rule (no `,` `.` `:` `;`).
- **What doesn't move:** fixed literals stay in each UI, matching exactly, as 5b's did.
- *Alternative:* re-implementing the four helpers in Swift. That would put the same rules in two places, and Android's tests would never catch the copy drifting.

### 5. The poster grid tile, shared by three screens

- **`Shared/PosterGrid.swift`** defines:
  - `PosterGridTile(item: PosterGridItem, badge:)`, which shows the 2:3 `AsyncImage` poster (or a placeholder), the title on one line, then the subtitle (truncating) with "★ rating" next to it when `ratingLabel` is set;
  - `PosterGrid`, a `LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 14)], spacing: 20)` with 20-point side padding.
- **Uses:** Search and Watchlist make tiles tappable through `onMovieClick`. Watchlist adds a bookmark badge (`bookmark.fill` on a dark circle) in the corner. Top TV Shows' tiles aren't tappable (tv-shows "Shows are not interactive").
- **Ids:** grid items are keyed by `id`, which the ViewModels already de-duplicate (tv-shows "Shows repeated across pages appear once").

### 6. Search: a `.searchable` field over local text

- **Layout:** `SearchView` sets `.navigationTitle("Search")` and `.searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search movies…")`. Under it come `state.headerLabel` and then the grid, or `ErrorStateView` with Retry calling `retry()`.
- **The text:** `query` is local `@State`, forwarded with `.onChange(of: query) { viewModel.onQueryChange(newQuery:) }`. This is the same reason and pattern as Login's fields (5b, task 7.4): a `Binding` read from `state` drops keystrokes. The ViewModel only echoes the query, so the two can't diverge.
- **Not done in Swift:** debouncing and cancellation stay in the ViewModel (movie-search "Searching starts after typing pauses", "A newer settled query supersedes an in-flight search").

### 7. Movie Detail: a scroll view under a header driven by scroll offset

- **States:**
  - `isLoading && movie == nil` → `ProgressView`;
  - `error != nil && movie == nil` → `ErrorStateView` with Retry calling `retry()`;
  - otherwise the content.
- **Scroll offset:** a `ScrollView` whose first child is a clear spacer as tall as the expanded header (340, or 220 when `verticalSizeClass == .compact`). `onGeometryChange(for: CGFloat.self)` reads the spacer's `minY` in the scroll view's coordinate space. That gives `offset = max(0, -minY)`.
- **The shared math:**
  - `fraction = collapseFraction(firstVisibleItemIndex: 0, firstVisibleItemScrollOffset: offset, expandedHeightPx: expanded, collapsedHeightPx: collapsed)`, where `collapsed` is the top safe-area inset plus 60;
  - the scroll view's bottom padding is `extraBottomPaddingPx(viewport, content, expanded - collapsed)`, measured with `onGeometryChange` on the content and the scroll view. That way a short page still collapses fully ("Short content").

  Points are passed where Android passes pixels. The functions don't depend on units.
- **The header** is an overlay pinned to the top, ignoring the top and side safe areas so it runs edge to edge in landscape, and its height interpolates from expanded to collapsed by `fraction`. The title is offset by the side insets. Inside it:
  - the backdrop `AsyncImage` keeps its expanded height, centered and clipped, or there's the gradient when the movie has no backdrop;
  - a bottom scrim, plus a black layer at `0.35 × fraction`;
  - the white title, scaled from 1 to 0.64 and moving from the bottom-left (x 20) to the strip's centre beside the back button (x 60), at most two lines.
- **The gradient:** `MovieGradient.colors(for: id)` uses Android's `(id - 1) mod n` rule over the same palette as `MovieGradients.kt`, copied as hex. It's theme, not copy.
- **Back:** the navigation bar stays, with `.toolbarBackground(.hidden, for: .navigationBar)`, an empty title and a white back chevron (`.toolbarColorScheme(.dark)`). The system back button and edge swipe keep working (movie-detail "Back returns to the previous screen"). Hiding the bar would disable the swipe.
- **The body, in Android's order:**
  1. `metadataLabel`, then " · ★ rating" in the accent colour when `hasRating`;
  2. the watchlist button ("In Watchlist" with `bookmark.fill`, or "Add to Watchlist" with `bookmark`), calling `onToggleWatchlist(movie:)` with the loaded detail;
  3. genre chips;
  4. when there are countries: "Made in" and a horizontally scrolling row of chips (flag + name), each calling `onCountryClick`;
  5. "Synopsis" and `synopsisText`;
  6. when the cast isn't empty: "Cast" and a horizontal row of 88-wide tiles, each an initial avatar (68), then the name and the role, each held to two lines.

### 8. The country sheet opens from local state

- **Presentation:** `.sheet(isPresented: $showsCountrySheet)` with `.presentationDetents([.medium, .large])`.
  - `showsCountrySheet` is local `@State`, set by `.onChange(of: state.countrySheet != nil)`.
  - Dismissing it (by swiping down) calls `onCountrySheetDismiss()` in `onDismiss`.
- **Why not bind to state directly:** a `Binding` straight to `state.countrySheet` would read the stale non-nil value for one main-queue hop after a dismiss, and could present the sheet again. That's the same hazard as decision 6.
- **The sheet, from `state.countrySheet`:**
  - **heading:** the flag at 36 points and the name (from `details` when loaded, else the chip), with the native name underneath when there is one;
  - **loading:** a `ProgressView`;
  - **details:** the shared `countryDetailRows`, each with a divider, the label on the left and the value on the right;
  - **error:** "Couldn't load country details." with Retry calling `retryCountry()`;
  - **not found:** "No details available for this country.".

  Failures stay inside the sheet. The ViewModel already keeps them out of the screen's error.

### 9. Watchlist, Profile and Top TV Shows

- **Watchlist:**
  - when `isEmpty`, a centred `bookmark` icon, "Your watchlist is empty" and "Movies you save will appear here.";
  - otherwise the "Watchlist" title, with "N saved" as a trailing toolbar item, over the badged grid.

  The list is live from Room, so it updates when Movie Detail toggles a movie (watchlist "Watchlist changes are reflected live").
- **Profile:**
  - a `ScrollView` with the title "Profile";
  - a centred 68-point initial avatar, then the name when it's non-null (the ViewModel already blanks out empty names), then the email;
  - a grouped card with "Top TV Shows" (a chevron that pushes `.topTvShows`), "Preferences", "Notifications" and "About" (chevrons, no action), and "Sign Out" in red, calling `onSignOut`.
  
  There's no "DISCOVER" section until 5c-2 (proposal, Pre-production debt).
- **Top TV Shows:**
  - **title:** `.navigationTitle("Top TV Shows")`, with "The highest rated series" as the subtitle line in a custom principal toolbar item;
  - **states:** `isLoading` → `ProgressView`; `error != nil && shows.isEmpty` → `ErrorStateView` with Retry calling `loadNextPage()`; `isEmpty` → "No shows to display"; otherwise the grid;
  - **footer:** a `ProgressView` while `isLoadingMore`, or "Couldn't load more shows." with Retry when `error != nil`;
  - **paging:** each tile's `onAppear` calls `TvShowsPaging.shouldLoadMore(index:count:hasError:)`. That's true when `count - index <= 6` and there's no error, mirroring Android's `PREFETCH_THRESHOLD`. When true, the tile calls `loadNextPage()`.
  
  Like Android, a failed page is never retried automatically, only by the footer's button (tv-shows "Load-more failure keeps loaded shows"). The ViewModel's guard makes repeated `onAppear` calls harmless.

### 10. Tests

- **`commonTest`:** the moved and new `ui/common` tests (decision 4). They run on the JVM and iOS.
- **`iosAppTests`:** `TvShowsPagingTests` for `shouldLoadMore`, covering inside the threshold, outside it, the last item, and an error present. This is Swift's only new logic.
- **Manual (simulator), task 8.x:**
  - each tab and that it keeps its state;
  - the Home avatar;
  - Detail from Home, Search and Watchlist, including the collapse, landscape, a movie without a backdrop, a short page, the watchlist toggle reflected on the Watchlist tab, and the country sheet (details, not-found, error with the Mac offline if possible);
  - Top TV Shows (paging, de-duplication, the footer error);
  - sign-out and a relaunch;
  - a different account's empty Watchlist.

## Risks / Trade-offs

- **[Risk] `TabView` rebuilds a tab's content and loses state**, for example if a tab's root view's identity changes. → Each tab's root is a stable view with no `id` churn. The manual check covers "Returning to Search" and a pushed Detail surviving a tab switch.
- **[Risk] The collapsing header jitters, because `onGeometryChange` reports a frame late.** → The header is an overlay whose height comes only from the measured offset, the same one-way data flow Android uses (layout reads the fraction, never writes it). If it still jitters, fall back to `visualEffect` on the header, which reads the same geometry in the render pass.
- **[Risk] A transparent navigation bar shows the system's scroll-edge material** when content scrolls under it on iOS 26. → `.toolbarBackground(.hidden)` applies in both states. If the material shows anyway, the pinned strip's own scrim covers it, and the manual check looks at it.
- **[Risk] Room on iOS hasn't been used from the UI yet.** `WatchlistDaoIosTest` covers the DAO on the native driver, but not the file in Application Support across relaunches. → The manual check saves a movie, relaunches, and expects it to still be there (watchlist "Watchlist survives app restarts").
- **[Risk] The first BFF and Apollo calls from iOS fail** for platform reasons: Darwin engine settings, ID-token timing. → `IosKoinGraphTest` and the real-engine network test cover resolution and errors. If the live call fails, it's a 5a bug to fix in `:shared` with a test, not a workaround in Swift.
- **[Trade-off] A stock tab bar with no animations.** → It's deliberate (proposal, Pre-production debt). Porting Compose's custom icon animations is its own piece of work, and it shouldn't hold back the screens.
- **[Trade-off] The country rows cross as `KotlinPair<NSString, NSString>`.** → Swift reads `first` and `second` as optional strings in one place. A dedicated row class would add API for one view. (Typed, confirmed in `Shared.h`, task 2.5.)

## Migration Plan

- **Branch:** `feature/ios-feature-screens`, from `kotlin_multiplatform` at 38adf86 (already created). The PR goes into `kotlin_multiplatform`.
- **Order:**
  1. The `:shared` moves (Android stays green).
  2. The navigation shell and sign-out.
  3. The poster grid, then Search, Watchlist, Detail with its sheet, Profile and Top TV Shows.
  4. Tests.
  5. Docs.
  6. The manual check.
- **Setup:** none new.
- **Rollback:** revert the PR. Android's only change is importing the moved helpers, which the moved tests and `assembleDebug` cover.
