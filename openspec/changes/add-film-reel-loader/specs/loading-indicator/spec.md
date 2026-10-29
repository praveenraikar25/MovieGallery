# Spec Delta

## Purpose
Defines the app's shared full-screen loading indicator: what it looks like, how it moves, and how it adapts to theme and accessibility settings. Screens decide when it is shown.

## ADDED Requirements

### Requirement: Full-screen loads show a film reel
Whenever a screen is waiting on its first data and shows the full-screen loading state, it SHALL show a film reel: a ring with six evenly spaced holes around a central hub, centred on the screen. The Home, Movie Detail and Top TV Shows screens SHALL all use it.

#### Scenario: Home loading
- **WHEN** the Home screen is first opened and its rails are still loading
- **THEN** a centred film reel is shown, with no circular progress spinner

#### Scenario: Movie Detail loading
- **WHEN** a movie's details are opened and have not yet arrived
- **THEN** the same film reel is shown, centred on the screen

#### Scenario: Loading finishes
- **WHEN** the data arrives or the load fails
- **THEN** the film reel disappears and the content or the error state is shown

### Requirement: The reel rotates continuously
While shown, the film reel SHALL rotate clockwise at a constant speed, completing one turn in about 1.6 seconds, without pausing or changing speed between turns.

#### Scenario: Reel keeps turning
- **WHEN** the reel has been visible for several seconds
- **THEN** it is still rotating at the same speed with no visible jump at the end of a turn

### Requirement: The reel follows the app theme
The film reel SHALL be drawn in the app's primary colour and SHALL remain clearly visible against the screen background in both the light and dark themes.

#### Scenario: Dark theme
- **WHEN** the app is in the dark theme and a loading state is shown
- **THEN** the reel is drawn in the dark theme's primary colour against the dark background

#### Scenario: Light theme
- **WHEN** the app is in the light theme and a loading state is shown
- **THEN** the reel is drawn in the light theme's primary colour against the light background

### Requirement: The reel is announced as "Loading"
The film reel SHALL be announced to screen readers as "Loading".

#### Scenario: Screen reader
- **WHEN** a screen reader reaches the loading state
- **THEN** it announces "Loading"

### Requirement: The reel stops spinning when animations are off
When the system setting that removes or disables animations is on, the film reel SHALL be shown without rotating, still drawn and still announced as "Loading".

#### Scenario: Animations disabled
- **WHEN** the system animation scale is set to off and a loading state is shown
- **THEN** the reel is visible and stationary

#### Scenario: Animations enabled
- **WHEN** the system animation scale is on and a loading state is shown
- **THEN** the reel rotates
