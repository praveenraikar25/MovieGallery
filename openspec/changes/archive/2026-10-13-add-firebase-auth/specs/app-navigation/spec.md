## MODIFIED Requirements

### Requirement: App starts at login
The app SHALL open on the login screen when no user is signed in and on the main tabbed screen when a user is already signed in. The main tabbed screen SHALL be reachable only while signed in.

#### Scenario: Cold start
- **WHEN** the app is launched and no user is signed in
- **THEN** the login screen is shown

#### Scenario: Cold start while signed in
- **WHEN** the app is launched and a user is already signed in
- **THEN** the main tabbed screen is shown on the Home tab
- **AND** the login screen is not shown
