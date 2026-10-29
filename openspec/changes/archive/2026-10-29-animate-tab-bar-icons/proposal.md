# Proposal

## Why

The bottom tab bar only recolours the icon when a tab is selected, so switching sections feels flat. A short, tab-specific animation gives clear feedback on selection and gives the app more character. A browser preview of the motion was reviewed and approved, including the playful sparks and smoke.

## What Changes

- Selecting a tab plays a short animation on its icon: Home builds the house and releases a puff of smoke, Search swings the magnifier in with a glint, Watchlist drops the bookmark and fills it with sparks, Profile draws the ring and pops the avatar.
- A soft accent pill slides to the selected tab, and the label settles in.
- The selected icon shows a soft accent fill.
- Animation is skipped when the user has turned system animations off.
- No haptic feedback is added (deferred decision).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-navigation`: adds requirements for selection animation on the bottom tab bar. Existing tab behavior (order, selection, state retention) is unchanged.

## Impact

- Backends touched: none.
- Code: `ui/components/MovieTabBar.kt` (selection indicator, per-tab animation trigger) and a new `ui/components/TabIcons.kt` (animated icon drawings). The existing tab icons from Material and `AppIcons.BookmarkOutline` are replaced by these drawings on the tab bar only.
- Dependencies: none.

## Out of scope

- Haptic feedback on tab select.
- Animated transitions between the tab screens' content.
- Animating icons anywhere other than the bottom tab bar.
- Changing tab order, labels, routes or navigation behavior.

## Pre-production debt

None.
