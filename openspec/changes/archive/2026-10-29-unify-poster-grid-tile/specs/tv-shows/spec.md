## ADDED Requirements

### Requirement: TV show tiles use the shared poster tile
Each show in the Top TV Shows grid SHALL be shown as the shared poster tile: the poster, the show's name below it, and a subtitle of the first-air year and up to two genres. "★ rating" SHALL be shown beside the subtitle only when the rating is non-zero, on a single line, and a long subtitle SHALL be truncated rather than wrapping or squeezing the rating. Show tiles are not tappable.

#### Scenario: Show with a long subtitle
- **WHEN** a show has a long subtitle and a rating of 8.5
- **THEN** its tile shows the subtitle truncated on one line and "★ 8.5" on one line beside it

#### Scenario: Show without a rating
- **WHEN** a show has a rating of 0.0
- **THEN** its tile shows no rating
