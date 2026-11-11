# Proposal

## Why

This is phase 5c-1 of the KMP migration. After 5b (PR #37), the iOS app signs in and shows Home, but nothing on Home leads anywhere. There's no tab bar, no other screen and no way to sign out.

This change adds every screen that runs on the backends iOS already reaches: the tab bar, Search, Movie Detail with its country sheet, Watchlist, Profile with sign-out, and Top TV Shows. Each one runs on the shared ViewModel that Android uses.

5c was split in two on 2026-11-11. This change needs no new SDK and no Firebase console work. 5c-2, a separate change, adds the AI chat with poster capture and Google sign-in. Both need new setup.

## What Changes

- **The main screen becomes four tabs:** Home, Search, Watchlist and Profile, in that order, using a stock SwiftUI `TabView`.
  - Each tab has its own navigation stack and keeps its state when you switch away and back.
  - Movie Detail and Top TV Shows are pushed onto the current tab's stack with the tab bar hidden. Back returns to the tab with its state intact.
- **Home** (from 5b) gains two things:
  - tapping a card opens Movie Detail;
  - the avatar, which shows the account's initial, switches to the Profile tab.
- **The Search tab** has:
  - the "Search" title and a search field with the prompt "Search movies…";
  - the shared header label ("Popular now", "Searching…", "N results");
  - a poster grid of results, and the shared full-screen error with Retry.
  
  Typing goes to the shared ViewModel, which debounces it. Swift doesn't debounce.
- **Movie Detail** has:
  - a backdrop header that collapses into a pinned strip as you scroll, with the generated gradient when there's no backdrop;
  - the metadata line and rating;
  - the "Add to Watchlist" / "In Watchlist" toggle;
  - genre chips;
  - the "Made in" country chips;
  - "Synopsis", with the shared fallback;
  - the "Cast" row;
  - a loading state, and a full-screen error with Retry.

  Tapping a country chip opens a sheet with the country's details. It has its own loading, not-found and error-with-Retry states.
- **The Watchlist tab** has:
  - the "Watchlist" title and the "N saved" count;
  - a poster grid with a bookmark badge on each movie, which opens Movie Detail;
  - the empty state: "Your watchlist is empty" / "Movies you save will appear here.".

  The watchlist is stored in Room through the iOS driver and kept per account. This is the first time iOS uses it.
- **The Profile tab** has:
  - the "Profile" title, the initial avatar, and the name (only when the account has one) and email;
  - the rows "Top TV Shows", "Preferences", "Notifications" and "About". The last three do nothing, as on Android.
  - "Sign Out", which signs out and returns to an empty Login screen. Nothing from the signed-in session can be reached again.
- **Top TV Shows** has:
  - the "Top TV Shows" title and "The highest rated series" subtitle;
  - a poster grid that loads the next page as you near the end. The tiles can't be tapped.
  - a full-screen loading state, an error and an empty state ("No shows to display");
  - when loading another page fails, a footer reading "Couldn't load more shows." with Retry.
  
  This is iOS's first call to the BFF, with the Firebase ID token that `BffAuth` adds.
- **Copy and layout logic move to `:shared`**, so both apps use one tested version, as 5b did for the error copy:
  - the poster-grid tile model and its "year · genres" subtitle (`PosterGridCard.kt`);
  - the country sheet's rows, with "—" for any missing value (`CountryDetailsSheet.kt`);
  - the synopsis fallback;
  - the header-collapse math (`HeaderCollapse.kt`).

  Android reads them from `:shared`, so its behavior doesn't change. Their `:app` tests move to `commonTest`.
- **Swift helper:** a small companion to `ObservedViewModel` that observes a second `StateFlow`, for Home's `avatarInitial`.
- **Tests:**
  - the moved logic is tested in `commonTest`;
  - XCTest covers Top TV Shows' page-trigger rule, the one piece of logic Swift adds;
  - a manual check on the simulator covers each screen against real backends.
- **Landscape on iOS.** 5b's project allowed portrait only. This change allows both landscape orientations, which Android already supports and movie-detail's "Landscape" scenario needs (decided 2026-11-11, during the simulator check).
- **Docs:** CLAUDE.md's repo map lists the new iOS folders, and the 5b note that the other screens are "phase 5c" is updated.

**Backends touched from iOS:**
- **TMDB:** Search and Movie Detail.
- **BFF:** Top TV Shows, the first BFF call from iOS.
- **Countries GraphQL:** the country sheet, the first Apollo call from iOS.
- **Firebase Auth:** sign-out, and the ID token the BFF calls carry.

Not touched: Firebase AI. Android's calls are unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. As in 5b, the specs describe behavior without naming a platform, and Android's behavior doesn't change, so the change sets `skip_specs: true`.

On iOS, these existing requirements are the acceptance criteria:
- **`app-navigation`:**
  - "Main screen has four bottom tabs";
  - "Tabs retain their state";
  - "Home avatar opens Profile" (its first scenario);
  - "Full-screen destinations open above the tabs" (for Movie Detail and Top TV Shows);
  - "Movie Detail is reachable from every movie list";
  - "Secondary features are reached from Profile" (for Top TV Shows).
- **`authentication`:** "Sign-out returns to login and clears history".
- **`movie-browse`:** "Tapping a card opens its details".
- **`movie-search`:** every requirement.
- **`movie-detail`:** every requirement.
- **`watchlist`:** every requirement.
- **`profile`:**
  - "Profile shows the signed-in account's identity";
  - "Profile lists settings entries";
  - "Placeholder entries are inert";
  - "Profile provides sign-out";
  - "Avatar initial reflects the signed-in account".
- **`tv-shows`:** every requirement.

**Not met on iOS by this change:**
- **`app-navigation`, "Back from a tab returns to Home" and the avatar's back scenario.** Both describe Android's system back button, which iOS doesn't have. There's nothing to port.
- **`app-navigation`, the four tab-animation requirements:** icon animations, the per-tab motifs, the sliding pill and the reduced-motion handling. This change uses the stock tab bar. A later visual-parity change ports them (see Pre-production debt).
- **`profile`, "Profile offers AI movie discovery".** It comes with the chat in 5c-2.
- **`authentication`, "Continue with Google".** 5c-2.

## Out of scope

- AI chat, poster capture, and the Profile "DISCOVER" entry that opens it (5c-2).
- Google sign-in on iOS (5c-2).
- Custom tab-bar animations, the selection pill, the film-reel loader, and the app's colours and type on iOS. The screens use stock SwiftUI with the same copy and layout, as 5b's did.
- New behavior on either platform. Anything Android doesn't do (TV show detail, a search empty state, pull to refresh) stays out.
- CI for the iOS build, signing for a real device, TestFlight.

## Pre-production debt

- **The AI chat and Google sign-in aren't on iOS.** Profile has no "DISCOVER" section, and Login has no Google button. 5c-2 adds both. Until then the iOS app isn't shippable.
- **The tab bar is stock.** The tab animations and pill that `app-navigation` requires aren't ported, so the iOS tab bar doesn't meet those four requirements until a later visual-parity change.
- **A stale spec note is carried forward.** The `movie-browse` note about developer-facing copy naming `local.properties` is out of date since 5b. This change skips specs, so it stays, and the next change that edits `movie-browse` should drop it.

No new placeholder behavior: the inert Profile rows are Android's existing, noted behavior.

## Impact

- **`:shared` `commonMain` `ui/common/`:**
  - new `PosterGridLabels.kt` (`PosterGridItem` and its mappers for `Movie` and `TvShow`);
  - new `CountryDetailRows.kt`;
  - new `HeaderCollapse.kt`;
  - `MovieLabels.kt` gains the synopsis fallback.
- **`:app`:**
  - `PosterGridCard.kt`, `CountryDetailsSheet.kt`, `MovieDetailScreen.kt` and `HeaderCollapse.kt` read from `:shared`, and `HeaderCollapse.kt` is deleted;
  - `HeaderCollapseTest` and `CountryDetailRowsTest` move to `commonTest`.
- **`iosApp/iosApp/`:**
  - new `Main/` (the tab view and route types), `Search/`, `Detail/`, `Watchlist/`, `Profile/`, `TvShows/` and `Shared/PosterGrid.swift`;
  - `RootView`, `HomeView`, `MovieRail` and `ObservedViewModel.swift` change.

  No project-file change (synchronized folders), no new Swift package.
- **`iosAppTests/`:** a test for the page-trigger rule.
- **Dependencies:** none.
- **Users:**
  - Android: none.
  - iOS: a development build on the simulator that has every screen except chat and Google sign-in.
- **Setup:** none beyond 5b's. Top TV Shows needs only a signed-in Firebase user.
