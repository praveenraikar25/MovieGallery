## Context

Profile is a top-level tab inside `MainScaffold`'s own nested `NavHost`. The bottom bar switches tabs with
`navigate(route) { popUpTo(start) { saveState = true }; launchSingleTop = true; restoreState = true }`. The Home avatar
is a plain `InitialAvatar` with no click handling. No backend is involved.

## Goals / Non-Goals

**Goals:** make the Home avatar switch to the Profile tab with identical navigation behaviour to the bottom bar.

**Non-Goals:** a separate Profile destination or route; making other avatars tappable.

## Decisions

- **Reuse tab switching, not a new route.** Extract the bottom bar's `navigate(...)` block in `MainScaffold` into one
  local `navigateToTab(route)` and call it from both the bar and `HomeScreen(onProfileClick = ...)`. This keeps
  state-restore, no-duplicate-on-reselect and "back returns to Home" behaviour identical, with no new route.
- **`InitialAvatar` gets an optional `onClick: (() -> Unit)? = null`.** When set, clip to a circle and use
  `clickable(onClickLabel = ..., role = Role.Button)`; when null the composable is unchanged, so Profile and cast
  tiles are unaffected.
- **`HomeScreen` takes `onProfileClick`**, keeping navigation out of `HomeViewModel`.

## Risks / Trade-offs

- **Test coverage:** the behaviour lives in Composables and `NavHostController`; there is no ViewModel logic and the
  project has no Compose UI tests, so the "one JVM test per scenario" rule cannot be met with the existing test setup.
  Scenarios are verified manually on a device/emulator; adding Compose test infrastructure is deliberately left out of
  this small change. Flagged for the reviewer.
- The 32dp avatar is below the 48dp touch-target guideline; the clickable area is widened to 48dp without changing
  the visual size (e.g. via `minimumInteractiveComponentSize`).
