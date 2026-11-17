# agent-actions Specification

## Purpose
On Android 16 QPR2 (API 36.1) and later, MovieGallery exposes a small set of tasks to on-device AI agents and assistants, which run them without opening the app's UI: search movies, list the watchlist, add a movie to the watchlist, and open a movie's details. Each action is a single, non-destructive task the app's UI already offers. Results come from TMDB and from the signed-in account's watchlist.

## Requirements

### Requirement: Agents can discover the app's actions
On API 36.1 and later, the app SHALL register exactly four agent actions, `searchMovies`, `getWatchlist`, `addToWatchlist` and `openMovieDetails`, each enabled by default and described in plain language with its inputs and result. Earlier versions SHALL register none, with the app unchanged.

> Note: Gemini calls only apps in Google's private preview, so today the actions are reachable through `adb` and test agents, not Gemini. API 36.0 doesn't index them.

#### Scenario: Listing the app's actions
- **WHEN** an agent or developer lists MovieGallery's actions on an API 37 device
- **THEN** exactly `searchMovies`, `getWatchlist`, `addToWatchlist` and `openMovieDetails` are listed, each enabled and described

#### Scenario: Older Android
- **WHEN** the app runs on Android 12
- **THEN** no actions are registered and the app works as before

### Requirement: Agents read an app description
The app SHALL publish an app description for agents stating that it is a movie discovery app backed by TMDB, that movies are identified by their TMDB movie id (which `searchMovies` returns), that adding to the watchlist is idempotent, and that the watchlist needs a signed-in account. Users SHALL see the summary "Search movies, see and add to your watchlist, and open a movie's details." where Android lists what agents can do in the app.

#### Scenario: Reading the description
- **WHEN** an agent reads MovieGallery's app description
- **THEN** it learns that `searchMovies` supplies the movie id that `addToWatchlist` and `openMovieDetails` take

### Requirement: Movie results describe each movie
Every movie an action returns SHALL carry its TMDB id, title, release year (absent when TMDB has no release date), average rating from 0 to 10 (0 when unrated), genres and synopsis (empty when TMDB has none). Results SHALL NOT include poster or backdrop images.

#### Scenario: Unreleased movie
- **WHEN** a returned movie has no release date or ratings yet
- **THEN** its release year is absent and its rating is 0

### Requirement: Search movies by title
`searchMovies` SHALL take a `query` and return up to 10 TMDB movies matching the title, best match first. Leading and trailing spaces in the query SHALL be ignored. A blank query SHALL return currently popular movies instead. A query that matches nothing SHALL return an empty list, not an error.

#### Scenario: Matching title
- **WHEN** an agent calls `searchMovies` with the query "dune"
- **THEN** it receives up to 10 movies including "Dune" (2021, id 438631) and "Dune: Part Two" (2024, id 693134), each with rating, genres and synopsis

#### Scenario: Blank query
- **WHEN** an agent calls `searchMovies` with an empty or whitespace-only query
- **THEN** it receives up to 10 currently popular movies

#### Scenario: No match
- **WHEN** an agent calls `searchMovies` with a title TMDB doesn't know
- **THEN** it receives an empty list

### Requirement: List the signed-in account's watchlist
`getWatchlist` SHALL return the movies on the signed-in account's watchlist, oldest added first. It SHALL return exactly what the Watchlist tab shows for that account, and an empty list when the watchlist is empty. It SHALL fail with a permission-required error reading "The user must sign in to MovieGallery to use the watchlist." when no one is signed in.

#### Scenario: Saved movies
- **WHEN** the signed-in user has saved "Dune: Part Two" and an agent calls `getWatchlist`
- **THEN** it receives "Dune: Part Two", matching the Watchlist tab

#### Scenario: Empty watchlist
- **WHEN** the signed-in user has saved nothing and an agent calls `getWatchlist`
- **THEN** it receives an empty list

#### Scenario: Signed out
- **WHEN** no one is signed in and an agent calls `getWatchlist`
- **THEN** it receives the permission-required error "The user must sign in to MovieGallery to use the watchlist."

### Requirement: Add a movie to the watchlist without removing anything
`addToWatchlist` SHALL take a TMDB `movieId`, add that movie to the signed-in account's watchlist, and return the movie and whether it was already saved (`wasAlreadyInWatchlist`). An already-saved movie SHALL leave the watchlist unchanged. It SHALL never remove a movie. Open screens SHALL reflect the change live, as for a change made in the app. When no one is signed in it SHALL fail with the same error as `getWatchlist`.

#### Scenario: New movie
- **WHEN** the signed-in user hasn't saved "Dune: Part Two" and an agent calls `addToWatchlist` with movie id 693134
- **THEN** the movie is added, the result reports `wasAlreadyInWatchlist` as false, and the Watchlist tab lists it with "1 saved"

#### Scenario: Movie already saved
- **WHEN** an agent calls `addToWatchlist` again with movie id 693134
- **THEN** the watchlist is unchanged, the movie is still saved, and the result reports `wasAlreadyInWatchlist` as true

#### Scenario: Signed out
- **WHEN** no one is signed in and an agent calls `addToWatchlist`
- **THEN** it receives the permission-required error "The user must sign in to MovieGallery to use the watchlist." and nothing is saved

### Requirement: Open a movie's details screen
`openMovieDetails` SHALL take a TMDB `movieId`, confirm the movie exists, and return an intent that opens MovieGallery's Movie Detail screen for that movie. The intent SHALL lead only to that screen. Opening it SHALL work whether the app was closed or already open, pushing Movie Detail over whatever was showing, so Back returns there. It SHALL also work when no one is signed in, in which case Back returns to the login screen.

#### Scenario: App closed
- **WHEN** the app isn't running and the intent returned for movie id 693134 is sent
- **THEN** the app opens on Movie Detail for "Dune: Part Two"

#### Scenario: App already open
- **WHEN** the app is showing another screen and the intent returned for movie id 27205 is sent
- **THEN** Movie Detail for "Inception" opens on top of it

### Requirement: Bad movie ids are rejected with typed errors
An action taking a `movieId` SHALL fail with an invalid-argument error, "movieId must be a positive TMDB movie ID.", for an id of 0 or less. It SHALL fail with a not-found error, "No movie has TMDB ID <id>.", for an id TMDB doesn't know. No failed action SHALL change the watchlist.

#### Scenario: Invalid id
- **WHEN** an agent calls `openMovieDetails` with movie id -5
- **THEN** it receives the invalid-argument error "movieId must be a positive TMDB movie ID."

#### Scenario: Unknown id
- **WHEN** an agent calls `openMovieDetails` or `addToWatchlist` with movie id 999999999
- **THEN** it receives the not-found error "No movie has TMDB ID 999999999." and nothing is saved

### Requirement: TMDB and unexpected failures reach the agent as app errors
Every action SHALL fail only with a typed error and a plain-language message. TMDB failures SHALL be app errors:
- TMDB unreachable: "Couldn't reach TMDB. The device may be offline."
- credentials rejected: "TMDB rejected MovieGallery's credentials."
- other HTTP failures: "TMDB returned HTTP <code>."
- unreadable responses: "MovieGallery couldn't read TMDB's response."

Anything else SHALL be reported as "MovieGallery failed unexpectedly: <error type>."

#### Scenario: Offline
- **WHEN** the device has no connection and an agent calls `searchMovies`
- **THEN** it receives the app error "Couldn't reach TMDB. The device may be offline."

> Note: Untested so far: the offline and HTTP-failure messages were never triggered, and no automated tests cover these actions.
