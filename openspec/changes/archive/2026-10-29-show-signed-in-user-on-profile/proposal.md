# Proposal

## Why

The Profile tab and the Home header show a hardcoded identity ("Alex Rivera", "alex.rivera@email.com", avatar "A")
regardless of who signed in. Now that Firebase Auth is in place, the real account is available and the placeholder is
misleading.

## What Changes

- Profile shows the signed-in account's email, and its display name when the account has one.
- When the account has no display name (typical for email/password accounts), the name row is hidden; no name is
  derived from the email and no name field is added to sign-up.
- The avatar initial on Profile and in the Home header comes from the account (display name, else email) instead of a
  hardcoded "A".
- The signed-in account's email and display name are exposed from the auth layer as an observable stream, so the
  screens follow sign-out and account switches.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `profile`: replaces the "Profile shows a placeholder user identity" requirement (resolves its `> Note:` gap) with
  an account-derived identity, including the hidden-name case and the avatar initial rule; also covers the Home
  header avatar, which the same note called out.

## Impact

- **Backends touched:** Firebase Auth only (read of the current user). TMDB, BFF and Firebase AI: none.
- **Code:** `domain/model` (new user model), `domain/repository/AuthRepository`, `data/repository/AuthRepositoryImpl`,
  a new use case, a new `ProfileViewModel`/`ProfileUiState`, `ProfileScreen`, and `HomeScreen`/`HomeViewModel`
  (avatar initial). Test fake `FakeAuthRepository` gains the new stream.
- **Dependencies:** none.

## Out of scope

- A name field at sign-up, or writing a display name to the account.
- Profile photo (`photoUrl`) or editing profile details.
- Deriving a username from the email.
- The inert "Preferences", "Notifications" and "About" rows.

## Pre-production debt

None. This change removes a hardcoded-user-data placeholder rather than adding one.
