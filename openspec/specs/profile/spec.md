# profile Specification

## Purpose
The Profile tab shows who is signed in and acts as the hub for secondary features, settings entries and signing out.

## Requirements

### Requirement: Profile shows the signed-in user
The Profile tab SHALL show the heading "Profile", an initial avatar, the user's display name and email address.

> Note: Placeholder behavior. The name "Alex Rivera" and email "alex.rivera@email.com" are hardcoded and do not reflect the credentials used to sign in. The Home header's avatar initial "A" is likewise hardcoded.

#### Scenario: Viewing profile
- **WHEN** the user opens the Profile tab
- **THEN** the avatar "A", the name "Alex Rivera" and the email "alex.rivera@email.com" are shown

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
