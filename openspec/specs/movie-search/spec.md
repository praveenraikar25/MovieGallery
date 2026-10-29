# movie-search Specification

## Purpose
The Search tab lets users find movies on TMDB by title as they type, and shows popular movies when no query has been entered.

## Requirements

### Requirement: Empty query shows popular movies
When the search query is blank (empty or whitespace only), the Search screen SHALL show TMDB's popular movies under the header "Popular now" instead of issuing a search.

#### Scenario: Opening Search
- **WHEN** the user opens the Search tab without typing
- **THEN** popular movies are shown under "Popular now"

#### Scenario: Clearing the query
- **WHEN** the user deletes all text from the search field
- **THEN** the popular movies are shown again under "Popular now"

### Requirement: Searching starts after typing pauses
The app SHALL wait until the query has been unchanged for 500 ms before requesting results, so that typing does not trigger a request per keystroke. The search field SHALL echo every keystroke immediately.

#### Scenario: Typing a title
- **WHEN** the user types "dune" quickly and then stops
- **THEN** a single search for "dune" is issued about 500 ms after the last keystroke

### Requirement: A newer settled query supersedes an in-flight search
When a new query settles (after the 500 ms pause) while a search is in flight, the in-flight search SHALL be abandoned so its results are never shown. Any edit that settles SHALL issue a new search, even when the settled text equals the previously searched query.

> Note: Cancellation waits for the debounce, so a search still in flight can complete and show its results during the 500 ms after the user edits the query.

#### Scenario: Query changes mid-request
- **WHEN** a search for "star" is in flight and the query "star wars" settles before it completes
- **THEN** the "star" search is abandoned and only results for "star wars" are shown

#### Scenario: Edit back to the same text
- **WHEN** results for "dune" are shown and the user edits it to "dunes" and back to "dune" within 500 ms
- **THEN** "dune" is searched again

### Requirement: Header reflects search progress
The header above the results SHALL read "Searching…" while a request is pending, "N results" (the number of movies returned) once a non-blank query completes, and be blank when an error is shown.

> Note: A query with zero matches shows "0 results" over an empty grid; there is no dedicated empty-results state.

#### Scenario: Results loaded
- **WHEN** a search for "inception" returns 12 movies
- **THEN** the header reads "12 results" and the movies are shown in a grid

### Requirement: Result tiles use the shared poster tile
Each movie in the Search grid SHALL be shown as the shared poster tile: the poster, the title below it, and a subtitle of the release year and up to two genres, with "★ rating" beside the subtitle only when the rating is non-zero, on a single line.

#### Scenario: Result with a rating
- **WHEN** a search result has a rating of 7.4
- **THEN** its tile shows "★ 7.4" beside the subtitle

#### Scenario: Unrated result
- **WHEN** a search result has a rating of 0.0
- **THEN** its tile shows no rating

### Requirement: Results exclude adult titles and show one page
Searches SHALL exclude adult titles and show only the first page of TMDB results.

#### Scenario: Many matches
- **WHEN** a query matches more movies than fit in one TMDB page
- **THEN** only the first page of matches is shown

### Requirement: Search failures offer retry
When a search or the popular-movies request fails, the Search screen SHALL show an error describing the failure with a Retry action that re-runs the current query.

#### Scenario: Retry after network failure
- **WHEN** a search fails with no connectivity and the user taps Retry after reconnecting
- **THEN** the current query is searched again and its results are shown

### Requirement: Tapping a result opens its details
Tapping a movie in the results SHALL open the Movie Detail screen for that movie.

#### Scenario: Open detail from search
- **WHEN** the user taps a search result
- **THEN** the Movie Detail screen for that movie is shown
