# tv-shows Specification

## Purpose
The Top TV Shows screen lists the highest-rated TV series from the app's BFF service as an endlessly scrolling grid.

## Requirements

### Requirement: Screen shows the ranked list of top TV shows
The Top TV Shows screen SHALL be titled "Top TV Shows" with the subtitle "The highest rated series", and SHALL show shows in a grid in the order the BFF service ranks them, each with its poster, name, a subtitle, and "★ rating" only when the rating is non-zero.

#### Scenario: First page loads
- **WHEN** the user opens Top TV Shows
- **THEN** a loading indicator is shown, then the first page of ranked shows

### Requirement: More pages load as the user scrolls
The screen SHALL request the next page when four or fewer loaded shows remain below the last visible one, SHALL show a loading indicator at the end of the grid while it loads, and SHALL NOT issue a second request while one is in flight.

#### Scenario: Scrolling near the end
- **WHEN** the user scrolls so four or fewer loaded shows remain below the viewport
- **THEN** the next page is requested once and appended when it arrives

### Requirement: Loading stops after the last page
The screen SHALL stop requesting pages once the page reported as the last by the service has been loaded.

#### Scenario: Reaching the end
- **WHEN** the final page has been loaded and the user scrolls to the bottom
- **THEN** no further requests are made

### Requirement: Shows repeated across pages appear once
When a show already in the list appears again in a later page, it SHALL NOT be added a second time.

#### Scenario: Rank shift between requests
- **WHEN** page 2 contains a show id already loaded from page 1
- **THEN** the list contains that show only once

### Requirement: First-page failure shows a full-screen error
When the first page fails to load, the screen SHALL show a full-screen error with a Retry action that requests the first page again.

> Note: Error copy is shared with the TMDB screens, so BFF failures read as TMDB errors (e.g. "TMDB rejected the request").

#### Scenario: Offline on open
- **WHEN** the first page fails with no connectivity
- **THEN** the screen shows "No connection" with a Retry button

### Requirement: Load-more failure keeps loaded shows
When a later page fails, the screen SHALL keep the shows already loaded and show "Couldn't load more shows." with a Retry action at the end of the grid; automatic loading on scroll SHALL pause until the user retries.

#### Scenario: Page 3 fails
- **WHEN** pages 1 and 2 are loaded and page 3 fails
- **THEN** pages 1 and 2 remain visible with the retry footer below them

### Requirement: Empty result has an empty state
When the service returns no shows and there is no error, the screen SHALL show "No shows to display".

#### Scenario: Empty response
- **WHEN** the first page returns zero shows
- **THEN** "No shows to display" is shown

### Requirement: Shows are not interactive
Tapping a TV show SHALL NOT navigate anywhere.

> Note: There is no TV show detail screen and TV shows cannot be added to the watchlist.

#### Scenario: Tapping a show
- **WHEN** the user taps a show
- **THEN** the screen does not change
