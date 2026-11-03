# movie-detail Specification

## Purpose
The Movie Detail screen shows everything the app knows about one TMDB movie, including its cast, and lets the user add it to or remove it from their watchlist.

## Requirements

### Requirement: Detail loads one movie by id
The Movie Detail screen SHALL load the movie identified by the id it was opened with, fetching its details and credits together, and SHALL show a loading indicator until they arrive.

#### Scenario: Opening a movie
- **WHEN** the user opens a movie from any list
- **THEN** a loading indicator is shown, followed by that movie's details

### Requirement: Detail shows the movie's core information
The loaded screen SHALL show a backdrop image (or a generated gradient when none exists), the title, a metadata line, the movie's genres as chips, and a "Synopsis" section. The backdrop and title appear in the header, which collapses as the user scrolls.

> Note: TMDB's tagline is fetched but not displayed.

#### Scenario: Fully populated movie
- **WHEN** a movie with a backdrop, release date, runtime, rating, genres and overview loads
- **THEN** all of these are shown on the screen

### Requirement: Header collapses into a pinned strip on scroll
As the user scrolls the Movie Detail screen, the backdrop, title and back control SHALL collapse in step with the scroll and settle into a header that stays pinned at the top. The pinned header SHALL show a darkened, cropped strip of the backdrop (or the generated gradient when there is none), the back control and the title, in white. The rest of the content SHALL scroll behind it.

#### Scenario: Scrolling down
- **WHEN** the user scrolls down from the top of a loaded movie
- **THEN** the backdrop shrinks progressively and the title becomes smaller and moves up beside the back control
- **AND** content passes behind the header instead of pushing it off screen

#### Scenario: Fully collapsed
- **WHEN** the user has scrolled past the full height of the expanded header
- **THEN** the header stays fixed at the top as a strip with the back control and the title
- **AND** no part of the screen's content is hidden from view other than by scrolling

#### Scenario: Scrolling back to the top
- **WHEN** the user scrolls back up to the top
- **THEN** the header expands to its full size again, with the large title at the bottom of the backdrop

#### Scenario: No backdrop
- **WHEN** a movie has no backdrop image
- **THEN** the header collapses and pins in the same way, over the generated gradient

#### Scenario: Landscape
- **WHEN** the device is in landscape
- **THEN** the header collapses and pins in the same way, starting from its shorter landscape height

#### Scenario: Short content
- **WHEN** the movie's content is too short to scroll the header fully collapsed
- **THEN** the header still collapses fully, with empty space after the last item so there is enough to scroll

### Requirement: Metadata line omits missing parts
The metadata line SHALL join only the parts TMDB provided, in the order release year, runtime as "N min", and up to two genres, separated by " · ", followed by "★ rating" only when the movie has a non-zero rating.

#### Scenario: Unreleased film
- **WHEN** a movie has a release year and genres but no runtime and a 0.0 rating
- **THEN** the metadata line shows the year and genres with no runtime and no rating

### Requirement: Missing synopsis has a fallback
When the movie has no overview, the Synopsis section SHALL read "No synopsis available."

#### Scenario: Blank overview
- **WHEN** a movie with an empty overview loads
- **THEN** the Synopsis section shows "No synopsis available."

### Requirement: Detail lists up to ten cast members
The screen SHALL show up to the first ten cast members in TMDB billing order, under a "Cast" heading, as a single row that scrolls horizontally. Each member SHALL be shown as a tile with an initial avatar, the name and the character role. When there is no cast, the heading SHALL be omitted.

#### Scenario: Large cast
- **WHEN** a movie with 40 credited cast members loads
- **THEN** only the top ten billed members are shown in the row

#### Scenario: Scrolling the cast
- **WHEN** the cast row holds more tiles than fit on screen and the user swipes it sideways
- **THEN** the remaining tiles scroll into view while the rest of the screen stays where it is

#### Scenario: Long names and roles
- **WHEN** a cast member has a name or character role too long for its tile
- **THEN** the text is shortened with an ellipsis so that all tiles keep the same height

#### Scenario: No cast
- **WHEN** a movie with no cast credits loads
- **THEN** no "Cast" heading is shown

### Requirement: Detail shows the movie's production countries
The loaded screen SHALL show a "Made in" label followed by one chip per production country TMDB lists for the movie, in TMDB's order. Each chip SHALL show the country's flag and its name as TMDB gives it. The chips SHALL sit on a single row that scrolls horizontally. When TMDB lists no production countries, the label and row SHALL be omitted.

#### Scenario: Single production country
- **WHEN** a movie whose only production country is South Korea loads
- **THEN** a "Made in" row shows one chip reading "🇰🇷 South Korea"

