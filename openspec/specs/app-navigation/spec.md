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

### Requirement: Home avatar opens Profile
The account avatar at the top right of the Home screen SHALL switch to the Profile tab, exactly as tapping the Profile tab in the bottom bar does. The Profile tab SHALL then be shown as selected.

#### Scenario: Tapping the Home avatar
- **WHEN** the user taps the avatar at the top right of Home
- **THEN** the Profile screen is shown and the Profile tab is selected

#### Scenario: Back after opening Profile from the avatar
- **WHEN** the user opened Profile from the Home avatar and presses system back
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

### Requirement: Selecting a tab plays its icon animation
When the user selects a bottom tab that is not already selected, that tab's icon SHALL play a short animation of about one second or less. It SHALL end with the icon fully drawn in the selected colour with a soft fill.

#### Scenario: Selecting Watchlist
- **WHEN** the user taps the Watchlist tab while on Home
- **THEN** the Watchlist screen is shown
- **AND** the Watchlist icon plays its drop-and-fill animation and ends selected

#### Scenario: Re-selecting the current tab
- **WHEN** the user taps the tab that is already selected
- **THEN** the icon animation does not play again

#### Scenario: Selecting Profile from the Home avatar
- **WHEN** the user taps the avatar at the top right of Home
- **THEN** the Profile tab is selected and its icon animation plays

### Requirement: Each tab has its own icon animation
The animation SHALL differ per tab. Home builds the house and releases a puff of smoke. Search swings the magnifier in and sweeps a glint across the lens. Watchlist drops the bookmark and fills it with three sparks. Profile draws the ring, pops the head up and sends out a ripple.

#### Scenario: Selecting Home
- **WHEN** the user taps the Home tab while on Search
- **THEN** the Home icon builds the house and releases a puff of smoke

### Requirement: A selection pill follows the selected tab
The bottom tab bar SHALL show a soft accent pill behind the selected tab's icon. When the selection changes, the pill SHALL slide to the new tab rather than jump, and the new tab's label SHALL settle in as the pill arrives.

#### Scenario: Switching from Home to Profile
- **WHEN** the user taps the Profile tab while on Home
- **THEN** the pill slides across the bar to sit behind the Profile icon

#### Scenario: Tabs are never left mid-animation
- **WHEN** the user taps two different tabs in quick succession
- **THEN** the pill ends behind the last tab tapped
- **AND** every icon ends in its fully drawn resting state

### Requirement: Tab animation respects the system animation setting
When the user has turned system animations off, selecting a tab SHALL show the new tab as selected immediately, with the pill and icon in their final state and no motion.

#### Scenario: Animations disabled in system settings
- **WHEN** system animations are off and the user taps the Search tab
- **THEN** the Search tab appears selected at once with no motion
