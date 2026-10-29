## ADDED Requirements

### Requirement: Result tiles use the shared poster tile
Each movie in the Search grid SHALL be shown as the shared poster tile: the poster, the title below it, and a subtitle of the release year and up to two genres, with "★ rating" beside the subtitle only when the rating is non-zero, on a single line.

#### Scenario: Result with a rating
- **WHEN** a search result has a rating of 7.4
- **THEN** its tile shows "★ 7.4" beside the subtitle

#### Scenario: Unrated result
- **WHEN** a search result has a rating of 0.0
- **THEN** its tile shows no rating
