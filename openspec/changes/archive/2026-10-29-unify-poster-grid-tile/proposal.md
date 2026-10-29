# Proposal

## Why

Movie Search, Watchlist and Top TV Shows each draw their grid tile with a separate copy of the same layout
(`GridMovieCard`, `WatchlistMovieCard`, `GridTvShowCard`). A fix made in one does not reach the others: the watchlist
tile's wrapped "★ 8.5" rating was fixed, but Top TV Shows still shows the same bug. Every future tile change would have
to be repeated in three places.

## What Changes

- One shared poster tile replaces the three grid tiles. It shows the poster, the title below it, a "year · genres"
  subtitle and, when the rating is non-zero, "★ rating" kept on a single line next to the subtitle (the subtitle
  truncates instead of the rating wrapping).
- The tile is tappable only where a tap target is supplied (Search, Watchlist), and can carry an optional corner badge
  (the bookmark on Watchlist).
- Top TV Shows tiles no longer wrap the rating onto several lines.
- **Visible changes from unifying:** Watchlist tiles show the title below the poster instead of over it, and Search
  tiles now also show the "★ rating" (they showed only "year · genres").
- The home-screen rail card is unchanged.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `watchlist`: the grid tile shows the title below the poster with the bookmark badge on the poster.
- `tv-shows`: the tile's rating stays on one line.
- `movie-search`: result tiles show "★ rating" (omitted when unrated) next to the subtitle.

## Impact

- **Backends touched:** none (display only; TMDB, BFF, Firebase AI and Firebase Auth are unaffected).
- **Code:** `ui/components` (new shared tile; `GridMovieCard`, `WatchlistMovieCard` and `GridTvShowCard` removed),
  `SearchScreen`, `WatchlistScreen`, `TopTvShowsScreen`. Also deletes the stray
  `openspec/changes/archive/2026-10-29-home-avatar-opens-profile/tasks.md-E` backup file.
- **Dependencies:** none.

## Out of scope

- The Home rail card (`RailMovieCard`).
- Changing `Movie` / `TvShow` domain models or merging them.
- Changing the poster, placeholder gradient or the corner badge's appearance.
- Any change to ordering, loading, empty or error states of the three screens.

## Pre-production debt

None.
