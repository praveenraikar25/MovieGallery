# profile Specification

## Purpose
The Profile tab shows the signed-in account's identity and acts as the hub for secondary features, settings entries and signing out.

## Requirements

### Requirement: Profile shows the signed-in account's identity
The Profile tab SHALL show the heading "Profile", an initial avatar, and the signed-in account's email address. It SHALL also show the account's display name above the email when the account has one, and SHALL omit the name row entirely when it does not.

#### Scenario: Viewing profile
- **WHEN** a user signed in with an account whose display name is "Jane Doe" and email is "jane@example.com" opens the Profile tab
- **THEN** the name "Jane Doe" and the email "jane@example.com" are shown

#### Scenario: Viewing profile of an account without a display name
- **WHEN** a user signed in with an email/password account "jane@example.com" that has no display name opens the Profile tab
- **THEN** the email "jane@example.com" is shown and no name row is shown

#### Scenario: Blank display name is treated as missing
- **WHEN** the signed-in account's display name is empty or only whitespace
- **THEN** no name row is shown

#### Scenario: Switching accounts
- **WHEN** the user signs out and signs in with a different account
- **THEN** the Profile tab shows the new account's details, never the previous account's

### Requirement: Profile offers AI movie discovery
The Profile tab SHALL show a "DISCOVER" section with a "Find Movies with AI" entry, described as "Chat with an assistant for personalized picks", that opens the AI chat.

#### Scenario: Opening AI chat
- **WHEN** the user taps "Find Movies with AI"
- **THEN** the AI Movie Search chat opens

### Requirement: Profile lists settings entries
The Profile tab SHALL list, in order, "Top TV Shows", "Preferences", "Notifications" and "About". "Top TV Shows" SHALL open the Top TV Shows screen.

#### Scenario: Opening Top TV Shows
- **WHEN** the user taps "Top TV Shows"
- **THEN** the Top TV Shows screen opens

### Requirement: Placeholder entries are inert
Tapping "Preferences", "Notifications" or "About" SHALL do nothing.

> Note: Placeholder behavior. These rows have no destination yet.

#### Scenario: Tapping Preferences
- **WHEN** the user taps "Preferences"
- **THEN** the screen does not change

### Requirement: Profile provides sign-out
The Profile tab SHALL show a "Sign Out" action that signs the user out and returns to the login screen.

#### Scenario: Signing out
- **WHEN** the user taps "Sign Out"
- **THEN** the login screen is shown

### Requirement: Avatar initial reflects the signed-in account
The avatar on the Profile tab and the avatar in the Home header SHALL show the first letter of the account's display name, uppercased, or of its email address when there is no display name.

#### Scenario: Initial from display name
- **WHEN** the account's display name is "jane doe"
- **THEN** both avatars show "J"

#### Scenario: Initial from email
- **WHEN** the account has no display name and its email is "sam@example.com"
- **THEN** both avatars show "S"
