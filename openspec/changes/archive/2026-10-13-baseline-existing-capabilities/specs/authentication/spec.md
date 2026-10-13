# Spec Delta

## Purpose

Controls access to the app: the user signs in on a login screen before reaching any content, and can sign out to return to it.

## ADDED Requirements

### Requirement: Sign-in requires both fields
The login screen SHALL reject a sign-in attempt when either the email or the password field is blank, showing the message "Email and password are required" and staying on the login screen.

#### Scenario: Blank email
- **WHEN** the user taps sign in with an empty email and any password
- **THEN** the screen shows "Email and password are required"
- **AND** the user remains on the login screen

#### Scenario: Blank password
- **WHEN** the user taps sign in with an email and an empty password
- **THEN** the screen shows "Email and password are required"

### Requirement: Sign-in checks a fixed credential
The app SHALL accept only the email `admin` with the password `admin`; any other non-blank combination SHALL show "Invalid email or password" and stay on the login screen.

> Note: Placeholder behavior. The credential is hardcoded in the app; there is no backend authentication, account creation, or password reset.

#### Scenario: Valid credentials
- **WHEN** the user enters email `admin` and password `admin` and taps sign in
- **THEN** the app navigates to the main tabbed screen

#### Scenario: Invalid credentials
- **WHEN** the user enters any other non-blank email/password and taps sign in
- **THEN** the screen shows "Invalid email or password"
- **AND** the user remains on the login screen

### Requirement: Editing a field clears the error
The login screen SHALL clear any displayed error message as soon as the user edits the email or password field.

#### Scenario: Correcting after an error
- **WHEN** an error message is visible and the user types in either field
- **THEN** the error message disappears

### Requirement: Successful sign-in removes login from history
After a successful sign-in, the login screen SHALL be removed from the back stack so that pressing back from the main screen does not return to it.

#### Scenario: Back after sign-in
- **WHEN** the user has signed in and presses system back on the main screen
- **THEN** the app does not show the login screen

### Requirement: Sign-in is not remembered
The app SHALL start on the login screen on every launch; a successful sign-in is not persisted.

> Note: Placeholder behavior. There is no session storage or token, so users must sign in after every process start.

#### Scenario: Relaunch after sign-in
- **WHEN** the user signed in previously and the app process is restarted
- **THEN** the app opens on the login screen

### Requirement: Sign-out returns to login and clears history
Signing out SHALL navigate to the login screen and clear the entire back stack, so no previously visited screen is reachable via back.

#### Scenario: Sign out
- **WHEN** the user taps "Sign Out" on the Profile screen
- **THEN** the app shows the login screen with empty fields
- **AND** pressing system back does not return to any in-app screen
