# Proposal

## Why

The account avatar at the top right of Home looks tappable but does nothing. Users expect it to lead to their
profile, and Profile already exists as a bottom tab.

## What Changes

- Tapping the avatar at the top right of Home switches to the Profile tab, exactly as tapping the Profile tab in the
  bottom bar does (Profile becomes the selected tab; tab state is saved and restored as usual).
- The avatar is announced to screen readers as a button ("Open profile").

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `app-navigation`: adds the requirement "Home avatar opens Profile", a second way to reach the Profile tab.

## Impact

- **Backends touched:** none (TMDB, BFF, Firebase AI, Firebase Auth: none).
- **Code:** `ui/components/InitialAvatar` (optional click handler), `ui/screens/home/HomeScreen` (new
  `onProfileClick` parameter), `ui/navigation/MainScaffold` (shared tab-switch function used by the bottom bar and
  the avatar).
- **Dependencies:** none.

## Out of scope

- Making the avatar tappable anywhere other than Home (Profile screen, cast tiles).
- Opening Profile as a separate screen above the tabs, or any new route.
- Changing what Profile shows.

## Pre-production debt

None.
