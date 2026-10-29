## ADDED Requirements

### Requirement: Watchlist tiles use the shared poster tile
Each saved movie in the Watchlist grid SHALL be shown as the shared poster tile: the poster with a bookmark badge in its corner, the title below the poster, and a subtitle of the release year and up to two genres. The tile SHALL show "★ rating" beside the subtitle only when the rating is non-zero, on a single line, and a long subtitle SHALL be truncated rather than wrapping or squeezing the rating.

#### Scenario: Saved movie with a long subtitle
- **WHEN** a saved movie has the subtitle "2026 · Science Fiction · Action" and a rating of 8.5
- **THEN** its tile shows the title below the poster, the subtitle on one line, and "★ 8.5" on one line beside it

#### Scenario: Saved movie without a rating
- **WHEN** a saved movie has a rating of 0.0
- **THEN** its tile shows no rating
