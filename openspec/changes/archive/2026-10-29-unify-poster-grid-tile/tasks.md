## 1. Shared tile

- [x] 1.1 Add `ui/components/PosterGridCard` (poster via `PosterBox`, title below, subtitle weighted with ellipsis, rating on one line, optional `onClick`, optional badge slot)
- [x] 1.2 Add plain mapper functions that build the subtitle ("year · genres", "TBA" fallback) and rating label (null when unrated) for `Movie` and `TvShow`

## 2. Adopt the tile

- [x] 2.1 `SearchScreen`: replace `GridMovieCard` with the shared tile
- [x] 2.2 `WatchlistScreen`: replace `WatchlistMovieCard` with the shared tile, passing the bookmark badge
- [x] 2.3 `TopTvShowsScreen`: replace `GridTvShowCard` with the shared tile (no click)
- [x] 2.4 Remove `GridMovieCard`, `WatchlistMovieCard` and `GridTvShowCard`; drop `PosterBox.overlayTitle` if nothing uses it any more
- [x] 2.5 Delete the stray `openspec/changes/archive/2026-10-29-home-avatar-opens-profile/tasks.md-E`

## 3. Tests

- [x] 3.1 JVM tests for the mappers: year and genres, missing year ("TBA"), no genres, zero rating omitted, rating formatted to one decimal

## 4. Verification

- [x] 4.1 Run `./gradlew ktlintFormat` then `./gradlew ktlintCheck`
- [x] 4.2 Run `./gradlew test`
- [x] 4.3 Run `./gradlew assembleDebug`
- [x] 4.4 Manually verify on a device that Search, Watchlist and Top TV Shows tiles keep the rating on one line with a long subtitle and that Search and Watchlist taps still open details
