## ADDED Requirements

### Requirement: Home avatar opens Profile
The account avatar at the top right of the Home screen SHALL switch to the Profile tab, exactly as tapping the Profile tab in the bottom bar does. The Profile tab SHALL then be shown as selected.

#### Scenario: Tapping the Home avatar
- **WHEN** the user taps the avatar at the top right of Home
- **THEN** the Profile screen is shown and the Profile tab is selected

#### Scenario: Back after opening Profile from the avatar
- **WHEN** the user opened Profile from the Home avatar and presses system back
- **THEN** the Home tab is shown
