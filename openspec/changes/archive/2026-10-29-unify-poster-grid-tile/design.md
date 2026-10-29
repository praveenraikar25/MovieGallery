## Context

`GridMovieCard` (Search), `WatchlistMovieCard` (Watchlist) and `GridTvShowCard` (Top TV Shows) are three near-identical
Composables in `ui/components`. All wrap `PosterBox` (2:3 poster), a title and a "year · genres" subtitle; they differ
only in tap handling, an optional bookmark badge, and where the title sits (Watchlist overlays it on the poster).
`Movie` and `TvShow` expose the same display helpers (`genreLabel`, `ratingLabel`, `hasRating`, year). The rating wrap
bug exists in `GridTvShowCard` because only `WatchlistMovieCard` was fixed. No backend is involved.

## Goals / Non-Goals

**Goals:** one tile implementation for all three grids; the one-line rating behaviour fixed once for all.

**Non-Goals:** the rail card; the domain models; changing screens' loading/empty/error states.

## Decisions

- **One `PosterGridCard` Composable in `ui/components`** taking plain display values (id, title, poster URL, subtitle,
  optional rating label, optional `onClick`, optional badge slot passed through to `PosterBox`). It knows nothing about
  `Movie` or `TvShow`.
- **Small mappers at the call sites** (or as `Movie.toGridCard...` helpers beside the tile) build the subtitle and
  rating label, reusing the existing `subtitleWithYear` logic so "TBA" fallback is unchanged. The domain layer stays
  free of UI types.
- **Row layout:** subtitle gets `weight(1f)` with ellipsis; rating is `maxLines = 1`, `softWrap = false`, with an 8dp gap
  and vertical centering (same fix already shipped for the watchlist tile).
- **Title below the poster everywhere;** the Watchlist overlay-title style is dropped. If `PosterBox.overlayTitle`
  has no remaining caller it can stay for the rail card or be removed.
- **Search gains the rating** as a consequence of using the one tile. This is a deliberate behaviour change, recorded
  in the movie-search delta.
- **Clickable only when `onClick` is given,** so Top TV Shows tiles stay non-interactive.

## Risks / Trade-offs

- **Visible change on two screens** (Watchlist title position, Search rating) rather than a pure refactor; called out
  in the proposal and specs.
- **Test coverage:** the tile is a Composable and the project has no Compose UI tests, so scenarios cannot be JVM
  tests. If the mappers (subtitle and rating label) are extracted as plain functions, they get JVM unit tests covering
  the year/genre/"TBA" and zero-rating cases; the layout itself is verified manually. Flagged for the reviewer.
