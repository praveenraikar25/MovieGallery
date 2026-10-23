# Spec Delta

## Purpose

Defines how MovieGallery presents itself outside its own screens, starting with the launcher icon shown on the home screen, in the app drawer and in themed-icon mode.

## ADDED Requirements

### Requirement: Branded launcher icon
The app SHALL display a MovieGallery launcher icon, not the default Android icon, on the home screen and in the app drawer. The icon SHALL be a terracotta tile containing a 2x2 grid of rounded poster tiles, the bottom-right tile dark with a play mark.

#### Scenario: Icon in the app drawer
- **WHEN** the app is installed on a device
- **THEN** the app drawer and home screen show the terracotta poster-grid icon next to the label "MovieGallery"

#### Scenario: Default icon never shown
- **WHEN** the app is installed on any supported Android version
- **THEN** the stock green Android icon is not shown at any launcher size or shape

### Requirement: Icon adapts to launcher shapes
The launcher icon SHALL render correctly under circle, squircle, rounded-square and other launcher masks, with the poster grid fully visible and not clipped.

#### Scenario: Circular mask
- **WHEN** the launcher masks the icon to a circle
- **THEN** all four poster tiles and the play mark remain fully visible

#### Scenario: Round icon variant
- **WHEN** a launcher requests the round icon
- **THEN** it shows the same poster-grid artwork

### Requirement: Themed monochrome icon
On devices that support themed icons, the launcher icon SHALL provide a single-colour variant that keeps the grid and play mark distinguishable when tinted by the system.

#### Scenario: Themed icons enabled
- **WHEN** the user enables themed icons in the launcher
- **THEN** the app icon is shown tinted in the system colour with the 2x2 grid and play mark still recognisable
