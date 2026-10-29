# Design

## Context

`MovieTabBar` renders four `TabBarItem`s in a `Row`; the selected one is tinted `primary`. Icons come from `Icons.Outlined.*` and `AppIcons.BookmarkOutline`, drawn at `Sizes.TabIcon` (24dp). `MainScaffold` derives the active tab from the nav back stack, so selection from the Home avatar or system back already flows through `active`. See proposal.md for motivation and the approved preview.

## Goals / Non-Goals

**Goals:**
- One animation per tab, driven by selection state only, so every selection path (tap, avatar, back) animates the same way.
- Self-contained inside the tab bar; no change to `MainScaffold` or navigation.

**Non-Goals:**
- Lottie or AnimatedVectorDrawable assets.
- Reusable icon-animation framework.

## Decisions

1. **Compose `Canvas` drawings per tab in `TabIcons.kt`, driven by an `Animatable<Float>` progress (0 to 1).** Each icon is a composable taking `progress` and `color`. Alternative: AnimatedVectorDrawable XML. Rejected because CLAUDE.md prohibits new XML for UI, and spring overshoot is awkward there. Alternative: Lottie. Rejected because it adds a dependency and a runtime for four tiny icons.
2. **Animation starts on the `selected` false-to-true edge via `LaunchedEffect(selected)`.** Selection state comes from `active`, so avatar and back-press selection animate too. Re-tapping the selected tab does not change `selected`, so nothing replays. On start, a tab animates from 0; an unselected tab rests at progress 1 drawn outlined, so rapid taps always end in a resting state.
3. **Strokes draw with `PathMeasure.getSegment`; squash, drop and pop use `spring` specs.** Per-tab sequencing (roof then walls then door; handle then glint) is expressed as sub-ranges of the single progress value, so timings stay in one place.
4. **Selection pill is a single `animateDpAsState` x-offset in `MovieTabBar`, computed from the active index and tab width.** Alternative: a pill inside each item with a crossfade. Rejected because a sliding pill needs one shared element.
5. **Reduced motion: read `Settings.Global.ANIMATOR_DURATION_SCALE`.** When 0, snap progress and offset to final values (`snapTo`). The decision is a small pure function taking the scale value, so it is JVM-testable.
6. **Drawing helpers that are pure math (segment ranges per tab, pill offset from index) live in a plain Kotlin file with no android imports** so the spec scenarios can be covered by JVM tests.
7. **Colour from `MaterialTheme.colorScheme.primary` / `movieColors.inactiveTab`; pill from `primary` at about 14% alpha.** No new theme tokens.

## Risks / Trade-offs

- [Canvas icons drift from Material icon shapes] → Draw to the same 24dp grid and compare the resting state against the current icons in the preview.
- [Rapid taps leave an icon half-drawn] → Each effect snaps the previously selected tab to rest on deselection.
- [Recomposition cost from animating progress] → Read progress inside `drawBehind`/`Canvas` lambdas so only draw runs, not composition.
- [No instrumented tests, so motion itself is unverified in CI] → Cover the pure math with JVM tests and verify visually on a device.

## Open Questions

- Haptic tick on select is out of scope for now; revisit after seeing the motion on a device.
