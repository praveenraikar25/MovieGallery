# Spec Delta

## Purpose

The Home tab lets users discover movies from TMDB through curated, horizontally scrolling rails, and open any movie's details.

## ADDED Requirements

### Requirement: Home shows four movie rails in a fixed order
The Home screen SHALL show up to four horizontally scrolling rails titled, in order, "Trending Now" (TMDB popular), "Now Playing", "Top Rated" and "Coming Soon" (TMDB upcoming), each listing the first page of that TMDB list.

#### Scenario: All rails load
- **WHEN** the Home screen loads and all four lists return movies
- **THEN** the four rails are shown in the order Trending Now, Now Playing, Top Rated, Coming Soon

### Requirement: Rails load together behind one loading state
The Home screen SHALL show a single loading indicator until all four rail requests have finished, then show the rails.

#### Scenario: Initial load
- **WHEN** the Home screen is first opened
- **THEN** a loading indicator is shown until every rail request completes

### Requirement: A failed or empty rail is hidden
A rail whose request fails or returns no movies SHALL be omitted, and the remaining rails SHALL still be shown in their usual relative order.

#### Scenario: One endpoint fails
- **WHEN** the "Now Playing" request fails and the other three succeed
- **THEN** Home shows Trending Now, Top Rated and Coming Soon with no error state

### Requirement: Home shows an error only when every rail is unavailable
When no rail has any movies, the Home screen SHALL show a full-screen error describing the first failure encountered, with a Retry action that reloads all rails.

#### Scenario: Offline
- **WHEN** all four requests fail with no connectivity
- **THEN** Home shows "No connection" with a Retry button

#### Scenario: Retry
- **WHEN** the user taps Retry on the Home error
- **THEN** the loading indicator is shown and all four rails are requested again

### Requirement: Missing token is called out
When TMDB rejects requests as unauthorized, the error SHALL tell the user the TMDB token is missing or invalid.

> Note: Developer-facing copy that names `local.properties`; acceptable for a demo build, not for end users.

#### Scenario: Unauthorized
- **WHEN** every rail fails with HTTP 401
- **THEN** Home shows "TMDB rejected the request" with guidance to check the TMDB token

### Requirement: Movie cards summarize a movie
Each rail card SHALL show the movie's poster (or a placeholder), title, and a subtitle of its rating and up to two genres, omitting the rating for unrated (0.0) films and falling back to the year, or "TBA" when no release date exists.

#### Scenario: Unreleased film
- **WHEN** a card is shown for a film with a 0.0 rating, no genres and no release date
- **THEN** its subtitle reads "TBA"

### Requirement: Tapping a card opens its details
Tapping a movie card on Home SHALL open the Movie Detail screen for that movie.

#### Scenario: Open detail
- **WHEN** the user taps a card in the "Top Rated" rail
- **THEN** the Movie Detail screen for that movie is shown

### Requirement: Home is not paginated or refreshed in place
Each rail SHALL show only the first page of its list, and Home SHALL NOT refresh automatically or via pull-to-refresh.

> Note: Content is fetched once per Home screen instance; there is no caching across launches.

#### Scenario: Returning to Home
- **WHEN** the user switches to another tab and back to Home
- **THEN** the previously loaded rails are shown without a new loading state
