# Spec Delta

## Purpose

The watchlist lets users save movies they want to watch and see them together on the Watchlist tab without reloading them from the network.

## ADDED Requirements

### Requirement: Movies are added and removed by toggling
Toggling a movie that is not in the watchlist SHALL add it, and toggling a movie already in the watchlist (matched by movie id) SHALL remove it. A movie SHALL appear in the watchlist at most once.

#### Scenario: Toggle twice
- **WHEN** the user toggles the same movie on and then off from Movie Detail
- **THEN** the movie is not in the watchlist

### Requirement: Watchlist changes are reflected live
Every screen showing watchlist state SHALL update immediately when the watchlist changes, without a reload.

#### Scenario: Saved from detail, seen on tab
- **WHEN** the user adds a movie on Movie Detail and returns to the Watchlist tab
- **THEN** the movie is already listed

### Requirement: Watchlist tab lists saved movies in the order added
The Watchlist tab SHALL show the heading "Watchlist", a count reading "N saved", and the saved movies in a grid in the order they were added (oldest first).

#### Scenario: Two saved movies
- **WHEN** the user saves movie A and then movie B
- **THEN** the Watchlist tab shows "2 saved" with A before B

### Requirement: Watchlist renders without the network
The Watchlist tab SHALL render saved movies from the information captured when they were saved, without making network requests.

#### Scenario: Offline
- **WHEN** the device is offline and the user opens the Watchlist tab
- **THEN** previously saved movies are still shown

### Requirement: Empty watchlist has an empty state
When no movies are saved, the Watchlist tab SHALL show "Your watchlist is empty" with the hint "Movies you save will appear here."

#### Scenario: First open
- **WHEN** the user opens the Watchlist tab before saving anything
- **THEN** the empty state is shown

### Requirement: Watchlist lasts only for the app process
The watchlist SHALL persist for the lifetime of the app process, across screens and sign-out, and SHALL be empty after the process restarts.

> Note: Placeholder behavior. The watchlist is held in memory only; it is not saved to disk or synced to an account, and it is shared by whoever signs in during the same process.

#### Scenario: Process restart
- **WHEN** the user saves movies and the app process is killed and relaunched
- **THEN** the watchlist is empty

#### Scenario: Sign out and back in
- **WHEN** the user saves a movie, signs out, and signs in again without the process restarting
- **THEN** the saved movie is still in the watchlist

### Requirement: Tapping a saved movie opens its details
Tapping a movie on the Watchlist tab SHALL open its Movie Detail screen.

#### Scenario: Open saved movie
- **WHEN** the user taps a saved movie
- **THEN** the Movie Detail screen for that movie is shown with "In Watchlist"
