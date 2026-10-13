# authentication Specification

## Purpose
Controls access to the app: the user signs in on a login screen before reaching any content, and can sign out to return to it.

## Requirements

### Requirement: Sign-in requires both fields
The login screen SHALL reject a sign-in or account-creation attempt when either the email or the password field is blank, showing the message "Email and password are required" and staying on the login screen.

#### Scenario: Blank email
- **WHEN** the user taps "Log In" or "Create Account" with an empty email and any password
- **THEN** the screen shows "Email and password are required"
- **AND** the user remains on the login screen

#### Scenario: Blank password
- **WHEN** the user taps "Log In" or "Create Account" with an email and an empty password
- **THEN** the screen shows "Email and password are required"

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

### Requirement: Sign-out returns to login and clears history
Signing out SHALL end the user's session, navigate to the login screen in its sign-in mode with empty fields, and clear the entire back stack, so no previously visited screen is reachable via back.

#### Scenario: Sign out
- **WHEN** the user taps "Sign Out" on the Profile screen
- **THEN** the app shows the login screen with empty fields
- **AND** pressing system back does not return to any in-app screen

#### Scenario: Session ended
- **WHEN** the user signs out and then restarts the app
- **THEN** the app opens on the login screen

### Requirement: Email and password sign-in
The login screen SHALL let a user sign in with the email and password of an existing account and, on success, navigate to the main tabbed screen. A wrong email or password SHALL show "Invalid email or password" and keep the user on the login screen.

#### Scenario: Valid account
- **WHEN** the user enters the email and password of an existing account and taps "Log In"
- **THEN** the app navigates to the main tabbed screen

#### Scenario: Wrong password
- **WHEN** the user enters an existing account's email with a wrong password and taps "Log In"
- **THEN** the screen shows "Invalid email or password"
- **AND** the user remains on the login screen

#### Scenario: Unknown account
- **WHEN** the user enters an email that has no account and taps "Log In"
- **THEN** the screen shows "Invalid email or password"

#### Scenario: Old placeholder credential
- **WHEN** the user enters email `admin` and password `admin` and taps "Log In"
- **THEN** the screen shows "Enter a valid email address"
- **AND** the user remains on the login screen

### Requirement: Email format is checked
The login screen SHALL reject an email that is not a valid email address, in both sign-in and create-account modes, showing "Enter a valid email address" without contacting the account service.

#### Scenario: Malformed email
- **WHEN** the user enters "not-an-email" and a password and taps "Log In" or "Create Account"
- **THEN** the screen shows "Enter a valid email address"
- **AND** the user remains on the login screen

### Requirement: Account creation
The login screen SHALL offer a "Create account" mode on the same screen. In that mode the primary button SHALL read "Create Account", and submitting a valid email and a password of at least 6 characters SHALL create the account, sign the user in, and navigate to the main tabbed screen.

#### Scenario: Switching to create-account mode
- **WHEN** the user taps "Don't have an account? Create account" on the login screen
- **THEN** the primary button reads "Create Account"
- **AND** the toggle reads "Already have an account? Log in"
- **AND** any error message is cleared

#### Scenario: Switching back
- **WHEN** the user taps "Already have an account? Log in"
- **THEN** the primary button reads "Log In"
- **AND** any error message is cleared

#### Scenario: Successful sign-up
- **WHEN** the user is in create-account mode, enters a new valid email and a password of at least 6 characters, and taps "Create Account"
- **THEN** the account is created and the app navigates to the main tabbed screen

#### Scenario: Password too short
- **WHEN** the user is in create-account mode and submits a password shorter than 6 characters
- **THEN** the screen shows "Password must be at least 6 characters"
- **AND** no account is created

#### Scenario: Email already registered
- **WHEN** the user is in create-account mode and submits an email that already has an account
- **THEN** the screen shows "An account with this email already exists"
- **AND** the user remains on the login screen

### Requirement: Continue with Google
The login screen SHALL let the user sign in or create an account with their Google account through "Continue with Google", and on success navigate to the main tabbed screen. If the user dismisses the Google account chooser, the screen SHALL stay on the login screen without showing an error. Any other Google sign-in failure SHALL show "Google sign-in failed. Please try again."

#### Scenario: Successful Google sign-in
- **WHEN** the user taps "Continue with Google" and picks a Google account
- **THEN** the app navigates to the main tabbed screen

#### Scenario: Cancelled Google sign-in
- **WHEN** the user taps "Continue with Google" and dismisses the account chooser
- **THEN** the user remains on the login screen
- **AND** no error message is shown

#### Scenario: Google sign-in fails
- **WHEN** the user taps "Continue with Google" and the sign-in cannot be completed
- **THEN** the screen shows "Google sign-in failed. Please try again."

### Requirement: Auth requests show progress and block repeat taps
While a sign-in, account-creation or Google sign-in request is in flight, the login screen SHALL show a progress indicator and SHALL disable the primary button, the mode toggle and "Continue with Google" so the request cannot be submitted twice.

#### Scenario: Request in flight
- **WHEN** the user taps "Log In" with valid-looking input
- **THEN** a progress indicator is shown and the login buttons are disabled until the request finishes

#### Scenario: Request finishes with an error
- **WHEN** an in-flight request fails
- **THEN** the progress indicator disappears, the buttons are enabled again, and the error message is shown

### Requirement: Auth failures show specific messages
The login screen SHALL show a message matching the failure instead of a generic one: "No internet connection. Check your network and try again." when the device cannot reach the account service, "Too many attempts. Try again later." when the account service is rate limiting, and "Something went wrong. Please try again." for any other failure. The user SHALL stay on the login screen with the entered fields preserved.

#### Scenario: Offline
- **WHEN** the user submits sign-in or account creation while the device has no connection
- **THEN** the screen shows "No internet connection. Check your network and try again."
- **AND** the email and password fields keep their values

#### Scenario: Rate limited
- **WHEN** the account service rejects a request because of too many attempts
- **THEN** the screen shows "Too many attempts. Try again later."

#### Scenario: Unexpected failure
- **WHEN** a sign-in or account-creation request fails for any other reason
- **THEN** the screen shows "Something went wrong. Please try again."

### Requirement: Sign-in is remembered
After a successful sign-in or account creation, the app SHALL keep the user signed in across app restarts until they sign out, and SHALL open on the main tabbed screen without showing the login screen.

#### Scenario: Relaunch after sign-in
- **WHEN** the user signed in previously and the app process is restarted
- **THEN** the app opens on the main tabbed screen

#### Scenario: Relaunch after sign-out
- **WHEN** the user signed out and the app process is restarted
- **THEN** the app opens on the login screen