#### Scenario: Co-production
- **WHEN** a movie produced in the United Kingdom, France and the United States, in that TMDB order, loads
- **THEN** the "Made in" row shows three chips in that order

#### Scenario: Scrolling many countries
- **WHEN** a movie lists more production countries than fit on screen and the user swipes the row sideways
- **THEN** the remaining chips scroll into view while the rest of the screen stays where it is

#### Scenario: No production countries
- **WHEN** a movie with no production countries loads
- **THEN** no "Made in" label or chips are shown

#### Scenario: Country code with no flag
- **WHEN** TMDB lists a production country whose code is not two letters
- **THEN** its chip shows the country name without a flag

### Requirement: Tapping a country chip opens its details
Tapping a production-country chip SHALL open a bottom sheet for that country, showing a loading indicator until its details arrive. The loaded sheet SHALL show the flag and name as a heading, the native name beneath it when it differs from the name, and rows labelled "Capital", "Continent", "Languages" and "Currency". A row whose value is missing SHALL show "—".

#### Scenario: Opening a country
- **WHEN** the user taps the "🇯🇵 Japan" chip
- **THEN** a sheet opens with a loading indicator, followed by Japan's flag and name, native name "日本", capital "Tokyo", continent "Asia", languages "Japanese" and currency "JPY"

#### Scenario: Several languages or currencies
- **WHEN** the country has more than one official language or currency
- **THEN** the "Languages" and "Currency" rows list them all, separated by ", "

#### Scenario: Native name equals name
- **WHEN** the country's native name is the same as its name
- **THEN** no native-name line is shown under the heading

#### Scenario: Missing capital
- **WHEN** the country has no capital on record
- **THEN** the "Capital" row shows "—"

#### Scenario: Dismissing the sheet
- **WHEN** the user swipes the sheet down or taps outside it
- **THEN** the sheet closes and the Movie Detail screen is unchanged

#### Scenario: Reopening a country
- **WHEN** the user reopens a country sheet they already loaded in this session
- **THEN** its details show without a loading indicator

### Requirement: Country details failures stay inside the sheet
When a country's details cannot be loaded, the sheet SHALL show "Couldn't load country details." with a "Retry" button that re-requests them. The failure SHALL NOT replace or alter the rest of the Movie Detail screen.

#### Scenario: Offline when opening a country
- **WHEN** the device has no connection and the user taps a country chip
- **THEN** the sheet shows "Couldn't load country details." and a "Retry" button
- **AND** the movie's details, cast and watchlist button remain visible behind the sheet

#### Scenario: Retrying
- **WHEN** the user taps "Retry" after connectivity returns
- **THEN** the sheet shows a loading indicator, followed by the country's details

### Requirement: Countries without details say so
When the countries source has no record for a production country's code, the sheet SHALL show the chip's flag and name as its heading and "No details available for this country." in place of the detail rows, with no "Retry" button.

#### Scenario: Historical country
- **WHEN** the user taps the chip for a country that no longer exists, such as "Soviet Union"
- **THEN** the sheet shows "Soviet Union" and "No details available for this country." with no "Retry" button

### Requirement: Watchlist toggle reflects membership
The screen SHALL show a button reading "Add to Watchlist" when the movie is not saved and "In Watchlist" when it is; tapping it SHALL toggle the movie's watchlist membership, and the label SHALL update immediately.

#### Scenario: Adding a movie
- **WHEN** the user taps "Add to Watchlist"
- **THEN** the button reads "In Watchlist" and the movie appears on the Watchlist tab

#### Scenario: Removing a movie
- **WHEN** the user taps "In Watchlist"
- **THEN** the button reads "Add to Watchlist" and the movie is removed from the Watchlist tab

### Requirement: Load failures offer retry
When the movie cannot be loaded, the screen SHALL show a full-screen error describing the failure with a Retry action that re-requests the movie; an unknown id SHALL show "Not found".

#### Scenario: Unknown movie id
- **WHEN** TMDB responds 404 for the requested id
- **THEN** the screen shows "Not found" with a Retry button

### Requirement: Back returns to the previous screen
The screen SHALL provide a back control that returns to the screen the user came from, and the control SHALL stay visible and tappable at every scroll position.

#### Scenario: Tapping back
- **WHEN** the user taps the back control on Movie Detail
- **THEN** the previously visible tab is shown

#### Scenario: Tapping back while scrolled
- **WHEN** the user has scrolled the screen and the header is collapsed, and taps the back control
- **THEN** the previously visible tab is shown
