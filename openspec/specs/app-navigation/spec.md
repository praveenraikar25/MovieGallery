# app-navigation Specification

## Purpose
Defines how the user moves through the app: the entry point, the bottom tab bar for top-level sections, and the full-screen destinations opened from those sections.

## Requirements

### Requirement: App starts at login
The app SHALL open on the login screen when no user is signed in and on the main tabbed screen when a user is already signed in. The main tabbed screen SHALL be reachable only while signed in.

#### Scenario: Cold start
- **WHEN** the app is launched and no user is signed in
- **THEN** the login screen is shown

#### Scenario: Cold start while signed in
- **WHEN** the app is launched and a user is already signed in
- **THEN** the main tabbed screen is shown on the Home tab
- **AND** the login screen is not shown

### Requirement: Main screen has four bottom tabs
The main screen SHALL show a bottom tab bar with, in order, Home, Search, Watchlist and Profile, and SHALL open on the Home tab after sign-in. The tab for the visible section SHALL be shown as selected.

#### Scenario: Landing after sign-in
- **WHEN** the user signs in
- **THEN** the Home tab is selected and its content is shown

#### Scenario: Switching tabs
- **WHEN** the user taps the Watchlist tab
- **THEN** the Watchlist screen is shown and the Watchlist tab is selected

### Requirement: Tabs retain their state
Switching away from a tab and back SHALL restore that tab's previous state (for example, a typed search query and its results) instead of recreating it. Re-selecting the current tab SHALL NOT stack a duplicate copy of it.

#### Scenario: Returning to Search
- **WHEN** the user types a query on Search, switches to Home, then returns to Search
- **THEN** the query and its results are still shown

### Requirement: Back from a tab returns to Home
Pressing system back on any tab other than Home SHALL return to the Home tab; pressing back on Home SHALL leave the app.

#### Scenario: Back from Profile
- **WHEN** the user is on the Profile tab and presses system back
- **THEN** the Home tab is shown

### Requirement: Full-screen destinations open above the tabs
Movie Detail, AI chat and Top TV Shows SHALL open as full-screen destinations without the bottom tab bar, and their back action SHALL return to the tab they were opened from.

#### Scenario: Opening a movie from Search
- **WHEN** the user taps a movie in Search results
- **THEN** the Movie Detail screen for that movie is shown without the tab bar
- **AND** tapping back returns to Search with its results intact

### Requirement: Movie Detail is reachable from every movie list
Tapping a movie on Home, Search or Watchlist SHALL open the Movie Detail screen for that movie's id.

#### Scenario: Opening a movie from Watchlist
- **WHEN** the user taps a saved movie on the Watchlist tab
- **THEN** the Movie Detail screen for that movie is shown

### Requirement: Secondary features are reached from Profile
AI chat and Top TV Shows SHALL be reachable only from entries on the Profile tab.

> Note: These features have no tab or Home entry point; discoverability is limited to Profile.

#### Scenario: Opening Top TV Shows
- **WHEN** the user taps "Top TV Shows" on Profile
- **THEN** the Top TV Shows screen is shown
