# Spec Delta

## ADDED Requirements

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
