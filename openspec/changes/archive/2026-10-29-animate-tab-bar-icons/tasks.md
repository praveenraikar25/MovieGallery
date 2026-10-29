# Tasks

## 1. Motion math and reduced-motion decision

- [x] 1.1 Add a pure Kotlin file (no android imports) with per-tab sub-range helpers, the pill offset-from-index function and the animations-enabled decision from an animator scale; verify with JVM unit tests under `app/src/test/.../ui/components/` covering: scale 0 disables motion, scale above 0 enables it, pill offset for each of the four indices, and sub-ranges stay within 0 to 1.
- [x] 1.2 Run `./gradlew :app:testDebugUnitTest --tests "com.raikar.moviegallery.ui.components.*"` and verify it passes.

## 2. Animated icons

- [ ] 2.1 Create `ui/components/TabIcons.kt` with the Home drawing (roof, walls, door, squash, smoke) taking progress and colour; verify it compiles and previews at progress 0, 0.5 and 1 in an Android Studio preview.
- [ ] 2.2 Add the Search drawing (arc swing, handle draw, glint) and verify previews at the same three progress values.
- [ ] 2.3 Add the Watchlist drawing (drop, top-down fill, three sparks) and verify previews.
- [ ] 2.4 Add the Profile drawing (ring draw, shoulders, head pop, ripple) and verify previews.
- [ ] 2.5 Confirm each drawing at progress 1 matches its current Material icon footprint on the 24dp grid, and that the soft selected fill appears; verify side by side in previews, light and dark.

## 3. Tab bar integration

- [ ] 3.1 Update `MovieTabBar.kt` to render the new icons, start each tab's animation on the unselected-to-selected edge, and snap deselected tabs to rest; verify on a device that tapping, the Home avatar and system back all animate, and re-tapping the current tab does not replay.
- [ ] 3.2 Add the sliding selection pill and label settle; verify the pill slides between all four tabs and ends behind the last tab when tapping quickly.
- [ ] 3.3 Wire the system animator-scale check so motion is skipped when it is 0; verify on a device with animations turned off that tabs switch with no motion.
- [x] 3.4 Run `./gradlew ktlintCheck test assembleDebug` and verify all pass.

## 4. Spec sync

- [ ] 4.1 Confirm each scenario in `specs/app-navigation/spec.md` is covered by a JVM test (tasks 1.1) or by the device checks in 3.1 to 3.3, and note any scenario verified only on device in the PR description.

## Workflow follow-up

- Archive the change after review so the new requirements merge into the app-navigation spec.
