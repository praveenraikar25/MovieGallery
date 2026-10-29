## RENAMED Requirements

- FROM: `### Requirement: Profile shows a placeholder user identity`
- TO: `### Requirement: Profile shows the signed-in account's identity`

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Avatar initial reflects the signed-in account
The avatar on the Profile tab and the avatar in the Home header SHALL show the first letter of the account's display name, uppercased, or of its email address when there is no display name.

#### Scenario: Initial from display name
- **WHEN** the account's display name is "jane doe"
- **THEN** both avatars show "J"

#### Scenario: Initial from email
- **WHEN** the account has no display name and its email is "sam@example.com"
- **THEN** both avatars show "S"
